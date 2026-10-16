package app.marketbrief.core.data

import kotlinx.coroutines.flow.Flow

/**
 * Interface for news providers.
 */
interface NewsProvider {
    val sourceId: String
    val displayName: String
    
    /**
     * Fetches latest articles.
     * @param category Optional category filter
     * @param limit Maximum number of articles to return
     */
    suspend fun fetchLatest(category: String? = null, limit: Int = 50): Result<List<Article>>
    
    /**
     * Searches articles.
     */
    suspend fun search(query: String, limit: Int = 50): Result<List<Article>>
}

/**
 * Article domain model alias.
 */
typealias Article = app.marketbrief.core.model.Article

/**
 * News repository interface.
 */
interface NewsRepository {
    val articles: Flow<List<Article>>
    val savedArticles: Flow<List<Article>>
    
    suspend fun refresh(category: String? = null)
    suspend fun saveArticle(article: Article)
    suspend fun unsaveArticle(articleId: String)
    suspend fun search(query: String): List<Article>
}