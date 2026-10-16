package app.marketbrief.feature.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.marketbrief.core.risk.Direction
import app.marketbrief.core.risk.Instrument

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InstrumentPicker(
    selectedInstrument: Instrument,
    onInstrumentSelected: (String) -> Unit,
    instruments: List<Instrument>
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
            value = selectedInstrument.symbol,
            onValueChange = {},
            label = { Text("Instrument") },
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
            instruments.forEach { instrument ->
                DropdownMenuItem(
                    text = { Text(instrument.symbol) },
                    onClick = {
                        onInstrumentSelected(instrument.symbol)
                        expanded = false
                    }
                )
            }
        }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = uiState.balanceText,
                    onValueChange = onBalanceChange,
                    label = { Text("Balance") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                TextField(
                    value = uiState.accountCurrencyText,
                    onValueChange = onAccountCurrencyChange,
                    label = { Text("Account Currency") },
                    modifier = Modifier.width(120.dp),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.riskIsPercent,
                    onClick = { onRiskTypeChange(true) },
                    label = { Text("Risk %") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !uiState.riskIsPercent,
                    onClick = { onRiskTypeChange(false) },
                    label = { Text("Fixed Amount") },
                    modifier = Modifier.weight(1f)
                )
            }

            TextField(
                value = uiState.riskValueText,
                onValueChange = onRiskValueChange,
                label = { Text(if (uiState.riskIsPercent) "Risk %" else "Risk Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.direction == Direction.LONG,
                    onClick = { onDirectionChange(Direction.LONG) },
                    label = { Text("Long") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.direction == Direction.SHORT,
                    onClick = { onDirectionChange(Direction.SHORT) },
                    label = { Text("Short") },
                    modifier = Modifier.weight(1f)
                )
            }

            TextField(
                value = uiState.entryText,
                onValueChange = onEntryChange,
                label = { Text("Entry Price") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = uiState.stopText,
                    onValueChange = onStopChange,
                    label = { Text(if (uiState.stopIsPips) "Stop Loss (pips)" else "Stop Loss (price)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                FilterChip(
                    selected = uiState.stopIsPips,
                    onClick = { onStopTypeChange(true) },
                    label = { Text("Pips") }
                )
                FilterChip(
                    selected = !uiState.stopIsPips,
                    onClick = { onStopTypeChange(false) },
                    label = { Text("Price") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = uiState.takeProfitText,
                    onValueChange = onTakeProfitChange,
                    label = { Text(if (uiState.takeProfitIsPips) "Take Profit (pips)" else "Take Profit (price)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                FilterChip(
                    selected = uiState.takeProfitIsPips,
                    onClick = { onTakeProfitTypeChange(true) },
                    label = { Text("Pips") }
                )
                FilterChip(
                    selected = !uiState.takeProfitIsPips,
                    onClick = { onTakeProfitTypeChange(false) },
                    label = { Text("Price") }
                )
            }

            TextField(
                value = uiState.leverageText,
                onValueChange = onLeverageChange,
                label = { Text("Leverage (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            TextField(
                value = uiState.rateOverrideText,
                onValueChange = onRateOverrideChange,
                label = { Text("Rate Override (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}