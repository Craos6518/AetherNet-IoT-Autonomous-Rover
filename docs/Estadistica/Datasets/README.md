# Datasets — AetherNet Validación Estadística Externa

> **Entrega académica:** `docs/Estadistica/Datasets/` es **espejo** de `stats/Dataset/` (canónico en el repo).
> Ambos datasets ya versionados — no requieren descarga. Licencias 100% FOSS (RNF-3.1).
> **Licencias:** Código del repo → Apache 2.0 (`LICENSE`); estos CSV → **CC BY-SA 4.0** (turbidez) y **CC0** (gestos) — ver [`NOTICE`](../../NOTICE) y [`stats/Dataset/README.md`](../../stats/Dataset/README.md). Compatibles por agregación.

## Resumen rápido

| # | Dataset | Filas | Tamaño | Sensor | Licencia | Fuente Kaggle |
|---|---|---|---|---|---|---|
| 1 | Water level — turbidez | 31.500 (3×10.500) | 3.4 MB | Ultrasónico genérico (us_value ≈ HC-SR04) + LiDAR + IMU | CC BY-SA 4.0 | https://www.kaggle.com/datasets/caetanoranieri/water-level-identification-with-lidar |
| 2 | Hand Gesture | 5.000 | 341 KB | **HC-SR04 idéntico AetherNet** | CC0 Dominio público | https://www.kaggle.com/datasets/marisolgil/hand-gesture-dataset |
| **Total** | — | **36.500** | **3.7 MB** | — | — | — |

Ambos cumplen mínimo 100 observaciones y aportan variables cualitativas + cuantitativas.

---

## Dataset 1 — Water level identification with distance sensors (principal)

### Ficha

- **Título:** Water level identification with distance sensors
- **Creador:** Caetano Ranieri (caetanoranieri) — https://www.kaggle.com/caetanoranieri
- **Fecha:** 2023-10-25, versión 1, ZIP 721 KB, 3.137 views / 508 downloads
- **Paper:** Ranieri et al. (2024). *Water level identification with laser sensors, inertial units, and machine learning.* Engineering Applications of Artificial Intelligence, 127, 107235. https://doi.org/10.1016/j.engappai.2023.107235
- **Licencia:** **CC BY-SA 4.0** — https://creativecommons.org/licenses/by-sa/4.0/ (atribución + share-alike, compatible RNF-3.1 FOSS)
- **Archivos:** `water-level_turbidity-low.csv` (10.500), `-medium.csv` (10.500), `-high.csv` (10.500)

### Diseño experimental

Banco de laboratorio con LiDAR + ultrasónico + IMU sincronizados. Factorial controlado:

- `turbidity`: low / medium / high (un archivo por nivel) — **cualitativa ordinal**
- `angle`: 0.0 / 2.5 / 5.0 / 7.5 / 10.0° (5 niveles) — cuantitativa discreta / factor
- `water_level`: 50 / 100 / 150 / 200 / 250 / 300 / 350 mm (7 niveles) — cuantitativa discreta, **verdad de terreno**
- 300 observaciones por combinación → 3×5×7×300 = **31.500 filas** balanceadas

### Variables (15 columnas)

| Variable | Descripción | Tipo |
|---|---|---|
| `id` | Identificador consecutivo | Cuant. discreta |
| `ir_value` | LiDAR distancia (infrarrojo) | Cuant. |
| `ir_strength` | Intensidad señal LiDAR (proxy turbidez) | Cuant. |
| `us_value` | **Ultrasónico crudo** (time-of-flight ≈ HC-SR04) — **variable principal para EMA** | Cuant. |
| `acc_x/y/z`, `gyr_x/y/z`, `gyr_acc_x/y/z` | IMU 9 ejes | Cuant. |
| `angle` | Inclinación sensor | Cuant. discreta |
| `water_level` | Nivel real agua (referencia para error) | Cuant. discreta |
| `turbidity` | Nivel turbidez (derivada del archivo) | Cual. ordinal |

### Relevancia AetherNet

- `us_value` usa mismo principio time-of-flight que `HC-SR04` del Rover (`RNF-2.1`, `HU-03` α=0.2, `rover-uno.ino:259`).
- Permite calcular error directo vs `water_level` y **reducción >85%** (`calculate_noise_reduction` en `stats/ema_filter.py:99`).
- Tres niveles de turbidez → compara robustez del EMA en condiciones adversas (OE-3 del anteproyecto).

### Uso

```bash
python3 stats/water_turbidity_analysis.py   # → stats/data/water_turbidity_report.json + water_us_vs_true.png
jupyter lab docs/Estadistica/notebook/EMA_Estadistica.ipynb  # §7c
```

---

## Dataset 2 — Hand Gesture Dataset (complementario — mismo HC-SR04)

### Ficha

- **Título:** Hand Gesture Dataset
- **Autora:** Marisol Gil Valenzuela (Ing. Industrial y de Sistemas, Univ. de Sonora; maestría IA/IoT)
- **Fecha:** 2026, DOI https://doi.org/10.34740/kaggle/dsv/16239431
- **Licencia:** **CC0 1.0 Dominio público** — https://creativecommons.org/publicdomain/zero/1.0/ (sin restricciones, compatible FOSS)
- **Archivo:** `gesture_dataset.csv` (5.000 filas, 5001 con header)
- **Captura:** 5 gestos × 50 fotogramas × 20 Hz (≈2.5 s por gesto)

### Contenido

Mediciones **HC-SR04 reales** (mismo modelo del Rover) durante gestos de mano:

- `gesto`: acercar / alejar / estático cerca / estático lejos / ninguno (cual. nominal)
- `mano`: derecha / izquierda (cual. nominal)
- `velocidad_subjetiva`: lenta / normal / rápida (cual. ordinal)
- `sujeto_id`: mgv1, etc. (cual. nominal)

Serie temporal por gesto: 50 frames @ 50 ms, con velocidad/aceleración/tendencia precalculadas.

### Variables (11 columnas)

| Variable | Descripción | Tipo |
|---|---|---|
| `frame_id` | 0–49 por gesto | Cuant. discreta |
| `timestamp_ms` | Marca tiempo (ms) | Cuant. |
| `sujeto_id` | Persona | Cual. nominal |
| `mano` | derecha/izquierda | Cual. nominal |
| `velocidad_subjetiva` | lenta/normal/rápida | Cual. ordinal |
| `gesto` | Tipo gesto (5 niveles) | Cual. nominal |
| `distancia_cm` | **HC-SR04 cm** — **misma unidad que Rover** | Cuant. |
| `velocidad_cm_s` | Derivada distancia | Cuant. |
| `aceleracion_cm_s2` | Derivada velocidad | Cuant. |
| `tendencia` | estable/subiendo/bajando | Cual. ordinal |
| `valido` | 1 válido / 0 no válido | Cual. binaria |

### Por qué es complementario

| Aspecto | Dataset 1 (turbidez) | Dataset 2 (gestos) |
|---|---|---|
| Sensor | Genérico ultrasónico | **HC-SR04 idéntico** |
| Volumen | Grande (31.5k) controlado | Mediano (5k) pero temporal |
| Movimiento | **No** (300 repeticiones estáticas) | **Sí** (50 frames @20Hz) |
| Evalúa reducción >85% | ✅ robusto | ✅ mismo sensor |
| Evalúa retardo/reacción | ❌ | ✅ mide rapidez EMA ante cambio real |

El anteproyecto lo usa para el OE-5: medir retardo del EMA ante obstáculo súbito — situación real del Rover.

### Uso

```bash
python3 -c "import pandas as pd; df=pd.read_csv('docs/Estadistica/Datasets/gesture_dataset.csv'); print(df.groupby('gesto')['distancia_cm'].describe())"
jupyter lab docs/Estadistica/notebook/EMA_Estadistica.ipynb  # §7d
```

---

## Ubicación en el repo

```
stats/Dataset/                          # canónico (usado por código)
├── water-level_turbidity-low.csv       # 10.500 filas
├── water-level_turbidity-medium.csv    # 10.500
├── water-level_turbidity-high.csv      # 10.500
├── gesture_dataset.csv                 # 5.000
└── README.md                           # este doc (versión canónica)

docs/Estadistica/Datasets/              # espejo para entrega académica (esta carpeta)
├── (mismos 4 CSV copiados)
└── README.md                           # este archivo

stats/data/                             # derivados (no datasets)
├── water_turbidity_report.json
├── water_us_vs_true.png / water_ir_by_angle.png
└── ema-real-531.csv (531 muestras propias)
```

Ambos espejos son idénticos (`diff` sin diferencias). El código importa desde `stats/Dataset/`; la entrega académica puede referenciar `docs/Estadistica/Datasets/`.

## Citas para informe EST-07

Incluir en bibliografía:

```
Ranieri, C.M. et al. (2024). Water level identification with laser sensors, inertial units, and machine learning.
Engineering Applications of Artificial Intelligence, 127, 107235. https://doi.org/10.1016/j.engappai.2023.107235
Licencia: CC BY-SA 4.0 — https://creativecommons.org/licenses/by-sa/4.0/

Marisol Gil. (2026). Hand Gesture Dataset [Dataset]. Kaggle. https://doi.org/10.34740/kaggle/dsv/16239431
Licencia: CC0 — https://creativecommons.org/publicdomain/zero/1.0/
```

## Verificación

```bash
wc -l stats/Dataset/*.csv docs/Estadistica/Datasets/*.csv
# 5001 gesture + 10501×3 water = 36504 líneas (36.500 datos + 4 headers)
python3 stats/water_turbidity_analysis.py --check  # valida 31.500 filas balanceadas
```
