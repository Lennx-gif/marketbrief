package app.marketbrief.feature.calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.data.RateRepository
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.Direction
import app.marketbrief.core.risk.Instrument
import app.marketbrief.core.risk.PositionSizer
import app.marketbrief.core.risk.RateNeeds
import app.marketbrief.core.risk.PriceLevel
import app.marketbrief.core.risk.RateSource
import app.marketbrief.core.risk.Rejection
import app.marketbrief.core.risk.RiskConverter
import app.marketbrief.core.risk.RiskInput
import app.marketbrief.core.risk.SizingOutcome
import app.marketbrief.core.risk.SizingResult
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.JournalSetupWriter
import app.marketbrief.core.storage.PlannedSetup
import app.marketbrief.core.storage.db.PresetDao
import app.marketbrief.core.storage.db.PresetEntity
import app.marketbrief.core.storage.db.TradeLogDao
import app.marketbrief.core.storage.db.TradeLogEntity
import app.marketbrief.core.storage.db.JournalAccountEntity
import app.marketbrief.core.storage.db.TradeSetupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val rateRepository: RateRepository,
    private val presetDao: PresetDao,
    private val tradeLogDao: TradeLogDao,
    private val journalSetupWriter: JournalSetupWriter
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState

    private val allInstruments = app.marketbrief.core.risk.InstrumentCatalog.default().all()

    val presets: StateFlow<List<PresetEntity>> = presetDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tradeLog: StateFlow<List<TradeLogEntity>> = tradeLogDao.observeRecent(TRADE_LOG_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        _uiState.update { it.copy(availableInstruments = allInstruments) }
        viewModelScope.launch {
            val defaults = prefs.calculatorDefaults.first()
            val saved = allInstruments.find { it.symbol == defaults.instrumentSymbol }
            if (saved != null || defaults.instrumentSymbol.isBlank()) {
                _uiState.update {
                    it.copy(
                        selectedInstrument = saved ?: it.selectedInstrument,
                        accountCurrencyText = defaults.accountCurrency,
                        balanceText = defaults.balanceText,
                        riskIsPercent = defaults.riskIsPercent,
                        riskValueText = defaults.riskValueText
                    )
                }
            }
            val journalOptions = journalSetupWriter.options()
            _uiState.update {
                it.copy(
                    journalAccounts = journalOptions.accounts,
                    journalSetups = journalOptions.setups,
                    selectedJournalAccountId = journalOptions.selectedAccountId
                )
            }
        }
    }

    fun onInstrumentSelected(symbol: String) {
        val instrument = allInstruments.find { it.symbol == symbol } ?: return
        _uiState.update { it.copy(selectedInstrument = instrument) }
        viewModelScope.launch { prefs.saveCalculatorDefaults(instrumentSymbol = instrument.symbol) }
    }

    fun onBalanceChange(value: String) {
        if (value.startsWith("-")) return
        _uiState.update { it.copy(balanceText = value) }
        viewModelScope.launch { prefs.saveCalculatorDefaults(balanceText = value) }
    }

    fun onAccountCurrencyChange(value: String) { _uiState.update { it.copy(accountCurrencyText = value.uppercase()) } }

    fun onRiskTypeChange(isPercent: Boolean) {
        if (_uiState.value.riskIsPercent == isPercent) return
        val currentVal = parseBigDecimal(_uiState.value.riskValueText)
        val balance = parseBigDecimal(_uiState.value.balanceText)
        val newRiskText = if (currentVal != null && balance != null && balance > BigDecimal.ZERO) {
            if (isPercent) {
                RiskConverter.cashToPercent(balance, currentVal)?.toPlainString() ?: _uiState.value.riskValueText
            } else {
                RiskConverter.percentToCash(balance, currentVal)?.toPlainString() ?: _uiState.value.riskValueText
            }
        } else {
            _uiState.value.riskValueText
        }
        _uiState.update { it.copy(riskIsPercent = isPercent, riskValueText = newRiskText) }
        viewModelScope.launch {
            prefs.saveCalculatorDefaults(riskIsPercent = isPercent, riskValueText = newRiskText)
        }
    }

    fun onRiskValueChange(value: String) {
        if (value.startsWith("-")) return
        _uiState.update { it.copy(riskValueText = value) }
        viewModelScope.launch { prefs.saveCalculatorDefaults(riskValueText = value) }
    }
    fun onDirectionChange(direction: Direction) { _uiState.update { it.copy(direction = direction) } }
    fun onEntryChange(value: String) { _uiState.update { it.copy(entryText = value) } }
    fun onStopChange(value: String) { _uiState.update { it.copy(stopText = value) } }
    fun onStopTypeChange(isPips: Boolean) { _uiState.update { it.copy(stopIsPips = isPips) } }
    fun onTakeProfitChange(value: String) { _uiState.update { it.copy(takeProfitText = value) } }
    fun onTakeProfitTypeChange(isPips: Boolean) { _uiState.update { it.copy(takeProfitIsPips = isPips) } }
    fun onLeverageChange(value: String) { _uiState.update { it.copy(leverageText = value) } }
    fun onRateOverrideChange(value: String) { _uiState.update { it.copy(rateOverrideText = value) } }

    fun useLivePrice() {
        val symbol = _uiState.value.selectedInstrument.symbol
        viewModelScope.launch {
            val quote = rateRepository.getQuote(symbol)
            if (quote != null) _uiState.update { it.copy(entryText = BigDecimal.valueOf(quote).stripTrailingZeros().toPlainString()) }
            else _uiState.update { it.copy(error = "Live quote unavailable; enter the price manually") }
        }
    }

    fun selectJournalAccount(accountId: Long?) {
        _uiState.update { it.copy(selectedJournalAccountId = accountId) }
        accountId?.let { id ->
            viewModelScope.launch { prefs.setSelectedJournalAccountId(id) }
        }
    }

    fun selectJournalSetup(setupId: String?) {
        _uiState.update { it.copy(selectedJournalSetupId = setupId, selectedChecklistItems = emptySet()) }
    }

    fun toggleChecklistItem(item: String, checked: Boolean) {
        _uiState.update {
            it.copy(selectedChecklistItems = if (checked) it.selectedChecklistItems + item else it.selectedChecklistItems - item)
        }
    }

    fun showSaveSetupDialog() {
        _uiState.update { it.copy(showSaveJournalDialog = true) }
        viewModelScope.launch {
            val options = journalSetupWriter.options()
            val current = _uiState.value
            val setupId = current.selectedJournalSetupId
                ?.takeIf { id -> options.setups.any { it.id == id } }
            _uiState.update {
                it.copy(
                    journalAccounts = options.accounts,
                    journalSetups = options.setups,
                    selectedJournalAccountId = options.selectedAccountId,
                    selectedJournalSetupId = setupId,
                    selectedChecklistItems = if (setupId == current.selectedJournalSetupId) {
                        current.selectedChecklistItems
                    } else {
                        emptySet()
                    }
                )
            }
        }
    }

    fun dismissSaveSetupDialog() {
        _uiState.update { it.copy(showSaveJournalDialog = false) }
    }

    fun saveSetup(saveAnyway: Boolean = false) {
        val state = _uiState.value
        val result = state.result ?: return
        viewModelScope.launch {
            val selectedAccount = state.journalAccounts.firstOrNull { it.id == state.selectedJournalAccountId }
            if (selectedAccount == null) {
                _uiState.update { it.copy(setupMessage = "Choose an active journal account before saving this trade.") }
                return@launch
            }
            if (selectedAccount.currency.uppercase() != state.accountCurrencyText.uppercase()) {
                _uiState.update {
                    it.copy(
                        setupMessage = "Calculator currency is ${state.accountCurrencyText.uppercase()}, but ${selectedAccount.name} uses ${selectedAccount.currency}. Update the calculator currency and recalculate before saving."
                    )
                }
                return@launch
            }
            val selectedChecklist = state.journalSetups
                .firstOrNull { it.id == state.selectedJournalSetupId }
                ?.checklist
                ?.lines()
                ?.filter { it.isNotBlank() }
                .orEmpty()
            val checklistResult = if (selectedChecklist.isEmpty()) {
                ""
            } else {
                "${selectedChecklist.mapIndexed { index, item -> "$index:$item" }.count { it in state.selectedChecklistItems }}/${selectedChecklist.size}"
            }
            val planned = toPlannedSetup(state, result, checklistResult = checklistResult)
            val warnings = journalSetupWriter.evaluate(planned).map { it.message }
            if (warnings.isNotEmpty() && !saveAnyway) {
                _uiState.update {
                    it.copy(
                        guardrailWarnings = warnings,
                        setupMessage = "BLOCK: ${warnings.joinToString()}. Choose Save anyway to override."
                    )
                }
                return@launch
            }
            journalSetupWriter.save(planned)
            planned.accountId?.let { prefs.setSelectedJournalAccountId(it) }
            _uiState.update {
                it.copy(showSaveJournalDialog = false, setupMessage = "Setup saved to journal.")
            }
        }
    }

    private fun toPlannedSetup(
        state: CalculatorUiState,
        result: SizingResult,
        accountId: Long? = state.selectedJournalAccountId,
        setupId: String? = state.selectedJournalSetupId,
        checklistResult: String = ""
    ) = PlannedSetup(
        symbol = state.selectedInstrument.symbol,
        assetClass = state.selectedInstrument.assetClass.name,
        direction = state.direction.name,
        entry = state.entryText,
        stop = state.stopText,
        stopIsPips = state.stopIsPips,
        takeProfit = state.takeProfitText.takeIf { it.isNotBlank() },
        lots = result.lots.toPlainString(),
        riskAmount = result.actualRisk.toPlainString(),
        riskPercent = result.actualRiskPercent.toPlainString(),
        accountCurrency = state.accountCurrencyText,
        accountId = accountId,
        setupId = setupId,
        checklistResult = checklistResult
    )

    fun calculate() {
        _uiState.update { it.copy(isCalculating = true, error = null, result = null, rateUsed = null, rateAsOf = null) }

        viewModelScope.launch {
            val state = _uiState.value
            val instrument = state.selectedInstrument

            val balance = parseBigDecimal(state.balanceText) ?: run {
                _uiState.update { it.copy(isCalculating = false, error = "Invalid balance") }
                return@launch
            }

            val accountCurrency = CurrencyCode(state.accountCurrencyText)
            val entry = parseBigDecimal(state.entryText) ?: run {
                _uiState.update { it.copy(isCalculating = false, error = "Invalid entry price") }
                return@launch
            }

            val riskInput: RiskInput = if (state.riskIsPercent) {
                val percent = parseBigDecimal(state.riskValueText) ?: run {
                    _uiState.update { it.copy(isCalculating = false, error = "Invalid risk percent") }
                    return@launch
                }
                RiskInput.PercentOfBalance(percent)
            } else {
                val amount = parseBigDecimal(state.riskValueText) ?: run {
                    _uiState.update { it.copy(isCalculating = false, error = "Invalid risk amount") }
                    return@launch
                }
                RiskInput.FixedAmount(amount)
            }

            val stopValue = parseBigDecimal(state.stopText) ?: run {
                _uiState.update { it.copy(isCalculating = false, error = "Invalid stop value") }
                return@launch
            }
            val stop: PriceLevel = if (state.stopIsPips) {
                PriceLevel.PipsFromEntry(stopValue)
            } else {
                PriceLevel.AtPrice(stopValue)
            }

            val takeProfit: PriceLevel? = if (state.takeProfitText.isNotBlank()) {
                val tpValue = parseBigDecimal(state.takeProfitText) ?: run {
                    _uiState.update { it.copy(isCalculating = false, error = "Invalid take profit value") }
                    return@launch
                }
                if (state.takeProfitIsPips) {
                    PriceLevel.PipsFromEntry(tpValue)
                } else {
                    PriceLevel.AtPrice(tpValue)
                }
            } else null

            val leverage = if (state.leverageText.isNotBlank()) parseBigDecimal(state.leverageText) else null
            val request = app.marketbrief.core.risk.SizingRequest(
                instrument = instrument,
                accountCurrency = accountCurrency,
                balance = balance,
                risk = riskInput,
                direction = state.direction,
                entry = entry,
                stop = stop,
                takeProfit = takeProfit,
                leverage = leverage
            )

            rateRepository.refresh(RateNeeds.forTrade(instrument, accountCurrency))
            val rateSource = buildRateSource(state, instrument, accountCurrency)
            val outcome = PositionSizer().size(request, rateSource)

            when (outcome) {
                is SizingOutcome.Sized -> {
                    persistDefaults(state)
                    logTrade(state, outcome.result, accountCurrency)
                    showRateUsed(state, instrument, accountCurrency)
                    val warnings = journalSetupWriter.evaluate(toPlannedSetup(state, outcome.result)).map { it.message }
                    _uiState.update { it.copy(isCalculating = false, result = outcome.result, guardrailWarnings = warnings, setupMessage = null) }
                }
                is SizingOutcome.Rejected -> {
                    val message = when (val reason = outcome.reason) {
                        is Rejection.InvalidInput -> reason.message
                        is Rejection.RateUnavailable -> "Exchange rate unavailable: ${reason.from} to ${reason.to}"
                        is Rejection.BelowMinimumLot -> "Risk too small. Minimum lot (${reason.minLot.toPlainString()}) would risk ${reason.riskAtMinLot.toPlainString()}"
                    }
                    _uiState.update { it.copy(isCalculating = false, error = message) }
                }
            }
        }
    }

    /** Live table first, the static majors as offline fallback; a manual override wins over both. */
    private fun buildRateSource(state: CalculatorUiState, instrument: Instrument, account: CurrencyCode): RateSource {
        val base: RateSource = LiveRateSource(rateRepository)
        val override = parseBigDecimal(state.rateOverrideText)
        return if (override != null && override.signum() > 0) {
            OverrideRateSource(base, instrument.quoteCurrency, account, override)
        } else {
            base
        }
    }

    private suspend fun persistDefaults(state: CalculatorUiState) {
        prefs.saveCalculatorDefaults(
            accountCurrency = state.accountCurrencyText,
            balanceText = state.balanceText,
            riskIsPercent = state.riskIsPercent,
            riskValueText = state.riskValueText
        )
    }

    private suspend fun logTrade(state: CalculatorUiState, result: SizingResult, accountCurrency: CurrencyCode) {
        runCatching {
            tradeLogDao.insert(
                TradeLogEntity(
                    symbol = state.selectedInstrument.symbol,
                    direction = state.direction.name,
                    entryText = state.entryText,
                    stopText = state.stopText,
                    stopIsPips = state.stopIsPips,
                    takeProfitText = state.takeProfitText,
                    lots = result.lots.toPlainString(),
                    riskAmount = result.actualRisk.toPlainString(),
                    accountCurrency = accountCurrency.code
                )
            )
        }
    }

    private suspend fun showRateUsed(state: CalculatorUiState, instrument: Instrument, account: CurrencyCode) {
        val override = parseBigDecimal(state.rateOverrideText)
        if (override != null && override.signum() > 0) {
            _uiState.update { it.copy(rateUsed = override, rateAsOf = null, rateIsStale = false) }
        } else {
            val live = rateRepository.getRate(instrument.quoteCurrency, account)?.let { BigDecimal.valueOf(it) }
            val asOf = rateRepository.lastUpdated.first().takeIf { it > 0 }
            _uiState.update { it.copy(rateUsed = live, rateAsOf = asOf, rateIsStale = asOf != null && System.currentTimeMillis() - asOf > 60 * 60 * 1000L) }
        }
    }

    fun onShowSavePreset() {
        _uiState.update { it.copy(showSavePresetDialog = true, presetNameText = it.selectedInstrument.symbol) }
    }

    fun dismissSavePreset() {
        _uiState.update { it.copy(showSavePresetDialog = false) }
    }

    fun onPresetNameChange(name: String) {
        _uiState.update { it.copy(presetNameText = name) }
    }

    fun savePreset() {
        val state = _uiState.value
        if (state.presetNameText.isBlank()) return
        viewModelScope.launch {
            presetDao.insert(
                PresetEntity(
                    name = state.presetNameText.trim(),
                    instrumentSymbol = state.selectedInstrument.symbol,
                    accountCurrency = state.accountCurrencyText,
                    balanceText = state.balanceText,
                    riskIsPercent = state.riskIsPercent,
                    riskValueText = state.riskValueText,
                    leverageText = state.leverageText
                )
            )
            _uiState.update { it.copy(showSavePresetDialog = false) }
        }
    }

    fun applyPreset(preset: PresetEntity) {
        val instrument = allInstruments.find { it.symbol == preset.instrumentSymbol } ?: return
        _uiState.update {
            it.copy(
                selectedInstrument = instrument,
                accountCurrencyText = preset.accountCurrency,
                balanceText = preset.balanceText,
                riskIsPercent = preset.riskIsPercent,
                riskValueText = preset.riskValueText,
                leverageText = preset.leverageText
            )
        }
        viewModelScope.launch { prefs.saveCalculatorDefaults(instrumentSymbol = instrument.symbol) }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch { presetDao.delete(preset.id) }
    }

    fun deleteLogEntry(entry: TradeLogEntity) {
        viewModelScope.launch { tradeLogDao.delete(entry.id) }
    }

    private fun parseBigDecimal(text: String): BigDecimal? = try {
        BigDecimal(text.trim())
    } catch (_: NumberFormatException) {
        null
    }

    private companion object {
        const val TRADE_LOG_LIMIT = 20
    }
}

/** Serves rates from the live table and falls back to the static majors when offline. */
private class LiveRateSource(private val rateRepository: RateRepository) : RateSource {
    override fun rate(from: CurrencyCode, to: CurrencyCode): BigDecimal? =
        rateRepository.getRate(from, to)?.toBigDecimal()
}

/** Applies the user's manual quote-to-account rate; every other leg goes through [base]. */
private class OverrideRateSource(
    private val base: RateSource,
    private val quote: CurrencyCode,
    private val account: CurrencyCode,
    private val override: BigDecimal
) : RateSource {
    override fun rate(from: CurrencyCode, to: CurrencyCode): BigDecimal? = when {
        from == quote && to == account -> override
        from == account && to == quote -> BigDecimal.ONE.divide(override, MathContext.DECIMAL128)
        else -> base.rate(from, to)
    }
}

data class CalculatorUiState(
    val availableInstruments: List<Instrument> = emptyList(),
    val selectedInstrument: Instrument = Instrument.forex("EURUSD"),
    val balanceText: String = "10000",
    val accountCurrencyText: String = "USD",
    val riskIsPercent: Boolean = true,
    val riskValueText: String = "1",
    val direction: Direction = Direction.LONG,
    val entryText: String = "",
    val stopText: String = "",
    val stopIsPips: Boolean = true,
    val takeProfitText: String = "",
    val takeProfitIsPips: Boolean = true,
    val leverageText: String = "30",
    val rateOverrideText: String = "",
    val guardrailWarnings: List<String> = emptyList(),
    val setupMessage: String? = null,
    val journalAccounts: List<JournalAccountEntity> = emptyList(),
    val journalSetups: List<TradeSetupEntity> = emptyList(),
    val selectedJournalAccountId: Long? = null,
    val selectedJournalSetupId: String? = null,
    val selectedChecklistItems: Set<String> = emptySet(),
    val showSaveJournalDialog: Boolean = false,
    val rateIsStale: Boolean = false,
    val isCalculating: Boolean = false,
    val result: SizingResult? = null,
    val error: String? = null,
    val rateUsed: BigDecimal? = null,
    val rateAsOf: Long? = null,
    val showSavePresetDialog: Boolean = false,
    val presetNameText: String = ""
)
