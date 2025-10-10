package com.drive.roadhazard.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.drive.roadhazard.data.RoadEvent

@Composable
fun DetailsScreen(
    eventsList: List<RoadEvent>
) {
    Column {
        Text(eventsList.toString())
    }
}