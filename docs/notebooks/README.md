# Notebooks — Centralizado en `notebooks/` (Raíz)

> **Autor:** Andres Felipe Martinez Henao

Todos los notebooks viven en **`notebooks/`** (raíz) — **CANÓNICO**. `docs/notebooks/` solo redirige (este archivo), `stats/notebooks/` es espejo **solo de `EMA_Estadistica.ipynb`** (compatibilidad). Ver `notebooks/README.md` como mapa maestro.

## 📑 Índice — 7 notebooks canónicos

| Notebook canónico | Descripción | Materia / Sprint | Ejecutar |
|---|---|---|---|
| `notebooks/AetherControl_Notebook.ipynb` | App Android **AetherControl** — Kotlin Compose + MVVM + capturas APP `docs/Programacion-Movil/capturas/` | Móvil Sprint 1-4 — RF-1.1/1.2 HU-01 | `jupyter notebook notebooks/AetherControl_Notebook.ipynb` |
| `notebooks/Firmware_Notebook.ipynb` | Firmware integrado — MEGA+Gateway+Rover+Joystick RF-2.1/3.1 HU-01/02/03/04 + fotos `docs/Firmware/fotos/` + logs `docs/logs/firmware_sprint3/` | Firmware Sprint 1-3 | `jupyter notebook notebooks/Firmware_Notebook.ipynb` |
| `notebooks/EMA_Estadistica.ipynb` | Estadística TS4D3 — EMA `S_t=α·Y_t+(1-α)·S_{t-1}` α=0.2 HU-03 + 36.5k datasets + Welch/ANOVA + gráficos `docs/fritzing/water_*.png` | Estadística Sprint 1-4 — RNF-2.1, RNF-2.2, HU-03 | `jupyter notebook notebooks/EMA_Estadistica.ipynb` |
| `notebooks/Diario_Hardware.ipynb` | **Hardware — diario campo** — inventario `docs/hardware-inventory.md`, fritzing `docs/fritzing/`, fotos `docs/Hardware/fotos/`, calce TT 6V | Hardware Sprint 1-3 | `jupyter notebook notebooks/Diario_Hardware.ipynb` |
| `notebooks/Diario_DevOps.ipynb` | **DevOps — diario campo** — Docker `docker-compose.yml`, Mosquitto `aethernet/#`, CI 6 jobs `ci.yml`, logs T1 | DevOps Sprint 1 | `jupyter notebook notebooks/Diario_DevOps.ipynb` |
| `notebooks/Diario_Administracion.ipynb` | **Admin — diario campo** — Scrum WBS 40, riesgos R-01..R-13, Gantt, tablero Kanban | Administración transversal | `jupyter notebook notebooks/Diario_Administracion.ipynb` |
| `notebooks/Diario_LowCode.ipynb` | **LowCode — diario campo** — Telegram Bot API directo, Node-RED deuda `automation/flows/intrusion_alert.json` | LowCode Sprint 4 | `jupyter notebook notebooks/Diario_LowCode.ipynb` |

```
notebooks/                       ← CANÓNICO (editar aquí)
├── AetherControl_Notebook.ipynb   ← Kotlin / MVVM / Compose + capturas
├── EMA_Estadistica.ipynb         ← Python / EMA / 36.5k
├── Firmware_Notebook.ipynb       ← MEGA+Gateway+Rover + fotos/logs
├── Diario_Hardware.ipynb         ← Hardware inventario + fotos
├── Diario_DevOps.ipynb           ← Docker/Mosquitto/CI
├── Diario_Administracion.ipynb   ← Scrum/riesgos/Gantt
├── Diario_LowCode.ipynb          ← Telegram/Node-RED
└── README.md                     ← mapa canónico (notebooks/README.md)

stats/notebooks/EMA_Estadistica.ipynb  ← espejo — SOLO EMA (no editar, `cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb`)
stats/notebooks/README.md              ← explica espejo (solo EMA)
docs/notebooks/README.md               ← redirección (este archivo) → ver notebooks/README.md
```

## Estructura por materia (docs/)

- `docs/Programacion-Movil/` — capturas APP → `AetherControl_Notebook.ipynb`
- `docs/Firmware/` — fotos MEGA/Gateway/Rover → `Firmware_Notebook.ipynb`
- `docs/Estadistica/` — 36.5k datasets + `water_*.png` → `EMA_Estadistica.ipynb`
- `docs/Hardware/` — inventario + fritzing + fotos → `Diario_Hardware.ipynb`
- `docs/DevOps/` — Docker/Mosquitto/CI + logs → `Diario_DevOps.ipynb`
- `docs/Administracion-Proyectos/` — Scrum/riesgos/Gantt → `Diario_Administracion.ipynb`
- `docs/Automatizacion-LowCode/` — Telegram/Node-RED → `Diario_LowCode.ipynb`

Solo `EMA_Estadistica.ipynb` se espeja en `stats/notebooks/` por compatibilidad histórica (`stats/ema_filter.py`); los 6 diarios restantes **solo viven en `notebooks/`** y no se copian a `stats/`.

## Abrir

```bash
# Centralizado (recomendado) — canónico
jupyter lab notebooks/
jupyter notebook notebooks/EMA_Estadistica.ipynb
jupyter notebook notebooks/AetherControl_Notebook.ipynb
jupyter notebook notebooks/Firmware_Notebook.ipynb
jupyter notebook notebooks/Diario_Hardware.ipynb
jupyter notebook notebooks/Diario_DevOps.ipynb
jupyter notebook notebooks/Diario_Administracion.ipynb
jupyter notebook notebooks/Diario_LowCode.ipynb
# VS Code
code notebooks/EMA_Estadistica.ipynb

# Compatibilidad (espejo — solo EMA)
jupyter lab stats/notebooks/EMA_Estadistica.ipynb
```

> **Nota paths:** Todos los notebooks detectan `stats/ema_filter.py`, `app/src/`, `docs/hardware-inventory.md`, `docs/fritzing/` y `docs/logs/firmware_sprint3/` vía `candidates = [Path.cwd(), Path("stats"), Path("../stats"), Path().resolve().parent / "stats"]` + fallback `importlib.util.spec_from_file_location`. Funciona desde `notebooks/` y desde `stats/notebooks/` (solo EMA). Ver `notebooks/README.md` para el mapa completo.

## Sincronizar espejo tras editar canónico

```bash
cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb
# Solo EMA se espeja — los 6 diarios (AetherControl, Firmware, Hardware, DevOps, Administracion, LowCode) NO se copian a stats/
# notebooks/README.md es canónico; este archivo (docs/notebooks/README.md) es solo redirección
```

## Referencias

- `docs/prd.md:51` KPI >85% · `docs/requirements.md:42` RNF-2.1/2.2 · `docs/architecture.md:72` flujo `sensor → EMA → MQTT → PG → stats`
- `stats/ema_filter.py`, `stats/experiments/alpha_sweep.py`, `stats/data/`, `firmware/test-ema-uno/test-ema-uno.ino`
- `app/src/main/java/com/aethernet/aethercontrol/` · `docs/roadmap.md:86` Estadística
- `docs/hardware-inventory.md`, `docs/fritzing/water_*.png`, `docs/logs/firmware_sprint3/`, `automation/flows/intrusion_alert.json`
- `notebooks/README.md` ← **mapa canónico 7 notebooks**

*Centralizado 2026-09-07 para evitar dispersión `stats/notebooks` vs `docs/notebooks` vs `notebooks/`. Actualizado 2026-09-11 a 7 diarios — solo `EMA_Estadistica.ipynb` espejado. Ver `notebooks/README.md`.*
