package app.marketbrief.core.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Twelve Data sends numbers as strings, and errors as a JSON body with status "error". */
@Serializable
data class TwelveQuoteDto(
    val symbol: String? = null,
    val close: String? = null,
    val timestamp: Long? = null,
    @SerialName("percent_change") val percentChange: String? = null,
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null
)

@Serializable
data class TwelveTimeSeriesDto(
    val values: List<TwelveValueDto> = emptyList(),
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null
)

@Serializable
data class TwelveValueDto(
    val datetime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String? = null
)

@Serializable
data class TwelveSearchDto(
    val data: List<TwelveSymbolDto> = emptyList(),
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null
)

@Serializable
data class TwelveSymbolDto(
    val symbol: String,
    @SerialName("instrument_name") val name: String? = null,
    @SerialName("instrument_type") val type: String? = null
)

sealed interface MarketDataError {
    val message: String
    data class RateLimited(override val message: String = "Market data limit reached. Try again shortly.") : MarketDataError
    data class BadKey(override val message: String = "Twelve Data key missing or invalid.") : MarketDataError
    data class NotAvailable(override val message: String = "Not available on the current data plan.") : MarketDataError
    data class Server(val code: Int, override val message: String) : MarketDataError
}

class MarketDataException(val error: MarketDataError) : Exception(error.message)
