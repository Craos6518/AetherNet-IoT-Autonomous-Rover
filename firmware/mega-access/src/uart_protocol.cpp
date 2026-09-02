#include "uart_protocol.h"
#include "config.h"
#include "door.h"
#include "keypad_control.h"
#include <ArduinoJson.h>

static unsigned long lastStatusMs = 0;

void uartInit() {
    GATEWAY_SERIAL.begin(GATEWAY_BAUD);
    lastStatusMs = millis();
}

void handleGatewayUart() {
    while (GATEWAY_SERIAL.available()) {
        String line = GATEWAY_SERIAL.readStringUntil('\n');
        line.trim();
        if (line.length() == 0) continue;
        processGatewayCommand(line);
    }
}

void processGatewayCommand(const String& cmd) {
    // Formato: CMD:TYPE:PARAMS  |  CMD:STATUS sin params
    if (!cmd.startsWith(F("CMD:"))) return;

    int firstColon = cmd.indexOf(':');
    int secondColon = cmd.indexOf(':', firstColon + 1);
    String type;
    String params;
    if (secondColon < 0) {
        // CMD:STATUS sin segundo ':'
        type = cmd.substring(firstColon + 1);
        params = "";
    } else {
        type = cmd.substring(firstColon + 1, secondColon);
        params = cmd.substring(secondColon + 1);
    }

    if (type == "ACCESS") {
        StaticJsonDocument<128> doc;
        DeserializationError err = deserializeJson(doc, params);
        if (!err && doc.containsKey("pin")) {
            String pin = doc["pin"].as<String>();
            pin.trim();
            processPinAttempt(pin);
        } else {
            Serial.print(F("CMD:ACCESS bad json: "));
            Serial.println(params);
        }
    } else if (type == "STATUS") {
        sendStatusToGateway();
    } else {
        // RELAY/LASER eliminados en esta rama — ignorar silenciosamente
        Serial.print(F("CMD ignored (unknown type): "));
        Serial.println(type);
    }
}

void sendAccessEvent(const String& jsonPayload) {
    GATEWAY_SERIAL.print(F("ACCESS:"));
    GATEWAY_SERIAL.println(jsonPayload);
    // Debug USB para confirmar TX
    Serial.print(F("[UART TX] ACCESS:"));
    Serial.println(jsonPayload);
}

void sendStatusToGateway() {
    StaticJsonDocument<256> doc;
    doc["door_locked"] = !isDoorUnlocked();
    doc["laser_armed"] = false; // reservado para mega-laser, fijo false aquí
    doc["free_ram"] = freeMemory();
    doc["uptime_ms"] = millis();

    String payload;
    serializeJson(doc, payload);
    GATEWAY_SERIAL.print(F("STATUS:"));
    GATEWAY_SERIAL.println(payload);
}

void sendPeriodicStatus() {
    unsigned long now = millis();
    if (now - lastStatusMs >= STATUS_INTERVAL_MS) {
        sendStatusToGateway();
        lastStatusMs = now;
    }
}

int freeMemory() {
    extern int __heap_start, *__brkval;
    int v;
    return (int)&v - (__brkval == 0 ? (int)&__heap_start : (int)__brkval);
}
