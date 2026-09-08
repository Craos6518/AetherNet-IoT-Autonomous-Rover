#pragma once
/*
 * =============================================================================
 * laser.h — Barrera Láser KY-008 No Bloqueante | RF-2.3 / HU-02 | 6º Sem UTP
 * Hardware: KY-008 TX pin 8 (emisor) + RX pin 7 (receptor fotoresistencia)
 * BDD HU-02: Dado armado, Cuando haz se interrumpe → Entonces SECURITY alta → Telegram
 * =============================================================================
 * DISEÑO NO BLOQUEANTE:
 *   handleLaser() se llama cada loop() y solo muestrea cada LASER_CHECK_INTERVAL_MS (50ms).
 *   Detecta flanco HIGH→LOW (haz intacto → interrumpido) con cooldown 3s para no spamear.
 *   Nunca usa delay() — intrusión dispara LED rojo 3s vía setLedMode(RED_INTRUSION) y
 *   envía SECURITY:JSON por UART sin bloquear keypad/door.
 */

#include <Arduino.h>

void laserInit();                 // Configura pines 8/7 y enciende láser
void handleLaser();               // Poll no bloqueante — llamar cada loop()
void setLaserArmed(bool armed);   // Arma/desarma barrera (CMD:LASER desde Gateway)
bool isLaserArmed();              // Consulta estado armado
bool isLaserBeamIntact();         // HIGH = haz intacto, LOW = interrumpido
void triggerIntrusionAlert();     // Dispara evento SECURITY + LED rojo 3s (no bloqueante)
