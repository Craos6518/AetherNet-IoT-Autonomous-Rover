#pragma once
/*
 * =============================================================================
 * door.h — Servo MG90S Cerrojo | 6º Semestre UTP | RF-2.2 / HU-01
  * Autor: Andres Felipe Martinez Henao
 * Hardware: Servo MG90S en pin 9 (PWM Timer1 MEGA) — 0° bloqueada, 90° desbloqueada
 * BDD: Dado bloqueada, Cuando "1234#" → Entonces 90° + LED verde 5s (auto-lock)
 * =============================================================================
 * QUÉ ES ESTO:
 *   Interfaz pública del módulo puerta. Como un .h en C es como un .ts con
 *   `export function` — declara qué pueden llamar otros módulos sin exponer
 *   variables internas (doorServo, doorUnlocked son static en door.cpp).
 *   Si vienes de React, es como el type de props de un componente Door.
 *   Si vienes de Python, es como definir las funciones públicas de una clase.
 *
 * POR QUÉ SERVO MG90S:
 *   Metal gear, torque 2.2 kg/cm a 5V — suficiente para mover cerrojo liviano.
 *   No es un servo 360°, es 0-180° — lo usamos 0° y 90° por mecánica.
 *   Librería Servo.h usa Timer1 del MEGA (16-bit, preciso). No usar con
 *   analogWrite en pines 11/12 que comparten Timer1 o habrá jitter.
 * =============================================================================
 */

#include <Arduino.h> // Para tipos básicos (aunque aquí no se usan, buena práctica)

void doorInit();           // Inicializa servo: attach pin 9 + posición 0° bloqueada (llamar en setup)
void unlockDoor();         // Desbloquea: servo 90° + LED verde + guarda millis() para auto-lock
void lockDoor();           // Bloquea: servo 0° + LED OFF (idempotente, seguro llamar varias veces)
void handleDoorAutoLock(); // Revisa cada loop() si pasaron 5000ms desde unlock → re-bloquea (no bloqueante)
bool isDoorUnlocked();     // Getter estado — lo usa led.cpp para sincronizar color verde/OFF
