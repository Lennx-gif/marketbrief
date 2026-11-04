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

data class ImportedCalendarEvent(val title: String, val currency: String, val instantEpochMillis: Long, val impact: String)

data class CalendarEvent(val id: String, val title: String, val currency: String, val instantEpochMillis: Long, val impact: String, val source: String)

interface EconomicCalendarProvider {
    fun observeEvents(): Flow<List<CalendarEvent>>
    suspend fun add(event: CalendarEvent)
    suspend fun importCsv(csv: String): Int
}

object FtsQuerySanitizer {
    fun prefixQuery(input: String): String? {
        val safe = input.trim().split(Regex("\\s+"))
            .map { it.replace(Regex("[^\\p{L}\\p{N}_-]"), "") }
            .filter { it.isNotBlank() && it.uppercase() !in setOf("AND", "OR", "NOT", "NEAR") }
        return safe.takeIf { it.isNotEmpty() }?.joinToString(" AND ") { "$it*" }
    }
}

object CalendarCsvParser {
    /** CSV columns: title,currency,instant,impact. Instant must be ISO-8601 with an offset or Z. */
    fun parse(csv: String): List<ImportedCalendarEvent> {
        val rows = csv.lineSequence().filter { it.isNotBlank() }.toList()
        if (rows.isEmpty()) return emptyList()
        val data = if (rows.first().startsWith("title,", ignoreCase = true)) rows.drop(1) else rows
        return data.mapNotNull { row ->
            val fields = splitCsvRow(row)
            if (fields.size != 4) return@mapNotNull null
            val instant = runCatching { java.time.Instant.parse(fields[2]).toEpochMilli() }
                .recoverCatching { java.time.OffsetDateTime.parse(fields[2]).toInstant().toEpochMilli() }
                .getOrNull() ?: return@mapNotNull null
            if (fields[0].isBlank() || fields[1].isBlank()) return@mapNotNull null
            ImportedCalendarEvent(fields[0].trim(), fields[1].trim().uppercase(), instant, fields[3].trim().uppercase())
        }
    }

    private fun splitCsvRow(row: String): List<String> {
        val out = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < row.length) {
            val c = row[i]
            when {
                c == '"' && quoted && i + 1 < row.length && row[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { out += cell.toString(); cell.clear() }
                else -> cell.append(c)
            }
            i++
        }
        out += cell.toString()
        return if (quoted) emptyList() else out
    }
}
