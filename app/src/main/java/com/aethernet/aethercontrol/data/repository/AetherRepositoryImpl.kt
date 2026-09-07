package com.aethernet.aethercontrol.data.repository

import com.aethernet.aethercontrol.data.remote.ApiService
import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.domain.mapper.LedStateMapper
import com.aethernet.aethercontrol.domain.model.LedUiState
import com.aethernet.aethercontrol.util.Result
import com.aethernet.aethercontrol.util.safeCall
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

    /**
     * MOV-02: deriva LedUiState del último evento. Solo lectura — no existe /api/led.
     * Fetch paralelo getAccessEvents(1) + getSecurityEvents(1), compara timestamp y mapea con LedStateMapper.
     * ventana GREEN 5000ms (HU-01 DOOR_AUTO_LOCK_MS), RED_FAIL 1000ms, RED intrusión 10000ms (HU-02).
     * Futura MOV-03 migrará a MQTT Mosquitto sin romper UI (misma firma getLedState).
     */
    override suspend fun getLedState(): Result<LedUiState> = safeCall {
        coroutineScope {
            val accessDeferred = async { api.getAccessEvents(limit = 1) }
            val securityDeferred = async { api.getSecurityEvents(limit = 1) }
            val accessEvents = accessDeferred.await()
            val securityEvents = securityDeferred.await()
            LedStateMapper.map(accessEvents, securityEvents)
        }
    }

    // MOV-03: MQTT/WebSocket suscripción a telemetría se añadirá aquí (no en ViewModel)
}
