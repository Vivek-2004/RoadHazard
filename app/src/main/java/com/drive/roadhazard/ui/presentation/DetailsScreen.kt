package com.drive.roadhazard.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.drive.roadhazard.data.RoadEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailsScreen(
    navController: NavController,
    eventsList: List<RoadEvent>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(eventsList) { event ->
                EventListItem(event = event)
            }
        }
        Button(
            onClick = { navController.navigate(NavigationDestination.MAIN_SCREEN.name) },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Confirm",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun EventListItem(event: RoadEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Type: ${event.type}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(text = "Location: (${event.latitude}, ${event.longitude})")
            Text(text = "Speed: ${event.speed} km/h")
            Text(text = "Time: ${formatTimestamp(event.timestamp)}")
            Text(text = "Confirmed: ${event.confirmed}")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}