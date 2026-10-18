package app.marketbrief.feature.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marketbrief.core.risk.AssetClass
import app.marketbrief.core.risk.Direction
import app.marketbrief.core.risk.Instrument
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.core.ui.components.AppleSegmentedControl
import app.marketbrief.core.ui.theme.MarketBriefTheme

@Composable
fun InstrumentPicker(
    selectedInstrument: Instrument,
    onInstrumentSelected: (String) -> Unit,
    instruments: List<Instrument>
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf<AssetClass?>(null) }

    AppleGroupedSection {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TRADABLE INSTRUMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = selectedInstrument.symbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = selectedInstrument.assetClass.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select Instrument",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Select Instrument",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search stocks, forex, crypto...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Asset Class Filter Pills
                    val assetClasses = listOf(null) + AssetClass.entries
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(assetClasses) { ac ->
                            ApplePillChip(
                                text = ac?.name ?: "All",
                                isSelected = selectedTab == ac,
                                onClick = { selectedTab = ac }
                            )
                        }
                    }

                    val filtered = instruments.filter { inst ->
                        (selectedTab == null || inst.assetClass == selectedTab) &&
                        (searchQuery.isBlank() || inst.symbol.contains(searchQuery, ignoreCase = true))
                    }

                    // Instrument list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filtered) { inst ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onInstrumentSelected(inst.symbol)
                                            showDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
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
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (inst.symbol == selectedInstrument.symbol) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                AppleHairlineDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CalculatorInputSection(
    uiState: CalculatorUiState,
    onBalanceChange: (String) -> Unit,
    onAccountCurrencyChange: (String) -> Unit,
    onRiskTypeChange: (Boolean) -> Unit,
    onRiskValueChange: (String) -> Unit,
    onDirectionChange: (Direction) -> Unit,
    onEntryChange: (String) -> Unit,
    onStopChange: (String) -> Unit,
    onStopTypeChange: (Boolean) -> Unit,
    onTakeProfitChange: (String) -> Unit,
    onTakeProfitTypeChange: (Boolean) -> Unit,
    onLeverageChange: (String) -> Unit,
    onRateOverrideChange: (String) -> Unit
) {
    val topCurrencies = listOf("USD", "EUR", "GBP", "KES", "JPY", "CAD", "AUD", "CHF")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Section 1: Account & Risk
        AppleGroupedSection {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ACCOUNT & RISK PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Currency quick select
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Account Currency: ${uiState.accountCurrencyText}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(topCurrencies) { ccy ->
                            ApplePillChip(
                                text = ccy,
                                isSelected = uiState.accountCurrencyText.equals(ccy, ignoreCase = true),
                                onClick = { onAccountCurrencyChange(ccy) }
                            )
                        }
                    }
                }

                AppleHairlineDivider()

                // Balance Input
                OutlinedTextField(
                    value = uiState.balanceText,
                    onValueChange = onBalanceChange,
                    label = { Text("Account Balance (${uiState.accountCurrencyText})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )

                // Direction: Long / Short
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Trade Direction",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppleSegmentedControl(
                        items = listOf(Direction.LONG, Direction.SHORT),
                        selectedItem = uiState.direction,
                        onItemSelected = onDirectionChange,
                        labelProvider = { if (it == Direction.LONG) "Long (Buy)" else "Short (Sell)" }
                    )
                }

                AppleHairlineDivider()

                // Risk Model
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Risk Sizing Mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppleSegmentedControl(
                        items = listOf(true, false),
                        selectedItem = uiState.riskIsPercent,
                        onItemSelected = onRiskTypeChange,
                        labelProvider = { if (it) "% of Balance" else "Cash Amount" }
                    )
                }

                OutlinedTextField(
                    value = uiState.riskValueText,
                    onValueChange = onRiskValueChange,
                    label = { Text(if (uiState.riskIsPercent) "Risk % (e.g. 1.0)" else "Cash at Risk (${uiState.accountCurrencyText})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Section 2: Order Pricing & Levels
        AppleGroupedSection {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ORDER PARAMETERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Entry Price
                OutlinedTextField(
                    value = uiState.entryText,
                    onValueChange = onEntryChange,
                    label = { Text("Entry Price") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )

                AppleHairlineDivider()

                // Stop Loss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stop Loss",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppleSegmentedControl(
                        items = listOf(false, true),
                        selectedItem = uiState.stopIsPips,
                        onItemSelected = onStopTypeChange,
                        labelProvider = { if (it) "Pips/Points" else "Exact Price" }
                    )
                }

                OutlinedTextField(
                    value = uiState.stopText,
                    onValueChange = onStopChange,
                    label = { Text(if (uiState.stopIsPips) "Stop Distance in Pips" else "Stop Loss Price") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )

                AppleHairlineDivider()

                // Take Profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Take Profit (Optional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppleSegmentedControl(
                        items = listOf(false, true),
                        selectedItem = uiState.takeProfitIsPips,
                        onItemSelected = onTakeProfitTypeChange,
                        labelProvider = { if (it) "Pips" else "Price" }
                    )
                }

                OutlinedTextField(
                    value = uiState.takeProfitText,
                    onValueChange = onTakeProfitChange,
                    label = { Text(if (uiState.takeProfitIsPips) "TP Distance in Pips" else "Take Profit Price") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}