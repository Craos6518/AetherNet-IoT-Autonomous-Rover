/*
 * =============================================================================
 * Test Laser KY-008 + LDR discreta + 10k en UNO | RF-2.3 HU-02 banco aislado
 * UsaUNO de pruebas antes de cablear MEGA (misma lógica laser.cpp sin MEGA pins 44-46)
 * Wiring UNO (5V logic, mismo divisor que MEGA):
 *  KY-008 VCC->5V GND->GND S->8 (OUTPUT HIGH=ON)
 *  LDR pata1->5V pata2->7 + 10k->GND (nodo ->7 INPUT sin pullup) + tubo negro anti luz
 *  LED externo: R->9 G->10 B->11 cada uno 220Ω a GND (UNO PWM), o LED RGB cátodo común
 *  Si tu LED es ánodo común: invierte logic en setLedColor (255-valor) como led.cpp:30
 * Baud: Serial 115200 monitor. Misma lógica que mega λaser.cpp:50ms poll + 3s cooldown
 * Objetivo: validar divisor 10k + alineación haz antes de migrar a MEGA pins 44-46/16-17
 * =============================================================================
 */
#include <Arduino.h>

#define LASER_TX_PIN 8
#define LASER_RX_PIN 7
#define LED_R_PIN 9   // PWM UNO (MEGA usa 44) — 220Ω en serie
#define LED_G_PIN 10  // PWM
#define LED_B_PIN 11  // PWM
// Para LED ánodo común pon true y setLedColor invierte 255-valor
#define LED_COMMON_ANODE true    // ánodo común 5V → invierte 255-valor como MEGA 44-46 led.cpp:30

#define CHECK_MS 50
#define COOLDOWN_MS 3000
#define LED_INTRUSION_MS 3000
#define LED_FAIL_MS 1000

static bool armed = true;
static bool lastBeam = true;
static unsigned long lastCheck = 0;
static unsigned long lastIntrusion = 0;
static unsigned long ledUntil = 0;
static bool ledOn = false;

void setLedColor(uint8_t r, uint8_t g, uint8_t b) {
#ifdef LED_COMMON_ANODE
  if (LED_COMMON_ANODE) { analogWrite(LED_R_PIN, 255 - r); analogWrite(LED_G_PIN, 255 - g); analogWrite(LED_B_PIN, 255 - b); return; }
#endif
  analogWrite(LED_R_PIN, r); analogWrite(LED_G_PIN, g); analogWrite(LED_B_PIN, b);
}
void ledRedIntrusion() { setLedColor(255,0,0); ledUntil = millis() + LED_INTRUSION_MS; ledOn = true; }
void ledOff() { setLedColor(0,0,0); ledOn = false; }

void setup() {
  Serial.begin(115200);
  unsigned long t0 = millis(); while(!Serial && millis()-t0<1500) delay(10);
  pinMode(LASER_TX_PIN, OUTPUT);
  pinMode(LASER_RX_PIN, INPUT); // IMPORTANTE sin PULLUP: divisor externo 5V->LDR->7+10k->GND
  pinMode(LED_R_PIN, OUTPUT); pinMode(LED_G_PIN, OUTPUT); pinMode(LED_B_PIN, OUTPUT);
  ledOff();
  digitalWrite(LASER_TX_PIN, HIGH);
  lastBeam = digitalRead(LASER_RX_PIN);
  Serial.println(F("\n=== Test Laser KY-008 + LDR discreta UNO ==="));
  Serial.println(F("Wiring: KY-008 S->8 VCC->5V GND->GND | LDR 5V->●->7 +10k->GND (INPUT) | LED R9 G10 B11 220Ω"));
  Serial.println(F("Comandos: 'a' arma, 'd' desarma, 's' status, 't' simula trigger"));
  Serial.print(F("Estado inicial beam=")); Serial.println(lastBeam?"HIGH(intacto)":"LOW(corte)");
  Serial.println(F("Corta haz con mano → debe salir INTRUSION + LED rojo 3s (verifica 5V->LDR ~3.3V / corte ~0.1V con multímetro)"));
}

void trigger() {
  Serial.println(F("!!! INTRUSION DETECTED — LASER BREAK !!!"));
  ledRedIntrusion();
  lastIntrusion = millis();
  // Simula SECURITY que MEGA mandaría por UART -> aquí solo log
  Serial.println(F("[SIM SECURITY:{\"event_type\":\"intrusion\",\"sensor\":\"laser-01\"}]"));
  Serial.println(F("Si esto fuera MEGA, aquí haría sendSecurityEvent() -> ESP32 -> MQTT aethernet/seguridad/intrusion"));
}

void loop() {
  // Serial commands
  if (Serial.available()) {
    char c = Serial.read();
    if (c=='a' || c=='A') { armed=true; digitalWrite(LASER_TX_PIN,HIGH); lastBeam=digitalRead(LASER_RX_PIN); Serial.println(F("ARMED TX HIGH")); }
    else if (c=='d' || c=='D') { armed=false; digitalWrite(LASER_TX_PIN,LOW); Serial.println(F("DISARMED TX LOW")); }
    else if (c=='s' || c=='S') { Serial.print(F("armed="));Serial.print(armed);Serial.print(F(" beam="));Serial.print(digitalRead(LASER_RX_PIN)?"HIGH":"LOW");Serial.print(F(" lastBeam="));Serial.println(lastBeam?"HIGH":"LOW"); }
    else if (c=='t' || c=='T') trigger();
  }
  if (armed && millis()-lastCheck >= CHECK_MS) {
    lastCheck = millis();
    bool cur = digitalRead(LASER_RX_PIN);
    if (lastBeam==HIGH && cur==LOW) {
      if (millis()-lastIntrusion >= COOLDOWN_MS || lastIntrusion==0) trigger();
    }
    lastBeam = cur;
  }
  if (ledOn && millis() >= ledUntil) ledOff();
}
