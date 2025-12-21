package com.example.sampleproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sampleproject.data.TelemetryData
import com.example.sampleproject.data.TelemetryFormatter

/**
 * Top navigation bar with telemetry data display
 */
@Composable
fun TopNavBar(
    telemetryData: TelemetryData,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Set nav bar gradient colors based on connection status
    val navBarColors = if (telemetryData.connected) {
        listOf(
            Color(0xFF0A0E27),
            Color(0xFF1A1F3A),
            Color(0xFF0F1419)
        )
    } else {
        listOf(
            Color(0xFFBF280A),
            Color(0xFFE84223),
            Color(0xFF8F0E00)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(
                brush = Brush.horizontalGradient(colors = navBarColors)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger menu
            Box {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { menuExpanded = true }
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .width(160.dp)
                        .background(Color(0xFF23232B).copy(alpha = 0.9f))
                ) {
                    DropdownMenuItem(
                        text = { Text("Disconnect", color = Color.White) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Disconnect",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDisconnect()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title
            Text(
                text = "Sample GCS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Telemetry display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection status
                ConnectionStatusWidget(isConnected = telemetryData.connected)

                DividerBlock()

                // Battery
                InfoBlock(
                    icon = Icons.Default.BatteryFull,
                    text = "${telemetryData.batteryPercent ?: "N/A"}%"
                )

                DividerBlock()

                // Voltage & Current
                InfoBlockGroup(
                    icon = Icons.Default.Bolt,
                    lines = listOf(
                        TelemetryFormatter.formatVoltage(telemetryData.voltage),
                        TelemetryFormatter.formatCurrent(telemetryData.currentA)
                    )
                )

                DividerBlock()

                // GPS
                InfoBlockGroup(
                    icon = Icons.Default.SatelliteAlt,
                    lines = listOf(
                        "${telemetryData.sats ?: "N/A"} sats",
                        "${telemetryData.hdop ?: "N/A"} hdop"
                    )
                )

                DividerBlock()

                // Mode & Armed status
                InfoBlockGroup(
                    icon = Icons.Default.Sync,
                    lines = listOf(
                        telemetryData.mode ?: "N/A",
                        if (telemetryData.armed) "Armed" else "Disarmed"
                    )
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusWidget(isConnected: Boolean) {
    val statusColor = if (isConnected) Color.Green else Color.Red
    val statusText = if (isConnected) "Connected" else "Disconnected"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor, shape = CircleShape)
        )
        Text(
            text = statusText,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DividerBlock() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color.White.copy(alpha = 0.3f))
    )
}

@Composable
fun InfoBlock(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun InfoBlockGroup(
    icon: ImageVector,
    lines: List<String>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Column {
            lines.forEach { line ->
                Text(
                    text = line,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

