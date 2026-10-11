# Email — DevOps TS6D3 (Julián Sánchez) — Versión final

**Asunto:** Propuesta y avances del componente DevOps (AetherNet) — TS6D3 Gr. 401

**Repositorio:** https://github.com/Craos6518/AetherNet-IoT-Autonomous-Rover

---

Estimado profesor Julián:

Espero que usted y sus seres queridos se encuentren bien tras el sismo del 10 de agosto (7:34 AM, mag. 7.4). Por mi parte, mi familia está a salvo y he podido apoyar labores de voluntariado local sin descuidar el trabajo académico desde casa. Solo tuvimos una semana de clases antes del evento.

Considerando que en la primera clase se indicó que se desarrollaría un proyecto integrador, aproveché este tiempo de forma autónoma para estructurar la propuesta formal del componente DevOps de **AetherNet IoT & Autonomous Rover** — proyecto FOSS 100% offline (sin nubes propietarias) — y avanzar en la infraestructura base que habilita a las demás áreas (App, Firmware, Estadística, LowCode).

**Alineación con el syllabus TS6D3 (T1–T6):**

* **T1 Fundamentos / CALMS:** Ciclo Plan→Code→Build→Test→Deploy→Operate→Monitor aplicado por sprints; Lean con base fundacional primero (Sprint 1 habilita todo); Sharing vía documentación trazable HU↔RF↔sprint↔archivo.
* **T2 Git:** Repositorio único como fuente de verdad, estrategia `main`/`develop`, feature branches y PRs; commits trazables a IDs del backlog (ej. `MOV-05: ...`).
* **T3 Linux / Shell:** Entorno Linux Mint + Docker CLI, scripts shell en pipeline (instalación `arduino-cli` vía curl, setup de cores/librerías), gestión de puertos serie y permisos.
* **T4 Docker + Compose (Taller 1):** 3 servicios orquestados (FastAPI + PostgreSQL 16 + Mosquitto 2.0) en red bridge `aethernet-net`, volúmenes persistentes, healthcheck `pg_isready` y `depends_on: service_healthy`, credenciales parametrizadas vía `env.example`.
* **T5 CI/CD (Taller 2):** Pipeline `.github/workflows/ci.yml` con 6 jobs en paralelo — `backend-test` (ruff/mypy/pytest), `firmware-compile` (matriz `arduino-cli` ESP32/AVR), `docker-build` (build + `docker-compose up` + `curl /health`), `stats-test`, `security-scan` (Trivy SARIF); `android-build` en plantilla desactivada (`if: false`) hasta existir estructura Gradle.
* **T6 Monitoreo:** Logs de contenedores y Mosquitto montados e inspeccionables; métricas conceptuales (KPIs PRD: latencia MQTT <50 ms, RF <10 ms, ruido >85%, FOSS 100%); stack Prometheus/Grafana registrado como brecha post-Sprint 4 — alerta de negocio (Telegram + LED RGB) sí implementada vía RF-4.1/HU-02.

**Talleres ↔ entregables reales:**

| Taller syllabus | Entregable AetherNet | Estado |
|---|---|---|
| Taller 1: Orquestación Docker Compose | `docker-compose.yml` (3 servicios, healthchecks, volúmenes, red interna) | ✅ Validado (`GET /health` ok) |
| Taller 2: CI con GitHub Actions | `.github/workflows/ci.yml` — matriz `arduino-cli` 3 firmwares + tests + docker-build | ✅ Activo en cada push |
| Proyecto Final: Flujo DevOps completo | Flujo Git → CI → Contenedores → Despliegue local `docker-compose up` → Logs | Base lista; DEVOPS-09 y T6 pendientes Sprint 4 |

**Entregables específicos propuestos:**

| Aplicación | Método / Herramienta | Resultado esperado |
|---|---|---|
| Orquestación local | Docker Compose (FastAPI + Postgres + Mosquitto) | `docker-compose up` levanta stack completo con healthchecks |
| CI firmware embebido | `arduino-cli` 1.5.1 en matriz (ESP32:esp32:esp32, AVR:mega/uno) | Compilación bloqueante en PR — ningún firmware se mergea si no compila |
| Calidad y seguridad | ruff + mypy + pytest + Trivy + docker-build verificado | Quality gate automatizado en 6 jobs paralelos |

**Estado actual:** Sprint 1 (infraestructura) cerrado 2026-09-01 — DEVOPS-01..05 validados en hardware (`docs/testing-rf-sprint1.md:32` 4/4, `nRF24L01 initialized`, `FAIL-SAFE 500ms`). DEVOPS-06..08 en curso Sprint 2. Caso especial IoT documentado: artefacto embebido compilado headless con FQBN por hardware y librerías pineadas (ArduinoJson 6.21.3).

**Documentación de referencia en el repositorio:**

* Mapa académico T1–T6 → aplicación: `docs/materias/devops.md`
* Backlog DevOps (DEVOPS-01..09): `docs/backlog.md:33`
* Sprints y habilitadores (Sprint 1 base fundacional): `docs/sprints.md:9`
* Requisitos RNF-1.1 (docker-compose) y RNF-1.2 (arduino-cli): `docs/requirements.md:38`
* Visión y KPIs: `docs/prd.md:5` y `docs/prd.md:46`
* Orquestación: `docker-compose.yml` + `backend/Dockerfile` + `env.example` + `backend/app/config.py`
* Pipeline CI: `.github/workflows/ci.yml` (jobs `backend-test`, `firmware-compile`, `docker-build`, `stats-test`, `security-scan`)
* Riesgos y ADR Tuya: `docs/risk-register.md:16` (R-01) y `docs/adr/adr-001-cancelacion-tuya.md` — RF-4.2 Won't, HU-02 solo LED RGB + Telegram
* Validación RF hardware: `docs/testing-rf-sprint1.md:32` y `docs/sprints.md:53`

Quedo atento a sus comentarios o ajustes para adaptar el cronograma de evaluación.

Un saludo cordial,

**Andres Felipe Martinez Henao**
Estudiante TS6D3 DevOps — Grupo 401
Proyecto Integrador: AetherNet IoT & Autonomous Rover
