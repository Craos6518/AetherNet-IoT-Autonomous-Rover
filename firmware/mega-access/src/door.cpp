#include "door.h"    // Interfaz que implementamos
#include "config.h"  // SERVO_PIN, SERVO_LOCKED/UNLOCKED, DOOR_AUTO_LOCK_MS
#include "led.h"     // setLedMode() para sincronizar LED verde con puerta
#include <Servo.h>   // Librería oficial Arduino Servo — maneja PWM 50Hz para servos

// --- Estado privado del módulo — static = solo visible en este .cpp (como private en clase) ---
// En C no hay clases, pero static a nivel archivo es encapsulamiento.
// En JS sería let doorUnlocked dentro de un closure; en Python, _door_unlocked privado.
static Servo doorServo;                  // Objeto servo — controla pulso PWM 1-2ms en pin 9
static bool doorUnlocked = false;        // Estado lógico — true si desbloqueada (90°)
static unsigned long doorUnlockTime = 0; // millis() cuando se desbloqueó — para auto-lock

// doorInit() — Inicializa hardware al boot (llamar en mega-access.ino:setup)
// Debe dejar la puerta en estado SEGURO: bloqueada (0°) por defecto.
// Es como poner isAuthenticated=false al iniciar una app — seguro por defecto.
void doorInit() {
    doorServo.attach(SERVO_PIN);      // Conecta servo al pin 9 — reserva Timer1, empieza a enviar pulsos
    doorServo.write(SERVO_LOCKED);    // 0° — cerrojo cerrado. write() convierte grados a pulso 1ms
    doorUnlocked = false;             // Estado coherente con servo físico
    // LED se inicializa en ledInit() aparte; aquí solo aseguramos que puerta y flag coincidan
    // Si no pones write() aquí, el servo puede quedarse en posición aleatoria al boot (peligroso)
}

// unlockDoor() — Desbloquea puerta (llamado cuando PIN correcto en keypad_control.cpp:71)
// Hace 3 cosas atómicas: mueve servo, marca estado, enciende LED verde y guarda tiempo.
// Es una "transacción" — todo o nada, estado consistente.
void unlockDoor() {
    doorServo.write(SERVO_UNLOCKED); // 90° — cerrojo abierto. El servo tarda ~300ms en llegar, pero no bloquea
    doorUnlocked = true;             // Flag para handleDoorAutoLock() y led.cpp
    doorUnlockTime = millis();       // Guarda "ahora" — millis() es ms desde boot (como Date.now())
    setLedMode(LedMode::GREEN_UNLOCKED); // LED verde sólido — feedback inmediato HU-01 Then
    Serial.println(F("Door UNLOCKED (90°)")); // Log USB — F() guarda en Flash, no RAM (MEGA 8KB RAM limitada)
    // Nota electrónica: el MG90S consume ~500mA al mover — asegúrate fuente 5V 1A mínimo o se resetea MEGA
}

// lockDoor() — Bloquea puerta (llamado por auto-lock o manual)
// Simétrico a unlockDoor pero inverso. Idempotente: si ya está bloqueada, no hace daño llamar de nuevo.
void lockDoor() {
    doorServo.write(SERVO_LOCKED); // 0° — cerrojo cerrado
    doorUnlocked = false;          // Actualiza flag
    setLedMode(LedMode::OFF);      // Apaga LED — sincronizado con puerta (ver led.cpp updateLed)
    Serial.println(F("Door LOCKED (0°)"));
}

// handleDoorAutoLock() — Revisa cada loop() si toca re-bloquear (NO BLOQUEANTE)
// Es el "temporizador" sin usar delay() — compara millis() actual vs guardado.
// En React sería setTimeout(() => lockDoor(), 5000) pero sin bloquear event loop.
// En Python sería asyncio.create_task(sleep(5); lock()).
// Se llama en mega-access.ino:loop cada iteración (~500Hz), así que reacciona en ~2ms tras vencer.
void handleDoorAutoLock() {
    // Solo si está desbloqueada Y ya pasaron 5000ms desde unlockDoor()
    // millis() - doorUnlockTime es seguro aunque millis() haga overflow a los 49 días (aritmética unsigned)
    if (doorUnlocked && (millis() - doorUnlockTime >= DOOR_AUTO_LOCK_MS)) {
        lockDoor(); // Re-bloquea automáticamente — ventana HU-01 cerrada
    }
}

// isDoorUnlocked() — Getter para otros módulos (led.cpp lo usa para sincronizar color)
// En C no hay getter/setter, pero esta función es el "public read" del estado privado.
// led.cpp pregunta esto cada updateLed() para saber si debe estar verde u OFF.
bool isDoorUnlocked() {
    return doorUnlocked;
}
