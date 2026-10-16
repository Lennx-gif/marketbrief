package app.marketbrief.core.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class InstrumentTest {

    @Test
    fun jpyPairs_usePointZeroOnePip() {
        val i = Instrument.forex("USD/JPY")
        assertEquals("USDJPY", i.symbol)
        assertEquals(0, BigDecimal("0.01").compareTo(i.pipSize))
        assertEquals(CurrencyCode("JPY"), i.quoteCurrency)
        assertEquals(CurrencyCode("USD"), i.baseCurrency)
        assertEquals(0, BigDecimal("100000").compareTo(i.contractSize))
    }

    @Test
    fun otherPairs_usePointZeroZeroZeroOnePip() {
        val i = Instrument.forex("eurgbp")
        assertEquals("EURGBP", i.symbol)
        assertEquals(0, BigDecimal("0.0001").compareTo(i.pipSize))
    }

    @Test(expected = IllegalArgumentException::class)
    fun badForexSymbol_isRejected() {
        Instrument.forex("EURUS")
    }

    @Test
    fun defaultCatalog_hasTwentyEightForexPairs_andNormalisesLookups() {
        val catalog = InstrumentCatalog.default()
        assertEquals(28, catalog.all().count { it.assetClass == AssetClass.FOREX })
        assertNotNull(catalog.find("usd/jpy"))
        assertNotNull(catalog.find("XAUUSD"))
    }

    @Test
    fun unknownSymbol_isNotGuessedAsForex() {
        assertNull(InstrumentCatalog.default().find("BTCUSD"))
    }

    @Test
    fun withInstrument_addsWithoutMutatingOriginal() {
        val original = InstrumentCatalog.default()
        val btc = Instrument(
            symbol = "BTCUSD",
            assetClass = AssetClass.CRYPTO,
            quoteCurrency = CurrencyCode("USD"),
            baseCurrency = CurrencyCode("BTC"),
            pipSize = BigDecimal("1"),
            contractSize = BigDecimal("1"),
            lotStep = BigDecimal("0.001"),
            minLot = BigDecimal("0.001"),
        )
        val extended = original.withInstrument(btc)
        assertNotNull(extended.find("BTC/USD"))
        assertNull(original.find("BTCUSD"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCurrencyCode_isRejected() {
        CurrencyCode("us")
    }
}
