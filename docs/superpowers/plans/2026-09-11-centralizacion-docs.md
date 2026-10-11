# Centralización de Documentación Dispersa — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Centralizar todos los `.md`/`docx`/`html`/`ics` dispersos en ubicaciones únicas y canónicas bajo `docs/<Materia-kebab>/` + `notebooks/` + `stats/Dataset/` y actualizar cada `href`/`Path()` que apunte a la ubicación antigua, sin duplicados ni carpetas con espacio.

**Architecture:** Tres fases secuenciales: (1) auditoría + definición de canónico (tabla origen→destino), (2) `git mv` físico + `README.md` redirecciones donde se exige retrocompatibilidad, (3) `grep`+`edit` masivo de referencias cruzadas + verificación de que `notebooks/*.ipynb` siguen abriendo y `pytest`/`arrow` no rompe. Cada materia crece como `docs/<Materia>/` (FIRMWARE, HARDWARE ya creados) y `docs/materias/` queda como capa académica (mapeo+roadmap+backlog).

**Tech Stack:** Git `mv`, Markdown, Jupyter (`nbformat 4`), `rg`/`grep`, Python `pathlib`, GitHub Actions `arduino-cli 1.5.1` (no tocar).

**Spec:** Mensaje usuario 2026-09-11: "Tenemos muchos archivos, muchos md regados... vas a centralizarlas en el lugar correspondiente, y actualizarás los archivos que apunten a ellos" + estado real `find docs -name *.md | sort` (64 archivos, `docs/Administracion de proyectos/` con espacio duplicado, `docs/Estadistica/` vs `stats/Dataset/`, `calendar/` en raíz, `docs/presentaciones/`, `docs/fritzing/`, `docs/logs/`, `docs/UTP/`, `docs/archivo/`, `docs/emails-docentes/`, `docs/adr/`, espejos `docs/Estadistica/Datasets/` + `stats/notebooks/` + `docs/notebooks/README.md`).

## Global Constraints

- 100% FOSS — no introducir dependencias de nube propietaria (RNF-3.1).
- Pines y umbrales (LED `44/45/46`, KY-008 `8/7`, EMA `α=0.2`, `VALID_PIN 1234`) no se tocan; solo se mueven docs.
- Cada `git mv` debe preservar historia (`git log --follow`).
- Nombres de carpeta **kebab-case sin espacios ni acentos**: `Administracion-Proyectos`, `Programacion-Movil`, `Automatizacion-LowCode` — ya adoptado en Tasks 1..10; corregir el único con espacio `Administracion de proyectos` → hyphen.
- `notebooks/` es canónico; `stats/notebooks/EMA_Estadistica.ipynb` es espejo **solo de EMA** (`cp`), `docs/notebooks/README.md` es redirección.
- `stats/Dataset/` es canónico 36.5k (3.7 MB); `docs/Estadistica/Datasets/` es espejo académico para entrega — no duplicar contenido, solo sincronizar `README.md`.
- No romper `notebooks/*.ipynb` (validar `json.load` + `Path.exists()` en cada task).
- Actualizar **todo** puntero textual: `docs/README.md`, `notebooks/README.md`, `docs/notebooks/README.md`, `docs/materias/README.md`, `docs/roadmap.md`, `docs/architecture.md`, `docs/sprints.md`, `docs/revision-sprint2.md`, `docs/Estadistica/*.md`, `docs/Administracion de proyectos/README.md`, `calendar/README.md`, `notebooks/*.ipynb` celdas `Path(...)`.

---

## File Structure — canónico objetivo

```
docs/                              # índice + docs transversales (no por materia)
├── README.md                      # MOD: tabla con paths canónicos nuevos
├── prd.md requirements.md architecture.md hardware-inventory.md
├── sprints.md backlog.md roadmap.md gantt.md risk-register.md
├── tablero-scrum.md branching-strategy.md test-plan.md
├── testing-rf-sprint1.md auditoria-secretos-sprint1.md revision-sprint2.md
├── UTP/                           # KEEP: 4 PDFs (2344 TS4D3, 2350 TS683, 2361 TS6C3, DEVOPS-EST.pdf)
├── adr/adr-001-cancelacion-tuya.md
├── archivo/                       # KEEP: cierre-mov01.md deuda-sprint1-sprint2.md (histórico)
├── emails-docentes/               # KEEP (o mover a Administracion-Proyectos/emails/ — decision Task 2)
├── fritzing/                      # KEEP: *.png *.fzz *.svg + compendio-planos.md etc.
├── logs/firmware_sprint3/         # KEEP
├── notebooks/                     # KEEP como redirección (1 archivo README.md)
├── presentaciones/                # KEEP (html guiones) — o mover a Administracion-Proyectos/presentaciones/
├── materias/                      # CAPA ACADÉMICA (mapeo+roadmap+backlog por asignatura)
│   ├── README.md
│   ├── estadistica.md / programacion-movil.md / devops.md / administracion-proyectos.md
│   ├── firmware.md / hardware.md
│   ├── roadmap-*.md (7) + backlog-*.md (5)
│   └── (no docx, no html, no csv aquí)
│
├── Administracion-Proyectos/      # CANÓNICO Administración (antes "Administracion de proyectos" con espacio)
│   ├── README.md                  # MOD: índice + refs a docx
│   ├── Plantilla Proyecto.docx    # MOVE desde "Administracion de proyectos/Plantilla Proyecto.docx"
│   ├── Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx # MOVE desde con espacio
│   ├── calendario/                # MOVE desde calendar/ (5 archivos .ics/.vcs/.csv + README.md)
│   │   ├── AetherNet_8semanas_Trabajo_Sprints.ics
│   │   ├── AetherNet_Completo_8semanas.ics
│   │   ├── AetherNet_Clases_UTP_2026-2.ics
│   │   ├── AetherNet_8semanas_Trabajo_Sprints.vcs
│   │   ├── AetherNet_Google_Calendar.csv
│   │   └── README.md
│   ├── presentaciones/            # MOVE (opcional) desde docs/presentaciones/*.html — o dejar y referenciar
│   └── capturas/                  # KEEP (tablero-kanban.png, gantt.png)
│
├── Estadistica/                   # CANÓNICO Estadística entrega
│   ├── README.md                  # KEEP (checklist fotos/gráficos)
│   ├── Anteproyecto_AetherNet-3.docx  # KEEP (ya aquí)
│   ├── Contraste_Admin_vs_Estadistica.docx # KEEP
│   ├── CONTRASTE_Admin_vs_Estadistica.md   # KEEP
│   ├── Datasets/README.md         # KEEP (espejo)
│   ├── Datasets/*.csv             # KEEP (espejo 4 csv 3.7 MB — ya sincronizado con stats/Dataset/)
│   └── fotos/                     # KEEP
│
├── Firmware/                      # CANÓNICO Firmware (ya creado Task 1)
├── Hardware/                      # CANÓNICO Hardware
├── Programacion-Movil/            # CANÓNICO Móvil
├── DevOps/                        # CANÓNICO DevOps
├── Automatizacion-LowCode/        # CANÓNICO LowCode
└── superpowers/plans/

calendar/                           # ELIMINAR tras mover — dejar README redirección o borrar (git rm)
docs/Administracion de proyectos/   # ELIMINAR tras mover (carpeta con espacio) — git rm -r
docs/presentaciones/                # OPCIONAL: vaciar si se mueve a Administracion-Proyectos/presentaciones/

notebooks/ (raíz)                   # CANÓNICO 7 ipynb + README.md
stats/
├── Dataset/                       # CANÓNICO 36.5k
├── notebooks/EMA_Estadistica.ipynb # ESPEJO (cp)
└── data/                          # derivados
```

---

### Task 1: Auditoría y mapa origen→destino (sin mover aún)

**Files:**
- Create: `docs/superpowers/plans/centralizacion-auditoria.md` (tabla)
- Modify: none (solo lectura)

**Interfaces:**
- Consumes: `find docs -type f | sort`, `grep -r "Administracion de proyectos\|docs/Estadistica\|calendar/\|presentaciones\|UTP" docs notebooks stats`
- Produces: tabla `origen → destino → refs encontradas` que Tasks 2..5 usan como contrato.

- [ ] **Step 1: Generar inventario completo**

```bash
find docs -type f -name "*.md" | sort > /tmp/docs_md.txt && wc -l /tmp/docs_md.txt && cat /tmp/docs_md.txt
find docs -type f | sort > /tmp/docs_all.txt && wc -l /tmp/docs_all.txt
ls -R docs "calendar" 2>&1 | head -n 120
grep -rn "Administracion de proyectos" --include="*.md" docs notebooks calendar stats 2>&1 | tee /tmp/refs_espacio.txt
grep -rn "docs/Estadistica" --include="*.md" docs notebooks stats 2>&1 | tee /tmp/refs_estadistica.txt
grep -rn "calendar/" --include="*.md" --include="*.ipynb" . 2>&1 | tee /tmp/refs_calendar.txt
grep -rn "presentaciones" --include="*.md" docs 2>&1 | tee /tmp/refs_presentaciones.txt
```

- [ ] **Step 2: Escribir tabla origen→destino en `docs/superpowers/plans/centralizacion-auditoria.md`**

```markdown
| Origen (actual) | Destino canónico | Acción | Refs que rompe si no se actualiza |
| docs/Administracion de proyectos/Plantilla Proyecto.docx | docs/Administracion-Proyectos/Plantilla Proyecto.docx | git mv | docs/Administracion de proyectos/README.md, docs/sprints.md:60, docs/README.md:30 |
| docs/Administracion de proyectos/Proyecto_AetherNet*.docx | docs/Administracion-Proyectos/Proyecto_AetherNet*.docx | git mv | docs/Administracion de proyectos/README.md, docs/revision-sprint2.md, docs/sprints.md |
| docs/Administracion de proyectos/README.md | docs/Administracion-Proyectos/README.md (merge) | git mv + merge | docs/README.md:30, docs/revision-sprint2.md |
| calendar/AetherNet_*.ics + .vcs + .csv + README.md | docs/Administracion-Proyectos/calendario/ | git mv 5 files | calendar/README.md, docs/sprints.md:60, docs/gantt.md, docs/Administracion de proyectos/README.md |
| docs/Estadistica/Anteproyecto_AetherNet-3.docx | KEEP (ya canónico) | — | — |
| docs/Estadistica/CONTRASTE_*.md + .docx | KEEP | — | docs/Estadistica/Datasets/README.md |
| docs/Estadistica/Datasets/ (espejo) | KEEP espejo de stats/Dataset/ | verificar diff | docs/roadmap.md:93, docs/architecture.md:55 |
| stats/Dataset/ | KEEP canónico | — | docs/Estadistica/Datasets/README.md |
| docs/presentaciones/*.html | OPCIÓN A: KEEP / OPCIÓN B: docs/Administracion-Proyectos/presentaciones/ | decidir Task 4 | docs/README.md, docs/presentaciones/guion-*.md |
| docs/fritzing/* | KEEP | — | notebooks/*.ipynb, docs/hardware-inventory.md |
| docs/logs/* | KEEP | — | notebooks/Firmware_Notebook.ipynb:15, Diario_DevOps.ipynb |
| docs/UTP/*.pdf | KEEP | — | docs/materias/*.md |
```

- [ ] **Step 3: Commit auditoría**

```bash
git add docs/superpowers/plans/centralizacion-auditoria.md
git commit -m "docs: auditoría centralización — mapa origen→destino sin mover aún (Task 1)"
```

---

### Task 2: Mover Administración con espacio → hyphen `Administracion-Proyectos`

**Files:**
- Move: `docs/Administracion de proyectos/Plantilla Proyecto.docx` → `docs/Administracion-Proyectos/Plantilla Proyecto.docx`
- Move: `docs/Administracion de proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx` → `docs/Administracion-Proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx`
- Move: `docs/Administracion de proyectos/README.md` → merge into `docs/Administracion-Proyectos/README.md`
- Delete: `docs/Administracion de proyectos/` (empty)
- Modify: every file in `/tmp/refs_espacio.txt`

**Interfaces:**
- Consumes: auditoría Task 1, `docs/Administracion-Proyectos/README.md:1`, `docs/Administracion de proyectos/README.md:45`
- Produces: carpeta con espacio eliminada; hyphen es único canónico.

- [ ] **Step 1: git mv docx + README (preserva historia)**

```bash
git mv "docs/Administracion de proyectos/Plantilla Proyecto.docx" "docs/Administracion-Proyectos/Plantilla Proyecto.docx"
git mv "docs/Administracion de proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx" "docs/Administracion-Proyectos/Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx"
# README merge: conservar índice de plantilla (14 secciones) + checklist fotos si existe en destino
cat "docs/Administracion de proyectos/README.md" >> /tmp/readme_src.md
cat docs/Administracion-Proyectos/README.md >> /tmp/readme_dst.md
# merge manual: el destino ya tiene capturas/ checklist; anteponer tabla docx del src
```

Editar `docs/Administracion-Proyectos/README.md` para fusionar: mantener cabecera `TS683` + tabla `Plantilla`/`Proyecto_AetherNet` (99KB 14 secciones) + sección `Cómo regenerar` + `Trabajo individual solo` del src, y sección `Capturas` del dst.

```markdown
# Administración de Proyectos — AetherNet
Carpeta canónica TS683 (UTP) — ver `docs/materias/administracion-proyectos.md`.
## Archivos (canónicos)
| Archivo | Descripción |
| Plantilla Proyecto.docx | Plantilla institucional 14 secciones — no editar |
| Proyecto_AetherNet_Andres_Felipe_Martinez_Henao.docx | Implementado 99KB 12 tablas 671 párrafos (09/09/2026) — 14 secciones Título→Anexos |
| README.md | Este índice |
| calendario/ | 5 archivos ICS/VCS/CSV + README (desde calendar/) |
| capturas/ | tablero-kanban.png, gantt.png |
```

- [ ] **Step 2: Borrar carpeta vacía y actualizar referencias**

```bash
rmdir "docs/Administracion de proyectos" 2>/dev/null || git rm -r "docs/Administracion de proyectos" 2>/dev/null
grep -rn "Administracion de proyectos" --include="*.md" docs notebooks stats calendar 2>&1 | cat
```

Reemplazar en cada archivo encontrado (ejemplos, repetir para todos):

```bash
# docs/README.md:30
# docs/sprints.md:60
# docs/revision-sprint2.md
# docs/Estadistica/CONTRASTE_Admin_vs_Estadistica.md
# calendar/README.md (si aún referencia antigua antes de mover)
```

Edit: `Administracion de proyectos/` → `Administracion-Proyectos/` y `docs/Administracion de proyectos/Proyecto_AetherNet` → `docs/Administracion-Proyectos/Proyecto_AetherNet`.

- [ ] **Step 3: Verificar**

```bash
ls -lh "docs/Administracion-Proyectos/" 2>&1
test ! -d "docs/Administracion de proyectos" && echo "OK espacio eliminado" || echo "FAIL aún existe"
grep -rn "Administracion de proyectos" --include="*.md" docs notebooks 2>&1 | wc -l  # esperado 0
```

- [ ] **Step 4: Commit**

```bash
git add docs/Administracion-Proyectos/ "docs/Administracion de proyectos" docs/README.md docs/sprints.md docs/revision-sprint2.md docs/Estadistica/CONTRASTE_Admin_vs_Estadistica.md 2>&1 | head
git commit -m "docs: centraliza Administracion-Proyectos — elimina carpeta con espacio (Task 2)"
```

---

### Task 3: Mover `calendar/` raíz → `docs/Administracion-Proyectos/calendario/`

**Files:**
- Move: `calendar/AetherNet_8semanas_Trabajo_Sprints.ics` → `docs/Administracion-Proyectos/calendario/AetherNet_8semanas_Trabajo_Sprints.ics`
- Move: `calendar/AetherNet_8semanas_Trabajo_Sprints.vcs` → `.../calendario/`
- Move: `calendar/AetherNet_Clases_UTP_2026-2.ics` → `.../calendario/`
- Move: `calendar/AetherNet_Completo_8semanas.ics` → `.../calendario/`
- Move: `calendar/AetherNet_Google_Calendar.csv` → `.../calendario/`
- Move: `calendar/README.md` → `.../calendario/README.md`
- Create: `calendar/README.md` redirección (opcional) o `git rm -r calendar`
- Modify: `docs/sprints.md`, `docs/gantt.md`, `docs/Administracion-Proyectos/README.md`, `notebooks/Diario_Administracion.ipynb`, `docs/tablero-scrum.md`

**Interfaces:**
- Consumes: `calendar/README.md:7`, `docs/Administracion-Proyectos/README.md`
- Produces: calendario canónico bajo Administración; raíz `calendar/` vaciada.

- [ ] **Step 1: git mv 6 archivos**

```bash
mkdir -p docs/Administracion-Proyectos/calendario
git mv calendar/AetherNet_8semanas_Trabajo_Sprints.ics docs/Administracion-Proyectos/calendario/
git mv calendar/AetherNet_8semanas_Trabajo_Sprints.vcs docs/Administracion-Proyectos/calendario/
git mv calendar/AetherNet_Clases_UTP_2026-2.ics docs/Administracion-Proyectos/calendario/
git mv calendar/AetherNet_Completo_8semanas.ics docs/Administracion-Proyectos/calendario/
git mv calendar/AetherNet_Google_Calendar.csv docs/Administracion-Proyectos/calendario/
git mv calendar/README.md docs/Administracion-Proyectos/calendario/README.md
```

- [ ] **Step 2: Dejar redirección en `calendar/README.md` y actualizar refs**

Crear `calendar/README.md`:

```markdown
# Redirección — calendario movido
> **Nuevo canónico:** `docs/Administracion-Proyectos/calendario/` — ver `docs/Administracion-Proyectos/calendario/README.md`
> Esta carpeta se mantiene como redirección para no romper links externos; el contenido vive en `docs/Administracion-Proyectos/calendario/`.
```

Actualizar en `docs/sprints.md`, `docs/gantt.md`, `docs/Administracion-Proyectos/README.md` (si menciona `calendar/`), `notebooks/Diario_Administracion.ipynb` celda 2/3 (`Path("calendar/")` → `Path("docs/Administracion-Proyectos/calendario")`), `docs/tablero-scrum.md`.

```bash
grep -rn "calendar/" --include="*.md" --include="*.ipynb" docs notebooks calendar 2>&1 | cat
# reemplazar calendar/ → docs/Administracion-Proyectos/calendario/ en cada hit
```

- [ ] **Step 3: Verificar**

```bash
ls -lh docs/Administracion-Proyectos/calendario/ 2>&1
cat calendar/README.md 2>&1 | head -n 5
grep -rn "calendar/" --include="*.md" docs notebooks 2>&1 | grep -v "Administracion-Proyectos/calendario" | grep -v "calendar/README.md" | wc -l  # esperado 0
python3 -c "import json; nb=json.load(open('notebooks/Diario_Administracion.ipynb')); txt=json.dumps(nb); print('calendar' in txt)"
```

- [ ] **Step 4: Commit**

```bash
git add docs/Administracion-Proyectos/calendario calendar docs/sprints.md docs/gantt.md notebooks/Diario_Administracion.ipynb docs/tablero-scrum.md
git commit -m "docs: centraliza calendar → docs/Administracion-Proyectos/calendario (Task 3)"
```

---

### Task 4: Consolidar `docs/Estadistica/` y `docs/presentaciones/` y `docs/materias/` — sin mover CSV espejo

**Files:**
- Keep: `docs/Estadistica/Anteproyecto_AetherNet-3.docx`, `Contraste_Admin_vs_Estadistica.docx`, `CONTRASTE_Admin_vs_Estadistica.md`, `Datasets/*`, `README.md`, `fotos/`
- Keep or Move: `docs/presentaciones/*.html` → `docs/Administracion-Proyectos/presentaciones/` (decidir)
- Modify: `docs/Estadistica/README.md` (añadir links a `docs/materias/estadistica.md`, `docs/materias/roadmap-estadistica.md`)
- Modify: `docs/materias/README.md` (ya 7 filas — añadir nota espejos)
- Modify: `docs/README.md` (actualizar mapa rápido si presentaciones se mueve)

**Interfaces:**
- Consumes: `docs/Estadistica/README.md:18`, `docs/presentaciones/guion-*.md`, `docs/materias/README.md:21`
- Produces: Estadística canónica limpia; presentaciones centralizadas bajo Administración si se decide mover.

- [ ] **Step 1: Decidir presentaciones — mantener o mover (recomendado: mover a Administración)**

Si se mueve:

```bash
mkdir -p docs/Administracion-Proyectos/presentaciones
git mv docs/presentaciones/presentacion-administracion-2026-08-26.html docs/Administracion-Proyectos/presentaciones/
git mv docs/presentaciones/presentacion-devops-2026-08-26.html docs/Administracion-Proyectos/presentaciones/
git mv docs/presentaciones/presentacion-estadistica-2026-08-26.html docs/Administracion-Proyectos/presentaciones/
git mv docs/presentaciones/presentacion-moviles-2026-08-26.html docs/Administracion-Proyectos/presentaciones/
git mv docs/presentaciones/guion-estadistica-2026-08-26.md docs/Administracion-Proyectos/presentaciones/
# Dejar redirección
```

Crear `docs/presentaciones/README.md`:

```markdown
# Redirección — presentaciones movidas
> **Nuevo canónico:** `docs/Administracion-Proyectos/presentaciones/` — 4 html + 1 guion
> Esta carpeta queda como redirección.
```

Si se decide KEEP: no mover, solo actualizar `docs/Estadistica/README.md` para referenciar `docs/materias/`.

- [ ] **Step 2: Actualizar `docs/Estadistica/README.md` con capa académica**

Editar para añadir al inicio:

```markdown
> **Capa académica:** `docs/materias/estadistica.md` (mapeo PDF TS4D3) + `docs/materias/roadmap-estadistica.md` (búsquedas) + `docs/materias/backlog-estadistica.md` — este `docs/Estadistica/README.md` es el **diario de campo** (fotos/gráficos), no la teoría.
```

Y en `docs/materias/README.md` añadir nota espejos:

```markdown
- `docs/Estadistica/Datasets/` es espejo de `stats/Dataset/` (canónico 3.7 MB 36.5k) — ver `stats/Dataset/README.md` y `docs/Estadistica/Datasets/README.md`
- `stats/notebooks/EMA_Estadistica.ipynb` es espejo de `notebooks/EMA_Estadistica.ipynb` (solo EMA)
```

- [ ] **Step 3: Verificar**

```bash
ls -lh docs/Estadistica/ docs/Administracion-Proyectos/presentaciones/ 2>&1 | head -n 20
grep -rn "presentaciones" --include="*.md" docs 2>&1 | head -n 20
diff -r stats/Dataset docs/Estadistica/Datasets 2>&1 | head -n 5 || echo "espejos OK si sin diff o solo README"
```

- [ ] **Step 4: Commit**

```bash
git add docs/Estadistica docs/presentaciones docs/Administracion-Proyectos/presentaciones docs/materias/README.md docs/README.md 2>&1 | head
git commit -m "docs: consolida Estadistica + presentaciones → Administración (Task 4)"
```

---

### Task 5: Unificar transversales `docs/fritzing/`, `docs/logs/`, `docs/UTP/`, `docs/adr/`, `docs/archivo/`

**Files:**
- Keep all 5 folders, solo actualizar índices
- Modify: `docs/fritzing/compendio-planos.md` (referenciar `docs/Hardware/README.md` + `docs/Firmware/README.md`)
- Modify: `docs/README.md` (mapa rápido — asegurar que fritzing/logs/UTP/adr/archivo aparecen)
- Modify: `docs/hardware-inventory.md` (si referencia fritzing con path antiguo — ya OK)
- Modify: `notebooks/*.ipynb` si referencian `docs/fritzing/` con path relativo viejo (verificar `../../docs/fritzing/`)

**Interfaces:**
- Consumes: `docs/fritzing/compendio-planos.md`, `docs/README.md:38`, `notebooks/Firmware_Notebook.ipynb:15`
- Produces: transversales documentados como canónicos únicos, sin duplicados.

- [ ] **Step 1: Verificar que no hay duplicados de fritzing/logs/UTP**

```bash
find . -type d -name "fritzing" 2>&1
find . -type d -name "logs" 2>&1
find . -type d -name "UTP" 2>&1
ls -lh docs/UTP/ docs/fritzing/ docs/logs/firmware_sprint3/ 2>&1 | head -n 20
grep -rn "fritzing\|docs/logs\|docs/UTP" --include="*.md" --include="*.ipynb" docs notebooks stats 2>&1 | wc -l
```

- [ ] **Step 2: Actualizar `docs/fritzing/compendio-planos.md` con refs a diarios**

Añadir al inicio:

```markdown
> **Diarios campo:** `notebooks/Diario_Hardware.ipynb` + `notebooks/Firmware_Notebook.ipynb` — fritzing aquí es referencia, fotos reales en `docs/Firmware/fotos/` y `docs/Hardware/fotos/`
```

- [ ] **Step 3: Commit**

```bash
git add docs/fritzing/compendio-planos.md docs/README.md 2>&1
git commit -m "docs: unifica transversales fritzing/logs/UTP/adr/archivo como canónicos únicos (Task 5)"
```

---

### Task 6: Actualizar referencias cruzadas masivas + validar notebooks y tests

**Files:**
- Modify: `docs/README.md:38` (índice maestro — mayor cambio)
- Modify: `notebooks/README.md:50`, `docs/notebooks/README.md:53`
- Modify: `docs/materias/README.md`, `docs/roadmap.md`, `docs/architecture.md`, `docs/sprints.md`, `docs/revision-sprint2.md`, `docs/gantt.md`, `docs/tablero-scrum.md`, `docs/risk-register.md`, `docs/backlog.md`
- Modify: `notebooks/*.ipynb` (cualquier celda con `Path("calendar/")` o `Administracion de proyectos`)
- Modify: `stats/Dataset/README.md`, `docs/Estadistica/Datasets/README.md` (verificar que no apuntan a path viejo)

**Interfaces:**
- Consumes: todos los moves Tasks 2..5
- Produces: cero `grep` huérfano; notebooks abren; tests verdes.

- [ ] **Step 1: Grep masivo de huérfanos**

```bash
grep -rn "Administracion de proyectos" --include="*.md" --include="*.ipynb" . 2>&1 | tee /tmp/final_espacio.txt
grep -rn "calendar/" --include="*.md" --include="*.ipynb" . 2>&1 | grep -v "Administracion-Proyectos/calendario" | grep -v "calendar/README.md" | tee /tmp/final_calendar.txt
grep -rn "docs/Estadistica/Datasets" --include="*.md" docs notebooks stats 2>&1 | tee /tmp/final_estadistica.txt
grep -rn "presentaciones" --include="*.md" docs 2>&1 | tee /tmp/final_presentaciones.txt
wc -l /tmp/final_*.txt
# esperado: final_espacio 0, final_calendar 0
```

- [ ] **Step 2: Actualizar `docs/README.md` — reescribir Mapa Rápido canónico**

Reemplazar tabla `| `notebooks/README.md` | ... |` + filas administración/calendar:

```markdown
| `Administracion-Proyectos/` | Gestión TS683: `Proyecto_AetherNet*.docx` + `calendario/*.ics` + `presentaciones/*.html` + `capturas/` | WBS 40, Gantt, kanban 14 |
| `Estadistica/` | Entrega TS4D3: `Anteproyecto*.docx`, `CONTRASTE*.md`, `Datasets/` espejo 36.5k, `fotos/` | 36.5k + Welch/ANOVA |
| `materias/` | Capa académica: `estadistica.md`, `programacion-movil.md`, `devops.md`, `administracion-proyectos.md`, `firmware.md`, `hardware.md` + 7 roadmaps + 5 backlogs | Mapeo PDF UTP |
| `calendar/` | **Redirección** → `docs/Administracion-Proyectos/calendario/` | ICS/VCS/CSV |
| `Administracion de proyectos/` | **ELIMINADA** — mover `git mv` a `Administracion-Proyectos/` (hyphen) | — |
```

- [ ] **Step 3: Actualizar `notebooks/README.md` y `docs/notebooks/README.md`**

En `notebooks/README.md` sección `Estructura por materia (docs/)` añadir:

```markdown
- `docs/Administracion-Proyectos/calendario/` — 5 archivos ICS/VCS/CSV (antes `calendar/`)
- `docs/Administracion de proyectos/` — **ELIMINADA** (espacio → hyphen)
```

En `docs/notebooks/README.md` actualizar tabla espejos si menciona `docs/Estadistica/Datasets` (ya OK, no cambiar).

- [ ] **Step 4: Validar notebooks + tests**

```bash
python3 -c "import json, pathlib; [json.load(open(str(p))) for p in pathlib.Path('notebooks').glob('*.ipynb')]; print('7 notebooks JSON OK')"
python3 -c "import pathlib; print(list(pathlib.Path('docs/Administracion-Proyectos/calendario').glob('*.ics')))"
pytest stats/tests/test_ema_filter.py -q 2>&1 | tail -n 5
# opcional: grep Path en ipynb
grep -n "Path(" notebooks/Diario_Administracion.ipynb 2>&1 | head -n 10
```

- [ ] **Step 5: Commit masivo**

```bash
git add docs/README.md notebooks/README.md docs/notebooks/README.md docs/materias/README.md docs/roadmap.md docs/architecture.md docs/sprints.md docs/gantt.md docs/tablero-scrum.md stats/Dataset/README.md docs/Estadistica/Datasets/README.md notebooks/*.ipynb 2>&1 | head
git commit -m "docs: actualiza referencias cruzadas masivas post-centralización + valida notebooks (Task 6)"
```

---

### Task 7: Verificación final — `find` + `grep` + `git status` + `git log --follow`

**Files:**
- No moves, solo lectura

**Interfaces:**
- Consumes: repo post Tasks 1..6
- Produces: evidence de que centralización es completa y sin huérfanos.

- [ ] **Step 1: Find final**

```bash
find docs -type f | sort | tee /tmp/final_find.txt && wc -l /tmp/final_find.txt
find docs -type d | sort | tee /tmp/final_dirs.txt && cat /tmp/final_dirs.txt
```

- [ ] **Step 2: Grep final huérfanos**

```bash
echo "=== espacio ===" && grep -rn "Administracion de proyectos" --include="*.md" --include="*.ipynb" . 2>&1 | wc -l
echo "=== calendar huérfano ===" && grep -rn "\"calendar/" --include="*.md" --include="*.ipynb" . 2>&1 | grep -v "Administracion-Proyectos/calendario" | wc -l
echo "=== presentaciones vieja ===" && grep -rn "docs/presentaciones" --include="*.md" docs 2>&1 | head -n 20
echo "=== UTP OK ===" && ls -lh docs/UTP/ 2>&1 | head -n 10
```

- [ ] **Step 3: Git follow + status**

```bash
git log --follow --name-status -- "docs/Administracion-Proyectos/Plantilla Proyecto.docx" 2>&1 | head -n 20
git status --short 2>&1 | head -n 30
git log --oneline -8 2>&1 | head -n 10
```

- [ ] **Step 4: Commit auditoría final (si hay fixes menores)**

```bash
git add /tmp/final_find.txt 2>/dev/null || true
git commit -m "docs: verificación final centralización — find/grep/follow OK (Task 7)" 2>&1 | tail -n 5 || echo "nada que commitear — OK"
```

---

## Self-Review

**1. Spec coverage:**
- Muchos md regados → Tasks 1..5 centralizan por tipo (admin, estadística, presentaciones, transversales)
- Carpetas externas/no relacionadas → Task 3 `calendar/` raíz → `docs/Administracion-Proyectos/calendario/`
- Centralizar en lugar correspondiente → Tasks 2..5 mueven cada doc a su materia canónica (`Administracion-Proyectos`, `Estadistica`, `presentaciones`)
- Actualizar archivos que apuntan a ellos → Task 6 grep masivo + edit en `README.md`, `sprints.md`, `gantt.md`, `notebooks/*.ipynb`, `Dataset/README`

**2. Placeholder scan:** Ningún `TBD/TODO` — todos los pasos tienen comandos `git mv`/`grep`/`edit` concretos con paths exactos.

**3. Type consistency:** `docs/Administracion-Proyectos/` hyphen consistente con `Programacion-Movil`, `Automatizacion-LowCode`, `DevOps` ya creados Tasks 1..10; `notebooks/` canónico, `stats/Dataset/` canónico, `docs/Estadistica/Datasets/` espejo, `stats/notebooks/` espejo solo EMA — todo coherente con `notebooks/README.md:13` y `docs/notebooks/README.md`.

---

Plan complete and saved to `docs/superpowers/plans/2026-09-11-centralizacion-docs.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration

2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
