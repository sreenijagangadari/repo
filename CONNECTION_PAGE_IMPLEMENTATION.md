# GCS Application - Connection Page Implementation

## Overview
This implementation creates a Ground Control Station (GCS) application with a connection page following MVVM architecture.

## Features Implemented

### 1. Connection Screen (`ConnectionScreen.kt`)
- **Heading**: "Connection" displayed prominently at the top
- **IP Address Field**: Text input for entering the IP address (default: 192.168.1.100)
- **Port Number Field**: Numeric text input for entering the port number (default: 5760)
- **Connect Button**: Initiates TCP connection with loading state
- **Error Dialog**: Displays "Connection failed" popup when connection fails
- **Proper UI/UX**:
  - Card-based layout with elevation and rounded corners
  - Proper spacing and alignment
  - Material Design 3 components
  - Loading indicator during connection
  - Responsive layout

### 2. Connection Logic (`ConnectionViewModel.kt`)
- **MVVM Architecture**: ViewModel manages connection state
- **TCP Connection**: Establishes socket connection to specified IP and port
- **Heartbeat Validation**: 
  - Listens for MAVLink heartbeat packets after connection
  - Waits up to 5 seconds for heartbeat
  - Only proceeds to main page if heartbeat is received
  - Shows error if no heartbeat detected
- **Input Validation**:
  - Validates IP address is not empty
  - Validates port number is between 1-65535
- **State Management**: Uses StateFlow for reactive UI updates
- **Error Handling**: Comprehensive error messages for various failure scenarios

### 3. Navigation (`AppNavigation.kt`)
- Connection page as the starting screen
- Navigates to Main page only on successful connection
- Removes connection screen from back stack after successful connection

### 4. Main Screen Placeholder (`MainScreen.kt`)
- Placeholder for the map UI and telemetry data (to be implemented next)

## File Structure
```
app/src/main/java/com/example/sampleproject/
├── MainActivity.kt (updated)
├── viewmodel/
│   └── ConnectionViewModel.kt (new)
├── ui/
│   └── screen/
│       ├── ConnectionScreen.kt (new)
│       └── MainScreen.kt (new)
└── navigation/
    ├── Screen.kt (new)
    └── AppNavigation.kt (new)
```

## Dependencies Used
- Jetpack Compose (UI)
- Navigation Compose (Screen navigation)
- Lifecycle ViewModel Compose (MVVM)
- Kotlin Coroutines (Async operations)
- StateFlow (State management)

## Permissions Required
The following permission is already added in AndroidManifest.xml:
- `INTERNET` - Required for TCP connection

## How to Test

### Testing with Real MAVLink Device:
1. Sync the project with Gradle in Android Studio
2. Make sure your device/emulator and MAVLink device are on the same network
3. Enter the IP address of your MAVLink device
4. Enter the port number (typically 5760 for MAVLink)
5. Click "Connect"
6. If heartbeat is received, you'll navigate to the main page
7. If connection fails, you'll see an error dialog

### Testing Connection Validation:
- **Empty IP**: Try connecting with an empty IP address - should show validation error
- **Invalid Port**: Enter port as 0 or 99999 - should show validation error
- **Wrong IP**: Enter a non-existent IP - should timeout and show connection failed
- **No Heartbeat**: Connect to a TCP server that doesn't send MAVLink heartbeat - should show "No heartbeat received" error

## Next Steps
1. Implement the Main Screen with:
   - Google Maps integration
   - Telemetry data display
   - Real-time updates from MAVLink connection
2. Add proper MAVLink library integration (optional - current uses raw socket)
3. Add disconnect functionality
4. Add connection status indicator

## Notes
- The current implementation uses raw TCP sockets for broader compatibility
- The heartbeat detection is simplified (looks for MAVLink packet markers 0xFE or 0xFD)
- For production, consider integrating full MAVLink library (`com.divpundir.mavlink` or similar)
- Connection timeout is set to 5 seconds for both TCP connection and heartbeat detection

