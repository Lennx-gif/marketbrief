package app.marketbrief.feature.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.storage.db.PresetEntity
import app.marketbrief.core.storage.db.TradeLogEntity
import app.marketbrief.core.ui.components.AppleBanner
import app.marketbrief.core.ui.components.AppleGroupedSection
import app.marketbrief.core.ui.components.AppleHairlineDivider
import app.marketbrief.core.ui.components.ApplePillChip
import app.marketbrief.feature.calculator.ui.CalculatorInputSection
import app.marketbrief.feature.calculator.ui.CalculatorResultCard
import app.marketbrief.feature.calculator.ui.CalculatorViewModel
import app.marketbrief.feature.calculator.ui.InstrumentPicker
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date

@Composable
fun CalculatorScreen(
    initialSymbol: String = "",
    onOpenJournal: () -> Unit = {},
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val tradeLog by viewModel.tradeLog.collectAsStateWithLifecycle()

    LaunchedEffect(initialSymbol) {
        if (initialSymbol.isNotBlank()) {
            viewModel.onInstrumentSelected(initialSymbol)
        }
    }

    if (uiState.showSavePresetDialog) {
        SavePresetDialog(
            name = uiState.presetNameText,
            onNameChange = { viewModel.onPresetNameChange(it) },
            onSave = { viewModel.savePreset() },
            onDismiss = { viewModel.dismissSavePreset() }
        )
    }

    if (uiState.showSaveJournalDialog) {
        SaveJournalSetupDialog(
            accounts = uiState.journalAccounts,
            setups = uiState.journalSetups,
            selectedAccountId = uiState.selectedJournalAccountId,
            selectedSetupId = uiState.selectedJournalSetupId,
            selectedChecklistItems = uiState.selectedChecklistItems,
            guardrailWarnings = uiState.guardrailWarnings,
            message = uiState.setupMessage,
            onAccountSelected = viewModel::selectJournalAccount,
            onSetupSelected = viewModel::selectJournalSetup,
            onChecklistItemChanged = viewModel::toggleChecklistItem,
            onSave = viewModel::saveSetup,
            onDismiss = viewModel::dismissSaveSetupDialog
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Position Calculator",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                ApplePillChip(
                    text = "Journal",
                    isSelected = true,
                    onClick = onOpenJournal
                )
            }

            Text(
                text = "Position Sizer",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (presets.isNotEmpty()) {
                PresetsSection(
                    presets = presets,
                    onApply = { viewModel.applyPreset(it) },
                    onDelete = { viewModel.deletePreset(it) },
                    onSave = { viewModel.onShowSavePreset() }
                )
            }

            InstrumentPicker(
                selectedInstrument = uiState.selectedInstrument,
                onInstrumentSelected = { viewModel.onInstrumentSelected(it) },
                instruments = uiState.availableInstruments
            )

            CalculatorInputSection(
                uiState = uiState,
                onBalanceChange = { viewModel.onBalanceChange(it) },
                onAccountCurrencyChange = { viewModel.onAccountCurrencyChange(it) },
                onRiskTypeChange = { viewModel.onRiskTypeChange(it) },
                onRiskValueChange = { viewModel.onRiskValueChange(it) },
                onDirectionChange = { viewModel.onDirectionChange(it) },
                onEntryChange = { viewModel.onEntryChange(it) },
                onStopChange = { viewModel.onStopChange(it) },
                onStopTypeChange = { viewModel.onStopTypeChange(it) },
                onTakeProfitChange = { viewModel.onTakeProfitChange(it) },
                onTakeProfitTypeChange = { viewModel.onTakeProfitTypeChange(it) },
                onLeverageChange = { viewModel.onLeverageChange(it) },
                onRateOverrideChange = { viewModel.onRateOverrideChange(it) },
                onUseLivePrice = { viewModel.useLivePrice() }
            )

            Button(
                onClick = { viewModel.calculate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isCalculating,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Calculate Position Size",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            uiState.result?.let { result ->
                CalculatorResultCard(result = result)
                RateUsedRow(uiState = uiState)
                if (uiState.guardrailWarnings.isNotEmpty()) {
                    AppleBanner(message = "BLOCK: ${uiState.guardrailWarnings.joinToString()}", isWarning = true)
                    Button(onClick = viewModel::showSaveSetupDialog, modifier = Modifier.fillMaxWidth()) { Text("Save anyway") }
                } else {
                    Button(onClick = viewModel::showSaveSetupDialog, modifier = Modifier.fillMaxWidth()) { Text("Save setup to journal") }
                }
            }

            uiState.setupMessage?.let { message ->
                AppleBanner(
                    message = message,
                    isWarning = message.startsWith("BLOCK:") ||
                        message.startsWith("Choose an active") ||
                        message.startsWith("Calculator currency")
                )
            }

            uiState.error?.let { error ->
                AppleBanner(message = error, isWarning = true)
            }

            if (tradeLog.isNotEmpty()) {
                TradeLogSection(
                    entries = tradeLog,
                    onDelete = { viewModel.deleteLogEntry(it) }
                )
            }
        }
    }
}

@Composable
private fun SaveJournalSetupDialog(
    accounts: List<app.marketbrief.core.storage.db.JournalAccountEntity>,
    setups: List<app.marketbrief.core.storage.db.TradeSetupEntity>,
    selectedAccountId: Long?,
    selectedSetupId: String?,
    selectedChecklistItems: Set<String>,
    guardrailWarnings: List<String>,
    message: String?,
    onAccountSelected: (Long?) -> Unit,
    onSetupSelected: (String?) -> Unit,
    onChecklistItemChanged: (String, Boolean) -> Unit,
    onSave: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedSetup = setups.firstOrNull { it.id == selectedSetupId }
    val checklist = selectedSetup?.checklist?.lines()?.filter { it.isNotBlank() }.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save planned trade") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Account")
                if (accounts.isEmpty()) {
                    Text("No active journal accounts are available.")
                } else {
                    accounts.forEach { account ->
                        TextButton(onClick = { onAccountSelected(account.id) }) {
                            Text(
                                if (selectedAccountId == account.id) "• ${account.name} (${account.currency})"
                                else "${account.name} (${account.currency})"
                            )
                        }
                    }
                }
                Text("Setup (optional)", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { onSetupSelected(null) }) {
                    Text(if (selectedSetupId == null) "• No setup" else "No setup")
                }
                setups.forEach { setup ->
                    TextButton(onClick = { onSetupSelected(setup.id) }) {
                        Text(if (selectedSetupId == setup.id) "• ${setup.name}" else setup.name)
                    }
                }
                checklist.forEachIndexed { index, item ->
                    val key = "$index:$item"
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = key in selectedChecklistItems,
                            onCheckedChange = { onChecklistItemChanged(key, it) }
                        )
                        Text(item)
                    }
                }
                if (guardrailWarnings.isNotEmpty()) {
                    AppleBanner(
                        message = "Account rule warning: ${guardrailWarnings.joinToString()}",
                        isWarning = true
                    )
                }
                message?.let { AppleBanner(message = it, isWarning = true) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(false) }) { Text("Save planned trade") }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                if (guardrailWarnings.isNotEmpty()) {
                    TextButton(onClick = { onSave(true) }) { Text("Override and save") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun PresetsSection(
    presets: List<PresetEntity>,
    onApply: (PresetEntity) -> Unit,
    onDelete: (PresetEntity) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "PRESETS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SavePresetChip(onClick = onSave)
            }
            items(presets, key = { it.id }) { preset ->
                PresetChip(
                    preset = preset,
                    onApply = { onApply(preset) },
                    onDelete = { onDelete(preset) }
                )
            }
        }
    }
}

@Composable
private fun SavePresetChip(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Save preset",
            modifier = Modifier.size(16.dp)
        )
        Text(text = "Save", fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun PresetChip(
    preset: PresetEntity,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onApply,
            shape = RoundedCornerShape(100.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Column {
                Text(text = preset.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${preset.instrumentSymbol} · ${preset.riskValueText}${if (preset.riskIsPercent) "%" else ""}",
                    fontSize = 10.sp
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete preset ${preset.name}",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save preset") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Preset name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RateUsedRow(uiState: app.marketbrief.feature.calculator.ui.CalculatorUiState) {
    val rate = uiState.rateUsed ?: return
    val quote = uiState.selectedInstrument.quoteCurrency.code
    val account = uiState.accountCurrencyText
    val source = uiState.rateAsOf?.let { "live · ${formatTime(it)}" } ?: "manual override"

    Text(
        text = "Rate used: 1 $quote = ${rate.toPlainString()} $account ($source)",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    if (uiState.rateIsStale) {
        AppleBanner(message = "Cached exchange rate is older than one hour. Verify it or enter a manual rate.", isWarning = true)
    }
}

@Composable
private fun TradeLogSection(
    entries: List<TradeLogEntity>,
    onDelete: (TradeLogEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "RECENT CALCULATIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        AppleGroupedSection {
            entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${entry.symbol} ${if (entry.direction == "LONG") "Long" else "Short"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Entry ${entry.entryText} · Stop ${entry.stopText}${if (entry.stopIsPips) " pips" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
            text = "${formatTime(entry.createdAt)} · risk ${entry.riskAmount} ${entry.accountCurrency}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${entry.lots} lots",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { onDelete(entry) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete log entry",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < entries.lastIndex) {
                    AppleHairlineDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ms))
