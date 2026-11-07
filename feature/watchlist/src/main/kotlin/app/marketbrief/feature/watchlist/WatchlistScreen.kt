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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.marketbrief.core.model.AssetClass
import app.marketbrief.core.risk.InstrumentCatalog
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.core.ui.theme.MarketBriefTheme
import app.marketbrief.feature.watchlist.ui.EconomicEvent
import app.marketbrief.feature.watchlist.ui.WatchlistItem
import app.marketbrief.feature.watchlist.ui.WatchlistViewModel
import kotlinx.coroutines.delay

@Composable
fun WatchlistScreen(
    onNavigateToCalculator: (String) -> Unit = {},
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var alertSymbol by rememberSaveable { mutableStateOf<String?>(null) }
    var alertTarget by rememberSaveable { mutableStateOf("") }
    var showCalendarDialog by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
            while (true) {
                delay(WatchlistViewModel.REFRESH_INTERVAL_MILLIS)
                viewModel.refresh()
            }
        }
    }

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
                        text = when {
                            uiState.lastQuoteUpdatedAt > 0L -> "Prices as of ${formatWatchlistTime(uiState.lastQuoteUpdatedAt)}"
                            uiState.watchlistItems.isNotEmpty() -> "Prices unavailable"
                            else -> "No instruments tracked"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.refresh(manual = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh prices",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Instrument",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            LiveTickerStrip(items = uiState.watchlistItems.filter { it.currentPrice != null })

            WatchlistControls(
                sortOrder = uiState.sortOrder,
                viewMode = uiState.viewMode,
                onSort = viewModel::setSortOrder,
                onViewMode = viewModel::setViewMode
            )

            uiState.refreshMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            // Watchlist Items Section
            if (uiState.watchlistItems.isEmpty()) {
                EmptyWatchlist(onAddItem = { showAddDialog = true })
            } else {
                SectionToggleRow(
                    title = "TRACKED INSTRUMENTS",
                    expanded = uiState.watchlistExpanded,
                    onToggle = { viewModel.setWatchlistExpanded(!uiState.watchlistExpanded) }
                )
                if (uiState.watchlistExpanded) {
                    AppleGroupedSection {
                        uiState.watchlistItems.forEachIndexed { index, item ->
                            WatchlistItemRow(
                                item = item,
                                compact = uiState.viewMode == "COMPACT",
                                onRemove = { viewModel.removeItem(item.symbol) },
                                onSizeTrade = { onNavigateToCalculator(item.symbol) },
                                onAddAlert = { alertTarget = ""; alertSymbol = item.symbol }
                            )
                            if (index < uiState.watchlistItems.lastIndex) {
                                AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }

            // Economic Calendar Section
            EconomicCalendarSection(
                events = uiState.economicEvents,
                impactFilter = uiState.calendarImpactFilter,
                expanded = uiState.calendarExpanded,
                onFilter = viewModel::setCalendarImpactFilter,
                onToggleExpanded = { viewModel.setCalendarExpanded(!uiState.calendarExpanded) },
                onAdd = { showCalendarDialog = true }
            )
            uiState.calendarMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            uiState.alertMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (uiState.priceAlerts.isNotEmpty()) {
                SectionToggleRow(
                    title = "PRICE ALERTS",
                    expanded = uiState.alertsExpanded,
                    onToggle = { viewModel.setAlertsExpanded(!uiState.alertsExpanded) }
                )
                if (uiState.alertsExpanded) {
                    AppleGroupedSection {
                        uiState.priceAlerts.forEach { alert ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${alert.symbol} ${alert.direction.lowercase()} ${alert.targetPrice}")
                                TextButton(onClick = { viewModel.deletePriceAlert(alert.id) }) { Text("Remove") }
                            }
                        }
                    }
                }
            }

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

    alertSymbol?.let { symbol ->
        AlertDialog(
            onDismissRequest = { alertSymbol = null },
            title = { Text("Price alert · $symbol") },
            text = { OutlinedTextField(value = alertTarget, onValueChange = { alertTarget = it }, label = { Text("Target price") }) },
            confirmButton = {
                Row {
                    TextButton(onClick = { viewModel.addPriceAlert(symbol, alertTarget, app.marketbrief.core.data.AlertDirection.ABOVE); alertSymbol = null }) { Text("Above") }
                    TextButton(onClick = { viewModel.addPriceAlert(symbol, alertTarget, app.marketbrief.core.data.AlertDirection.BELOW); alertSymbol = null }) { Text("Below") }
                }
            },
            dismissButton = { TextButton(onClick = { alertSymbol = null }) { Text("Cancel") } }
        )
    }

    if (showCalendarDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var currency by rememberSaveable { mutableStateOf("USD") }
        var instant by rememberSaveable { mutableStateOf("") }
        var impact by rememberSaveable { mutableStateOf("HIGH") }
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text("Add calendar event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Event title") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(currency, { currency = it }, label = { Text("Currency") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(impact, { impact = it.uppercase() }, label = { Text("Impact") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(instant, { instant = it }, label = { Text("2026-11-04T08:00:00Z") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.addCalendarEvent(title, currency, instant, impact)) {
                        showCalendarDialog = false
                    }
                }) { Text("Add event") }
            },
            dismissButton = { TextButton(onClick = { showCalendarDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun WatchlistItemRow(
    item: WatchlistItem,
    compact: Boolean,
    onRemove: () -> Unit,
    onSizeTrade: () -> Unit,
    onAddAlert: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
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
            }
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.width(58.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MarketBriefTheme.colors.subtlePill
                ) {
                    Text(
                        text = item.assetClass.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
                if (!compact) {
                    Text(
                        text = item.name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Column(
                modifier = Modifier.width(76.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.currentPrice?.let(::formatPrice) ?: "No price yet",
                    fontSize = if (item.currentPrice == null) 10.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isStale) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                val change = item.changePercent
                val changeText = when {
                    change == null -> "Change n/a"
                    item.isBullish == true -> "↑ ${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(change))}%"
                    else -> "↓ ${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(change))}%"
                }
                val changeColor = when {
                    change == null || item.isStale -> MaterialTheme.colorScheme.onSurfaceVariant
                    item.isBullish == true -> MarketBriefTheme.colors.bullish
                    else -> MarketBriefTheme.colors.bearish
                }
                Text(changeText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = changeColor, maxLines = 1)
                if (item.isStale) {
                    Text("Stale", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = onSizeTrade,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Size in Calculator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onAddAlert, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = "Add price alert", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LiveTickerStrip(items: List<WatchlistItem>) {
    if (items.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.symbol }) { item ->
            Surface(shape = RoundedCornerShape(100.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.symbol, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(item.currentPrice?.let(::formatPrice) ?: "No price", fontSize = 11.sp)
                    val change = item.changePercent
                    if (change != null) {
                        val up = item.isBullish == true
                        Text(
                            "${if (up) "↑" else "↓"} ${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(change))}%",
                            fontSize = 11.sp,
                            color = when {
                                item.isStale -> MaterialTheme.colorScheme.onSurfaceVariant
                                up -> MarketBriefTheme.colors.bullish
                                else -> MarketBriefTheme.colors.bearish
                            }
                        )
                    }
                    if (item.isStale) Text("stale", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WatchlistControls(
    sortOrder: String,
    viewMode: String,
    onSort: (String) -> Unit,
    onViewMode: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("MANUAL", "SYMBOL", "CHANGE_DESC", "CHANGE_ASC")) { sort ->
                ApplePillChip(
                    text = when (sort) {
                        "SYMBOL" -> "Symbol"
                        "CHANGE_DESC" -> "Top movers"
                        "CHANGE_ASC" -> "Bottom movers"
                        else -> "Manual"
                    },
                    isSelected = sortOrder == sort,
                    onClick = { onSort(sort) }
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("DETAILED", "COMPACT")) { mode ->
                ApplePillChip(
                    text = if (mode == "DETAILED") "Detailed" else "Compact",
                    isSelected = viewMode == mode,
                    onClick = { onViewMode(mode) }
                )
            }
        }
    }
}

@Composable
private fun SectionToggleRow(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
        Text(if (expanded) "Hide" else "Show", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
    }
}

private fun formatPrice(price: java.math.BigDecimal): String =
    price.stripTrailingZeros().toPlainString()

private fun formatWatchlistTime(epochMillis: Long): String =
    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(epochMillis))

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
fun EconomicCalendarSection(
events: List<EconomicEvent>,
impactFilter: String,
expanded: Boolean,
onFilter: (String) -> Unit,
onToggleExpanded: () -> Unit,
onAdd: () -> Unit = {}
) {
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ECONOMIC CALENDAR",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggleExpanded) { Text(if (expanded) "Hide" else "Show") }
            TextButton(onClick = onAdd) { Text("Add event") }
        }
    }

    if (expanded) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("ALL", "HIGH", "MEDIUM", "LOW")) { filter ->
                ApplePillChip(
                    text = filter.lowercase().replaceFirstChar { it.uppercase() },
                    isSelected = impactFilter == filter,
                    onClick = { onFilter(filter) }
                )
            }
        }
        val filteredEvents = events.filter {
            impactFilter == "ALL" || it.impact.equals(impactFilter, ignoreCase = true)
        }
        val next = filteredEvents
            .filter { it.impact.equals("HIGH", true) && it.instant >= System.currentTimeMillis() }
            .minByOrNull { it.instant }
        next?.let { event ->
            val minutes = ((event.instant - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
            Text("Next high-impact event: ${event.title} · ${minutes / 60}h ${minutes % 60}m", color = MaterialTheme.colorScheme.primary)
        }

        AppleGroupedSection {
            if (events.isEmpty()) {
                Text(
                    "No events yet. Add one manually.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (filteredEvents.isEmpty()) {
                Text(
                    "No events match this impact filter.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filteredEvents.forEachIndexed { index, event ->
                    EconomicEventRow(event = event)
                    if (index < filteredEvents.lastIndex) {
                        AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                    }
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
