package app.marketbrief.core.data.remote

import app.marketbrief.core.data.positioning.CrowdPosition
import app.marketbrief.core.data.positioning.PositioningError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MyfxbookLoginDto(
    val error: Boolean = false,
    val message: String = "",
    val session: String? = null
)

@Serializable
data class MyfxbookOutlookDto(
    val error: Boolean = false,
    val message: String = "",
    val symbols: List<MyfxbookSymbolDto> = emptyList()
)

@Serializable
data class MyfxbookSymbolDto(
    val name: String,
    val longPercentage: Double = 0.0,
    val shortPercentage: Double = 0.0,
    val longVolume: Double = 0.0,
    val shortVolume: Double = 0.0
)

/**
 * Minimal Myfxbook API client. Login returns a session bound to the caller's IP.
 * The password travels in the query string, so this client must never be given request logging.
 * Responses are decoded from text rather than by content type, because the content type is unconfirmed.
 */
class MyfxbookClient(
    private val client: HttpClient = NewsApiProvider.createDefaultClient(),
    private val baseUrl: String = "https://www.myfxbook.com/api"
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    suspend fun login(email: String, password: String): Result<String> = call {
        val dto = json.decodeFromString<MyfxbookLoginDto>(
            client.get("$baseUrl/login.json") {
                parameter("email", email)
                parameter("password", password)
            }.bodyAsText()
        )
        if (dto.error || dto.session.isNullOrBlank()) throw PositioningError.BadCredentials(dto.message)
        dto.session
    }

    suspend fun communityOutlook(session: String): Result<List<CrowdPosition>> = call {
        val dto = json.decodeFromString<MyfxbookOutlookDto>(
            client.get("$baseUrl/get-community-outlook.json") { parameter("session", session) }.bodyAsText()
        )
        if (dto.error) throw classify(dto.message)
        mapOutlook(dto)
    }

    suspend fun logout(session: String) {
        runCatching { client.get("$baseUrl/logout.json") { parameter("session", session) }.bodyAsText() }
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        fun classify(message: String): PositioningError {
            val m = message.lowercase()
            return when {
                "session" in m -> PositioningError.SessionExpired()
                "limit" in m || "too many" in m -> PositioningError.RateLimited()
                else -> PositioningError.Server(message)
            }
        }

        fun mapOutlook(dto: MyfxbookOutlookDto): List<CrowdPosition> =
            dto.symbols.map {
                CrowdPosition(
                    symbol = it.name.replace("/", "").uppercase(),
                    longPercent = it.longPercentage,
                    shortPercent = it.shortPercentage,
                    longVolume = it.longVolume,
                    shortVolume = it.shortVolume
                )
            }
    }
}
