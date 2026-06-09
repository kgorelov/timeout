package com.example.timeuntil.watchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import android.view.SurfaceHolder
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
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

class TimeUntilWatchFaceService : WatchFaceService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var renderer: TimeUntilCanvasRenderer? = null
    private val eventSelectionManager by lazy { EventSelectionManager(applicationContext) }

    private lateinit var passiveMonitoringClient: PassiveMonitoringClient

    private fun handleTap(x: Int, y: Int) {
        val bounds = renderer?.surfaceHolder?.surfaceFrame ?: return
        val height = bounds.height()
        if (height <= 0) return

        val relativeY = y.toFloat() / height

        when {
            relativeY > 0.75f -> {
                Log.d(TAG, "Bottom tapped - launching health app")
                launchHealthApp()
            }
            relativeY > 0.25f && relativeY <= 0.75f -> {
                Log.d(TAG, "Center tapped - launching calendar app")
                launchCalendarApp()
            }
        }
    }

    private fun launchHealthApp() {
        val packages = listOf(
            "com.mobvoi.companion.at",        // Mobvoi Health (New)
            "com.mobvoi.ticwear.health.main", // TicHealth (Legacy)
            "com.google.android.apps.fitness", // Google Fit
            "com.samsung.android.app.shealth" // Samsung Health
        )
        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            }
        }
        // Fallback to implicit fitness intent
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_FITNESS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch health app", e)
        }
    }

    private fun launchCalendarApp() {
        val pkg = "com.google.android.calendar"
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            try {
                val implicitIntent = Intent(Intent.ACTION_MAIN)
                implicitIntent.addCategory(Intent.CATEGORY_APP_CALENDAR)
                implicitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(implicitIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not launch calendar app", e)
            }
        }
    }

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

    private val healthCallback = object : PassiveListenerCallback {
        override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
            val steps = dataPoints.getData(DataType.STEPS_DAILY).lastOrNull()?.value?.toInt()
            val heartRate = dataPoints.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.toInt()
            val calories = dataPoints.getData(DataType.CALORIES_DAILY).lastOrNull()?.value?.toInt()

            serviceScope.launch {
                withContext(Dispatchers.Main) {
                    renderer?.let {
                        var changed = false
                        if (steps != null && it.stepCount != steps) {
                            it.stepCount = steps
                            changed = true
                        }
                        if (heartRate != null && it.heartRate != heartRate) {
                            it.heartRate = heartRate
                            changed = true
                        }
                        if (calories != null && it.calories != calories) {
                            it.calories = calories
                            changed = true
                        }
                        if (changed) {
                            it.invalidate()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        passiveMonitoringClient = HealthServices.getClient(this).passiveMonitoringClient

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

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
            canvasType = CanvasType.HARDWARE
        )

        this.renderer = newRenderer

        // Set up health listener
        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.STEPS_DAILY, DataType.HEART_RATE_BPM, DataType.CALORIES_DAILY))
            .build()

        try {
            passiveMonitoringClient.setPassiveListenerCallback(config, healthCallback)
            Log.d(TAG, "Passive listener set")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set passive listener", e)
        }

        // Start updates
        serviceScope.launch {
            try {
                // Delay a bit to ensure listener is active and then flush to get initial data
                delay(1000)
                passiveMonitoringClient.flushAsync().await()
                Log.d(TAG, "Passive listener flushed")
            } catch (e: Exception) {
                Log.e(TAG, "Flush failed", e)
            }

            while (isActive) {
                updateNextEvent()
                delay(60000)
            }
        }

        val watchFace = WatchFace(
            WatchFaceType.DIGITAL,
            newRenderer
        )

        watchFace.setTapListener(object : WatchFace.TapListener {
            override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
                if (tapType == TapType.UP) {
                    handleTap(tapEvent.xPos, tapEvent.yPos)
                }
            }
        })

        return watchFace
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
        try {
            passiveMonitoringClient.clearPassiveListenerCallbackAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Clear callback failed", e)
        }
        renderer = null
        super.onDestroy()
    }
}
