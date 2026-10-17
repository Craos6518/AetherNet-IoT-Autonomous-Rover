package com.aethernet.aethercontrol.domain.model

// =============================================================================
// PinTransport.kt — Canal auto de envío PIN | 6º Semestre UTP | HU-01 RF-1.3/RF-2.2
// Autor: Andres Felipe Martinez Henao
// Analogía React: este enum es como `type PinTransport = 'bluetooth' | 'mqtt' | 'none'`
// en TS — el canal que PinViewModel elige solo según qué esté conectado.
// FOSS: enum puro Kotlin (RNF-3.1).
// Origen: HU-01 + RF-1.3 (contingencia BT HC-06) + RF-2.2 (cerrojo MEGA).
// Regla: BT Connected > MQTT Connected > NONE. BT gana porque funciona sin Wi-Fi
// (plan B local SPP); MQTT es el camino normal vía Gateway.
// =============================================================================

/**
 * Canal de envío del PIN elegido automáticamente por PinViewModel.resolveTransport().
 * BLUETOOTH = HC-06 SPP conectado (BluetoothManager). MQTT = broker conectado.
 * NONE = sin canal (la UI muestra hint para conectar uno de los dos).
 */
enum class PinTransport {
    BLUETOOTH, // HC-06 SPP conectado — sendAccessCommand por BT (nano-access-compacto.ino)
    MQTT, // broker conectado — sendAccessCommand por MQTT (gateway-esp32.ino:332)
    NONE // sin canal — ni BT ni MQTT conectados
}
