package app.marketbrief.feature.analysis

import app.marketbrief.core.analysis.MarketAnalyzer
import app.marketbrief.core.data.InMemoryNewsRepository
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.positioning.CrowdPosition
import app.marketbrief.core.data.positioning.PositioningRepository
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.Quote
import app.marketbrief.core.model.Timeframe
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.feature.analysis.ui.AnalysisMode
import app.marketbrief.feature.analysis.ui.AnalysisViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeMarketData(private val fail: Boolean = false) : MarketDataRepository {
        override val quotes = MutableStateFlow<Map<AssetId, Quote>>(emptyMap())
        override suspend fun getCandles(assetId: AssetId, timeframe: Timeframe, limit: Int) =
            if (fail) Result.failure(IllegalStateException("no data"))
            else Result.success((1..limit).map { i ->
                Candle(i * 3_600_000L, 100.0 + i * 0.1, 101.0 + i * 0.1, 99.0 + i * 0.1, 100.5 + i * 0.1, 1000.0)
            })
        override suspend fun getQuote(assetId: AssetId): Result<Quote> = Result.failure(IllegalStateException())
        override suspend fun refreshQuotes(ids: Collection<AssetId>) {}
        override suspend fun searchSymbols(query: String) = Result.success(emptyList<AssetId>())
    }

    private class FakePositioning(connected: Boolean = false) : PositioningRepository {
        override val connected = MutableStateFlow(connected)
        override val positions = MutableStateFlow(
            if (connected) listOf(CrowdPosition("EURUSD", 62.0, 38.0, 10.0, 6.0)) else emptyList()
        )
        override val lastUpdated = MutableStateFlow<Long?>(null)
        override suspend fun loadConnectionState() {}
        override suspend fun connect(email: String, password: String) = Result.success(Unit)
        override suspend fun disconnect() {}
        override suspend fun refresh(force: Boolean) = Result.success(Unit)
    }

    private fun createViewModel(fail: Boolean = false, crowd: Boolean = false): AnalysisViewModel {
        val repo = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = mockk<UserPrefsRepository>(relaxed = true)
        every { prefs.analysisView } returns flowOf("SENTIMENT")
        return AnalysisViewModel(repo, prefs, FakeMarketData(fail), FakePositioning(crowd))
    }

    @Test
    fun testAnalysisViewModelInitialState() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val state = viewModel.uiState.value

        assertNotNull(state)
        assertEquals(AnalysisMode.SENTIMENT, state.mode)
        assertEquals("EURUSD", state.selectedAsset)
        assertEquals("H1", state.selectedTimeframe)
        assertTrue(state.availableAssets.contains("EURUSD"))
        assertNotNull(state.snapshot)
    }

    @Test
    fun testAnalysisViewModelSelectAsset() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        viewModel.onAssetSelected("GBPUSD")

        val state = viewModel.uiState.value
        assertEquals("GBPUSD", state.selectedAsset)
        assertNotNull(state.snapshot)
        assertEquals("GBPUSD", state.snapshot?.assetId?.symbol)
    }

    @Test
    fun testFailedCandleLoadShowsErrorInsteadOfFakeData() = runTest {
        val viewModel = createViewModel(fail = true)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val state = viewModel.uiState.value
        assertEquals(null, state.snapshot)
        assertEquals("no data", state.technicalError)
    }

    @Test
    fun testCrowdPositioningComesFromRepository() = runTest {
        val viewModel = createViewModel(crowd = true)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val state = viewModel.uiState.value
        assertTrue(state.crowdConnected)
        assertEquals(62.0, state.crowd.single().longPercent, 0.0)
    }

    @Test
    fun testMarketAnalyzer() {
        val candles = (1..50).map { i ->
            Candle(
                timestamp = i.toLong() * 3600000,
                open = 100.0 + i * 0.1,
                high = 101.0 + i * 0.1,
                low = 99.0 + i * 0.1,
                close = 100.5 + i * 0.1,
                volume = 1000.0
            )
        }
        val assetId = AssetId("EURUSD", AssetClass.FOREX)
        val analyzer = MarketAnalyzer()
        val snapshot = analyzer.analyze(assetId, candles, candles.last().close)

        assertEquals(assetId, snapshot.assetId)
        assertNotNull(snapshot.rsi14)
        assertNotNull(snapshot.trend)
        assertNotNull(snapshot.pivotPoints)
    }
}