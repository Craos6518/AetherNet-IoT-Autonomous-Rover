# Documentación Académica — Hardware
Proyecto: AetherNet IoT & Autonomous Rover
Área: Hardware / Electrónica — ver `docs/hardware-inventory.md` + `docs/Firmware/` + `docs/Hardware/` + `docs/fritzing/`
> Mapeo sin PDF propio: el hardware no tiene asignatura UTP con PDF; se documenta como inventario + electrónica aplicada, trazable a RF-2.2/2.3/3.1/3.2 y HU-01/02/03/04.

## Mapa componente → función → pines → foto → fritzing

| Componente | Función y protocolo | Pines | Fritzing | Foto real |
|---|---|---|---|---|
| Gateway ESP32-WROOM-32U + nRF24L01 | Puente WiFi/MQTT↔UART↔RF | CE5 CSN15 SCK18 MOSI23 MISO19 + UART 16/17 38400 | `docs/fritzing/AetherNet-P2-RF-Link-v1-breadboard.png` | `docs/Firmware/fotos/gateway-esp32.jpg` — ![FOTO PENDIENTE] |
| MEGA 2560 + Keypad 4x4 + Servo MG90S + LED RGB | Cerrojo PIN + feedback local | Keypad ROW 22/24/26/28 COL 30/32/34/36, Servo 9, LED 44/45/46 | `docs/fritzing/AetherNet-P3-MEGA-Cerrojo-v1-breadboard.png` | `docs/Firmware/fotos/mega-panel.jpg` — ![FOTO PENDIENTE] |
| KY-008 + LDR discreta + 10kΩ | Barrera láser HU-02 | KY-008 S→8, LDR 5V→●→7 +10k→GND INPUT | `docs/fritzing/AetherNet-P5-Laser-v1-breadboard.png` | `docs/Firmware/fotos/laser-ldr.jpg` — ![FOTO PENDIENTE] |
| Rover UNO + L298N + HC-SR04 + TCRT×3 | Tracción + evasión + anti-caída | ENA5 IN1 6 IN2 7 IN3 8 IN4 9 ENB11, TRIG2 ECHO3, IR A0/A1/A2 | `docs/fritzing/AetherNet-P4-Rover-v1-breadboard.png` | `docs/Hardware/fotos/rover-uno.jpg` + `chasis-tt.jpg` — ![FOTO PENDIENTE] |
| TT 6V 1:48 ×4 (calce 2026-09-11) | Chasis compacto | L298N ENA/ENB 2 motores por canal | — | `docs/Hardware/fotos/chasis-tt.jpg` — ver `hardware-inventory.md:36` |

## Brechas / fotos pendientes (trazable)
- Todas las fotos reales están en checklist `docs/Hardware/README.md` y `docs/Firmware/README.md` — marcar [x] al tomar con 12MP luz natural + regla.
- Si falta foto: en `docs/Hardware/notebook/Diario_Hardware.ipynb` deja `![FOTO PENDIENTE](fotos/mega-panel.jpg)` con instrucción.

## Trazabilidad
- RF-2.2 HU-01 (cerrojo), RF-2.3 HU-02 (láser), RF-3.1/3.2 HU-03/04 (Rover), RF-2.1 DEVOPS-05 (RF link)
