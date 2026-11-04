package app.marketbrief.core.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Client-side guard for provider credit limits (Twelve Data free: 8 per minute, 800 per day).
 * Tracks in memory, so a process restart resets it; the server still enforces the real limit.
 */
class RequestBudget(
    private val perMinute: Int,
    private val perDay: Int,
    private val clock: () -> Long = System::currentTimeMillis,
    private val dailyAcquirer: (suspend (Int) -> Boolean)? = null,
    private val dailyRemaining: (suspend () -> Int)? = null
) {
    private val recent = ArrayDeque<Long>()
    private var dayKey = -1L
    private var usedToday = 0

    private val mutex = Mutex()

    suspend fun tryAcquire(cost: Int = 1): Boolean = mutex.withLock {
        val now = clock()
        val today = now / DAY_MS
        if (today != dayKey) {
            dayKey = today
            usedToday = 0
        }
        while (recent.isNotEmpty() && now - recent.first() >= MINUTE_MS) recent.removeFirst()
        if (usedToday + cost > perDay || recent.size + cost > perMinute) return@withLock false
        if (dailyAcquirer != null && !dailyAcquirer.invoke(cost)) return@withLock false
        repeat(cost) { recent.addLast(now) }
        usedToday += cost
        true
    }

    suspend fun remainingToday(): Int = mutex.withLock {
        val today = clock() / DAY_MS
        if (dailyRemaining != null) return@withLock dailyRemaining.invoke().coerceIn(0, perDay)
        if (today != dayKey) perDay else perDay - usedToday
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val DAY_MS = 86_400_000L
    }
}
