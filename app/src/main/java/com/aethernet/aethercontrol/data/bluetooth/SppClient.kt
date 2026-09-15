package com.aethernet.aethercontrol.data.bluetooth

// =============================================================================
// SppClient.kt — Cliente Bluetooth Classic SPP | 6º Semestre UTP | MOV-07 RF-1.3
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años electrónica (HC-06), 2 años JS/React (Socket/Client pattern),
//              2 años Android/Kotlin
// Analogía React: este class es como un WebSocket/Serial client con StateFlow de conexión.
// FOSS: BluetoothAdapter + BluetoothSocket (Android SDK API estándar, Apache 2.0) — RNF-3.1.
// Origen: MOV-07 (Sprint 3, RF-1.3) — Fallback Bluetooth SPP para nodo de acceso (HC-06) si cae Wi-Fi.
// =============================================================================

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log // MOV-07 debug — filtrar con `adb logcat -s AetherBT`
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

sealed class BluetoothConnectionState {
    object Disconnected : BluetoothConnectionState()
    object Connecting : BluetoothConnectionState()
    data class Connected(val deviceName: String, val deviceAddress: String) : BluetoothConnectionState()
    data class Error(val msg: String) : BluetoothConnectionState()
}

class SppClient(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>()
    val incomingMessages: SharedFlow<String> = _incomingMessages

    private var socket: BluetoothSocket? = null
    private var readerThread: Thread? = null

    companion object {
        const val TAG = "AetherBT" // tag único MOV-07 — `adb logcat -s AetherBT`
        // Standard SPP UUID for Bluetooth Classic (HC-06, etc.)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: return@withContext Result.Error("Bluetooth no soportado en este dispositivo")
        if (!adapter.isEnabled) {
            return@withContext Result.Error("Bluetooth está desactivado")
        }

        _connectionState.value = BluetoothConnectionState.Connecting

        try {
            val device: BluetoothDevice? = if (deviceAddress != null) {
                adapter.getRemoteDevice(deviceAddress)
            } else {
                val bonded = adapter.bondedDevices
                bonded?.firstOrNull { it.name?.contains("HC-06", ignoreCase = true) == true }
                    ?: bonded?.firstOrNull()
            }

            if (device == null) {
                _connectionState.value = BluetoothConnectionState.Error("No se encontró dispositivo HC-06 emparejado")
                return@withContext Result.Error("No se encontró HC-06 emparejado. Empareja el HC-06 en Ajustes Bluetooth.")
            }

            try { socket?.close() } catch (_: Exception) {}

            val tmpSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            try { adapter.cancelDiscovery() } catch (_: Exception) {}

            tmpSocket.connect()
            socket = tmpSocket

            val name = try { device.name ?: "HC-06" } catch (_: Exception) { "HC-06" }
            val address = device.address ?: ""

            _connectionState.value = BluetoothConnectionState.Connected(name, address)
            Log.d(TAG, "connected: name=$name address=$address") // MOV-07: confirma A QUÉ HC-06 se conectó (hay 3 en lab)
            startReading(tmpSocket)

            Result.Success(Unit)
        } catch (e: Exception) {
            _connectionState.value = BluetoothConnectionState.Error(e.message ?: "Error de conexión Bluetooth")
            Result.Error("Fallo conexión Bluetooth: ${e.message}", e)
        }
    }

    fun disconnect() {
        try {
            readerThread?.interrupt()
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        _connectionState.value = BluetoothConnectionState.Disconnected
    }

    suspend fun sendCommand(command: String): Result<Unit> = withContext(Dispatchers.IO) {
        val s = socket
        if (s == null || !s.isConnected) {
            return@withContext Result.Error("Bluetooth no conectado")
        }
        try {
            val writer = s.outputStream
            val payload = if (command.endsWith("\n")) command else "$command\n"
            writer.write(payload.toByteArray(Charsets.UTF_8))
            writer.flush()
            Log.d(TAG, "TX: $command") // MOV-07: comando enviado por SPP
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Error enviando por Bluetooth: ${e.message}", e)
        }
    }

    suspend fun sendAccessCommand(pin: String): Result<Unit> {
        if (pin.length !in 4..6 || !pin.all { it.isDigit() }) {
            return Result.Error("PIN inválido (4-6 dígitos)")
        }
        val cmd = "CMD:ACCESS:{\"pin\":\"$pin\"}"
        return sendCommand(cmd)
    }

    private fun startReading(sock: BluetoothSocket) {
        readerThread = Thread({
            try {
                val reader = BufferedReader(InputStreamReader(sock.inputStream, Charsets.UTF_8))
                while (!Thread.currentThread().isInterrupted) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        kotlinx.coroutines.runBlocking {
                            Log.d(TAG, "RX: $line") // MOV-07: respuesta cruda del HC-06 antes del flow
                            _incomingMessages.emit(line)
                        }
                    }
                }
            } catch (_: IOException) {
                _connectionState.value = BluetoothConnectionState.Disconnected
            }
        }, "BluetoothReaderThread")
        readerThread?.start()
    }
}
