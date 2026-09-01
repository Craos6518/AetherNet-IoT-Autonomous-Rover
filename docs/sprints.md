# Planeación de Sprints — AetherNet IoT

Fuente base: sección 4 del documento académico (PDF Proyecto Integrador UTP). Cada tarea se referencia contra `docs/requirements.md` para que un agente de código sepa qué Historia de Usuario o Requisito Funcional está habilitando, y no adelante trabajo de un sprint futuro sin que exista la base del sprint anterior.

> **Nota:** este archivo describe la planeación *original*. Si el equipo se desvía de fechas o alcance, actualízalo aquí — un agente que lea un sprint desactualizado puede proponer trabajo que ya no aplica o saltarse dependencias reales.

---

## Sprint 1 (Semanas 1-2): Infraestructura & Firmware Base

- Configuración del entorno Docker (FastAPI, PostgreSQL, Mosquitto MQTT) en Linux Mint. → RNF-1.1
- Creación del pipeline CI/CD en GitHub Actions con `arduino-cli` para compilar C++. → RNF-1.2
- Pruebas de comunicación SPI (nRF24L01) entre ESP32 y Arduino UNO. → RF-2.1 (base), RF-3.1

**Habilita:** toda la infraestructura sobre la que corren los sprints siguientes. Ningún sprint posterior debería avanzar sin que esto esté cerrado.

---

## Sprint 2 (Semanas 3-4): Domótica Fija & Control de Acceso

- Implementación del cerrojo (Teclado 4x4 + Servo MG90S) en Arduino MEGA. → RF-2.2, HU-01
- **LED RGB local en el MEGA** (feedback verde/rojo de acceso). → HU-01, HU-02 (ver `docs/hardware-inventory.md`)
- Primeras pantallas en Jetpack Compose (Kotlin): control bombillo Tuya + estado LED local. → RF-1.1

**Depende de:** Sprint 1 (Docker + UART funcionando).

---

## Sprint 3 (Semanas 5-6): Rover Tanque Autónomo & Telemetría

- Montaje mecánico del chasis oruga con motorreductores 9-12V y L298N.
- Algoritmo anti-caída (3x TCRT5000) y evasión de obstáculos (HC-SR04). → RF-3.2
- Joystick virtual en Jetpack Compose enviando comandos de baja latencia. → RF-1.2

**Depende de:** Sprint 1 (enlace RF ESP32 ↔ UNO probado).

---

## Sprint 4 (Semanas 7-8): LowCode, Filtrado Estadístico y Cierre

- Filtro de Kalman / EMA en Python para procesar datos de sensores (HC-SR04, KY-037). → RNF-2.1, HU-03
- Flujos en Node-RED con bot de Telegram para notificaciones de seguridad. → RF-4.1, HU-02
- **Integración del bombillo Tuya vía `tuya-local`** (parpadeo rojo en intrusión). → RF-4.2, HU-02
- Pruebas de integración End-to-End, documentación y pruebas unitarias.

**Depende de:** Sprint 2 (evento de intrusión ya disparándose desde el MEGA) y Sprint 3 (telemetría del Rover ya fluyendo).

---

## Estado actual

- Sprint activo: **Sprint 2 (Semanas 3-4): Domótica Fija & Control de Acceso — En curso**
- Última actualización: **2026-09-01 — MOV-01 real ✅ Done `f03190b` `feature/app-setup-mvvm` (RF-1.1, RNF-3.1) — verificado `192.168.1.14:8000/health` en SM-X620 + `assembleDebug`/`testDebugUnitTest` verdes** (ver `docs/cierre-mov01.md:4`, `docs/deuda-sprint1-sprint2.md:36`); `develop` al día con `a051dd4` EMA bench 531 + `f1ffcaa` stats + `f03190b` MOV-01
  - **MOV-01 real (2026-09-01):** `f03190b` implementa MVVM base real — `AetherControlApp.kt`, `ServiceLocator` DI manual, `Retrofit`+`kotlinx.serialization`, DTOs espejo `schemas.py`, `PreferencesManager` `DataStore` con `updateBaseUrl()` dinámico, `network_security_config` cleartext, `DashboardViewModel` `StateFlow` + `NavGraph` + `DashboardScreen` con editor `Backend URL`, `MainActivity` refactor + tests `turbine`. Verificado `docker 0.0.0.0:8000` + `curl 192.168.1.14:8000/health 200 ok` + App `ok` (antes `CLEARTEXT/SocketTimeout` fix). Cierra deuda `ACT-05` plantilla-only.
  - **Sinceramiento MOV-01 (2026-08-31):** dueño confirma 0 líneas Kotlin propias — `app/app/src/main/java/.../MainActivity.kt:22` y `DashboardViewModel.kt:32` son plantilla wizard `Empty Activity + Compose` con `TODO MQTT` sin implementación. Infra verde (`app/gradlew` 9.5.0 `tasks` OK, KSP 1.9.22-1.0.17, `ci.yml:241` condicional) pero **MOV-01 pasa a ⚠️ plantilla-only** — ver `docs/cierre-mov01.md:4`. Lógica MVVM real queda para `feature/app-setup-mvvm` en Sprint 2.
  - **LOW-01 pivot (2026-08-31):** `tuya-local` bloqueado por acceso difícil a `local_key` (R-01). Alternativa oficial en evaluación: **retirar o reemplazar por skill Alexa / Google Home** (ver `docs/risk-register.md:16` R-01). Si se confirma incompatibilidad, LOW-04 migrará a skill y se documentará excepción a `RNF-3.1` FOSS.
  - **Rotación cerrada (2026-08-31):** producción rotada (password prod cambiado); AP lab `FELIPE./2516f751` recreado idéntico para compatibilidad local — `R-12` Resuelto (ver `docs/auditoria-secretos-sprint1.md:15` §5 + `docs/risk-register.md:35`).
  - **PM-02 completado (2026-08-31):** `https://github.com/users/Craos6518/projects/14` — ver `docs/tablero-scrum.md:49`.
- Avances Sprint 1 verificados 2026-08-26 (base) + deltas 2026-08-31:
  - [x] DEVOPS-01/06: `docker-compose.yml` sin `version` obsoleta, `backend/app/main.py:16` + `schemas.py` + `routers/events.py:8` operativos; `GET /health` responde `{"status":"ok","database":"ok"}`; `POST /api/access-events` (HU-01), `/sensor-events` (HU-03), `/security-events` (HU-02), `/rover/telemetry` (RF-3.3) validados con curl + `docker compose up` (FastAPI 1.0.0-sprint1, Postgres 16, Mosquitto 2.0). `paho-mqtt==2.1.0` corregido en `backend/requirements.txt:8`.
  - [x] DEVOPS-02: `backend/mosquitto/config/mosquitto.conf:32` + `acl.conf` creada (topics `aethernet/#`), montada en `docker-compose.yml:32`.
  - [x] DEVOPS-03: `backend/app/models.py:34` fix `metadata` reservado -> `event_metadata`, `init.sql` alineado, `backend/pyproject.toml` con ruff ignore B008/S110/BLE001.
  - [x] DEVOPS-04: Pipeline `ci.yml:90` corregido (FQBN rover `arduino:avr:uno`, ArduinoJson pinned 6.21.3); compilación local OK: `mega-access` 19252 bytes/7%, `rover-uno` 6900 bytes/21% (arduino-cli 1.5.1); estructura sketch `firmware/*/*.ino` creada para CI.
  - [x] DEVOPS-05: Procedimiento RF documentado en `docs/testing-rf-sprint1.md` (SPI, round-trip, fail-safe 500ms, KPI <10ms).
  - Tests: `backend/tests/test_health.py:6` (6 tests, mock DB) + `stats/tests/test_ema_filter.py` pasan; ruff `All checks passed`.
- Deuda Sprint 1→2 saldada 2026-08-29 (8/8) — deltas 2026-08-31:
  - [x] PM-08: `docs/auditoria-secretos-sprint1.md:10` H-01/H-02 (FELIPE./2516f751 en `ed557b7/ddebd48`) — H-01 **Cerrado 2026-08-31** prod rotada + AP lab recreado
  - [x] DEVOPS-11/08: `gateway-esp32.ino:20-51` sin hardcode + `secrets.h.example` + `backend/.env.example` + `.gitignore:219` (`firmware/**/secrets.h`)
  - [x] DEVOPS-10: `ci.yml:14` 1.5.1, `ci.yml:99-107` fallback secrets, `ci.yml:241` android-build condicional, `.gitignore:72-73` (MOV-12); verificado ruff/pytest/mypy/arduino-cli verde
  - [x] PM-03: `risk-register.md:32` R-12 Resuelto / R-13 + R-01 pivot skill Alexa/Google Home
  - [x] MOV-01/MOV-12: `docs/cierre-mov01.md:4` ✅ Done `f03190b` 2026-09-01 — `AetherControlApp`+`ServiceLocator` DI manual, `Retrofit`+DTOs, `PreferencesManager` `updateBaseUrl`, `network_security_config` cleartext, `DashboardViewModel` `StateFlow`+`NavGraph`+`DashboardScreen` editor URL, `assembleDebug`+`testDebugUnitTest` verdes + `curl 192.168.1.14:8000/health` ok en SM-X620 — cierra plantilla-only
  - [x] PM-02: `docs/tablero-scrum.md:49` `https://github.com/users/Craos6518/projects/14` (Kanban 6 cols)
  - [x] PM-04: `docs/gantt.md:7` Mermaid Sprints 1-4 + deuda — actualizar LOW-01 a bloqueado 2026-08-31
  - [x] EST-01: `stats/ema_filter.py` + bench `a051dd4` 531 muestras (adelantado Sprint 1-2)
- Próximo (Sprint 2): `feature/firmware-mega-cerrojo` (RF-2.2/HU-01 teclado 4x4+MG90S), `feature/firmware-mega-laser` (RF-2.3/KY-008), LED RGB `hardware-inventory.md:9` (HU-01 verde / HU-02 rojo), `feature/app-pantallas-domotica` (MOV-02), `feature/app-mqtt-telemetria` (MOV-03), `feature/app-pin-cerrojo` (MOV-04), `feature/app-setup-mvvm` (**MOV-01 real**), `feature/backend-endpoints` (DEVOPS-06/07), `feature/automation-mqtt-sub` (LOW-02). LOW-01 pivot a skill si se confirma bloqueo.
