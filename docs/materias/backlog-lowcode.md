# Roadmap + Backlog — Automatizaciones y LowCode (transversal, sin asignatura)

**Nota:** el backlog original (`docs/backlog.md` Área 3) define esta área operativa, pero no tiene PDF académico propio. Se documenta aquí para que "todos los temas necesarios para completar el proyecto" queden cubiertos — HU-02 y RF-4.x dependen íntegramente de este área.

**Estado al Aug 2026:** `automation/flows/intrusion_alert.json` con nodos definidos (mqtt-intrusion → function-parse → telegram-alert/http-telegram, tuya-bulb-alert → tuya-local-send) · **falta:** validación tuya-local del bombillo (riesgo #1 del proyecto), despliegue real de Node-RED, bot de Telegram creado, pruebas end-to-end.

---

## Roadmap de temas necesarios

| Tema | Profundidad | Para qué | Específico |
|---|---|---|---|
| Node-RED: instalación, flujos, deploy desde JSON | Operativo | Todos los LOW | El flujo ya existe como JSON: saber importarlo, conectarlo al broker y depurar con nodos debug |
| Nodos MQTT en Node-RED: QoS, reconexión, topics wildcard | Operativo | LOW-02 | Suscripción a `aethernet/seguridad/intrusion` con credenciales usuario `nodered` (ACL del DEVOPS-02) |
| Telegram Bot API vía BotFather: token, chat_id, sendMessage | Operativo | LOW-03/RF-4.1 | Token en variable de entorno de Node-RED, JAMÁS en el JSON versionado; mensaje con prioridad alta según HU-02 |
| **tuya-local**: obtener local_key/device_id, nodo o llamada HTTP directa al bombillo | Operativo crítico | LOW-04/RF-4.2 | Riesgo #1 del proyecto: validar compatibilidad YA (Sprint 1), aunque la integración sea Sprint 4. Herramientas FOSS permitidas: tinytuya para descubrir claves locales |
| Function nodes JavaScript: parseo de payloads, debounce, rate-limit | Operativo | LOW-05 | El nodo function-parse-intrusion ya existe: revisar que filtre eventos duplicados (láser puede disparar múltiples veces) |
| Manejo de caída del broker (reconnect node / status handling) | Básico | LOW-07 | Comportamiento esperado: cola local breve o pérdida aceptable documentada |

## Backlog

### LOW-01 — Validar compatibilidad tuya-local del bombillo ⚠️ RIESGO CRÍTICO
| M · Sprint 1 (¡no esperar al 4!) · Origen RF-4.2 |
**Qué hacer exactamente:** identificar modelo exacto del bombillo; con tinytuya: descubrir device_id + local_key en red LAN; comando de prueba encender/apagar SIN nube (`tuya-cli set --id X --dps 20 true` o equivalente). Si NO soporta control local → escalonar decisión de compra alternativa INMEDIATAMENTE.
**Criterios:**
- [ ] Evidencia (captura/log) de toggle exitoso 100% offline
- [ ] Resultado registrado en risk-register (cerrado/mitigado/cambiado)

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

### LOW-04 — Integración tuya-local en Node-RED
| M · Sprint 4 · Depende de LOW-01, LOW-02 · Origen RF-4.2 |
**Qué hacer exactamente:** conectar nodo tuya-local-send existente con las credenciales locales obtenidas; definir patrón de la alerta lumínica (parpadeo rojo ×N según HU-02): secuencia on/off rojo implementada con nodos delay/function; restaurar color/estado previo tras la alerta.
**Criterios:**
- [ ] Bombillo parpadea rojo ante evento real sin tocar Tuya Cloud (verificar firewall/bloqueo de internet del bombillo opcional pero demostrativo)
- [ ] Estado normal restaurado tras N parpadeos

### LOW-05 — Cadena completa de intrusión
| M · Sprint 4 · Depende de LOW-03, LOW-04, MEGA-láser operativo · Origen HU-02 |
**Qué hacer exactamente:** prueba end-to-end documentada: interrupción física del láser KY-008 → MEGA publica UART→gateway→MQTT → Node-RED dispara Telegram + bombillo → evidencia en video/capturas para el informe.
**Criterios:**
- [ ] Latencia total intrusión→Telegram medida y registrada (<3 s objetivo interno)
- [ ] Falsos positivos = 0 en 10 disparos de prueba (KPI PRD)

### LOW-06 — Dashboard Node-RED (opcional) | C · Sprint 4 — solo si sobra tiempo: dashboard con estado de sensores reutilizando los mismos topics.
### LOW-07 — Reconexión Node-RED↔broker | C · Sprint 4 — probar apagando Mosquitto 30 s: flujo se recupera sin intervención; documentar comportamiento observado.
