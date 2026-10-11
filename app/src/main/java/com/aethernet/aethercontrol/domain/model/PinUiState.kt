package com.aethernet.aethercontrol.domain.model

// =============================================================================
// PinUiState.kt — Estado UI PIN Cerrojo | 6º Semestre UTP | MOV-04 HU-01, RF-2.2
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (useState form), 2 años electrónica (keypad 4x4),
//              1 año C (PIN buffer), 1 año PostgreSQL (pin_hash), 2 años Python
// Analogía React: este data class es como `type PinFormState = { pinInput: string, isLoading: boolean, error?: string, ... }`
// en TS — estado de un formulario PIN con 7 campos (como useState en React con object).
// Analogía Python: como `PinUiState` dataclass con defaults — aquí data class Kotlin.
// Analogía C: como `char pinInput[7]` + flags en keypad_control.cpp:12 inputBuffer (String) — aquí con validación.
// Analogía PostgreSQL: pinInput nunca se guarda en DB, solo pin_hash (como password en auth) — aquí solo RAM.
// FOSS: data class pura Kotlin (RNF-3.1).
// Origen: MOV-04 HU-01, RF-2.2 S — envía comando cerrojo via MQTT aethernet/access/command {"pin":"1234"}
// (gateway-esp32.ino:70) y observa feedback via MOV-02 LedUiState + MOV-03 accessEventFlow.
// Flujo: PinScreen Grid 4x3 → PinViewModel.onPinDigit() → pinInput → sendPin() → MqttManager.publishAccessCommand
// → Gateway handleAccessCommand → MEGA processPinAttempt → LED verde 5s → accessEventFlow → lastResultSuccess.
// No persiste PIN — pinInput vive solo en RAM, se limpia tras envío (como inputBuffer = "" en keypad_control.cpp:48).
// =============================================================================

/**
 * MOV-04 — Estado UI para módulo PIN/cerrojo (HU-01, RF-2.2).
 * Fuente: firmware/mega-access/src/keypad_control.cpp:44 processPinAttempt -> MEGA gateway -> backend/app/routers/events.py
 * Cliente app envía comando via MQTT aethernet/access/command {"pin":"1234"} (gateway-esp32.ino:70)
 * y observa feedback via MOV-02 LedUiState (verde 5s) + MOV-03 accessEventFlow (confirmación).
 *
 * No persiste PIN — pinInput vive solo en RAM, se limpia tras envío (seguridad, como inputBuffer en C).
 */
data class PinUiState(
    val pinInput: String = "", // dígitos acumulados — como inputBuffer en C keypad_control.cpp:12, pero en Compose StateFlow
    val isLoading: Boolean = false, // true mientras sendAccessCommand en vuelo — como isLoading en React Query (muestra CircularProgressIndicator en PinScreen:136)
    val isValid: Boolean = false, // true si pinInput 4..6 dígitos — como PinValidator.isValidFormat(pin) (ver PinValidator.kt:21)
    val error: String? = null, // mensaje error validación/throttle/MQTT — nullable, se pinta rojo en PinScreen:81
    val lastResultSuccess: Boolean? = null, // null = sin envío aún, true/false tras accessEventFlow (como lastResult en React)
    val lastMessage: String? = null, // "✓ Desbloqueado" / "✕ Denegado" / "Enviado, esperando confirmación…" — feedback UI (PinScreen:83)
    val attemptsInWindow: Int = 0 // para throttling UI (max 5 / 60s en PinViewModel:114) — como rate limit en Express
)
