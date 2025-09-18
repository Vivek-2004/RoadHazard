package com.drive.roadhazard.repositories

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.EventUpload
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.network.RoadHazardAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class EventRepository {

    companion object {
        private const val TAG = "EventRepository"

        // Replace with your actual backend URL
        private const val BASE_URL =
            "http://192.168.1.100:8080/api/" // Use your local IP for testing
        // For production: private const val BASE_URL = "https://your-actual-domain.com/api/"
    }

    // Enhanced HTTP Client with logging
    private val httpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, "Network: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // API Setup with proper error handling
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api by lazy { retrofit.create(RoadHazardAPI::class.java) }

    // Periodic upload handling
    private val handler = Handler(Looper.getMainLooper())
    private val uploadRunnable = object : Runnable {
        override fun run() {
            uploadPendingEvents()
            handler.postDelayed(this, 60000) // Upload every 60 seconds
        }
    }

    private var pendingEvents: MutableList<RoadEvent>? = null
    private var isUploadRunning = false

    fun startPeriodicUpload(detectedEvents: MutableList<RoadEvent>) {
        if (isUploadRunning) return

        pendingEvents = detectedEvents
        isUploadRunning = true
        handler.post(uploadRunnable)
        Log.d(TAG, "Started periodic upload")
    }

    fun stopPeriodicUpload() {
        handler.removeCallbacks(uploadRunnable)
        pendingEvents = null
        isUploadRunning = false
        Log.d(TAG, "Stopped periodic upload")
    }

    fun uploadPendingEvents() {
        val eventsToUpload = pendingEvents?.let { events ->
            synchronized(events) {
                events.filter { it.confirmed }.toList() // Only upload confirmed events
            }
        } ?: return

        if (eventsToUpload.isNotEmpty()) {
            Log.d(TAG, "Uploading ${eventsToUpload.size} events")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.uploadEvents(EventUpload(eventsToUpload))
                    Log.d(TAG, "Upload successful: ${response.size} events processed")

                    // Remove uploaded events from pending list
                    pendingEvents?.let { events ->
                        synchronized(events) {
                            events.removeAll(eventsToUpload.toSet())
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Upload failed: ${e.message}", e)
                    // Events remain in pending list for retry
                }
            }
        }
    }

    fun fetchNearbyEvents(
        lat: Double,
        lng: Double,
        radius: Double = 5.0,
        onResult: (List<EventResponse>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Fetching nearby events for location: $lat, $lng")
                val events = api.getNearbyEvents(lat, lng, radius)

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Fetched ${events.size} nearby events")
                    onResult(events)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch nearby events: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }

    // Manual upload method for immediate uploads
    fun uploadEvents(events: List<RoadEvent>, onResult: (Boolean) -> Unit) {
        if (events.isEmpty()) {
            onResult(true)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.uploadEvents(EventUpload(events))
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Manual upload successful")
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Manual upload failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    // Test connection method
    fun testConnection(onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try to fetch nearby events for a test location
                api.getNearbyEvents(0.0, 0.0, 1.0)
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Connection test successful")
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }
}