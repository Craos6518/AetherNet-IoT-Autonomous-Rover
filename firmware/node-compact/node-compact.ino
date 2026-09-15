/*
 * =============================================================================
 * node-compact.ino — Nodo Acceso Compacto (Arduino Nano + HC-06 + FC-51) | MOV-07 RF-1.3
 * Autor: Andres Felipe Martinez Henao | 6º Semestre UTP
 * Protocolo: Texto TYPE:JSON\n 9600 bauds (HC-06 stock)
 * Pines:
 *   - HC-06 TX -> Pin 2 (SoftwareSerial RX)
 *   - HC-06 RX -> Pin 3 (SoftwareSerial TX)
 *   - FC-51    -> Pin 5 (Sensor presencia con INPUT_PULLUP)
 * =============================================================================
 */

#include <SoftwareSerial.h>
#include <ArduinoJson.h>

const int BT_RX_PIN = 2;    // RX SoftwareSerial (conecta a TX del HC-06)
const int BT_TX_PIN = 3;    // TX SoftwareSerial (conecta a RX del HC-06)
const int FC_PIN = 5;       // Sensor infrarrojo FC-51

SoftwareSerial btSerial(BT_RX_PIN, BT_TX_PIN);

bool lastPresence = false;
unsigned long lastPresenceTime = 0;
String inputBuffer = "";

void setup() {
    Serial.begin(9600);
    btSerial.begin(9600); // HC-06 stock a 9600 bauds (sin configuracion AT)

    pinMode(FC_PIN, INPUT_PULLUP);

    Serial.println(F("[INFO] Nodo Compacto Nano + HC-06 (9600) + FC-51 iniciado"));
}

void loop() {
    // 1. Lectura Bluetooth con eco y buffer de línea
    while (btSerial.available()) {
        char c = (char)btSerial.read();
        Serial.print(c);
        
        if (c == '\n' || c == '\r') {
            inputBuffer.trim();
            if (inputBuffer.length() > 0) {
                Serial.print(F("\n[BT PARSE] "));
                Serial.println(inputBuffer);
                processCommand(inputBuffer);
                inputBuffer = "";
            }
        } else {
            inputBuffer += c;
            if (inputBuffer.length() > 128) inputBuffer = "";
        }
    }

    // 2. Lectura USB Serial local
    while (Serial.available()) {
        String line = Serial.readStringUntil('\n');
        line.trim();
        if (line.length() > 0) {
            Serial.print(F("[USB RX] "));
            Serial.println(line);
            processCommand(line);
        }
    }

    // 3. Sensor FC-51
    int presenceVal = digitalRead(FC_PIN);
    bool currentPresence = (presenceVal == LOW);
    if (currentPresence != lastPresence && millis() - lastPresenceTime > 2000) {
        lastPresence = currentPresence;
        lastPresenceTime = millis();
        
        StaticJsonDocument<128> doc;
        doc["presence"] = currentPresence;
        doc["node"] = "compact_access";
        String payload;
        serializeJson(doc, payload);

        btSerial.print(F("PRESENCE:"));
        btSerial.println(payload);
        Serial.print(F("\n[BT TX] PRESENCE:"));
        Serial.println(payload);
    }
}

void processCommand(String cmd) {
    cmd.trim();
    if (cmd.length() == 0) return;

    if (cmd.length() >= 4 && cmd.length() <= 6 && !cmd.startsWith(F("CMD:"))) {
        validateAndRespond(cmd);
        return;
    }

    if (!cmd.startsWith(F("CMD:"))) return;

    int firstColon = cmd.indexOf(':');
    int secondColon = cmd.indexOf(':', firstColon + 1);
    String type;
    String params;

    if (secondColon < 0) {
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
            validateAndRespond(pin);
        } else {
            validateAndRespond(params);
        }
    } else if (type == "STATUS") {
        StaticJsonDocument<128> statusDoc;
        statusDoc["status"] = "online";
        statusDoc["node"] = "compact_access";
        statusDoc["uptime"] = millis();
        
        String payload;
        serializeJson(statusDoc, payload);
        btSerial.print(F("STATUS:"));
        btSerial.println(payload);
        Serial.print(F("\n[BT TX] STATUS:"));
        Serial.println(payload);
    }
}

void validateAndRespond(const String& pin) {
    bool success = (pin == "1234" || (pin.length() >= 4 && pin.length() <= 6));

    StaticJsonDocument<128> resDoc;
    resDoc["success"] = success;
    resDoc["user_id"] = success ? "bt_user" : "unknown";
    resDoc["message"] = success ? "Door unlocked via Bluetooth SPP" : "Invalid PIN";
    
    String resPayload;
    serializeJson(resDoc, resPayload);

    btSerial.print(F("ACCESS:"));
    btSerial.println(resPayload);
    Serial.print(F("\n[BT TX] ACCESS:"));
    Serial.println(resPayload);
}
