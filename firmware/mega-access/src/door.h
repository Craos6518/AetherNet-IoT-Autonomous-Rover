#pragma once
/*
 * door.h — Servo MG90S cerrojo RF-2.2 / HU-01
 * 0° bloqueada, 90° desbloqueada, auto-lock 5000ms.
 */

#include <Arduino.h>

void doorInit();
void unlockDoor();
void lockDoor();
void handleDoorAutoLock();
bool isDoorUnlocked();
