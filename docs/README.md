# Docs — Índice de Documentación AetherNet

> **Proyecto Integrador** 5º semestre Ingeniería de Sistemas / Desarrollo de Software (UTP) — 100% FOSS. Ver `AGENTS.md` para orden de lectura de agentes.

## Mapa Rápido

| Doc | Qué responde | Estado 2026-09-10 (`docs/revision-sprint2-completa`) |
|---|---|---|
| `prd.md` | Visión, alcance in/out, KPIs, restricciones técnicas | KPI `>85%` EMA, `FOSS`, LowCode Telegram directo (LOW-02 deuda) |
| `requirements.md` | RF/RNF + HU BDD `HU-01..04` (`Dado/Cuando/Entonces`) | HU-02 Telegram directo (Node-RED deuda 2026-09-09) |
| `hardware-inventory.md` | Matriz hardware por subsistema (MEGA 44/45/46 LED RGB + laser 7/8, sin Tuya ADR-001) | LED local + laser v2 `c7ce065` |
| `architecture.md` | Cómo se comunican (RF UART MQTT, §4 EMA, §5 HU-02, §7 topics) | Topics `aethernet/#` definidos, Node-RED deuda, laser v2 |
| `roadmap.md` | Conocimientos por materia + `notebooks/EMA_Estadistica.ipynb` + datasets 36.5k | LOW-02 deuda, EMA Welch externo |
| `sprints.md` | Planeación Scrum + estado Sprint 2 (revisión 2026-09-10) | Sprint 2 Done (cerrojo+laser+MOV-02..04+DEVOPS-06/07) |
| `backlog.md` | Backlog MoSCoW por materia (fuente de verdad qué falta) | DEVOPS-01..08 Done, FW-MEGA Done, EST datasets 36.5k |
| `notebooks/README.md` | **Centralizado `notebooks/` (canónico)** + espejo `stats/notebooks/` + `docs/Estadistica/Datasets/` | 36.5k filas espejo |
| `gantt.md` / `tablero-scrum.md` / `branching-strategy.md` | Gestión (Gantt, Kanban 14, ramas) | Gantt Sprint2 Done, tablero Sprint2 Done |
| `risk-register.md` / `auditoria-secretos-sprint1.md` | Riesgos + secretos rotados `FELIPE.` | R-01/R-07 cerrados, R-12 resuelto |
| `fritzing/` | Esquemas + `docs/fritzing/*.png` (ema-demo, alpha_sweep, rover, water_*) | + `water_us_vs_true.png`/`water_ir_by_angle.png` P5 laser v1 |
| `adr/adr-001-cancelacion-tuya.md` | ADR Tuya cancelado | R-01 + `docs/revision-sprint2.md` (esta revisión) |

## Notebooks + Datasets — Centralización 2026-09-07, actualizado 2026-09-10

- **Canónico:** `notebooks/` (raíz) — ver `notebooks/README.md`
  - `notebooks/EMA_Estadistica.ipynb` — TS4D3 Estadística (EMA, t-Student)
  - `notebooks/AetherControl_Notebook.ipynb` — Kotlin/App (MVVM, Compose)
- **Espejo:** `stats/notebooks/EMA_Estadistica.ipynb` + `stats/notebooks/README.md` — no editar
- **Redirección:** `docs/notebooks/README.md` — mapa + `cp notebooks/... stats/notebooks/...`
- **Datasets externos (36.5k):** `stats/Dataset/` canónico (water 31.5k CC BY-SA 4.0 + gesture 5k CC0) + espejo `docs/Estadistica/Datasets/` — ver `stats/Dataset/README.md` y `docs/Estadistica/Datasets/README.md`; análisis `stats/water_turbidity_analysis.py` + `stats/data/water_turbidity_report.json` + `notebooks/EMA_Estadistica.ipynb` §7c
- **Administración:** `docs/Administracion de proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` (99KB, 14 secciones) + `README.md`

Abrir: `jupyter lab notebooks/` (ver `notebooks/README.md` §Cómo ejecutar).

## Orden de lectura (AGENTS.md)

1. `prd.md` → 2. `requirements.md` → 3. `hardware-inventory.md` → 4. `sprints.md` → 5. `roadmap.md` → 6. `backlog.md` → 7. `notebooks/README.md`

Si contradicción, señalar antes de implementar — no asumir prioridad.
