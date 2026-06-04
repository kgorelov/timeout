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
import android.util.Log
import com.example.timeuntil.shared.Event
import com.example.timeuntil.shared.EventSource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes

private const val TAG = "TimeUntilRenderer"

class TimeUntilCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    interactiveDrawModeUpdateDelayMillis = 16L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = true
) {
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

    private val topTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.LTGRAY
        textAlign = Paint.Align.CENTER
        textSize = 26f
    }

    // Default to a mock event so we never have a "blank" frame
    var nextEvent: Event? = Event("mock", "Syncing...", Clock.System.now().plus(60.minutes), null, EventSource.ROUTINE, true)
    var previousEventTime: Instant? = Clock.System.now()

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

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        canvas.drawColor(Color.BLACK)

        drawTopInfo(canvas, bounds, zonedDateTime)

        val now = Clock.System.now()
        val event = nextEvent

        if (event == null) {
            drawNoEvents(canvas, bounds)
        } else {
            drawProgressRing(canvas, bounds, event, now)
            drawCountdown(canvas, bounds, event, now)
        }
    }

    private fun drawTopInfo(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")

        val timeText = zonedDateTime.format(timeFormatter)
        val dateText = zonedDateTime.format(dateFormatter)

        canvas.drawText(
            "$timeText • $dateText",
            bounds.centerX().toFloat(),
            bounds.top + (bounds.height() * 0.15f),
            topTextPaint
        )
    }

    private fun drawProgressRing(canvas: Canvas, bounds: Rect, event: Event, now: Instant) {
        val start = previousEventTime ?: now.minus(1.minutes)
        val end = event.startTime

        val total = end.toEpochMilliseconds() - start.toEpochMilliseconds()
        val elapsed = now.toEpochMilliseconds() - start.toEpochMilliseconds()

        val progress = if (total > 0) (elapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
        val margin = 20f
        val rect = android.graphics.RectF(
            bounds.left + margin,
            bounds.top + margin,
            bounds.right - margin,
            bounds.bottom - margin
        )

        canvas.drawOval(rect, ringPaint)

        progressPaint.color = textPaint.color
        canvas.drawArc(rect, -90f, progress * 360f, false, progressPaint)
    }

    private fun drawNoEvents(canvas: Canvas, bounds: Rect) {
        textPaint.color = Color.WHITE
        canvas.drawText("No Events", bounds.centerX().toFloat(), bounds.centerY().toFloat(), textPaint)
    }

    private fun drawCountdown(canvas: Canvas, bounds: Rect, event: Event, now: Instant) {
        val remainingMillis = event.startTime.toEpochMilliseconds() - now.toEpochMilliseconds()
        val minutesRemaining = remainingMillis / 60000

        textPaint.color = when {
            minutesRemaining <= 5 -> Color.RED
            minutesRemaining <= 15 -> Color.rgb(255, 165, 0)
            minutesRemaining <= 60 -> Color.YELLOW
            else -> Color.WHITE
        }

        val countdownText = formatCountdown(remainingMillis)
        canvas.drawText(countdownText, bounds.centerX().toFloat(), bounds.centerY().toFloat() + 20f, textPaint)

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

    private fun formatCountdown(remainingMillis: Long): String {
        val totalMinutes = remainingMillis / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            remainingMillis > 0 -> "<1m"
            else -> "0m"
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {}
}
