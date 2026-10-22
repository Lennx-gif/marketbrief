package app.marketbrief.core.data.remote

/**
 * Client-side guard for provider credit limits (Twelve Data free: 8 per minute, 800 per day).
 * Tracks in memory, so a process restart resets it; the server still enforces the real limit.
 */
class RequestBudget(
    private val perMinute: Int,
    private val perDay: Int,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val recent = ArrayDeque<Long>()
    private var dayKey = -1L
    private var usedToday = 0

    @Synchronized
    fun tryAcquire(cost: Int = 1): Boolean {
        val now = clock()
        val today = now / DAY_MS
        if (today != dayKey) {
            dayKey = today
            usedToday = 0
        }
        while (recent.isNotEmpty() && now - recent.first() >= MINUTE_MS) recent.removeFirst()
        if (usedToday + cost > perDay || recent.size + cost > perMinute) return false
        repeat(cost) { recent.addLast(now) }
        usedToday += cost
        return true
    }

    @Synchronized
    fun remainingToday(): Int {
        val today = clock() / DAY_MS
        return if (today != dayKey) perDay else perDay - usedToday
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val DAY_MS = 86_400_000L
    }
}
