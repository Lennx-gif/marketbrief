package app.marketbrief.core.model

import kotlinx.serialization.Serializable

/**
 * Source of market data.
 */
enum class MarketDataSource {
    TWELVE_DATA,
    ALPHA_VANTAGE,
    FINNHUB,
    YAHOO_FINANCE,
    CUSTOM
}

/**
 * Timeframe for candles and analysis.
 */
enum class Timeframe {
    M1(60_000L),
    M5(300_000L),
    M15(900_000L),
    H1(3_600_000L),
    H4(14_400_000L),
    D1(86_400_000L),
    W1(604_800_000L),
    MN1(2_592_000_000L);

    val millis: Long
    constructor(millis: Long) { this.millis = millis }
}

/**
 * Represents an asset class for categorization and feature gating.
 */
enum class AssetClass {
    FOREX,
    CRYPTO,
    EQUITY,
    COMMODITY,
    INDEX,
    STOCK
}

/**
 * Unique identifier for an asset, combining symbol and asset class.
 */
@Serializable
data class AssetId(
    val symbol: String,
    val assetClass: AssetClass
) {
    override fun toString(): String = "$symbol ($assetClass)"
}

/**
 * ISO 4217 currency code.
 */
@Serializable
@JvmInline
value class CurrencyCode(val code: String) {
    init {
        require(code.length in 3..6 && code.all { it in 'A'..'Z' }) { "Invalid currency code: '$code'" }
    }

    override fun toString(): String = code
}

/**
 * A single OHLCV candle.
 */
@Serializable
data class Candle(
    val timestamp: Long, // Unix timestamp in milliseconds
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double = 0.0
)

/**
 * Real-time quote for an asset.
 */
@Serializable
data class Quote(
    val assetId: AssetId,
    val bid: Double,
    val ask: Double,
    val timestamp: Long
) {
    val mid: Double
        get() = (bid + ask) / 2
    val spread: Double
        get() = ask - bid
}

/**
 * News article model.
 */
@Serializable
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    val sourceId: String,
    val sourceName: String,
    val publishedAt: Long,
    val categories: List<String> = emptyList(),
    val relatedSymbols: List<String> = emptyList()
)