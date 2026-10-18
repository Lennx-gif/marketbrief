package app.marketbrief.feature.watchlist.ui

import androidx.lifecycle.ViewModel
import app.marketbrief.core.model.AssetClass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                watchlistItems = listOf(
                    WatchlistItem("EURUSD", "Euro / US Dollar", AssetClass.FOREX, BigDecimal("1.0850"), 0.32, true),
                    WatchlistItem("GBPUSD", "British Pound / US Dollar", AssetClass.FOREX, BigDecimal("1.2650"), -0.18, false),
                    WatchlistItem("XAUUSD", "Spot Gold", AssetClass.COMMODITY, BigDecimal("2645.20"), 0.84, true),
                    WatchlistItem("AAPL", "Apple Inc.", AssetClass.STOCK, BigDecimal("228.50"), 1.15, true),
                    WatchlistItem("BTCUSD", "Bitcoin", AssetClass.CRYPTO, BigDecimal("68420.00"), 2.40, true)
                ),
                economicEvents = listOf(
                    EconomicEvent("US Non-Farm Payrolls", "USD", "12:30 UTC", "High"),
                    EconomicEvent("ECB Interest Rate Decision", "EUR", "13:45 UTC", "High"),
                    EconomicEvent("UK CPI (YoY)", "GBP", "07:00 UTC", "Medium"),
                    EconomicEvent("US Initial Jobless Claims", "USD", "13:30 UTC", "Medium")
                )
            )
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
    }

    fun removeItem(symbol: String) {
        _uiState.update { it.copy(watchlistItems = it.watchlistItems.filter { it.symbol != symbol }) }
    }
}

data class WatchlistUiState(
    val watchlistItems: List<WatchlistItem> = emptyList(),
    val economicEvents: List<EconomicEvent> = emptyList()
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