package app.marketbrief.core.data.remote

import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.MarketDataSource
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.Quote
import app.marketbrief.core.model.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory cache in front of a provider. A fresh cache entry costs no credits.
 * Stale data is served when the provider fails, so the UI shows old prices rather than nothing.
 */
class CachedMarketDataRepository(
    private val source: MarketDataSource,
    private val quoteTtlMs: Long = 2 * 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) : MarketDataRepository {

    private data class Stamped<T>(val value: T, val at: Long)

    private val lock = Mutex()
    private val candleCache = HashMap<Pair<AssetId, Timeframe>, Stamped<List<Candle>>>()
    private val _quotes = MutableStateFlow<Map<AssetId, Quote>>(emptyMap())
    private val quoteTimes = HashMap<AssetId, Long>()

    override val quotes: Flow<Map<AssetId, Quote>> = _quotes.asStateFlow()

    override suspend fun getQuote(assetId: AssetId): Result<Quote> {
        lock.withLock {
            val at = quoteTimes[assetId]
            val cached = _quotes.value[assetId]
            if (cached != null && at != null && clock() - at < quoteTtlMs) return Result.success(cached)
        }
        val fetched = source.getQuote(assetId)
        fetched.onSuccess { q ->
            lock.withLock {
                quoteTimes[assetId] = clock()
                _quotes.value = _quotes.value + (assetId to q)
            }
        }
        if (fetched.isFailure) _quotes.value[assetId]?.let { return Result.success(it) }
        return fetched
    }

    /** Refreshes each id in turn; the budget in the source stops the loop early when credits run out. */
    override suspend fun refreshQuotes(ids: Collection<AssetId>) {
        for (id in ids) {
            val result = getQuote(id)
            if ((result.exceptionOrNull() as? MarketDataException)?.error is MarketDataError.RateLimited) break
        }
    }

    override suspend fun getCandles(assetId: AssetId, timeframe: Timeframe, limit: Int): Result<List<Candle>> {
        val key = assetId to timeframe
        lock.withLock {
            val hit = candleCache[key]
            if (hit != null && hit.value.size >= limit && clock() - hit.at < candleTtl(timeframe)) {
                return Result.success(hit.value.takeLast(limit))
            }
        }
        val fetched = source.getCandles(assetId, timeframe, limit)
        fetched.onSuccess { list -> lock.withLock { candleCache[key] = Stamped(list, clock()) } }
        if (fetched.isFailure) candleCache[key]?.let { return Result.success(it.value.takeLast(limit)) }
        return fetched
    }

    override suspend fun searchSymbols(query: String): Result<List<AssetId>> = source.searchSymbols(query)

    private fun candleTtl(tf: Timeframe): Long = when (tf) {
        Timeframe.M1 -> 60_000L
        Timeframe.M5 -> 5 * 60_000L
        Timeframe.M15 -> 15 * 60_000L
        Timeframe.H1, Timeframe.H4 -> 60 * 60_000L
        Timeframe.D1, Timeframe.W1, Timeframe.MN1 -> 6 * 60 * 60_000L
    }
}
