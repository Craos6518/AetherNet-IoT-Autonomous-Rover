#pragma once // Evita incluir 2 veces — como export guard en JS, pero en C
/*
 * =============================================================================
 * AetherNet MEGA — config.h | Única Fuente de Verdad | 6º Semestre UTP
 * Autor: Est. Tec. Desarrollo Software + Ing. Sistemas (2 años electrónica/C)
 * Sprint: RF-2.2 + HU-01 (feature/firmware-mega-cerrojo) — MVP sin relés/láser
 * Origen: docs/requirements.md:22 RF-2.2, HU-01 BDD
 *         docs/hardware-inventory.md:9 LED local (Tuya cancelado ADR-001)
 *         docs/prd.md §6 Contingencia Edge (puerta funciona sin WiFi)
 * =============================================================================
 * POR QUÉ ESTE ARCHIVO ES EL MÁS IMPORTANTE:
 *   Todo el pinout y constantes viven AQUÍ. Si cambias un cable, cambias aquí
 *   y todo el código se adapta. Es como un .env o un constants.js en web —
 *   single source of truth. Aprendí en 2 años de Arduino que tener pines
 *   hardcodeados en cada .cpp es un infierno de bugs.
 *
 * EXPERIENCIA ELECTRÓNICA APLICADA:
 *   - MEGA tiene 54 pines digitales (0-53) + 16 analógicos (A0-A15). No todos son PWM.
 *   - PWM solo en 2-13 y 44-46. LED RGB necesita PWM para mezclar colores.
 *   - Keypad 4x4 usa 8 pines digitales cualquiera, pero el cableado físico invierte filas/cols.
 *   - Servo MG90S necesita PWM + Timer1 (pin 9 es Timer1 en MEGA).
 * =============================================================================
 */

#include <Arduino.h> // Para byte, pinMode, etc.

// ---------------------------------------------------------------------------
// PINOUT — Arduino MEGA 2560 | Verificado con multímetro y cableado real
// ---------------------------------------------------------------------------
// Keypad 4x4 — FIX TRANSPOSE 2026-08-26: el cableado físico invierte filas/columnas
// Observado en test: tecla (0,1) debía dar '2' pero daba '4' — patrón transpose.
// Significa que lo que creíamos filas eran columnas y viceversa.
// Fix: invertir ROW_PINS y COL_PINS. Ahora (0,1) da '2' correcto.
// Si vienes de React, es como tener rows/cols de una grid CSS invertidos por wiring.
// Ver keypad_control.cpp:10 para el mapa lógico que usa estos pines.
static const byte ROW_PINS[4] = {30, 32, 34, 36}; // Filas del keypad — van a R1-R4 del módulo
static const byte COL_PINS[4] = {22, 24, 26, 28}; // Columnas — van a C1-C4
// NOTA: Estos 8 pines son digitales cualquiera, no necesitan ser PWM. Solo lectura/escritura.

// Servo MG90S — cerrojo físico de la puerta
// El servo gira 0-180°, pero lo usamos 0° (bloqueada) y 90° (desbloqueada) por mecánica del cerrojo.
// Pin 9 es PWM + Timer1 en MEGA — la librería Servo.h usa Timer1 internamente.
// Si usas pin 44/45/46 (Timer5) también sirve, pero 9 es estándar y libre.
#define SERVO_PIN 9
static const int SERVO_LOCKED = 0;     // 0° = cerrojo cerrado — puerta bloqueada (estado seguro)
static const int SERVO_UNLOCKED = 90;  // 90° = cerrojo abierto — puerta desbloqueada (HU-01 Then)
// En C usamos #define para pin (preprocesador) y const int para ángulos (tipado).

// LED RGB local — feedback inmediato sin red (hardware-inventory.md:9)
// Es el ÚNICO indicador visual tras cancelar bombillo Tuya (ADR-001 2026-09-01).
// Ventaja: funciona aunque caiga WiFi/MQTT — es Edge, no depende de backend.
// Como un indicador LED en un router — no necesita internet para encenderse.
// Hardware real: LED ánodo común (common anode) — pin común a 5V, cátodos R/G/B a 44/45/46 vía 220Ω a GND
// Cada color se controla por PWM: analogWrite(0-255). Pero lógica invertida por ánodo común.
#define LED_R_PIN 44 // Rojo — PWM (Timer5 en MEGA)
#define LED_G_PIN 45 // Verde — PWM
#define LED_B_PIN 46 // Azul — PWM
#define LED_COMMON_ANODE true // Flag para invertir PWM en led.cpp:20 (255 - valor)
// Lógica invertida: LOW (0V) en pin = corriente fluye = LED enciende.
// HIGH (5V) = sin diferencial = apagado. Por eso analogWrite(255) apaga y 0 enciende al máximo.
// Si fuera cátodo común, sería directo: 255 = brillo máximo. Lo aprendí midiendo con multímetro.

// Láser KY-008 — ACTIVO en feature/firmware-mega-laser-v2 (RF-2.3, HU-02)
// HU-02: barrera láser detecta intrusión si se interrumpe el haz → SECURITY vía UART → Telegram + LED rojo.
// Hardware: KY-008 emisor (láser rojo 650nm) en pin 8, receptor fotoresistencia/LDR en pin 7 con pullup.
#define LASER_TX_PIN 8  // Emisor láser — OUTPUT digital (HIGH = láser ON)
#define LASER_RX_PIN 7  // Receptor — INPUT_PULLUP (HIGH = haz intacto, LOW = haz interrumpido)

// UART hacia Gateway ESP32 — MEGA Serial2: TX=16, RX=17 (baud 38400 estable)
// MEGA tiene 4 UARTs: Serial (USB 0/1), Serial1 (18/19), Serial2 (16/17), Serial3 (14/15)
// Usamos Serial2 porque Serial es debug USB y Serial1/3 libres para futuro.
// 38400 no 115200: a 115200 el divisor 1k/2k (5V MEGA -> 3.3V ESP32) distorsiona.
// 38400 es más tolerante a flancos lentos. Ver gateway-esp32.ino:65 mismo baud.
// Cruzado: MEGA TX17 -> ESP32 RX16, MEGA RX16 <- ESP32 TX17 + GND común obligatorio.
#define GATEWAY_SERIAL Serial2 // Alias para legibilidad — como const gatewaySerial = Serial2 en JS
#define GATEWAY_BAUD 38400     // Baudrate — ambos lados idénticos o hay framing errors

// ---------------------------------------------------------------------------
// PARÁMETROS LÓGICOS — RF-2.2 / HU-01 | BDD Dado/Cuando/Entonces
// ---------------------------------------------------------------------------
// PIN MVP hardcodeado — MOV-04 migrará a EEPROM/multiusuario con PostgreSQL
// Por ahora es "1234" para validar HU-01 sin complejidad de storage.
// En producción iría en EEPROM + hash + endpoint POST /api/access-events con múltiples usuarios.
// Como hardcodear admin/admin en dev — solo para MVP.
static const char VALID_PIN[] = "1234";  // PIN correcto — 4 dígitos (hasta 6 por buffer)
static const uint8_t PIN_MAX_LEN = 6;    // Buffer máximo — evita overflow de String inputBuffer

// Ventana de desbloqueo — sincronizada con LED verde (HU-01 Then: servo 90° + LED verde 5s)
// Si desbloqueas, tienes 5s para abrir la puerta antes de que se re-bloquee sola.
// Igual que un token JWT expira — ventana temporal de acceso.
static const unsigned long DOOR_AUTO_LOCK_MS = 5000;  // 5 segundos — ver door.cpp:32 handleDoorAutoLock

// UART — throttling para no saturar
static const unsigned long STATUS_INTERVAL_MS = 5000;  // STATUS cada 5s, no cada 100ms (antes spameaba)
// Antes mandaba STATUS cada 100ms y llenaba buffer UART del ESP32. 5s es suficiente para dashboard.

// Keypad — anti-rebote y hold
static const unsigned long KEYPAD_DEBOUNCE_MS = 50;  // 50ms debounce — evita dobles pulsaciones por rebote mecánico
static const unsigned long KEYPAD_HOLD_MS = 500;     // 500ms para detectar tecla mantenida (no usado en HU-01, pero lib lo necesita)
// Debounce es como debounce() en JS — ignorar eventos rápidos duplicados.

// LED timings no bloqueantes — máquina de estados en led.cpp
// Cada modo tiene duración distinta. No usan delay(), sino millis() comparado en updateLed().
static const unsigned long LED_RED_FAIL_MS = 1000;   // Rojo 1s en PIN erróneo — feedback error (como toast error en React)
static const unsigned long LED_BLUE_TAP_MS = 50;     // Azul 50ms por dígito — feedback táctil (como ripple en Material)
static const unsigned long LED_CLEAR_FLASH_MS = 100; // Rojo 100ms en '*' borra — confirma borrado
static const unsigned long LED_RED_INTRUSION_MS = 3000; // Rojo 3s en intrusión láser HU-02 — alerta sostenida sin bloquear
// Estos timings son UX físico — el usuario siente si el sistema responde. 50ms es perceptible pero no molesto.

// Láser KY-008 — timings no bloqueantes (laser.cpp)
static const unsigned long LASER_CHECK_INTERVAL_MS = 50;      // Poll cada 50ms — suficiente sin saturar loop
static const unsigned long LASER_INTRUSION_COOLDOWN_MS = 3000; // Cooldown 3s tras disparo — evita spam SECURITY si haz queda interrumpido
