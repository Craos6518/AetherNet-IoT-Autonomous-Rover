/*
 * AetherNet - Gateway ESP32
 * Central gateway: MQTT/WebSockets ↔ UART (MEGA) ↔ RF nRF24L01 (Rover)
 * 
 * Hardware: ESP32-WROOM-32U + nRF24L01 (SPI)
 * 
 * Pinout:
 * nRF24L01: CE=GPIO5, CSN=GPIO18, SCK=GPIO18, MOSI=GPIO23, MISO=GPIO19
 * UART to MEGA: TX=GPIO17 (RX2), RX=GPIO16 (TX2)
 */

#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <RF24.h>
#include <SPI.h>

// ============================================================================
// CONFIGURATION — credenciales en secrets.h (DEVOPS-11, PM-08)
// Ver firmware/gateway-esp32/secrets.h.example; secrets.h está gitignoreado.
// CI compila sin secrets.h usando defaults sanos (AetherNet-LAN/changeme).
// ============================================================================
#if __has_include("secrets.h")
#include "secrets.h"
#endif
#ifndef WIFI_SSID
#define WIFI_SSID "AetherNet-LAN"
#endif
#ifndef WIFI_PASSWORD
#define WIFI_PASSWORD "changeme"
#endif
#ifndef MQTT_BROKER
#define MQTT_BROKER "192.168.1.100"
#endif
#ifndef MQTT_PORT
#define MQTT_PORT 1883
#endif
#ifndef MQTT_CLIENT_ID
#define MQTT_CLIENT_ID "gateway-esp32"
#endif
#ifndef MQTT_USER
#define MQTT_USER ""
#endif
#ifndef MQTT_PASS
#define MQTT_PASS ""
#endif
#ifndef BACKEND_HOST
#define BACKEND_HOST "192.168.1.100"
#endif
#ifndef BACKEND_PORT
#define BACKEND_PORT 8000
#endif
#ifndef BACKEND_ACCESS_PATH
#define BACKEND_ACCESS_PATH "/api/access-events"
#endif

// nRF24L01 pins — CSN 15 corrige colisión SCK 18 (DEVOPS-05 reapertura 2026-09-01)
#define NRF_CE_PIN 5
#define NRF_CSN_PIN 15

// UART to MEGA — 38400 + divisor 1k/2k MEGA16->ESP32_16, GND común (HU-01)
#define MEGA_SERIAL Serial2
#define MEGA_BAUD 38400

// Topics
#define TOPIC_ROVER_CMD "aethernet/rover/command"
#define TOPIC_ROVER_TELEMETRY "aethernet/rover/telemetry"
#define TOPIC_ACCESS_CMD "aethernet/access/command"
#define TOPIC_ACCESS_EVENT "aethernet/access/event" // solo para debug/fallback, el flujo principal es HTTP POST
#define TOPIC_SECURITY_EVENT "aethernet/seguridad/intrusion"
#define TOPIC_SYSTEM_STATUS "aethernet/system/status"

// ============================================================================
// GLOBALS
// ============================================================================
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
RF24 radio(NRF_CE_PIN, NRF_CSN_PIN);

const byte roverAddress[6] = "ROVER";
const byte gatewayAddress[6] = "GATEW";

unsigned long lastMqttReconnect = 0;
unsigned long lastStatusPublish = 0;
unsigned long lastRfCheck = 0;

// RF packet structure
#pragma pack(push, 1)
struct RoverCommand {
    int16_t left_pwm;
    int16_t right_pwm;
    uint8_t mode; // 0=stop, 1=manual, 2=auto
    uint16_t checksum;
};

struct RoverTelemetry {
    int16_t left_pwm;
    int16_t right_pwm;
    uint16_t ultrasonic_cm;
    bool ir_left;
    bool ir_center;
    bool ir_right;
    int8_t rf_rssi;
    uint16_t checksum;
};
#pragma pack(pop)

// ============================================================================
// SETUP
// ============================================================================
void setup() {
    Serial.begin(115200);
    while (!Serial) delay(10);
    Serial.println("\n=== AetherNet Gateway ESP32 Starting ===");

    // Initialize UART to MEGA
    MEGA_SERIAL.begin(MEGA_BAUD, SERIAL_8N1, 16, 17); // RX=16, TX=17
    Serial.println("UART to MEGA initialized");

    // Initialize nRF24L01 — no bloqueante para HU-01 (cerrojo) sin rover
    if (!radio.begin()) {
        Serial.println("WARN: nRF24L01 not detected! RF rover deshabilitado, HU-01 sigue via UART/HTTP");
        // no while(1) — permite que WiFi/MQTT/HTTP sigan para cerrojo
    } else {
        radio.setPALevel(RF24_PA_HIGH);
        radio.setDataRate(RF24_2MBPS);
        radio.setChannel(76);
        radio.openWritingPipe(roverAddress);
        radio.openReadingPipe(1, gatewayAddress);
        radio.enableAckPayload();
        radio.startListening();
        Serial.println("nRF24L01 initialized");
    }

    // Connect WiFi — no bloqueante para prueba (timeout 10s)
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to WiFi ");
    Serial.print(WIFI_SSID);
    unsigned long wifiStart = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - wifiStart < 10000) {
        delay(500);
        Serial.print(".");
    }
    Serial.println();
    if (WiFi.status() == WL_CONNECTED) {
        Serial.printf("WiFi connected: %s\n", WiFi.localIP().toString().c_str());
    } else {
        Serial.println("WiFi NOT connected — continuando sin WiFi (HU-01 via UART seguirá, HTTP reintentará en loop)");
    }

    // MQTT setup
    mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
    mqttClient.setCallback(mqttCallback);
    mqttClient.setBufferSize(1024);

    // Publish startup status
    publishSystemStatus("online");
}

void loop() {
    // MQTT connection maintenance
    if (!mqttClient.connected()) {
        if (millis() - lastMqttReconnect > 5000) {
            mqttReconnect();
            lastMqttReconnect = millis();
        }
    } else {
        mqttClient.loop();
    }

    // Handle RF communication with Rover
    handleRfCommunication();

    // Handle UART communication with MEGA
    handleMegaUart();

    // Periodic status publish
    if (millis() - lastStatusPublish > 30000) {
        publishSystemStatus("heartbeat");
        lastStatusPublish = millis();
    }

    delay(10);
}

// ============================================================================
// MQTT CALLBACKS
// ============================================================================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
    // Construir payloadStr con length explícito (sin asumir null-terminated)
    String topicStr = String(topic);
    String payloadStr;
    payloadStr.reserve(length + 1);
    for (unsigned int i = 0; i < length; i++) payloadStr += (char)payload[i];

    Serial.printf("MQTT RX: %s -> %s\n", topicStr.c_str(), payloadStr.c_str());

    if (topicStr == TOPIC_ROVER_CMD) {
        handleRoverCommand(payloadStr);
    } else if (topicStr == TOPIC_ACCESS_CMD) {
        handleAccessCommand(payloadStr);
    }
}

void mqttReconnect() {
    Serial.print("MQTT reconnecting...");
    while (!mqttClient.connected()) {
        if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASS)) {
            Serial.println("connected");
            // Subscribe solo a topics vigentes (relés eliminados)
            mqttClient.subscribe(TOPIC_ROVER_CMD);
            mqttClient.subscribe(TOPIC_ACCESS_CMD);
            mqttClient.subscribe("aethernet/system/command");
        } else {
            Serial.printf("failed, rc=%d retry in 5s\n", mqttClient.state());
            delay(5000);
        }
    }
}

// ============================================================================
// ROVER RF COMMUNICATION
// ============================================================================
void handleRfCommunication() {
    // Check for incoming telemetry from Rover
    if (radio.available()) {
        RoverTelemetry telem;
        radio.read(&telem, sizeof(telem));

        if (verifyChecksum(telem)) {
            publishRoverTelemetry(telem);
            lastRfCheck = millis();
        }
    }

    // Send ACK payload if needed (handled automatically by RF24 with ack payload)
}

void handleRoverCommand(String payload) {
    StaticJsonDocument<256> doc;
    DeserializationError err = deserializeJson(doc, payload);
    if (err) {
        Serial.printf("JSON parse error: %s\n", err.c_str());
        return;
    }

    RoverCommand cmd;
    cmd.left_pwm = doc["left_pwm"] | 0;
    cmd.right_pwm = doc["right_pwm"] | 0;
    cmd.mode = doc["mode"] | 0;
    cmd.checksum = calculateChecksum(cmd);

    // Send via RF
    radio.stopListening();
    bool ok = radio.write(&cmd, sizeof(cmd));
    radio.startListening();

    if (ok) {
        Serial.printf("RF TX: L=%d R=%d mode=%d\n", cmd.left_pwm, cmd.right_pwm, cmd.mode);
    } else {
        Serial.println("RF TX failed");
    }
}

void publishRoverTelemetry(const RoverTelemetry& telem) {
    StaticJsonDocument<512> doc;
    doc["left_pwm"] = telem.left_pwm;
    doc["right_pwm"] = telem.right_pwm;
    doc["ultrasonic_cm"] = telem.ultrasonic_cm;
    doc["ir_left"] = telem.ir_left;
    doc["ir_center"] = telem.ir_center;
    doc["ir_right"] = telem.ir_right;
    doc["rf_rssi"] = telem.rf_rssi;
    doc["timestamp"] = millis();

    String output;
    serializeJson(doc, output);
    mqttClient.publish(TOPIC_ROVER_TELEMETRY, output.c_str());
}

// ============================================================================
// MEGA UART COMMUNICATION
// ============================================================================
void handleMegaUart() {
    while (MEGA_SERIAL.available()) {
        String line = MEGA_SERIAL.readStringUntil('\n');
        line.trim();
        if (line.length() == 0) continue;
        Serial.printf("[MEGA UART RX] %s\n", line.c_str());
        processMegaMessage(line);
    }
    // Heartbeat si no hay datos 10s (debug cableado)
    static unsigned long lastDbg = 0;
    if (millis() - lastDbg > 10000) {
        lastDbg = millis();
        if (!MEGA_SERIAL.available()) {
            // Serial.printf("[MEGA UART] idle — verifica TX 17↔RX16, GND común, 115200\n");
        }
    }
}

void forwardAccessToBackend(String payload);

void processMegaMessage(String msg) {
    // Expected format: TYPE:PAYLOAD
    // e.g., ACCESS:{"user_id":"keypad_user","pin_hash":"abc","success":true,"source":"keypad"}
    //       SECURITY:{"event_type":"intrusion","sensor":"laser-01"} (reservado mega-laser)
    //       STATUS:{"door_locked":true,...}

    int colonIdx = msg.indexOf(':');
    if (colonIdx < 0) return;

    String type = msg.substring(0, colonIdx);
    String payload = msg.substring(colonIdx + 1);

    if (type == "ACCESS") {
        forwardAccessToBackend(payload);
        // Fallback debug: también publica por MQTT si hay listeners
        if (mqttClient.connected()) {
            mqttClient.publish(TOPIC_ACCESS_EVENT, payload.c_str());
        }
    } else if (type == "SECURITY") {
        mqttClient.publish(TOPIC_SECURITY_EVENT, payload.c_str());
    } else if (type == "STATUS") {
        // Forward MEGA status
        mqttClient.publish("aethernet/mega/status", payload.c_str());
    }
}

void handleAccessCommand(String payload) {
    // Forward to MEGA via UART — payload debe contener {"pin":"1234"}
    // Validación mínima antes de reenviar
    StaticJsonDocument<128> tmp;
    if (deserializeJson(tmp, payload) || !tmp.containsKey("pin")) {
        Serial.printf("CMD:ACCESS bad payload (sin pin): %s\n", payload.c_str());
        return;
    }
    MEGA_SERIAL.println("CMD:ACCESS:" + payload);
}

void forwardAccessToBackend(String payload) {
    // Flujo principal HU-01: HTTP POST directo a FastAPI — más rápido que MQTT bridge
    // Si WiFi caído, solo se encola por MQTT fallback (arriba)
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("Backend POST skipped — WiFi down (MQTT fallback only)");
        return;
    }

    // Normaliza payload al schema AccessEventCreate (user_id, pin_hash, success, source)
    // El payload del MEGA ya trae esos campos; si viene incompleto se descarta
    StaticJsonDocument<256> doc;
    DeserializationError err = deserializeJson(doc, payload);
    if (err) {
        Serial.printf("ACCESS payload JSON error: %s\n", err.c_str());
        return;
    }
    if (!doc.containsKey("user_id") || !doc.containsKey("pin_hash") || !doc.containsKey("success")) {
        Serial.printf("ACCESS payload missing fields: %s\n", payload.c_str());
        return;
    }

    // Construir body mínimo permitido por backend (ignora timestamp/source extra)
    StaticJsonDocument<256> body;
    body["user_id"] = doc["user_id"].as<const char*>();
    body["pin_hash"] = doc["pin_hash"].as<const char*>();
    body["success"] = doc["success"].as<bool>();
    body["source"] = doc.containsKey("source") ? doc["source"].as<const char*>() : "keypad";

    String jsonBody;
    serializeJson(body, jsonBody);

    HTTPClient http;
    String url = String("http://") + BACKEND_HOST + ":" + String(BACKEND_PORT) + BACKEND_ACCESS_PATH;
    http.begin(url);
    http.addHeader("Content-Type", "application/json");
    http.setTimeout(3000);
    int code = http.POST(jsonBody);
    if (code == 200 || code == 201) {
        Serial.printf("Backend ACCESS POST ok (%d): %s\n", code, jsonBody.c_str());
    } else {
        Serial.printf("Backend ACCESS POST fail code=%d url=%s body=%s\n", code, url.c_str(), jsonBody.c_str());
        if (code > 0) Serial.println(http.getString());
    }
    http.end();
}

// ============================================================================
// SYSTEM STATUS
// ============================================================================
void publishSystemStatus(const char* status) {
    StaticJsonDocument<256> doc;
    doc["status"] = status;
    doc["wifi_rssi"] = WiFi.RSSI();
    doc["free_heap"] = ESP.getFreeHeap();
    doc["uptime_ms"] = millis();
    doc["rf_connected"] = (millis() - lastRfCheck < 5000);

    String output;
    serializeJson(doc, output);
    mqttClient.publish(TOPIC_SYSTEM_STATUS, output.c_str());
}

// ============================================================================
// UTILITIES
// ============================================================================
uint16_t calculateChecksum(const RoverCommand& cmd) {
    uint16_t sum = 0;
    const uint8_t* bytes = (const uint8_t*)&cmd;
    for (size_t i = 0; i < sizeof(cmd) - 2; i++) {
        sum += bytes[i];
    }
    return sum;
}

bool verifyChecksum(const RoverTelemetry& telem) {
    uint16_t sum = 0;
    const uint8_t* bytes = (const uint8_t*)&telem;
    for (size_t i = 0; i < sizeof(telem) - 2; i++) {
        sum += bytes[i];
    }
    return sum == telem.checksum;
}