# Arquitectura Técnica — AetherNet IoT & Autonomous Rover

Este documento describe **cómo** está construido el sistema. El PRD describe el *qué* y el *para quién*; `requirements.md` describe *qué debe cumplir*; este documento describe las decisiones de diseño y cómo se comunican los subsistemas entre sí. Referencia obligatoria antes de tocar cualquier protocolo de comunicación (ver regla en `AGENTS.md` §4).

---

## 1. Vista general del sistema

AetherNet es un sistema distribuido de 3 capas que vive completo dentro de una misma LAN (restricción de `prd.md` §6):

1. **Capa de percepción/actuación (Edge)** — microcontroladores que leen sensores y accionan actuadores en tiempo real, sin depender de la red para su función crítica.
2. **Capa de coordinación (Gateway + Backend)** — enruta eventos entre Edge y las capas de interfaz/automatización, persiste el histórico.
3. **Capa de interfaz y automatización** — app móvil (control humano) y Telegram Bot directo (notificaciones); Node-RED en **deuda técnica 2026-09-09** (no deploy esta iteración, ver `automation/flows/intrusion_alert.json` como referencia).

```
┌─────────────────────────────────────────────────────────────────┐
│                         LAN (misma subred)                       │
│                                                                   │
│  ┌──────────────┐  RF 2.4GHz   ┌──────────────┐                 │
│  │ Rover (UNO)  │◄────────────►│              │                 │
│  │ L298N        │  nRF24L01    │              │                 │
│  │ HC-SR04      │              │              │   UART          │
│  │ 3x TCRT5000  │              │  Gateway     │◄───────────────┐│
│  └──────────────┘              │  ESP32       │                ││
│                                 │              │      ┌─────────▼┴──────┐
│                                 └──────┬───────┘      │ MEGA (Acceso)    │
│                                        │ MQTT/WS       │ Teclado 4x4      │
│                                        │               │ Servo MG90S      │
│                          ┌─────────────▼────────┐      │ Láser KY-008     │
│                          │  Backend (Docker)     │      │ LED RGB local    │
│                          │  FastAPI + Mosquitto  │      └──────────────────┘
│                          │  MQTT + PostgreSQL    │
│                          └──┬────────────────┬───┘
│                             │ MQTT/WS         │ MQTT
│                    ┌────────▼───────┐  ┌──────▼─────────┐
│                    │ App AetherControl│  │  Telegram Bot  │
│                    │ (Kotlin/Compose) │  │  (HTTP directo │
│                    └──────────────────┘  │  automation/)  │
│                                         └────┬──────┘
│                                          Telegram
│                                          Bot API
│  (Node-RED `automation/` en deuda 2026-09-09 — ref JSON)
└─────────────────────────────────────────────────────────────────┘
```

## 2. Subsistemas y responsabilidades

| Subsistema | Responsabilidad | No es responsable de |
|---|---|---|
| **Gateway ESP32** (`firmware/gateway-esp32/`) | Servidor MQTT/WebSockets; traduce MQTT↔UART hacia el MEGA; traduce MQTT↔RF hacia el Rover | Lógica de negocio (vive en backend/Node-RED); persistencia |
| **MEGA — Acceso/Potencia** (`firmware/mega-access/`) | Teclado 4x4, servo MG90S, LED RGB local, **láser KY-008 + LDR discreta** (`laser.cpp` `7/8` `INPUT` 10k, `c7ce065` RF-2.3 HU-02), **sin matriz de relés** (eliminada 2026-08-26) — todo con procesamiento **Edge**, sin depender de red | Notificaciones remotas (vía `SECURITY:`→Gateway→Telegram directo; Node-RED referencia deuda) |
| **Rover UNO** (`firmware/rover-uno/`) | Tracción (L298N), evasión de obstáculos (HC-SR04), anti-caída (TCRT5000), recepción de comandos RF | Decisión de "modo patrullaje" a alto nivel (eso llega como comando desde la app vía Gateway) |
| **Backend** (`backend/`) | FastAPI (API REST + WebSockets, 8 endpoints `routers/events.py:25`), Mosquitto (bus `aethernet/#` + `$SYS/#`, `acl.conf:1`), PostgreSQL (4 tablas `models.py:34`) | Reglas de automatización (Telegram directo esta iteración; Node-RED deuda); UI |
| **App AetherControl** (`app/`) | Dashboard en tiempo real, joystick virtual, envío de PIN, fallback Bluetooth SPP | Almacenamiento persistente (consume el histórico vía backend, no lo posee) |
| **Telegram Bot** (`automation/flows/intrusion_alert.json` referencia) | Notificación directa intrusión: `aethernet/seguridad/intrusion` → HTTP `api.telegram.org/bot.../sendMessage` (RF-4.1 HU-02). Node-RED **DEUDA 2026-09-09** — flujo existe como JSON pero no se deploya | Control de acceso físico (MEGA Edge) |
| **stats/** + `notebooks/EMA_Estadistica.ipynb` | Filtro EMA (y prototipo Kalman) sobre lecturas HC-SR04; prueba t-Student/Welch + ANOVA; validación externa 36.5k filas (`water_turbidity` 31.5k + `gesture` 5k, `water_turbidity_analysis.py:1` + `water_turbidity_report.json`, bitácora canónica `notebooks/`, espejo `stats/notebooks/`, `docs/Estadistica/Datasets/`) | Actuar sobre motores directamente — EMA corre en firmware (ver §4), `stats/`+`notebooks/` son análisis offline/histórico |

## 3. Protocolos de comunicación

| Enlace | Protocolo | Por qué | Latencia objetivo (PRD) |
|---|---|---|---|
| Gateway ↔ Rover | RF 2.4GHz (nRF24L01), SPI | Sin dependencia de Wi-Fi; baja latencia para control de motores en tiempo real | < 10 ms |
| Gateway ↔ MEGA | UART (serial) | Enlace punto a punto simple, confiable a corta distancia física (mismo panel) | — |
| Gateway ↔ Backend (HU-01) | HTTP POST (`POST /api/access-events`) | Flujo cerrojo: MEGA→UART→Gateway→HTTP directo a FastAPI, más rápido que MQTT bridge (decisión `feature/firmware-mega-cerrojo` 2026-08-26); fallback MQTT `aethernet/access/event` solo debug | — |
| Gateway ↔ Backend ↔ App (+ Telegram) | MQTT (Mosquitto) + WebSockets | Pub/sub desacopla productores (sensores) de consumidores (app, Telegram bot); `aethernet/#` (`acl.conf:1` `rover/telemetry:69` `access/event:71` `seguridad/intrusion:72` `system/status:73`); WebSockets push dashboard | < 50 ms |
| App ↔ nodos críticos (fallback) | Bluetooth SPP | Contingencia si cae el Wi-Fi (RF-1.3); no reemplaza MQTT, es solo respaldo | — |
| Gateway/Backend ↔ Telegram | HTTPS (Telegram Bot API) | Única salida a Internet; notificación directa sin Node-RED esta iteración (flujo JSON `automation/flows/intrusion_alert.json` como referencia) | — |

> **Nota de diseño:** Telegram es la única dependencia de red externa (Internet) de todo el sistema. Si el router pierde Internet, las notificaciones fallan pero el acceso físico (MEGA) sigue operando — ver Contingencia en `prd.md` §6 y riesgo R-09 en `risk-register.md`.

## 4. Flujo de datos: filtrado estadístico

```
Sensor (HC-SR04 / KY-037)
   → lectura analógica/digital en firmware (UNO/gateway)
   → filtro EMA en tiempo real: S_t = α·Y_t + (1-α)·S_{t-1}, α=0.2 (stats/ema_filter.py:15, rover-uno.ino:259, notebooks/EMA_Estadistica.ipynb:2)
   → valor suavizado usado para decisión inmediata (evasión de obstáculos)
   → evento/lectura publicado por MQTT
   → persistido en PostgreSQL (tabla de eventos/sensores, backend/app/models.py, init.sql)
   → extraído posteriormente por stats/ (psycopg2/SQLAlchemy, stats/serial_plot_ema.py, visualize_ema.py)
   → análisis descriptivo + prueba t-Student/Welch + ANOVA (validado externo 36.5k `water_turbidity_analysis.py` + `water_turbidity_report.json`) → notebooks/EMA_Estadistica.ipynb §7c (canónico, docs/notebooks/README.md) + `docs/Estadistica/Datasets/` → docs/reporte final (EST-07)
```

El EMA corre **en el firmware** (decisión en tiempo real); el análisis estadístico más pesado (t-Student, descriptivos) corre **offline en `stats/`** sobre el histórico ya persistido y se documenta en `notebooks/EMA_Estadistica.ipynb` (canónico centralizado). No son el mismo paso — confundirlos es un error común al implementar EST-02 vs. EST-04/05.

## 5. Flujo de evento: HU-02 (alerta de intrusión)

1. Láser KY-008 se interrumpe → MEGA detecta el corte.
2. MEGA enciende **LED RGB local en rojo** (feedback inmediato, sin red) — RF-2.3, HU-02.
3. MEGA reporta el evento al Gateway vía UART.
4. Gateway publica el evento en un topic MQTT (ej. `aethernet/seguridad/intrusion`).
5. Backend lo persiste en PostgreSQL.
6. Gateway/Backend dispara mensaje a Telegram vía Bot API directo (RF-4.1) — Node-RED en deuda 2026-09-09 (flujo `automation/flows/intrusion_alert.json` es referencia, no deploy).
7. La app, también suscrita, refleja la alerta en el dashboard en tiempo real (RF-1.1).

Los pasos 2 y 6 son **independientes entre sí**: el LED RGB local funciona aunque MQTT/Node-RED estén caídos (ver `hardware-inventory.md`).

## 6. Decisiones de diseño y alternativas descartadas

| Decisión | Alternativa considerada | Por qué se descartó |
|---|---|---|
| MQTT (Mosquitto) como bus central | HTTP polling directo desde la app | Polling no escala bien a eventos en tiempo real y no desacopla productores/consumidores |
| Bombillo Tuya cancelado (2026-09-01) | `tuya-local` / Tuya Cloud API | Cancelado por incompatibilidad `local_key` (R-01) y complejidad; se mantiene solo LED RGB local + Telegram para HU-02 |
| RF (nRF24L01) para Gateway↔Rover | Wi-Fi (ESP-NOW o socket) | RF dedicado da latencia más predecible para control de motores; Wi-Fi ya está ocupado por MQTT/telemetría general |
| EMA sobre Kalman para el filtro en producción | Filtro de Kalman completo | EMA es suficiente para el KPI de >85% de reducción de ruido con muchísima menor complejidad de implementación en firmware con recursos limitados; Kalman queda como comparación conceptual (ver `roadmap.md` §5) |
| Node-RED no deployado Sprint 2-3 (deuda 2026-09-09) | Node-RED en Docker Compose con sub `aethernet/#` | Asesor indica no usar Node-RED esta iteración; se mantiene `automation/flows/intrusion_alert.json` como referencia JSON y se notifica vía Telegram HTTP directo — simplifica deploy LAN y cumple RF-4.1/HU-02 sin capa LowCode |

## 7. Topics MQTT y payloads (definidos DEVOPS-02 — ya no pendiente)

- **Topics canónicos** (`backend/mosquitto/config/acl.conf:1`, `gateway-esp32.ino:68`, `backend/app/routers/events.py:25`):
  - `aethernet/rover/telemetry` (Rover→Gateway→App, `RoverTelemetry` RF-3.1)
  - `aethernet/rover/command` (App→Gateway→Rover joystick RF-1.2)
  - `aethernet/access/command` (App→Gateway `70` `publishAccessCommand`)
  - `aethernet/access/event` (`71` `ACCESS:hash` desde MEGA→Gateway→Backend `POST /api/access-events`)
  - `aethernet/seguridad/intrusion` (`72` `SECURITY:` láser HU-02 → Telegram)
  - `aethernet/system/status` (`73` gateway heartbeat) + `aethernet/mega/status` + `aethernet/sensor/*` (HU-03)
  - Namespace `aethernet/#` + `$SYS/#` (`acl.conf:1`)
- **Payloads JSON** (ver `backend/app/schemas.py:1` Pydantic): `AccessEventCreate {user_id, pin_hash, success, source}`, `SensorEvent {sensor_type, value}`, `SecurityEvent {event_type, sensor, severity}`, `RoverTelemetry {l_vel, r_vel, distance_cm, session_id}`
- **Node-RED:** en **deuda técnica 2026-09-09** — no corre en Docker Compose esta iteración; `automation/flows/intrusion_alert.json` queda como referencia exportable para Sprint 4 si se retoma.
