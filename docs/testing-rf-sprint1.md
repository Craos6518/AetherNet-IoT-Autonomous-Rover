# Prueba SPI / RF nRF24L01 — Sprint 1 DEVOPS-05 (RF-3.1, RF-3.3)

Objetivo: validar enlace 2.4 GHz Gateway ESP32 ↔ Rover UNO antes de Sprint 3 (joystick).
Origen: `docs/sprints.md:13` — "Pruebas de comunicación SPI (nRF24L01) entre ESP32 y Arduino UNO".

## Hardware mínimo
- ESP32-WROOM con nRF24L01 (CE=5, CSN=15 corrige 18 colisión SCK) — `firmware/gateway-esp32/gateway-esp32.ino:61` + `firmware/gateway-esp32/secrets.h:7` `FELIPE.` `2516f751` → `192.168.1.14:1883`
- Arduino UNO con nRF24L01 (CE=4, CSN=10) — `firmware/rover-uno/rover-uno.ino:53`
- Fuente 3.3V estable para nRF24 (no 5V), condensador 10µF ideal (o 22µF válido) en paralelo VCC/GND ≤5mm `docs/fritzing/plano-sprint1-nrf24-reapertura.md:28` — 47/100µF reservados Sprint 3.

## Procedimiento sin soldar (banco de pruebas)
1. Flashear firmwares ya compilados en CI:
   ```bash
   arduino-cli compile --fqbn esp32:esp32:esp32 ./firmware/gateway-esp32
   arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno
   ```
   En este repo ya verificado local: `mega-access` y `rover-uno` compilan (AVR 1.8.8, RF24 1.6.2, ArduinoJson 6.21.3).
2. Conectar ambos por USB y abrir monitores serie a 115200:
   ```bash
   arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=115200  # ESP32 CP2102
   arduino-cli monitor -p /dev/ttyACM0 -c baudrate=115200  # UNO R3 (no ttyUSB1)
   ```
3. Test SPI: al arrancar debe verse `nRF24L01 initialized` en ambos. Si aparece `ERROR: nRF24L01 not detected!` revisar cableado SPI y alimentación.
4. Test RF round-trip:
   - En ESP32, publicar por MQTT (o forzar vía Serial) un comando: `{"left_pwm":120,"right_pwm":120,"mode":1}`
   - Verificar en monitor UNO: `RF RX: L=120 R=120 mode=1`
   - Verificar en monitor ESP32: `RF TX: L=120 R=120 mode=1` y luego telemetría `aethernet/rover/telemetry` con `ultrasonic_cm`.
5. Test fail-safe HU-04 / RF-3.3:
   - Mantener Rover en `mode=1` con movimiento, desconectar alimentación del nRF del Gateway.
   - En <500 ms debe verse en UNO: `!!! FAIL-SAFE ACTIVATED: No RF signal !!!` y motores detenidos. Reconectar debe reanudar.

## Criterios de aceptación Sprint 1 — ✅ CERRADO 2026-09-01 (reapertura)
- [x] Ambos firmwares compilan en CI (`firmware-compile` job verde) — `gateway 1045900b 79%` `rover 6900b 21%` `arduino-cli 1.5.1`.
- [x] SPI detecta nRF24 en ambos lados (`radio.begin()` true) — `ESP32 nRF24L01 initialized` `gateway-esp32.ino:134` `ttyUSB0` + `UNO nRF24L01 initialized` `rover-uno.ino:145` `ttyACM0` — fix `CSN 18→15` + `C1/C2 10µF en paralelo ≤5mm` (antes `0.16V` en serie).
- [x] Latencia RF <10 ms (KPI `docs/prd.md:50`) — `mosquitto_pub 192.168.1.14` → `RF TX: L=120 R=120 mode=1` `gateway-esp32.ino:261` → `RF RX: L=120 R=120 mode=1` `rover-uno.ino:202` en <10ms (RTT/2 `millis()`), `isChipConnected=1` `test-nrf24` validado.
- [x] Fail-safe corta PWM si no hay paquete válido en 300-500 ms (RF-3.3) — `!!! FAIL-SAFE ACTIVATED: No RF signal !!!` `rover-uno.ino:220` `500ms` `FAILSAFE_TIMEOUT_MS` tras `mode=1` sin nuevo `RoverCommand` — verificado `ttyACM0` post `RF RX`.

> Evidencia 2026-09-01 18:21: `ESP32 WiFi connected: 192.168.1.23 FELIPE.` `secrets.h:7` + `MQTT connected` + `mosquitto 192.168.1.14:1883 Up` + `docker compose ps`.

## Notas
- Canal 76, 2 MBPS, PA_HIGH — `gateway.ino:100` y `rover.ino:138` deben coincidir.
- Si hay interferencia WiFi, cambiar `radio.setChannel()` en ambos.
- Documentar RSSI real cuando se tenga hardware; placeholder actual `-70` en `rover-uno.ino:238`.
