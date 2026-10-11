# Diario de Campo — Firmware

> **Proyecto:** AetherNet IoT & Autonomous Rover — 100% FOSS (`docs/prd.md:53`)
> **Stack:** `arduino-cli 1.5.1` + `ESP32-WROOM-32U` + `MEGA 2560` + `UNO R3` + `nRF24L01` + `Mosquitto` + `FastAPI`
> **Notebook canónico:** `docs/Firmware/notebook/Firmware_Notebook.ipynb` (lee) · **Logs:** `docs/logs/firmware_sprint3/` (T1-T4)

## Checklist fotos pendientes (dejar `![FOTO PENDIENTE]` en notebook si no existe)

- [ ] **MEGA 2560 panel** — `fotos/mega-panel.jpg` — vista cenital Keypad 4x4 (30/32/34/36 + 22/24/26/28) + Servo MG90S pin 9 + LED RGB 44/45/46
- [ ] **KY-008 + LDR discreta** — `fotos/laser-ldr.jpg` — láser KY-008 S→8 + LDR 5V→●→7 + 10kΩ→GND con tubo negro anti-luz
- [ ] **Gateway ESP32** — `fotos/gateway-esp32.jpg` — ESP32-WROOM-32U + nRF24L01 CE5 CSN15 SCK18 MOSI23 MISO19 + UART 16/17 38400 bd + divisor 5V→3.3V
- [ ] **Rover UNO** — `fotos/rover-uno.jpg` — UNO + L298N ENA5 IN1-6 IN2-7 IN3-8 IN4-9 ENB11 + HC-SR04 TRIG2 ECHO3 + TCRT A0/A1/A2 + nRF CE4 CSN10
- [ ] **Chasis TT 6V** — `fotos/chasis-tt.jpg` — 4× TT 6V 1:48 calce 2026-09-11 (ver `hardware-inventory.md:36`) con regla

**Cómo tomar:** luz natural, fondo blanco, 12MP, incluir regla/escala. Si falta: en el notebook deja:

```markdown
![FOTO PENDIENTE](fotos/mega-panel.jpg) — tomar foto cenital panel MEGA con regla, 12MP
```

## Fritzing (referencia)

- `docs/fritzing/AetherNet-P3-MEGA-Cerrojo-v1-breadboard.png` + `.fzz` + `.svg`
- `docs/fritzing/AetherNet-P2-RF-Link-v1-breadboard.png`
- `docs/fritzing/AetherNet-P4-Rover-v1-breadboard.png`
- `docs/fritzing/AetherNet-P5-Laser-v1-breadboard.png`

## Logs por terminal (2026-09-11)

| Terminal | Archivo | Ver |
|---|---|---|
| T1 Docker+MQTT | `docs/logs/firmware_sprint3/live_2026-09-11/T1_mqtt_live.log` + `_docker` | `docker ps` + `mosquitto_sub aethernet/#` |
| T2 MEGA | `T2_mega_live.log` | `ACCESS:hash` + `SECURITY: intrusion` |
| T3 Gateway | `T3_gateway_live.log` + `T3_gateway_30s.log` (1.1M) | `MQTT RX→RF TX` |
| T4 Rover | `T4_rover_live.log` + `T_telemetry_invertida.log` (89 líneas) | `RF RX L/R mode` + `ir_left/ir_right` |
