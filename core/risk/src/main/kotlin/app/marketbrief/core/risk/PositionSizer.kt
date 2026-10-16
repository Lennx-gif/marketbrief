package app.marketbrief.core.risk

import java.math.BigDecimal
import java.math.RoundingMode

enum class Direction(internal val sign: BigDecimal) {
    LONG(BigDecimal.ONE),
    SHORT(BigDecimal.ONE.negate()),
}

sealed interface RiskInput {
    /** Percent of balance, e.g. 1 means 1%. */
    data class PercentOfBalance(val percent: BigDecimal) : RiskInput

    /** Fixed amount in the account currency. */
    data class FixedAmount(val amount: BigDecimal) : RiskInput
}

/** A stop-loss or take-profit, given either as an absolute price or as a distance from entry in pips. */
sealed interface PriceLevel {
    data class AtPrice(val price: BigDecimal) : PriceLevel
    data class PipsFromEntry(val pips: BigDecimal) : PriceLevel
}

data class SizingRequest(
    val instrument: Instrument,
    val accountCurrency: CurrencyCode,
    val balance: BigDecimal,
    val risk: RiskInput,
    val direction: Direction,
    val entry: BigDecimal,
    val stop: PriceLevel,
    val takeProfit: PriceLevel? = null,
    val leverage: BigDecimal? = null,
)

sealed interface SizingWarning {
    data class HighRisk(val riskPercent: BigDecimal, val threshold: BigDecimal) : SizingWarning
    data class CappedAtMaxLot(val maxLot: BigDecimal) : SizingWarning
}

/**
 * All money values are in the account currency and rounded to 2 decimals, pip values to 4, pips to 1,
 * reward-to-risk to 2. Compare with compareTo, not equals, since BigDecimal equality includes scale.
 * Format with toPlainString() for display.
 */
data class SizingResult(
    val lots: BigDecimal,
    val units: BigDecimal,
    val targetRisk: BigDecimal,
    val actualRisk: BigDecimal,
    val actualRiskPercent: BigDecimal,
    val stopPrice: BigDecimal,
    val stopPips: BigDecimal,
    val pipValuePerLot: BigDecimal,
    val takeProfitPrice: BigDecimal?,
    val takeProfitPips: BigDecimal?,
    val potentialProfit: BigDecimal?,
    val rewardRisk: BigDecimal?,
    val marginRequired: BigDecimal?,
    val warnings: List<SizingWarning>,
)

sealed interface Rejection {
    data class InvalidInput(val message: String) : Rejection
    data class RateUnavailable(val from: CurrencyCode, val to: CurrencyCode) : Rejection

    /** The risk budget buys less than the broker's minimum lot. [riskAtMinLot] is what the minimum would actually risk. */
    data class BelowMinimumLot(
        val affordableLots: BigDecimal,
        val minLot: BigDecimal,
        val riskAtMinLot: BigDecimal,
    ) : Rejection
}

sealed interface SizingOutcome {
    data class Sized(val result: SizingResult) : SizingOutcome
    data class Rejected(val reason: Rejection) : SizingOutcome
}

private val HUNDRED = BigDecimal(100)

private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

/** Rounds down to a multiple of [step] so the position never risks more than asked. */
private fun floorToStep(value: BigDecimal, step: BigDecimal): BigDecimal =
    // Normalising to 12 decimals first absorbs division noise from inverted rates (1/150 is not exact).
    // Example: USDJPY at 150, 1000 USD, 1% risk, 5-pip stop is exactly 0.30 lots but computes as
    // 0.2999999999999999999999999999999999, which would floor to 0.29 without this.
    value.setScale(12, RoundingMode.HALF_UP).divide(step, 0, RoundingMode.DOWN).multiply(step)

/**
 * Sizes a position so that being stopped out loses at most the chosen risk.
 *
 *     lots = riskAmount / (stopDistance * contractSize * quoteToAccountRate)
 *
 * where stopDistance is a price difference. Pips only matter for input and display, so the same maths
 * serves forex, metals, indices and crypto. Never throws for bad user input; returns [SizingOutcome.Rejected].
 */
class PositionSizer(
    private val warnAboveRiskPercent: BigDecimal = BigDecimal("2"),
) {

    fun size(request: SizingRequest, rates: RateSource): SizingOutcome {
        val instrument = request.instrument
        val entry = request.entry
        val balance = request.balance
        val dir = request.direction.sign

        if (balance.signum() <= 0) return invalid("Balance must be greater than zero.")
        if (entry.signum() <= 0) return invalid("Entry price must be greater than zero.")
        request.leverage?.let { if (it < BigDecimal.ONE) return invalid("Leverage must be 1 or more.") }

        val riskAmount: BigDecimal
        val riskPercent: BigDecimal
        when (val risk = request.risk) {
            is RiskInput.PercentOfBalance -> {
                if (risk.percent.signum() <= 0 || risk.percent > HUNDRED) {
                    return invalid("Risk must be more than 0% and at most 100% of balance.")
                }
                riskPercent = risk.percent
                riskAmount = balance.multiply(risk.percent, CALC).divide(HUNDRED, CALC)
            }
            is RiskInput.FixedAmount -> {
                if (risk.amount.signum() <= 0) return invalid("Risk amount must be greater than zero.")
                if (risk.amount > balance) return invalid("Risk amount can't exceed the account balance.")
                riskAmount = risk.amount
                riskPercent = risk.amount.multiply(HUNDRED, CALC).divide(balance, CALC)
            }
        }

        val stopDistance: BigDecimal
        val stopPrice: BigDecimal
        when (val stop = request.stop) {
            is PriceLevel.AtPrice -> {
                stopPrice = stop.price
                stopDistance = entry.subtract(stop.price).multiply(dir)
            }
            is PriceLevel.PipsFromEntry -> {
                if (stop.pips.signum() <= 0) return invalid("Stop-loss distance must be greater than zero.")
                stopDistance = stop.pips.multiply(instrument.pipSize)
                stopPrice = entry.subtract(stopDistance.multiply(dir))
            }
        }
        if (stopDistance.signum() <= 0) {
            return invalid(
                if (request.direction == Direction.LONG) "For a long trade the stop-loss must be below the entry price."
                else "For a short trade the stop-loss must be above the entry price."
            )
        }
        if (stopPrice.signum() <= 0) return invalid("Stop-loss price must be greater than zero.")

        val tpDistance: BigDecimal?
        val tpPrice: BigDecimal?
        when (val tp = request.takeProfit) {
            null -> {
                tpDistance = null
                tpPrice = null
            }
            is PriceLevel.AtPrice -> {
                tpPrice = tp.price
                tpDistance = tp.price.subtract(entry).multiply(dir)
            }
            is PriceLevel.PipsFromEntry -> {
                if (tp.pips.signum() <= 0) return invalid("Take-profit distance must be greater than zero.")
                tpDistance = tp.pips.multiply(instrument.pipSize)
                tpPrice = entry.add(tpDistance.multiply(dir))
            }
        }
        if (tpDistance != null && tpDistance.signum() <= 0) {
            return invalid(
                if (request.direction == Direction.LONG) "For a long trade the take-profit must be above the entry price."
                else "For a short trade the take-profit must be below the entry price."
            )
        }

        val rate = rates.rate(instrument.quoteCurrency, request.accountCurrency)
            ?: return SizingOutcome.Rejected(
                Rejection.RateUnavailable(instrument.quoteCurrency, request.accountCurrency)
            )
        if (rate.signum() <= 0) return invalid("Exchange rate must be greater than zero.")

        val lossPerLot = stopDistance.multiply(instrument.contractSize, CALC).multiply(rate, CALC)
        val affordable = floorToStep(riskAmount.divide(lossPerLot, CALC), instrument.lotStep)

        val warnings = mutableListOf<SizingWarning>()
        val lots = if (affordable > instrument.maxLot) {
            warnings += SizingWarning.CappedAtMaxLot(instrument.maxLot)
            instrument.maxLot
        } else {
            affordable
        }
        if (lots < instrument.minLot) {
            return SizingOutcome.Rejected(
                Rejection.BelowMinimumLot(
                    affordableLots = affordable,
                    minLot = instrument.minLot,
                    riskAtMinLot = instrument.minLot.multiply(lossPerLot, CALC).money(),
                )
            )
        }
        if (riskPercent > warnAboveRiskPercent) {
            warnings += SizingWarning.HighRisk(riskPercent.setScale(2, RoundingMode.HALF_UP), warnAboveRiskPercent)
        }

        val units = lots.multiply(instrument.contractSize)
        val actualRisk = lots.multiply(lossPerLot, CALC)

        return SizingOutcome.Sized(
            SizingResult(
                lots = lots,
                units = units,
                targetRisk = riskAmount.money(),
                actualRisk = actualRisk.money(),
                actualRiskPercent = actualRisk.multiply(HUNDRED, CALC).divide(balance, CALC)
                    .setScale(2, RoundingMode.HALF_UP),
                stopPrice = stopPrice,
                stopPips = stopDistance.divide(instrument.pipSize, CALC).setScale(1, RoundingMode.HALF_UP),
                pipValuePerLot = instrument.pipSize.multiply(instrument.contractSize, CALC)
                    .multiply(rate, CALC).setScale(4, RoundingMode.HALF_UP),
                takeProfitPrice = tpPrice,
                takeProfitPips = tpDistance?.divide(instrument.pipSize, CALC)?.setScale(1, RoundingMode.HALF_UP),
                potentialProfit = tpDistance?.multiply(units, CALC)?.multiply(rate, CALC)?.money(),
                rewardRisk = tpDistance?.divide(stopDistance, CALC)?.setScale(2, RoundingMode.HALF_UP),
                marginRequired = request.leverage?.let {
                    units.multiply(entry, CALC).multiply(rate, CALC).divide(it, CALC).money()
                },
                warnings = warnings,
            )
        )
    }

    private fun invalid(message: String): SizingOutcome =
        SizingOutcome.Rejected(Rejection.InvalidInput(message))
}
