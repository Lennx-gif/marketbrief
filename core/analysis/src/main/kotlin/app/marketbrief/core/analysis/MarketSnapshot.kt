package app.marketbrief.core.analysis

import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.analysis.Indicators
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Snapshot of market analysis for an asset.
 */
data class MarketSnapshot(
    val assetId: AssetId,
    val timestamp: Long,
    val currentPrice: BigDecimal,
    val sma20: BigDecimal?,
    val sma50: BigDecimal?,
    val sma200: BigDecimal?,
    val ema20: BigDecimal?,
    val ema50: BigDecimal?,
    val rsi14: BigDecimal?,
    val atr14: BigDecimal?,
    val atrPercent: BigDecimal?,
    val macd: Triple<BigDecimal, BigDecimal, BigDecimal>?,
    val bollingerBands: Triple<BigDecimal, BigDecimal, BigDecimal>?,
    val stochastic: Pair<BigDecimal, BigDecimal>?,
    val pivotPoints: Map<String, BigDecimal>?,
    val trend: Trend,
    val volatility: Volatility
) {
    /**
     * Price relative to moving averages.
     */
    enum class Trend {
        STRONG_UPTREND,   // Price > SMA20 > SMA50 > SMA200
        UPTREND,          // Price > SMA20 > SMA50
        NEUTRAL,          // Mixed signals
        DOWNTREND,        // Price < SMA20 < SMA50
        STRONG_DOWNTREND  // Price < SMA20 < SMA50 < SMA200
    }

    /**
     * Volatility classification based on ATR as % of price.
     */
    enum class Volatility {
        VERY_LOW,   // < 0.5%
        LOW,        // 0.5% - 1%
        NORMAL,     // 1% - 2%
        HIGH,       // 2% - 4%
        VERY_HIGH   // > 4%
    }
}

/**
 * Analyzes market data and produces snapshots.
 */
class MarketAnalyzer {

    fun analyze(
        assetId: AssetId,
        candles: List<Candle>,
        currentPrice: Double
    ): MarketSnapshot {
        val price = BigDecimal.valueOf(currentPrice)
        
        // Moving averages
        val sma20 = Indicators.sma(candles, 20)
        val sma50 = Indicators.sma(candles, 50)
        val sma200 = Indicators.sma(candles, 200)
        val ema20 = Indicators.ema(candles, 20)
        val ema50 = Indicators.ema(candles, 50)
        
        // Momentum
        val rsi14 = Indicators.rsi(candles, 14)
        
        // Volatility
        val atr14 = Indicators.atr(candles, 14)
                val atrPercent = atr14?.let { it.divide(price, CALC).multiply(BigDecimal(100), CALC)
            .setScale(2, RoundingMode.HALF_UP) }
        
        // MACD
        val macd = Indicators.macd(candles)
        
        // Bollinger Bands
        val bollingerBands = Indicators.bollingerBands(candles)
        
        // Stochastic
        val stochastic = Indicators.stochastic(candles)
        
        // Pivot points (using last candle as previous period)
        val pivotPoints = if (candles.isNotEmpty()) {
            val last = candles.last()
            Indicators.pivotPoints(last.high, last.low, last.close)
        } else null
        
        // Determine trend
        val trend = determineTrend(price, sma20, sma50, sma200)
        
        // Determine volatility
        val volatility = determineVolatility(atrPercent)
        
        return MarketSnapshot(
            assetId = assetId,
            timestamp = System.currentTimeMillis(),
            currentPrice = price,
            sma20 = sma20,
            sma50 = sma50,
            sma200 = sma200,
            ema20 = ema20,
            ema50 = ema50,
            rsi14 = rsi14,
            atr14 = atr14,
            atrPercent = atrPercent,
            macd = macd,
            bollingerBands = bollingerBands,
            stochastic = stochastic,
            pivotPoints = pivotPoints,
            trend = trend,
            volatility = volatility
        )
    }

    private fun determineTrend(
        price: BigDecimal,
        sma20: BigDecimal?,
        sma50: BigDecimal?,
        sma200: BigDecimal?
    ): MarketSnapshot.Trend {
        if (sma20 == null || sma50 == null) return MarketSnapshot.Trend.NEUTRAL
        
        val above20 = price > sma20
        val above50 = price > sma50
        val sma20Above50 = sma20 > sma50
        
                return when {
            above20 && above50 && sma20Above50 && sma200 != null && sma50 > sma200 -> MarketSnapshot.Trend.STRONG_UPTREND
            above20 && above50 && sma20Above50 -> MarketSnapshot.Trend.UPTREND
            !above20 && !above50 && !sma20Above50 && sma200 != null && sma50 < sma200 -> MarketSnapshot.Trend.STRONG_DOWNTREND
            !above20 && !above50 && !sma20Above50 -> MarketSnapshot.Trend.DOWNTREND
            else -> MarketSnapshot.Trend.NEUTRAL
        }
    }

    private fun determineVolatility(atrPercent: BigDecimal?): MarketSnapshot.Volatility {
            return atrPercent?.let { percent ->
                when {
                    percent < BigDecimal("0.5") -> MarketSnapshot.Volatility.VERY_LOW
                    percent < BigDecimal("1.0") -> MarketSnapshot.Volatility.LOW
                    percent < BigDecimal("2.0") -> MarketSnapshot.Volatility.NORMAL
                    percent < BigDecimal("4.0") -> MarketSnapshot.Volatility.HIGH
                    else -> MarketSnapshot.Volatility.VERY_HIGH
                }
            } ?: MarketSnapshot.Volatility.NORMAL
        }
    }