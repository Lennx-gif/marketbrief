package app.marketbrief.feature.analysis

import app.marketbrief.core.analysis.MarketAnalyzer
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.feature.analysis.ui.AnalysisViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

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

    @Test
    fun testAnalysisViewModelInitialState() = runTest {
        val viewModel = AnalysisViewModel()
        
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals("EURUSD", state.selectedAsset)
        assertEquals("H1", state.selectedTimeframe)
        assertTrue(state.availableAssets.contains("EURUSD"))
        assertNotNull(state.snapshot)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun testAnalysisViewModelSelectAsset() = runTest {
        val viewModel = AnalysisViewModel()
        viewModel.onAssetSelected("GBPUSD")
        
        val state = viewModel.uiState.value
        assertEquals("GBPUSD", state.selectedAsset)
        assertNotNull(state.snapshot)
        assertEquals("GBPUSD", state.snapshot?.assetId?.symbol)
    }

    @Test
    fun testAnalysisViewModelSelectTimeframe() = runTest {
        val viewModel = AnalysisViewModel()
        viewModel.onTimeframeSelected("M15")
        
        val state = viewModel.uiState.value
        assertEquals("M15", state.selectedTimeframe)
        assertNotNull(state.snapshot)
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