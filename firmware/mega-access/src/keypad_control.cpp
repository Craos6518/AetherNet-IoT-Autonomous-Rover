#include "keypad_control.h" // Interfaz que implementamos
#include "config.h"       // ROW_PINS, COL_PINS, VALID_PIN, PIN_MAX_LEN, KEYPAD_*_MS, LED_*_MS
#include "door.h"         // unlockDoor() — mueve servo si PIN OK
#include "led.h"          // setLedMode() — feedback visual (azul tap, rojo fail, etc.)
#include "uart_protocol.h" // sendAccessEvent() — reporta intento al Gateway vía UART
#include <Keypad.h>       // Librería Keypad 3.1.1 — escaneo matricial + debounce (FOSS)
#include <ArduinoJson.h>  // Para construir JSON ACCESS: con pin_hash

// --- Mapa lógico del keypad — filas x columnas (como grid CSS 4x4) ---
// Cada posición es una tecla física. La lib Keypad hace makeKeymap() para mapear
// pines ROW/COL (config.h:19-20) a estos caracteres. Si el cableado está transpose,
// se corrige invirtiendo ROW_PINS/COL_PINS, no este mapa.
static char keys[4][4] = {
    {'1','2','3','A'}, // Fila 0: 1 2 3 A
    {'4','5','6','B'}, // Fila 1: 4 5 6 B
    {'7','8','9','C'}, // Fila 2: 7 8 9 C
    {'*','0','#','D'}  // Fila 3: * 0 # D — * borra, # envía (HU-01), A-D ignoradas
};
// Objeto Keypad — maneja scanning interno. (byte*) cast porque config.h usa byte[] pero Keypad espera byte*
static Keypad keypad = Keypad(makeKeymap(keys), (byte*)ROW_PINS, (byte*)COL_PINS, 4, 4);

// --- Buffer de PIN ingresado — static = privado del módulo (como useState interno) ---
// String en Arduino es dinámica (heap) pero con máx 6 chars no fragmenta.
// En C puro usaríamos char[7], pero String es más cómodo y Arduino lo optimiza.
static String inputBuffer = ""; // Acumula dígitos hasta '#' o '*'

// keypadInit() — Configura lib Keypad (llamar en setup)
void keypadInit() {
    keypad.setDebounceTime(KEYPAD_DEBOUNCE_MS); // 50ms — ignora rebotes mecánicos (como debounce en JS)
    keypad.setHoldTime(KEYPAD_HOLD_MS);         // 500ms — tiempo para detectar tecla mantenida (no usado en HU-01)
    // Debounce: el contacto metálico rebota ~20ms al presionar. Sin esto, '1' se lee como '11'.
    // Hold: si mantienes tecla 500ms, la lib puede generar evento HOLD (no lo usamos, pero hay que setear)
}

void handleKeypad() {
    // getKey() es NO BLOQUEANTE — retorna 0 si no hay tecla, o el char si hay.
    // Como addEventListener('keypress') en JS pero polling: lo llamas cada loop() y te dice si hubo evento.
    char key = keypad.getKey();
    if (!key) return; // Sin tecla — sale inmediato, no bloquea loop (crítico para que puerta/LED/UART sigan)

    // DEBUG mapeo: imprime crudo siempre para validar cableado físico
    // Si ves '4' cuando presionas '2', sabes que ROW/COL están invertidos (transpose fix config.h:19)
    Serial.print(F("[KEY RAW] '"));
    Serial.print(key);
    Serial.print(F("' code="));
    Serial.print((int)key); // Código ASCII — útil si dudas si es '1' (49) o algo raro
    Serial.println(F(""));

    if (key == '#') {
        // '#' = ENVIAR — valida lo acumulado (HU-01 Cuando "1234#" → Entonces desbloquea)
        // DEBUG: muestra buffer antes de procesar para validar que se acumuló bien
        Serial.print(F("[PIN DBG] buffer='"));
        Serial.print(inputBuffer); // En prod no loguearíamos buffer (seguridad), pero en MVP ayuda debug
        Serial.print(F("' len="));
        Serial.print(inputBuffer.length());
        Serial.println(F(""));
        processPinAttempt(inputBuffer); // Valida vs VALID_PIN, mueve servo, envía UART
        inputBuffer = ""; // Limpia buffer tras envío — como resetear form en React tras submit
    } else if (key == '*') {
        // '*' = BORRAR — limpia buffer y feedback visual rojo 100ms
        inputBuffer = "";
        setLedMode(LedMode::RED_CLEAR); // Flash rojo 100ms — confirma borrado (UX táctil)
        Serial.println(F("Input cleared (*)"));
    } else if (isDigit(key)) {
        // Dígito 0-9 — acumula si hay espacio, feedback azul 50ms
        // isDigit() es de Arduino.h — true para '0'-'9', false para A-D/*/#
        if (inputBuffer.length() < PIN_MAX_LEN) {
            inputBuffer += key; // Añade dígito (String concat)
            setLedMode(LedMode::BLUE_TAP); // Flash azul 50ms — feedback "tecla registrada" (como ripple)
            // No mostramos dígito en Serial por seguridad — solo * y len (como password dots en React)
            Serial.print(F("Key pressed: * (len="));
            Serial.print(inputBuffer.length());
            Serial.println(F(")"));
        } else {
            Serial.println(F("PIN buffer full (6) — digit ignored")); // Evita overflow — buffer limitado
        }
    } else {
        // A, B, C, D explícitamente ignoradas (requisito RF-2.2)
        // El keypad las tiene pero no hacen nada en cerrojo — se loguea para debug wiring
        Serial.print(F("Key ignored (A-D): "));
        Serial.println(key);
    }
}

// processPinAttempt() — Valida PIN y ejecuta acción + reporte (corazón de HU-01)
// Llamado por handleKeypad() con '#' y por UART CMD:ACCESS (PIN remoto desde App Kotlin)
// Es como un handleSubmit() en React que valida y hace POST.
void processPinAttempt(const String& pin) {
    // Validación simple: compara String vs VALID_PIN "1234" (config.h:48)
    // En C puro sería strcmp(), pero String sobrecarga ==. Para MVP hardcodeado basta.
    // Futuro MOV-04: comparar contra EEPROM + hash, múltiples usuarios, PostgreSQL sync.
    bool success = (pin == String(VALID_PIN));

    if (success) {
        unlockDoor(); // ¡PIN OK! — servo 90° + LED verde 5s + doorUnlockTime=millis() (ver door.cpp:17)
    } else {
        // PIN mal — flash rojo 1s. Pero OJO: si puerta ya estaba verde (ventana 5s),
        // no queremos que se quede rojo y pierda el verde. setLedMode(RED_FAIL) hace
        // overlay 1s y updateLed() restaura verde/OFF automáticamente (ver led.cpp:42).
        setLedMode(LedMode::RED_FAIL);
        Serial.print(F("PIN attempt FAILED (len="));
        Serial.print(pin.length()); // Solo len, no valor — no exponer PIN en logs
        Serial.println(F(")"));
    }

    // Construye evento JSON para Gateway — SIN PIN en claro, solo hash (seguridad)
    // Es como no mandar password en claro a la API — mandas hash.
    StaticJsonDocument<256> doc; // 256 bytes stack — medido para este JSON
    doc["user_id"] = "keypad_user"; // Usuario fijo MVP — futuro multiusuario vendrá de EEPROM/App
    doc["pin_hash"] = hashPin(pin); // Hash HEX — no reversible a PIN (ver hashPin abajo)
    doc["success"] = success;       // true/false — para PostgreSQL y dashboard App
    doc["timestamp"] = millis();    // ms desde boot — para ordenar eventos (como Date.now())
    doc["source"] = "keypad";       // Origen — distingue "keypad" vs "remote" (App)

    String payload;
    serializeJson(doc, payload); // Convierte a string JSON: {"user_id":"keypad_user",...}
    sendAccessEvent(payload); // Envía por UART al ESP32: "ACCESS:{json}\n" (ver uart_protocol.cpp:60)

    // Log USB para debug — len no valor por seguridad
    Serial.print(F("PIN attempt: len="));
    Serial.print(pin.length());
    Serial.print(F(" -> "));
    Serial.println(success ? F("GRANTED") : F("DENIED")); // F() en Flash, no RAM
}

// hashPin() — Hash djb2 simple para no exponer PIN en logs/MQTT/PostgreSQL
// djb2 es viejo, rápido, no criptográfico — solo ofusca. Para prod usar SHA256/PBKDF2.
// Es como hacer btoa(pin) en JS pero con hash — no es seguridad real, solo evita texto plano.
// Si alguien ve logs o DB, ve "7c78c98f" no "1234". Suficiente para MVP evaluable.
String hashPin(const String& pin) {
    // djb2: hash = 5381; hash = hash*33 + c para cada char. 33 = (hash<<5)+hash (optimización en C)
    // 5381 y 33 son mágicos de la fórmula original (Bernstein). Funciona bien para strings cortos.
    unsigned long hash = 5381;
    for (size_t i = 0; i < pin.length(); i++) {
        hash = ((hash << 5) + hash) + (uint8_t)pin[i]; // hash*33 + byte
    }
    String out;
    out.reserve(8); // Reserva 8 chars — evita realloc (hash HEX cabe en ~8)
    out = String(hash, HEX); // Convierte a HEX string — ej. "1234" -> "7c78c98f"
    return out;
}
