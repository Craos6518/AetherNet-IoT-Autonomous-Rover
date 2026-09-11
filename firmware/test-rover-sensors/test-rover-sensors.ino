/*
 * =============================================================================
 * AetherNet - TEST Rover Sensores TCRT5000 + HC-SR04 (sin RF, sin Gateway)
 * Autor: Andres Felipe Martinez Henao — 2a electronica/Arduino, 1a C
 * Objetivo: validar HW antes de implementar RF-3.2 anti-caida + evasión
 * Mapeo 2026-09-11: TCRT A0 trasero / A1 izq / A2 der (antes doc A0 izq)
 * HC-SR04: TRIG 2 ECHO 3 MAX 200cm EMA α=0.2
 * Salida Serial 115200: RAW + decisión <500 borde + EMA
 * Uso: arduino-cli monitor -p /dev/ttyACM0 -c baudrate=115200
 *      pasa cartulina blanca/negra a 5mm sobre cada TCRT y mano a 10/30cm frente a HC-SR04
 * =============================================================================
 */
#include <Arduino.h>
#include <NewPing.h>

#define IR_REAR_PIN A0   // Trasero
#define IR_LEFT_PIN A1   // Izq
#define IR_RIGHT_PIN A2  // Der
#define IR_THRESHOLD 500

#define ULTRASONIC_TRIG 2
#define ULTRASONIC_ECHO 3
#define MAX_DISTANCE_CM 200
#define EMA_ALPHA 0.2f

NewPing sonar(ULTRASONIC_TRIG, ULTRASONIC_ECHO, MAX_DISTANCE_CM);
float ema = 0; bool emaInit=false;

void setup(){
  Serial.begin(115200);
  while(!Serial) delay(10);
  pinMode(IR_REAR_PIN, INPUT);
  pinMode(IR_LEFT_PIN, INPUT);
  pinMode(IR_RIGHT_PIN, INPUT);
  Serial.println("\n=== TEST Rover Sensores TCRT5000 + HC-SR04 ===");
  Serial.println("TCRT A0 trasero A1 izq A2 der | RAW 0-1023 | <500=borde | HC-SR04 TRIG2 ECHO3 EMA α=0.2");
  Serial.println("Instrucciones: 1) TCRT: pasa cartulina blanca (RAW alto ~800) / negra-vacío (RAW bajo ~100) a 5mm");
  Serial.println("               2) HC-SR04: mano 5cm→30cm→100cm frente al sensor");
  Serial.println("Cabecera: millis, A0_rear, A1_left, A2_right, borde_R/L/T, US_raw, US_ema");
}

void loop(){
  int rRaw = analogRead(IR_REAR_PIN);
  int lRaw = analogRead(IR_LEFT_PIN);
  int riRaw = analogRead(IR_RIGHT_PIN);
  bool rCliff = rRaw < IR_THRESHOLD;
  bool lCliff = lRaw < IR_THRESHOLD;
  bool riCliff = riRaw < IR_THRESHOLD;

  unsigned int usRaw = sonar.ping_cm(); // 0 = sin eco
  if(usRaw>0 && usRaw<=MAX_DISTANCE_CM){
    if(!emaInit){ ema=usRaw; emaInit=true; }
    else ema = EMA_ALPHA*usRaw + (1-EMA_ALPHA)*ema;
  }

  Serial.print(millis()); Serial.print(" | ");
  Serial.print("A0_Rear="); Serial.print(rRaw); Serial.print(rCliff?" BORDE":" OK");
  Serial.print(" | A1_Left="); Serial.print(lRaw); Serial.print(lCliff?" BORDE":" OK");
  Serial.print(" | A2_Right="); Serial.print(riRaw); Serial.print(riCliff?" BORDE":" OK");
  Serial.print(" | US_raw="); 
  if(usRaw==0) Serial.print("--");
  else Serial.print(usRaw);
  Serial.print("cm EMA="); 
  if(!emaInit) Serial.print("--");
  else { Serial.print((int)ema); Serial.print("cm"); }
  // Alerta borge
  if(rCliff || lCliff || riCliff) Serial.print(" <<< BORDE DETECTADO");
  if(emaInit && ema < 30) Serial.print(" <<< OBSTACULO <30cm");
  Serial.println();

  delay(200); // 5Hz legible
}
