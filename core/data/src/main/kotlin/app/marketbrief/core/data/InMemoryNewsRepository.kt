package app.marketbrief.core.data

import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.model.FeedQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation of NewsRepository with pre-populated market news and full reactive contract.
 */
class InMemoryNewsRepository(
    initialArticles: List<Article> = defaultArticles()
) : NewsRepository {

    private val allArticles = initialArticles.toMutableList()
    private val _articles = MutableStateFlow<List<Article>>(initialArticles)
    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observeFeed(query: FeedQuery): Flow<List<Article>> = _articles.map { list ->
        var filtered = list
        if (Category.Saved in query.categories) {
            val saved = _bookmarkedIds.value
            return@map filtered.filter { it.id in saved }
        }
        if (Category.All !in query.categories) {
            val catLabels = query.categories.map { it.label }
            filtered = filtered.filter { article ->
                article.categories.any { cat -> catLabels.any { it.equals(cat, ignoreCase = true) } }
            }
        }
        when (val country = query.country) {
            is CountryFilter.Home -> {
                // Return articles prioritizing home region
                filtered.sortedByDescending { it.region == "KE" }
            }
            is CountryFilter.Global -> filtered
            is CountryFilter.Only -> filtered.filter { it.region in country.isoCodes }
        }
    }

    override fun observeBookmarkIds(): Flow<Set<String>> = _bookmarkedIds.asStateFlow()

    override fun observeDownloadedIds(): Flow<Set<String>> = _downloadedIds.asStateFlow()

    override suspend fun refresh(query: FeedQuery): RefreshResult {
        // Simulates successful refresh
        return RefreshResult.Success(_articles.value.size)
    }

    override suspend fun setBookmarked(articleId: String, saved: Boolean) {
        _bookmarkedIds.update { current ->
            if (saved) current + articleId else current - articleId
        }
    }

    override suspend fun download(articleId: String): DownloadResult {
        _downloadedIds.update { it + articleId }
        return DownloadResult.Success(articleId, isFullText = true)
    }

    override suspend fun search(query: String): List<Article> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _articles.value
        return allArticles.filter {
            it.title.lowercase().contains(q) ||
            it.summary.lowercase().contains(q) ||
            it.relatedSymbols.any { sym -> sym.lowercase().contains(q) }
        }
    }

    companion object {
        fun defaultArticles(): List<Article> {
            val now = System.currentTimeMillis()
            return listOf(
                Article(
                    id = "news-1",
                    title = "Federal Reserve Signals Measured Pace on Benchmark Rate Adjustments",
                    summary = "Federal Reserve policymakers emphasized a cautious stance on monetary easing, pointing to resilient labor markets and steady progress toward their long-term 2% inflation target.",
                    url = "https://example.com/fed-policy",
                    imageUrl = null,
                    sourceId = "reuters",
                    sourceName = "Reuters",
                    publishedAt = now - 1000 * 60 * 25,
                    categories = listOf("Finance", "Macro", "Forex"),
                    relatedSymbols = listOf("EURUSD", "USDJPY", "US30"),
                    region = "US",
                    sentiment = 0.25f
                ),
                Article(
                    id = "news-2",
                    title = "Tech Titans Accelerate Enterprise AI Infrastructure Capex",
                    summary = "Major technology firms reported record datacenter investments driven by surging demand for generative AI training and accelerated computing infrastructure.",
                    url = "https://example.com/tech-ai",
                    imageUrl = null,
                    sourceId = "bloomberg",
                    sourceName = "Bloomberg",
                    publishedAt = now - 1000 * 60 * 75,
                    categories = listOf("Tech", "AI", "Stocks"),
                    relatedSymbols = listOf("SPX500"),
                    region = "US",
                    sentiment = 0.65f
                ),
                Article(
                    id = "news-3",
                    title = "Spot Gold Reasserts Bullish Trend Amid Heightened Safe-Haven Demand",
                    summary = "Precious metals traded higher across global desks as central banks maintained aggressive gold accumulation programs and currency diversification initiatives.",
                    url = "https://example.com/gold-trend",
                    imageUrl = null,
                    sourceId = "marketwatch",
                    sourceName = "MarketWatch",
                    publishedAt = now - 1000 * 60 * 120,
                    categories = listOf("Finance", "Forex", "Commodities"),
                    relatedSymbols = listOf("XAUUSD"),
                    region = "Global",
                    sentiment = 0.70f
                ),
                Article(
                    id = "news-4",
                    title = "Bitcoin and Major Crypto Assets Consolidate Above Critical Support",
                    summary = "Digital asset liquidity expanded following steady institutional spot ETF inflows, with market participants closely monitoring derivatives open interest.",
                    url = "https://example.com/crypto-consolidation",
                    imageUrl = null,
                    sourceId = "coindesk",
                    sourceName = "CoinDesk",
                    publishedAt = now - 1000 * 60 * 210,
                    categories = listOf("Crypto", "Finance"),
                    relatedSymbols = listOf("BTCUSD", "ETHUSD"),
                    region = "Global",
                    sentiment = 0.35f
                ),
                Article(
                    id = "news-5",
                    title = "Kenya Trade Balance Strengthens on Robust Agricultural and Tea Exports",
                    summary = "East Africa commercial flows recorded notable surplus driven by strong European and Gulf market demand for premium specialty tea, coffee, and horticultural products.",
                    url = "https://example.com/kenya-trade",
                    imageUrl = null,
                    sourceId = "businessdaily",
                    sourceName = "Business Daily",
                    publishedAt = now - 1000 * 60 * 330,
                    categories = listOf("Trade", "Finance"),
                    relatedSymbols = listOf("KES"),
                    region = "KE",
                    sentiment = 0.60f
                )
            )
        }
    }
}
