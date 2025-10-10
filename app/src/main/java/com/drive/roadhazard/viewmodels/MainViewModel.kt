package com.drive.roadhazard.viewmodels

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import com.drive.roadhazard.managers.LocationManager
import com.drive.roadhazard.managers.SensorEventManager
import com.drive.roadhazard.repositories.EventRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Managers
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
    val detectedEvents = mutableListOf<RoadEvent>()
    val mapEvents = mutableStateListOf<EventResponse>()
    var pendingEvent by mutableStateOf<RoadEvent?>(null)

    init {
        locationManager = LocationManager(application) { location, speed ->
            currentLocation = location
            currentSpeed = speed
            sensorEventManager.updateLocationAndSpeed(location, speed)
            fetchNearbyEvents(location)
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

    fun reportNewHazard(latitude: Double, longitude: Double, type: String, description: String? = null) {
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

    private fun fetchNearbyEvents(location: Location) {
        viewModelScope.launch {
            eventRepository.fetchNearbyEvents(
                location.latitude,
                location.longitude
            ) { events ->
                mapEvents.clear()
                mapEvents.addAll(events)
            }
        }
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

    fun stopSensorCollections() {
        sensorEventManager.stopSensorCollection()
        locationManager.stopLocationUpdates()
        eventRepository.stopPeriodicUpload()
    }

    override fun onCleared() {
        super.onCleared()
        sensorEventManager.stopSensorCollection()
        locationManager.stopLocationUpdates()
        eventRepository.stopPeriodicUpload()
    }

}