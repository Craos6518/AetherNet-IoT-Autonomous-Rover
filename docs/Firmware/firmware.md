# Documentación Académica — Firmware
Proyecto: AetherNet IoT & Autonomous Rover
Área: Firmware C++ (MEGA/Gateway/Rover) — `firmware/*` + `docs/Firmware/` + `.github/workflows/ci.yml`
> Sin PDF UTP propio: se mapea como electrónica + sistemas embebidos, trazable a RF-2.1/2.2/2.3/3.1/3.2, HU-01/02/03/04, RNF-1.2.

Mapa:
- C++ Arduino (setup/loop, millis no bloqueante, PWM, interrupciones) → `firmware/mega-access/src/door.cpp`, `firmware/rover-uno/rover-uno.ino:80 FAILSAFE 500ms`, `led.cpp`
- UART/RF/SPI (nRF24L01 RF24 lib, SPI CE/CSN, UART 38400) → `firmware/gateway-esp32/gateway-esp32.ino:59`, `firmware/rover-uno/rover-uno.ino:372 verifyChecksum`, `firmware/mega-access/src/uart_protocol.cpp:40`
- Sensores (HC-SR04 TRIG2 ECHO3, TCRT A0/A1/A2 threshold 500, KY-008 + LDR 7/8) → `firmware/rover-uno/rover-uno.ino:259 EMA α=0.2`, `firmware/test-rover-sensors/test-rover-sensors.ino`, `firmware/mega-access/src/laser.cpp:15`
- EMA en firmware (S_t=α·Y_t+(1-α)·S_{t-1} α=0.2) → `stats/ema_filter.py:15` ↔ `rover-uno.ino:259`
- Trazabilidad: RF-2.1 DEVOPS-05, RF-2.2 HU-01, RF-2.3 HU-02, RF-3.1/3.2 HU-03/04, RNF-1.2 (arduino-cli CI)

Brechas: fotos pendientes ver `docs/Firmware/README.md` checklist.
