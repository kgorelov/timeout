package com.example.timeuntil.data

import com.example.timeuntil.shared.Routine
import java.time.DayOfWeek
import java.time.LocalTime

class RoutineManager {
    // For MVP, let's start with some hardcoded routines that can be edited later.
    // In a full implementation, these would be loaded from DataStore/Room.

    fun getRoutines(): List<Routine> {
        return listOf(
            Routine("r1", "Wake up", LocalTime.of(7, 0), DayOfWeek.values().toSet(), true),
            Routine("r2", "Start Work", LocalTime.of(9, 0), setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), true),
            Routine("r3", "Lunch", LocalTime.of(12, 0), DayOfWeek.values().toSet(), true),
            Routine("r4", "End Work", LocalTime.of(18, 0), setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), true),
            Routine("r5", "Sleep", LocalTime.of(23, 0), DayOfWeek.values().toSet(), true)
        )
    }
}
