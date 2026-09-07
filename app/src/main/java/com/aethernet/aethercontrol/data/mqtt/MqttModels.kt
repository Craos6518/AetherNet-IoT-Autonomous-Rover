package com.aethernet.aethercontrol.data.mqtt

import kotlinx.serialization.Serializable

/**
 * MOV-03 — Modelos MQTT (RF-1.1, RNF-3.1).
 * Topics Mosquitto: backend/mosquitto/config/mosquitto.conf:4 1883 tcp + :8 9001 ws
 *   acl.conf:14 aethernet/# + :7 rover/#, access/#, seguridad/#, system/#, sensor/#
 * Gateway publica: firmware/gateway-esp32/gateway-esp32.ino:69 rover/telemetry :71 access/event :72 seguridad/intrusion :73 system/status
 * Gateway subscribe: :207 rover/command + access/command (verificado mosquitto_pub 192.168.1.14 docs/sprints.md:53)
 * WebSocket 9001 es MQTT sobre WebSocket, no HTTP — backend/app/main.py no expone WS propio en Sprint2.
 */
@Serializable
data class RoverTelemetryMqtt(
    val left_pwm: Int = 0,
    val right_pwm: Int = 0,
    val ultrasonic_cm: Int? = null,
    val ir_left: Boolean? = null,
    val ir_center: Boolean? = null,
    val ir_right: Boolean? = null,
    val rf_rssi: Int? = null,
    val timestamp: Long? = null
)

@Serializable
data class AccessEventMqtt(
    val user_id: String,
    val pin_hash: String,
    val success: Boolean,
    val source: String = "keypad"
)

@Serializable
data class SecurityEventMqtt(
    val event_type: String,
    val severity: String? = null,
    val description: String? = null
)

/** Estado conexión MQTT expuesto a UI como StateFlow — para banner y fallback polling. */
sealed interface MqttConnectionState {
    data object Disconnected : MqttConnectionState
    data object Connecting : MqttConnectionState
    data class Connected(val broker: String) : MqttConnectionState
    data class Error(val msg: String) : MqttConnectionState
}
