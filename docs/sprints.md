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

- Montaje mecánico del chasis oruga con motorreductores **TT 6V 1:48 ×4** (antes 9-12V, **calce 2026-09-11 por espacio** ver `hardware-inventory.md:36` — torque 3.5→0.8 KG·cm -77%, vel 350→120 RPM) y L298N.
- Algoritmo anti-caída (3x TCRT5000) y evasión de obstáculos (HC-SR04). → RF-3.2 — HW existe, bug `digitalRead(A0)` vs `analogRead` pendiente calibrar `IR_THRESHOLD 500`.
- Joystick virtual en Jetpack Compose enviando comandos de baja latencia. → RF-1.2 — ✅ Done `feature/app-joystick-virtual` 2026-09-11 (ver Estado actual)

**Depende de:** Sprint 1 (enlace RF ESP32 ↔ UNO probado).

---

## Sprint 4 (Semanas 7-8): LowCode, Filtrado Estadístico y Cierre

- Filtro de Kalman / EMA en Python para procesar datos de sensores (HC-SR04, KY-037). → RNF-2.1, HU-03
- ~~Flujo Node-RED suscripción MQTT~~ — **DEUDA TÉCNICA / CANCELADO 2026-09-09** (LOW-02 asesor: no se usará Node-RED) + Bot de Telegram directo para notificaciones de seguridad. → RF-4.1, HU-02
- ~~Integración del bombillo Tuya vía `tuya-local`~~ — **CANCELADO 2026-09-01** (ADR-001, R-01 políticas API propietaria). Intrusión solo vía LED RGB local + Telegram. → RF-4.2 cancelado, HU-02 simplificada
- Pruebas de integración End-to-End, documentación y pruebas unitarias.

**Depende de:** Sprint 2 (evento de intrusión ya disparándose desde el MEGA) y Sprint 3 (telemetría del Rover ya fluyendo).

---

## Estado actual

- Sprint activo: **Sprint 3 (Semanas 5-6) — En curso (Sprint 2 ✅ CERRADO documental 2026-09-10, Sprint 1 ✅ 2026-09-01) — MOV-05/06 ✅ Done**
- Rama activa: **`feature/app-joystick-virtual@3ca12c9`** — Joystick nRF24 validado HW 2026-09-11 + calce TT 6V 1:48 + `notebooks/Firmware_Notebook.ipynb` + `docs/logs/firmware_sprint3/live_2026-09-11/`; base `docs/revision-sprint2-completa@ec5d70b`
- Última actualización: **2026-09-11 — Sprint 3 MOV-05/06 ✅ + calce Rover + auditoría sensores**
  - **2026-09-11 09:43 — MOV-05/06 RF-1.2 ✅ Done `feature/app-joystick-virtual@3ca12c9` (11h 23:00 + 07:00-09:29):** `JoystickScreen Canvas 120dp` `JoystickMapper deadband 60` `ViewModel throttle 50ms` `MqttManager QoS0 aethernet/rover/command:69` → `gateway-esp32 CE5 CSN15 Ch76 2MBPS` `RF TX 250→249 99.6%` → `rover-uno CE4 CSN10 ENA5/ENB11 6900b` `RF RX 232` + `FAIL-SAFE 500ms HU-04` + `aethernet/rover/telemetry rf_rssi -70` — validado `mosquitto_sub 563 líneas` `T3 33K` `T4 7.3K` `T2_mega 1.5K` `docker health ok` — `assembleDebug` + `JoystickMapperTest 10` + `ViewModelTest 6` verdes. **Calce Rover 2026-09-11:** chasis TT 6V 1:48 ×4 por espacio (antes 9-12V 3.5 KG·cm) -77% torque `hardware-inventory.md:36` + `rover-uno.ino:6,89` `MIN_PWM 60` TT. **Notebook:** `notebooks/Firmware_Notebook.ipynb` 16 celdas + `docs/logs/firmware_sprint3/` 20 logs. **Pin 16↔16 17↔17** invertido documentado (mal diseño MEGA/ESP32 pero operativo). **Sensores:** `KY-008` Sprint 2 Done, `nRF24` Sprint 1 Done, `LED RGB` Sprint 2 Done, `TCRT5000` **sin banco dedicado** (bug `digitalRead` vs `analogRead`, `IR_THRESHOLD 500` sin calibrar) + `HC-SR04` `ultrasonic_cm:0` en logs → pendiente Sprint 4 `EST-02`, `KY-037` pendiente `EST-03` — ver auditoría 2026-09-11.
  - **2026-09-10 — Revisión Sprint 2 completa**
  - **2026-09-10 (esta revisión):** rama `docs/revision-sprint2-completa` creada desde `sprint/2-domotica-acceso` tip `ec5d70b`; auditoría doc↔código identifica desalineación `architecture.md` (Node-RED deuda), `gantt.md`/`backlog.md` y `prd.md` (alcance LowCode); se sincronizan 9 docs + se crea `docs/revision-sprint2.md` (bitácora + trazabilidad RF→archivo)
  - **2026-09-09:** `ec5d70b` docs(admin,estadística): `Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` (99KB, 14 secciones, WBS 40 paquetes, costos COP 176k/1.176M) + `docs/Administracion de proyectos/README.md` + `docs/Estadistica/CONTRASTE_Admin_vs_Estadistica.md` + bench 36.5k filas (`water_turbidity 31.5k` + `gesture 5k`) + `stats/water_turbidity_analysis.py:1` 364 líneas + `stats/data/water_turbidity_report.json` + PNGs `water_us_vs_true.png`/`water_ir_by_angle.png`
  - **2026-09-07 23:41:** `c7ce065` merge `feature/firmware-mega-laser-v2` → `sprint/2-domotica-acceso` (RF-2.3 HU-02): `firmware/mega-access/src/laser.{h,cpp}` barrera KY-008 no bloqueante + `LDR discreta 10k INPUT` (`config.h:8/7` sin PULLUP) + `firmware/test-laser-uno/test-laser-uno.ino` banco aislado + `laserInit/handleLaser/triggerIntrusionAlert` + `LED rojo 3s` no bloqueante + `SECURITY:` UART 38400 → Gateway → `POST /api/security-events` + `aethernet/seguridad/intrusion:72`
  - **2026-09-07 — DEVOPS-06/07 ✅ Done `feature/backend-endpoints` (estilo estudiante): `backend/app/config.py:58` fix `extra=ignore` para POSTGRES_USER, `backend/tests/test_events.py` 14 tests (POST/GET 4 recursos + filtros + 422 + openapi 8 ops) + `backend/tests/test_health.py` +1 degraded, total 21 verdes, `ruff`/`mypy` OK, `openapi.json` 8 ops verificadas — Previo MOV-04 ✅ Done `feature/app-pin-cerrojo` Plan B (HU-01 RF-2.2 S): `PinViewModel` throttle 5/60s `PinScreen` 4x3 dots `aethernet/access/command:70`→`handleAccessCommand:332`→`CMD:ACCESS:40`→`VALID_PIN 1234:48`→`ACCESS:hash 7c78c98f` POST 201+`aethernet/access/event:71` verificado mosquitto pub/sub + curl limit 3 `assembleDebug`/`testDebugUnitTest` 35 verdes — Previo MOV-03 ✅ Done `feature/app-mqtt-telemetria` (RF-1.1 <50ms `prd.md:50`): `MqttManager` Paho 1.2.5 tcp://host:1883 + ws://9001 fallback `mosquitto.conf:4/8` `acl.conf:14` aethernet/#, `RoverTelemetryMqtt` `AccessEventMqtt` `MqttConnectionState`, `SharedFlow` `aethernet/rover/telemetry:69` `access/event:71` `seguridad/intrusion:72` `system/status:73` gateway, `ServiceLocator mqttManager` deriva broker de `getCurrentBaseUrl()` `ServiceLocator.kt:114`, `AetherRepository` Flows `mqttConnectionState` `roverTelemetryFlow`, `DashboardViewModel` colecta push `lastRover` + `lastSync` y fallback poll 5s `MOV-02` si `Disconnected`, UI `DashboardScreen` `MQTT ●/○/✕` + `Card Rover L/R US` — verificado `assembleDebug` `testDebugUnitTest` 10+11 verdes + `mosquitto_sub -h 192.168.1.14 -t aethernet/#` push <50ms. Previo MOV-02 ✅ Done `feature/app-pantallas-domotica` (RF-1.1 HU-01/HU-02 solo lectura): `LedState.kt` + `LedStateMapper` 5s/10s/1s espejo `led.cpp:64`/`config.h:52` + `AetherRepository.getLedState()` paralelo 1+1 + `DashboardViewModel` poll 5s cancelable + `LedStatusCard` 64dp previews — verificado `assembleDebug`/`testDebugUnitTest` verdes + grep tuya solo CANCELADO (ADR-001). Sprint 1 ✅ CERRADO 2026-09-01 — DEVOPS-05 RF nRF24L01 validado HW `docs/testing-rf-sprint1.md:32` 4/4 ✅ — `ESP32 nRF24L01 initialized` `ttyUSB0 WiFi FELIPE. 192.168.1.23 MQTT connected` + `UNO nRF24L01 initialized` `ttyACM0` + `mosquitto_pub 192.168.1.14` → `RF TX: L=120 R=120 mode=1` `gateway-esp32.ino:261` → `RF RX: L=120 R=120 mode=1` `rover-uno.ino:202` + `FAIL-SAFE 500ms` `docs/sprints.md:13` (fix `CSN 18→15` `gateway-esp32.ino:61` + `C1/C2 10µF en paralelo` + `SSID FELIPE.` punto) — plano `docs/fritzing/plano-sprint1-nrf24-reapertura.md` validado. Anterior hito MOV-01 real ✅ Done `f03190b` `feature/app-setup-mvvm` (RF-1.1, RNF-3.1) — verificado `192.168.1.14:8000/health` en SM-X620 + `assembleDebug`/`testDebugUnitTest` verdes** (ver `docs/cierre-mov01.md:4`, `docs/deuda-sprint1-sprint2.md:36`); `develop` al día con `a051dd4` EMA bench 531 + `f1ffcaa` stats + `f03190b` MOV-01

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
- Deuda Sprint 1→2 saldada 2026-08-29 (8/8) — deltas 2026-08-31:
  - [x] PM-08: `docs/auditoria-secretos-sprint1.md:10` H-01/H-02 (FELIPE./2516f751 en `ed557b7/ddebd48`) — H-01 **Cerrado 2026-08-31** prod rotada + AP lab recreado
  - [x] DEVOPS-11/08: `gateway-esp32.ino:20-51` sin hardcode + `secrets.h.example` + `backend/.env.example` + `.gitignore:219` (`firmware/**/secrets.h`)
  - [x] DEVOPS-10: `ci.yml:14` 1.5.1, `ci.yml:99-107` fallback secrets, `ci.yml:241` android-build condicional, `.gitignore:72-73` (MOV-12); verificado ruff/pytest/mypy/arduino-cli verde
  - [x] PM-03: `risk-register.md:32` R-12 Resuelto / R-13 + R-01 pivot skill Alexa/Google Home
  - [x] MOV-01/MOV-12: `docs/cierre-mov01.md:4` ✅ Done `f03190b` 2026-09-01 — `AetherControlApp`+`ServiceLocator` DI manual, `Retrofit`+DTOs, `PreferencesManager` `updateBaseUrl`, `network_security_config` cleartext, `DashboardViewModel` `StateFlow`+`NavGraph`+`DashboardScreen` editor URL, `assembleDebug`+`testDebugUnitTest` verdes + `curl 192.168.1.14:8000/health` ok en SM-X620 — cierra plantilla-only
  - [x] PM-02: `docs/tablero-scrum.md:49` `https://github.com/users/Craos6518/projects/14` (Kanban 6 cols)
  - [x] PM-04: `docs/gantt.md:7` Mermaid Sprints 1-4 + deuda — actualizar LOW-01 a bloqueado 2026-08-31
  - [x] EST-01: `stats/ema_filter.py` + bench `a051dd4` 531 muestras (adelantado Sprint 1-2)
- Próximo (Sprint 2 cierre): `feature/firmware-mega-cerrojo` ✅ Done (RF-2.2 HU-01), `feature/firmware-mega-laser-v2` ✅ Done (RF-2.3 HU-02 `c7ce065`), LED RGB `hardware-inventory.md:9` (HU-01 verde 5s / HU-02 rojo 3s), `feature/app-pantallas-domotica` ✅ Done, `feature/app-mqtt-telemetria` ✅ Done, `feature/app-pin-cerrojo` ✅ Done, `feature/backend-endpoints` ✅ Done (DEVOPS-06/07 21 tests). **Pendiente Sprint 2:** merge `sprint/2-domotica-acceso` → `develop` + `sprints.md`/`backlog.md`→ `docs/archivo/`. ~~`feature/automation-mqtt-sub` (LOW-02)~~ → **DEUDA TÉCNICA / CANCELADO 2026-09-09** asesor (no Node-RED) — solo Telegram directo `automation/flows/intrusion_alert.json` como referencia, no deploy. `EST-01..06` datasets 36.5k adelantados — `EST-02/03/07` quedan Sprint 4.
