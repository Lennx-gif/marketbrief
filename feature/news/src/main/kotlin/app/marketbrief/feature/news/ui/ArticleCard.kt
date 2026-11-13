package app.marketbrief.feature.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marketbrief.core.model.Article
import app.marketbrief.core.model.Quote
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.theme.MarketBriefTheme
import coil.compose.AsyncImage

@Composable
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onDownload: () -> Unit,
    onSizeTrade: (String) -> Unit,
    onShare: () -> Unit,
    relatedQuote: Quote? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161B1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            article.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Category (teal), relative time, region and sentiment tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = article.categories.firstOrNull() ?: "News",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5CE1E6)
                )
                Text(
                    text = formatRelativeTime(article.publishedAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                article.region?.let { region ->
                    Surface(shape = RoundedCornerShape(4.dp), color = MarketBriefTheme.colors.subtlePill) {
                        Text(
                            text = region,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                article.sentiment?.let { score ->
                    val color = when {
                        score >= 0.15f -> MarketBriefTheme.colors.bullish
                        score <= -0.15f -> MarketBriefTheme.colors.bearish
                        else -> MarketBriefTheme.colors.neutral
                    }
                    val label = when {
                        score >= 0.15f -> "Bullish"
                        score <= -0.15f -> "Bearish"
                        else -> "Neutral"
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = article.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (article.summary.isNotBlank()) {
                Text(
                    text = article.summary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            relatedQuote?.let { quote ->
                RelatedSymbolChip(symbol = quote.assetId.symbol, quote = quote)
            }

            // Source row with actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourceAvatar(name = article.sourceName, size = 24)
                    Text(
                        text = "${article.sourceName} · ${formatRelativeTime(article.publishedAt)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val symbol = article.relatedSymbols.firstOrNull() ?: "EURUSD"
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { onSizeTrade(symbol) },
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFF1F4E52)
                    ) {
                        Text(
                            text = "Size $symbol",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5CE1E6),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBookmarkToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFF5CE1E6) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedSymbolChip(symbol: String, quote: Quote, modifier: Modifier = Modifier) {
    val change = quote.changePercent
    val isUp = change != null && change >= 0.0
    val stale = System.currentTimeMillis() - quote.timestamp > 5 * 60_000L
    val changeColor = when {
        change == null || stale -> MaterialTheme.colorScheme.onSurfaceVariant
        isUp -> MarketBriefTheme.colors.bullish
        else -> MarketBriefTheme.colors.bearish
    }
    val price = java.math.BigDecimal.valueOf(quote.mid).stripTrailingZeros().toPlainString()
    val changeText = change?.let {
        val arrow = if (isUp) "↑" else "↓"
        "$arrow ${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(it))}%"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(symbol, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(price, fontSize = 11.sp, color = if (stale) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            if (changeText != null) {
                Text(changeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = changeColor)
            }
            if (stale) Text("stale", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days}d ago"
    }
}