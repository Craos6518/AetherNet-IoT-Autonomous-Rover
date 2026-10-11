# Firmware AetherNet — Documentación Técnica

> **Autor:** Andres Felipe Martinez Henao
> **Experiencia:** 2 años electrónica y Arduino | 1 año C | 2 años Python | 2 años HTML/CSS/JS/React | 1 año PostgreSQL  
> **Proyecto Integrador:** AetherNet IoT & Autonomous Rover — 100% FOSS (RNF-3.1)  
> **Stack firmware:** C++ (Arduino Framework) + PlatformIO + arduino-cli (CI)  
> **Fecha doc:** 2026-09-07 — Sprint 2 en curso (Sprint 1 cerrado 2026-09-01)  
> **Ramas vigentes:** `feature/firmware-mega-cerrojo` (RF-2.2/HU-01) | `feature/firmware-mega-laser` reservada HU-02 | Rover HU-03/HU-04

---

## 0. Cómo leer este documento

Si vienes de **web (React)** como yo, piensa en cada firmware como un **componente con estado y ciclo de vida** — igual que un `useEffect` + `useState`, pero sin sistema operativo: `setup()` es el montaje y `loop()` es el render loop a 100 Hz. Si vienes de **electrónica**, es la lógica que corre *bare-metal* sobre el micro, donde cada `delay()` te puede costar un evento perdido y cada `analogWrite()` es PWM real sobre un pin.

Este README cubre **todo** lo que hay en `firmware/` línea por línea. Referencio cada archivo como `ruta:línea` para que puedas ir directo en VS Code / PlatformIO.

---

## 1. Visión general y mapa de carpetas

```
firmware/
├── gateway-esp32/          # ESP32-WROOM-32U — Gateway central LAN ↔ RF ↔ UART
│   ├── gateway-esp32.ino   # 424 líneas — cerebro de enrutamiento (ver §3)
│   ├── platformio.ini      # esp32dev, PubSubClient, ArduinoJson 6.21.3, RF24 1.4.1
│   ├── secrets.h           # gitignoreado — credenciales LAN reales (ver §3.1)
│   ├── secrets.h.example   # plantilla commiteable para CI y nuevos clones
│   └── secrets.h.bak       # backup local (no usar en CI)
├── mega-access/            # Arduino MEGA 2560 — Cerrojo físico Edge (ver §4)
│   ├── mega-access.ino     # 42 líneas — wrapper setup/loop que orquesta 4 módulos
│   ├── platformio.ini      # megaatmega2560, Keypad 3.1.1, Servo 1.2.1, ArduinoJson 6.21.3
│   └── src/
│       ├── config.h        # 64 líneas — única fuente de verdad de pines y constantes
│       ├── door.h/.cpp     # Servo MG90S + auto-lock no bloqueante 5 s
│       ├── led.h/.cpp      # LED RGB ánodo común, máquina de estados con timers
│       ├── keypad_control.h/.cpp  # keypad 4x4, buffer 6, hash djb2, validación PIN
│       └── uart_protocol.h/.cpp   # protocolo TYPE:JSON\n hacia ESP32, throttling STATUS
├── rover-uno/              # Arduino UNO — Rover tanque (ver §5)
│   ├── rover-uno.ino       # 384 líneas — RF + L298N + HC-SR04 EMA + TCRT5000 + failsafe
│   ├── src/rover.ino       # duplicado legacy del anterior (no compila separado, solo ref)
│   └── platformio.ini      # uno, RF24 1.4.1, NewPing 1.9.1
├── test-ema-uno/           # Banco B — solo HC-SR04 + EMA α=0.2, salida CSV para Plotter
│   └── test-ema-uno.ino    # 54 líneas — valida HU-03 sin RF ni motores
├── test-nrf24-uno/         # Banco A1 — solo nRF24L01 en UNO (aislar error DEVOPS-05)
│   └── test-nrf24-uno.ino  # 58 líneas — isChipConnected + printDetails
└── test-nrf24-esp32/       # Banco A2 — solo nRF24L01 en ESP32 (CSN 15 fix)
    └── test-nrf24-esp32.ino # 72 líneas — valida colisión SCK 18 corregida
```

**Decisión arquitectónica clave** (aprendida a las malas): Cada `platformio.ini` pinnea `ArduinoJson@6.21.3`. En CI (`ci.yml:90/97`) se instala esa misma versión. La 7.x rompe API (`StaticJsonDocument` → `JsonDocument`), y nos tumbó el build una vez. FOSS no significa "última versión siempre".

---

## 2. Arquitectura de comunicación — dónde vive cada protocolo

```
┌─────────────┐  RF 2.4GHz (nRF24L01)  ┌──────────────┐  UART 38400  ┌──────────────┐
│  ROVER UNO  │◄──────────────────────►│ GATEWAY ESP32│◄────────────►│  MEGA 2560   │
│  L298N+HCSR │  Canal 76, 2Mbps,      │ WiFi+MQTT+HTTP│ Serial2     │ Keypad+Servo │
│  TCRT5000   │  PA_HIGH, ACK payload  │ 192.168.1.14  │ TX17/RX16   │ LED RGB      │
└─────────────┘  RoverCommand/         └──────┬───────┘  TYPE:JSON\n └──────────────┘
                   RoverTelemetry               │ HTTP POST / MQTT
                                              ▼
                                    ┌──────────────────┐
                                    │ Backend FastAPI  │  aethernet/* topics
                                    │ Mosquitto 1883   │◄────► App / Node-RED
                                    │ PostgreSQL       │
                                    └──────────────────┘
```

**Por qué tres enlaces distintos** (lo que entendí en Sistemas / Redes):

| Enlace | Protocolo | Por qué no otro | Latencia objetivo |
|---|---|---|---|
| Gateway ↔ Rover | RF nRF24L01 (SPI) | WiFi tiene jitter; ESP-NOW comparte radio con MQTT. RF dedicado = latencia predecible para motores | <10 ms (`prd.md:50`) |
| Gateway ↔ MEGA | UART Serial2 | Están en el mismo panel a 20 cm. UART es punto a punto, sin stack IP, sin colisiones | ~1 ms a 38400 bd |
| Todo ↔ Backend/App | MQTT + HTTP POST | MQTT pub/sub desacopla productores/consumidores. HTTP POST para HU-01 es más directo que bridge MQTT (decisión 2026-08-26, ver `architecture.md:63`) | <50 ms |

---

## 3. Gateway ESP32 — `gateway-esp32/gateway-esp32.ino:1`

### 3.1 Qué hace y por qué existe

El ESP32 es el **único con WiFi** de los tres micros. Hace de traductor: habla **MQTT/HTTP** hacia la LAN y **UART/RF** hacia el hardware real. Sin él, el MEGA y el Rover estarían aislados (el MEGA no tiene radio, el UNO no tiene WiFi).

**Experiencia electrónica:** El nRF24L01 es de 3.3V. Si lo alimentas a 5V lo quemas. Y el ESP32 tiene picos de corriente al transmitir WiFi que pueden hacer caer la alimentación del nRF si comparten regulador sin condensador. Por eso el `C1 10µF ≤5mm del nRF` que se menciona en `test-nrf24-esp32.ino:8`.

### 3.2 Credenciales — `secrets.h` vs `secrets.h.example:1`

```cpp
// gateway-esp32.ino:25-57 — patrón __has_include + #ifndef defaults
#if __has_include("secrets.h")
#include "secrets.h"       // credenciales reales LAN (gitignoreado)
#endif
#ifndef WIFI_SSID
#define WIFI_SSID "AetherNet-LAN"  // fallback CI sin secrets.h
#endif
```

- `secrets.h:7-18` tiene `FELIPE./2516f751` + `192.168.1.14` (broker/backend Docker host del lab). **Nunca se commitea** (`.gitignore:219` + `docs/auditoria-secretos-sprint1.md` H-01 rotado 2026-08-31).
- `secrets.h.example:1` es la plantilla. CI hace `cp secrets.h.example secrets.h` si falta (`ci.yml:102-104`).
- En mi flujo local hago `cp secrets.h.example secrets.h` y edito solo `secrets.h`.

### 3.3 Pines y hardware — `gateway-esp32.ino:60-65`

```cpp
#define NRF_CE_PIN 5        // CE  — GPIO5
#define NRF_CSN_PIN 15      // CSN — GPIO15 (fix colisión SCK, ver 3.6)
#define MEGA_SERIAL Serial2 // UART2
#define MEGA_BAUD 38400     // no 115200 (ver 3.6)
```

| Componente | Pines ESP32 | Nota crítica |
|---|---|---|
| nRF24L01 SPI | CE=5, CSN=15, SCK=18, MOSI=23, MISO=19 | VSPI por defecto del ESP32 |
| UART a MEGA | RX=16, TX=17, 38400 bd | Cruzado: ESP32 TX17 → MEGA RX16, ESP32 RX16 ← MEGA TX17 + **GND común** obligatorio |

### 3.4 Setup — `gateway-esp32.ino:113-160`

Orden de inicialización (importa):

1. `Serial.begin(115200)` — debug USB.
2. `MEGA_SERIAL.begin(38400, SERIAL_8N1, 16, 17)` — UART a MEGA con pines explícitos (`gateway-esp32.ino:119`). Sin esto el ESP32 usaría UART2 por defecto en otros pines.
3. `radio.begin()` — **no bloqueante** (`gateway-esp32.ino:123-135`). Si falla, imprime `WARN` y sigue. Antes había `while(1)` que dejaba el cerrojo muerto si el nRF no estaba conectado. HU-01 no depende del Rover, así que no debe bloquear.
4. `WiFi.begin()` — timeout 10 s (`gateway-esp32.ino:140-151`). No bloqueante infinito. Si cae WiFi, HU-01 sigue vía UART y reintenta HTTP en `loop()`.
5. `mqttClient.setServer/setCallback` — `PubSubClient` con buffer 1024 (`gateway-esp32.ino:156`).

### 3.5 Loop — `gateway-esp32.ino:162-186`

Ciclo a ~100 Hz (`delay(10)` al final):

```cpp
void loop() {
    // 1. MQTT reconnect cada 5s si desconectado
    // 2. handleRfCommunication() — revisa ACK payload del Rover
    // 3. handleMegaUart() — lee líneas TYPE:JSON\n del MEGA
    // 4. publishSystemStatus() cada 30s
}
```

**No bloqueante por diseño:** Nada usa `delay()` largo. Todo es por `millis()` comparado. Si vienes de JS, es como evitar `await sleep(5000)` en el hilo principal — bloqueas el event loop.

### 3.6 RF hacia Rover — `gateway-esp32.ino:226-281`

Paquetes binarios empaquetados (`#pragma pack(push,1)` para evitar padding del compilador C — sin esto `sizeof(RoverCommand)` daría 6 en vez de 5 por alineación):

```cpp
// gateway-esp32.ino:90-108
struct RoverCommand {
    int16_t left_pwm;   // -255..255
    int16_t right_pwm;  // -255..255
    uint8_t mode;       // 0=stop, 1=manual, 2=auto
    uint16_t checksum;  // suma simple bytes previos
};
struct RoverTelemetry {
    int16_t left_pwm; int16_t right_pwm;
    uint16_t ultrasonic_cm;
    bool ir_left, ir_center, ir_right;
    int8_t rf_rssi;
    uint16_t checksum;
};
```

- `handleRoverCommand(payload)` (`gateway-esp32.ino:241-265`): Parsea JSON MQTT `{"left_pwm":120,"right_pwm":120,"mode":1}`, calcula checksum, hace `radio.stopListening() → radio.write() → radio.startListening()`. El `stopListening` es obligatorio — el nRF no puede TX y RX a la vez.
- `handleRfCommunication()` (`gateway-esp32.ino:226-239`): Si `radio.available()`, lee `RoverTelemetry`, verifica checksum, publica a `aethernet/rover/telemetry`.

**Bug histórico corregido — CSN 15 vs 18:**
`gateway-esp32.ino:61` usa `CSN=15`. El diseño original usaba `CSN=18` que colisiona con `SCK=18` (mismo GPIO). SPI se quedaba mudo, `radio.begin()` devolvía false. Se movió a 15 y se documentó en `test-nrf24-esp32.ino:7` y `plano-sprint1-nrf24-reapertura.md:30`. Lección: siempre verificar que CE/CSN/SCK/MOSI/MISO no repitan pin.

**Checksum simple** (`gateway-esp32.ino:408-415`): Suma de bytes. No es CRC, pero detecta corrupción RF básica con costo casi cero en UNO/ESP32. Patrón: `suma todos los bytes menos los 2 del checksum y compara`.

### 3.7 UART hacia MEGA — `gateway-esp32.ino:286-387`

```cpp
void handleMegaUart() {
    while (MEGA_SERIAL.available()) {
        String line = MEGA_SERIAL.readStringUntil('\n'); // TYPE:JSON\n
        line.trim();
        processMegaMessage(line);
    }
}
void processMegaMessage(String msg) {
    int colonIdx = msg.indexOf(':');
    String type = msg.substring(0, colonIdx);    // ACCESS / SECURITY / STATUS
    String payload = msg.substring(colonIdx+1);  // JSON
    if (type=="ACCESS") forwardAccessToBackend(payload);
    else if (type=="SECURITY") mqttClient.publish(TOPIC_SECURITY_EVENT, payload.c_str());
    else if (type=="STATUS") mqttClient.publish("aethernet/mega/status", payload.c_str());
}
```

- `forwardAccessToBackend(payload)` (`gateway-esp32.ino:343-387`): **Flujo principal HU-01**. No es MQTT bridge, es `HTTP POST http://192.168.1.14:8000/api/access-events` directo con `HTTPClient`. Más rápido, y queda persistido en PostgreSQL aunque Mosquitto esté caído. Solo si WiFi está caído hace skip y deja el fallback MQTT (`gateway-esp32.ino:346-348`).
- Valida que el JSON tenga `user_id`, `pin_hash`, `success` antes de postear. Si falta algo, loguea y descarta — evita `500` del backend.
- `handleAccessCommand(payload)` (`gateway-esp32.ino:332-341`): Camino inverso App → MEGA. Valida que tenga `pin`, reenvía `MEGA_SERIAL.println("CMD:ACCESS:"+payload)`.

**Baud 38400 no 115200:** `gateway-esp32.ino:65` y `mega-access/src/config.h:42` usan 38400. A 115200 con divisor resistivo 1k/2k (MEGA 5V → ESP32 3.3V) había framing errors. A 38400 la señal es más tolerante. El `README` viejo decía 115200 y estaba desactualizado.

### 3.8 MQTT — `gateway-esp32.ino:191-221`

```cpp
void mqttCallback(char* topic, byte* payload, unsigned int length) {
    // Construye String con length explícito — payload NO es null-terminated
    for (i<length) payloadStr += (char)payload[i];
    if (topic=="aethernet/rover/command") handleRoverCommand(payloadStr);
    else if (topic=="aethernet/access/command") handleAccessCommand(payloadStr);
}
void mqttReconnect() {
    // subscribe solo a topics vigentes (relés eliminados)
    mqttClient.subscribe(TOPIC_ROVER_CMD);
    mqttClient.subscribe(TOPIC_ACCESS_CMD);
    mqttClient.subscribe("aethernet/system/command");
}
```

Topics (`gateway-esp32.ino:68-73`):

| Topic | Dirección | Uso Sprint 2 |
|---|---|---|
| `aethernet/rover/command` | App → Rover | Joystick (Sprint 3) |
| `aethernet/rover/telemetry` | Rover → App | Telemetría HC-SR04/TCRT |
| `aethernet/access/command` | App → MEGA | PIN remoto `{"pin":"1234"}` |
| `aethernet/access/event` | MEGA → App (fallback) | Solo debug, principal es HTTP POST |
| `aethernet/seguridad/intrusion` | MEGA → Node-RED | Reservado HU-02 láser |
| `aethernet/system/status` | Gateway → Todos | Heartbeat cada 30s |
| `aethernet/mega/status` | MEGA → Gateway | `door_locked`, `free_ram`, `uptime_ms` cada 5s |

### 3.9 `platformio.ini:1` Gateway

```ini
[env:esp32dev]
platform = espressif32
board = esp32dev
framework = arduino
lib_deps = PubSubClient@^2.8, ArduinoJson@6.21.3, RF24@^1.4.1
build_flags = -DCORE_DEBUG_LEVEL=3
```

---

## 4. MEGA Access — `mega-access/` (HU-01 / RF-2.2)

### 4.1 Filosofía Edge — por qué todo es local

El MEGA **no necesita WiFi ni backend para abrir la puerta**. Teclado → validación → servo → LED todo corre en el MEGA. Solo *reporta* el evento al Gateway por UART. Si se cae el router, la puerta sigue funcionando (`prd.md:61` Contingencia). Esto lo entendí comparando con React: es como tener estado local (`useState`) que funciona offline y sincroniza al backend cuando hay conexión.

### 4.2 `mega-access.ino:1` — Orquestador

```cpp
#include "src/config.h"
#include "src/led.h"
#include "src/door.h"
#include "src/keypad_control.h"
#include "src/uart_protocol.h"

void setup() {
    Serial.begin(115200);
    ledInit(); doorInit(); uartInit(); keypadInit();
    sendStatusToGateway();
}
void loop() {
    handleKeypad();        // lee tecla si hay
    handleDoorAutoLock();  // millis() >= 5000 → lock
    updateLed();           // máquina de estados LED
    handleGatewayUart();   // CMD:ACCESS / CMD:STATUS
    sendPeriodicStatus();  // cada 5s throttled
}
```

Cada función es **no bloqueante**. `loop()` corre a cientos de Hz. Ningún `delay()` largo.

### 4.3 `src/config.h:1` — Única fuente de verdad

Este archivo es el que más duele si está mal. Todo el pinout y constantes viven aquí:

```cpp
// Keypad 4x4 — fix transpose 2026-08-26
static const byte ROW_PINS[4] = {30, 32, 34, 36};
static const byte COL_PINS[4] = {22, 24, 26, 28};
// Antes estaban invertidos (22,24,26,28 como rows). El cableado físico
// invierte filas/columnas y (0,1) daba '4' en vez de '2'. Se corrigió invirtiendo ROW/COL.

// Servo MG90S
#define SERVO_PIN 9
static const int SERVO_LOCKED = 0;    // 0°  = bloqueada
static const int SERVO_UNLOCKED = 90; // 90° = desbloqueada (HU-01 Then)

// LED RGB ánodo común — cátodos a 44/45/46 vía 220Ω a GND
#define LED_R_PIN 44
#define LED_G_PIN 45
#define LED_B_PIN 46
#define LED_COMMON_ANODE true  // LOW enciende, HIGH apaga (255 - valor)

// Parámetros HU-01
static const char VALID_PIN[] = "1234";           // MVP hardcodeado, MOV-04 migrará a EEPROM
static const uint8_t PIN_MAX_LEN = 6;
static const unsigned long DOOR_AUTO_LOCK_MS = 5000;   // ventana desbloqueo = LED verde
static const unsigned long STATUS_INTERVAL_MS = 5000;  // throttled, antes 100ms spam
static const unsigned long KEYPAD_DEBOUNCE_MS = 50;
static const unsigned long LED_RED_FAIL_MS = 1000;
static const unsigned long LED_BLUE_TAP_MS = 50;
```

**Detalles que aprendí en electrónica:**

- **Ánodo común** (`config.h:33`): El pin común del LED va a 5V, cada cátodo (R/G/B) va a un pin PWM vía resistencia. `analogWrite(0)` = 5V en el pin = 0V diferencial = apagado. Por eso `led.cpp:22` hace `255 - valor` para invertir.
- **PWM en MEGA:** Pines 44/45/46 son PWM (`analogWrite` funciona). Si usas un pin no-PWM, `analogWrite` hace digital HIGH/LOW y el color no mezcla.
- **Buffer 6** (`PIN_MAX_LEN`): El PIN MVP es "1234" pero el buffer permite hasta 6 para probar. Si llega 7mo dígito, se ignora (`keypad_control.cpp:58`).
- **STATUS 5s no 100ms:** Al inicio spameaba STATUS cada 100ms y saturaba UART/MQTT. Se throttled a 5000 ms (`config.h:55`).

### 4.4 `src/door.h:1` / `src/door.cpp:1` — Servo MG90S

```cpp
// door.h
void doorInit(); void unlockDoor(); void lockDoor();
void handleDoorAutoLock(); bool isDoorUnlocked();

// door.cpp
static Servo doorServo;
static bool doorUnlocked = false;
static unsigned long doorUnlockTime = 0;

void doorInit() { doorServo.attach(SERVO_PIN); doorServo.write(SERVO_LOCKED); }
void unlockDoor() {
    doorServo.write(SERVO_UNLOCKED); // 90°
    doorUnlocked = true;
    doorUnlockTime = millis();
    setLedMode(GREEN_UNLOCKED); // LED verde sincronizado
}
void handleDoorAutoLock() {
    if (doorUnlocked && millis() - doorUnlockTime >= DOOR_AUTO_LOCK_MS) lockDoor();
}
void lockDoor() {
    doorServo.write(SERVO_LOCKED); // 0°
    doorUnlocked = false;
    setLedMode(OFF);
}
```

- `Servo.h` usa Timer1 del MEGA. No interfiere con `analogWrite` en 44/45/46 (Timer5).
- `handleDoorAutoLock()` se llama cada `loop()` — no usa `delay(5000)`. Si usara delay, el keypad quedaría congelado 5 s.
- El LED verde dura exactamente `DOOR_AUTO_LOCK_MS` porque `unlockDoor()` pone `GREEN_UNLOCKED` y `lockDoor()` lo apaga. Están acoplados a propósito para el BDD `Entonces LED pasa a verde` (`requirements.md:60`).

### 4.5 `src/led.h:1` / `src/led.cpp:1` — LED RGB no bloqueante

Máquina de estados con 5 modos:

```cpp
// led.h:9
enum class LedMode : uint8_t { OFF, GREEN_UNLOCKED, RED_FAIL, BLUE_TAP, RED_CLEAR };
```

| Modo | Color | Duración | Cuándo |
|---|---|---|---|
| `OFF` | apagado | persistente | puerta bloqueada en reposo |
| `GREEN_UNLOCKED` | verde `0,255,0` | 5 s (hasta `lockDoor`) | PIN correcto — sincronizado a puerta |
| `RED_FAIL` | rojo `255,0,0` | 1 s | PIN erróneo |
| `BLUE_TAP` | azul `0,0,255` | 50 ms | cada dígito pulsado (feedback) |
| `RED_CLEAR` | rojo | 100 ms | `*` borra buffer |

**Lógica no bloqueante** (`led.cpp:42-88`):

```cpp
void setLedMode(LedMode mode) {
    if (mode==GREEN_UNLOCKED || mode==OFF) { // persistentes — sin timer
        currentMode = mode; previousPersistentMode = mode; modeDurationMs=0;
    } else { // transitorios — guardan el persistente para restaurar
        previousPersistentMode = currentMode;
        currentMode = mode; modeStartMs = millis();
        modeDurationMs = (mode==RED_FAIL?1000 : mode==BLUE_TAP?50 : 100);
    }
    applyMode(mode);
}
void updateLed() {
    // Si transitorio venció → restaura verde/OFF según isDoorUnlocked()
    if (modeDurationMs>0 && millis()-modeStartMs >= modeDurationMs) {
        currentMode = isDoorUnlocked()?GREEN_UNLOCKED:OFF;
        applyMode(currentMode);
    }
    // Si puerta cambió y no hay transitorio activo → refleja
    if (modeDurationMs==0 && currentMode != expectedPersistent) { ... }
}
```

**Por qué es así:** Si pones PIN mal mientras la puerta está verde (ventana 5 s), debe hacer flash rojo 1 s y **volver a verde**, no quedarse en rojo ni apagarse. `previousPersistentMode` guarda el verde para restaurarlo. Esto lo validé con `led.cpp:64-88`.

`setLedColor()` (`led.cpp:19-30`) invierte para ánodo común:

```cpp
void setLedColor(uint8_t r,g,b) {
#ifdef LED_COMMON_ANODE
    analogWrite(LED_R_PIN, 255 - r); // 0 brillo máximo
#else
    analogWrite(LED_R_PIN, r);
#endif
}
```

### 4.6 `src/keypad_control.h:1` / `src/keypad_control.cpp:1` — Teclado 4x4

```cpp
// keypad_control.cpp:10-16
static char keys[4][4] = {
    {'1','2','3','A'},
    {'4','5','6','B'},
    {'7','8','9','C'},
    {'*','0','#','D'}
};
static Keypad keypad = Keypad(makeKeymap(keys), (byte*)ROW_PINS, (byte*)COL_PINS, 4, 4);
static String inputBuffer = "";
```

`handleKeypad()` (`keypad_control.cpp:26-65`):

- `keypad.getKey()` — no bloqueante, retorna 0 si no hay tecla.
- `'#'` → `processPinAttempt(inputBuffer)` + clear buffer.
- `'*'` → clear buffer + `setLedMode(RED_CLEAR)` (flash 100 ms).
- `isDigit(key)` → si `buffer.length < 6` añade y `BLUE_TAP` 50 ms, si no ignora.
- `A,B,C,D` → explícitamente ignoradas (requisito RF-2.2). Se loguea `Key ignored` para debug de cableado.

`processPinAttempt(pin)` (`keypad_control.cpp:67-97`):

```cpp
void processPinAttempt(const String& pin) {
    bool success = (pin == String(VALID_PIN)); // "1234"
    if (success) unlockDoor();
    else setLedMode(RED_FAIL);

    StaticJsonDocument<256> doc;
    doc["user_id"] = "keypad_user";
    doc["pin_hash"] = hashPin(pin); // no PIN en claro
    doc["success"] = success;
    doc["timestamp"] = millis();
    doc["source"] = "keypad";
    serializeJson(doc, payload);
    sendAccessEvent(payload); // UART → Gateway
}
String hashPin(const String& pin) {
    unsigned long hash = 5381; // djb2
    for (i<pin.length()) hash = ((hash<<5)+hash) + pin[i];
    return String(hash, HEX); // ej "1234" → 7c78c98f
}
```

**Por qué djb2 y no bcrypt:** Es un hash simple para no exponer el PIN en logs/MQTT/PostgreSQL. No es criptográfico — para el MVP basta. En producción iría `PBKDF2` o al menos `SHA256` y validación en backend, pero eso es MOV-04 futuro con EEPROM multiusuario.

**Validación BDD HU-01** (`requirements.md:57-60`):

```
Dado puerta bloqueada (doorInit 0°)
Cuando "1234#" (handleKeypad → processPinAttempt success)
Entonces servo 90° (unlockDoor) + LED verde 5s + ACCESS:JSON vía UART
```

Verificado con `mosquitto_sub -h 192.168.1.14 -t aethernet/#` y `curl /api/access-events?limit=3` (`sprints.md:53`).

### 4.7 `src/uart_protocol.h:1` / `src/uart_protocol.cpp:1` — UART

Formato de línea: `TYPE:JSON_PAYLOAD\n` a 38400 bd por `Serial2`.

**MEGA → Gateway (TX):**

| Prefijo | JSON ejemplo | Cuándo |
|---|---|---|
| `ACCESS:` | `{"user_id":"keypad_user","pin_hash":"7c78c98f","success":true,"timestamp":12345,"source":"keypad"}` | cada `processPinAttempt` |
| `STATUS:` | `{"door_locked":true,"laser_armed":false,"free_ram":7123,"uptime_ms":98765}` | cada 5 s + al boot + on `CMD:STATUS` |

**Gateway → MEGA (RX):**

| Comando | Ejemplo | Acción |
|---|---|---|
| `CMD:ACCESS:{"pin":"1234"}` | desde App vía `aethernet/access/command` | `processPinAttempt("1234")` |
| `CMD:STATUS` | poll del Gateway | `sendStatusToGateway()` |
| `CMD:RELAY` / `CMD:LASER` | — | **eliminados** en esta rama (sin hardware, ver `config.h:37` y `uart_protocol.cpp:53-56` ignora silencioso) |

```cpp
// uart_protocol.cpp:23-58
void processGatewayCommand(const String& cmd) {
    if (!cmd.startsWith("CMD:")) return;
    int firstColon = cmd.indexOf(':');
    int secondColon = cmd.indexOf(':', firstColon+1);
    String type = (secondColon<0) ? cmd.substring(firstColon+1)
                                  : cmd.substring(firstColon+1, secondColon);
    String params = (secondColon<0) ? "" : cmd.substring(secondColon+1);
    if (type=="ACCESS") { deserializeJson(doc, params); processPinAttempt(doc["pin"]); }
    else if (type=="STATUS") sendStatusToGateway();
    else Serial.print("CMD ignored (unknown type): "+type);
}
void sendPeriodicStatus() {
    if (millis()-lastStatusMs >= STATUS_INTERVAL_MS) { sendStatusToGateway(); lastStatusMs=millis(); }
}
int freeMemory() { // AVR heap check
    extern int __heap_start, *__brkval; int v;
    return (int)&v - (__brkval==0 ? (int)&__heap_start : (int)__brkval);
}
```

**Throttling STATUS:** Antes se enviaba cada 100 ms y llenaba el buffer UART del ESP32. Ahora 5000 ms (`config.h:55`).

### 4.8 `platformio.ini:1` MEGA

```ini
[env:megaatmega2560]
platform = atmelavr
board = megaatmega2560
framework = arduino
lib_deps = Keypad@^3.1.1, Servo@^1.2.1, ArduinoJson@^6.21.3
```

Sin `RF24` — el MEGA no tiene radio. Esa lib solo va en Gateway y Rover.

---

## 5. Rover UNO — `rover-uno/rover-uno.ino:1` (RF-3.1/3.2/3.3, HU-03/HU-04)

### 5.1 Qué hace

Tanque oruga con tracción diferencial (L298N), evasión de obstáculos (HC-SR04 + EMA α=0.2), anti-caída (3x TCRT5000), y control RF desde el Gateway. **Fail-safe 500 ms** (HU-04): si no llega RF válido, corta motores.

### 5.2 Pines — `rover-uno.ino:33-54` (revisado, conflicto corregido)

```cpp
// L298N — 2 PWM + 4 dirección
#define MOTOR_ENA 5   // PWM izquierda (OC0B)
#define MOTOR_IN1 6   // dir izq
#define MOTOR_IN2 7
#define MOTOR_IN3 8   // dir der
#define MOTOR_IN4 9   // PWM compatible (OC1A)
#define MOTOR_ENB 11  // PWM derecha (OC2A) — movido de 10 para liberar CSN

// HC-SR04
#define ULTRASONIC_TRIG 2
#define ULTRASONIC_ECHO 3  // INT1 en UNO, usado por NewPing con pulseIn
#define MAX_DISTANCE_CM 200

// TCRT5000 — analógico, threshold 500 (calibrar con cartulina blanca/negra)
#define IR_LEFT_PIN A0
#define IR_CENTER_PIN A1
#define IR_RIGHT_PIN A2
#define IR_THRESHOLD 500

// nRF24L01 — SPI hardware UNO: SCK13, MOSI11, MISO12 + CE4/CSN10
#define NRF_CE_PIN 4
#define NRF_CSN_PIN 10  // conflicto original con ENB=10 resuelto moviendo ENB a 11
```

**Lección electrónica (2 años Arduino):** En UNO los pines PWM son 3,5,6,9,10,11. Si pones `ENB=10` y `CSN=10` a la vez, el L298N y el nRF pelean el mismo pin y ninguno funciona. El comentario `rover-uno.ino:11-22` documenta el razonamiento. El fix fue `ENB→11` (también PWM) y dejar `CSN=10` libre. Siempre hacer matriz de pines antes de cablear.

| Componente | Pines UNO | Conflicto original | Solución |
|---|---|---|---|
| L298N ENA/ENB | 5/11 (PWM) | ENB 10 choque con nRF CSN | ENB movido a 11 |
| L298N IN1-4 | 6,7,8,9 | — | — |
| HC-SR04 | TRIG 2, ECHO 3 | — | — |
| TCRT5000 | A0,A1,A2 | — | — |
| nRF24L01 SPI | CE 4, CSN 10, SCK 13, MOSI 11, MISO 12 | CSN 10 vs ENB 10 | ENB→11 (MOSI 11 comparte con SPI pero ENB es PWM, no SPI — funciona porque L298N no usa SPI) |

> **Nota:** `MOSI 11` es tanto `MOTOR_ENB` como `SPI MOSI`. En UNO el 11 es compartido. Esto funciona porque el L298N ENB solo lee PWM, y el nRF solo usa MOSI durante `radio.write/read` (SPI transaction). Pero si ves jitter en motor al transmitir RF, es por esto — ideal sería mover ENB a 3 o 9, pero 9 ya es IN4. Queda como deuda documentada.

### 5.3 Paquetes RF — `rover-uno.ino:83-101` (idénticos al Gateway)

```cpp
#pragma pack(push,1)
struct RoverCommand { int16_t left_pwm, right_pwm; uint8_t mode; uint16_t checksum; };
struct RoverTelemetry { int16_t left_pwm, right_pwm; uint16_t ultrasonic_cm;
                        bool ir_left, ir_center, ir_right; int8_t rf_rssi; uint16_t checksum; };
#pragma pack(pop)
```

Gateway y Rover deben compilar con el mismo `pack(1)` o `sizeof` no coincide y el checksum falla siempre.

### 5.4 Setup — `rover-uno.ino:114-152`

1. `pinMode` motores + `stopMotors()` — seguridad: arranca detenido.
2. `pinMode` IR como `INPUT` (sin pullup — TCRT5000 da tensión analógica).
3. `radio.begin()` + `setPALevel(HIGH) / setDataRate(2MBPS) / setChannel(76) / enableAckPayload / startListening`. Si falla, `Serial.println ERROR` pero sigue (para test sin nRF).
4. `prepareAckPayload()` — precarga telemetría para que el Gateway la reciba en el ACK del próximo `write`.

### 5.5 Loop 100 Hz — `rover-uno.ino:157-185`

```cpp
void loop() {
    handleRfCommands();  // 1. lee RF si available
    checkFailsafe();     // 2. 500ms sin RF → stop
    readSensors();       // 3. HC-SR04 EMA + IR
    if (!failsafeActive) {
        if (currentMode==1) executeManualCommand();
        else if (currentMode==2) executeAutoMode();
    } else stopMotors();
    updateAckPayload();  // 5. escribe telemetría para próximo ACK
    delay(10);
}
```

### 5.6 RF — `rover-uno.ino:190-246`

```cpp
void handleRfCommands() {
    if (radio.available()) {
        radio.read(&cmd, sizeof(cmd));
        if (verifyChecksum(cmd)) {
            lastCommand = cmd; currentMode = cmd.mode;
            lastValidPacketTime = millis(); failsafeActive = false;
        } else Serial.println("Checksum error");
    }
}
void checkFailsafe() {
    if (millis() - lastValidPacketTime > FAILSAFE_TIMEOUT_MS) { // 500
        failsafeActive = true; currentMode=0; stopMotors();
    }
}
RoverTelemetry buildTelemetry() {
    telem.ultrasonic_cm = (uint16_t)ultrasonicEma; // EMA filtrado
    telem.ir_left = digitalRead(IR_LEFT_PIN) < IR_THRESHOLD;
    // ... center, right
    telem.rf_rssi = -70; // placeholder — RF24 no expone RSSI real
    telem.checksum = calculateChecksum(telem);
}
```

**Fail-safe HU-04** (`rover-uno.ino:214-223`, `requirements.md:80-87`): 500 ms sin paquete válido → `stopMotors()` y `currentMode=0`. No retoma hasta nuevo `handleRfCommands` válido. Esto evita que el tanque siga recto contra una pared si se cae el RF. Probado con `mosquitto_pub` cortando TX.

**ACK payload:** `radio.writeAckPayload(1, &telem, sizeof(telem))` (`rover-uno.ino:226/231`) envía telemetría *piggyback* en el ACK hardware del nRF. No necesita `radio.write()` separado — ahorra un slot de TX y mantiene telemetría aunque el Rover esté en `startListening`.

### 5.7 EMA — `rover-uno.ino:65-66` / `rover-uno.ino:251-264` (HU-03 / RNF-2.1)

Fórmula (`prd.md:51`, `architecture.md:75`):

```
S_t = α·Y_t + (1-α)·S_{t-1}   con α=0.2
```

```cpp
#define EMA_ALPHA 0.2f
float ultrasonicEma = 0; bool ultrasonicInitialized = false;

void readSensors() {
    unsigned int raw = sonar.ping_cm(); // NewPing, 0 = sin eco
    if (raw>0 && raw<=MAX_DISTANCE_CM) {
        if (!ultrasonicInitialized) { ultrasonicEma = raw; ultrasonicInitialized = true; }
        else ultrasonicEma = EMA_ALPHA * raw + (1.0-EMA_ALPHA) * ultrasonicEma;
    }
}
```

- `ping_cm()` ya filtra timeout. Si `raw==0` se mantiene `ultrasonicEma` anterior (no cae a 0).
- KPI `prd.md:51`: reducción ruido >85%. Con α=0.2, un pico de 100 cm se atenúa a 20 cm en la primera muestra y se diluye en ~5 muestras (~250 ms a 20 Hz). Suficiente para no frenar por ruido, pero responde a obstáculo real.
- El prototipo offline está en `stats/ema_filter.py:15` y el banco `test-ema-uno.ino:42` valida con Serial Plotter.

### 5.8 Motores — `rover-uno.ino:269-315`

```cpp
void setMotorSpeeds(int leftPwm, rightPwm) {
    leftPwm = constrain(leftPwm, -255, 255);
    if (leftPwm>=0) { digitalWrite(IN1,HIGH); digitalWrite(IN2,LOW); }
    else { digitalWrite(IN1,LOW); digitalWrite(IN2,HIGH); leftPwm=-leftPwm; }
    analogWrite(ENA, leftPwm);
    // igual derecha con IN3/IN4/ENB
}
void executeManualCommand() {
    int l=lastCommand.left_pwm, r=lastCommand.right_pwm;
    if (abs(l)<60) l=0; if (abs(r)<60) r=0; // deadband fricción
    setMotorSpeeds(l,r);
}
```

`MIN_PWM_FOR_MOVEMENT=60` (`rover-uno.ino:70`): Por debajo de 60 el motor no vence fricción oruga y solo zumba/calienta. Deadband evita jitter del joystick cerca de 0.

### 5.9 Autónomo — `rover-uno.ino:320-363` (RF-3.2)

```cpp
void executeAutoMode() {
    bool obstacleFront = ultrasonicEma < 30;     // OBSTACLE_DISTANCE_CM
    bool criticalClose = ultrasonicEma < 15;     // CRITICAL_DISTANCE_CM
    bool cliffLeft = analogRead(A0) < 500;
    // ...
    if (criticalClose) { leftSpeed=-120; rightSpeed=-120; } // reversa emergencia
    else if (obstacleFront) {
        if (cliffLeft && !cliffRight) { leftSpeed=150; rightSpeed=-150; } // giro
        else { leftSpeed=150; rightSpeed=-150; } // default giro dcha
    } else if (cliffCenter||cliffLeft||cliffRight) { leftSpeed=-120; rightSpeed=-120; }
    else { leftSpeed=120; rightSpeed=120; } // avance
    setMotorSpeeds(leftSpeed,rightSpeed);
}
```

Lógica simple pero efectiva para Sprint 3. Mejora futura: PID o bug algorithm. Por ahora evita obstáculo girando y evita caída revirtiendo.

### 5.10 `platformio.ini:1` Rover

```ini
[env:uno]
platform = atmelavr
board = uno
lib_deps = RF24@^1.4.1, NewPing@^1.9.1
```

### 5.11 `src/rover.ino` duplicado legacy

`rover-uno/src/rover.ino:1` es copia idéntica de `rover-uno.ino` (384 líneas). Queda como referencia pero **no compila como src** — PlatformIO compila `rover-uno.ino` en la raíz del sketch. No tocar ambos a la vez; editar solo `rover-uno.ino`.

---

## 6. Bancos de prueba aislados — `test-*/`

Aprendí que debuguear todo integrado es un infierno. Estos sketches prueban **un subsistema a la vez** sin depender de WiFi/MQTT/motores.

### 6.1 `test-ema-uno/test-ema-uno.ino:1` — Banco B (HU-03)

Solo HC-SR04 + EMA. Cableado `TRIG 2 / ECHO 3`. Salida CSV `raw,ema` para **Tools → Serial Plotter**.

```cpp
#define EMA_ALPHA 0.2f
unsigned int raw = sonar.ping_cm();
if (raw==0) raw = lastRaw; // mantiene último válido
ema = EMA_ALPHA*raw + (1-EMA_ALPHA)*ema;
Serial.print(raw); Serial.print(","); Serial.println(ema,1);
```

Uso: mueve cartón 10→100 cm frente al sensor, ves `raw` ruidosa vs `ema` suave en el Plotter. Valida `stats/ema_filter.py:17` sin necesidad de RF ni L298N.

### 6.2 `test-nrf24-uno/test-nrf24-uno.ino:1` — DEVOPS-05 lado UNO

Solo SPI/RF. Pines `CE 4 / CSN 10 / SCK13/MOSI11/MISO12`. Hace `radio.begin()` + `printDetails()` + loop `isChipConnected()` cada 2 s. Si ves `isChipConnected=1` y `printDetails` con registros ≠ `0x00/0xFF`, SPI comunica. Si ves `0xFF`, es MISO sin conexión; `0x00` es sin alimentación.

### 6.3 `test-nrf24-esp32/test-nrf24-esp32.ino:1` — DEVOPS-05 lado ESP32

Solo SPI/RF. Pines `CE 5 / CSN 15 / SCK18/MOSI23/MISO19` (fix CSN 15). Mismo patrón `isChipConnected()` cada 2 s + `write dummy` para probar TX sin receptor. Documenta el fix de colisión SCK 18 y el condensador `C1 10µF ≤5mm`.

**Flujo de validación Sprint 1** (`sprints.md:52`, `testing-rf-sprint1.md:32`):

1. Flashear `test-nrf24-esp32` → ver `OK: nRF24L01 initialized` en `ttyUSB0`.
2. Flashear `test-nrf24-uno` → ver `OK` en `ttyACM0`.
3. Flashear `gateway-esp32` + `rover-uno` → `mosquitto_pub -h 192.168.1.14 -t aethernet/rover/command -m '{"left_pwm":120,"right_pwm":120,"mode":1}'` → ver `RF TX: L=120` en ESP32 y `RF RX: L=120` en UNO.

---

## 7. Protocolos y formatos — referencia rápida

### 7.1 RF binario (Gateway ↔ Rover) — `firmware/README.md:96-125` original + `gateway-esp32.ino:90`

- Canal 76, 2 Mbps, PA HIGH, direcciones `"GATEW"/"ROVER"`, `enableAckPayload`.
- `RoverCommand` 5 bytes + `RoverTelemetry` ~14 bytes, `#pragma pack(1)`, checksum suma.
- Telemetría va como **ACK payload** — no necesita TX explícito del Rover.

### 7.2 UART texto (Gateway ↔ MEGA) — `uart_protocol.cpp:1`

```
MEGA → Gateway:  "ACCESS:{\"user_id\":\"keypad_user\",\"pin_hash\":\"7c78c98f\",\"success\":true,...}\n"
                 "STATUS:{\"door_locked\":true,\"laser_armed\":false,\"free_ram\":1234,\"uptime_ms\":56789}\n"
Gateway → MEGA:  "CMD:ACCESS:{\"pin\":\"1234\"}\n"
                 "CMD:STATUS\n"
```

Baud **38400** (`config.h:42`, `gateway-esp32.ino:65`), `Serial2` en ambos. Cruzado + GND común + divisor 1k/2k en MEGA TX→ESP32 RX (5V→3.3V).

### 7.3 MQTT topics — `gateway-esp32.ino:68-73` + `architecture.md:60`

| Topic | QoS | Retain | Descripción |
|---|---|---|---|
| `aethernet/rover/command` | 0 | N | Joystick / modo |
| `aethernet/rover/telemetry` | 0 | N | US cm + IR + PWM + RSSI |
| `aethernet/access/command` | 1 | N | PIN remoto |
| `aethernet/access/event` | 0 | N | Fallback debug (principal HTTP) |
| `aethernet/seguridad/intrusion` | 1 | N | Láser (reservado laser) |
| `aethernet/system/status` | 0 | N | Heartbeat ESP32 |
| `aethernet/mega/status` | 0 | N | Estado MEGA |

### 7.4 HTTP — `gateway-esp32.ino:343-387`

```
POST http://192.168.1.14:8000/api/access-events
Content-Type: application/json
{"user_id":"keypad_user","pin_hash":"7c78c98f","success":true,"source":"keypad"}
→ 201 Created (o 200) + {"id":...}
```

Validado con `curl` en `ci.yml:164-168` y `sprints.md:53` (`curl /api/access-events limit 3`).

---

## 8. Pinout consolidado — tabla única

### Gateway ESP32-WROOM-32U

| Función | Pin | Tipo | Nota |
|---|---|---|---|
| nRF CE | GPIO5 | OUT | — |
| nRF CSN | GPIO15 | OUT | fix de GPIO18 |
| nRF SCK | GPIO18 | SPI SCK | VSPI |
| nRF MOSI | GPIO23 | SPI MOSI | — |
| nRF MISO | GPIO19 | SPI MISO | — |
| UART RX | GPIO16 | IN | ← MEGA TX17 |
| UART TX | GPIO17 | OUT | → MEGA RX16 |

### MEGA 2560 — Cerrojo (RF-2.2/HU-01)

| Función | Pin | Tipo | Estado |
|---|---|---|---|
| Keypad rows | 30,32,34,36 | IN/OUT | Keypad lib |
| Keypad cols | 22,24,26,28 | IN/OUT | transpose fix |
| Servo MG90S | 9 | PWM | 0° lock / 90° unlock |
| LED R/G/B | 44,45,46 | PWM | ánodo común 220Ω |
| UART RX/TX | 16/17 | Serial2 | 38400 |
| Láser TX/RX | 8/7 | — | reservado laser |

### UNO — Rover

| Función | Pin | Tipo | Nota |
|---|---|---|---|
| ENA/ENB | 5/11 | PWM | ENB movido de 10 |
| IN1-4 | 6,7,8,9 | OUT | L298N dir |
| TRIG/ECHO | 2/3 | OUT/IN | HC-SR04 |
| IR L/C/R | A0/A1/A2 | ADC | threshold 500 |
| nRF CE/CSN | 4/10 | OUT | SPI 13/11/12 |

---

## 9. Flujos BDD trazados en código

### HU-01 — `requirements.md:53-60` Control de acceso

```
Dado puerta bloqueada
  → doorInit() servo 0° + LED OFF + doorUnlocked=false
Cuando usuario "1234#"
  → handleKeypad() buffer "1234" → processPinAttempt("1234")
  → success=true → unlockDoor() servo 90° + LED verde + doorUnlockTime=millis()
  → sendAccessEvent() → "ACCESS:{...pin_hash:7c78c98f...}" por Serial2 38400
  → Gateway handleMegaUart() → forwardAccessToBackend() POST 201 + publish aethernet/access/event
Entonces LED verde 5s + puerta desbloqueada
  → loop() handleDoorAutoLock() a los 5000ms → lockDoor() servo 0° + LED OFF
  → App PinViewModel recibe accessEventFlow → UI ✓ + LedStatusCard verde
  → Backend persiste en PostgreSQL → GET /api/access-events lo lista
```

### HU-03 — `requirements.md:71-78` EMA

```
Dado HC-SR04 ruidoso (picos 10-100 cm)
Cuando EMA α=0.2 en readSensors()
  → ultrasonicEma = 0.2*raw + 0.8*previa
Entonces señal estabilizada → executeAutoMode() no frena por pico aislado
  → telemetría ultrasonic_cm ya filtrada → backend /sensor-events → stats t-Student
```

### HU-04 — `requirements.md:80-87` Fail-safe

```
Dado Rover en movimiento (mode 1 o 2)
Cuando 500ms sin RF válido (lastValidPacketTime)
Entonces checkFailsafe() → failsafeActive=true → stopMotors() PWM 0 + IN LOW
  → permanece detenido hasta nuevo handleRfCommands válido
```

---

## 10. Dependencias, tooling y CI

### Librerías (todas FOSS, RNF-3.1)

| Lib | Versión pinneada | Dónde | Por qué pin |
|---|---|---|---|
| ArduinoJson | **6.21.3** | los 3 firmwares | 7.x cambia API, rompe CI |
| RF24 (TMRh20) | 1.4.1 | Gateway + Rover | — |
| PubSubClient | 2.8 | Gateway | MQTT 1883 |
| Keypad | 3.1.1 | MEGA | — |
| Servo | 1.2.1 | MEGA | — |
| NewPing | 1.9.1 | Rover | HC-SR04 no bloqueante |

### Compilación local (PlatformIO — recomendado)

```bash
# Requisitos: pip install platformio
cd firmware/gateway-esp32 && pio run
cd ../mega-access && pio run
cd ../rover-uno && pio run
# Ver tamaño: pio run -v (MEGA 19252 bytes ~7%, Rover 6900 ~21%)
```

### Compilación CI (arduino-cli — `ci.yml:58-111`)

```bash
arduino-cli compile --fqbn esp32:esp32:esp32 ./firmware/gateway-esp32
arduino-cli compile --fqbn arduino:avr:mega ./firmware/mega-access
arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno
# Matriz en ci.yml:64-66 compila los 3 en paralelo
```

### Flasheo

```bash
pio run -t upload -d firmware/gateway-esp32  # /dev/ttyUSB0 921600
pio run -t upload -d firmware/mega-access     # /dev/ttyACM0 115200
pio run -t upload -d firmware/rover-uno       # /dev/ttyACM1 115200
# o arduino-cli upload -p /dev/ttyUSB0 --fqbn esp32:esp32:esp32 firmware/gateway-esp32
```

### Monitor serial

```bash
pio device monitor -b 115200  # o arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=115200
# Gateway: [MEGA UART RX] ACCESS:... / RF TX: L=120 / Backend POST ok (201)
# MEGA: [KEY RAW] '1' / Door UNLOCKED (90°) / [UART TX] ACCESS:...
# Rover: RF RX: L=120 R=120 mode=1 / !!! FAIL-SAFE ACTIVATED !!!
```

---

## 11. Troubleshooting — lo que me costó aprender (2 años electrónica)

| Síntoma | Causa real encontrada | Fix |
|---|---|---|
| `WARN: nRF24L01 not detected!` en ESP32 | CSN en GPIO18 colisión con SCK 18 | Mover CSN a 15 (`gateway-esp32.ino:61`), test con `test-nrf24-esp32` |
| `ERROR: nRF24L01 not detected!` en UNO | VCC 3.3V sin condensador, o MISO sin continuidad | C 10µF ≤5mm + verificar SCK13/MOSI11/MISO12 continuidad |
| `Key (0,1)=2 → '4'` | Filas/columnas invertidas por cableado | Invertir ROW_PINS/COL_PINS (`config.h:19-20`), validar con `[KEY RAW]` log |
| UART sin datos | GND no común entre MEGA y ESP32 | Unir GNDs + divisor 1k/2k MEGA TX→ESP32 RX |
| Framing errors a 115200 | Divisor lento para 115200 | Bajar a 38400 (`config.h:42`) |
| Servo no gira a 90° | `Servo.attach(9)` sin `write(90)` tras attach | `doorInit` hace `write(SERVO_LOCKED)` + `unlockDoor write(90)` |
| LED no mezcla colores | Pin no PWM | Usar 44/45/46 (PWM MEGA) + `LED_COMMON_ANODE` invertido |
| Motor zumba no avanza | PWM <60 fricción oruga | `MIN_PWM_FOR_MOVEMENT 60` deadband (`rover-uno.ino:70/311`) |
| Checksum siempre falla | `#pragma pack` olvidado → sizeof distinto | `pack(push,1)` en ambos lados |
| CI falla `ArduinoJson 7` | `StaticJsonDocument` deprecado | Pinnear `6.21.3` en todos los `platformio.ini` y `ci.yml` |
| `WiFi NOT connected` pero UART sigue | Router caído | Esperado — diseño Edge: cerrojo sigue, HTTP reintenta en loop |

**Tip React/JS para debug:** Piensa en `Serial.println` como `console.log`. El `Serial Plotter` de Arduino IDE es como el `Chart.js` del `test-ema-uno` — te grafica `raw,ema` en tiempo real sin escribir frontend.

---

## 12. Seguridad y secretos

- `secrets.h` gitignoreado (`.gitignore:219`), `secrets.h.example` commiteable. CI usa fallback (`ci.yml:99-107`).
- `hashPin()` djb2 no es criptográfico — solo evita PIN en claro en logs/MQTT/DB. Para producción: migrar a `PBKDF2` + validación en backend + EEPROM con múltiples hashes (deuda MOV-04).
- `VALID_PIN="1234"` hardcodeado en `config.h:48` — MVP Sprint 2. MOV-04 migrará a EEPROM + endpoint `POST /api/access-events` con múltiples usuarios (fuente PostgreSQL, ver `backend/app/models.py`).
- `pin_hash` en `ACCESS:` no viaja el PIN — el Gateway hace `POST` con hash, no con PIN.

---

## 13. Roadmap firmware y deuda conocida

| Tema | Estado | Archivo/ID |
|---|---|---|
| Cerrojo RF-2.2/HU-01 | ✅ Done Sprint 2 | `mega-access/*`, `gateway-esp32.ino:343` |
| LED RGB local HU-01/HU-02 | ✅ Done | `led.*`, `config.h:30-34` |
| EMA α=0.2 HU-03 | ✅ Rover + test-ema | `rover-uno.ino:251`, `test-ema-uno.ino:42` |
| Fail-safe 500ms HU-04 | ✅ | `rover-uno.ino:214` |
| RF nRF24L01 DEVOPS-05 | ✅ validado HW | `test-nrf24-*`, `gateway/rover` |
| Láser KY-008 RF-2.3/HU-02 | ⏳ `feature/firmware-mega-laser` | `config.h:37` reservado, `SECURITY:` en `uart_protocol` |
| Relés 8ch | ❌ eliminado | `config.h:37` + `uart_protocol.cpp:53` — sin hardware 2026-08-26 |
| Tuya/bombillo RF-4.2 | ❌ cancelado ADR-001 | R-01 políticas API, viola RNF-3.1 |
| Múltiples PIN / EEPROM | 📋 MOV-04 futuro | `VALID_PIN` hardcodeado → EEPROM |
| TCRT5000 calibración fina | 📋 Sprint 3 | `IR_THRESHOLD 500` a calibrar con cartulinas |
| RSSI real nRF | 📋 | `rf_rssi=-70` placeholder (`rover-uno.ino:243`), RF24 no expone |
| rover-uno/src/rover.ino duplicado | 📋 deuda | Unificar a un solo sketch |
| ENB/MOSI comparten GPIO11 en UNO | 📋 deuda HW | Revisar si causa jitter PWM al TX RF |

---

## 14. Referencias cruzadas (para evaluadores)

- `docs/prd.md:50` KPIs latencia <50ms MQTT / <10ms RF / EMA α=0.2
- `docs/requirements.md:53-87` HU-01..HU-04 BDD Dado/Cuando/Entonces
- `docs/hardware-inventory.md:9` LED RGB local como único indicador visual (Tuya cancelado ADR-001)
- `docs/architecture.md:60-63` protocolos y flujo EMA
- `docs/sprints.md:53` estado verificado 2026-09-07 + `docs/testing-rf-sprint1.md:32`
- `docs/roadmap.md:106` C++ embebido como conocimiento transversal
- `docs/backlog.md:13` backlog operativo (MOV/DEVOPS/LOW/EST)
- `.github/workflows/ci.yml:58-111` pipeline arduino-cli
- `stats/ema_filter.py:15` prototipo Python del EMA (referencia para `rover-uno.ino:66`)

---

## 15. Glosario rápido (para quien viene de web)

| Término embebido | Equivalente web | Qué es |
|---|---|---|
| `setup()` / `loop()` | `componentDidMount` / `requestAnimationFrame` | Init una vez + loop infinito |
| `millis()` | `Date.now()` | ms desde boot, para timers no bloqueantes |
| `analogWrite(pin, 0-255)` | `element.style.opacity` | PWM — duty cycle, no DAC real |
| `SPI` | `WebSocket` binario | Bus síncrono maestro-esclavo, 4 hilos, muy rápido |
| `UART Serial2` | `fetch` texto | Serial asíncrono, 2 hilos + GND, `baud` = bps |
| `isChipConnected()` | `navigator.onLine` | ¿El chip responde por SPI? |
| `StaticJsonDocument` | `JSON.parse/stringify` | JSON en RAM estática (sin heap) |
| `PROGMEM F("")` | — | String en flash, no en RAM (AVR tiene 2KB RAM) |
| `#pragma pack(1)` | — | Sin padding de struct (como `ArrayBuffer` packed) |

---

*Documentado como estudiante 6º semestre que combina electrónica (soldar, medir 3.3V, entender ánodo común) con software (C sin GC, Python para stats, React para la app, PostgreSQL para el histórico). Cada decisión tiene su `por qué` y su `dónde` en el código — si algo no cuadra con `docs/` avísame antes de codear (regla `AGENTS.md:1`).*
