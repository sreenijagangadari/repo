package com.example.sampleproject.data

import com.divpundir.mavlink.definitions.common.*
import com.divpundir.mavlink.api.wrap
import com.divpundir.mavlink.definitions.minimal.MavModeFlag

/**
 * Single source of truth for all telemetry data parsing from MAVLink messages.
 * This class centralizes all data extraction from MAVLink common.xml messages.
 */

// Telemetry state data class
data class TelemetryData(
    // Connection status
    val connected: Boolean = false,
    val fcuDetected: Boolean = false,
    val armable: Boolean = false,

    // Battery data
    val voltage: Float? = null,
    val batteryPercent: Int? = null,
    val currentA: Float? = null,

    // GPS data
    val sats: Int? = null,
    val hdop: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Flight mode and armed status
    val mode: String? = null,
    val armed: Boolean = false,

    // Altitude and speed
    val altitudeMsl: Float = 0f,
    val altitudeRelative: Float = 0f,
    val airspeed: Float? = null,
    val groundspeed: Float? = null,
    val heading: Float = 0f,

    // Mission data
    val currentWaypoint: Int = 0,
    val totalWaypoints: Int = 0
)

/**
 * Parser for MAVLink HEARTBEAT message
 */
object HeartbeatParser {
    fun parseMode(customMode: UInt): String {
        return when (customMode) {
            0u -> "Stabilize"
            1u -> "Acro"
            2u -> "AltHold"
            3u -> "Auto"
            4u -> "Guided"
            5u -> "Loiter"
            6u -> "RTL"
            7u -> "Circle"
            8u -> "Position"
            9u -> "Land"
            10u -> "OF_Loiter"
            11u -> "Drift"
            13u -> "Sport"
            14u -> "Flip"
            15u -> "AutoTune"
            16u -> "PosHold"
            17u -> "Brake"
            18u -> "Throw"
            19u -> "Avoid_ADSB"
            20u -> "Guided_NoGPS"
            21u -> "Smart_RTL"
            22u -> "FlowHold"
            23u -> "Follow"
            24u -> "ZigZag"
            25u -> "SystemID"
            26u -> "AutoRotate"
            27u -> "Auto_RTL"
            else -> "Mode $customMode"
        }
    }

    fun isArmed(baseMode: UInt): Boolean {
        return (baseMode and MavModeFlag.SAFETY_ARMED.value) != 0u
    }
}

/**
 * Parser for MAVLink SYS_STATUS message
 */
object SysStatusParser {
    fun parseVoltage(voltageBattery: UShort): Float? {
        return if (voltageBattery.toUInt() == 0xFFFFu) null
        else voltageBattery.toFloat() / 1000f
    }

    fun parseBatteryPercent(batteryRemaining: Byte): Int? {
        return if (batteryRemaining.toInt() == -1) null
        else batteryRemaining.toInt()
    }

    fun isArmable(sysStatus: SysStatus): Boolean {
        val SENSOR_3D_GYRO = 1u
        val present = (sysStatus.onboardControlSensorsPresent.value and SENSOR_3D_GYRO) != 0u
        val enabled = (sysStatus.onboardControlSensorsEnabled.value and SENSOR_3D_GYRO) != 0u
        val healthy = (sysStatus.onboardControlSensorsHealth.value and SENSOR_3D_GYRO) != 0u
        return present && enabled && healthy
    }
}

/**
 * Parser for MAVLink BATTERY_STATUS message
 */
object BatteryStatusParser {
    fun parseCurrent(currentBattery: Short): Float? {
        return if (currentBattery.toInt() == -1) null
        else currentBattery / 100f
    }
}

/**
 * Parser for MAVLink GPS_RAW_INT message
 */
object GpsRawIntParser {
    fun parseSatellites(satellitesVisible: UByte): Int? {
        val sats = satellitesVisible.toInt()
        return if (sats >= 0) sats else null
    }

    fun parseHdop(eph: UShort): Float? {
        return if (eph.toUInt() == 0xFFFFu) null
        else eph.toFloat() / 100f
    }
}

/**
 * Parser for MAVLink GLOBAL_POSITION_INT message
 */
object GlobalPositionIntParser {
    fun parseLatitude(lat: Int): Double? {
        return if (lat != Int.MIN_VALUE) lat / 10_000_000.0 else null
    }

    fun parseLongitude(lon: Int): Double? {
        return if (lon != Int.MIN_VALUE) lon / 10_000_000.0 else null
    }

    fun parseAltitudeMsl(alt: Int): Float {
        return alt / 1000f
    }

    fun parseAltitudeRelative(relativeAlt: Int): Float {
        return relativeAlt / 1000f
    }
}

/**
 * Parser for MAVLink VFR_HUD message
 */
object VfrHudParser {
    fun parseAirspeed(airspeed: Float): Float? {
        return if (airspeed > 0f) airspeed else null
    }

    fun parseGroundspeed(groundspeed: Float): Float? {
        return if (groundspeed > 0f) groundspeed else null
    }

    fun parseHeading(heading: Short): Float {
        return heading.toFloat()
    }
}

/**
 * Utility functions for formatting telemetry data
 */
object TelemetryFormatter {
    fun formatSpeed(speed: Float?): String {
        if (speed == null) return "N/A"
        return when {
            speed >= 1f -> "%.1f m/s".format(java.util.Locale.US, speed)
            else -> "0 m/s"
        }
    }

    fun formatVoltage(voltage: Float?): String {
        return voltage?.let { "%.2f V".format(java.util.Locale.US, it) } ?: "N/A"
    }

    fun formatCurrent(current: Float?): String {
        return current?.let { "%.1f A".format(java.util.Locale.US, it) } ?: "N/A"
    }

    fun formatAltitude(altitude: Float): String {
        return "%.1f m".format(java.util.Locale.US, altitude)
    }

    fun formatCoordinate(coord: Double?): String {
        return coord?.let { "%.6f°".format(java.util.Locale.US, it) } ?: "N/A"
    }
}

