/*
 * Test motores independiente: ciclo R-adelante / R-atras / L-adelante / L-atras / stop.
 * Motor quieto NO debe moverse. Muestra en Serial qué se manda.
 */
#define MOTOR_ENA 3 // swap lado físico
#define MOTOR_IN1 8
#define MOTOR_IN2 9
#define MOTOR_IN3 6
#define MOTOR_IN4 7
#define MOTOR_ENB 5

void setLeft(int v) {
    if (v >= 0) { digitalWrite(MOTOR_IN1, HIGH); digitalWrite(MOTOR_IN2, LOW); }
    else { digitalWrite(MOTOR_IN1, LOW); digitalWrite(MOTOR_IN2, HIGH); v = -v; }
    analogWrite(MOTOR_ENA, v);
}
void setRight(int v) {
    if (v >= 0) { digitalWrite(MOTOR_IN3, HIGH); digitalWrite(MOTOR_IN4, LOW); }
    else { digitalWrite(MOTOR_IN3, LOW); digitalWrite(MOTOR_IN4, HIGH); v = -v; }
    analogWrite(MOTOR_ENB, v);
}

void setup() {
    Serial.begin(115200);
    pinMode(MOTOR_ENA, OUTPUT); pinMode(MOTOR_IN1, OUTPUT); pinMode(MOTOR_IN2, OUTPUT);
    pinMode(MOTOR_IN3, OUTPUT); pinMode(MOTOR_IN4, OUTPUT); pinMode(MOTOR_ENB, OUTPUT);
    Serial.println("=== TEST MOTORES ===");
}

void loop() {
    Serial.println("ESPERA 2s (stop)");
    setLeft(0); setRight(0); delay(2000);

    Serial.println("DERECHO ADELANTE (L quieto)");
    setLeft(0); setRight(150); delay(2500);

    Serial.println("DERECHO ATRAS (L quieto)");
    setLeft(0); setRight(-150); delay(2500);

    Serial.println("IZQUIERDO ADELANTE (R quieto)");
    setLeft(150); setRight(0); delay(2500);

    Serial.println("IZQUIERDO ATRAS (R quieto)");
    setLeft(-150); setRight(0); delay(2500);
}
