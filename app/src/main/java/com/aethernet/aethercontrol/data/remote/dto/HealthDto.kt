package com.aethernet.aethercontrol.data.remote.dto

// =============================================================================
// HealthDto.kt — DTOs Health y Root | 6º Semestre UTP | RF-1.1
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años Python (Pydantic), 2 años JS/React (Zod/fetch), 2 años electrónica (health check),
//              1 año PostgreSQL (backend health)
// Analogía React: estos DTOs son como `type HealthResponse = {status: string, database: string, version: string}`
// en TypeScript — reflejan el JSON que devuelve FastAPI GET /health (backend/app/schemas.py:17).
// Analogía Python: como `HealthResponse(BaseModel)` en Pydantic — aquí con @Serializable de kotlinx.serialization.
// Analogía C: como `struct HealthResponse { char status[16]; char database[32]; }` pero con parsing JSON auto.
// Analogía PostgreSQL: `database` es el resultado de `SELECT 1` en backend/app/main.py:86 — "ok" o "error: ...".
// FOSS: kotlinx.serialization (Apache 2.0) + Retrofit — RNF-3.1, sin SDK propietario.
// Espejo backend: backend/app/schemas.py:17 HealthResponse y main.py:68 root — contrato Retrofit espejo.
// Uso: ApiService.getHealth(): HealthResponse y getRoot(): RootResponse (ver ApiService.kt:24).
// =============================================================================

import kotlinx.serialization.Serializable // @Serializable genera parser JSON auto — como Zod schema en JS

/**
 * Espejo de backend/app/schemas.py:17 HealthResponse (RF-1.1, RNF-1.1).
 * Backend retorna: { "status": "ok", "database": "ok", "version": "1.0.0-sprint1" }
 * Si DB cae, status "degraded" y database "error: ..." (ver backend/app/main.py:88).
 */
@Serializable
data class HealthResponse(
    val status: String,   // "ok" o "degraded" — como health en k8s probe (ver backend/app/main.py:90)
    val database: String, // "ok" o "error: ..." — resultado de SELECT 1 (backend/app/main.py:86)
    val version: String   // "1.0.0-sprint1" — como version en package.json, útil para debug
)

/**
 * Espejo de backend/app/main.py:68 GET / root.
 * Backend retorna: { "name": "AetherNet IoT API", "version": "1.0.0-sprint1", "docs": "/docs", "health": "/health" }
 */
@Serializable
data class RootResponse(
    val name: String,    // "AetherNet IoT API" — como name en package.json
    val version: String, // "1.0.0-sprint1"
    val docs: String,    // "/docs" — Swagger UI auto-generado FastAPI (como /api-docs en Express)
    val health: String   // "/health" — endpoint health check (ver HealthResponse)
)
