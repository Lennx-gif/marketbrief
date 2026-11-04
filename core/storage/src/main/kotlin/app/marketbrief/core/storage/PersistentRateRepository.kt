package app.marketbrief.core.storage

import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.RateRepository
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.PairRateTable
import app.marketbrief.core.storage.db.RateCacheDao
import app.marketbrief.core.storage.db.RateCacheEntity
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class PersistentRateRepository @Inject constructor(
    private val marketData: MarketDataRepository,
    private val dao: RateCacheDao
) : RateRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val table = MutableStateFlow(PairRateTable(emptyMap()))
    private val updated = MutableStateFlow(0L)
    override val rates: Flow<PairRateTable> = table.asStateFlow()
    override val lastUpdated: Flow<Long> = updated.asStateFlow()

    init {
        scope.launch {
            dao.observeAll().collect { rows ->
                table.value = PairRateTable(rows.mapNotNull { row -> row.rate.toBigDecimalOrNull()?.let { row.currencyPair to it } }.toMap())
                updated.value = rows.maxOfOrNull { it.updatedAt } ?: 0L
            }
        }
    }

    override suspend fun refresh(requiredPairs: Set<String>) {
        val cachedRows = dao.observeAll().first()
        table.value = PairRateTable(cachedRows.mapNotNull { row -> row.rate.toBigDecimalOrNull()?.let { row.currencyPair to it } }.toMap())
        updated.value = cachedRows.maxOfOrNull { it.updatedAt } ?: 0L
        if (requiredPairs.isEmpty()) return
        val symbols = requiredPairs.take(2)
        marketData.refreshQuotes(symbols.map { AssetId(it, AssetClass.FOREX) })
        val quotes = marketData.quotes.first()
        val rows = symbols.mapNotNull { symbol ->
            quotes[AssetId(symbol, AssetClass.FOREX)]?.let { quote ->
                quote.mid.takeIf { it > 0 }?.let { RateCacheEntity(symbol, BigDecimal.valueOf(it).toPlainString(), quote.timestamp) }
            }
        }
        if (rows.isNotEmpty()) dao.saveAll(rows)
    }

    override fun getRate(from: CurrencyCode, to: CurrencyCode): Double? = table.value.rate(from, to)?.toDouble()

    override suspend fun getQuote(symbol: String): Double? {
        val type = if (symbol.startsWith("XAU") || symbol.startsWith("XAG")) AssetClass.COMMODITY else AssetClass.FOREX
        return marketData.getQuote(AssetId(symbol, type)).getOrNull()?.mid
    }
}
