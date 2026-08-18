/*
 * AetherNet - MEGA Access Control
 * Handles: 4x4 Keypad, Servo MG90S (door lock), KY-008 Laser, LED RGB, Relays
 * Communicates with Gateway ESP32 via UART
 * 
 * Hardware: Arduino MEGA 2560
 * Pinout:
 * Keypad: Rows 22,24,26,28 | Cols 30,32,34,36
 * Servo: Pin 9 (PWM)
 * Laser KY-008: Pin 8 (digital out), Pin 7 (digital in - receiver)
 * LED RGB: Pins 44(R), 45(G), 46(B) - PWM
 * Relays: Pins 40-47 (8 relays)
 * UART to ESP32: Serial2 (RX=16, TX=17)
 */

#include <Arduino.h>
#include <Keypad.h>
#include <Servo.h>

// ============================================================================
// PIN DEFINITIONS
// ============================================================================
// Keypad 4x4
const byte ROW_PINS[4] = {22, 24, 26, 28};
const byte COL_PINS[4] = {30, 32, 34, 36};
char keys[4][4] = {
    {'1','2','3','A'},
    {'4','5','6','B'},
    {'7','8','9','C'},
    {'*','0','#','D'}
};
Keypad keypad = Keypad(makeKeymap(keys), ROW_PINS, COL_PINS, 4, 4);

// Servo (door lock)
#define SERVO_PIN 9
Servo doorServo;
const int SERVO_LOCKED = 0;    // 0 degrees = locked
const int SERVO_UNLOCKED = 90; // 90 degrees = unlocked

// Laser tripwire (KY-008)
#define LASER_TX_PIN 8   // Laser emitter
#define LASER_RX_PIN 7   // Photoresistor/receiver

// LED RGB (local feedback)
#define LED_R_PIN 44
#define LED_G_PIN 45
#define LED_B_PIN 46

// Relay matrix (8 channels)
#define RELAY_START_PIN 40
const int RELAY_PINS[8] = {40, 41, 42, 43, 44, 45, 46, 47}; // Note: 44-46 conflict with LED, adjust if needed

// UART to ESP32 Gateway
#define GATEWAY_SERIAL Serial2
#define GATEWAY_BAUD 115200

// ============================================================================
// CONFIGURATION
// ============================================================================
const char* VALID_PIN = "1234"; // TODO: Move to EEPROM/secure storage
const unsigned long LASER_CHECK_INTERVAL = 50; // ms
const unsigned long DOOR_AUTO_LOCK_MS = 5000; // Auto-lock after 5s
const unsigned long UART_SEND_INTERVAL = 100; // ms

// ============================================================================
// STATE VARIABLES
// ============================================================================
String inputBuffer = "";
bool doorUnlocked = false;
unsigned long doorUnlockTime = 0;
bool laserArmed = true;
bool lastLaserState = true; // HIGH = beam intact
unsigned long lastLaserCheck = 0;
unsigned long lastUartSend = 0;
bool relayStates[8] = {false};

// ============================================================================
// SETUP
// ============================================================================
void setup() {
    Serial.begin(115200);
    while (!Serial) delay(10);
    Serial.println("\n=== AetherNet MEGA Access Control Starting ===");

    // Initialize pins
    pinMode(LASER_TX_PIN, OUTPUT);
    pinMode(LASER_RX_PIN, INPUT_PULLUP);
    digitalWrite(LASER_TX_PIN, HIGH); // Laser ON

    pinMode(LED_R_PIN, OUTPUT);
    pinMode(LED_G_PIN, OUTPUT);
    pinMode(LED_B_PIN, OUTPUT);
    setLedColor(0, 0, 0); // Off initially

    for (int i = 0; i < 8; i++) {
        pinMode(RELAY_PINS[i], OUTPUT);
        digitalWrite(RELAY_PINS[i], HIGH); // Relays active LOW
    }

    // Initialize servo
    doorServo.attach(SERVO_PIN);
    doorServo.write(SERVO_LOCKED);
    doorUnlocked = false;

    // Initialize UART to Gateway
    GATEWAY_SERIAL.begin(GATEWAY_BAUD, SERIAL_8N1, 16, 17);

    // Keypad setup
    keypad.setDebounceTime(50);
    keypad.setHoldTime(500);

    // Send ready status
    sendStatusToGateway();
    setLedColor(0, 255, 0); // Green = ready
    delay(500);
    setLedColor(0, 0, 0);

    Serial.println("MEGA Access Control ready");
}

// ============================================================================
// MAIN LOOP
// ============================================================================
void loop() {
    handleKeypad();
    handleLaser();
    handleDoorAutoLock();
    handleGatewayUart();
    sendPeriodicStatus();
}

// ============================================================================
// KEYPAD HANDLING
// ============================================================================
void handleKeypad() {
    char key = keypad.getKey();
    if (!key) return;

    if (key == '#') {
        processPinAttempt(inputBuffer);
        inputBuffer = "";
    } else if (key == '*') {
        inputBuffer = ""; // Clear
        setLedColor(255, 0, 0); // Red flash for clear
        delay(100);
        setLedColor(0, 0, 0);
    } else if (isDigit(key)) {
        if (inputBuffer.length() < 6) { // Max 6 digits
            inputBuffer += key;
            // Visual feedback per keypress
            setLedColor(0, 0, 255); // Blue
            delay(50);
            setLedColor(0, 0, 0);
        }
    }
}

void processPinAttempt(String pin) {
    bool success = (pin == VALID_PIN);

    // Visual feedback
    if (success) {
        setLedColor(0, 255, 0); // Green
        unlockDoor();
    } else {
        setLedColor(255, 0, 0); // Red
        delay(1000);
        setLedColor(0, 0, 0);
    }

    // Send event to Gateway
    StaticJsonDocument<256> doc;
    doc["user_id"] = "keypad_user";
    doc["pin_hash"] = hashPin(pin);
    doc["success"] = success;
    doc["timestamp"] = millis();
    doc["source"] = "keypad";

    String payload;
    serializeJson(doc, payload);
    GATEWAY_SERIAL.println("ACCESS:" + payload);

    // Log locally
    Serial.printf("PIN attempt: %s -> %s\n", pin.c_str(), success ? "GRANTED" : "DENIED");
}

String hashPin(String pin) {
    // Simple hash for logging (not cryptographic)
    unsigned long hash = 5381;
    for (char c : pin) {
        hash = ((hash << 5) + hash) + c;
    }
    return String(hash, HEX);
}

void unlockDoor() {
    doorServo.write(SERVO_UNLOCKED);
    doorUnlocked = true;
    doorUnlockTime = millis();
    Serial.println("Door UNLOCKED");
}

void lockDoor() {
    doorServo.write(SERVO_LOCKED);
    doorUnlocked = false;
    Serial.println("Door LOCKED");
}

void handleDoorAutoLock() {
    if (doorUnlocked && (millis() - doorUnlockTime > DOOR_AUTO_LOCK_MS)) {
        lockDoor();
    }
}

// ============================================================================
// LASER TRIPWIRE
// ============================================================================
void handleLaser() {
    if (!laserArmed) return;

    if (millis() - lastLaserCheck < LASER_CHECK_INTERVAL) return;
    lastLaserCheck = millis();

    bool currentState = digitalRead(LASER_RX_PIN); // HIGH = beam intact

    // Detect beam break (transition from HIGH to LOW)
    if (lastLaserState == HIGH && currentState == LOW) {
        triggerIntrusionAlert();
    }

    lastLaserState = currentState;
}

void triggerIntrusionAlert() {
    Serial.println("!!! INTRUSION DETECTED - LASER BREAK !!!");

    // Local visual feedback - RED
    setLedColor(255, 0, 0);

    // Send to Gateway
    StaticJsonDocument<256> doc;
    doc["event_type"] = "intrusion";
    doc["sensor"] = "laser-01";
    doc["location"] = "entrance";
    doc["severity"] = "high";
    doc["timestamp"] = millis();

    String payload;
    serializeJson(doc, payload);
    GATEWAY_SERIAL.println("SECURITY:" + payload);

    // Keep red for 3 seconds
    delay(3000);
    setLedColor(0, 0, 0);
}

void setLaserArmed(bool armed) {
    laserArmed = armed;
    if (armed) {
        digitalWrite(LASER_TX_PIN, HIGH);
        Serial.println("Laser ARMED");
    } else {
        digitalWrite(LASER_TX_PIN, LOW);
        Serial.println("Laser DISARMED");
    }
}

// ============================================================================
// LED RGB CONTROL
// ============================================================================
void setLedColor(int r, int g, int b) {
    analogWrite(LED_R_PIN, r);
    analogWrite(LED_G_PIN, g);
    analogWrite(LED_B_PIN, b);
}

// ============================================================================
// RELAY CONTROL
// ============================================================================
void setRelay(int index, bool state) {
    if (index < 0 || index >= 8) return;
    relayStates[index] = state;
    digitalWrite(RELAY_PINS[index], state ? LOW : HIGH); // Active LOW
    Serial.printf("Relay %d -> %s\n", index, state ? "ON" : "OFF");

    // Report to Gateway
    StaticJsonDocument<128> doc;
    doc["id"] = index;
    doc["state"] = state;
    String payload;
    serializeJson(doc, payload);
    GATEWAY_SERIAL.println("RELAY:" + payload);
}

void toggleRelay(int index) {
    setRelay(index, !relayStates[index]);
}

// ============================================================================
// GATEWAY UART COMMUNICATION
// ============================================================================
void handleGatewayUart() {
    if (GATEWAY_SERIAL.available()) {
        String line = GATEWAY_SERIAL.readStringUntil('\n');
        line.trim();
        if (line.length() > 0) {
            processGatewayCommand(line);
        }
    }
}

void processGatewayCommand(String cmd) {
    // Expected: CMD:TYPE:PARAMS
    // CMD:ACCESS:{"pin":"1234"}
    // CMD:RELAY:1:true
    // CMD:LASER:true
    // CMD:STATUS

    if (!cmd.startsWith("CMD:")) return;

    int firstColon = cmd.indexOf(':');
    int secondColon = cmd.indexOf(':', firstColon + 1);

    if (secondColon < 0) return;

    String type = cmd.substring(firstColon + 1, secondColon);
    String params = cmd.substring(secondColon + 1);

    if (type == "ACCESS") {
        StaticJsonDocument<128> doc;
        deserializeJson(doc, params);
        if (doc.containsKey("pin")) {
            processPinAttempt(doc["pin"].as<String>());
        }
    } else if (type == "RELAY") {
        int relayId = params.substring(0, params.indexOf(':')).toInt();
        bool state = params.substring(params.indexOf(':') + 1) == "true";
        setRelay(relayId, state);
    } else if (type == "LASER") {
        setLaserArmed(params == "true");
    } else if (type == "STATUS") {
        sendStatusToGateway();
    }
}

void sendStatusToGateway() {
    StaticJsonDocument<512> doc;
    doc["door_locked"] = !doorUnlocked;
    doc["laser_armed"] = laserArmed;
    doc["laser_beam_intact"] = lastLaserState;
    doc["free_ram"] = ESP.getFreeHeap(); // Not available on AVR, use alternative
    doc["uptime_ms"] = millis();

    JsonArray relays = doc.createNestedArray("relays");
    for (int i = 0; i < 8; i++) {
        relays.add(relayStates[i]);
    }

    String payload;
    serializeJson(doc, payload);
    GATEWAY_SERIAL.println("STATUS:" + payload);
}

void sendPeriodicStatus() {
    if (millis() - lastUartSend > UART_SEND_INTERVAL) {
        sendStatusToGateway();
        lastUartSend = millis();
    }
}

// ============================================================================
// EXTERNAL COMMAND HANDLERS (called from gateway commands)
// ============================================================================
// These can be triggered via MQTT -> Gateway -> UART -> MEGA