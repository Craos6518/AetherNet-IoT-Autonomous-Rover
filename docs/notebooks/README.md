# Notebooks — Centralizado en `notebooks/` (Raíz)

> **Autor 6º semestre:** Tecnología en Desarrollo de Software + Ingeniería de Sistemas (UTP) — 2y electrónica/Arduino, 1y C, 2y Python, 2y React, 1y PostgreSQL

Todos los notebooks viven en **`notebooks/`** (raíz). `docs/notebooks/` solo redirige, `stats/notebooks/` es espejo de compatibilidad.

| Notebook canónico | Descripción | Materia / Sprint | `sys.path` |
|---|---|---|---|
| `notebooks/AetherControl_Notebook.ipynb` | App Android **AetherControl** — Kotlin desde cero (`val`/`var`, null-safety, `sealed interface`, Compose `StateFlow`/`ViewModel`/`Repository`) + 7 ejercicios integradores | Móvil Sprint 1-2 — RF-1.1, RNF-3.1 | detecta `app/src/` |
| `notebooks/EMA_Estadistica.ipynb` | Estadística TS4D3 — EMA `S_t=α·Y_t+(1-α)·S_{t-1}` α=0.2 HU-03, ruido HC-SR04 σ8, KPI >85%, Monte Carlo 100× `alpha_sweep.py`, banco UNO `test-ema-uno.ino` D2/D3, `t-Student` plan | Estadística Sprint 1-4 — RNF-2.1, RNF-2.2, HU-03 | detecta `stats/ema_filter.py` |

```
notebooks/                       ← CANÓNICO (editar aquí)
├── AetherControl_Notebook.ipynb   ← Kotlin / MVVM / Compose
├── EMA_Estadistica.ipynb         ← Python / EMA / Estadística
└── README.md                     ← este mapa + paths

stats/notebooks/EMA_Estadistica.ipynb  ← espejo (no editar, `cp notebooks/... stats/notebooks/...`)
stats/notebooks/README.md              ← explica espejo
docs/notebooks/README.md               ← redirección (este archivo)
```

## Abrir

```bash
# Centralizado (recomendado)
jupyter lab notebooks/
jupyter notebook notebooks/EMA_Estadistica.ipynb
jupyter notebook notebooks/AetherControl_Notebook.ipynb
# VS Code
code notebooks/EMA_Estadistica.ipynb

# Compatibilidad (espejo)
jupyter lab stats/notebooks/EMA_Estadistica.ipynb
```

> **Nota paths:** Ambos notebooks detectan `stats/ema_filter.py` y `app/src/` vía `candidates = [Path.cwd(), Path("stats"), Path("../stats"), Path().resolve().parent / "stats"]` + fallback `importlib.util.spec_from_file_location`. Funciona desde `notebooks/` y `stats/notebooks/`.

## Sincronizar espejo tras editar canónico

```bash
cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb
# AetherControl no se espeja en stats/ — solo en notebooks/
```

## Referencias

- `docs/prd.md:51` KPI >85% · `docs/requirements.md:42` RNF-2.1/2.2 · `docs/architecture.md:72` flujo `sensor → EMA → MQTT → PG → stats`
- `stats/ema_filter.py`, `stats/experiments/alpha_sweep.py`, `stats/data/`, `firmware/test-ema-uno/test-ema-uno.ino`
- `app/src/main/java/com/aethernet/aethercontrol/` · `docs/roadmap.md:86` Estadística

*Centralizado 2026-09-07 para evitar dispersión `stats/notebooks` vs `docs/notebooks` vs `notebooks/`. Ver `notebooks/README.md`.*
