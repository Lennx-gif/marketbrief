package app.marketbrief.feature.news.model

import app.marketbrief.core.model.Article

sealed interface NewsUiState {
    object Loading : NewsUiState

    data class Success(
        val userName: String = "Roosevelt Huel",
        val selectedCategory: String = "Trending",
        val categories: List<String> = listOf("Trending", "Politics", "Sports", "Health", "Tech"),
        val publishers: List<String> = emptyList(),
        val heroArticle: Article? = null,
        val breakingNews: List<Article> = emptyList(),
        val latestNews: List<Article> = emptyList(),
        val bookmarkedArticleIds: Set<String> = emptySet(),
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null
    ) : NewsUiState

    data class Error(val message: String) : NewsUiState
}