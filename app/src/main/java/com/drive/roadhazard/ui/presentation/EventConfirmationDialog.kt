package com.drive.roadhazard.ui.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drive.roadhazard.data.EventType
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType

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