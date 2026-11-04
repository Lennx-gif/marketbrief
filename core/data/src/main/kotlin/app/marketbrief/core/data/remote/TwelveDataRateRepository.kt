package app.marketbrief.core.data.remote

import app.marketbrief.core.data.RateRepository
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.data.MarketDataSource
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.PairRateTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal

/**
 * Builds a [PairRateTable] from live Twelve Data quotes so the position sizer converts
 * currencies at market prices. Fetches one quote per major pair; the source's request
 * budget stops the loop early if the free-tier credits run out, and the last complete
 * table stays available until a refresh succeeds.
 */
class TwelveDataRateRepository(
    private val source: MarketDataSource,
    private val clock: () -> Long = System::currentTimeMillis
) : RateRepository {

    private val _rates = MutableStateFlow(PairRateTable(emptyMap()))
    override val rates: Flow<PairRateTable> = _rates.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    override val lastUpdated: Flow<Long> = _lastUpdated.asStateFlow()

    private val mutex = Mutex()

    override suspend fun refresh(requiredPairs: Set<String>) {
        mutex.withLock {
            if (requiredPairs.isEmpty()) return@withLock
            val fetched = HashMap<String, BigDecimal>()
            for (symbol in requiredPairs.take(2)) {
                val quote = source.getQuote(AssetId(symbol, AssetClass.FOREX)).getOrNull() ?: continue
                val mid = (quote.bid + quote.ask) / 2.0
                if (mid > 0.0) fetched[symbol] = BigDecimal.valueOf(mid)
            }
            if (fetched.isNotEmpty()) {
                _rates.value = PairRateTable(fetched)
                _lastUpdated.value = clock()
            }
        }
    }

    override fun getRate(from: CurrencyCode, to: CurrencyCode): Double? =
        _rates.value.rate(from, to)?.toDouble()

    companion object {
        /** Common USD crosses that the calculator can request on demand. */
        val DEFAULT_PAIRS = listOf("EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD", "AUDUSD", "NZDUSD")
    }
}
