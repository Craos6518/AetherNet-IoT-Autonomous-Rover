package com.aethernet.aethercontrol.data.remote.dto

// =============================================================================
// SensorEventDto.kt — DTOs Sensor Events | 6º Semestre UTP | RNF-2.1, HU-03
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 1 año PostgreSQL (init.sql sensor_events), 2 años Python (Pandas EMA),
//              2 años JS/React (DTOs), 2 años electrónica (HC-SR04, KY-037, TCRT5000)
// Analogía React: como `interface SensorEvent { sensor_id: string, value: number, filtered_value?: number }`
// en TS — tipa el JSON de telemetría con valor raw y filtrado EMA.
// Analogía Python: como `SensorEventCreate(BaseModel)` en backend/app/schemas.py:47 y
// `value`/`filtered_value` en stats/ema_filter.py — aquí el DTO lleva ambos para graficar raw vs ema.
// Analogía C: como `struct SensorEvent { char sensor_id[64]; float value; float filtered_value; }` en C
// pero con JSON (ArduinoJson) para HTTP/MQTT.
// Analogía PostgreSQL: campos 1:1 con backend/app/models.py:36 SensorEvent (value Numeric(10,4),
// filtered_value Numeric(10,4), unit VARCHAR(16), metadata JSONB) e init.sql:15.
// FOSS: kotlinx.serialization — RNF-3.1.
// Espejo backend: backend/app/schemas.py:47 SensorEventCreate y :56 SensorEventOut.
// Nota: @SerialName("metadata") no aplica aquí porque usamos "metadata" en DTO; backend usa
// validation_alias="event_metadata" (models.py:47) pero serializa como "metadata" — mantenemos nombre API.
// Fallback eventMetadata por si backend envía "event_metadata" por bug.
// =============================================================================

import kotlinx.serialization.SerialName // mapea nombre JSON distinto a campo Kotlin — como @JsonProperty en Jackson
import kotlinx.serialization.Serializable

/**
 * Espejo backend/app/schemas.py:47 SensorEventCreate (RNF-2.1 EMA) y :56 SensorEventOut.
 * Para telemetría HC-SR04 (ultrasonic cm), KY-037 (sound db), TCRT5000 (ir boolean) — con EMA.
 */
@Serializable
data class SensorEventCreate(
    val sensor_id: String, // ej. "hc-sr04-01" — deviceId IoT, como A0 en Arduino pero con nombre
    val sensor_type: String, // "ultrasonic", "sound", "ir", "laser", "rf" — indexado para EST-06 (filtrar por tipo)
    val value: Float, // raw — ej. 42.5 cm sin filtrar — como raw en rover-uno.ino:239 y stats serial_plot_ema.py raw_q
    val filtered_value: Float? = null, // EMA α=0.2 — nullable si aún no hay filtro (como ema en rover-uno.ino:240)
    val unit: String, // "cm", "db", "boolean" — para UI y conversión (como unit en stats/data)
    val metadata: Map<String, String> = emptyMap() // extras {"alpha":"0.2"} — como event_metadata JSONB en init.sql:23
)

@Serializable
data class SensorEventOut(
    val id: String, // UUID como String
    val sensor_id: String,
    val sensor_type: String,
    val value: Float,
    val filtered_value: Float? = null,
    val unit: String,
    val timestamp: String, // ISO-8601 TIMESTAMPTZ — parse con Instant.parse
    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap(), // nombre API "metadata" — como validation_alias en Pydantic
    // Fallback para event_metadata si backend lo envía con ese nombre (por bug o versión vieja)
    @SerialName("event_metadata")
    val eventMetadata: Map<String, String>? = null // nullable fallback — si viene con nombre columna, lo captura
)
