/*
 * =============================================================================
 * AetherNet - Test nRF24L01 UNO TX (2×UNO link) | 6º Semestre UTP
 * UNO-A TX -> UNO-B RX  CE=4 CSN=10 SCK=13 MOSI=11 MISO=12  3.3V + C 10µF ≤5mm
 * Canal 76 2MBPS PA_LOW (estable)  Addr UNO_A->UNO_B  Payload counter uint32 + ack
 * =============================================================================
 */
#include <SPI.h>
#include <RF24.h>

#define NRF_CE_PIN 4
#define NRF_CSN_PIN 10

RF24 radio(NRF_CE_PIN, NRF_CSN_PIN);
const byte addrTX[6] = "UNO_A";
const byte addrRX[6] = "UNO_B";

uint32_t counter = 0;
unsigned long last = 0;

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(10);
  delay(1000);
  Serial.println("\n=== AetherNet nRF24 UNO TX (A->B) ===");
  Serial.println("Pines: CE=4 CSN=10 SCK=13 MOSI=11 MISO=12  Canal 76");
  if (!radio.begin()) {
    Serial.println("ERROR: nRF not detected! Revisa 3.3V+C 10uF CE4 CSN10");
    while (1) delay(1000);
  }
  radio.setPALevel(RF24_PA_LOW);        // LOW mas estable que HIGH con UNO 3.3V debil
  radio.setDataRate(RF24_250KBPS);      // 250K mas robusto que 2MBPS para test link
  radio.setChannel(76);
  radio.setRetries(5, 15);
  radio.setPayloadSize(8);
  radio.openWritingPipe(addrRX);        // TX hacia B
  radio.openReadingPipe(1, addrTX);     // RX ack opcional
  radio.stopListening();
  radio.printDetails();
  Serial.println("--- TX listo cada 1s ---");
}

void loop() {
  if (millis() - last > 1000) {
    last = millis();
    if (!radio.isChipConnected()) {
      Serial.println("[FAIL] isChipConnected=0 -> revisa 3.3V/C");
      return;
    }
    counter++;
    uint32_t payload[2] = { counter, (uint32_t)millis() };
    bool ok = radio.write(&payload, sizeof(payload));
    Serial.print("["); Serial.print(millis()); Serial.print("] TX #");
    Serial.print(counter); Serial.print(" -> "); Serial.println(ok ? "ACK OK (RX recibio)" : "FAIL/no ACK (RX apagado o fuera)");
  }
}
