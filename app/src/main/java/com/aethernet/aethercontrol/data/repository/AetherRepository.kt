package com.aethernet.aethercontrol.data.repository

import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.HealthResponse
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryOut
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.data.mqtt.AccessEventMqtt
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.mqtt.RoverTelemetryMqtt
import com.aethernet.aethercontrol.data.mqtt.SecurityEventMqtt
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventOut
import com.aethernet.aethercontrol.domain.model.LedUiState
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

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

    /** MOV-02: deriva LedUiState del último evento (RF-1.1 HU-01/HU-02, solo lectura). */
    suspend fun getLedState(): Result<LedUiState>

    // MOV-03: MQTT/WebSocket telemetría en tiempo real <50ms prd.md:50 — Mosquitto 1883/9001
    val mqttConnectionState: StateFlow<MqttConnectionState>
    val roverTelemetryFlow: SharedFlow<RoverTelemetryMqtt>
    val accessEventFlow: SharedFlow<AccessEventMqtt>
    val securityEventFlow: SharedFlow<SecurityEventMqtt>
    suspend fun connectMqtt(httpBaseUrl: String)
    fun disconnectMqtt()
}
