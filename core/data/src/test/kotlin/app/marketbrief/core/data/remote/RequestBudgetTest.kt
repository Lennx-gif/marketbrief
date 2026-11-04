package app.marketbrief.core.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class RequestBudgetTest {
    @Test
    fun `blocks the ninth call inside one minute and recovers after it`() = runTest {
        var now = 1_000_000_000_000L
        val budget = RequestBudget(perMinute = 8, perDay = 800, clock = { now })
        repeat(8) { assertTrue(budget.tryAcquire()) }
        assertFalse(budget.tryAcquire())
        now += 61_000
        assertTrue(budget.tryAcquire())
    }

    @Test
    fun `daily cap holds and resets next day`() = runTest {
        var now = 1_000_000_000_000L
        val budget = RequestBudget(perMinute = 8, perDay = 3, clock = { now })
        repeat(3) { assertTrue(budget.tryAcquire()); now += 61_000 }
        assertFalse(budget.tryAcquire())
        now += 86_400_000
        assertTrue(budget.tryAcquire())
    }
}
