package app.marketbrief.core.analysis

import app.marketbrief.core.model.Candle
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** 34 significant digits for intermediate calculations. */
internal val CALC = MathContext(34, RoundingMode.HALF_EVEN)

/**
 * Technical indicators computed from candle data.
 * All calculations use BigDecimal for precision.
 */
object Indicators {

    /**
     * Simple Moving Average.
     * @param candles List of candles (most recent last)
     * @param period Number of periods
     * @return SMA value or null if insufficient data
     */
    fun sma(candles: List<Candle>, period: Int): BigDecimal? {
        if (candles.size < period) return null
        val closes = candles.takeLast(period).map { BigDecimal.valueOf(it.close) }
            var sum = BigDecimal.ZERO
            for (close in closes) {
                sum = sum.add(close)
            }
            return sum.divide(BigDecimal(period), CALC)
        }

    /**
     * Exponential Moving Average using Wilder's smoothing.
     * @param candles List of candles (most recent last)
     * @param period Number of periods
     * @return EMA value or null if insufficient data
     */
    fun ema(candles: List<Candle>, period: Int): BigDecimal? {
        if (candles.size < period) return null
        val multiplier = BigDecimal(2).divide(BigDecimal(period + 1), CALC)
        var ema = sma(candles, period)!!
        for (i in period until candles.size) {
            val close = BigDecimal.valueOf(candles[i].close)
            ema = close.subtract(ema).multiply(multiplier, CALC).add(ema, CALC)
        }
        return ema
    }

    /**
     * Relative Strength Index (Wilder's method).
     * @param candles List of candles (most recent last)
     * @param period Number of periods (typically 14)
     * @return RSI value (0-100) or null if insufficient data
     */
    fun rsi(candles: List<Candle>, period: Int = 14): BigDecimal? {
        if (candles.size < period + 1) return null
        
        var gains = BigDecimal.ZERO
        var losses = BigDecimal.ZERO
        
        // Initial average gain/loss
        for (i in 1..period) {
            val change = BigDecimal.valueOf(candles[i].close - candles[i - 1].close)
            if (change.signum() >= 0) gains = gains.add(change, CALC)
            else losses = losses.add(change.abs(), CALC)
        }
        
        var avgGain = gains.divide(BigDecimal(period), CALC)
        var avgLoss = losses.divide(BigDecimal(period), CALC)
        
        // Wilder's smoothing for subsequent periods
        for (i in period + 1 until candles.size) {
            val change = BigDecimal.valueOf(candles[i].close - candles[i - 1].close)
            val gain = if (change.signum() >= 0) change else BigDecimal.ZERO
            val loss = if (change.signum() < 0) change.abs() else BigDecimal.ZERO
            
            avgGain = avgGain.multiply(BigDecimal(period - 1), CALC).add(gain, CALC).divide(BigDecimal(period), CALC)
            avgLoss = avgLoss.multiply(BigDecimal(period - 1), CALC).add(loss, CALC).divide(BigDecimal(period), CALC)
        }
        
        if (avgLoss == BigDecimal.ZERO) return BigDecimal(100)
        val rs = avgGain.divide(avgLoss, CALC)
        return BigDecimal(100).subtract(BigDecimal(100).divide(BigDecimal.ONE.add(rs), CALC), CALC)
    }

    /**
     * Average True Range (Wilder's method).
     * @param candles List of candles (most recent last)
     * @param period Number of periods (typically 14)
     * @return ATR value or null if insufficient data
     */
    fun atr(candles: List<Candle>, period: Int = 14): BigDecimal? {
        if (candles.size < period + 1) return null
        
        var trSum = BigDecimal.ZERO
        
        // Initial TR sum
        for (i in 1..period) {
            val high = BigDecimal.valueOf(candles[i].high)
            val low = BigDecimal.valueOf(candles[i].low)
            val prevClose = BigDecimal.valueOf(candles[i - 1].close)
            
            val tr1 = high.subtract(low, CALC)
            val tr2 = high.subtract(prevClose, CALC).abs()
            val tr3 = low.subtract(prevClose, CALC).abs()
            
            trSum = trSum.add(tr1.max(tr2).max(tr3), CALC)
        }
        
        var atr = trSum.divide(BigDecimal(period), CALC)
        
        // Wilder's smoothing
        for (i in period + 1 until candles.size) {
            val high = BigDecimal.valueOf(candles[i].high)
            val low = BigDecimal.valueOf(candles[i].low)
            val prevClose = BigDecimal.valueOf(candles[i - 1].close)
            
            val tr1 = high.subtract(low, CALC)
            val tr2 = high.subtract(prevClose, CALC).abs()
            val tr3 = low.subtract(prevClose, CALC).abs()
            
            val tr = tr1.max(tr2).max(tr3)
            atr = atr.multiply(BigDecimal(period - 1), CALC).add(tr, CALC).divide(BigDecimal(period), CALC)
        }
        
        return atr
    }

    /**
     * MACD (Moving Average Convergence Divergence).
     * @param candles List of candles (most recent last)
     * @param fastPeriod Fast EMA period (typically 12)
     * @param slowPeriod Slow EMA period (typically 26)
     * @param signalPeriod Signal line period (typically 9)
     * @return Triple of (MACD line, Signal line, Histogram) or null if insufficient data
     */
    fun macd(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<BigDecimal, BigDecimal, BigDecimal>? {
        if (candles.size < slowPeriod + signalPeriod) return null
        
        val fastEma = ema(candles, fastPeriod)
        val slowEma = ema(candles, slowPeriod)
        
        if (fastEma == null || slowEma == null) return null
        
        val macdLine = fastEma.subtract(slowEma, CALC)
        
        // Calculate signal line (EMA of MACD line)
        // We need to build MACD line history
        val macdHistory = mutableListOf<BigDecimal>()
        for (i in slowPeriod until candles.size) {
            val fast = ema(candles.subList(0, i + 1), fastPeriod)
            val slow = ema(candles.subList(0, i + 1), slowPeriod)
            if (fast != null && slow != null) {
                macdHistory.add(fast.subtract(slow, CALC))
            }
        }
        
        if (macdHistory.size < signalPeriod) return null
        
        val signalLine = emaFromValues(macdHistory, signalPeriod)
        val histogram = macdLine.subtract(signalLine, CALC)
        
        return Triple(macdLine, signalLine, histogram)
    }

    private fun emaFromValues(values: List<BigDecimal>, period: Int): BigDecimal {
        val multiplier = BigDecimal(2).divide(BigDecimal(period + 1), CALC)
            var sum = BigDecimal.ZERO
            for (i in 0 until period) {
                sum = sum.add(values[i])
        }
            var ema = sum.divide(BigDecimal(period), CALC)
            for (i in period until values.size) {
                ema = values[i].subtract(ema).multiply(multiplier, CALC).add(ema, CALC)
            }
            return ema
        }

    /**
     * Bollinger Bands.
     * @param candles List of candles (most recent last)
     * @param period Number of periods (typically 20)
     * @param stdDev Number of standard deviations (typically 2)
     * @return Triple of (Upper band, Middle band, Lower band) or null if insufficient data
     */
    fun bollingerBands(
        candles: List<Candle>,
        period: Int = 20,
        stdDev: Int = 2
    ): Triple<BigDecimal, BigDecimal, BigDecimal>? {
        if (candles.size < period) return null
        
        val recent = candles.takeLast(period)
        val closes = recent.map { BigDecimal.valueOf(it.close) }
        
            var sum = BigDecimal.ZERO
            for (close in closes) {
                sum = sum.add(close)
            }
            val middle = sum.divide(BigDecimal(period), CALC)
        
            // Standard deviation
            var varianceSum = BigDecimal.ZERO
            for (close in closes) {
                val diff = close.subtract(middle, CALC)
                varianceSum = varianceSum.add(diff.pow(2))
            }
            val variance = varianceSum.divide(BigDecimal(period), CALC)
            val deviation = BigDecimal(Math.sqrt(variance.toDouble()))
        
            val upper = middle.add(deviation.multiply(BigDecimal(stdDev), CALC), CALC)
            val lower = middle.subtract(deviation.multiply(BigDecimal(stdDev), CALC), CALC)
        
            return Triple(upper, middle, lower)
        }

    /**
     * Stochastic Oscillator.
     * @param candles List of candles (most recent last)
     * @param kPeriod %K period (typically 14)
     * @param dPeriod %D period (typically 3)
     * @return Pair of (%K, %D) or null if insufficient data
     */
    fun stochastic(
        candles: List<Candle>,
        kPeriod: Int = 14,
        dPeriod: Int = 3
    ): Pair<BigDecimal, BigDecimal>? {
        if (candles.size < kPeriod + dPeriod - 1) return null
        
        val kValues = mutableListOf<BigDecimal>()
        
        for (i in kPeriod - 1 until candles.size) {
            val window = candles.subList(i - kPeriod + 1, i + 1)
            val highestHigh = window.maxByOrNull { it.high }?.high ?: return null
            val lowestLow = window.minByOrNull { it.low }?.low ?: return null
            val close = candles[i].close
            
            val range = highestHigh - lowestLow
            if (range == 0.0) {
                kValues.add(BigDecimal(50))
            } else {
                val k = BigDecimal(100).multiply(BigDecimal.valueOf(close - lowestLow), CALC)
                    .divide(BigDecimal.valueOf(range), CALC)
                kValues.add(k)
            }
        }
        
        if (kValues.size < dPeriod) return null
        
        val k = kValues.last()
                var dSum = BigDecimal.ZERO
                for (i in kValues.size - dPeriod until kValues.size) {
                    dSum = dSum.add(kValues[i])
                }
                val d = dSum.divide(BigDecimal(dPeriod), CALC)
        
                return Pair(k, d)
    }

    /**
     * Pivot Points (Standard).
     * @param high Previous period high
     * @param low Previous period low
     * @param close Previous period close
     * @return Map of pivot levels
     */
    fun pivotPoints(high: Double, low: Double, close: Double): Map<String, BigDecimal> {
        val h = BigDecimal.valueOf(high)
        val l = BigDecimal.valueOf(low)
        val c = BigDecimal.valueOf(close)
        
        val pp = h.add(l, CALC).add(c, CALC).divide(BigDecimal(3), CALC)
        val r1 = pp.multiply(BigDecimal(2), CALC).subtract(l, CALC)
        val s1 = pp.multiply(BigDecimal(2), CALC).subtract(h, CALC)
        val r2 = pp.add(h.subtract(l, CALC), CALC)
        val s2 = pp.subtract(h.subtract(l, CALC), CALC)
        val r3 = h.add(BigDecimal(2).multiply(pp.subtract(l, CALC), CALC), CALC)
        val s3 = l.subtract(BigDecimal(2).multiply(h.subtract(pp, CALC), CALC), CALC)
        
        return mapOf(
            "PP" to pp, "R1" to r1, "S1" to s1,
            "R2" to r2, "S2" to s2, "R3" to r3, "S3" to s3
        )
    }
}