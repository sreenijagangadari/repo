# GCS Application - Connection Page - Quick Start Guide

## What Was Created

I've successfully implemented the **Connection Page** for your GCS (Ground Control Station) application with all the requested features:

### ✅ Features Implemented:

1. **"Connection" Heading** - Bold, prominent header
2. **IP Address Text Field** - For entering the drone/device IP address
3. **Port Number Text Field** - For entering the TCP port (numeric input only)
4. **TCP Connection with Heartbeat Validation**:
   - Establishes TCP socket connection
   - Waits for MAVLink heartbeat packet
   - Only navigates to next page if heartbeat is received
   - Shows "Connection failed" error popup if no heartbeat or connection fails
5. **Proper UI Design**:
   - Clean, centered card-based layout
   - Material Design 3 components
   - Proper spacing and alignment
   - Loading indicator during connection
   - Responsive error handling

### 📁 Files Created:

1. **`viewmodel/ConnectionViewModel.kt`** - Handles all connection logic (MVVM pattern)
2. **`ui/screen/ConnectionScreen.kt`** - The connection page UI
3. **`ui/screen/MainScreen.kt`** - Placeholder for main page (to be implemented)
4. **`navigation/Screen.kt`** - Screen route definitions
5. **`navigation/AppNavigation.kt`** - Navigation setup
6. **`MainActivity.kt`** - Updated to use navigation

### 🏗️ Architecture:

Following **MVVM (Model-View-ViewModel)** pattern:
- **Model**: Connection state (IP, port, connection status)
- **View**: ConnectionScreen composable
- **ViewModel**: ConnectionViewModel (manages state and business logic)

## How to Use

### Step 1: Sync Project
Open the project in Android Studio and click "Sync Project with Gradle Files" (or File → Sync Project with Gradle Files). This will resolve any import issues.

### Step 2: Run the App
1. Connect your Android device or start an emulator
2. Click the Run button in Android Studio
3. The app will open with the Connection screen

### Step 3: Connect to Your Device
1. Enter the IP address of your MAVLink device (e.g., 192.168.1.100)
2. Enter the port number (e.g., 5760)
3. Click "Connect"
4. The app will:
   - Show "Connecting..." with a loading spinner
   - Attempt TCP connection
   - Wait for heartbeat packet
   - Navigate to main page if successful
   - Show error dialog if failed

## Connection Flow

```
User clicks "Connect"
        ↓
Validate IP & Port
        ↓
Establish TCP Connection
        ↓
Listen for Heartbeat (5 sec timeout)
        ↓
    ┌───┴───┐
    ↓       ↓
Heartbeat   No Heartbeat
Received    
    ↓           ↓
Navigate    Show Error
to Main     Dialog
Page        
```

## Error Scenarios Handled

| Scenario | Error Message |
|----------|--------------|
| Empty IP Address | "IP address cannot be empty" |
| Invalid Port | "Invalid port number" |
| Connection Failed | "Connection failed: Unable to connect to TCP server" |
| No Heartbeat | "Connection failed: No heartbeat received" |
| Connection Lost | "Connection lost: [error details]" |

## Default Values

- **IP Address**: 192.168.1.100
- **Port**: 5760 (standard MAVLink port)
- **Connection Timeout**: 5 seconds
- **Heartbeat Timeout**: 5 seconds

## Next Steps

The connection page is complete and ready to test! The next phase will be to implement the Main Page with:

1. **Google Maps Integration** - Display map with drone location
2. **Telemetry Data Display** - Show real-time flight data
3. **MAVLink Message Handling** - Process and display various telemetry messages

## Important Notes

⚠️ **Before Testing:**
- Make sure your Android device and MAVLink device are on the same network
- Verify the IP address and port of your MAVLink device
- Ensure the MAVLink device is sending heartbeat messages

📱 **Permissions:**
- INTERNET permission is already added to AndroidManifest.xml
- Required for TCP network connections

🔧 **Technical Details:**
- Uses raw TCP sockets for maximum compatibility
- Listens for MAVLink packet markers (0xFE for v1, 0xFD for v2)
- Implements proper connection lifecycle management
- Automatic cleanup on ViewModel disposal

## Testing Checklist

- [ ] Gradle sync completes successfully
- [ ] App builds without errors
- [ ] Connection screen displays correctly
- [ ] IP address field accepts input
- [ ] Port number field accepts only numbers
- [ ] Connect button triggers connection
- [ ] Loading indicator shows during connection
- [ ] Error dialog appears on connection failure
- [ ] Navigation to main page works on success
- [ ] Back button behavior is correct

---

**Status**: ✅ Connection Page Implementation Complete

The connection page is fully implemented with proper UI, MVVM architecture, TCP connection handling, and heartbeat validation. You can now build and test the application!

