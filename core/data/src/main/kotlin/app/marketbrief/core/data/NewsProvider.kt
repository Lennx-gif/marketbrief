package app.marketbrief.core.data

import app.marketbrief.core.data.remote.NewsError
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.FeedQuery
import kotlinx.coroutines.flow.Flow

/**
 * Interface for news providers.
 */
interface NewsProvider {
    val sourceId: String
    val displayName: String

    /**
     * Fetches articles matching a query from the provider.
     */
    suspend fun fetchEverything(
        query: String,
        regionTag: String? = null,
        pageSize: Int = 30
    ): Result<List<Article>>

    suspend fun search(query: String, limit: Int = 30): Result<List<Article>> =
        fetchEverything(query = query, pageSize = limit)
}

/**
 * Result of refreshing feed from remote network provider.
 */
sealed interface RefreshResult {
    data class Success(val count: Int) : RefreshResult
    data class Error(val error: NewsError) : RefreshResult
    data object BudgetExhausted : RefreshResult
}

/**
 * Result of offline article downloading.
 */
sealed interface DownloadResult {
    data class Success(val articleId: String, val isFullText: Boolean) : DownloadResult
    data class Failure(val message: String) : DownloadResult
}

/**
 * Reactive Room-backed news repository interface.
 */
interface NewsRepository {
    fun observeFeed(query: FeedQuery): Flow<List<Article>>
    fun observeBookmarkIds(): Flow<Set<String>>
    fun observeDownloadedIds(): Flow<Set<String>>
    suspend fun refresh(query: FeedQuery): RefreshResult
    suspend fun setBookmarked(articleId: String, saved: Boolean)
    suspend fun download(articleId: String): DownloadResult
    suspend fun search(query: String): List<Article>
    fun observeArticleBody(articleId: String): Flow<app.marketbrief.core.model.ArticleBody?>
    suspend fun getArticleBody(articleId: String): app.marketbrief.core.model.ArticleBody?
    suspend fun extractArticleBody(articleId: String): Result<app.marketbrief.core.model.ArticleBody>
    fun observeSourcePreferences(): Flow<Map<String, String>> = kotlinx.coroutines.flow.flowOf(emptyMap())
    fun observeAvailableSources(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun setSourcePreference(source: String, weight: String) = Unit
    suspend fun articleTags(articleId: String): List<String> = emptyList()
    suspend fun setArticleTag(articleId: String, tag: String, enabled: Boolean) = Unit
    fun observeSavedTags(): Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun savedArticlesWithTag(tag: String): List<Article> = emptyList()

    suspend fun getArticleById(articleId: String): Article? = null
    suspend fun isBookmarked(articleId: String): Boolean = false
    suspend fun isDownloaded(articleId: String): Boolean = false

    // Compatibility default delegates
    val articles: Flow<List<Article>>
        get() = observeFeed(FeedQuery())

    val savedArticles: Flow<List<Article>>
        get() = observeFeed(FeedQuery(categories = setOf(Category.Saved)))

    suspend fun refresh(category: String? = null) {
        val cat = Category.entries.firstOrNull { it.label.equals(category, ignoreCase = true) } ?: Category.All
        refresh(FeedQuery(categories = setOf(cat)))
    }

    suspend fun saveArticle(article: Article) = setBookmarked(article.id, true)
    suspend fun unsaveArticle(articleId: String) = setBookmarked(articleId, false)
}

/** App-layer hook for scheduling follow-up work after a successful foreground refresh. */
interface NewsRefreshScheduler {
    suspend fun schedulePrefetch()
}

interface DailyDigestScheduler { suspend fun reschedule(enabled: Boolean, time: String) }

object NoOpNewsRefreshScheduler : NewsRefreshScheduler {
    override suspend fun schedulePrefetch() = Unit
}

object TopArticleSelector {
    fun select(articles: List<Article>, articleIdsWithFreshBodies: Set<String>, limit: Int = 3): List<Article> =
        articles.asSequence().filterNot { it.id in articleIdsWithFreshBodies }.take(limit).toList()
}
