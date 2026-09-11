# Notebooks — Centralizado en `docs/<Materia>/notebook/` (canónico)

> **Autor:** Andres Felipe Martinez Henao
> Todos los notebooks viven en **`docs/<Materia>/notebook/`** — **CANÓNICO**. `notebooks/README.md` y `stats/notebooks/` son compatibilidad. Ver `docs/README.md`.

## 📑 Índice — 7 notebooks canónicos

| Notebook canónico | Descripción | Materia / Sprint | Ejecutar |
|---|---|---|---|
| `docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` | App Android **AetherControl** — Kotlin Compose + MVVM | Móvil Sprint 1-4 — RF-1.1/1.2 HU-01 | `jupyter notebook docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` |
| `docs/Firmware/notebook/Firmware_Notebook.ipynb` | Firmware integrado — MEGA+Gateway+Rover+Joystick | Firmware Sprint 1-3 | `jupyter notebook docs/Firmware/notebook/Firmware_Notebook.ipynb` |
| `docs/Estadistica/notebook/EMA_Estadistica.ipynb` | Estadística TS4D3 — EMA α=0.2 HU-03 + 36.5k datasets | Estadística Sprint 1-4 — RNF-2.1, RNF-2.2, HU-03 | `jupyter notebook docs/Estadistica/notebook/EMA_Estadistica.ipynb` |
| `docs/Hardware/notebook/Diario_Hardware.ipynb` | Hardware — diario campo | Hardware Sprint 1-3 | `jupyter notebook docs/Hardware/notebook/Diario_Hardware.ipynb` |
| `docs/DevOps/notebook/Diario_DevOps.ipynb` | DevOps — diario campo | DevOps Sprint 1 | `jupyter notebook docs/DevOps/notebook/Diario_DevOps.ipynb` |
| `docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` | Admin — diario campo — WBS 40, riesgos | Administración transversal | `jupyter notebook docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` |
| `docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` | LowCode — Telegram Bot API directo | LowCode Sprint 4 | `jupyter notebook docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` |

```
docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb  ← CANÓNICO
docs/Estadistica/notebook/EMA_Estadistica.ipynb                 ← CANÓNICO
docs/Firmware/notebook/Firmware_Notebook.ipynb                  ← CANÓNICO
docs/Hardware/notebook/Diario_Hardware.ipynb                    ← CANÓNICO
docs/DevOps/notebook/Diario_DevOps.ipynb                        ← CANÓNICO
docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb
docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb
notebooks/README.md               ← redirección
stats/notebooks/EMA_Estadistica.ipynb  ← espejo solo EMA (cp docs/Estadistica/notebook/EMA_Estadistica.ipynb stats/notebooks/)
```

## Estructura por materia (docs/)

- `docs/Programacion-Movil/` — `programacion-movil.md` + `roadmap-movil.md` + `backlog-movil.md` + `notebook/AetherControl_Notebook.ipynb`
- `docs/Firmware/` — `firmware.md` + `roadmap-firmware.md` + `notebook/Firmware_Notebook.ipynb`
- `docs/Estadistica/` — `estadistica.md` + `roadmap-estadistica.md` + `backlog-estadistica.md` + `notebook/EMA_Estadistica.ipynb` + `Datasets/` espejo 36.5k
- `docs/Hardware/` — `hardware.md` + `roadmap-hardware.md` + `notebook/Diario_Hardware.ipynb`
- `docs/DevOps/` — `devops.md` + `roadmap-devops.md` + `backlog-devops.md` + `notebook/Diario_DevOps.ipynb`
- `docs/Administracion-Proyectos/` — `administracion-proyectos.md` + `roadmap-*.md` + `backlog-*.md` + `notebook/Diario_Administracion.ipynb` + `calendario/`
- `docs/Automatizacion-LowCode/` — `roadmap-lowcode.md` + `backlog-lowcode.md` + `notebook/Diario_LowCode.ipynb`

Solo `EMA_Estadistica.ipynb` se espeja en `stats/notebooks/` por compatibilidad (`stats/ema_filter.py`); los 6 diarios restantes solo viven en `docs/<Materia>/notebook/` y no se copian a `stats/`.

## Abrir

```bash
jupyter lab docs/Estadistica/notebook/
jupyter lab docs/Programacion-Movil/notebook/
jupyter lab docs/Firmware/notebook/
# compat
jupyter lab stats/notebooks/EMA_Estadistica.ipynb
```

> **Nota paths:** notebooks detectan `stats/ema_filter.py` etc. vía `candidates` — funcionan desde `docs/<Materia>/notebook/` porque resuelven `Path.cwd().parents`.

## Sincronizar espejo tras editar canónico

```bash
cp docs/Estadistica/notebook/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb
# Solo EMA se espeja — los 6 diarios restantes NO se copian a stats/
```

## Referencias

- `docs/prd.md:51` KPI >85% · `docs/requirements.md:42` RNF-2.1/2.2 · `docs/architecture.md:72` flujo `sensor → EMA → MQTT → PG → stats`
- `stats/ema_filter.py`, `stats/experiments/alpha_sweep.py`, `stats/data/`, `firmware/test-ema-uno/test-ema-uno.ino`
- `docs/README.md` ← índice maestro

*Centralizado 2026-09-11 en `docs/<Materia>/notebook/` — 7 diarios canónicos, solo `EMA_Estadistica.ipynb` espejado en `stats/notebooks/`.*
