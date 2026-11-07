package app.marketbrief.feature.settings

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class DebugInfoInput(
    val versionName: String,
    val versionCode: Int,
    val flavor: String,
    val newsRequestsRemaining: Int,
    val twelveDataCreditsRemaining: Int,
    val myfxbookRequestsRemaining: Int,
    val lastMarketRefreshEpochMillis: Long,
    val cachedArticleCount: Int,
    val twelveDataKeyConfigured: Boolean,
    val myfxbookConnected: Boolean
)

object DebugInfoBuilder {
    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    fun formatVersion(versionName: String, versionCode: Int, flavor: String): String =
        "$versionName ($versionCode) · $flavor"

    fun build(input: DebugInfoInput): String = buildString {
        appendLine("MarketBrief ${formatVersion(input.versionName, input.versionCode, input.flavor)}")
        appendLine("NewsAPI requests remaining: ${input.newsRequestsRemaining}")
        appendLine("Twelve Data credits remaining: ${input.twelveDataCreditsRemaining}")
        appendLine("Myfxbook requests remaining: ${input.myfxbookRequestsRemaining}")
        appendLine(
            "Last market refresh: ${
                input.lastMarketRefreshEpochMillis.takeIf { it > 0L }?.let {
                    timestampFormatter.format(Instant.ofEpochMilli(it))
                } ?: "never"
            }"
        )
        appendLine("Articles saved on this device: ${input.cachedArticleCount}")
        appendLine("Twelve Data key configured: ${input.twelveDataKeyConfigured}")
        append("Myfxbook connected: ${input.myfxbookConnected}")
    }
}
