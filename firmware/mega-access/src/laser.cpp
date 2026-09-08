#include "laser.h"
#include "config.h"
#include "led.h"
#include "uart_protocol.h" // sendSecurityEvent()
#include <Arduino.h>

// --- Estado privado ---
static bool laserArmed = true;            // Por defecto armado (HU-02 Dado armado)
static bool lastBeamState = true;         // HIGH = intacto, LOW = interrumpido
static unsigned long lastCheckMs = 0;
static unsigned long lastIntrusionMs = 0; // Para cooldown

void laserInit() {
    pinMode(LASER_TX_PIN, OUTPUT);
    pinMode(LASER_RX_PIN, INPUT_PULLUP);
    digitalWrite(LASER_TX_PIN, HIGH); // Láser ON al boot
    laserArmed = true;
    lastBeamState = digitalRead(LASER_RX_PIN); // Lee inicial (pullup → HIGH si haz llega)
    lastCheckMs = millis();
    // Deja lastIntrusionMs en 0 para permitir primer disparo inmediato
    Serial.println(F("Laser KY-008 init — TX:8 HIGH, RX:7 PULLUP, armed=true"));
}

void handleLaser() {
    if (!laserArmed) return;
    unsigned long now = millis();
    if (now - lastCheckMs < LASER_CHECK_INTERVAL_MS) return;
    lastCheckMs = now;

    bool current = digitalRead(LASER_RX_PIN); // HIGH = haz intacto, LOW = interrumpido

    // Detecta flanco HIGH→LOW (haz se rompe). Ignora LOW→HIGH (restauración) y estado estable.
    if (lastBeamState == HIGH && current == LOW) {
        // Cooldown para no spamear si persona queda parada en el haz
        if (now - lastIntrusionMs >= LASER_INTRUSION_COOLDOWN_MS || lastIntrusionMs == 0) {
            triggerIntrusionAlert();
            lastIntrusionMs = now;
        }
    }
    lastBeamState = current;
}

void triggerIntrusionAlert() {
    Serial.println(F("!!! INTRUSION DETECTED — LASER BREAK !!!"));
    // Feedback local inmediato sin red — rojo 3s no bloqueante (led.cpp lo restaura tras duration)
    setLedMode(LedMode::RED_INTRUSION);

    // Envía SECURITY al Gateway → Backend POST /api/security-events → Node-RED → Telegram (HU-02)
    // Payload mínimo HU-02: event_type=intrusion, sensor=laser-01, severity=high
    String payload = String(F("{\"event_type\":\"intrusion\",\"sensor\":\"laser-01\",\"location\":\"entrance\",\"severity\":\"high\",\"timestamp\":")) + String(millis()) + F("}");
    // Construido manual para no usar ArduinoJson aquí (ahorra RAM), Gateway lo reenvía tal cual
    sendSecurityEvent(payload);
}

void setLaserArmed(bool armed) {
    laserArmed = armed;
    digitalWrite(LASER_TX_PIN, armed ? HIGH : LOW);
    if (armed) {
        // Al re-armar, re-lee haz para no disparar falso por transición
        lastBeamState = digitalRead(LASER_RX_PIN);
        Serial.println(F("Laser ARMED (TX HIGH)"));
    } else {
        Serial.println(F("Laser DISARMED (TX LOW)"));
    }
}

bool isLaserArmed() {
    return laserArmed;
}

bool isLaserBeamIntact() {
    return lastBeamState == HIGH;
}
