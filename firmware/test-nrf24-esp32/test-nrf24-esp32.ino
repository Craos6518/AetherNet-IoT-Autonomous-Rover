/*
 * =============================================================================
 * AetherNet - Test nRF24L01 ESP32 (Validación Aislada DEVOPS-05) | 6º Semestre UTP
  * Autor: Andres Felipe Martinez Henao
 *        1 año C, 2 años JS/React) — Solo SPI/RF, sin WiFi/MQTT/UART a MEGA
 * Objetivo: Aislar "WARN: nRF24L01 not detected!" visto en gateway-esp32.ino:124
 * Cableado docs/fritzing/plano-sprint1-nrf24-reapertura.md:30
 *   CE→GPIO5, CSN→GPIO15 (corrige 18 colisión SCK), SCK→18, MOSI→23, MISO→19
 *   VCC→3V3 + C1 10µF (o 22µF) ≤5mm a GND, GND→GND, IRQ NC
 *   3.3V estable — si UNO 3.3V cae, usar AMS1117 externo (ESP32 3V3 es más fuerte)
 * =============================================================================
 * FIX HISTÓRICO CSN 18 -> 15:
 *   Diseño inicial usaba CSN=18 que colisiona con SCK=18 (mismo GPIO).
 *   SPI quedaba mudo — radio.begin() siempre false. Se movió a 15 y revivió.
 *   Este test valida el fix: si aquí da OK con CSN 15 y gateway también, el fix funciona.
 *   Lección: siempre matriz de pines antes de cablear — lo aprendí perdiendo 1 día.
 *
 * ELECTRÓNICA ESP32 vs UNO:
 *   - ESP32 3.3V es más fuerte (500mA) que UNO 50mA — nRF va mejor en ESP32.
 *   - SPI ESP32: VSPI por defecto SCK18/MOSI23/MISO19 (no 13/11/12 como UNO).
 *   - Condensador C1 10µF pegado al nRF filtra picos WiFi que tumbarían alimentación.
 * =============================================================================
 */

#include <SPI.h>  // SPI ESP32 — VSPI SCK18/MOSI23/MISO19 (fijos en ESP32)
#include <RF24.h> // Driver nRF24L01 TMRh20 (FOSS)

#define NRF_CE_PIN 5   // CE GPIO5 — libre, no colisiona con SPI
#define NRF_CSN_PIN 15 // CSN GPIO15 — FIX de 18 (SCK) a 15 (amarillo en plano)

RF24 radio(NRF_CE_PIN, NRF_CSN_PIN); // Objeto nRF

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(10);
  delay(1000); // Estabiliza alimentación — nRF tarda ~100ms en boot
  Serial.println("\n=== AetherNet nRF24L01 Validation ESP32 ===");
  Serial.printf("Pines: CE=%d CSN=%d SCK=18 MOSI=23 MISO=19\n", NRF_CE_PIN, NRF_CSN_PIN);
  // Log pines — verifica que el fix CSN 15 está aplicado (si ves 18, es código viejo)
  Serial.println("Iniciando SPI + radio.begin()...");

  if (!radio.begin()) {
    // begin() escribe registro CONFIG y lee de vuelta — si no coincide, SPI falla
    Serial.println("ERROR: nRF24L01 not detected! Revisa:");
    Serial.println("  1) VCC 3.3V + C1 10µF (franja - a GND) pegado al nRF (≤5mm)");
    Serial.println("  2) CSN movido de IO18 -> IO15 (cable amarillo) — verifica no sea 18 viejo");
    Serial.println("  3) SCK 18, MOSI 23, MISO 19, CE 5, GND continuidad");
    Serial.println("  4) 3.3V con multímetro en VCC-GND del nRF (no 5V — lo quemas)");
    // Consejos accionables — qué medir con multímetro y qué color de cable (ver plano)
    // No bloquear con while(1) — deja loop mostrar reintento cada 2s (como health check en web)
  } else {
    Serial.println("OK: nRF24L01 initialized"); // SPI OK — chip responde
    // Config idéntica a gateway-esp32.ino — deben coincidir para que se hablen
    radio.setPALevel(RF24_PA_HIGH); // Potencia máx
    radio.setDataRate(RF24_2MBPS);  // 2 Mbps baja latencia
    radio.setChannel(76);           // Canal 76
    radio.setRetries(5, 15);        // 5 reintentos, 15*250us delay — para TX dummy
    // Direcciones de prueba — no son ROVER/GATEW, solo para validar SPI sin interferir
    const byte addr[6] = "TEST1";
    radio.openWritingPipe(addr);
    radio.openReadingPipe(1, addr);
    radio.stopListening(); // Modo TX para printDetails (printDetails necesita TX para leer bien en algunas libs)

    Serial.println("--- radio.printDetails() ---");
    radio.printDetails(); // Dump registros — si ves 0x00/0xFF en todo, SPI no comunica (MISO/MOSI/SCK/CSN)
    Serial.println("--- Config OK ---");
    Serial.println("Si ves printDetails con registros != 0x00/0xFF, SPI OK.");
    Serial.println("Si ves 0x00 o 0xFF en todo, SPI no comunica (revisa MISO/MOSI/SCK/CSN).");
    // Guía para leer printDetails sin saber qué es cada registro — no necesitas ser experto RF
  }
}

void loop() {
  // Test periódico cada 2s — como setInterval en JS, pero con millis() no bloqueante
  static unsigned long last = 0;
  if (millis() - last > 2000) {
    last = millis();
    // Re-test sin reiniciar placa — útil para tocar cables y ver si revive sin reboot
    bool ok = radio.isChipConnected(); // Lee registro CONFIG y verifica bit — rápido
    Serial.printf("[%lu ms] isChipConnected=%d ", millis(), ok);
    if (!ok) {
      Serial.println("-> FAIL (revisa cableado/alimentación)");
    } else {
      Serial.println("-> OK");
      // Test TX dummy (sin ACK esperado, solo verifica que write no bloquea/crashea)
      // Si no hay receptor, write devuelve false (no ACK) — es normal, no es error
      const char payload[] = "hello";
      bool sent = radio.write(&payload, sizeof(payload)); // Intenta TX 5 bytes
      Serial.printf("  write dummy: %s (sin receptor, false es normal)\n", sent ? "OK" : "FAIL/no ACK");
      // Si sent==false sin receptor es esperado — no indica falla SPI, solo que nadie hizo ACK
      // Si SPI fallara, isChipConnected ya habría dado FAIL antes
    }
  }
}
