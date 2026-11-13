package app.marketbrief.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.model.FeedQuery
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.feature.news.model.NewsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val prefs: UserPrefsRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("Trending")
    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _events = Channel<NewsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val articlesFlow = repository.observeFeed(defaultFeedQuery())
        .map { articles ->
            articles.map { it.copy(categories = normalizeCategories(it.categories)) }
        }

    // Convert bookmarks flow to StateFlow for easy current value access
    private val bookmarksState = repository.observeBookmarkIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val uiState: StateFlow<NewsUiState> = combine(
        listOf(
            articlesFlow as Flow<Any>,
            bookmarksState as Flow<Any>,
            _selectedCategory as Flow<Any>,
            _searchQuery as Flow<Any>,
            _isRefreshing as Flow<Any>,
            _errorMessage as Flow<Any>,
            prefs.preferredCategories as Flow<Any>
        )
    ) { values ->
        val articles = values[0] as List<Article>
        val bookmarkedIds = values[1] as Set<String>
        val selectedCategory = values[2] as String
        val searchQuery = values[3] as String
        val isRefreshing = values[4] as Boolean
        val errorMessage = values[5] as String?
        val preferredCategories = (values[6] as? Set<String>).orEmpty()

        val filtered = filterArticles(articles, selectedCategory, searchQuery, bookmarkedIds, preferredCategories)

        val heroArticle = filtered.firstOrNull()
        val breakingNews = filtered.drop(1).take(4)
        val latestNews = filtered.drop(5)

        val publishers = articles.map { it.sourceName }.distinct().take(10)

        val categoryChips = if (preferredCategories.isNotEmpty()) {
            (listOf("Trending", "Saved") + preferredCategories + listOf("Finance", "Forex", "Crypto", "Tech", "Trade", "Politics")).distinct()
        } else {
            listOf("Trending", "Politics", "Sports", "Health", "Tech")
        }

        NewsUiState.Success(
            selectedCategory = selectedCategory,
            categories = categoryChips,
            heroArticle = heroArticle,
            breakingNews = breakingNews,
            latestNews = latestNews,
            bookmarkedArticleIds = bookmarkedIds,
            publishers = publishers,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NewsUiState.Loading
    )

    init {
        viewModelScope.launch {
            prefs.feedCategories.collect { categories ->
                val category = categories.firstOrNull { it != Category.Saved } ?: Category.All
                _selectedCategory.value = mapCategoryToDisplayName(category)
            }
        }
    }

    fun selectCategory(category: String) {
        onCategorySelected(category)
    }

    fun filterToSaved() {
        onCategorySelected("Saved")
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        persistCategorySelection(category)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            val currentlyBookmarked = articleId in bookmarksState.value
            repository.setBookmarked(articleId, !currentlyBookmarked)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            when (val result = repository.refresh(defaultFeedQuery())) {
                is app.marketbrief.core.data.RefreshResult.Success -> {
                    _errorMessage.value = null
                }
                is app.marketbrief.core.data.RefreshResult.BudgetExhausted -> {
                    _errorMessage.value = "Daily request quota reached. Showing cached articles."
                }
                is app.marketbrief.core.data.RefreshResult.Error -> {
                    _errorMessage.value = "Network error: Showing cached headlines."
                }
            }
            _isRefreshing.value = false
        }
    }

    private fun defaultFeedQuery(): FeedQuery {
        return FeedQuery(
            categories = setOf(Category.All),
            country = CountryFilter.Global
        )
    }

    private fun filterArticles(
        articles: List<Article>,
        selectedCategory: String,
        searchQuery: String,
        bookmarkedIds: Set<String>,
        preferredCategories: Set<String>
    ): List<Article> {
        val filtered = articles.filter { article ->
            val matchesCategory = when (selectedCategory) {
                "Trending" -> true
                "Saved" -> article.id in bookmarkedIds
                else -> article.categories.any { it.equals(selectedCategory, ignoreCase = true) }
            }
            val matchesSearch = searchQuery.isBlank() ||
                article.title.contains(searchQuery, ignoreCase = true) ||
                article.summary.contains(searchQuery, ignoreCase = true) ||
                article.sourceName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        return if (selectedCategory == "Trending" && preferredCategories.isNotEmpty()) {
            filtered.sortedByDescending { article ->
                article.categories.any { cat -> preferredCategories.any { it.equals(cat, ignoreCase = true) } }
            }
        } else {
            filtered
        }
    }

    private fun normalizeCategories(categories: List<String>): List<String> {
        return categories.map { cat ->
            when (cat.lowercase()) {
                "macro" -> "Finance"
                "ai" -> "Tech"
                else -> cat
            }
        }.distinct()
    }

    private fun mapCategoryToDisplayName(category: Category): String {
        return when (category) {
            Category.All -> "Trending"
            Category.Trade -> "Trade"
            Category.Finance -> "Finance"
            Category.Forex -> "Forex"
            Category.Tech -> "Tech"
            Category.Politics -> "Politics"
            Category.Stocks -> "Stocks"
            Category.Crypto -> "Crypto"
            Category.Commodities -> "Commodities"
            else -> "Trending"
        }
    }

    private fun persistCategorySelection(displayName: String) {
        val category = when (displayName) {
            "Trending" -> Category.All
            "Trade" -> Category.Trade
            "Finance" -> Category.Finance
            "Forex" -> Category.Forex
            "Tech" -> Category.Tech
            "Politics" -> Category.Politics
            "Stocks" -> Category.Stocks
            "Crypto" -> Category.Crypto
            "Commodities" -> Category.Commodities
            else -> Category.All
        }
        viewModelScope.launch {
            prefs.setFeedCategories(setOf(category))
        }
    }

    sealed interface NewsEvent {
        data class Message(val text: String) : NewsEvent
        data class OpenArticle(val articleId: String) : NewsEvent
    }

    // Compatibility methods for existing code
    fun onAction(action: Any) {
        // Legacy compatibility - no-op for now
    }

    fun saveArticle(article: Article) = toggleBookmark(article.id)
}