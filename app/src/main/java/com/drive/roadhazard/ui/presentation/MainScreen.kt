package com.drive.roadhazard.ui.presentation

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.drive.roadhazard.R
import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.RoadEvent
import com.drive.roadhazard.data.VehicleType
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MainScreen(
    navController: NavController,
    currentLocation: Location?,
    selectedVehicleType: VehicleType,
    currentSpeed: Float,
    detectedEvents: List<RoadEvent>,
    mapEvents: List<EventResponse>,
    onStopClick: () -> Unit
) {
    var mapCenterTarget by remember { mutableStateOf<GeoPoint?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                }
            },
            update = { mapView ->
                mapCenterTarget?.let { centerPoint ->
                    mapView.controller.animateTo(centerPoint)
                    mapCenterTarget = null
                }
                mapView.overlays.clear()
                mapEvents.forEach { event ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(event.latitude, event.longitude)
                    marker.title = event.type

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
                    mapView.overlays.add(marker)
                }

                currentLocation?.let { location ->
                    val myLocationMarker = Marker(mapView)
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    myLocationMarker.position = geoPoint
                    myLocationMarker.title = "Current Location"
                    myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    myLocationMarker.icon = mapView.context.getDrawable(R.drawable.location)
                    mapView.overlays.add(myLocationMarker)
                    mapView.controller.setZoom(19.0)
                    mapView.controller.setCenter(geoPoint)
                }
                mapView.invalidate()
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .padding(vertical = 18.dp, horizontal = 10.dp)
                    .weight(0.75f),
                containerColor = Color.Red,
                onClick = {
                    onStopClick()
                }
            ) {
                Text(
                    text = "S  T  O  P",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }

            FloatingActionButton(
                modifier = Modifier
                    .padding(vertical = 18.dp, horizontal = 10.dp)
                    .weight(0.25f),
                onClick = {
                    currentLocation?.let {
                        mapCenterTarget = GeoPoint(it.latitude, it.longitude)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.location),
                    contentDescription = "Current Location"
                )
            }
        }

        FloatingActionButton(
            onClick = {},
            shape = CircleShape,
            modifier = Modifier
                .padding(bottom = 82.dp, end = 12.dp)
                .align(Alignment.BottomEnd)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSpeed.toInt().toString(),
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "km/h",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }

        // Details
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clickable(
                    onClick = {
                        navController.navigate(NavigationDestination.DETAILS_SCREEN.name)
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Vehicle: ${selectedVehicleType.name}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${currentLocation?.latitude}, ${currentLocation?.longitude}",
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