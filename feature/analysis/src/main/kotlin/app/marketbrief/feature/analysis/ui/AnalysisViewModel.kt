package app.marketbrief.feature.analysis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.analysis.MarketAnalyzer
import app.marketbrief.core.analysis.MarketSnapshot
import app.marketbrief.core.analysis.sentiment.SentimentAggregator
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.data.positioning.CrowdPosition
import app.marketbrief.core.data.positioning.PositioningRepository
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.FeedQuery
import app.marketbrief.core.model.Sentiment
import app.marketbrief.core.model.Timeframe
import app.marketbrief.core.storage.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val technicalError: String? = null,
    val isLoading: Boolean = false,
    // Myfxbook crowd positioning
    val crowdConnected: Boolean = false,
    val crowd: List<CrowdPosition> = emptyList(),
    val crowdError: String? = null
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val prefs: UserPrefsRepository,
    private val marketData: MarketDataRepository,
    private val positioning: PositioningRepository
) : ViewModel() {

    private val mode = MutableStateFlow(AnalysisMode.SENTIMENT)
    private val timeWindow = MutableStateFlow(SentimentTimeWindow.H24)
    private val technicalAsset = MutableStateFlow("EURUSD")
    private val technicalTimeframe = MutableStateFlow("H1")
    private val technicalSnapshot = MutableStateFlow<MarketSnapshot?>(null)
    private val technicalError = MutableStateFlow<String?>(null)
    private val technicalLoading = MutableStateFlow(false)
    private val crowdError = MutableStateFlow<String?>(null)

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
        combine(technicalAsset, technicalTimeframe, technicalSnapshot) { asset, tf, snap -> Triple(asset, tf, snap) },
        technicalError,
        technicalLoading
    ) { (asset, tf, snap), error, loading ->
        TechnicalData(asset, tf, snap, error, loading)
    }

    private val crowdFlow = combine(
        positioning.connected,
        positioning.positions,
        crowdError
    ) { connected, positions, error ->
        CrowdData(connected, positions, error)
    }

    val uiState: StateFlow<AnalysisUiState> = combine(
        mode,
        timeWindow,
        sentimentFlow,
        technicalFlow,
        crowdFlow
    ) { m, tw, sentData, techData, crowdData ->
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
            technicalError = techData.error,
            isLoading = techData.loading,
            crowdConnected = crowdData.connected,
            crowd = crowdData.positions,
            crowdError = crowdData.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState(availableAssets = availableAssets))

    init {
        loadTechnicalSnapshot("EURUSD", "H1")
        refreshCrowd()
    }

    /** Safe to call whenever the Sentiment tab opens: the repository skips the network if the cache is fresh. */
    fun refreshCrowd(force: Boolean = false) {
        viewModelScope.launch {
            crowdError.value = positioning.refresh(force).exceptionOrNull()
                ?.takeUnless { it is app.marketbrief.core.data.positioning.PositioningError.NotConnected }
                ?.message
        }
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
            technicalLoading.value = true
            technicalError.value = null
            val tf = Timeframe.entries.firstOrNull { it.name == timeframe } ?: Timeframe.H1
            val assetId = AssetId(symbol, assetClassOf(symbol))
            marketData.getCandles(assetId, tf, CANDLE_COUNT)
                .onSuccess { candles ->
                    if (candles.isEmpty()) {
                        technicalSnapshot.value = null
                        technicalError.value = "No price data returned for $symbol."
                    } else {
                        technicalSnapshot.value = analyzer.analyze(assetId, candles, candles.last().close)
                    }
                }
                .onFailure { e ->
                    technicalSnapshot.value = null
                    technicalError.value = e.message ?: "Could not load price data."
                }
            technicalLoading.value = false
        }
    }

    private fun assetClassOf(symbol: String): AssetClass = when {
        symbol.startsWith("XAU") || symbol.startsWith("XAG") -> AssetClass.COMMODITY
        symbol.startsWith("BTC") || symbol.startsWith("ETH") -> AssetClass.CRYPTO
        symbol in setOf("SPX500", "US30", "NAS100") -> AssetClass.INDEX
        else -> AssetClass.FOREX
    }

    private companion object {
        const val CANDLE_COUNT = 200
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
    val snapshot: MarketSnapshot?,
    val error: String?,
    val loading: Boolean
)

private data class CrowdData(
    val connected: Boolean,
    val positions: List<CrowdPosition>,
    val error: String?
)
