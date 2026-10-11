package com.aethernet.aethercontrol.data.remote.dto

// =============================================================================
// RoverTelemetryDto.kt — DTOs Rover Telemetry | 6º Semestre UTP | RF-3.1, RF-3.3, HU-04
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años electrónica/Arduino (L298N, HC-SR04, TCRT5000, nRF24L01),
//              1 año C (struct RoverTelemetry), 1 año PostgreSQL (rover_telemetry),
//              2 años Python (Pydantic), 2 años JS/React (DTO)
// Analogía React: como `interface RoverTelemetry { left_motor_pwm: number, ultrasonic_distance_cm?: number, ir_left?: boolean }`
// en TS — tipa cada paquete RF del Rover que llega por MQTT/HTTP.
// Analogía Python: como `RoverTelemetryCreate(BaseModel)` en backend/app/schemas.py:95 con
// `ge=-255 le=255` para PWM — aquí sin validación (Retrofit solo serializa), validación en backend.
// Analogía C: espejo directo de `struct RoverTelemetry` en firmware/rover-uno/rover-uno.ino:91
// (int16_t left_pwm, right_pwm, uint16_t ultrasonic_cm, bool ir_left/center/right, int8_t rf_rssi)
// pero en Kotlin con `Int` y `Boolean` + JSON (como ArduinoJson en rover.ino).
// Analogía PostgreSQL: 1:1 con backend/app/models.py:73 RoverTelemetry y init.sql:37 rover_telemetry
// (session_id UUID INDEX, left_motor_pwm SMALLINT, ultrasonic_distance_cm NUMERIC(6,2), ir_* BOOLEAN).
// FOSS: kotlinx.serialization — RNF-3.1.
// Espejo backend: backend/app/schemas.py:95 RoverTelemetryCreate y :106 RoverTelemetryOut.
// Flujo: Rover UNO lee HC-SR04 EMA + TCRT → RF nRF24L01 → Gateway publishRoverTelemetry → MQTT aethernet/rover/telemetry
// → Backend POST /api/rover/telemetry (session_id) → App MqttManager → DashboardScreen Card "Rover L=120 R=120 US=35cm".
// =============================================================================

import kotlinx.serialization.Serializable

/**
 * Espejo backend/app/schemas.py:95 RoverTelemetryCreate (RF-3.1, RF-3.3, HU-04).
 * Payload POST /api/rover/telemetry — telemetría RF por sesión (como struct RoverTelemetry en C).
 */
@Serializable
data class RoverTelemetryCreate(
    val session_id: String, // UUID como String — agrupa telemetría por run joystick (como sessionId en analytics, models.py:77)
    val left_motor_pwm: Int, // -255..255 — como int16_t left_pwm en C rover.ino:92 (SmallInteger en models.py:78, constrain en rover.ino:271)
    val right_motor_pwm: Int, // -255..255 — der
    val ultrasonic_distance_cm: Float? = null, // 0-500cm, nullable si HC-SR04 timeout — como ultrasonicEma en rover.ino:240 (Numeric(6,2) en models.py:80)
    val ir_left: Boolean? = null, // TCRT true si borde (<500) — como bool ir_left en C rover.ino:240 (BOOLEAN en models.py:81)
    val ir_center: Boolean? = null,
    val ir_right: Boolean? = null,
    val rf_rssi: Int? = null // -120..0 dBm — placeholder -70 en rover.ino:243 (RF24 sin RSSI real, SmallInteger en models.py:84)
)

@Serializable
data class RoverTelemetryOut(
    val id: String, // UUID PK — como id en Prisma
    val session_id: String,
    val left_motor_pwm: Int,
    val right_motor_pwm: Int,
    val ultrasonic_distance_cm: Float? = null,
    val ir_left: Boolean? = null,
    val ir_center: Boolean? = null,
    val ir_right: Boolean? = null,
    val rf_rssi: Int? = null,
    val timestamp: String // ISO-8601 — para ORDER BY timestamp DESC en GET /api/rover/telemetry (events.py:148) y para t-Student EST-05
)
