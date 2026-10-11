#pragma once
/*
 * uart_protocol.h — UART hacia Gateway ESP32
 * TX: ACCESS: / STATUS:    RX: CMD:*
 * Throttled STATUS cada STATUS_INTERVAL_MS (5s).
 */

#include <Arduino.h>

void uartInit();
void handleGatewayUart();
void processGatewayCommand(const String& cmd);
void sendAccessEvent(const String& jsonPayload);
void sendStatusToGateway();
void sendPeriodicStatus();

// helper AVR
int freeMemory();
