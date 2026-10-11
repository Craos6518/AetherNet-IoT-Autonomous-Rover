package com.aethernet.aethercontrol.data.repository

// =============================================================================
// AetherRepository.kt — Contrato Repositorio | 6º Semestre UTP | MOV-01 4.1 RF-1.1
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años Python (repository pattern), 2 años JS/React (service layer),
//              1 año PostgreSQL (backend routers/events.py), 1 año C
// Analogía React: este interface es como `interface ApiService { getHealth(): Promise<HealthResponse> }`
// en TypeScript — contrato que describe qué puede hacer la app con el backend sin exponer Retrofit.
// Analogía Python: como `class AetherRepository(ABC)` con `@abstractmethod` — aquí interface Kotlin.
// Analogía C: como `struct` de punteros a función en C — contrato que AetherRepositoryImpl implementa.
// FOSS: interface pura Kotlin, sin lib propietaria (RNF-3.1).
// Origen: MOV-01 4.1 (RF-1.1, backends routers/events.py) — no expone ApiService ni DTO crudo si existe capa domain distinta.
// Por ahora DTO = domain model para MOV-01 (mapeo futuro si diverge, como DTO vs Entity en backend/app/schemas.py vs models.py).
// Usado por: DashboardViewModel, PinViewModel, ServiceLocator.repository — inyectado via ViewModelFactory.
// =============================================================================

import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.HealthResponse
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryOut
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.data.mqtt.AccessEventMqtt // MQTT push — como WebSocket message en React
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState // estado MQTT — como connectionState en WebSocket
import com.aethernet.aethercontrol.data.mqtt.RoverTelemetryMqtt
import com.aethernet.aethercontrol.data.mqtt.SecurityEventMqtt
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventOut
import com.aethernet.aethercontrol.domain.model.LedUiState // LED derivado — como selector en Redux
import com.aethernet.aethercontrol.util.Result // wrapper Success/Error/Loading — como {ok, data, error} en TS
import kotlinx.coroutines.flow.SharedFlow // SharedFlow — como Observable (push, no state)
import kotlinx.coroutines.flow.StateFlow // StateFlow — como useState reactivo (state + observable)

/**
 * Contrato de repositorio — MOV-01 4.1 (RF-1.1, backends routers/events.py).
 * No expone ApiService ni DTO crudo si existiera capa domain distinta (como no exponer Prisma Client en service).
 * Por ahora DTO = domain model para MOV-01 (mapeo futuro si diverge, como mapear DTO a Entity con Mapper).
 * Si vienes de React: es como `interface AetherRepository { getHealth(): Promise<Result<HealthResponse>> }` en TS.
 */
interface AetherRepository {

    suspend fun getHealth(): Result<HealthResponse> // GET /health — como `fetch('/health').then(r=>r.json())` en JS

    suspend fun getAccessEvents(limit: Int = 50): Result<List<AccessEventOut>> // GET /api/access-events?limit=50 — HU-01
    suspend fun postAccessEvent(payload: AccessEventCreate): Result<AccessEventOut> // POST /api/access-events — para App que simula MEGA (tests)

    suspend fun getSensorEvents(limit: Int = 50): Result<List<SensorEventOut>> // GET /api/sensor-events — RNF-2.1 HU-03 (HC-SR04 EMA)
    suspend fun postSensorEvent(payload: SensorEventCreate): Result<SensorEventOut>

    suspend fun getSecurityEvents(limit: Int = 50): Result<List<SecurityEventOut>> // GET /api/security-events — HU-02 láser
    suspend fun postSecurityEvent(payload: SecurityEventCreate): Result<SecurityEventOut>

    suspend fun getRoverTelemetry(limit: Int = 50): Result<List<RoverTelemetryOut>> // GET /api/rover/telemetry — RF-3.1
    suspend fun postRoverTelemetry(payload: RoverTelemetryCreate): Result<RoverTelemetryOut>

    /** MOV-02: deriva LedUiState del último evento (RF-1.1 HU-01/HU-02, solo lectura) — como selector Redux que computa estado. */
    suspend fun getLedState(): Result<LedUiState> // no existe /api/led — calcula de access+security (LedStateMapper)

    // MOV-03: MQTT/WebSocket telemetría en tiempo real <50ms prd.md:50 — Mosquitto 1883/9001 (como WebSocket en React)
    val mqttConnectionState: StateFlow<MqttConnectionState> // estado conexión — como connectionState en WebSocket (Disconnected/Connected)
    val roverTelemetryFlow: SharedFlow<RoverTelemetryMqtt> // push Rover — como `socket.on('rover', data)` en JS (SharedFlow no replay)
    val accessEventFlow: SharedFlow<AccessEventMqtt> // push access — PinViewModel colecta para lastResultSuccess
    val securityEventFlow: SharedFlow<SecurityEventMqtt> // push security — DashboardViewModel colecta para LED rojo intrusión
    suspend fun connectMqtt(httpBaseUrl: String) // conecta Paho con httpBaseUrl -> tcp://host:1883 (MqttManager:86)
    fun disconnectMqtt() // desconecta — como `socket.disconnect()` en JS

    /** MOV-04: envía comando de cerrojo/PIN desde la app (HU-01, RF-2.2 S). Usa MQTT si está conectado, fallback HTTP POST source=app. */
    suspend fun sendAccessCommand(pin: String): Result<Unit> // publish aethernet/access/command {"pin":"1234"} -> Gateway -> MEGA (ver MqttManager:162)

    /** MOV-05 RF-1.2: envía vectores joystick normalizados -1..1 o PWM directo -255..255 al Rover vía MQTT aethernet/rover/command. */
    suspend fun sendRoverCommand(leftPwm: Int, rightPwm: Int, mode: Int = 1): Result<Unit> // publish aethernet/rover/command {"left_pwm":..} -> Gateway handleRoverCommand:303 -> radio.write -> rover-uno.ino:219
    suspend fun sendRoverVector(x: Float, y: Float): Result<Unit> // helper x,y -1..1 → PWM tank-steering (JoystickMapper)
}
