package com.example.timeuntil.watchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.example.timeuntil.data.EventSelectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

private const val TAG = "TimeUntilService"

class TimeUntilWatchFaceService : WatchFaceService(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var renderer: TimeUntilCanvasRenderer? = null
    private val eventSelectionManager by lazy { EventSelectionManager(applicationContext) }

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps = -1

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                val batteryPct = level / scale.toFloat()
                renderer?.let {
                    it.batteryLevel = batteryPct
                    it.invalidate()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val steps = event.values[0].toInt()
            if (initialSteps == -1) {
                initialSteps = steps
            }
            val stepsToday = steps - initialSteps
            renderer?.let {
                it.stepCount = stepsToday
                it.invalidate()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        Log.d(TAG, "createWatchFace")

        val newRenderer = TimeUntilCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE // Return to hardware rendering
        )

        this.renderer = newRenderer

        // Start updates
        serviceScope.launch {
            while (isActive) {
                updateNextEvent()
                delay(60000)
            }
        }

        return WatchFace(
            WatchFaceType.DIGITAL,
            newRenderer
        )
    }

    private suspend fun updateNextEvent() {
        val event = withContext(Dispatchers.IO) {
            try {
                eventSelectionManager.getNextEvent()
            } catch (e: Exception) {
                Log.e(TAG, "Fetch failed", e)
                null
            }
        }

        withContext(Dispatchers.Main) {
            renderer?.let { r ->
                if (event != null && r.nextEvent?.id != event.id) {
                    Log.i(TAG, "Updated to: ${event.title}")
                    r.previousEventTime = Clock.System.now()
                    r.nextEvent = event
                    r.invalidate()
                }
            }
        }
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        serviceScope.cancel()
        unregisterReceiver(batteryReceiver)
        sensorManager.unregisterListener(this)
        renderer = null
        super.onDestroy()
    }
}
