package com.aethernet.aethercontrol.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Espejo backend/app/schemas.py:95 RoverTelemetryCreate y :106 RoverTelemetryOut (RF-3.1, RF-3.3, HU-04).
 */
@Serializable
data class RoverTelemetryCreate(
    val session_id: String,
    val left_motor_pwm: Int,
    val right_motor_pwm: Int,
    val ultrasonic_distance_cm: Float? = null,
    val ir_left: Boolean? = null,
    val ir_center: Boolean? = null,
    val ir_right: Boolean? = null,
    val rf_rssi: Int? = null
)

@Serializable
data class RoverTelemetryOut(
    val id: String,
    val session_id: String,
    val left_motor_pwm: Int,
    val right_motor_pwm: Int,
    val ultrasonic_distance_cm: Float? = null,
    val ir_left: Boolean? = null,
    val ir_center: Boolean? = null,
    val ir_right: Boolean? = null,
    val rf_rssi: Int? = null,
    val timestamp: String
)
