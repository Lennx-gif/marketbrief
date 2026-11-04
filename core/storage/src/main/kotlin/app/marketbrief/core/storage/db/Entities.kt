package app.marketbrief.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Fts4
import androidx.room.Index
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

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val instrumentSymbol: String,
    val accountCurrency: String,
    val balanceText: String,
    val riskIsPercent: Boolean,
    val riskValueText: String,
    val leverageText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trade_log")
data class TradeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val direction: String,
    val entryText: String,
    val stopText: String,
    val stopIsPips: Boolean,
    val takeProfitText: String,
    val lots: String,
    val riskAmount: String,
    val accountCurrency: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val symbol: String,
    val assetClass: String,
    val direction: String,
    val entry: String,
    val stop: String,
    val stopIsPips: Boolean,
    val takeProfit: String?,
    val lots: String,
    val riskAmount: String,
    val riskPercent: String,
    val accountCurrency: String,
    val status: String,
    val openedAt: Long?,
    val closedAt: Long?,
    val exitPrice: String?,
    val pnl: String?,
    val rMultiple: String?,
    val notes: String,
    val tags: String
)

@Entity(tableName = "journal_headlines", primaryKeys = ["entryId", "articleId"])
data class JournalHeadlineEntity(
    val entryId: String,
    val articleId: String,
    val title: String,
    val url: String
)

@Entity(tableName = "account_settings")
data class AccountSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val startingBalance: String,
    val currency: String,
    val maxRiskPerTradePercent: String,
    val dailyLossLimitPercent: String,
    val maxOpenRiskPercent: String,
    val maxDrawdownPercent: String,
    val drawdownMode: String,
    val resetTimezone: String
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val currency: String,
    val instant: Long,
    val impact: String,
    val source: String
)

@Entity(tableName = "price_alerts", indices = [Index(value = ["symbol"])])
data class PriceAlertEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val assetClass: String,
    val targetPrice: String,
    val direction: String,
    val createdAt: Long,
    val triggeredAt: Long?,
    val lastCheckedPrice: String?
)

@Entity(tableName = "article_tags", primaryKeys = ["articleId", "tag"])
data class ArticleTagEntity(val articleId: String, val tag: String)

@Entity(tableName = "source_preferences")
data class SourcePrefEntity(@PrimaryKey val sourceName: String, val weight: String)

@Entity(tableName = "rate_cache")
data class RateCacheEntity(@PrimaryKey val currencyPair: String, val rate: String, val updatedAt: Long)

@Fts4
@Entity(tableName = "article_fts")
data class ArticleFtsEntity(val articleId: String, val title: String, val summary: String)

@Fts4
@Entity(tableName = "article_body_fts")
data class ArticleBodyFtsEntity(val articleId: String, val markdown: String)
