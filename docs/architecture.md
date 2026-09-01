# Arquitectura Técnica — AetherNet IoT & Autonomous Rover

Este documento describe **cómo** está construido el sistema. El PRD describe el *qué* y el *para quién*; `requirements.md` describe *qué debe cumplir*; este documento describe las decisiones de diseño y cómo se comunican los subsistemas entre sí. Referencia obligatoria antes de tocar cualquier protocolo de comunicación (ver regla en `AGENTS.md` §4).

---

## 1. Vista general del sistema

AetherNet es un sistema distribuido de 3 capas que vive completo dentro de una misma LAN (restricción de `prd.md` §6):

1. **Capa de percepción/actuación (Edge)** — microcontroladores que leen sensores y accionan actuadores en tiempo real, sin depender de la red para su función crítica.
2. **Capa de coordinación (Gateway + Backend)** — enruta eventos entre Edge y las capas de interfaz/automatización, persiste el histórico.
3. **Capa de interfaz y automatización** — app móvil (control humano) y Node-RED (reglas automáticas + notificaciones).

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
│                    │ App AetherControl│  │  Node-RED       │
│                    │ (Kotlin/Compose) │  │  (automation/)  │
│                    └──────────────────┘  └────┬───────┬────┘
│                                                │       │
│                                          Telegram   tuya-local
│                                          Bot API    (LAN, HTTP/TCP)
│                                                        │
│                                                 ┌──────▼──────────┐
│                                                 │ Bombillo Mercury │
│                                                 │ LB401 (RGB)      │
│                                                 └──────────────────┘
└─────────────────────────────────────────────────────────────────┘
```

## 2. Subsistemas y responsabilidades

| Subsistema | Responsabilidad | No es responsable de |
|---|---|---|
| **Gateway ESP32** (`firmware/gateway-esp32/`) | Servidor MQTT/WebSockets; traduce MQTT↔UART hacia el MEGA; traduce MQTT↔RF hacia el Rover | Lógica de negocio (vive en backend/Node-RED); persistencia |
| **MEGA — Acceso/Potencia** (`firmware/mega-access/`) | Teclado 4x4, servo de cerrojo, láser, LED RGB local — todo con procesamiento **Edge**, sin depender de red | Notificaciones remotas (eso es Node-RED, vía el evento que reporta al Gateway) |
| **Rover UNO** (`firmware/rover-uno/`) | Tracción (L298N), evasión de obstáculos (HC-SR04), anti-caída (TCRT5000), recepción de comandos RF | Decisión de "modo patrullaje" a alto nivel (eso llega como comando desde la app vía Gateway) |
| **Backend** (`backend/`) | FastAPI (API REST + WebSockets), Mosquitto (bus de eventos pub/sub), PostgreSQL (histórico de accesos/eventos) | Reglas de automatización (eso es Node-RED); UI |
| **App AetherControl** (`app/`) | Dashboard en tiempo real, joystick virtual, envío de PIN, fallback Bluetooth SPP | Almacenamiento persistente (consume el histórico vía backend, no lo posee) |
| **Node-RED** (`automation/`) | Motor de reglas: escucha eventos MQTT y dispara Telegram / cambios en el bombillo Tuya | Control de acceso físico (eso es del MEGA, de forma independiente) |
| **stats/** | Filtro EMA (y prototipo de Kalman) sobre lecturas de sensores; prueba t-Student RF vs. Wi-Fi | Actuar sobre los motores directamente — el filtro corre en firmware (ver §4), `stats/` es para el análisis offline/histórico |

## 3. Protocolos de comunicación

| Enlace | Protocolo | Por qué | Latencia objetivo (PRD) |
|---|---|---|---|
| Gateway ↔ Rover | RF 2.4GHz (nRF24L01), SPI | Sin dependencia de Wi-Fi; baja latencia para control de motores en tiempo real | < 10 ms |
| Gateway ↔ MEGA | UART (serial) | Enlace punto a punto simple, confiable a corta distancia física (mismo panel) | — |
| Gateway ↔ Backend ↔ App/Node-RED | MQTT (Mosquitto) + WebSockets | Pub/sub desacopla productores (sensores) de consumidores (app, Node-RED); WebSockets para push en tiempo real al dashboard | < 50 ms |
| App ↔ nodos críticos (fallback) | Bluetooth SPP | Contingencia si cae el Wi-Fi (RF-1.3); no reemplaza MQTT, es solo respaldo | — |
| Node-RED ↔ Bombillo Tuya | `tuya-local` (LAN, sin nube) | Cumplir RNF-3.1 (100% FOSS) evitando Tuya Cloud | — |
| Node-RED ↔ Telegram | HTTPS (Telegram Bot API) | Única salida a Internet del sistema; solo para notificaciones, no para control | — |

> **Nota de diseño:** Telegram es la única dependencia de red externa (Internet) de todo el sistema. Si el router pierde Internet, las notificaciones fallan pero el acceso físico (MEGA) sigue operando — ver Contingencia en `prd.md` §6 y riesgo R-09 en `risk-register.md`.

## 4. Flujo de datos: filtrado estadístico

```
Sensor (HC-SR04 / KY-037)
   → lectura analógica/digital en firmware (UNO/gateway)
   → filtro EMA en tiempo real: S_t = α·Y_t + (1-α)·S_{t-1}, α=0.2
   → valor suavizado usado para decisión inmediata (evasión de obstáculos)
   → evento/lectura publicado por MQTT
   → persistido en PostgreSQL (tabla de eventos/sensores)
   → extraído posteriormente por stats/ (psycopg2/SQLAlchemy)
   → análisis descriptivo + prueba t-Student (RF vs. Wi-Fi) → docs/reporte final (EST-07)
```

El EMA corre **en el firmware** (decisión en tiempo real); el análisis estadístico más pesado (t-Student, descriptivos) corre **offline en `stats/`** sobre el histórico ya persistido. No son el mismo paso — confundirlos es un error común al implementar EST-02 vs. EST-04/05.

## 5. Flujo de evento: HU-02 (alerta de intrusión)

1. Láser KY-008 se interrumpe → MEGA detecta el corte.
2. MEGA enciende **LED RGB local en rojo** (feedback inmediato, sin red) — RF-2.3, HU-02.
3. MEGA reporta el evento al Gateway vía UART.
4. Gateway publica el evento en un topic MQTT (ej. `aethernet/seguridad/intrusion`).
5. Backend lo persiste en PostgreSQL.
6. Node-RED, suscrito al mismo topic, dispara en paralelo:
   - Mensaje a Telegram (RF-4.1).
   - Cambio del bombillo Tuya a rojo parpadeante vía `tuya-local` (RF-4.2).
7. La app, también suscrita, refleja la alerta en el dashboard en tiempo real (RF-1.1).

Los pasos 2 y 6 son **independientes entre sí**: el LED local funciona aunque MQTT/Node-RED estén caídos (ver nota de diseño en `hardware-inventory.md`).

## 6. Decisiones de diseño y alternativas descartadas

| Decisión | Alternativa considerada | Por qué se descartó |
|---|---|---|
| MQTT (Mosquitto) como bus central | HTTP polling directo desde la app | Polling no escala bien a eventos en tiempo real y no desacopla productores/consumidores |
| `tuya-local` para el bombillo | Tuya Cloud API | Viola RNF-3.1 (100% FOSS, sin nube propietaria) |
| RF (nRF24L01) para Gateway↔Rover | Wi-Fi (ESP-NOW o socket) | RF dedicado da latencia más predecible para control de motores; Wi-Fi ya está ocupado por MQTT/telemetría general |
| EMA sobre Kalman para el filtro en producción | Filtro de Kalman completo | EMA es suficiente para el KPI de >85% de reducción de ruido con muchísima menor complejidad de implementación en firmware con recursos limitados; Kalman queda como comparación conceptual (ver `roadmap.md` §5) |

## 7. Pendiente de definición

- Esquema exacto de topics MQTT (nomenclatura, namespacing por subsistema) — documentar aquí una vez definido en DEVOPS-02.
- Formato exacto de los mensajes (payload JSON) entre subsistemas.
- Si Node-RED corre dentro de Docker Compose o como proceso separado en el host.
