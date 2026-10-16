package app.marketbrief.core.storage

import app.marketbrief.core.model.Article
import app.marketbrief.core.storage.db.ArticleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageTest {

    @Test
    fun testArticleEntityMapping() {
        val article = Article(
            id = "test-123",
            title = "Central Bank Keeps Benchmark Rates Steady",
            summary = "Monetary policy committee maintains rate.",
            url = "https://example.com/rates",
            imageUrl = "https://example.com/image.jpg",
            sourceId = "reuters",
            sourceName = "Reuters",
            publishedAt = 1729000000000L,
            categories = listOf("Finance", "Macro"),
            relatedSymbols = listOf("EURUSD", "USDJPY"),
            region = "KE",
            sentiment = 0.35f
        )

        val entity = ArticleEntity.fromDomain(article)
        assertEquals("test-123", entity.id)
        assertEquals("KE", entity.region)
        assertEquals("Finance,Macro", entity.categories)
        assertEquals("EURUSD,USDJPY", entity.relatedSymbols)

        val restored = entity.toDomain()
        assertEquals(article.id, restored.id)
        assertEquals(article.title, restored.title)
        assertEquals(article.categories, restored.categories)
        assertEquals(article.relatedSymbols, restored.relatedSymbols)
        assertEquals(article.region, restored.region)
        assertEquals(article.sentiment, restored.sentiment)
    }

    @Test
    fun testBudgetConstants() {
        assertEquals(100, UserPrefsRepository.DAILY_BUDGET_LIMIT)
        assertNotNull(UserPrefsRepository.KEY_HOME_COUNTRY)
        assertNotNull(UserPrefsRepository.KEY_THEME_MODE)
        assertNotNull(UserPrefsRepository.KEY_BUDGET_COUNT)
    }
}
