package app.marketbrief.feature.calculator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marketbrief.core.risk.AssetClass
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.Direction
import app.marketbrief.core.risk.Instrument
import app.marketbrief.core.risk.PositionSizer
import app.marketbrief.core.risk.PriceLevel
import app.marketbrief.core.risk.RateSource
import app.marketbrief.core.risk.Rejection
import app.marketbrief.core.risk.RiskInput
import app.marketbrief.core.risk.SizingOutcome
import app.marketbrief.core.risk.SizingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState

    private val allInstruments = listOf(
        Instrument.forex("EURUSD"),
        Instrument.forex("GBPUSD"),
        Instrument.forex("USDJPY"),
        Instrument.forex("USDCHF"),
        Instrument.forex("USDCAD"),
        Instrument.forex("AUDUSD"),
        Instrument.forex("NZDUSD"),
        Instrument.forex("EURGBP"),
        Instrument.forex("EURJPY"),
        Instrument.forex("GBPJPY"),
        Instrument(
            symbol = "XAUUSD",
            assetClass = AssetClass.METAL,
            quoteCurrency = CurrencyCode("USD"),
            baseCurrency = CurrencyCode("XAU"),
            pipSize = BigDecimal("0.1"),
            contractSize = BigDecimal("100")
        )
    )

    init {
        _uiState.update { it.copy(availableInstruments = allInstruments) }
        if (allInstruments.isNotEmpty()) {
            onInstrumentSelected(allInstruments.first().symbol)
        }
    }

    fun onInstrumentSelected(symbol: String) {
        val instrument = allInstruments.find { it.symbol == symbol } ?: allInstruments.first()
        _uiState.update { it.copy(selectedInstrument = instrument) }
    }

    fun onBalanceChange(value: String) { _uiState.update { it.copy(balanceText = value) } }
    fun onAccountCurrencyChange(value: String) { _uiState.update { it.copy(accountCurrencyText = value.uppercase()) } }
    fun onRiskTypeChange(isPercent: Boolean) { _uiState.update { it.copy(riskIsPercent = isPercent) } }
    fun onRiskValueChange(value: String) { _uiState.update { it.copy(riskValueText = value) } }
    fun onDirectionChange(direction: Direction) { _uiState.update { it.copy(direction = direction) } }
    fun onEntryChange(value: String) { _uiState.update { it.copy(entryText = value) } }
    fun onStopChange(value: String) { _uiState.update { it.copy(stopText = value) } }
    fun onStopTypeChange(isPips: Boolean) { _uiState.update { it.copy(stopIsPips = isPips) } }
    fun onTakeProfitChange(value: String) { _uiState.update { it.copy(takeProfitText = value) } }
    fun onTakeProfitTypeChange(isPips: Boolean) { _uiState.update { it.copy(takeProfitIsPips = isPips) } }
    fun onLeverageChange(value: String) { _uiState.update { it.copy(leverageText = value) } }
    fun onRateOverrideChange(value: String) { _uiState.update { it.copy(rateOverrideText = value) } }

    fun calculate() {
        _uiState.update { it.copy(isCalculating = true, error = null, result = null) }

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

            val outcome = PositionSizer().size(request, SimpleRateSource())
            _uiState.update { it.copy(isCalculating = false) }

            when (outcome) {
                is SizingOutcome.Sized -> _uiState.update { it.copy(result = outcome.result) }
                is SizingOutcome.Rejected -> {
                    val message = when (val reason = outcome.reason) {
                        is Rejection.InvalidInput -> reason.message
                        is Rejection.RateUnavailable -> "Exchange rate unavailable: ${reason.from} to ${reason.to}"
                        is Rejection.BelowMinimumLot -> "Risk too small. Minimum lot (${reason.minLot.toPlainString()}) would risk ${reason.riskAtMinLot.toPlainString()}"
                    }
                    _uiState.update { it.copy(error = message) }
                }
            }
        }
    }

    private fun parseBigDecimal(text: String): BigDecimal? = try {
        BigDecimal(text.trim())
    } catch (_: NumberFormatException) {
        null
    }
}

class SimpleRateSource : RateSource {
    override fun rate(from: CurrencyCode, to: CurrencyCode): BigDecimal? {
        if (from == to) return BigDecimal.ONE
        val mockRates = mapOf(
            "EURUSD" to BigDecimal("1.0850"),
            "GBPUSD" to BigDecimal("1.2650"),
            "USDJPY" to BigDecimal("150.25"),
            "USDCHF" to BigDecimal("0.8950"),
            "USDCAD" to BigDecimal("1.3650"),
            "AUDUSD" to BigDecimal("0.6550"),
            "NZDUSD" to BigDecimal("0.6050"),
            "EURGBP" to BigDecimal("0.8570"),
            "EURJPY" to BigDecimal("163.00"),
            "GBPJPY" to BigDecimal("190.00"),
            "XAUUSD" to BigDecimal("2000.00")
        )

        val pair = "${from.code}${to.code}"
        val inversePair = "${to.code}${from.code}"
        val context = MathContext(34, RoundingMode.HALF_EVEN)
        return mockRates[pair] ?: mockRates[inversePair]?.let { BigDecimal.ONE.divide(it, context) }
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
    val isCalculating: Boolean = false,
    val result: SizingResult? = null,
    val error: String? = null
)