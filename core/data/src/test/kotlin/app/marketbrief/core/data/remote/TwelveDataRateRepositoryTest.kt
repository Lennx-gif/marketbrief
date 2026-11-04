package app.marketbrief.core.data.remote

import app.marketbrief.core.data.MarketDataSource
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.MarketDataSource as ProviderId
import app.marketbrief.core.model.Quote
import app.marketbrief.core.model.Timeframe
import app.marketbrief.core.risk.CurrencyCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwelveDataRateRepositoryTest {

    private class FakeSource(var quotes: Map<String, Double>) : MarketDataSource {
        override val sourceId: ProviderId = ProviderId.TWELVE_DATA

        override suspend fun getQuote(assetId: AssetId): Result<Quote> {
            val price = quotes[assetId.symbol]
                ?: return Result.failure(IllegalStateException("no quote for ${assetId.symbol}"))
            return Result.success(Quote(assetId, price, price, 0L, null))
        }

        override suspend fun getCandles(assetId: AssetId, timeframe: Timeframe, limit: Int): Result<List<Candle>> =
            Result.failure(IllegalStateException("unused"))

        override suspend fun searchSymbols(query: String): Result<List<AssetId>> =
            Result.success(emptyList())
    }

    @Test
    fun refreshBuildsTableFromQuotes() = runTest {
        val repo = TwelveDataRateRepository(FakeSource(mapOf("EURUSD" to 1.10, "USDJPY" to 150.0)))

        repo.refresh(setOf("EURUSD", "USDJPY"))

        assertEquals(1.10, repo.getRate(CurrencyCode("EUR"), CurrencyCode("USD"))!!, 1e-9)
        assertTrue(Math.abs(repo.getRate(CurrencyCode("USD"), CurrencyCode("EUR"))!! - 1.0 / 1.10) < 1e-9)
    }

    @Test
    fun resolvesTwoHopRatesThroughUsdPivot() = runTest {
        val repo = TwelveDataRateRepository(FakeSource(mapOf("EURUSD" to 1.10, "USDJPY" to 150.0)))

        repo.refresh(setOf("EURUSD", "USDJPY"))

        val jpyToEur = repo.getRate(CurrencyCode("JPY"), CurrencyCode("EUR"))!!
        assertTrue(Math.abs(jpyToEur - 1.0 / (150.0 * 1.10)) < 1e-9)
        val eurToJpy = repo.getRate(CurrencyCode("EUR"), CurrencyCode("JPY"))!!
        assertTrue(Math.abs(eurToJpy - 1.10 * 150.0) < 1e-6)
    }

    @Test
    fun failedRefreshKeepsLastGoodTable() = runTest {
        val source = FakeSource(mapOf("EURUSD" to 1.10))
        val repo = TwelveDataRateRepository(source)

        repo.refresh(setOf("EURUSD"))
        assertEquals(1.10, repo.getRate(CurrencyCode("EUR"), CurrencyCode("USD"))!!, 1e-9)

        source.quotes = emptyMap()
        repo.refresh(setOf("EURUSD"))

        assertEquals(1.10, repo.getRate(CurrencyCode("EUR"), CurrencyCode("USD"))!!, 1e-9)
    }

    @Test
    fun emptyTableReturnsNull() = runTest {
        val repo = TwelveDataRateRepository(FakeSource(emptyMap()))

        repo.refresh(setOf("EURUSD"))

        assertNull(repo.getRate(CurrencyCode("EUR"), CurrencyCode("USD")))
    }
}
