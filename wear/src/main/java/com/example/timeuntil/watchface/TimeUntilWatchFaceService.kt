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

private const val TAG = "TimeUntilService"

class TimeUntilWatchFaceService : WatchFaceService() {
    
    override fun onCreate() {
        Log.d(TAG, "Service onCreate")
        super.onCreate()
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        Log.d(TAG, "createWatchFace: start")
        
        val renderer = TimeUntilCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.SOFTWARE
        )

        Log.d(TAG, "createWatchFace: renderer created")
        
        return WatchFace(
            WatchFaceType.DIGITAL,
            renderer
        )
    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        Log.d(TAG, "createComplicationSlotsManager")
        return ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
    }

    override fun createUserStyleSchema(): UserStyleSchema {
        Log.d(TAG, "createUserStyleSchema")
        return UserStyleSchema(emptyList())
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        super.onDestroy()
    }
}
