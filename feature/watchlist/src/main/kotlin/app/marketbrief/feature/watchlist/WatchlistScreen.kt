package app.marketbrief.feature.watchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.risk.InstrumentCatalog
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.core.ui.theme.MarketBriefTheme
import app.marketbrief.feature.watchlist.ui.EconomicEvent
import app.marketbrief.feature.watchlist.ui.WatchlistItem
import app.marketbrief.feature.watchlist.ui.WatchlistViewModel

@Composable
fun WatchlistScreen(
    onNavigateToCalculator: (String) -> Unit = {},
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Apple Minimalist Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Watchlist",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.watchlistItems.size} Watched Quotes · Live Market Rates",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Instrument",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Watchlist Items Section
            if (uiState.watchlistItems.isEmpty()) {
                EmptyWatchlist(onAddItem = { showAddDialog = true })
            } else {
                Text(
                    text = "TRACKED INSTRUMENTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                AppleGroupedSection {
                    uiState.watchlistItems.forEachIndexed { index, item ->
                        WatchlistItemRow(
                            item = item,
                            onRemove = { viewModel.removeItem(item.symbol) },
                            onSizeTrade = { onNavigateToCalculator(item.symbol) }
                        )
                        if (index < uiState.watchlistItems.lastIndex) {
                            AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }

            // Economic Calendar Section
            EconomicCalendarSection(events = uiState.economicEvents)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        AddInstrumentDialog(
            currentSymbols = uiState.watchlistItems.map { it.symbol }.toSet(),
            onToggleSymbol = { viewModel.toggleItem(it) },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun WatchlistItemRow(
    item: WatchlistItem,
    onRemove: () -> Unit,
    onSizeTrade: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.symbol,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MarketBriefTheme.colors.subtlePill
                ) {
                    Text(
                        text = item.assetClass.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = item.name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Price & Change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.currentPrice?.toPlainString() ?: "—",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val changeText = if (item.changePercent >= 0) "+${item.changePercent}%" else "${item.changePercent}%"
                val changeColor = if (item.changePercent >= 0) MarketBriefTheme.colors.bullish else MarketBriefTheme.colors.bearish
                Text(
                    text = changeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor
                )
            }

            // Quick Calculator Shortcut
            IconButton(
                onClick = onSizeTrade,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Size in Calculator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyWatchlist(onAddItem: () -> Unit) {
    AppleGroupedSection {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Watchlist is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Add currency pairs, stocks, crypto or commodities to monitor quotes and sizing in real time.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onAddItem,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add Instruments", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AddInstrumentDialog(
    currentSymbols: Set<String>,
    onToggleSymbol: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allCatalog = remember { InstrumentCatalog.default().all() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Customize Watchlist",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Filter symbols...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                val filtered = allCatalog.filter {
                    searchQuery.isBlank() || it.symbol.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(filtered) { inst ->
                        val isTracked = inst.symbol in currentSymbols
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSymbol(inst.symbol) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = inst.symbol,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MarketBriefTheme.colors.subtlePill
                                ) {
                                    Text(
                                        text = inst.assetClass.name,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (isTracked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Tracked",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        AppleHairlineDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun EconomicCalendarSection(events: List<EconomicEvent>) {
    if (events.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ECONOMIC CALENDAR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            AppleGroupedSection {
                events.forEachIndexed { index, event ->
                    EconomicEventRow(event = event)
                    if (index < events.lastIndex) {
                        AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EconomicEventRow(event: EconomicEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = event.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${event.currency} · ${event.time}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        val impactColor = when (event.impact.lowercase()) {
            "high" -> MarketBriefTheme.colors.bearish
            "medium" -> MaterialTheme.colorScheme.tertiary
            else -> MarketBriefTheme.colors.neutral
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = impactColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = "${event.impact} Impact",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = impactColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}