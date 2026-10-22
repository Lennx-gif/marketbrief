package app.marketbrief.feature.watchlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val marketData: MarketDataRepository
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
                ),
                economicEvents = listOf(  // Sample schedule: no calendar provider is connected yet.
                    EconomicEvent("US Non-Farm Payrolls", "USD", "12:30 UTC", "High"),
                    EconomicEvent("ECB Interest Rate Decision", "EUR", "13:45 UTC", "High"),
                    EconomicEvent("UK CPI (YoY)", "GBP", "07:00 UTC", "Medium"),
                    EconomicEvent("US Initial Jobless Claims", "USD", "13:30 UTC", "Medium")
                )
            )
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
}

data class WatchlistUiState(
    val watchlistItems: List<WatchlistItem> = emptyList(),
    val economicEvents: List<EconomicEvent> = emptyList(),
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
    val impact: String
)