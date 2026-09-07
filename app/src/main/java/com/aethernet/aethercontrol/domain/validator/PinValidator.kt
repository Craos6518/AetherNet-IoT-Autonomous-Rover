package com.aethernet.aethercontrol.domain.validator

// =============================================================================
// PinValidator.kt — Validador PIN y Hash djb2 | 6º Semestre UTP | MOV-04 HU-01, RF-2.2
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 1 año C (djb2 en keypad_control.cpp:99), 2 años Python (hashlib),
//              2 años JS/React (validación form), 2 años electrónica (keypad 4x4)
// Analogía React: este object es como `const PinValidator = { isValidFormat(pin) { return /^\d{4,6}$/.test(pin) } }`
// en JS — valida formato antes de enviar, como validar email en form antes de fetch.
// Analogía Python: como `re.match(r'^\d{4,6}$', pin)` en Python o `hash_pin` en stats.
// Analogía C: espejo de firmware/mega-access/src/config.h:48 VALID_PIN / :49 PIN_MAX_LEN=6
// y keypad_control.cpp:51 isDigit(key) + :99 hashPin() djb2 HEX — misma lógica, dos lenguajes.
// Analogía PostgreSQL: pin_hash aquí es el mismo que se guarda en backend/app/models.py:25
// String(128) — nunca el PIN claro (como password hash en auth).
// FOSS: validación pura Kotlin, sin lib propietaria (RNF-3.1).
// Espejo firmware: config.h:48 VALID_PIN "1234" / :49 PIN_MAX_LEN=6 y keypad_control.cpp:99 hashPin() -> HEX.
// Reglas MVP Sprint 2 (FOSS, sin KDF fuerte): solo dígitos 0-9, 4..6 longitud (HU-01 usa 1234).
// =============================================================================

/**
 * MOV-04 — Validador PIN para envío de comandos de cerrojo desde la app (HU-01, RF-2.2).
 * Espejo firmware/mega-access/src/config.h:48 VALID_PIN / :49 PIN_MAX_LEN=6
 * y firmware/mega-access/src/keypad_control.cpp:51 isDigit + :44 processPinAttempt
 *
 * Reglas MVP Sprint 2 (FOSS, sin criptografía fuerte):
 * - Solo dígitos 0-9, longitud 4..6 (config.h:49 hasta 6 dígitos, HU-01 usa 1234)
 * - No logs con PIN claro fuera de memoria volátil — hash djb2 para audit trail (keypad_control.cpp:99)
 * - rate-limit/throttle a nivel ViewModel (PinViewModel:108 5/60s + 100ms debounce), no aquí
 * - No hardcodea VALID_PIN para validación de formato; la apertura real la valida el MEGA via MQTT CMD:ACCESS.
 *   Sin embargo se expone isCorrectForDemo() solo para tests/preview offline (no en producción).
 */
object PinValidator {

    const val MIN_LEN = 4 // mínimo 4 dígitos — como PIN_MAX_LEN pero mínimo (HU-01 "1234")
    const val MAX_LEN = 6 // máximo 6 — config.h:49 PIN_MAX_LEN=6 (MEGA buffer 6)

    /** Caracteres permitidos: solo dígito 0-9, igual que isDigit(key) en keypad_control.cpp:51 */
    fun isValidFormat(pin: String): Boolean {
        if (pin.length !in MIN_LEN..MAX_LEN) return false // longitud 4..6 — como `if (len < 4 || len > 6) return false` en C
        return pin.all { it.isDigit() } // todos dígitos — como `isDigit(key)` en C, pero para todo el string (como /^\d+$/ en JS)
    }

    /** Hash djb2 idéntico a firmware/mega-access/src/keypad_control.cpp:99 hashPin() -> HEX */
    fun hashPin(pin: String): String {
        // djb2: hash = 5381; hash = hash*33 + c (hash <<5 + hash) — clásico Bernstein, usado en keypad_control.cpp:99
        // En C es `unsigned long hash = 5381; hash = ((hash<<5)+hash) + pin[i];` — aquí Long para evitar overflow
        var hash = 5381L // Long para 64-bit, luego mascara a 32-bit unsigned para espejo Arduino unsigned long
        for (c in pin) {
            hash = ((hash shl 5) + hash) + c.code // shl 5 = <<5 = *32, + hash = *33 — como (hash*33)+c en C
            // Mantener en 32-bit unsigned para espejo Arduino (unsigned long en AVR es 32-bit)
            hash = hash and 0xFFFFFFFFL // máscara 32-bit — como `hash & 0xFFFFFFFF` en JS con >>>0
        }
        return hash.toString(16) // String(hash, HEX) en Arduino -> lowercase sin 0x — como hash.toString(16) en JS
    }

    /** Solo para demos/tests offline — NO usar para autenticar en prod (MEGA valida via MQTT CMD:ACCESS). */
    fun isCorrectForDemo(pin: String, expected: String = "1234"): Boolean = pin == expected // "1234" es VALID_PIN en config.h:48 (MVP hardcodeado)
}
