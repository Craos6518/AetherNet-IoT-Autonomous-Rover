# Administración de Proyectos — AetherNet

Carpeta canónica para la asignatura **TS683 Administración y Planeación de Proyectos de Software** (UTP) — ver `docs/Administracion-Proyectos/administracion-proyectos.md`, `docs/Administracion-Proyectos/roadmap-administracion.md`.

## Archivos (canónicos)

| Archivo | Descripción |
|---|---|
| `Plantilla Proyecto.docx` | Plantilla institucional original (14 secciones) — no editar, es la fuente base. |
| `Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` | **Documento implementado** — plantilla diligenciada con datos reales de AetherNet. Trabajo individual: **Andres Felipe Martinez Henao** (09/09/2026) — 99 KB, 12 tablas, 671 párrafos. |
| `README.md` | Este índice. |
| `calendario/` | 6 archivos `*.ics`/`*.vcs`/`*.csv`/`README.md` — ver `calendario/README.md`. |
| `capturas/` | `tablero-kanban.png`, `gantt.png`, `risk-register.png` — checklist fotos |
| `presentacion-administracion-2026-08-26.html` | Slides administración | — |

## Documento implementado — 14 secciones

1. **Título:** AetherNet IoT & Autonomous Rover — Plataforma distribuida 100% FOSS.
2. **Introducción y justificación:** problema nube propietaria, vacío integración app↔firmware↔backend, justificación FOSS/LAN/EMA.
3. **Objetivos:** 1 general + 5 específicos (OE-01..05) con RF/HU/KPI y sprint.
4. **Alcance:** In-Scope (app, backend Docker, Gateway/MEGA/Rover, EMA, Telegram) vs Out-of-Scope (Tuya cancelado ADR-001, iOS, visión, AWS/GCP).
5. **Metodología:** Scrum 4 sprints/8 sem adaptado a solo + Kanban Projects 14 + CI arduino-cli + Gantt Mermaid.
6. **WBS:** 7 fases / ~40 paquetes (Gestión, Infra, App, Firmware Edge, LowCode, Estadística, Cierre).
7. **Cronograma:** tabla 8 filas + Gantt mermaid (`docs/gantt.md`) — responsable único.
8. **Riesgos:** 13 riesgos R-01..R-13 (4 cerrados/resueltos, R-02/R-03 críticos) — `docs/risk-register.md`.
9. **Métricas:** 5 KPIs PRD (<50ms, <10ms, >85%, 0%, 100% FOSS) + métricas proceso (21+35 tests verdes, ruff/mypy).
10. **Costos:** ~$176 hardware desembolsable / $1 176 con mano de obra académica (FOSS = $0 licencias).
11. **Avance:** Corte 1 (Sprint1+deuda 1→2 ✅ 8/8) y Corte 2 (Sprint2 en curso 09/09/2026, MOV-02/03/04 + DEVOPS-06/07 ✅).
12. **Entregables:** 12 ítems (app, backend, 3 firmwares, notebooks, Telegram, manuales, gestión, CI).
13. **Evaluación interna (solo):** dificultades solo/context-switch, lecciones (infra primero, EST-01 adelantado), mejoras (buffer, mDNS).
14. **Anexos:** Figura arquitectura 3 capas, Tabla hardware/pines, Topics MQTT `aethernet/#`, fragmentos código (`ema_filter.py:15`, `rover-uno.ino:259`, `led.cpp:64`), evidencias mosquitto/curl/arduino-cli, encuesta validación.

Portada institucional UTP, índice paginado, tablas/figuras numeradas, numeración de páginas y redacción formal según plantilla §14.

## Cómo regenerar

```bash
pip install python-docx --break-system-packages
python3 /tmp/generate_docx.py  # lee Plantilla Proyecto.docx y genera el implementado
```

Fuente de verdad cruzada: `docs/prd.md`, `requirements.md`, `hardware-inventory.md`, `sprints.md`, `backlog.md`, `architecture.md`, `gantt.md`, `risk-register.md`, `roadmap.md`, `docs/Estadistica/notebook/EMA_Estadistica.ipynb`.

## Trabajo individual

Modalidad **solo** — responsable único de todo el WBS y cronograma: **Andres Felipe Martinez Henao**.

## Diario de campo — Capturas checklist

> Ver `docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` para bitácora.

- [ ] **Tablero Kanban** — `capturas/tablero-kanban.png` — screenshot `https://github.com/users/Craos6518/projects/14` 6 columnas
- [ ] **Gantt Mermaid** — `capturas/gantt.png` — render `docs/gantt.md` Mermaid 4 sprints + deuda
- [ ] **Riesgos R-01..R-13** — `capturas/risk-register.png` — tabla `risk-register.md`
- [ ] **Reunión Sprint** — `fotos/reunion-sprint.jpg` — foto retrospectiva (opcional)

Si falta: `![CAPTURA PENDIENTE](capturas/tablero-kanban.png)` en `Diario_Administracion.ipynb`.
