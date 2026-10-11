# Datasets — Validación externa del filtro EMA (AetherNet)

> **Ubicación canónica:** `stats/Dataset/` (36.500 filas, 3.7 MB) — espejo en `docs/Estadistica/Datasets/` para entrega académica.
> Ambos datasets ya versionados en el repo, sin descarga adicional. Ver también `docs/Estadistica/Datasets/README.md`.
> **Licencias:** Código del repo → Apache 2.0 (`LICENSE`); estos CSV → **CC BY-SA 4.0** (turbidez) y **CC0** (gestos) — ver [`NOTICE`](../../NOTICE) y sección Licencia abajo. Compatibles FOSS (RNF-3.1) por agregación.

---

## Dataset 1 — Water level identification with distance sensors

> 31.500 filas, 3.4 MB (3 archivos × 10.500)

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

Ver `stats/water_turbidity_analysis.py:1` y `docs/Estadistica/notebook/EMA_Estadistica.ipynb §7c` para reproducciones, y `stats/data/water_turbidity_report.json:1` para métricas versionables.

## Uso (Dataset 1)

```bash
python3 stats/water_turbidity_analysis.py   # → stats/data/water_turbidity_report.json + PNGs
jupyter lab docs/Estadistica/notebook/EMA_Estadistica.ipynb  # §7c
```

No requiere descarga adicional — los 3 CSV ya están versionados en este repo (derivado CC BY-SA 4.0, citar).

---

## Dataset 2 — Hand Gesture Dataset (complementario HC-SR04)

> 5.000 filas, 341 KB — **mismo sensor que AetherNet (HC-SR04)**, con movimiento real en el tiempo.

### Fuente

- **Kaggle:** https://www.kaggle.com/datasets/marisolgil/hand-gesture-dataset
- **Autora:** Marisol Gil Valenzuela (Ing. Industrial y de Sistemas, Univ. de Sonora; maestría en IA e IoT)
- **Licencia:** **CC0 — Dominio público** (sin restricciones, sin atribución obligatoria) — https://creativecommons.org/publicdomain/zero/1.0/ — 100% compatible RNF-3.1 FOSS
- **Cita (DOI):** Marisol Gil. (2026). Hand Gesture Dataset [Dataset]. Kaggle. https://doi.org/10.34740/kaggle/dsv/16239431
- **Fecha:** 2026, versión 1 — 5.000 filas, 5 gestos × 50 fotogramas × 20 Hz

### Contenido

Mediciones **HC-SR04** reales durante 5 tipos de gestos de mano capturados a **20 Hz (50 ms)**:

- `gesto` — 5 niveles: `acercar / alejar / estático cerca / estático lejos / ninguno`
- `mano` — `derecha / izquierda`
- `velocidad_subjetiva` — `lenta / normal / rápida`
- 50 fotogramas por gesto (≈2.5 s), con `velocidad_cm_s`, `aceleracion_cm_s2`, `tendencia` ya calculadas

Columnas (`gesture_dataset.csv:1`):
```
frame_id, timestamp_ms, sujeto_id, mano, velocidad_subjetiva, gesto,
distancia_cm, velocidad_cm_s, aceleracion_cm_s2, tendencia, valido
```
- `distancia_cm` — misma unidad y sensor que `HC-SR04` del Rover (AetherNet)
- `valido` — 1 = lectura válida, 0 = inválida (filtrado)

Diseño: 5.000 filas completas (cumple mínimo 100 observaciones), con variables cualitativas (`gesto`, `mano`, `velocidad_subjetiva`) y cuantitativas (`distancia_cm`, `velocidad_cm_s`).

### Relevancia AetherNet

- **Mismo hardware:** `HC-SR04` idéntico al del Rover — valida EMA α=0.2 sobre sensor real, no solo principio time-of-flight genérico.
- **Movimiento temporal:** único dataset con **serie temporal real** (50 frames @ 20 Hz) — permite medir **retardo de reacción** del EMA ante cambio real de distancia (p. ej. aparición súbita de obstáculo), que el dataset 1 de turbidez **no puede** evaluar (mediciones repetidas sin movimiento).
- **Complemento perfecto:** Dataset 1 (turbidez) → robustez y reducción >85% en volumen grande controlado; Dataset 2 (gestos) → rapidez de respuesta en movimiento real.

### Uso (Dataset 2)

```bash
python3 stats/gesture_analysis.py        # (si existe) → stats/data/gesture_report.json
jupyter lab docs/Estadistica/notebook/EMA_Estadistica.ipynb  # §7d — análisis gestos + retardo EMA
# Análisis directo
python3 -c "import pandas as pd; df=pd.read_csv('stats/Dataset/gesture_dataset.csv'); print(df.groupby('gesto')['distancia_cm'].describe())"
```

Ambos CSV ya versionados en `stats/Dataset/gesture_dataset.csv` y espejo `docs/Estadistica/Datasets/gesture_dataset.csv` (CC0, sin restricciones).
