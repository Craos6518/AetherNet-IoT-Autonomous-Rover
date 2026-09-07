package com.aethernet.aethercontrol.domain.model

// =============================================================================
// UiModels.kt — UiState Dashboard | 6º Semestre UTP | MOV-01 5.1, MOV-02, MOV-03
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años JS/React (useState, Redux), 2 años Python (dataclass),
//              2 años electrónica (MqttManager), 1 año C (StateFlow)
// Analogía React: este data class es como `type DashboardUiState = { isLoading: boolean, isConnected: boolean, health: HealthResponse | null, ... }`
// en TypeScript — un objeto que junta todo el estado que DashboardScreen necesita (como Redux state).
// Analogía Python: como `@dataclass class DashboardUiState:` en Python con defaults — aquí data class Kotlin con defaults.
// Analogía C: como `struct DashboardState { bool isLoading; bool isConnected; ... }` en C pero con Flow reactivo.
// FOSS: data class pura Kotlin, sin lib propietaria (RNF-3.1).
// Origen: MOV-01 5.1 (RF-1.1 Dashboard), MOV-02 LedState (RF-1.1 LED local), MOV-03 MqttState + Rover (RF-1.1 <50ms prd.md:50).
// Usado por: DashboardViewModel:34 MutableStateFlow<DashboardUiState> y DashboardScreen:46 collectAsStateWithLifecycle.
// =============================================================================

import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState // estado MQTT (Disconnected/Connecting/Connected/Error)
import com.aethernet.aethercontrol.data.mqtt.RoverTelemetryMqtt // telemetría Rover push (left_pwm, ultrasonic_cm, ir_*)
import com.aethernet.aethercontrol.data.remote.dto.HealthResponse // health backend GET /health (status, database, version)

/**
 * UiState para Dashboard — MOV-01 5.1 (RF-1.1).
 * Junta todo lo que DashboardScreen pinta: health, LED, MQTT, Rover.
 * MOV-02 extiende con ledState: LedUiState (RF-1.1 LED local solo lectura, HU-01/HU-02).
 * MOV-03 extiende con mqttState + lastRover (RF-1.1 tiempo real <50ms prd.md:50, Mosquitto 1883/9001).
 * Si vienes de React: es el `state` de un `useReducer` con 7 campos — cada setState actualiza un campo.
 * Si vienes de Python: es como `DashboardUiState` dataclass con 7 attributes y defaults.
 */
data class DashboardUiState(
    val isLoading: Boolean = false, // true si fetch health/LED en curso — como isLoading en React Query (muestra CircularProgressIndicator)
    val isConnected: Boolean = false, // true si health ok — como isConnected en React (banner Desconectado si false en DashboardScreen:65)
    val health: HealthResponse? = null, // respuesta GET /health — nullable hasta primer fetch (como data en React Query)
    val error: String? = null, // mensaje error health — nullable, se pinta rojo en DashboardScreen:134
    val lastSync: Long? = null, // millis última sync exitosa — para "Última sync: 123456" en DashboardScreen:153
    val ledState: LedUiState = LedUiState(), // LED local — default UNKNOWN hasta primer getLedState (MOV-02, ver LedState.kt:38)
    val mqttState: MqttConnectionState = MqttConnectionState.Disconnected, // MQTT — default Disconnected hasta connectMqtt (MOV-03, ver MqttModels.kt:40)
    val lastRover: RoverTelemetryMqtt? = null // última telemetría Rover push — nullable hasta primer MQTT aethernet/rover/telemetry (MOV-03)
)
