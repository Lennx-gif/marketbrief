package app.marketbrief.feature.analysis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.analysis.MarketAnalyzer
import app.marketbrief.core.analysis.MarketSnapshot
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class AnalysisViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState

    private val analyzer = MarketAnalyzer()

    private val availableAssets = listOf(
        "EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD",
        "AUDUSD", "NZDUSD", "EURGBP", "EURJPY", "GBPJPY",
        "XAUUSD", "BTCUSD", "ETHUSD", "SPX500", "US30"
    )

    init {
        _uiState.update { it.copy(availableAssets = availableAssets, selectedAsset = availableAssets.first()) }
        loadSnapshot()
    }

    fun onAssetSelected(asset: String) {
        _uiState.update { it.copy(selectedAsset = asset) }
        loadSnapshot()
    }

    fun onTimeframeSelected(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        loadSnapshot()
    }

    private fun loadSnapshot() {
        val state = _uiState.value
        viewModelScope.launch {
            val candles = generateMockCandles(state.selectedAsset, state.selectedTimeframe)
            val currentPrice = candles.last().close
            val assetId = AssetId(state.selectedAsset, AssetClass.FOREX)
            val snapshot = analyzer.analyze(assetId, candles, currentPrice)
            _uiState.update { it.copy(snapshot = snapshot, isLoading = false) }
        }
    }

    private fun generateMockCandles(symbol: String, timeframe: String): List<Candle> {
        val candles = mutableListOf<Candle>()
        var price = when (symbol) {
            "EURUSD" -> 1.0850
            "GBPUSD" -> 1.2650
            "USDJPY" -> 150.25
            "XAUUSD" -> 2000.0
            "BTCUSD" -> 45000.0
            else -> 1.0
        }

        val now = System.currentTimeMillis()
        val intervalMs = when (timeframe) {
            "M1" -> 60_000L
            "M5" -> 300_000L
            "M15" -> 900_000L
            "M30" -> 1_800_000L
            "H1" -> 3_600_000L
            "H4" -> 14_400_000L
            "D1" -> 86_400_000L
            "W1" -> 604_800_000L
            else -> 3_600_000L
        }

        for (i in 199 downTo 0) {
            val timestamp = now - (i * intervalMs)
            val change = (Math.random() - 0.5) * 0.002 * price
            val open = price
            val close = price + change
            val high = max(open, close) + Math.abs(Math.random() * 0.001 * price)
            val low = min(open, close) - Math.abs(Math.random() * 0.001 * price)
            val volume = (Math.random() * 1000).toDouble()

            candles.add(Candle(timestamp, open, high, low, close, volume))
            price = close
        }

        return candles
    }
}

data class AnalysisUiState(
    val availableAssets: List<String> = emptyList(),
    val selectedAsset: String = "EURUSD",
    val selectedTimeframe: String = "H1",
    val snapshot: MarketSnapshot? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)