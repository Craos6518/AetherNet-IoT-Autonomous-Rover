# AetherNet — IoT & Autonomous Rover

Plataforma distribuida de domótica modular, telemetría estadística y robótica móvil. Proyecto Integrador de 5º semestre de Ingeniería de Sistemas / Desarrollo de Software (UTP) — **100% FOSS**, sin dependencia de nubes propietarias (AWS, GCP, Azure, Tuya Cloud).

El sistema controla acceso físico (teclado + cerrojo), detecta intrusiones (trampa láser), notifica en tiempo real vía Telegram + LED RGB local, y opera un rover tanque con evasión autónoma — todo coordinado desde app Android y backend en la misma LAN (bombillo Tuya cancelado 2026-09-01).

## 🧠 ¿Eres un agente de código?

Lee **[`AGENTS.md`](./AGENTS.md)** antes de tocar cualquier archivo. Ahí está el orden de lectura de `/docs`, el mapa de stack por componente y las convenciones de trabajo — no se repiten aquí para evitar que queden desincronizados.

## 🗂️ Estructura del repositorio

```
.
├── app/                      # App Android "AetherControl" — Kotlin, Jetpack Compose, MVVM
├── backend/                  # FastAPI + PostgreSQL + Mosquitto MQTT + Docker Compose
├── automation/               # Node-RED, Telegram Bot API (tuya-local cancelado 2026-09-01)
├── firmware/
│   ├── gateway-esp32/        # Gateway central — ESP32, UART, nRF24L01
│   ├── mega-access/          # Control de acceso/potencia — Arduino MEGA
│   └── rover-uno/            # Rover tanque autónomo — Arduino UNO, L298N
├── stats/                    # Filtrado EMA / prueba t-Student — Python o R
├── docs/                     # PRD, requisitos, hardware, sprints, roadmap, backlog
├── .github/workflows/        # CI/CD con arduino-cli
├── AGENTS.md
└── README.md
```

> Rutas orientativas hasta confirmar la estructura real del repo — ajústalas aquí si difieren.

## 🚀 Levantar el entorno local

Requisitos: Docker y Docker Compose.

```bash
git clone https://github.com/Craos6518/AetherNet-IoT-Autonomous-Rover.git
cd AetherNet-IoT-Autonomous-Rover
cp .env.example .env    # ajusta las variables (ver DEVOPS-08 en docs/backlog.md)
docker-compose up       # levanta FastAPI + PostgreSQL + Mosquitto MQTT
```

<!-- TODO: agrega instrucciones de compilación/flasheo de firmware por microcontrolador (arduino-cli) y de build de la app Kotlin -->

## 📚 Documentación

El detalle completo (visión, requisitos, hardware, sprints, roadmap, backlog) vive en [`/docs`](./docs). Para saber en qué orden leerlos y por qué, ver la sección 1 de [`AGENTS.md`](./AGENTS.md).

## 🧪 Estado actual

Sprint activo: `<pendiente de definir>` — ver [`docs/sprints.md`](./docs/sprints.md#estado-actual).

**2026-09-01:** bombillo Tuya / `tuya-local` **CANCELADO** (R-01). Riesgo crítico cerrado; HU-02 ahora solo Telegram + LED RGB local.

## 📄 Licencia

<!-- TODO: el stack debe ser 100% FOSS (RNF-3.1), pero falta definir la licencia del repositorio en sí -->