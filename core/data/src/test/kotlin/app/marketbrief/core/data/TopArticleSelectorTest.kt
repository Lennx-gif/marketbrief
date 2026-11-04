package app.marketbrief.core.data

import app.marketbrief.core.model.Article
import org.junit.Assert.assertEquals
import org.junit.Test

class TopArticleSelectorTest {
    private fun article(id: String) = Article(id, id, "", "https://$id.test", null, "s", "s", 1L, emptyList(), emptyList(), null, null)

    @Test fun selectsThreeUncachedArticlesInFeedOrder() {
        val items = (1..5).map { article("$it") }
        assertEquals(listOf("2", "3", "5"), TopArticleSelector.select(items, setOf("1", "4")).map { it.id })
    }
}
