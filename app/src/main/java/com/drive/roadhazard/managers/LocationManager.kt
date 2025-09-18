package com.drive.roadhazard.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import androidx.core.content.ContextCompat
import android.location.LocationManager as AndroidLocationManager

class LocationManager(
    private val context: Context,
    private val onLocationUpdate: (Location, Float) -> Unit
) : LocationListener {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private var currentLocation: Location? = null

    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                locationManager.requestLocationUpdates(
                    AndroidLocationManager.GPS_PROVIDER,
                    1000,
                    1f,
                    this
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        val speed = if (location.hasSpeed()) {
            location.speed * 3.6f // Convert m/s to km/h
        } else {
            0f
        }
        onLocationUpdate(location, speed)
    }

    fun getCurrentLocation(): Location? = currentLocation
}