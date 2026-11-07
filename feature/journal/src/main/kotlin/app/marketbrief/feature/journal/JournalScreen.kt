package app.marketbrief.feature.journal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marketbrief.core.storage.db.JournalAttachmentEntity
import app.marketbrief.core.storage.db.JournalEntryEntity
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun JournalScreen(onBack: () -> Unit, viewModel: JournalViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var closing by rememberSaveable { mutableStateOf<String?>(null) }
    var exitPrice by rememberSaveable { mutableStateOf("") }
    var pnlOverride by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var editNotes by rememberSaveable { mutableStateOf("") }
    var editTags by rememberSaveable { mutableStateOf("") }
    var deleteConfirm by rememberSaveable { mutableStateOf<String?>(null) }
    var previewAttachment by rememberSaveable { mutableStateOf<String?>(null) }
    var removeAttachment by rememberSaveable { mutableStateOf<String?>(null) }
    var operationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddAccount by rememberSaveable { mutableStateOf(false) }
    var newAccountName by rememberSaveable { mutableStateOf("") }
    var newAccountType by rememberSaveable { mutableStateOf("LIVE") }
    var newAccountCurrency by rememberSaveable { mutableStateOf("USD") }
    var newAccountBalance by rememberSaveable { mutableStateOf("10000") }

    val activeAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }
    val editableAccount = activeAccount ?: state.accounts.firstOrNull()
    var accountName by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.name ?: "Account 1") }
    var accountType by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.type ?: "LIVE") }
    var balance by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.startingBalance ?: state.account?.startingBalance ?: "10000") }
    var currency by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.currency ?: state.account?.currency ?: "USD") }
    var maxRisk by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.maxRiskPerTradePercent ?: state.account?.maxRiskPerTradePercent ?: "1") }
    var dailyLoss by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.dailyLossLimitPercent ?: state.account?.dailyLossLimitPercent ?: "3") }
    var maxOpen by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.maxOpenRiskPercent ?: state.account?.maxOpenRiskPercent ?: "5") }
    var maxDrawdown by rememberSaveable(state.selectedAccountId) { mutableStateOf(editableAccount?.maxDrawdownPercent ?: state.account?.maxDrawdownPercent ?: "10") }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val dailyDraft = state.dailyEntries.firstOrNull { it.date == selectedDate }
    var dailyPlan by rememberSaveable(selectedDate) { mutableStateOf(dailyDraft?.planFollowed?.toString() ?: "") }
    var dailyMood by rememberSaveable(selectedDate) { mutableStateOf(dailyDraft?.mood ?: "") }
    var dailyEnergy by rememberSaveable(selectedDate) { mutableStateOf(dailyDraft?.energy?.toString() ?: "") }
    var dailyNotes by rememberSaveable(selectedDate) { mutableStateOf(dailyDraft?.notes ?: "") }
    var dailyLessons by rememberSaveable(selectedDate) { mutableStateOf(dailyDraft?.lessons ?: "") }
    var dailyTags by rememberSaveable(selectedDate) { mutableStateOf(dailyDraft?.tags ?: "") }
    var setupName by rememberSaveable { mutableStateOf("") }
    var setupDescription by rememberSaveable { mutableStateOf("") }
    var setupTags by rememberSaveable { mutableStateOf("") }
    var setupChecklist by rememberSaveable { mutableStateOf("") }
    var editingSetupId by rememberSaveable { mutableStateOf<String?>(null) }
    var attachmentTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val entryId = attachmentTarget
        if (uri != null && entryId != null) viewModel.addTradeAttachment(entryId, uri)
        attachmentTarget = null
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { destination ->
            try {
                val stream = context.contentResolver.openOutputStream(destination)
                    ?: throw IOException("The selected export destination could not be opened.")
                stream.use { it.write(viewModel.csv().toByteArray(Charsets.UTF_8)) }
                operationMessage = "CSV export saved."
            } catch (error: IOException) {
                operationMessage = error.message ?: "Could not save the CSV export."
            } catch (error: SecurityException) {
                operationMessage = error.message ?: "Permission to save the CSV export was denied."
            }
        }
    }
    val markdownExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let { destination ->
            try {
                val stream = context.contentResolver.openOutputStream(destination)
                    ?: throw IOException("The selected export destination could not be opened.")
                stream.use { it.write(viewModel.markdown().toByteArray(Charsets.UTF_8)) }
                operationMessage = "Markdown export saved."
            } catch (error: IOException) {
                operationMessage = error.message ?: "Could not save the Markdown export."
            } catch (error: SecurityException) {
                operationMessage = error.message ?: "Permission to save the Markdown export was denied."
            }
        }
    }

    val visibleEntries = state.entries
        .filter { state.selectedAccountId == null || it.accountId == state.selectedAccountId }
        .filter { state.statusFilter == "ALL" || it.status == state.statusFilter }
        .filter {
            val cutoff = when (state.dateRange) {
                "7D" -> System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                "30D" -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                else -> 0L
            }
            it.createdAt >= cutoff
        }

    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Row {
                        TextButton(onClick = { export.launch("marketbrief-journal.csv") }) { Text("CSV") }
                        TextButton(onClick = { markdownExport.launch("marketbrief-journal.md") }) { Text("Markdown") }
                    }
                }
                Text("Trade Journal", style = MaterialTheme.typography.headlineMedium)
                Text(
                    buildString {
                        append("${state.stats.closedCount} closed · ")
                        append("${state.stats.winRate.setScale(1, java.math.RoundingMode.HALF_UP)}% wins · ")
                        append("Avg R ${state.stats.averageR.setScale(2, java.math.RoundingMode.HALF_UP)}")
                        if (activeAccount != null) {
                            append(" · Expectancy ${state.stats.expectancy.setScale(2, java.math.RoundingMode.HALF_UP)} ${activeAccount.currency}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                if (activeAccount != null) {
                    Text(
                        "Max drawdown: ${state.stats.maxDrawdown} ${activeAccount.currency} · Profit factor: ${state.stats.profitFactor?.setScale(2, java.math.RoundingMode.HALF_UP) ?: "—"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        "Expectancy, drawdown, and profit factor are shown separately by account in Stats.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.selectAccount(null) }) {
                        Text(if (state.selectedAccountId == null) "• All accounts" else "All accounts")
                    }
                    state.accounts.forEach { account ->
                        TextButton(onClick = { viewModel.selectAccount(account.id) }) {
                            Text(
                                if (state.selectedAccountId == account.id) "• ${account.name}" else account.name
                            )
                        }
                    }
                    TextButton(onClick = { showAddAccount = true }) { Text("+ Account") }
                }
                Text(
                    activeAccount?.let { "${it.type} · ${it.currency}" } ?: "Showing trades across all accounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.selectedAccountId == null) {
                    val currencies = state.allAccounts.map { it.currency }.distinct().sorted()
                    Text(
                        "Currencies: ${currencies.joinToString().ifBlank { "none" }}. " +
                            "P&L remains in each trade's currency and is not combined across currencies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (activeAccount != null) {
                    TextButton(onClick = viewModel::archiveSelectedAccount) {
                        Text("Archive ${activeAccount.name}")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("TRADES", "DAILY", "SETUPS", "STATS", "ACCOUNT").forEach { tab ->
                        TextButton(onClick = { viewModel.setTab(tab) }) {
                            Text(if (state.selectedTab == tab) "• ${tab.lowercase()}" else tab.lowercase())
                        }
                    }
                }
            }

            when (state.selectedTab) {
                "TRADES" -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "PLANNED", "OPEN", "CLOSED", "CANCELLED").forEach { filter ->
                                TextButton(onClick = { viewModel.setStatusFilter(filter) }) {
                                    Text(if (state.statusFilter == filter) "• ${filter.lowercase()}" else filter.lowercase())
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "7D", "30D").forEach { range ->
                                TextButton(onClick = { viewModel.setDateRange(range) }) {
                                    Text(if (state.dateRange == range) "• $range" else range)
                                }
                            }
                        }
                    }
                    if (visibleEntries.isEmpty()) {
                        item { Text("No trades yet. Calculate a position and choose Save setup to journal it.") }
                    }
                    items(visibleEntries, key = { it.id }) { entry ->
                        TradeEntryCard(
                            entry = entry,
                            setupName = state.allSetups.firstOrNull { it.id == entry.setupId }?.name,
                            attachments = state.attachments.filter {
                                it.ownerType == "TRADE" && it.ownerId == entry.id
                            },
                            headlines = state.headlines.filter { it.entryId == entry.id },
                            onOpen = { viewModel.setStatus(entry, "OPEN") },
                            onClose = { closing = entry.id; exitPrice = ""; pnlOverride = "" },
                            onCancel = { viewModel.setStatus(entry, "CANCELLED") },
                            onEdit = { editing = entry.id; editNotes = entry.notes; editTags = entry.tags },
                            onAttach = { attachmentTarget = entry.id; attachmentPicker.launch("image/*") },
                            onViewAttachment = { previewAttachment = it.id },
                            onRemoveAttachment = { removeAttachment = it.id },
                            onOpenHeadline = { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (error: ActivityNotFoundException) {
                                    operationMessage = error.message ?: "No app can open this headline link."
                                } catch (error: IllegalArgumentException) {
                                    operationMessage = error.message ?: "This headline link is invalid."
                                }
                            },
                            onDelete = { deleteConfirm = entry.id }
                        )
                    }
                }

                "DAILY" -> {
                    item {
                        Text("Daily trading journal", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(selectedDate, { selectedDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { dailyPlan = "true" }) { Text(if (dailyPlan == "true") "• Plan followed" else "Plan followed") }
                            TextButton(onClick = { dailyPlan = "false" }) { Text(if (dailyPlan == "false") "• Plan not followed" else "Plan not followed") }
                        }
                        OutlinedTextField(dailyMood, { dailyMood = it }, label = { Text("Mood") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(dailyEnergy, { dailyEnergy = it }, label = { Text("Energy (1–5)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(dailyNotes, { dailyNotes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        OutlinedTextField(dailyLessons, { dailyLessons = it }, label = { Text("Lessons") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        OutlinedTextField(dailyTags, { dailyTags = it }, label = { Text("Tags") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            viewModel.saveDailyEntry(
                                selectedDate,
                                dailyPlan.toBooleanStrictOrNull(),
                                dailyMood,
                                dailyEnergy.toIntOrNull(),
                                dailyNotes,
                                dailyLessons,
                                dailyTags
                            )
                        }) { Text("Save daily entry") }
                    }
                    items(state.dailyEntries, key = { it.id }) { daily ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(daily.date, style = MaterialTheme.typography.titleSmall)
                                Text("${daily.mood} · energy ${daily.energy ?: "—"} · plan ${daily.planFollowed?.let { if (it) "followed" else "not followed" } ?: "not recorded"}")
                                if (daily.notes.isNotBlank()) Text(daily.notes)
                                if (daily.lessons.isNotBlank()) Text("Lesson: ${daily.lessons}")
                                TextButton(onClick = {
                                    selectedDate = daily.date
                                    dailyPlan = daily.planFollowed?.toString().orEmpty()
                                    dailyMood = daily.mood
                                    dailyEnergy = daily.energy?.toString().orEmpty()
                                    dailyNotes = daily.notes
                                    dailyLessons = daily.lessons
                                    dailyTags = daily.tags
                                }) { Text("Edit entry") }
                            }
                        }
                    }
                }

                "SETUPS" -> {
                    item {
                        Text("Setup playbook", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(setupName, { setupName = it }, label = { Text("Setup name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(setupDescription, { setupDescription = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(setupTags, { setupTags = it }, label = { Text("Tags") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            setupChecklist,
                            { setupChecklist = it },
                            label = { Text("Checklist (one item per line)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Button(onClick = {
                            if (setupName.isNotBlank()) {
                                viewModel.saveSetup(
                                    setupName,
                                    setupDescription,
                                    setupTags,
                                    setupChecklist.lines().filter { it.isNotBlank() },
                                    setupId = editingSetupId
                                )
                                setupName = ""
                                setupDescription = ""
                                setupTags = ""
                                setupChecklist = ""
                                editingSetupId = null
                            } else {
                                viewModel.saveSetup(setupName, setupDescription, setupTags, emptyList(), setupId = editingSetupId)
                            }
                        }) { Text(if (editingSetupId == null) "Save setup" else "Update setup") }
                        if (editingSetupId != null) {
                            TextButton(onClick = {
                                editingSetupId = null
                                setupName = ""
                                setupDescription = ""
                                setupTags = ""
                                setupChecklist = ""
                            }) { Text("Cancel edit") }
                        }
                    }
                    items(state.setups, key = { it.id }) { setup ->
                        val stats = state.setupStats[setup.id]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(setup.name, style = MaterialTheme.typography.titleSmall)
                                if (setup.description.isNotBlank()) Text(setup.description)
                                Text("Checklist: ${setup.checklist.lines().count { it.isNotBlank() }} items")
                                val usedCount = state.entries.count { it.setupId == setup.id }
                                Text("Used $usedCount times · ${stats?.closedCount ?: 0} closed · win rate ${stats?.winRate?.setScale(1, java.math.RoundingMode.HALF_UP) ?: "0"}% · average R ${stats?.averageR?.setScale(2, java.math.RoundingMode.HALF_UP) ?: "0"}")
                                TextButton(onClick = {
                                    editingSetupId = setup.id
                                    setupName = setup.name
                                    setupDescription = setup.description
                                    setupTags = setup.tags
                                    setupChecklist = setup.checklist
                                }) { Text("Edit") }
                                TextButton(onClick = { viewModel.archiveSetup(setup) }) { Text("Archive") }
                            }
                        }
                    }
                }

                "STATS" -> {
                    item {
                        Text("Account performance", style = MaterialTheme.typography.titleMedium)
                        state.allAccounts.forEach { account ->
                            val stats = state.accountStats[account.id] ?: app.marketbrief.core.risk.TradeStats.calculate(emptyList())
                            Text("${account.name}${if (account.archived) " · archived" else ""} (${account.currency}) · ${stats.closedCount} closed · ${stats.winRate.setScale(1, java.math.RoundingMode.HALF_UP)}% wins · Avg R ${stats.averageR.setScale(2, java.math.RoundingMode.HALF_UP)}")
                            val accountTrades = state.entries
                                .filter { it.accountId == account.id && it.status == "CLOSED" && it.pnl != null }
                                .sortedBy { it.closedAt ?: it.createdAt }
                            val equity = buildList {
                                var balance = account.startingBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO
                                add(balance)
                                accountTrades.forEach { trade ->
                                    trade.pnl?.toBigDecimalOrNull()?.let { pnl -> balance += pnl; add(balance) }
                                }
                            }
                            Text("Equity · ${account.currency}", style = MaterialTheme.typography.labelMedium)
                            EquityCurve(values = equity)
                        }
                        Text("Setup performance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                        state.allSetups.forEach { setup ->
                            val stats = state.setupStats[setup.id]
                            Text("${setup.name}${if (setup.archived) " · archived" else ""} · ${stats?.closedCount ?: 0} closed · Avg R ${stats?.averageR?.setScale(2, java.math.RoundingMode.HALF_UP) ?: "0"}")
                        }
                    }
                }

                "ACCOUNT" -> {
                    item {
                        Text(
                            "Account & risk rules · ${editableAccount?.name ?: "Account"}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(accountName, { accountName = it }, label = { Text("Account name") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("LIVE", "DEMO", "PROP").forEach { type ->
                                TextButton(onClick = { accountType = type }) {
                                    Text(if (accountType == type) "• $type" else type)
                                }
                            }
                        }
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
                        Button(
                            onClick = {
                                viewModel.saveAccount(
                                    balance, currency, maxRisk, dailyLoss, maxOpen, maxDrawdown,
                                    editableAccount?.drawdownMode ?: "STATIC",
                                    editableAccount?.resetTimezone ?: "Africa/Nairobi",
                                    accountName,
                                    accountType
                                )
                            }
                        ) { Text("Save account rules") }
                    }
                }

                else -> Unit
            }
        }
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("OK") } }
        )
    }

    operationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { operationMessage = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { operationMessage = null }) { Text("OK") } }
        )
    }

    previewAttachment?.let { attachmentId ->
        state.attachments.firstOrNull { it.id == attachmentId }?.let { attachment ->
            AttachmentPreviewDialog(attachment, onDismiss = { previewAttachment = null })
        }
    }

    removeAttachment?.let { attachmentId ->
        state.attachments.firstOrNull { it.id == attachmentId }?.let { attachment ->
            AlertDialog(
                onDismissRequest = { removeAttachment = null },
                title = { Text("Remove screenshot?") },
                text = { Text("The private image file will be deleted from this device.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTradeAttachment(attachment)
                        removeAttachment = null
                    }) { Text("Remove") }
                },
                dismissButton = { TextButton(onClick = { removeAttachment = null }) { Text("Cancel") } }
            )
        }
    }

    if (showAddAccount) {
        AlertDialog(
            onDismissRequest = { showAddAccount = false },
            title = { Text("Add account") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(newAccountName, { newAccountName = it }, label = { Text("Account name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("LIVE", "DEMO", "PROP").forEach { type ->
                            TextButton(onClick = { newAccountType = type }) {
                                Text(if (newAccountType == type) "• $type" else type)
                            }
                        }
                    }
                    OutlinedTextField(newAccountCurrency, { newAccountCurrency = it.uppercase() }, label = { Text("Currency") })
                    OutlinedTextField(newAccountBalance, { newAccountBalance = it }, label = { Text("Starting balance") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addAccount(newAccountName, newAccountType, newAccountCurrency, newAccountBalance)
                    showAddAccount = false
                    newAccountName = ""
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddAccount = false }) { Text("Cancel") } }
        )
    }

    closing?.let { entryId ->
        val entry = state.entries.firstOrNull { it.id == entryId }
        if (entry != null) {
            AlertDialog(
                onDismissRequest = { closing = null },
                title = { Text("Close ${entry.symbol}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(exitPrice, { exitPrice = it }, label = { Text("Exit price") })
                        OutlinedTextField(pnlOverride, { pnlOverride = it }, label = { Text("Manual P&L override (optional)") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.close(entry, exitPrice, pnlOverride); closing = null }) { Text("Save close") }
                },
                dismissButton = { TextButton(onClick = { closing = null }) { Text("Cancel") } }
            )
        }
    }

    editing?.let { entryId ->
        val entry = state.entries.firstOrNull { it.id == entryId }
        if (entry != null) {
            AlertDialog(
                onDismissRequest = { editing = null },
                title = { Text("Edit ${entry.symbol}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(editNotes, { editNotes = it }, label = { Text("Notes") }, minLines = 3)
                        OutlinedTextField(editTags, { editTags = it }, label = { Text("Tags (comma separated)") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.updateEntry(entry, editNotes, editTags); editing = null }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
            )
        }
    }

    deleteConfirm?.let { entryId ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Delete trade?") },
            text = { Text("This action permanently removes the selected journal trade.") },
            confirmButton = {
                TextButton(onClick = {
                    state.entries.firstOrNull { it.id == entryId }?.let(viewModel::deleteEntry)
                    deleteConfirm = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TradeEntryCard(
    entry: JournalEntryEntity,
    setupName: String?,
    attachments: List<JournalAttachmentEntity>,
    headlines: List<app.marketbrief.core.storage.db.JournalHeadlineEntity>,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onCancel: () -> Unit,
    onEdit: () -> Unit,
    onAttach: () -> Unit,
    onViewAttachment: (JournalAttachmentEntity) -> Unit,
    onRemoveAttachment: (JournalAttachmentEntity) -> Unit,
    onOpenHeadline: (String) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${entry.symbol} · ${entry.direction} · ${entry.status}", style = MaterialTheme.typography.titleMedium)
            Text("Entry ${entry.entry} · Stop ${entry.stop}${if (entry.stopIsPips) " pips" else ""} · ${entry.lots} lots · Risk ${entry.riskAmount} ${entry.accountCurrency}")
            setupName?.let { Text("Setup: $it") }
            entry.checklistResult.takeIf { it.isNotBlank() }?.let { Text("Setup checklist score: $it") }
            if (entry.notes.isNotBlank()) Text(entry.notes)
            if (entry.tags.isNotBlank()) Text(entry.tags, color = MaterialTheme.colorScheme.primary)
            if (entry.status == "PLANNED") Button(onClick = onOpen) { Text("Mark open") }
            if (entry.status == "OPEN") Button(onClick = onClose) { Text("Close trade") }
            if (entry.status in listOf("PLANNED", "OPEN")) TextButton(onClick = onCancel) { Text("Cancel trade") }
            if (entry.status == "CLOSED") Text("P&L ${entry.pnl} ${entry.accountCurrency} · ${entry.rMultiple}R")
            headlines.forEach { headline ->
                TextButton(onClick = { onOpenHeadline(headline.url) }) {
                    Text("Related news: ${headline.title}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit notes") }
                TextButton(onClick = onAttach) { Text("Attach screenshot") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            attachments.forEachIndexed { index, attachment ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Screenshot ${index + 1}")
                    TextButton(onClick = { onViewAttachment(attachment) }) { Text("View") }
                    TextButton(onClick = { onRemoveAttachment(attachment) }) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewDialog(
    attachment: JournalAttachmentEntity,
    onDismiss: () -> Unit
) {
    val image by produceState(
        initialValue = AttachmentImageState(),
        key1 = attachment.filePath
    ) {
        try {
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                android.graphics.BitmapFactory.decodeFile(attachment.filePath, options)
            }
            value = AttachmentImageState(isLoaded = true, bitmap = bitmap)
        } catch (_: SecurityException) {
            value = AttachmentImageState(isLoaded = true, bitmap = null)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trade screenshot") },
        text = {
            val bitmap = image.bitmap
            if (!image.isLoaded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Loading image…")
                }
            } else if (bitmap == null) {
                Text("This image could not be loaded.")
            } else {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Attached trade screenshot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private data class AttachmentImageState(
    val isLoaded: Boolean = false,
    val bitmap: android.graphics.Bitmap? = null
)

@Composable
private fun EquityCurve(values: List<BigDecimal>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        if (values.isEmpty()) return@Canvas
        val minimum = values.minOrNull() ?: return@Canvas
        val maximum = values.maxOrNull() ?: return@Canvas
        val spread = (maximum - minimum).takeIf { it.signum() != 0 } ?: BigDecimal.ONE
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = if (values.size == 1) size.width / 2f
            else size.width * index / (values.size - 1).toFloat()
            val normalized = (value - minimum).divide(spread, java.math.MathContext.DECIMAL64).toFloat()
            val y = size.height - normalized * (size.height - 8.dp.toPx()) - 4.dp.toPx()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(
            color = color.copy(alpha = 0.18f),
            start = androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height - 1f),
            strokeWidth = 1.dp.toPx()
        )
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
    }
}
