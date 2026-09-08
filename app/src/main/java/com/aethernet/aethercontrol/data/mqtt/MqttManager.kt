package com.aethernet.aethercontrol.data.mqtt

// =============================================================================
// MqttManager.kt — Gestor MQTT Paho | 6º Semestre UTP | MOV-03 RF-1.1, RNF-3.1
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años Python (paho-mqtt), 2 años JS/React (MQTT.js, WebSocket),
//              2 años electrónica/Arduino (nRF24L01, UART), 1 año C (callbacks),
//              1 año PostgreSQL (backend Mosquitto)
// Analogía React: este class es como `class MqttClient { connect(url) { this.client = mqtt.connect(url) } }`
// en JS con MQTT.js — aquí con Paho Java, pero con StateFlow/SharedFlow en vez de EventEmitter.
// Analogía Python: como `paho.mqtt.client.Client()` en backend stats/ pero en Kotlin con coroutines.
// Analogía C: Paho callbacks son como `mqttCallback(char* topic, byte* payload)` en gateway-esp32.ino:191
// (MqttCallbackExtended) — aquí con `MqttCallbackExtended` interface.
// FOSS: Paho MQTTv3 (EPL 1.0) — RNF-3.1, sin AWS IoT SDK propietario.
// Origen: MOV-03 RF-1.1, Mosquitto tcp://host:1883 (mosquitto.conf:4) + fallback ws://host:9001 (:8),
// broker derivado de HttpUrl via ServiceLocator.kt:114 getCurrentBaseUrl() (http://192.168.1.14:8000/ -> tcp://192.168.1.14:1883).
// Topics: firmware/gateway-esp32/gateway-esp32.ino:69/71/72/73 + acl.conf:7 (aethernet/rover/# etc.).
// Paho connect() bloquea -> withContext(Dispatchers.IO). SharedFlow extraBufferCapacity=32 no pierde si UI rota (config).
// =============================================================================

import android.content.Context // para MqttManager(context) — necesita Context para nada, pero lo pide Paho (ver ServiceLocator:113)
import java.util.UUID // para id aethernet-app-xxxxxxxx (como gateway-esp32.ino:41 gateway-esp32)
import kotlinx.coroutines.Dispatchers // IO dispatcher — como worker thread en Node
import kotlinx.coroutines.flow.MutableSharedFlow // SharedFlow — como EventEmitter en Node pero con buffer
import kotlinx.coroutines.flow.MutableStateFlow // StateFlow — como useState en React pero reactivo (StateFlow)
import kotlinx.coroutines.flow.SharedFlow // tipo público SharedFlow — como Observable en RxJS
import kotlinx.coroutines.flow.StateFlow // tipo público StateFlow
import kotlinx.coroutines.flow.asSharedFlow // expone como read-only (como .asObservable() en RxJS)
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext // withContext(Dispatchers.IO) — como await en worker thread (no bloquea UI)
import kotlinx.serialization.json.Json // JSON lenient — como JSON.parse con try/catch en JS
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken // token entrega — como ack en MQTT QoS 1
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended // callback con connectComplete(reconnect, serverURI)
import org.eclipse.paho.client.mqttv3.MqttClient // cliente Paho — como mqtt.connect(url) en MQTT.js
import org.eclipse.paho.client.mqttv3.MqttConnectOptions // opciones — como {clean: true, keepalive: 15} en MQTT.js
import org.eclipse.paho.client.mqttv3.MqttMessage // mensaje — como {topic, payload} en MQTT.js
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence // persistencia en memoria — como sessionStorage en JS (no disco)

/**
 * MOV-03 — Manager MQTT (RF-1.1, RNF-3.1).
 * Envuelve Paho MqttClient (EPL 1.0 FOSS) y expone SharedFlow + StateFlow para Repository/UI.
 * No en ViewModel — respeta MVVM data/repository/AetherRepositoryImpl.kt:54 comentario MOV-03 (separación capas).
 *
 * Broker derivado de HttpUrl via ServiceLocator.kt:114 getCurrentBaseUrl()
 *   http://192.168.1.14:8000/ -> tcp://192.168.1.14:1883 (lanza MqttManager:91)
 *   10.0.2.2 emulador -> tcp://10.0.2.2:1883 (mapeo host)
 *   ws://host:9001 deja como fallback si firewall bloquea 1883 (mosquitto.conf:8 listener 9001 websockets).
 *
 * Topics: firmware/gateway-esp32/gateway-esp32.ino:69 rover/telemetry :71 access/event :72 seguridad/intrusion :73 system/status
 *   + acl.conf:7 aethernet/rover/#, access/#, seguridad/#, system/#, sensor/#
 * Paho connect() bloquea -> withContext(Dispatchers.IO) (no bloquea Main Thread, como async en React).
 * SharedFlow extraBufferCapacity=32 no pierde eventos si UI rota (como buffer en RxJS).
 */
class MqttManager(private val appContext: Context) {

    private var client: MqttClient? = null // cliente Paho — nullable hasta connect() (como mqtt client en JS null hasta connect)

    // StateFlow para UI — como useState<MqttConnectionState> en React pero reactivo (DashboardScreen:74 collectAsStateWithLifecycle)
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow() // read-only — como .asObservable()

    // SharedFlow con buffer 32 — si UI no colecta (rotación), no pierde 32 últimos (como event queue en Node)
    private val _roverTelemetry = MutableSharedFlow<RoverTelemetryMqtt>(extraBufferCapacity = 32)
    val roverTelemetry: SharedFlow<RoverTelemetryMqtt> = _roverTelemetry.asSharedFlow()

    private val _accessEvents = MutableSharedFlow<AccessEventMqtt>(extraBufferCapacity = 32)
    val accessEvents: SharedFlow<AccessEventMqtt> = _accessEvents.asSharedFlow()

    private val _securityEvents = MutableSharedFlow<SecurityEventMqtt>(extraBufferCapacity = 32)
    val securityEvents: SharedFlow<SecurityEventMqtt> = _securityEvents.asSharedFlow()

    // Topics suscritos — como `client.subscribe('aethernet/#')` en MQTT.js pero explícitos (ver acl.conf:14)
    private val topics = listOf(
        "aethernet/rover/telemetry",     // gateway-esp32.ino:69 — Rover → App (left_pwm, ultrasonic_cm, ir_*)
        "aethernet/access/event",        // gateway-esp32.ino:71 — MEGA → App (user_id, pin_hash, success)
        "aethernet/seguridad/intrusion", // gateway-esp32.ino:72 — MEGA láser → Node-RED Telegram → App
        "aethernet/system/status"        // gateway-esp32.ino:73 — Gateway heartbeat (no emite a UI, solo mantiene vivo)
    )

    // JSON lenient — ignoreUnknownKeys true evita crash si backend añade campo nuevo (como Zod passthrough)
    private val jsonLenient = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /**
     * Deriva broker tcp de HttpUrl (Retrofit baseUrl).
     * Ej: http://192.168.1.14:8000/ -> tcp://192.168.1.14:1883 (puerto Mosquitto 1883, ver mosquitto.conf:4)
     * Si necesitas WebSocket cambia retorno a ws://host:9001 (mosquitto.conf:8) — útil si firewall bloquea 1883.
     * Como `httpUrl.replace('http://', 'tcp://').replace(':8000/', ':1883')` en JS pero con split.
     */
    fun getBrokerHostFromHttpUrl(httpUrl: String): String {
        val host = httpUrl
            .removePrefix("http://") // quita esquema — como url.replace('http://', '') en JS
            .removePrefix("https://")
            .substringBefore(":") // antes de :8000 — host puro
            .substringBefore("/") // antes de / — sin path
            .trim()
            .ifBlank { "10.0.2.2" } // fallback emulador si httpUrl vacío (como DEFAULT_BASE_URL en PreferencesManager:28)
        return "tcp://$host:1883" // Mosquitto tcp 1883 — como `mqtt://host:1883` en MQTT.js
    }

    fun getBrokerWsFromHttpUrl(httpUrl: String): String {
        val host = httpUrl
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")
            .substringBefore("/")
            .trim()
            .ifBlank { "10.0.2.2" }
        return "ws://$host:9001" // Mosquitto WS 9001 — como `ws://host:9001` en MQTT.js over WebSocket
    }

    suspend fun connect(httpBaseUrl: String) = withContext(Dispatchers.IO) { // IO thread — como worker en Node, no bloquea UI
        if (client?.isConnected == true) return@withContext // ya conectado — no reconecta (como `if (client.connected) return` en JS)
        _connectionState.value = MqttConnectionState.Connecting // UI muestra "MQTT ○ Conectando..." (DashboardScreen:78)
        try {
            val brokerUri = getBrokerHostFromHttpUrl(httpBaseUrl) // deriva tcp://host:1883
            val id = "aethernet-app-${UUID.randomUUID().toString().take(8)}" // id único 8 chars — gateway usa gateway-esp32 (gateway.ino:41)
            // Limpia cliente previo si quedó a medias (como `client.end()` en MQTT.js antes de reconnect)
            try { client?.close() } catch (_: Exception) {}
            client = MqttClient(brokerUri, id, MemoryPersistence()).apply { // Paho client — como `mqtt.connect(brokerUri, {clientId: id})` en JS
                setCallback(object : MqttCallbackExtended { // callback — como `client.on('message', ...)` en MQTT.js pero con interfaz
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) { // al conectar/reconectar — como `client.on('connect', ...)`
                        try {
                            topics.forEach { t -> subscribe(t, 0) } // subscribe QoS 0 LAN <50ms prd.md:50 — como `client.subscribe('aethernet/#', {qos:0})`
                        } catch (_: Exception) {}
                        _connectionState.value = MqttConnectionState.Connected(brokerUri) // UI "MQTT ● tcp://..." (DashboardScreen:77)
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) { // mensaje llegó — como `client.on('message', (topic, payload) => ...)`
                        parse(topic, String(message.payload)) // parse JSON y emit a SharedFlow (ver parse:131)
                    }

                    override fun connectionLost(cause: Throwable?) { // conexión caída — como `client.on('close', ...)` en MQTT.js
                        _connectionState.value = MqttConnectionState.Error(cause?.message ?: "lost") // UI "MQTT ✕ lost" — DashboardViewModel volverá a polling 5s
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {} // entrega QoS 1/2 completa — no usado (QoS 0)
                })
            }
            val opts = MqttConnectOptions().apply { // opciones — como { clean: true, keepalive: 15, reconnectPeriod: 1000 } en MQTT.js
                isAutomaticReconnect = true // auto-reconnect — como reconnectPeriod en MQTT.js (MOV-09 pulirá backoff)
                isCleanSession = true // clean session — como `clean: true` en MQTT.js (no persiste suscripciones)
                connectionTimeout = 5 // 5s timeout — como `connectTimeout: 5000` en MQTT.js
                keepAliveInterval = 15 // 15s keepalive — como `keepalive: 15` en MQTT.js (ping cada 15s)
            }
            client!!.connect(opts) // bloquea hasta conectar — por eso withContext(Dispatchers.IO) (como `await client.connectAsync()` en MQTT.js)
            // connectComplete se disparará; por si Paho no invoca extended (bug versiones viejas), forza:
            if (client?.isConnected == true && _connectionState.value is MqttConnectionState.Connecting) {
                _connectionState.value = MqttConnectionState.Connected(brokerUri)
                try { topics.forEach { t -> client?.subscribe(t, 0) } } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            _connectionState.value = MqttConnectionState.Error(e.message ?: "connect fail") // UI "MQTT ✕ ..." — fallback a HTTP polling 5s
        }
    }

    private fun parse(topic: String, payload: String) {
        // Parsea JSON por topic — como `switch(topic) { case 'aethernet/rover/telemetry': JSON.parse(payload) }` en JS
        try {
            when {
                topic.startsWith("aethernet/rover/telemetry") -> { // Rover → App
                    val obj = jsonLenient.decodeFromString<RoverTelemetryMqtt>(payload) // parse — como JSON.parse con Zod
                    _roverTelemetry.tryEmit(obj) // emit a SharedFlow — como `emitter.emit('rover', obj)` en Node
                }
                topic.startsWith("aethernet/access") -> { // MEGA → App
                    val obj = jsonLenient.decodeFromString<AccessEventMqtt>(payload)
                    _accessEvents.tryEmit(obj) // PinViewModel colecta para lastResultSuccess (PinViewModel:40)
                }
                topic.startsWith("aethernet/seguridad") -> { // láser → App
                    val obj = jsonLenient.decodeFromString<SecurityEventMqtt>(payload)
                    _securityEvents.tryEmit(obj) // DashboardViewModel colecta para LED rojo intrusión 10s
                }
                topic.startsWith("aethernet/system") -> {
                    // status heartbeat — no emite, solo mantiene conexión viva (como ping en WebSocket)
                }
            }
        } catch (_: Exception) {
            // payload malformado no rompe app — como try/catch en JSON.parse (ignora mensaje corrupto, no crashea)
        }
    }

    /**
     * MOV-04 — Publica comando de acceso/cerrojo (HU-01, RF-2.2 S).
     * Payload idéntico al esperado por gateway-esp32.ino:70/332 handleAccessCommand:
     *   {"pin":"1234"} -> Gateway subscribe aethernet/access/command -> publish CMD:ACCESS -> MEGA processPinAttempt
     * QoS 0 LAN <50ms (prd.md:50) — no retenido (como publish con {qos:0, retain:false} en MQTT.js).
     * Retorna Result para ViewModel feedback sin exponer excepciones Paho (como safeCall en util/Result.kt).
     */
    suspend fun publishAccessCommand(pin: String): com.aethernet.aethercontrol.util.Result<Unit> =
        withContext(Dispatchers.IO) { // IO thread — no bloquea UI (como worker)
            try {
                val c = client
                if (c == null || !c.isConnected) {
                    return@withContext com.aethernet.aethercontrol.util.Result.Error("MQTT no conectado — verifica 1883/9001 y broker ${getBrokerHostFromHttpUrl("http://temp")}")
                }
                // Validación mínima: firmware espera pin 4..6 dígitos (config.h:49 PIN_MAX_LEN, PinValidator.kt:21)
                if (pin.length !in 4..6 || !pin.all { it.isDigit() }) {
                    return@withContext com.aethernet.aethercontrol.util.Result.Error("PIN inválido (4-6 dígitos)")
                }
                val payload = """{"pin":"$pin"}""" // JSON — como `JSON.stringify({pin})` en JS
                val msg = MqttMessage(payload.toByteArray()).apply { qos = 0; isRetained = false } // QoS 0, no retain — como {qos:0, retain:false} en MQTT.js
                c.publish("aethernet/access/command", msg) // publish — como `client.publish('aethernet/access/command', payload)` en MQTT.js
                com.aethernet.aethercontrol.util.Result.Success(Unit) // éxito — como {ok:true} en TS
            } catch (e: Exception) {
                com.aethernet.aethercontrol.util.Result.Error(e.message ?: "publish fail", e) // error — como {ok:false, error:e.message}
            }
        }

    fun disconnect() {
        // Cierra cliente Paho — como `client.end()` en MQTT.js
        try {
            client?.disconnect() // disconnect — como `client.end()` en MQTT.js
            client?.close() // close — libera recursos (como `client.removeAllListeners()` en JS)
        } catch (_: Exception) {}
        client = null
        _connectionState.value = MqttConnectionState.Disconnected // UI "MQTT - Desconectado" — DashboardViewModel volverá a polling
    }
}
