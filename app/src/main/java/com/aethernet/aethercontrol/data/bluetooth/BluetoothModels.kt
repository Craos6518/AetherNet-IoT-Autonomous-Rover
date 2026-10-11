package com.aethernet.aethercontrol.data.bluetooth

// =============================================================================
// BluetoothModels.kt — Modelos + parser fallback SPP | 6º Semestre UTP | MOV-07 RF-1.3
// Autor: Andres Felipe Martinez Henao
// Analogía React: estos data class son como `type BtMessage = { type: 'ACCESS', success: boolean }`
// — tipos TS del protocolo del Nodo Acceso Compacto (nano-access-compacto.ino).
// Analogía C: espejo del parse "TYPE:PAYLOAD" del firmware (misma convención
// que uart_protocol.cpp del MEGA y processMegaMessage del gateway — "ACCESS:"/"PRESENCE:").
// FOSS: data class + object puro Kotlin (RNF-3.1) — sin dependencias BT de plataforma.
// Origen: MOV-07 RF-1.3 — contingencia Bluetooth SPP cuando cae el Wi-Fi
// (docs/requirements.md:18, docs/backlog.md:24). Nodo: firmware/nano-access-compacto.
// =============================================================================

/** Información mínima de un dispositivo BT emparejado (HC-06) para la lista de la UI. */
data class BluetoothDeviceInfo(
    val name: String,     // nombre del módulo — "HC-06" o "HC-04" normalmente
    val address: String   // MAC — "XX:XX:XX:XX:XX:XX" para createRfcommSocketToServiceRecord
)

/** Resultado de validación PIN del Nodo Compacto — espejo ACCESS:{...} del firmware. */
data class BtAccessResult(
    val success: Boolean,          // true = PIN ok (Nano validó contra VALID_PIN 1234)
    val pinHash: String?,          // hash djb2 8 hex — "7c78c98f" (mismo esquema MEGA keypad_control.cpp:130)
    val source: String?            // "bluetooth" — el firmware reporta que vino por SPP
)

/** Estado de conexión Bluetooth SPP — como MqttConnectionState pero para BT (MqttModels.kt). */
sealed interface BtConnectionState {
    object Disconnected : BtConnectionState                  // sin socket — como `BT -`
    data class Connecting(val deviceName: String) : BtConnectionState // conectando SPP
    data class Connected(val deviceName: String) : BtConnectionState // socket abierto + reader activo
    data class Error(val msg: String) : BtConnectionState    // fallo socket/rfcomm — como `BT ✕`
}

/** Mensaje parseado de una línea del Nodo Compacto — sealed interface (como union type TS). */
sealed interface BtLineMessage {
    data class Access(val result: BtAccessResult) : BtLineMessage  // "ACCESS:{json}"
    data class Presence(val presence: Boolean) : BtLineMessage     // "PRESENCE:{json}"
}

/**
 * Parser de líneas del Nodo Compacto — puro, sin Android (por eso es testeable con JUnit).
 * Formato: `TYPE:{json}` donde TYPE ∈ {ACCESS, PRESENCE}.
 * Espejo del protocolo del firmware: nano-access-compacto.ino "TYPE:PAYLOAD" y del
 * gateway processMegaMessage (gateway-esp32.ino:403) — misma convención de 2 campos.
 * Si viene basura o tipo desconocido devuelve null (el reader lo ignora, no crashea).
 */
object BtPayloadParser {

    fun parseLine(line: String): BtLineMessage? {
        val trimmed = line.trim() // quita \r\n — como `.trim()` en Kotlin
        if (trimmed.isEmpty()) return null
        val idx = trimmed.indexOf(':') // separador TYPE:PAYLOAD — como split(':')[0] en JS
        if (idx <= 0) return null
        val type = trimmed.substring(0, idx)
        val payload = trimmed.substring(idx + 1).trim() // el JSON
        return when (type) {
            "ACCESS" -> parseAccess(payload)
            "PRESENCE" -> parsePresence(payload)
            else -> null // ruido de otro nodo BT — ignorar
        }
    }

    private fun parseAccess(payload: String): BtLineMessage.Access? {
        // {"user_id":"hc06_user","pin_hash":"7c78c98f","success":true,"source":"bluetooth"}
        val success = extractJsonValue("success", payload) == "true"
        val pinHash = extractJsonValue("pin_hash", payload).ifEmpty { null }
        val source = extractJsonValue("source", payload).ifEmpty { null }
        return BtLineMessage.Access(BtAccessResult(success, pinHash, source))
    }

    private fun parsePresence(payload: String): BtLineMessage.Presence? {
        // {"presence":true} — FC-51 activo LOW (LOW = objeto detectado), firmware invierte
        val v = extractJsonValue("presence", payload)
        return if (v == "true" || v == "false") BtLineMessage.Presence(v == "true") else null
    }

    /**
     * Extrae el valor de "key" de un mini-JSON plano (sin ArduinoJson en el cliente).
     * Soporta `"key":"texto"`, `"key":true`, `"key":123` y espacios intermedios,
     * igual que la versión del firmware Nano (nano-access-compacto.ino extractPin).
     * Como `.match(/\"key\"\s*:\s*(\"[^\"]*\"|\w+)/)` en JS regex pero manual.
     */
    private fun extractJsonValue(key: String, json: String): String {
        val k = "\"$key\""
        val i = json.indexOf(k)
        if (i < 0) return ""
        val colon = json.indexOf(':', i)
        if (colon < 0) return ""
        var j = colon + 1
        while (j < json.length && json[j] == ' ') j++ // skip espacios — whitespace tolerant
        if (j >= json.length) return ""
        if (json[j] == '"') { // valor string — cierra en la siguiente comilla
            val end = json.indexOf('"', j + 1)
            return if (end < 0) "" else json.substring(j + 1, end)
        }
        // valor sin comillas (booleano/número) — hasta ',' o '}'
        var end = j
        while (end < json.length && json[end] != ',' && json[end] != '}') end++
        return json.substring(j, end).trim()
    }
}