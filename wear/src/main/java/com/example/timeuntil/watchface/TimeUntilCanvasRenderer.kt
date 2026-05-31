package com.example.timeuntil.watchface

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.example.timeuntil.data.EventSelectionManager
import com.example.timeuntil.shared.Event
import com.example.timeuntil.shared.EventSource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TimeUntilCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<TimeUntilCanvasRenderer.TimeUntilSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    interactiveUpdateIntervalMillis = 1000,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = true
) {
    class TimeUntilSharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 70f
    }

    private val subTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.GRAY
        textAlign = Paint.Align.CENTER
        textSize = 30f
    }

    private val eventSelectionManager = EventSelectionManager(context)
    private var nextEvent: Event? = null
    private var lastEventUpdate: Long = 0

    private val ringPaint = Paint().apply {
        isAntiAlias = true
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private val progressPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }

    private var previousEventTime: Instant? = null

    override suspend fun createSharedAssets(): TimeUntilSharedAssets {
        return TimeUntilSharedAssets()
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: TimeUntilSharedAssets
    ) {
        val now = Clock.System.now()

        // Update next event every minute or if currently null
        if (nextEvent == null || now.toEpochMilliseconds() - lastEventUpdate > 60000 || (nextEvent?.startTime ?: Instant.DISTANT_PAST) < now) {
            val oldEvent = nextEvent
            nextEvent = eventSelectionManager.getNextEvent()
            lastEventUpdate = now.toEpochMilliseconds()

            // For progress ring, we need some reference point.
            // If we don't have a real previous event, we use -1 hour from now as a fallback.
            if (nextEvent != oldEvent) {
                previousEventTime = now // For MVP, let's just reset progress when event changes
            }
        }

        canvas.drawColor(Color.BLACK)

        val event = nextEvent

        if (event == null || event.startTime < now) {
            drawNoEvents(canvas, bounds)
        } else {
            drawProgressRing(canvas, bounds, event, now)
            drawCountdown(canvas, bounds, event, now)
        }
    }

    private fun drawProgressRing(canvas: Canvas, bounds: Rect, event: Event, now: Instant) {
        val start = previousEventTime ?: now.minus(1.minutes)
        val end = event.startTime

        val total = (end - start).inWholeMilliseconds
        val elapsed = (now - start).inWholeMilliseconds

        val progress = if (total > 0) (elapsed.toFloat() / total).coerceIn(0f, 1f) else 1f

        val margin = 20f
        val rect = android.graphics.RectF(
            bounds.left + margin,
            bounds.top + margin,
            bounds.right - margin,
            bounds.bottom - margin
        )

        canvas.drawOval(rect, ringPaint)

        progressPaint.color = textPaint.color // Match countdown color
        canvas.drawArc(rect, -90f, progress * 360f, false, progressPaint)
    }

    private fun drawNoEvents(canvas: Canvas, bounds: Rect) {
        textPaint.color = Color.WHITE
        canvas.drawText("No Events", bounds.centerX().toFloat(), bounds.centerY().toFloat(), textPaint)
    }

    private fun drawCountdown(canvas: Canvas, bounds: Rect, event: Event, now: Instant) {
        val remaining = event.startTime - now
        val remainingMillis = remaining.inWholeMilliseconds

        // Color logic
        val minutesRemaining = remaining.inWholeMinutes
        textPaint.color = when {
            minutesRemaining <= 5 -> Color.RED
            minutesRemaining <= 15 -> Color.rgb(255, 165, 0) // Orange
            minutesRemaining <= 60 -> Color.YELLOW
            else -> Color.WHITE
        }

        val countdownText = formatCountdown(remaining)
        canvas.drawText(countdownText, bounds.centerX().toFloat(), bounds.centerY().toFloat() + 20f, textPaint)

        // Subtext
        subTextPaint.color = Color.GRAY
        canvas.drawText("Until:", bounds.centerX().toFloat(), bounds.centerY().toFloat() - 80f, subTextPaint)
        canvas.drawText(event.title, bounds.centerX().toFloat(), bounds.centerY().toFloat() + 70f, subTextPaint)

        val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val startTimeLocal = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(event.startTime.toEpochMilliseconds()),
            java.time.ZoneId.systemDefault()
        )
        canvas.drawText(startTimeLocal.format(timeFormatter), bounds.centerX().toFloat(), bounds.centerY().toFloat() + 110f, subTextPaint)
    }

    private fun formatCountdown(duration: Duration): String {
        val hours = duration.inWholeHours
        val minutes = duration.inWholeMinutes % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            duration.inWholeSeconds > 0 -> "<1m"
            else -> "0m"
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: TimeUntilSharedAssets
    ) {
        // Not used for now
    }
}
