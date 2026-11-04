package app.marketbrief.feature.watchlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.CalendarEvent
import app.marketbrief.core.data.EconomicCalendarProvider
import app.marketbrief.core.data.AlertDirection
import app.marketbrief.core.data.AlertEvaluator
import app.marketbrief.core.data.AlertSnapshot
import app.marketbrief.core.data.PriceAlertNotifier
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.storage.db.PriceAlertDao
import app.marketbrief.core.storage.db.PriceAlertEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val marketData: MarketDataRepository,
    private val alertDao: PriceAlertDao,
    private val alertNotifier: PriceAlertNotifier,
    private val calendarProvider: EconomicCalendarProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                watchlistItems = listOf(
                    WatchlistItem("EURUSD", "Euro / US Dollar", AssetClass.FOREX, null),
                    WatchlistItem("GBPUSD", "British Pound / US Dollar", AssetClass.FOREX, null),
                    WatchlistItem("XAUUSD", "Spot Gold", AssetClass.COMMODITY, null),
                    WatchlistItem("AAPL", "Apple Inc.", AssetClass.STOCK, null),
                    WatchlistItem("BTCUSD", "Bitcoin", AssetClass.CRYPTO, null)
                )
            )
        }
        viewModelScope.launch {
            alertDao.observeActive().collect { alerts -> _uiState.update { it.copy(priceAlerts = alerts) } }
        }
        viewModelScope.launch {
            calendarProvider.observeEvents().collect { events ->
                _uiState.update { state -> state.copy(economicEvents = events.map { event ->
                    EconomicEvent(event.title, event.currency, Instant.ofEpochMilli(event.instantEpochMillis).atZone(ZoneId.of("Africa/Nairobi")).format(DateTimeFormatter.ofPattern("EEE HH:mm")), event.impact, event.instantEpochMillis, event.id)
                }) }
            }
        }
    }

    init {
        viewModelScope.launch {
            marketData.quotes.collect { quotes ->
                _uiState.update { state ->
                    state.copy(
                        watchlistItems = state.watchlistItems.map { item ->
                            val q = quotes[AssetId(item.symbol, item.assetClass)] ?: return@map item
                            val change = q.changePercent ?: 0.0
                            item.copy(
                                currentPrice = BigDecimal.valueOf(q.mid),
                                changePercent = change,
                                isBullish = change >= 0.0
                            )
                        }
                    )
                }
                evaluateAlerts(quotes)
            }
        }
        refresh()
    }

    /** Fetches live prices. Costs one Twelve Data credit per symbol; cached quotes are reused for two minutes. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val ids = _uiState.value.watchlistItems.map { AssetId(it.symbol, it.assetClass) }
            marketData.refreshQuotes(ids)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleItem(symbol: String) {
        val exists = _uiState.value.watchlistItems.any { it.symbol == symbol }
        if (exists) {
            removeItem(symbol)
        } else {
            addItem(symbol)
        }
    }

    fun addItem(symbol: String) {
        if (_uiState.value.watchlistItems.any { it.symbol == symbol }) return

        val assetClass = when {
            symbol in listOf("AAPL", "MSFT", "NVDA", "TSLA", "AMZN", "GOOGL") -> AssetClass.STOCK
            symbol.contains("XAU") || symbol.contains("XAG") -> AssetClass.COMMODITY
            symbol.contains("BTC") || symbol.contains("ETH") -> AssetClass.CRYPTO
            symbol.contains("SPX") || symbol.contains("US30") || symbol.contains("NAS") -> AssetClass.INDEX
            else -> AssetClass.FOREX
        }

        val name = when (symbol) {
            "AAPL" -> "Apple Inc."
            "MSFT" -> "Microsoft Corp."
            "NVDA" -> "Nvidia Corp."
            "TSLA" -> "Tesla Inc."
            "AMZN" -> "Amazon.com Inc."
            "GOOGL" -> "Alphabet Inc."
            "SPX500" -> "S&P 500 Index"
            "US30" -> "Dow Jones Industrial"
            "NAS100" -> "Nasdaq 100 Index"
            "BTCUSD" -> "Bitcoin"
            "ETHUSD" -> "Ethereum"
            "USDJPY" -> "US Dollar / Japanese Yen"
            "USDCHF" -> "US Dollar / Swiss Franc"
            "USDCAD" -> "US Dollar / Canadian Dollar"
            "AUDUSD" -> "Australian Dollar / USD"
            "NZDUSD" -> "New Zealand Dollar / USD"
            else -> symbol
        }

        val newItem = WatchlistItem(
            symbol = symbol,
            name = name,
            assetClass = assetClass,
            currentPrice = null,
            changePercent = 0.0,
            isBullish = true
        )
        _uiState.update { it.copy(watchlistItems = it.watchlistItems + newItem) }
        viewModelScope.launch { marketData.getQuote(AssetId(symbol, assetClass)) }
    }

    fun removeItem(symbol: String) {
        _uiState.update { it.copy(watchlistItems = it.watchlistItems.filter { it.symbol != symbol }) }
    }

    fun addPriceAlert(symbol: String, target: String, direction: AlertDirection) {
        val item = _uiState.value.watchlistItems.firstOrNull { it.symbol == symbol } ?: return
        if (target.toDoubleOrNull() == null) return
        viewModelScope.launch {
            alertDao.upsert(PriceAlertEntity(UUID.randomUUID().toString(), symbol, item.assetClass.name, target, direction.name, System.currentTimeMillis(), null, _uiState.value.watchlistItems.firstOrNull { it.symbol == symbol }?.currentPrice?.toPlainString()))
        }
    }

    fun deletePriceAlert(id: String) { viewModelScope.launch { alertDao.delete(id) } }

    fun addCalendarEvent(title: String, currency: String, instant: String, impact: String) {
        val epoch = runCatching { Instant.parse(instant).toEpochMilli() }
            .recoverCatching { java.time.OffsetDateTime.parse(instant).toInstant().toEpochMilli() }.getOrNull() ?: return
        viewModelScope.launch { calendarProvider.add(CalendarEvent(UUID.randomUUID().toString(), title, currency.uppercase(), epoch, impact.uppercase(), "USER")) }
    }

    fun importCalendarCsv(csv: String) {
        viewModelScope.launch {
            val count = calendarProvider.importCsv(csv)
            _uiState.update { it.copy(calendarMessage = "$count events imported") }
        }
    }

    private suspend fun evaluateAlerts(quotes: Map<AssetId, app.marketbrief.core.model.Quote>) {
        alertDao.active().forEach { entity ->
            val assetClass = runCatching { AssetClass.valueOf(entity.assetClass) }.getOrDefault(AssetClass.FOREX)
            val price = quotes[AssetId(entity.symbol, assetClass)]?.mid ?: return@forEach
            val target = entity.targetPrice.toDoubleOrNull() ?: return@forEach
            val evaluation = AlertEvaluator.evaluate(AlertSnapshot(entity.id, target, AlertDirection.valueOf(entity.direction), entity.lastCheckedPrice?.toDoubleOrNull()), price)
            alertDao.upsert(entity.copy(lastCheckedPrice = price.toString(), triggeredAt = if (evaluation.crossed) System.currentTimeMillis() else null))
            if (evaluation.crossed) {
                alertNotifier.notify(entity.id, entity.symbol, entity.targetPrice)
                _uiState.update { it.copy(alertMessage = "${entity.symbol} crossed ${entity.targetPrice}") }
            }
        }
    }
}

data class WatchlistUiState(
    val watchlistItems: List<WatchlistItem> = emptyList(),
    val economicEvents: List<EconomicEvent> = emptyList(),
    val priceAlerts: List<PriceAlertEntity> = emptyList(),
    val alertMessage: String? = null,
    val calendarMessage: String? = null,
    val isRefreshing: Boolean = false
)

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val assetClass: AssetClass,
    val currentPrice: BigDecimal?,
    val changePercent: Double = 0.0,
    val isBullish: Boolean = true
)

data class EconomicEvent(
    val title: String,
    val currency: String,
    val time: String,
    val impact: String,
    val instant: Long = 0L,
    val id: String = ""
)
