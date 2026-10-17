/*
 * Calibración TCRT5000 — imprime RAW A0/A1/A2 cada 200ms.
 * Uso: pasa cartulina blanca, negra y vacío(off-track) a ~5mm por cada sensor
 * y anota los valores. No decide BORDE: solo muestra RAW.
 */
#define IR_REAR_PIN A0
#define IR_LEFT_PIN A1
#define IR_RIGHT_PIN A2

void setup() {
  Serial.begin(115200);
  pinMode(IR_REAR_PIN, INPUT);
  pinMode(IR_LEFT_PIN, INPUT);
  pinMode(IR_RIGHT_PIN, INPUT);
  Serial.println("millis,A0_rear,A1_left,A2_right,nota(blanca/negra/vacio)");
}

void loop() {
  Serial.print(millis());
  Serial.print(",");
  Serial.print(analogRead(IR_REAR_PIN));
  Serial.print(",");
  Serial.print(analogRead(IR_LEFT_PIN));
  Serial.print(",");
  Serial.println(analogRead(IR_RIGHT_PIN));
  delay(200);
}
