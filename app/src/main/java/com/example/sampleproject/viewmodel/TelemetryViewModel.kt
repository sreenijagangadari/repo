package com.example.sampleproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sampleproject.data.TelemetryData
import com.example.sampleproject.repository.TelemetryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing telemetry data and MAVLink communication
 */
class TelemetryViewModel : ViewModel() {
    private var repository: TelemetryRepository? = null

    private val _telemetryState = MutableStateFlow(TelemetryData())
    val telemetryState: StateFlow<TelemetryData> = _telemetryState.asStateFlow()

    fun connectToFcu(ip: String, port: Int) {
        repository = TelemetryRepository(ip, port)
        repository?.start()

        // Collect telemetry updates
        viewModelScope.launch {
            repository?.telemetryState?.collect { data ->
                _telemetryState.value = data
            }
        }
    }

    fun arm() {
        viewModelScope.launch {
            repository?.arm()
        }
    }

    fun disarm() {
        viewModelScope.launch {
            repository?.disarm()
        }
    }

    fun disconnect() {
        repository?.closeConnection()
        repository = null
        _telemetryState.value = TelemetryData()
    }

    override fun onCleared() {
        super.onCleared()
        repository?.closeConnection()
    }
}

