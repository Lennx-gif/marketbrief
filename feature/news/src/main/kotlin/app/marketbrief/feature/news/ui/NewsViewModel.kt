package app.marketbrief.feature.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.NewsRepository
import app.marketbrief.core.model.Article
import app.marketbrief.core.entitlements.EntitlementChecker
import app.marketbrief.core.entitlements.Feature
import app.marketbrief.core.entitlements.Gate
import app.marketbrief.core.entitlements.PlanSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val planSource: PlanSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState

    private val entitlementChecker = EntitlementChecker(planSource.plan)

    init {
        loadArticles()
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category, articles = emptyList(), isLoading = true) }
        loadArticles(category)
    }

    fun refresh(category: String? = null) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadArticles(category)
    }

    private fun loadArticles(category: String? = null) {
        viewModelScope.launch {
            repository.refresh(category)
            val result = repository.articles.first()
            _uiState.update { it.copy(articles = result, isLoading = false) }
        }
    }

    fun saveArticle(article: Article) {
        val gate = entitlementChecker.gate(Feature.NEWS_SAVED)
        when (gate) {
            is Gate.Open -> viewModelScope.launch { repository.saveArticle(article) }
            is Gate.Locked -> _uiState.update { it.copy(error = gate.upgradeMessage) }
            is Gate.LimitExceeded -> _uiState.update { it.copy(error = "Saved articles limit reached (${gate.max})") }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            val results = repository.search(query)
            _uiState.update { it.copy(searchResults = results, isLoading = false) }
        }
    }
}

data class NewsUiState(
    val selectedCategory: String = "Tech",
    val articles: List<Article> = emptyList(),
    val searchResults: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)