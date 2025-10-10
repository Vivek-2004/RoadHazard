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

    // State
    var currentLocation by mutableStateOf<Location?>(null)
    var currentSpeed by mutableStateOf(0f)
    var selectedVehicleType by mutableStateOf(VehicleType.TWO_WHEELER)
    var selectedOrientation by mutableStateOf(PhoneOrientation.MOUNTER)
    var isLoggedIn by mutableStateOf(true)
    var jwt by mutableStateOf("")
    val detectedEvents = mutableListOf<RoadEvent>()
    val mapEvents = mutableStateListOf<EventResponse>()
    var pendingEvent by mutableStateOf<RoadEvent?>(null)
    var permissionsGranted by mutableStateOf(false)

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

    fun onLoginSuccess(_jwt: String) {
        jwt = _jwt
        isLoggedIn = true
        if (permissionsGranted) {
            startSensorCollection()
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        permissionsGranted = isGranted
        if (isGranted && isLoggedIn) {
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
        eventRepository.startPeriodicUpload(detectedEvents)
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

    var isRegisterSuccess by mutableStateOf(false)


    fun signUp(email: String, password: String, name: String, phoneNumber: String) {
        viewModelScope.launch {
            isRegisterSuccess = eventRepository.signUp(email, password, name, phoneNumber)
        }
    }

    fun signIn(email: String, password: String) {
        println("vivek")
        Log.e("EventRepository", "Login Button Clicked")
        viewModelScope.launch {
            jwt = eventRepository.signIn(email, password)
        }
    }

    fun reportNewHazard(latitude: Double, longitude: Double, type: String, description: String? = null) {
        viewModelScope.launch {
            eventRepository.reportHazard(jwt, latitude, longitude, type, description)
        }
    }
}