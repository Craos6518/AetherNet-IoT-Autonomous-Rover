/*
 * AetherNet - Test nRF24L01 ESP32 (validación aislada DEVOPS-05)
 * Solo SPI/RF — sin WiFi, sin MQTT, sin UART a MEGA.
 * Para aislar "WARN: nRF24L01 not detected!" visto en gateway-esp32.ino:124
 * 
 * Cableado esperado docs/fritzing/plano-sprint1-nrf24-reapertura.md:30
 * CE → GPIO5, CSN → GPIO15 (corrige 18 colisión SCK), SCK→18, MOSI→23, MISO→19
 * VCC→3V3 + C1 10µF ideal (o 22µF válido) ≤5mm a GND, GND→GND, IRQ NC
 * 3.3V estable — si UNO 3.3V cae, usar AMS1117 externo.
 */

#include <SPI.h>
#include <RF24.h>

#define NRF_CE_PIN 5
#define NRF_CSN_PIN 15

RF24 radio(NRF_CE_PIN, NRF_CSN_PIN);

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(10);
  delay(1000);
  Serial.println("\n=== AetherNet nRF24L01 Validation ESP32 ===");
  Serial.printf("Pines: CE=%d CSN=%d SCK=18 MOSI=23 MISO=19\n", NRF_CE_PIN, NRF_CSN_PIN);
  Serial.println("Iniciando SPI + radio.begin()...");

  if (!radio.begin()) {
    Serial.println("ERROR: nRF24L01 not detected! Revisa:");
    Serial.println("  1) VCC 3.3V + C1 10µF (franja - a GND) pegado al nRF");
    Serial.println("  2) CSN movido de IO18 -> IO15 (amarillo)");
    Serial.println("  3) SCK 18, MOSI 23, MISO 19, CE 5, GND");
    Serial.println("  4) 3.3V con multímetro en VCC-GND del nRF (no 5V)");
    // No bloquear con while(1) — deja loop mostrar reintento cada 2s
  } else {
    Serial.println("OK: nRF24L01 initialized");
    radio.setPALevel(RF24_PA_HIGH);
    radio.setDataRate(RF24_2MBPS);
    radio.setChannel(76);
    radio.setRetries(5, 15);
    // Direcciones de prueba
    const byte addr[6] = "TEST1";
    radio.openWritingPipe(addr);
    radio.openReadingPipe(1, addr);
    radio.stopListening(); // modo TX para printDetails

    Serial.println("--- radio.printDetails() ---");
    radio.printDetails();
    Serial.println("--- Config OK ---");
    Serial.println("Si ves printDetails con registros != 0x00/0xFF, SPI OK.");
    Serial.println("Si ves 0x00 o 0xFF en todo, SPI no comunica (MISO/MOSI/SCK/CSN).");
  }
}

void loop() {
  static unsigned long last = 0;
  if (millis() - last > 2000) {
    last = millis();
    // Re-test begin sin reiniciar placa
    bool ok = radio.isChipConnected();
    Serial.printf("[%lu ms] isChipConnected=%d ", millis(), ok);
    if (!ok) {
      Serial.println("-> FAIL (revisa cableado/alimentación)");
    } else {
      Serial.println("-> OK");
      // Test TX dummy (sin ACK esperado, solo verifica que write no bloquea)
      const char payload[] = "hello";
      bool sent = radio.write(&payload, sizeof(payload));
      Serial.printf("  write dummy: %s (sin receptor, false es normal)\n", sent ? "OK" : "FAIL/no ACK");
    }
  }
}
