package app.marketbrief.core.risk

import java.math.BigDecimal
import java.math.RoundingMode

object RiskConverter {
    private val ONE_HUNDRED = BigDecimal("100")
    val DEFAULT_HIGH_RISK_THRESHOLD_PERCENT = BigDecimal("5.0")

    /**
     * Converts risk percentage of balance to a cash amount in account currency.
     * Formula: cash = (balance * percent) / 100
     * Returns null if balance is <= 0, percent is null, or percent is negative.
     * Result is rounded to 2 decimal places using HALF_UP.
     */
    fun percentToCash(balance: BigDecimal?, percent: BigDecimal?): BigDecimal? {
        if (balance == null || percent == null) return null
        if (balance <= BigDecimal.ZERO || percent < BigDecimal.ZERO) return null
        return balance.multiply(percent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
    }

    /**
     * Converts a cash risk amount to a percentage of balance.
     * Formula: percent = (cash * 100) / balance
     * Returns null if balance is <= 0, cash is null, or cash is negative.
     * Result is rounded to 2 decimal places using HALF_UP.
     */
    fun cashToPercent(balance: BigDecimal?, cash: BigDecimal?): BigDecimal? {
        if (balance == null || cash == null) return null
        if (balance <= BigDecimal.ZERO || cash < BigDecimal.ZERO) return null
        return cash.multiply(ONE_HUNDRED).divide(balance, 2, RoundingMode.HALF_UP)
    }

    /**
     * Returns true if the risk percentage exceeds the soft warning threshold (default 5.0%).
     */
    fun isHighRisk(percent: BigDecimal?, threshold: BigDecimal = DEFAULT_HIGH_RISK_THRESHOLD_PERCENT): Boolean {
        if (percent == null) return false
        return percent > threshold
    }
}
