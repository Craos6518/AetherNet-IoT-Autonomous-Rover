# Diario de Campo — Automatización LowCode

> **Notebook:** `docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` · **Flujo:** `automation/flows/intrusion_alert.json` (referencia, NO deploy — deuda 2026-09-09) · **Telegram directo:** `backend/app/routers/events.py` → `api.telegram.org/bot.../sendMessage`

## Capturas — checklist

- [ ] **BotFather** — `capturas/botfather.png` — screenshot creación bot vía BotFather (`/newbot`)
- [ ] **Telegram alerta real** — `capturas/telegram-alert.png` — mensaje intrusión `aethernet/seguridad/intrusion` en chat
- [ ] **Node-RED flujo (deuda)** — `capturas/node-red-flow.png` — screenshot `automation/flows/intrusion_alert.json` importado (si se retoma Sprint 4)
- [ ] **LED RGB intrusión** — `capturas/led-rojo-intrusion.jpg` — foto LED 44/45/46 en rojo 3s (ver `firmware/mega-access/src/laser.cpp`)

Si falta:

```markdown
![CAPTURA PENDIENTE](capturas/telegram-alert.png) — tomar captura chat Telegram intrusión real
```

## Estado

- LOW-02 Node-RED **DEUDA TÉCNICA 2026-09-09** — no se deploya, solo flujo JSON referencia.
- HU-02 ahora solo LED RGB + Telegram HTTP directo (sin broker Node-RED).
