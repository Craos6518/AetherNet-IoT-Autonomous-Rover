# Auditoría Centralización — Mapa Origen→Destino

Generado Task 1 — 2026-09-11. Fuente: `find docs -type f | sort` (118 archivos) + `find docs -name *.md | sort` (65) + `grep -rn` refs.

## Inventario

- `docs/Administracion de proyectos/` (con espacio, 3 archivos: Plantilla 111K + Proyecto 100K + README 3.2K) **duplica** `docs/Administracion-Proyectos/` (hyphen, 1 README + capturas/)
- `calendar/` raíz (6 archivos: 4 .ics + 1 .vcs + 1 .csv + README 7K) — debe vivir bajo Administración
- `docs/Estadistica/` (keep): `Anteproyecto_AetherNet-3.docx` 82K + `Contraste_Admin...docx` 14K + `CONTRASTE...md` 15K + `Datasets/` espejo 4 CSV 3.7MB + `README.md` 1.1K + `fotos/` vacía — ya canónico
- `docs/presentaciones/` (4 .html 37-73K + 1 guion .md 7.7K) — opcional mover a Administración
- Transversales keep: `docs/fritzing/` (21 archivos), `docs/logs/firmware_sprint3/` (15 logs), `docs/UTP/` (4 PDFs), `docs/adr/`, `docs/archivo/`, `docs/emails-docentes/`
- Espejos verificados: `stats/Dataset/` 3.7MB canónico ↔ `docs/Estadistica/Datasets/` espejo (diff sin diff esperado); `notebooks/` 7 ipynb canónico ↔ `stats/docs/Estadistica/notebook/EMA_Estadistica.ipynb` espejo solo EMA

## Mapa Origen→Destino

| Origen (actual) | Destino canónico | Acción | Refs que rompe si no se actualiza | Count refs |
|---|---|---|---|---|
| `docs/Administracion de proyectos/Plantilla Proyecto.docx` | `docs/Administracion-Proyectos/Plantilla Proyecto.docx` | `git mv` | `docs/Administracion de proyectos/README.md`, `docs/sprints.md:59`, `docs/README.md:30`, `calendar/README.md:4`, `docs/Estadistica/CONTRASTE...md:151` | ~38 hits incluye plan self |
| `docs/Administracion de proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` | `docs/Administracion-Proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` | `git mv` | same | same |
| `docs/Administracion de proyectos/README.md` | `docs/Administracion-Proyectos/README.md` (merge) | `git mv` + merge 14 secciones + capturas | `docs/README.md:30`, `docs/revision-sprint2.md:35` | 2 |
| `docs/Administracion de proyectos/` (carpeta) | **ELIMINAR** | `rmdir` / `git rm -r` | — | — |
| `calendar/AetherNet_8semanas_Trabajo_Sprints.ics` | `docs/Administracion-Proyectos/calendario/AetherNet_8semanas_Trabajo_Sprints.ics` | `git mv` | `calendar/README.md:67` internal, `docs/Administracion-Proyectos/README.md` (futuro) | ~40 hits calendar/ |
| `calendar/AetherNet_8semanas_Trabajo_Sprints.vcs` | `.../calendario/` | `git mv` | — | — |
| `calendar/AetherNet_Clases_UTP_2026-2.ics` | `.../calendario/` | `git mv` | — | — |
| `calendar/AetherNet_Completo_8semanas.ics` | `.../calendario/` | `git mv` | — | — |
| `calendar/AetherNet_Google_Calendar.csv` | `.../calendario/` | `git mv` | — | — |
| `calendar/README.md` | `.../calendario/README.md` | `git mv` | `docs/gantt.md`, `docs/tablero-scrum.md`, `docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` | 3 |
| `calendar/` (raíz tras vaciar) | **REDIRECCIÓN** `calendar/README.md` | create | — | — |
| `docs/Estadistica/Anteproyecto_AetherNet-3.docx` | KEEP (ya canónico) | — | — | — |
| `docs/Estadistica/Contraste_Admin_vs_Estadistica.docx` | KEEP | — | — | — |
| `docs/Estadistica/CONTRASTE_Admin_vs_Estadistica.md` | KEEP (actualizar refs espacio→hyphen) | `edit` | `docs/Administracion de proyectos/Proyecto_AetherNet...docx` | 2 |
| `docs/Estadistica/Datasets/` | KEEP espejo de `stats/Dataset/` | `diff -r` verify | `docs/roadmap.md:93`, `docs/architecture.md:56` | — |
| `stats/Dataset/` | KEEP canónico | — | — | — |
| `docs/presentaciones/*.html` (4) + `guion-estadistica-*.md` | OPCIÓN A: KEEP / OPCIÓN B: `docs/Administracion-Proyectos/presentaciones/` | `git mv` si B | `docs/README.md:19` | ~3 |
| `docs/fritzing/*` | KEEP | — | `notebooks/*.ipynb`, `docs/hardware-inventory.md` | ~20 |
| `docs/logs/firmware_sprint3/` | KEEP | — | `docs/Firmware/notebook/Firmware_Notebook.ipynb`, `Diario_DevOps.ipynb` | ~5 |
| `docs/UTP/*.pdf` | KEEP | — | `docs/materias/*.md` | ~6 |
| `docs/adr/` `docs/archivo/` `docs/emails-docentes/` | KEEP | — | `docs/backlog.md`, `docs/roadmap.md` | — |

## Refs detectadas (resumen)

- `refs_espacio.txt`: 38 líneas (incluye self plan + 5 reales: `docs/README.md:30`, `revision-sprint2.md:35`, `sprints.md:59`, `Estadistica/CONTRASTE...md:4+151`, `calendar/README.md:4+86`)
- `refs_calendar.txt`: 40 líneas ( mayormente self plan + `calendar/README.md:67+73` internal)
- `refs_estadistica.txt`: 56 líneas ( espejos correctos, no huérfanos)
