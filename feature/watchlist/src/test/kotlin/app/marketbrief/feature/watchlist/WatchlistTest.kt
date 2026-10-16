package app.marketbrief.feature.watchlist

import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.Quote
import app.marketbrief.feature.watchlist.ui.EconomicEvent
import app.marketbrief.feature.watchlist.ui.WatchlistItem
import app.marketbrief.feature.watchlist.ui.WatchlistViewModel
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
class WatchlistTest {

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
    fun testAssetIdCreation() {
        val assetId = AssetId("EURUSD", AssetClass.FOREX)
        assertEquals("EURUSD", assetId.symbol)
        assertEquals(AssetClass.FOREX, assetId.assetClass)
    }

    @Test
    fun testEconomicEventCreation() {
        val event = EconomicEvent(
            title = "FOMC Rate Decision",
            currency = "USD",
            time = "18:00",
            impact = "High"
        )
        
        assertEquals("FOMC Rate Decision", event.title)
        assertEquals("USD", event.currency)
        assertEquals("18:00", event.time)
        assertEquals("High", event.impact)
    }

    @Test
    fun testWatchlistViewModelInitialState() = runTest {
        val viewModel = WatchlistViewModel()
        
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state.watchlistItems.isNotEmpty())
        assertTrue(state.economicEvents.isNotEmpty())
    }

    @Test
    fun testWatchlistViewModelAddAndRemoveItem() = runTest {
        val viewModel = WatchlistViewModel()
        val initialCount = viewModel.uiState.value.watchlistItems.size
        
        viewModel.addItem("BTCUSD")
        assertEquals(initialCount + 1, viewModel.uiState.value.watchlistItems.size)
        assertTrue(viewModel.uiState.value.watchlistItems.any { it.symbol == "BTCUSD" })
        
        viewModel.removeItem("BTCUSD")
        assertEquals(initialCount, viewModel.uiState.value.watchlistItems.size)
        assertFalse(viewModel.uiState.value.watchlistItems.any { it.symbol == "BTCUSD" })
    }

    @Test
    fun testQuoteCreation() {
        val assetId = AssetId("EURUSD", AssetClass.FOREX)
        val quote = Quote(
            assetId = assetId,
            bid = 1.0850,
            ask = 1.0852,
            timestamp = System.currentTimeMillis()
        )
        
        assertEquals(1.0850, quote.bid, 0.0001)
        assertEquals(1.0852, quote.ask, 0.0001)
        assertEquals(assetId, quote.assetId)
    }

    @Test
    fun testCandleCreation() {
        val candle = Candle(
            timestamp = System.currentTimeMillis(),
            open = 1.0850,
            high = 1.0860,
            low = 1.0840,
            close = 1.0855,
            volume = 1000.0
        )
        
        assertEquals(1.0850, candle.open, 0.0001)
        assertEquals(1.0855, candle.close, 0.0001)
    }
}