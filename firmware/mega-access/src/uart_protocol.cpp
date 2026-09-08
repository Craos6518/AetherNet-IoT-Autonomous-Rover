#include "uart_protocol.h" // Interfaz que implementamos
#include "config.h"       // GATEWAY_SERIAL (Serial2), GATEWAY_BAUD (38400), STATUS_INTERVAL_MS
#include "door.h"         // isDoorUnlocked() — para STATUS door_locked
#include "keypad_control.h" // processPinAttempt() — para CMD:ACCESS remoto
#include <ArduinoJson.h>  // Para parsear CMD:ACCESS json y construir STATUS json

// --- Timer para throttling STATUS — static privado (como useRef en React) ---
static unsigned long lastStatusMs = 0; // millis() último STATUS enviado

// uartInit() — Inicializa UART hacia ESP32 (llamar en setup)
void uartInit() {
    GATEWAY_SERIAL.begin(GATEWAY_BAUD); // Serial2.begin(38400) — MEGA TX17 -> ESP32 RX16, RX16 <- TX17
    lastStatusMs = millis(); // Inicializa timer — primer STATUS será en 5s, no inmediato (evita burst al boot)
    // Nota: GATEWAY_SERIAL es alias de Serial2 (config.h:41) — como const gatewaySerial = Serial2 en JS
}

// handleGatewayUart() — Lee comandos del ESP32 sin bloquear (llamar cada loop)
// Es el "listener" de comandos remotos — como socket.on('message') en JS pero polling.
void handleGatewayUart() {
    // while() lee TODAS las líneas disponibles sin bloquear — si hay 3 comandos, los 3 se procesan en mismo loop
    while (GATEWAY_SERIAL.available()) {
        String line = GATEWAY_SERIAL.readStringUntil('\n'); // Lee hasta \n (como readline en Python)
        line.trim(); // Quita \r, \n y espacios — ESP32 manda \r\n, MEGA manda \n
        if (line.length() == 0) continue; // Línea vacía (solo \n) — ignora
        processGatewayCommand(line); // Rutea según "CMD:TYPE:..."
    }
}

// processGatewayCommand() — Parsea "CMD:TYPE:PARAMS" y ejecuta
// Formato: "CMD:ACCESS:{\"pin\":\"1234\"}" o "CMD:STATUS" sin params
// Es como un router Express: app.post('/cmd/:type', handler) pero en Serial.
void processGatewayCommand(const String& cmd) {
    // Validación: debe empezar con "CMD:" — si no, no es para nosotros (ruido o debug)
    if (!cmd.startsWith(F("CMD:"))) return; // F() en Flash — no ocupa RAM

    // Parsea TYPE y PARAMS — split por ':' (como cmd.split(':') en JS)
    // Hay 2 casos: "CMD:STATUS" (1 colon) y "CMD:ACCESS:{json}" (2 colons)
    int firstColon = cmd.indexOf(':'); // Primer ':' tras CMD
    int secondColon = cmd.indexOf(':', firstColon + 1); // Segundo ':' si existe
    String type;
    String params;
    if (secondColon < 0) {
        // Sin segundo ':' — es "CMD:STATUS" (comando sin payload)
        type = cmd.substring(firstColon + 1); // Desde tras "CMD:" hasta fin
        params = "";
    } else {
        // Con segundo ':' — es "CMD:ACCESS:{json}"
        type = cmd.substring(firstColon + 1, secondColon); // Entre primer y segundo ':'
        params = cmd.substring(secondColon + 1); // Tras segundo ':' hasta fin (el JSON)
    }

    if (type == "ACCESS") {
        // PIN remoto desde App Kotlin: MqttManager.publishAccessCommand -> Gateway -> UART -> aquí
        // Params es JSON como {"pin":"1234"} — hay que parsearlo y validar
        StaticJsonDocument<128> doc; // 128 bytes — suficiente para {"pin":"1234"}
        DeserializationError err = deserializeJson(doc, params);
        if (!err && doc.containsKey("pin")) {
            String pin = doc["pin"].as<String>(); // Extrae "1234"
            pin.trim(); // Quita espacios por si App manda " 1234 "
            processPinAttempt(pin); // ¡Reusa misma validación que keypad físico! (ver keypad_control.cpp:67)
            // Así App y keypad comparten lógica — DRY, no duplicar validación
        } else {
            Serial.print(F("CMD:ACCESS bad json: "));
            Serial.println(params); // Log para debug — App mandó JSON malformado sin "pin"
        }
    } else if (type == "STATUS") {
        // Poll del Gateway — pide estado actual (door, RAM, uptime)
        sendStatusToGateway(); // Responde inmediato con STATUS:{json}
    } else {
        // RELAY/LASER eliminados en esta rama (sin hardware 2026-08-26) — ignora silencioso
        // Antes había CMD:RELAY y CMD:LASER, ahora no hay relés ni láser en cerrojo
        Serial.print(F("CMD ignored (unknown type): "));
        Serial.println(type);
    }
}

// sendAccessEvent() — Envía evento de acceso al Gateway (llamado por keypad_control.cpp:91)
// Formato: "ACCESS:{\"user_id\":\"keypad_user\",\"pin_hash\":\"abc\",...}\n"
void sendAccessEvent(const String& jsonPayload) {
    GATEWAY_SERIAL.print(F("ACCESS:")); // Prefijo TYPE:
    GATEWAY_SERIAL.println(jsonPayload); // JSON + \n — println añade \r\n
    // Debug USB para confirmar TX sin necesidad de ver UART2 con logic analyzer
    // En web sería console.log('[TX]', payload) — aquí Serial es el devtools
    Serial.print(F("[UART TX] ACCESS:"));
    Serial.println(jsonPayload);
}

// sendStatusToGateway() — Envía estado periódico al Gateway (cada 5s + al boot + on CMD:STATUS)
// El Gateway lo reenvía a MQTT aethernet/mega/status y App lo muestra en dashboard.
void sendStatusToGateway() {
    StaticJsonDocument<256> doc; // 256 bytes — para door_locked, free_ram, uptime, etc.
    doc["door_locked"] = !isDoorUnlocked(); // true si bloqueada (invertido de isDoorUnlocked)
    doc["laser_armed"] = false; // Reservado para mega-laser (HU-02) — fijo false en cerrojo
    doc["free_ram"] = freeMemory(); // RAM libre — si baja mucho, hay leak de String (MEGA 8KB)
    doc["uptime_ms"] = millis(); // Tiempo desde boot — para detectar reinicios (como uptime en Linux)

    String payload;
    serializeJson(doc, payload); // JSON string
    GATEWAY_SERIAL.print(F("STATUS:"));
    GATEWAY_SERIAL.println(payload); // "STATUS:{json}\n"
}

// sendPeriodicStatus() — Throttled cada STATUS_INTERVAL_MS (5000ms) — llamar cada loop
// Evita spam UART/MQTT. Antes era cada 100ms y saturaba. Ahora 5s es suficiente para dashboard.
// Es como throttling en JS: solo envía si pasaron 5s desde el último.
void sendPeriodicStatus() {
    unsigned long now = millis();
    if (now - lastStatusMs >= STATUS_INTERVAL_MS) { // ¿Pasaron 5s?
        sendStatusToGateway();
        lastStatusMs = now; // Resetea timer
    }
}

// freeMemory() — Calcula RAM libre en AVR (MEGA 2560)
// Truco AVR: heap crece hacia arriba desde __heap_start, stack crece hacia abajo desde RAMEND.
// La diferencia entre stack pointer (&v) y heap pointer (__brkval) es RAM libre.
// En JS/Python no te preocupas de heap/stack, aquí con 8KB RAM sí.
// Si freeMemory() baja de ~500 bytes, riesgo de crasheo por fragmentación de String.
int freeMemory() {
    extern int __heap_start, *__brkval; // Símbolos del linker AVR — inicio heap y tope actual
    int v; // Variable en stack — su dirección es el stack pointer actual
    return (int)&v - (__brkval == 0 ? (int)&__heap_start : (int)__brkval);
    // Si __brkval==0 no se ha hecho malloc aún, heap está en __heap_start
}
