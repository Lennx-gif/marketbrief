package app.marketbrief.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.DownloadResult
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.data.RefreshResult
import app.marketbrief.core.entitlements.EntitlementChecker
import app.marketbrief.core.entitlements.Feature
import app.marketbrief.core.entitlements.Gate
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Category
import app.marketbrief.core.model.CountryFilter
import app.marketbrief.core.model.FeedQuery
import app.marketbrief.core.storage.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Banner(
    val message: String,
    val isWarning: Boolean = false,
    val actionLabel: String? = null
)

data class ArticleUi(
    val article: Article,
    val bookmarked: Boolean,
    val downloaded: Boolean
)

data class FeedUiState(
    val articles: List<ArticleUi> = emptyList(),
    val selectedCategories: Set<Category> = setOf(Category.All),
    val country: CountryFilter = CountryFilter.Home,
    val homeCountryName: String = "Kenya",
    val isRefreshing: Boolean = false,
    val banner: Banner? = null
)

sealed interface FeedAction {
    data class ToggleCategory(val category: Category) : FeedAction
    data class SetCountry(val filter: CountryFilter) : FeedAction
    data class ToggleBookmark(val article: Article) : FeedAction
    data class Download(val articleId: String) : FeedAction
    data object Refresh : FeedAction
    data object DismissBanner : FeedAction
}

sealed interface FeedEvent {
    data class Message(val text: String) : FeedEvent
    data class OpenCalculator(val symbol: String) : FeedEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val prefs: UserPrefsRepository,
    planSource: PlanSource
) : ViewModel() {

    private val query = MutableStateFlow(FeedQuery())
    private val refreshing = MutableStateFlow(false)
    private val banner = MutableStateFlow<Banner?>(null)
    private val gate = EntitlementChecker(planSource.plan)

    private val _events = Channel<FeedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val feedFlow = combine(
        query.flatMapLatest { repository.observeFeed(it) },
        repository.observeBookmarkIds(),
        repository.observeDownloadedIds()
    ) { articles, saved, downloaded ->
        articles.map { ArticleUi(it, it.id in saved, it.id in downloaded) }
    }

    private val metaFlow = combine(
        query,
        prefs.homeCountry,
        refreshing,
        banner
    ) { q, homeCountry, isRef, b ->
        FeedMeta(q, homeCountry, isRef, b)
    }

    val uiState: StateFlow<FeedUiState> = combine(
        feedFlow,
        metaFlow
    ) { articlesUi, meta ->
        FeedUiState(
            articles = articlesUi,
            selectedCategories = meta.query.categories,
            country = meta.query.country,
            homeCountryName = meta.homeCountry,
            isRefreshing = meta.isRefreshing,
            banner = meta.banner
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    init {
        refresh()
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.ToggleCategory -> {
                query.value = query.value.toggle(action.category)
            }
            is FeedAction.SetCountry -> {
                query.value = query.value.copy(country = action.filter)
            }
            is FeedAction.ToggleBookmark -> {
                handleBookmarkToggle(action.article)
            }
            is FeedAction.Download -> {
                handleDownload(action.articleId)
            }
            is FeedAction.Refresh -> {
                refresh()
            }
            is FeedAction.DismissBanner -> {
                banner.value = null
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            when (val result = repository.refresh(query.value)) {
                is RefreshResult.Success -> {
                    banner.value = null
                }
                is RefreshResult.BudgetExhausted -> {
                    banner.value = Banner(
                        message = "Daily 100-request quota reached. Serving offline Room cache.",
                        isWarning = true,
                        actionLabel = "Dismiss"
                    )
                }
                is RefreshResult.Error -> {
                    banner.value = Banner(
                        message = "Network error: Showing cached headlines.",
                        isWarning = true,
                        actionLabel = "Retry"
                    )
                }
            }
            refreshing.value = false
        }
    }

    private fun handleBookmarkToggle(article: Article) {
        when (val check = gate.gate(Feature.NEWS_SAVED)) {
            is Gate.Open -> {
                val currentSaved = uiState.value.articles.firstOrNull { it.article.id == article.id }?.bookmarked ?: false
                viewModelScope.launch {
                    repository.setBookmarked(article.id, !currentSaved)
                    _events.send(FeedEvent.Message(if (!currentSaved) "Article saved to Bookmarks" else "Removed from Bookmarks"))
                }
            }
            is Gate.Locked -> {
                viewModelScope.launch {
                    _events.send(FeedEvent.Message(check.upgradeMessage))
                }
            }
            is Gate.LimitExceeded -> {
                viewModelScope.launch {
                    _events.send(FeedEvent.Message("Bookmark limit reached for your plan (${check.max})"))
                }
            }
        }
    }

    private fun handleDownload(articleId: String) {
        viewModelScope.launch {
            when (val res = repository.download(articleId)) {
                is DownloadResult.Success -> {
                    val msg = if (res.isFullText) "Article saved for offline reading" else "Saved preview for offline reading"
                    _events.send(FeedEvent.Message(msg))
                }
                is DownloadResult.Failure -> {
                    _events.send(FeedEvent.Message(res.message))
                }
            }
        }
    }

    // Compatibility methods
    fun onCategorySelected(category: String) {
        val cat = Category.entries.firstOrNull { it.label.equals(category, ignoreCase = true) } ?: Category.All
        onAction(FeedAction.ToggleCategory(cat))
    }

    fun saveArticle(article: Article) = onAction(FeedAction.ToggleBookmark(article))
}

private data class FeedMeta(
    val query: FeedQuery,
    val homeCountry: String,
    val isRefreshing: Boolean,
    val banner: Banner?
)