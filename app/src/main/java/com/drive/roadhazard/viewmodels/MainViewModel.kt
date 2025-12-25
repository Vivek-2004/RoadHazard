package com.drive.roadhazard.viewmodels

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.EventType
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import com.drive.roadhazard.managers.LocationManager
import com.drive.roadhazard.managers.SensorEventManager
import com.drive.roadhazard.repositories.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val eventRepository = EventRepository()
    private val locationManager: LocationManager
    private val sensorEventManager: SensorEventManager

    var permissionsGranted by mutableStateOf(false)

    var isLoggedIn by mutableStateOf(true)
    var isRegisterSuccess by mutableStateOf(false)
    var jwt by mutableStateOf("")

    // State
    var currentLocation by mutableStateOf<Location?>(null)
    var currentSpeed by mutableStateOf(0f)
    var selectedVehicleType by mutableStateOf(VehicleType.TWO_WHEELER)
    var selectedOrientation by mutableStateOf(PhoneOrientation.MOUNTER)
    var detectedEvents = mutableListOf<RoadEvent>()
    val mapEvents = mutableStateListOf<EventResponse>()
    var pendingEvent by mutableStateOf<RoadEvent?>(null)
    var showStopText by mutableStateOf(true)

    // New state for Warning Notification
    var activeWarning by mutableStateOf<String?>(null)

    // Buffer for speed breakers to detect multiple events
    private val recentSpeedBreakers = mutableListOf<RoadEvent>()
    private var speedBreakerTimerJob: Job? = null

    init {
        locationManager = LocationManager(application) { location, speed ->
            currentLocation = location
            currentSpeed = speed
            sensorEventManager.updateLocationAndSpeed(location, speed)
            // Check for hazards nearby
            checkProximityToHazards(location)
        }

        sensorEventManager = SensorEventManager(application) { roadEvent ->
            if (currentSpeed < 7) {
                Log.d("MainViewModel", "Event detected below 5 km/h, ignoring.")
                return@SensorEventManager
            }

            when (roadEvent.type) {
                EventType.POTHOLE -> {
                    // Show potholes immediately (if speed is sufficient)
                    pendingEvent = roadEvent
                }

                EventType.SPEED_BREAKER -> {
                    recentSpeedBreakers.add(roadEvent)
                    // If a timer isn't already running, start one
                    if (speedBreakerTimerJob == null || speedBreakerTimerJob?.isCompleted == true) {
                        speedBreakerTimerJob = viewModelScope.launch {
                            delay(2000) // Wait 2 seconds from the *first* event
                            if (recentSpeedBreakers.size > 1) {
                                // More than one detected in 2s
                                val representativeEvent = recentSpeedBreakers.last()
                                pendingEvent =
                                    representativeEvent.copy(type = EventType.MULTIPLE_SPEED_BREAKERS)
                            } else if (recentSpeedBreakers.size == 1) {
                                pendingEvent = recentSpeedBreakers.first()
                            }
                            recentSpeedBreakers.clear()
                        }
                    }
                }

                else -> {
                    pendingEvent = roadEvent
                }
            }
        }

        // --- FETCH ALERT SYSTEM DATA ON INIT ---
        val sharedPref = application.getSharedPreferences("Road", Context.MODE_PRIVATE)
        val savedJwt = sharedPref.getString("jwt", "")
        if (!savedJwt.isNullOrBlank()) {
            jwt = savedJwt
            isLoggedIn = true
            fetchHazards()
        }
    }

    private fun checkProximityToHazards(location: Location) {
        // Find hazards within 20 meters
        val nearbyHazard = mapEvents.firstOrNull { event ->
            val eventLoc = Location("").apply {
                latitude = event.latitude
                longitude = event.longitude
            }
            location.distanceTo(eventLoc) <= 30.0f
        }

        activeWarning = nearbyHazard?.let {
            val typeName = when (it.type) {
                "speed_breaker" -> "Speed Breaker"
                "pothole" -> "Pothole"
                "broken_patch" -> "Broken Patch"
                else -> "Hazard"
            }
            "⚠️ $typeName Ahead ⚠️"
        }
    }

    fun fetchHazards() {
        viewModelScope.launch {
            if (jwt.isNotBlank()) {
                val remoteHazards = eventRepository.getAllHazards(jwt)
                mapEvents.clear()
                remoteHazards.forEach { hazard ->
                    // Map API hazard types to local types expected by UI/Logic
                    val mappedType = when (hazard.hazardType) {
                        "SINGLE_SPEED_BUMP" -> "speed_breaker"
                        "MULTIPLE_SPEED_BUMP" -> "speed_breaker"
                        "POTHOLE" -> "pothole"
                        "ROAD_PATCH" -> "broken_patch"
                        else -> "unknown"
                    }
                    mapEvents.add(
                        EventResponse(
                            latitude = hazard.latitude,
                            longitude = hazard.longitude,
                            type = mappedType
                        )
                    )
                }
                Log.d("MainViewModel", "Fetched ${mapEvents.size} hazards from backend.")
            }
        }
    }

    fun signUp(email: String, password: String, name: String, phoneNumber: String) {
        viewModelScope.launch {
            isRegisterSuccess = eventRepository.signUp(email, password, name, phoneNumber)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            jwt = eventRepository.signIn(email, password)
        }
    }

    fun reportNewHazard(
        latitude: Double,
        longitude: Double,
        type: String,
        description: String? = null
    ) {
        viewModelScope.launch {
            if (jwt.isNotEmpty()) {
                eventRepository.reportHazard(jwt, latitude, longitude, type, description)
                Log.d("MainViewModel", "Reported hazard: $type at $latitude, $longitude")
            } else {
                Log.e("MainViewModel", "Failed to report hazard: JWT Token is missing")
            }
        }
    }

    fun onLoginSuccess(_jwt: String) {
        jwt = _jwt
        isLoggedIn = true
        fetchHazards() // Fetch hazards when login succeeds
    }

    fun onPermissionResult(isGranted: Boolean) {
        permissionsGranted = isGranted
    }

    fun onVehicleSelectionComplete() {
        sensorEventManager.updateVehicleSettings(
            selectedVehicleType,
            selectedOrientation
        )
        if (permissionsGranted) {
            startSensorCollection()
        }
    }

    fun startSensorCollection() {
        if (!permissionsGranted) return
        sensorEventManager.startSensorCollection(
            selectedVehicleType,
            selectedOrientation
        )
        locationManager.startLocationUpdates()
    }

    fun stopSensorCollections() {
        sensorEventManager.stopSensorCollection()
        locationManager.stopLocationUpdates()
    }

    fun confirmEvent(confirm: Boolean) {
        pendingEvent?.let { event ->
            if (confirm) {
                val confirmedEvent = event.copy(confirmed = true)
                synchronized(detectedEvents) {
                    detectedEvents.add(confirmedEvent)
                }

                // Map Android EventType to API Strings
                val apiType = when (event.type) {
                    EventType.POTHOLE -> "POTHOLE"
                    EventType.SPEED_BREAKER -> "SINGLE_SPEED_BUMP"
                    EventType.MULTIPLE_SPEED_BREAKERS -> "MULTIPLE_SPEED_BUMP"
                    EventType.BROKEN_PATCH -> "ROAD_PATCH"
                }

                // Send the report to the backend using the stored JWT
                reportNewHazard(
                    latitude = event.latitude,
                    longitude = event.longitude,
                    type = apiType
                )
            }
            pendingEvent = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensorCollections()
    }
}