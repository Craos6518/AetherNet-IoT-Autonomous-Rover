# Notebooks Stats — Espejo Centralizado

> Todos los notebooks viven en `notebooks/` (raíz). Este `stats/notebooks/` es **espejo de compatibilidad** para no romper imports antiguos (`import sys; sys.path.insert`).

- Canónico: `docs/Estadistica/notebook/EMA_Estadistica.ipynb` — 1:1 con este archivo (sincronizado).
- Canónico App: `docs/Programacion-Movil/notebook/AetherControl_Notebook.ipynb` (no duplicado aquí).

**No edites este espejo** — edita `docs/Estadistica/notebook/EMA_Estadistica.ipynb` y sincroniza:

```bash
cp docs/Estadistica/notebook/EMA_Estadistica.ipynb stats/docs/Estadistica/notebook/EMA_Estadistica.ipynb
```

Abrir canónico:

```bash
jupyter notebook docs/Estadistica/notebook/EMA_Estadistica.ipynb
jupyter lab notebooks/
```

Ver `docs/README.md` y `docs/docs/README.md`.
