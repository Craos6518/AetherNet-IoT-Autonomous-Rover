#include "keypad_control.h"
#include "config.h"
#include "door.h"
#include "led.h"
#include "uart_protocol.h"
#include <Keypad.h>
#include <ArduinoJson.h>

// Keypad wiring
static char keys[4][4] = {
    {'1','2','3','A'},
    {'4','5','6','B'},
    {'7','8','9','C'},
    {'*','0','#','D'}
};
static Keypad keypad = Keypad(makeKeymap(keys), (byte*)ROW_PINS, (byte*)COL_PINS, 4, 4);

// Buffer de PIN ingresado
static String inputBuffer = "";

void keypadInit() {
    keypad.setDebounceTime(KEYPAD_DEBOUNCE_MS);
    keypad.setHoldTime(KEYPAD_HOLD_MS);
}

void handleKeypad() {
    char key = keypad.getKey();
    if (!key) return;

    // DEBUG mapeo: imprime crudo siempre para validar cableado
    Serial.print(F("[KEY RAW] '"));
    Serial.print(key);
    Serial.print(F("' code="));
    Serial.print((int)key);
    Serial.println(F(""));

    if (key == '#') {
        // DEBUG: muestra buffer antes de procesar (temporal para validar mapeo)
        Serial.print(F("[PIN DBG] buffer='"));
        Serial.print(inputBuffer);
        Serial.print(F("' len="));
        Serial.print(inputBuffer.length());
        Serial.println(F(""));
        processPinAttempt(inputBuffer);
        inputBuffer = "";
    } else if (key == '*') {
        inputBuffer = "";
        setLedMode(LedMode::RED_CLEAR);
        Serial.println(F("Input cleared (*)"));
    } else if (isDigit(key)) {
        if (inputBuffer.length() < PIN_MAX_LEN) {
            inputBuffer += key;
            setLedMode(LedMode::BLUE_TAP);
            Serial.print(F("Key pressed: * (len="));
            Serial.print(inputBuffer.length());
            Serial.println(F(")"));
        } else {
            Serial.println(F("PIN buffer full (6) — digit ignored"));
        }
    } else {
        // A, B, C, D explícitamente ignoradas (requisito)
        Serial.print(F("Key ignored (A-D): "));
        Serial.println(key);
    }
}

void processPinAttempt(const String& pin) {
    bool success = (pin == String(VALID_PIN));

    if (success) {
        unlockDoor(); // pone LED verde y timer 5s
    } else {
        // Si la puerta ya estaba desbloqueada, mantener verde tras el flash rojo.
        // setLedMode(RED_FAIL) hará overlay 1s y updateLed() restaurará verde/OFF.
        setLedMode(LedMode::RED_FAIL);
        Serial.print(F("PIN attempt FAILED (len="));
        Serial.print(pin.length());
        Serial.println(F(")"));
    }

    // Evento hacia Gateway — JSON sin PIN claro, solo hash
    StaticJsonDocument<256> doc;
    doc["user_id"] = "keypad_user";
    doc["pin_hash"] = hashPin(pin);
    doc["success"] = success;
    doc["timestamp"] = millis();
    doc["source"] = "keypad";

    String payload;
    serializeJson(doc, payload);
    sendAccessEvent(payload);

    Serial.print(F("PIN attempt: len="));
    Serial.print(pin.length());
    Serial.print(F(" -> "));
    Serial.println(success ? F("GRANTED") : F("DENIED"));
}

String hashPin(const String& pin) {
    // djb2 — no criptográfico, solo para log sin exponer PIN
    unsigned long hash = 5381;
    for (size_t i = 0; i < pin.length(); i++) {
        hash = ((hash << 5) + hash) + (uint8_t)pin[i];
    }
    String out;
    out.reserve(8);
    out = String(hash, HEX);
    return out;
}
