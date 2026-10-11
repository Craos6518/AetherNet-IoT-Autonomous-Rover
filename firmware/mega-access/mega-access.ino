/*
 * AetherNet — MEGA Access Control (cerrojo)
 * RF-2.2 + HU-01 — feature/firmware-mega-cerrojo
 * Hardware: Arduino MEGA 2560 — Keypad 4x4 + Servo MG90S + LED RGB + UART a ESP32
 * Sin relés, sin láser en esta rama. LED verde sincronizado a ventana 5s.
 *
 * Flujo HU-01: Dado bloqueada, Cuando "1234#" → Entonces servo 90°, LED verde 5s, evento ACCESS vía UART.
 */

#include <Arduino.h>
#include "src/config.h"
#include "src/led.h"
#include "src/door.h"
#include "src/keypad_control.h"
#include "src/uart_protocol.h"

void setup() {
    Serial.begin(115200);
    // Esperar Serial solo en debug USB; no bloquea si no hay host
    unsigned long t0 = millis();
    while (!Serial && millis() - t0 < 1500) { delay(10); }
    Serial.println(F("\n=== AetherNet MEGA Cerrojo RF-2.2/HU-01 ==="));

    ledInit();
    doorInit();
    uartInit();
    keypadInit();

    // Estado inicial: bloqueada, LED OFF
    // doorInit ya pone servo 0°, ledInit OFF
    sendStatusToGateway();
    Serial.println(F("MEGA Cerrojo listo — PIN MVP: 1234 | Auto-lock 5s | LED verde sincronizado"));
    Serial.println(F("Teclas: # envía, * borra, A-D ignoradas"));
}

void loop() {
    handleKeypad();
    handleDoorAutoLock();
    updateLed();
    handleGatewayUart();
    sendPeriodicStatus();
}
