package app.marketbrief.core.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RiskConverterTest {

    @Test
    fun `standard percent to cash conversion and roundtrip`() {
        val balance = BigDecimal("10000.00")
        val percent = BigDecimal("1.00")

        val cash = RiskConverter.percentToCash(balance, percent)
        assertEquals(BigDecimal("100.00"), cash)

        val convertedPercent = RiskConverter.cashToPercent(balance, cash)
        assertEquals(BigDecimal("1.00"), convertedPercent)
    }

    @Test
    fun `rounding mode HALF_UP is applied properly`() {
        val balance = BigDecimal("3333.33")
        val percent = BigDecimal("1.00")
        val cash = RiskConverter.percentToCash(balance, percent)
        assertEquals(BigDecimal("33.33"), cash)

        val balance2 = BigDecimal("1000.00")
        val cash2 = BigDecimal("15.55")
        val percent2 = RiskConverter.cashToPercent(balance2, cash2)
        assertEquals(BigDecimal("1.56"), percent2)
    }

    @Test
    fun `zero or negative balance returns null`() {
        val percent = BigDecimal("1.00")
        val cash = BigDecimal("100.00")

        assertNull(RiskConverter.percentToCash(BigDecimal.ZERO, percent))
        assertNull(RiskConverter.percentToCash(BigDecimal("-1000"), percent))
        assertNull(RiskConverter.cashToPercent(BigDecimal.ZERO, cash))
        assertNull(RiskConverter.cashToPercent(BigDecimal("-1000"), cash))
    }

    @Test
    fun `negative risk input returns null`() {
        val balance = BigDecimal("10000.00")
        assertNull(RiskConverter.percentToCash(balance, BigDecimal("-1.00")))
        assertNull(RiskConverter.cashToPercent(balance, BigDecimal("-100.00")))
    }

    @Test
    fun `very small balance conversion`() {
        val balance = BigDecimal("0.50")
        val percent = BigDecimal("2.00")
        val cash = RiskConverter.percentToCash(balance, percent)
        assertEquals(BigDecimal("0.01"), cash)
    }

    @Test
    fun `very large balance conversion`() {
        val balance = BigDecimal("10000000000.00")
        val percent = BigDecimal("0.50")
        val cash = RiskConverter.percentToCash(balance, percent)
        assertEquals(BigDecimal("50000000.00"), cash)
    }

    @Test
    fun `high risk threshold detection`() {
        assertFalse(RiskConverter.isHighRisk(BigDecimal("1.00")))
        assertFalse(RiskConverter.isHighRisk(BigDecimal("5.00")))
        assertTrue(RiskConverter.isHighRisk(BigDecimal("5.01")))
        assertTrue(RiskConverter.isHighRisk(BigDecimal("10.00")))
        assertFalse(RiskConverter.isHighRisk(null))
    }
}
