package app.marketbrief.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.risk.ClosedTradeResult
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.InstrumentCatalog
import app.marketbrief.core.risk.TradeStats
import app.marketbrief.core.data.RateRepository
import app.marketbrief.core.storage.db.AccountSettingsEntity
import app.marketbrief.core.storage.db.JournalDao
import app.marketbrief.core.storage.db.JournalEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.MathContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalUiState(
    val entries: List<JournalEntryEntity> = emptyList(),
    val stats: TradeStats = TradeStats.calculate(emptyList()),
    val account: AccountSettingsEntity? = null
)

@HiltViewModel
class JournalViewModel @Inject constructor(private val dao: JournalDao, private val rateRepository: RateRepository) : ViewModel() {
    val uiState: StateFlow<JournalUiState> = kotlinx.coroutines.flow.combine(
        dao.observeEntries(), dao.observeAccount()
    ) { entries, account ->
        val closed = entries.filter { it.status == "CLOSED" }.mapNotNull { entry ->
            val pnl = entry.pnl?.toBigDecimalOrNull() ?: return@mapNotNull null
            val r = entry.rMultiple?.toBigDecimalOrNull() ?: return@mapNotNull null
            ClosedTradeResult(pnl, r)
        }
        JournalUiState(entries, TradeStats.calculate(closed), account)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    fun setStatus(entry: JournalEntryEntity, status: String) {
        viewModelScope.launch { dao.upsertEntry(entry.copy(status = status, openedAt = if (status == "OPEN") System.currentTimeMillis() else entry.openedAt)) }
    }

    fun close(entry: JournalEntryEntity, exitPrice: String, pnlOverride: String?) {
        val exit = exitPrice.toBigDecimalOrNull() ?: return
        val pnl = pnlOverride?.toBigDecimalOrNull() ?: run {
            val open = entry.entry.toBigDecimalOrNull() ?: return
            val lots = entry.lots.toBigDecimalOrNull() ?: return
            val instrument = InstrumentCatalog.default().find(entry.symbol) ?: return
            val quoteCurrency = instrument.quoteCurrency
            val accountCurrency = CurrencyCode(entry.accountCurrency)
            val rate = if (quoteCurrency == accountCurrency) BigDecimal.ONE else rateRepository.getRate(quoteCurrency, accountCurrency)?.let(BigDecimal::valueOf) ?: return
            val move = if (entry.direction == "LONG") exit - open else open - exit
            move * lots * instrument.contractSize * rate
        }
        val risk = entry.riskAmount.toBigDecimalOrNull()?.takeIf { it.signum() != 0 } ?: return
        val r = pnl.divide(risk, MathContext.DECIMAL128)
        viewModelScope.launch { dao.upsertEntry(entry.copy(status = "CLOSED", closedAt = System.currentTimeMillis(), exitPrice = exit.toPlainString(), pnl = pnl.toPlainString(), rMultiple = r.toPlainString())) }
    }

    fun saveAccount(balance: String, currency: String, maxRisk: String, dailyLoss: String, maxOpen: String, maxDrawdown: String, mode: String, timezone: String) {
        if (balance.toBigDecimalOrNull() == null) return
        viewModelScope.launch {
            dao.saveAccount(AccountSettingsEntity(1, balance, currency, maxRisk, dailyLoss, maxOpen, maxDrawdown, mode, timezone))
        }
    }

    fun csv(): String {
        val entries = uiState.value.entries
        val header = "id,createdAt,symbol,direction,entry,stop,lots,riskAmount,currency,status,exitPrice,pnl,rMultiple,notes\n"
        return header + entries.joinToString("\n") { e ->
            listOf(e.id, e.createdAt, e.symbol, e.direction, e.entry, e.stop, e.lots, e.riskAmount, e.accountCurrency, e.status, e.exitPrice.orEmpty(), e.pnl.orEmpty(), e.rMultiple.orEmpty(), e.notes)
                .joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" }
        }
    }
}
