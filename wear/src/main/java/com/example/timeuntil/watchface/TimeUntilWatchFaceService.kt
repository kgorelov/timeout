package com.example.timeuntil.watchface

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

class TimeUntilWatchFaceService : WatchFaceService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var renderer: TimeUntilCanvasRenderer? = null
    private val eventSelectionManager by lazy { EventSelectionManager(applicationContext) }

    override fun onCreate() {
        Log.d(TAG, "Service onCreate")
        super.onCreate()
        
        // Start background update loop
        serviceScope.launch {
            while (isActive) {
                updateNextEvent()
                delay(60000) // Update every minute
            }
        }
    }

    private suspend fun updateNextEvent() {
        Log.v(TAG, "Background update starting...")
        val event = withContext(Dispatchers.IO) {
            try {
                eventSelectionManager.getNextEvent()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch event", e)
                null
            }
        }
        
        withContext(Dispatchers.Main) {
            renderer?.let { r ->
                if (r.nextEvent != event) {
                    Log.i(TAG, "New event pushed to renderer: ${event?.title ?: "None"}")
                    r.previousEventTime = Clock.System.now()
                    r.nextEvent = event
                }
                r.isDataLoaded = true
                r.invalidate()
            }
        }
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
            canvasType = CanvasType.SOFTWARE
        )
        
        this.renderer = newRenderer

        // Trigger immediate update
        serviceScope.launch {
            updateNextEvent()
        }

        return WatchFace(
            WatchFaceType.DIGITAL,
            newRenderer
        )
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        serviceScope.cancel()
        renderer = null
        super.onDestroy()
    }
}
