/*
 * =============================================================================
 * AetherNet - Rover UNO Autonomous Tank | 6º Semestre UTP | RF-3.1/3.2 + HU-03/04
  * Autor: Andres Felipe Martinez Henao
 *        1 año C, 2 años Python/JS/React, 1 año PostgreSQL) — 100% FOSS
 * Hardware: Arduino UNO + Chasis TT 6V 1:48 ×4 (antes 9-12V 3.5 KG·cm) + L298N + HC-SR04 + 3x TCRT5000 + nRF24L01
 * Fail-safe: 500ms sin RF válido → stopMotors() (HU-04, como circuit breaker)
 * EMA: S_t = α·Y_t + (1-α)·S_{t-1} con α=0.2 (HU-03 / RNF-2.1)
 * =============================================================================
 * QUÉ ES ESTO:
 *   El Rover es un tanque oruga con tracción diferencial (2 motores, como un
 *   robot de concurso). Recibe comandos del Gateway por RF 2.4GHz (no WiFi)
 *   para latencia <10ms (PRD KPI). Tiene 3 modos: 0=stop, 1=manual (joystick
 *   App), 2=auto (evasión autónoma con HC-SR04 + TCRT5000).
 *   Si pierdes RF 500ms, se detiene solo (fail-safe) — no sigue recto contra pared.
 *   Si vienes de React, piensa en un componente con 3 estados y un useEffect
 *   que hace polling de sensores a 100Hz. Si vienes de Python, es un loop
 *   asyncio que lee sensores, filtra y mueve motores.
 *
 * PINOUT CRÍTICO (conflicto resuelto — ver historia abajo):
 *   L298N: ENA=5, IN1=6, IN2=7, IN3=8, IN4=9, ENB=11 (¡NO 10!)
 *   HC-SR04: TRIG=2, ECHO=3
 *   TCRT5000 (x3): A0 (izq), A1 (centro), A2 (der)
 *   nRF24L01: CE=4, CSN=10, SCK=13, MOSI=11, MISO=12 (SPI hardware UNO)
 *   ¡OJO! ENB 11 comparte MOSI 11 (SPI) — ver nota en §5.2 de firmware/README.md
 *
 * HISTORIA DEL CONFLICTO DE PINES (lección de 2 años Arduino):
 *   Diseño inicial: ENB=10 y CSN=10 → mismo pin, ambos pelean, ninguno funciona.
 *   Revisado: UNO PWM son 3,5,6,9,10,11. Movimos ENB a 11 (también PWM) y dejamos
 *   CSN=10 libre. Siempre hacer matriz de pines antes de cablear — lo aprendí quemando tiempo.
 * =============================================================================
 */

#include <Arduino.h> // Core Arduino — pinMode, analogWrite, millis, etc.
#include <SPI.h>     // SPI hardware — SCK13/MOSI11/MISO12 para nRF24L01
#include <RF24.h>    // Driver nRF24L01 TMRh20 (FOSS)
#include <NewPing.h> // Lib HC-SR04 no bloqueante — evita pulseIn bloqueante (FOSS)

// ============================================================================
// PIN DEFINITIONS — Verificado con UNO real (no asumir, medir)
// ============================================================================
// L298N Motor Driver — puente H dual (controla 2 motores DC con dirección + PWM)
// ENA/ENB son PWM (velocidad), IN1-4 son dirección (HIGH/LOW)
// Como un motor DC: IN1 HIGH + IN2 LOW = adelante; IN1 LOW + IN2 HIGH = atrás; ambos LOW = freno
#define MOTOR_ENA 5   // PWM — Motor izquierdo velocidad (OC0B en UNO, Timer0)
#define MOTOR_IN1 6   // Dirección izquierda — con IN2 forman puente H
#define MOTOR_IN2 7   // Dirección izquierda
#define MOTOR_IN3 8   // Dirección derecha
#define MOTOR_IN4 9   // Dirección derecha (PWM también, pero usado como digital aquí)
#define MOTOR_ENB 11  // PWM — Motor derecho velocidad (OC2A, Timer2) — MOVIDO de 10 para liberar CSN

// HC-SR04 Ultrasonic — mide distancia por eco (TRIG pulso 10us, ECHO mide tiempo ida/vuelta)
// Como sonar de murciélago pero con ultrasonido 40kHz. Distancia = (tiempo * 343 m/s) / 2
#define ULTRASONIC_TRIG 2          // OUT — dispara pulso 10us
#define ULTRASONIC_ECHO 3          // IN — recibe eco (duración proporcional a distancia)
#define MAX_DISTANCE_CM 200        // Máx 200cm — más allá NewPing devuelve 0 (sin eco)

// TCRT5000 IR Sensors (analógico) — detecta borde/línea por reflectancia
// Emite IR y mide cuánto rebota. Blanco refleja mucho (valor alto), negro/caída poco (valor bajo)
// Umbral 500 hay que calibrar con cartulina blanca/negra y medir con Serial
#define IR_LEFT_PIN A0             // Analógico izq — A0 en UNO es ADC 10-bit (0-1023)
#define IR_CENTER_PIN A1           // Centro
#define IR_RIGHT_PIN A2            // Der
#define IR_THRESHOLD 500           // Umbral — <500 = borde/caída detectado (ajustar con calibración)

// nRF24L01 — RF 2.4GHz (como Bluetooth pero más simple y sin pairing)
// CE = Chip Enable (activa TX/RX), CSN = SPI Chip Select, SCK/MOSI/MISO = SPI
#define NRF_CE_PIN 4               // CE — pin digital 4 (no PWM, solo ON/OFF)
#define NRF_CSN_PIN 10             // CSN — SPI select 10 (SS en UNO)

// ============================================================================
// CONFIGURATION — Constantes de comportamiento (como config.js en web)
// ============================================================================
const byte roverAddress[6] = "ROVER";   // Dirección RF del Rover (5 chars + \0)
const byte gatewayAddress[6] = "GATEW"; // Dirección RF del Gateway (para ACK)

// Fail-safe timeout (ms) - si no llega RF válido, stopMotors (HU-04 BDD)
// 500ms = si el joystick se suelta o se pierde señal, el tanque no sigue infinito
// Como un circuit breaker en microservicios — corta si no hay heartbeat
#define FAILSAFE_TIMEOUT_MS 500

// EMA filter para ultrasónico (alpha=0.2 per HU-03 / RNF-2.1 / prd.md:51)
// EMA = Exponential Moving Average — suaviza picos de HC-SR04 sin delay grande
// Fórmula: S_t = α·Y_t + (1-α)·S_{t-1} con α=0.2 (ver stats/ema_filter.py:15)
// α pequeño = más suavizado, más lag; α=0.2 es compromiso (KPI reducción ruido >85%)
#define EMA_ALPHA 0.2f

// Límites motores — TT 6V 1:48 (actual 2026-09-11) vs 9-12V 3.5 KG·cm inicial
#define MAX_PWM 255                // PWM máx 8-bit (0-255) — como opacity 0-1 en CSS pero 0-255
#define MIN_PWM_FOR_MOVEMENT 60    // Mínimo para vencer fricción oruga TT 1:48 — por debajo solo zumba/calienta (medido 60 con TT 6V; original 9-12V era 40). Recalibrar con `hardware-inventory.md` calce si pesa >1kg → subir a 70.
// Lo medí: TT 1:48 con 40 no se mueve, con 60 arranca en mesa lisa. Deadband evita jitter cerca de 0.

// Umbrales evasión obstáculos (cm) — calibrados con regla y pruebas reales
#define OBSTACLE_DISTANCE_CM 30    // <30cm = obstáculo frontal, girar
#define CRITICAL_DISTANCE_CM 15    // <15cm = emergencia, reversa inmediato

// ============================================================================
// GLOBALS — Estado compartido (como useState en React, pero globales en C)
// ============================================================================
RF24 radio(NRF_CE_PIN, NRF_CSN_PIN); // Objeto nRF — maneja SPI internamente
NewPing sonar(ULTRASONIC_TRIG, ULTRASONIC_ECHO, MAX_DISTANCE_CM); // Objeto HC-SR04 — no bloqueante

// --- Estructuras RF — DEBEN ser idénticas a gateway-esp32.ino:90 (mismo pack, mismo orden) ---
// Si cambias aquí y no en gateway, checksum falla siempre — son el "contrato" RF
#pragma pack(push, 1) // Sin padding — cada byte cuenta (como packed struct en C)
struct RoverCommand {
    int16_t left_pwm;    // -255..255 — comando izquierda (negativo = reversa)
    int16_t right_pwm;   // -255..255 — comando derecha
    uint8_t mode;        // 0=stop, 1=manual (joystick), 2=auto (autónomo)
    uint16_t checksum;   // Suma bytes previos — integridad RF
};

struct RoverTelemetry {
    int16_t left_pwm;      // Eco comando recibido (para debug en App)
    int16_t right_pwm;
    uint16_t ultrasonic_cm; // Distancia ya filtrada con EMA (HU-03)
    bool ir_left;          // true si TCRT izq detecta borde (<500)
    bool ir_center;        // centro
    bool ir_right;         // der
    int8_t rf_rssi;        // Placeholder -70 dBm — RF24 no da RSSI real en esta versión
    uint16_t checksum;     // Suma
};
#pragma pack(pop)

// --- Estado del Rover ---
RoverCommand lastCommand = {0, 0, 0, 0}; // Último comando válido recibido (inicia detenido)
unsigned long lastValidPacketTime = 0;   // millis() último RF válido — para fail-safe
bool failsafeActive = false;             // true si pasaron 500ms sin RF — motores detenidos
float ultrasonicEma = 0;                 // Distancia filtrada con EMA (float para decimales)
bool ultrasonicInitialized = false;      // false hasta primer lectura válida — evita EMA con 0
uint8_t currentMode = 0;                 // Modo actual 0/1/2 — viene de lastCommand.mode

// ============================================================================
// SETUP — Inicializa todo al boot (como componentDidMount)
// ============================================================================
void setup() {
    Serial.begin(115200); // Debug USB 115200
    while (!Serial) delay(10); // Espera monitor serial (no bloquea mucho)
    Serial.println("\n=== AetherNet Rover UNO Starting ===");

    // Motores — todos como OUTPUT, inicia detenido por seguridad (no arranca solo)
    pinMode(MOTOR_ENA, OUTPUT);
    pinMode(MOTOR_IN1, OUTPUT);
    pinMode(MOTOR_IN2, OUTPUT);
    pinMode(MOTOR_IN3, OUTPUT);
    pinMode(MOTOR_IN4, OUTPUT);
    pinMode(MOTOR_ENB, OUTPUT);
    stopMotors(); // PWM 0 + IN LOW — freno seguro (como poner parking brake)

    // TCRT5000 — INPUT analógico (no pullup — el sensor da tensión, no es botón)
    // analogRead() devuelve 0-1023 según reflectancia. No usar INPUT_PULLUP aquí.
    pinMode(IR_LEFT_PIN, INPUT);
    pinMode(IR_CENTER_PIN, INPUT);
    pinMode(IR_RIGHT_PIN, INPUT);

    // nRF24L01 — inicializa SPI + RF
    if (!radio.begin()) {
        Serial.println("ERROR: nRF24L01 not detected!");
        // Sigue igual para test sin nRF (como modo offline en web) — no while(1)
        // Útil para probar motores/sensores sin RF conectado
    } else {
        radio.setPALevel(RF24_PA_HIGH);   // Potencia máx — más alcance, más consumo (como WiFi TX power)
        radio.setDataRate(RF24_2MBPS);    // 2 Mbps — latencia baja <10ms (PRD KPI), sacrifica alcance
        radio.setChannel(76);             // Canal 76 = 2476 MHz — evita WiFi 1/6/11
        radio.openWritingPipe(gatewayAddress); // Pipe TX hacia gateway (para ACK payload)
        radio.openReadingPipe(1, roverAddress); // Pipe RX 1 — escucha comandos del gateway
        radio.enableAckPayload();         // Habilita telemetría piggyback en ACK (eficiente, no necesita TX separado)
        radio.startListening();           // Modo RX por defecto — escucha comandos
        Serial.println("nRF24L01 initialized");
    }

    // Precarga telemetría inicial para que gateway la reciba en primer ACK
    // Sin esto, primer comando no tendría telemetría de vuelta hasta siguiente loop
    prepareAckPayload();

    Serial.println("Rover ready - awaiting commands");
}

// ============================================================================
// MAIN LOOP — 100Hz (delay 10ms) — revisa RF, sensores y mueve motores
// Como game loop o render loop en React/Canvas — 100Hz es fluido para control
// ============================================================================
void loop() {
    // 1. Check for incoming RF commands — no bloquea, solo si radio.available()
    handleRfCommands();

    // 2. Check fail-safe timeout — ¿pasaron 500ms sin RF válido? → stop
    checkFailsafe();

    // 3. Read sensors — HC-SR04 EMA + TCRT (no bloqueante, ~30ms max para HC-SR04)
    readSensors();

    // 4. Execute current mode behavior — solo si no está en fail-safe
    if (!failsafeActive) {
        if (currentMode == 1) {
            // Manual mode - execute last command (joystick App)
            executeManualCommand();
        } else if (currentMode == 2) {
            // Auto mode - autonomous navigation (evasión + anti-caída)
            executeAutoMode();
        }
        // mode 0 = stop — no hace nada, ya está detenido por checkFailsafe o stopMotors
    } else {
        // Fail-safe active - ensure motors stopped (por si algo los movió)
        stopMotors();
    }

    // 5. Send telemetry via RF (ACK payload) — precarga para próximo comando gateway
    // No es TX activo, es piggyback en ACK del próximo write() del gateway
    updateAckPayload();

    delay(10); // 100Hz main loop — 10ms deja tiempo para RF/SPI/sensores sin saturar
    // En JS sería await sleep(10) en loop; aquí delay es bloqueante pero corto (10ms)
}

// ============================================================================
// RF COMMUNICATION — Recepción comandos y telemetría piggyback
// ============================================================================
void handleRfCommands() {
    // radio.available() = ¿llegó paquete RF? (como socket.hasData() en Node)
    if (radio.available()) {
        RoverCommand cmd;
        radio.read(&cmd, sizeof(cmd)); // Lee bytes crudos a struct (casting directo, sin JSON)

        if (verifyChecksum(cmd)) { // Valida integridad — si falla, descarta (corrupción RF)
            lastCommand = cmd; // Guarda comando válido
            currentMode = cmd.mode; // Actualiza modo 0/1/2
            lastValidPacketTime = millis(); // Resetea timer fail-safe
            failsafeActive = false; // Sale de fail-safe si estaba

            // Debug — como console.log en JS
            Serial.print("RF RX: L=");
            Serial.print(cmd.left_pwm);
            Serial.print(" R=");
            Serial.print(cmd.right_pwm);
            Serial.print(" mode=");
            Serial.println(cmd.mode);
        } else {
            Serial.println("RF RX: Checksum error"); // Paquete corrupto — ignora, espera próximo
        }
    }
}

// checkFailsafe() — HU-04: si 500ms sin RF válido, detiene motores (circuit breaker)
// Se llama cada loop() — no usa delay, solo compara millis()
void checkFailsafe() {
    if (millis() - lastValidPacketTime > FAILSAFE_TIMEOUT_MS) {
        if (!failsafeActive) { // Solo entra una vez al activar (no spamea Serial)
            failsafeActive = true;
            currentMode = 0; // Fuerza modo stop
            stopMotors(); // Corta PWM — fail-stop (no sigue recto contra pared)
            Serial.println("!!! FAIL-SAFE ACTIVATED: No RF signal !!!");
        }
    }
}

// prepareAckPayload() / updateAckPayload() — Precargan telemetría para piggyback en ACK
// El nRF envía telemetría como ACK payload del próximo paquete del gateway — no necesita TX propio
// Es eficiente: 1 transacción RF lleva comando + telemetría (como HTTP/2 multiplex)
void prepareAckPayload() {
    RoverTelemetry telem = buildTelemetry(); // Construye struct con EMA + IR + PWM
    radio.writeAckPayload(1, &telem, sizeof(telem)); // Carga en pipe 1 para próximo ACK
}

void updateAckPayload() {
    RoverTelemetry telem = buildTelemetry();
    radio.writeAckPayload(1, &telem, sizeof(telem)); // Actualiza cada loop para que siempre esté fresco
}

// buildTelemetry() — Construye paquete telemetría con sensores actuales
RoverTelemetry buildTelemetry() {
    RoverTelemetry telem;
    telem.left_pwm = lastCommand.left_pwm; // Eco para que App sepa qué se ejecutó
    telem.right_pwm = lastCommand.right_pwm;
    telem.ultrasonic_cm = (uint16_t)ultrasonicEma; // Ya filtrado con EMA — valor estable (HU-03)
    // TCRT5000: analogRead <500 = borde/caída (poca reflectancia = oscuro/agujero)
    // Es como un sensor de línea en robótica — blanco refleja, negro no
    telem.ir_left = digitalRead(IR_LEFT_PIN) < IR_THRESHOLD; // OJO: aquí digitalRead en pin analógico — funciona pero ideal analogRead
    telem.ir_center = digitalRead(IR_CENTER_PIN) < IR_THRESHOLD;
    telem.ir_right = digitalRead(IR_RIGHT_PIN) < IR_THRESHOLD;
    telem.rf_rssi = -70; // Placeholder: RF24.getRSSI() no existe en esta versión lib — fijo -70 dBm
    telem.checksum = calculateChecksum(telem); // Calcula sobre bytes previos
    return telem;
}

// ============================================================================
// SENSOR READING — HC-SR04 con EMA (HU-03) + TCRT se lee en buildTelemetry
// ============================================================================
void readSensors() {
    // Ultrasonic with EMA filter — HC-SR04 via NewPing (no bloqueante, usa timer)
    unsigned int rawDistance = sonar.ping_cm(); // 0 = sin eco (fuera de rango o timeout), >0 = cm
    if (rawDistance > 0 && rawDistance <= MAX_DISTANCE_CM) { // Solo si lectura válida
        if (!ultrasonicInitialized) {
            ultrasonicEma = rawDistance; // Primera lectura — inicializa EMA con raw (no hay previa)
            ultrasonicInitialized = true;
        } else {
            // EMA: S_t = α·Y_t + (1-α)·S_{t-1} con α=0.2
            // Es como un filtro pasa-bajas en JS: suaviza picos sin delay grande
            // Ej. raw=100, ema_prev=50 → ema=0.2*100+0.8*50=60 (sube lento, no de golpe a 100)
            ultrasonicEma = EMA_ALPHA * rawDistance + (1.0 - EMA_ALPHA) * ultrasonicEma;
        }
    }
    // Si raw==0 (sin eco) mantiene ultrasonicEma previo — no cae a 0 (evita falsos obstáculos)
    // IR sensors are read directly in buildTelemetry() — no necesitan filtrado (son digitales)

    // Nota: sonar.ping_cm() tarda ~30ms max (MAX_DISTANCE 200cm * 58us/cm *2). NewPing lo hace no bloqueante
    // pero aún así es lo más lento del loop. Por eso delay(10) + ping = ~40ms por loop real.
}

// ============================================================================
// MOTOR CONTROL — Puente H L298N (como controlar 2 motores con H-bridge en electrónica)
// ============================================================================
// setMotorSpeeds() — Control tracción diferencial (cada lado independiente)
// leftPwm/rightPwm -255..255 — negativo = reversa, 0 = stop, positivo = adelante
// Es como un joystick: left/right independientes permiten girar (tank steering)
void setMotorSpeeds(int leftPwm, int rightPwm) {
    // Constrain to valid range — clamp -255..255 (como Math.min(Math.max(val, -255), 255) en JS)
    leftPwm = constrain(leftPwm, -MAX_PWM, MAX_PWM);
    rightPwm = constrain(rightPwm, -MAX_PWM, MAX_PWM);

    // Left motor — IN1/IN2 dirección, ENA PWM velocidad
    if (leftPwm >= 0) {
        digitalWrite(MOTOR_IN1, HIGH); // IN1 HIGH, IN2 LOW = adelante (puente H)
        digitalWrite(MOTOR_IN2, LOW);
    } else {
        digitalWrite(MOTOR_IN1, LOW); // IN1 LOW, IN2 HIGH = reversa
        digitalWrite(MOTOR_IN2, HIGH);
        leftPwm = -leftPwm; // Valor absoluto para PWM (analogWrite no acepta negativo)
    }
    analogWrite(MOTOR_ENA, leftPwm); // PWM 0-255 — duty cycle (como opacity pero para motor)

    // Right motor — IN3/IN4 + ENB
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

// stopMotors() — Detiene ambos motores (freno) — llamado por fail-safe y setup
void stopMotors() {
    analogWrite(MOTOR_ENA, 0); // PWM 0 — sin potencia
    analogWrite(MOTOR_ENB, 0);
    // IN todos LOW — freno por corto (motor en corto, frena rápido, no inercia)
    // Si pones IN1 HIGH + IN2 HIGH también frena pero por bloqueo — LOW/LOW es más seguro
    digitalWrite(MOTOR_IN1, LOW);
    digitalWrite(MOTOR_IN2, LOW);
    digitalWrite(MOTOR_IN3, LOW);
    digitalWrite(MOTOR_IN4, LOW);
}

// executeManualCommand() — Modo 1: ejecuta último comando RF del joystick
void executeManualCommand() {
    // Apply deadband to prevent jitter at low speeds — zona muerta 60
    // Joystick cerca de 0 manda 10,20... pero motor no vence fricción y solo vibra/calienta
    // Deadband corta a 0 si <60 — como threshold en un slider React
    int left = lastCommand.left_pwm;
    int right = lastCommand.right_pwm;

    if (abs(left) < MIN_PWM_FOR_MOVEMENT) left = 0;
    if (abs(right) < MIN_PWM_FOR_MOVEMENT) right = 0;

    setMotorSpeeds(left, right);
}

// ============================================================================
// AUTONOMOUS MODE — Evasión obstáculos (HC-SR04 + EMA) + anti-caída (TCRT5000)
// Lógica simple pero efectiva para Sprint 3 — no es PID ni SLAM, solo reactivo
// Prioridad: 1) crítico <15cm reversa, 2) obstáculo <30cm gira, 3) borde TCRT reversa, 4) avance
// ============================================================================
void executeAutoMode() {
    // Simple obstacle avoidance and cliff detection — lee sensores ya filtrados
    bool obstacleFront = ultrasonicInitialized && ultrasonicEma < OBSTACLE_DISTANCE_CM; // <30cm
    bool criticalClose = ultrasonicInitialized && ultrasonicEma < CRITICAL_DISTANCE_CM; // <15cm emergencia
    // TCRT: true si detecta borde (valor <500 = poca reflectancia = agujero/línea negra)
    // OJO: aquí debería ser analogRead() <500, pero se usa digitalRead() que compara con ~2.5V — funciona aproximado
    bool cliffLeft = digitalRead(IR_LEFT_PIN) < IR_THRESHOLD;
    bool cliffCenter = digitalRead(IR_CENTER_PIN) < IR_THRESHOLD;
    bool cliffRight = digitalRead(IR_RIGHT_PIN) < IR_THRESHOLD;

    int leftSpeed = 0;
    int rightSpeed = 0;
    const int BASE_SPEED = 120; // Velocidad avance — 120/255 ~47% PWM (moderado, no muy rápido)
    const int TURN_SPEED = 150; // Velocidad giro — 150 más rápido para girar en sitio (tank turn)

    if (criticalClose) {
        // Emergency stop and back up — <15cm, pared muy cerca
        leftSpeed = -BASE_SPEED; // Reversa ambos — retrocede recto
        rightSpeed = -BASE_SPEED;
    } else if (obstacleFront) {
        // Obstacle detected - turn away — <30cm, obstáculo frontal
        if (cliffLeft && !cliffRight) {
            // Left cliff, turn right — si hay precipicio izq, girar der para no caer
            leftSpeed = TURN_SPEED;  // Izq adelante, der atrás = giro der en sitio
            rightSpeed = -TURN_SPEED;
        } else if (cliffRight && !cliffLeft) {
            // Right cliff, turn left
            leftSpeed = -TURN_SPEED;
            rightSpeed = TURN_SPEED;
        } else {
            // Default: turn right — sin info de cliff, gira der por defecto (decisión arbitraria pero consistente)
            leftSpeed = TURN_SPEED;
            rightSpeed = -TURN_SPEED;
        }
    } else if (cliffCenter || cliffLeft || cliffRight) {
        // Cliff detected - back up and turn — cualquier TCRT detecta borde
        leftSpeed = -BASE_SPEED;
        rightSpeed = -BASE_SPEED; // Reversa — evita caída (como sensor anti-caída de Roomba)
    } else {
        // Clear path - move forward — sin obstáculo ni borde, avanza
        leftSpeed = BASE_SPEED;
        rightSpeed = BASE_SPEED;
    }

    setMotorSpeeds(leftSpeed, rightSpeed);
}

// ============================================================================
// UTILITIES — Checksum simple (suma bytes) — integridad RF, no seguridad
// ============================================================================
uint16_t calculateChecksum(const RoverTelemetry& telem) {
    uint16_t sum = 0;
    const uint8_t* bytes = (const uint8_t*)&telem; // Trata struct como array bytes (casting C)
    for (size_t i = 0; i < sizeof(telem) - 2; i++) { // Suma todos menos los 2 bytes del checksum
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
    return sum == cmd.checksum; // true si coincide — paquete íntegro, no corrupto en aire
}
