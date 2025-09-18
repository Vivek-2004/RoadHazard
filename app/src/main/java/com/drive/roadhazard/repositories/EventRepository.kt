package com.drive.roadhazard.repositories

import android.os.Handler
import android.os.Looper
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.EventUpload
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.network.RoadSurpAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EventRepository {

    // API Setup
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://your-backend-api.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api by lazy { retrofit.create(RoadSurpAPI::class.java) }

    // Periodic upload handling
    private val handler = Handler(Looper.getMainLooper())
    private val uploadRunnable = object : Runnable {
        override fun run() {
            uploadPendingEvents()
            handler.postDelayed(this, 600000) // Upload every 60 seconds
        }
    }

    private var pendingEvents: MutableList<RoadEvent>? = null

    fun startPeriodicUpload(detectedEvents: MutableList<RoadEvent>) {
        pendingEvents = detectedEvents
        handler.post(uploadRunnable)
    }

    fun stopPeriodicUpload() {
        handler.removeCallbacks(uploadRunnable)
        pendingEvents = null
    }

    fun uploadPendingEvents() {
        val eventsToUpload = pendingEvents?.let { events ->
            synchronized(events) {
                events.toList()
            }
        } ?: return

        if (eventsToUpload.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.uploadEvents(EventUpload(eventsToUpload))
                    pendingEvents?.let { events ->
                        synchronized(events) {
                            events.clear()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun fetchNearbyEvents(
        lat: Double,
        lng: Double,
        onResult: (List<EventResponse>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = api.getNearbyEvents(lat, lng)
                withContext(Dispatchers.Main) {
                    onResult(events)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }

    // Manual upload method if needed
    fun uploadEvents(events: List<RoadEvent>, onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.uploadEvents(EventUpload(events))
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }
}