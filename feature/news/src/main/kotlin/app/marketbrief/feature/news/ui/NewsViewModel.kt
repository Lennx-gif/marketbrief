package app.marketbrief.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.DownloadResult
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.data.MarketDataRepository
import app.marketbrief.core.data.RefreshResult
import app.marketbrief.core.entitlements.EntitlementChecker
import app.marketbrief.core.entitlements.Feature
import app.marketbrief.core.entitlements.Gate
import app.marketbrief.core.entitlements.PlanSource
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.model.AssetId
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    val downloaded: Boolean,
    val relatedQuote: app.marketbrief.core.model.Quote? = null
)

data class FeedUiState(
    val articles: List<ArticleUi> = emptyList(),
    val selectedCategories: Set<Category> = setOf(Category.All),
    val country: CountryFilter = CountryFilter.Global,
    val homeCountryName: String = "Kenya",
    val feedSearchText: String = "",
    val isRefreshing: Boolean = false,
    val banner: Banner? = null,
    val savedSearchText: String = "",
    val savedTags: List<String> = emptyList(),
    val selectedSavedTag: String? = null,
    val usdKesRate: Double? = null,
    val showSourceStrip: Boolean = false,
    val showMarketTicker: Boolean = false
)

sealed interface FeedAction {
    data class ToggleCategory(val category: Category) : FeedAction
    data class SetCountry(val filter: CountryFilter) : FeedAction
    data class SetSourceStrip(val enabled: Boolean) : FeedAction
    data class SetMarketTicker(val enabled: Boolean) : FeedAction
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
    private val marketData: MarketDataRepository,
    planSource: PlanSource
) : ViewModel() {

    private val query = MutableStateFlow(FeedQuery())
    private val queryLoaded = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)
    private val banner = MutableStateFlow<Banner?>(null)
    private val savedSearch = MutableStateFlow("")
    private val feedSearch = MutableStateFlow("")
    private val selectedSavedTag = MutableStateFlow<String?>(null)
    private val gate = EntitlementChecker(planSource.plan)
    private val usdKes = MutableStateFlow<Double?>(null)

    private val _events = Channel<FeedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val savedSearchResults = combine(query, savedSearch, selectedSavedTag) { q, text, tag ->
        if (Category.Saved in q.categories) tag to text else null
    }.flatMapLatest { search ->
        when {
            search == null -> flowOf<List<Article>?>(null)
            search.first != null -> kotlinx.coroutines.flow.flow { emit(repository.savedArticlesWithTag(search.first!!)) }
            search.second.isNotBlank() -> kotlinx.coroutines.flow.flow { emit(repository.search(search.second)) }
            else -> flowOf<List<Article>?>(null)
        }
    }

    private val feedFlow = combine(
        query.flatMapLatest { repository.observeFeed(it) },
        savedSearchResults,
        repository.observeBookmarkIds(),
        repository.observeDownloadedIds(),
        marketData.quotes
    ) { articles, searchResults, saved, downloaded, quotes ->
        val quotesBySymbol = quotes.values.associateBy { it.assetId.symbol.uppercase() }
        (searchResults ?: articles).map { article ->
            val requestedSymbols = article.relatedSymbols.map { symbol ->
                when (symbol.uppercase()) {
                    "XAU" -> "XAUUSD"
                    "BTC" -> "BTCUSD"
                    else -> symbol.uppercase()
                }
            }
            ArticleUi(
                article = article,
                bookmarked = article.id in saved,
                downloaded = article.id in downloaded,
                relatedQuote = requestedSymbols.firstNotNullOfOrNull(quotesBySymbol::get)
            )
        }
    }

    private val metaFlow = combine(
        query,
        prefs.homeCountry,
        refreshing,
        banner,
        combine(savedSearch, feedSearch) { saved, feed -> saved to feed }
    ) { q, homeCountry, isRef, b, searches ->
        FeedMeta(q, homeCountry, isRef, b, searches.first, searches.second)
    }

    private val displayPrefsFlow = combine(
        prefs.showSourceStrip,
        prefs.showMarketTickerOnFeed
    ) { showSources, showTicker -> showSources to showTicker }

    private val rateAndDisplayFlow = combine(usdKes, displayPrefsFlow) { rate, displayPrefs ->
        rate to displayPrefs
    }

    val uiState: StateFlow<FeedUiState> = combine(
        feedFlow,
        metaFlow,
        repository.observeSavedTags(),
        selectedSavedTag,
        rateAndDisplayFlow
    ) { articlesUi, meta, tags, tag, rateAndDisplay ->
        val rate = rateAndDisplay.first
        val displayPrefs = rateAndDisplay.second
        val searchText = if (Category.Saved in meta.query.categories) meta.savedSearch else meta.feedSearch
        val visibleArticles = if (searchText.isBlank()) articlesUi else articlesUi.filter { item ->
            item.article.title.contains(searchText, ignoreCase = true) ||
                item.article.summary.contains(searchText, ignoreCase = true) ||
                item.article.sourceName.contains(searchText, ignoreCase = true)
        }
        FeedUiState(
            articles = visibleArticles,
            selectedCategories = meta.query.categories,
            country = meta.query.country,
            homeCountryName = meta.homeCountry,
            feedSearchText = meta.feedSearch,
            isRefreshing = meta.isRefreshing,
            banner = meta.banner,
            savedSearchText = meta.savedSearch,
            savedTags = tags,
            selectedSavedTag = tag,
            usdKesRate = rate,
            showSourceStrip = displayPrefs.first,
            showMarketTicker = displayPrefs.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    init {
        viewModelScope.launch {
            query.value = FeedQuery(
                categories = prefs.feedCategories.first(),
                country = prefs.feedCountry.first()
            )
            queryLoaded.value = true
            refresh()
        }
        viewModelScope.launch {
            prefs.kenyaTradeLens.collect { enabled ->
                if (enabled) {
                    val usdKesAsset = AssetId("USDKES", AssetClass.FOREX)
                    marketData.refreshQuotes(listOf(usdKesAsset))
                    usdKes.value = marketData.quotes.first()[usdKesAsset]?.mid
                } else usdKes.value = null
            }
        }
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.ToggleCategory -> {
                query.value = query.value.toggle(action.category)
                persistFeedQuery()
            }
            is FeedAction.SetCountry -> {
                query.value = query.value.copy(country = action.filter)
                viewModelScope.launch { prefs.setFeedCountry(action.filter) }
            }
            is FeedAction.SetSourceStrip -> {
                viewModelScope.launch { prefs.setShowSourceStrip(action.enabled) }
            }
            is FeedAction.SetMarketTicker -> {
                viewModelScope.launch { prefs.setShowMarketTickerOnFeed(action.enabled) }
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
            if (!queryLoaded.value) return@launch
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

    fun onSavedSearchChanged(value: String) { savedSearch.value = value }
    fun onFeedSearchChanged(value: String) { feedSearch.value = value }
    fun onSavedTagSelected(value: String?) { selectedSavedTag.value = value }

    private fun persistFeedQuery() {
        val selected = query.value
        viewModelScope.launch { prefs.setFeedCategories(selected.categories) }
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
    val banner: Banner?,
    val savedSearch: String,
    val feedSearch: String
)
