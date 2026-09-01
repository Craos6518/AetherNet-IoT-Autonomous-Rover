# Tablero Scrum — AetherNet IoT (PM-02)

Resuelve deuda Sprint 1→2 PM-02. Fuente: `docs/backlog.md:9-79` + `docs/sprints.md:9-48` + `docs/branching-strategy.md:27`.

> **Dónde vivir:** GitHub Projects (recomendado) → Project "AetherNet IoT — Sprints 1-4" vinculado a `docs/deuda-sprint1-sprint2.md`. Este MD es el **export estático** para sustentación/offline. Al crear el Project, importar estas columnas y vincular cada Issue a `feature/*`.

## Columnas (Kanban)

| Columna | Contenido (IDs backlog) | Sprint |
|---|---|---|
| **Backlog** | `MOV-05..10`, `LOW-03..05`, `EST-02..07`, `DEVOPS-09` | 3-4 |
| **Sprint 1 Done** | `DEVOPS-01..05`, `MOV-01` ✅, `LOW-01`, `EST-01`, `PM-03/04` (cerrados `sprints.md:52`) | 1 |
| **Sprint 2 In Progress** | `feature/firmware-mega-cerrojo` (HU-01 RF-2.2), `feature/firmware-mega-laser`, `MOV-02..04`, `DEVOPS-06..08`, `LOW-02` | 2 |
| **Deuda Sprint 1→2 (esta entrega)** | `PM-08` ✅ `DEVOPS-11` ✅ `DEVOPS-10` ✅ `PM-03` ✅ `MOV-01` ✅ `MOV-12` ✅ | 1→2 |
| **Blocked** | (vacía tras esta deuda; antes: `MOV-12` por `.gitignore:69` y `DEVOPS-11` por hardcode) | — |
| **Done (Sprint 2)** | Se mueve aquí al mergear `sprint/2-domotica-acceso → develop` con CI verde | 2 |

## Tarjetas — Deuda (ya saldada en esta rama `feature/firmware-mega-cerrojo`)

| Tarjeta | Issue | Estado | Rama | Evidencia |
|---|---|---|---|---|
| PM-08 Auditoría secretos | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `docs/auditoria-secretos-sprint1.md:10` H-01/H-02 |
| DEVOPS-11 Rotar secrets | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `gateway-esp32.ino:20-51` + `secrets.h.example` + `backend/.env.example` |
| DEVOPS-10 Pipeline verde | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `ci.yml:14,99-107,241` + `.gitignore:72-73` |
| PM-03 Matriz riesgos | — | Done 2026-08-29 | `feature/firmware-mega-cerrojo` | `risk-register.md:32` R-12/R-13 |
| MOV-01 Setup MVVM | MOV-01 | Done 2026-08-29 | `feature/firmware-mega-cerrojo` + `feature/app-setup-mvvm` | `docs/cierre-mov01.md`, `app/gradlew tasks` OK |
| MOV-12 Build Android | MOV-12 | Done (fusionado MOV-01) | `feature/firmware-mega-cerrojo` | `app/build.gradle.kts:4` KSP 1.9.22-1.0.17, `app/gradlew` 9.5.0 |

## Tarjetas — Sprint 2 activo (para crear Issues)

Cada tarjeta: `ID: título (RF/HU) #feature/rama`.

- `MOV-02: Pantallas domótica (RF-1.1)` → `feature/app-pantallas-domotica`
- `MOV-03: Cliente MQTT (RF-1.1)` → `feature/app-mqtt-telemetria`
- `MOV-04: PIN cerrojo (HU-01)` → `feature/app-pin-cerrojo`
- `DEVOPS-06/07: Endpoints + PyTest (RF-2.1)` → `feature/backend-endpoints`
- `LOW-02: Node-RED MQTT sub (RF-4.1)` → `feature/automation-mqtt-sub`
- `FW-MEGA-CERROJO: Teclado 4x4+MG90S (RF-2.2 HU-01)` → `feature/firmware-mega-cerrojo` (esta rama)
- `FW-MEGA-LASER: KY-008 (RF-2.3 HU-02)` → `feature/firmware-mega-laser`

## Cómo crear el Project en GitHub

1. `GitHub → Projects → New project → Board` con las 6 columnas arriba.
2. `Add item → Create issue` por cada tarjeta Sprint 2; asignar label `Sprint 2`, `area:movil/devops/lowcode`.
3. Conectar repo: `Project Settings → Manage access → Link repository`.
4. Automatización: `Workflow → Auto-add to project (on issue opened)` + `Status update on PR merged → Done`.
5. Exportar link y pegarlo en `docs/sprints.md:52` "Estado actual" y aquí abajo.

**Link del Project (rellenar al crearlo):** `https://github.com/Craos6518/AetherNet-IoT-Autonomous-Rover/projects/1` (placeholder)

## Relación con docs

- `docs/sprints.md:52` refleja "Sprint 2 En curso" — actualizar al cerrar deuda con link a este tablero.
- `docs/deuda-sprint1-sprint2.md` es el checkpoint vivo; este tablero es la vista Kanban del mismo backlog.
