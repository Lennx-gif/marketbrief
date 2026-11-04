package app.marketbrief.core.storage

import app.marketbrief.core.analysis.sentiment.AssetTagger
import app.marketbrief.core.analysis.sentiment.CategoryClassifier
import app.marketbrief.core.analysis.sentiment.SentimentScorer
import app.marketbrief.core.data.DownloadResult
import app.marketbrief.core.data.NewsProvider
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.data.RefreshResult
import app.marketbrief.core.data.remote.NewsApiException
import app.marketbrief.core.data.remote.NewsError
import app.marketbrief.core.data.remote.NewsQueries
import app.marketbrief.core.data.FtsQuerySanitizer
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.ArticleBody
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.model.FeedQuery
import app.marketbrief.core.storage.db.ArticleBodyDao
import app.marketbrief.core.storage.db.ArticleBodyEntity
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val bookmarkDao: BookmarkDao,
    private val downloadDao: DownloadDao,
    private val articleBodyDao: ArticleBodyDao,
    private val provider: NewsProvider,
    private val prefs: UserPrefsRepository,
    private val articleSearchDao: app.marketbrief.core.storage.db.ArticleSearchDao,
    private val sourcePrefDao: app.marketbrief.core.storage.db.SourcePrefDao,
    private val articleTagDao: app.marketbrief.core.storage.db.ArticleTagDao,
    private val refreshScheduler: app.marketbrief.core.data.NewsRefreshScheduler,
    private val scorer: SentimentScorer = SentimentScorer(),
    private val classifier: CategoryClassifier = CategoryClassifier(),
    private val tagger: AssetTagger = AssetTagger(),
    private val extractor: ArticleExtractor = ArticleExtractor()
) : NewsRepository {

    private val extractionLocks = ConcurrentHashMap<String, Mutex>()

    override fun observeFeed(query: FeedQuery): Flow<List<Article>> {
        return combine(
            articleDao.observeAll(),
            bookmarkDao.observeBookmarkIds(),
            prefs.homeCountry,
            sourcePrefDao.observeAll()
        ) { entityList, bookmarkIds, homeCountry, sourcePrefs ->
            val savedSet = bookmarkIds.toSet()
            val weights = sourcePrefs.associate { it.sourceName to it.weight }
            val domainList = entityList.map { it.toDomain() }

            if (Category.Saved in query.categories) {
                return@combine domainList.filter { it.id in savedSet }
            }

            var filtered = domainList.filter { weights[it.sourceName] != "MUTE" }
            if (Category.All !in query.categories) {
                val categoryLabels = query.categories.map { it.label }
                filtered = filtered.filter { article ->
                    article.categories.any { cat -> categoryLabels.any { it.equals(cat, ignoreCase = true) } }
                }
            }

            when (val countryFilter = query.country) {
                is CountryFilter.Home -> {
                    rankArticles(filtered, homeCountry, weights)
                }
                is CountryFilter.Global -> {
                    rankArticles(filtered, null, weights)
                }
                is CountryFilter.Only -> {
                    val subset = filtered.filter { article ->
                        countryFilter.isoCodes.any { code ->
                            article.region?.equals(code, ignoreCase = true) == true ||
                            article.title.contains(code, ignoreCase = true)
                        }
                    }
                    rankArticles(if (subset.isNotEmpty()) subset else filtered, null, weights)
                }
            }
        }
    }

    override fun observeBookmarkIds(): Flow<Set<String>> =
        bookmarkDao.observeBookmarkIds().map { it.toSet() }

    override fun observeDownloadedIds(): Flow<Set<String>> =
        downloadDao.observeDownloadedIds().map { it.toSet() }

    override fun observeArticleBody(articleId: String): Flow<ArticleBody?> =
        articleBodyDao.observeBody(articleId).map { it?.toDomain() }

    override suspend fun getArticleBody(articleId: String): ArticleBody? = withContext(Dispatchers.IO) {
        articleBodyDao.getBody(articleId)?.toDomain()
    }

    override suspend fun extractArticleBody(articleId: String): Result<ArticleBody> = extractionLocks.computeIfAbsent(articleId) { Mutex() }.withLock {
        withContext(Dispatchers.IO) {
            val cached = articleBodyDao.getBody(articleId)
            if (cached != null && (cached.kind == "FULL" || cached.failureReason != null)) {
                return@withContext Result.success(cached.toDomain())
            }
            val articleEntity = articleDao.getById(articleId)
                ?: return@withContext Result.failure(IllegalStateException("Article not found in cache: $articleId"))

            val body = extractor.extractFromUrl(articleEntity.toDomain())
            articleBodyDao.insertBody(ArticleBodyEntity.fromDomain(body))
            articleSearchDao.removeBody(articleId)
            articleSearchDao.indexBody(app.marketbrief.core.storage.db.ArticleBodyFtsEntity(articleId, body.markdown))
            Result.success(body)
        }
    }

    override fun observeSourcePreferences(): Flow<Map<String, String>> = sourcePrefDao.observeAll().map { rows -> rows.associate { it.sourceName to it.weight } }
    override fun observeAvailableSources(): Flow<List<String>> = articleDao.observeSourceNames()
    override suspend fun setSourcePreference(source: String, weight: String) {
        if (weight == "NORMAL") sourcePrefDao.clear(source) else sourcePrefDao.set(app.marketbrief.core.storage.db.SourcePrefEntity(source, weight))
    }
    override suspend fun articleTags(articleId: String): List<String> = articleTagDao.observeTags(articleId).first()
    override suspend fun setArticleTag(articleId: String, tag: String, enabled: Boolean) {
        if (enabled) articleTagDao.add(app.marketbrief.core.storage.db.ArticleTagEntity(articleId, tag.trim())) else articleTagDao.remove(articleId, tag.trim())
    }
    override fun observeSavedTags(): Flow<List<String>> = articleTagDao.observeAllTags()
    override suspend fun savedArticlesWithTag(tag: String): List<Article> {
        val saved = bookmarkDao.observeBookmarkIds().first().toSet()
        return articleTagDao.articleIdsForTag(tag).filter { it in saved }.mapNotNull { articleDao.getById(it)?.toDomain() }
    }

    override suspend fun refresh(query: FeedQuery): RefreshResult = withContext(Dispatchers.IO) {
        val homeCountry = prefs.homeCountry.first()
        val kenyaQuery = prefs.kenyaTradeLens.first() && homeCountry.equals("Kenya", true) && query.country is CountryFilter.Home && query.categories.none { it != Category.All }
        val requestCount = if (kenyaQuery) 2 else 1
        if (!prefs.canMakeRequest(requestCount)) {
            return@withContext RefreshResult.BudgetExhausted
        }

        val (queryString, regionTag) = buildQuery(query, homeCountry)

        val result = provider.fetchEverything(queryString, regionTag = regionTag, pageSize = 30)
        prefs.recordRequest(1)
        result.fold(
            onSuccess = { articles ->
                val kenyaArticles = if (kenyaQuery) {
                    provider.fetchEverything(NewsQueries.kenyaTrade(), regionTag = "Kenya", pageSize = 30).also { prefs.recordRequest(1) }.getOrDefault(emptyList())
                } else emptyList()
                val combinedArticles = (articles + kenyaArticles).distinctBy { it.id }
                val scoredEntities = combinedArticles.map { article ->
                    val score = article.sentiment ?: scorer.score("${article.title} ${article.summary}")
                    val autoCategories = if (article.categories.isEmpty()) {
                        classifier.classify(article.title, article.summary).map { it.label }
                    } else article.categories
                    val autoSymbols = if (article.relatedSymbols.isEmpty()) {
                        tagger.tag("${article.title} ${article.summary}").toList()
                    } else article.relatedSymbols

                    val enrichedArticle = article.copy(
                        sentiment = score,
                        categories = autoCategories,
                        relatedSymbols = autoSymbols
                    )
                    ArticleEntity.fromDomain(enrichedArticle)
                }

                articleDao.insertArticles(scoredEntities)
                scoredEntities.forEach { articleSearchDao.removeArticle(it.id) }
                articleSearchDao.indexArticles(scoredEntities.map { app.marketbrief.core.storage.db.ArticleFtsEntity(it.id, it.title, it.summary) })

                // Prune non-saved/non-downloaded older than 30 days
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                articleDao.pruneOldArticles(thirtyDaysAgo)
                runCatching { refreshScheduler.schedulePrefetch() }

                RefreshResult.Success(combinedArticles.size)
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
        val body = extractArticleBody(articleId).getOrElse { return@withContext DownloadResult.Failure(it.message ?: "Article extraction failed") }
        downloadDao.insertDownload(
            DownloadEntity(
                articleId = articleId,
                bodyText = body.markdown,
                isFullText = (body.kind == "FULL")
            )
        )

        DownloadResult.Success(articleId = articleId, isFullText = (body.kind == "FULL"))
    }

    override suspend fun search(query: String): List<Article> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val safe = FtsQuerySanitizer.prefixQuery(query) ?: return@withContext emptyList()
        val savedIds = bookmarkDao.observeBookmarkIds().first().toSet()
        val ids = (articleSearchDao.searchArticles(safe) + articleSearchDao.searchBodies(safe)).toSet().intersect(savedIds)
        ids.mapNotNull { articleDao.getById(it)?.toDomain() }
    }

    private fun rankArticles(articles: List<Article>, homeCountry: String?, sourceWeights: Map<String, String>): List<Article> {
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
            if (sourceWeights[article.sourceName] == "BOOST") score += 10.0

            score
        }
    }

    private fun buildQuery(query: FeedQuery, homeCountry: String): Pair<String, String?> {
        val cat = query.categories.firstOrNull { it != Category.All && it != Category.Saved }
        return when {
            cat != null -> NewsQueries.forCategory(cat) to null
            query.country is CountryFilter.Home -> NewsQueries.homeTrade(homeCountry) to homeCountry
            query.country is CountryFilter.Only -> {
                val countries = (query.country as CountryFilter.Only).isoCodes
                val firstCountry = countries.firstOrNull() ?: homeCountry
                NewsQueries.homeTrade(firstCountry) to firstCountry
            }
            else -> NewsQueries.forCategory(Category.Finance) to null
        }
    }
}
