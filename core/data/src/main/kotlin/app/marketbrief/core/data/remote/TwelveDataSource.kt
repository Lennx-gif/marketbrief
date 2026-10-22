package app.marketbrief.core.data.remote

import app.marketbrief.core.data.MarketDataSource as MarketDataSourceApi
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
import app.marketbrief.core.model.Candle
import app.marketbrief.core.model.MarketDataSource as ProviderId
import app.marketbrief.core.model.Quote
import app.marketbrief.core.model.Timeframe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Twelve Data (free plan: 8 credits/minute, 800/day; one credit per symbol per call).
 * Twelve Data's terms on free-plan display use are unconfirmed, so keep this for personal use.
 */
class TwelveDataSource(
    private val apiKey: String,
    private val client: HttpClient = NewsApiProvider.createDefaultClient(),
    private val budget: RequestBudget = RequestBudget(perMinute = 8, perDay = 800),
    private val baseUrl: String = "https://api.twelvedata.com"
) : MarketDataSourceApi {

    override val sourceId: ProviderId = ProviderId.TWELVE_DATA

    override suspend fun getQuote(assetId: AssetId): Result<Quote> = guarded {
        val dto: TwelveQuoteDto = client.get("$baseUrl/quote") {
            parameter("symbol", SymbolMapper.toTwelveData(assetId))
            parameter("apikey", apiKey)
        }.body()
        throwIfError(dto.status, dto.code, dto.message)
        mapQuote(assetId, dto) ?: throw MarketDataException(MarketDataError.Server(0, "Unreadable quote for ${assetId.symbol}"))
    }

    override suspend fun getCandles(assetId: AssetId, timeframe: Timeframe, limit: Int): Result<List<Candle>> = guarded {
        val dto: TwelveTimeSeriesDto = client.get("$baseUrl/time_series") {
            parameter("symbol", SymbolMapper.toTwelveData(assetId))
            parameter("interval", SymbolMapper.interval(timeframe))
            parameter("outputsize", limit.coerceIn(1, 500))
            parameter("timezone", "UTC")
            parameter("apikey", apiKey)
        }.body()
        throwIfError(dto.status, dto.code, dto.message)
        mapCandles(dto)
    }

    override suspend fun searchSymbols(query: String): Result<List<AssetId>> = guarded {
        val dto: TwelveSearchDto = client.get("$baseUrl/symbol_search") {
            parameter("symbol", query)
            parameter("outputsize", 10)
        }.body()
        throwIfError(dto.status, dto.code, dto.message)
        dto.data.map { AssetId(it.symbol.replace("/", "").uppercase(), classify(it.type)) }.distinct()
    }

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> {
        if (apiKey.isBlank()) return Result.failure(MarketDataException(MarketDataError.BadKey()))
        if (!budget.tryAcquire()) return Result.failure(MarketDataException(MarketDataError.RateLimited()))
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun throwIfError(status: String?, code: Int?, message: String?) {
            if (status != "error" && code == null) return
            val text = message ?: "Request failed"
            throw MarketDataException(
                when (code) {
                    429 -> MarketDataError.RateLimited(text)
                    401 -> MarketDataError.BadKey(text)
                    403 -> MarketDataError.NotAvailable(text)
                    else -> MarketDataError.Server(code ?: 0, text)
                }
            )
        }

        fun mapQuote(assetId: AssetId, dto: TwelveQuoteDto): Quote? {
            val price = dto.close?.toDoubleOrNull() ?: return null
            return Quote(
                assetId = assetId,
                bid = price,
                ask = price, // Twelve Data's quote has no bid/ask, only the last price.
                timestamp = (dto.timestamp?.times(1000)) ?: System.currentTimeMillis(),
                changePercent = dto.percentChange?.toDoubleOrNull()
            )
        }

        /** Twelve Data returns newest first; the analysis code expects oldest first. */
        fun mapCandles(dto: TwelveTimeSeriesDto): List<Candle> =
            dto.values.mapNotNull { v ->
                val ts = parseDateTime(v.datetime) ?: return@mapNotNull null
                val o = v.open.toDoubleOrNull() ?: return@mapNotNull null
                val h = v.high.toDoubleOrNull() ?: return@mapNotNull null
                val l = v.low.toDoubleOrNull() ?: return@mapNotNull null
                val c = v.close.toDoubleOrNull() ?: return@mapNotNull null
                Candle(ts, o, h, l, c, v.volume?.toDoubleOrNull() ?: 0.0)
            }.sortedBy { it.timestamp }

        fun parseDateTime(raw: String): Long? = try {
            val iso = raw.trim().replace(' ', 'T').let { if (it.length == 10) "${it}T00:00:00" else it }
            LocalDateTime.parse(iso).toInstant(TimeZone.UTC).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }

        fun classify(type: String?): AssetClass = when (type?.lowercase()) {
            "physical currency" -> AssetClass.FOREX
            "digital currency" -> AssetClass.CRYPTO
            "index" -> AssetClass.INDEX
            "commodity" -> AssetClass.COMMODITY
            else -> AssetClass.STOCK
        }
    }
}
