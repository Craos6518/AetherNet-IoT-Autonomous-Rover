/*
 * =============================================================================
 * AetherNet - Test nRF24L01 UNO RX (2×UNO link) | 6º Semestre UTP
 * UNO-B RX <- UNO-A TX  CE=4 CSN=10 SCK=13 MOSI=11 MISO=12  3.3V + C 10µF ≤5mm
 * Canal 76 250KBPS PA_LOW  Addr UNO_B escucha, UNO_A escribe
 * =============================================================================
 */
#include <SPI.h>
#include <RF24.h>

#define NRF_CE_PIN 4
#define NRF_CSN_PIN 10

RF24 radio(NRF_CE_PIN, NRF_CSN_PIN);
const byte addrTX[6] = "UNO_A";
const byte addrRX[6] = "UNO_B";

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(10);
  delay(1000);
  Serial.println("\n=== AetherNet nRF24 UNO RX (B<-A) ===");
  Serial.println("Pines: CE=4 CSN=10 SCK=13 MOSI=11 MISO=12  Canal 76");
  if (!radio.begin()) {
    Serial.println("ERROR: nRF not detected! Revisa 3.3V+C 10uF CE4 CSN10");
    while (1) delay(1000);
  }
  radio.setPALevel(RF24_PA_LOW);
  radio.setDataRate(RF24_250KBPS);
  radio.setChannel(76);
  radio.setRetries(5, 15);
  radio.setPayloadSize(8);
  radio.openReadingPipe(1, addrRX);     // RX en B
  radio.openWritingPipe(addrTX);
  radio.startListening();
  radio.printDetails();
  Serial.println("--- RX escuchando ---");
}

void loop() {
  static unsigned long lastCheck = 0;
  if (millis() - lastCheck > 2000) {
    lastCheck = millis();
    bool ok = radio.isChipConnected();
    Serial.print("["); Serial.print(millis()); Serial.print("] isChipConnected=");
    Serial.println(ok ? "1 OK" : "0 FAIL");
  }
  if (radio.available()) {
    uint32_t payload[2] = {0,0};
    radio.read(&payload, sizeof(payload));
    Serial.print(">>> RX OK #"); Serial.print(payload[0]);
    Serial.print(" t="); Serial.print(payload[1]); Serial.println(" ms  << LINK OK >>");
  }
}
