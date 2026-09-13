/*
 * =============================================================================
 * AetherNet - Test nRF24L01 UNO (Validación Aislada DEVOPS-05) | 6º Semestre UTP
  * Autor: Andres Felipe Martinez Henao
 *        1 año C, 2 años Python) — Solo SPI/RF, sin L298N/HC-SR04/TCRT/EMA
 * Objetivo: Aislar "ERROR: nRF24L01 not detected!" visto en rover-uno.ino:135
 * Cableado docs/fritzing/plano-sprint1-nrf24-reapertura.md:38
 *   CE→D4, CSN→D10, SCK→13, MOSI→11, MISO→12, VCC→3.3V + C2 10µF en paralelo ≤5mm a GND
 * =============================================================================
 * POR QUÉ BANCO AISLADO:
 *   Si rover-uno.ino dice "not detected" no sabes si es SPI, alimentación o código.
 *   Este sketch prueba SOLO nRF24L01 — sin motores que metan ruido, sin HC-SR04
 *   que bloquee, sin EMA. Si aquí da OK y rover no, el problema es código rover;
 *   si aquí también falla, es hardware (cableado/alimentación). Divide y vencerás.
 *   En Python sería como test unitario que mockea todo menos el módulo RF.
 *
 * ELECTRÓNICA (2 años me enseñaron):
 *   - VCC 3.3V ONLY — 5V quema el nRF. Y el UNO 3.3V es débil (50mA) — si cae <3.0V
 *     el nRF no arranca. Solución: condensador 10µF pegado al nRF (≤5mm) + si falla
 *     usar 3.3V del ESP32 o AMS1117 externo.
 *   - C2 en PARALELO (no serie) — va entre VCC y GND del nRF, no en serie al cable.
 *   - SPI: SCK13, MOSI11, MISO12 son fijos en UNO (hardware SPI). CE/CSN son libres.
 * =============================================================================
 */
#include <SPI.h>  // SPI hardware UNO — SCK13/MOSI11/MISO12 (fijos, no elegibles)
#include <RF24.h> // Driver nRF24L01 TMRh20 (FOSS) — maneja registros SPI

#define NRF_CE_PIN 4   // CE — Chip Enable D4 (digital, no PWM)
#define NRF_CSN_PIN 10 // CSN — Chip Select SPI D10 (SS en UNO, debe ser OUTPUT aunque no lo uses como SS)

RF24 radio(NRF_CE_PIN, NRF_CSN_PIN); // Objeto nRF — encapsula SPI y registros

void setup() {
  Serial.begin(115200); // Debug 115200
  while (!Serial) delay(10); // Espera monitor
  delay(1000); // 1s para que estabilice alimentación — el nRF tarda en boot (como esperar a que DB conecte)
  Serial.println("\n=== AetherNet nRF24L01 Validation UNO ===");
  Serial.println("Pines: CE=4 CSN=10 SCK=13 MOSI=11 MISO=12");
  // Log de pines — si alguien cableó mal, aquí lo ve y corrige sin leer código

  if (!radio.begin()) {
    // begin() hace SPI test: escribe registro y lee de vuelta. Si no coincide, falla.
    Serial.println("ERROR: nRF24L01 not detected! Revisa:");
    Serial.println("  1) VCC 3.3V + C2 10µF en PARALELO (no serie) pegado al nRF (≤5mm)");
    Serial.println("  2) CE4, CSN10, SCK13, MOSI11, MISO12 continuidad (tester)");
    Serial.println("  3) 3.3V en VCC-GND del nRF con multímetro (UNO 3.3V débil, si cae <3.0 usa ESP32 3V3 o AMS1117)");
    // Consejos accionables — no solo "error", sino qué medir y dónde (aprendido en lab)
  } else {
    Serial.println("OK: nRF24L01 initialized"); // ¡Éxito! SPI comunica, chip responde
    // Configuración idéntica a rover-uno.ino y gateway-esp32.ino — deben coincidir o no se hablan
    radio.setPALevel(RF24_PA_HIGH);   // Potencia máx — más alcance, más consumo
    radio.setDataRate(RF24_2MBPS);    // 2 Mbps — latencia baja, sacrifica alcance vs 250Kbps
    radio.setChannel(76);             // Canal 76 = 2476 MHz — evita WiFi 1/6/11
    radio.openReadingPipe(1, (byte*)"ROVER"); // Pipe RX 1 — escucha como Rover
    radio.openWritingPipe((byte*)"GATEW");    // Pipe TX — para responder como Gateway si hace falta
    radio.startListening();           // Modo RX — escucha (como server.listen() en Node)
    Serial.println("--- radio.printDetails() ---");
    radio.printDetails(); // Dump registros SPI — si ves 0x00 o 0xFF en todo, SPI no comunica
    // printDetails muestra CONFIG, EN_AA, SETUP_AW, RF_CH, RF_SETUP, etc. Útil para comparar con ESP32
    Serial.println("--- Listo para RX ---");
  }
}

void loop() {
  // Test periódico cada 2s — no spamea, pero verifica que sigue conectado (como health check)
  static unsigned long last = 0; // static = persiste entre loops (como let last fuera de función en JS)
  if (millis() - last > 2000) {
    last = millis();
    bool ok = radio.isChipConnected(); // Test SPI: lee registro y verifica — rápido, no bloquea
    Serial.print("["); Serial.print(millis()); Serial.print("] isChipConnected=");
    Serial.print(ok); Serial.println(ok ? " -> OK" : " -> FAIL");
    if (ok) {
      if (radio.available()) {
        // Si llegó algo por RF (ej. gateway mandó "hello"), léelo
        // En validación real, gateway-esp32 manda y aquí lo ves
        char buf[32] = {0}; // Buffer 32 bytes (máx payload nRF)
        radio.read(&buf, sizeof(buf)); // Lee bytes crudos
        Serial.print("  RF RX raw: "); Serial.println(buf); // Muestra payload (si es texto)
      } else {
        Serial.println("  Sin RF (normal si ESP32 no TX)"); // Sin RX es normal si ESP32 no transmite
      }
    }
    // Si isChipConnected FAIL, revisa cableado/alimentación — no es bug de código
  }
}
