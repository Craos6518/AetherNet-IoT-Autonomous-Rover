/*
 * =============================================================================
 * test-bluetooth-uno.ino — Test de Banco para 3 Módulos HC-06 con Arduino UNO
 * Tableta de prueba: Samsung Galaxy Tab S10 (App Bluetooth Terminal o AetherControl)
 * Conexiones:
 *   - HC-06 VCC -> 5V (o 3.3V según breakout)
 *   - HC-06 GND -> GND
 *   - HC-06 TX  -> Arduino D2 (SoftwareSerial RX)
 *   - HC-06 RX  -> Arduino D3 (SoftwareSerial TX con divisor resistivo 1k/2k)
 * =============================================================================
 */

#include <SoftwareSerial.h>

const int BT_RX = 2; // Conecta a TX del HC-06
const int BT_TX = 3; // Conecta a RX del HC-06

SoftwareSerial btSerial(BT_RX, BT_TX);

void setup() {
    Serial.begin(9600);   // Monitor serial USB (PC)
    btSerial.begin(9600); // Bluetooth HC-06 (Samsung Galaxy Tab S10)

    Serial.println(F("========================================"));
    Serial.println(F("[TEST] Banco Bluetooth HC-06 con Arduino UNO"));
    Serial.println(F("[INFO] Abre tu Samsung Galaxy Tab S10, empareja"));
    Serial.println(F("[INFO] y conecta a HC-06 (PIN: 1234 o 0000)."));
    Serial.println(F("========================================"));
}

void loop() {
    // Lo que llega por Bluetooth (desde Samsung Tab S10) se reenvía al monitor serial de la PC
    if (btSerial.available()) {
        char c = btSerial.read();
        Serial.write(c);
    }

    // Lo que escribas en el monitor serial de la PC se envía por Bluetooth a la Samsung Tab S10
    if (Serial.available()) {
        char c = Serial.read();
        btSerial.write(c);
        Serial.write(c); // Eco local en PC
    }
}
