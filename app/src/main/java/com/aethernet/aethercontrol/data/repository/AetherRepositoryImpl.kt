package com.aethernet.aethercontrol.data.repository

import com.aethernet.aethercontrol.data.remote.ApiService
import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.util.Result
import com.aethernet.aethercontrol.util.safeCall

/**
 * Implementación real — MOV-01 4.2 (RF-1.1).
 * Inyectada vía ServiceLocator, no vía constructor de Activity.
 * Sin lógica MQTT — // MOV-03 comentario.
 */
class AetherRepositoryImpl(
    private val api: ApiService
) : AetherRepository {

    override suspend fun getHealth() = safeCall { api.getHealth() }

    override suspend fun getAccessEvents(limit: Int) = safeCall { api.getAccessEvents(limit = limit) }
    override suspend fun postAccessEvent(payload: AccessEventCreate) = safeCall { api.createAccessEvent(payload) }

    override suspend fun getSensorEvents(limit: Int) = safeCall { api.getSensorEvents(limit = limit) }
    override suspend fun postSensorEvent(payload: SensorEventCreate) = safeCall { api.createSensorEvent(payload) }

    override suspend fun getSecurityEvents(limit: Int) = safeCall { api.getSecurityEvents(limit = limit) }
    override suspend fun postSecurityEvent(payload: SecurityEventCreate) = safeCall { api.createSecurityEvent(payload) }

    override suspend fun getRoverTelemetry(limit: Int) = safeCall { api.getRoverTelemetry(limit = limit) }
    override suspend fun postRoverTelemetry(payload: RoverTelemetryCreate) = safeCall { api.createRoverTelemetry(payload) }

    // MOV-03: MQTT/WebSocket suscripción a telemetría se añadirá aquí (no en ViewModel)
}
