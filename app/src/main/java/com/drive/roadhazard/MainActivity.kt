package com.drive.roadhazard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import com.drive.roadhazard.managers.LocationManager
import com.drive.roadhazard.managers.SensorEventManager
import com.drive.roadhazard.repositories.EventRepository
import com.drive.roadhazard.ui.screens.EventConfirmationDialog
import com.drive.roadhazard.ui.screens.LoginScreen
import com.drive.roadhazard.ui.screens.MapScreen
import com.drive.roadhazard.ui.screens.VehicleSelectionScreen
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    // Managers
    private lateinit var sensorEventManager: SensorEventManager
    private lateinit var locationManager: LocationManager
    private lateinit var eventRepository: EventRepository

    // State
    private var currentLocation: Location? = null
    private var currentSpeed = 0f
    private var selectedVehicleType by mutableStateOf(VehicleType.TWO_WHEELER)
    private var selectedOrientation by mutableStateOf(PhoneOrientation.MOUNTER)
    private var isLoggedIn by mutableStateOf(true)
    private var currentUser by mutableStateOf("")
    private val detectedEvents = mutableListOf<RoadEvent>()
    private val mapEvents = mutableStateListOf<EventResponse>()
    private var pendingEvent by mutableStateOf<RoadEvent?>(null)
    private var permissionsGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        startSensorCollection()
//        if (allGranted && isLoggedIn) {
//            startSensorCollection()
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid
        Configuration.getInstance()
            .load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        // Initialize managers
        initializeManagers()

        // Request permissions first
        requestPermissions()

        setContent {
            RoadSurpTheme {
                if (isLoggedIn) {
                    MainScreen()
                } else {
                    LoginScreen { username ->
                        currentUser = username
                        isLoggedIn = true
                        // Start sensor collection only if permissions are granted
                        if (permissionsGranted) {
                            startSensorCollection()
                        }
                    }
                }
            }
        }
    }

    private fun initializeManagers() {
        // Initialize Event Repository
        eventRepository = EventRepository()

        // Initialize Location Manager
        locationManager = LocationManager(this) { location, speed ->
            currentLocation = location
            currentSpeed = speed

            // Update sensor manager with current location and speed
            if (::sensorEventManager.isInitialized) {
                sensorEventManager.updateLocationAndSpeed(location, speed)
            }

            // Fetch nearby events when location changes
            eventRepository.fetchNearbyEvents(
                location.latitude,
                location.longitude
            ) { events ->
                mapEvents.clear()
                mapEvents.addAll(events)
            }
        }

        // Initialize Sensor Event Manager
        sensorEventManager = SensorEventManager(this) { roadEvent ->
            // Show confirmation dialog for detected events
            pendingEvent = roadEvent
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
                selectedVehicleType = selectedVehicleType,
                selectedOrientation = selectedOrientation,
                onVehicleTypeChange = { selectedVehicleType = it },
                onOrientationChange = { selectedOrientation = it },
                onSelectionComplete = {
                    currentScreen = "map"
                    // Update sensor manager with new vehicle settings
                    sensorEventManager.updateVehicleSettings(
                        selectedVehicleType,
                        selectedOrientation
                    )
                    // Start sensor collection if not already started
                    if (permissionsGranted) {
                        startSensorCollection()
                    }
                }
            )

            "map" -> MapScreen(
                currentLocation = currentLocation,
                currentUser = currentUser,
                selectedVehicleType = selectedVehicleType,
                currentSpeed = currentSpeed,
                detectedEvents = detectedEvents.toList(),
                mapEvents = mapEvents.toList()
            )
        }

        // Event confirmation dialog
        pendingEvent?.let { event ->
            EventConfirmationDialog(
                event = event,
                onConfirm = { confirm ->
                    confirmEvent(confirm)
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
            permissionsGranted = true
        }
    }

    private fun startSensorCollection() {
        if (!permissionsGranted) return

        sensorEventManager.startSensorCollection(
            selectedVehicleType,
            selectedOrientation
        )

        locationManager.startLocationUpdates()

        // Start periodic event upload
        eventRepository.startPeriodicUpload(detectedEvents)
    }

    private fun confirmEvent(confirm: Boolean) {
        pendingEvent?.let { event ->
            if (confirm) {
                val confirmedEvent = event.copy(confirmed = true)
                synchronized(detectedEvents) {
                    detectedEvents.add(confirmedEvent)
                }
            }
            pendingEvent = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sensorEventManager.isInitialized) {
            sensorEventManager.stopSensorCollection()
        }
        if (::locationManager.isInitialized) {
            locationManager.stopLocationUpdates()
        }
        eventRepository.stopPeriodicUpload()
    }
}