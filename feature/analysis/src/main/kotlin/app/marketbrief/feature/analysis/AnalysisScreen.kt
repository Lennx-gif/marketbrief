package app.marketbrief.feature.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.marketbrief.core.analysis.MarketSnapshot
import app.marketbrief.core.model.Sentiment
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.core.ui.components.AppleSegmentedControl
import app.marketbrief.core.ui.theme.MarketBriefTheme
import app.marketbrief.feature.analysis.ui.AnalysisMode
import app.marketbrief.feature.analysis.ui.AnalysisViewModel
import app.marketbrief.feature.analysis.ui.SentimentTimeWindow
import java.math.RoundingMode

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cheap when the cache is fresh; picks up a Myfxbook connection made in Settings.
    LaunchedEffect(Unit) { viewModel.refreshCrowd() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Title & Mode Segment Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Analysis",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                AppleSegmentedControl(
                    items = listOf(AnalysisMode.SENTIMENT, AnalysisMode.TECHNICALS),
                    selectedItem = uiState.mode,
                    onItemSelected = { viewModel.setMode(it) },
                    labelProvider = { if (it == AnalysisMode.SENTIMENT) "Feed Sentiment" else "Technicals" },
                    modifier = Modifier.width(260.dp)
                )
            }

            if (uiState.mode == AnalysisMode.SENTIMENT) {
                SentimentSection(
                    uiState = uiState,
                    onTimeWindowSelected = { viewModel.setTimeWindow(it) }
                )
            } else {
                TechnicalsSection(
                    uiState = uiState,
                    onAssetSelected = { viewModel.onAssetSelected(it) },
                    onTimeframeSelected = { viewModel.onTimeframeSelected(it) }
                )
            }
        }
    }
}

@Composable
fun SentimentSection(
    uiState: app.marketbrief.feature.analysis.ui.AnalysisUiState,
    onTimeWindowSelected: (SentimentTimeWindow) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time window pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AppleSegmentedControl(
                    items = listOf(SentimentTimeWindow.H24, SentimentTimeWindow.D7, SentimentTimeWindow.D30),
                    selectedItem = uiState.timeWindow,
                    onItemSelected = onTimeWindowSelected,
                    labelProvider = { it.label }
                )
            }
        }

        // Major Assets / Pairs Sentiment Cards
        item {
            Text(
                text = "ASSET SENTIMENT HEATMAP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            val displayAssets = listOf(
                "EURUSD" to "Euro / US Dollar",
                "GBPUSD" to "British Pound / US Dollar",
                "USDJPY" to "US Dollar / Japanese Yen",
                "XAU" to "Spot Gold (XAUUSD)",
                "BTC" to "Bitcoin (BTCUSD)",
                "USD" to "US Dollar Index",
                "EUR" to "Euro Index",
                "KES" to "Kenya Shilling (KES)"
            )

            AppleGroupedSection {
                displayAssets.forEachIndexed { index, (symbol, name) ->
                    val sentiment = uiState.sentiments[symbol] ?: Sentiment(0f, "Neutral", 0)
                    SentimentRow(symbol = symbol, name = name, sentiment = sentiment)
                    if (index < displayAssets.lastIndex) {
                        AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }

        // Crowd Positioning (Myfxbook Community Outlook)
        item { CrowdPositioningSection(uiState = uiState) }

        // Category Headline Volume Breakdown
        if (uiState.showVolume && uiState.categoryVolumes.isNotEmpty()) {
            item {
                Text(
                    text = "HEADLINE VOLUME BY CATEGORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                AppleGroupedSection {
                    uiState.categoryVolumes.entries.toList().forEachIndexed { index, (cat, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = cat, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "$count articles",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (index < uiState.categoryVolumes.size - 1) {
                            AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        }

        // Disclaimer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sentiment scores are on-device NLP estimations computed from recent financial news. Scores range from -1.0 (strongly bearish) to +1.0 (strongly bullish).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SentimentRow(
    symbol: String,
    name: String,
    sentiment: Sentiment
) {
    val score = sentiment.score
    val isBullish = score >= 0.15f
    val isBearish = score <= -0.15f
    val accentColor = when {
        isBullish -> MarketBriefTheme.colors.bullish
        isBearish -> MarketBriefTheme.colors.bearish
        else -> MarketBriefTheme.colors.neutral
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = symbol,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val icon = when {
                    isBullish -> Icons.Default.TrendingUp
                    isBearish -> Icons.Default.TrendingDown
                    else -> Icons.Default.TrendingFlat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )

                val signedScore = if (score > 0) "+%.2f".format(score) else "%.2f".format(score)
                Text(
                    text = "$signedScore ${sentiment.label}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            // Visual Sentiment Bar
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MarketBriefTheme.colors.subtlePill)
            ) {
                val progress = ((score + 1f) / 2f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .background(accentColor)
                )
            }

            Text(
                text = "${sentiment.headlineCount} headlines",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TechnicalsSection(
    uiState: app.marketbrief.feature.analysis.ui.AnalysisUiState,
    onAssetSelected: (String) -> Unit,
    onTimeframeSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        uiState.technicalError?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        item {
            AssetSelector(
                selectedAsset = uiState.selectedAsset,
                onAssetSelected = onAssetSelected,
                assets = uiState.availableAssets
            )
        }

        item {
            TimeframeSelector(
                selectedTimeframe = uiState.selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected
            )
        }

        uiState.snapshot?.let { snapshot ->
            item {
                MarketSnapshotCard(snapshot = snapshot)
            }

            item {
                IndicatorsSection(snapshot = snapshot)
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetSelector(
    selectedAsset: String,
    onAssetSelected: (String) -> Unit,
    assets: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedAsset,
            onValueChange = {},
            label = { Text("Instrument") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MarketBriefTheme.colors.cardSurface,
                unfocusedContainerColor = MarketBriefTheme.colors.cardSurface
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            assets.forEach { asset ->
                DropdownMenuItem(
                    text = { Text(asset) },
                    onClick = {
                        onAssetSelected(asset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TimeframeSelector(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit
) {
    val timeframes = listOf("M1", "M5", "M15", "H1", "H4", "D1", "W1")

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(timeframes) { tf ->
            ApplePillChip(
                text = tf,
                isSelected = tf == selectedTimeframe,
                onClick = { onTimeframeSelected(tf) }
            )
        }
    }
}

@Composable
fun MarketSnapshotCard(snapshot: MarketSnapshot) {
    AppleGroupedSection {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Market Snapshot: ${snapshot.assetId}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SnapshotItem("Price", snapshot.currentPrice.toPlainString())
                SnapshotItem("Trend", snapshot.trend.name)
                SnapshotItem("Volatility", snapshot.volatility.name)
            }

            AppleHairlineDivider()

            snapshot.rsi14?.let { rsi ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SnapshotItem("RSI(14)", rsi.setScale(2, RoundingMode.HALF_UP).toPlainString())
                    SnapshotItem("ATR(14)", snapshot.atr14?.setScale(4, RoundingMode.HALF_UP)?.toPlainString() ?: "N/A")
                    SnapshotItem("ATR %", snapshot.atrPercent?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: "N/A")
                }
            }
        }
    }
}

@Composable
fun SnapshotItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun IndicatorsSection(snapshot: MarketSnapshot) {
    AppleGroupedSection {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Technical Indicators", fontSize = 16.sp, fontWeight = FontWeight.Bold)

            snapshot.bollingerBands?.let { bb ->
                IndicatorRow(
                    "Bollinger Bands",
                    "U: ${bb.first.setScale(4, RoundingMode.HALF_UP)} | M: ${bb.second.setScale(4, RoundingMode.HALF_UP)} | L: ${bb.third.setScale(4, RoundingMode.HALF_UP)}"
                )
            }

            snapshot.stochastic?.let { stoch ->
                IndicatorRow(
                    "Stochastic",
                    "%K: ${stoch.first.setScale(2, RoundingMode.HALF_UP)} | %D: ${stoch.second.setScale(2, RoundingMode.HALF_UP)}"
                )
            }

            snapshot.pivotPoints?.let { pivots ->
                IndicatorRow(
                    "Pivot Points",
                    pivots.entries.take(4).joinToString(", ") { "${it.key}: ${it.value.setScale(4, RoundingMode.HALF_UP)}" }
                )
            }
        }
    }
}

@Composable
fun IndicatorRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val crowdMajors = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "AUDUSD", "USDCAD")

/** Crowd long/short share from Myfxbook traders. It describes what other retail traders hold, not where price will go. */
@Composable
fun CrowdPositioningSection(uiState: app.marketbrief.feature.analysis.ui.AnalysisUiState) {
    Column {
        Text(
            text = "CROWD POSITIONING (MYFXBOOK)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        val shown = uiState.crowd
            .filter { it.symbol in crowdMajors }
            .sortedBy { crowdMajors.indexOf(it.symbol) }
            .ifEmpty { uiState.crowd.take(6) }

        when {
            !uiState.crowdConnected -> CrowdNote("Connect your Myfxbook account in Settings (gear icon on the Feed) to see crowd long/short positioning.")
            shown.isEmpty() -> CrowdNote(uiState.crowdError ?: "No positioning data yet.")
            else -> {
                AppleGroupedSection {
                    shown.forEachIndexed { index, p ->
                        val longPct = p.longPercent
                        val shortPct = p.shortPercent
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = p.symbol, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${longPct.toInt()}% Long · ${shortPct.toInt()}% Short",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (longPct >= 50) MarketBriefTheme.colors.bullish else MarketBriefTheme.colors.bearish
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(longPct.toFloat().coerceAtLeast(0.1f))
                                        .fillMaxHeight()
                                        .background(MarketBriefTheme.colors.bullish)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(shortPct.toFloat().coerceAtLeast(0.1f))
                                        .fillMaxHeight()
                                        .background(MarketBriefTheme.colors.bearish)
                                )
                            }
                        }
                        if (index < shown.lastIndex) {
                            AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
                uiState.crowdError?.let { CrowdNote("Showing the last data. $it") }
            }
        }
    }
}

@Composable
private fun CrowdNote(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}
