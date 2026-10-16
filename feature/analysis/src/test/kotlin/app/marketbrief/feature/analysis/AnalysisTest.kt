package app.marketbrief.feature.analysis

import app.marketbrief.core.analysis.MarketAnalyzer
import app.marketbrief.core.data.InMemoryNewsRepository
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.feature.analysis.ui.AnalysisMode
import app.marketbrief.feature.analysis.ui.AnalysisViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun createViewModel(): AnalysisViewModel {
        val repo = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = mockk<UserPrefsRepository>(relaxed = true)
        every { prefs.analysisView } returns flowOf("SENTIMENT")
        return AnalysisViewModel(repo, prefs)
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