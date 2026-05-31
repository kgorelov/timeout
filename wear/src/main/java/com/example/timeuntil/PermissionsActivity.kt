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
    private val permissionGranted = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted.value = isGranted
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
                                text = "Calendar Access",
                                style = MaterialTheme.typography.title3
                            )
                        }
                        item {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Needed to display your next event.",
                                style = MaterialTheme.typography.body2
                            )
                        }
                        item {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                            ) {
                                Text(
                                    text = if (permissionGranted.value) "Permission Granted" else "Grant Permission"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
