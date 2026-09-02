/*
 * AetherNet - Test nRF24L01 UNO (validación aislada DEVOPS-05)
 * Solo SPI/RF — sin L298N, sin HC-SR04, sin TCRT, sin EMA.
 * Para aislar "ERROR: nRF24L01 not detected!" rover-uno.ino:135
 * Cableado docs/fritzing/plano-sprint1-nrf24-reapertura.md:38
 * CE→D4, CSN→D10, SCK→13, MOSI→11, MISO→12, VCC→3.3V + C2 10µF en paralelo ≤5mm a GND
 */
#include <SPI.h>
#include <RF24.h>

#define NRF_CE_PIN 4
#define NRF_CSN_PIN 10

RF24 radio(NRF_CE_PIN, NRF_CSN_PIN);

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(10);
  delay(1000);
  Serial.println("\n=== AetherNet nRF24L01 Validation UNO ===");
  Serial.println("Pines: CE=4 CSN=10 SCK=13 MOSI=11 MISO=12");
  if (!radio.begin()) {
    Serial.println("ERROR: nRF24L01 not detected! Revisa:");
    Serial.println("  1) VCC 3.3V + C2 10µF en PARALELO (no serie) pegado al nRF");
    Serial.println("  2) CE4, CSN10, SCK13, MOSI11, MISO12 continuidad");
    Serial.println("  3) 3.3V en VCC-GND del nRF (UNO 3.3V débil, si cae <3.0 usa ESP32 3V3)");
  } else {
    Serial.println("OK: nRF24L01 initialized");
    radio.setPALevel(RF24_PA_HIGH);
    radio.setDataRate(RF24_2MBPS);
    radio.setChannel(76);
    radio.openReadingPipe(1, (byte*)"ROVER");
    radio.openWritingPipe((byte*)"GATEW");
    radio.startListening();
    Serial.println("--- radio.printDetails() ---");
    radio.printDetails();
    Serial.println("--- Listo para RX ---");
  }
}

void loop() {
  static unsigned long last = 0;
  if (millis() - last > 2000) {
    last = millis();
    bool ok = radio.isChipConnected();
    Serial.print("["); Serial.print(millis()); Serial.print("] isChipConnected=");
    Serial.print(ok); Serial.println(ok ? " -> OK" : " -> FAIL");
    if (ok) {
      if (radio.available()) {
        char buf[32] = {0};
        radio.read(&buf, sizeof(buf));
        Serial.print("  RF RX raw: "); Serial.println(buf);
      } else {
        Serial.println("  Sin RF (normal si ESP32 no TX)");
      }
    }
  }
}
