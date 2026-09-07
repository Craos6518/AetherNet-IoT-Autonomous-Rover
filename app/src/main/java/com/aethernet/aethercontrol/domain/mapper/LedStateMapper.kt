package com.aethernet.aethercontrol.domain.mapper

// =============================================================================
// LedStateMapper.kt — Mapper LED Determinista | 6º Semestre UTP | MOV-02 RF-1.1, HU-01/HU-02
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años electrónica/Arduino (LED RGB 44/45/46, DOOR_AUTO_LOCK_MS 5000),
//              2 años JS/React (mapper como selector), 1 año C (led.cpp millis),
//              1 año PostgreSQL (access/security_events)
// Analogía React: este object es como `const mapLedState = (accessEvents, securityEvents) => LedUiState`
// en JS — función pura que deriva UI state de datos (como selector en Redux).
// Analogía Python: como `calculate_led_state(accessEvents, securityEvents, nowMs)` en Python — pura, testeable sin Android.
// Analogía C: espejo de firmware/mega-access/src/led.cpp:57 updateLed() con millis() — aquí con System.currentTimeMillis()
// y ventanas 5000/1000/10000 (como config.h:52 DOOR_AUTO_LOCK_MS y :62 LED_RED_FAIL_MS pero en Kotlin).
// Analogía PostgreSQL: lee backend/app/models.py:20 AccessEvent.success + :55 SecurityEvent.event_type + :61 timestamp
// y backend/app/routers/events.py:31 /api/access-events + /api/security-events — no existe /api/led, se calcula.
// FOSS: mapper puro Kotlin, sin dependencia (RNF-3.1), testeado con LedStateMapperTest.
// Espejo firmware: config.h:52 DOOR_AUTO_LOCK_MS=5000, :62 LED_RED_FAIL_MS=1000, led.cpp:64 isDoorUnlocked().
// =============================================================================

import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.domain.model.LedColor
import com.aethernet.aethercontrol.domain.model.LedState
import com.aethernet.aethercontrol.domain.model.LedUiState
import java.time.Instant // para parse ISO-8601 con TZ (como Date.parse en JS pero con zona)

/**
 * Mapper determinista LED local — MOV-02 (RF-1.1, HU-01/HU-02).
 * Espejo firmware/mega-access/src/led.cpp:64 + config.h:52 DOOR_AUTO_LOCK_MS=5000
 * y :62 LED_RED_FAIL_MS=1000.
 *
 * Fuente de verdad: último evento entre AccessEvent y SecurityEvent (no existe /api/led).
 * backend/app/models.py:20 AccessEvent.success + :55 SecurityEvent.event_type + :61 timestamp
 * backend/app/routers/events.py:31 / 92 / 105
 *
 * Ventanas (como millis() en led.cpp:64):
 * - GREEN_UNLOCKED: AccessEvent success=true && now-ts < 5000ms (HU-01 verde 5s, throttling STATUS_INTERVAL_MS)
 * - RED_INTRUSION: SecurityEvent event_type=="intrusion" && now-ts < 10000ms (HU-02 rojo; Node-RED Telegram no bloquea)
 * - RED_FAIL: último access success=false && now-ts < 1000ms (informativo, no bloquea UI)
 * - OFF: resto (puerta bloqueada, sin evento reciente)
 * - UNKNOWN: sin datos (colecciones vacías o backend vacío)
 *
 * Ánodo común led.cpp:22 (255-valor) NO aplica aquí: app solo refleja color, no escribe PWM (no analogWrite).
 * Solo lectura — no POST, no MQTT (MOV-03 migrará a Mosquitto push sin romper UI — misma firma getLedState).
 *
 * Timestamps: ISO-8601 con timezone (server_default=func.now() TIMESTAMPTZ). Parse con Instant.parse.
 * Si parse falla, se trata timestamp como 0 (UNKNOWN) — no crashea (como try/catch en JS).
 * Cualquier mención a Tuya debe quedar marcada CANCELADO — ADR-001 §Verificación (no usar bombillo).
 */
object LedStateMapper {

    const val GREEN_WINDOW_MS = 5_000L      // config.h:52 DOOR_AUTO_LOCK_MS=5000 — verde 5s tras success (HU-01)
    const val RED_INTRUSION_WINDOW_MS = 10_000L // HU-02 rojo intrusión 10s — más visible que verde (10s informativo)
    const val RED_FAIL_WINDOW_MS = 1_000L   // config.h:62 LED_RED_FAIL_MS=1000 — rojo fallo PIN 1s (informativo)

    fun map(
        accessEvents: List<AccessEventOut>, // lista de access (normalmente 1, limit=1 en AetherRepositoryImpl:55)
        securityEvents: List<SecurityEventOut>, // lista de security (limit=1)
        nowMs: Long = System.currentTimeMillis() // ahora — como millis() en C led.cpp:64 (inyectable para tests)
    ): LedUiState {
        // Último evento por timestamp — como SELECT * ORDER BY timestamp DESC LIMIT 1 en SQL
        val latestAccess = accessEvents.maxByOrNull { parseTs(it.timestamp) } // maxBy timestamp — como ORDER BY DESC LIMIT 1
        val latestSecurity = securityEvents.maxByOrNull { parseTs(it.timestamp) }

        // Sin ningún evento -> Desconocido (backend vacío, recién instalado) — como UNKNOWN en LedState.kt:27
        if (latestAccess == null && latestSecurity == null) {
            return LedUiState(
                color = LedColor.UNKNOWN,
                state = LedState.UNKNOWN,
                label = "Desconocido", // label UI — como default en React props
                lastEventAt = null,
                source = null
            )
        }

        val accessTs = latestAccess?.let { parseTs(it.timestamp) } ?: 0L
        val securityTs = latestSecurity?.let { parseTs(it.timestamp) } ?: 0L

        // Determinar cuál es el último evento global (tie -> prioriza security intrusión por criticidad — seguridad > acceso)
        val securityIsLatest = latestSecurity != null && securityTs >= accessTs
        val accessIsLatest = latestAccess != null && accessTs > securityTs

        // 1) Si el último es intrusión y está dentro de ventana -> ROJO intrusión (HU-02, prioridad máxima)
        if (securityIsLatest && latestSecurity != null) {
            val isIntrusion = latestSecurity.event_type.equals("intrusion", ignoreCase = true) // case-insensitive — como event_type == "intrusion" en SQL WHERE
            val age = nowMs - securityTs // edad evento — como nowMs - timestamp en JS (millis() - timestamp en C)
            if (isIntrusion && age in 0 until RED_INTRUSION_WINDOW_MS) { // en ventana 0..10000 — como millis() - ts < 10000 en led.cpp
                return LedUiState(
                    color = LedColor.RED,
                    state = LedState.RED_INTRUSION,
                    label = "Rojo intrusión", // rojo intrusión — como setLedMode(RED) en led.cpp
                    lastEventAt = securityTs,
                    source = "security"
                )
            }
            // Intrusión expirada o no-intrusión -> caer a evaluación access/off (no rojo permanente)
        }

        // 2) Si el último es access success -> GREEN 5s (HU-01)
        if (accessIsLatest && latestAccess != null) {
            val age = nowMs - accessTs
            if (latestAccess.success && age in 0 until GREEN_WINDOW_MS) { // success true y age <5000 — como doorUnlocked && millis()-doorUnlockTime < 5000 en door.cpp
                return LedUiState(
                    color = LedColor.GREEN,
                    state = LedState.GREEN_UNLOCKED,
                    label = "Verde desbloqueado", // verde 5s — como GREEN_UNLOCKED en LedState.kt:24
                    lastEventAt = accessTs,
                    source = "access"
                )
            }
            if (!latestAccess.success && age in 0 until RED_FAIL_WINDOW_MS) { // fallo PIN y age <1000 — como RED_FAIL en led.cpp
                return LedUiState(
                    color = LedColor.RED,
                    state = LedState.RED_FAIL,
                    label = "Rojo fallo PIN",
                    lastEventAt = accessTs,
                    source = "access"
                )
            }
            // Access expirado -> OFF si no hay intrusión vigente
            // Antes de decidir OFF, verificar si hay intrusión vigente aunque no sea el último (intrusión puede persistir 10s)
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
                label = "Apagado", // OFF — como LedColor.OFF en LedState.kt:18 (puerta bloqueada, sin evento reciente)
                lastEventAt = accessTs,
                source = "access"
            )
        }

        // 3) Último es security no-intrusión expirado o access expirado -> OFF
        // Verificar si access vigente pero no era el último? (edge: timestamps muy cercanos, race)
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

        // Default OFF — hay eventos pero ninguno vigente (todos expirados)
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
     * Parse ISO-8601 con timezone. Backend usa DateTime(timezone=True) server_default=func.now() (TIMESTAMPTZ).
     * Ejemplos: "2026-09-01T12:00:00.123456+00:00", "2026-09-01T12:00:00Z", "2026-09-01 12:00:00.123456+00:00"
     * Tolerante: sin zona -> asume UTC (como new Date(iso) en JS que asume UTC si no hay TZ).
     */
    fun parseTs(iso: String): Long {
        return try {
            // Normaliza: reemplaza espacio por T si viniera de Postgres sin T (init.sql:11 NOW() sin T a veces)
            val normalized = iso.trim().replace(" ", "T")
            // Instant.parse soporta Z y offset con segundos; fallback para strings sin zona
            try {
                Instant.parse(normalized).toEpochMilli() // como Date.parse(iso) en JS pero con millis
            } catch (_: Exception) {
                // Intenta parsear sin offset asumiendo UTC: "2026-09-01T12:00:00.123456" -> añade Z
                val withZone = if (normalized.contains("+") || normalized.endsWith("Z")) normalized else "${normalized}Z"
                Instant.parse(withZone).toEpochMilli()
            }
        } catch (_: Exception) {
            0L // si parse falla, timestamp 0 — se trata como UNKNOWN (no crashea, como try/catch en JS)
        }
    }
}
