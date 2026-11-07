package app.marketbrief.feature.journal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import app.marketbrief.core.data.RateRepository
import app.marketbrief.core.risk.ClosedTradeResult
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.InstrumentCatalog
import app.marketbrief.core.risk.JournalAnalytics
import app.marketbrief.core.risk.ScopedClosedTrade
import app.marketbrief.core.risk.TradeStats
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.db.AccountSettingsEntity
import app.marketbrief.core.storage.db.DailyEntryDao
import app.marketbrief.core.storage.db.DailyEntryEntity
import app.marketbrief.core.storage.db.JournalAccountDao
import app.marketbrief.core.storage.db.JournalAccountEntity
import app.marketbrief.core.storage.db.JournalAttachmentDao
import app.marketbrief.core.storage.db.JournalAttachmentEntity
import app.marketbrief.core.storage.db.JournalDao
import app.marketbrief.core.storage.db.JournalEntryEntity
import app.marketbrief.core.storage.db.JournalHeadlineEntity
import app.marketbrief.core.storage.db.TradeSetupDao
import app.marketbrief.core.storage.db.TradeSetupEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.MathContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

data class JournalUiState(
    val entries: List<JournalEntryEntity> = emptyList(),
    val stats: TradeStats = TradeStats.calculate(emptyList()),
    val account: AccountSettingsEntity? = null,
    val accounts: List<JournalAccountEntity> = emptyList(),
    val allAccounts: List<JournalAccountEntity> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedTab: String = "TRADES",
    val statusFilter: String = "ALL",
    val dateRange: String = "ALL",
    val setups: List<TradeSetupEntity> = emptyList(),
    val allSetups: List<TradeSetupEntity> = emptyList(),
    val dailyEntries: List<DailyEntryEntity> = emptyList(),
    val attachments: List<JournalAttachmentEntity> = emptyList(),
    val headlines: List<JournalHeadlineEntity> = emptyList(),
    val accountStats: Map<Long, TradeStats> = emptyMap(),
    val setupStats: Map<String, TradeStats> = emptyMap(),
    val message: String? = null
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val dao: JournalDao,
    private val rateRepository: RateRepository,
    private val accountDao: JournalAccountDao,
    private val setupDao: TradeSetupDao,
    private val dailyDao: DailyEntryDao,
    private val attachmentDao: JournalAttachmentDao,
    private val prefs: UserPrefsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<JournalUiState> = combine(
        dao.observeEntries(),
        dao.observeAccount(),
        accountDao.observeActive(),
        accountDao.observeAll(),
        setupDao.observeActive(),
        setupDao.observeAll(),
        dailyDao.observeAll(),
        prefs.selectedJournalAccountId,
        prefs.selectedJournalTab,
        prefs.journalStatusFilter,
        prefs.journalDateRange,
        attachmentDao.observeAll(),
        dao.observeHeadlines(),
        _message
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val entries = values[0] as List<JournalEntryEntity>
        val settings = values[1] as AccountSettingsEntity?
        val accounts = values[2] as List<JournalAccountEntity>
        val allAccounts = values[3] as List<JournalAccountEntity>
        val setups = values[4] as List<TradeSetupEntity>
        val allSetups = values[5] as List<TradeSetupEntity>
        val daily = values[6] as List<DailyEntryEntity>
        val selectedId = values[7] as Long?
        val selectedTab = values[8] as String
        val statusFilter = values[9] as String
        val dateRange = values[10] as String
        val attachments = values[11] as List<JournalAttachmentEntity>
        val headlines = values[12] as List<JournalHeadlineEntity>
        val message = values[13] as String?
        val closed = entries.filter { it.status == "CLOSED" }.mapNotNull { entry ->
            val pnl = entry.pnl?.toBigDecimalOrNull() ?: return@mapNotNull null
            val r = entry.rMultiple?.toBigDecimalOrNull() ?: return@mapNotNull null
            entry to ClosedTradeResult(pnl, r)
        }.sortedBy { (entry, _) -> entry.closedAt ?: entry.createdAt }
        val scoped = closed.map { (entry, result) -> ScopedClosedTrade(entry.accountId, entry.setupId, result) }
        val accountId = selectedId?.takeIf { id -> accounts.any { it.id == id } }
        JournalUiState(
            entries = entries,
            stats = TradeStats.calculate(closed.filter { accountId == null || it.first.accountId == accountId }.map { it.second }),
            account = settings,
            accounts = accounts,
            allAccounts = allAccounts,
            selectedAccountId = accountId,
            selectedTab = selectedTab,
            statusFilter = statusFilter,
            dateRange = dateRange,
            setups = setups,
            allSetups = allSetups,
            dailyEntries = daily,
            attachments = attachments,
            headlines = headlines,
            accountStats = JournalAnalytics.statsByAccount(scoped),
            setupStats = JournalAnalytics.statsBySetup(scoped),
            message = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    init {
        viewModelScope.launch {
            if (accountDao.observeActive().first().isEmpty()) {
                val old = dao.observeAccount().first()
                accountDao.upsert(
                    JournalAccountEntity(
                        id = 1,
                        name = "Account 1",
                        type = "LIVE",
                        currency = old?.currency ?: "USD",
                        startingBalance = old?.startingBalance ?: "10000",
                        maxRiskPerTradePercent = old?.maxRiskPerTradePercent ?: "1",
                        dailyLossLimitPercent = old?.dailyLossLimitPercent ?: "3",
                        maxOpenRiskPercent = old?.maxOpenRiskPercent ?: "5",
                        maxDrawdownPercent = old?.maxDrawdownPercent ?: "10",
                        drawdownMode = old?.drawdownMode ?: "STATIC",
                        resetTimezone = old?.resetTimezone ?: "Africa/Nairobi",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun selectAccount(accountId: Long?) {
        viewModelScope.launch {
            if (accountId == null) prefs.clearSelectedJournalAccountId()
            else prefs.setSelectedJournalAccountId(accountId)
        }
    }

    fun setTab(tab: String) {
        viewModelScope.launch { prefs.setSelectedJournalTab(tab) }
    }

    fun setStatusFilter(value: String) {
        viewModelScope.launch { prefs.setJournalStatusFilter(value) }
    }

    fun setDateRange(value: String) {
        viewModelScope.launch { prefs.setJournalDateRange(value) }
    }

    fun setStatus(entry: JournalEntryEntity, status: String) {
        viewModelScope.launch {
            dao.upsertEntry(
                entry.copy(
                    status = status,
                    openedAt = if (status == "OPEN") System.currentTimeMillis() else entry.openedAt
                )
            )
        }
    }

    fun updateEntry(entry: JournalEntryEntity, notes: String, tags: String) {
        viewModelScope.launch { dao.upsertEntry(entry.copy(notes = notes, tags = tags)) }
    }

    fun deleteEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            val attachments = attachmentDao.forOwner("TRADE", entry.id)
            dao.deleteEntry(entry.id)
            dao.deleteHeadlines(entry.id)
            var fileCleanupFailed = false
            attachments.forEach { attachment ->
                val deleted = withContext(Dispatchers.IO) {
                    val file = File(attachment.filePath)
                    !file.exists() || file.delete()
                }
                attachmentDao.delete(attachment.id)
                if (!deleted) fileCleanupFailed = true
            }
            setMessage(
                if (fileCleanupFailed) "Trade deleted, but a private screenshot file could not be removed."
                else "Trade deleted."
            )
        }
    }

    fun deleteTradeAttachment(attachment: JournalAttachmentEntity) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                val file = File(attachment.filePath)
                !file.exists() || file.delete()
            }
            if (!deleted) {
                setMessage("Could not remove the private screenshot file.")
                return@launch
            }
            attachmentDao.delete(attachment.id)
            setMessage("Screenshot removed.")
        }
    }

    fun addTradeAttachment(entryId: String, uri: Uri) {
        viewModelScope.launch {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") != true) {
                setMessage("Choose an image file for the trade attachment.")
                return@launch
            }
            val extension = mimeType.substringAfter('/', "img").take(8).filter(Char::isLetterOrDigit)
            val directory = File(context.filesDir, "journal")
            var destination: File? = null
            try {
                withContext(Dispatchers.IO) {
                    if (!directory.exists() && !directory.mkdirs()) {
                        throw IOException("Could not create the private journal folder.")
                    }
                    val file = File(directory, "${UUID.randomUUID()}.$extension")
                    destination = file
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("The selected image is no longer available.")
                    input.use { source -> file.outputStream().use(source::copyTo) }
                    attachmentDao.add(
                        JournalAttachmentEntity(
                            id = UUID.randomUUID().toString(),
                            ownerType = "TRADE",
                            ownerId = entryId,
                            filePath = file.absolutePath,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                setMessage("Screenshot attached to trade.")
            } catch (error: IOException) {
                destination?.delete()
                setMessage(error.message ?: "Could not save the selected screenshot.")
            } catch (error: SecurityException) {
                destination?.delete()
                setMessage(error.message ?: "Permission to save the selected screenshot was denied.")
            }
        }
    }

    fun close(entry: JournalEntryEntity, exitPrice: String, pnlOverride: String?) {
        val exit = exitPrice.toBigDecimalOrNull()
        if (exit == null || exit.signum() <= 0) {
            setMessage("Enter a valid exit price greater than zero.")
            return
        }
        val manualPnl = pnlOverride?.takeIf { it.isNotBlank() }?.toBigDecimalOrNull()
        if (!pnlOverride.isNullOrBlank() && manualPnl == null) {
            setMessage("Enter a valid P&L amount or leave the override blank.")
            return
        }
        val pnl = manualPnl ?: run {
            val open = entry.entry.toBigDecimalOrNull()
            val lots = entry.lots.toBigDecimalOrNull()
            val instrument = InstrumentCatalog.default().find(entry.symbol)
            if (open == null || lots == null || instrument == null) {
                setMessage("Could not calculate P&L. Enter a manual P&L amount.")
                return
            }
            val quoteCurrency = instrument.quoteCurrency
            val accountCurrency = runCatching { CurrencyCode(entry.accountCurrency) }.getOrNull()
            val rate = when {
                accountCurrency == null -> null
                quoteCurrency == accountCurrency -> BigDecimal.ONE
                else -> rateRepository.getRate(quoteCurrency, accountCurrency)?.let(BigDecimal::valueOf)
            }
            if (rate == null) {
                setMessage("Exchange rate unavailable. Enter a manual P&L amount.")
                return
            }
            val move = if (entry.direction == "LONG") exit - open else open - exit
            move * lots * instrument.contractSize * rate
        }
        val risk = entry.riskAmount.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        if (risk == null) {
            setMessage("Trade risk is invalid; update it before closing this trade.")
            return
        }
        val r = pnl.divide(risk, MathContext.DECIMAL128)
        viewModelScope.launch {
            dao.upsertEntry(
                entry.copy(
                    status = "CLOSED",
                    closedAt = System.currentTimeMillis(),
                    exitPrice = exit.toPlainString(),
                    pnl = pnl.toPlainString(),
                    rMultiple = r.toPlainString()
                )
            )
            setMessage("Trade closed.")
        }
    }

    fun saveAccount(
        balance: String,
        currency: String,
        maxRisk: String,
        dailyLoss: String,
        maxOpen: String,
        maxDrawdown: String,
        mode: String,
        timezone: String,
        name: String,
        type: String
    ) {
        val starting = balance.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        if (starting == null || currency.length != 3 || name.isBlank() || type !in setOf("LIVE", "DEMO", "PROP")) {
            setMessage("Enter an account name, supported account type, positive balance, and three-letter currency.")
            return
        }
        viewModelScope.launch {
            val selected = uiState.value.accounts.firstOrNull { it.id == uiState.value.selectedAccountId }
                ?: uiState.value.accounts.firstOrNull()
            val account = JournalAccountEntity(
                id = selected?.id ?: 0,
                name = name.trim(),
                type = type,
                currency = currency.uppercase(),
                startingBalance = starting.toPlainString(),
                maxRiskPerTradePercent = maxRisk,
                dailyLossLimitPercent = dailyLoss,
                maxOpenRiskPercent = maxOpen,
                maxDrawdownPercent = maxDrawdown,
                drawdownMode = mode,
                resetTimezone = timezone,
                createdAt = selected?.createdAt ?: System.currentTimeMillis()
            )
            val id = accountDao.upsert(account)
            prefs.setSelectedJournalAccountId(if (account.id == 0L) id else account.id)
            dao.saveAccount(
                AccountSettingsEntity(
                    id = 1,
                    startingBalance = starting.toPlainString(),
                    currency = currency.uppercase(),
                    maxRiskPerTradePercent = maxRisk,
                    dailyLossLimitPercent = dailyLoss,
                    maxOpenRiskPercent = maxOpen,
                    maxDrawdownPercent = maxDrawdown,
                    drawdownMode = mode,
                    resetTimezone = timezone
                )
            )
            setMessage("Account rules saved.")
        }
    }

    fun addAccount(name: String, type: String, currency: String, balance: String) {
        val amount = balance.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        if (name.isBlank() || currency.length != 3 || amount == null || type !in setOf("LIVE", "DEMO", "PROP")) {
            setMessage("Enter a name, supported account type, currency, and positive starting balance.")
            return
        }
        viewModelScope.launch {
            val id = accountDao.upsert(
                JournalAccountEntity(
                    name = name.trim(),
                    type = type,
                    currency = currency.uppercase(),
                    startingBalance = amount.toPlainString(),
                    maxRiskPerTradePercent = "1",
                    dailyLossLimitPercent = "3",
                    maxOpenRiskPercent = "5",
                    maxDrawdownPercent = "10",
                    drawdownMode = "STATIC",
                    resetTimezone = "Africa/Nairobi",
                    createdAt = System.currentTimeMillis()
                )
            )
            prefs.setSelectedJournalAccountId(id)
            setMessage("Account added.")
        }
    }

    fun archiveSelectedAccount() {
        val selected = uiState.value.accounts.firstOrNull { it.id == uiState.value.selectedAccountId }
        if (selected == null) {
            setMessage("Select an account before archiving it.")
            return
        }
        if (uiState.value.accounts.size <= 1) {
            setMessage("Keep at least one active account.")
            return
        }
        viewModelScope.launch {
            accountDao.archive(selected.id)
            prefs.clearSelectedJournalAccountId()
            setMessage("${selected.name} archived. Its trades remain in the journal.")
        }
    }

    fun saveDailyEntry(
        date: String,
        planFollowed: Boolean?,
        mood: String,
        energy: Int?,
        notes: String,
        lessons: String,
        tags: String
    ) {
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
        if (parsed == null || energy != null && energy !in 1..5) {
            setMessage("Enter a valid date and energy rating from 1 to 5.")
            return
        }
        viewModelScope.launch {
            val old = dailyDao.getByDate(date)
            dailyDao.upsert(
                DailyEntryEntity(
                    id = old?.id ?: UUID.randomUUID().toString(),
                    accountId = uiState.value.selectedAccountId,
                    date = date,
                    planFollowed = planFollowed,
                    mood = mood,
                    energy = energy,
                    notes = notes,
                    lessons = lessons,
                    tags = tags
                )
            )
            setMessage("Daily entry saved.")
        }
    }

    fun saveSetup(
        name: String,
        description: String,
        tags: String,
        checklist: List<String>,
        color: String = "TEAL",
        setupId: String? = null
    ) {
        if (name.isBlank() || checklist.any { it.isBlank() }) {
            setMessage("Enter a setup name and remove any blank checklist items.")
            return
        }
        viewModelScope.launch {
            val existing = setupId?.let { id -> uiState.value.setups.firstOrNull { it.id == id } }
            setupDao.upsert(
                TradeSetupEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    description = description.trim(),
                    tags = tags,
                    checklist = checklist.map { it.trim() }.joinToString("\n"),
                    color = color,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
            )
            setMessage(if (existing == null) "Setup saved." else "Setup updated.")
        }
    }

    fun archiveSetup(setup: TradeSetupEntity) {
        viewModelScope.launch { setupDao.archive(setup.id) }
    }

    fun csv(): String {
        val current = uiState.value
        val accountNames = current.allAccounts.associate { it.id to it.name }
        val setupNames = current.allSetups.associate { it.id to it.name }
        val header = "id,createdAt,accountName,setupName,symbol,direction,entry,stop,lots,riskAmount,currency,status,exitPrice,pnl,rMultiple,checklistResult,tags,notes\n"
        return header + current.entries.joinToString("\n") { entry ->
            listOf(
                entry.id, entry.createdAt, accountNames[entry.accountId].orEmpty(),
                entry.setupId?.let(setupNames::get).orEmpty(), entry.symbol, entry.direction,
                entry.entry, entry.stop, entry.lots, entry.riskAmount, entry.accountCurrency, entry.status,
                entry.exitPrice.orEmpty(), entry.pnl.orEmpty(), entry.rMultiple.orEmpty(),
                entry.checklistResult, entry.tags, entry.notes
            ).joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" }
        }
    }

    fun markdown(): String {
        val current = uiState.value
        val accountNames = current.allAccounts.associate { it.id to it.name }
        val setupNames = current.allSetups.associate { it.id to it.name }
        return buildString {
            appendLine("# MarketBrief Trade Journal")
            appendLine()
            appendLine("| Date | Account | Setup | Symbol | Direction | Status | Entry | Stop | Exit | P&L | Currency | R | Checklist | Tags | Notes |")
            appendLine("| --- | --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- | ---: | --- | --- | --- |")
            current.entries.forEach { entry ->
                val date = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
                val cells = listOf(
                    date,
                    accountNames[entry.accountId].orEmpty(),
                    entry.setupId?.let(setupNames::get).orEmpty(),
                    entry.symbol,
                    entry.direction,
                    entry.status,
                    entry.entry,
                    entry.stop,
                    entry.exitPrice.orEmpty(),
                    entry.pnl.orEmpty(),
                    entry.accountCurrency,
                    entry.rMultiple.orEmpty(),
                    entry.checklistResult,
                    entry.tags,
                    entry.notes
                )
                append("| ")
                append(cells.joinToString(" | ") { it.replace("|", "\\|").replace("\n", " ") })
                appendLine(" |")
            }
        }
    }

    private fun setMessage(message: String) {
        _message.value = message
    }

    fun dismissMessage() {
        _message.value = null
    }
}
