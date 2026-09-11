/*
 * =============================================================================
 * AetherNet - TEST LINK UNO RX <- ESP32 TX (solo nRF24L01) | RF-3.1
 * Companion de test-link-esp32-rover.ino
 * =============================================================================
 * CABLEADO UNO (SPI hardware):
 *   nRF VCC -> 3.3V + C2 10uF paralelo pegado al nRF (<=5mm)
 *   GND  -> GND
 *   CE   -> D4
 *   CSN  -> D10 (SS)
 *   SCK  -> D13
 *   MOSI -> D11
 *   MISO -> D12
 *   IRQ  -> NC
 *   VCC 3.3V ONLY - UNO 3.3V es debil (50mA), si cae <3.0V usa 3V3 del ESP32/AMS1117
 *
 * CONFIG RF: idem ESP32 - Ch76 2MBPS PA_HIGH Retries 5,15 Addr ROVER/GATEW
 * Debe coincidir bit a bit o checksum falla / no hay ACK
 * =============================================================================
 */
#include <SPI.h>
#include <RF24.h>

#define NRF_CE_PIN 4
#define NRF_CSN_PIN 10

RF24 radio(NRF_CE_PIN, NRF_CSN_PIN);
const byte roverAddress[6] = "ROVER";
const byte gatewayAddress[6] = "GATEW";

#pragma pack(push, 1)
struct RoverCommand {
    int16_t left_pwm;
    int16_t right_pwm;
    uint8_t mode;
    uint16_t checksum;
};
#pragma pack(pop)

uint16_t calcChecksum(const RoverCommand &c) {
    uint16_t s=0;
    const uint8_t *b=(const uint8_t*)&c;
    for(size_t i=0;i<sizeof(c)-2;i++) s+=b[i];
    return s;
}
bool verifyChecksum(const RoverCommand &c) {
    return calcChecksum(c)==c.checksum;
}

unsigned long lastChipCheck=0;
uint32_t rxCount=0;

void setup() {
  Serial.begin(115200);
  while(!Serial) delay(10);
  delay(1000);
  Serial.println("\n=== LINK TEST UNO RX <- ESP32 TX (solo nRF24) ===");
  Serial.println("Pines: CE=4 CSN=10 SCK=13 MOSI=11 MISO=12  Ch76 2MBPS PA_HIGH");
  if(!radio.begin()) {
    Serial.println("ERROR: nRF no detectado! Revisa:");
    Serial.println(" 1) 3.3V + C2 10uF paralelo pegado al nRF (<=5mm)");
    Serial.println(" 2) CE4 CSN10 SCK13 MOSI11 MISO12 continuidad (tester)");
    Serial.println(" 3) Mide 3.3V en VCC-GND (UNO 3.3V debil, si <3.0 usa AMS1117)");
    while(1){ Serial.println("[HALT] revisa cableado"); delay(2000); }
  }
  radio.setPALevel(RF24_PA_HIGH);
  radio.setDataRate(RF24_2MBPS);
  radio.setChannel(76);
  radio.setRetries(5,15);
  radio.setPayloadSize(sizeof(RoverCommand));
  radio.openWritingPipe(gatewayAddress);
  radio.openReadingPipe(1, roverAddress);
  radio.startListening();
  Serial.println("--- radio.printDetails() ---");
  radio.printDetails();
  Serial.println("--- Escuchando... espera TX del ESP32 cada 1s ---");
}

void loop() {
  if(millis()-lastChipCheck > 2000) {
    lastChipCheck=millis();
    bool ok=radio.isChipConnected();
    Serial.print("["); Serial.print(millis()); Serial.print("] isChipConnected=");
    Serial.print(ok?"1 OK":"0 FAIL");
    if(!ok) Serial.print(" -> revisa 3.3V/C/pines");
    Serial.print(" | RX total="); Serial.println(rxCount);
  }
  if(radio.available()) {
    RoverCommand cmd;
    radio.read(&cmd, sizeof(cmd));
    if(verifyChecksum(cmd)) {
      rxCount++;
      Serial.print(">>> RX OK #"); Serial.print(rxCount);
      Serial.print(" L="); Serial.print(cmd.left_pwm);
      Serial.print(" R="); Serial.print(cmd.right_pwm);
      Serial.print(" mode="); Serial.print(cmd.mode);
      Serial.print(" chk OK  << LINK OK >> t="); Serial.println(millis());
    } else {
      Serial.println("RX checksum FAIL (corrupcion RF o pack mismatch)");
    }
  }
}
