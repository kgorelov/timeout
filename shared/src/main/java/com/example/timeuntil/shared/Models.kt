package com.example.timeuntil.shared

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import java.time.DayOfWeek

enum class EventSource {
    CALENDAR,
    ROUTINE
}

data class Event(
    val id: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant?,
    val source: EventSource,
    val isRoutine: Boolean
)

data class Routine(
    val id: String,
    val title: String,
    val time: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val enabled: Boolean
)
