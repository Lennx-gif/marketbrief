package app.marketbrief.core.data

import app.marketbrief.core.data.remote.NewsApiArticleDto
import app.marketbrief.core.data.remote.NewsApiProvider
import app.marketbrief.core.data.remote.NewsQueries
import app.marketbrief.core.data.remote.SourceDto
import app.marketbrief.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsApiProviderTest {

    @Test
    fun testCleanTitleRemovesSourceSuffix() {
        val title = "Gold Rallies to Fresh All-Time High - Bloomberg"
        val cleaned = NewsApiProvider.cleanTitle(title, "Bloomberg")
        assertEquals("Gold Rallies to Fresh All-Time High", cleaned)
    }

    @Test
    fun testCleanHtmlStripsTagsAndEntities() {
        val raw = "<p>Global markets &amp; commodities &quot;soar&quot; after <b>Fed</b> rate cut.</p>"
        val cleaned = NewsApiProvider.cleanHtml(raw)
        assertEquals("Global markets & commodities \"soar\" after Fed rate cut.", cleaned)
    }

    @Test
    fun testMapDtoDropsRemovedArticles() {
        val dto = NewsApiArticleDto(
            title = "[Removed]",
            url = "https://example.com/removed",
            description = "Some description"
        )
        val article = NewsApiProvider.mapDtoToArticle(dto)
        assertNull(article)
    }

    @Test
    fun testMapDtoDropsNonHttpUrls() {
        val dto = NewsApiArticleDto(
            title = "Valid Title",
            url = "file:///local/path",
            description = "Some description"
        )
        val article = NewsApiProvider.mapDtoToArticle(dto)
        assertNull(article)
    }

    @Test
    fun testUrlNormalizationAndSha256Id() {
        val url1 = "https://example.com/markets/gold-surge/?utm_source=twitter"
        val url2 = "https://example.com/markets/gold-surge"

        val norm1 = NewsApiProvider.normalizeUrl(url1)
        val norm2 = NewsApiProvider.normalizeUrl(url2)

        assertEquals("https://example.com/markets/gold-surge", norm2)
        val hash = NewsApiProvider.sha256(norm2)
        assertEquals(64, hash.length)
    }

    @Test
    fun testParsePublishedAtIsoTimestamp() {
        val iso = "2026-10-16T12:00:00Z"
        val millis = NewsApiProvider.parsePublishedAt(iso)
        assertTrue("Millis should be valid positive timestamp", millis > 0L)
    }

    @Test
    fun testNewsQueriesHomeTrade() {
        val query = NewsQueries.homeTrade("Kenya")
        assertTrue(query.contains("\"Kenya\""))
        assertTrue(query.contains("trade"))
        assertTrue(query.contains("exports"))
    }

    @Test
    fun testNewsQueriesCategory() {
        val cryptoQuery = NewsQueries.forCategory(Category.Crypto)
        assertTrue(cryptoQuery.contains("bitcoin"))
        assertTrue(cryptoQuery.contains("crypto"))
    }
}
