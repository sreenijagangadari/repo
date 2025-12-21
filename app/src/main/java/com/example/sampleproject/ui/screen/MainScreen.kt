package com.example.sampleproject.ui.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sampleproject.R
import com.example.sampleproject.data.TelemetryFormatter
import com.example.sampleproject.ui.components.TopNavBar
import com.example.sampleproject.viewmodel.TelemetryViewModel
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * Creates a BitmapDescriptor from a vector drawable with optional rotation
 */
fun getBitmapDescriptor(context: android.content.Context, id: Int, rotation: Float = 0f, size: Int = 96): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, id)!!
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, size, size)
    vectorDrawable.draw(canvas)

    // Apply rotation if needed
    val rotatedBitmap = if (rotation != 0f) {
        val matrix = Matrix()
        matrix.postRotate(rotation, size / 2f, size / 2f)
        Bitmap.createBitmap(bitmap, 0, 0, size, size, matrix, true)
    } else {
        bitmap
    }

    return BitmapDescriptorFactory.fromBitmap(rotatedBitmap)
}

@Composable
fun MainScreen(
    telemetryViewModel: TelemetryViewModel = viewModel(),
    onDisconnect: () -> Unit
) {
    val telemetryState by telemetryViewModel.telemetryState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Map state
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    val defaultLocation = LatLng(17.385044, 78.486671) // Hyderabad, India


    // Camera position - follow drone if GPS available
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            if (telemetryState.latitude != null && telemetryState.longitude != null) {
                LatLng(telemetryState.latitude!!, telemetryState.longitude!!)
            } else {
                defaultLocation
            },
            15f
        )
    }

    // Update camera when drone position changes
    LaunchedEffect(telemetryState.latitude, telemetryState.longitude) {
        if (telemetryState.latitude != null && telemetryState.longitude != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(telemetryState.latitude!!, telemetryState.longitude!!),
                cameraPositionState.position.zoom
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                myLocationButtonEnabled = false
            )
        ) {
            // Drone marker with custom icon
            if (telemetryState.latitude != null && telemetryState.longitude != null) {
                // Create drone icon inside GoogleMap where BitmapDescriptorFactory is initialized
                val droneIcon = remember(telemetryState.heading) {
                    getBitmapDescriptor(context, R.drawable.ic_drone, telemetryState.heading, 120)
                }

                Marker(
                    state = MarkerState(
                        position = LatLng(telemetryState.latitude!!, telemetryState.longitude!!)
                    ),
                    icon = droneIcon,
                    flat = true,
                    title = "Drone",
                    snippet = "Alt: ${TelemetryFormatter.formatAltitude(telemetryState.altitudeRelative)} | Hdg: ${telemetryState.heading.toInt()}°"
                )
            }
        }

        // Top navigation bar
        TopNavBar(
            telemetryData = telemetryState,
            onDisconnect = onDisconnect,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Map type toggle button
        SmallFloatingActionButton(
            onClick = {
                mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 12.dp),
            containerColor = Color(0xFF1A1D26).copy(alpha = 0.9f),
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (mapType == MapType.NORMAL) Icons.Default.Satellite else Icons.Default.Map,
                contentDescription = "Toggle Map Type",
                modifier = Modifier.size(20.dp)
            )
        }

        // Recenter button
        SmallFloatingActionButton(
            onClick = {
                if (telemetryState.latitude != null && telemetryState.longitude != null) {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                LatLng(telemetryState.latitude!!, telemetryState.longitude!!),
                                17f
                            )
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 124.dp, end = 12.dp),
            containerColor = Color(0xFF1A1D26).copy(alpha = 0.9f),
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter on Drone",
                modifier = Modifier.size(20.dp)
            )
        }

        // Telemetry panel (bottom) - compact version
        TelemetryPanel(
            telemetryState = telemetryState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // Arm/Disarm button
        if (telemetryState.connected) {
            SmallFloatingActionButton(
                onClick = {
                    if (telemetryState.armed) {
                        telemetryViewModel.disarm()
                    } else {
                        telemetryViewModel.arm()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 12.dp),
                containerColor = if (telemetryState.armed) Color(0xFFE53935) else Color(0xFF43A047),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (telemetryState.armed) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (telemetryState.armed) "Disarm" else "Arm",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TelemetryPanel(
    telemetryState: com.example.sampleproject.data.TelemetryData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1D26).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Altitude
            TelemetryItem(
                label = "ALT",
                value = "%.1f".format(telemetryState.altitudeRelative),
                unit = "m"
            )

            VerticalDivider()

            // Ground Speed
            TelemetryItem(
                label = "GS",
                value = "%.1f".format(telemetryState.groundspeed ?: 0f),
                unit = "m/s"
            )

            VerticalDivider()

            // Heading
            TelemetryItem(
                label = "HDG",
                value = "${telemetryState.heading.toInt()}",
                unit = "°"
            )

            VerticalDivider()

            // Coordinates
            CoordinateItem(
                lat = telemetryState.latitude,
                lon = telemetryState.longitude
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color.White.copy(alpha = 0.2f))
    )
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFF8E9AAF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                color = Color(0xFF8E9AAF),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
fun CoordinateItem(
    lat: Double?,
    lon: Double?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COORD",
            color = Color(0xFF8E9AAF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Text(
            text = lat?.let { "%.4f".format(it) } ?: "---",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = lon?.let { "%.4f".format(it) } ?: "---",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

