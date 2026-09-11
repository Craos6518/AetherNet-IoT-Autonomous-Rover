#pragma once
/*
 * =============================================================================
 * uart_protocol.h — UART Gateway ESP32 <-> MEGA | 6º Semestre UTP | HU-01
  * Autor: Andres Felipe Martinez Henao
 *        1 año C, 1 año PostgreSQL) — Protocolo texto TYPE:JSON\n 38400 bd
 * TX: ACCESS: / STATUS:  |  RX: CMD:ACCESS / CMD:STATUS (throttled 5s)
 * =============================================================================
 * QUÉ ES ESTO:
 *   Interfaz del módulo UART — como un `api.ts` en React que define
 *   `fetchAccessEvent()` y `pollStatus()`. Declara funciones que implementa
 *   uart_protocol.cpp sin exponer lastStatusMs (static privado).
 *   Si vienes de Python, es como el header de un módulo serial.
 *
 * PROTOCOLO (simple, texto, debuggeable con monitor serial):
 *   MEGA -> ESP32: "ACCESS:{json}\n" (intento PIN) y "STATUS:{json}\n" (heartbeat)
 *   ESP32 -> MEGA: "CMD:ACCESS:{json}\n" (PIN remoto App) y "CMD:STATUS\n" (poll)
 *   Es texto no binario para poder leerlo en Serial Monitor sin decodificar.
 *   Baud 38400 — ambos lados idénticos (config.h:42 y gateway-esp32.ino:65).
 * =============================================================================
 */

#include <Arduino.h> // Para String

void uartInit(); // Inicializa Serial2 38400 + resetea timer STATUS (llamar en setup)
void handleGatewayUart(); // Lee líneas CMD:* del ESP32 sin bloquear (llamar cada loop)
void processGatewayCommand(const String& cmd); // Parsea "CMD:TYPE:PARAMS" y rutea (interno, pero testeable)
void sendAccessEvent(const String& jsonPayload); // Envía "ACCESS:{json}\n" al Gateway (llamado por keypad)
void sendSecurityEvent(const String& jsonPayload); // Envía "SECURITY:{json}\n" al Gateway (laser HU-02)
void sendStatusToGateway(); // Envía "STATUS:{json}\n" con door_locked, laser_armed, beam, free_ram, uptime
void sendPeriodicStatus(); // Throttled cada 5s — evita spam UART (llamar cada loop)

// helper AVR — cuánto RAM libre queda (MEGA 8KB, hay que vigilar leaks de String)
int freeMemory();
