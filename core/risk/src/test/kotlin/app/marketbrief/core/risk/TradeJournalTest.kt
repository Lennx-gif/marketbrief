package app.marketbrief.core.risk

import java.math.BigDecimal as D
import org.junit.Assert.*
import org.junit.Test

class TradeJournalTest {
    @Test fun rateNeedsAreCappedAtTwoAndSkipQuoteAccountMatch() {
        val eurUsd = Instrument.forex("EURUSD")
        val usdJpy = Instrument.forex("USDJPY")
        assertTrue(RateNeeds.forTrade(eurUsd, CurrencyCode("USD")).isEmpty())
        assertEquals(setOf("USDJPY"), RateNeeds.forTrade(usdJpy, CurrencyCode("USD")))
        assertEquals(2, RateNeeds.forTrade(usdJpy, CurrencyCode("EUR")).size)
    }

    @Test fun statsHandleNoTradesAndLosses() {
        assertEquals(0, TradeStats.calculate(emptyList()).closedCount)
        val stats = TradeStats.calculate(listOf(ClosedTradeResult(D("-10"), D("-1")), ClosedTradeResult(D("-5"), D("-0.5"))))
        assertEquals(D("0"), stats.winRate)
        assertEquals(D("15"), stats.maxDrawdown)
        assertEquals(D("0"), stats.profitFactor)
    }

    @Test fun dailyLossBoundaryBlocks() {
        val rules = RiskRules(D("2"), D("3"), D("5"), D("10"))
        val account = AccountRiskState(D("10000"), D("10000"), D("10000"), D("-250"), D("0"))
        assertTrue(GuardrailEvaluator.evaluate(rules, account, ProposedTrade(D("60"))).any { it.rule == "dailyLossLimit" })
    }

    @Test fun trailingDrawdownUsesPeakBalance() {
        val rules = RiskRules(D("2"), D("10"), D("10"), D("5"), DrawdownMode.TRAILING)
        val account = AccountRiskState(D("10000"), D("9500"), D("11000"), D("0"), D("0"))
        assertTrue(GuardrailEvaluator.evaluate(rules, account, ProposedTrade(D("100"))).any { it.rule == "maxDrawdown" })
    }

    @Test fun journalAnalyticsGroupByAccountAndSetupAndComputeBalance() {
        val trades = listOf(
            ScopedClosedTrade(1, "breakout", ClosedTradeResult(D("100"), D("1"))),
            ScopedClosedTrade(1, "breakout", ClosedTradeResult(D("-50"), D("-0.5"))),
            ScopedClosedTrade(2, null, ClosedTradeResult(D("25"), D("0.5")))
        )

        assertEquals(2, JournalAnalytics.statsByAccount(trades).size)
        assertEquals(2, JournalAnalytics.statsByAccount(trades)[1L]?.closedCount)
        assertEquals(1, JournalAnalytics.statsBySetup(trades).size)
        assertEquals(D("1050"), JournalAnalytics.accountBalance(D("1000"), listOf(D("100"), D("-50"))))
        assertTrue(JournalAnalytics.checklistScore(2, 3)!!.compareTo(D("66.666")) > 0)
        assertNull(JournalAnalytics.checklistScore(0, 0))
    }
}
