# 📚 Notebooks — AetherNet IoT & Autonomous Rover

Carpeta centralizada de **todos** los notebooks del proyecto. Antes estaban dispersos en `stats/notebooks/` y `docs/notebooks/`; ahora viven aquí.

## 📑 Índice

| Notebook | Descripción | Materia / Sprint | Ejecutar |
|---|---|---|---|
| [`AetherControl_Notebook.ipynb`](AetherControl_Notebook.ipynb) | App Android **AetherControl** — Notas de dev/estudiante, decisiones arquitectura (ADR), palabras reservadas y sintaxis Kotlin desde cero (`val`/`var`, tipos, null-safety, listas, `if`/`when`, `for`/`while`, `fun`, `data class`, `sealed interface`, lambdas, Compose) + 7 ejercicios integradores con `StateFlow`/`ViewModel`/`Repository` | Movil (Sprint 1) — RF-1.1, RNF-3.1 | `jupyter notebook notebooks/AetherControl_Notebook.ipynb` |
| [`EMA_Estadistica.ipynb`](EMA_Estadistica.ipynb) | Estadística aplicada — Filtro EMA `S_t = α·Y_t + (1-α)·S_{t-1}` (α=0.2 HU-03), simulación ruido HC-SR04, KPI >85%, barrido α Monte Carlo 100×, banco físico UNO, t-Student plan | TS4D3 Estadística (Sprint 1-4) — RNF-2.1, RNF-2.2, HU-03 | `jupyter notebook notebooks/EMA_Estadistica.ipynb` |

## 🗂️ Estructura del proyecto

```
notebooks/                      ← ESTÁS AQUÍ (centralizado)
├── AetherControl_Notebook.ipynb  ← Kotlin / Android / MVVM
├── EMA_Estadistica.ipynb        ← Python / EMA / Estadística
└── README.md                    ← este archivo

stats/notebooks/EMA_Estadistica.ipynb  ← copia espejo (no editar, usa la de notebooks/)
docs/notebooks/README.md               ← redirección
```

## ▶️ Cómo ejecutar

Desde la **raíz** del repo:

```bash
# Opción 1: Jupyter Lab
jupyter lab notebooks/

# Opción 2: Notebook clásico
jupyter notebook notebooks/AetherControl_Notebook.ipynb
jupyter notebook notebooks/EMA_Estadistica.ipynb

# Opción 3: VS Code — abre el .ipynb directo
code notebooks/AetherControl_Notebook.ipynb
```

> **Nota de paths:** Ambos notebooks están parcheados para funcionar tanto si los abres desde `notebooks/` como desde `stats/notebooks/`. Detectan automáticamente `stats/ema_filter.py` y `app/src/...`.

## 🔗 Referencias

- `docs/prd.md`, `docs/requirements.md`, `docs/architecture.md`
- `app/src/main/java/com/aethernet/aethercontrol/` — código Android real
- `stats/ema_filter.py`, `stats/visualize_ema.py`, `stats/data/`

---
*Última organización: centralizado en `notebooks/` (raíz) para evitar dispersión.*
