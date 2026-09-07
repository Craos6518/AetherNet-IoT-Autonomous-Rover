#pragma once
/*
 * =============================================================================
 * keypad_control.h — Teclado Matricial 4x4 No Bloqueante | 6º Semestre UTP
 * Autor: Est. Tec. Desarrollo Software + Ing. Sistemas (2 años Arduino/C,
 *        2 años Python/JS/React) — RF-2.2 / HU-01
 * Hardware: Keypad 4x4 (8 pines MEGA) — librería Keypad 3.1.1 (FOSS)
 * BDD: '#' envía PIN, '*' borra, A-D ignoradas, buffer máx 6, hash djb2
 * =============================================================================
 * QUÉ ES ESTO:
 *   Interfaz pública del módulo teclado. Como un `export` en JS/TS — declara
 *   qué funciones pueden usar otros módulos (door, uart, mega-access.ino)
 *   sin exponer el buffer interno inputBuffer (static en .cpp).
 *   Si vienes de React, es como el type de un hook useKeypad().
 *
 * POR QUÉ KEYPAD 4x4 MATRICIAL:
 *   16 teclas con solo 8 pines (4 filas + 4 columnas) — multiplexado por filas.
 *   La lib Keypad escanea filas/cols y detecta cruce. Sin lib habría que
 *   hacer scanning manual con debounce — mucho más código y bugs.
 * =============================================================================
 */

#include <Arduino.h> // Para String

void keypadInit(); // Configura debounce 50ms y hold 500ms (llamar en setup)
void handleKeypad(); // Lee tecla si hay (no bloqueante, llamar cada loop) — como onKeyPress en React
// Llamado por keypad (tecla '#') y por comando UART CMD:ACCESS (PIN remoto desde App)
void processPinAttempt(const String& pin); // Valida PIN vs VALID_PIN, mueve servo, envía ACCESS via UART
String hashPin(const String& pin); // Hash djb2 HEX — evita PIN en claro en logs/MQTT/PostgreSQL (no criptográfico)
