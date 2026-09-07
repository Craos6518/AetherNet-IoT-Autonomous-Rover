#include "led.h"    // Interfaz LedMode, setLedMode, updateLed
#include "config.h" // LED_R/G/B_PIN, LED_COMMON_ANODE, LED_*_MS timings
#include "door.h"   // isDoorUnlocked() para sincronizar verde/OFF con puerta

// --- Estado privado — static = encapsulado en este archivo (como private en clase) ---
static LedMode currentMode = LedMode::OFF;              // Modo actual visible
static LedMode previousPersistentMode = LedMode::OFF;   // Último persistente (OFF o GREEN) para restaurar tras transitorio
static unsigned long modeStartMs = 0;                   // millis() cuando empezó transitorio
static unsigned long modeDurationMs = 0;                // Duración transitorio (0 = persistente, sin timer)

// ledInit() — Inicializa hardware LED (llamar en setup antes que doorInit)
void ledInit() {
    pinMode(LED_R_PIN, OUTPUT); // 44 rojo — OUTPUT para analogWrite PWM
    pinMode(LED_G_PIN, OUTPUT); // 45 verde
    pinMode(LED_B_PIN, OUTPUT); // 46 azul
    setLedColor(0, 0, 0);       // Apagado inicial — (0,0,0) = negro/off
    currentMode = LedMode::OFF;
    previousPersistentMode = LedMode::OFF;
    // Estado inicial: apagado, puerta bloqueada. Coherente con doorInit() 0°.
}

// setLedColor() — Escribe color RGB directo con PWM (como element.style.background = `rgb(r,g,b)` en CSS)
// Pero con truco: ánodo común invierte lógica (ver config.h:33)
void setLedColor(uint8_t r, uint8_t g, uint8_t b) {
#ifdef LED_COMMON_ANODE
    // Ánodo común: pin común a 5V, cátodos a pines via 220Ω.
    // Para encender rojo: pin R a 0V (LOW) fluye corriente. Para apagar: 5V (HIGH) sin diferencial.
    // analogWrite(pin, 0) = 0V = brillo máximo; 255 = 5V = apagado. Por eso 255 - valor.
    // Lo medí con multímetro: analogWrite(0) da 0V, analogWrite(255) da 5V.
    analogWrite(LED_R_PIN, 255 - r);
    analogWrite(LED_G_PIN, 255 - g);
    analogWrite(LED_B_PIN, 255 - b);
#else
    // Cátodo común (no usado, pero deja opción) — lógica directa
    analogWrite(LED_R_PIN, r);
    analogWrite(LED_G_PIN, g);
    analogWrite(LED_B_PIN, b);
#endif
}

// applyMode() — Helper interno que traduce LedMode a color RGB (como mapear state a CSS class)
static void applyMode(LedMode m) {
    switch (m) {
        case LedMode::OFF:              setLedColor(0, 0, 0); break;     // Negro — apagado
        case LedMode::GREEN_UNLOCKED:   setLedColor(0, 255, 0); break;   // Verde puro — éxito HU-01
        case LedMode::RED_FAIL:
        case LedMode::RED_CLEAR:        setLedColor(255, 0, 0); break;   // Rojo — error o clear (mismo color, distinta duración)
        case LedMode::BLUE_TAP:         setLedColor(0, 0, 255); break;   // Azul — tap dígito
    }
}

// setLedMode() — Cambia modo con lógica de overlay no bloqueante
// Idea clave: modos persistentes (OFF/GREEN) no tienen timer; transitorios (RED/BLUE) sí.
// Si estás en verde (puerta abierta) y llega RED_FAIL, guarda verde para restaurarlo tras 1s.
// Es como un toast en React que se superpone pero vuelve al contenido anterior.
void setLedMode(LedMode mode) {
    // Modos persistentes (OFF / GREEN_UNLOCKED) no usan timer; son el "estado base"
    if (mode == LedMode::GREEN_UNLOCKED || mode == LedMode::OFF) {
        currentMode = mode;
        previousPersistentMode = mode; // Actualiza base — es el nuevo normal
        modeDurationMs = 0;            // 0 = sin expiración (persiste hasta que door o error cambie)
        applyMode(mode);
        return;
    }
    // Modo transitorio: guarda el persistente actual para restaurar después
    // Solo guarda si estábamos en persistente — si ya hay overlay, no pisar el guardado
    if (currentMode == LedMode::GREEN_UNLOCKED || currentMode == LedMode::OFF) {
        previousPersistentMode = currentMode; // Ej. GREEN antes de RED_FAIL
    }
    currentMode = mode;
    modeStartMs = millis(); // Marca inicio — como Date.now() en JS
    // Asigna duración según tipo (ver config.h:61-64)
    if (mode == LedMode::RED_FAIL) modeDurationMs = LED_RED_FAIL_MS;       // 1000ms
    else if (mode == LedMode::BLUE_TAP) modeDurationMs = LED_BLUE_TAP_MS;  // 50ms
    else if (mode == LedMode::RED_CLEAR) modeDurationMs = LED_CLEAR_FLASH_MS; // 100ms
    else modeDurationMs = 0;
    applyMode(mode); // Aplica color inmediatamente
}

// updateLed() — Se llama cada loop() para manejar expiración y sincronización con puerta
// Es el "tick" de la máquina de estados — sin esto los transitorios nunca expirarían.
void updateLed() {
    // Sincronización con puerta: calcula qué persistente debería estar según isDoorUnlocked()
    // Si puerta desbloqueada → verde; si bloqueada → OFF. Esto debe reflejarse si no hay overlay.
    bool unlocked = isDoorUnlocked(); // Pregunta a door.cpp — fuente de verdad de puerta
    LedMode expectedPersistent = unlocked ? LedMode::GREEN_UNLOCKED : LedMode::OFF;

    // Caso 1: Estamos en transitorio y ya venció → restaura el persistente esperado
    // Ej. RED_FAIL 1s vencido → vuelve a GREEN si puerta sigue abierta, o OFF si ya se re-bloqueó
    if (modeDurationMs > 0 && (millis() - modeStartMs >= modeDurationMs)) {
        // Transitorio vencido — calcula cuánto pasó con resta unsigned (seguro ante overflow 49 días)
        currentMode = expectedPersistent; // No restaura previousPersistentMode ciegamente — recalcula según puerta actual
        previousPersistentMode = expectedPersistent;
        modeDurationMs = 0; // Vuelve a persistente
        applyMode(currentMode);
        return;
    }
    // Caso 2: Estamos en persistente pero la puerta cambió de estado (auto-lock) → refleja
    // Solo si no estamos en overlay transitorio (modeDurationMs==0) — no pisar flash rojo/azul activo
    if (modeDurationMs == 0 && currentMode != expectedPersistent) {
        // No pisar un transitorio activo — solo si estábamos en persistente
        if (currentMode == LedMode::GREEN_UNLOCKED || currentMode == LedMode::OFF) {
            currentMode = expectedPersistent;
            previousPersistentMode = expectedPersistent;
            applyMode(currentMode);
        }
    }
    // Si estamos en transitorio no vencido, no hace nada — deja que termine su 50/100/1000ms
}

// isDoorUnlockedLed() — Helper debug — delega a door.h
// Existe por si alguien quiere preguntar LED sin incluir door.h, pero el estado real vive en door.cpp
bool isDoorUnlockedLed() {
    return isDoorUnlocked();
}
