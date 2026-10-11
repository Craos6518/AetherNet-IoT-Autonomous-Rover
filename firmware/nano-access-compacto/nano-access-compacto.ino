// =============================================================================
// nano-access-compacto.ino — Nodo Acceso Compacto (RF-1.3 / MOV-07) | Sprint 3
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años electrónica/Arduino (nRF24L01, UART, SoftwareSerial),
//              2 años Python (parsing), 2 años JS/React (WebSocket), 1 año C
// Analogía React: este sketch es como un `WebSocket echo server` + `presence hook`
// — escucha eventos (Bluetooth SPP) y emite push (PRESENCE/ACCESS) al cliente.
// Analogía Python: como `pyserial` listener con `json.loads` por línea.
// Analogía C: espejo de mega-access uart_protocol.cpp (CMD:TYPE:PARAMS) pero
// en NANO portable — misma convención "CMD:ACCESS:{\"pin\":\"1234\"}".
// FOSS: ArduinoJson 6.x + SoftwareSerial (avr) — RNF-3.1, sin SDK de nube.
// Origen: RF-1.3 — contingencia Bluetooth SPP para operar nodos críticos si
// cae el Wi-Fi (docs/requirements.md:18, docs/backlog.md:24 MOV-07).
// Hardware: `hardware-inventory.md:12` — Arduino Nano + FC-51 (presencia) + HC-06 (BT SPP).
//   Pines:
//     D2 = SoftwareSerial RX  <- HC-06 TX (blanco/tx)
//     D3 = SoftwareSerial TX  -> HC-06 RX (verde/rx)
//     D4 = FC-51 OUT (INPUT, activo LOW: LOW = objeto detectado)
//     D13 = LED_BUILTIN (feedback local PIN ok = parpadeo rápido)
//   HC-06 default: baud 9600, pareja 1234. ¡Sintonizar 9600 y no 38400!
// Protocolo (espejo gateway-esp32.ino:403 CMD:TYPE:PARAMS y uart_protocol.cpp):
//   BT/USB -> NANO : CMD:ACCESS:{"pin":"1234"}  ... valida PIN 4..6 dígitos
//   BT/USB -> NANO : POLL:PRESENCE              ... responde estado presencia
//   NANO -> BT/USB : ACCESS:{"user_id":"hc06_user","pin_hash":"7c78c98f","success":true,"source":"bluetooth"}
//   NANO -> BT/USB : PRESENCE:{"presence":true} (push al cambiar + respuesta POLL)
// El PIN nunca viaja EN CLARO de vuelta — solo hash djb2 (mismo esquema
// mega-access keypad_control.cpp:130 hashPin -> pin_hash 8 hex).
// =============================================================================

#include <SoftwareSerial.h> // HC-06 BT — como `serialport` en Python pero soft
// JSON manual (sin ArduinoJson): parsing trivial + RAM 2KB del Nano se cuida.
// Ventaja: compila igual con ArduinoJson 6.21.3 (CI) o 7.4.3 (local).

// --- Pines del nodo compacto (ver compendio fritzing hardware-inventory.md:12) ---
#define HC06_RX_PIN 2 // D2 RX software <- HC-06 TX  (entrada BT)
#define HC06_TX_PIN 3 // D3 TX software -> HC-06 RX  (salida BT)
#define FC51_PIN   4 // D4 FC-51 OUT — INPUT, activo LOW (objeto cerca = LOW)
#define LED_PIN    LED_BUILTIN // D13 — feedback local (como LedStatusCard de la app)

// --- BT SPP: HC-06 default 9600 — distinto del UART MEGA 38400 (config.h:42) ---
#define BT_BAUD 9600   // HC-06 baud fábrica — como `baudRate: 9600` en pyserial
#define DEBUG_BAUD 115200 // Serial USB monitoreo/programación (CH340 ttyUSB1)

// --- PIN cerrojo validación — espejo exacto mega-access config.h:48/49 ---
#define PIN_MAX_LEN 6  // max 6 — como config.h:49 PIN_MAX_LEN
static const char VALID_PIN[] = "1234"; // PIN correcto — mismo que MEGA (config.h:48)

// --- Timers no bloqueantes (como millis() pattern en C) ---
static unsigned long lastPresenceCheckMs = 0; // último muestreo FC-51
#define PRESENCE_CHECK_MS 100 // 10 Hz — suficiente para presencia discreta
static bool lastPresence = false; // estado previo — para push SOLO al cambiar

// Objeto SoftwareSerial para HC-06 — como `serial = serial.Serial(port, 9600)`
SoftwareSerial hc06(HC06_RX_PIN, HC06_TX_PIN); // RX, TX

// hashPin() — djb2 espejo de mega-access keypad_control.cpp:130 (pin_hash 8 hex)
// djb2 no es criptográfico — solo evita PIN en claro sobre BT/DB (como btoa)
static String hashPin(const String& pin) {
    unsigned long hash = 5381;
    for (size_t i = 0; i < pin.length(); i++) {
        hash = ((hash << 5) + hash) + (uint8_t)pin[i]; // hash*33 + byte
    }
    char buf[9];
    snprintf(buf, sizeof(buf), "%08lx", hash); // minúscula -> "7c78c98f" (idéntico a access-events)
    return String(buf);
}

// broadcast() — envía línea a ambos canales (BT al app + USB al monitor)
// Como escribir a `webSocket.emit` y `console.log` a la vez en JS.
static void broadcast(const String& line) {
    hc06.println(line);     // al HC-06 (app Android sobre BT SPP)
    Serial.println(line);   // al USB (debug/PRUEBA autónoma con serial)
}

// sendAccessResult() — construye ACCESS:{json} SIN PIN (solo hash) — como
// `response.json({ user_id, pin_hash, success, source })` en Express
static void sendAccessResult(bool success) {
    String out = String("{\"user_id\":\"hc06_user\",") +
                 "\"pin_hash\":\"" + hashPin(String(VALID_PIN)) + "\"," + // hash "1234" -> 7c78c98f
                 "\"success\":" + (success ? "true" : "false") + "," +
                 "\"source\":\"bluetooth\"" + "}";
    broadcast("ACCESS:" + out);
}

// sendPresence() — push o respuesta POLL — como `socket.emit('presence', bool)`
static void sendPresence(bool presence) {
    String out = String("PRESENCE:{\"presence\":") + (presence ? "true" : "false") + "}";
    broadcast(out);
}

// extractPin() — extrae valor de "pin" de {"pin":"1234"} SIN ArduinoJson
// Tolerante a espacios: { "pin" : "1234" } también funciona. Devuelve "" si no hay.
static String extractPin(const String& params) {
    int key = params.indexOf("\"pin\"");          // busca clave "pin"
    if (key < 0) return "";
    int colon = params.indexOf(':', key);         // ':' después de "pin"
    if (colon < 0) return "";
    int open = params.indexOf('"', colon + 1);    // comilla apertura del valor
    if (open < 0) return "";
    int close = params.indexOf('"', open + 1);    // comilla cierre
    if (close < 0) return "";
    return params.substring(open + 1, close);
}

// handleLine() — procesa una línea completa (BT o USB) — como router Express:
// "CMD:ACCESS:{\"pin\":\"1234\"}" (2 colons) o "POLL:PRESENCE" (1 colon)
static void handleLine(const String& line) {
    int firstColon = line.indexOf(':');
    if (firstColon < 0) return; // sin ':' — ruido, ignora

    String type = line.substring(0, firstColon); // "CMD" o "POLL"
    if (type == "CMD") {
        // "CMD:ACCESS:{json}" — segundo ':' separa ACCESS del payload
        int secondColon = line.indexOf(':', firstColon + 1);
        if (secondColon < 0) return;
        String cmd = line.substring(firstColon + 1, secondColon); // "ACCESS"
        String params = line.substring(secondColon + 1);          // {"pin":"1234"}
        if (cmd == "ACCESS") {
            // Parsea y valida {"pin":"1234"} — manual (sin ArduinoJson, RAM 2KB)
            String pin = extractPin(params);
            pin.trim();
            if (pin.length() < 4 || pin.length() > PIN_MAX_LEN) {
                broadcast("ACCESS:{...} bad payload"); // debug — no spoofear formato
                return;
            }
            bool ok = (pin == String(VALID_PIN));

            // REGLA DE SEGURIDAD RF: no reflejar el PIN — solo OK/FAIL (como no
            // loguear passwords). El gateway/MEGA tampoco lo hace (keypad_control.cpp:52).
            if (ok) {
                // Éxito: parpadeo LED 4x rápido (feedback local como MEGA LED verde)
                for (int i = 0; i < 4; i++) { digitalWrite(LED_PIN, HIGH); delay(80); digitalWrite(LED_PIN, LOW); delay(80); }
            } else {
                digitalWrite(LED_PIN, HIGH); delay(250); digitalWrite(LED_PIN, LOW); // fallo: destello largo
            }
            sendAccessResult(ok);
        }
    } else if (type == "POLL") {
        if (line.substring(firstColon + 1) == "PRESENCE") {
            sendPresence(digitalRead(FC51_PIN) == LOW); // respuesta inmediata al app
        }
    }
}

// handleHc06Channel() — lee líneas del HC-06 (BT) — buffer por canal, sin bloquear
static void handleHc06Channel() {
    static String line;
    while (hc06.available()) {
        char c = (char)hc06.read();
        if (c == '\n') { line.trim(); if (line.length() > 0) handleLine(line); line = ""; }
        else if (c != '\r') line += c;
    }
}

// handleUsbChannel() — lee líneas del USB (monitor / tester)
static void handleUsbChannel() {
    static String line;
    while (Serial.available()) {
        char c = (char)Serial.read();
        if (c == '\n') { line.trim(); if (line.length() > 0) handleLine(line); line = ""; }
        else if (c != '\r') line += c;
    }
}

void setup() {
    pinMode(FC51_PIN, INPUT);       // FC-51 — INPUT (activo LOW)
    pinMode(LED_PIN, OUTPUT);       // LED built-in D13
    digitalWrite(LED_PIN, LOW);

    Serial.begin(DEBUG_BAUD);       // USB CH340 — monitoreo/programación
    hc06.begin(BT_BAUD);            // HC-06 Bluetooth SPP — 9600 default

    // Banner al USB (no al BT: el app se conecta después y pide POLL)
    lastPresence = (digitalRead(FC51_PIN) == LOW);
    Serial.println(F("Nano Acceso Compacto listo (RF-1.3 MOV-07)"));
    Serial.println(F("HC-06: SPP 9600 D2=RX D3=TX | FC-51: D4 activo-LOW"));
    Serial.println(F("Comandos: CMD:ACCESS:{\"pin\":\"1234\"} | POLL:PRESENCE"));
    Serial.flush();
}

void loop() {
    // 1) Escucha BT (HC-06) y USB — comandos desde app o monitor serial
    handleHc06Channel();
    handleUsbChannel();

    // 2) Muestreo no bloqueante FC-51 cada 100 ms — push SOLO al cambiar estado
    unsigned long now = millis();
    if (now - lastPresenceCheckMs >= PRESENCE_CHECK_MS) {
        lastPresenceCheckMs = now;
        bool nowPresence = (digitalRead(FC51_PIN) == LOW);
        if (nowPresence != lastPresence) { // transición — push al app (y debug USB)
            lastPresence = nowPresence;
            sendPresence(nowPresence);
        }
    }
}