# Tablero Scrum — AetherNet IoT (PM-02)

Resuelve deuda Sprint 1→2 PM-02. Fuente: `docs/backlog.md:9-79` + `docs/sprints.md:9-48` + `docs/branching-strategy.md:27`.

> **Dónde vivir:** GitHub Projects (recomendado) → Project "AetherNet IoT — Sprints 1-4" vinculado a `docs/deuda-sprint1-sprint2.md`. Este MD es el **export estático** para sustentación/offline. Al crear el Project, importar estas columnas y vincular cada Issue a `feature/*`.

## Columnas (Kanban)

| Columna | Contenido (IDs backlog) | Sprint |
|---|---|---|
| **Backlog** | `MOV-05..10`, `LOW-03..05`, `EST-02..07`, `DEVOPS-09` | 3-4 |
| **Sprint 1 Done** | `DEVOPS-01..05`, `MOV-01` ⚠️ plantilla-only, `EST-01`, `PM-03/04` (cerrados `sprints.md:52`; `LOW-01` ⏳ bloqueado — ver R-01) | 1 |
| **Sprint 2 In Progress** | (vacía — todo movido a Done) | 2 |
| **Deuda Sprint 1→2 (esta entrega)** | `PM-08` ✅ `DEVOPS-11` ✅ `DEVOPS-10` ✅ `PM-03` ✅ `MOV-01` ⚠️ plantilla-only `MOV-12` ✅ | 1→2 |
| **Blocked** | (vacía tras esta deuda; antes: `MOV-12` por `.gitignore:69` y `DEVOPS-11` por hardcode) | — |
| **Done (Sprint 2)** | `FW-MEGA-CERROJO` ✅ `c7ce065` + `FW-MEGA-LASER-v2` ✅ laser KY-008 LDR 10k + `MOV-02` ✅ `MOV-03` ✅ `MOV-04` ✅ (35 tests) + `DEVOPS-06/07` ✅ 21 tests + `DEVOPS-08` ✅ + `EST datasets 36.5k` ✅ | 2 |

## Tarjetas — Deuda (ya saldada en esta rama `feature/firmware-mega-cerrojo`)

| Tarjeta | Issue | Estado | Rama | Evidencia |
|---|---|---|---|---|
| PM-08 Auditoría secretos | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `docs/auditoria-secretos-sprint1.md:10` H-01/H-02 |
| DEVOPS-11 Rotar secrets | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `gateway-esp32.ino:20-51` + `secrets.h.example` + `backend/.env.example` |
| DEVOPS-10 Pipeline verde | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `ci.yml:14,99-107,241` + `.gitignore:72-73` |
| PM-03 Matriz riesgos | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `risk-register.md:32` R-12/R-13 |
| MOV-01 Setup MVVM | MOV-01 | ⚠️ Plantilla-only 2026-08-31 (sincerado) | `feature/firmware-mega-cerrojo` + `feature/app-setup-mvvm` | `docs/cierre-mov01.md:4` — 0 líneas Kotlin propias, `MainActivity.kt:22`/`DashboardViewModel.kt:32` plantilla wizard; infra `app/gradlew tasks` OK |
| MOV-12 Build Android | MOV-12 | Done (infra, fusionado MOV-01) | `feature/firmware-mega-cerrojo` | `app/build.gradle.kts:4` KSP 1.9.22-1.0.17, `app/gradlew` 9.5.0 — build verde pero sin lógica |

## Tarjetas — Sprint 2 Done (mergeadas a `sprint/2-domotica-acceso` tip `ec5d70b`)

| Tarjeta | Estado | Rama | Evidencia |
|---|---|---|---|
| `MOV-02: Pantallas domótica (RF-1.1)` | ✅ Done 2026-09-07 | `feature/app-pantallas-domotica` | `LedState.kt` + `LedStateMapper` 5s/10s/1s `LedStatusCard` `assembleDebug` verde |
| `MOV-03: Cliente MQTT (RF-1.1)` | ✅ Done 2026-09-07 | `feature/app-mqtt-telemetria` | `MqttManager` Paho 1.2.5 `aethernet/#` `mosquitto_sub` <50ms 21 tests |
| `MOV-04: PIN cerrojo (HU-01)` | ✅ Done 2026-09-07 | `feature/app-pin-cerrojo` | `PinViewModel` throttle `aethernet/access/command:70` → `ACCESS:hash` 35 tests |
| `DEVOPS-06/07: Endpoints + PyTest (RF-2.1)` | ✅ Done 2026-09-07 | `feature/backend-endpoints` | `routers/events.py:25` 8 ops `test_events.py` 14 + `test_health.py` 7 =21 verdes |
| `DEVOPS-08: .env.example` | ✅ Done | `feature/firmware-mega-cerrojo` | `backend/.env.example` + `secrets.h.example` + `.gitignore:219` |
| `LOW-02: Node-RED MQTT sub (RF-4.1)` | ⛔ DEUDA 2026-09-09 | — | No implementado (asesor); `automation/flows/intrusion_alert.json` referencia |
| `FW-MEGA-CERROJO: Teclado 4x4+MG90S (RF-2.2 HU-01)` | ✅ Done 2026-08-26 | `feature/firmware-mega-cerrojo` | `config.h` `door.cpp` `keypad_control.cpp` `led.cpp` `uart_protocol.cpp` |
| `FW-MEGA-LASER-v2: KY-008 (RF-2.3 HU-02)` | ✅ Done 2026-09-07 `c7ce065` | `feature/firmware-mega-laser-v2` | `laser.{h,cpp}` `7/8` `INPUT` 10k `test-laser-uno` |
| `EST datasets 36.5k` | ✅ Done 2026-09-09 `ec5d70b/144fda2` | `sprint/2-domotica-acceso` | `water_turbidity 31.5k` + `gesture 5k` `water_turbidity_analysis.py` + report JSON |

## Tarjetas — Sprint 2 revisión (esta rama `docs/revision-sprint2-completa`)

| Tarjeta | Estado | Rama | Evidencia |
|---|---|---|---|
| `PM-05: sprints.md` | 🔄 Actualizado 2026-09-10 | `docs/revision-sprint2-completa` | laser v2 + datasets 36.5k + admin doc |
| `PM-03/04: gantt + tablero` | 🔄 Sincronizado 2026-09-10 | `docs/revision-sprint2-completa` | Gantt Sprint2 Done, tablero Done Sprint2 |
| `DOCS: architecture/prd/requirements` | 🔄 Sincronizado 2026-09-10 | `docs/revision-sprint2-completa` | Node-RED deuda, topics canónicos, HU-02 Telegram directo |

## Cómo crear el Project en GitHub

1. `GitHub → Projects → New project → Board` con las 6 columnas arriba.
2. `Add item → Create issue` por cada tarjeta Sprint 2; asignar label `Sprint 2`, `area:movil/devops/lowcode`.
3. Conectar repo: `Project Settings → Manage access → Link repository`.
4. Automatización: `Workflow → Auto-add to project (on issue opened)` + `Status update on PR merged → Done`.
5. Exportar link y pegarlo en `docs/sprints.md:52` "Estado actual" y aquí abajo.

**Link del Project:** `https://github.com/users/Craos6518/projects/14` (creado 2026-08-31 — reemplaza placeholder `projects/1`)

## Relación con docs

- `docs/sprints.md:52` refleja "Sprint 2 En curso, fase cierre documental" (2026-09-10) — actualizado con rama revisión.
- `docs/revision-sprint2.md` es la bitácora de esta revisión (trazabilidad RF→archivo).
- `docs/deuda-sprint1-sprint2.md` es el checkpoint vivo previo; este tablero es la vista Kanban del mismo backlog.
