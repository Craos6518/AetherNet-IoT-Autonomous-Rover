package com.aethernet.aethercontrol.data.bluetooth

// =============================================================================
// BluetoothManager.kt — Cliente Bluetooth SPP (fallback RF-1.3) | 6º Semestre UTP | MOV-07
// Autor: Andres Felipe Martinez Henao
// Analogía React: este class es como `class SocketClient { connect(url) { this.sock = new WebSocket(url) } }`
// — aquí con BluetoothAdapter + BluetoothSocket (RFCOMM/SPP) para el Nodo Acceso Compacto.
// Analogía Python: como `socket.connect(('address', 1)); socket.sendall(b'...')` en pyserial+BT.
// Analogía C: espejo del lado app del protocolo nano-access-compacto.ino — el firmware escucha
// `CMD:ACCESS:{"pin":"1234"}` y responde `ACCESS:{...}` / `PRESENCE:{...}` (lo que parseamos).
// FOSS: Android Bluetooth API (Apache 2.0) — RNF-3.1, sin SDK BT propietario.
// Origen: MOV-07 RF-1.3 — "conexión de contingencia vía Bluetooth SPP si cae el Wi-Fi"
// (docs/requirements.md:18, docs/backlog.md:24). Nodo: hardware-inventory.md:12.
// =============================================================================

import android.annotation.SuppressLint // lint BT_FRAUD ojo — lecturas permitidas
import android.bluetooth.BluetoothAdapter // getDefaultAdapter() — como `navigator.bluetooth` en Web BT
import android.bluetooth.BluetoothDevice // getBondedDevices — como el "paired list" de BT settings
import android.bluetooth.BluetoothSocket // RFCOMM socket — SPP UUID 00001101-...-00805F9B34FB
import android.content.Context // contexto de app
import com.aethernet.aethercontrol.util.Result // Result — como {ok, error} en TS
import java.io.BufferedReader // leer líneas del socket — como readline en pyserial
import java.io.InputStreamReader
import java.util.UUID // SPP UUID canónico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * MOV-07 — Gestor Bluetooth SPP (RF-1.3, fallback cuando MQTT/Wi-Fi no están).
 * Envuelve BluetoothAdapter + BluetoothSocket y expone StateFlow/SharedFlow igual que
 * MqttManager (misma arquitectura: data/bluetooth/BluetoothManager.kt ↔ data/mqtt/MqttManager.kt).
 *
 * Notas de seguridad/permisos:
 *  - Android 12+ (API 31+) exige permiso runtime BLUETOOTH_CONNECT para leer dispositivos
 *    emparejados y abrir socket — la UI lo pide con RequestPermission antes de listar
 *    (ver BluetoothScreen.kt). En API 24..30 basta BLUETOOTH (normal, se otorga sola).
 *  - `listPaired()` devuelve [] si no hay permiso (SecurityException capturada) — la UI
 *    muestra "Habilita Bluetooth" en vez de crashear (como un catch de `navigator.bluetooth`).
 *  - HC-06 es SPP clásico, NO BLE — por eso SPP_UUID y createRfcommSocketToServiceRecord.
 *
 * Protocolo con Nodo Compacto (nano-access-compacto.ino):
 *  sendAccessCommand("1234") -> "CMD:ACCESS:{\"pin\":\"1234\"}\n" -> Nano valida ->
 *  responde "ACCESS:{\"user_id\":\"hc06_user\",...}" (emit by accessEvents) y
 *  "PRESENCE:{\"presence\":true}" (emit by presence) + "POLL:PRESENCE" para sondeo.
 */
class BluetoothManager(private val appContext: Context) {

    companion object {
        // SPP UUID canónico (RFCOMM serial) — es lo que HC-06 usa en Android
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val MAX_PIN_LEN = 6 // espejo firmware PIN_MAX_LEN
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter() // null si sin BT

    // StateFlow conexión — como MqttManager._connectionState (MqttManager.kt:59)
    private val _connectionState = MutableStateFlow<BtConnectionState>(BtConnectionState.Disconnected)
    val connectionState: StateFlow<BtConnectionState> = _connectionState.asStateFlow()

    // SharedFlow resultados ACCESS: y push PRESENCE: — buffer para pérdida mínima en rotación
    private val _accessEvents = MutableSharedFlow<BtAccessResult>(extraBufferCapacity = 16)
    val accessEvents: SharedFlow<BtAccessResult> = _accessEvents.asSharedFlow()
    private val _presence = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    val presence: SharedFlow<Boolean> = _presence.asSharedFlow()

    private var socket: BluetoothSocket? = null // socket RFCOMM activo (null si desconectado)
    private var readerThread: Thread? = null    // thread lector del socket (como worker en JS)

    /** true si el dispositivo tiene Bluetooth (getDefaultAdapter() != null). */
    fun isSupported(): Boolean = adapter != null

    /**
     * Lista dispositivos BT emparejados (HC-06 ya pareado desde settings del teléfono).
     * Requiere BLUETOOTH_CONNECT (API 31+) — la UI lo pide antes de llamar. Sin permiso
     * devuelve [] (no crashea): la pantalla muestra el botón "Habilitar Bluetooth".
     * @SuppressLint: no usamos los nuevos listener APIs, solo getBondedDevices (lectura).
     */
    @SuppressLint("MissingPermission")
    fun listPaired(): List<BluetoothDeviceInfo> {
        val a = adapter ?: return emptyList() // sin BT
        return try {
            a.bondedDevices.map { device -> // bondedDevices — como `pairedDevices` en BT settings
                BluetoothDeviceInfo(device.name ?: device.address, device.address)
            }.sortedBy { it.name } // orden alfabético — como `.sort()` en JS
        } catch (_: SecurityException) {
            emptyList() // sin permiso runtime — UI invita a pedirlo
        }
    }

    /**
     * Conecta por SPP al dispositivo emparejado (HC-06). Bloquea en RfcommChannel hasta
     * conectar (por eso withContext(Dispatchers.IO)). Tras conectar arranca el thread lector
     * que parsea ACCESS/PRESENCE y emite a los SharedFlow (como WebSocket onmessage en JS).
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (socket?.isConnected == true) return@withContext Result.Success(Unit) // ya conectado
            disconnect() // limpia restos — como `socket.close()` antes de reconnect en JS
            val device: BluetoothDevice = adapter!!.getRemoteDevice(address) // MAC válida
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID) // socket RFCOMM
            _connectionState.value = BtConnectionState.Connecting(device.name ?: address)
            s.connect() // bloquea hasta SPP ok — como `await fetch()` en JS (IO thread)
            socket = s
            startReader(s, device.name ?: address) // reader de líneas ACCESS/PRESENCE
            _connectionState.value = BtConnectionState.Connected(device.name ?: address)
            Result.Success(Unit)
        } catch (e: SecurityException) {
            _connectionState.value = BtConnectionState.Error("Permiso Bluetooth denegado")
            Result.Error("Permiso Bluetooth denegado", e)
        } catch (e: Exception) {
            disconnect() // limpia socket a medias PRIMERO — disconnect() pone Disconnected
            _connectionState.value = BtConnectionState.Error(e.message ?: "connect fail") // Error DESPUÉS
            Result.Error(e.message ?: "connect fail", e)
        }
    }

    /** Lector de socket en thread dedicado (como worker) — no bloquea Main/IO de llamadas. */
    private fun startReader(s: BluetoothSocket, deviceName: String) {
        socket = s
        readerThread = Thread {
            try {
                val reader = BufferedReader(InputStreamReader(s.inputStream)) // readline BT
                while (!s.isConnected && !Thread.currentThread().isInterrupted) {
                    // espera a que se abra inputStream (primera iteración)
                }
                while (s.isConnected) {
                    val raw = reader.readLine() ?: break // EOF — se cerró (Nano apagado/BT off)
                    val msg = BtPayloadParser.parseLine(raw) // parsea TYPE:PAYLOAD (BluetoothModels.kt)
                    when (msg) {
                        is BtLineMessage.Access -> _accessEvents.tryEmit(msg.result) // ACCESS: emit
                        is BtLineMessage.Presence -> _presence.tryEmit(msg.presence) // PRESENCE: emit
                        null -> Unit // línea desconocida — ignora (no crashea)
                    }
                }
            } catch (_: Exception) {
                // socket murió — desconexión limpia (Nano fuera de rango / BT apagado)
            } finally {
                if (socket === s) { // solo si este mismo socket sigue activo — evita pisar reconexiones
                    try { s.close() } catch (_: Exception) {}
                    _connectionState.value = BtConnectionState.Disconnected
                }
            }
        }.apply { isDaemon = true; start() }
    }

    /**
     * Envía comando de acceso al Nodo Compacto por BT (RF-1.3): "CMD:ACCESS:{\"pin\":\"1234\"}".
     * Espejo de publishAccessCommand de MqttManager pero por SPP (MqttManager.kt:188).
     * Valida 4..6 dígitos igual que el firmware — no envía PIN inválido por la radio.
     */
    suspend fun sendAccessCommand(pin: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (pin.length !in 4..MAX_PIN_LEN || !pin.all { it.isDigit() }) {
            return@withContext Result.Error("PIN inválido (4-6 dígitos)")
        }
        val s = socket
        if (s == null || !s.isConnected) {
            return@withContext Result.Error("Bluetooth no conectado — conecta al HC-06")
        }
        return@withContext try {
            val payload = """CMD:ACCESS:{"pin":"$pin"}""" // protocolo Nano (hex escapes ok en Kotlin)
            s.outputStream.write((payload + "\n").toByteArray(Charsets.UTF_8)) // línea a BT
            s.outputStream.flush()
            Result.Success(Unit) // ack de validación llega por accessEvents (ACCESS:{...})
        } catch (e: Exception) {
            Result.Error(e.message ?: "BT write fail", e)
        }
    }

    /** Sondea presencia FC-51: "POLL:PRESENCE" -> Nano responde PRESENCE:{...} (pushed a UI). */
    suspend fun requestPresence(): Result<Unit> = withContext(Dispatchers.IO) {
        val s = socket
        if (s == null || !s.isConnected) return@withContext Result.Error("BT no conectado")
        return@withContext try {
            s.outputStream.write("POLL:PRESENCE\n".toByteArray(Charsets.UTF_8))
            s.outputStream.flush()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "BT write fail", e)
        }
    }

    /** Cierra socket + reader — como `client.end()` en MQTT.js (MqttManager.disconnect). */
    fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        readerThread?.interrupt() // despierta reader — como `worker.terminate()` en JS
        readerThread = null
        socket = null
        _connectionState.value = BtConnectionState.Disconnected
    }
}