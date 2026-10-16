package app.marketbrief.core.data

import app.marketbrief.core.model.Article
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.MarketDataSource
import app.marketbrief.core.model.Quote
import app.marketbrief.core.model.Timeframe
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.Instrument
import app.marketbrief.core.risk.PairRateTable
import org.junit.Test
import org.junit.Assert.*

class DataTest {

    @Test
    fun testArticleCreation() {
        val article = Article(
            id = "test-1",
            title = "Test Article",
            summary = "Test summary",
            url = "https://example.com",
            imageUrl = "https://example.com/image.jpg",
            sourceId = "test-source",
            sourceName = "Test Source",
            publishedAt = System.currentTimeMillis(),
            categories = listOf("Forex", "Technical"),
            relatedSymbols = listOf("EURUSD", "GBPUSD")
        )
        
        assertEquals("test-1", article.id)
        assertEquals("Test Article", article.title)
        assertEquals(2, article.categories.size)
        assertEquals(2, article.relatedSymbols.size)
    }

    @Test
    fun testAssetIdCreation() {
        val assetId = AssetId("EURUSD", AssetClass.FOREX)
        assertEquals("EURUSD", assetId.symbol)
        assertEquals(AssetClass.FOREX, assetId.assetClass)
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
    fun testPairRateTable() {
        val pairs = mapOf(
            "EURUSD" to java.math.BigDecimal("1.0850"),
            "GBPUSD" to java.math.BigDecimal("1.2650"),
            "USDJPY" to java.math.BigDecimal("150.25")
        )
        
        val rateTable = PairRateTable(pairs)
        
        // Direct rate
        val eurUsd = rateTable.rate(CurrencyCode("EUR"), CurrencyCode("USD"))
        assertNotNull(eurUsd)
        assertEquals(java.math.BigDecimal("1.0850"), eurUsd!!)
        
        // Inverse rate
        val usdEur = rateTable.rate(CurrencyCode("USD"), CurrencyCode("EUR"))
        assertNotNull(usdEur)
        assertEquals(java.math.BigDecimal("1").divide(java.math.BigDecimal("1.0850"), 10, java.math.RoundingMode.HALF_UP), usdEur!!)
        
        // Cross rate through USD
        val eurJpy = rateTable.rate(CurrencyCode("EUR"), CurrencyCode("JPY"))
        assertNotNull(eurJpy)
        // EURUSD * USDJPY = 1.0850 * 150.25 = 163.02125
        val expected = java.math.BigDecimal("1.0850").multiply(java.math.BigDecimal("150.25"))
        assertEquals(expected.setScale(10, java.math.RoundingMode.HALF_UP), eurJpy!!.setScale(10, java.math.RoundingMode.HALF_UP))
    }

    @Test
    fun testInstrumentForex() {
        val instrument = Instrument.forex("EURUSD")
        
        assertEquals("EURUSD", instrument.symbol)
        assertEquals(AssetClass.FOREX, instrument.assetClass)
        assertEquals(CurrencyCode("USD"), instrument.quoteCurrency)
        assertEquals(CurrencyCode("EUR"), instrument.baseCurrency)
        assertEquals(java.math.BigDecimal("0.0001"), instrument.pipSize)
        assertEquals(java.math.BigDecimal("100000"), instrument.contractSize)
    }

    @Test
    fun testInstrumentForexJpy() {
        val instrument = Instrument.forex("USDJPY")
        
        assertEquals("USDJPY", instrument.symbol)
        assertEquals(CurrencyCode("JPY"), instrument.quoteCurrency)
        assertEquals(CurrencyCode("USD"), instrument.baseCurrency)
        assertEquals(java.math.BigDecimal("0.01"), instrument.pipSize)
    }
}