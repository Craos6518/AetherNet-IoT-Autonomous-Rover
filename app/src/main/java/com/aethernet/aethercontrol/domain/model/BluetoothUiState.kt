package com.aethernet.aethercontrol.domain.model

// =============================================================================
// BluetoothUiState.kt — Estado UI fallback Bluetooth SPP | 6º Semestre UTP | MOV-07 RF-1.3
// Autor: Andres Felipe Martinez Henao
// Analogía React: esta data class es como `type BluetoothScreenState = { devices: [], connection, presence, pinInput, ... }`
// en TS — estado del Nodo Acceso Compacto (contingencia BT, RF-1.3).
// Analogía C: espejo del estado del firmware nano-access-compacto (connection/pin/presence).
// FOSS: data class Kotlin pura (RNF-3.1).
// Origen: MOV-07 RF-1.3 — pantalla "Bluetooth Fallback" (docs/backlog.md:24).
// Flujo: BluetoothScreen lista emparejados -> connect(address) (BluetoothManager) ->
// sendPin() -> CMD:ACCESS:{"pin":"1234"} -> NANO valida -> ACCESS:{success} -> lastResultSuccess.
// Presencia: FC-51 push + POLL cada 1s (requestPresence) -> presence Boolean?.
// =============================================================================

import com.aethernet.aethercontrol.data.bluetooth.BluetoothDeviceInfo
import com.aethernet.aethercontrol.data.bluetooth.BtConnectionState

/**
 * MOV-07 — Estado UI para el fallback Bluetooth SPP (RF-1.3, Nodo Acceso Compacto).
 * Misma forma que PinUiState (pinInput/isValid/lastResult...) pero con BT en vez de MQTT.
 * presence: null = aún sin dato del FC-51; true/false cuando el Nano reportó o respondió POLL.
 */
data class BluetoothUiState(
    val devices: List<BluetoothDeviceInfo> = emptyList(), // emparejados — lista de HC-06
    val connection: BtConnectionState = BtConnectionState.Disconnected, // estado socket SPP
    val presence: Boolean? = null, // true = objeto detectado (FC-51 activo LOW) — null = sin dato
    val pinInput: String = "",     // dígitos acumulados — como inputBuffer en C
    val isLoading: Boolean = false, // true mientras sendAccessCommand en vuelo
    val isValid: Boolean = false,  // true si pinInput 4..6 dígitos
    val error: String? = null,     // error permisos/connect/write — rojo en UI
    val lastResultSuccess: Boolean? = null, // true/false tras ACCESS:{success} del Nano
    val lastMessage: String? = null // "✓ Desbloqueado" / "✕ Denegado" / "Enviado…"
)