package app.marketbrief.core.analysis

import app.marketbrief.core.model.Candle
import kotlin.math.max
import kotlin.math.min
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.math.RoundingMode

class IndicatorsTest {

    private fun createTestCandles(count: Int, basePrice: Double = 100.0): List<Candle> {
        val candles = mutableListOf<Candle>()
        var price = basePrice
        val now = System.currentTimeMillis()
        
        for (i in 0 until count) {
            val change = (Math.random() - 0.5) * 2.0
            val open = price
            val close = price + change
            val high = max(open, close) + Math.abs(Math.random() * 1.0)
            val low = min(open, close) - Math.abs(Math.random() * 1.0)
            val volume = (Math.random() * 1000).toDouble()
            
            candles.add(Candle(now + i * 3600000, open, high, low, close, volume))
            price = close
        }
        
        return candles
    }

    @Test
    fun testSma() {
        val candles = listOf(
            Candle(1, 10.0, 11.0, 9.0, 10.0, 100.0),
            Candle(2, 10.0, 11.0, 9.0, 11.0, 100.0),
            Candle(3, 11.0, 12.0, 10.0, 12.0, 100.0),
            Candle(4, 12.0, 13.0, 11.0, 13.0, 100.0),
            Candle(5, 13.0, 14.0, 12.0, 14.0, 100.0),
        )
        
        val sma = Indicators.sma(candles, 3)
        assertNotNull(sma)
        // Last 3 closes: 12, 13, 14 -> average = 13
        assertEquals(BigDecimal("13"), sma!!.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun testSmaInsufficientData() {
        val candles = listOf(
            Candle(1, 10.0, 11.0, 9.0, 10.0, 100.0),
            Candle(2, 10.0, 11.0, 9.0, 11.0, 100.0),
        )
        
        val sma = Indicators.sma(candles, 3)
        assertNull(sma)
    }

    @Test
    fun testEma() {
        val candles = listOf(
            Candle(1, 10.0, 11.0, 9.0, 10.0, 100.0),
            Candle(2, 10.0, 11.0, 9.0, 11.0, 100.0),
            Candle(3, 11.0, 12.0, 10.0, 12.0, 100.0),
            Candle(4, 12.0, 13.0, 11.0, 13.0, 100.0),
            Candle(5, 13.0, 14.0, 12.0, 14.0, 100.0),
        )
        
        val ema = Indicators.ema(candles, 3)
        assertNotNull(ema)
        // EMA should be close to the last values
        assertTrue(ema!!.compareTo(BigDecimal("12")) > 0)
        assertTrue(ema.compareTo(BigDecimal("14")) < 0)
    }

    @Test
    fun testRsi() {
        // Create candles with clear uptrend
        val candles = mutableListOf<Candle>()
        var price = 100.0
        val now = System.currentTimeMillis()
        
        for (i in 0..20) {
            val change = 1.0 // Consistent gains
            val open = price
            val close = price + change
            val high = close + 0.5
            val low = open - 0.5
            candles.add(Candle(now + i * 3600000, open, high, low, close, 100.0))
            price = close
        }
        
        val rsi = Indicators.rsi(candles, 14)
        assertNotNull(rsi)
        // With consistent gains, RSI should be high (> 70)
        assertTrue(rsi!!.compareTo(BigDecimal("70")) > 0)
    }

    @Test
    fun testRsiDowntrend() {
        // Create candles with clear downtrend
        val candles = mutableListOf<Candle>()
        var price = 100.0
        val now = System.currentTimeMillis()
        
        for (i in 0..20) {
            val change = -1.0 // Consistent losses
            val open = price
            val close = price + change
            val high = open + 0.5
            val low = close - 0.5
            candles.add(Candle(now + i * 3600000, open, high, low, close, 100.0))
            price = close
        }
        
        val rsi = Indicators.rsi(candles, 14)
        assertNotNull(rsi)
        // With consistent losses, RSI should be low (< 30)
        assertTrue(rsi!!.compareTo(BigDecimal("30")) < 0)
    }

    @Test
    fun testAtr() {
        val candles = listOf(
            Candle(1, 100.0, 102.0, 99.0, 101.0, 100.0),
            Candle(2, 101.0, 103.0, 100.0, 102.0, 100.0),
            Candle(3, 102.0, 104.0, 101.0, 103.0, 100.0),
            Candle(4, 103.0, 105.0, 102.0, 104.0, 100.0),
            Candle(5, 104.0, 106.0, 103.0, 105.0, 100.0),
            Candle(6, 105.0, 107.0, 104.0, 106.0, 100.0),
            Candle(7, 106.0, 108.0, 105.0, 107.0, 100.0),
            Candle(8, 107.0, 109.0, 106.0, 108.0, 100.0),
            Candle(9, 108.0, 110.0, 107.0, 109.0, 100.0),
            Candle(10, 109.0, 111.0, 108.0, 110.0, 100.0),
            Candle(11, 110.0, 112.0, 109.0, 111.0, 100.0),
            Candle(12, 111.0, 113.0, 110.0, 112.0, 100.0),
            Candle(13, 112.0, 114.0, 111.0, 113.0, 100.0),
            Candle(14, 113.0, 115.0, 112.0, 114.0, 100.0),
            Candle(15, 114.0, 116.0, 113.0, 115.0, 100.0),
            Candle(16, 115.0, 117.0, 114.0, 116.0, 100.0),
        )
        
        val atr = Indicators.atr(candles, 14)
        assertNotNull(atr)
        // TR for each is about 2-3, so ATR should be around 2-3
        assertTrue(atr!!.compareTo(BigDecimal("1")) > 0)
        assertTrue(atr.compareTo(BigDecimal("5")) < 0)
    }

    @Test
    fun testBollingerBands() {
        val candles = listOf(
            Candle(1, 100.0, 101.0, 99.0, 100.0, 100.0),
            Candle(2, 100.0, 101.0, 99.0, 101.0, 100.0),
            Candle(3, 101.0, 102.0, 100.0, 102.0, 100.0),
            Candle(4, 102.0, 103.0, 101.0, 103.0, 100.0),
            Candle(5, 103.0, 104.0, 102.0, 104.0, 100.0),
            Candle(6, 104.0, 105.0, 103.0, 105.0, 100.0),
            Candle(7, 105.0, 106.0, 104.0, 106.0, 100.0),
            Candle(8, 106.0, 107.0, 105.0, 107.0, 100.0),
            Candle(9, 107.0, 108.0, 106.0, 108.0, 100.0),
            Candle(10, 108.0, 109.0, 107.0, 109.0, 100.0),
            Candle(11, 109.0, 110.0, 108.0, 110.0, 100.0),
            Candle(12, 110.0, 111.0, 109.0, 111.0, 100.0),
            Candle(13, 111.0, 112.0, 110.0, 112.0, 100.0),
            Candle(14, 112.0, 113.0, 111.0, 113.0, 100.0),
            Candle(15, 113.0, 114.0, 112.0, 114.0, 100.0),
            Candle(16, 114.0, 115.0, 113.0, 115.0, 100.0),
            Candle(17, 115.0, 116.0, 114.0, 116.0, 100.0),
            Candle(18, 116.0, 117.0, 115.0, 117.0, 100.0),
            Candle(19, 117.0, 118.0, 116.0, 118.0, 100.0),
            Candle(20, 118.0, 119.0, 117.0, 119.0, 100.0),
        )
        
        val bb = Indicators.bollingerBands(candles, 20, 2)
        assertNotNull(bb)
        assertTrue(bb!!.first.compareTo(bb.second) > 0) // Upper > Middle
        assertTrue(bb.second.compareTo(bb.third) > 0) // Middle > Lower
    }

    @Test
    fun testPivotPoints() {
        val pivots = Indicators.pivotPoints(110.0, 100.0, 105.0)
        
        assertNotNull(pivots["PP"])
        assertNotNull(pivots["R1"])
        assertNotNull(pivots["S1"])
        assertNotNull(pivots["R2"])
        assertNotNull(pivots["S2"])
        assertNotNull(pivots["R3"])
        assertNotNull(pivots["S3"])
        
        // PP = (H + L + C) / 3 = (110 + 100 + 105) / 3 = 105
        assertEquals(BigDecimal("105"), pivots["PP"]!!.setScale(2, RoundingMode.HALF_UP))
        
        // R1 = 2*PP - L = 210 - 100 = 110
        assertEquals(BigDecimal("110"), pivots["R1"]!!.setScale(2, RoundingMode.HALF_UP))
        
        // S1 = 2*PP - H = 210 - 110 = 100
        assertEquals(BigDecimal("100"), pivots["S1"]!!.setScale(2, RoundingMode.HALF_UP))
    }
}