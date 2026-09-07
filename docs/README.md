# Docs — Índice de Documentación AetherNet

> **Proyecto Integrador** 5º semestre Ingeniería de Sistemas / Desarrollo de Software (UTP) — 100% FOSS. Ver `AGENTS.md` para orden de lectura de agentes.

## Mapa Rápido

| Doc | Qué responde | Estado 2026-09-07 |
|---|---|---|
| `prd.md` | Visión, alcance in/out, KPIs, restricciones técnicas | KPI `>85%` EMA, `FOSS` |
| `requirements.md` | RF/RNF + HU BDD `HU-01..HU-03` (`Dado/Cuando/Entonces`) | fuente de verdad tests |
| `hardware-inventory.md` | Matriz hardware por subsistema (MEGA 44/45/46 LED RGB, sin Tuya ADR-001) | LED local único visual |
| `architecture.md` | Cómo se comunican (RF UART MQTT, §4 EMA, §5 HU-02) — **con `notebooks/`** | + `notebooks/` en §4 |
| `roadmap.md` | Conocimientos por materia + `notebooks/EMA_Estadistica.ipynb` central | + `notebooks/` en §5 |
| `sprints.md` | Planeación Scrum + estado Sprint 2 (MOV-04 Pin cerrojo) | `MOV-01..04` Done |
| `backlog.md` | Backlog MoSCoW por materia (fuente de verdad qué falta) | `EST-01..08` etc. |
| `notebooks/README.md` | **Centralizado `notebooks/` (canónico)** + espejo `stats/notebooks/` | nuevo 2026-09-07 |
| `gantt.md` / `tablero-scrum.md` / `branching-strategy.md` | Gestión (Gantt, Kanban 14, ramas) | — |
| `risk-register.md` / `auditoria-secretos-sprint1.md` | Riesgos + secretos rotados `FELIPE.` | R-12 resuelto |
| `fritzing/` | Esquemas + `docs/fritzing/*.png` (ema-demo, alpha_sweep, rover etc.) | — |
| `adr/adr-001-cancelacion-tuya.md` | ADR Tuya cancelado | R-01 |

## Notebooks — Centralización 2026-09-07

- **Canónico:** `notebooks/` (raíz) — ver `notebooks/README.md`
  - `notebooks/EMA_Estadistica.ipynb` — TS4D3 Estadística (EMA, t-Student)
  - `notebooks/AetherControl_Notebook.ipynb` — Kotlin/App (MVVM, Compose)
- **Espejo:** `stats/notebooks/EMA_Estadistica.ipynb` + `stats/notebooks/README.md` — no editar
- **Redirección:** `docs/notebooks/README.md` — mapa + `cp notebooks/... stats/notebooks/...`

Abrir: `jupyter lab notebooks/` (ver `notebooks/README.md` §Cómo ejecutar).

## Orden de lectura (AGENTS.md)

1. `prd.md` → 2. `requirements.md` → 3. `hardware-inventory.md` → 4. `sprints.md` → 5. `roadmap.md` → 6. `backlog.md` → 7. `notebooks/README.md`

Si contradicción, señalar antes de implementar — no asumir prioridad.
