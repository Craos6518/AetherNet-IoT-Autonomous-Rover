#pragma once
/*
 * led.h — Control LED RGB local no bloqueante
 * HU-01: verde mientras puerta desbloqueada (5s), rojo 1s en fallo, azul 50ms por dígito.
 */

#include <Arduino.h>

enum class LedMode : uint8_t {
    OFF,
    GREEN_UNLOCKED,  // verde sólido mientras doorUnlocked (sincronizado a DOOR_AUTO_LOCK_MS)
    RED_FAIL,        // fallo PIN
    BLUE_TAP,        // dígito pulsado
    RED_CLEAR        // '*' borrado
};

void ledInit();
void setLedColor(uint8_t r, uint8_t g, uint8_t b);
// Llamar tras cambios de estado; updateLed() en loop restaura tras timers
void setLedMode(LedMode mode);
void updateLed();
// Helpers consultados por otros módulos
bool isDoorUnlockedLed(); // solo para debug, el estado real vive en door.h
