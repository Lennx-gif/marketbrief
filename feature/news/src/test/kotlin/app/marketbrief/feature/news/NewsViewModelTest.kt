package app.marketbrief.feature.news

import app.marketbrief.core.model.Article
import app.marketbrief.core.entitlements.Plan
import app.marketbrief.feature.news.ui.NewsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        val repository = app.marketbrief.core.data.InMemoryNewsRepository(emptyList())
        val planSource = object : app.marketbrief.core.entitlements.PlanSource {
            override val plan: Plan = Plan.PERSONAL
        }
        val viewModel = NewsViewModel(repository, planSource)
        
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state.articles.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun testArticleCreation() {
        val article = Article(
            id = "test-1",
            title = "EURUSD Breaks Resistance",
            summary = "Technical analysis shows EURUSD breaking key resistance at 1.0900",
            url = "https://example.com/article",
            imageUrl = "https://example.com/image.jpg",
            sourceId = "forex-factory",
            sourceName = "Forex Factory",
            publishedAt = System.currentTimeMillis(),
            categories = listOf("Forex", "Technical Analysis"),
            relatedSymbols = listOf("EURUSD", "EURJPY")
        )
        
        assertEquals("test-1", article.id)
        assertEquals("EURUSD Breaks Resistance", article.title)
        assertEquals(2, article.categories.size)
        assertEquals(2, article.relatedSymbols.size)
    }
}