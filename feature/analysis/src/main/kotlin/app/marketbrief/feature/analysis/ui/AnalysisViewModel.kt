package app.marketbrief.feature.analysis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.analysis.MarketAnalyzer
import app.marketbrief.core.analysis.MarketSnapshot
import app.marketbrief.core.analysis.sentiment.SentimentAggregator
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.FeedQuery
import app.marketbrief.core.model.Sentiment
import app.marketbrief.core.storage.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

enum class AnalysisMode {
    SENTIMENT,
    TECHNICALS
}

enum class SentimentTimeWindow(val label: String, val durationMs: Long) {
    H24("24 Hours", 24L * 60 * 60 * 1000),
    D7("7 Days", 7L * 24 * 60 * 60 * 1000),
    D30("30 Days", 30L * 24 * 60 * 60 * 1000)
}

data class AnalysisUiState(
    val mode: AnalysisMode = AnalysisMode.SENTIMENT,
    val timeWindow: SentimentTimeWindow = SentimentTimeWindow.H24,
    val sentiments: Map<String, Sentiment> = emptyMap(),
    val categoryVolumes: Map<String, Int> = emptyMap(),
    val showVolume: Boolean = false,
    // Technicals state
    val availableAssets: List<String> = emptyList(),
    val selectedAsset: String = "EURUSD",
    val selectedTimeframe: String = "H1",
    val snapshot: MarketSnapshot? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val prefs: UserPrefsRepository
) : ViewModel() {

    private val mode = MutableStateFlow(AnalysisMode.SENTIMENT)
    private val timeWindow = MutableStateFlow(SentimentTimeWindow.H24)
    private val technicalAsset = MutableStateFlow("EURUSD")
    private val technicalTimeframe = MutableStateFlow("H1")
    private val technicalSnapshot = MutableStateFlow<MarketSnapshot?>(null)

    private val analyzer = MarketAnalyzer()
    private val aggregator = SentimentAggregator()

    val availableAssets = listOf(
        "EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD",
        "AUDUSD", "NZDUSD", "EURGBP", "EURJPY", "GBPJPY",
        "XAUUSD", "BTCUSD", "ETHUSD", "SPX500", "US30"
    )

    private val sentimentFlow = combine(
        timeWindow,
        newsRepository.observeFeed(FeedQuery()),
        prefs.analysisView
    ) { tw, articles, viewPref ->
        val cutoff = System.currentTimeMillis() - tw.durationMs
        val windowArticles = articles.filter { it.publishedAt >= cutoff }
        val sentiments = aggregator.aggregate(windowArticles)

        val categoryCounts = windowArticles
            .flatMap { it.categories }
            .groupingBy { it }
            .eachCount()

        SentimentData(
            sentiments = sentiments,
            categoryVolumes = categoryCounts,
            showVolume = viewPref.equals("BOTH", ignoreCase = true)
        )
    }

    private val technicalFlow = combine(
        technicalAsset,
        technicalTimeframe,
        technicalSnapshot
    ) { asset, tf, snap ->
        TechnicalData(asset, tf, snap)
    }

    val uiState: StateFlow<AnalysisUiState> = combine(
        mode,
        timeWindow,
        sentimentFlow,
        technicalFlow
    ) { m, tw, sentData, techData ->
        AnalysisUiState(
            mode = m,
            timeWindow = tw,
            sentiments = sentData.sentiments,
            categoryVolumes = sentData.categoryVolumes,
            showVolume = sentData.showVolume,
            availableAssets = availableAssets,
            selectedAsset = techData.asset,
            selectedTimeframe = techData.timeframe,
            snapshot = techData.snapshot,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState(availableAssets = availableAssets))

    init {
        loadTechnicalSnapshot("EURUSD", "H1")
    }

    fun setMode(newMode: AnalysisMode) {
        mode.value = newMode
    }

    fun setTimeWindow(newWindow: SentimentTimeWindow) {
        timeWindow.value = newWindow
    }

    fun onAssetSelected(asset: String) {
        technicalAsset.value = asset
        loadTechnicalSnapshot(asset, technicalTimeframe.value)
    }

    fun onTimeframeSelected(timeframe: String) {
        technicalTimeframe.value = timeframe
        loadTechnicalSnapshot(technicalAsset.value, timeframe)
    }

    private fun loadTechnicalSnapshot(symbol: String, timeframe: String) {
        viewModelScope.launch {
            val candles = generateMockCandles(symbol, timeframe)
            val currentPrice = candles.last().close
            val assetId = AssetId(symbol, AssetClass.FOREX)
            technicalSnapshot.value = analyzer.analyze(assetId, candles, currentPrice)
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
            val volume = (Math.random() * 1000)

            candles.add(Candle(timestamp, open, high, low, close, volume))
            price = close
        }

        return candles
    }
}

private data class SentimentData(
    val sentiments: Map<String, app.marketbrief.core.model.Sentiment>,
    val categoryVolumes: Map<String, Int>,
    val showVolume: Boolean
)

private data class TechnicalData(
    val asset: String,
    val timeframe: String,
    val snapshot: MarketSnapshot?
)