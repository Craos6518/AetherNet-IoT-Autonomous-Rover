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
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <RF24.h>
#include <SPI.h>

// ============================================================================
// CONFIGURATION
// ============================================================================
#define WIFI_SSID "AetherNet-LAN"
#define WIFI_PASSWORD "changeme"
#define MQTT_BROKER "192.168.1.100"
#define MQTT_PORT 1883
#define MQTT_CLIENT_ID "gateway-esp32"
#define MQTT_USER ""
#define MQTT_PASS ""

// nRF24L01 pins — CSN 15 corrige colisión SCK 18 (DEVOPS-05 reapertura 2026-09-01)
#define NRF_CE_PIN 5
#define NRF_CSN_PIN 15

// UART to MEGA
#define MEGA_SERIAL Serial2
#define MEGA_BAUD 115200

// Topics
#define TOPIC_ROVER_CMD "aethernet/rover/command"
#define TOPIC_ROVER_TELEMETRY "aethernet/rover/telemetry"
#define TOPIC_ACCESS_CMD "aethernet/access/command"
#define TOPIC_ACCESS_EVENT "aethernet/access/event"
#define TOPIC_SECURITY_EVENT "aethernet/seguridad/intrusion"
#define TOPIC_RELAY_CMD "aethernet/relay/+"
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

    // Initialize nRF24L01
    if (!radio.begin()) {
        Serial.println("ERROR: nRF24L01 not detected!");
        while (1) delay(1000);
    }
    radio.setPALevel(RF24_PA_HIGH);
    radio.setDataRate(RF24_2MBPS);
    radio.setChannel(76);
    radio.openWritingPipe(roverAddress);
    radio.openReadingPipe(1, gatewayAddress);
    radio.enableAckPayload();
    radio.startListening();
    Serial.println("nRF24L01 initialized");

    // Connect WiFi
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println();
    Serial.printf("WiFi connected: %s\n", WiFi.localIP().toString().c_str());

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
    String topicStr = String(topic);
    String payloadStr = String((char*)payload);

    Serial.printf("MQTT RX: %s -> %s\n", topicStr.c_str(), payloadStr.c_str());

    if (topicStr == TOPIC_ROVER_CMD) {
        handleRoverCommand(payloadStr);
    } else if (topicStr == TOPIC_ACCESS_CMD) {
        handleAccessCommand(payloadStr);
    } else if (topicStr.startsWith("aethernet/relay/")) {
        handleRelayCommand(topicStr, payloadStr);
    }
}

void mqttReconnect() {
    Serial.print("MQTT reconnecting...");
    while (!mqttClient.connected()) {
        if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASS)) {
            Serial.println("connected");
            // Subscribe to topics
            mqttClient.subscribe(TOPIC_ROVER_CMD);
            mqttClient.subscribe(TOPIC_ACCESS_CMD);
            mqttClient.subscribe("aethernet/relay/+");
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
    if (MEGA_SERIAL.available()) {
        String line = MEGA_SERIAL.readStringUntil('\n');
        line.trim();
        if (line.length() > 0) {
            processMegaMessage(line);
        }
    }
}

void processMegaMessage(String msg) {
    // Expected format: TYPE:PAYLOAD
    // e.g., ACCESS:{"user":"admin","success":true}
    //       SECURITY:{"type":"intrusion","sensor":"laser-01"}
    //       RELAY:{"id":1,"state":true}

    int colonIdx = msg.indexOf(':');
    if (colonIdx < 0) return;

    String type = msg.substring(0, colonIdx);
    String payload = msg.substring(colonIdx + 1);

    if (type == "ACCESS") {
        mqttClient.publish(TOPIC_ACCESS_EVENT, payload.c_str());
    } else if (type == "SECURITY") {
        mqttClient.publish(TOPIC_SECURITY_EVENT, payload.c_str());
    } else if (type == "RELAY") {
        mqttClient.publish("aethernet/relay/event", payload.c_str());
    } else if (type == "STATUS") {
        // Forward MEGA status
        mqttClient.publish("aethernet/mega/status", payload.c_str());
    }
}

void handleAccessCommand(String payload) {
    // Forward to MEGA via UART
    MEGA_SERIAL.println("CMD:ACCESS:" + payload);
}

void handleRelayCommand(String topic, String payload) {
    // Extract relay ID from topic: aethernet/relay/1
    int lastSlash = topic.lastIndexOf('/');
    String relayId = topic.substring(lastSlash + 1);

    StaticJsonDocument<128> doc;
    doc["id"] = relayId.toInt();
    DeserializationError err = deserializeJson(doc, payload);
    if (!err && doc.containsKey("state")) {
        String cmd = "CMD:RELAY:" + String(doc["id"].as<int>()) + ":" + String(doc["state"].as<bool>());
        MEGA_SERIAL.println(cmd);
    }
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