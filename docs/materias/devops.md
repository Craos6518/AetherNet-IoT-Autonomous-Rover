# Documentación Académica — DevOps

**Proyecto:** AetherNet IoT & Autonomous Rover
**Asignatura UTP:** DevOps (electiva, según `docs/UTP/DEVOPS - EST.pdf`)
**Área del proyecto:** Área 2 — DevOps (Infraestructura, CI/CD, Firmware base) · ver `docs/backlog.md` §Área 2

> Mapeo del contenido académico del PDF contra lo implementado en el proyecto. Referencias cruzadas: RNF-1.1, RNF-1.2 · backlog DEVOPS-01 a DEVOPS-09.

---

## 1. Contenido académico según el PDF

| Tema | Contenido |
|---|---|
| **T1** | Fundamentos DevOps: cultura, evolución, ciclo de vida, modelo **CALMS** (Culture, Automation, Lean, Measurement, Sharing) |
| **T2** | Introducción a Git: repos locales/remotos, init/clone/add/commit/push/pull; ramificación, merge, resolución de conflictos, trabajo colaborativo |
| **T3** | Fundamentos de Linux: comandos de terminal (ls, cd, grep, ps, chmod); automatización con scripts de shell |
| **T4** | Contenedores: imágenes, Dockerfile, Docker Hub; orquestación multi-contenedor con **Docker Compose** (redes internas, volúmenes, variables de entorno, dependencias entre servicios) |
| **T5** | CI/CD: pipelines con **GitHub Actions/GitLab CI**, pruebas automáticas, despliegue automático, manejo de variables de entorno y seguridad |
| **T6** | Monitoreo, métricas y logs: observabilidad (**Prometheus + Grafana**, logs centralizados Loki/ELK), alertas por umbrales |

Talleres del PDF: Taller 1 (Docker Compose multi-servicio), Taller 2 (CI básico con GitHub Actions), Proyecto Final (flujo DevOps completo). **Este proyecto cumple el rol de Proyecto Final.**

---

## 2. Mapa: tema académico → aplicación en el proyecto → área

### T1 — Fundamentos DevOps / CALMS ✅ Aplicado

| Pilar CALMS | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| **C**ulture | Decisión de arquitectura 100% FOSS sin nubes propietarias; documentación viva como contrato entre áreas (`AGENTS.md`) | `AGENTS.md`, RNF-3.1, `docs/prd.md` §1 |
| **A**utomation | Compilación firmware, linting, tests y build de contenedores 100% automatizados en cada push | `.github/workflows/ci.yml` |
| **L**ean | Entrega incremental por sprints con base fundacional primero (Sprint 1 = infraestructura que habilita todo) | `docs/sprints.md` Sprint 1 |
| **M**easurement | KPIs medibles definidos antes de construir (latencia <50 ms MQTT, <10 ms RF, >85% reducción ruido); healthcheck de Postgres en Compose | `docs/prd.md` §5, `docker-compose.yml` (healthcheck) |
| **S**haring | Documentación indexada y trazable HU ↔ RF ↔ sprint ↔ archivo para humanos y agentes de código | `AGENTS.md`, `docs/backlog.md` §final |

### T2 — Git ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Repos remotos, push/pull | Repositorio GitHub único como fuente de verdad | Raíz del repo |
| Ramificación | Estrategia de branches `main`/`develop` configurada como trigger del pipeline | `.github/workflows/ci.yml:3-9` (`on.push.branches`) |
| Trabajo colaborativo / PRs | PRs hacia main/develop disparan CI; commits referencian ítems del backlog (ej. `MOV-05: implementa joystick virtual`) | `.github/workflows/ci.yml:10-12`; convención en `docs/backlog.md` §"Cómo se relaciona" |

### T3 — Fundamentos de Linux ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Terminal, permisos, procesos | Entorno de desarrollo y despliegue local sobre Linux Mint; gestión de servicios con Docker CLI | `docs/sprints.md` Sprint 1 |
| Scripts de automatización | Pipeline ejecuta scripts shell (instalación de arduino-cli vía curl, setup de cores y librerías) | `.github/workflows/ci.yml` job `firmware-compile` |

### T4 — Contenedores y Docker Compose ✅ Aplicado (Taller 1 del PDF)

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Imagen personalizada con Dockerfile | Backend FastAPI construido desde su propio Dockerfile | `backend/Dockerfile` → referenciado en `docker-compose.yml` (servicio `fastapi.build`) |
| Orquestación multi-servicio (frontend/backend/BD) | **Tres servicios orquestados:** PostgreSQL 16, Mosquitto MQTT 2.0, FastAPI | `docker-compose.yml` |
| Red interna | Red bridge dedicada `aethernet-net`; los servicios se resuelven por nombre (FastAPI apunta a host `mosquitto` y `postgres`) | `docker-compose.yml` (networks + env vars) |
| Volúmenes persistentes | Volumen `postgres_data` para persistencia de BD; montajes read-only de configs de Mosquitto e `init.sql` | `docker-compose.yml` (volumes) |
| Dependencias entre servicios | `depends_on` con condición `service_healthy`: FastAPI arranca solo cuando Postgres pasa `pg_isready` | `docker-compose.yml` (healthcheck + depends_on) |
| Variables de entorno | Credenciales parametrizadas con valores por defecto (`${POSTGRES_USER:-aethernet}`), plantilla sin secretos reales | `docker-compose.yml`, `env.example`, `backend/app/config.py` (pydantic-settings) |

### T5 — Integración y Despliegue Continuo ✅ Aplicado (Taller 2 del PDF)

El pipeline `.github/workflows/ci.yml` implementa un flujo CI completo con **6 jobs**, varios en paralelo:

| Job del pipeline | Qué hace | Tema PDF que evidencia |
|---|---|---|
| `backend-test` | Lint (**ruff**) + tipos (**mypy**) + tests (**pytest**) sobre Python 3.12 con cache de pip | T5: automatización de pruebas |
| `firmware-compile` | Matriz de compilación con **arduino-cli** de los 3 firmwares (`gateway-esp32` con core ESP32; `mega-access` y `rover-uno` con core AVR) instalando librerías por firmware | T4+T5: infraestructura automatizada — caso especial IoT no cubierto literalmente en el PDF |
| `docker-build` | Build sin cache + `docker-compose up` real + verificación del endpoint `/health` con curl + teardown | T4+T5: prueba de despliegue |
| `stats-test` | PyTest sobre el módulo estadístico | T5 |
| `android-build` | Plantilla lista (JDK 17 + Android SDK + gradlew) pero **desactivada** (`if: false`) hasta que exista estructura Gradle | Brecha declarada |
| `security-scan` | Escaneo de vulnerabilidades con **Trivy** subiendo resultados SARIF a GitHub Security | T5: "buenas prácticas de seguridad" |

Despliegue continuo: el PDF menciona Heroku/Vercel/Render — **no aplica**: el despliegue es local (LAN) por diseño FOSS/offline (`docs/prd.md` §6). El equivalente es el script de arranque único `docker-compose up` (backlog DEVOPS-09).

### T6 — Monitoreo, métricas y logs ⚠️ Parcialmente aplicado

| Lo que se ve en el contenido | Estado en el proyecto | Dónde |
|---|---|---|
| Logs centralizados | **Parcial:** volúmenes de log de Mosquitto montados; logs de contenedores inspeccionables vía Docker; `docker-compose ps` en CI | `docker-compose.yml` (servicio mosquitto), job `docker-build` |
| Prometheus + Grafana | **No implementado.** Las métricas existen conceptualmente (KPIs PRD, telemetría MQTT del Rover) pero no hay stack de observabilidad | Gap registrado — candidato a mejora post-Sprint 4 |
| Alertas por umbrales | **Implementado a nivel negocio, no infra:** las alertas de intrusión (Telegram + LED RGB rojo) son reglas sobre eventos, no sobre salud del sistema | `automation/flows/intrusion_alert.json`, RF-4.1/HU-02 |

---

## 3. Caso especial: CI/CD para firmware embebido

Lo distintivo de este proyecto vs. un DevOps web clásico: el artefacto a compilar corre en microcontroladores, no en servidores.

- **Herramienta headless:** `arduino-cli` fijado a versión (`ARDUINO_CLI_VERSION: 1.0.4`) para builds reproducibles.
- **Matriz por hardware:** cada firmware declara su board FQBN (`esp32:esp32:esp32` vs `arduino:avr:mega`).
- **Regla de calidad del equipo:** ningún push de firmware se mergea si no compila en CI (convención `AGENTS.md` §3) — la integración continua reemplaza la falta de pruebas unitarias embebidas.

---

## 4. Pendientes / brechas (trazable al backlog)

| Ítem backlog | Descripción | Sprint |
|---|---|---|
| DEVOPS-02 | ACLs de Mosquitto | 1 |
| DEVOPS-07 | Tests PyTest de endpoints FastAPI | 2 |
| DEVOPS-09 | Script de arranque único | 4 |
| — | Stack Prometheus/Grafana (T6 del PDF aún sin cobertura real) | Post-proyecto |

## 5. Trazabilidad

- **Requisitos:** RNF-1.1 (orquestación docker-compose), RNF-1.2 (pipeline CI/CD con arduino-cli)
- **Áreas que dependen de esta:** todas (Móviles consume el broker/API; LowCode suscribe topics; Estadística necesita BD con datos; Administración mide avance sobre esta base)
