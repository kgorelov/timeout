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

    private val sideTextPaint = Paint().apply {
        isAntiAlias = true
        color = Color.GRAY
        textAlign = Paint.Align.CENTER
        textSize = 22f
    }

    private val arcPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    // Default to a mock event so we never have a "blank" frame
    var nextEvent: Event? = Event("mock", "Syncing...", Clock.System.now().plus(60.minutes), null, EventSource.ROUTINE, true)
    var previousEventTime: Instant? = Clock.System.now()

    var batteryLevel: Float = 1.0f // 0.0 to 1.0
    var stepCount: Int = 0
    var stepGoal: Int = 10000

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
        drawBatteryArc(canvas, bounds)
        drawStepsArc(canvas, bounds)

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

    private fun drawBatteryArc(canvas: Canvas, bounds: Rect) {
        val margin = 40f
        val rect = android.graphics.RectF(
            bounds.left + margin,
            bounds.top + margin,
            bounds.right - margin,
            bounds.bottom - margin
        )

        // Draw battery arc on the left (150 to 210 degrees)
        val startAngle = 150f
        val sweepAngle = 60f

        arcPaint.color = Color.DKGRAY
        canvas.drawArc(rect, startAngle, sweepAngle, false, arcPaint)

        arcPaint.color = when {
            batteryLevel > 0.5f -> Color.GREEN
            batteryLevel > 0.2f -> Color.YELLOW
            else -> Color.RED
        }
        canvas.drawArc(rect, startAngle, sweepAngle * batteryLevel, false, arcPaint)

        // Draw rotated text
        canvas.save()
        val textX = bounds.left + margin - 15f
        val textY = bounds.centerY().toFloat()
        canvas.rotate(-90f, textX, textY)
        canvas.drawText(
            "BATTERY ${(batteryLevel * 100).toInt()}%",
            textX,
            textY,
            sideTextPaint
        )
        canvas.restore()
    }

    private fun drawStepsArc(canvas: Canvas, bounds: Rect) {
        val margin = 40f
        val rect = android.graphics.RectF(
            bounds.left + margin,
            bounds.top + margin,
            bounds.right - margin,
            bounds.bottom - margin
        )

        // Draw steps arc on the right (-30 to 30 degrees)
        val startAngle = -30f
        val sweepAngle = 60f

        arcPaint.color = Color.DKGRAY
        canvas.drawArc(rect, startAngle, sweepAngle, false, arcPaint)

        val progress = (stepCount.toFloat() / stepGoal).coerceIn(0f, 1f)
        arcPaint.color = when {
            progress > 0.8f -> Color.GREEN
            progress > 0.3f -> Color.YELLOW
            else -> Color.RED
        }
        canvas.drawArc(rect, startAngle, sweepAngle * progress, false, arcPaint)

        // Draw rotated text
        canvas.save()
        val textX = bounds.right - margin + 15f
        val textY = bounds.centerY().toFloat()
        canvas.rotate(90f, textX, textY)
        canvas.drawText(
            "STEPS $stepCount",
            textX,
            textY,
            sideTextPaint
        )
        canvas.restore()
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
