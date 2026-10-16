package app.marketbrief.core.data.remote

import app.marketbrief.core.data.NewsProvider
import app.marketbrief.core.model.Article
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class NewsApiProvider(
    private val apiKey: String,
    private val client: HttpClient
) : NewsProvider {

    constructor(apiKey: String) : this(apiKey, createDefaultClient())

    override val sourceId: String = "newsapi"
    override val displayName: String = "NewsAPI.org"

    override suspend fun fetchEverything(
        query: String,
        regionTag: String?,
        pageSize: Int
    ): Result<List<Article>> {
        if (apiKey.isBlank()) {
            return Result.failure(NewsApiException(NewsError.BadKey("NewsAPI key is not configured in local.properties")))
        }

        return try {
            val response = client.get("https://newsapi.org/v2/everything") {
                header("X-Api-Key", apiKey)
                parameter("q", query)
                parameter("language", "en")
                parameter("sortBy", "publishedAt")
                parameter("searchIn", "title,description")
                parameter("pageSize", pageSize.coerceIn(1, 100))
            }

            if (response.status == HttpStatusCode.TooManyRequests) {
                return Result.failure(NewsApiException(NewsError.RateLimited()))
            }

            if (response.status == HttpStatusCode.Unauthorized) {
                return Result.failure(NewsApiException(NewsError.BadKey()))
            }

            val apiResponse: NewsApiResponse = response.body()

            if (apiResponse.status == "error") {
                val error = when (apiResponse.code) {
                    "rateLimited", "maximumResultsReached" -> NewsError.RateLimited(apiResponse.message ?: "Rate limited")
                    "apiKeyInvalid", "apiKeyMissing" -> NewsError.BadKey(apiResponse.message ?: "Invalid key")
                    else -> NewsError.Server(response.status.value, apiResponse.message ?: "Server error")
                }
                return Result.failure(NewsApiException(error))
            }

            val articles = apiResponse.articles.mapNotNull { dto ->
                mapDtoToArticle(dto, regionTag)
            }
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun createDefaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                })
            }
        }

        fun mapDtoToArticle(dto: NewsApiArticleDto, regionTag: String? = null): Article? {
            val rawTitle = dto.title?.trim() ?: return null
            if (rawTitle.isBlank() || rawTitle.equals("[Removed]", ignoreCase = true)) return null

            val rawUrl = dto.url?.trim() ?: return null
            if (!rawUrl.startsWith("http://", ignoreCase = true) && !rawUrl.startsWith("https://", ignoreCase = true)) {
                return null
            }

            val sourceName = dto.source?.name?.trim() ?: "Unknown"
            val title = cleanTitle(rawTitle, sourceName)
            val summary = cleanHtml(dto.description)
            val publishedAt = parsePublishedAt(dto.publishedAt)
            val id = sha256(normalizeUrl(rawUrl))

            return Article(
                id = id,
                title = title,
                summary = summary,
                url = rawUrl,
                imageUrl = dto.urlToImage?.takeIf { it.isNotBlank() },
                sourceId = dto.source?.id ?: "newsapi",
                sourceName = sourceName,
                publishedAt = publishedAt,
                region = regionTag
            )
        }

        fun cleanHtml(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            var text = raw.replace(Regex("<[^>]*>"), " ")
            text = text.replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
            return text.replace(Regex("\\s+"), " ").trim()
        }

        fun cleanTitle(title: String, sourceName: String): String {
            var t = cleanHtml(title)
            val suffix = " - $sourceName"
            if (t.endsWith(suffix, ignoreCase = true)) {
                t = t.substring(0, t.length - suffix.length).trim()
            }
            return t
        }

        fun normalizeUrl(rawUrl: String): String {
            return try {
                val uri = java.net.URI(rawUrl)
                val scheme = (uri.scheme ?: "https").lowercase()
                val host = (uri.host ?: "").lowercase()
                val path = (uri.path ?: "").trimEnd('/')
                "$scheme://$host$path"
            } catch (_: Exception) {
                rawUrl.trim().trimEnd('/')
            }
        }

        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun parsePublishedAt(raw: String?): Long {
            if (raw.isNullOrBlank()) return System.currentTimeMillis()
            return try {
                kotlinx.datetime.Instant.parse(raw).toEpochMilliseconds()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

class NewsApiException(val error: NewsError) : Exception(
    when (error) {
        is NewsError.RateLimited -> error.message
        is NewsError.BadKey -> error.message
        is NewsError.Offline -> error.message
        is NewsError.Server -> "HTTP ${error.code}: ${error.message}"
        is NewsError.Unknown -> error.message
    }
)
