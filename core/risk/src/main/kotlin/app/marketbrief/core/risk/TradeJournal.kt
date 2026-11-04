package app.marketbrief.core.risk

import java.math.BigDecimal
import java.math.MathContext

enum class TradeStatus { PLANNED, OPEN, CLOSED, CANCELLED }
enum class GuardrailSeverity { WARN, BLOCK }
enum class DrawdownMode { STATIC, TRAILING }

data class RiskRules(
    val maxRiskPerTradePercent: BigDecimal,
    val dailyLossLimitPercent: BigDecimal,
    val maxOpenRiskPercent: BigDecimal,
    val maxDrawdownPercent: BigDecimal,
    val drawdownMode: DrawdownMode = DrawdownMode.STATIC
)

data class AccountRiskState(
    val startingBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val peakBalance: BigDecimal,
    val todayClosedPnl: BigDecimal,
    val openRisk: BigDecimal
)

data class ProposedTrade(val riskAmount: BigDecimal)
data class Violation(val rule: String, val severity: GuardrailSeverity, val message: String)

object GuardrailEvaluator {
    fun evaluate(rules: RiskRules, account: AccountRiskState, proposed: ProposedTrade): List<Violation> {
        if (account.startingBalance.signum() <= 0 || account.currentBalance.signum() <= 0) return emptyList()
        val result = mutableListOf<Violation>()
        val tradePercent = proposed.riskAmount * HUNDRED / account.currentBalance
        if (tradePercent > rules.maxRiskPerTradePercent) result += Violation("maxRiskPerTrade", GuardrailSeverity.BLOCK, "Trade risk exceeds ${rules.maxRiskPerTradePercent}%.")
        val dailyLoss = account.todayClosedPnl.negate().coerceAtLeast(BigDecimal.ZERO) + proposed.riskAmount
        val dailyLimit = account.startingBalance * rules.dailyLossLimitPercent / HUNDRED
        if (dailyLoss > dailyLimit) result += Violation("dailyLossLimit", GuardrailSeverity.BLOCK, "Worst-case daily loss would exceed the ${rules.dailyLossLimitPercent}% limit.")
        val openLimit = account.currentBalance * rules.maxOpenRiskPercent / HUNDRED
        if (account.openRisk + proposed.riskAmount > openLimit) result += Violation("maxOpenRisk", GuardrailSeverity.BLOCK, "Open risk would exceed ${rules.maxOpenRiskPercent}% of balance.")
        val drawdownBase = if (rules.drawdownMode == DrawdownMode.TRAILING) account.peakBalance else account.startingBalance
        val drawdownLimit = drawdownBase * rules.maxDrawdownPercent / HUNDRED
        if (drawdownBase - account.currentBalance + proposed.riskAmount > drawdownLimit) result += Violation("maxDrawdown", GuardrailSeverity.BLOCK, "Worst-case drawdown would exceed ${rules.maxDrawdownPercent}%.")
        return result
    }

    private val HUNDRED = BigDecimal("100")
    private fun BigDecimal.coerceAtLeast(other: BigDecimal) = if (this < other) other else this
}

data class ClosedTradeResult(val pnl: BigDecimal, val rMultiple: BigDecimal)
data class TradeStats(
    val closedCount: Int,
    val winRate: BigDecimal,
    val averageR: BigDecimal,
    val expectancy: BigDecimal,
    val profitFactor: BigDecimal?,
    val maxDrawdown: BigDecimal
) {
    companion object {
        fun calculate(trades: List<ClosedTradeResult>): TradeStats {
            if (trades.isEmpty()) return TradeStats(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO)
            val wins = trades.filter { it.pnl.signum() > 0 }
            val grossWins = wins.fold(BigDecimal.ZERO) { a, t -> a + t.pnl }
            val grossLosses = trades.filter { it.pnl.signum() < 0 }.fold(BigDecimal.ZERO) { a, t -> a + t.pnl.abs() }
            var equity = BigDecimal.ZERO
            var peak = BigDecimal.ZERO
            var maxDd = BigDecimal.ZERO
            trades.forEach { trade ->
                equity += trade.pnl
                if (equity > peak) peak = equity
                val dd = peak - equity
                if (dd > maxDd) maxDd = dd
            }
            val n = BigDecimal(trades.size)
            val averageR = trades.fold(BigDecimal.ZERO) { a, t -> a + t.rMultiple }.divide(n, MathContext.DECIMAL128)
            val expectancy = trades.fold(BigDecimal.ZERO) { a, t -> a + t.pnl }.divide(n, MathContext.DECIMAL128)
            return TradeStats(trades.size, BigDecimal(wins.size).multiply(HUNDRED).divide(n, MathContext.DECIMAL128), averageR, expectancy,
                if (grossLosses.signum() == 0) null else grossWins / grossLosses, maxDd)
        }
        private val HUNDRED = BigDecimal("100")
    }
}
