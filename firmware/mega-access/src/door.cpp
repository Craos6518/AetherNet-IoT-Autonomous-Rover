#include "door.h"
#include "config.h"
#include "led.h"
#include <Servo.h>

static Servo doorServo;
static bool doorUnlocked = false;
static unsigned long doorUnlockTime = 0;

void doorInit() {
    doorServo.attach(SERVO_PIN);
    doorServo.write(SERVO_LOCKED);
    doorUnlocked = false;
    // LED se inicializa en ledInit(); aquí solo aseguramos estado coherente
}

void unlockDoor() {
    doorServo.write(SERVO_UNLOCKED);
    doorUnlocked = true;
    doorUnlockTime = millis();
    setLedMode(LedMode::GREEN_UNLOCKED);
    Serial.println(F("Door UNLOCKED (90°)"));
}

void lockDoor() {
    doorServo.write(SERVO_LOCKED);
    doorUnlocked = false;
    setLedMode(LedMode::OFF);
    Serial.println(F("Door LOCKED (0°)"));
}

void handleDoorAutoLock() {
    if (doorUnlocked && (millis() - doorUnlockTime >= DOOR_AUTO_LOCK_MS)) {
        lockDoor();
    }
}

bool isDoorUnlocked() {
    return doorUnlocked;
}
