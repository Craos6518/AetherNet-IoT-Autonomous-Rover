package com.aethernet.aethercontrol.ui.viewmodel

// =============================================================================
// DashboardViewModel.kt — ViewModel Dashboard + LED + MQTT | 6º Semestre UTP | MOV-01 5.2 + MOV-02 + MOV-03
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (useState, useEffect, Redux), 2 años Python (asyncio),
//              2 años electrónica (LED 44/45/46, MqttManager), 1 año PostgreSQL (health)
// Analogía React: este class es como `function useDashboard() { const [state, setState] = useState(...); useEffect(()=>fetchHealth(), []) }`
// en React con hooks — aquí con ViewModel + StateFlow + viewModelScope.launch (coroutines).
// Analogía Python: como `class DashboardViewModel` con `async def refresh_health()` y `asyncio.create_task` para polling/MQTT.
// Analogía C: cada `viewModelScope.launch { repo.getHealth() }` es como `handleDoorAutoLock()` en door.cpp:32 — async sin bloquear UI loop.
// FOSS: AndroidX ViewModel + Coroutines (Apache 2.0) — RNF-3.1, sin Hilt.
// Origen: MOV-01 5.2 + MOV-02 + MOV-03 (RF-1.1, RNF-3.1) — polling 5s STATUS_INTERVAL_MS=5000 config.h:55 fallback,
// MQTT Mosquitto tcp://host:1883 (mosquitto.conf:4 + acl.conf:14 aethernet/#), gateway publica aethernet/rover/telemetry:69 etc.
// Si Connected → stopPolling ahorro batería <50ms prd.md:50; si Disconnected/Error → startPolling fallback (base MOV-09).
// =============================================================================

import androidx.lifecycle.ViewModel // ViewModel — como `useState` + `useEffect` en React pero con ciclo vida Android
import androidx.lifecycle.viewModelScope // scope — como `useEffect` con cancel en unmount (onCleared)
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState // estado MQTT — como connectionState en WebSocket JS
import com.aethernet.aethercontrol.data.repository.AetherRepository // repo — como `apiService` en React
import com.aethernet.aethercontrol.domain.model.DashboardUiState // UiState — como `type State` en React useState
import com.aethernet.aethercontrol.domain.model.LedColor // LED — como enum en TS
import com.aethernet.aethercontrol.domain.model.LedState
import com.aethernet.aethercontrol.domain.model.LedUiState
import com.aethernet.aethercontrol.util.Result // Result — como {ok, data, error} en TS
import kotlinx.coroutines.Job // Job — handle de coroutine, como `setInterval` id en JS (para cancel)
import kotlinx.coroutines.delay // delay — como `await sleep(ms)` en JS
import kotlinx.coroutines.flow.MutableStateFlow // MutableStateFlow — como `useState` pero reactivo (StateFlow)
import kotlinx.coroutines.flow.StateFlow // StateFlow — read-only, como `state` en React
import kotlinx.coroutines.flow.asStateFlow // expone como read-only — como `.asObservable()` en RxJS
import kotlinx.coroutines.flow.update // update { copy(...) } — como `setState(prev => ({...prev, field: value}))` en React
import kotlinx.coroutines.isActive // isActive — como `signal.aborted` en AbortController
import kotlinx.coroutines.launch // launch — como `useEffect(() => { fetch() }, [])` pero con coroutine

/**
 * ViewModel — MOV-01 5.2 + MOV-02 + MOV-03 (RF-1.1, RNF-3.1).
 * MOV-02: polling 5s STATUS_INTERVAL_MS=5000 config.h:55 fallback — hasta que MQTT conecte.
 * MOV-03: MQTT Mosquitto tcp://host:1883 (mosquitto.conf:4 + acl.conf:14 aethernet/#),
 *         gateway publica aethernet/rover/telemetry:69 etc. Si Connected → stopPolling ahorro batería <50ms prd.md:50.
 *         Si Disconnected/Error → startPolling fallback (base MOV-09 reconexión).
 * Si vienes de React: es como `function DashboardViewModel() { const [state, setState] = useState(initial); useEffect(refreshHealth, []) }`
 */
class DashboardViewModel(
    private val repo: AetherRepository, // inyectado via ViewModelFactory (ServiceLocator.repository) — como props en React
    private val autoPollLed: Boolean = true, // auto polling LED — como `autoPoll` prop en React (para tests, false)
    private val autoConnectMqtt: Boolean = true // auto conectar MQTT — como `autoConnect` prop
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState()) // state interno — como `const [state, setState] = useState(initial)` en React
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow() // público read-only — como `state` en React

    // Jobs — handles para cancel en onCleared (como `clearInterval(id)` en JS)
    private var ledPollingJob: Job? = null // polling HTTP 5s — como setInterval(refreshLedState, 5000) en JS
    private var mqttJob: Job? = null // colecta rover telemetry — como `socket.on('rover', handler)` en JS
    private var mqttStateJob: Job? = null // colecta estado MQTT — como `socket.on('connect', handler)`
    private var mqttAccessJob: Job? = null // colecta access events MQTT push — como `socket.on('access', ...)`
    private var mqttSecurityJob: Job? = null // colecta security events — como `socket.on('security', ...)`
    private var ledExpiryJob: Job? = null // expira LED verde/rojo a OFF — como `setTimeout(() => setLed(OFF), 5000)` en JS

    init {
        refreshHealth() // fetch health al crear — como `useEffect(() => refreshHealth(), [])` en React
        refreshLedState() // fetch LED al crear — como `useEffect(() => refreshLedState(), [])`
        if (autoPollLed) startLedPolling() // polling 5s — como `setInterval(refreshLedState, 5000)`
        if (autoConnectMqtt) connectMqttAndCollect() // conecta MQTT y colecta — como `useEffect(() => connectMqtt(), [])`
    }

    fun refreshHealth() {
        viewModelScope.launch { // launch en viewModelScope — como `useEffect` con async (se cancela en onCleared)
            _uiState.update { it.copy(isLoading = true, error = null) } // loading — como `setState({isLoading: true})` en React
            when (val r = repo.getHealth()) { // repo.getHealth() -> Result<HealthResponse> — como `await fetch('/health')` en JS
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = true, // conectado — como `setState({isConnected: true, health: r.data})`
                            health = r.data,
                            lastSync = System.currentTimeMillis(), // timestamp — como `Date.now()` en JS
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = false, // error — banner "Desconectado" en DashboardScreen:65
                            error = r.msg // mensaje error — como `error.message` en JS
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /** MOV-02: refresh LED solo lectura — deriva de /api/access-events + /api/security-events (LedStateMapper). */
    fun refreshLedState() {
        viewModelScope.launch {
            _uiState.update { it.copy(ledState = it.ledState.copy(isLoading = true, error = null)) }
            when (val r = repo.getLedState()) { // getLedState hace 2 fetch paralelos + map (AetherRepositoryImpl:53)
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

    /** Polling 5s — STATUS_INTERVAL_MS=5000 config.h:55 evita spam HTTP; cancelable en onCleared() (como clearInterval). */
    fun startLedPolling(intervalMs: Long = 5000L) {
        if (ledPollingJob?.isActive == true) return // ya polling — no duplica (como `if (interval) return` en JS)
        ledPollingJob = viewModelScope.launch {
            while (isActive) { // while activo — como `setInterval` pero con coroutine (se cancela en onCleared)
                delay(intervalMs) // espera 5s — como `await sleep(5000)` en JS
                refreshLedState() // fetch LED — como `fetchLedState()` en JS interval
            }
        }
    }

    fun stopLedPolling() {
        ledPollingJob?.cancel() // cancela — como `clearInterval(id)` en JS
        ledPollingJob = null
    }

    val isPolling: Boolean get() = ledPollingJob?.isActive == true // getter — como `isPolling` derived state en React

    /** MOV-03: conecta MQTT y colecta telemetría + estado. Fallback a polling 5s si MQTT cae (ahorro batería). */
    fun connectMqttAndCollect(httpBaseUrl: String? = null) {
        viewModelScope.launch {
            val url = httpBaseUrl ?: try { // URL para derivar tcp://host:1883 — como `getCurrentBaseUrl()` en ServiceLocator
                com.aethernet.aethercontrol.core.di.ServiceLocator.getCurrentBaseUrl()
            } catch (_: Exception) { "http://10.0.2.2:8000/" } // fallback emulador si ServiceLocator no init (tests)
            repo.connectMqtt(url) // conecta Paho — como `mqtt.connect(url)` en MQTT.js (ver MqttManager:86)
        }
        // Colecta rover telemetry push — como `socket.on('rover', (data) => setState({lastRover: data}))` en JS
        mqttJob?.cancel()
        mqttJob = viewModelScope.launch {
            repo.roverTelemetryFlow.collect { telem -> // SharedFlow — como `socket.on('rover/telemetry', ...)` en JS
                _uiState.update { it.copy(lastRover = telem, lastSync = System.currentTimeMillis(), isConnected = true) }
            }
        }
        // MOV-03 FIX: LED vía MQTT push <50ms (no solo HTTP poll 5s). Cuando MQTT está Connected se para polling,
        // por eso aquí colectamos access/security y pintamos LedUiState directo con ventana 5s/1s/10s espejo led.cpp:22
        mqttAccessJob?.cancel()
        mqttAccessJob = viewModelScope.launch {
            repo.accessEventFlow.collect { ev -> // push access — como `socket.on('access/event', ...)`
                val now = System.currentTimeMillis()
                if (ev.success) {
                    // HU-01 verde 5s DOOR_AUTO_LOCK_MS config.h:52 — luego auto OFF vía scheduleLedExpiry
                    _uiState.update {
                        it.copy(
                            ledState = LedUiState(
                                color = LedColor.GREEN,
                                state = LedState.GREEN_UNLOCKED,
                                label = "Verde desbloqueado", // como setLedMode(GREEN_UNLOCKED) en door.cpp:20
                                lastEventAt = now,
                                source = "access",
                                isLoading = false,
                                error = null
                            ),
                            lastSync = now,
                            isConnected = true
                        )
                    }
                    scheduleLedExpiry(5000L) // expira a OFF en 5s — como `setTimeout(() => setLed(OFF), 5000)` en JS
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
            repo.securityEventFlow.collect { ev -> // push security — como `socket.on('seguridad/intrusion', ...)`
                if (ev.event_type.equals("intrusion", ignoreCase = true)) { // case-insensitive — como `event_type === 'intrusion'` en JS
                    val now = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            ledState = LedUiState(
                                color = LedColor.RED,
                                state = LedState.RED_INTRUSION,
                                label = "Rojo intrusión", // como setLedMode(RED) en MEGA intrusión
                                lastEventAt = now,
                                source = "security",
                                isLoading = false,
                                error = null
                            ),
                            lastSync = now,
                            isConnected = true
                        )
                    }
                    scheduleLedExpiry(10000L) // HU-02 rojo 10s (LedStateMapper:36 RED_INTRUSION_WINDOW_MS)
                }
            }
        }
        // Estado MQTT — ahorro batería <50ms: si hay MQTT vivo (Connected), no hace falta poll HTTP 5s
        mqttStateJob?.cancel()
        mqttStateJob = viewModelScope.launch {
            repo.mqttConnectionState.collect { st -> // StateFlow — como `socket.on('connect', ...)` / `on('close', ...)`
                _uiState.update { it.copy(mqttState = st) }
                // ahorro: si MQTT vivo, para polling; si cae, reanuda polling (fallback HTTP)
                when (st) {
                    is MqttConnectionState.Connected -> stopLedPolling() // MQTT vivo → no poll (ahorro batería, menos HTTP)
                    is MqttConnectionState.Disconnected,
                    is MqttConnectionState.Error -> if (autoPollLed) startLedPolling() // MQTT caído → poll HTTP 5s (fallback)
                    else -> {} // Connecting mantiene estado previo (no cambia polling)
                }
            }
        }
    }

    private fun scheduleLedExpiry(delayMs: Long) {
        ledExpiryJob?.cancel() // cancela previo — como `clearTimeout(prevId)` en JS (solo un expiry a la vez)
        ledExpiryJob = viewModelScope.launch {
            delay(delayMs) // espera ventana — como `await sleep(delayMs)` en JS
            // solo revierte a Apagado si no ha llegado otro evento más reciente dentro de la ventana (evita pisar nuevo evento)
            val age = System.currentTimeMillis() - (_uiState.value.ledState.lastEventAt ?: 0L)
            if (age >= delayMs - 100) { // margen 100ms — como debounce en JS
                _uiState.update {
                    it.copy(
                        ledState = LedUiState(
                            color = LedColor.OFF,
                            state = LedState.OFF,
                            label = "Apagado", // OFF — como LedState.OFF en LedStatusCard
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
        repo.disconnectMqtt() // como `socket.disconnect()` en JS
        mqttJob?.cancel(); mqttJob = null
        mqttStateJob?.cancel(); mqttStateJob = null
        mqttAccessJob?.cancel(); mqttAccessJob = null
        mqttSecurityJob?.cancel(); mqttSecurityJob = null
        ledExpiryJob?.cancel(); ledExpiryJob = null
        _uiState.update { it.copy(mqttState = MqttConnectionState.Disconnected) }
    }

    override fun onCleared() {
        // Limpia todo al destruir ViewModel — como `useEffect return () => cleanup` en React (evita leaks)
        disconnectMqtt()
        stopLedPolling()
        super.onCleared()
    }
}
