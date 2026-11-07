package app.marketbrief.core.storage

import app.marketbrief.core.data.CalendarEvent
import app.marketbrief.core.data.EconomicCalendarProvider
import app.marketbrief.core.storage.db.CalendarDao
import app.marketbrief.core.storage.db.CalendarEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomEconomicCalendarProvider @Inject constructor(private val dao: CalendarDao) : EconomicCalendarProvider {
    override fun observeEvents(): Flow<List<CalendarEvent>> = dao.observeEvents().map { rows ->
        rows.map { CalendarEvent(it.id, it.title, it.currency, it.instant, it.impact, it.source) }
    }
    override suspend fun add(event: CalendarEvent) {
        dao.upsert(listOf(CalendarEventEntity(event.id, event.title, event.currency, event.instantEpochMillis, event.impact, event.source)))
    }
}
