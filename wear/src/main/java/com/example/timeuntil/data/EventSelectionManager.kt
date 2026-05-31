package com.example.timeuntil.data

import android.content.Context
import com.example.timeuntil.shared.Event
import com.example.timeuntil.shared.EventSource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import java.time.ZoneId

class EventSelectionManager(private val context: Context) {
    private val calendarManager = CalendarManager(context)
    private val routineManager = RoutineManager()

    fun getNextEvent(): Event? {
        val now = Clock.System.now()
        val calendarEvent = try {
            calendarManager.getNextEvent()
        } catch (e: SecurityException) {
            null
        }

        val routineEvents = getUpcomingRoutineEvents(now)
        
        val allEvents = mutableListOf<Event>()
        calendarEvent?.let { allEvents.add(it) }
        allEvents.addAll(routineEvents)

        return allEvents
            .filter { it.startTime > now }
            .minByOrNull { it.startTime }
    }

    private fun getUpcomingRoutineEvents(now: Instant): List<Event> {
        val routines = routineManager.getRoutines().filter { it.enabled }
        val result = mutableListOf<Event>()
        
        val nowDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(now.toEpochMilliseconds()),
            ZoneId.systemDefault()
        )

        for (routine in routines) {
            // Check today
            if (routine.daysOfWeek.contains(nowDateTime.dayOfWeek)) {
                val routineToday = nowDateTime.withHour(routine.time.hour).withMinute(routine.time.minute).withSecond(0).withNano(0)
                if (routineToday.isAfter(nowDateTime)) {
                    result.add(routineToEvent(routine, routineToday))
                }
            }
            
            // Check tomorrow (if no event today)
            val tomorrow = nowDateTime.plusDays(1)
            if (routine.daysOfWeek.contains(tomorrow.dayOfWeek)) {
                val routineTomorrow = tomorrow.withHour(routine.time.hour).withMinute(routine.time.minute).withSecond(0).withNano(0)
                result.add(routineToEvent(routine, routineTomorrow))
            }
        }
        
        return result
    }

    private fun routineToEvent(routine: com.example.timeuntil.shared.Routine, dateTime: LocalDateTime): Event {
        val instant = Instant.fromEpochMilliseconds(
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        return Event(
            id = routine.id,
            title = routine.title,
            startTime = instant,
            endTime = null,
            source = EventSource.ROUTINE,
            isRoutine = true
        )
    }
}
