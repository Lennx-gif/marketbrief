package app.marketbrief.feature.news.ui

import app.marketbrief.core.model.Article
import app.marketbrief.core.model.ArticleBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleLoadStateTest {
    private val article = Article(
        id = "article-1",
        title = "Headline",
        summary = "Available summary",
        url = "https://publisher.example/story",
        sourceId = "publisher",
        sourceName = "Publisher",
        publishedAt = 1L
    )

    @Test
    fun `full extraction maps to reader mode`() {
        val result = articleLoadState(
            article,
            ArticleBody(article.id, "Full article body", kind = "FULL"),
            isLoading = false,
            extractionFailed = false
        )

        assertEquals(ArticleLoadState.Full("Full article body"), result)
    }

    @Test
    fun `paywall extraction maps to a preview with a reason`() {
        val result = articleLoadState(
            article,
            ArticleBody(article.id, article.summary, kind = "PREVIEW", failureReason = "PAYWALL"),
            isLoading = false,
            extractionFailed = false
        )

        assertEquals(ArticleLoadState.Preview(article.summary, ArticleLoadFailure.Paywall), result)
    }

    @Test
    fun `loading takes precedence over cached preview`() {
        assertTrue(articleLoadState(article, null, isLoading = true, extractionFailed = false) is ArticleLoadState.Loading)
    }

    @Test
    fun `empty extraction and summary maps to failure`() {
        val noSummary = article.copy(summary = "")
        assertEquals(
            ArticleLoadState.Failed(ArticleLoadFailure.Network),
            articleLoadState(noSummary, null, isLoading = false, extractionFailed = true)
        )
    }
}
