# Prueba SPI / RF nRF24L01 — Sprint 1 DEVOPS-05 (RF-3.1, RF-3.3)

Objetivo: validar enlace 2.4 GHz Gateway ESP32 ↔ Rover UNO antes de Sprint 3 (joystick).
Origen: `docs/sprints.md:13` — "Pruebas de comunicación SPI (nRF24L01) entre ESP32 y Arduino UNO".

## Hardware mínimo
- ESP32-WROOM con nRF24L01 (CE=5, CSN=18) — `firmware/gateway-esp32/src/gateway.ino:31`
- Arduino UNO con nRF24L01 (CE=4, CSN=10) — `firmware/rover-uno/src/rover.ino:53`
- Fuente 3.3V estable para nRF24 (no 5V), condensador 10µF en VCC/GND del módulo.

## Procedimiento sin soldar (banco de pruebas)
1. Flashear firmwares ya compilados en CI:
   ```bash
   arduino-cli compile --fqbn esp32:esp32:esp32 ./firmware/gateway-esp32
   arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno
   ```
   En este repo ya verificado local: `mega-access` y `rover-uno` compilan (AVR 1.8.8, RF24 1.6.2, ArduinoJson 6.21.3).
2. Conectar ambos por USB y abrir monitores serie a 115200:
   ```bash
   arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=115200  # ESP32
   arduino-cli monitor -p /dev/ttyUSB1 -c baudrate=115200  # UNO
   ```
3. Test SPI: al arrancar debe verse `nRF24L01 initialized` en ambos. Si aparece `ERROR: nRF24L01 not detected!` revisar cableado SPI y alimentación.
4. Test RF round-trip:
   - En ESP32, publicar por MQTT (o forzar vía Serial) un comando: `{"left_pwm":120,"right_pwm":120,"mode":1}`
   - Verificar en monitor UNO: `RF RX: L=120 R=120 mode=1`
   - Verificar en monitor ESP32: `RF TX: L=120 R=120 mode=1` y luego telemetría `aethernet/rover/telemetry` con `ultrasonic_cm`.
5. Test fail-safe HU-04 / RF-3.3:
   - Mantener Rover en `mode=1` con movimiento, desconectar alimentación del nRF del Gateway.
   - En <500 ms debe verse en UNO: `!!! FAIL-SAFE ACTIVATED: No RF signal !!!` y motores detenidos. Reconectar debe reanudar.

## Criterios de aceptación Sprint 1
- [ ] Ambos firmwares compilan en CI (`firmware-compile` job verde).
- [ ] SPI detecta nRF24 en ambos lados (`radio.begin()` true).
- [ ] Latencia RF <10 ms (KPI `docs/prd.md:50`) — medir con `millis()` entre TX y RX en logs.
- [ ] Fail-safe corta PWM si no hay paquete válido en 300-500 ms (RF-3.3).

## Notas
- Canal 76, 2 MBPS, PA_HIGH — `gateway.ino:100` y `rover.ino:138` deben coincidir.
- Si hay interferencia WiFi, cambiar `radio.setChannel()` en ambos.
- Documentar RSSI real cuando se tenga hardware; placeholder actual `-70` en `rover-uno.ino:238`.
