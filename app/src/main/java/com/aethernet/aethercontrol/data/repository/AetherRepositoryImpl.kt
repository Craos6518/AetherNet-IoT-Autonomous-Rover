package com.aethernet.aethercontrol.data.repository

// =============================================================================
// AetherRepositoryImpl.kt — Implementación Repositorio | 6º Semestre UTP | MOV-01 4.2 + MOV-03
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años Python (service impl), 2 años JS/React (api service), 2 años electrónica (Gateway),
//              1 año C (safeCall)
// Analogía React: este class es como `class AetherRepositoryImpl implements AetherRepository { async getHealth() { return safeCall(() => fetch('/health')) } }`
// en TS — implementa el contrato con `safeCall` (try/catch) y delega a `ApiService` (Retrofit) y `MqttManager` (Paho).
// Analogía Python: como `class AetherRepositoryImpl(AetherRepository): def get_health(self): return safe_call(self.api.get_health)` en Python.
// Analogía C: como implementar `struct` de funciones en C — aquí con `override suspend fun` (async).
// FOSS: impl pura Kotlin, sin dependencia propietaria (RNF-3.1), inyectada via ServiceLocator (DI manual, no Hilt).
// Origen: MOV-01 4.2 + MOV-03 (RF-1.1) — Inyectada vía ServiceLocator.repository (ver ServiceLocator.kt:116).
// Pattern: Repository envuelve ApiService (HTTP) + MqttManager (MQTT push) — ViewModel no conoce Paho ni Retrofit directo (MVVM).
// =============================================================================

import com.aethernet.aethercontrol.data.mqtt.AccessEventMqtt
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.mqtt.MqttManager
import com.aethernet.aethercontrol.data.mqtt.RoverTelemetryMqtt
import com.aethernet.aethercontrol.data.mqtt.SecurityEventMqtt
import com.aethernet.aethercontrol.data.remote.ApiService // Retrofit — como `fetch` en JS pero tipado
import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.domain.mapper.LedStateMapper // mapper LED — como selector en Redux
import com.aethernet.aethercontrol.domain.model.LedUiState
import com.aethernet.aethercontrol.util.Result // wrapper Success/Error — como {ok, data, error} en TS
import com.aethernet.aethercontrol.util.safeCall // safeCall — como try/catch wrapper (ver util/Result.kt:20)
import kotlinx.coroutines.async // async — como Promise.all en JS (paralelo)
import kotlinx.coroutines.coroutineScope // scope — como `await Promise.all([p1, p2])`
import kotlinx.coroutines.flow.MutableSharedFlow // fallback flows vacíos si mqtt null (tests)
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementación real — MOV-01 4.2 + MOV-03 (RF-1.1).
 * Inyectada vía ServiceLocator, MQTT delegado a MqttManager (no en ViewModel — respeta MVVM).
 * Si vienes de React: es como `class ApiServiceImpl { getHealth = () => safeCall(() => fetch('/health')) }` en TS.
 */
class AetherRepositoryImpl(
    private val api: ApiService, // Retrofit — HTTP GET/POST a FastAPI (ver ApiService.kt:22)
    private val mqtt: MqttManager? = null // Paho — MQTT push, nullable para tests sin Context (como mock en Jest)
) : AetherRepository {

    // Cada método envuelve `api.*` en `safeCall` — captura IOException/HttpException a Result.Error (ver util/Result.kt:20)
    // Como `try { Success(await fetch) } catch(e) { Error(e.message) }` en JS, pero reusado (DRY)
    override suspend fun getHealth() = safeCall { api.getHealth() } // GET /health — como `fetch('/health')`

    override suspend fun getAccessEvents(limit: Int) = safeCall { api.getAccessEvents(limit = limit) } // GET /api/access-events?limit=50
    override suspend fun postAccessEvent(payload: AccessEventCreate) = safeCall { api.createAccessEvent(payload) } // POST

    override suspend fun getSensorEvents(limit: Int) = safeCall { api.getSensorEvents(limit = limit) }
    override suspend fun postSensorEvent(payload: SensorEventCreate) = safeCall { api.createSensorEvent(payload) }

    override suspend fun getSecurityEvents(limit: Int) = safeCall { api.getSecurityEvents(limit = limit) }
    override suspend fun postSecurityEvent(payload: SecurityEventCreate) = safeCall { api.createSecurityEvent(payload) }

    override suspend fun getRoverTelemetry(limit: Int) = safeCall { api.getRoverTelemetry(limit = limit) }
    override suspend fun postRoverTelemetry(payload: RoverTelemetryCreate) = safeCall { api.createRoverTelemetry(payload) }

    /**
     * MOV-02: deriva LedUiState del último evento. Solo lectura — no existe /api/led en backend.
     * Fetch paralelo getAccessEvents(1) + getSecurityEvents(1) con `async`, compara timestamp y mapea con LedStateMapper.
     * Ventana GREEN 5000ms (HU-01 DOOR_AUTO_LOCK_MS config.h:52), RED_FAIL 1000ms (LED_RED_FAIL_MS), RED intrusión 10000ms (HU-02).
     * Futura MOV-03 migrará a MQTT Mosquitto push sin romper UI (misma firma getLedState) — ViewModel no cambia.
     * Si vienes de React: es como `const [access, security] = await Promise.all([fetch('/api/access-events?limit=1'), fetch('/api/security-events?limit=1')])`
     */
    override suspend fun getLedState(): Result<LedUiState> = safeCall {
        coroutineScope { // scope para async — como `await Promise.all` en JS
            val accessDeferred = async { api.getAccessEvents(limit = 1) } // fetch access limit 1 — como `fetch('/api/access-events?limit=1')`
            val securityDeferred = async { api.getSecurityEvents(limit = 1) } // fetch security limit 1 — paralelo (async)
            val accessEvents = accessDeferred.await() // espera ambos — como `await Promise.all`
            val securityEvents = securityDeferred.await()
            LedStateMapper.map(accessEvents, securityEvents) // mapea a LedUiState — como selector Redux (ver LedStateMapper.kt:39)
        }
    }

    // MOV-03: MQTT/WebSocket suscripción a telemetría — delega a MqttManager, fallback a flows vacíos si mqtt null (tests sin Context)
    // Como `this.mqtt?.roverTelemetry ?? new Subject()` en RxJS — si no hay MQTT (tests), no crashea
    override val mqttConnectionState: StateFlow<MqttConnectionState>
        get() = mqtt?.connectionState ?: MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    override val roverTelemetryFlow: SharedFlow<RoverTelemetryMqtt>
        get() = mqtt?.roverTelemetry ?: MutableSharedFlow() // fallback vacío — tests no necesitan MQTT real
    override val accessEventFlow: SharedFlow<AccessEventMqtt>
        get() = mqtt?.accessEvents ?: MutableSharedFlow()
    override val securityEventFlow: SharedFlow<SecurityEventMqtt>
        get() = mqtt?.securityEvents ?: MutableSharedFlow()

    override suspend fun connectMqtt(httpBaseUrl: String) {
        mqtt?.connect(httpBaseUrl) // delega a MqttManager.connect — deriva tcp://host:1883 de httpBaseUrl (MqttManager:86)
    }

    override fun disconnectMqtt() {
        mqtt?.disconnect() // delega — como `socket.disconnect()` en JS
    }

    // MOV-04 — S — HU-01 RF-2.2 — envía comando cerrojo vía MQTT aethernet/access/command {"pin":"1234"}
    // Payload espejo gateway-esp32.ino:70/332 handleAccessCommand: {"pin":"1234"} -> Gateway publish CMD:ACCESS -> MEGA processPinAttempt
    // No fallback HTTP: HTTP solo registra (POST /api/access-events), no acciona servo — MQTT es el que mueve hardware (Paho publish).
    // Retorna Error si MQTT no Connected — ViewModel muestra "MQTT no conectado" (PinViewModel:103).
    override suspend fun sendAccessCommand(pin: String): Result<Unit> {
        val m = mqtt ?: return Result.Error("MQTT no inicializado") // sin MqttManager (tests sin Context) — error
        val st = m.connectionState.value // estado actual — como `socket.connected` en JS
        if (st !is MqttConnectionState.Connected) {
            return Result.Error("MQTT no conectado — verifica 1883/9001 y broker ${m.getBrokerHostFromHttpUrl("http://temp")}") // como `if (!socket.connected) return Error`
        }
        return m.publishAccessCommand(pin) // delega a MqttManager.publishAccessCommand — valida 4..6 dígitos y publish (MqttManager:162)
    }
}
