package app.marketbrief.feature.news

import app.marketbrief.core.data.InMemoryNewsRepository
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Category
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.feature.news.model.NewsUiState
import app.marketbrief.feature.news.ui.NewsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

    private fun createMockPrefs(
        preferredCategories: Set<String> = emptySet(),
        feedCategories: Set<Category> = setOf(Category.All)
    ): UserPrefsRepository {
        val prefs = mockk<UserPrefsRepository>(relaxed = true)
        every { prefs.feedCategories } returns flowOf(feedCategories)
        every { prefs.preferredCategories } returns flowOf(preferredCategories)
        every { prefs.homeCountry } returns flowOf("Kenya")
        every { prefs.dailyBudgetUsed } returns flowOf(0)
        every { prefs.showSourceStrip } returns flowOf(false)
        every { prefs.showMarketTickerOnFeed } returns flowOf(false)
        return prefs
    }

    @Test
    fun testInitialState() = runTest {
        val repository = InMemoryNewsRepository(emptyList())
        val prefs = createMockPrefs()
        val viewModel = NewsViewModel(repository, prefs)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.filterIsInstance<NewsUiState.Success>().first()

        assertTrue(state.latestNews.isEmpty())
        assertNull(state.errorMessage)
        assertEquals("Trending", state.selectedCategory)
        assertEquals(listOf("Trending", "Politics", "Sports", "Health", "Tech"), state.categories)
    }

    @Test
    fun testCategorySelection() = runTest {
        val repository = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = createMockPrefs()
        val viewModel = NewsViewModel(repository, prefs)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        // Wait for initial state
        viewModel.uiState.filterIsInstance<NewsUiState.Success>().first()

        // Select a category
        viewModel.onCategorySelected("Finance")

        val state = viewModel.uiState.value
        when (state) {
            is NewsUiState.Success -> {
                assertEquals("Finance", state.selectedCategory)
                assertNotNull(state.heroArticle)
                assertTrue(state.breakingNews.isNotEmpty())
                assertTrue(state.latestNews.isEmpty())
            }
            else -> throw AssertionError("Expected Success state but was $state")
        }
    }

    @Test
    fun testSearchFiltersLocallyAndCanBeCleared() = runTest {
        val repository = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = createMockPrefs()
        val viewModel = NewsViewModel(repository, prefs)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        // Wait for initial state
        viewModel.uiState.filterIsInstance<NewsUiState.Success>().first()

        // Search for non-matching query
        viewModel.onSearchQueryChanged("no article matches this query")
        var state = viewModel.uiState.value
        when (state) {
            is NewsUiState.Success -> {
                assertTrue(state.latestNews.isEmpty())
                assertNull(state.heroArticle)
                assertTrue(state.breakingNews.isEmpty())
            }
            else -> throw AssertionError("Expected Success state but was $state")
        }

        // Clear search
        viewModel.onSearchQueryChanged("")
        state = viewModel.uiState.value
        when (state) {
            is NewsUiState.Success -> {
                assertNotNull(state.heroArticle)
                assertTrue(state.breakingNews.isNotEmpty())
                assertTrue(state.latestNews.isEmpty())
            }
            else -> throw AssertionError("Expected Success state but was $state")
        }
    }

    @Test
    fun testToggleBookmark() = runTest {
        val repository = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = createMockPrefs()
        val viewModel = NewsViewModel(repository, prefs)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        // Wait for initial state
        val initialState = viewModel.uiState.filterIsInstance<NewsUiState.Success>().first()
        val articleId = "news-1"
        assertTrue(!initialState.bookmarkedArticleIds.contains(articleId))

        // Toggle bookmark
        viewModel.toggleBookmark(articleId)

        val state = viewModel.uiState.value
        when (state) {
            is NewsUiState.Success -> {
                assertTrue(state.bookmarkedArticleIds.contains(articleId))
            }
            else -> throw AssertionError("Expected Success state but was $state")
        }

        // Toggle again to remove
        viewModel.toggleBookmark(articleId)

        val state2 = viewModel.uiState.value
        when (state2) {
            is NewsUiState.Success -> {
                assertTrue(!state2.bookmarkedArticleIds.contains(articleId))
            }
            else -> throw AssertionError("Expected Success state but was $state2")
        }
    }

    @Test
    fun testPreferredCategoriesPrioritizeFeed() = runTest {
        val articles = listOf(
            Article(
                id = "art-finance",
                title = "Finance Article",
                summary = "Summary",
                url = "https://example.com/finance",
                sourceId = "source",
                sourceName = "Source",
                publishedAt = 1000L,
                categories = listOf("Finance")
            ),
            Article(
                id = "art-tech",
                title = "Tech Article",
                summary = "Summary",
                url = "https://example.com/tech",
                sourceId = "source",
                sourceName = "Source",
                publishedAt = 2000L,
                categories = listOf("Tech")
            )
        )
        val repository = InMemoryNewsRepository(articles)
        val prefs = createMockPrefs(preferredCategories = setOf("Tech"))
        val viewModel = NewsViewModel(repository, prefs)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.filterIsInstance<NewsUiState.Success>().first()

        assertTrue(state.categories.contains("Trending"))
        assertTrue(state.categories.contains("Saved"))
        assertTrue(state.categories.contains("Tech"))

        // Because "Tech" is in preferredCategories, "art-tech" is sorted first on "Trending" feed!
        assertEquals("art-tech", state.heroArticle?.id)
    }

    @Test
    fun testFilterToSavedShowsOnlyBookmarkedArticles() = runTest {
        val articles = listOf(
            Article(
                id = "art-1",
                title = "Article 1",
                summary = "Summary",
                url = "https://example.com/1",
                sourceId = "source",
                sourceName = "Source",
                publishedAt = 1000L,
                categories = listOf("Finance")
            ),
            Article(
                id = "art-2",
                title = "Article 2",
                summary = "Summary",
                url = "https://example.com/2",
                sourceId = "source",
                sourceName = "Source",
                publishedAt = 2000L,
                categories = listOf("Tech")
            )
        )
        val repository = InMemoryNewsRepository(articles)
        val prefs = createMockPrefs()
        val viewModel = NewsViewModel(repository, prefs)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.uiState.filterIsInstance<NewsUiState.Success>().first()

        // Bookmark art-2
        viewModel.toggleBookmark("art-2")

        // Filter to Saved
        viewModel.filterToSaved()

        val state = viewModel.uiState.value
        when (state) {
            is NewsUiState.Success -> {
                assertEquals("Saved", state.selectedCategory)
                assertEquals("art-2", state.heroArticle?.id)
                assertTrue(state.breakingNews.isEmpty())
                assertTrue(state.latestNews.isEmpty())
            }
            else -> throw AssertionError("Expected Success state but was $state")
        }
    }

    @Test
    fun testSelectCategoryMethod() = runTest {
        val repository = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = createMockPrefs()
        val viewModel = NewsViewModel(repository, prefs)

        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.uiState.filterIsInstance<NewsUiState.Success>().first()

        viewModel.selectCategory("Forex")

        val state = viewModel.uiState.value
        when (state) {
            is NewsUiState.Success -> {
                assertEquals("Forex", state.selectedCategory)
            }
            else -> throw AssertionError("Expected Success state but was $state")
        }
    }
}
