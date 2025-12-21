# Sample GCS - Ground Control Station

A comprehensive Ground Control Station (GCS) application for drone control using MAVLink protocol over TCP.

## Features

### ✅ Implemented

1. **MAVLink Communication**
   - TCP connection to flight controller
   - Automatic heartbeat management
   - FCU detection and connection monitoring
   - Message parsing from MAVLink common.xml

2. **Telemetry Display**
   - Connection status indicator
   - Battery voltage and percentage
   - Current draw (Amperage)
   - GPS satellite count and HDOP
   - Flight mode display
   - Armed/Disarmed status
   - Altitude (MSL and relative)
   - GPS coordinates (latitude/longitude)
   - Groundspeed and airspeed
   - Heading

3. **Top Navigation Bar**
   - Real-time telemetry display
   - Color-coded connection status
     - Connected: Blue/dark gradient
     - Disconnected: Red gradient
   - All telemetry fields from MAVLink messages

4. **Google Maps Integration**
   - Interactive map with satellite/normal view toggle
   - Drone position marker with real-time updates
   - Camera following drone position
   - Telemetry panel overlay

5. **Single Source of Data (Data.kt)**
   - Centralized data parsing
   - Type-safe parsers for each MAVLink message type:
     - HeartbeatParser - Mode and armed status
     - SysStatusParser - Battery and sensor health
     - BatteryStatusParser - Current consumption
     - GpsRawIntParser - GPS data
     - GlobalPositionIntParser - Position and altitude
     - VfrHudParser - Speed and heading
   - Telemetry formatters for display

6. **Architecture**
   - MVVM architecture
   - Repository pattern for MAVLink communication
   - StateFlow for reactive data updates
   - Kotlin Coroutines for async operations

## Project Structure

```
app/src/main/java/com/example/sampleproject/
├── data/
│   └── Data.kt                    # Single source of telemetry data parsing
├── repository/
│   └── TelemetryRepository.kt     # MAVLink communication layer
├── viewmodel/
│   ├── TelemetryViewModel.kt      # Telemetry state management
│   └── ConnectionViewModel.kt     # (Legacy - can be removed)
├── ui/
│   ├── components/
│   │   └── TopNavBar.kt          # Top navigation with telemetry
│   └── screen/
│       ├── ConnectionScreen.kt    # TCP connection setup
│       └── MainScreen.kt         # Main map view with telemetry
└── navigation/
    ├── Screen.kt                  # Navigation routes
    └── AppNavigation.kt          # NavHost setup
```

## Dependencies

### MAVLink
- `com.divpundir.mavlink:definitions:1.2.8` - MAVLink message definitions
- `com.divpundir.mavlink:connection-tcp:1.2.8` - TCP connection
- `com.divpundir.mavlink:adapter-coroutines:1.2.8` - Coroutines adapter

### Google Maps
- `com.google.android.gms:play-services-maps:19.2.0`
- `com.google.maps.android:maps-compose:4.4.2`
- `com.google.maps.android:android-maps-utils:3.8.2`

### Android Jetpack
- Navigation Compose
- Lifecycle & ViewModel
- Material 3

## Setup Instructions

### 1. Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create/select a project
3. Enable "Maps SDK for Android"
4. Create API credentials (API Key)
5. Add the API key to `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_ACTUAL_API_KEY_HERE" />
```

### 2. Connection Setup

#### For Android Emulator:
- IP Address: `10.0.2.2` (localhost on host machine)
- Port: `5762` (default MAVLink port)

#### For Physical Device:
- Use the actual IP address of the machine running the flight controller
- Ensure device and FC are on the same network

### 3. MAVLink Backend

The app uses **mavlink-kotlin** package:
- Repository: https://github.com/divyanshupundir/mavlink-kotlin.git
- Automatically handles message serialization/deserialization
- Type-safe Kotlin API for MAVLink messages

## Data Flow

1. **Connection Establishment**
   - User enters IP and port in ConnectionScreen
   - TelemetryViewModel creates TelemetryRepository
   - Repository establishes TCP connection
   - Waits for FCU heartbeat

2. **Telemetry Updates**
   - Repository collects MAVLink messages
   - Parsers in Data.kt extract relevant fields
   - Updates flow to TelemetryViewModel
   - UI observes StateFlow and updates reactively

3. **Map Updates**
   - GPS position from GLOBAL_POSITION_INT
   - Camera position updates when drone moves
   - Marker shows current location

## Telemetry Fields in Top Nav Bar

As per requirements, the top navigation bar displays:

1. **Connection Status** - Green dot (connected) / Red dot (disconnected)
2. **Battery** - Percentage from SYS_STATUS
3. **Voltage** - Battery voltage in volts
4. **Current** - Current draw in amperes (from BATTERY_STATUS)
5. **Sat Count** - Number of GPS satellites
6. **HDOP** - GPS horizontal dilution of precision
7. **Mode** - Current flight mode (Auto, Loiter, RTL, etc.)
8. **Armed Status** - Armed/Disarmed state

## MAVLink Messages Used

| Message ID | Message Name | Purpose |
|------------|--------------|---------|
| 0 | HEARTBEAT | Connection status, mode, armed state |
| 1 | SYS_STATUS | Battery, sensors, armable status |
| 24 | GPS_RAW_INT | Satellites, HDOP |
| 33 | GLOBAL_POSITION_INT | Position, altitude |
| 74 | VFR_HUD | Speed, heading, altitude |
| 147 | BATTERY_STATUS | Current consumption |
| 42 | MISSION_CURRENT | Current waypoint |

## Testing

### Simulator Setup (SITL)
1. Run ArduPilot SITL with MAVProxy
2. Configure MAVProxy to output on TCP port 5762:
   ```
   output add 127.0.0.1:5762
   ```
3. Use `10.0.2.2:5762` in emulator

### Hardware Testing
1. Connect flight controller via telemetry radio or WiFi
2. Use companion computer (Raspberry Pi) as TCP bridge
3. Enter companion computer's IP in the app

## Future Enhancements

- Mission planning and upload
- Waypoint display on map
- Geofencing
- Arm/Disarm controls (already implemented in code)
- Mode switching
- Parameter configuration
- Flight log recording
- Video streaming integration

## References

- Previous app codebase: https://github.com/Hrushikesh-Gangisetty/SampleGCS.git
- MAVLink Kotlin: https://github.com/divyanshupundir/mavlink-kotlin.git
- MAVLink protocol: https://mavlink.io/en/
- ArduPilot modes: https://ardupilot.org/copter/docs/flight-modes.html

## License

This project is for educational purposes.

