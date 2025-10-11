package com.drive.roadhazard.viewmodels

import android.app.Application
import android.location.Location
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

    var testList = listOf(
        RoadEvent(
            type = EventType.POTHOLE,
            latitude = 22.5726,
            longitude = 88.3639,
            timestamp = System.currentTimeMillis() - 600000,
            confidence = 0.92f,
            speed = 45.5f
        ),
        RoadEvent(
            type = EventType.SPEED_BREAKER,
            latitude = 22.5742,
            longitude = 88.3701,
            timestamp = System.currentTimeMillis() - 1200000,
            confidence = 0.87f,
            speed = 38.2f
        ),
        RoadEvent(
            type = EventType.SPEED_BREAKER,
            latitude = 22.5689,
            longitude = 88.3611,
            timestamp = System.currentTimeMillis() - 300000,
            confidence = 0.96f,
            speed = 22.0f,
            confirmed = true
        ),
        RoadEvent(
            type = EventType.BROKEN_PATCH,
            latitude = 22.5795,
            longitude = 88.3724,
            timestamp = System.currentTimeMillis() - 900000,
            confidence = 0.75f,
            speed = 10.0f
        ),
        RoadEvent(
            type = EventType.POTHOLE,
            latitude = 22.5803,
            longitude = 88.3698,
            timestamp = System.currentTimeMillis() - 1800000,
            confidence = 0.83f,
            speed = 5.0f,
            confirmed = true
        )
    )

    init {
        detectedEvents.addAll(testList)
        locationManager = LocationManager(application) { location, speed ->
            currentLocation = location
            currentSpeed = speed
            sensorEventManager.updateLocationAndSpeed(location, speed)
        }

        sensorEventManager = SensorEventManager(application) { roadEvent ->
            pendingEvent = roadEvent
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
            eventRepository.reportHazard(jwt, latitude, longitude, type, description)
        }
    }

    fun onLoginSuccess(_jwt: String) {
        jwt = _jwt
        isLoggedIn = true
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
            }
            pendingEvent = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensorCollections()
    }

}