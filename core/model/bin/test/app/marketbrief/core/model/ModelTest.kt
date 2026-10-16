package app.marketbrief.core.model

import org.junit.Test
import org.junit.Assert.*

class ModelTest {

    @Test
    fun testAssetIdCreation() {
        val assetId = AssetId("EURUSD", AssetClass.FOREX)
        assertEquals("EURUSD", assetId.symbol)
        assertEquals(AssetClass.FOREX, assetId.assetClass)
        assertEquals("EURUSD (FOREX)", assetId.toString())
    }

    @Test
    fun testAssetIdEquality() {
        val asset1 = AssetId("EURUSD", AssetClass.FOREX)
        val asset2 = AssetId("EURUSD", AssetClass.FOREX)
        val asset3 = AssetId("GBPUSD", AssetClass.FOREX)
        
        assertEquals(asset1, asset2)
        assertNotEquals(asset1, asset3)
    }

    @Test
    fun testAssetIdHashCode() {
        val asset1 = AssetId("EURUSD", AssetClass.FOREX)
        val asset2 = AssetId("EURUSD", AssetClass.FOREX)
        
        assertEquals(asset1.hashCode(), asset2.hashCode())
    }

    @Test
    fun testCurrencyCodeValidation() {
        val usd = CurrencyCode("USD")
        assertEquals("USD", usd.code)
        
        // Test invalid codes
        try {
            CurrencyCode("usd") // lowercase
            fail("Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        
        try {
            CurrencyCode("US") // too short
            fail("Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testCurrencyCodeEquality() {
        val ccy1 = CurrencyCode("USD")
        val ccy2 = CurrencyCode("USD")
        val ccy3 = CurrencyCode("EUR")
        
        assertEquals(ccy1, ccy2)
        assertNotEquals(ccy1, ccy3)
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
    fun testCandleProperties() {
        val timestamp = System.currentTimeMillis()
        val candle = Candle(
            timestamp = timestamp,
            open = 1.0850,
            high = 1.0860,
            low = 1.0840,
            close = 1.0855,
            volume = 1000.0
        )
        
        assertEquals(timestamp, candle.timestamp)
        assertEquals(1.0850, candle.open, 0.0001)
        assertEquals(1.0860, candle.high, 0.0001)
        assertEquals(1.0840, candle.low, 0.0001)
        assertEquals(1.0855, candle.close, 0.0001)
        assertEquals(1000.0, candle.volume, 0.001)
    }

    @Test
    fun testQuoteProperties() {
        val assetId = AssetId("EURUSD", AssetClass.FOREX)
        val timestamp = System.currentTimeMillis()
        val quote = Quote(
            assetId = assetId,
            bid = 1.0850,
            ask = 1.0852,
            timestamp = timestamp
        )
        
        assertEquals(assetId, quote.assetId)
        assertEquals(1.0850, quote.bid, 0.0001)
        assertEquals(1.0852, quote.ask, 0.0001)
        assertEquals(timestamp, quote.timestamp)
        assertEquals(1.0851, quote.mid, 0.0001)
        assertEquals(0.0002, quote.spread, 0.0001)
    }

    @Test
    fun testArticleProperties() {
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
        assertEquals("Test summary", article.summary)
        assertEquals("https://example.com", article.url)
        assertEquals("https://example.com/image.jpg", article.imageUrl)
        assertEquals("test-source", article.sourceId)
        assertEquals("Test Source", article.sourceName)
        assertEquals(2, article.categories.size)
        assertEquals(2, article.relatedSymbols.size)
    }

    @Test
    fun testTimeframeValues() {
        assertEquals(60_000L, Timeframe.M1.millis)
        assertEquals(300_000L, Timeframe.M5.millis)
        assertEquals(900_000L, Timeframe.M15.millis)
        assertEquals(3_600_000L, Timeframe.H1.millis)
        assertEquals(14_400_000L, Timeframe.H4.millis)
        assertEquals(86_400_000L, Timeframe.D1.millis)
        assertEquals(604_800_000L, Timeframe.W1.millis)
    }

    @Test
    fun testAssetClassValues() {
        assertEquals("FOREX", AssetClass.FOREX.name)
        assertEquals("CRYPTO", AssetClass.CRYPTO.name)
        assertEquals("EQUITY", AssetClass.EQUITY.name)
        assertEquals("COMMODITY", AssetClass.COMMODITY.name)
        assertEquals("INDEX", AssetClass.INDEX.name)
    }
}