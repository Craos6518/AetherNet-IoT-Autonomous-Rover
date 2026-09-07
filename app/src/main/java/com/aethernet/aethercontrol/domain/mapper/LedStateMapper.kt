package com.aethernet.aethercontrol.domain.mapper

import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.domain.model.LedColor
import com.aethernet.aethercontrol.domain.model.LedState
import com.aethernet.aethercontrol.domain.model.LedUiState
import java.time.Instant

/**
 * Mapper determinista LED local — MOV-02 (RF-1.1, HU-01/HU-02).
 * Espejo firmware/mega-access/src/led.cpp:64 + config.h:52 DOOR_AUTO_LOCK_MS=5000
 * y :62 LED_RED_FAIL_MS=1000.
 *
 * Fuente de verdad: último evento entre AccessEvent y SecurityEvent (no existe /api/led).
 * backend/app/models.py:20 AccessEvent.success + :55 SecurityEvent.event_type + :61 timestamp
 * backend/app/routers/events.py:31 / 92 / 105
 *
 * Ventanas:
 * - GREEN_UNLOCKED: AccessEvent success=true && now-ts < 5000ms (HU-01 verde 5s, throttling STATUS_INTERVAL_MS)
 * - RED_INTRUSION: SecurityEvent event_type=="intrusion" && now-ts < 10000ms (HU-02 rojo; Node-RED Telegram no bloquea)
 * - RED_FAIL: último access success=false && now-ts < 1000ms (informativo, no bloquea UI)
 * - OFF: resto (puerta bloqueada, sin evento reciente)
 * - UNKNOWN: sin datos (colecciones vacías o backend vacío)
 *
 * Ánodo común led.cpp:22 (255-valor) NO aplica aquí: app solo refleja, no escribe PWM.
 * Solo lectura — no POST, no MQTT (MOV-03 migra a Mosquitto sin romper UI).
 *
 * Timestamps: ISO-8601 con timezone (server_default=func.now()). Parse con Instant.parse.
 * Si parse falla, se trata timestamp como 0 (UNKNOWN).
 * Cualquier mención a Tuya debe quedar marcada CANCELADO — ADR-001 §Verificación.
 */
object LedStateMapper {

    const val GREEN_WINDOW_MS = 5_000L      // config.h:52 DOOR_AUTO_LOCK_MS
    const val RED_INTRUSION_WINDOW_MS = 10_000L // HU-02 rojo visible más que verde (10s informativo)
    const val RED_FAIL_WINDOW_MS = 1_000L   // config.h:62 LED_RED_FAIL_MS

    fun map(
        accessEvents: List<AccessEventOut>,
        securityEvents: List<SecurityEventOut>,
        nowMs: Long = System.currentTimeMillis()
    ): LedUiState {
        val latestAccess = accessEvents.maxByOrNull { parseTs(it.timestamp) }
        val latestSecurity = securityEvents.maxByOrNull { parseTs(it.timestamp) }

        // Sin ningún evento -> Desconocido (backend vacío)
        if (latestAccess == null && latestSecurity == null) {
            return LedUiState(
                color = LedColor.UNKNOWN,
                state = LedState.UNKNOWN,
                label = "Desconocido",
                lastEventAt = null,
                source = null
            )
        }

        val accessTs = latestAccess?.let { parseTs(it.timestamp) } ?: 0L
        val securityTs = latestSecurity?.let { parseTs(it.timestamp) } ?: 0L

        // Determinar cuál es el último evento global (tie -> prioriza security intrusión por criticidad)
        val securityIsLatest = latestSecurity != null && securityTs >= accessTs
        val accessIsLatest = latestAccess != null && accessTs > securityTs

        // 1) Si el último es intrusión y está dentro de ventana -> ROJO intrusión (HU-02)
        if (securityIsLatest && latestSecurity != null) {
            val isIntrusion = latestSecurity.event_type.equals("intrusion", ignoreCase = true)
            val age = nowMs - securityTs
            if (isIntrusion && age in 0 until RED_INTRUSION_WINDOW_MS) {
                return LedUiState(
                    color = LedColor.RED,
                    state = LedState.RED_INTRUSION,
                    label = "Rojo intrusión",
                    lastEventAt = securityTs,
                    source = "security"
                )
            }
            // Intrusión expirada o no-intrusión -> caer a evaluación access/off
        }

        // 2) Si el último es access success -> GREEN 5s
        if (accessIsLatest && latestAccess != null) {
            val age = nowMs - accessTs
            if (latestAccess.success && age in 0 until GREEN_WINDOW_MS) {
                return LedUiState(
                    color = LedColor.GREEN,
                    state = LedState.GREEN_UNLOCKED,
                    label = "Verde desbloqueado",
                    lastEventAt = accessTs,
                    source = "access"
                )
            }
            if (!latestAccess.success && age in 0 until RED_FAIL_WINDOW_MS) {
                return LedUiState(
                    color = LedColor.RED,
                    state = LedState.RED_FAIL,
                    label = "Rojo fallo PIN",
                    lastEventAt = accessTs,
                    source = "access"
                )
            }
            // Access expirado -> OFF si no hay intrusión vigente
            // Antes de decidir OFF, verificar si hay intrusión vigente aunque no sea el último (intrusión puede persistir)
            if (latestSecurity != null) {
                val isIntrusion = latestSecurity.event_type.equals("intrusion", ignoreCase = true)
                val secAge = nowMs - securityTs
                if (isIntrusion && secAge in 0 until RED_INTRUSION_WINDOW_MS) {
                    return LedUiState(
                        color = LedColor.RED,
                        state = LedState.RED_INTRUSION,
                        label = "Rojo intrusión",
                        lastEventAt = securityTs,
                        source = "security"
                    )
                }
            }
            return LedUiState(
                color = LedColor.OFF,
                state = LedState.OFF,
                label = "Apagado",
                lastEventAt = accessTs,
                source = "access"
            )
        }

        // 3) Último es security no-intrusión expirado o access expirado -> OFF
        // Verificar si access vigente pero no era el último? (edge: timestamps muy cercanos)
        if (latestAccess != null) {
            val age = nowMs - accessTs
            if (latestAccess.success && age in 0 until GREEN_WINDOW_MS) {
                return LedUiState(
                    color = LedColor.GREEN,
                    state = LedState.GREEN_UNLOCKED,
                    label = "Verde desbloqueado",
                    lastEventAt = accessTs,
                    source = "access"
                )
            }
            if (!latestAccess.success && age in 0 until RED_FAIL_WINDOW_MS) {
                return LedUiState(
                    color = LedColor.RED,
                    state = LedState.RED_FAIL,
                    label = "Rojo fallo PIN",
                    lastEventAt = accessTs,
                    source = "access"
                )
            }
        }

        // Default OFF — hay eventos pero ninguno vigente
        val fallbackTs = maxOf(accessTs, securityTs).takeIf { it > 0 }
        val fallbackSource = if (securityTs >= accessTs && latestSecurity != null) "security" else "access"
        return LedUiState(
            color = LedColor.OFF,
            state = LedState.OFF,
            label = "Apagado",
            lastEventAt = fallbackTs,
            source = fallbackSource
        )
    }

    /**
     * Parse ISO-8601 con timezone. Backend usa DateTime(timezone=True) server_default=func.now().
     * Ejemplos: "2026-09-01T12:00:00.123456+00:00", "2026-09-01T12:00:00Z"
     * Tolerante: sin zona -> asume UTC.
     */
    fun parseTs(iso: String): Long {
        return try {
            // Normaliza: reemplaza espacio por T si viniera de Postgres sin T
            val normalized = iso.trim().replace(" ", "T")
            // Instant.parse soporta Z y offset con segundos; fallback para strings sin zona
            try {
                Instant.parse(normalized).toEpochMilli()
            } catch (_: Exception) {
                // Intenta parsear sin offset asumiendo UTC: "2026-09-01T12:00:00.123456"
                // Añade Z si no hay zona
                val withZone = if (normalized.contains("+") || normalized.endsWith("Z")) normalized else "${normalized}Z"
                Instant.parse(withZone).toEpochMilli()
            }
        } catch (_: Exception) {
            0L
        }
    }
}
