package com.aethernet.aethercontrol.data.remote.dto

// =============================================================================
// SecurityEventDto.kt — DTOs Security Events | 6º Semestre UTP | HU-02, RF-2.3
// Autor: Andres Felipe Martinez Henao
// Experiencia: 1 año PostgreSQL (security_events), 2 años Python (schemas), 2 años electrónica (láser KY-008),
//              2 años JS/React (DTOs)
// Analogía React: como `interface SecurityEvent { event_type: string, severity: "low"|"medium"|..., acknowledged: boolean }`
// en TS — tipa eventos de intrusión del láser.
// Analogía Python: como `SecurityEventCreate(BaseModel)` en backend/app/schemas.py:73 y :79 SecurityEventOut
// — aquí con @Serializable para Retrofit.
// Analogía C: como `enum SecurityEvent { INTRUSION, ACCESS_DENIED, RF_FAILSTOP }` + struct en C pero con JSON.
// Analogía PostgreSQL: 1:1 con backend/app/models.py:55 SecurityEvent y init.sql:26 security_events
// (event_type VARCHAR(32) INDEX, severity VARCHAR(16) DEFAULT 'medium', acknowledged BOOLEAN DEFAULT FALSE).
// FOSS: kotlinx.serialization — RNF-3.1.
// Espejo backend: backend/app/schemas.py:73 SecurityEventCreate y :79 SecurityEventOut.
// Flujo: Láser KY-008 se interrumpe → MEGA led.cpp:64 RED_INTRUSION → Gateway publish aethernet/seguridad/intrusion
// → Backend POST /api/security-events → Node-RED Telegram (RF-4.1) → App GET → rojo 10s en LedStatusCard.
// =============================================================================

import kotlinx.serialization.Serializable

/**
 * Espejo backend/app/schemas.py:73 SecurityEventCreate (HU-02, RF-2.3).
 * Payload POST /api/security-events — evento seguridad con severidad.
 */
@Serializable
data class SecurityEventCreate(
    val event_type: String, // "intrusion" (láser KY-008), "access_denied" (PIN fallido), "rf_failstop" (HU-04)
    val severity: String = "medium", // "low", "medium" (default), "high", "critical" — como priority en Jira / severity en models.py:60
    val description: String? = null // "Laser interrupted at door" — TEXT nullable en init.sql:30
)

@Serializable
data class SecurityEventOut(
    val id: String, // UUID como String
    val event_type: String,
    val severity: String, // low/medium/high/critical
    val description: String? = null, // nullable — puede ser solo event_type sin detalle
    val timestamp: String, // ISO-8601 — para LedStateMapper:167 parse y ventana 10s roja intrusión
    val acknowledged: Boolean, // ¿admin vio alerta? — como `read` en notificaciones React (models.py:63), default FALSE
    val acknowledged_at: String? = null, // cuándo se marcó visto — nullable hasta ack (models.py:64)
    val acknowledged_by: String? = null // quién lo atendió — "admin" — nullable
)
