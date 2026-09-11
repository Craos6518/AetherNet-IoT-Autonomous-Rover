/*
 * =============================================================================
 * AetherNet - TEST LINK ESP32 -> UNO (solo nRF24L01) | RF-3.1 / RF-2.1
 * Propósito: Probar enlace Rover (UNO) + ESP32 SOLO por RF, sin WiFi/MQTT/UART
 * Autor: basado en gateway-esp32.ino:90 + rover-uno.ino:105 (mismo pack/checksum)
 * =============================================================================
 * CABLEADO ESP32 (VSPI):
 *   nRF VCC -> 3V3 + C1 10uF (o 22uF) en PARALELO pegado al nRF (<=5mm, franja - a GND)
 *   GND  -> GND
 *   CE   -> GPIO5
 *   CSN  -> GPIO15 (FIX colisión SCK=18, ver gateway-esp32.ino:61)
 *   SCK  -> GPIO18
 *   MOSI -> GPIO23
 *   MISO -> GPIO19
 *   IRQ  -> NC
 *   VCC 3.3V ONLY - 5V quema el chip
 *
 * CONFIG RF (debe coincidir EXACTO con UNO):
 *   Canal 76 (2476 MHz), 2MBPS, PA_HIGH, Retries 5*250us*15, Addr ROVER/GATEW
 *   Struct RoverCommand #pragma pack(1) + checksum suma
 *
 * USO:
 *   1) Flashear UNO RX primero, abrir monitor 115200
 *   2) Flashear este ESP32 TX, abrir monitor 115200
 *   3) Ver TX: "ACK OK" cada 1s y RX: "RX OK #n" en UNO
 * =============================================================================
 */
#include <SPI.h>
#include <RF24.h>

#define NRF_CE_PIN 5
#define NRF_CSN_PIN 15

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

uint32_t counter=0;
unsigned long lastTx=0;
unsigned long lastChipCheck=0;

void setup() {
  Serial.begin(115200);
  while(!Serial) delay(10);
  delay(1000);
  Serial.println("\n=== LINK TEST ESP32 TX -> UNO RX (solo nRF24) ===");
  Serial.printf("Pines: CE=%d CSN=%d SCK=18 MOSI=23 MISO=19  Ch76 2MBPS PA_HIGH\n", NRF_CE_PIN, NRF_CSN_PIN);

  if(!radio.begin()) {
    Serial.println("ERROR: nRF no detectado! Revisa:");
    Serial.println(" 1) 3.3V + C1 10uF paralelo pegado al nRF");
    Serial.println(" 2) CSN=15 (no 18) | SCK18 MOSI23 MISO19 CE5 continuidad");
    Serial.println(" 3) Mide 3.3V en VCC-GND del nRF (no 5V)");
    while(1) { Serial.println("[HALT] revisa cableado"); delay(2000); }
  }
  radio.setPALevel(RF24_PA_HIGH);
  radio.setDataRate(RF24_2MBPS);
  radio.setChannel(76);
  radio.setRetries(5,15);
  radio.setPayloadSize(sizeof(RoverCommand));
  radio.openWritingPipe(roverAddress);
  radio.openReadingPipe(1, gatewayAddress);
  radio.stopListening(); // TX mode
  Serial.println("--- radio.printDetails() ---");
  radio.printDetails();
  Serial.println("--- TX cada 1000ms: left=120 right=120 mode=1 ---");
}

void loop() {
  // health cada 2s
  if(millis()-lastChipCheck > 2000) {
    lastChipCheck=millis();
    bool ok=radio.isChipConnected();
    Serial.printf("[%lu] isChipConnected=%d %s\n", millis(), ok, ok?"OK":"FAIL (revisa SPI/3.3V)");
  }
  if(millis()-lastTx > 1000) {
    lastTx=millis();
    counter++;
    RoverCommand cmd;
    cmd.left_pwm = 120;
    cmd.right_pwm = 120;
    cmd.mode = 1;
    cmd.checksum = calcChecksum(cmd);
    // necesita estar en TX mode
    radio.stopListening();
    bool ok = radio.write(&cmd, sizeof(cmd));
    Serial.printf("[%lu] TX #%lu L=%d R=%d mode=%d -> %s\n",
      millis(), (unsigned long)counter, cmd.left_pwm, cmd.right_pwm, cmd.mode,
      ok ? "ACK OK (UNO recibio)" : "FAIL/no ACK (UNO apagado/fuera/canal distinto)");
    // si UNO tiene ackPayload, aqui llegaria, pero en test simple no hace falta
    if(radio.isAckPayloadAvailable()) {
      char ack[32]={0};
      radio.read(&ack, sizeof(ack));
      Serial.print("  ACK payload: "); Serial.println(ack);
    }
  }
}
