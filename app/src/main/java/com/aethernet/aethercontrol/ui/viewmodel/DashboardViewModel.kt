package com.aethernet.aethercontrol.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.domain.model.DashboardUiState
import com.aethernet.aethercontrol.domain.model.LedColor
import com.aethernet.aethercontrol.domain.model.LedState
import com.aethernet.aethercontrol.domain.model.LedUiState
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
    private var mqttAccessJob: Job? = null
    private var mqttSecurityJob: Job? = null
    private var ledExpiryJob: Job? = null

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
        // MOV-03 FIX: LED vía MQTT push <50ms (no solo HTTP poll). Cuando MQTT está Connected se para polling,
        // por eso aquí colectamos access/security y pintamos LedUiState directo con ventana 5s/1s/10s espejo led.cpp
        mqttAccessJob?.cancel()
        mqttAccessJob = viewModelScope.launch {
            repo.accessEventFlow.collect { ev ->
                val now = System.currentTimeMillis()
                if (ev.success) {
                    // HU-01 verde 5s DOOR_AUTO_LOCK_MS config.h:52 — luego auto OFF
                    _uiState.update {
                        it.copy(
                            ledState = LedUiState(
                                color = LedColor.GREEN,
                                state = LedState.GREEN_UNLOCKED,
                                label = "Verde desbloqueado",
                                lastEventAt = now,
                                source = "access",
                                isLoading = false,
                                error = null
                            ),
                            lastSync = now,
                            isConnected = true
                        )
                    }
                    scheduleLedExpiry(5000L)
                } else {
                    // fallo PIN rojo 1s LED_RED_FAIL_MS config.h:62 — luego OFF
                    _uiState.update {
                        it.copy(
                            ledState = LedUiState(
                                color = LedColor.RED,
                                state = LedState.RED_FAIL,
                                label = "Rojo fallo PIN",
                                lastEventAt = now,
                                source = "access",
                                isLoading = false,
                                error = null
                            ),
                            lastSync = now,
                            isConnected = true
                        )
                    }
                    scheduleLedExpiry(1000L)
                }
            }
        }
        mqttSecurityJob?.cancel()
        mqttSecurityJob = viewModelScope.launch {
            repo.securityEventFlow.collect { ev ->
                if (ev.event_type.equals("intrusion", ignoreCase = true)) {
                    val now = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            ledState = LedUiState(
                                color = LedColor.RED,
                                state = LedState.RED_INTRUSION,
                                label = "Rojo intrusión",
                                lastEventAt = now,
                                source = "security",
                                isLoading = false,
                                error = null
                            ),
                            lastSync = now,
                            isConnected = true
                        )
                    }
                    scheduleLedExpiry(10000L) // HU-02 rojo 10s (LedStateMapper RED_INTRUSION_WINDOW_MS)
                }
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

    private fun scheduleLedExpiry(delayMs: Long) {
        ledExpiryJob?.cancel()
        ledExpiryJob = viewModelScope.launch {
            delay(delayMs)
            // solo revierte a Apagado si no ha llegado otro evento más reciente dentro de la ventana
            val age = System.currentTimeMillis() - (_uiState.value.ledState.lastEventAt ?: 0L)
            if (age >= delayMs - 100) {
                _uiState.update {
                    it.copy(
                        ledState = LedUiState(
                            color = LedColor.OFF,
                            state = LedState.OFF,
                            label = "Apagado",
                            lastEventAt = it.ledState.lastEventAt,
                            source = it.ledState.source,
                            isLoading = false,
                            error = null
                        )
                    )
                }
            }
        }
    }

    fun disconnectMqtt() {
        repo.disconnectMqtt()
        mqttJob?.cancel(); mqttJob = null
        mqttStateJob?.cancel(); mqttStateJob = null
        mqttAccessJob?.cancel(); mqttAccessJob = null
        mqttSecurityJob?.cancel(); mqttSecurityJob = null
        ledExpiryJob?.cancel(); ledExpiryJob = null
        _uiState.update { it.copy(mqttState = MqttConnectionState.Disconnected) }
    }

    override fun onCleared() {
        disconnectMqtt()
        stopLedPolling()
        super.onCleared()
    }
}
