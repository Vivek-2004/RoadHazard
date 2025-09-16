package com.drive.roadhazard.ui.screens

import android.location.Location
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.EventType
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RoadSurP Monitor",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = {
                if (username.isNotBlank() && password.isNotBlank()) {
                    onLoginSuccess(username)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = if (isRegistering) "Register" else "Login",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(
            onClick = { isRegistering = !isRegistering },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = if (isRegistering) "Already have an account? Login" else "Don't have an account? Register",
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun VehicleSelectionScreen(
    selectedVehicleType: VehicleType,
    selectedOrientation: PhoneOrientation,
    onVehicleTypeChange: (VehicleType) -> Unit,
    onOrientationChange: (PhoneOrientation) -> Unit,
    onSelectionComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select Your Vehicle",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Vehicle type selection
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(VehicleType.values()) { vehicle ->
                VehicleCard(
                    vehicleType = vehicle,
                    isSelected = selectedVehicleType == vehicle,
                    onSelect = { onVehicleTypeChange(vehicle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Phone Orientation",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Phone orientation selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PhoneOrientation.values().forEach { orientation ->
                OrientationCard(
                    orientation = orientation,
                    isSelected = selectedOrientation == orientation,
                    onSelect = { onOrientationChange(orientation) }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSelectionComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Text(
                text = "Start Detection",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MapScreen(
    currentLocation: Location?,
    currentUser: String,
    selectedVehicleType: VehicleType,
    currentSpeed: Float,
    detectedEvents: List<RoadEvent>,
    mapEvents: List<EventResponse>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        println(currentLocation)
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

@Composable
fun VehicleCard(
    vehicleType: VehicleType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val vehicleInfo = when (vehicleType) {
        VehicleType.TWO_WHEELER -> "🏍️" to "Two Wheeler"
        VehicleType.THREE_WHEELER -> "🛺" to "Three Wheeler"
        VehicleType.FOUR_WHEELER -> "🚗" to "Four Wheeler"
        VehicleType.BUS -> "🚌" to "Bus"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vehicleInfo.first,
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = vehicleInfo.second,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun OrientationCard(
    orientation: PhoneOrientation,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val orientationInfo = when (orientation) {
        PhoneOrientation.POCKET -> "📱" to "Pocket"
        PhoneOrientation.MOUNTER -> "📱" to "Mounter"
        PhoneOrientation.DASHBOARD -> "📱" to "Dashboard"
    }

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp)
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFF4CAF50) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E8) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = orientationInfo.first,
                fontSize = 24.sp
            )
            Text(
                text = orientationInfo.second,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EventConfirmationDialog(
    event: RoadEvent,
    onConfirm: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onConfirm(false) },
        title = {
            Text(
                text = "Road Event Detected!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                val eventIcon = when (event.type) {
                    EventType.SPEED_BREAKER -> "⚠️"
                    EventType.POTHOLE -> "🕳️"
                    EventType.BROKEN_PATCH -> "🚧"
                }

                Text(
                    text = "$eventIcon ${event.type.name.replace("_", " ")} detected",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Location: ${
                        String.format(
                            "%.6f",
                            event.latitude
                        )
                    }, ${String.format("%.6f", event.longitude)}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Speed: ${event.speed.toInt()} km/h",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Confidence: ${(event.confidence * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(true) }
            ) {
                Text("Yes", color = Color(0xFF4CAF50))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm(false) }
            ) {
                Text("No", color = Color(0xFFF44336))
            }
        }
    )
}