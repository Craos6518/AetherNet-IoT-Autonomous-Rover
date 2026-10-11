package com.aethernet.aethercontrol.data.remote

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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Contrato Retrofit espejo backend/app/main.py:48 (title AetherNet IoT API)
 * y routers/events.py:31,58,92,123 (RF-1.1, RNF-3.1).
 */
interface ApiService {

    @GET("/")
    suspend fun getRoot(): RootResponse

    @GET("health")
    suspend fun getHealth(): HealthResponse

    // Access Events
    @POST("api/access-events")
    suspend fun createAccessEvent(@Body payload: AccessEventCreate): AccessEventOut

    @GET("api/access-events")
    suspend fun getAccessEvents(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<AccessEventOut>

    // Sensor Events
    @POST("api/sensor-events")
    suspend fun createSensorEvent(@Body payload: SensorEventCreate): SensorEventOut

    @GET("api/sensor-events")
    suspend fun getSensorEvents(
        @Query("sensor_type") sensorType: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<SensorEventOut>

    // Security Events
    @POST("api/security-events")
    suspend fun createSecurityEvent(@Body payload: SecurityEventCreate): SecurityEventOut

    @GET("api/security-events")
    suspend fun getSecurityEvents(
        @Query("event_type") eventType: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<SecurityEventOut>

    // Rover Telemetry
    @POST("api/rover/telemetry")
    suspend fun createRoverTelemetry(@Body payload: RoverTelemetryCreate): RoverTelemetryOut

    @GET("api/rover/telemetry")
    suspend fun getRoverTelemetry(
        @Query("session_id") sessionId: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<RoverTelemetryOut>
}
