package com.example.sampleproject.repository

import android.util.Log
import com.divpundir.mavlink.adapters.coroutines.asCoroutine
import com.divpundir.mavlink.adapters.coroutines.tryConnect
import com.divpundir.mavlink.adapters.coroutines.trySendUnsignedV2
import com.divpundir.mavlink.api.wrap
import com.divpundir.mavlink.connection.tcp.TcpClientMavConnection
import com.divpundir.mavlink.definitions.common.*
import com.divpundir.mavlink.definitions.minimal.*
import com.divpundir.mavlink.api.MavFrame
import com.divpundir.mavlink.api.MavMessage
import com.divpundir.mavlink.definitions.ardupilotmega.ArdupilotmegaDialect
import com.example.sampleproject.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Repository for MAVLink telemetry communication.
 * Handles connection, message parsing, and telemetry state management.
 */
class TelemetryRepository(
    private val ip: String,
    private val port: Int
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val gcsSystemId: UByte = 255u
    val gcsComponentId: UByte = 1u

    var fcuSystemId: UByte = 0u
    var fcuComponentId: UByte = 0u

    private val _telemetryState = MutableStateFlow(TelemetryData())
    val telemetryState: StateFlow<TelemetryData> = _telemetryState.asStateFlow()

    private val connection = TcpClientMavConnection(
        host = ip,
        port = port,
        dialect = ArdupilotmegaDialect
    ).asCoroutine()
    private lateinit var mavFrame: SharedFlow<MavFrame<out MavMessage<*>>>

    private val lastFcuHeartbeatTime = AtomicLong(0L)
    private val HEARTBEAT_TIMEOUT_MS = 5000L

    private var intentionalDisconnect = false

    fun start() {
        Log.i("TelemetryRepo", "Starting telemetry repository for $ip:$port")

        // Connection management
        scope.launch {
            try {
                reconnect()
            } catch (e: Exception) {
                Log.e("TelemetryRepo", "Connection failed", e)
                _telemetryState.update { it.copy(connected = false, fcuDetected = false) }
            }
        }

        // Heartbeat timeout monitor
        scope.launch {
            while (isActive) {
                delay(1000)
                if (_telemetryState.value.fcuDetected && lastFcuHeartbeatTime.get() > 0L) {
                    val timeSinceLastHeartbeat = System.currentTimeMillis() - lastFcuHeartbeatTime.get()
                    if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                        if (_telemetryState.value.connected) {
                            Log.w("TelemetryRepo", "FCU heartbeat timeout")
                            _telemetryState.update { it.copy(connected = false, fcuDetected = false) }
                            lastFcuHeartbeatTime.set(0L)
                        }
                    }
                }
            }
        }

        // Send GCS heartbeat
        scope.launch {
            val heartbeat = Heartbeat(
                type = MavType.GCS.wrap(),
                autopilot = MavAutopilot.INVALID.wrap(),
                baseMode = MavModeFlag.CUSTOM_MODE_ENABLED.wrap(),
                customMode = 0u,
                systemStatus = MavState.ACTIVE.wrap(),
                mavlinkVersion = 3u
            )
            while (isActive) {
                try {
                    connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, heartbeat)
                } catch (e: Exception) {
                    Log.e("TelemetryRepo", "Failed to send heartbeat", e)
                }
                delay(1000)
            }
        }

        // Shared message stream
        mavFrame = connection.mavFrame.shareIn(scope, SharingStarted.Eagerly, replay = 0)

        // Start message collectors
        collectHeartbeat()
        collectSysStatus()
        collectBatteryStatus()
        collectGpsRawInt()
        collectGlobalPositionInt()
        collectVfrHud()
        collectMissionCurrent()
    }

    private suspend fun reconnect() {
        while (scope.isActive) {
            try {
                if (connection.tryConnect(scope)) {
                    Log.i("TelemetryRepo", "Connection successful")
                    return
                }
            } catch (e: Exception) {
                Log.e("TelemetryRepo", "Connection attempt failed", e)
            }
            delay(2000)
        }
    }

    private fun collectHeartbeat() {
        scope.launch {
            mavFrame
                .filter { frame ->
                    val msg = frame.message
                    if (msg is Heartbeat) {
                        val isNotGCS = msg.type != MavType.GCS.wrap()
                        val isAutopilot = msg.autopilot != MavAutopilot.INVALID.wrap()
                        return@filter isNotGCS && isAutopilot
                    }
                    false
                }
                .collect { frame ->
                    lastFcuHeartbeatTime.set(System.currentTimeMillis())

                    if (!_telemetryState.value.fcuDetected) {
                        fcuSystemId = frame.systemId
                        fcuComponentId = frame.componentId

                        val hb = frame.message as Heartbeat
                        val armed = (hb.baseMode.value and MavModeFlag.SAFETY_ARMED.value) != 0u
                        val mode = HeartbeatParser.parseMode(hb.customMode)

                        Log.i("TelemetryRepo", "FCU detected: sysId=$fcuSystemId, mode=$mode, armed=$armed")

                        _telemetryState.update { state ->
                            state.copy(
                                fcuDetected = true,
                                connected = true,
                                mode = mode,
                                armed = armed
                            )
                        }

                        // Set message intervals
                        launch {
                            delay(500)
                            setMessageRate(1u, 1f)   // SYS_STATUS
                            setMessageRate(24u, 1f)  // GPS_RAW_INT
                            setMessageRate(33u, 5f)  // GLOBAL_POSITION_INT
                            setMessageRate(74u, 5f)  // VFR_HUD
                            setMessageRate(147u, 1f) // BATTERY_STATUS
                        }
                    } else if (!_telemetryState.value.connected) {
                        Log.i("TelemetryRepo", "FCU heartbeat resumed")
                        _telemetryState.update { it.copy(connected = true) }
                    }

                    // Update mode and armed status
                    val hb = frame.message as Heartbeat
                    val armed = (hb.baseMode.value and MavModeFlag.SAFETY_ARMED.value) != 0u
                    val mode = HeartbeatParser.parseMode(hb.customMode)

                    if (mode != _telemetryState.value.mode || armed != _telemetryState.value.armed) {
                        _telemetryState.update { it.copy(mode = mode, armed = armed) }
                    }
                }
        }
    }

    private fun collectSysStatus() {
        scope.launch {
            mavFrame
                .filter { _telemetryState.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<SysStatus>()
                .collect { sys ->
                    val voltage = SysStatusParser.parseVoltage(sys.voltageBattery)
                    val batteryPercent = SysStatusParser.parseBatteryPercent(sys.batteryRemaining)
                    val armable = SysStatusParser.isArmable(sys)

                    _telemetryState.update {
                        it.copy(voltage = voltage, batteryPercent = batteryPercent, armable = armable)
                    }
                }
        }
    }

    private fun collectBatteryStatus() {
        scope.launch {
            mavFrame
                .filter { _telemetryState.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<BatteryStatus>()
                .collect { battery ->
                    if (battery.id.toInt() == 0) {
                        val current = BatteryStatusParser.parseCurrent(battery.currentBattery)
                        _telemetryState.update { it.copy(currentA = current) }
                    }
                }
        }
    }

    private fun collectGpsRawInt() {
        scope.launch {
            mavFrame
                .filter { _telemetryState.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<GpsRawInt>()
                .collect { gps ->
                    val sats = GpsRawIntParser.parseSatellites(gps.satellitesVisible)
                    val hdop = GpsRawIntParser.parseHdop(gps.eph)

                    _telemetryState.update { it.copy(sats = sats, hdop = hdop) }
                }
        }
    }

    private fun collectGlobalPositionInt() {
        scope.launch {
            mavFrame
                .filter { _telemetryState.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<GlobalPositionInt>()
                .collect { gp ->
                    val lat = GlobalPositionIntParser.parseLatitude(gp.lat)
                    val lon = GlobalPositionIntParser.parseLongitude(gp.lon)
                    val altMsl = GlobalPositionIntParser.parseAltitudeMsl(gp.alt)
                    val altRel = GlobalPositionIntParser.parseAltitudeRelative(gp.relativeAlt)

                    _telemetryState.update {
                        it.copy(
                            latitude = lat,
                            longitude = lon,
                            altitudeMsl = altMsl,
                            altitudeRelative = altRel
                        )
                    }
                }
        }
    }

    private fun collectVfrHud() {
        scope.launch {
            mavFrame
                .filter { _telemetryState.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<VfrHud>()
                .collect { hud ->
                    val airspeed = VfrHudParser.parseAirspeed(hud.airspeed)
                    val groundspeed = VfrHudParser.parseGroundspeed(hud.groundspeed)
                    val heading = VfrHudParser.parseHeading(hud.heading)

                    _telemetryState.update {
                        it.copy(
                            airspeed = airspeed,
                            groundspeed = groundspeed,
                            heading = heading,
                            altitudeMsl = hud.alt
                        )
                    }
                }
        }
    }

    private fun collectMissionCurrent() {
        scope.launch {
            mavFrame
                .filter { _telemetryState.value.fcuDetected && it.systemId == fcuSystemId }
                .map { it.message }
                .filterIsInstance<MissionCurrent>()
                .collect { mission ->
                    _telemetryState.update { it.copy(currentWaypoint = mission.seq.toInt()) }
                }
        }
    }

    private suspend fun setMessageRate(messageId: UInt, hz: Float) {
        val intervalUsec = if (hz <= 0f) 0f else (1_000_000f / hz)
        val cmd = CommandLong(
            targetSystem = fcuSystemId,
            targetComponent = fcuComponentId,
            command = MavCmd.SET_MESSAGE_INTERVAL.wrap(),
            confirmation = 0u,
            param1 = messageId.toFloat(),
            param2 = intervalUsec,
            param3 = 0f,
            param4 = 0f,
            param5 = 0f,
            param6 = 0f,
            param7 = 0f
        )
        try {
            connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, cmd)
        } catch (e: Exception) {
            Log.e("TelemetryRepo", "Failed to set message interval", e)
        }
    }

    suspend fun arm() {
        val cmd = CommandLong(
            targetSystem = fcuSystemId,
            targetComponent = fcuComponentId,
            command = MavCmd.COMPONENT_ARM_DISARM.wrap(),
            confirmation = 0u,
            param1 = 1f,
            param2 = 0f,
            param3 = 0f,
            param4 = 0f,
            param5 = 0f,
            param6 = 0f,
            param7 = 0f
        )
        connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, cmd)
        Log.i("TelemetryRepo", "Arm command sent")
    }

    suspend fun disarm() {
        val cmd = CommandLong(
            targetSystem = fcuSystemId,
            targetComponent = fcuComponentId,
            command = MavCmd.COMPONENT_ARM_DISARM.wrap(),
            confirmation = 0u,
            param1 = 0f,
            param2 = 0f,
            param3 = 0f,
            param4 = 0f,
            param5 = 0f,
            param6 = 0f,
            param7 = 0f
        )
        connection.trySendUnsignedV2(gcsSystemId, gcsComponentId, cmd)
        Log.i("TelemetryRepo", "Disarm command sent")
    }

    fun closeConnection() {
        try {
            intentionalDisconnect = true
            Log.i("TelemetryRepo", "User-initiated disconnect")
            runBlocking {
                connection.close()
            }
            scope.cancel()
        } catch (e: Exception) {
            Log.e("TelemetryRepo", "Error closing connection", e)
        }
    }
}



