package app.marketbrief.core.data.remote

import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Timeframe
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TwelveDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    private val eurusd = AssetId("EURUSD", AssetClass.FOREX)

    @Test
    fun `quote maps last price and percent change`() {
        val dto = json.decodeFromString<TwelveQuoteDto>(
            """{"symbol":"EUR/USD","name":"Euro","close":"1.08520","percent_change":"0.31","timestamp":1729500000,"is_market_open":true}"""
        )
        val q = TwelveDataSource.mapQuote(eurusd, dto)!!
        assertEquals(1.0852, q.bid, 1e-9)
        assertEquals(1.0852, q.ask, 1e-9)
        assertEquals(0.31, q.changePercent!!, 1e-9)
        assertEquals(1_729_500_000_000L, q.timestamp)
    }

    @Test
    fun `quote without a price is rejected`() {
        val dto = json.decodeFromString<TwelveQuoteDto>("""{"symbol":"EUR/USD"}""")
        assertNull(TwelveDataSource.mapQuote(eurusd, dto))
    }

    @Test
    fun `candles come back oldest first and bad rows are dropped`() {
        val dto = json.decodeFromString<TwelveTimeSeriesDto>(
            """{"meta":{"symbol":"EUR/USD"},"status":"ok","values":[
              {"datetime":"2024-10-21 15:00:00","open":"1.0860","high":"1.0870","low":"1.0850","close":"1.0865"},
              {"datetime":"2024-10-21 14:00:00","open":"1.0850","high":"1.0865","low":"1.0845","close":"1.0860"},
              {"datetime":"2024-10-21 13:00:00","open":"x","high":"1","low":"1","close":"1"}]}"""
        )
        val candles = TwelveDataSource.mapCandles(dto)
        assertEquals(2, candles.size)
        assertTrue(candles[0].timestamp < candles[1].timestamp)
        assertEquals(1.0860, candles[0].close, 1e-9)
        assertEquals(0.0, candles[0].volume, 0.0)
    }

    @Test
    fun `daily candles use a date-only timestamp`() {
        assertEquals(1_729_468_800_000L, TwelveDataSource.parseDateTime("2024-10-21"))
    }

    @Test
    fun `error bodies map to typed errors`() {
        val rate = assertThrows(MarketDataException::class.java) {
            TwelveDataSource.throwIfError("error", 429, "run out of API credits")
        }
        assertTrue(rate.error is MarketDataError.RateLimited)
        val key = assertThrows(MarketDataException::class.java) {
            TwelveDataSource.throwIfError("error", 401, "apikey invalid")
        }
        assertTrue(key.error is MarketDataError.BadKey)
        TwelveDataSource.throwIfError("ok", null, null) // no throw
    }

    @Test
    fun `mapper and interval names`() {
        assertEquals("EUR/USD", SymbolMapper.toTwelveData(eurusd))
        assertEquals("4h", SymbolMapper.interval(Timeframe.H4))
        assertEquals("1day", SymbolMapper.interval(Timeframe.D1))
    }
}
