#pragma once
/*
 * keypad_control.h — RF-2.2 keypad 4x4 no bloqueante, buffer hasta 6 dígitos.
 * '#' envía, '*' borra, A-D ignoradas explícitamente.
 */

#include <Arduino.h>

void keypadInit();
void handleKeypad();
// Llamado por keypad y por comando UART CMD:ACCESS
void processPinAttempt(const String& pin);
String hashPin(const String& pin);
