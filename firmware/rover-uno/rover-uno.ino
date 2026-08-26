/*
 * AetherNet - Rover UNO Autonomous Tank
 * Handles: L298N dual motor driver, HC-SR04 ultrasonic, 3x TCRT5000 IR, nRF24L01 RF
 * Fail-safe: stops motors if no valid RF packet received within timeout
 * 
 * Hardware: Arduino UNO
 * Pinout:
 * L298N: ENA=5, IN1=6, IN2=7, IN3=8, IN4=9, ENB=10
 * HC-SR04: TRIG=2, ECHO=3
 * TCRT5000 (x3): A0 (left), A1 (center), A2 (right)
 * nRF24L01: CE=4, CSN=10 (but 10 used by L298N ENB, so use CSN=10, CE=4... wait, conflict!)
 * 
 * REVISED Pinout for UNO (avoiding conflicts):
 * L298N: ENA=5, IN1=6, IN2=7, IN3=8, IN4=9, ENB=10 (PWM pins: 5,6,9,10)
 * HC-SR04: TRIG=2, ECHO=3
 * TCRT5000: A0, A1, A2
 * nRF24L01: CE=4, CSN=10 -- CONFLICT with ENB!
 * 
 * Solution: Move L298N ENB to pin 11 (not PWM on UNO... wait, 11 IS PWM)
 * Actually UNO PWM pins: 3, 5, 6, 9, 10, 11
 * So: ENA=5, IN1=6, IN2=7, IN3=8, IN4=9, ENB=11
 * nRF24L01: CE=4, CSN=10 (10 is free now)
 */

#include <Arduino.h>
#include <SPI.h>
#include <RF24.h>
#include <NewPing.h>

// ============================================================================
// PIN DEFINITIONS
// ============================================================================
// L298N Motor Driver
#define MOTOR_ENA 5   // PWM - Left motor speed
#define MOTOR_IN1 6   // Left motor direction
#define MOTOR_IN2 7   // Left motor direction
#define MOTOR_IN3 8   // Right motor direction
#define MOTOR_IN4 9   // Right motor direction
#define MOTOR_ENB 11  // PWM - Right motor speed

// HC-SR04 Ultrasonic
#define ULTRASONIC_TRIG 2
#define ULTRASONIC_ECHO 3
#define MAX_DISTANCE_CM 200

// TCRT5000 IR Sensors (analog)
#define IR_LEFT_PIN A0
#define IR_CENTER_PIN A1
#define IR_RIGHT_PIN A2
#define IR_THRESHOLD 500  // Adjust based on calibration

// nRF24L01
#define NRF_CE_PIN 4
#define NRF_CSN_PIN 10

// ============================================================================
// CONFIGURATION
// ============================================================================
const byte roverAddress[6] = "ROVER";
const byte gatewayAddress[6] = "GATEW";

// Fail-safe timeout (ms) - if no valid packet received, stop motors
#define FAILSAFE_TIMEOUT_MS 500

// EMA filter for ultrasonic (alpha=0.2 per HU-03)
#define EMA_ALPHA 0.2f

// Motor limits
#define MAX_PWM 255
#define MIN_PWM_FOR_MOVEMENT 60  // Below this, motors may not overcome friction

// Obstacle avoidance thresholds
#define OBSTACLE_DISTANCE_CM 30
#define CRITICAL_DISTANCE_CM 15

// ============================================================================
// GLOBALS
// ============================================================================
RF24 radio(NRF_CE_PIN, NRF_CSN_PIN);
NewPing sonar(ULTRASONIC_TRIG, ULTRASONIC_ECHO, MAX_DISTANCE_CM);

// RF packet structures (must match gateway)
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

// State
RoverCommand lastCommand = {0, 0, 0, 0};
unsigned long lastValidPacketTime = 0;
bool failsafeActive = false;
float ultrasonicEma = 0;
bool ultrasonicInitialized = false;
uint8_t currentMode = 0; // 0=stop, 1=manual, 2=auto

// ============================================================================
// SETUP
// ============================================================================
void setup() {
    Serial.begin(115200);
    while (!Serial) delay(10);
    Serial.println("\n=== AetherNet Rover UNO Starting ===");

    // Motor pins
    pinMode(MOTOR_ENA, OUTPUT);
    pinMode(MOTOR_IN1, OUTPUT);
    pinMode(MOTOR_IN2, OUTPUT);
    pinMode(MOTOR_IN3, OUTPUT);
    pinMode(MOTOR_IN4, OUTPUT);
    pinMode(MOTOR_ENB, OUTPUT);
    stopMotors();

    // IR sensors
    pinMode(IR_LEFT_PIN, INPUT);
    pinMode(IR_CENTER_PIN, INPUT);
    pinMode(IR_RIGHT_PIN, INPUT);

    // nRF24L01
    if (!radio.begin()) {
        Serial.println("ERROR: nRF24L01 not detected!");
        // Continue anyway for testing
    } else {
        radio.setPALevel(RF24_PA_HIGH);
        radio.setDataRate(RF24_2MBPS);
        radio.setChannel(76);
        radio.openWritingPipe(gatewayAddress);
        radio.openReadingPipe(1, roverAddress);
        radio.enableAckPayload();
        radio.startListening();
        Serial.println("nRF24L01 initialized");
    }

    // Prepare initial ACK payload
    prepareAckPayload();

    Serial.println("Rover ready - awaiting commands");
}

// ============================================================================
// MAIN LOOP
// ============================================================================
void loop() {
    // 1. Check for incoming RF commands
    handleRfCommands();

    // 2. Check fail-safe timeout
    checkFailsafe();

    // 3. Read sensors
    readSensors();

    // 4. Execute current mode behavior
    if (!failsafeActive) {
        if (currentMode == 1) {
            // Manual mode - execute last command
            executeManualCommand();
        } else if (currentMode == 2) {
            // Auto mode - autonomous navigation
            executeAutoMode();
        }
    } else {
        // Fail-safe active - ensure motors stopped
        stopMotors();
    }

    // 5. Send telemetry via RF (ACK payload)
    updateAckPayload();

    delay(10); // 100Hz main loop
}

// ============================================================================
// RF COMMUNICATION
// ============================================================================
void handleRfCommands() {
    if (radio.available()) {
        RoverCommand cmd;
        radio.read(&cmd, sizeof(cmd));

        if (verifyChecksum(cmd)) {
            lastCommand = cmd;
            currentMode = cmd.mode;
            lastValidPacketTime = millis();
            failsafeActive = false;

            // Debug
            Serial.print("RF RX: L=");
            Serial.print(cmd.left_pwm);
            Serial.print(" R=");
            Serial.print(cmd.right_pwm);
            Serial.print(" mode=");
            Serial.println(cmd.mode);
        } else {
            Serial.println("RF RX: Checksum error");
        }
    }
}

void checkFailsafe() {
    if (millis() - lastValidPacketTime > FAILSAFE_TIMEOUT_MS) {
        if (!failsafeActive) {
            failsafeActive = true;
            currentMode = 0;
            stopMotors();
            Serial.println("!!! FAIL-SAFE ACTIVATED: No RF signal !!!");
        }
    }
}

void prepareAckPayload() {
    RoverTelemetry telem = buildTelemetry();
    radio.writeAckPayload(1, &telem, sizeof(telem));
}

void updateAckPayload() {
    RoverTelemetry telem = buildTelemetry();
    radio.writeAckPayload(1, &telem, sizeof(telem));
}

RoverTelemetry buildTelemetry() {
    RoverTelemetry telem;
    telem.left_pwm = lastCommand.left_pwm;
    telem.right_pwm = lastCommand.right_pwm;
    telem.ultrasonic_cm = (uint16_t)ultrasonicEma;
    telem.ir_left = digitalRead(IR_LEFT_PIN) < IR_THRESHOLD;
    telem.ir_center = digitalRead(IR_CENTER_PIN) < IR_THRESHOLD;
    telem.ir_right = digitalRead(IR_RIGHT_PIN) < IR_THRESHOLD;
    telem.rf_rssi = -70; // Placeholder: RF24.getRSSI() no disponible en esta versión de RF24
    telem.checksum = calculateChecksum(telem);
    return telem;
}

// ============================================================================
// SENSOR READING
// ============================================================================
void readSensors() {
    // Ultrasonic with EMA filter
    unsigned int rawDistance = sonar.ping_cm();
    if (rawDistance > 0 && rawDistance <= MAX_DISTANCE_CM) {
        if (!ultrasonicInitialized) {
            ultrasonicEma = rawDistance;
            ultrasonicInitialized = true;
        } else {
            ultrasonicEma = EMA_ALPHA * rawDistance + (1.0 - EMA_ALPHA) * ultrasonicEma;
        }
    }

    // IR sensors are read directly in buildTelemetry()
}

// ============================================================================
// MOTOR CONTROL
// ============================================================================
void setMotorSpeeds(int leftPwm, int rightPwm) {
    // Constrain to valid range
    leftPwm = constrain(leftPwm, -MAX_PWM, MAX_PWM);
    rightPwm = constrain(rightPwm, -MAX_PWM, MAX_PWM);

    // Left motor
    if (leftPwm >= 0) {
        digitalWrite(MOTOR_IN1, HIGH);
        digitalWrite(MOTOR_IN2, LOW);
    } else {
        digitalWrite(MOTOR_IN1, LOW);
        digitalWrite(MOTOR_IN2, HIGH);
        leftPwm = -leftPwm;
    }
    analogWrite(MOTOR_ENA, leftPwm);

    // Right motor
    if (rightPwm >= 0) {
        digitalWrite(MOTOR_IN3, HIGH);
        digitalWrite(MOTOR_IN4, LOW);
    } else {
        digitalWrite(MOTOR_IN3, LOW);
        digitalWrite(MOTOR_IN4, HIGH);
        rightPwm = -rightPwm;
    }
    analogWrite(MOTOR_ENB, rightPwm);
}

void stopMotors() {
    analogWrite(MOTOR_ENA, 0);
    analogWrite(MOTOR_ENB, 0);
    digitalWrite(MOTOR_IN1, LOW);
    digitalWrite(MOTOR_IN2, LOW);
    digitalWrite(MOTOR_IN3, LOW);
    digitalWrite(MOTOR_IN4, LOW);
}

void executeManualCommand() {
    // Apply deadband to prevent jitter at low speeds
    int left = lastCommand.left_pwm;
    int right = lastCommand.right_pwm;

    if (abs(left) < MIN_PWM_FOR_MOVEMENT) left = 0;
    if (abs(right) < MIN_PWM_FOR_MOVEMENT) right = 0;

    setMotorSpeeds(left, right);
}

// ============================================================================
// AUTONOMOUS MODE
// ============================================================================
void executeAutoMode() {
    // Simple obstacle avoidance and cliff detection
    bool obstacleFront = ultrasonicInitialized && ultrasonicEma < OBSTACLE_DISTANCE_CM;
    bool criticalClose = ultrasonicInitialized && ultrasonicEma < CRITICAL_DISTANCE_CM;
    bool cliffLeft = digitalRead(IR_LEFT_PIN) < IR_THRESHOLD;
    bool cliffCenter = digitalRead(IR_CENTER_PIN) < IR_THRESHOLD;
    bool cliffRight = digitalRead(IR_RIGHT_PIN) < IR_THRESHOLD;

    int leftSpeed = 0;
    int rightSpeed = 0;
    const int BASE_SPEED = 120;
    const int TURN_SPEED = 150;

    if (criticalClose) {
        // Emergency stop and back up
        leftSpeed = -BASE_SPEED;
        rightSpeed = -BASE_SPEED;
    } else if (obstacleFront) {
        // Obstacle detected - turn away
        if (cliffLeft && !cliffRight) {
            // Left cliff, turn right
            leftSpeed = TURN_SPEED;
            rightSpeed = -TURN_SPEED;
        } else if (cliffRight && !cliffLeft) {
            // Right cliff, turn left
            leftSpeed = -TURN_SPEED;
            rightSpeed = TURN_SPEED;
        } else {
            // Default: turn right
            leftSpeed = TURN_SPEED;
            rightSpeed = -TURN_SPEED;
        }
    } else if (cliffCenter || cliffLeft || cliffRight) {
        // Cliff detected - back up and turn
        leftSpeed = -BASE_SPEED;
        rightSpeed = -BASE_SPEED;
    } else {
        // Clear path - move forward
        leftSpeed = BASE_SPEED;
        rightSpeed = BASE_SPEED;
    }

    setMotorSpeeds(leftSpeed, rightSpeed);
}

// ============================================================================
// UTILITIES
// ============================================================================
uint16_t calculateChecksum(const RoverTelemetry& telem) {
    uint16_t sum = 0;
    const uint8_t* bytes = (const uint8_t*)&telem;
    for (size_t i = 0; i < sizeof(telem) - 2; i++) {
        sum += bytes[i];
    }
    return sum;
}

bool verifyChecksum(const RoverCommand& cmd) {
    uint16_t sum = 0;
    const uint8_t* bytes = (const uint8_t*)&cmd;
    for (size_t i = 0; i < sizeof(cmd) - 2; i++) {
        sum += bytes[i];
    }
    return sum == cmd.checksum;
}