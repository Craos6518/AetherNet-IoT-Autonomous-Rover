package com.aethernet.aethercontrol.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.domain.model.DashboardUiState
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel — MOV-01 5.2 + MOV-02 + MOV-03 (RF-1.1, RNF-3.1).
 * MOV-02: polling 5s STATUS_INTERVAL_MS=5000 config.h:55 fallback.
 * MOV-03: MQTT Mosquitto tcp://host:1883 (mosquitto.conf:4 + acl.conf:14 aethernet/#),
 *         gateway publica aethernet/rover/telemetry:69 etc. Si Connected → stopPolling ahorro batería <50ms prd.md:50.
 *         Si Disconnected/Error → startPolling fallback (base MOV-09).
 */
class DashboardViewModel(
    private val repo: AetherRepository,
    private val autoPollLed: Boolean = true,
    private val autoConnectMqtt: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var ledPollingJob: Job? = null
    private var mqttJob: Job? = null
    private var mqttStateJob: Job? = null

    init {
        refreshHealth()
        refreshLedState()
        if (autoPollLed) startLedPolling()
        if (autoConnectMqtt) connectMqttAndCollect()
    }

    fun refreshHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = repo.getHealth()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = true,
                            health = r.data,
                            lastSync = System.currentTimeMillis(),
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = false,
                            error = r.msg
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /** MOV-02: refresh LED solo lectura — deriva de /api/access-events + /api/security-events. */
    fun refreshLedState() {
        viewModelScope.launch {
            _uiState.update { it.copy(ledState = it.ledState.copy(isLoading = true, error = null)) }
            when (val r = repo.getLedState()) {
                is Result.Success -> {
                    _uiState.update { it.copy(ledState = r.data.copy(isLoading = false, error = null)) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(ledState = it.ledState.copy(isLoading = false, error = r.msg)) }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(ledState = it.ledState.copy(isLoading = true)) }
                }
            }
        }
    }

    /** Polling 5s — STATUS_INTERVAL_MS=5000 config.h:55 evita spam; cancelable en onCleared(). */
    fun startLedPolling(intervalMs: Long = 5000L) {
        if (ledPollingJob?.isActive == true) return
        ledPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalMs)
                refreshLedState()
            }
        }
    }

    fun stopLedPolling() {
        ledPollingJob?.cancel()
        ledPollingJob = null
    }

    val isPolling: Boolean get() = ledPollingJob?.isActive == true

    /** MOV-03: conecta MQTT y colecta telemetría + estado. Fallback a polling si cae. */
    fun connectMqttAndCollect(httpBaseUrl: String? = null) {
        viewModelScope.launch {
            val url = httpBaseUrl ?: try {
                com.aethernet.aethercontrol.core.di.ServiceLocator.getCurrentBaseUrl()
            } catch (_: Exception) { "http://10.0.2.2:8000/" }
            repo.connectMqtt(url)
        }
        mqttJob?.cancel()
        mqttJob = viewModelScope.launch {
            repo.roverTelemetryFlow.collect { telem ->
                _uiState.update { it.copy(lastRover = telem, lastSync = System.currentTimeMillis(), isConnected = true) }
            }
        }
        mqttStateJob?.cancel()
        mqttStateJob = viewModelScope.launch {
            repo.mqttConnectionState.collect { st ->
                _uiState.update { it.copy(mqttState = st) }
                // ahorro batería <50ms: si hay MQTT vivo, no hace falta poll 5s
                when (st) {
                    is MqttConnectionState.Connected -> stopLedPolling()
                    is MqttConnectionState.Disconnected,
                    is MqttConnectionState.Error -> if (autoPollLed) startLedPolling()
                    else -> {} // Connecting mantiene estado previo
                }
            }
        }
    }

    fun disconnectMqtt() {
        repo.disconnectMqtt()
        mqttJob?.cancel()
        mqttJob = null
        mqttStateJob?.cancel()
        mqttStateJob = null
        _uiState.update { it.copy(mqttState = MqttConnectionState.Disconnected) }
    }

    override fun onCleared() {
        disconnectMqtt()
        stopLedPolling()
        super.onCleared()
    }
}
