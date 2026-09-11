#pragma once
/*
 * =============================================================================
 * led.h — Control LED RGB Local No Bloqueante | 6º Semestre UTP | HU-01 / HU-02
  * Autor: Andres Felipe Martinez Henao
 * Hardware: LED RGB ánodo común en 44(R)/45(G)/46(B) PWM — 220Ω a GND
 * BDD: Verde 5s si PIN OK, rojo 1s si falla, azul 50ms por dígito
 * =============================================================================
 * QUÉ ES ESTO:
 *   Máquina de estados para LED RGB sin usar delay() — todo con millis().
 *   Si vienes de React, piensa en un componente con state LedMode y useEffect
 *   que hace setTimeout para volver al estado anterior. Aquí lo hacemos manual
 *   porque en Arduino no hay setTimeout — solo loop() + millis().
 *   Si vienes de Python, es como una state machine con timestamps.
 *
 * POR QUÉ NO delay():
 *   delay(1000) congelaría todo el loop() 1s — keypad no respondería, puerta
 *   no haría auto-lock, UART se llenaría. Con millis() cada módulo avanza
 *   sin bloquear a los demás. Principio #1 de embebidos que aprendí a las malas.
 *
 * COLORES (como CSS rgb()):
 *   OFF = (0,0,0) apagado | GREEN = (0,255,0) éxito | RED = (255,0,0) error
 *   BLUE = (0,0,255) tap | RED_CLEAR igual que RED pero 100ms
 *   En ánodo común (config.h:33) se invierte: analogWrite(255 - valor)
 * =============================================================================
 */

#include <Arduino.h> // Para uint8_t, etc.

// Enum class — estados posibles del LED (como union type en TS: 'off' | 'green' | ...)
// uint8_t = 1 byte, eficiente en RAM (MEGA tiene 8KB, cada byte cuenta)
enum class LedMode : uint8_t {
    OFF,             // Apagado — puerta bloqueada en reposo (estado inicial y final)
    GREEN_UNLOCKED,  // Verde sólido mientras doorUnlocked (sincronizado a DOOR_AUTO_LOCK_MS=5000)
    RED_FAIL,        // Rojo 1s — PIN erróneo (feedback error, como toast rojo en React)
    BLUE_TAP,        // Azul 50ms — dígito pulsado (feedback táctil, como ripple Material)
    RED_CLEAR,       // Rojo 100ms — '*' borra buffer (confirma borrado)
    RED_INTRUSION    // Rojo 3s — intrusión láser HU-02 (alerta sostenida, no bloqueante)
};

void ledInit(); // Configura pines 44/45/46 como OUTPUT y apaga LED (llamar en setup)
void setLedColor(uint8_t r, uint8_t g, uint8_t b); // Escribe PWM directo 0-255 (respeta ánodo común)
// Llamar tras cambios de estado; updateLed() en loop restaura tras timers
void setLedMode(LedMode mode); // Cambia modo — maneja timers y guarda persistente para restaurar
void updateLed(); // Revisa cada loop() si venció transitorio o si puerta cambió — restaura color
// Helpers consultados por otros módulos
bool isDoorUnlockedLed(); // Solo para debug — el estado real vive en door.h (isDoorUnlocked)
