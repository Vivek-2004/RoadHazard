package com.drive.roadhazard.ui.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.drive.roadhazard.R
import com.drive.roadhazard.viewmodels.MainViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

@Composable
fun MainScreen(
    navController: NavController, viewModel: MainViewModel
) {
    val context = LocalContext.current

    var mapCenterTarget by remember { mutableStateOf<GeoPoint?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var hasCenteredInitially by remember { mutableStateOf(false) }

    val currentLocation = viewModel.currentLocation
    val selectedVehicleType = viewModel.selectedVehicleType
    val currentSpeed = viewModel.currentSpeed
    val detectedEvents = viewModel.detectedEvents.toList()
    val mapEvents = viewModel.mapEvents.toList()
    val pendingEvent = viewModel.pendingEvent
    val activeWarning = viewModel.activeWarning

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = {
                    navController.navigate(NavigationDestination.CONFIGURATION_SCREEN.name)
                    viewModel.stopSensorCollections()
                    viewModel.showStopText = true
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("No") }
            })
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("S T O P") },
            text = { Text("Do you want to stop your journey?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.showStopText = false
                    viewModel.stopSensorCollections()
                    navController.navigate(NavigationDestination.DETAILS_SCREEN.name)
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("No") }
            })
    }

    if (pendingEvent != null) {
        EventConfirmationDialog(
            event = pendingEvent, onConfirm = { viewModel.confirmEvent(it) })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView }, update = { mv ->
            mv.overlays.clear()

            mv.overlays.add(ScaleBarOverlay(mv))

            mv.overlays.add(
                CompassOverlay(
                    context, InternalCompassOrientationProvider(context), mv
                ).apply { enableCompass() })

            // Event markers
            mapEvents.forEach { event ->
                mv.overlays.add(
                    Marker(mv).apply {
                        position = GeoPoint(event.latitude, event.longitude)
                        title = event.type
                        icon = mv.context.getDrawable(R.drawable.speed_breaker)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { marker, _ ->
                            marker.showInfoWindow()
                            true
                        }
                    })
            }

            // Current location marker (NO camera forcing)
            currentLocation?.let {
                val point = GeoPoint(it.latitude, it.longitude)
                mv.overlays.add(
                    Marker(mv).apply {
                        position = point
                        icon = mv.context.getDrawable(R.drawable.location)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Current Location"
                    })

                // ✅ CENTER ONLY ONCE
                if (!hasCenteredInitially) {
                    mv.controller.setZoom(19.0)
                    mv.controller.animateTo(point)
                    hasCenteredInitially = true
                }
            }

            // Manual recenter via FAB
            mapCenterTarget?.let {
                mv.controller.animateTo(it)
                mapCenterTarget = null
            }

            mv.invalidate()
        })

        // --- WARNING NOTIFICATION BANNER ---
        if (activeWarning != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Red),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .wrapContentSize()
            ) {
                Text(
                    text = activeWarning,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .padding(18.dp)
                    .weight(0.75f),
                containerColor = if (viewModel.showStopText) Color.Red else Color.Gray,
                onClick = {
                    if (viewModel.showStopText) showStopDialog = true
                }) {
                Text(
                    text = if (viewModel.showStopText) "S  T  O  P" else "S T O P P E D",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            FloatingActionButton(
                modifier = Modifier
                    .padding(18.dp)
                    .weight(0.25f), onClick = {}) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentSpeed.toInt().toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text("km/h", fontSize = 12.sp, color = Color.LightGray)
                }
            }
        }

        // Recenter FAB
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 82.dp),
            shape = CircleShape,
            onClick = {
                currentLocation?.let {
                    mapCenterTarget = GeoPoint(it.latitude, it.longitude)
                }
            }) {
            Icon(
                painter = painterResource(R.drawable.location), contentDescription = "Recenter"
            )
        }

        // Info card
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(vertical = 88.dp, horizontal = 18.dp)
                .clickable {
                    if (detectedEvents.isNotEmpty()) {
                        navController.navigate(NavigationDestination.DETAILS_SCREEN.name)
                    } else {
                        Toast.makeText(context, "No Hazards Detected Yet !", Toast.LENGTH_SHORT)
                            .show()
                    }
                }, colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Vehicle: ${selectedVehicleType.name}", fontSize = 12.sp, color = Color.Gray)
                Text(
                    "${currentLocation?.latitude}, ${currentLocation?.longitude}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text("Events: ${detectedEvents.size}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}