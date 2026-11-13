package app.marketbrief.feature.news.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun MajorEventsTicker(
    events: List<MajorEvent> = MajorEvent.defaultEvents(),
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Major Events",
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Live",
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }

        // Swipeable news carousel with stable card dimensions
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(events, key = { it.id }) { event ->
                MajorEventCard(event = event)
            }
        }
    }
}

@Composable
private fun MajorEventCard(event: MajorEvent) {
    val iconColor = event.iconColor ?: MaterialTheme.colorScheme.primary
    val isPositive = event.impact > 0
    val hasImage = !event.imageUrl.isNullOrBlank()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161B1E),
        modifier = Modifier
            .width(280.dp)
            .height(148.dp)
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = if (isPositive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // News-related background image with gradient scrim
            if (hasImage) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with category and impact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = event.icon,
                            contentDescription = null,
                            tint = if (hasImage) Color.White else iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = event.category,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (hasImage) Color.White else iconColor,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(
                                    color = if (hasImage) Color.White.copy(alpha = 0.2f) else iconColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer { rotationZ = if (isPositive) 0f else 180f }
                        )
                        Text(
                            text = "${if (isPositive) "+" else ""}${event.impact}%",
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Event title
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                // Affected symbols
                if (event.symbols.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        event.symbols.take(3).forEach { symbol ->
                            Text(
                                text = symbol,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = if (hasImage) Color(0xFF80D8FF) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .background(
                                        color = if (hasImage) Color(0x3300E5FF) else MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                        if (event.symbols.size > 3) {
                            Text(
                                text = "+${event.symbols.size - 3}",
                                fontSize = 11.sp,
                                color = if (hasImage) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class MajorEvent(
    val id: String,
    val category: String,
    val title: String,
    val impact: Float, // percentage
    val symbols: List<String>,
    val icon: ImageVector = Icons.Default.FlashOn,
    val iconColor: Color? = null,
    val imageUrl: String? = null
) {
    companion object {
        fun defaultEvents(): List<MajorEvent> {
            return listOf(
                MajorEvent(
                    id = "evt-1",
                    category = "Fed Decision",
                    title = "FOMC holds rates steady, signals one more hike",
                    impact = 0.8f,
                    symbols = listOf("EURUSD", "USDJPY", "US30", "SPX500"),
                    icon = Icons.Default.AccountBalance,
                    iconColor = Color(0xFF1E88E5),
                    imageUrl = "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?w=600&auto=format&fit=crop&q=80"
                ),
                MajorEvent(
                    id = "evt-2",
                    category = "Earnings",
                    title = "NVIDIA beats on AI demand, raises guidance",
                    impact = 3.2f,
                    symbols = listOf("NVDA", "SPX500", "SOX"),
                    icon = Icons.Default.TrendingUp,
                    iconColor = Color(0xFF43A047),
                    imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80"
                ),
                MajorEvent(
                    id = "evt-3",
                    category = "Crypto",
                    title = "Bitcoin ETF inflows hit record $2.1B weekly",
                    impact = 4.5f,
                    symbols = listOf("BTCUSD", "ETHUSD", "COIN"),
                    icon = Icons.Default.CurrencyBitcoin,
                    iconColor = Color(0xFFF9A825),
                    imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80"
                ),
                MajorEvent(
                    id = "evt-4",
                    category = "Commodities",
                    title = "Gold breaks $2,400 on central bank buying",
                    impact = 1.8f,
                    symbols = listOf("XAUUSD", "GLD", "GDX"),
                    icon = Icons.Default.Diamond,
                    iconColor = Color(0xFFFFC107),
                    imageUrl = "https://images.unsplash.com/photo-1610375461246-83df859d849d?w=600&auto=format&fit=crop&q=80"
                ),
                MajorEvent(
                    id = "evt-5",
                    category = "Geopolitics",
                    title = "Oil spikes on Middle East supply concerns",
                    impact = 2.3f,
                    symbols = listOf("USOIL", "UKOIL", "XLE"),
                    icon = Icons.Default.LocalGasStation,
                    iconColor = Color(0xFFE53935),
                    imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
                ),
                MajorEvent(
                    id = "evt-6",
                    category = "Forex",
                    title = "Yen weakens past 155 on BoJ policy divergence",
                    impact = -1.2f,
                    symbols = listOf("USDJPY", "EURJPY", "GBPJPY"),
                    icon = Icons.Default.AttachMoney,
                    iconColor = Color(0xFF8E24AA),
                    imageUrl = "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=600&auto=format&fit=crop&q=80"
                )
            )
        }
    }
}