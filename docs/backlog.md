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
| MOV-01 | Setup proyecto Android (Kotlin + Compose + arquitectura MVVM base) | M | 1 | DEVOPS-01 (para saber endpoints) | RF-1.1 |
| MOV-02 | Pantallas de control: LED local (solo lectura) — ~~bombillo Tuya cancelado~~ | M | 2 | MEGA firmware básico | RF-1.1 |
| MOV-03 | Cliente MQTT/WebSocket en la app, suscripción a topics de telemetría | M | 2 | DEVOPS-02 (broker corriendo) | RF-1.1 |
| MOV-04 | Módulo de PIN/clave para envío de comandos de cerrojo desde la app | S | 2 | MOV-03 | HU-01 |
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
| DEVOPS-01 | `docker-compose.yml` con FastAPI + PostgreSQL + Mosquitto MQTT | M | 1 | — | RNF-1.1 |
| DEVOPS-02 | Configuración de Mosquitto (topics, ACLs mínimas) | M | 1 | DEVOPS-01 | RF-2.1, RF-4.2 |
| DEVOPS-03 | Esquema inicial de PostgreSQL (tabla de eventos/accesos) | M | 1 | DEVOPS-01 | RNF-2.2 |
| DEVOPS-04 | Pipeline GitHub Actions con `arduino-cli` (compilación de firmware en cada push) | M | 1 | — | RNF-1.2 |
| DEVOPS-05 | Prueba de comunicación SPI/RF entre ESP32 y Arduino UNO (nRF24L01) | M | 1 | — | RF-3.1 |
| DEVOPS-06 | Endpoints FastAPI mínimos (health check, registro de eventos) | M | 2 | DEVOPS-01, DEVOPS-03 | RF-2.1 |
| DEVOPS-07 | Tests PyTest para endpoints de FastAPI | S | 2 | DEVOPS-06 | RNF (DevOps) |
| DEVOPS-08 | Documentar variables de entorno / `.env.example` (sin credenciales reales) | S | 2 | DEVOPS-01 | — |
| DEVOPS-09 | Pipeline de despliegue local (script de arranque único: `docker-compose up`) | C | 4 | todo lo anterior | — |

**Riesgo del área:** es la base fundacional (Sprint 1). Cualquier atraso aquí bloquea Móviles, LowCode y Estadística por igual — priorizar sobre cualquier otra área si hay que elegir.

---

## Área 3 — Automatizaciones y LowCode (Node-RED, Telegram)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| LOW-01 | ~~Confirmar bombillo `tuya-local`~~ — **CANCELADO 2026-09-01** (ADR-001, R-01 políticas API) | W | — | — | RF-4.2 cancelado |
| LOW-02 | Flujo Node-RED: suscripción a topics MQTT de eventos | M | 2 | DEVOPS-02 | RF-4.1 |
| LOW-03 | Bot de Telegram: creación vía BotFather + nodo de envío de mensajes | M | 4 | LOW-02 | RF-4.1, HU-02 |
| LOW-04 | ~~Integración `tuya-local` en Node-RED~~ — **CANCELADO 2026-09-01** (ADR-001) | W | — | — | RF-4.2 cancelado |
| LOW-05 | Flujo de alerta de intrusión (láser → Telegram + LED RGB rojo) | M | 4 | LOW-03, MEGA-láser | HU-02 (sin bombillo) |
| LOW-06 | Cuadro de mando ejecutivo no-code (dashboard Node-RED opcional) | C | 4 | LOW-02 | Entregable PDF |
| LOW-07 | Manejo de reconexión si Node-RED pierde el broker MQTT | C | 4 | LOW-02 | — |

**Riesgo del área:** ~~LOW-01~~ cancelado 2026-09-01. HU-02 ahora solo depende de Telegram + LED RGB local; RF-4.2 fuera de alcance.

---

## Área 4 — Administración y Planeación de Proyectos (Gestión)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| PM-01 | Product Backlog consolidado con criterios BDD (este documento + `requirements.md`) | M | Pre-Sprint 1 | — | Entregable PDF |
| PM-02 | Tablero Scrum (GitHub Projects u otro) reflejando este backlog | M | Pre-Sprint 1 | PM-01 | — |
| PM-03 | Matriz de riesgos formal (incluir riesgo LOW-01 como riesgo alto) | M | Sprint 1 | LOW-01 identificado | Entregable PDF |
| PM-04 | Diagrama de Gantt con dependencias entre sprints (base: `docs/sprints.md`) | M | Sprint 1 | PM-01 | Entregable PDF |
| PM-05 | Actualizar "Estado actual" en `docs/sprints.md` al cierre de cada sprint | M | Continuo | — | — |
| PM-06 | Retro corta al cierre de cada sprint (qué se atrasó y por qué) | S | Continuo | — | — |
| PM-07 | Planning Poker para reestimar tareas si un sprint se atrasa >20% | C | Según necesidad | — | — |

**Riesgo del área:** esta área no bloquea código directamente, pero si PM-05 no se mantiene actualizado, un agente de código (o un colaborador nuevo) va a trabajar con información de sprint desactualizada — ver advertencia en `docs/sprints.md`.

---

## Área 5 — Estadística (Filtrado y Analítica)

| ID | Tarea | Prioridad | Sprint | Depende de | Origen |
|---|---|---|---|---|---|
| EST-01 | Prototipo del filtro EMA en Python (offline, con datos simulados) | M | 1-2 (prototipo temprano) | — | RNF-2.1 |
| EST-02 | Implementación del EMA en firmware C++ (HC-SR04, α=0.2) | M | 4 | DEVOPS-05, EST-01 validado | RNF-2.1, HU-03 |
| EST-03 | Implementación del EMA para el sensor de sonido KY-037 | S | 4 | EST-02 | RNF-2.1 |
| EST-04 | Script de extracción de histórico desde PostgreSQL (`psycopg2`/SQLAlchemy) | M | 4 | DEVOPS-03 con datos reales | RNF-2.2 |
| EST-05 | Prueba de hipótesis t-Student (latencia RF vs. Wi-Fi) | M | 4 | EST-04, datos de ambos protocolos disponibles | RNF-2.2 |
| EST-06 | Análisis descriptivo (medias, varianzas, gráficas) de sensores | S | 4 | EST-04 | RNF-2.2 |
| EST-07 | Reporte final con resultados de t-Student y % de reducción de ruido (KPI >85%) | M | 4 | EST-02, EST-05 | KPI `prd.md` |

**Riesgo del área:** casi todo cae en Sprint 4 según el plan original, lo cual es ajustado — **EST-01 (prototipo offline) debería adelantarse a Sprint 1-2** con datos simulados, para no descubrir problemas de calibración del α a última hora.

---

## Resumen de riesgos críticos para cumplir el tiempo estipulado

1. ~~LOW-01 (tuya-local)~~ — **CANCELADO 2026-09-01** (ADR-001, políticas API).
2. **EST-01** (prototipo EMA) — adelantar a Sprint 1-2 en vez de dejarlo todo para el cierre.
3. **DEVOPS-01 a DEVOPS-05** — son la base de todo; cualquier atraso aquí es atraso de proyecto completo, no solo de un área.
4. **PM-05** — sin esto, la coordinación entre áreas (y entre agentes de código, si se usan) se degrada silenciosamente.

## Cómo se relaciona con el resto de la documentación

- IDs de este backlog pueden citarse en commits/PRs (ej. `MOV-05: implementa joystick virtual`).
- Cada ítem "Must" sin cerrar al final de su sprint debe reflejarse en la matriz de riesgos (PM-03) y en el Gantt (PM-04).
- Este archivo es operativo y debe actualizarse conforme avanza el proyecto; `docs/roadmap.md` explica el *conocimiento* necesario, este archivo explica el *trabajo* necesario.
