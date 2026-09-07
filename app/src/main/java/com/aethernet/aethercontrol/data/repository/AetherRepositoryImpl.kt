package com.aethernet.aethercontrol.data.repository

import com.aethernet.aethercontrol.data.mqtt.AccessEventMqtt
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.mqtt.MqttManager
import com.aethernet.aethercontrol.data.mqtt.RoverTelemetryMqtt
import com.aethernet.aethercontrol.data.mqtt.SecurityEventMqtt
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementación real — MOV-01 4.2 + MOV-03 (RF-1.1).
 * Inyectada vía ServiceLocator, MQTT delegado a MqttManager (no en ViewModel).
 */
class AetherRepositoryImpl(
    private val api: ApiService,
    private val mqtt: MqttManager? = null
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

    // MOV-03: MQTT/WebSocket suscripción a telemetría — delega a MqttManager, fallback HTTP si null (tests)
    override val mqttConnectionState: StateFlow<MqttConnectionState>
        get() = mqtt?.connectionState ?: MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    override val roverTelemetryFlow: SharedFlow<RoverTelemetryMqtt>
        get() = mqtt?.roverTelemetry ?: MutableSharedFlow()
    override val accessEventFlow: SharedFlow<AccessEventMqtt>
        get() = mqtt?.accessEvents ?: MutableSharedFlow()
    override val securityEventFlow: SharedFlow<SecurityEventMqtt>
        get() = mqtt?.securityEvents ?: MutableSharedFlow()

    override suspend fun connectMqtt(httpBaseUrl: String) {
        mqtt?.connect(httpBaseUrl)
    }

    override fun disconnectMqtt() {
        mqtt?.disconnect()
    }
}
