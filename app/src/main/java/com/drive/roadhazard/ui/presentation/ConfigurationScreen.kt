package com.drive.roadhazard.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drive.roadhazard.data.PhoneOrientation
import com.drive.roadhazard.data.VehicleType

@Composable
fun ConfigurationScreen(
    selectedVehicleType: VehicleType,
    selectedOrientation: PhoneOrientation,
    onVehicleTypeChange: (VehicleType) -> Unit,
    onOrientationChange: (PhoneOrientation) -> Unit,
    onSelectionComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 10.dp),
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
            items(VehicleType.entries.toTypedArray()) { vehicle ->
                VehicleCard(
                    vehicleType = vehicle,
                    isSelected = selectedVehicleType == vehicle,
                    onSelect = { onVehicleTypeChange(vehicle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))

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
            PhoneOrientation.entries.forEach { orientation ->
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