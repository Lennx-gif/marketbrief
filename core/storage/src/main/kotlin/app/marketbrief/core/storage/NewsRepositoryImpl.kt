package app.marketbrief.core.storage

import app.marketbrief.core.analysis.sentiment.SentimentScorer
import app.marketbrief.core.data.DownloadResult
import app.marketbrief.core.data.NewsProvider
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.data.RefreshResult
import app.marketbrief.core.data.remote.NewsApiException
import app.marketbrief.core.data.remote.NewsError
import app.marketbrief.core.data.remote.NewsQueries
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.model.FeedQuery
import app.marketbrief.core.storage.db.ArticleDao
import app.marketbrief.core.storage.db.ArticleEntity
import app.marketbrief.core.storage.db.BookmarkDao
import app.marketbrief.core.storage.db.BookmarkEntity
import app.marketbrief.core.storage.db.DownloadDao
import app.marketbrief.core.storage.db.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val bookmarkDao: BookmarkDao,
    private val downloadDao: DownloadDao,
    private val provider: NewsProvider,
    private val prefs: UserPrefsRepository,
    private val scorer: SentimentScorer = SentimentScorer()
) : NewsRepository {

    override fun observeFeed(query: FeedQuery): Flow<List<Article>> {
        return combine(
            articleDao.observeAll(),
            bookmarkDao.observeBookmarkIds(),
            prefs.homeCountry
        ) { entityList, bookmarkIds, homeCountry ->
            val savedSet = bookmarkIds.toSet()
            val domainList = entityList.map { it.toDomain() }

            if (Category.Saved in query.categories) {
                return@combine domainList.filter { it.id in savedSet }
            }

            var filtered = domainList
            if (Category.All !in query.categories) {
                val categoryLabels = query.categories.map { it.label }
                filtered = filtered.filter { article ->
                    article.categories.any { cat -> categoryLabels.any { it.equals(cat, ignoreCase = true) } }
                }
            }

            when (val countryFilter = query.country) {
                is CountryFilter.Home -> {
                    // Prioritize home country
                    rankArticles(filtered, homeCountry)
                }
                is CountryFilter.Global -> {
                    rankArticles(filtered, null)
                }
                is CountryFilter.Only -> {
                    val subset = filtered.filter { it.region in countryFilter.isoCodes }
                    rankArticles(subset, null)
                }
            }
        }
    }

    override fun observeBookmarkIds(): Flow<Set<String>> =
        bookmarkDao.observeBookmarkIds().map { it.toSet() }

    override fun observeDownloadedIds(): Flow<Set<String>> =
        downloadDao.observeDownloadedIds().map { it.toSet() }

    override suspend fun refresh(query: FeedQuery): RefreshResult = withContext(Dispatchers.IO) {
        if (!prefs.canMakeRequest(1)) {
            return@withContext RefreshResult.BudgetExhausted
        }

        val homeCountry = prefs.homeCountry.first()
        val (queryString, regionTag) = buildQuery(query, homeCountry)

        val result = provider.fetchEverything(queryString, regionTag = regionTag, pageSize = 30)
        result.fold(
            onSuccess = { articles ->
                val scoredEntities = articles.map { article ->
                    val score = article.sentiment ?: scorer.score("${article.title} ${article.summary}")
                    val scoredArticle = article.copy(sentiment = score)
                    ArticleEntity.fromDomain(scoredArticle)
                }

                articleDao.insertArticles(scoredEntities)
                prefs.recordRequest(1)

                // Prune non-saved/non-downloaded older than 30 days
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                articleDao.pruneOldArticles(thirtyDaysAgo)

                RefreshResult.Success(articles.size)
            },
            onFailure = { throwable ->
                val newsError = if (throwable is NewsApiException) {
                    throwable.error
                } else {
                    NewsError.Offline(throwable.message ?: "Failed to connect to news provider")
                }
                RefreshResult.Error(newsError)
            }
        )
    }

    override suspend fun setBookmarked(articleId: String, saved: Boolean) = withContext(Dispatchers.IO) {
        if (saved) {
            bookmarkDao.bookmark(BookmarkEntity(articleId = articleId))
        } else {
            bookmarkDao.unbookmark(articleId = articleId)
        }
    }

    override suspend fun download(articleId: String): DownloadResult = withContext(Dispatchers.IO) {
        val article = articleDao.getById(articleId) ?: return@withContext DownloadResult.Failure("Article not found in cache")
        var isFullText = false
        var body = article.summary

        try {
            val doc = Jsoup.connect(article.url)
                .userAgent("Mozilla/5.0 (Android; Mobile) MarketBrief/1.0")
                .timeout(7000)
                .get()

            val paragraphs = doc.select("article p, .article-body p, p").map { it.text().trim() }.filter { it.length > 40 }
            if (paragraphs.isNotEmpty()) {
                body = paragraphs.joinToString("\n\n")
                isFullText = true
            }
        } catch (_: Exception) {
            // Fall back to article summary
            isFullText = false
        }

        downloadDao.insertDownload(DownloadEntity(articleId = articleId, bodyText = body, isFullText = isFullText))
        DownloadResult.Success(articleId = articleId, isFullText = isFullText)
    }

    override suspend fun search(query: String): List<Article> = withContext(Dispatchers.IO) {
        articleDao.search(query).map { it.toDomain() }
    }

    private fun rankArticles(articles: List<Article>, homeCountry: String?): List<Article> {
        val now = System.currentTimeMillis()
        return articles.sortedByDescending { article ->
            var score = 0.0
            // Recency score (decays over 48 hours)
            val ageHours = (now - article.publishedAt).coerceAtLeast(0) / (1000.0 * 60 * 60)
            score += (48.0 - ageHours).coerceAtLeast(0.0)

            // Region boost
            if (homeCountry != null && (article.region.equals(homeCountry, ignoreCase = true) ||
                    article.title.contains(homeCountry, ignoreCase = true))) {
                score += 15.0
            }

            // Financial topic relevance boost
            if (article.categories.any { it.equals("Trade", true) || it.equals("Finance", true) || it.equals("Forex", true) }) {
                score += 8.0
            }

            score
        }
    }

    private fun buildQuery(query: FeedQuery, homeCountry: String): Pair<String, String?> {
        val cat = query.categories.firstOrNull { it != Category.All && it != Category.Saved }
        return when {
            cat != null -> NewsQueries.forCategory(cat) to null
            query.country is CountryFilter.Home -> NewsQueries.homeTrade(homeCountry) to homeCountry
            else -> NewsQueries.forCategory(Category.Finance) to null
        }
    }
}
