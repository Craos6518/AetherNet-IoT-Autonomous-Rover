#include "led.h"
#include "config.h"
#include "door.h"

static LedMode currentMode = LedMode::OFF;
static LedMode previousPersistentMode = LedMode::OFF;
static unsigned long modeStartMs = 0;
static unsigned long modeDurationMs = 0;

void ledInit() {
    pinMode(LED_R_PIN, OUTPUT);
    pinMode(LED_G_PIN, OUTPUT);
    pinMode(LED_B_PIN, OUTPUT);
    setLedColor(0, 0, 0);
    currentMode = LedMode::OFF;
    previousPersistentMode = LedMode::OFF;
}

void setLedColor(uint8_t r, uint8_t g, uint8_t b) {
    analogWrite(LED_R_PIN, r);
    analogWrite(LED_G_PIN, g);
    analogWrite(LED_B_PIN, b);
}

static void applyMode(LedMode m) {
    switch (m) {
        case LedMode::OFF:              setLedColor(0, 0, 0); break;
        case LedMode::GREEN_UNLOCKED:   setLedColor(0, 255, 0); break;
        case LedMode::RED_FAIL:
        case LedMode::RED_CLEAR:        setLedColor(255, 0, 0); break;
        case LedMode::BLUE_TAP:         setLedColor(0, 0, 255); break;
    }
}

void setLedMode(LedMode mode) {
    // Modos persistentes (OFF / GREEN_UNLOCKED) no usan timer; los transitorios sí.
    if (mode == LedMode::GREEN_UNLOCKED || mode == LedMode::OFF) {
        currentMode = mode;
        previousPersistentMode = mode;
        modeDurationMs = 0;
        applyMode(mode);
        return;
    }
    // Modo transitorio: guarda el persistente para restaurar
    if (currentMode == LedMode::GREEN_UNLOCKED || currentMode == LedMode::OFF) {
        previousPersistentMode = currentMode;
    }
    currentMode = mode;
    modeStartMs = millis();
    if (mode == LedMode::RED_FAIL) modeDurationMs = LED_RED_FAIL_MS;
    else if (mode == LedMode::BLUE_TAP) modeDurationMs = LED_BLUE_TAP_MS;
    else if (mode == LedMode::RED_CLEAR) modeDurationMs = LED_CLEAR_FLASH_MS;
    else modeDurationMs = 0;
    applyMode(mode);
}

void updateLed() {
    // Sincronización con puerta: si está desbloqueada y no hay overlay transitorio, forzar verde
    bool unlocked = isDoorUnlocked();
    LedMode expectedPersistent = unlocked ? LedMode::GREEN_UNLOCKED : LedMode::OFF;

    // Si estamos en modo transitorio y ya venció, restaurar el persistente esperado
    if (modeDurationMs > 0 && (millis() - modeStartMs >= modeDurationMs)) {
        // Transitorio vencido
        currentMode = expectedPersistent;
        previousPersistentMode = expectedPersistent;
        modeDurationMs = 0;
        applyMode(currentMode);
        return;
    }
    // Si estamos en modo persistente pero la puerta cambió de estado, reflejarlo
    // Solo si no estamos en overlay transitorio
    if (modeDurationMs == 0 && currentMode != expectedPersistent) {
        // No pisar un transitorio activo
        if (currentMode == LedMode::GREEN_UNLOCKED || currentMode == LedMode::OFF) {
            currentMode = expectedPersistent;
            previousPersistentMode = expectedPersistent;
            applyMode(currentMode);
        }
    }
}

bool isDoorUnlockedLed() {
    return isDoorUnlocked();
}
