package app.marketbrief.feature.watchlist

import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.Quote
import app.marketbrief.core.model.Timeframe
import app.marketbrief.feature.watchlist.ui.EconomicEvent
import app.marketbrief.feature.watchlist.ui.WatchlistItem
import app.marketbrief.feature.watchlist.ui.WatchlistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val fakeMarketData = object : MarketDataRepository {
        val state = MutableStateFlow<Map<AssetId, Quote>>(emptyMap())
        override val quotes = state
        override suspend fun getCandles(assetId: AssetId, timeframe: Timeframe, limit: Int) =
            Result.success(emptyList<Candle>())
        override suspend fun getQuote(assetId: AssetId): Result<Quote> = Result.failure(IllegalStateException("offline"))
        override suspend fun refreshQuotes(ids: Collection<AssetId>) {}
        override suspend fun searchSymbols(query: String) = Result.success(emptyList<AssetId>())
    }

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
        val viewModel = WatchlistViewModel(fakeMarketData)
        
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state.watchlistItems.isNotEmpty())
        assertTrue(state.economicEvents.isNotEmpty())
    }

    @Test
    fun testWatchlistViewModelAddAndRemoveItem() = runTest {
        val viewModel = WatchlistViewModel(fakeMarketData)
        val initialCount = viewModel.uiState.value.watchlistItems.size
        
        viewModel.addItem("ETHUSD")
        assertEquals(initialCount + 1, viewModel.uiState.value.watchlistItems.size)
        assertTrue(viewModel.uiState.value.watchlistItems.any { it.symbol == "ETHUSD" })
        
        viewModel.removeItem("ETHUSD")
        assertEquals(initialCount, viewModel.uiState.value.watchlistItems.size)
        assertFalse(viewModel.uiState.value.watchlistItems.any { it.symbol == "ETHUSD" })
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

    @Test
    fun testLiveQuoteUpdatesPriceAndDirection() = runTest {
        val viewModel = WatchlistViewModel(fakeMarketData)
        val eur = AssetId("EURUSD", AssetClass.FOREX)
        fakeMarketData.state.value = mapOf(eur to Quote(eur, 1.1, 1.1, 0L, changePercent = -0.4))

        val item = viewModel.uiState.value.watchlistItems.first { it.symbol == "EURUSD" }
        assertEquals(1.1, item.currentPrice!!.toDouble(), 1e-9)
        assertEquals(-0.4, item.changePercent, 1e-9)
        assertFalse(item.isBullish)
    }
}
