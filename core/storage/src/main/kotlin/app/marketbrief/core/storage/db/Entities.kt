package app.marketbrief.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.marketbrief.core.model.Article

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    val sourceId: String,
    val sourceName: String,
    val publishedAt: Long,
    val categories: String, // Comma-delimited
    val relatedSymbols: String, // Comma-delimited
    val region: String?,
    val sentiment: Float?,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Article = Article(
        id = id,
        title = title,
        summary = summary,
        url = url,
        imageUrl = imageUrl,
        sourceId = sourceId,
        sourceName = sourceName,
        publishedAt = publishedAt,
        categories = if (categories.isBlank()) emptyList() else categories.split(",").map { it.trim() },
        relatedSymbols = if (relatedSymbols.isBlank()) emptyList() else relatedSymbols.split(",").map { it.trim() },
        region = region,
        sentiment = sentiment
    )

    companion object {
        fun fromDomain(article: Article, fetchedAt: Long = System.currentTimeMillis()): ArticleEntity = ArticleEntity(
            id = article.id,
            title = article.title,
            summary = article.summary,
            url = article.url,
            imageUrl = article.imageUrl,
            sourceId = article.sourceId,
            sourceName = article.sourceName,
            publishedAt = article.publishedAt,
            categories = article.categories.joinToString(","),
            relatedSymbols = article.relatedSymbols.joinToString(","),
            region = article.region,
            sentiment = article.sentiment,
            fetchedAt = fetchedAt
        )
    }
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val articleId: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val articleId: String,
    val bodyText: String,
    val isFullText: Boolean = true,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "article_body")
data class ArticleBodyEntity(
    @PrimaryKey val articleId: String,
    val markdown: String,
    val kind: String = "FULL",
    val wordCount: Int = 0,
    val extractedAt: Long = System.currentTimeMillis(),
    val failureReason: String? = null
) {
    fun toDomain(): app.marketbrief.core.model.ArticleBody = app.marketbrief.core.model.ArticleBody(
        articleId = articleId,
        markdown = markdown,
        kind = kind,
        wordCount = wordCount,
        extractedAt = extractedAt,
        failureReason = failureReason
    )

    companion object {
        fun fromDomain(body: app.marketbrief.core.model.ArticleBody): ArticleBodyEntity = ArticleBodyEntity(
            articleId = body.articleId,
            markdown = body.markdown,
            kind = body.kind,
            wordCount = body.wordCount,
            extractedAt = body.extractedAt,
            failureReason = body.failureReason
        )
    }
}
