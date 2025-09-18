package com.drive.roadhazard.ui.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drive.roadhazard.viewmodels.MainViewModel

@Composable
fun MyApp(viewModel: MainViewModel) {

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    // var currentScreen = currentBackStackEntry?.destination?.route

    Scaffold(containerColor = Color.LightGray) { innerPadding ->
        NavHost(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            navController = navController,
            startDestination = NavigationDestination.CONFIGURATION_SCREEN.name
        ) {
            composable(route = NavigationDestination.CONFIGURATION_SCREEN.name) {
                ConfigurationScreen(
                    selectedVehicleType = viewModel.selectedVehicleType,
                    selectedOrientation = viewModel.selectedOrientation,
                    onVehicleTypeChange = { viewModel.selectedVehicleType = it },
                    onOrientationChange = { viewModel.selectedOrientation = it },
                    onSelectionComplete = {
                        navController.navigate(NavigationDestination.MAIN_SCREEN.name)
                        viewModel.onVehicleSelectionComplete()
                    }
                )
            }

            composable(route = NavigationDestination.MAIN_SCREEN.name) {
                MainScreen(
                    currentLocation = viewModel.currentLocation,
                    selectedVehicleType = viewModel.selectedVehicleType,
                    currentSpeed = viewModel.currentSpeed,
                    detectedEvents = viewModel.detectedEvents.toList(),
                    mapEvents = viewModel.mapEvents.toList()
                )
            }
        }
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