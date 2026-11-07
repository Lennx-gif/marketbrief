package app.marketbrief.feature.news.ui

import app.marketbrief.core.model.Article
import app.marketbrief.core.model.ArticleBody

sealed interface ArticleLoadState {
    data object Loading : ArticleLoadState
    data class Full(val markdown: String) : ArticleLoadState
    data class Preview(val summary: String, val reason: ArticleLoadFailure) : ArticleLoadState
    data class Failed(val reason: ArticleLoadFailure) : ArticleLoadState
}

enum class ArticleLoadFailure {
    Network,
    Paywall,
    LowQuality,
    Blocked
}

fun articleLoadState(
    article: Article,
    body: ArticleBody?,
    isLoading: Boolean,
    extractionFailed: Boolean
): ArticleLoadState = when {
    isLoading -> ArticleLoadState.Loading
    body?.kind == "FULL" && body.markdown.isNotBlank() -> ArticleLoadState.Full(body.markdown)
    else -> {
        val reason = when (body?.failureReason?.uppercase()) {
            "PAYWALL" -> ArticleLoadFailure.Paywall
            "BLOCKED" -> ArticleLoadFailure.Blocked
            "EMPTY", "LOW_QUALITY" -> ArticleLoadFailure.LowQuality
            "NETWORK_ERROR", "TIMEOUT" -> ArticleLoadFailure.Network
            else -> if (extractionFailed) ArticleLoadFailure.Network else ArticleLoadFailure.LowQuality
        }
        val preview = body?.markdown?.takeIf { it.isNotBlank() } ?: article.summary
        if (preview.isBlank()) ArticleLoadState.Failed(reason)
        else ArticleLoadState.Preview(preview, reason)
    }
}
