package com.aethernet.aethercontrol.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo backend/app/schemas.py:47 SensorEventCreate y :56 SensorEventOut (RNF-2.1, HU-03).
 * Nota: @SerialName("event_metadata") no aplica aquí porque usamos "metadata" en DTO;
 * el backend usa validation_alias/serialization_alias. Mantenemos metadata como nombre API.
 */
@Serializable
data class SensorEventCreate(
    val sensor_id: String,
    val sensor_type: String,
    val value: Float,
    val filtered_value: Float? = null,
    val unit: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class SensorEventOut(
    val id: String,
    val sensor_id: String,
    val sensor_type: String,
    val value: Float,
    val filtered_value: Float? = null,
    val unit: String,
    val timestamp: String,
    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap(),
    // Fallback para event_metadata si backend lo envía con ese nombre
    @SerialName("event_metadata")
    val eventMetadata: Map<String, String>? = null
)
