package app.marketbrief.feature.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.feature.watchlist.ui.WatchlistViewModel

@Composable
fun WatchlistScreen(viewModel: WatchlistViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.watchlistItems.isEmpty()) {
            EmptyWatchlist(onAddItem = { viewModel.addItem("EURUSD") })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.watchlistItems.forEach { item ->
                    WatchlistItemCard(
                        item = item,
                        onRemove = { viewModel.removeItem(item.symbol) },
                        onSizeTrade = { }
                    )
                }
            }
        }

        EconomicCalendarSection(events = uiState.economicEvents)
    }
}

@Composable
fun EmptyWatchlist(onAddItem: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No watchlist items",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add instruments to track them here",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddItem) {
                Text("Add First Item")
            }
        }
    }
}

@Composable
fun WatchlistItemCard(
    item: app.marketbrief.feature.watchlist.ui.WatchlistItem,
    onRemove: (String) -> Unit,
    onSizeTrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = item.symbol,
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "${item.assetClass} • ${item.currentPrice?.toPlainString() ?: "N/A"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onSizeTrade() }) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Size Trade"
                    )
                }
                IconButton(onClick = { onRemove(item.symbol) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove"
                    )
                }
            }
        }
    }
}

@Composable
fun EconomicCalendarSection(events: List<app.marketbrief.feature.watchlist.ui.EconomicEvent>) {
    if (events.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Economic Calendar", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                events.forEach { event ->
                    EconomicEventRow(event = event)
                }
            }
        }
    }
}

@Composable
fun EconomicEventRow(event: app.marketbrief.feature.watchlist.ui.EconomicEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = event.title, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Text(text = "${event.currency} • ${event.time}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Surface(
            color = when (event.impact) {
                "High" -> MaterialTheme.colorScheme.errorContainer
                "Medium" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
            shape = RoundedCornerShape(50),
            tonalElevation = 1.dp
        ) {
            Text(
                text = event.impact,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}