package com.drive.roadhazard.managers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import com.drive.roadhazard.sensors.SensorProcessor

class SensorEventManager(
    private val context: Context,
    private val onEventDetected: (RoadEvent) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorProcessor = SensorProcessor()

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var currentLocation: Location? = null
    private var currentSpeed = 0f
    private var selectedVehicleType = VehicleType.TWO_WHEELER
    private var selectedOrientation = PhoneOrientation.MOUNTER

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    fun startSensorCollection(
        vehicleType: VehicleType,
        orientation: PhoneOrientation
    ) {
        selectedVehicleType = vehicleType
        selectedOrientation = orientation

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun stopSensorCollection() {
        sensorManager.unregisterListener(this)
    }

    fun updateVehicleSettings(vehicleType: VehicleType, orientation: PhoneOrientation) {
        selectedVehicleType = vehicleType
        selectedOrientation = orientation
    }

    fun updateLocationAndSpeed(location: Location?, speed: Float) {
        currentLocation = location
        currentSpeed = speed
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val (reorientedX, reorientedY, reorientedZ) = sensorProcessor.reorientAcceleration(
                event.values[0],
                event.values[1],
                event.values[2]
            )
            processSensorData(reorientedZ)
        }
    }

    private fun processSensorData(reorientedZ: Float) {
        currentLocation?.let { location ->
            val detectedType = sensorProcessor.detectEvent(
                reorientedZ, currentSpeed, selectedVehicleType, System.currentTimeMillis()
            )

            detectedType?.let { type ->
                val roadEvent = RoadEvent(
                    type = type,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    confidence = 0.8f,
                    speed = currentSpeed
                )
                onEventDetected(roadEvent)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }
}