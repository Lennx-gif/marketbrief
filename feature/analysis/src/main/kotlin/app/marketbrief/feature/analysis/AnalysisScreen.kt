package app.marketbrief.feature.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.analysis.MarketSnapshot
import app.marketbrief.feature.analysis.ui.AnalysisViewModel
import java.math.RoundingMode

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AssetSelector(
            selectedAsset = uiState.selectedAsset,
            onAssetSelected = { viewModel.onAssetSelected(it) },
            assets = uiState.availableAssets
        )

        TimeframeSelector(
            selectedTimeframe = uiState.selectedTimeframe,
            onTimeframeSelected = { viewModel.onTimeframeSelected(it) }
        )

        uiState.snapshot?.let { snapshot ->
            MarketSnapshotCard(snapshot = snapshot)
        }

        ChartPlaceholder()
        IndicatorsSection(snapshot = uiState.snapshot)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AssetSelector(
    selectedAsset: String,
    onAssetSelected: (String) -> Unit,
    assets: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TextField(
            value = selectedAsset,
            onValueChange = {},
            label = { Text("Asset") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
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
    val timeframes = listOf("M1", "M5", "M15", "M30", "H1", "H4", "D1", "W1")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        timeframes.forEach { tf ->
            FilterChip(
                selected = tf == selectedTimeframe,
                onClick = { onTimeframeSelected(tf) },
                label = { Text(tf, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
fun MarketSnapshotCard(snapshot: MarketSnapshot) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Market Snapshot: ${snapshot.assetId}",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SnapshotItem("Price", snapshot.currentPrice.toPlainString())
                SnapshotItem("Trend", snapshot.trend.name)
                SnapshotItem("Volatility", snapshot.volatility.name)
            }

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

            snapshot.macd?.let { macd ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SnapshotItem("MACD", macd.first.setScale(4, RoundingMode.HALF_UP).toPlainString())
                    SnapshotItem("Signal", macd.second.setScale(4, RoundingMode.HALF_UP).toPlainString())
                    SnapshotItem("Histogram", macd.third.setScale(4, RoundingMode.HALF_UP).toPlainString())
                }
            }
        }
    }
}

@Composable
fun SnapshotItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ChartPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chart View (TODO: Integrate chart library)",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun IndicatorsSection(snapshot: MarketSnapshot?) {
    snapshot?.let { snap ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Indicators", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

                snap.bollingerBands?.let { bb ->
                    IndicatorRow(
                        "Bollinger Bands",
                        "Upper: ${bb.first.setScale(4, RoundingMode.HALF_UP)} | Mid: ${bb.second.setScale(4, RoundingMode.HALF_UP)} | Lower: ${bb.third.setScale(4, RoundingMode.HALF_UP)}"
                    )
                }

                snap.stochastic?.let { stoch ->
                    IndicatorRow(
                        "Stochastic",
                        "%K: ${stoch.first.setScale(2, RoundingMode.HALF_UP)} | %D: ${stoch.second.setScale(2, RoundingMode.HALF_UP)}"
                    )
                }

                snap.pivotPoints?.let { pivots ->
                    IndicatorRow(
                        "Pivot Points",
                        pivots.entries.joinToString(", ") { "${it.key}: ${it.value.setScale(4, RoundingMode.HALF_UP)}" }
                    )
                }
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
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}