package com.aethernet.aethercontrol.data.repository

import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.HealthResponse
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryOut
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventOut
import com.aethernet.aethercontrol.util.Result

/**
 * Contrato de repositorio — MOV-01 4.1 (RF-1.1, backends routers/events.py).
 * No expone ApiService ni DTO crudo si existiera capa domain distinta.
 * Por ahora DTO = domain model para MOV-01 (mapeo futuro si diverge).
 */
interface AetherRepository {

    suspend fun getHealth(): Result<HealthResponse>

    suspend fun getAccessEvents(limit: Int = 50): Result<List<AccessEventOut>>
    suspend fun postAccessEvent(payload: AccessEventCreate): Result<AccessEventOut>

    suspend fun getSensorEvents(limit: Int = 50): Result<List<SensorEventOut>>
    suspend fun postSensorEvent(payload: SensorEventCreate): Result<SensorEventOut>

    suspend fun getSecurityEvents(limit: Int = 50): Result<List<SecurityEventOut>>
    suspend fun postSecurityEvent(payload: SecurityEventCreate): Result<SecurityEventOut>

    suspend fun getRoverTelemetry(limit: Int = 50): Result<List<RoverTelemetryOut>>
    suspend fun postRoverTelemetry(payload: RoverTelemetryCreate): Result<RoverTelemetryOut>
}
