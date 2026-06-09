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

    private val stepCallback = object : PassiveListenerCallback {
        override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
            val steps = dataPoints.getData(DataType.STEPS_DAILY).lastOrNull()?.value?.toInt() ?: 0
            Log.d(TAG, "Step update: $steps")
            serviceScope.launch {
                withContext(Dispatchers.Main) {
                    renderer?.let {
                        if (it.stepCount != steps) {
                            it.stepCount = steps
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

        // Set up steps listener
        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.STEPS_DAILY))
            .build()

        try {
            passiveMonitoringClient.setPassiveListenerCallback(config, stepCallback)
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
        try {
            passiveMonitoringClient.clearPassiveListenerCallbackAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Clear callback failed", e)
        }
        renderer = null
        super.onDestroy()
    }
}
