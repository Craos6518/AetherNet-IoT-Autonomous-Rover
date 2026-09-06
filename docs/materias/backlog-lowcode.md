# Roadmap + Backlog — Automatizaciones y LowCode (transversal, sin asignatura)

**Nota:** el backlog original (`docs/backlog.md` Área 3) define esta área operativa, pero no tiene PDF académico propio. Se documenta aquí para que "todos los temas necesarios para completar el proyecto" queden cubiertos — HU-02 y RF-4.x dependen íntegramente de este área.

**Estado al 2026-09-01 — CANCELADO Tuya (ADR-001):** bombillo/`tuya-local` cancelado por políticas API propietaria (RNF-3.1) + `local_key` (R-01). Flujo vigente: `mqtt-intrusion → function-parse → telegram-alert/http-telegram` + LED RGB local. Pendiente: despliegue Node-RED, bot Telegram, pruebas E2E.

---

## Roadmap de temas necesarios

| Tema | Profundidad | Para qué | Específico |
|---|---|---|---|
| Node-RED: instalación, flujos, deploy desde JSON | Operativo | Todos los LOW | El flujo ya existe como JSON: saber importarlo, conectarlo al broker y depurar con nodos debug |
| Nodos MQTT en Node-RED: QoS, reconexión, topics wildcard | Operativo | LOW-02 | Suscripción a `aethernet/seguridad/intrusion` con credenciales usuario `nodered` (ACL del DEVOPS-02) |
| Telegram Bot API vía BotFather: token, chat_id, sendMessage | Operativo | LOW-03/RF-4.1 | Token en variable de entorno de Node-RED, JAMÁS en el JSON versionado; mensaje con prioridad alta según HU-02 |
| ~~tuya-local~~ — **CANCELADO 2026-09-01** (R-01, RF-4.2 cancelado) | — | — | Eliminado; intrusión solo vía Telegram + LED RGB |
| Function nodes JavaScript: parseo de payloads, debounce, rate-limit | Operativo | LOW-05 | El nodo function-parse-intrusion ya existe: revisar que filtre eventos duplicados (láser puede disparar múltiples veces) |
| Manejo de caída del broker (reconnect node / status handling) | Básico | LOW-07 | Comportamiento esperado: cola local breve o pérdida aceptable documentada |

## Backlog

### LOW-01 — ~~Validar compatibilidad tuya-local~~ — **CANCELADO 2026-09-01** (R-01)
| W · Cancelado · RF-4.2 fuera de alcance |
**Decisión ADR-001:** descartado por políticas de integración (API) propietaria (viola RNF-3.1, requiere Tuya Cloud) + `local_key` inaccesible. Ver `docs/adr/adr-001-cancelacion-tuya.md`.

### LOW-02 — Flujo Node-RED suscrito a eventos ✅ ESQUELETO
**Falta exactamente:** importar JSON al runtime Node-RED (contenedor recomendado en la misma LAN), configurar credenciales broker, probar recepción real publicando intrusión simulada con mosquitto_pub.

**Criterios:**
- [ ] Nodo debug muestra el evento parseado correctamente

### LOW-03 — Bot de Telegram
| M · Sprint 4 · Depende de LOW-02 · Origen RF-4.1/HU-02 |
**Qué hacer exactamente:** crear bot vía BotFather; obtener token + chat_id del equipo; configurar nodo http-telegram existente con token por variable de entorno (`process.env.TELEGRAM_TOKEN`); probar mensaje simple; luego formato final del mensaje de alerta (hora, sensor, acción sugerida).
**Criterios:**
- [ ] Alerta llega al chat <2 s tras publicar intrusión
- [ ] Token NO aparece en ningún archivo versionado (auditar con grep)

### LOW-04 — ~~Integración tuya-local~~ — **CANCELADO 2026-09-01** (R-01)
| W · Cancelado · RF-4.2 fuera de alcance |
**Decisión:** sin objeto tras cancelación del bombillo. Ver `automation/flows/intrusion_alert.json` simplificado (solo Telegram).

### LOW-05 — Cadena completa de intrusión (sin bombillo)
| M · Sprint 4 · Depende de LOW-03, MEGA-láser operativo · Origen HU-02 |
**Qué hacer exactamente:** E2E documentada: interrupción láser KY-008 → MEGA (LED RGB rojo) → UART→gateway→MQTT → Node-RED dispara Telegram → evidencia en video/capturas.
**Criterios:**
- [ ] Latencia total intrusión→Telegram medida y registrada (<3 s objetivo interno)
- [ ] Falsos positivos = 0 en 10 disparos de prueba (KPI PRD)

### LOW-06 — Dashboard Node-RED (opcional) | C · Sprint 4 — solo si sobra tiempo: dashboard con estado de sensores reutilizando los mismos topics.
### LOW-07 — Reconexión Node-RED↔broker | C · Sprint 4 — probar apagando Mosquitto 30 s: flujo se recupera sin intervención; documentar comportamiento observado.
