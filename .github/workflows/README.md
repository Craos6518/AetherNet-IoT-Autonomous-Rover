# CI/CD — `.github/workflows/ci.yml`

> **Autor:** Estudiante 6º semestre Tecnología en Desarrollo de Software + 6º semestre Ingeniería de Sistemas y Computación (UTP)  
> **Experiencia:** 2 años electrónica y Arduino | 1 año Programación C | 2 años Python | 2 años HTML/CSS/JavaScript/React | 1 año PostgreSQL  
> **Stack:** GitHub Actions (ubuntu-latest), `arduino-cli 1.5.1`, Python 3.12, Docker Compose, Trivy — 100% FOSS (RNF-3.1)  
> **Sprint:** 1 ✅ (RNF-1.2 pipeline con `arduino-cli` en cada `push`), 2+ 🔄 — `docs/sprints.md:61`  

---

## 0. Cómo leer este workflow si vienes de web / electrónica / Python

Si vienes de **React (2 años)**: este `ci.yml` es el `package.json` de `scripts` pero para CI — `on: push` es como `on: push` en tu `ci.yml` de React, `runs-on: ubuntu-latest` es el runner como `Vercel`, y cada `job` es un `npm run lint && npm test && npm run build`. La diferencia es que aquí el “build” compila `C++` con `arduino-cli` en vez de `Vite`.

Si vienes de **electrónica / C (1 año C, 2 años Arduino)**: el firmware no se flashea en CI, se **compila**. `arduino-cli compile --fqbn` con `esp32:esp32:esp32`, `arduino:avr:mega`, `arduino:avr:uno` es el `platformio.ini` pero en CLI. Si compila, el pinout y las `lib_deps` (`ArduinoJson 6.21.3` pinneado) están sanos. Si falla, no mergeas.

Si vienes de **Python / PostgreSQL (2 años Python, 1 año PG)**: `backend-test` y `stats-test` son `npm test` con `pytest` pero con `setup-python 3.12 cache pip`, `ruff` (como `eslint`), `mypy` (como `tsc`), y `pytest -v --tb=short` con `mock DB` (ver `backend/tests/test_health.py`). `docker-build` levanta `postgres:16-alpine` real y hace `curl POST /api/access-events` como harías `psql` + `curl` local.

Este README cubre **todo** lo que hay en `.github/workflows/ci.yml:1` job por job con `ruta:línea`.

---

## 1. Mapa del workflow

```
.github/workflows/ci.yml (343 líneas, 6 jobs, 1 env)
├── on: push ['**'] + pull_request [main,develop]          # dispara en cada commit y PR
├── permissions: contents read + security-events write      # checkout + SARIF Trivy
├── env: ARDUINO_CLI_VERSION 1.5.1                          # pin, no latest
└── jobs:
    ├── backend-test     (FastAPI ruff/mypy/pytest, mock DB)          # 20-79 — sin Docker
    ├── firmware-compile (matrix gateway-esp32 / mega-access / rover-uno) # 58-140 — arduino-cli
    ├── docker-build     (postgres → /health → POST/GET 4 endpoints)  # 117-212 — integración real
    │    └── needs [backend-test, firmware-compile]                   # solo si ambos verdes
    ├── stats-test       (pytest 14 tests EMA α=0.2 KPI>85%)          # 216-278
    ├── android-build    (JDK 17 + Gradle assembleDebug, skip si no hay app/) # 250-317
    └── security-scan    (Trivy fs → SARIF → Security tab)             # 324-342 — no bloquea
```

`android-build` y `security-scan` corren en paralelo con los demás (no `needs`), los otros 4 forman la cadena crítica `Sprint 1`.

---

## 2. Visión general — qué valida y por qué importa

| Job `ci.yml` | Qué valida | Analogía React | Si falla, qué rompe |
|---|---|---|---|
| `backend-test:20` | `backend/app/*.py` lint + types + 6 tests mock PG | `eslint + tsc + jest` | `HU-01..HU-03` (`POST /api/*` roto) |
| `firmware-compile:58` | `firmware/*` compila con `FQBN` + `lib_deps` pin `6.21.3` | `npm run build` + `tsc` | `RF-2.2` cerrojo, `RF-3.1` Rover RF |
| `docker-build:117` | `docker compose up` → `postgres healthy` → `GET /health` → `POST/GET` 4 endpoints | `docker compose up && npm run test:integration` | Stack real no levanta |
| `stats-test:216` | `stats/ema_filter.py` 14 tests, `KPI >85%` | `npm test` en `stats/` | `HU-03` EMA no filtra |
| `android-build:250` | `app:assembleDebug` Kotlin/Compose | `npm run build` Android | `RF-1.1` Dashboard |
| `security-scan:287` | `Trivy` vulnerabilidades `Docker/Python` | `npm audit` | CVEs |

**RNF-1.2** exige `arduino-cli` en cada `push` — este workflow lo cumple. **RNF-3.1** exige FOSS — todo es `ubuntu-latest` + `arduino-cli` + `Trivy` sin CircleCI propietario. Si un job falla, el PR no se puede mergear (branch protection).

---

## 3. Triggers, permisos y env

### `on:3`
```yaml
on:
  push: { branches: ['**'] }          # cualquier rama — como `on: push` en CI de React (cada commit validado)
  pull_request: { branches: [main, develop] } # PR a main/develop — como `on: pull_request` que protege main
```

### `permissions:9`
```yaml
permissions: { contents: read, security-events: write } # least privilege — read para checkout, write SARIF para Trivy Security tab
```

### `env:13`
```yaml
env: { ARDUINO_CLI_VERSION: 1.5.1 } # pin — como `NODE_VERSION: 18` en CI React (evita breaking con latest)
```

---

## 4. `backend-test:20` — FastAPI sin Docker (mock DB)

> **Analogía React:** `setup-python 3.12 cache pip` es `setup-node cache npm`, `ruff check` es `eslint`, `mypy` es `tsc --noEmit`, `pytest` es `jest`.

| Paso `ci.yml` | Qué hace | Archivo espejo local | React equiv. |
|---|---|---|---|
| `checkout@v4:27` | clona repo | — | `actions/checkout@v4` |
| `setup-python@v5 3.12 cache pip:30` | instala Python 3.12 + cache | local `python -m venv .venv` | `setup-node 18 cache npm` |
| `pip install -r requirements.txt:38` | `FastAPI 0.111 + SQLAlchemy + asyncpg + Pydantic` | `backend/requirements.txt:1` | `npm ci` |
| `ruff check app/:43` | lint `backend/pyproject.toml:1 ignore B008/S110/BLE001` | `ruff check app/` local | `eslint .` |
| `mypy app/:48` | type check `ignore_missing_imports` | `mypy app/` | `tsc --noEmit` |
| `pytest -v --tb=short:53` | 6 tests `tests/test_health.py:6` con `MagicMock` `fake_add` `uuid4` sin PG real | `pytest -v` | `npm test -- --watchAll=false` |

`defaults.run.working-directory: ./backend` evita repetir `cd backend` (como `working-directory: ./app` en React).

---

## 5. `firmware-compile:58` — Matrix 3 firmwares (arduino-cli)

> **Analogía React:** `strategy.matrix.firmware` es `matrix node:[16,18]` pero con `gateway-esp32/mega-access/rover-uno`.

```yaml
strategy.matrix.firmware: [gateway-esp32, mega-access, rover-uno]
```

| Paso | Filtro `if` | Qué hace | FQBN | Libs `platformio.ini` |
|---|---|---|---|---|
| `Install arduino-cli:71` | — | `curl install.sh | sh -s 1.5.1` + `echo $(pwd)/bin >> $GITHUB_PATH` + `arduino-cli version` | — | — |
| `Setup config:79` | — | `config init` + `core update-index --additional-urls https://espressif.github.io/.../package_esp32_index.json` | — | — |
| `Install ESP32 core:85` | `== gateway-esp32` | `core install esp32:esp32` | `esp32:esp32:esp32` | `PubSubClient`, `ArduinoJson@6.21.3` pin, `RF24` |
| `Install AVR core:92` | `!= gateway-esp32` | `core install arduino:avr` | `arduino:avr:mega` / `arduino:avr:uno` | `Keypad`, `Servo`, `RF24`, `NewPing`, `ArduinoJson@6.21.3`, `PubSubClient` |
| `Prepare secrets fallback:99` | `== gateway-esp32` | `if [ ! -f secrets.h ] && [ -f secrets.h.example ]; then cp secrets.h.example secrets.h; fi; cat secrets.h` | — | `firmware/gateway-esp32/secrets.h` gitignored, `secrets.h.example` plantilla `DEVOPS-11`; `gateway-esp32.ino:28 #if __has_include` fallback `AetherNet-LAN/changeme` |
| `Compile:108` | — | `if gateway → esp32:esp32:esp32 elif rover → arduino:avr:uno else mega; arduino-cli compile --fqbn $FQBN ./firmware/${matrix.firmware}` | (ver left) | `firmware/gateway-esp32.ino:1` 424 líneas, `mega-access.ino:1` 42 líneas, `rover-uno.ino:1` 384 líneas |

**Por qué `ArduinoJson@6.21.3` pin:** `7.x` rompe `StaticJsonDocument` → `JsonDocument` (CI falló una vez). Pin es como `package-lock.json` en React.

**Reproduce local:**
```bash
arduino-cli compile --fqbn esp32:esp32:esp32 ./firmware/gateway-esp32
arduino-cli compile --fqbn arduino:avr:mega ./firmware/mega-access
arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno
# o: pio run -d firmware/mega-access
```

---

## 6. `docker-build:117` — Stack real (postgres → /health → POST/GET)

> **Analogía React:** `docker compose up && curl --retry 30 http://localhost:3000/health` con `wait-for-it.sh`, pero para `postgres + mosquitto + fastapi`.

`needs: [backend-test, firmware-compile]` — solo si ambos verdes (como `needs: [lint, test]` en React).

| Paso `ci.yml` | Qué hace | Espejo local | React equiv. |
|---|---|---|---|
| `Build:125` | `docker compose build --no-cache` | `backend/Dockerfile:1 python:3.12-slim + gcc/libpq` | `docker build --no-cache` |
| `Start:129` | `docker compose up -d && ps` | `docker-compose.yml:1` `postgres:16-alpine + mosquitto:2.0 + fastapi:8000` | `docker compose up -d` |
| `Wait Postgres:134` | `for i 1..30; docker inspect aethernet-postgres Health.Status healthy? sleep 2; else logs; exit 1; done` | `docker-compose.yml:15 healthcheck pg_isready -U aethernet` | `wait-for-it.sh postgres:5432` |
| `Wait /health:147` | `for i 1..30; curl -sf http://localhost:8000/health | grep '"status":"ok"'? sleep 2; else logs; exit 1` | `backend/app/main.py:78 GET /health {status:"ok", database:"ok"}` | `curl --retry 30 http://localhost:3000/health` |
| `Integration POST/GET:161` | `set -e; curl POST 4 endpoints + grep; for ep in ... curl GET ?limit=5 | grep "\["; curl GET / | grep AetherNet` | `backend/app/routers/events.py:31` 4 `POST/GET` | `fetch POST/GET` JS |
| `Logs on failure:200` | `if: failure() docker compose logs --tail=100 postgres/mosquitto/fastapi` | `docker logs` | `if: failure() docker logs` |
| `Cleanup:208` | `if: always() docker compose down -v` | `docker compose down -v` limpia `postgres_data` | `if: always() docker compose down -v` |

**Detalle `Integration` (como `fetch` en JS):**
```bash
curl -sf -X POST http://localhost:8000/api/access-events -H "Content-Type: application/json" -d '{"user_id":"ci_user","pin_hash":"abc123","success":true}' | grep -q "ci_user" # HU-01 MEGA 1234# → Gateway 192.168.1.14 → HTTP POST → PostgreSQL
curl -sf -X POST .../api/sensor-events -d '{"sensor_id":"ci-hc-sr04","sensor_type":"ultrasonic","value":42.5,"filtered_value":41.2,"unit":"cm","metadata":{"alpha":0.2}}' # HU-03 HC-SR04 EMA stats/ema_filter.py:32
curl -sf -X POST .../api/security-events -d '{"event_type":"intrusion","severity":"high"}' # HU-02 KY-008 → Telegram
SESSION=$(python3 -c "import uuid; print(uuid.uuid4())"); curl -sf -X POST .../api/rover/telemetry -d "{\"session_id\":\"$SESSION\", \"left_motor_pwm\":120, ...}" # RF-3.3 nRF24L01 session_id
for ep in access-events sensor-events security-events rover/telemetry; do curl -sf ".../api/$ep?limit=5" | grep -q "\[" || exit 1; done # GET lista → JSON array como Array.isArray()
curl -sf http://localhost:8000/ | grep -q "AetherNet" # GET / root
```

**Reproduce local:**
```bash
docker compose build --no-cache && docker compose up -d
for i in $(seq 1 30); do curl -sf http://localhost:8000/health | grep -q '"status":"ok"' && break; sleep 2; done
curl -sf -X POST http://localhost:8000/api/access-events -H "Content-Type: application/json" -d '{"user_id":"ci_user","pin_hash":"abc123","success":true}' | jq
curl -sf "http://localhost:8000/api/access-events?limit=5" | jq
docker compose down -v
```

---

## 7. `stats-test:216` + `android-build:250` + `security-scan:287`

### `stats-test:249` — 2 años Python (TS4D3)
`working-directory: ./stats`, `setup-python 3.12 cache pip`, `if [ -f requirements.txt ]; then pip install -r requirements.txt; fi` (`pandas/scipy/matplotlib` ver `stats/requirements.txt:1`), `pip install pytest pytest-cov`, `if [ -f pyproject.toml ] || ls test_*.py; then pytest -v --tb=short; else skip; fi` (14 tests `stats/tests/test_ema_filter.py:1` `HU-03 KPI >85%`).

### `android-build:250` — 2 años React (Gradle como Vite), `if: exists` check
`Check:292 id: check` `if [ -f app/build.gradle.kts ] || [ -f gradlew ]; then echo exists=true >> $GITHUB_OUTPUT; else exists=false; fi` (MOV-12 skip si app vacía). `Set up JDK 17:302 if exists==true uses setup-java@v5 temurin 17` (AGP 8.7 necesita 17), `Setup Android SDK:309 setup-android@v3` (SDK 37), `Build:313 chmod +x gradlew && ./gradlew :app:assembleDebug --stacktrace` (como `npm run build` → `debug/app-debug.apk`).

### `security-scan:324` — 1 año PG + React (npm audit)
`Run Trivy:331 uses aquasecurity/trivy-action@master with scan-type fs scan-ref . format sarif output trivy-results.sarif` (como `npm audit` pero para `fs`), `Upload:339 uses codeql-action/upload-sarif@v3 continue-on-error true sarif_file trivy-results.sarif` (muestra en `Security` tab).

---

## 8. Validación local fiel a CI

```bash
# Valida YAML sintáctico (como `eslint` para YAML)
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml')); print('YAML OK')"
# Backend sin Docker (igual que CI backend-test)
cd backend && ruff check app/ && mypy app/ && pytest -v --tb=short
# Firmware (igual que CI matrix)
arduino-cli compile --fqbn arduino:avr:mega ./firmware/mega-access   # 19252 bytes
arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno      # 6900 bytes
cp firmware/gateway-esp32/secrets.h.example firmware/gateway-esp32/secrets.h # fallback DEVOPS-11
arduino-cli compile --fqbn esp32:esp32:esp32 ./firmware/gateway-esp32
# Stats (igual que CI stats-test)
cd stats && pip install -r requirements.txt && pytest -v --tb=short # 14 passed
# Docker integración (igual que CI docker-build)
docker compose build --no-cache && docker compose up -d && curl -sf http://localhost:8000/health | jq
# Android (igual que CI android-build)
./gradlew :app:assembleDebug --stacktrace # debug/app-debug.apk
./gradlew :app:testDebugUnitTest          # 34 tests PinValidator etc.
# Trivy (igual que CI security-scan)
trivy fs --format sarif --output trivy-results.sarif .
```

---

## 9. Troubleshooting — qué hacer si un job falla

| Job rojo | Causa típica (2 años debug) | Fix en 1 línea |
|---|---|---|
| `backend-test ruff` | `B008` `Depends` falso positivo | Ya está `ignore B008` en `backend/pyproject.toml:1`; si nuevo `B008`, añade a `ignore` |
| `backend-test mypy` | `ignore_missing_imports false` | Está `true` en `pyproject.toml:13`; si añades lib sin stubs, deja `true` |
| `backend-test pytest` | `tests` con `SELECT 1` sin mock | Ver `backend/tests/test_health.py:6` `MagicMock` `fake_add` `uuid4` |
| `firmware-compile` `gateway-esp32` | `secrets.h: No such file` | CI hace `cp secrets.h.example secrets.h` `ci.yml:102`; local igual |
| `firmware-compile` `ArduinoJson` | `7.x` rompe `StaticJsonDocument` | Pin `ArduinoJson@6.21.3` en `ci.yml:90/97` + `platformio.ini` |
| `firmware-compile` `esp32:esp32:esp32` not found | `core update-index` sin URL | `ci.yml:83` `--additional-urls https://espressif.github.io/.../package_esp32_index.json` |
| `docker-build Wait Postgres` | `aethernet-postgres` no healthy | `docker compose logs postgres`; verifica `.env` `POSTGRES_*` y `init.sql:4 uuid-ossp` |
| `docker-build Wait /health` | `fastapi` no `status ok` | `docker compose logs fastapi`; verifica `backend/app/main.py:78 SELECT 1` + `DATABASE_URL asyncpg://...@postgres:5432` (`host postgres` no `localhost` en Docker) |
| `docker-build Integration` `access-events` | `grep ci_user` falla | `curl -s http://localhost:8000/api/access-events?limit=5 | jq` ver si `POST` retornó `201` con `id` |
| `stats-test` | `No tests found` | `ci.yml:274` `if [ -f pyproject.toml ] || ls test_*.py` — crea `stats/tests/test_*.py` |
| `android-build` `assembleDebug` | `SDK not found` | CI hace `setup-android@v3`; local instala `cmdline-tools` y `sdkmanager --install platform 37 build-tools` |
| `security-scan` `upload-sarif` | `permission denied` | `permissions: security-events: write` ya en `ci.yml:11`; verifica `trivy-results.sarif` existe |

---

## 10. Referencias cruzadas (para evaluadores)

- `docs/requirements.md:40 RNF-1.2` pipeline con `arduino-cli` en cada `push` → `ci.yml:58 firmware-compile`
- `docs/requirements.md:22 RF-2.2` cerrojo `MEGA` + `docs/prd.md:30` In-Scope `Docker` → `ci.yml:117 docker-build`
- `docs/sprints.md:61` Sprint 1 cierre `docker-build` `POST/GET 4 endpoints` + `docs/sprints.md:12` `RF-2.1 → RNF-1.2` + `docs/sprints.md:53` estado `192.168.1.14:8000` (aquí `localhost:8000` en CI)
- `docs/architecture.md:31` `FastAPI+Mosquitto+PostgreSQL` + `docs/roadmap.md:86` conocimiento `PostgreSQL/FastAPI/Docker` para desplegar `backend/`
- `backend/app/main.py:78 GET /health` `SELECT 1` → `ci.yml:151 grep '"status":"ok"'` + `backend/app/routers/events.py:31` 4 `POST/GET` → `ci.yml:161` `curl`
- `firmware/gateway-esp32/gateway-esp32.ino:20 secrets.h` `__has_include` fallback → `ci.yml:99` `cp secrets.h.example` + `firmware/**/platformio.ini` `lib_deps` pin `6.21.3`
- `stats/tests/test_ema_filter.py:1` 14 tests `KPI >85%` → `ci.yml:275 pytest` + `app/build.gradle.kts:1` `assembleDebug` → `ci.yml:317` `gradlew`
- `AGENTS.md:4` RNF-3.1 `100% FOSS` → `ci.yml:250` sin `CircleCI` propietario + `docs/risk-register.md` `DEVOPS-11` secretos rotados

---

## 11. Glosario (si vienes de web)

| Término CI | Equiv. React | Qué es |
|---|---|---|
| `on: push ['**']` | `onPush` en `GitHub Actions` React | Dispara workflow en cada push a cualquier rama |
| `runs-on: ubuntu-latest` | `runs-on` runner | VM FOSS donde corre el job |
| `uses: actions/checkout@v4` | `checkout` | Clona repo en runner |
| `setup-python@v5 cache pip` | `setup-node cache npm` | Instala lenguaje + cache deps |
| `strategy.matrix.firmware` | `matrix node:[16,18]` | 3 jobs en paralelo, uno por firmware |
| `needs: [backend-test, firmware-compile]` | `needs: [lint, test]` | Espera a que ambos verdes |
| `docker compose up -d` | `docker compose up` | Levanta stack `postgres+mosquitto+fastapi` detach |
| `curl -sf ... | grep -q` | `fetch(...).then(r=>r.ok && r.json())` | Health/integration check |
| `if: failure() / always()` | `if: failure()` | Solo si falla / siempre (logs/cleanup) |
| `SARIF` `upload-sarif` | `SARIF` CodeQL | Sube vulnerabilidades a `Security` tab |

---

*Documentado como estudiante 6º semestre que compara `GitHub Actions` en React (`setup-node` vs `setup-python`, `npm ci` vs `pip install`, `eslint` vs `ruff`, `tsc` vs `mypy`, `jest` vs `pytest`) con `arduino-cli` (`FQBN` como `platformio.ini`, `lib_deps` pin `6.21.3`) y `PostgreSQL` (`pg_isready` + `SELECT 1` + `curl POST/GET` como `psql` + `fetch`), reproducible local con `docker compose up` + `arduino-cli compile` + `pytest` + `gradlew assembleDebug`, FOSS de extremo a extremo (`RNF-3.1`).*
