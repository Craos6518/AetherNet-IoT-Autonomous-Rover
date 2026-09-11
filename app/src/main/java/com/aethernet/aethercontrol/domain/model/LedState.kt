package com.aethernet.aethercontrol.domain.model

// =============================================================================
// LedState.kt — Modelos LED Local | 6º Semestre UTP | MOV-02 RF-1.1, HU-01/HU-02
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años electrónica/Arduino (LED RGB 44/45/46 ánodo común, config.h:30),
//              2 años JS/React (enum como union type), 1 año C (LedMode enum class),
//              1 año PostgreSQL (led derivado de access/security_events)
// Analogía React: estos enums son como `type LedColor = "GREEN" | "RED" | "OFF" | "UNKNOWN"`
// en TypeScript — tipan el estado visual del LED local para LedStatusCard.
// Analogía Python: como `LedMode` enum class en firmware/mega-access/src/led.h:15
// (LedMode::GREEN_UNLOCKED etc.) pero en Kotlin con sealed/data class.
// Analogía C: espejo de firmware/mega-access/src/led.h:15 LedMode : uint8_t
// (OFF, GREEN_UNLOCKED, RED_FAIL, BLUE_TAP, RED_CLEAR) — aquí simplificado a 5 estados UI.
// Analogía PostgreSQL: LedUiState deriva de backend/app/models.py:20 AccessEvent.success
// y :55 SecurityEvent.event_type + :61 timestamp — no existe /api/led, se calcula.
// FOSS: clase pura Kotlin, sin dependencia (RNF-3.1).
// Origen: RF-1.1, HU-01 (verde 5s), HU-02 (rojo intrusión), hardware-inventory.md:9 LED RGB local
// (Tuya cancelado ADR-001), config.h:30 LED_COMMON_ANODE, :52 DOOR_AUTO_LOCK_MS=5000, :62 LED_RED_FAIL_MS=1000.
// =============================================================================

/**
 * MOV-02 — Pantallas LED local solo lectura (RF-1.1, HU-01/HU-02).
 * RF: docs/requirements.md:16 RF-1.1, 57 HU-01, 69 HU-02
 * HW: docs/hardware-inventory.md:9 LED RGB local MEGA pines 44/45/46 ánodo común (255-valor en led.cpp:22)
 *     firmware/mega-access/src/config.h:30 LED_COMMON_ANODE, :52 DOOR_AUTO_LOCK_MS=5000, :62 LED_RED_FAIL_MS=1000
 *     firmware/mega-access/src/led.cpp:64 isDoorUnlocked() espejo (no bloqueante, millis())
 * ADR: docs/adr/adr-001-cancelacion-tuya.md:19 — bombillo Tuya CANCELADO, solo LED RGB local (RNF-3.1).
 *
 * Solo lectura: no envía comandos (solo deriva estado del último evento).
 * Fuente de verdad: backend/app/routers/events.py /api/access-events + /api/security-events + modelos.py:20 success + :55 event_type
 * Polling 5s alineado a STATUS_INTERVAL_MS=5000 config.h:55 — deja interfaz lista para MOV-03 MQTT sin romper UI.
 */
enum class LedColor {
    GREEN, // verde 0xFF4CAF50 — desbloqueado 5s (HU-01, como setLedMode(GREEN_UNLOCKED) en door.cpp:20)
    RED, // rojo 0xFFF44336 — intrusión 10s o fallo PIN 1s (como setLedMode(RED_FAIL) en keypad_control.cpp:75)
    OFF, // apagado 0xFF9E9E9E — bloqueado, sin evento reciente (como led.cpp:64 OFF)
    UNKNOWN // gris 0xFFBDBDBD — sin datos, backend vacío o desconectado (ver LedStateMapper:48)
}

enum class LedState {
    OFF, // apagado — puerta bloqueada (door.cpp:26 lockDoor)
    GREEN_UNLOCKED, // verde 5s — success=true y age<5000 (HU-01, LedStateMapper:35 GREEN_WINDOW_MS)
    RED_INTRUSION, // rojo intrusión 10s — event_type=="intrusion" y age<10000 (HU-02, LedStateMapper:36)
    RED_FAIL, // rojo fallo PIN 1s — success=false y age<1000 (LedStateMapper:37)
    UNKNOWN // desconocido — sin eventos (colecciones vacías)
}

/**
 * Estado UI del LED local — consumido por DashboardScreen / LedStatusCard.
 * - color: mapeo visual directo (Color 0xFF4CAF50 / 0xFFF44336 / 0xFF9E9E9E / UNKNOWN) — como CSS color en React
 * - state: enum para lógica (como LedMode en C led.h:15)
 * - label: "Verde desbloqueado" / "Rojo intrusión" / "Apagado" / "Desconocido" — texto UI (como label en React)
 * - lastEventAt: epoch millis del evento origen (para "hace Xs" relativo en LedStatusCard:63)
 * - source: "access" | "security" | null — origen evento (como source en MEGA uart_protocol.cpp:60)
 * - isLoading / error: para propagar Result.Error sin crashear (banner Desconectado en DashboardScreen:65)
 */
data class LedUiState(
    val color: LedColor = LedColor.UNKNOWN, // default UNKNOWN — hasta que llegue primer evento (como initial state en React)
    val state: LedState = LedState.UNKNOWN,
    val label: String = "Desconocido", // label UI — como `label` en React props
    val lastEventAt: Long? = null, // millis — para relative "hace 3s" en LedStatusCard:63 (como Date.now() - timestamp)
    val source: String? = null, // "access" o "security" — como event source en backend
    val isLoading: Boolean = false, // true si fetch LED en curso (como isLoading en React Query)
    val error: String? = null // mensaje error si getLedState falló (como error en Result.Error)
)
