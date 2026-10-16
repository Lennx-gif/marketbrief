package app.marketbrief.core.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RateSourceTest {

    private fun bd(s: String) = BigDecimal(s)
    private fun ccy(s: String) = CurrencyCode(s)

    private val table = PairRateTable(
        mapOf(
            "USD/JPY" to bd("150.00"),
            "eurusd" to bd("1.1000"),
            "GBPUSD" to bd("1.2500"),
        )
    )

    private fun assertClose(expected: BigDecimal, actual: BigDecimal?) {
        assertNotNull(actual)
        val diff = expected.subtract(actual).abs()
        assertTrue("expected $expected but was $actual", diff < bd("0.000000000001"))
    }

    @Test
    fun sameCurrency_isOne() {
        assertEquals(0, BigDecimal.ONE.compareTo(table.rate(ccy("JPY"), ccy("JPY"))))
    }

    @Test
    fun direct_usesPairPrice() {
        assertEquals(0, bd("1.25").compareTo(table.rate(ccy("GBP"), ccy("USD"))))
        assertEquals(0, bd("150").compareTo(table.rate(ccy("USD"), ccy("JPY"))))
    }

    @Test
    fun inverse_dividesOneByPrice() {
        assertClose(BigDecimal.ONE.divide(bd("150"), CALC), table.rate(ccy("JPY"), ccy("USD")))
    }

    @Test
    fun oneHopThroughUsd() {
        // JPY to EUR needs USDJPY and EURUSD: 1 / (150 * 1.10)
        assertClose(BigDecimal.ONE.divide(bd("165"), CALC), table.rate(ccy("JPY"), ccy("EUR")))
        // GBP to EUR: 1.25 / 1.10
        assertClose(bd("1.25").divide(bd("1.10"), CALC), table.rate(ccy("GBP"), ccy("EUR")))
    }

    @Test
    fun unknownCurrency_isNull() {
        assertNull(table.rate(ccy("AUD"), ccy("USD")))
        assertNull(table.rate(ccy("USD"), ccy("CHF")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonPositivePrice_isRejected() {
        PairRateTable(mapOf("EURUSD" to bd("0")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedKey_isRejected() {
        PairRateTable(mapOf("EUR" to bd("1.1")))
    }
}
