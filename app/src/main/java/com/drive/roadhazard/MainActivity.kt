package com.drive.roadhazard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.drive.roadhazard.ui.screens.EventConfirmationDialog
import com.drive.roadhazard.ui.screens.LoginScreen
import com.drive.roadhazard.ui.screens.MapScreen
import com.drive.roadhazard.ui.screens.VehicleSelectionScreen
import com.drive.roadhazard.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onPermissionResult(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            RoadSurpTheme {
                if (viewModel.isLoggedIn) {
                    MainScreen()
                } else {
                    LoginScreen { username ->
                        viewModel.onLoginSuccess(username)
                    }
                }
            }
        }
    }

    @Composable
    private fun RoadSurpTheme(content: @Composable () -> Unit) {
        MaterialTheme(content = content)
    }

    @Composable
    private fun MainScreen() {
        var currentScreen by remember { mutableStateOf("vehicle_selection") }

        when (currentScreen) {
            "vehicle_selection" -> VehicleSelectionScreen(
                selectedVehicleType = viewModel.selectedVehicleType,
                selectedOrientation = viewModel.selectedOrientation,
                onVehicleTypeChange = { viewModel.selectedVehicleType = it },
                onOrientationChange = { viewModel.selectedOrientation = it },
                onSelectionComplete = {
                    currentScreen = "map"
                    viewModel.onVehicleSelectionComplete()
                }
            )

            "map" -> MapScreen(
                currentLocation = viewModel.currentLocation,
                currentUser = viewModel.currentUser,
                selectedVehicleType = viewModel.selectedVehicleType,
                currentSpeed = viewModel.currentSpeed,
                detectedEvents = viewModel.detectedEvents.toList(),
                mapEvents = viewModel.mapEvents.toList()
            )
        }

        viewModel.pendingEvent?.let { event ->
            EventConfirmationDialog(
                event = event,
                onConfirm = { confirm ->
                    viewModel.confirmEvent(confirm)
                }
            )
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
        )

        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            viewModel.onPermissionResult(true)
        }
    }
}