#pragma once
/*
 * AetherNet MEGA — config.h
 * RF-2.2 + HU-01 (feature/firmware-mega-cerrojo)
 * MVP Sprint 2 — sin relés, sin láser en esta rama.
 *
 * Origen: docs/requirements.md:22 RF-2.2, HU-01 BDD Dado/Cuando/Entonces
 *         docs/hardware-inventory.md:9 LED local
 *         docs/prd.md §6 Contingencia Edge
 */

#include <Arduino.h>

// ---------------------------------------------------------------------------
// PINOUT — Arduino MEGA 2560
// ---------------------------------------------------------------------------
// Keypad 4x4
static const byte ROW_PINS[4] = {22, 24, 26, 28};
static const byte COL_PINS[4] = {30, 32, 34, 36};

// Servo MG90S — cerrojo
#define SERVO_PIN 9
static const int SERVO_LOCKED = 0;     // 0° = bloqueada
static const int SERVO_UNLOCKED = 90;  // 90° = desbloqueada (HU-01)

// LED RGB local — feedback inmediato sin red (hardware-inventory.md:9)
#define LED_R_PIN 44
#define LED_G_PIN 45
#define LED_B_PIN 46

// Láser KY-008 — reservado para feature/firmware-mega-laser (RF-2.3, HU-02)
// Se define para no perder el pinout, pero NO se usa en esta rama.
// #define LASER_TX_PIN 8
// #define LASER_RX_PIN 7

// UART hacia Gateway ESP32
#define GATEWAY_SERIAL Serial2
#define GATEWAY_BAUD 115200
// MEGA Serial2: RX=16, TX=17 (mega-access.ino:13)

// ---------------------------------------------------------------------------
// PARÁMETROS LÓGICOS — RF-2.2 / HU-01
// ---------------------------------------------------------------------------
// PIN MVP hardcodeado — MOV-04 migrará a EEPROM/multijuario
static const char VALID_PIN[] = "1234";  // hasta 6 dígitos (buffer limita)
static const uint8_t PIN_MAX_LEN = 6;

// Ventana de desbloqueo — sincronizada con LED verde
static const unsigned long DOOR_AUTO_LOCK_MS = 5000;  // 5 s

// UART
static const unsigned long STATUS_INTERVAL_MS = 5000;  // throttled, antes 100ms spam

// Keypad
static const unsigned long KEYPAD_DEBOUNCE_MS = 50;
static const unsigned long KEYPAD_HOLD_MS = 500;

// LED timings no bloqueantes
static const unsigned long LED_RED_FAIL_MS = 1000;   // PIN erróneo
static const unsigned long LED_BLUE_TAP_MS = 50;     // feedback dígito
static const unsigned long LED_CLEAR_FLASH_MS = 100; // '*' borra
