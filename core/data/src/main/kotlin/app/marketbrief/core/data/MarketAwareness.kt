package app.marketbrief.core.data

import kotlinx.coroutines.flow.Flow

enum class AlertDirection { ABOVE, BELOW }
data class AlertSnapshot(val id: String, val target: Double, val direction: AlertDirection, val lastPrice: Double?, val triggered: Boolean = false)
data class AlertEvaluation(val alert: AlertSnapshot, val crossed: Boolean)

object AlertEvaluator {
    fun evaluate(alert: AlertSnapshot, currentPrice: Double): AlertEvaluation {
        if (alert.triggered || !currentPrice.isFinite()) return AlertEvaluation(alert, false)
        val previous = alert.lastPrice
        val crossed = previous != null && when (alert.direction) {
            AlertDirection.ABOVE -> previous < alert.target && currentPrice >= alert.target
            AlertDirection.BELOW -> previous > alert.target && currentPrice <= alert.target
        }
        return AlertEvaluation(alert.copy(lastPrice = currentPrice, triggered = crossed), crossed)
    }
}

data class CalendarEvent(val id: String, val title: String, val currency: String, val instantEpochMillis: Long, val impact: String, val source: String)

interface EconomicCalendarProvider {
    fun observeEvents(): Flow<List<CalendarEvent>>
    suspend fun add(event: CalendarEvent)
}

object FtsQuerySanitizer {
    fun prefixQuery(input: String): String? {
        val safe = input.trim().split(Regex("\\s+"))
            .map { it.replace(Regex("[^\\p{L}\\p{N}_-]"), "") }
            .filter { it.isNotBlank() && it.uppercase() !in setOf("AND", "OR", "NOT", "NEAR") }
        return safe.takeIf { it.isNotEmpty() }?.joinToString(" AND ") { "$it*" }
    }
}
