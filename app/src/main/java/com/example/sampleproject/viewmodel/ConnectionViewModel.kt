package com.example.sampleproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket
import java.io.InputStream
import java.io.OutputStream

data class ConnectionState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val ipAddress: String = "10.0.2.2",
    val port: String = "5762"
)

class ConnectionViewModel : ViewModel() {
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var heartbeatJob: Job? = null

    fun updateIpAddress(ip: String) {
        _connectionState.value = _connectionState.value.copy(ipAddress = ip)
    }

    fun updatePort(port: String) {
        _connectionState.value = _connectionState.value.copy(port = port)
    }

    fun connectToTcp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(
                isConnecting = true,
                errorMessage = null
            )

            try {
                val ip = _connectionState.value.ipAddress
                val port = _connectionState.value.port.toIntOrNull()

                // Validate inputs
                if (ip.isBlank()) {
                    _connectionState.value = _connectionState.value.copy(
                        isConnecting = false,
                        errorMessage = "IP address cannot be empty"
                    )
                    return@launch
                }

                if (port == null || port !in 1..65535) {
                    _connectionState.value = _connectionState.value.copy(
                        isConnecting = false,
                        errorMessage = "Invalid port number"
                    )
                    return@launch
                }

                // Attempt TCP connection
                var connectionSuccess = false
                withContext(Dispatchers.IO) {
                    try {
                        socket = Socket()
                        socket?.connect(java.net.InetSocketAddress(ip, port), 5000)

                        if (socket?.isConnected == true) {
                            inputStream = socket?.getInputStream()
                            outputStream = socket?.getOutputStream()
                            connectionSuccess = true
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }

                if (connectionSuccess) {
                    // Start heartbeat monitoring using atomic state
                    val heartbeatState = MutableStateFlow(false)

                    heartbeatJob = viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val buffer = ByteArray(263) // MAVLink v1 max packet size
                            var heartbeatTimeout = 0

                            while (socket?.isConnected == true && heartbeatTimeout < 50 && !heartbeatState.value) {
                                val available = inputStream?.available() ?: 0
                                if (available > 0) {
                                    val bytesRead = inputStream?.read(buffer, 0, minOf(available, buffer.size)) ?: 0
                                    if (bytesRead > 0) {
                                        // Check for MAVLink heartbeat (message ID 0)
                                        // MAVLink packet starts with 0xFE (v1) or 0xFD (v2)
                                        for (i in 0 until bytesRead) {
                                            if (buffer[i] == 0xFE.toByte() || buffer[i] == 0xFD.toByte()) {
                                                // Simplified check - in real implementation, parse full packet
                                                heartbeatState.value = true
                                                break
                                            }
                                        }
                                    }
                                }
                                delay(100)
                                heartbeatTimeout++
                            }
                        } catch (_: Exception) {
                            // Connection lost - will be handled by timeout
                        }
                    }

                    // Wait for heartbeat with timeout
                    val startTime = System.currentTimeMillis()
                    val timeoutMillis = 5000L

                    while (!heartbeatState.value && (System.currentTimeMillis() - startTime) < timeoutMillis) {
                        delay(100)
                    }

                    heartbeatJob?.cancel()

                    if (heartbeatState.value) {
                        _connectionState.value = _connectionState.value.copy(
                            isConnecting = false,
                            isConnected = true,
                            errorMessage = null
                        )
                        onSuccess()
                    } else {
                        // No heartbeat received
                        closeConnection()
                        _connectionState.value = _connectionState.value.copy(
                            isConnecting = false,
                            isConnected = false,
                            errorMessage = "Connection failed: No heartbeat received"
                        )
                    }
                } else {
                    _connectionState.value = _connectionState.value.copy(
                        isConnecting = false,
                        errorMessage = "Connection failed: Unable to connect to TCP server"
                    )
                }

            } catch (e: Exception) {
                closeConnection()
                _connectionState.value = _connectionState.value.copy(
                    isConnecting = false,
                    errorMessage = "Connection failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _connectionState.value = _connectionState.value.copy(errorMessage = null)
    }

    fun disconnect() {
        closeConnection()
        _connectionState.value = _connectionState.value.copy(
            isConnecting = false,
            isConnected = false,
            errorMessage = null
        )
    }

    private fun closeConnection() {
        try {
            heartbeatJob?.cancel()
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            socket = null
            inputStream = null
            outputStream = null
        } catch (_: Exception) {
            // Ignore close errors
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeConnection()
    }
}

