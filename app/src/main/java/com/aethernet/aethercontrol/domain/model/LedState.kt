package com.aethernet.aethercontrol.domain.model

/**
 * MOV-02 — Pantallas LED local solo lectura (RF-1.1, HU-01/HU-02).
 * RF: docs/requirements.md:16 RF-1.1, 57 HU-01, 69 HU-02
 * HW: docs/hardware-inventory.md:9 LED RGB local MEGA pines 44/45/46 ánodo común
 *     firmware/mega-access/src/config.h:30 LED_COMMON_ANODE, :52 DOOR_AUTO_LOCK_MS=5000, :62 LED_RED_FAIL_MS=1000
 *     firmware/mega-access/src/led.cpp:64 isDoorUnlocked() espejo
 * ADR: docs/adr/adr-001-cancelacion-tuya.md:19 — bombillo Tuya CANCELADO, solo LED RGB local.
 *
 * Solo lectura: no envía comandos, solo deriva estado del último evento.
 * Fuente de verdad: backend/app/routers/events.py /api/access-events + /api/security-events + modelos.py:20 success + :55 event_type
 * Polling 5s alineado a STATUS_INTERVAL_MS=5000 config.h:55 — deja interfaz lista para MOV-03 MQTT sin romper UI.
 */
enum class LedColor {
    GREEN,
    RED,
    OFF,
    UNKNOWN
}

enum class LedState {
    OFF,
    GREEN_UNLOCKED,
    RED_INTRUSION,
    RED_FAIL,
    UNKNOWN
}

/**
 * Estado UI del LED local — consumido por DashboardScreen / LedStatusCard.
 * - color: mapeo visual directo (Color 0xFF4CAF50 / 0xFFF44336 / 0xFF9E9E9E / UNKNOWN)
 * - label: "Verde desbloqueado" / "Rojo intrusión" / "Apagado" / "Desconectado" / "Desconocido"
 * - lastEventAt: epoch millis del evento origen (para "hace Xs" relativo)
 * - source: "access" | "security" | null
 * - isLoading / error: para propagar Result.Error sin crashear (banner Desconectado)
 */
data class LedUiState(
    val color: LedColor = LedColor.UNKNOWN,
    val state: LedState = LedState.UNKNOWN,
    val label: String = "Desconocido",
    val lastEventAt: Long? = null,
    val source: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
