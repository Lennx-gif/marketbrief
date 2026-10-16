package app.marketbrief.feature.watchlist.ui

import androidx.lifecycle.ViewModel
import app.marketbrief.core.model.AssetClass
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

@HiltViewModel
class WatchlistViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                watchlistItems = listOf(
                    WatchlistItem("EURUSD", AssetClass.FOREX, BigDecimal("1.0850")),
                    WatchlistItem("GBPUSD", AssetClass.FOREX, BigDecimal("1.2650")),
                    WatchlistItem("XAUUSD", AssetClass.COMMODITY, BigDecimal("2000.00"))
                ),
                economicEvents = listOf(
                    EconomicEvent("US Non-Farm Payrolls", "USD", "12:30", "High"),
                    EconomicEvent("ECB Interest Rate Decision", "EUR", "13:45", "High"),
                    EconomicEvent("UK CPI", "GBP", "07:00", "Medium")
                )
            )
        }
    }

    fun addItem(symbol: String) {
        val assetClass = when {
            symbol.contains("XAU") || symbol.contains("XAG") -> AssetClass.COMMODITY
            symbol.contains("BTC") || symbol.contains("ETH") -> AssetClass.CRYPTO
            symbol.contains("SPX") || symbol.contains("US30") -> AssetClass.INDEX
            else -> AssetClass.FOREX
        }

        val newItem = WatchlistItem(symbol, assetClass, null)
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
    val assetClass: AssetClass,
    val currentPrice: BigDecimal?
)

data class EconomicEvent(
    val title: String,
    val currency: String,
    val time: String,
    val impact: String
)