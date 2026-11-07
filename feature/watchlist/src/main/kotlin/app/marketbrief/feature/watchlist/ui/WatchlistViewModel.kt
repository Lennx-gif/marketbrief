package app.marketbrief.feature.watchlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.AlertDirection
import app.marketbrief.core.data.AlertEvaluator
import app.marketbrief.core.data.AlertSnapshot
import app.marketbrief.core.data.CalendarEvent
import app.marketbrief.core.data.EconomicCalendarProvider
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.PriceAlertNotifier
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Quote
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.db.PriceAlertDao
import app.marketbrief.core.storage.db.PriceAlertEntity
import app.marketbrief.core.storage.db.WatchlistDao
import app.marketbrief.core.storage.db.WatchlistItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val marketData: MarketDataRepository,
    private val alertDao: PriceAlertDao,
    private val alertNotifier: PriceAlertNotifier,
    private val calendarProvider: EconomicCalendarProvider,
    private val watchlistDao: WatchlistDao,
    private val prefs: UserPrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> =
        _uiState.stateIn(viewModelScope, SharingStarted.Eagerly, WatchlistUiState())
    private var persistedItems: List<WatchlistItemEntity> = emptyList()
    private var quoteSnapshot: Map<AssetId, Quote> = emptyMap()

    init {
        viewModelScope.launch {
            combine(
                watchlistDao.observeAll(),
                prefs.watchlistSortOrder,
                prefs.watchlistViewMode
            ) { items, sortOrder, viewMode -> Triple(items, sortOrder, viewMode) }
                .collect { (items, sortOrder, viewMode) ->
                    persistedItems = items
                    _uiState.update { it.copy(sortOrder = sortOrder, viewMode = viewMode) }
                    publishWatchlistItems()
                }
        }
        viewModelScope.launch { seedDefaultWatchlistIfNeeded() }
        viewModelScope.launch {
            combine(
                prefs.calendarImpactFilter,
                prefs.watchlistExpanded,
                prefs.calendarExpanded,
                prefs.alertsExpanded
            ) { impact, watchlistExpanded, calendarExpanded, alertsExpanded ->
                listOf(impact, watchlistExpanded.toString(), calendarExpanded.toString(), alertsExpanded.toString())
            }.collect { values ->
                _uiState.update {
                    it.copy(
                        calendarImpactFilter = values[0],
                        watchlistExpanded = values[1].toBoolean(),
                        calendarExpanded = values[2].toBoolean(),
                        alertsExpanded = values[3].toBoolean()
                    )
                }
            }
        }
        viewModelScope.launch {
            prefs.providerBudgetRemaining("twelve_data", TWELVE_DATA_DAILY_CREDITS)
                .collect { remaining -> _uiState.update { it.copy(twelveDataCreditsRemaining = remaining) } }
        }
        viewModelScope.launch {
            alertDao.observeActive().collect { alerts -> _uiState.update { it.copy(priceAlerts = alerts) } }
        }
        viewModelScope.launch {
            calendarProvider.observeEvents().collect { events ->
                _uiState.update { state ->
                    state.copy(economicEvents = events.map { event ->
                        EconomicEvent(
                            event.title,
                            event.currency,
                            Instant.ofEpochMilli(event.instantEpochMillis)
                                .atZone(ZoneId.of("Africa/Nairobi"))
                                .format(DateTimeFormatter.ofPattern("EEE HH:mm")),
                            event.impact,
                            event.instantEpochMillis,
                            event.id
                        )
                    })
                }
            }
        }
        viewModelScope.launch {
            marketData.quotes.collect { quotes ->
                quoteSnapshot = quotes
                publishWatchlistItems()
                evaluateAlerts(quotes)
            }
        }
    }

    private suspend fun seedDefaultWatchlistIfNeeded() {
        if (prefs.watchlistSeeded.first()) return
        if (watchlistDao.all().isEmpty()) {
            watchlistDao.upsert(
                DEFAULT_WATCHLIST.mapIndexed { index, item ->
                    WatchlistItemEntity(item.first, item.second.name, index, System.currentTimeMillis())
                }
            )
        }
        prefs.setWatchlistSeeded()
    }

    private fun publishWatchlistItems() {
        val now = System.currentTimeMillis()
        val items = persistedItems.mapNotNull { entity ->
            val assetClass = AssetClass.entries.firstOrNull { it.name == entity.assetClass } ?: return@mapNotNull null
            val quote = quoteSnapshot[AssetId(entity.symbol, assetClass)]
            val change = quote?.changePercent
            WatchlistItem(
                symbol = entity.symbol,
                name = instrumentName(entity.symbol),
                assetClass = assetClass,
                currentPrice = quote?.let { BigDecimal.valueOf(it.mid) },
                changePercent = change,
                isBullish = change?.let { it >= 0.0 },
                quoteUpdatedAt = quote?.timestamp ?: 0L,
                isStale = quote != null && now - quote.timestamp > STALE_QUOTE_MS
            )
        }
        val sorted = when (_uiState.value.sortOrder) {
            "SYMBOL" -> items.sortedBy { it.symbol }
            "CHANGE_DESC" -> items.sortedWith(compareBy<WatchlistItem> { it.changePercent == null }.thenByDescending { it.changePercent })
            "CHANGE_ASC" -> items.sortedWith(compareBy<WatchlistItem> { it.changePercent == null }.thenBy { it.changePercent })
            else -> items
        }
        _uiState.update {
            it.copy(
                watchlistItems = sorted,
                lastQuoteUpdatedAt = sorted.maxOfOrNull { item -> item.quoteUpdatedAt } ?: 0L
            )
        }
    }

    fun refresh(manual: Boolean = false) {
        viewModelScope.launch {
            val ids = watchlistDao.all().map { row ->
                val type = AssetClass.entries.firstOrNull { it.name == row.assetClass } ?: return@map null
                AssetId(row.symbol, type)
            }.filterNotNull()
            if (ids.isEmpty()) return@launch
            if (!manual && _uiState.value.twelveDataCreditsRemaining <= ids.size + MANUAL_REFRESH_RESERVE) {
                _uiState.update { it.copy(refreshMessage = "Automatic refresh paused to reserve Twelve Data credits.") }
                return@launch
            }
            _uiState.update { it.copy(isRefreshing = true, refreshMessage = null) }
            try {
                marketData.refreshQuotes(ids)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun toggleItem(symbol: String) {
        if (persistedItems.any { it.symbol == symbol }) removeItem(symbol) else addItem(symbol)
    }

    fun addItem(symbol: String) {
        val normalized = symbol.trim().uppercase()
        if (normalized.isBlank() || persistedItems.any { it.symbol == normalized }) return
        val assetClass = assetClassFor(normalized)
        viewModelScope.launch {
            val rows = watchlistDao.all()
            if (rows.any { it.symbol == normalized }) return@launch
            watchlistDao.upsert(
                listOf(WatchlistItemEntity(normalized, assetClass.name, rows.size, System.currentTimeMillis()))
            )
            marketData.getQuote(AssetId(normalized, assetClass)).onFailure {
                _uiState.update { state -> state.copy(refreshMessage = "Price unavailable; the instrument remains saved.") }
            }
        }
    }

    fun removeItem(symbol: String) {
        viewModelScope.launch { watchlistDao.delete(symbol) }
    }

    fun reorder(symbol: String, newPosition: Int) {
        viewModelScope.launch {
            val items = watchlistDao.all().toMutableList()
            val from = items.indexOfFirst { it.symbol == symbol }
            if (from < 0 || newPosition !in items.indices) return@launch
            val item = items.removeAt(from)
            items.add(newPosition, item)
            watchlistDao.upsert(items.mapIndexed { index, row -> row.copy(position = index) })
        }
    }

    fun setSortOrder(value: String) {
        viewModelScope.launch { prefs.setWatchlistSortOrder(value) }
    }

    fun setViewMode(value: String) {
        viewModelScope.launch { prefs.setWatchlistViewMode(value) }
    }

    fun setCalendarImpactFilter(value: String) {
        viewModelScope.launch { prefs.setCalendarImpactFilter(value) }
    }

    fun setWatchlistExpanded(value: Boolean) {
        viewModelScope.launch { prefs.setWatchlistExpanded(value) }
    }

    fun setCalendarExpanded(value: Boolean) {
        viewModelScope.launch { prefs.setCalendarExpanded(value) }
    }

    fun setAlertsExpanded(value: Boolean) {
        viewModelScope.launch { prefs.setAlertsExpanded(value) }
    }

    fun addPriceAlert(symbol: String, target: String, direction: AlertDirection) {
        val item = _uiState.value.watchlistItems.firstOrNull { it.symbol == symbol } ?: return
        val price = target.toBigDecimalOrNull()
        if (price == null || price <= BigDecimal.ZERO) {
            _uiState.update { it.copy(alertMessage = "Enter a valid target price greater than zero.") }
            return
        }
        viewModelScope.launch {
            alertDao.upsert(
                PriceAlertEntity(
                    UUID.randomUUID().toString(),
                    symbol,
                    item.assetClass.name,
                    price.toPlainString(),
                    direction.name,
                    System.currentTimeMillis(),
                    null,
                    item.currentPrice?.toPlainString()
                )
            )
            _uiState.update { it.copy(alertMessage = "Price alert saved.") }
        }
    }

    fun deletePriceAlert(id: String) {
        viewModelScope.launch { alertDao.delete(id) }
    }

    fun addCalendarEvent(title: String, currency: String, instant: String, impact: String): Boolean {
        val epoch = runCatching { Instant.parse(instant).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(instant).toInstant().toEpochMilli() }.getOrNull()
        if (title.isBlank() || currency.isBlank() || epoch == null ||
            impact.uppercase() !in setOf("LOW", "MEDIUM", "HIGH")
        ) {
            _uiState.update { it.copy(calendarMessage = "Enter a title, currency, valid ISO-8601 time, and impact.") }
            return false
        }
        viewModelScope.launch {
            calendarProvider.add(
                CalendarEvent(
                    UUID.randomUUID().toString(),
                    title.trim(),
                    currency.trim().uppercase(),
                    epoch,
                    impact.uppercase(),
                    "USER"
                )
            )
            _uiState.update { it.copy(calendarMessage = "Event added.") }
        }
        return true
    }

    private suspend fun evaluateAlerts(quotes: Map<AssetId, Quote>) {
        alertDao.active().forEach { entity ->
            val assetClass = AssetClass.entries.firstOrNull { it.name == entity.assetClass } ?: return@forEach
            val direction = AlertDirection.entries.firstOrNull { it.name == entity.direction } ?: return@forEach
            val price = quotes[AssetId(entity.symbol, assetClass)]?.mid ?: return@forEach
            val target = entity.targetPrice.toDoubleOrNull() ?: return@forEach
            val evaluation = AlertEvaluator.evaluate(
                AlertSnapshot(entity.id, target, direction, entity.lastCheckedPrice?.toDoubleOrNull()),
                price
            )
            alertDao.upsert(
                entity.copy(
                    lastCheckedPrice = price.toString(),
                    triggeredAt = if (evaluation.crossed) System.currentTimeMillis() else null
                )
            )
            if (evaluation.crossed) {
                alertNotifier.notify(entity.id, entity.symbol, entity.targetPrice)
                _uiState.update { it.copy(alertMessage = "${entity.symbol} crossed ${entity.targetPrice}") }
            }
        }
    }

    private fun assetClassFor(symbol: String): AssetClass = when {
        symbol in setOf("AAPL", "MSFT", "NVDA", "TSLA", "AMZN", "GOOGL") -> AssetClass.STOCK
        symbol.contains("XAU") || symbol.contains("XAG") -> AssetClass.COMMODITY
        symbol.contains("BTC") || symbol.contains("ETH") -> AssetClass.CRYPTO
        symbol.contains("SPX") || symbol.contains("US30") || symbol.contains("NAS") -> AssetClass.INDEX
        else -> AssetClass.FOREX
    }

    private fun instrumentName(symbol: String): String = when (symbol) {
        "EURUSD" -> "Euro / US Dollar"
        "GBPUSD" -> "British Pound / US Dollar"
        "XAUUSD" -> "Spot Gold"
        "AAPL" -> "Apple Inc."
        "BTCUSD" -> "Bitcoin"
        "MSFT" -> "Microsoft Corp."
        "NVDA" -> "Nvidia Corp."
        "TSLA" -> "Tesla Inc."
        "AMZN" -> "Amazon.com Inc."
        "GOOGL" -> "Alphabet Inc."
        "SPX500" -> "S&P 500 Index"
        "US30" -> "Dow Jones Industrial"
        "NAS100" -> "Nasdaq 100 Index"
        "ETHUSD" -> "Ethereum"
        "USDJPY" -> "US Dollar / Japanese Yen"
        "USDCHF" -> "US Dollar / Swiss Franc"
        "USDCAD" -> "US Dollar / Canadian Dollar"
        "AUDUSD" -> "Australian Dollar / US Dollar"
        "NZDUSD" -> "New Zealand Dollar / US Dollar"
        else -> symbol
    }

    companion object {
        const val TWELVE_DATA_DAILY_CREDITS = 800
        const val MANUAL_REFRESH_RESERVE = 100
        const val REFRESH_INTERVAL_MILLIS = 3 * 60_000L
        private const val STALE_QUOTE_MS = 5 * 60_000L
        private val DEFAULT_WATCHLIST = listOf(
            "EURUSD" to AssetClass.FOREX,
            "GBPUSD" to AssetClass.FOREX,
            "XAUUSD" to AssetClass.COMMODITY,
            "AAPL" to AssetClass.STOCK,
            "BTCUSD" to AssetClass.CRYPTO
        )
    }
}

data class WatchlistUiState(
    val watchlistItems: List<WatchlistItem> = emptyList(),
    val economicEvents: List<EconomicEvent> = emptyList(),
    val priceAlerts: List<PriceAlertEntity> = emptyList(),
    val alertMessage: String? = null,
    val calendarMessage: String? = null,
    val refreshMessage: String? = null,
    val isRefreshing: Boolean = false,
    val sortOrder: String = "MANUAL",
    val viewMode: String = "DETAILED",
    val calendarImpactFilter: String = "ALL",
    val watchlistExpanded: Boolean = true,
    val calendarExpanded: Boolean = true,
    val alertsExpanded: Boolean = true,
    val twelveDataCreditsRemaining: Int = TWELVE_DATA_DAILY_CREDITS,
    val lastQuoteUpdatedAt: Long = 0L
) {
    companion object {
        private const val TWELVE_DATA_DAILY_CREDITS = 800
    }
}

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val assetClass: AssetClass,
    val currentPrice: BigDecimal?,
    val changePercent: Double? = null,
    val isBullish: Boolean? = null,
    val quoteUpdatedAt: Long = 0L,
    val isStale: Boolean = false
)

data class EconomicEvent(
    val title: String,
    val currency: String,
    val time: String,
    val impact: String,
    val instant: Long = 0L,
    val id: String = ""
)
