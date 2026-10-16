package app.marketbrief.feature.calculator

import app.marketbrief.feature.calculator.ui.CalculatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        val viewModel = CalculatorViewModel()
        
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals("EURUSD", state.selectedInstrument.symbol)
        assertEquals("10000", state.balanceText)
        assertEquals("USD", state.accountCurrencyText)
        assertTrue(state.riskIsPercent)
        assertEquals("1", state.riskValueText)
        assertNull(state.result)
        assertNull(state.error)
    }

    @Test
    fun testCalculatePositionSize() = runTest {
        val viewModel = CalculatorViewModel()
        
        viewModel.onInstrumentSelected("EURUSD")
        viewModel.onBalanceChange("10000")
        viewModel.onRiskTypeChange(true)
        viewModel.onRiskValueChange("2")
        viewModel.onStopTypeChange(true)
        viewModel.onStopChange("50")
        viewModel.onEntryChange("1.0850")
        viewModel.onAccountCurrencyChange("USD")
        
        viewModel.calculate()
        
        val state = viewModel.uiState.value
        assertNotNull(state.result)
        assertNull(state.error)
        
        val result = state.result!!
        assertEquals("EURUSD", state.selectedInstrument.symbol)
        assertTrue(result.lots > java.math.BigDecimal.ZERO)
    }

    @Test
    fun testCalculateWithJpyPair() = runTest {
        val viewModel = CalculatorViewModel()
        
        viewModel.onInstrumentSelected("USDJPY")
        viewModel.onBalanceChange("10000")
        viewModel.onRiskValueChange("2")
        viewModel.onStopTypeChange(true)
        viewModel.onStopChange("50")
        viewModel.onEntryChange("150.25")
        viewModel.onAccountCurrencyChange("USD")
        
        viewModel.calculate()
        
        val state = viewModel.uiState.value
        assertNotNull(state.result)
        assertNull(state.error)
        val result = state.result!!
        assertEquals("USDJPY", state.selectedInstrument.symbol)
        assertTrue(result.lots > java.math.BigDecimal.ZERO)
    }

    @Test
    fun testInvalidInput() = runTest {
        val viewModel = CalculatorViewModel()
        viewModel.onBalanceChange("not_a_number")
        viewModel.calculate()
        
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertNull(state.result)
    }
}