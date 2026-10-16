package app.marketbrief.core.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

private fun bd(s: String) = BigDecimal(s)

/** BigDecimal.equals compares scale too, so compare numerically. */
private fun assertNum(expected: String, actual: BigDecimal?, label: String = "") {
    assertNotNull("$label was null", actual)
    assertTrue("$label: expected $expected but was $actual", bd(expected).compareTo(actual) == 0)
}

private fun SizingOutcome.sized(): SizingResult =
    (this as? SizingOutcome.Sized)?.result ?: throw AssertionError("Expected Sized but was $this")

private fun SizingOutcome.rejection(): Rejection =
    (this as? SizingOutcome.Rejected)?.reason ?: throw AssertionError("Expected Rejected but was $this")

// Prices are deliberately round numbers so expected values can be checked by hand.
// EURUSD here is only used when converting to a EUR account, not as a trade entry.
private val rates = PairRateTable(
    mapOf(
        "USDJPY" to bd("150.00"),
        "USDCHF" to bd("0.9000"),
        "EURUSD" to bd("1.1000"),
        "GBPUSD" to bd("1.2500"),
    )
)

private val sizer = PositionSizer()

private fun request(
    instrument: Instrument,
    entry: String,
    stop: PriceLevel,
    direction: Direction = Direction.LONG,
    balance: String = "10000",
    risk: RiskInput = RiskInput.PercentOfBalance(bd("1")),
    account: String = "USD",
    takeProfit: PriceLevel? = null,
    leverage: String? = null,
) = SizingRequest(
    instrument = instrument,
    accountCurrency = CurrencyCode(account),
    balance = bd(balance),
    risk = risk,
    direction = direction,
    entry = bd(entry),
    stop = stop,
    takeProfit = takeProfit,
    leverage = leverage?.let { bd(it) },
)

private fun price(s: String) = PriceLevel.AtPrice(bd(s))
private fun pips(s: String) = PriceLevel.PipsFromEntry(bd(s))

private val eurusd = Instrument.forex("EURUSD")
private val usdjpy = Instrument.forex("USDJPY")

class PositionSizerTest {

    @Test
    fun inexactRateInversion_doesNotShaveOffALotStep() {
        // Regression: 1/150 is not exact, and for these inputs the raw lot size comes out as
        // 0.2999999999999999999999999999999999 (and similar) even though the true answer is exactly
        // 0.30. Without normalising before flooring, these size to 0.29, 0.02 and 0.44.
        val cases = listOf(
            Triple("149.95", "1", "0.30"),   // 5-pip stop
            Triple("149.50", "1", "0.03"),   // 50-pip stop
            Triple("149.95", "1.5", "0.45"), // 5-pip stop, 1.5% risk
        )
        for ((stop, riskPercent, expectedLots) in cases) {
            val r = sizer.size(
                request(
                    usdjpy, "150.00", price(stop),
                    balance = "1000",
                    risk = RiskInput.PercentOfBalance(bd(riskPercent)),
                ),
                rates,
            ).sized()
            assertNum(expectedLots, r.lots, "lots for stop $stop at $riskPercent%")
            assertNum(if (riskPercent == "1.5") "15.00" else "10.00", r.actualRisk, "actualRisk for stop $stop")
        }
    }

    @Test
    fun usdjpy_usdAccount_exactBoundary() {
        // 100 USD / (20,000 JPY loss per lot / 150) is exactly 0.75 lots.
        val r = sizer.size(request(usdjpy, "150.00", price("149.80")), rates).sized()
        assertNum("0.75", r.lots, "lots")
        assertNum("75000", r.units, "units")
        assertNum("100.00", r.targetRisk, "targetRisk")
        assertNum("100.00", r.actualRisk, "actualRisk")
        assertNum("20.0", r.stopPips, "stopPips")
        assertNum("6.6667", r.pipValuePerLot, "pipValuePerLot")
        assertTrue(r.warnings.isEmpty())
    }

    @Test
    fun usdchf_short_roundsDownAndRiskAtThresholdDoesNotWarn() {
        val r = sizer.size(
            request(
                Instrument.forex("USDCHF"), "0.9000", price("0.9040"),
                direction = Direction.SHORT,
                balance = "5000",
                risk = RiskInput.PercentOfBalance(bd("2")),
            ),
            rates,
        ).sized()
        assertNum("0.22", r.lots, "lots")
        assertNum("100.00", r.targetRisk, "targetRisk")
        assertNum("97.78", r.actualRisk, "actualRisk")
        assertNum("11.1111", r.pipValuePerLot, "pipValuePerLot")
        assertNum("40.0", r.stopPips, "stopPips")
        assertTrue("2% is the threshold, not above it", r.warnings.isEmpty())
    }

    @Test
    fun eurusd_quoteIsAccountCurrency() {
        val r = sizer.size(request(eurusd, "1.0850", price("1.0820")), rates).sized()
        assertNum("0.33", r.lots, "lots")
        assertNum("99.00", r.actualRisk, "actualRisk")
        assertNum("10.0000", r.pipValuePerLot, "pipValuePerLot")
        assertNum("30.0", r.stopPips, "stopPips")
        assertNum("0.99", r.actualRiskPercent, "actualRiskPercent")
    }

    @Test
    fun cross_eurgbp_convertsGbpLossToUsd() {
        val r = sizer.size(request(Instrument.forex("EURGBP"), "0.8600", price("0.8570")), rates).sized()
        assertNum("0.26", r.lots, "lots")
        assertNum("97.50", r.actualRisk, "actualRisk")
        assertNum("12.5000", r.pipValuePerLot, "pipValuePerLot")
    }

    @Test
    fun eurAccount_triangulatesJpyThroughUsd_exactBoundary() {
        // JPY to EUR = 1 / (150 * 1.10) = 1/165. 100 EUR risk / (50,000 JPY loss per lot / 165) = exactly 0.33.
        val r = sizer.size(
            request(usdjpy, "150.00", price("149.50"), account = "EUR"),
            rates,
        ).sized()
        assertNum("0.33", r.lots, "lots")
        assertNum("100.00", r.actualRisk, "actualRisk")
    }

    @Test
    fun short_withTakeProfitAndLeverage() {
        val r = sizer.size(
            request(
                eurusd, "1.0850", price("1.0880"),
                direction = Direction.SHORT,
                takeProfit = price("1.0760"),
                leverage = "30",
            ),
            rates,
        ).sized()
        assertNum("0.33", r.lots, "lots")
        assertNum("90.0", r.takeProfitPips, "takeProfitPips")
        assertNum("3.00", r.rewardRisk, "rewardRisk")
        assertNum("297.00", r.potentialProfit, "potentialProfit")
        assertNum("1193.50", r.marginRequired, "marginRequired")
    }

    @Test
    fun pipInputs_resolveToPrices_longAndShort() {
        val long = sizer.size(
            request(eurusd, "1.0850", pips("30"), takeProfit = pips("60")),
            rates,
        ).sized()
        assertNum("1.0820", long.stopPrice, "long stopPrice")
        assertNum("1.0910", long.takeProfitPrice, "long takeProfitPrice")
        assertNum("0.33", long.lots, "long lots")
        assertNum("2.00", long.rewardRisk, "long rewardRisk")

        val short = sizer.size(
            request(eurusd, "1.0850", pips("30"), direction = Direction.SHORT, takeProfit = pips("60")),
            rates,
        ).sized()
        assertNum("1.0880", short.stopPrice, "short stopPrice")
        assertNum("1.0790", short.takeProfitPrice, "short takeProfitPrice")
    }

    @Test
    fun metal_usesSamePathWithDifferentInstrument() {
        val gold = InstrumentCatalog.default().find("XAUUSD")
        assertNotNull(gold)
        val r = sizer.size(request(gold!!, "2000.00", price("1995.00")), rates).sized()
        assertNum("0.20", r.lots, "lots")
        assertNum("20", r.units, "units")
        assertNum("100.00", r.actualRisk, "actualRisk")
        assertNum("50.0", r.stopPips, "stopPips")
        assertNum("10.0000", r.pipValuePerLot, "pipValuePerLot")
    }

    @Test
    fun fixedAmountRisk_matchesEquivalentPercent() {
        val r = sizer.size(
            request(eurusd, "1.0850", price("1.0820"), risk = RiskInput.FixedAmount(bd("100"))),
            rates,
        ).sized()
        assertNum("0.33", r.lots, "lots")
        assertNum("100.00", r.targetRisk, "targetRisk")
    }

    @Test
    fun customLotStep_isRespected() {
        val coarse = Instrument.forex("EURUSD", lotStep = bd("0.1"), minLot = bd("0.1"))
        val r = sizer.size(request(coarse, "1.0850", price("1.0820")), rates).sized()
        assertNum("0.3", r.lots, "lots")
        assertNum("90.00", r.actualRisk, "actualRisk")
    }

    @Test
    fun highRisk_addsWarning() {
        val r = sizer.size(
            request(eurusd, "1.0850", price("1.0820"), risk = RiskInput.PercentOfBalance(bd("3"))),
            rates,
        ).sized()
        val warning = r.warnings.filterIsInstance<SizingWarning.HighRisk>().single()
        assertNum("3.00", warning.riskPercent, "warning percent")
    }

    @Test
    fun hugeSize_isCappedAtMaxLotWithWarning() {
        val r = sizer.size(
            request(eurusd, "1.0850", pips("1"), balance = "10000000"),
            rates,
        ).sized()
        assertNum("100", r.lots, "lots")
        assertNum("1000.00", r.actualRisk, "actualRisk")
        assertTrue(r.warnings.any { it is SizingWarning.CappedAtMaxLot })
    }

    @Test
    fun budgetBelowMinimumLot_isRejectedWithWhatMinimumWouldRisk() {
        val rejection = sizer.size(
            request(eurusd, "1.0850", price("1.0820"), balance = "100"),
            rates,
        ).rejection() as Rejection.BelowMinimumLot
        assertNum("0.00", rejection.affordableLots, "affordableLots")
        assertNum("0.01", rejection.minLot, "minLot")
        assertNum("3.00", rejection.riskAtMinLot, "riskAtMinLot")
    }

    @Test
    fun missingRate_isRejected() {
        val onlyGbp = PairRateTable(mapOf("GBPUSD" to bd("1.25")))
        val rejection = sizer.size(request(usdjpy, "150.00", price("149.80"), account = "EUR"), onlyGbp).rejection()
        assertEquals(Rejection.RateUnavailable(CurrencyCode("JPY"), CurrencyCode("EUR")), rejection)
    }

    @Test
    fun stopOnWrongSide_isRejected() {
        assertTrue(
            sizer.size(request(eurusd, "1.0850", price("1.0900")), rates).rejection() is Rejection.InvalidInput
        )
        assertTrue(
            sizer.size(
                request(eurusd, "1.0850", price("1.0800"), direction = Direction.SHORT), rates
            ).rejection() is Rejection.InvalidInput
        )
        assertTrue(
            sizer.size(request(eurusd, "1.0850", price("1.0850")), rates).rejection() is Rejection.InvalidInput
        )
    }

    @Test
    fun takeProfitOnWrongSide_isRejected() {
        assertTrue(
            sizer.size(
                request(eurusd, "1.0850", price("1.0820"), takeProfit = price("1.0800")), rates
            ).rejection() is Rejection.InvalidInput
        )
    }

    @Test
    fun invalidNumbers_areRejectedNotThrown() {
        val badInputs = listOf(
            request(eurusd, "1.0850", price("1.0820"), balance = "0"),
            request(eurusd, "0", price("0.9")),
            request(eurusd, "1.0850", price("1.0820"), risk = RiskInput.PercentOfBalance(bd("0"))),
            request(eurusd, "1.0850", price("1.0820"), risk = RiskInput.PercentOfBalance(bd("150"))),
            request(eurusd, "1.0850", price("1.0820"), risk = RiskInput.FixedAmount(bd("20000"))),
            request(eurusd, "1.0850", pips("0")),
            request(eurusd, "1.0850", price("1.0820"), leverage = "0.5"),
        )
        badInputs.forEachIndexed { i, req ->
            assertTrue("case $i should be InvalidInput", sizer.size(req, rates).rejection() is Rejection.InvalidInput)
        }
    }

    @Test
    fun stopPipsLargerThanPrice_isRejected() {
        // 20,000 pips of 0.0001 = 2.0, which would put a long's stop at a negative price.
        assertTrue(
            sizer.size(request(eurusd, "1.0850", pips("20000")), rates).rejection() is Rejection.InvalidInput
        )
    }

    @Test
    fun neverRisksMoreThanRequested_andLotsAlwaysOnStep() {
        val cases = listOf(
            Triple(usdjpy, "150.00", "USD"),
            Triple(Instrument.forex("EURGBP"), "0.8600", "USD"),
            Triple(usdjpy, "150.00", "EUR"),
            Triple(Instrument.forex("USDCHF"), "0.9000", "USD"),
        )
        for ((instrument, entry, account) in cases) {
            for (stopPips in 5..120 step 3) {
                val r = sizer.size(
                    request(instrument, entry, pips(stopPips.toString()), account = account),
                    rates,
                ).sized()
                assertTrue(
                    "${instrument.symbol}/$account/${stopPips}p: risk ${r.actualRisk} > ${r.targetRisk}",
                    r.actualRisk <= r.targetRisk,
                )
                assertTrue(
                    "${instrument.symbol}/$account/${stopPips}p: ${r.lots} not on 0.01 step",
                    r.lots.remainder(bd("0.01")).signum() == 0,
                )
            }
        }
    }

    @Test
    fun sizingIsDeterministic() {
        val req = request(usdjpy, "150.00", price("149.80"))
        assertEquals(sizer.size(req, rates), sizer.size(req, rates))
        assertNull((sizer.size(req, rates).sized()).takeProfitPrice)
    }
}
