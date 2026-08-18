Asunto: Avance proyecto integrador AetherNet — TS6D3 DevOps (contexto post-sismo)

Estimado profesor Julián:

Le escribo con mucho respeto por la situación que vive nuestra comunidad tras el **sismo del 10 de agosto (7:34 AM, mag. 7.4)**. Sé que muchos compañeros han sido afectados directamente — algunos viajando a sus lugares de origen, otros como voluntarios en zonas críticas, y varios con daños en sus hogares. En mi caso, mi familia y yo no sufrimos afectaciones graves, estoy colaborando como voluntario en lo que puedo, y cuento con las condiciones para seguir avanzando desde casa.

Solo alcanzamos a tener **una semana de clases** antes del evento. Mientras se define el retorno (presencial o virtual), quiero aprovechar este tiempo para **ir construyendo el proyecto integrador** de forma progresiva, de modo que al retomar actividades tenga una base sólida y pueda enfocarme en la integración y validación con el equipo.

---

### ¿Qué es AetherNet y de dónde nace?

**AetherNet IoT & Autonomous Rover** es mi **Proyecto Integrador de 5º semestre** (Ingeniería de Sistemas / Desarrollo de Software, UTP). Nace de la necesidad de demostrar que es posible construir un **ecosistema completo de domótica y robótica móvil 100% de código abierto (FOSS)**, sin depender de nubes propietarias (AWS, Azure, Tuya Cloud, etc.).

El sistema integra **5 subsistemas reales** que se comunican entre sí:
1. **App móvil Android** (Kotlin + Jetpack Compose) — interfaz de usuario
2. **Backend local en contenedores** (FastAPI + PostgreSQL + Mosquitto MQTT) — cerebro central
3. **Gateway ESP32** — puente entre Wi-Fi/MQTT y radiofrecuencia (2.4 GHz) / Puerto serie
4. **Controlador Arduino MEGA** — cerrojo electrónico (teclado + servo), trampa láser, relés
5. **Rover tanque autónomo (Arduino UNO)** — navegación por radiofrecuencia, evasión de obstáculos, anti-caída

**El "para qué":** Un sistema de seguridad y automatización que sigue funcionando aunque se caiga Internet o el router (procesamiento en el borde / *edge computing*), con alertas en tiempo real (Telegram) y control remoto desde el celular.

---

### Stack tecnológico (en términos simples)

| Componente | Tecnología | Qué hace |
|------------|------------|----------|
| **App** | Kotlin, Jetpack Compose (UI declarativa), MVVM | Pantallas reactivas, joystick virtual, MQTT, Bluetooth |
| **Backend** | Python (FastAPI), PostgreSQL, Docker, Docker Compose | API REST, base de datos, broker de mensajes MQTT |
| **Automatización** | Node-RED (programación visual / LowCode), Telegram Bot | Reglas: "si hay intrusión → avisa a Telegram + enciende luz roja" |
| **Firmware Gateway** | C++ (ESP32), Arduino CLI | Wi-Fi + Radio 2.4GHz (nRF24L01) + Puerto serie (UART) |
| **Firmware Acceso** | C++ (Arduino MEGA) | Teclado 4x4, Servomotor, Láser KY-008, LED RGB, Relés |
| **Firmware Rover** | C++ (Arduino UNO) | Motores (L298N), Ultrasonido HC-SR04, Sensores IR TCRT5000, Radio nRF24L01 |
| **Estadística** | Python (Pandas, SciPy) / R | Filtro EMA, Prueba t-Student, Análisis Weibull |
| **CI/CD** | GitHub Actions + `arduino-cli` | Compila y valida firmware C++ en cada push automáticamente |

> **Nota:** `arduino-cli` es la herramienta oficial de línea de comandos de Arduino para compilar código sin abrir el IDE. GitHub Actions es el servicio de automatización de GitHub (corre scripts en la nube gratis para repos públicos).

---

### Alineación con TS6D3 DevOps — Qué tema del syllabus cubre cada parte

| Tema del Syllabus | Cómo lo cubre AetherNet (ejemplo concreto) | Qué puedo avanzar en casa (software-only) |
|-------------------|--------------------------------------------|-------------------------------------------|
| **T1: Fundamentos DevOps** (Cultura CALMS, ciclo de vida) | Todo el proyecto vive este ciclo: Plan → Code → Build → Test → Deploy → Operate → Monitor | Documentar arquitectura, definir *Definition of Done* |
| **T2: Git** (ramas, merge, conflictos, trabajo en equipo) | Repositorio real con *feature branches*, *pull requests*, *commits convencionales* | ✅ Ya estructurado; pulir flujo de ramas |
| **T3: Linux / Shell** (terminal, permisos, scripts) | Entorno Docker/WSL, scripts de arranque único (`docker-compose up`), permisos de puertos serie | 🟡 Scripts de despliegue local, variables de entorno |
| **T4: Docker + Docker Compose** | **Core del backend**: 3 contenedores (API + BD + Broker MQTT) conectados en red interna, volúmenes persistentes | 🟡 `docker-compose.yml` completo, healthchecks, redes |
| **T5: CI/CD** (GitHub Actions, pipelines, despliegue) | **Pipeline obligatorio**: en cada `push` compila 3 firmwares C++ (ESP32, MEGA, UNO) con `arduino-cli` y falla si hay errores | 🟡 Workflow YAML listo, cache de dependencias, artefactos |
| **T6: Monitoreo** (Prometheus, Grafana, alertas) | KPIs medibles: Latencia RF <10ms, Filtro EMA >85% ruido, Falsos positivos láser 0%, FOSS 100% | ⏳ Dashboards Grafana + alertas básicas (pendiente lab) |

---

### Talleres del syllabus → Entregables reales del proyecto

| Taller Syllabus | Entregable en AetherNet | Estado |
|-----------------|-------------------------|--------|
| **Taller 1: Orquestación Docker Compose** | `backend/docker-compose.yml` con 3 servicios, healthchecks, volúmenes, red interna | 🟡 En curso |
| **Taller 2: CI con GitHub Actions** | `.github/workflows/ci-firmware.yml` compila ESP32 + MEGA + UNO con `arduino-cli` | 🟡 Planeado |
| **Proyecto Final: Flujo DevOps completo** | **El proyecto integrador entero**: Git → CI → Contenedores → Despliegue local → Monitoreo → Docs | 🟡 Base lista, integra en Sprint 4 |

---

### Mi plan durante la contingencia

**Importante:** Cuento con **todos los componentes electrónicos en casa** (ESP32, Arduino MEGA, Arduino UNO, módulos nRF24L01, HC-SR04, TCRT5000, KY-008, servos, relés, LED RGB, teclado 4x4, L298N, chasis oruga, bombillo Tuya, ESP8266, Arduino Nano, HC-06, sensores varios) y mi laptop con entorno de desarrollo completo. Esto me permite **flashear, depurar y probar comunicación real entre microcontroladores desde casa** (UART, SPI, Radio 2.4GHz), no solo compilar.

| Ahora (casa — hardware + software) | Cuando volvamos al lab (integración completa) |
|------------------------------------|-----------------------------------------------|
| `docker-compose.yml` completo + salud de contenedores | Pruebas de integración E2E sistema completo, latencia real, calibración |
| Pipeline `arduino-cli` compilando 3 firmwares + **flasheo y depuración real ESP32/MEGA/UNO** | Validación KPIs finales con sistema integrado |
| Scripts de arranque, variables `.env.example`, docs de arquitectura | Métricas reales, alertas, KPIs finales |
| Dashboards Grafana/Prometheus con datos simulados → **pruebas con datos reales de sensores** | Dashboards finales con datos de producción |
| **Comunicación UART ESP32↔MEGA, Radio nRF24L01 ESP32↔UNO, sensores HC-SR04/TCRT5000/KY-008** | Integración con Node-RED, Telegram, Tuya-local, App móvil |

---

### Evidencia técnica disponible (repositorio vivo, público)
- `backend/docker-compose.yml` — Orquestación 3 servicios
- `.github/workflows/ci-firmware.yml` — Pipeline `arduino-cli` (compila 3 firmwares)
- `docs/prd.md` — Visión, KPIs, restricciones
- `docs/requirements.md` — 14 Requisitos Funcionales + 8 No Funcionales + 4 Historias BDD
- `docs/sprints.md` — Planificación 4 sprints (8 semanas) con dependencias
- `docs/backlog.md` — ~40 tareas MoSCoW (Must/Should/Could/Won't) trazadas a RF/HU

---

Le pido disculpas si este correo resulta largo o inoportuno. Mi único objetivo es **transparentar qué estoy haciendo, por qué, y cómo se conecta con su materia**, para que cuando se reanuden clases podamos alinear expectativas y evaluación. Quedo a su disposición para ajustar alcances, ritmos o criterios según lo que usted y la facultad definan.

Con respeto y solidaridad,

**[Su Nombre]**
Estudiante TS6D3 DevOps - Grupo 401
Proyecto Integrador: AetherNet IoT & Autonomous Rover