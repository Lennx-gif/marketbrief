package app.marketbrief.core.data

import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.MarketDataSource
import app.marketbrief.core.model.Quote
import app.marketbrief.core.model.Timeframe
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.PairRateTable
import app.marketbrief.core.risk.RateSource
import kotlinx.coroutines.flow.Flow

/**
 * Repository for FX rates.
 */
interface RateRepository {
    val rates: Flow<PairRateTable>
    val lastUpdated: Flow<Long>
    
    suspend fun refresh()
    fun getRate(from: CurrencyCode, to: CurrencyCode): Double?
}

/**
 * Interface for market data sources (candles, quotes).
 */
interface MarketDataSource {
    val sourceId: MarketDataSource
    
    suspend fun getCandles(
        assetId: AssetId,
        timeframe: Timeframe,
        limit: Int
    ): Result<List<Candle>>
    
    suspend fun getQuote(assetId: AssetId): Result<Quote>
    
    suspend fun searchSymbols(query: String): Result<List<AssetId>>
}

/**
 * Repository for market data.
 */
interface MarketDataRepository {
    val quotes: Flow<Map<AssetId, Quote>>
    
    suspend fun getCandles(
        assetId: AssetId,
        timeframe: Timeframe,
        limit: Int
    ): Result<List<Candle>>
    
    suspend fun getQuote(assetId: AssetId): Result<Quote>
    suspend fun searchSymbols(query: String): Result<List<AssetId>>
}