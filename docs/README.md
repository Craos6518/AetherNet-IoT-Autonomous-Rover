# Docs — Índice de Documentación AetherNet

> **Proyecto Integrador** 5º semestre Ingeniería de Sistemas / Desarrollo de Software (UTP) — 100% FOSS. Ver `AGENTS.md` para orden de lectura de agentes.

## Mapa Rápido

| Doc | Qué responde | Estado 2026-09-10 (`docs/revision-sprint2-completa`) |
|---|---|---|
| `prd.md` | Visión, alcance in/out, KPIs, restricciones técnicas | KPI `>85%` EMA, `FOSS`, LowCode Telegram directo (LOW-02 deuda) |
| `requirements.md` | RF/RNF + HU BDD `HU-01..04` (`Dado/Cuando/Entonces`) | HU-02 Telegram directo (Node-RED deuda 2026-09-09) |
| `hardware-inventory.md` | Matriz hardware por subsistema (MEGA 44/45/46 LED RGB + laser 7/8, sin Tuya ADR-001) | LED local + laser v2 `c7ce065` |
| `architecture.md` | Cómo se comunican (RF UART MQTT, §4 EMA, §5 HU-02, §7 topics) | Topics `aethernet/#` definidos, Node-RED deuda, laser v2 |
| `roadmap.md` | Conocimientos por materia + `docs/Estadistica/notebook/EMA_Estadistica.ipynb` + datasets 36.5k | LOW-02 deuda, EMA Welch externo |
| `sprints.md` | Planeación Scrum + estado Sprint 2 (revisión 2026-09-10) | Sprint 2 Done (cerrojo+laser+MOV-02..04+DEVOPS-06/07) |
| `backlog.md` | Backlog MoSCoW por materia (fuente de verdad qué falta) | DEVOPS-01..08 Done, FW-MEGA Done, EST datasets 36.5k |
| `notebooks/README.md` | **Redirección** → `docs/<Materia>/notebook/*.ipynb` canónico | 7 notebooks 36.5k espejo |
| `gantt.md` / `tablero-scrum.md` / `branching-strategy.md` | Gestión (Gantt, Kanban 14, ramas) | Gantt Sprint2 Done, tablero Sprint2 Done |
| `risk-register.md` / `auditoria-secretos-sprint1.md` | Riesgos + secretos rotados `FELIPE.` | R-01/R-07 cerrados, R-12 resuelto |
| `fritzing/` | Esquemas + `docs/fritzing/*.png` (ema-demo, alpha_sweep, rover, water_*) | + `water_us_vs_true.png`/`water_ir_by_angle.png` P5 laser v1 |
| `adr/adr-001-cancelacion-tuya.md` | ADR Tuya cancelado | R-01 + `docs/revision-sprint2.md` (esta revisión) |
| `Administracion-Proyectos/` | TS683: `administracion-proyectos.md` + `roadmap-*.md` + `backlog-*.md` + `notebook/Diario_Administracion.ipynb` + `Proyecto_AetherNet*.docx` + `calendario/*.ics` | WBS 40, Gantt, kanban 14 |
| `Estadistica/` | TS4D3: `estadistica.md` + `roadmap-*.md` + `backlog-*.md` + `notebook/EMA_Estadistica.ipynb` + `Datasets/` espejo 36.5k | Welch/ANOVA |
| `Programacion-Movil/` | TS6C3: `programacion-movil.md` + `roadmap-*.md` + `backlog-*.md` + `notebook/AetherControl_Notebook.ipynb` | MVVM Compose |
| `Firmware/` | `firmware.md` + `roadmap-firmware.md` + `notebook/Firmware_Notebook.ipynb` | MEGA+Gateway+Rover |
| `Hardware/` | `hardware.md` + `roadmap-hardware.md` + `notebook/Diario_Hardware.ipynb` | Inventario + pines |
| `DevOps/` | `devops.md` + `roadmap-*.md` + `backlog-*.md` + `notebook/Diario_DevOps.ipynb` | Docker/CI |
| `Automatizacion-LowCode/` | `roadmap-lowcode.md` + `backlog-lowcode.md` + `notebook/Diario_LowCode.ipynb` | Telegram |
| `materias/` | **Redirección** → `docs/<Materia>/` canónico (ver arriba) | Mapeo PDF UTP |
| `calendar/` | **Redirección** → `docs/Administracion-Proyectos/calendario/` | ICS/VCS/CSV |

## Notebooks + Datasets — Centralización 2026-09-11

- **Canónico:** `docs/<Materia>/notebook/` — ver `notebooks/README.md` redirect
  - `docs/Estadistica/notebook/EMA_Estadistica.ipynb` — TS4D3 Estadística (EMA, t-Student)
  - `docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` — Kotlin/App MVVM Compose
  - `docs/Firmware/notebook/Firmware_Notebook.ipynb` — MEGA+Gateway+Rover
  - `docs/Hardware/notebook/Diario_Hardware.ipynb` — Hardware
  - `docs/DevOps/notebook/Diario_DevOps.ipynb` — Docker/CI
  - `docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` — Scrum
  - `docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` — Telegram
- **Espejo:** `stats/notebooks/EMA_Estadistica.ipynb` (`cp docs/Estadistica/notebook/EMA_Estadistica.ipynb stats/notebooks/`) — no editar
- **Redirección:** `notebooks/README.md` y `docs/notebooks/README.md` → canónico arriba
- **Datasets externos (36.5k):** `stats/Dataset/` canónico (water 31.5k CC BY-SA 4.0 + gesture 5k CC0) + espejo `docs/Estadistica/Datasets/` — ver `stats/Dataset/README.md` y `docs/Estadistica/Datasets/README.md`; análisis `stats/water_turbidity_analysis.py` + `stats/data/water_turbidity_report.json` + `docs/Estadistica/notebook/EMA_Estadistica.ipynb` §7c
- **Administración:** `docs/Administracion-Proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` (99KB, 14 secciones) + `README.md` + `calendario/*.ics`

Abrir: `jupyter lab docs/Estadistica/notebook/` (ver `notebooks/README.md` §Cómo ejecutar).

## Orden de lectura (AGENTS.md)

1. `prd.md` → 2. `requirements.md` → 3. `hardware-inventory.md` → 4. `sprints.md` → 5. `roadmap.md` → 6. `backlog.md` → 7. `notebooks/README.md`

Si contradicción, señalar antes de implementar — no asumir prioridad.
