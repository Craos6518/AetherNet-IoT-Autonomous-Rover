package com.aethernet.aethercontrol.data.remote

// =============================================================================
// ApiService.kt — Contrato Retrofit | 6º Semestre UTP | RF-1.1, RNF-3.1
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años HTML/CSS/JS/React (fetch, axios), 2 años Python (FastAPI),
//              1 año PostgreSQL (backend routers)
// Analogía React: este interface es como `interface ApiService { getHealth(): Promise<HealthResponse>; getAccessEvents(limit: number): Promise<AccessEventOut[]> }`
// en TypeScript con `axios` — aquí con Retrofit + suspend (coroutines) en vez de Promise.
// Analogía Python: como `class ApiService` con `@GET("/health") def get_health()` en FastAPI pero en cliente Kotlin (espejo servidor).
// FOSS: Retrofit + OkHttp + kotlinx.serialization (Square, Apache 2.0) — RNF-3.1, sin SDK propietario.
// Origen: Contrato Retrofit espejo backend/app/main.py:48 (title AetherNet IoT API) y routers/events.py:31,58,92,123 (RF-1.1).
// Cada @GET/@POST mapea 1:1 a un endpoint FastAPI — como `fetch('/api/access-events')` en JS pero tipado con DTOs.
// =============================================================================

import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.HealthResponse
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryOut
import com.aethernet.aethercontrol.data.remote.dto.RootResponse
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventOut
import retrofit2.http.Body // @Body — como `body: JSON.stringify(payload)` en fetch()
import retrofit2.http.GET // @GET — como `fetch(url, {method: 'GET'})` en JS
import retrofit2.http.POST // @POST — como `fetch(url, {method: 'POST'})` en JS
import retrofit2.http.Query // @Query — como `?limit=50` en URL (query params)

/**
 * Contrato Retrofit espejo backend/app/main.py:48 (title AetherNet IoT API)
 * y routers/events.py:31,58,92,123 (RF-1.1, RNF-3.1).
 * Cada método es un endpoint FastAPI — Retrofit genera el `fetch` tipado con DTOs (como axios con Zod).
 * Si vienes de React: es como `interface ApiService { getHealth(): Promise<HealthResponse> }` en TS con `axios.get('/health')`.
 */
interface ApiService {

    @GET("/")
    suspend fun getRoot(): RootResponse // GET / — como `fetch('/')` en JS, retorna {name, version, docs, health} (backend/app/main.py:68)

    @GET("health")
    suspend fun getHealth(): HealthResponse // GET /health — como `fetch('/health')` en JS, retorna {status, database, version} (backend/app/main.py:78, ci.yml:149)

    // Access Events — HU-01, RF-2.2 — MEGA keypad → Gateway → Backend → App
    @POST("api/access-events")
    suspend fun createAccessEvent(@Body payload: AccessEventCreate): AccessEventOut // POST — como `fetch('/api/access-events', {method: 'POST', body: JSON.stringify(payload)})` en JS

    @GET("api/access-events")
    suspend fun getAccessEvents(
        @Query("limit") limit: Int = 50, // limit — como `?limit=50` en URL (paginación, como limit en SQL)
        @Query("offset") offset: Int = 0 // offset — como `?offset=0` (para infinite scroll en React, como OFFSET en PostgreSQL)
    ): List<AccessEventOut> // List — como `Promise<AccessEventOut[]>` en TS (array JSON)

    // Sensor Events — RNF-2.1, HU-03 — HC-SR04 EMA
    @POST("api/sensor-events")
    suspend fun createSensorEvent(@Body payload: SensorEventCreate): SensorEventOut

    @GET("api/sensor-events")
    suspend fun getSensorEvents(
        @Query("sensor_type") sensorType: String? = null, // filtro opcional — como `?sensor_type=ultrasonic` en JS (WHERE sensor_type en SQL)
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<SensorEventOut>

    // Security Events — HU-02, RF-2.3 — láser KY-008 → Telegram
    @POST("api/security-events")
    suspend fun createSecurityEvent(@Body payload: SecurityEventCreate): SecurityEventOut

    @GET("api/security-events")
    suspend fun getSecurityEvents(
        @Query("event_type") eventType: String? = null, // filtro — como `?event_type=intrusion` en JS
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<SecurityEventOut>

    // Rover Telemetry — RF-3.1, RF-3.3, HU-04 — nRF24L01 → Gateway → Backend
    @POST("api/rover/telemetry")
    suspend fun createRoverTelemetry(@Body payload: RoverTelemetryCreate): RoverTelemetryOut

    @GET("api/rover/telemetry")
    suspend fun getRoverTelemetry(
        @Query("session_id") sessionId: String? = null, // filtro por sesión — como `?session_id=uuid` en JS (WHERE session_id en SQL)
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<RoverTelemetryOut>
}
