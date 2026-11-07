package app.marketbrief.core.storage

import app.marketbrief.core.storage.db.ArticleDao
import app.marketbrief.core.storage.db.JournalDao
import app.marketbrief.core.storage.db.JournalEntryEntity
import app.marketbrief.core.storage.db.JournalHeadlineEntity
import app.marketbrief.core.storage.db.JournalAccountDao
import app.marketbrief.core.storage.db.JournalAccountEntity
import app.marketbrief.core.storage.db.TradeSetupDao
import app.marketbrief.core.storage.db.TradeSetupEntity
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.risk.AccountRiskState
import app.marketbrief.core.risk.GuardrailEvaluator
import app.marketbrief.core.risk.RiskRules
import app.marketbrief.core.risk.DrawdownMode
import app.marketbrief.core.risk.ProposedTrade
import app.marketbrief.core.risk.Violation
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class PlannedSetup(
    val symbol: String, val assetClass: String, val direction: String, val entry: String,
    val stop: String, val stopIsPips: Boolean, val takeProfit: String?, val lots: String,
    val riskAmount: String, val riskPercent: String, val accountCurrency: String,
    val accountId: Long? = null, val setupId: String? = null, val checklistResult: String = ""
)

data class JournalSetupOptions(
    val accounts: List<JournalAccountEntity>,
    val setups: List<TradeSetupEntity>,
    val selectedAccountId: Long?
)

interface JournalSetupWriter {
    suspend fun options(): JournalSetupOptions
    suspend fun evaluate(setup: PlannedSetup): List<Violation> = emptyList()
    suspend fun save(setup: PlannedSetup): String
}

class RoomJournalSetupWriter @Inject constructor(
    private val journalDao: JournalDao,
    private val articleDao: ArticleDao,
    private val accountDao: JournalAccountDao,
    private val setupDao: TradeSetupDao,
    private val prefs: UserPrefsRepository
) : JournalSetupWriter {
    override suspend fun options(): JournalSetupOptions {
        var accounts = accountDao.observeActive().first()
        if (accounts.isEmpty()) {
            val settings = journalDao.observeAccount().first()
            accountDao.upsert(
                JournalAccountEntity(
                    id = 1,
                    name = "Account 1",
                    type = "LIVE",
                    currency = settings?.currency ?: "USD",
                    startingBalance = settings?.startingBalance ?: "10000",
                    maxRiskPerTradePercent = settings?.maxRiskPerTradePercent ?: "1",
                    dailyLossLimitPercent = settings?.dailyLossLimitPercent ?: "3",
                    maxOpenRiskPercent = settings?.maxOpenRiskPercent ?: "5",
                    maxDrawdownPercent = settings?.maxDrawdownPercent ?: "10",
                    drawdownMode = settings?.drawdownMode ?: "STATIC",
                    resetTimezone = settings?.resetTimezone ?: "Africa/Nairobi",
                    createdAt = System.currentTimeMillis()
                )
            )
            accounts = accountDao.observeActive().first()
        }
        val selected = prefs.selectedJournalAccountId.first()
            ?.takeIf { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id
        return JournalSetupOptions(accounts, setupDao.observeActive().first(), selected)
    }

    override suspend fun evaluate(setup: PlannedSetup): List<Violation> {
        val accounts = accountDao.observeActive().first()
        val selectedAccount = setup.accountId?.let { accountDao.get(it) }
            ?: prefs.selectedJournalAccountId.first()?.let { accountDao.get(it) }
            ?: accounts.firstOrNull()
        val settings = journalDao.observeAccount().first()
        val starting = selectedAccount?.startingBalance?.toBigDecimalOrNull()
            ?: settings?.startingBalance?.toBigDecimalOrNull()
            ?: BigDecimal("10000")
        val rules = RiskRules(
            selectedAccount?.maxRiskPerTradePercent?.toBigDecimalOrNull() ?: BigDecimal("1"),
            selectedAccount?.dailyLossLimitPercent?.toBigDecimalOrNull() ?: BigDecimal("3"),
            selectedAccount?.maxOpenRiskPercent?.toBigDecimalOrNull() ?: BigDecimal("5"),
            selectedAccount?.maxDrawdownPercent?.toBigDecimalOrNull() ?: BigDecimal("10"),
            if (selectedAccount?.drawdownMode == "TRAILING") DrawdownMode.TRAILING else DrawdownMode.STATIC
        )
        val closed = journalDao.closedEntries()
            .filter { selectedAccount == null || it.accountId == selectedAccount.id }
            .sortedBy { it.closedAt ?: Long.MAX_VALUE }
        val pnl = closed.mapNotNull { it.pnl?.toBigDecimalOrNull() }
        val current = pnl.fold(starting) { a, b -> a + b }
        var peak = starting
        var running = starting
        pnl.forEach { running += it; if (running > peak) peak = running }
        val zone = runCatching {
            ZoneId.of(selectedAccount?.resetTimezone ?: settings?.resetTimezone ?: "Africa/Nairobi")
        }.getOrDefault(ZoneId.of("Africa/Nairobi"))
        val today = LocalDate.now(zone)
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayPnl = closed.filter { (it.closedAt ?: 0L) >= dayStart }.mapNotNull { it.pnl?.toBigDecimalOrNull() }.fold(BigDecimal.ZERO, BigDecimal::add)
        val openRisk = journalDao.openEntries()
            .filter { selectedAccount == null || it.accountId == selectedAccount.id }
            .mapNotNull { it.riskAmount.toBigDecimalOrNull() }
            .fold(BigDecimal.ZERO, BigDecimal::add)
        return GuardrailEvaluator.evaluate(rules, AccountRiskState(starting, current, peak, todayPnl, openRisk), ProposedTrade(setup.riskAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO))
    }

    override suspend fun save(setup: PlannedSetup): String {
        val id = UUID.randomUUID().toString()
        val accountId = setup.accountId
            ?: prefs.selectedJournalAccountId.first()
            ?: accountDao.observeActive().first().firstOrNull()?.id
            ?: 1L
        journalDao.upsertEntry(JournalEntryEntity(
            id, System.currentTimeMillis(), setup.symbol, setup.assetClass, setup.direction,
            setup.entry, setup.stop, setup.stopIsPips, setup.takeProfit, setup.lots,
            setup.riskAmount, setup.riskPercent, setup.accountCurrency, "PLANNED",
            null, null, null, null, null, "", "",
            accountId = accountId,
            setupId = setup.setupId,
            checklistResult = setup.checklistResult
        ))
        val linked = articleDao.recentRelated(setup.symbol, System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        journalDao.addHeadlines(linked.map { JournalHeadlineEntity(id, it.id, it.title, it.url) })
        return id
    }
}
