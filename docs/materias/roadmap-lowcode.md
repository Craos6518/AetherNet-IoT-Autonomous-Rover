# Roadmap por Materia — Automatización LowCode (Telegram + Node-RED deuda)

**Alcance:** notificaciones HU-02 vía Telegram Bot API directo (RF-4.1). Node-RED deuda técnica 2026-09-09 — solo flujo JSON `automation/flows/intrusion_alert.json` como referencia. Ver `docs/materias/backlog-lowcode.md`, `notebooks/Diario_LowCode.ipynb`.

Backlog operativo detallado: [`backlog-lowcode.md`](backlog-lowcode.md) · Diario ejecutable: [`../../notebooks/Diario_LowCode.ipynb`](../../notebooks/Diario_LowCode.ipynb)

---

## Temas necesarios

| Tema | Profundidad | Para qué RF | Búsqueda Google sugerida |
|---|---|---|---|
| Telegram Bot API sendMessage | Implementar | RF-4.1 HU-02 | Telegram Bot API sendMessage BotFather tutorial |
| Telegram parse_mode Markdown | Operativo | RF-4.1 | Telegram Bot Markdown formatting sendMessage |
| Node-RED mqtt in/out (deuda) | Conceptual | RF-4.1 (deuda) | Node-RED MQTT subscribe publish tutorial Mosquitto |
| Node-RED function node JS | Conceptual | — (deuda) | Node-RED function node JavaScript tutorial |
| Tuya cancelado ADR-001 | Referencia | RF-4.2 Won't | Tuya local_key API policy alternative LED RGB |
| FastAPI POST security-events | Operativo | HU-02 | FastAPI POST security events Telegram notification |
| LED RGB rojo intrusión | Operativo | HU-02 | Arduino LED RGB anode common PWM intrusión |

---

## Estado 2026-09-09 — LOW-02 deuda

**LOW-02 Node-RED deuda técnica (2026-09-09):** flujo `automation/flows/intrusion_alert.json` queda solo como **referencia JSON exportable** (asesor: no deploy esta iteración). `mqtt in → function-parse-intrusion → telegram-alert → http-telegram → debug` no se ejecuta en runtime.

**HU-02 vigente:** solo **LED RGB rojo 3 s** (`firmware/mega-access/src/laser.cpp` + `led.h` pines 44/45/46) + **Telegram Bot API directo** (`backend/app/routers/events.py` `POST /api/security-events` RF-4.1) + **App dashboard** (`aethernet/seguridad/intrusion`). HU-02 solo LED RGB + Telegram. Ver `docs/materias/backlog-lowcode.md` LOW-02/LOW-05 y `docs/adr/adr-001-cancelacion-tuya.md` para Tuya cancelado.

**Flujo HU-02 validado sin Node-RED:** `MEGA laser KY-008 → UART SECURITY:{"event_type":"intrusion"} → Gateway ESP32 → MQTT aethernet/seguridad/intrusion → (Telegram directo + LED rojo + App)` — ver `notebooks/Diario_LowCode.ipynb` celda Estado con diagrama Mermaid.

---

## Búsquedas Google por bloque (copiar tal cual)

| Tema | Búsqueda sugerida |
|---|---|
| Telegram Bot API | Telegram Bot API sendMessage BotFather tutorial |
| Telegram Markdown | Telegram Bot Markdown formatting sendMessage |
| Node-RED MQTT deuda | Node-RED MQTT subscribe publish tutorial Mosquitto |
| Node-RED function | Node-RED function node JavaScript tutorial |
| Tuya cancelado | Tuya local_key API policy alternative LED RGB |
| FastAPI security-events | FastAPI POST security events Telegram notification |
| LED RGB intrusión | Arduino LED RGB anode common PWM intrusión |

---

## Orden crítico

1. **Telegram Bot API directo primero (RF-4.1):** `POST /api/security-events` + `sendMessage` con `parse_mode Markdown` desbloquea HU-02 sin depender de Node-RED.
2. **LED RGB rojo 3 s** (`laser.cpp`) en paralelo — feedback visual local ya validado.
3. **Node-RED solo como deuda documentada:** importar `intrusion_alert.json` y probar `mqtt in aethernet/seguridad/intrusion` queda para Sprint 4 si se retoma (ver `docs/Automatizacion-LowCode/README.md`).
