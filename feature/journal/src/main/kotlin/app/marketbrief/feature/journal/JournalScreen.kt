package app.marketbrief.feature.journal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.storage.db.JournalEntryEntity
import java.text.NumberFormat

@Composable
fun JournalScreen(onBack: () -> Unit, viewModel: JournalViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var closing by remember { mutableStateOf<JournalEntryEntity?>(null) }
    var exitPrice by remember { mutableStateOf("") }
    var pnlOverride by remember { mutableStateOf("") }
    var balance by remember(state.account) { mutableStateOf(state.account?.startingBalance ?: "10000") }
    var currency by remember(state.account) { mutableStateOf(state.account?.currency ?: "USD") }
    var maxRisk by remember(state.account) { mutableStateOf(state.account?.maxRiskPerTradePercent ?: "1") }
    var dailyLoss by remember(state.account) { mutableStateOf(state.account?.dailyLossLimitPercent ?: "3") }
    var maxOpen by remember(state.account) { mutableStateOf(state.account?.maxOpenRiskPercent ?: "5") }
    var maxDrawdown by remember(state.account) { mutableStateOf(state.account?.maxDrawdownPercent ?: "10") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { stream -> stream.write(viewModel.csv().toByteArray()) } }
    }

    Scaffold { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) { Text("Back") }
                    TextButton(onClick = { export.launch("marketbrief-journal.csv") }) { Text("Export CSV") }
                }
                Text("Trade Journal", style = MaterialTheme.typography.headlineMedium)
                Text("${state.stats.closedCount} closed · ${state.stats.winRate.setScale(1, java.math.RoundingMode.HALF_UP)}% wins · Avg R ${state.stats.averageR.setScale(2, java.math.RoundingMode.HALF_UP)} · Expectancy ${state.stats.expectancy.setScale(2, java.math.RoundingMode.HALF_UP)}")
                Text("Max drawdown: ${state.stats.maxDrawdown} · Profit factor: ${state.stats.profitFactor?.setScale(2, java.math.RoundingMode.HALF_UP) ?: "—"}")
            }
            item {
                Text("ACCOUNT & RISK RULES", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(balance, { balance = it }, label = { Text("Starting balance") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(maxRisk, { maxRisk = it }, label = { Text("Max risk / trade %") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(dailyLoss, { dailyLoss = it }, label = { Text("Daily loss %") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(maxOpen, { maxOpen = it }, label = { Text("Max open risk %") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(maxDrawdown, { maxDrawdown = it }, label = { Text("Max drawdown %") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = { viewModel.saveAccount(balance, currency, maxRisk, dailyLoss, maxOpen, maxDrawdown, "STATIC", "Africa/Nairobi") }) { Text("Save account rules") }
            }
            if (state.entries.isEmpty()) item { Text("No setups yet. Calculate a position and choose Save setup to journal it.") }
            items(state.entries, key = { it.id }) { entry ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${entry.symbol} · ${entry.direction} · ${entry.status}", style = MaterialTheme.typography.titleMedium)
                    Text("Entry ${entry.entry} · Stop ${entry.stop}${if (entry.stopIsPips) " pips" else ""} · ${entry.lots} lots · Risk ${entry.riskAmount} ${entry.accountCurrency}")
                    if (entry.status == "PLANNED") Button(onClick = { viewModel.setStatus(entry, "OPEN") }) { Text("Mark open") }
                    if (entry.status == "OPEN") Button(onClick = { closing = entry; exitPrice = ""; pnlOverride = "" }) { Text("Close trade") }
                    if (entry.status in listOf("PLANNED", "OPEN")) TextButton(onClick = { viewModel.setStatus(entry, "CANCELLED") }) { Text("Cancel") }
                    if (entry.status == "CLOSED") Text("P&L ${entry.pnl} · ${entry.rMultiple}R")
                }
            }
        }
    }
    closing?.let { entry ->
        AlertDialog(
            onDismissRequest = { closing = null },
            title = { Text("Close ${entry.symbol}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(exitPrice, { exitPrice = it }, label = { Text("Exit price") })
                    OutlinedTextField(pnlOverride, { pnlOverride = it }, label = { Text("Manual P&L override (optional)") })
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.close(entry, exitPrice, pnlOverride.takeIf { it.isNotBlank() }); closing = null }) { Text("Save close") } },
            dismissButton = { TextButton(onClick = { closing = null }) { Text("Cancel") } }
        )
    }
}
