package com.aethernet.aethercontrol.data.mqtt

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * MOV-03 — Manager MQTT (RF-1.1, RNF-3.1).
 * Envuelve Paho MqttClient (EPL 1.0 FOSS) y expone SharedFlow + StateFlow.
 * No en ViewModel — respeta MVVM data/repository/AetherRepositoryImpl.kt:54 comentario MOV-03.
 *
 * Broker derivado de HttpUrl via ServiceLocator.kt:114 getCurrentBaseUrl()
 *   http://192.168.1.14:8000/ -> tcp://192.168.1.14:1883
 *   10.0.2.2 emulador -> tcp://10.0.2.2:1883
 *   ws://host:9001 deja como fallback si firewall bloquea 1883 (mosquitto.conf:8).
 *
 * Topics: firmware/gateway-esp32/gateway-esp32.ino:69/71/72/73 + acl.conf:7
 * Paho connect() bloquea -> withContext(Dispatchers.IO). SharedFlow extraBufferCapacity=32 no pierde si UI rota.
 */
class MqttManager(private val appContext: Context) {

    private var client: MqttClient? = null

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val _roverTelemetry = MutableSharedFlow<RoverTelemetryMqtt>(extraBufferCapacity = 32)
    val roverTelemetry: SharedFlow<RoverTelemetryMqtt> = _roverTelemetry.asSharedFlow()

    private val _accessEvents = MutableSharedFlow<AccessEventMqtt>(extraBufferCapacity = 32)
    val accessEvents: SharedFlow<AccessEventMqtt> = _accessEvents.asSharedFlow()

    private val _securityEvents = MutableSharedFlow<SecurityEventMqtt>(extraBufferCapacity = 32)
    val securityEvents: SharedFlow<SecurityEventMqtt> = _securityEvents.asSharedFlow()

    private val topics = listOf(
        "aethernet/rover/telemetry",     // gateway-esp32.ino:69
        "aethernet/access/event",        // gateway-esp32.ino:71
        "aethernet/seguridad/intrusion", // gateway-esp32.ino:72
        "aethernet/system/status"        // gateway-esp32.ino:73
    )

    private val jsonLenient = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /**
     * Deriva broker tcp de HttpUrl.
     * Ej: http://192.168.1.14:8000/ -> tcp://192.168.1.14:1883
     * Si necesitas WebSocket cambia retorno a ws://host:9001
     */
    fun getBrokerHostFromHttpUrl(httpUrl: String): String {
        val host = httpUrl
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")
            .substringBefore("/")
            .trim()
            .ifBlank { "10.0.2.2" }
        return "tcp://$host:1883"
    }

    fun getBrokerWsFromHttpUrl(httpUrl: String): String {
        val host = httpUrl
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")
            .substringBefore("/")
            .trim()
            .ifBlank { "10.0.2.2" }
        return "ws://$host:9001"
    }

    suspend fun connect(httpBaseUrl: String) = withContext(Dispatchers.IO) {
        if (client?.isConnected == true) return@withContext
        _connectionState.value = MqttConnectionState.Connecting
        try {
            val brokerUri = getBrokerHostFromHttpUrl(httpBaseUrl)
            val id = "aethernet-app-${UUID.randomUUID().toString().take(8)}" // gateway usa gateway-esp32 gateway-esp32.ino:41
            // Limpia cliente previo si quedó a medias
            try { client?.close() } catch (_: Exception) {}
            client = MqttClient(brokerUri, id, MemoryPersistence()).apply {
                setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        try {
                            topics.forEach { t -> subscribe(t, 0) } // QoS 0 LAN <50ms prd.md:50
                        } catch (_: Exception) {}
                        _connectionState.value = MqttConnectionState.Connected(brokerUri)
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        parse(topic, String(message.payload))
                    }

                    override fun connectionLost(cause: Throwable?) {
                        _connectionState.value = MqttConnectionState.Error(cause?.message ?: "lost")
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })
            }
            val opts = MqttConnectOptions().apply {
                isAutomaticReconnect = true // MOV-09 pulirá con backoff
                isCleanSession = true
                connectionTimeout = 5
                keepAliveInterval = 15
            }
            client!!.connect(opts)
            // connectComplete se disparará; por si Paho no invoca extended, forza:
            if (client?.isConnected == true && _connectionState.value is MqttConnectionState.Connecting) {
                _connectionState.value = MqttConnectionState.Connected(brokerUri)
                try { topics.forEach { t -> client?.subscribe(t, 0) } } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            _connectionState.value = MqttConnectionState.Error(e.message ?: "connect fail")
        }
    }

    private fun parse(topic: String, payload: String) {
        try {
            when {
                topic.startsWith("aethernet/rover/telemetry") -> {
                    val obj = jsonLenient.decodeFromString<RoverTelemetryMqtt>(payload)
                    _roverTelemetry.tryEmit(obj)
                }
                topic.startsWith("aethernet/access") -> {
                    val obj = jsonLenient.decodeFromString<AccessEventMqtt>(payload)
                    _accessEvents.tryEmit(obj)
                }
                topic.startsWith("aethernet/seguridad") -> {
                    val obj = jsonLenient.decodeFromString<SecurityEventMqtt>(payload)
                    _securityEvents.tryEmit(obj)
                }
                topic.startsWith("aethernet/system") -> {
                    // status heartbeat no emite, solo mantiene conexión viva
                }
            }
        } catch (_: Exception) {
            // payload malformado no rompe app
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
            client?.close()
        } catch (_: Exception) {}
        client = null
        _connectionState.value = MqttConnectionState.Disconnected
    }
}
