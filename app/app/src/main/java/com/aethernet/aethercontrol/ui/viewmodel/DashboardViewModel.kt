package com.aethernet.aethercontrol.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data class Connected(
        val relayStates: Map<String, Boolean> = emptyMap(),
        val roverTelemetry: RoverTelemetry? = null,
        val lastAccessEvent: AccessEvent? = null
    ) : DashboardUiState

    data class Disconnected(val reason: String) : DashboardUiState

    object Connecting : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

data class RoverTelemetry(
    val leftMotorPwm: Int,
    val rightMotorPwm: Int,
    val ultrasonicDistanceCm: Double?,
    val irLeft: Boolean,
    val irCenter: Boolean,
    val irRight: Boolean,
    val rfRssi: Int?
)

data class AccessEvent(
    val userId: String,
    val success: Boolean,
    val timestamp: Long,
    val source: String
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Connecting)
    val uiState: LiveData<DashboardUiState> = _uiState.asLiveData()

    private val _connectionStatus = MutableStateFlow<String>("Desconectado")
    val connectionStatus: LiveData<String> = _connectionStatus.asLiveData()

    init {
        // TODO: Implement MQTT connection and subscribe to topics
        // For now, simulate disconnected state
        _uiState.value = DashboardUiState.Disconnected("MQTT no configurado")
        _connectionStatus.value = "Desconectado"
    }

    fun connect() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Connecting
            _connectionStatus.value = "Conectando..."
            // TODO: Implement actual MQTT connection
            // mqttClient.connect()...
        }
    }

    fun sendJoystickCommand(x: Float, y: Float) {
        // TODO: Publish to MQTT topic aethernet/rover/command
    }

    fun sendPinCommand(pin: String) {
        // TODO: Publish to MQTT topic aethernet/access/pin
    }

    fun toggleRelay(relayId: String, state: Boolean) {
        // TODO: Publish to MQTT topic aethernet/relay/{relayId}
    }
}