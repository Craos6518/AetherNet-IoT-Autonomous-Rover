# 📚 Notebooks — AetherNet IoT & Autonomous Rover

Carpeta centralizada de **todos** los notebooks del proyecto. Antes estaban dispersos en `stats/notebooks/` y `docs/notebooks/`; ahora viven aquí. `notebooks/` es **CANÓNICO** (editar aquí), `stats/notebooks/` solo espejo de `EMA_Estadistica.ipynb`, `docs/notebooks/README.md` solo redirige.

## 📑 Índice

| Notebook | Descripción | Materia / Sprint | Ejecutar |
|---|---|---|---|
| [`AetherControl_Notebook.ipynb`](AetherControl_Notebook.ipynb) | App Android **AetherControl** — Kotlin Compose + MVVM + capturas APP `docs/Programacion-Movil/capturas/` | Móvil Sprint 1-4 — RF-1.1/1.2 HU-01 | `jupyter notebook notebooks/AetherControl_Notebook.ipynb` |
| [`Firmware_Notebook.ipynb`](Firmware_Notebook.ipynb) | Firmware integrado — MEGA+Gateway+Rover+Joystick RF-2.1/3.1 HU-01/02/03/04 + fotos `docs/Firmware/fotos/` + logs `docs/logs/firmware_sprint3/` | Firmware Sprint 1-3 | `jupyter notebook notebooks/Firmware_Notebook.ipynb` |
| [`EMA_Estadistica.ipynb`](EMA_Estadistica.ipynb) | Estadística TS4D3 — EMA α=0.2 HU-03 + 36.5k datasets + Welch/ANOVA + gráficos `docs/fritzing/water_*.png` | Estadística Sprint 1-4 | `jupyter notebook notebooks/EMA_Estadistica.ipynb` |
| [`Diario_Hardware.ipynb`](Diario_Hardware.ipynb) | **Hardware — diario campo** — inventario `docs/hardware-inventory.md`, fritzing `docs/fritzing/`, fotos `docs/Hardware/fotos/`, calce TT 6V | Hardware Sprint 1-3 | `jupyter notebook notebooks/Diario_Hardware.ipynb` |
| [`Diario_DevOps.ipynb`](Diario_DevOps.ipynb) | **DevOps — diario campo** — Docker `docker-compose.yml`, Mosquitto `aethernet/#`, CI 6 jobs `ci.yml`, logs T1 | DevOps Sprint 1 | `jupyter notebook notebooks/Diario_DevOps.ipynb` |
| [`Diario_Administracion.ipynb`](Diario_Administracion.ipynb) | **Admin — diario campo** — Scrum WBS 40, riesgos R-01..R-13, Gantt, tablero Kanban | Administración transversal | `jupyter notebook notebooks/Diario_Administracion.ipynb` |
| [`Diario_LowCode.ipynb`](Diario_LowCode.ipynb) | **LowCode — diario campo** — Telegram Bot API directo, Node-RED deuda `automation/flows/intrusion_alert.json` | LowCode Sprint 4 | `jupyter notebook notebooks/Diario_LowCode.ipynb` |

## 🗂️ Estructura del proyecto

```
notebooks/                      ← CANÓNICO (editar aquí)
├── AetherControl_Notebook.ipynb  ← Kotlin / MVVM / Compose + capturas
├── EMA_Estadistica.ipynb        ← Python / EMA / 36.5k
├── Firmware_Notebook.ipynb      ← MEGA+Gateway+Rover + fotos/logs
├── Diario_Hardware.ipynb        ← Hardware inventario + fotos
├── Diario_DevOps.ipynb          ← Docker/Mosquitto/CI
├── Diario_Administracion.ipynb  ← Scrum/riesgos/Gantt
├── Diario_LowCode.ipynb         ← Telegram/Node-RED
└── README.md                    ← este mapa
```

stats/notebooks/EMA_Estadistica.ipynb  ← copia espejo (no editar, usa la de notebooks/ — `cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb`)
docs/notebooks/README.md               ← redirección (ver canónico aquí)

## 🗃️ Estructura por materia (docs/)

Carpetas espejo de cada diario en `docs/` (fuente canónica para fotos/capturas/logs que los notebooks leen):

- `docs/Programacion-Movil/` — capturas APP `capturas/` (Dashboard, PinScreen, Joystick) → `AetherControl_Notebook.ipynb`
- `docs/Firmware/` — fotos hardware `fotos/` (MEGA/Gateway/Rover) + fritzing `docs/fritzing/` → `Firmware_Notebook.ipynb`
- `docs/Estadistica/` — datasets 36.5k `Datasets/` + gráficos `docs/fritzing/water_*.png` + `alpha_sweep.png` → `EMA_Estadistica.ipynb`
- `docs/Hardware/` — inventario `docs/hardware-inventory.md` + fritzing `docs/fritzing/` + fotos `fotos/` + calce TT 6V → `Diario_Hardware.ipynb`
- `docs/DevOps/` — Docker `docker-compose.yml`, Mosquitto `aethernet/#`, CI `ci.yml`, logs `docs/logs/firmware_sprint3/` → `Diario_DevOps.ipynb`
- `docs/Administracion-Proyectos/` — Scrum WBS 40, riesgos R-01..R-13, Gantt `docs/gantt.md`, tablero Kanban → `Diario_Administracion.ipynb`
- `docs/Automatizacion-LowCode/` — Telegram Bot API directo, Node-RED deuda `automation/flows/intrusion_alert.json` → `Diario_LowCode.ipynb`

## ▶️ Cómo ejecutar

Desde la **raíz** del repo:

```bash
# Opción 1: Jupyter Lab (recomendado — todos los diarios)
jupyter lab notebooks/

# Opción 2: Notebook clásico (uno por uno)
jupyter notebook notebooks/AetherControl_Notebook.ipynb
jupyter notebook notebooks/Firmware_Notebook.ipynb
jupyter notebook notebooks/EMA_Estadistica.ipynb
jupyter notebook notebooks/Diario_Hardware.ipynb
jupyter notebook notebooks/Diario_DevOps.ipynb
jupyter notebook notebooks/Diario_Administracion.ipynb
jupyter notebook notebooks/Diario_LowCode.ipynb

# Opción 3: VS Code — abre el .ipynb directo
code notebooks/Diario_Hardware.ipynb
```

> **Nota de paths:** Todos los notebooks están parcheados para funcionar tanto si los abres desde `notebooks/` como desde `stats/notebooks/` (solo EMA). Detectan automáticamente `stats/ema_filter.py`, `app/src/...`, `docs/hardware-inventory.md`, `docs/fritzing/`, `docs/logs/firmware_sprint3/` y `docs/<Materia>/` vía `candidates = [Path.cwd(), Path("stats"), Path("../stats"), Path().resolve().parent / "stats"]` + fallback `importlib.util.spec_from_file_location`. Ver `docs/notebooks/README.md` para espejo.

## 🔗 Referencias

- `docs/prd.md`, `docs/requirements.md`, `docs/architecture.md`
- `app/src/main/java/com/aethernet/aethercontrol/` — código Android real
- `stats/ema_filter.py`, `stats/visualize_ema.py`, `stats/data/`
- `docs/hardware-inventory.md`, `docs/fritzing/`, `docs/logs/firmware_sprint3/`
- `docs/materias/` — roadmaps por materia con búsquedas Google y datasheets

---
*Última organización: centralizado en `notebooks/` (raíz) para evitar dispersión. 7 diarios canónicos — solo `EMA_Estadistica.ipynb` se espeja en `stats/notebooks/` (`cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb`). Ver `docs/notebooks/README.md`.*
