package com.aethernet.aethercontrol.data.mqtt

// =============================================================================
// MqttModels.kt — Modelos MQTT y Estado Conexión | 6º Semestre UTP | MOV-03 RF-1.1, RNF-3.1
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años electrónica/Arduino (nRF24L01, UART), 2 años Python (Mosquitto paho-mqtt),
//              2 años JS/React (MQTT.js, WebSocket), 1 año C (struct RoverTelemetry)
// Analogía React: estos data class son como `interface MqttMessage { left_pwm: number, ... }` en TS
// — tipan el JSON que llega por MQTT (como props de un evento WebSocket en React).
// Analogía Python: como `RoverTelemetry(BaseModel)` en backend/app/schemas.py:95 pero para MQTT push
// (no HTTP). Aquí con @Serializable para que MqttManager jsonLenient decode auto.
// Analogía C: espejo de `struct RoverTelemetry` en firmware/rover-uno/rover-uno.ino:91
// (int16_t left_pwm etc.) pero con `val ultrasonic_cm: Int?` nullable (Kotlin) vs `uint16_t` (C).
// Analogía PostgreSQL: campos mapean a backend/app/models.py:73 RoverTelemetry Columns
// pero por MQTT llegan como JSON, no por HTTP POST.
// FOSS: Paho MQTTv3 (EPL 1.0) + kotlinx.serialization (Apache 2.0) — RNF-3.1, sin SDK propietario.
// Origen: MOV-03 RF-1.1, Mosquitto 1883 tcp + 9001 ws (backend/mosquitto/config/mosquitto.conf:4),
// acl.conf:14 aethernet/#, Gateway publica firmware/gateway-esp32/gateway-esp32.ino:69/71/72/73.
// Gateway subscribe: :207 rover/command + access/command (verificado mosquitto_pub 192.168.1.14 docs/sprints.md:53).
// WebSocket 9001 es MQTT sobre WebSocket, no HTTP — backend/app/main.py no expone WS propio en Sprint2.
// =============================================================================

import kotlinx.serialization.Serializable // @Serializable genera parser MQTT payload JSON auto (como Zod en JS)

/**
 * MOV-03 — Modelos MQTT (RF-1.1, RNF-3.1).
 * Topics Mosquitto: backend/mosquitto/config/mosquitto.conf:4 1883 tcp + :8 9001 ws
 *   acl.conf:14 aethernet/# + :7 rover/#, access/#, seguridad/#, system/#, sensor/#
 * Gateway publica: firmware/gateway-esp32/gateway-esp32.ino:69 rover/telemetry :71 access/event :72 seguridad/intrusion :73 system/status
 * Gateway subscribe: :207 rover/command + access/command (verificado mosquitto_pub 192.168.1.14 docs/sprints.md:53)
 * WebSocket 9001 es MQTT sobre WebSocket, no HTTP — backend/app/main.py no expone WS propio en Sprint2.
 * Si vienes de React: es como `type RoverTelemetryMqtt = {left_pwm: number, ultrasonic_cm?: number}` en TS.
 */
@Serializable
data class RoverTelemetryMqtt(
    val left_pwm: Int = 0, // -255..255 — como left_motor_pwm SMALLINT en backend/app/models.py:78, int16_t en C rover.ino:85
    val right_pwm: Int = 0,
    val ultrasonic_cm: Int? = null, // nullable — si HC-SR04 timeout (rover.ino:239 0 = sin eco), aquí null
    val ir_left: Boolean? = null, // TCRT true si borde (<500) — como bool ir_left en C rover.ino:240 (nullable si desconectado)
    val ir_center: Boolean? = null,
    val ir_right: Boolean? = null,
    val rf_rssi: Int? = null, // -120..0 dBm — placeholder -70 en rover.ino:243 (RF24 sin RSSI real)
    val timestamp: Long? = null // millis — como millis() en C, pero aquí Long? (nullable hasta primer publish)
)

@Serializable
data class AccessEventMqtt(
    val user_id: String, // "keypad_user" o ID App — como user_id VARCHAR(64) en init.sql:8
    val pin_hash: String, // hash HEX djb2 — nunca "1234" — como pin_hash VARCHAR(128) en init.sql:9 (PinValidator.hashPin)
    val success: Boolean, // true = verde 5s, false = rojo 1s — como success BOOLEAN en init.sql:10
    val source: String = "keypad" // "keypad" (MEGA), "app" (MQTT), "bluetooth" — default keypad
)

@Serializable
data class SecurityEventMqtt(
    val event_type: String, // "intrusion" (láser), "access_denied", "rf_failstop" — como event_type VARCHAR(32) en init.sql:28
    val severity: String? = null, // "low/medium/high/critical" — nullable, default medium en backend
    val description: String? = null // "Laser interrupted" — nullable TEXT en init.sql:30
)

/** Estado conexión MQTT expuesto a UI como StateFlow — para banner "MQTT ● tcp://192.168.1.14:1883" y fallback polling. */
sealed interface MqttConnectionState { // sealed = union type en TS: "Disconnected" | "Connecting" | {broker:string} | {msg:string}
    data object Disconnected : MqttConnectionState // inicial y tras disconnect() — como isConnected false en React
    data object Connecting : MqttConnectionState // conectando — muestra "MQTT ○ Conectando..." en DashboardScreen:78
    data class Connected(val broker: String) : MqttConnectionState // conectado — "MQTT ● tcp://host:1883" (MqttManager:100)
    data class Error(val msg: String) : MqttConnectionState // error — "MQTT ✕ lost" — repasa a polling 5s (DashboardViewModel:208)
}
