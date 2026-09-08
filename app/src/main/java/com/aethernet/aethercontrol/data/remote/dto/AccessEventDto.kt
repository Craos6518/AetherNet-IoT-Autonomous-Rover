package com.aethernet.aethercontrol.data.remote.dto

// =============================================================================
// AccessEventDto.kt — DTOs Access Events | 6º Semestre UTP | HU-01, RF-2.2
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 1 año PostgreSQL (init.sql access_events), 2 años Python (schemas.py),
//              2 años JS/React (DTO como interface TS), 2 años electrónica (keypad 4x4)
// Analogía React: estos data class son como `interface AccessEvent { user_id: string, pin_hash: string, ... }`
// en TypeScript — tipan el JSON que viaja entre App (Retrofit) y Backend (FastAPI).
// Analogía Python: como `AccessEventCreate(BaseModel)` y `AccessEventOut(BaseModel)` en backend/app/schemas.py:26/33
// — aquí con @Serializable para que Retrofit + kotlinx.serialization parseen auto.
// Analogía C: como `struct AccessEvent { char user_id[64]; char pin_hash[128]; bool success; }` en C
// pero con JSON serialización automática (como ArduinoJson en gateway-esp32.ino:366).
// Analogía PostgreSQL: campos mapean 1:1 a backend/app/models.py:20 AccessEvent Columns
// y a init.sql:6 access_events (id UUID, user_id VARCHAR(64), pin_hash VARCHAR(128), success BOOLEAN, timestamp TIMESTAMPTZ).
// FOSS: kotlinx.serialization (Apache 2.0) — RNF-3.1.
// Espejo backend: backend/app/schemas.py:26 AccessEventCreate y :33 AccessEventOut.
// Flujo: MEGA keypad "1234#" → Gateway aethernet/access/event → Backend POST /api/access-events → App GET → DTO.
// =============================================================================

import kotlinx.serialization.Serializable // genera serializer JSON — como Zod schema en JS

/**
 * Espejo backend/app/schemas.py:26 AccessEventCreate (HU-01, RF-2.2).
 * Payload POST /api/access-events — Gateway lo envía con pin_hash (nunca PIN claro).
 * En C es como `hashPin(pin)` djb2 HEX en firmware/mega-access/src/keypad_control.cpp:99.
 */
@Serializable
data class AccessEventCreate(
    val user_id: String, // ej. "keypad_user" (físico) o ID App — como user_id VARCHAR(64) en init.sql:8
    val pin_hash: String, // hash HEX del PIN (djb2), nunca "1234" — como pin_hash VARCHAR(128) en init.sql:9
    val success: Boolean, // true = desbloqueó (LED verde 5s), false = falló (LED rojo 1s) — como success BOOLEAN en init.sql:10
    val source: String = "keypad" // "keypad" (MEGA físico), "app" (MQTT), "bluetooth" (RF-1.3 futuro) — default keypad
)

@Serializable
data class AccessEventOut(
    val id: String, // UUID como String para evitar serializador custom (Sprint 1) — en backend es UUID tipo nativo
    val user_id: String,
    val pin_hash: String,
    val success: Boolean,
    val timestamp: String, // ISO-8601 con TZ (server_default func.now() en backend/app/models.py:27) — parse con Instant.parse en LedStateMapper:167
    val source: String
)
