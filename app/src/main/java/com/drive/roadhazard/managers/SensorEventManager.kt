package com.drive.roadhazard.managers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import com.drive.roadhazard.Config
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import com.drive.roadhazard.sensors.SensorProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

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

        // --- START: Simulation logic ---
        if (Config.IS_SIMULATION_MODE) {
            // Start a coroutine to generate fake data
            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    // Wait for a random time between 8 and 15 seconds
                    delay(Random.nextLong(8000, 15000))

                    // --- NEW: Tell LocationManager to slow down ---
                    SimulationState.isEventIncoming.value = true
                    delay(500) // Give it a moment to react

                    // --- NEW: Randomly choose an event to simulate ---
                    when (Random.nextInt(3)) {
                        0 -> simulateSpeedBreaker()
                        1 -> simulatePothole()
                        2 -> simulateBrokenPatch()
                    }
                }
            }
        } else {
            // --- Original logic ---
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            }
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
            }
        }
        // --- END: Simulation logic ---
    }

    // --- START: New simulation functions ---
    private fun simulateSpeedBreaker() {
        Log.d("SensorEventManager", "SIMULATING: Speed Breaker")
        val fakePositiveSpike = 25.0f
        val fakeNegativeSpike = -15.0f
        processSimulatedSensorData(fakePositiveSpike)
        processSimulatedSensorData(fakeNegativeSpike)
    }

    private fun simulatePothole() {
        Log.d("SensorEventManager", "SIMULATING: Pothole")
        val fakeNegativeSpike = -20.0f
        processSimulatedSensorData(fakeNegativeSpike)
    }

    private suspend fun simulateBrokenPatch() {
        Log.d("SensorEventManager", "SIMULATING: Broken Patch")
        // A broken patch is a series of smaller, rapid jolts
        repeat(5) {
            val randomJolt = Random.nextDouble(-8.0, 8.0).toFloat()
            processSimulatedSensorData(randomJolt)
            delay(150)
        }
    }
    // --- END: New simulation functions ---

    fun stopSensorCollection() {
        if (!Config.IS_SIMULATION_MODE) {
            sensorManager.unregisterListener(this)
        }
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
            val (reorientedX, reorientedY, reorientedZ) = sensorProcessor.reorientAcceleration(event.values[0], event.values[1], event.values[2])
            processSimulatedSensorData(reorientedZ)
        }
    }

    private fun processSimulatedSensorData(reorientedZ: Float) {
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