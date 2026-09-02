/*
 * Banco B — Test EMA aislado solo UNO (sin ESP32, sin nRF24L01, sin L298N)
 * Filtro: stats/ema_filter.py:15 -> rover.ino:66  (α=0.2 HU-03 / RNF-2.1)
 * Wiring: HC-SR04 VCC→5V GND→GND TRIG→D2 ECHO→D3  (rover.ino:42)
 * Salida: Serial 115200  "raw,ema"  para Serial Plotter (Tools → Serial Plotter)
 * Uso: mueve una carpeta/cartón 10→100 cm frente al HC-SR04, ve raw (ruidosa) vs ema (suave).
 */
#include <Arduino.h>
#include <NewPing.h>

#define ULTRASONIC_TRIG 2
#define ULTRASONIC_ECHO 3
#define MAX_DISTANCE_CM 200
#define EMA_ALPHA 0.2f          // HU-03 / RNF-2.1  — mismo que stats/ema_filter.py:17

NewPing sonar(ULTRASONIC_TRIG, ULTRASONIC_ECHO, MAX_DISTANCE_CM);

float ema = 0;
bool inited = false;
unsigned long lastPrint = 0;

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(10);
  Serial.println("raw,ema"); // header para Plotter (CSV)
  Serial.println("=== Test EMA UNO — solo HC-SR04 α=0.2 ===");
  Serial.println("Mueve objeto 10-100 cm frente al sensor. raw=ruidoso, ema=filtrado.");
}

void loop() {
  // NewPing ping_cm() ya filtra timeout; 0 = sin eco
  unsigned int raw = sonar.ping_cm();
  // Mantener último valor si no hay eco (evita caídas a 0)
  static unsigned int lastRaw = 50;
  if (raw == 0 || raw > MAX_DISTANCE_CM) raw = lastRaw;
  else lastRaw = raw;

  if (!inited) {
    ema = raw;
    inited = true;
  } else {
    ema = EMA_ALPHA * raw + (1.0f - EMA_ALPHA) * ema;
  }

  // 20 Hz (50 ms) — suficiente para Plotter, evita saturar Serial
  if (millis() - lastPrint >= 50) {
    lastPrint = millis();
    Serial.print(raw);
    Serial.print(",");
    Serial.println(ema, 1); // 1 decimal
  }

  delay(10);
}
