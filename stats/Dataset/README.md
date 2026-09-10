# Dataset — Water level identification with distance sensors

> **Ubicación canónica:** `stats/Dataset/` (31.500 filas, 3.4 MB)

## Fuente

- **Kaggle:** https://www.kaggle.com/datasets/caetanoranieri/water-level-identification-with-lidar
- **Paper asociado:** Ranieri, C.M., Foletto, A.V., Garcia, R.D., Matos, S.N., Medina, M.M., Marcolino, L.S. and Ueyama, J., 2024. *Water level identification with laser sensors, inertial units, and machine learning.* **Engineering Applications of Artificial Intelligence**, 127, p.107235. DOI: https://doi.org/10.1016/j.engappai.2023.107235
- **Cita solicitada por autores** (ver JSON `license` en Kaggle `application/ld+json`):
  ```
  Ranieri, C.M., Foletto, A.V., Garcia, R.D., Matos, S.N., Medina, M.M., Marcolino, L.S. and Ueyama, J., 2024.
  Water level identification with laser sensors, inertial units, and machine learning.
  Engineering Applications of Artificial Intelligence, 127, p.107235. doi: 10.1016/j.engappai.2023.107235.
  ```
- **Licencia:** **CC BY-SA 4.0** — https://creativecommons.org/licenses/by-sa/4.0/
  Compatible con `RNF-3.1` (100% FOSS) — requiere **atribución + share-alike**. Incluir cita arriba en cualquier informe derivado (`EST-07`).
- **Fecha:** `2023-10-25`, versión 1, ZIP `721.055 bytes`, `3.137 views / 508 downloads` a 2026-09.
- **Creador:** Caetano Ranieri — https://www.kaggle.com/caetanoranieri

## Contenido

Banco de laboratorio que sincroniza **LiDAR** (`ir_value`, `ir_strength`), **ultrasónico** (`us_value`) e **IMU** (`acc_*`, `gyr_*`) en dispositivo experimental. Variables controladas de forma factorial:

- `angle` — 5 niveles: `0.0 / 2.5 / 5.0 / 7.5 / 10.0` grados
- `water_level` — 7 niveles: `50 / 100 / 150 / 200 / 250 / 300 / 350` mm
- `turbidity` — 3 niveles: `low / medium / high` (un archivo por nivel)

Diseño balanceado: `3 × 5 × 7 × 300 = 31.500` filas (`10500` por archivo). Cada combo `(turbidity, angle, water_level)` tiene exactamente `300` muestras (`n_combos 35`, `unique_counts [300]`).

Columnas (ver `water-level_turbidity-low.csv:1`):
```
id, ir_value, ir_strength, us_value, acc_x, acc_y, acc_z,
gyr_acc_x, gyr_acc_y, gyr_acc_z, gyr_x, gyr_y, gyr_z, angle, water_level
```

## Relevancia AetherNet

- `us_value` ≈ `HC-SR04` (`RNF-2.1`, `HU-03` EMA α=0.2, `rover-uno.ino:259`) — mismo principio time-of-flight.
- `ir_strength` ≈ proxy turbidez/condición ambiental (impacto láser vs niebla/polvo).
- Permite validar offline el pipeline `EMA + descriptivo + t-Student Welch` (`EST-06`, `EST-05` adaptado) previo a captura Rover real (`EST-09` latencias).

Ver `stats/water_turbidity_analysis.py:1` y `notebooks/EMA_Estadistica.ipynb §7c` para reproducciones, y `stats/data/water_turbidity_report.json:1` para métricas versionables.

## Uso

```bash
python3 stats/water_turbidity_analysis.py   # → stats/data/water_turbidity_report.json + PNGs
jupyter lab notebooks/EMA_Estadistica.ipynb  # §7c
```

No requiere descarga adicional — los 3 CSV ya están versionados en este repo (derivado CC BY-SA 4.0, citar).
