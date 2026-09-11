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

## Documentación Académica por Materia — Índice Detallado (centralizado desde `docs/README.md`)

| Materia | Código | Carpeta canónica | Documento académico | Roadmap | Backlog | Notebook |
|---|---|---|---|---|---|---|
| Estadística | TS4D3 | `docs/Estadistica/` | [estadistica.md](Estadistica/estadistica.md) | [roadmap-estadistica.md](Estadistica/roadmap-estadistica.md) | [backlog-estadistica.md](Estadistica/backlog-estadistica.md) | [notebook/EMA_Estadistica.ipynb](Estadistica/notebook/EMA_Estadistica.ipynb) |
| DevOps | Electiva | `docs/DevOps/` | [devops.md](DevOps/devops.md) | [roadmap-devops.md](DevOps/roadmap-devops.md) | [backlog-devops.md](DevOps/backlog-devops.md) | [notebook/Diario_DevOps.ipynb](DevOps/notebook/Diario_DevOps.ipynb) |
| Administración y Planeación | TS683 | `docs/Administracion-Proyectos/` | [administracion-proyectos.md](Administracion-Proyectos/administracion-proyectos.md) | [roadmap-administracion.md](Administracion-Proyectos/roadmap-administracion.md) | [backlog-administracion.md](Administracion-Proyectos/backlog-administracion.md) | [notebook/Diario_Administracion.ipynb](Administracion-Proyectos/notebook/Diario_Administracion.ipynb) |
| Programación Móvil | TS6C3 | `docs/Programacion-Movil/` | [programacion-movil.md](Programacion-Movil/programacion-movil.md) | [roadmap-movil.md](Programacion-Movil/roadmap-movil.md) | [backlog-movil.md](Programacion-Movil/backlog-movil.md) | [notebook/AetherControl_Notebook.ipynb](Programacion-Movil/notebook/AetherControl_Notebook.ipynb) |
| Automatizaciones LowCode | — | `docs/Automatizacion-LowCode/` | — | [roadmap-lowcode.md](Automatizacion-LowCode/roadmap-lowcode.md) | [backlog-lowcode.md](Automatizacion-LowCode/backlog-lowcode.md) | [notebook/Diario_LowCode.ipynb](Automatizacion-LowCode/notebook/Diario_LowCode.ipynb) |
| Firmware | — | `docs/Firmware/` | [firmware.md](Firmware/firmware.md) | [roadmap-firmware.md](Firmware/roadmap-firmware.md) | — | [notebook/Firmware_Notebook.ipynb](Firmware/notebook/Firmware_Notebook.ipynb) |
| Hardware | — | `docs/Hardware/` | [hardware.md](Hardware/hardware.md) | [roadmap-hardware.md](Hardware/roadmap-hardware.md) | — | [notebook/Diario_Hardware.ipynb](Hardware/notebook/Diario_Hardware.ipynb) |

## Notebooks + Datasets — Centralización 2026-09-11

| Notebook | Materia | Canónico | Ejecutar |
|---|---|---|---|
| `AetherControl_Notebook.ipynb` | Programación Móvil TS6C3 | `docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` | `jupyter notebook docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` |
| `EMA_Estadistica.ipynb` | Estadística TS4D3 | `docs/Estadistica/notebook/EMA_Estadistica.ipynb` | `jupyter notebook docs/Estadistica/notebook/EMA_Estadistica.ipynb` |
| `Firmware_Notebook.ipynb` | Firmware | `docs/Firmware/notebook/Firmware_Notebook.ipynb` | `jupyter notebook docs/Firmware/notebook/Firmware_Notebook.ipynb` |
| `Diario_Hardware.ipynb` | Hardware | `docs/Hardware/notebook/Diario_Hardware.ipynb` | `jupyter notebook docs/Hardware/notebook/Diario_Hardware.ipynb` |
| `Diario_DevOps.ipynb` | DevOps | `docs/DevOps/notebook/Diario_DevOps.ipynb` | `jupyter notebook docs/DevOps/notebook/Diario_DevOps.ipynb` |
| `Diario_Administracion.ipynb` | Administración TS683 | `docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` | `jupyter notebook docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` |
| `Diario_LowCode.ipynb` | LowCode | `docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` | `jupyter notebook docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` |

- **Canónico:** `docs/<Materia>/notebook/` — 7 notebooks
- **Espejo:** `stats/notebooks/EMA_Estadistica.ipynb` (`cp docs/Estadistica/notebook/EMA_Estadistica.ipynb stats/notebooks/`) — no editar
- **Datasets externos (36.5k):** `stats/Dataset/` canónico (water 31.5k CC BY-SA 4.0 + gesture 5k CC0) + espejo `docs/Estadistica/Datasets/` — ver `stats/Dataset/README.md` y `docs/Estadistica/Datasets/README.md`; análisis `stats/water_turbidity_analysis.py` + `stats/data/water_turbidity_report.json` + `docs/Estadistica/notebook/EMA_Estadistica.ipynb` §7c
- **Administración:** `docs/Administracion-Proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` (99KB, 14 secciones) + `README.md` + `calendario/*.ics`

Abrir: `jupyter lab docs/Estadistica/notebook/`.

## Orden de lectura (AGENTS.md)

1. `prd.md` → 2. `requirements.md` → 3. `hardware-inventory.md` → 4. `sprints.md` → 5. `roadmap.md` → 6. `backlog.md` → 7. `docs/README.md` § Notebooks

Si contradicción, señalar antes de implementar — no asumir prioridad.
