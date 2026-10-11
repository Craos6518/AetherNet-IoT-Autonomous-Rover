/*
 * =============================================================================
 * AetherNet — MEGA Access Control (Cerrojo) | 6º Semestre UTP
  * Autor: Andres Felipe Martinez Henao
 *        2 años Python/JS/React, 1 año PostgreSQL) — Proyecto Integrador FOSS
 * Módulo: RF-2.2 + HU-01 — feature/firmware-mega-cerrojo
 * Hardware: Arduino MEGA 2560 — Keypad 4x4 + Servo MG90S + LED RGB + UART ESP32
 * Sin relés, sin láser en esta rama. LED verde sincronizado a ventana 5s.
 * =============================================================================
 * QUÉ HACE ESTE ARCHIVO:
 *   Es el ORQUESTADOR — como el App.jsx en React que importa y monta todos los
 *   componentes. Aquí solo se hace setup() y loop() llamando a los 4 módulos
 *   en src/ (config, led, door, keypad, uart). Nada de lógica aquí, solo wiring.
 *   Si vienes de Python, piensa en main.py que importa módulos y corre el event loop.
 *
 * FLUJO HU-01 BDD (requirements.md:57-60):
 *   Dado puerta bloqueada (doorInit 0°), Cuando "1234#" → Entonces servo 90°,
 *   LED verde 5s, evento ACCESS vía UART al ESP32 -> HTTP POST -> PostgreSQL.
 *   Todo el flujo es NO BLOQUEANTE — loop() corre a ~500Hz sin delay() largo.
 *
 * POR QUÉ MEGA Y NO UNO:
 *   El MEGA tiene 54 pines digitales y 16 analógicos. Necesitamos 8 para keypad
 *   + 1 servo + 3 LED + 2 UART = 14 pines. Un UNO se queda corto. Además el MEGA
 *   tiene 4 UARTs hardware (Serial, Serial1, Serial2, Serial3) — usamos Serial2
 *   para ESP32 y Serial para debug USB sin conflicto.
 * =============================================================================
 */

// --- Imports de módulos — cada .h es como un import en JS/Python ---
#include <Arduino.h>          // Core Arduino — Serial, millis, pinMode, etc.
#include "src/config.h"       // Configuración central: pines + constantes (ver config.h:1)
#include "src/led.h"          // Módulo LED RGB — máquina de estados no bloqueante
#include "src/door.h"         // Módulo puerta — servo MG90S + auto-lock 5s
#include "src/keypad_control.h" // Módulo teclado — buffer 6, validación PIN, hash
#include "src/laser.h"        // Módulo láser KY-008 — barrera HU-02 no bloqueante
#include "src/uart_protocol.h" // Módulo UART — protocolo TYPE:JSON\n hacia ESP32

// setup() — Se ejecuta UNA vez al encender (como useEffect(() => {}, []) en React)
// Aquí inicializamos cada periférico en orden. El orden importa: LED y puerta primero
// para que la puerta arranque bloqueada y el LED apagado (estado seguro por defecto).
void setup() {
    Serial.begin(115200); // Debug USB — 115200 para logs rápidos en monitor serial
    // Esperar Serial solo si hay host USB conectado; no bloquea si no hay PC (ej. alimentación externa)
    // En React sería como esperar a que devtools conecte pero no bloquear la app si no está.
    unsigned long t0 = millis(); // Guarda tiempo inicio (millis() = ms desde boot, como Date.now())
    while (!Serial && millis() - t0 < 1500) { delay(10); } // Máx 1.5s esperando, luego sigue igual
    Serial.println(F("\n=== AetherNet MEGA Cerrojo RF-2.2/HU-01 ==="));
    // F("") guarda string en Flash (PROGMEM), no en RAM. El MEGA tiene 8KB RAM, hay que cuidarla.
    // En JS no te preocupas de RAM, aquí cada byte cuenta.

    ledInit();   // Inicializa LED RGB — pines 44/45/46 como OUTPUT, apagado (ver led.cpp:10)
    doorInit();  // Inicializa servo MG90S — attach pin 9, posición 0° bloqueada (ver door.cpp:10)
    laserInit(); // Inicializa láser KY-008 — TX 8 HIGH, RX 7 PULLUP, armado (ver laser.cpp:10)
    uartInit();  // Inicializa UART a ESP32 — Serial2 38400 (ver uart_protocol.cpp:9)
    keypadInit(); // Inicializa keypad 4x4 — debounce 50ms, hold 500ms (ver keypad_control.cpp:21)

    // Estado inicial seguro: puerta bloqueada (0°), LED OFF
    // doorInit ya pone servo 0°, ledInit ya apaga LED — redundancia explícita por seguridad
    sendStatusToGateway(); // Envía STATUS inicial al ESP32 para que sepa que arrancamos (ver uart_protocol.cpp:68)
    Serial.println(F("MEGA Cerrojo+Laser listo — PIN 1234 | Auto-lock 5s | LED verde 5s / rojo 3s intrusión"));
    Serial.println(F("Teclas: # envía, * borra, A-D ignoradas"));
    // Log de ayuda — el evaluador ve esto en monitor y sabe qué probar sin leer código
}

// loop() — Se ejecuta INFINITAMENTE (~500Hz, sin delay largo)
// Es el "render loop" — cada iteración revisa si hay tecla, si toca re-bloquear,
// actualiza LED, revisa UART y envía heartbeat. Todo NO BLOQUEANTE.
// En React sería el event loop que atiende clicks, timers y fetch sin bloquear UI.
// En Python sería asyncio loop. Aquí lo hacemos con millis() en cada módulo.
void loop() {
    handleKeypad();       // 1) Lee keypad si hay tecla (no bloquea, ver keypad_control.cpp:26)
    handleLaser();        // 2) Poll láser KY-008 cada 50ms — detecta intrusión sin bloquear (laser.cpp:18)
    handleDoorAutoLock(); // 3) Si puerta desbloqueada y pasaron 5000ms → re-bloquea (ver door.cpp:32)
    updateLed();          // 4) Máquina de estados LED — restaura color tras timers (ver led.cpp:64)
    handleGatewayUart();  // 5) Revisa si ESP32 envió CMD:ACCESS/LASER/STATUS (ver uart_protocol.cpp:14)
    sendPeriodicStatus(); // 6) Cada 5s envía STATUS al Gateway (throttled, ver uart_protocol.cpp:81)
    // Sin delay() aquí — cada función usa millis() para temporizar. Así keypad responde instantáneo
    // aunque LED esté en flash rojo 1s o puerta en ventana 5s. Principio clave de embebidos.
}
