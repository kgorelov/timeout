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
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    item {
                        Text(text = "Calendar Permission")
                    }
                    item {
                        Button(onClick = {
                            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }) {
                            Text(text = if (permissionGranted.value) "Granted" else "Grant Permission")
                        }
                    }
                }
            }
        }
    }
}
