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
- Primeras pantallas en Jetpack Compose (Kotlin): estado LED RGB local (bombillo Tuya cancelado ADR-001). → RF-1.1

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
- ~~Integración del bombillo Tuya vía `tuya-local`~~ — **CANCELADO 2026-09-01** (ADR-001, R-01 políticas API propietaria). Intrusión solo vía LED RGB local + Telegram. → RF-4.2 cancelado, HU-02 simplificada
- Pruebas de integración End-to-End, documentación y pruebas unitarias.

**Depende de:** Sprint 2 (evento de intrusión ya disparándose desde el MEGA) y Sprint 3 (telemetría del Rover ya fluyendo).

---

## Estado actual

- Sprint activo: **Sprint 2 (Semanas 3-4) — En curso (Sprint 1 ✅ CERRADO 2026-09-01)**
- Última actualización: **2026-09-01 — Sprint 1 ✅ CERRADO — DEVOPS-05 RF nRF24L01 validado HW `docs/testing-rf-sprint1.md:32` 4/4 ✅ — `ESP32 nRF24L01 initialized` `ttyUSB0 WiFi FELIPE. 192.168.1.23 MQTT connected` + `UNO nRF24L01 initialized` `ttyACM0` + `mosquitto_pub 192.168.1.14` → `RF TX: L=120 R=120 mode=1` `gateway-esp32.ino:261` → `RF RX: L=120 R=120 mode=1` `rover-uno.ino:202` + `FAIL-SAFE 500ms` `docs/sprints.md:13` (fix `CSN 18→15` `gateway-esp32.ino:61` + `C1/C2 10µF en paralelo` + `SSID FELIPE.` punto) — plano `docs/fritzing/plano-sprint1-nrf24-reapertura.md` validado. Anterior hito MOV-01 real ✅ Done `f03190b` `feature/app-setup-mvvm` (RF-1.1, RNF-3.1) — verificado `192.168.1.14:8000/health` en SM-X620 + `assembleDebug`/`testDebugUnitTest` verdes** (ver `docs/cierre-mov01.md:4`, `docs/deuda-sprint1-sprint2.md:36`); `develop` al día con `a051dd4` EMA bench 531 + `f1ffcaa` stats + `f03190b` MOV-01
  - **MOV-01 real (2026-09-01):** `f03190b` implementa MVVM base real — `AetherControlApp.kt`, `ServiceLocator` DI manual, `Retrofit`+`kotlinx.serialization`, DTOs espejo `schemas.py`, `PreferencesManager` `DataStore` con `updateBaseUrl()` dinámico, `network_security_config` cleartext, `DashboardViewModel` `StateFlow` + `NavGraph` + `DashboardScreen` con editor `Backend URL`, `MainActivity` refactor + tests `turbine`. Verificado `docker 0.0.0.0:8000` + `curl 192.168.1.14:8000/health 200 ok` + App `ok` (antes `CLEARTEXT/SocketTimeout` fix). Cierra deuda `ACT-05` plantilla-only.
  - **Sinceramiento MOV-01 (2026-08-31):** dueño confirma 0 líneas Kotlin propias — `app/app/src/main/java/.../MainActivity.kt:22` y `DashboardViewModel.kt:32` son plantilla wizard `Empty Activity + Compose` con `TODO MQTT` sin implementación. Infra verde (`app/gradlew` 9.5.0 `tasks` OK, KSP 1.9.22-1.0.17, `ci.yml:241` condicional) pero **MOV-01 pasa a ⚠️ plantilla-only** — ver `docs/cierre-mov01.md:4`. Lógica MVVM real queda para `feature/app-setup-mvvm` en Sprint 2.
  - **LOW-01 cancelación (2026-09-01):** `tuya-local`/bombillo **CANCELADO** definitivamente por políticas de integración (API) propietaria (viola RNF-3.1, requiere cuenta Tuya Cloud) + `local_key` inaccesible (R-01, ADR-001). No se reemplaza por skill Alexa/Google (decisión del equipo). RF-4.2 → Won't; HU-02 solo LED RGB + Telegram.
  - **Rotación cerrada (2026-08-31):** producción rotada (password prod cambiado); AP lab `FELIPE./2516f751` recreado idéntico para compatibilidad local — `R-12` Resuelto (ver `docs/auditoria-secretos-sprint1.md:15` §5 + `docs/risk-register.md:35`).
  - **PM-02 completado (2026-08-31):** `https://github.com/users/Craos6518/projects/14` — ver `docs/tablero-scrum.md:49`.
- Avances Sprint 1 verificados 2026-08-26 (base) + deltas 2026-08-31:
  - [x] DEVOPS-01/06: `docker-compose.yml` sin `version` obsoleta, `backend/app/main.py:16` + `schemas.py` + `routers/events.py:8` operativos; `GET /health` responde `{"status":"ok","database":"ok"}`; `POST /api/access-events` (HU-01), `/sensor-events` (HU-03), `/security-events` (HU-02), `/rover/telemetry` (RF-3.3) validados con curl + `docker compose up` (FastAPI 1.0.0-sprint1, Postgres 16, Mosquitto 2.0). `paho-mqtt==2.1.0` corregido en `backend/requirements.txt:8`.
  - [x] DEVOPS-02: `backend/mosquitto/config/mosquitto.conf:32` + `acl.conf` creada (topics `aethernet/#`), montada en `docker-compose.yml:32`.
  - [x] DEVOPS-03: `backend/app/models.py:34` fix `metadata` reservado -> `event_metadata`, `init.sql` alineado, `backend/pyproject.toml` con ruff ignore B008/S110/BLE001.
  - [x] DEVOPS-04: Pipeline `ci.yml:90` corregido (FQBN rover `arduino:avr:uno`, ArduinoJson pinned 6.21.3); compilación local OK: `mega-access` 19252 bytes/7%, `rover-uno` 6900 bytes/21% (arduino-cli 1.5.1); estructura sketch `firmware/*/*.ino` creada para CI.
  - [x] DEVOPS-05: ✅ VALIDADO HW 2026-09-01 — `docs/testing-rf-sprint1.md:32` 4/4: `nRF24L01 initialized` ambos (`ESP32 CSN15` `ttyUSB0` + `UNO` `ttyACM0`), `RF TX/RX L=120` `mosquitto_pub 192.168.1.14` + `isChipConnected=1` `test-nrf`, `FAIL-SAFE 500ms` `rover-uno.ino:220`.
  - Tests: `backend/tests/test_health.py:6` (6 tests, mock DB) + `stats/tests/test_ema_filter.py` pasan; ruff `All checks passed`.
- Próximo: cerrar Sprint 1 con `docker compose up` + `curl /health` en CI, y arrancar Sprint 2 (cerrojo MEGA + LED RGB + pantallas Compose).
