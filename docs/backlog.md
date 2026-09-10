# Backlog — AetherNet IoT & Autonomous Rover

Backlog operativo por área de trabajo (materia), derivado de `docs/requirements.md` y encajado en la ventana de 8 semanas / 4 sprints de `docs/sprints.md`. Prioridad en escala MoSCoW:

- **M (Must)** — sin esto el proyecto no es evaluable/funcional. No negociable.
- **S (Should)** — importante, pero el sistema sobrevive sin ello temporalmente.
- **C (Could)** — mejora deseable si sobra tiempo.
- **W (Won't, esta iteración)** — explícitamente fuera de alcance para las 8 semanas (queda registrado para no perderlo, no para hacerlo).

Cada ítem referencia su RF/HU de origen y el sprint donde debe quedar cerrado según `docs/sprints.md`. Si un ítem se atrasa de su sprint, es una señal de riesgo para todo lo que dependa de él — revisar la columna "Depende de".

---

## Área 1 — Programación Para Móviles (App AetherControl)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| MOV-01 | Setup proyecto Android (Kotlin + Compose + arquitectura MVVM base) — ✅ Done `f03190b` `feature/app-setup-mvvm` 2026-09-01 (StateFlow+Repo+Navigation, DI manual, cleartext fix, verificado `192.168.1.14:8000/health` ok en SM-X620) | M | 1 | DEVOPS-01 (para saber endpoints) | RF-1.1 |
| MOV-02 | Pantallas de control LED local solo lectura — ✅ Done `feature/app-pantallas-domotica` 2026-09-07 (RF-1.1 HU-01/HU-02: LedStatusCard 64dp + poll 5s + mapper 5s/10s/1s espejo `led.cpp:64` + `config.h:52`) — bombillo Tuya CANCELADO ADR-001 | M | 2 | MEGA firmware básico | RF-1.1 |
| MOV-03 | Cliente MQTT/WebSocket en la app, suscripción a topics telemetría — ✅ Done `feature/app-mqtt-telemetria` 2026-09-07 (RF-1.1 <50ms `prd.md:50`: `MqttManager` Paho 1.2.5 tcp://host:1883 + ws://9001 fallback, `SharedFlow` `aethernet/rover/telemetry:69` `access/event:71` `seguridad/intrusion:72` `system/status:73` gateway `acl.conf:14` aethernet/#, `ServiceLocator mqttManager` deriva broker de `getCurrentBaseUrl()`, `AetherRepository` Flows + `DashboardViewModel` colecta push y fallback poll 5s `MOV-02`, UI `MQTT ●` + `Card Rover L/R US`) — verificado `assembleDebug`/`testDebugUnitTest` + `mosquitto_sub -h 192.168.1.14 -t aethernet/#` | M | 2 | DEVOPS-02 (broker corriendo) | RF-1.1 |
| MOV-04 | Módulo de PIN/clave para envío de comandos de cerrojo desde la app — ✅ Done `feature/app-pin-cerrojo` 2026-09-07 Plan B (HU-01 RF-2.2 S): `PinViewModel` throttle 5/60s + `PinScreen` 4x3 dots 6x `MqttManager.publishAccessCommand` `aethernet/access/command:70` → Gateway `handleAccessCommand:332` → `CMD:ACCESS uart_protocol.cpp:40` → `processPinAttempt==VALID_PIN config.h:48` → `unlockDoor 90° 5s:52` + `LED verde 5s/rojo 1s:62` → `ACCESS:hash` → `forwardAccessToBackend:343` POST 201 + `aethernet/access/event:71` → app `accessEventFlow` `PinViewModel` confirm `✓/✕` + `LedStatusCard` verde, verificado `mosquitto_sub aethernet/#` `{"pin":"1234"} success:true pin_hash 7c78c98f / {"pin":"11234"} false b916ae0` + `curl /api/access-events limit 3` persistido + `assembleDebug`/`testDebugUnitTest` 35 verdes | S | 2 | MOV-03 | HU-01 |
| MOV-05 | Joystick virtual en Compose (captura de vectores X,Y) | M | 3 | — | RF-1.2 |
| MOV-06 | Envío de comandos del joystick con baja latencia (throttling/debounce) | M | 3 | MOV-05, RF-2.1 operativo | RF-1.2 |
| MOV-07 | Fallback Bluetooth SPP para nodos críticos si cae el Wi-Fi | S | 3 | Nodo Acceso Compacto (HC-06) | RF-1.3 |
| MOV-08 | Dashboard consolidado (estado LED local, telemetría, alertas) | S | 4 | MOV-02, MOV-03 | RF-1.1 |
| MOV-09 | Manejo de errores de red / reconexión automática MQTT | C | 4 | MOV-03 | — |
| MOV-10 | Pruebas unitarias (JUnit) de ViewModels críticos | S | 4 | MOV-02 a MOV-06 | RNF (calidad) |

**Riesgo del área:** MOV-06 (latencia del joystick) depende de que el enlace RF del Rover ya esté probado. MOV-02 solo incluye LED RGB local (bombillo Tuya cancelado ADR-001).

---

## Área 2 — DevOps (Infraestructura, CI/CD, Firmware base)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| DEVOPS-01 | `docker-compose.yml` con FastAPI + PostgreSQL + Mosquitto MQTT — ✅ Done Sprint 1 (`docker-compose.yml:1` FastAPI 1.0.0-sprint1 + Postgres 16 + Mosquitto 2.0, `docker compose up` OK) | M | 1 | — | RNF-1.1 |
| DEVOPS-02 | Configuración de Mosquitto (topics, ACLs mínimas) — ✅ Done Sprint 1 (`mosquitto.conf:1` 1883/9001 + `acl.conf:1` `aethernet/#` + `$SYS/#`, `docker-compose.yml:32` mount) | M | 1 | DEVOPS-01 | RF-2.1, RF-4.2 |
| DEVOPS-03 | Esquema inicial de PostgreSQL (tabla de eventos/accesos) — ✅ Done Sprint 1 (`backend/app/models.py:34` 4 tablas + `init.sql:1` uuid-ossp, `event_metadata` fix) | M | 1 | DEVOPS-01 | RNF-2.2 |
| DEVOPS-04 | Pipeline GitHub Actions con `arduino-cli` (compilación de firmware en cada push) — ✅ Done Sprint 1 (`ci.yml:90` FQBN `arduino:mega:atmega2560`/`arduino:avr:uno` + `arduino-cli 1.5.1` + `ArduinoJson 6.21.3`, 19252/6900 bytes) | M | 1 | — | RNF-1.2 |
| DEVOPS-05 | Prueba de comunicación SPI/RF entre ESP32 y Arduino UNO (nRF24L01) — ✅ Done 2026-09-01 (`docs/testing-rf-sprint1.md:32` 4/4 HW `ttyUSB0`+`ttyACM0` `RF TX/RX L=120` + `isChipConnected=1`, `rover-uno.ino:220` fail-safe 500ms) | M | 1 | — | RF-3.1 |
| DEVOPS-06 | Endpoints FastAPI mínimos (health, registro eventos) — ✅ Done `feature/backend-endpoints` 2026-09-07 (8 endpoints POST/GET 4 recursos + /health degraded, validación limit 1..200, filtros sensor_type/event_type/session_id, ruff/mypy/pytest 21 verde, docker integration verificado) | M | 2 | DEVOPS-01, DEVOPS-03 | RF-2.1 |
| DEVOPS-07 | Tests PyTest para endpoints de FastAPI — ✅ Done `feature/backend-endpoints` 2026-09-07 (`test_events.py` 14 tests + `test_health.py` 7, total 21 verdes, mock DB + integración) | S | 2 | DEVOPS-06 | RNF (DevOps) |
| FW-MEGA-CERROJO | Cerrojo MEGA teclado 4x4 + Servo MG90S + LED RGB local — ✅ Done `feature/firmware-mega-cerrojo` 2026-08-26 (`config.h:17` `ROW_PINS 30/32/34/36 COL_PINS 22/24/26/28` `SERVO_PIN 9` `LED 44/45/46` `VALID_PIN 1234` `DOOR_AUTO_LOCK_MS 5000`, `door.cpp` `keypad_control.cpp` `led.cpp` `uart_protocol.cpp` 38400 bd) | M | 2 | DEVOPS-05 | RF-2.2, HU-01 |
| FW-MEGA-LASER | Barrera láser KY-008 + LDR discreta no bloqueante — ✅ Done `feature/firmware-mega-laser-v2` 2026-09-07 `c7ce065` (`laser.h/cpp` `LASER_TX 8` `LASER_RX 7 INPUT` 10k→GND, `LASER_CHECK 50ms` `COOLDOWN 3s`, `triggerIntrusionAlert` → `SECURITY:` → `POST /api/security-events` + `aethernet/seguridad/intrusion`, `firmware/test-laser-uno` banco aislado, `hardware-inventory.md:9` `fritzing/compendio P5`) | M | 2 | FW-MEGA-CERROJO | RF-2.3, HU-02 |
| DEVOPS-08 | Documentar variables de entorno / `.env.example` (sin credenciales reales) — ✅ Done `backend/.env.example:1` + `firmware/gateway-esp32/secrets.h.example:1` + `.gitignore:219` `firmware/**/secrets.h` (DEVOPS-11) | S | 2 | DEVOPS-01 | — |
| DEVOPS-09 | Pipeline de despliegue local (script de arranque único: `docker-compose up`) | C | 4 | todo lo anterior | — |

**Riesgo del área:** es la base fundacional (Sprint 1). Cualquier atraso aquí bloquea Móviles, LowCode y Estadística por igual — priorizar sobre cualquier otra área si hay que elegir.

---

## Área 3 — Automatizaciones y LowCode (Node-RED, Telegram)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| LOW-01 | ~~Confirmar bombillo `tuya-local`~~ — **CANCELADO 2026-09-01** (ADR-001, R-01 políticas API) | W | — | — | RF-4.2 cancelado |
| LOW-02 | ~~Flujo Node-RED: suscripción a topics MQTT de eventos~~ — **DEUDA TÉCNICA / CANCELADO 2026-09-09** (indicación asesor: no se usará Node-RED en esta iteración; se documenta como deuda, no se implementa) | W | — | — | RF-4.1 → Won't |
| LOW-03 | Bot de Telegram: creación vía BotFather + nodo de envío de mensajes | M | 4 | DEVOPS-02 (directo, LOW-02 deuda) | RF-4.1, HU-02 |
| LOW-04 | ~~Integración `tuya-local` en Node-RED~~ — **CANCELADO 2026-09-01** (ADR-001) | W | — | — | RF-4.2 cancelado |
| LOW-05 | Flujo de alerta de intrusión (láser → Telegram + LED RGB rojo) | M | 4 | LOW-03, MEGA-láser | HU-02 (sin bombillo) |
| LOW-06 | Cuadro de mando ejecutivo no-code (dashboard Node-RED opcional) | C | 4 | — (LOW-02 deuda) | Entregable PDF |
| LOW-07 | Manejo de reconexión si Node-RED pierde el broker MQTT | W | — | — (LOW-02 deuda) | — |

**Riesgo del área:** ~~LOW-01~~ cancelado 2026-09-01 + ~~LOW-02~~ deuda técnica 2026-09-09 (asesor). HU-02 ahora solo depende de Telegram directo + LED RGB local; RF-4.1/4.2 fuera de Node-RED en esta iteración.

---

## Área 4 — Administración y Planeación de Proyectos (Gestión)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| PM-01 | Product Backlog consolidado con criterios BDD (este documento + `requirements.md`) | M | Pre-Sprint 1 | — | Entregable PDF |
| PM-02 | Tablero Scrum (GitHub Projects u otro) reflejando este backlog — ✅ Done 2026-08-31 `https://github.com/users/Craos6518/projects/14` (Kanban 6 cols: Backlog/Sprint1 Done/Sprint2 In Progress/Deuda/Blocked/Done Sprint2) | M | Pre-Sprint 1 | PM-01 | — |
| PM-03 | Matriz de riesgos formal (incluir riesgo LOW-01 como riesgo alto) — ✅ Done `docs/risk-register.md:1` R-01..R-13 (R-01/R-07 cerrados, R-12 resuelto, R-02/R-03 críticos) | M | Sprint 1 | LOW-01 identificado | Entregable PDF |
| PM-04 | Diagrama de Gantt con dependencias entre sprints (base: `docs/sprints.md`) — ✅ Done `docs/gantt.md:1` Mermaid 4 sprints + deuda 1→2 (actualizado 2026-09-10 en `docs/revision-sprint2-completa`) | M | Sprint 1 | PM-01 | Entregable PDF |
| PM-05 | Actualizar "Estado actual" en `docs/sprints.md` al cierre de cada sprint — 🔄 Continuo (actualizado 2026-09-10 `docs/revision-sprint2-completa` con laser v2 + datasets 36.5k) | M | Continuo | — | — |
| PM-06 | Retro corta al cierre de cada sprint (qué se atrasó y por qué) | S | Continuo | — | — |
| PM-07 | Planning Poker para reestimar tareas si un sprint se atrasa >20% | C | Según necesidad | — | — |

**Riesgo del área:** esta área no bloquea código directamente, pero si PM-05 no se mantiene actualizado, un agente de código (o un colaborador nuevo) va a trabajar con información de sprint desactualizada — ver advertencia en `docs/sprints.md`.

---

## Área 5 — Estadística (Filtrado y Analítica)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| EST-01 | Prototipo del filtro EMA en Python (offline, con datos simulados) — ✅ Done Sprint 1-2 (`stats/ema_filter.py:15` `S_t=α·Y_t+(1-α)·S_{t-1}` α=0.2, `EMAFilter` + `calculate_noise_reduction` KPI >85%, bench `a051dd4` 531 muestras) | M | 1-2 (prototipo temprano) | — | RNF-2.1 |
| EST-02 | Implementación del EMA en firmware C++ (HC-SR04, α=0.2) | M | 4 | DEVOPS-05, EST-01 validado | RNF-2.1, HU-03 |
| EST-03 | Implementación del EMA para el sensor de sonido KY-037 | S | 4 | EST-02 | RNF-2.1 |
| EST-04 | Script de extracción de histórico desde PostgreSQL (`psycopg2`/SQLAlchemy) | M | 4 | DEVOPS-03 con datos reales | RNF-2.2 |
| EST-05 | Prueba de hipótesis t-Student (latencia RF vs. Wi-Fi) — 🔄 Adelantado parcial: `stats/water_turbidity_analysis.py:1` implementa Welch t-Student + ANOVA sobre 36.5k filas (EST-06/05 §7c) como validación externa previa a datos Rover reales; `EST-04` (extracción PG) queda Sprint 4 con histórico real | M | 4 | EST-04, datos de ambos protocolos disponibles | RNF-2.2 |
| EST-06 | Análisis descriptivo (medias, varianzas, gráficas) de sensores — 🔄 Adelantado parcial: `stats/water_turbidity_analysis.py` + `stats/data/water_turbidity_report.json` + `water_us_vs_true.png`/`water_ir_by_angle.png` + `notebooks/EMA_Estadistica.ipynb` §7c sobre 36.5k filas (water 31.5k + gesture 5k, CC BY-SA 4.0/CC0) — descriptivo completo Sprint 4 con datos propios | S | 4 | EST-04 | RNF-2.2 |
| EST-07 | Reporte final con resultados de t-Student y % de reducción de ruido (KPI >85%) | M | 4 | EST-02, EST-05 | KPI `prd.md` |

**Riesgo del área:** casi todo cae en Sprint 4 según el plan original, lo cual es ajustado — **EST-01 (prototipo offline) debería adelantarse a Sprint 1-2** con datos simulados, para no descubrir problemas de calibración del α a última hora.

---

## Resumen de riesgos críticos para cumplir el tiempo estipulado

1. ~~LOW-01 (tuya-local)~~ — **CANCELADO 2026-09-01** (ADR-001, políticas API) + ~~LOW-02~~ **DEUDA 2026-09-09** (no Node-RED).
2. **EST-01** (prototipo EMA) — ✅ adelantado Sprint 1-2 (`ema_filter.py` + bench 531) — pendiente port C++ `EST-02/03` Sprint 4.
3. **DEVOPS-01 a DEVOPS-05** — son la base de todo; cualquier atraso aquí es atraso de proyecto completo, no solo de un área.
4. **PM-05** — sin esto, la coordinación entre áreas (y entre agentes de código, si se usan) se degrada silenciosamente.

## Cómo se relaciona con el resto de la documentación

- IDs de este backlog pueden citarse en commits/PRs (ej. `MOV-05: implementa joystick virtual`).
- Cada ítem "Must" sin cerrar al final de su sprint debe reflejarse en la matriz de riesgos (PM-03) y en el Gantt (PM-04).
- Este archivo es operativo y debe actualizarse conforme avanza el proyecto; `docs/roadmap.md` explica el *conocimiento* necesario, este archivo explica el *trabajo* necesario.
