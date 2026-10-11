# Capturas APP — AetherControl

> **Ubicación:** `docs/Programacion-Movil/capturas/` · **Tomar en:** SM-X620 1080×2400 o emulador 1080×2400 · **Notebook:** `docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` § 📸 Capturas APP

## Checklist — 5 capturas obligatorias

- [ ] **dashboard-led.png** — Dashboard LED — LedStatusCard verde/rojo 64dp + estado 5s/10s/1s — SM-X620 1080×2400
- [ ] **pin-screen.png** — PinScreen — 4×3 dots + throttle 5/60s
- [ ] **joystick-drag.png** — Joystick — Canvas 120dp drag X,Y -1..1 + deadband 60
- [ ] **mqtt-status.png** — MQTT estado — indicador ●/○/✕ + `MqttConnectionState` (`Connected`/`Connecting`/`Disconnected`/`Error`)
- [ ] **rover-card.png** — Rover Card — telemetría L/R US + rf_rssi -70

## Cómo tomar

**Android Studio:** Run app → pantalla → **Logcat → Screen Capture** (o Device Manager → Take Screenshot)

**adb:**

```bash
adb exec-out screencap -p > docs/Programacion-Movil/capturas/dashboard-led.png
adb exec-out screencap -p > docs/Programacion-Movil/capturas/pin-screen.png
adb exec-out screencap -p > docs/Programacion-Movil/capturas/joystick-drag.png
adb exec-out screencap -p > docs/Programacion-Movil/capturas/mqtt-status.png
adb exec-out screencap -p > docs/Programacion-Movil/capturas/rover-card.png
```

**Verificación:**

```bash
ls -lh docs/Programacion-Movil/capturas/*.png
```

Si falta:

```markdown
![CAPTURA PENDIENTE](dashboard-led.png) — tomar en SM-X620 1080×2400, dashboard LedStatusCard
```

## Logs útiles

- `mosquitto_sub -h 192.168.1.14 -t aethernet/# -v` — ver `aethernet/rover/command`, `aethernet/rover/telemetry`
- `adb logcat | grep AetherControl` — `MqttManager`, `JoystickViewModel` throttle 50ms
