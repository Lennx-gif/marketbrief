package app.marketbrief.core.storage

import app.marketbrief.core.storage.db.ArticleDao
import app.marketbrief.core.storage.db.JournalDao
import app.marketbrief.core.storage.db.JournalEntryEntity
import app.marketbrief.core.storage.db.JournalHeadlineEntity
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
    val riskAmount: String, val riskPercent: String, val accountCurrency: String
)

interface JournalSetupWriter {
    suspend fun evaluate(setup: PlannedSetup): List<Violation> = emptyList()
    suspend fun save(setup: PlannedSetup): String
}

class RoomJournalSetupWriter @Inject constructor(
    private val journalDao: JournalDao,
    private val articleDao: ArticleDao
) : JournalSetupWriter {
    override suspend fun evaluate(setup: PlannedSetup): List<Violation> {
        val settings = journalDao.observeAccount().first()
        val starting = settings?.startingBalance?.toBigDecimalOrNull() ?: BigDecimal("10000")
        val rules = RiskRules(
            settings?.maxRiskPerTradePercent?.toBigDecimalOrNull() ?: BigDecimal("1"),
            settings?.dailyLossLimitPercent?.toBigDecimalOrNull() ?: BigDecimal("3"),
            settings?.maxOpenRiskPercent?.toBigDecimalOrNull() ?: BigDecimal("5"),
            settings?.maxDrawdownPercent?.toBigDecimalOrNull() ?: BigDecimal("10"),
            if (settings?.drawdownMode == "TRAILING") DrawdownMode.TRAILING else DrawdownMode.STATIC
        )
        val closed = journalDao.closedEntries().sortedBy { it.closedAt ?: Long.MAX_VALUE }
        val pnl = closed.mapNotNull { it.pnl?.toBigDecimalOrNull() }
        val current = pnl.fold(starting) { a, b -> a + b }
        var peak = starting
        var running = starting
        pnl.forEach { running += it; if (running > peak) peak = running }
        val zone = runCatching { ZoneId.of(settings?.resetTimezone ?: "Africa/Nairobi") }.getOrDefault(ZoneId.of("Africa/Nairobi"))
        val today = LocalDate.now(zone)
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayPnl = closed.filter { (it.closedAt ?: 0L) >= dayStart }.mapNotNull { it.pnl?.toBigDecimalOrNull() }.fold(BigDecimal.ZERO, BigDecimal::add)
        val openRisk = journalDao.openEntries().mapNotNull { it.riskAmount.toBigDecimalOrNull() }.fold(BigDecimal.ZERO, BigDecimal::add)
        return GuardrailEvaluator.evaluate(rules, AccountRiskState(starting, current, peak, todayPnl, openRisk), ProposedTrade(setup.riskAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO))
    }

    override suspend fun save(setup: PlannedSetup): String {
        val id = UUID.randomUUID().toString()
        journalDao.upsertEntry(JournalEntryEntity(
            id, System.currentTimeMillis(), setup.symbol, setup.assetClass, setup.direction,
            setup.entry, setup.stop, setup.stopIsPips, setup.takeProfit, setup.lots,
            setup.riskAmount, setup.riskPercent, setup.accountCurrency, "PLANNED",
            null, null, null, null, null, "", ""
        ))
        val linked = articleDao.recentRelated(setup.symbol, System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        journalDao.addHeadlines(linked.map { JournalHeadlineEntity(id, it.id, it.title, it.url) })
        return id
    }
}
