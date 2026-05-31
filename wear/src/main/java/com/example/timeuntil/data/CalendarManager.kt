package com.example.timeuntil.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import com.example.timeuntil.shared.Event
import com.example.timeuntil.shared.EventSource
import kotlinx.datetime.Instant

class CalendarManager(private val context: Context) {

    fun getNextEvent(): Event? {
        val now = System.currentTimeMillis()
        val end = now + 24 * 60 * 60 * 1000 // Look ahead 24 hours

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, now)
        ContentUris.appendId(uriBuilder, end)

        val cursor = context.contentResolver.query(
            uriBuilder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getString(0)
                val title = it.getString(1)
                val startTime = it.getLong(2)
                val endTime = it.getLong(3)
                val allDay = it.getInt(4) != 0

                if (allDay) {
                    // For MVP, we might want to skip all-day events or handle them differently.
                    // Let's look for the next non-all-day event if this one is all-day.
                    while (it.moveToNext()) {
                        val nextAllDay = it.getInt(4) != 0
                        if (!nextAllDay) {
                            return Event(
                                id = it.getString(0),
                                title = it.getString(1),
                                startTime = Instant.fromEpochMilliseconds(it.getLong(2)),
                                endTime = Instant.fromEpochMilliseconds(it.getLong(3)),
                                source = EventSource.CALENDAR,
                                isRoutine = false
                            )
                        }
                    }
                }

                return Event(
                    id = id,
                    title = title,
                    startTime = Instant.fromEpochMilliseconds(startTime),
                    endTime = Instant.fromEpochMilliseconds(endTime),
                    source = EventSource.CALENDAR,
                    isRoutine = false
                )
            }
        }

        return null
    }
}
