package com.example.timeuntil

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign

class PermissionsActivity : ComponentActivity() {
    private val calendarPermissionGranted = mutableStateOf(false)
    private val activityPermissionGranted = mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        calendarPermissionGranted.value = permissions[Manifest.permission.READ_CALENDAR] ?: false
        activityPermissionGranted.value = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val listState = rememberScalingLazyListState()
                Scaffold(
                    positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
                ) {
                    ScalingLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        autoCentering = AutoCenteringParams(itemIndex = 1)
                    ) {
                        item {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Required Permissions",
                                style = MaterialTheme.typography.title3,
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Text(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                text = "Grant permissions to see events and steps.",
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Button(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                onClick = {
                                    requestPermissionsLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.ACTIVITY_RECOGNITION
                                        )
                                    )
                                }
                            ) {
                                Text(
                                    text = if (calendarPermissionGranted.value && activityPermissionGranted.value)
                                        "All Granted" else "Grant All"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
