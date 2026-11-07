package app.marketbrief.feature.news

import app.marketbrief.core.data.InMemoryNewsRepository
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.entitlements.Plan
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.feature.news.ui.FeedAction
import app.marketbrief.feature.news.ui.NewsViewModel
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
        val repository = InMemoryNewsRepository(emptyList())
        val prefs = mockk<UserPrefsRepository>(relaxed = true)
        every { prefs.homeCountry } returns flowOf("Kenya")
        every { prefs.dailyBudgetUsed } returns flowOf(0)
        every { prefs.feedCategories } returns flowOf(setOf(Category.All))
        every { prefs.feedCountry } returns flowOf(CountryFilter.Global)
        every { prefs.showSourceStrip } returns flowOf(false)
        every { prefs.showMarketTickerOnFeed } returns flowOf(false)

        val planSource = object : PlanSource {
            override val plan: Plan = Plan.PERSONAL
        }

        val marketData = mockk<MarketDataRepository>(relaxed = true)
        every { marketData.quotes } returns flowOf(emptyMap())
        val viewModel = NewsViewModel(repository, prefs, marketData, planSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state.articles.isEmpty())
        assertNull(state.banner)
        assertEquals("Kenya", state.homeCountryName)
    }

    @Test
    fun testCategoryToggle() = runTest {
        val repository = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = mockk<UserPrefsRepository>(relaxed = true)
        every { prefs.homeCountry } returns flowOf("Kenya")
        every { prefs.dailyBudgetUsed } returns flowOf(0)
        every { prefs.feedCategories } returns flowOf(setOf(Category.All))
        every { prefs.feedCountry } returns flowOf(CountryFilter.Global)
        every { prefs.showSourceStrip } returns flowOf(false)
        every { prefs.showMarketTickerOnFeed } returns flowOf(false)

        val planSource = object : PlanSource {
            override val plan: Plan = Plan.PERSONAL
        }

        val marketData = mockk<MarketDataRepository>(relaxed = true)
        every { marketData.quotes } returns flowOf(emptyMap())
        val viewModel = NewsViewModel(repository, prefs, marketData, planSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onAction(FeedAction.ToggleCategory(Category.Trade))

        val state = viewModel.uiState.value
        assertTrue("Trade category should be selected", Category.Trade in state.selectedCategories)
        coVerify { prefs.setFeedCategories(match { Category.Trade in it }) }
    }

    @Test
    fun feedSearchFiltersLocallyAndCanBeCleared() = runTest {
        val repository = InMemoryNewsRepository(InMemoryNewsRepository.defaultArticles())
        val prefs = mockk<UserPrefsRepository>(relaxed = true)
        every { prefs.homeCountry } returns flowOf("Kenya")
        every { prefs.dailyBudgetUsed } returns flowOf(0)
        every { prefs.feedCategories } returns flowOf(setOf(Category.All))
        every { prefs.feedCountry } returns flowOf(CountryFilter.Global)
        every { prefs.showSourceStrip } returns flowOf(false)
        every { prefs.showMarketTickerOnFeed } returns flowOf(false)

        val planSource = object : PlanSource {
            override val plan: Plan = Plan.PERSONAL
        }
        val marketData = mockk<MarketDataRepository>(relaxed = true)
        every { marketData.quotes } returns flowOf(emptyMap())
        val viewModel = NewsViewModel(repository, prefs, marketData, planSource)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onFeedSearchChanged("no article matches this query")
        assertTrue(viewModel.uiState.value.articles.isEmpty())

        viewModel.onFeedSearchChanged("")
        assertTrue(viewModel.uiState.value.articles.isNotEmpty())
    }
}
