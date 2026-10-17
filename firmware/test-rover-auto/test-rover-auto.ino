/*
 * TEST STANDALONE modo autónomo — sin RF, sin Gateway, sin App.
 * Motores + HC-SR04 EMA + TCRT5000 anti-caída (IR_THRESHOLD 400).
 * Levanta el rover con las ruedas en el aire para prueba segura sin moverse,
 * o ponlo sobre la pista con baterías para prueba real.
 * Serial 115200: decide acción + velocidades + lecturas.
 */
#include <Arduino.h>
#include <NewPing.h>

#define MOTOR_ENA 3 // swap lado físico
#define MOTOR_IN1 8
#define MOTOR_IN2 9
#define MOTOR_IN3 6
#define MOTOR_IN4 7
#define MOTOR_ENB 5
#define ULTRASONIC_TRIG 2
#define ULTRASONIC_ECHO A3
#define MAX_DISTANCE_CM 200
#define EMA_ALPHA 0.2f
#define MAX_PWM 255
#define OBSTACLE_DISTANCE_CM 30
#define CRITICAL_DISTANCE_CM 15
#define IR_REAR_PIN A0
#define IR_LEFT_PIN A1
#define IR_RIGHT_PIN A2
#define IR_THRESHOLD 400

NewPing sonar(ULTRASONIC_TRIG, ULTRASONIC_ECHO, MAX_DISTANCE_CM);
float ultrasonicEma = 0; bool ultrasonicInitialized = false;

void setMotorSpeeds(int leftPwm, int rightPwm) {
    leftPwm = constrain(leftPwm, -MAX_PWM, MAX_PWM);
    rightPwm = constrain(rightPwm, -MAX_PWM, MAX_PWM);
    if (leftPwm >= 0) { digitalWrite(MOTOR_IN1, HIGH); digitalWrite(MOTOR_IN2, LOW); }
    else { digitalWrite(MOTOR_IN1, LOW); digitalWrite(MOTOR_IN2, HIGH); leftPwm = -leftPwm; }
    analogWrite(MOTOR_ENA, leftPwm);
    if (rightPwm >= 0) { digitalWrite(MOTOR_IN3, HIGH); digitalWrite(MOTOR_IN4, LOW); }
    else { digitalWrite(MOTOR_IN3, LOW); digitalWrite(MOTOR_IN4, HIGH); rightPwm = -rightPwm; }
    analogWrite(MOTOR_ENB, rightPwm);
}

void setup() {
    Serial.begin(115200);
    pinMode(MOTOR_ENA, OUTPUT); pinMode(MOTOR_IN1, OUTPUT); pinMode(MOTOR_IN2, OUTPUT);
    pinMode(MOTOR_IN3, OUTPUT); pinMode(MOTOR_IN4, OUTPUT); pinMode(MOTOR_ENB, OUTPUT);
    pinMode(IR_REAR_PIN, INPUT); pinMode(IR_LEFT_PIN, INPUT); pinMode(IR_RIGHT_PIN, INPUT);
    Serial.println("=== TEST AUTO STANDALONE (sin RF) ===");
}

void loop() {
    unsigned int raw = sonar.ping_cm();
    if (raw > 0 && raw <= MAX_DISTANCE_CM) {
        if (!ultrasonicInitialized) { ultrasonicEma = raw; ultrasonicInitialized = true; }
        else ultrasonicEma = EMA_ALPHA * raw + (1.0f - EMA_ALPHA) * ultrasonicEma;
    }

    bool obstacleFront = ultrasonicInitialized && ultrasonicEma < OBSTACLE_DISTANCE_CM;
    bool criticalClose = ultrasonicInitialized && ultrasonicEma < CRITICAL_DISTANCE_CM;
    int a0 = analogRead(IR_REAR_PIN), a1 = analogRead(IR_LEFT_PIN), a2 = analogRead(IR_RIGHT_PIN);
    bool cliffRear = a0 > IR_THRESHOLD, cliffLeft = a1 > IR_THRESHOLD, cliffRight = a2 > IR_THRESHOLD;

    int leftSpeed = 0, rightSpeed = 0;
    const int BASE_SPEED = 120, TURN_SPEED = 150;
    const char* action = "CLEAR";

    if (criticalClose) { leftSpeed = -BASE_SPEED; rightSpeed = -BASE_SPEED; action = "REVERSA CRITICO<15cm"; }
    else if (obstacleFront) {
        if (cliffLeft && !cliffRight) { leftSpeed = TURN_SPEED; rightSpeed = -TURN_SPEED; action = "GIRO DER (borde izq)"; }
        else if (cliffRight && !cliffLeft) { leftSpeed = -TURN_SPEED; rightSpeed = TURN_SPEED; action = "GIRO IZQ (borde der)"; }
        else { leftSpeed = TURN_SPEED; rightSpeed = -TURN_SPEED; action = "GIRO DER (obstaculo)"; }
    } else if (cliffRear && !cliffLeft && !cliffRight) {
        leftSpeed = BASE_SPEED; rightSpeed = BASE_SPEED; action = "AVANCE (borde trasero)";
    } else if (cliffRear || cliffLeft || cliffRight) {
        leftSpeed = -BASE_SPEED; rightSpeed = -BASE_SPEED; action = "REVERSA BORDE TCRT";
    } else { leftSpeed = BASE_SPEED; rightSpeed = BASE_SPEED; action = "AVANCE"; }

    setMotorSpeeds(leftSpeed, rightSpeed);

    Serial.print("A0="); Serial.print(a0); Serial.print(cliffRear?" BORDE":" OK");
    Serial.print(" | A1="); Serial.print(a1); Serial.print(cliffLeft?" BORDE":" OK");
    Serial.print(" | A2="); Serial.print(a2); Serial.print(cliffRight?" BORDE":" OK");
    Serial.print(" | US_ema="); if (!ultrasonicInitialized) Serial.print("--"); else Serial.print((int)ultrasonicEma);
    Serial.print("cm | US_raw="); if(raw==0) Serial.print("--"); else Serial.print(raw); Serial.print(" | "); Serial.print(action);
    Serial.print(" L="); Serial.print(leftSpeed); Serial.print(" R="); Serial.println(rightSpeed);

    delay(200);
}
