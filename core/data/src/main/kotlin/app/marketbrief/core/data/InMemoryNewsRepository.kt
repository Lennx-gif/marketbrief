package app.marketbrief.core.data

import app.marketbrief.core.model.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory implementation of NewsRepository with pre-populated market news.
 */
class InMemoryNewsRepository(
    initialArticles: List<Article> = defaultArticles()
) : NewsRepository {

    private val allArticles = initialArticles.toMutableList()
    private val _articles = MutableStateFlow<List<Article>>(initialArticles)
    override val articles: Flow<List<Article>> = _articles.asStateFlow()

    private val _savedArticles = MutableStateFlow<List<Article>>(emptyList())
    override val savedArticles: Flow<List<Article>> = _savedArticles.asStateFlow()

    override suspend fun refresh(category: String?) {
        if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) {
            _articles.value = allArticles.toList()
        } else {
            val filtered = allArticles.filter { article ->
                article.categories.any { it.equals(category, ignoreCase = true) }
            }
            _articles.value = if (filtered.isNotEmpty()) filtered else allArticles.toList()
        }
    }

    override suspend fun saveArticle(article: Article) {
        _savedArticles.update { current ->
            if (current.none { it.id == article.id }) current + article else current
        }
    }

    override suspend fun unsaveArticle(articleId: String) {
        _savedArticles.update { current ->
            current.filter { it.id != articleId }
        }
    }

    override suspend fun search(query: String): List<Article> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _articles.value
        return allArticles.filter {
            it.title.lowercase().contains(q) ||
            (it.summary?.lowercase()?.contains(q) == true) ||
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
                    relatedSymbols = listOf("EURUSD", "USDJPY", "US30")
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
                    relatedSymbols = listOf("SPX500")
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
                    categories = listOf("Finance", "Forex"),
                    relatedSymbols = listOf("XAUUSD")
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
                    relatedSymbols = listOf("BTCUSD", "ETHUSD")
                ),
                Article(
                    id = "news-5",
                    title = "AI Startup Funding Surges as Seed Valuations Defy Broader Slowdown",
                    summary = "Venture investments in autonomous coding agents, foundation model optimization, and synthetic media tooling captured the majority of early-stage software funding.",
                    url = "https://example.com/startup-ai",
                    imageUrl = null,
                    sourceId = "techcrunch",
                    sourceName = "TechCrunch",
                    publishedAt = now - 1000 * 60 * 330,
                    categories = listOf("Tech", "Startups", "AI"),
                    relatedSymbols = emptyList()
                )
            )
        }
    }
}
