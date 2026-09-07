# Notebooks Stats — Espejo Centralizado

> Todos los notebooks viven en `notebooks/` (raíz). Este `stats/notebooks/` es **espejo de compatibilidad** para no romper imports antiguos (`import sys; sys.path.insert`).

- Canónico: `notebooks/EMA_Estadistica.ipynb` — 1:1 con este archivo (sincronizado).
- Canónico App: `notebooks/AetherControl_Notebook.ipynb` (no duplicado aquí).

**No edites este espejo** — edita `notebooks/EMA_Estadistica.ipynb` y sincroniza:

```bash
cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb
```

Abrir canónico:

```bash
jupyter notebook notebooks/EMA_Estadistica.ipynb
jupyter lab notebooks/
```

Ver `notebooks/README.md` y `docs/notebooks/README.md`.
