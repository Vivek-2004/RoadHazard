package com.drive.roadhazard.ui.presentation

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MainScreen(
    currentLocation: Location?,
    currentUser: String,
    selectedVehicleType: VehicleType,
    currentSpeed: Float,
    detectedEvents: List<RoadEvent>,
    mapEvents: List<EventResponse>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    currentLocation?.let { location ->
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(location.latitude, location.longitude))
                    }

                    // Add markers for detected events
                    mapEvents.forEach { event ->
                        val marker = Marker(this)
                        marker.position = GeoPoint(event.latitude, event.longitude)
                        marker.title = event.type

                        // Set marker icon based on event type
                        when (event.type.lowercase()) {
                            "pothole" -> marker.setAnchor(
                                Marker.ANCHOR_CENTER,
                                Marker.ANCHOR_BOTTOM
                            )

                            "speed_breaker" -> marker.setAnchor(
                                Marker.ANCHOR_CENTER,
                                Marker.ANCHOR_BOTTOM
                            )

                            "broken_patch" -> marker.setAnchor(
                                Marker.ANCHOR_CENTER,
                                Marker.ANCHOR_BOTTOM
                            )
                        }

                        overlays.add(marker)
                    }

                    invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Status overlay
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "User: $currentUser",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Vehicle: ${selectedVehicleType.name}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Speed: ${currentSpeed.toInt()} km/h",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Events: ${detectedEvents.size}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}