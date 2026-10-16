package app.marketbrief.core.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class NewsApiResponse(
    val status: String,
    val totalResults: Int = 0,
    val articles: List<NewsApiArticleDto> = emptyList(),
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class NewsApiArticleDto(
    val source: SourceDto? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val urlToImage: String? = null,
    val publishedAt: String? = null,
    val content: String? = null
)

@Serializable
data class SourceDto(
    val id: String? = null,
    val name: String? = null
)

/**
 * Typed hierarchy of errors returned by news provider or network.
 */
sealed interface NewsError {
    data class RateLimited(val message: String = "NewsAPI daily rate limit reached.") : NewsError
    data class BadKey(val message: String = "Invalid or missing NewsAPI key.") : NewsError
    data class Offline(val message: String = "No network connection. Showing cached articles.") : NewsError
    data class Server(val code: Int, val message: String) : NewsError
    data class Unknown(val message: String) : NewsError
}
