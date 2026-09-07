/*
 * =============================================================================
 * AetherNet - Gateway ESP32  |  Proyecto Integrador 6º Semestre
 * Autor: Estudiante Tecnología en Desarrollo de Software + Ing. Sistemas (UTP)
 * Experiencia: 2 años electrónica/Arduino, 1 año C, 2 años Python/JS/React,
 *              1 año PostgreSQL | Enfoque 100% FOSS (RNF-3.1)
 * =============================================================================
 * QUÉ ES ESTO:
 *   El ESP32 es el "traductor universal" del sistema. Es el ÚNICO micro con
 *   WiFi, así que hace de puente entre 3 mundos que no se hablan entre sí:
 *     1) MQTT/WebSockets <-> Backend FastAPI + App React Native (Kotlin)
 *     2) UART Serial2     <-> Arduino MEGA (cerrojo físico)
 *     3) RF nRF24L01 SPI  <-> Rover UNO (tanque)
 *   Sin este gateway, el MEGA y el Rover quedarían aislados (uno no tiene
 *   radio, el otro no tiene WiFi). Es como un "API Gateway" en microservicios
 *   pero en hardware — si vienes de React, piensa en un proxy que traduce
 *   fetch() a WebSocket y a Serial.
 *
 * HARDWARE REAL (2 años soldando me enseñaron esto):
 *   - ESP32-WROOM-32U + nRF24L01. El nRF es 3.3V ONLY — si le metes 5V lo quemas.
 *   - El ESP32 tiene picos de corriente al transmitir WiFi que tumban la
 *     alimentación del nRF si comparten regulador sin condensador C1 10µF
 *     pegado al módulo (ver test-nrf24-esp32.ino:8). Lo aprendí quemando uno.
 *   - UART a MEGA necesita GND común + divisor 1k/2k en TX del MEGA (5V -> 3.3V)
 *
 * PINOUT FÍSICO (verificado con multímetro):
 *   nRF24L01: CE=GPIO5, CSN=GPIO15 (¡NO 18!), SCK=GPIO18, MOSI=GPIO23, MISO=GPIO19
 *   UART MEGA: TX=GPIO17 (hacia MEGA RX16), RX=GPIO16 (desde MEGA TX17)
 *
 * FLUJO HU-01 (Control de Acceso):
 *   MEGA keypad "1234#" -> UART "ACCESS:{pin_hash}" -> ESP32 -> HTTP POST
 *   http://backend:8000/api/access-events -> PostgreSQL (tabla access_events)
 *   -> MQTT aethernet/access/event -> App Kotlin actualiza UI
 *   Igual que un form React que hace POST a /api y refresca con WebSocket.
 * =============================================================================
 */

// --- Librerías base ---
#include <Arduino.h>      // Framework Arduino (setup/loop, Serial, millis, etc.)
#include <WiFi.h>         // WiFi ESP32 — 2.4GHz only (el ESP32 no hace 5GHz, como aprendí en Redes)
#include <HTTPClient.h>   // Cliente HTTP para POST al backend FastAPI (como fetch() en JS)
#include <PubSubClient.h> // Cliente MQTT — pub/sub desacoplado (como WebSocket pero más liviano)
#include <ArduinoJson.h>  // JSON sin malloc dinámico — StaticJsonDocument vive en stack (C puro)
#include <RF24.h>         // Driver nRF24L01 de TMRh20 (FOSS, ver RF24 library docs)
#include <SPI.h>          // Bus SPI hardware — 4 hilos, usado por nRF (SCK/MOSI/MISO/CSN)

// ============================================================================
// CONFIGURACIÓN — Credenciales en secrets.h (DEVOPS-11, PM-08)
// Patrón que aprendí en DevOps: nunca hardcodear secretos en git.
// secrets.h está en .gitignore; secrets.h.example es plantilla para CI/clones.
// Si no existe secrets.h (ej. CI), usamos defaults sanos para que compile.
// En C esto se hace con #if __has_include + #ifndef — en JS sería env vars.
// Ver firmware/gateway-esp32/secrets.h.example y docs/auditoria-secretos-sprint1.md H-01
// ============================================================================
#if __has_include("secrets.h")
#include "secrets.h" // Si existe (local), trae WIFI_SSID, WIFI_PASSWORD, MQTT_BROKER, etc.
#endif
// Cada #ifndef es un "fallback" — solo define si secrets.h no lo hizo. Así CI compila sin secretos reales.
#ifndef WIFI_SSID
#define WIFI_SSID "AetherNet-LAN" // SSID por defecto para CI (no hay router real en GitHub Actions)
#endif
#ifndef WIFI_PASSWORD
#define WIFI_PASSWORD "changeme" // Password dummy — en prod viene de secrets.h real
#endif
#ifndef MQTT_BROKER
#define MQTT_BROKER "192.168.1.100" // IP Mosquitto en LAN (en lab es 192.168.1.14, ver secrets.h)
#endif
#ifndef MQTT_PORT
#define MQTT_PORT 1883 // Puerto MQTT sin TLS (LAN cerrada, FOSS Mosquitto)
#endif
#ifndef MQTT_CLIENT_ID
#define MQTT_CLIENT_ID "gateway-esp32" // ID único en el broker — si duplicas, te kickean
#endif
#ifndef MQTT_USER
#define MQTT_USER "" // Sin auth en LAN lab (en prod iría user/pass)
#endif
#ifndef MQTT_PASS
#define MQTT_PASS ""
#endif
#ifndef BACKEND_HOST
#define BACKEND_HOST "192.168.1.100" // IP FastAPI (mismo host que Mosquitto en docker-compose)
#endif
#ifndef BACKEND_PORT
#define BACKEND_PORT 8000 // Puerto FastAPI (uvicorn)
#endif
#ifndef BACKEND_ACCESS_PATH
#define BACKEND_ACCESS_PATH "/api/access-events" // Endpoint HU-01 — POST AccessEventCreate -> PostgreSQL
#endif

// --- Pines nRF24L01 — ¡OJO! CSN 15 corrige colisión SCK 18 (DEVOPS-05 2026-09-01) ---
// Antes CSN estaba en 18 = mismo pin que SCK 18 -> SPI mudo, radio.begin() siempre false.
// Lo movimos a 15 y todo revivió. Lección: siempre matriz de pines antes de cablear.
#define NRF_CE_PIN 5      // Chip Enable — activa el nRF (OUT)
#define NRF_CSN_PIN 15    // Chip Select — SPI select (OUT), fix colisión

// --- UART hacia MEGA — 38400 + divisor 1k/2k MEGA 5V->ESP32 3.3V, GND común ---
// Probamos 115200 y daba framing errors por el divisor resistivo lento. 38400 es más tolerante.
// Es como bajar el baudrate de un fetch() para que no se corrompa en cable largo.
#define MEGA_SERIAL Serial2 // UART2 hardware del ESP32 (UART0 es USB debug)
#define MEGA_BAUD 38400     // Baudrate — ambos lados deben coincidir (ver mega-access/src/config.h:42)

// --- Topics MQTT — nombres como endpoints REST pero pub/sub ---
// En MQTT te suscribes a un topic y recibes push (como WebSocket). En REST haces poll.
// Mosquitto es el broker central (ver docker-compose.yml). ACL en mosquitto/acl.conf
#define TOPIC_ROVER_CMD "aethernet/rover/command"       // App -> Rover (joystick, modo)
#define TOPIC_ROVER_TELEMETRY "aethernet/rover/telemetry" // Rover -> App (US, IR, PWM)
#define TOPIC_ACCESS_CMD "aethernet/access/command"     // App -> MEGA (PIN remoto {"pin":"1234"})
#define TOPIC_ACCESS_EVENT "aethernet/access/event"     // MEGA -> App (fallback debug, principal es HTTP POST)
#define TOPIC_SECURITY_EVENT "aethernet/seguridad/intrusion" // MEGA -> Node-RED (láser HU-02, reservado)
#define TOPIC_SYSTEM_STATUS "aethernet/system/status"   // Gateway -> Todos (heartbeat)

// ============================================================================
// GLOBALES — Estado compartido entre setup() y loop()
// En React serían useState/useRef; aquí son variables globales porque no hay clases.
// Cuidado: en C estas viven en RAM estática, no en heap. Por eso usamos StaticJsonDocument.
// ============================================================================
WiFiClient wifiClient;              // Socket TCP base para MQTT (capa transporte)
PubSubClient mqttClient(wifiClient); // Cliente MQTT sobre WiFiClient (capa aplicación)
RF24 radio(NRF_CE_PIN, NRF_CSN_PIN); // Objeto nRF24L01 — maneja SPI internamente

const byte roverAddress[6] = "ROVER";   // Dirección RF del Rover (5 bytes + \0)
const byte gatewayAddress[6] = "GATEW"; // Dirección RF del Gateway (para ACK payload)

unsigned long lastMqttReconnect = 0; // Timestamp último intento MQTT (para throttling cada 5s)
unsigned long lastStatusPublish = 0; // Último heartbeat 30s
unsigned long lastRfCheck = 0;       // Último paquete RF válido (para rf_connected)

// --- Estructuras RF binarias — deben ser idénticas en rover-uno.ino ---
// #pragma pack(push,1) evita padding del compilador C. Sin esto, struct de 5 bytes
// ocuparía 6 por alineación a 2 bytes y el checksum fallaría siempre.
// Es como usar ArrayBuffer packed en JS — cada byte cuenta en RF.
#pragma pack(push, 1)
struct RoverCommand {
    int16_t left_pwm;    // -255 a 255 — PWM izquierda (negativo = reversa, como CSS transform)
    int16_t right_pwm;   // -255 a 255 — PWM derecha
    uint8_t mode;        // 0=stop, 1=manual (joystick), 2=auto (evasión)
    uint16_t checksum;   // Suma simple de bytes previos — detección corrupción RF
};

struct RoverTelemetry {
    int16_t left_pwm;      // Eco del comando recibido (para debug)
    int16_t right_pwm;
    uint16_t ultrasonic_cm; // Distancia HC-SR04 ya filtrada con EMA α=0.2 (HU-03)
    bool ir_left;          // TCRT5000 izquierda — true si detecta línea/borde
    bool ir_center;        // Centro
    bool ir_right;         // Derecha
    int8_t rf_rssi;        // Placeholder -70 dBm (RF24 no expone RSSI real)
    uint16_t checksum;     // Misma lógica suma
};
#pragma pack(pop) // Restaura alineación normal

// ============================================================================
// SETUP — Se ejecuta UNA vez al boot (como componentDidMount en React)
// Orden importa: UART primero, luego RF, luego WiFi, luego MQTT.
// Si inviertes, puedes bloquear HU-01 por esperar RF que no existe.
// ============================================================================
void setup() {
    Serial.begin(115200); // Debug USB — 115200 es estándar, rápido pero estable
    while (!Serial) delay(10); // Espera a que el monitor serial conecte (máx 10ms, no bloquea mucho)
    Serial.println("\n=== AetherNet Gateway ESP32 Starting ===");

    // 1) UART hacia MEGA — con pines explícitos (¡crítico!)
    // Si no pasas RX=16 TX=17, ESP32 usa pines por defecto y no llega nada al MEGA.
    // SERIAL_8N1 = 8 data bits, no parity, 1 stop bit (estándar)
    MEGA_SERIAL.begin(MEGA_BAUD, SERIAL_8N1, 16, 17); // RX=16, TX=17
    Serial.println("UART to MEGA initialized");

    // 2) nRF24L01 — NO BLOQUEANTE para HU-01
    // Antes había while(1) si fallaba, y dejaba el cerrojo muerto aunque no uses Rover.
    // Ahora solo WARN y sigue — HU-01 (cerrojo) no depende del Rover, así que no debe bloquear.
    // Esto es como no hacer throw si un microservicio opcional cae.
    if (!radio.begin()) {
        Serial.println("WARN: nRF24L01 not detected! RF rover deshabilitado, HU-01 sigue via UART/HTTP");
        // no while(1) — permite que WiFi/MQTT/HTTP sigan para cerrojo
    } else {
        radio.setPALevel(RF24_PA_HIGH);   // Potencia máxima — más alcance, más consumo
        radio.setDataRate(RF24_2MBPS);    // 2 Mbps — latencia <10ms (PRD KPI), menos alcance que 250Kbps
        radio.setChannel(76);             // Canal 76 = 2476 MHz — evita WiFi canal 1/6/11
        radio.openWritingPipe(roverAddress);   // Pipe TX hacia Rover
        radio.openReadingPipe(1, gatewayAddress); // Pipe RX 1 para telemetría (ACK payload)
        radio.enableAckPayload();         // Habilita telemetría piggyback en ACK (eficiente)
        radio.startListening();           // Modo RX por defecto (escucha telemetría)
        Serial.println("nRF24L01 initialized");
    }

    // 3) WiFi — NO BLOQUEANTE con timeout 10s
    // Antes while(WiFi.status()!=WL_CONNECTED) infinito -> si no hay router, nunca llegas a loop().
    // Ahora 10s y sigue — HU-01 vía UART no necesita WiFi, HTTP reintentará en loop().
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to WiFi ");
    Serial.print(WIFI_SSID);
    unsigned long wifiStart = millis(); // Timestamp inicio (como Date.now() en JS)
    while (WiFi.status() != WL_CONNECTED && millis() - wifiStart < 10000) {
        delay(500);
        Serial.print("."); // Feedback visual — cada punto = 500ms esperando
    }
    Serial.println();
    if (WiFi.status() == WL_CONNECTED) {
        Serial.printf("WiFi connected: %s\n", WiFi.localIP().toString().c_str());
    } else {
        Serial.println("WiFi NOT connected — continuando sin WiFi (HU-01 via UART seguirá, HTTP reintentará en loop)");
    }

    // 4) MQTT — configuración, no conexión aún (se conecta en loop())
    mqttClient.setServer(MQTT_BROKER, MQTT_PORT); // Broker Mosquitto en LAN
    mqttClient.setCallback(mqttCallback);         // Función que recibe mensajes (como onMessage en WebSocket)
    mqttClient.setBufferSize(1024);               // Buffer 1KB — suficiente para JSON telemetría

    // Publica estado inicial "online" (si MQTT ya conecta, genial; si no, loop lo reintentará)
    publishSystemStatus("online");
}

void loop() {
    // 1) Mantenimiento MQTT — reconecta cada 5s si cae (throttling evita spam)
    // Es como un reconnect WebSocket con backoff fijo.
    if (!mqttClient.connected()) {
        if (millis() - lastMqttReconnect > 5000) {
            mqttReconnect(); // Intenta conectar + suscribirse
            lastMqttReconnect = millis();
        }
    } else {
        mqttClient.loop(); // ¡CRÍTICO! Debe llamarse seguido o no llegan callbacks (como socket.poll())
    }

    // 2) RF con Rover — revisa si hay telemetría nueva (no bloquea)
    handleRfCommunication();

    // 3) UART con MEGA — lee líneas "TYPE:JSON\n" del cerrojo (no bloquea)
    handleMegaUart();

    // 4) Heartbeat cada 30s — para que App/Node-RED sepan que seguimos vivos
    // Similar a un ping de keepalive en WebSocket.
    if (millis() - lastStatusPublish > 30000) {
        publishSystemStatus("heartbeat");
        lastStatusPublish = millis();
    }

    delay(10); // 10ms = 100Hz loop — suficiente para RF/UART/MQTT sin quemar CPU
}

// ============================================================================
// MQTT CALLBACKS — Se ejecutan cuando llega un mensaje suscrito
// Como un event listener en JS: mqttClient.on('message', callback)
// OJO: payload NO viene null-terminated, hay que usar length explícito.
// ============================================================================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
    // Construir payloadStr con length explícito (sin asumir null-terminated)
    // En C, un char* no sabe su longitud; en JS un string sí. Aquí hay que copiar byte a byte.
    String topicStr = String(topic);
    String payloadStr;
    payloadStr.reserve(length + 1); // Reserva RAM para evitar realloc (optimización)
    for (unsigned int i = 0; i < length; i++) payloadStr += (char)payload[i];

    Serial.printf("MQTT RX: %s -> %s\n", topicStr.c_str(), payloadStr.c_str());

    // Ruteo por topic — como un switch de endpoints REST
    if (topicStr == TOPIC_ROVER_CMD) {
        handleRoverCommand(payloadStr); // App joystick -> RF al Rover
    } else if (topicStr == TOPIC_ACCESS_CMD) {
        handleAccessCommand(payloadStr); // App PIN remoto -> UART al MEGA
    }
}

void mqttReconnect() {
    Serial.print("MQTT reconnecting...");
    while (!mqttClient.connected()) {
        // Intenta conectar con ID + user/pass (si hay)
        if (mqttClient.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASS)) {
            Serial.println("connected");
            // Suscribe solo a topics vigentes (relés eliminados 2026-08-26 — sin hardware)
            mqttClient.subscribe(TOPIC_ROVER_CMD);
            mqttClient.subscribe(TOPIC_ACCESS_CMD);
            mqttClient.subscribe("aethernet/system/command"); // Comandos sistema
        } else {
            Serial.printf("failed, rc=%d retry in 5s\n", mqttClient.state());
            delay(5000); // Espera antes de reintentar (evita loop frenético)
        }
    }
}

// ============================================================================
// ROVER RF COMMUNICATION — Traducción MQTT JSON <-> RF binario
// Aquí convierto JSON humano (desde App) a struct binario compacto (para RF)
// y viceversa. Como transformar JSON a ArrayBuffer en JS.
// ============================================================================
void handleRfCommunication() {
    // Revisa si el Rover envió telemetría (vía ACK payload piggyback)
    if (radio.available()) {
        RoverTelemetry telem;
        radio.read(&telem, sizeof(telem)); // Lee bytes crudos a struct

        if (verifyChecksum(telem)) { // Valida que no se corrompió en el aire
            publishRoverTelemetry(telem); // Retransmite a MQTT para App/Backend
            lastRfCheck = millis(); // Actualiza timestamp para rf_connected
        }
        // Si checksum falla, se descarta silenciosamente — el próximo paquete lo corrige
    }

    // Send ACK payload if needed (handled automatically by RF24 with ack payload)
    // No hace falta TX explícito — el Rover precarga telemetría con writeAckPayload
}

void handleRoverCommand(String payload) {
    // Parsea JSON de App: {"left_pwm":120,"right_pwm":120,"mode":1}
    StaticJsonDocument<256> doc; // 256 bytes stack — suficiente para comando (medido)
    DeserializationError err = deserializeJson(doc, payload);
    if (err) {
        Serial.printf("JSON parse error: %s\n", err.c_str());
        return; // JSON malformado — descarta y espera próximo (no crashea)
    }

    // Construye struct binario para RF
    RoverCommand cmd;
    cmd.left_pwm = doc["left_pwm"] | 0;  // |0 = default 0 si falta (como ?? 0 en JS)
    cmd.right_pwm = doc["right_pwm"] | 0;
    cmd.mode = doc["mode"] | 0;          // 0=stop por defecto (seguro)
    cmd.checksum = calculateChecksum(cmd); // Calcula checksum sobre bytes previos

    // Envía vía RF — hay que cambiar de RX a TX y viceversa (half-duplex)
    radio.stopListening(); // Cambia a modo TX
    bool ok = radio.write(&cmd, sizeof(cmd)); // Bloquea ~5ms esperando ACK
    radio.startListening(); // Vuelve a RX para telemetría

    if (ok) {
        Serial.printf("RF TX: L=%d R=%d mode=%d\n", cmd.left_pwm, cmd.right_pwm, cmd.mode);
    } else {
        Serial.println("RF TX failed"); // No llegó ACK — Rover apagado o fuera de alcance
    }
}

void publishRoverTelemetry(const RoverTelemetry& telem) {
    // Convierte struct binario a JSON para MQTT (App lo espera en JSON)
    StaticJsonDocument<512> doc; // 512 para telemetría completa
    doc["left_pwm"] = telem.left_pwm;
    doc["right_pwm"] = telem.right_pwm;
    doc["ultrasonic_cm"] = telem.ultrasonic_cm; // Ya filtrado con EMA α=0.2 en UNO
    doc["ir_left"] = telem.ir_left;
    doc["ir_center"] = telem.ir_center;
    doc["ir_right"] = telem.ir_right;
    doc["rf_rssi"] = telem.rf_rssi;
    doc["timestamp"] = millis(); // Timestamp gateway (no del Rover)

    String output;
    serializeJson(doc, output); // JSON string
    mqttClient.publish(TOPIC_ROVER_TELEMETRY, output.c_str()); // Push a App/Node-RED
}

// ============================================================================
// MEGA UART COMMUNICATION — Puente entre mundo físico (cerrojo) y LAN
// Protocolo texto simple: "TYPE:JSON_PAYLOAD\n" a 38400 bd
// TYPE = ACCESS / SECURITY / STATUS. Es como un mini HTTP pero por Serial.
// ============================================================================
void handleMegaUart() {
    // Lee todas las líneas disponibles sin bloquear (while, no if)
    while (MEGA_SERIAL.available()) {
        String line = MEGA_SERIAL.readStringUntil('\n'); // Hasta \n (como readline en Python)
        line.trim(); // Quita \r \n espacios (el MEGA manda \r\n)
        if (line.length() == 0) continue; // Línea vacía — ignora
        Serial.printf("[MEGA UART RX] %s\n", line.c_str()); // Debug USB para ver qué envía MEGA
        processMegaMessage(line); // Rutea según TYPE
    }
    // Heartbeat debug si no hay datos 10s (ayuda a detectar cable suelto)
    static unsigned long lastDbg = 0;
    if (millis() - lastDbg > 10000) {
        lastDbg = millis();
        if (!MEGA_SERIAL.available()) {
            // Desactivado por defecto para no spamear, pero útil si UART parece muerto:
            // Serial.printf("[MEGA UART] idle — verifica TX 17↔RX16, GND común, 38400\n");
        }
    }
}

void forwardAccessToBackend(String payload); // Forward declaration (C necesita declarar antes de usar)

// Rutea mensaje del MEGA según prefijo antes de ':'
void processMegaMessage(String msg) {
    // Formato esperado: TYPE:PAYLOAD
    // e.g., ACCESS:{"user_id":"keypad_user","pin_hash":"abc","success":true,"source":"keypad"}
    //       SECURITY:{"event_type":"intrusion","sensor":"laser-01"} (reservado mega-laser HU-02)
    //       STATUS:{"door_locked":true,...}

    int colonIdx = msg.indexOf(':'); // Busca primer ':' (como split(':')[0] en JS)
    if (colonIdx < 0) return; // Sin ':' — mensaje corrupto, ignora

    String type = msg.substring(0, colonIdx); // Antes de ':'
    String payload = msg.substring(colonIdx + 1); // Después de ':'

    if (type == "ACCESS") {
        forwardAccessToBackend(payload); // ¡Flujo principal HU-01! HTTP POST a FastAPI
        // Fallback debug: también publica por MQTT si hay listeners (útil para debug con mosquitto_sub)
        if (mqttClient.connected()) {
            mqttClient.publish(TOPIC_ACCESS_EVENT, payload.c_str());
        }
    } else if (type == "SECURITY") {
        mqttClient.publish(TOPIC_SECURITY_EVENT, payload.c_str()); // Láser -> Node-RED Telegram
    } else if (type == "STATUS") {
        // Reenvía estado MEGA a MQTT para dashboard App
        mqttClient.publish("aethernet/mega/status", payload.c_str());
    }
}

void handleAccessCommand(String payload) {
    // Camino inverso: App -> Gateway (MQTT) -> MEGA (UART)
    // Payload debe ser {"pin":"1234"} — PIN remoto desde App Kotlin
    // Validación mínima antes de reenviar (no confíes en la red, valida siempre)
    StaticJsonDocument<128> tmp;
    if (deserializeJson(tmp, payload) || !tmp.containsKey("pin")) {
        Serial.printf("CMD:ACCESS bad payload (sin pin): %s\n", payload.c_str());
        return;
    }
    MEGA_SERIAL.println("CMD:ACCESS:" + payload); // Reenvía tal cual al MEGA por UART
}

void forwardAccessToBackend(String payload) {
    // FLUJO PRINCIPAL HU-01: HTTP POST directo a FastAPI — más rápido que MQTT bridge
    // ¿Por qué HTTP y no solo MQTT? Porque HTTP POST persiste directo en PostgreSQL
    // y devuelve 201, mientras MQTT necesita que backend esté suscrito. Más confiable.
    // Si WiFi caído, solo se encola por MQTT fallback (arriba) y se reintentará.
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("Backend POST skipped — WiFi down (MQTT fallback only)");
        return;
    }

    // Normaliza payload al schema AccessEventCreate del backend (ver backend/app/schemas.py)
    // El MEGA ya trae user_id, pin_hash, success, source; si viene incompleto se descarta
    // para no provocar 422 Unprocessable Entity en FastAPI.
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

    // Construye body mínimo permitido por backend (ignora timestamp/source extra si hay)
    // Es como hacer {user_id, pin_hash, success, source} = pick(doc, [...]) en JS
    StaticJsonDocument<256> body;
    body["user_id"] = doc["user_id"].as<const char*>();
    body["pin_hash"] = doc["pin_hash"].as<const char*>();
    body["success"] = doc["success"].as<bool>();
    body["source"] = doc.containsKey("source") ? doc["source"].as<const char*>() : "keypad";

    String jsonBody;
    serializeJson(body, jsonBody); // JSON string para HTTP

    // HTTP POST — como fetch('http://backend:8000/api/access-events', {method:'POST', body: jsonBody})
    HTTPClient http;
    String url = String("http://") + BACKEND_HOST + ":" + String(BACKEND_PORT) + BACKEND_ACCESS_PATH;
    http.begin(url);
    http.addHeader("Content-Type", "application/json");
    http.setTimeout(3000); // 3s timeout — no bloquea loop mucho
    int code = http.POST(jsonBody);
    if (code == 200 || code == 201) {
        Serial.printf("Backend ACCESS POST ok (%d): %s\n", code, jsonBody.c_str());
        // En PostgreSQL queda insertado en access_events (ver backend/app/models.py)
    } else {
        Serial.printf("Backend ACCESS POST fail code=%d url=%s body=%s\n", code, url.c_str(), jsonBody.c_str());
        if (code > 0) Serial.println(http.getString()); // Body de error FastAPI (útil para debug)
    }
    http.end(); // ¡Siempre cerrar! Libera socket (como res.close() en Node)
}

// ============================================================================
// SYSTEM STATUS — Heartbeat para observabilidad (como /health en FastAPI)
// Publica cada 30s en aethernet/system/status para que App y Node-RED sepan
// que el Gateway sigue vivo. Si no llega en 60s, algo murió.
// ============================================================================
void publishSystemStatus(const char* status) {
    StaticJsonDocument<256> doc;
    doc["status"] = status; // "online" al boot, "heartbeat" periódico
    doc["wifi_rssi"] = WiFi.RSSI(); // Intensidad WiFi dBm (-30 excelente, -90 malo)
    doc["free_heap"] = ESP.getFreeHeap(); // RAM libre — si baja mucho, hay leak
    doc["uptime_ms"] = millis(); // Tiempo desde boot — para detectar reinicios
    doc["rf_connected"] = (millis() - lastRfCheck < 5000); // true si RF llegó en últimos 5s

    String output;
    serializeJson(doc, output);
    mqttClient.publish(TOPIC_SYSTEM_STATUS, output.c_str());
}

// ============================================================================
// UTILITIES — Checksum simple para detectar corrupción RF
// No es CRC ni hash criptográfico — solo suma de bytes. Barato en CPU/RAM
// para UNO/ESP32. Detecta 99% de errores de un bit. Suficiente para RF corta.
// Es como un checksum de integridad, no de seguridad.
// ============================================================================
uint16_t calculateChecksum(const RoverCommand& cmd) {
    uint16_t sum = 0;
    const uint8_t* bytes = (const uint8_t*)&cmd; // Trata struct como array de bytes (casting en C)
    for (size_t i = 0; i < sizeof(cmd) - 2; i++) { // Suma todos menos los 2 bytes del checksum
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
    return sum == telem.checksum; // true si coincide — paquete íntegro
}
