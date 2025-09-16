package com.drive.roadhazard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.drive.roadhazard.data.EventUpload
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import com.drive.roadhazard.network.RoadSurpAPI
import com.drive.roadhazard.sensors.SensorProcessor
import com.drive.roadhazard.ui.screens.EventConfirmationDialog
import com.drive.roadhazard.ui.screens.LoginScreen
import com.drive.roadhazard.ui.screens.MapScreen
import com.drive.roadhazard.ui.screens.VehicleSelectionScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity(), SensorEventListener, LocationListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private val sensorProcessor = SensorProcessor()
    private var currentLocation: Location? = null
    private var currentSpeed = 0f
    private var selectedVehicleType by mutableStateOf(VehicleType.TWO_WHEELER)
    private var selectedOrientation by mutableStateOf(PhoneOrientation.MOUNTER)
    private var isLoggedIn by mutableStateOf(true) // make it false
    private var currentUser by mutableStateOf("")
    private val detectedEvents = mutableListOf<RoadEvent>()
    private val mapEvents = mutableStateListOf<EventResponse>()
    private var pendingEvent by mutableStateOf<RoadEvent?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted && isLoggedIn) {
            startSensorCollection()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val uploadRunnable = object : Runnable {
        override fun run() {
            uploadPendingEvents()
            handler.postDelayed(this, 60000) // Upload every 60 seconds
        }
    }

    // API Setup
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://your-backend-api.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api by lazy { retrofit.create(RoadSurpAPI::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize sensors and location
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Request permissions
        requestPermissions()

        // Initialize OSMDroid
        Configuration.getInstance()
            .load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        setContent {
            RoadSurpTheme {
                if (isLoggedIn) {
                    MainScreen {
                        startSensorCollection()
                    }
                } else {
                    LoginScreen { username ->
                        currentUser = username
                        isLoggedIn = true
                        startSensorCollection()
                    }
                }
            }
        }
    }

    @Composable
    private fun RoadSurpTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            content = content
        )
    }

    @Composable
    private fun MainScreen(testLambda: () -> Unit) {
        testLambda()
        var currentScreen by remember { mutableStateOf("vehicle_selection") }

        when (currentScreen) {
            "vehicle_selection" -> VehicleSelectionScreen(
                selectedVehicleType = selectedVehicleType,
                selectedOrientation = selectedOrientation,
                onVehicleTypeChange = { selectedVehicleType = it },
                onOrientationChange = { selectedOrientation = it },
                onSelectionComplete = { currentScreen = "map" }
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
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startSensorCollection() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    1f,
                    this
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        handler.post(uploadRunnable)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val (x, y, z) = sensorProcessor.reorientAcceleration(
                    event.values[0], event.values[1], event.values[2]
                )

                currentLocation?.let { location ->
                    val detectedType = sensorProcessor.detectEvent(
                        z, currentSpeed, selectedVehicleType, System.currentTimeMillis()
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

                        // Show confirmation dialog
                        pendingEvent = roadEvent
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        currentSpeed = if (location.hasSpeed()) {
            location.speed * 3.6f // Convert m/s to km/h
        } else {
            0f
        }

        // Fetch nearby events
        fetchNearbyEvents(location.latitude, location.longitude)
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

    private fun uploadPendingEvents() {
        val eventsToUpload = synchronized(detectedEvents) {
            detectedEvents.toList()
        }

        if (eventsToUpload.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.uploadEvents(EventUpload(eventsToUpload))
                    synchronized(detectedEvents) {
                        detectedEvents.clear()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun fetchNearbyEvents(lat: Double, lng: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = api.getNearbyEvents(lat, lng)
                withContext(Dispatchers.Main) {
                    mapEvents.clear()
                    mapEvents.addAll(events)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(uploadRunnable)
    }
}