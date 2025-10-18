package com.drive.roadhazard.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.drive.roadhazard.R
import com.drive.roadhazard.data.EventType
import com.drive.roadhazard.data.RoadEvent

@Composable
fun EventConfirmationDialog(
    event: RoadEvent,
    onConfirm: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = { onConfirm(false) }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Event Detected!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                val eventIcon = when (event.type) {
                    EventType.SPEED_BREAKER -> "⚠️"
                    EventType.POTHOLE -> "🕳️"
                    EventType.BROKEN_PATCH -> "🚧"
                }

                Text(
                    text = "$eventIcon ${event.type.name.replace("_", " ")}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InfoRow(
                    "Location",
                    "${String.format("%.6f", event.latitude)}, ${
                        String.format(
                            "%.6f",
                            event.longitude
                        )
                    }"
                )
                InfoRow("Speed", "${event.speed.toInt()} km/h")

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onConfirm(false) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "No",
                            modifier = Modifier.size(20.dp)
                        )
                        Text("No", modifier = Modifier.padding(start = 8.dp))
                    }
                    TextButton(
                        onClick = { onConfirm(true) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = "Yes",
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Yes", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
    }
}