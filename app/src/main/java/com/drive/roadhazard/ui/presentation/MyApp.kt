package com.drive.roadhazard.ui.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drive.roadhazard.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var currentScreen = currentBackStackEntry?.destination?.route
    var isStoppedButtonClicked by remember { mutableStateOf(false) }


    val screenName = when (currentScreen) {
        NavigationDestination.CONFIGURATION_SCREEN.name -> "Vehicle Configuration"
        NavigationDestination.MAIN_SCREEN.name -> "Map"
        NavigationDestination.DETAILS_SCREEN.name -> "Detected Hazards"
        else -> "Road Hazard"
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenName,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
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
                        viewModel.onVehicleSelectionComplete()
                        navController.navigate(NavigationDestination.MAIN_SCREEN.name)
                    }
                )
            }

            composable(route = NavigationDestination.MAIN_SCREEN.name) {
                MainScreen(
                    navController = navController,
                    viewModel = viewModel
                )
            }

            composable(route = NavigationDestination.DETAILS_SCREEN.name) {
                DetailsScreen(
                    navController = navController,
                    eventsList = viewModel.detectedEvents.toList()
                )
            }

        }
    }
}