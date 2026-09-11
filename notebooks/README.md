# 📚 Notebooks — Redirección a `docs/<Materia>/notebook/`

> **Nuevo canónico:** cada notebook vive en su materia `docs/<Materia>/notebook/` (git mv preserva historia). Esta carpeta `notebooks/` queda como **redirect** para compatibilidad. Ver `docs/README.md` § Notebooks.

## 📑 Índice canónico actual

| Notebook | Materia | Nuevo canónico | Ejecutar |
|---|---|---|---|
| `AetherControl_Notebook.ipynb` | Programación Móvil TS6C3 | `docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` | `jupyter notebook docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` |
| `EMA_Estadistica.ipynb` | Estadística TS4D3 | `docs/Estadistica/notebook/EMA_Estadistica.ipynb` | `jupyter notebook docs/Estadistica/notebook/EMA_Estadistica.ipynb` |
| `Firmware_Notebook.ipynb` | Firmware | `docs/Firmware/notebook/Firmware_Notebook.ipynb` | `jupyter notebook docs/Firmware/notebook/Firmware_Notebook.ipynb` |
| `Diario_Hardware.ipynb` | Hardware | `docs/Hardware/notebook/Diario_Hardware.ipynb` | `jupyter notebook docs/Hardware/notebook/Diario_Hardware.ipynb` |
| `Diario_DevOps.ipynb` | DevOps | `docs/DevOps/notebook/Diario_DevOps.ipynb` | `jupyter notebook docs/DevOps/notebook/Diario_DevOps.ipynb` |
| `Diario_Administracion.ipynb` | Administración TS683 | `docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` | `jupyter notebook docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb` |
| `Diario_LowCode.ipynb` | LowCode | `docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` | `jupyter notebook docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb` |

```
notebooks/                      ← REDIRECCIÓN (este README)
docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb  ← CANÓNICO
docs/Estadistica/notebook/EMA_Estadistica.ipynb                 ← CANÓNICO
docs/Firmware/notebook/Firmware_Notebook.ipynb                  ← CANÓNICO
docs/Hardware/notebook/Diario_Hardware.ipynb                    ← CANÓNICO
docs/DevOps/notebook/Diario_DevOps.ipynb                        ← CANÓNICO
docs/Administracion-Proyectos/notebook/Diario_Administracion.ipynb
docs/Automatizacion-LowCode/notebook/Diario_LowCode.ipynb
stats/docs/Estadistica/notebook/EMA_Estadistica.ipynb  ← espejo solo EMA (cp desde docs/Estadistica/notebook/)
docs/notebooks/README.md               ← redirección
```

## ▶️ Cómo ejecutar (nuevo)

```bash
jupyter lab docs/Estadistica/notebook/
jupyter lab docs/Programacion-Movil/notebook/
jupyter lab docs/Firmware/notebook/
# compat
jupyter lab docs/Hardware/notebook/ docs/DevOps/notebook/ docs/Administracion-Proyectos/notebook/ docs/Automatizacion-LowCode/notebook/
```

> **Nota paths:** los notebooks detectan `stats/ema_filter.py` etc. vía `candidates` — funcionan desde cualquier `docs/<Materia>/notebook/` porque resuelven `Path.cwd().parent` etc. Espejo solo EMA: `cp docs/Estadistica/notebook/EMA_Estadistica.ipynb stats/docs/Estadistica/notebook/EMA_Estadistica.ipynb`

## 🔗 Referencias canónicas

- `docs/Estadistica/` `docs/Programacion-Movil/` `docs/Firmware/` `docs/Hardware/` `docs/DevOps/` `docs/Administracion-Proyectos/` `docs/Automatizacion-LowCode/` — cada materia con su `*.md` + `roadmap-*.md` + `backlog-*.md` + `notebook/`
- `docs/README.md` — índice maestro
