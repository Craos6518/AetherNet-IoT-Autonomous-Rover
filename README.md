# AetherNet — IoT & Autonomous Rover

Plataforma distribuida de domótica modular, telemetría estadística y robótica móvil. Proyecto Integrador del 5º semestre de Ingeniería de Sistemas / Desarrollo de Software (UTP), con enfoque en automatización local, control de acceso físico y navegación autónoma con hardware abierto y sin dependencia de nubes propietarias.

El sistema integra una app Android, un backend local con FastAPI + PostgreSQL + Mosquitto, firmware en ESP32, Arduino MEGA y UNO, y un rover con evasión autónoma. En la versión vigente, la notificación de intrusión se gestiona por Telegram directo y el LED RGB local del MEGA sigue siendo el indicador físico principal, mientras que la integración con bombillo Tuya quedó cancelada por incompatibilidad con la política FOSS del proyecto.

## 🧠 Regla de trabajo

Lee [AGENTS.md](AGENTS.md) antes de tocar cualquier archivo. Allí se define el orden de lectura, el mapa del stack por componente y las convenciones del repositorio.

## 🗂️ Estructura del repositorio

```text
.
├── app/                        # App Android "AetherControl" — Kotlin + Jetpack Compose
├── backend/                    # FastAPI + PostgreSQL + Mosquitto + Docker Compose
├── automation/                 # Referencia JSON del flujo de alertas e integración Telegram
├── firmware/
│   ├── gateway-esp32/         # Gateway central — ESP32 + UART + RF24
│   ├── mega-access/           # Control de acceso y alarma — Arduino MEGA
│   ├── rover-uno/             # Rover tanque autónomo — Arduino UNO + L298N
│   └── ...                    # bancos de prueba e integración
├── stats/                      # EMA, datasets, scripts de análisis y validación
├── docs/                       # PRD, requisitos, hardware, sprints, roadmap y backlog
├── .github/workflows/          # CI/CD para firmware y validación del proyecto
├── AGENTS.md
├── README.md
├── LICENSE
├── NOTICE
├── docker-compose.yml
├── build.gradle.kts
└── settings.gradle.kts
```

## 🚀 Levantar el entorno local

Requisitos: Docker y Docker Compose.

```bash
git clone https://github.com/Craos6518/AetherNet-IoT-Autonomous-Rover.git
cd AetherNet-IoT-Autonomous-Rover
cp backend/.env.example backend/.env
cp firmware/gateway-esp32/secrets.h.example firmware/gateway-esp32/secrets.h
docker compose up --build
```

Para la app Android se usa Gradle local:

```bash
./gradlew assembleDebug
```

## 📚 Documentación central

La documentación del proyecto vive en [docs/README.md](docs/README.md). El orden recomendado de lectura es:

1. [docs/prd.md](docs/prd.md)
2. [docs/requirements.md](docs/requirements.md)
3. [docs/hardware-inventory.md](docs/hardware-inventory.md)
4. [docs/sprints.md](docs/sprints.md)
5. [docs/roadmap.md](docs/roadmap.md)
6. [docs/backlog.md](docs/backlog.md)
7. [docs/architecture.md](docs/architecture.md)

## 🧪 Estado actual (2026-09-14)

- Sprint activo: Sprint 3, centrado en rover, telemetría y validación del enlace RF + evasión por sensores.
- Integración de seguridad: Telegram directo vía Bot API HTTP; Node-RED queda como referencia técnica y no como flujo activo de producción.
- Bombillo inteligente Tuya: cancelado por incompatibilidad con la política FOSS del proyecto.
- LED RGB local en Arduino MEGA: sigue siendo el indicador visual principal para acceso y alarma local.
- El backend FastAPI y el servicio MQTT están documentados como una base local de coordinación y persistencia, no como sistema de nube.

## 📱 App AetherControl

La app Android está diseñada como una interfaz de supervisión y control local, con:

- Dashboard en tiempo real.
- Estado de MQTT y backend.
- LED local derivado de eventos de acceso y seguridad.
- Pantalla de PIN para desbloqueo físico.
- Joystick virtual para control del rover.
- Arquitectura MVVM con flujo de estados y repositorio central.

## 🧰 Stack principal

- Android: Kotlin + Jetpack Compose + MVVM + StateFlow
- Backend: FastAPI + SQLAlchemy + PostgreSQL + Mosquitto MQTT
- Firmware: ESP32, Arduino MEGA, Arduino UNO
- Protocolos clave: UART, RF24, MQTT, HTTP
- Estadística: EMA, análisis descriptivo y validación experimental

## 📄 Licencia

El código del proyecto se distribuye bajo la [LICENSE](LICENSE) (Apache 2.0).

Los datasets usados para análisis y validación conservan sus licencias de origen; la documentación y el código del proyecto mantienen la compatibilidad con el principio de software 100% FOSS.

## 🔎 Repositorios y documentación clave

- [docs/architecture.md](docs/architecture.md)
- [docs/prd.md](docs/prd.md)
- [docs/requirements.md](docs/requirements.md)
- [docs/backlog.md](docs/backlog.md)
- [docs/roadmap.md](docs/roadmap.md)
- [docs/hardware-inventory.md](docs/hardware-inventory.md)
- [stats/README.md](stats/README.md)
- [backend/README.md](backend/README.md)
- [app/README.md](app/README.md)
- [firmware/README.md](firmware/README.md)
