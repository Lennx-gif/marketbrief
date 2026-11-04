package app.marketbrief.feature.calculator

import app.marketbrief.core.data.RateRepository
import app.marketbrief.core.risk.CurrencyCode
import app.marketbrief.core.risk.PairRateTable
import app.marketbrief.core.storage.CalculatorDefaults
import app.marketbrief.core.storage.UserPrefsRepository
import app.marketbrief.core.storage.JournalSetupWriter
import app.marketbrief.core.storage.PlannedSetup
import app.marketbrief.core.storage.db.PresetDao
import app.marketbrief.core.storage.db.PresetEntity
import app.marketbrief.core.storage.db.TradeLogDao
import app.marketbrief.core.storage.db.TradeLogEntity
import app.marketbrief.feature.calculator.ui.CalculatorViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val defaultDefaults = CalculatorDefaults(
        instrumentSymbol = "EURUSD",
        accountCurrency = "USD",
        balanceText = "10000",
        riskIsPercent = true,
        riskValueText = "1"
    )

    private lateinit var prefs: UserPrefsRepository
    private lateinit var rateRepository: FakeRateRepository
    private lateinit var presetDao: FakePresetDao
    private lateinit var tradeLogDao: FakeTradeLogDao
    private lateinit var journalSetupWriter: FakeJournalSetupWriter

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        prefs = mockk(relaxed = true)
        every { prefs.calculatorDefaults } returns MutableStateFlow(defaultDefaults)
        coEvery { prefs.saveCalculatorDefaults(any(), any(), any(), any(), any()) } just runs
        rateRepository = FakeRateRepository()
        presetDao = FakePresetDao()
        tradeLogDao = FakeTradeLogDao()
        journalSetupWriter = FakeJournalSetupWriter()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CalculatorViewModel(prefs, rateRepository, presetDao, tradeLogDao, journalSetupWriter)

    @Test
    fun testInitialStateUsesPersistedDefaults() = runTest {
        val viewModel = viewModel()

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
        val viewModel = viewModel()

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
        val viewModel = viewModel()

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
        val viewModel = viewModel()
        viewModel.onBalanceChange("not_a_number")
        viewModel.calculate()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertNull(state.result)
    }

    @Test
    fun testSuccessfulCalculateLogsTrade() = runTest {
        val viewModel = viewModel()
        viewModel.onBalanceChange("10000")
        viewModel.onRiskValueChange("2")
        viewModel.onStopChange("50")
        viewModel.onEntryChange("1.0850")

        viewModel.calculate()

        assertEquals(1, tradeLogDao.items.size)
        val entry = tradeLogDao.items.single()
        assertEquals("EURUSD", entry.symbol)
        assertEquals("LONG", entry.direction)
        assertEquals("1.0850", entry.entryText)
        assertTrue(entry.lots.toBigDecimal() > BigDecimal.ZERO)
    }

    @Test
    fun testFailedCalculateDoesNotLogTrade() = runTest {
        val viewModel = viewModel()
        viewModel.onBalanceChange("not_a_number")

        viewModel.calculate()

        assertEquals(0, tradeLogDao.items.size)
    }

    @Test
    fun testSuccessfulCalculatePersistsDefaults() = runTest {
        val viewModel = viewModel()
        viewModel.onBalanceChange("25000")
        viewModel.onRiskValueChange("0.5")
        viewModel.onEntryChange("1.0850")
        viewModel.onStopChange("30")

        viewModel.calculate()

        coVerify {
            prefs.saveCalculatorDefaults(
                instrumentSymbol = null,
                accountCurrency = "USD",
                balanceText = "25000",
                riskIsPercent = true,
                riskValueText = "0.5"
            )
        }
    }

    @Test
    fun testInstrumentSelectionPersistsLastInstrument() = runTest {
        val viewModel = viewModel()

        viewModel.onInstrumentSelected("USDJPY")

        coVerify { prefs.saveCalculatorDefaults(instrumentSymbol = "USDJPY") }
    }

    @Test
    fun testRateUsedComesFromLiveTable() = runTest {
        val viewModel = viewModel()
        viewModel.onEntryChange("1.0850")
        viewModel.onStopChange("50")

        viewModel.calculate()

        val state = viewModel.uiState.value
        assertNotNull(state.result)
        assertEquals(0, state.rateUsed!!.compareTo(BigDecimal.ONE))
        assertEquals(TEST_AS_OF, state.rateAsOf)
    }

    @Test
    fun testManualRateOverrideWins() = runTest {
        val viewModel = viewModel()
        viewModel.onEntryChange("1.0850")
        viewModel.onStopChange("50")
        viewModel.onRateOverrideChange("1.10")

        viewModel.calculate()

        val state = viewModel.uiState.value
        assertNotNull(state.result)
        assertEquals(0, state.rateUsed!!.compareTo(BigDecimal("1.10")))
        assertNull(state.rateAsOf)
    }

    @Test
    fun testSaveAndApplyPreset() = runTest {
        val viewModel = viewModel()
        viewModel.onBalanceChange("5000")
        viewModel.onRiskValueChange("0.5")
        viewModel.onPresetNameChange("Scalp")
        viewModel.savePreset()

        assertEquals(1, presetDao.items.size)
        val preset = presetDao.items.single()
        assertEquals("Scalp", preset.name)
        assertEquals("EURUSD", preset.instrumentSymbol)

        viewModel.onBalanceChange("99999")
        viewModel.onRiskValueChange("3")
        viewModel.applyPreset(preset)

        val state = viewModel.uiState.value
        assertEquals("5000", state.balanceText)
        assertEquals("0.5", state.riskValueText)
    }

    @Test
    fun testRefreshesRatesWhenCalculating() = runTest {
        val viewModel = viewModel()
        viewModel.onEntryChange("1.0850")
        viewModel.onStopChange("50")
        viewModel.calculate()
        assertTrue(rateRepository.refreshCount >= 1)
    }
}

private const val TEST_AS_OF = 1_700_000_000_000L

private class FakeRateRepository : RateRepository {
    private val table = MutableStateFlow(
        PairRateTable(mapOf("EURUSD" to BigDecimal("1.0850"), "USDJPY" to BigDecimal("150.25")))
    )
    override val rates: MutableStateFlow<PairRateTable> = table
    override val lastUpdated: MutableStateFlow<Long> = MutableStateFlow(TEST_AS_OF)
    var refreshCount = 0

    override suspend fun refresh(requiredPairs: Set<String>) {
        refreshCount++
    }

    override fun getRate(from: CurrencyCode, to: CurrencyCode): Double? =
        table.value.rate(from, to)?.toDouble()
}

private class FakeJournalSetupWriter : JournalSetupWriter {
    val saved = mutableListOf<PlannedSetup>()
    override suspend fun save(setup: PlannedSetup): String { saved += setup; return "entry-${saved.size}" }
}

private class FakePresetDao : PresetDao {
    val items = mutableListOf<PresetEntity>()
    private val flow = MutableStateFlow<List<PresetEntity>>(emptyList())

    override fun observeAll(): Flow<List<PresetEntity>> = flow

    override suspend fun insert(preset: PresetEntity): Long {
        val id = (items.maxOfOrNull { it.id } ?: 0L) + 1
        items.add(preset.copy(id = id))
        flow.value = items.toList()
        return id
    }

    override suspend fun delete(id: Long) {
        items.removeAll { it.id == id }
        flow.value = items.toList()
    }
}

private class FakeTradeLogDao : TradeLogDao {
    val items = mutableListOf<TradeLogEntity>()
    private val flow = MutableStateFlow<List<TradeLogEntity>>(emptyList())

    override fun observeRecent(limit: Int): Flow<List<TradeLogEntity>> = flow

    override suspend fun insert(entry: TradeLogEntity) {
        items.add(entry.copy(id = items.size + 1L))
        flow.value = items.toList()
    }

    override suspend fun delete(id: Long) {
        items.removeAll { it.id == id }
        flow.value = items.toList()
    }

    override suspend fun clearAll() {
        items.clear()
        flow.value = emptyList()
    }
}
