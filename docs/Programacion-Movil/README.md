# Diario de Campo — Programación Móvil (App AetherControl)

> **Notebook:** `docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` · **Código:** `app/src/main/java/com/aethernet/aethercontrol/` · **HU:** HU-01, RF-1.1/1.2/1.3

## Capturas APP — checklist (tomar en SM-X620 o emulador, 1080×2400)

- [ ] **Dashboard LED** — `capturas/dashboard-led.png` — LedStatusCard verde/rojo 64dp + estado 5s/10s/1s
- [ ] **PinScreen** — `capturas/pin-screen.png` — 4×3 dots + throttle 5/60s
- [ ] **Joystick** — `capturas/joystick-drag.png` — Canvas 120dp drag X,Y -1..1 + deadband 60
- [ ] **MQTT estado** — `capturas/mqtt-status.png` — indicador ●/○/✕ + `MqttConnectionState`
- [ ] **Rover Card** — `capturas/rover-card.png` — telemetría L/R US + rf_rssi -70

**Cómo tomar:** Android Studio → Run app → screenshot (Logcat → Screen Capture) o `adb exec-out screencap -p > captura.png`. Si falta:

```markdown
![CAPTURA PENDIENTE](capturas/dashboard-led.png) — tomar en SM-X620 1080×2400, dashboard LedStatusCard
```

## Logs APP relevantes

- `mosquitto_sub -h 192.168.1.14 -t aethernet/# -v` — ver `aethernet/rover/command`, `aethernet/rover/telemetry`
- `adb logcat | grep AetherControl` — `MqttManager`, `JoystickViewModel throttle 50ms`
