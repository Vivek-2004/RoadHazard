package com.drive.roadhazard.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.location.LocationManager as AndroidLocationManager

class LocationManager(
    private val context: Context,
    private val onLocationUpdate: (Location, Float) -> Unit
) {

    companion object {
        private const val TAG = "LocationManager"
        private const val LOCATION_REQUEST_INTERVAL = 2000L // 2 seconds
        private const val FASTEST_INTERVAL = 1000L // 1 second
        private const val MIN_DISTANCE = 5f // 5 meters
    }

    // Use Google Play Services Location API (more accurate)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null

    // Fallback to system LocationManager
    private val systemLocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private var currentLocation: Location? = null
    private var isLocationUpdatesActive = false

    init {
        initializeGooglePlayServices()
    }

    private fun initializeGooglePlayServices() {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            locationRequest =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_REQUEST_INTERVAL)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                    .setMaxUpdateDelayMillis(LOCATION_REQUEST_INTERVAL * 2)
                    .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        handleLocationUpdate(location)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    Log.d(TAG, "Location availability: ${availability.isLocationAvailable}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Google Play Services Location: ${e.message}")
        }
    }

    fun startLocationUpdates() {
        if (isLocationUpdatesActive) {
            Log.d(TAG, "Location updates already active")
            return
        }

        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        Log.d(TAG, "Starting location updates")

        try {
            // Try Google Play Services first
            startGooglePlayServicesLocation()
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Google Play Services failed, falling back to system LocationManager: ${e.message}"
            )
            startSystemLocationManager()
        }
    }

    private fun startGooglePlayServicesLocation() {
        if (!::fusedLocationClient.isInitialized || locationCallback == null) {
            throw Exception("Google Play Services not properly initialized")
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                Log.d(TAG, "Google Play Services location updates started successfully")
                isLocationUpdatesActive = true

                // Get last known location immediately
                getLastKnownLocation()
            }.addOnFailureListener { exception ->
                Log.e(
                    TAG,
                    "Failed to start Google Play Services location updates: ${exception.message}"
                )
                startSystemLocationManager()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when starting location updates: ${e.message}")
            throw e
        }
    }

    private fun startSystemLocationManager() {
        try {
            systemLocationManager.requestLocationUpdates(
                AndroidLocationManager.GPS_PROVIDER,
                LOCATION_REQUEST_INTERVAL,
                MIN_DISTANCE,
                systemLocationListener
            )

            // Also request network provider for faster initial fix
            if (systemLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)) {
                systemLocationManager.requestLocationUpdates(
                    AndroidLocationManager.NETWORK_PROVIDER,
                    LOCATION_REQUEST_INTERVAL,
                    MIN_DISTANCE,
                    systemLocationListener
                )
            }

            Log.d(TAG, "System LocationManager started successfully")
            isLocationUpdatesActive = true

            // Get last known location
            getLastKnownLocationFromSystem()

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when starting system location updates: ${e.message}")
        }
    }

    private val systemLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Provider disabled: $provider")
        }
    }

    private fun getLastKnownLocation() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    Log.d(TAG, "Got last known location from Google Play Services")
                    handleLocationUpdate(it)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get last known location: ${exception.message}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when getting last known location: ${e.message}")
        }
    }

    private fun getLastKnownLocationFromSystem() {
        if (!hasLocationPermission()) return

        try {
            val gpsLocation =
                systemLocationManager.getLastKnownLocation(AndroidLocationManager.GPS_PROVIDER)
            val networkLocation =
                systemLocationManager.getLastKnownLocation(AndroidLocationManager.NETWORK_PROVIDER)

            // Use the most recent location
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }

                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

            bestLocation?.let {
                Log.d(TAG, "Got last known location from system LocationManager")
                handleLocationUpdate(it)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when getting last known location: ${e.message}")
        }
    }

    private fun handleLocationUpdate(location: Location) {
        currentLocation = location
        val speed = if (location.hasSpeed()) {
            location.speed * 3.6f // Convert m/s to km/h
        } else {
            0f
        }

        Log.d(
            TAG,
            "Location updated: lat=${location.latitude}, lng=${location.longitude}, speed=${speed}km/h"
        )
        onLocationUpdate(location, speed)
    }

    fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            Log.d(TAG, "Location updates not active")
            return
        }

        Log.d(TAG, "Stopping location updates")

        try {
            // Stop Google Play Services location updates
            if (::fusedLocationClient.isInitialized && locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback!!)
            }

            // Stop system location manager updates
            systemLocationManager.removeUpdates(systemLocationListener)

            isLocationUpdatesActive = false
            Log.d(TAG, "Location updates stopped successfully")

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when stopping location updates: ${e.message}")
        }
    }

    fun getCurrentLocation(): Location? = currentLocation

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationAvailable(): Boolean {
        return currentLocation != null
    }
}