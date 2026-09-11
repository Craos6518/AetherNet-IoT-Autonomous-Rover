# Stats — Análisis Estadístico AetherNet

> **Autor:** Andres Felipe Martinez Henao
> **Experiencia:** 2 años electrónica y Arduino | 1 año Programación C | 2 años Python | 2 años HTML/CSS/JS/React | 1 año PostgreSQL
> **Materia:** TS4D3 Estadística — `docs/UTP/2344_TS4D3 Estadística.pdf` | RNF-2.1, RNF-2.2, HU-03, KPI PRD >85%
> **Stack:** Python 3.11+ (Pandas, NumPy, SciPy, Matplotlib) + PostgreSQL + Arduino C (EMA portado) — 100% FOSS (RNF-3.1)
> **Estado:** Sprint 1-2 ✅ (EST-01 prototipo + EST-08 barrido α + 14 tests + banco físico UNO) | Sprint 3-4 ⏳ (t-Student, descriptivo, KY-037)

---

## 0. Cómo leer este módulo (si vienes de web/electrónica)

Si vienes de **React/JS**, piensa en `stats/` como el `utils/` que valida con datos si tu filtro de sensores funciona antes de meterlo al firmware. En vez de `useState` aquí hay `EMAFilter` con `alpha`, y en vez de `fetch` hay `psycopg2` leyendo PostgreSQL.

Si vienes de **electrónica/Arduino (C)**, aquí está el gemelo en Python del `EMA α=0.2` que corre en `rover-uno.ino:66` y `test-ema-uno.ino:14`. Mismo `S_t = α·Y_t + (1-α)·S_{t-1}` pero con capacidad de simular 100 Monte Carlo, graficar y medir KPI sin flashear el UNO cada vez.

Si vienes de **PostgreSQL**, aquí se cierra el ciclo: `Gateway → HTTP POST → FastAPI → PostgreSQL (sensor_events)` → `stats/db_extract.py (futuro EST-10)` → Pandas → `t-Student` → informe.

Este README cubre **todo** lo que hay en `stats/` archivo por archivo, con `ruta:línea` para ir directo en VS Code.

---

## 1. Mapa de carpetas

```
stats/
├── ema_filter.py              # 164 líneas — núcleo EMA, MultiSensor, simulación y métricas
├── requirements.txt           # 8 deps pinneadas (pandas 2.2.2, numpy 1.26, scipy 1.13, etc.)
├── pyproject.toml             # 50 líneas — proyecto, pytest, ruff, mypy (FOSS)
├── tests/
│   └── test_ema_filter.py     # 178 líneas — 14 tests (KPI >85%, rampa, escalón, multi-sensor)
├── experiments/
│   └── alpha_sweep.py         # 184 líneas — EST-08 Monte Carlo 100× para justificar α=0.2
├── visualize_ema.py           # 140 líneas — CLI matplotlib (single vs compare, CSV/PNG)
├── serial_plot_ema.py         # 150 líneas — puente Serial 115200 UNO → gráfica viva + CSV
├── notebooks/                 # ⚠️ espejo de notebooks/ (canónico) — ver notebooks/README.md
│   ├── EMA_Estadistica.ipynb  # espejo de notebooks/EMA_Estadistica.ipynb — no editar, editar canónico
│   └── README.md              # explica espejo → notebooks/
└── data/
    ├── ema_demo.json           # Demo 20 muestras α=0.2, σ8, 89.3% reducción (generado por ema_filter.py:main)
    ├── ema-real-531.csv        # 531 muestras reales UNO 30s (serial_plot_ema.py --seconds 30)
    ├── alpha_sweep.json        # Resultados Monte Carlo 6 α (ver §6)
    └── alpha_sweep.png         # Gráfica 3 paneles (reducción / retardo / falsos)
```

**Principio aprendido con 2 años Python:** un `utils` bien testeado te ahorra re-flashear el UNO 20 veces. Aquí prototipo offline (Sprint 1-2) y solo cuando pasa KPI lo porto a C.

---

## 2. Fundamento teórico — EMA (lo que vi en clase y apliqué)

**Definición** (`ema_filter.py:5` y `notebooks/EMA_Estadistica.ipynb §2`):

```
S_0 = Y_0                         # primer valor inicializa (EMAFilter:27)
S_t = α · Y_t + (1-α) · S_{t-1}   # α = 0.2 (HU-03, RNF-2.1)
```

- **α bajo 0.05-0.1:** mucho suavizado, retardo grande (21 / 10 muestras hasta cruzar umbral 30 cm). Como un `debounce` muy largo en JS — filtra todo pero llegas tarde.
- **α alto 0.5-0.8:** poco suavizado, rápido (1 muestra) pero no filtra. Como sin `debounce` — reaccionas a cada pico.
- **α=0.2 (HU-03):** 20% muestra actual + 80% historia. Balance validado en `alpha_sweep.py:113` — 82.9% con picos / 89.3% sin picos, retardo 4.3 muestras (~43 ms a 100 Hz), falsos 0.03/100.

**Métricas:**

```
MSE = (1/n) Σ(Y_i - T_i)²          # error cuadrático medio vs true 50 cm
Reducción % = (1 - MSE_filt/MSE_raw)·100   # KPI >85% prd.md:51
Retardo = muestras desde escalón 50→20 hasta cruzar umbral 30 (rover OBSTACLE_DISTANCE_CM)
Falsos = filt <30 cuando true=50 (seguridad — frenar sin obstáculo real)
```

**Por qué EMA y no mediana/Kalman:** EMA es **O(1)** memoria y cómputo — una variable `_state` y una multiplicación. Para UNO con 2 KB RAM a 100 Hz es perfecto. Mediana necesita buffer, Kalman matrices — fuera de alcance Sprint 1, queda como discusión en `notebooks §8` e informe final.

**C ↔ Python port:** Idéntico en `firmware/rover-uno/rover-uno.ino:259` (`ultrasonicEma = 0.2*raw + 0.8*previa`) y `firmware/test-ema-uno/test-ema-uno.ino:42`. Lo que valido aquí con `seed=42` reproducible, lo flasheo al UNO y veo `raw,ema` en Serial Plotter.

---

## 3. `ema_filter.py:1` — Núcleo (164 líneas)

### 3.1 `EMAFilter:15` — `@dataclass` con validación

```python
@dataclass
class EMAFilter:  # como un hook useEMA(alpha) en React, pero en Python
    alpha: float = 0.2
    _state: Optional[float] = None
    _initialized: bool = False

    def __post_init__(self):  # valida (0,1] — como propTypes en React
        if not 0 < self.alpha <= 1:
            raise ValueError("Alpha must be in (0, 1]")

    def update(self, value: float) -> float:  # el corazón — como setState con filtro
        if not self._initialized:
            self._state = value  # S_0 = Y_0
            self._initialized = True
            return value
        self._state = self.alpha * value + (1 - self.alpha) * self._state  # S_t
        return self._state
```

- **`reset():35`** y **`current_value:41` / `is_initialized:46`** — getters como en C `isDoorUnlocked()`, útiles para tests y para reiniciar entre experimentos.
- **Python 2 años:** usar `@dataclass` evita boilerplate `__init__`, y `Optional[float]` deja claro que `None` es estado no inicializado (como `null` en JS).

### 3.2 `MultiSensorEMA:50` — Diccionario por sensor

```python
class MultiSensorEMA:  # como un Map<sensorId, EMAFilter> en JS
    def __init__(self, alpha=0.2):
        self.filters: dict[str, EMAFilter] = {}  # uno por HC-SR04 vs KY-037

    def get_filter(self, sensor_id: str) -> EMAFilter:  # lazy create
        if sensor_id not in self.filters:
            self.filters[sensor_id] = EMAFilter(alpha=self.alpha)
        return self.filters[sensor_id]
```

Aísla HC-SR04 (distancia cm) de KY-037 (sonido dB) — cada uno con su `_state`. En firmware sería un array de `EMAFilter` por sensor, aquí es `dict`. `get_state():77` serializa para debug/JSON.

### 3.3 `simulate_noisy_signal:88` — Generador reproducible

```python
def simulate_noisy_signal(true_values, noise_std=5.0, seed=42):
    random.seed(seed)  # reproducible — mismo seed = mismos picos (como seed en RNG de JS)
    return [v + random.gauss(0, noise_std) for v in true_values]  # gauss = Normal(0,σ)
```

Simula HC-SR04 ruidoso: `true=50 cm` + `gauss(0, σ8)`. Con `seed=42` los tests son determinísticos (CI no flaky). Es lo que en electrónica sería inyectar ruido con generador, aquí es Python puro.

### 3.4 `calculate_noise_reduction:99` — KPI cuantitativo

```python
def calculate_noise_reduction(raw_values, filtered_values, true_values):
    raw_mse = statistics.mean((r-t)**2 for r,t in zip(raw, true))
    filtered_mse = statistics.mean((f-t)**2 for f,t in zip(filtered, true))
    reduction_pct = (1 - filtered_mse/raw_mse)*100
    return {"raw_mse":..., "filtered_mse":..., "noise_reduction_pct": reduction_pct, "meets_kpi": reduction_pct>85}
```

- Usa `statistics.mean` (stdlib, FOSS) no NumPy para no depender de heavy deps en CI.
- `meets_kpi:122` es el **KPI PRD >85%** — el test `test_ema_filter.py:118` lo aserta.

### 3.5 `__main__:126` — Demo que genera `data/ema_demo.json:1`

Simula `true 50cm ×100 + σ8`, filtra α=0.2, calcula métricas (89.3% sin picos), guarda `data/ema_demo.json` con `Path(__file__).parent / "data"` (ruta robusta al cwd, fix Sprint 1). Es el `npm run demo` del módulo.

---

## 4. `pyproject.toml:1` y `requirements.txt:1` — Proyecto reproducible

```toml
# pyproject.toml:1
[project]
name = "aethernet-stats"
requires-python = ">=3.11"
dependencies = [pandas 2.2.0, numpy 1.26, scipy 1.13, psycopg2-binary 2.9.9, sqlalchemy 2.0.30, matplotlib 3.8.0]

[tool.pytest.ini_options]  # como jest.config en JS
testpaths = ["tests"]
addopts = "-v --tb=short --cov=. --cov-report=term-missing"

[tool.ruff]               # linter como ESLint — E,F,I,W,UP,B,C4,SIM,T20
line-length = 100

[tool.mypy]               # type checker como TSC — disallow_untyped_defs true
```

- **Pinned en `requirements.txt:1`** (`pandas==2.2.2`, `numpy==1.26.4`) para CI reproducible — igual que pinear `ArduinoJson 6.21.3` en firmware.
- **PostgreSQL 1 año:** `psycopg2-binary` + `sqlalchemy` para `EST-10 db_extract.py` (Sprint 4) — leer `sensor_events` desde FastAPI/Postgres como haría un `fetch` pero con SQL.

**Instalación:**

```bash
cd stats
python -m venv .venv && source .venv/bin/activate  # como nvm use
pip install -r requirements.txt          # o pip install -e .[dev] con pyproject.toml
```

---

## 5. `tests/test_ema_filter.py:1` — 14 tests (178 líneas, 90% coverage)

### 5.1 `TestEMAFilter:7` — 6 tests básicos

| Test | Qué valida | Clave |
|---|---|---|
| `test_initialization:10` | `alpha=0.2`, `current_value None`, `not initialized` | estado inicial como `isDoorUnlocked() == false` |
| `test_invalid_alpha:16` | `0.0`, `1.5`, `-0.1` → `ValueError` | como `propTypes` — falla rápido si α malo |
| `test_first_value_initializes:24` | `update(100)` → `100`, `is_initialized True` | `S_0 = Y_0` |
| `test_constant_signal:31` | 50 constante ×10 → sigue 50 exacto | sin drift |
| `test_step_response:38` | α=0.5 escalón 0→100, 50 luego >90 | converge exponencial, no salto (como animación ease) |
| `test_reset:52` | `update` luego `reset()` → `None`, `update(10)==10` | como `reset()` en C `ultrasonicInitialized=false` |

### 5.2 `TestMultiSensorEMA:62` — 4 tests aislamiento

`test_independent_filters:65` verifica `sensor1 10` y `sensor2 20` no se mezclan — cada uno su `EMAFilter`. `test_get_state:87` serializa `{value, initialized}` para debug.

### 5.3 `TestNoiseReduction:96` — KPI

```python
def test_noise_reduction_kpi:99():
    true = [50]*200
    noisy = simulate_noisy_signal(true, noise_std=10.0, seed=123)  # ruido fuerte
    filt = [EMAFilter(0.2).update(v) for v in noisy]
    m = calculate_noise_reduction(noisy, filt, true)
    assert m["meets_kpi"]  # >85% — si falla, el filtro no sirve en Rover
```

Imprime `Raw MSE / Filtered MSE / Reduction %` para ver en CI. `test_different_alphas:121` itera α 0.1/0.2/0.3/0.5 y loguea — útil para elegir α.

### 5.4 `TestSimulatedSensorData:133` — Patrones reales

- **`test_ultrasonic_approach:136`** — rampa 100→10 cm en 50 muestras (objeto acercándose). Valida varianza reducida y lag <15 cm. Con señal dinámica MSE no sirve (EMA tiene lag inherente), así que se mide seguimiento.
- **`test_sudden_obstacle:155`** — escalón 100→20 cm, debe cruzar 50 en ≤10 muestras (α=0.2 tarda ~4). Si no detecta en 10, el Rover chocaría.

**Ejecución:**

```bash
cd stats && pytest tests/ -v                  # 14 passed
pytest --cov=. --cov-report=term-missing      # 90% coverage
```

Todos verdes en `feature/stats-ema-prototipo` (`notebooks/EMA_Estadistica.ipynb` celda 4).

---

## 6. `visualize_ema.py:1` — CLI Matplotlib (140 líneas)

Reutiliza exactamente `EMAFilter`/`simulate_noisy_signal` de `ema_filter.py:5` — sin duplicar lógica (DRY). Como un `Chart.js` en Python.

**Modos:**

```bash
python stats/visualize_ema.py                          # single α=0.2, 50cm σ8
python stats/visualize_ema.py --alpha 0.5 --seed 42    # otro α
python stats/visualize_ema.py --compare                # 5 α superpuestos (0.1/0.2/0.3/0.5/0.8)
python stats/visualize_ema.py --live-obstacle          # rampa 100→10 cm (test_ultrasonic_approach)
python stats/visualize_ema.py --save docs/fritzing/ema-demo.png
python stats/visualize_ema.py --csv stats/data/ema_demo.csv
```

**Qué hace `plot_single:38`:** genera `true` (50 constante o rampa 100→10), `noisy` gauss, `filt` EMA, calcula `metrics`, imprime `Raw MSE / Filtered MSE / Reducción / KPI`, grafica `True -- gris`, `Raw rojo α0.6`, `EMA azul`, línea umbral rover 30 cm naranja punteada (`visualize_ema.py:73`), y marca detección si `filt` cruza 30.

**Qué hace `plot_compare:92`:** misma `noisy` para 5 α, superpone 5 curvas EMA con label `α=0.2 (89%)`. Ves de golpe que α=0.1 suaviza más pero tarda, α=0.8 casi no filtra.

**Experiencia React:** es como un `LineChart` con `recharts` pero con Matplotlib — `fig, ax = plt.subplots(figsize=(10,4.2))` es el `ResponsiveContainer`, `ax.plot` son las series.

**PNGs generados (ya en `docs/fritzing/`):** `ema-demo.png` (89.3%), `ema-compare.png`, `ema-obstacle.png` — usados en notebook §5.

---

## 7. `serial_plot_ema.py:1` — Puente Serial UNO → Python (150 líneas)

> **Lee `Serial 115200` de `firmware/test-ema-uno/test-ema-uno.ino:36` (`raw,ema`) y grafica en vivo. Es el `Serial Plotter` de Arduino IDE pero con superpoderes (CSV + PNG).**

**Uso (requiere `pyserial` + `matplotlib`):**

```bash
/tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0
/tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0 --seconds 30 --save stats/data/ema-real-531.csv --png docs/fritzing/ema-real-531.png
/tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0 --window 200
```

- **`--port /dev/ttyACM0`** — `arduino-cli board list` te dice el puerto (como `ls /dev/tty*`).
- **`--seconds 0` infinito** (ventana viva con `FuncAnimation` 50 ms, `deque(maxlen=window)` como ring buffer en JS).
- **`--seconds 30` captura** — bloquea 30s, guarda `time_s,raw,ema` CSV y PNG `raw rojo vs ema azul + umbral 30` (ver `serial_plot_ema.py:81`).

**Cómo funciona `main:26`:** abre `serial.Serial(port, 115200, timeout=1)`, espera 2s (UNO resetea al abrir Serial), `reset_input_buffer()`, loop `readline().decode().strip()`, ignora `raw,ema` header y `===`, parsea `raw,ema` float, `elapsed = time.time()-t0`, guarda en `deque` (vivo) y `all_rows` (CSV). Con `--seconds` termina, hace `plt.plot(times, raws) / emas` y `savefig`.

**Dato real ya capturado:** `stats/data/ema-real-531.csv:1` — 531 muestras 30s, `raw` y `ema` con `α=0.2` idéntico a `ema_filter.py:17` y `rover-uno.ino:66`. Ver §9.

**Electrónica 2 años:** este script es el `osciloscopio` del HC-SR04 — ves `raw` ruidoso vs `ema` estable sin tocar el Rover.

---

## 8. `experiments/alpha_sweep.py:1` — EST-08 Monte Carlo 100× (184 líneas)

> **Barrido de α con justificación cuantitativa para `AGENTS.md:4` (cambiar α requiere confirmación humana).**

Para cada `α ∈ {0.05,0.1,0.2,0.3,0.5,0.8}` hace `N_MC=100` trials de:

1. **Reducción %** con `50cm + σ8 + 3% picos 150cm` (`inject_spikes:29`)
2. **Retardo** a escalón `50→20` hasta cruzar umbral 30 (`THRESHOLD_CM:26` = `rover OBSTACLE_DISTANCE_CM 30`)
3. **Falsos obstáculos** `filt <30` cuando `true=50`

**Salidas versionables:**

```bash
python stats/experiments/alpha_sweep.py
# → stats/data/alpha_sweep.json (conclusion + recommended_alpha)
# → stats/data/alpha_sweep.png + docs/fritzing/alpha_sweep.png (3 paneles bar)
```

**Resultados (`stats/data/alpha_sweep.json:1`, resumido en `notebooks §6`):**

| α | Reducción mean | Retardo mean (muestras 10ms) | Falsos /100 | KPI >85% |
|---|---|---|---|---|
| 0.05 | 94.2% | 21.0 (210 ms) | 0.05 | ✓ pero inseguro (lento) |
| **0.1** | **92.8%** | **9.9 (99 ms)** | **0.0** | **✓** |
| **0.2** | **82.9%** con picos / **89.3%** sin picos | **4.3 (43 ms)** | **0.03** | ✗ con picos / **✓ sin picos** |
| 0.3 | 78.7% | 2.8 (28 ms) | 0.02 | ✗ |
| 0.5 | 64.2% | 1.1 (11 ms) | 0.0 | ✗ |
| 0.8 | 32.3% | 0.1 (1 ms) | 0.12 | ✗ |

**Conclusión `alpha_sweep.json:152`:** `α=0.2 se sostiene` — balance entre reducción 82.9% (89.3% sin picos), retardo 4.3 muestras (~43 ms a 100 Hz, ~6 cm invasión a 1.4 m/s), y 0.03 falsos/100. α=0.1 filtra más (92.8%) pero tarda 9.9 muestras (+56 ms → +8 cm invasión). α=0.5 pierde KPI (64.2%). Heurística `alpha_sweep.py:116` elige 0.2 si `KPI>85 && delay≤6 && falsos<1`, si no fallback 0.2.

**Gráfica `alpha_sweep.png:137`:** 3 barras `Reducción %` (KPI 85 verde `--`), `Retardo muestras`, `Falsos /100` — copia en `docs/fritzing/`.

**Conexión con firmware:** cambiar `EMA_ALPHA 0.2f` en `rover-uno.ino:66` es cambiar umbral de seguridad — por eso `AGENTS.md:4` exige confirmación humana. Este script decide si 0.2 se sostiene (sí).

---

## 9. `notebooks/EMA_Estadistica.ipynb:1` — Bitácora viva TS4D3 (10 celdas, **canónica en `notebooks/`**)

> **Materia Estadística UTP → Proyecto. Todos los números reproducibles (`notebooks §9`). Centralizada en `notebooks/` (ver `notebooks/README.md` y `docs/notebooks/README.md`). `stats/notebooks/` es espejo.**

**Índice (`notebooks §1`):** 1 UTP→Proyecto, 2 Teoría EMA, 3 Impl Python, 4 Tests, 5 Viz, 6 Barrido α, 7 Banco UNO, 7b Datos reales 531, 8 Roadmap Sprint 3-4, 9 Reproducibilidad.

**§1 Mapeo UTP (tabla):**

| Unidad UTP | Tema | Aplicación AetherNet | Sprint | Evidencia |
|---|---|---|---|---|
| U1 Descriptiva | media, varianza, DE, hist, outliers >3σ | Caracterizar ruido HC-SR04/KY-037 | 4 | `descriptive.py` EST-06 |
| U2 Probabilidad | Normal, `random.gauss` | `simulate_noisy_signal` σ8 | 1-2 | `ema_filter.py:88` |
| U3 Series temporales | EMA `α=0.2`, trade-off, Kalman | Núcleo filtro tiempo real | 1-2 | `ema_filter.py:15`, `rover.ino:66` |
| U4 Hipótesis | t-Student Welch `H0: μRF=μWiFi`, Shapiro+Levene, IC95%, d Cohen | Latencia RF vs Wi-Fi | 4 | `hypothesis_tests.py` EST-05 |
| U4 Métricas | MSE, reducción % KPI >85% | `calculate_noise_reduction` | 1-2 | `ema_filter.py:99` |

**§2-3 Teoría + código** con celdas ejecutables: imports `sys.path.insert(0, ...)`, demo KPI 89.3% (`notebooks celda 2`), constante y escalón (`celda 3`), tests `pytest` 14 passed (`celda 4`), plots inline `visualize_ema.py` recodificado en notebook (`celda 5-6`), PNGs `docs/fritzing/` (`celda 7`), JSON barrido (`celda 8`), CSV real 531 con `pandas describe()` y proxy KPI (`celda 10`).

**§7-7b Banco físico:** wiring `TRIG→D2 ECHO→D3` (`test-ema-uno.ino:9`), flasheo `arduino-cli compile/upload/monitor`, Fritzing `docs/fritzing/ema-uno-esquematico.md`, captura real `serial_plot_ema.py --seconds 30` → `ema-real-531.csv:1` (532 líneas) y `docs/fritzing/ema-real-531.png` (rampa 3→26 cm en 30s, `raw std 3-4 cm` vs `ema std 3 cm`, 0 cruces 30 cm, proxy reducción ~60% porque ruido real interior es bajo — no hay picos 150 cm).

**§8 Roadmap Sprint 3-4 (dependencias `docs/materias/backlog-estadistica.md:84`):**

```
EST-01 ✅ → EST-08 (α) ──────────────┐
DEVOPS-12 (bridge BD) → EST-10 ─────┼→ EST-06 → EST-07 (informe)
DEVOPS-13 (tramas)    → EST-09      │
KY-037 físico        → EST-11 → EST-02 ┘
```

| ID | Tarea | Qué hacer | Salida |
|---|---|---|---|
| EST-09 | Latencias t-Student | Acordar `t_app/t_gw_rx`, capturar 50 pares RF vs Wi-Fi | `latency_rf.csv` / `latency_wifi.csv` |
| EST-10 | Conector PG→Pandas | `stats/db_extract.py::load_sensor_events()` vía `DATABASE_URL` | DataFrame |
| EST-11 | KY-037 previo | 60s silencio + palmada → hist → `abs(desv-baseline)` antes de EMA | PNG + decisión |
| EST-02 | EMA KY-037 firmware | Replicar `rover.ino:259` en sketch micrófono | CI verde |
| EST-05 | t-Student | Welch + supuestos, IC95%, d Cohen, fallback Mann-Whitney | `hypothesis_tests.py` |
| EST-06 | Descriptivo | `sensor_summary.md` medias, varianzas, histogramas | Reporte |
| EST-07 | Informe final | Plantilla tipificada | PDF |

**§9 Reproducibilidad:** `seed 42` en `simulate_noisy_signal`, `N_MC 100` en `alpha_sweep.py:23`, `seed=42*1000+alpha*100+t*17` determinístico, `Path(__file__).parent` ruta robusta, `pip freeze` en `requirements.txt`.

---

## 10. `data/` — Artefactos versionables

| Archivo | Origen | Qué contiene | Uso |
|---|---|---|---|
| `ema_demo.json:1` | `ema_filter.py:main` | 20 muestras `true 50`, `noisy σ8`, `filtered α0.2`, `metrics 89.3% KPI True` | Demo repro, validación PRD |
| `ema-real-531.csv:1` | `serial_plot_ema.py -p /dev/ttyACM0 --seconds 30` + UNO físico | 531 filas `time_s,raw,ema` (16→3→26 cm rampa 30s, `test-ema-uno.ino:36` 115200) | Baseline real Sprint 2 |
| `alpha_sweep.json:1` | `experiments/alpha_sweep.py:109` | `sigma 8, n_mc 100, threshold 30, results 6 α, conclusion, recommended_alpha 0.2` | Justificación α `AGENTS.md:4` |
| `alpha_sweep.png` | `alpha_sweep.py:167` | 3 paneles `Reducción / Retardo / Falsos` | `docs/fritzing/alpha_sweep.png` copia |
| `ema-demo.png`, `ema-compare.png`, `ema-real-531.png` | `visualize_ema.py` / `serial_plot_ema.py` | PNGs Matplotlib (Fritzing) | Informe y notebook §5/7b |

**PostgreSQL 1 año:** `sensor_events` en `backend/app/models.py` (tabla con `value`, `filtered_value`, `unit cm`) es la fuente futura para `EST-10` — aquí simulamos con `gauss` hasta tener datos reales en BD.

---

## 11. Instalación, ejecución y CI

```bash
# Entorno (como nvm + npm install en React)
cd stats
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt          # deps pinneadas
# o
pip install -e .[dev]                   # desde pyproject.toml + ruff/mypy/pytest-cov

# Demo EMA
python ema_filter.py                     # → data/ema_demo.json + consola 89.3%

# Tests (14, KPI >85%)
pytest tests/ -v                         # todos verdes
pytest --cov=. --cov-report=term-missing # 90% coverage (tool.pytest.ini_options:34)

# Lint + type (como ESLint + TSC)
ruff check . && ruff format --check .    # tool.ruff:39 line-length 100
mypy .                                   # tool.mypy:46 disallow_untyped_defs

# Visualizaciones
python visualize_ema.py                  # single α0.2
python visualize_ema.py --compare        # 5 α
python visualize_ema.py --live-obstacle  # rampa 100→10
python visualize_ema.py --save docs/fritzing/ema-demo.png --csv data/ema_demo.csv

# Barrido α (EST-08)
python experiments/alpha_sweep.py        # → data/alpha_sweep.json/.png + copy docs/fritzing/

# Banco físico UNO (requiere firmware/test-ema-uno flasheado)
arduino-cli compile --fqbn arduino:avr:uno firmware/test-ema-uno
arduino-cli upload -p /dev/ttyACM0 --fqbn arduino:avr:uno firmware/test-ema-uno
python serial_plot_ema.py -p /dev/ttyACM0                              # vivo
python serial_plot_ema.py -p /dev/ttyACM0 --seconds 30 --save data/ema-real-531.csv --png docs/fritzing/ema-real-531.png

# Notebook (canónico centralizado)
jupyter lab notebooks/EMA_Estadistica.ipynb  # canónico — celdas 1-10 reproducibles (ver docs/notebooks/README.md)
# espejo: jupyter lab stats/notebooks/EMA_Estadistica.ipynb  # solo compatibilidad
```

**CI (`.github/workflows/ci.yml:stats-test`):** `pip install pytest pytest-cov`, `pytest -v --tb=short` con `pythonpath stats/.` — valida 14 tests en cada push (RNF-1.2). Notebook centralizado en `notebooks/`.

---

## 12. Conexión con firmware y backend (cierre del ciclo)

```
HC-SR04 (UNO D2/D3) ──NewPing──► raw cm ──EMA α0.2──► ultrasonicEma
        │ firmware/test-ema-uno:36 raw,ema Serial 115200 ──► serial_plot_ema.py ──► data/ema-real-531.csv
        │ firmware/rover-uno:259 S_t = 0.2·Y_t + 0.8·S_{t-1} ──► RoverTelemetry.ultrasonic_cm (RF) ──► Gateway ──► MQTT aethernet/rover/telemetry
        │                                                                                         │
        │                                                                                         ▼
        │ stats/ema_filter.py:32 ←── prototipo offline (simula gauss) ──┐              FastAPI POST /api/sensor-events
        │ stats/tests/test_ema_filter.py:99 ←── valida KPI >85% ────────┤              PostgreSQL sensor_events
        │ stats/experiments/alpha_sweep.py ←── justifica α=0.2 ─────────┘                     │
        │ notebooks/EMA_Estadistica.ipynb (canónico, stats/notebooks/ espejo) ←── bitácora TS4D3     ▼
        │                                                                              stats/db_extract.py (EST-10 futuro)
        │                                                                              Pandas + SciPy t-Student (EST-05)
        └────────────────────────────────────────────────────────────────────────────────────► Informe EST-07 (PDF)
```

**HTML/CSS/JS/React 2 años:** si `ema_filter.py` fuera un componente React, `EMAFilter` sería el `useState` con `alpha` como prop, `MultiSensorEMA` un `Context` por sensor, y `calculate_noise_reduction` un `useMemo` que deriva KPI.

**C 1 año:** `ema_filter.py:32` y `rover-uno.ino:259` son la misma fórmula — una en Python con `float` y otra en C con `float` + `bool initialized`. Validar aquí ahorra flashear C 20 veces.

**PostgreSQL 1 año:** `psycopg2-binary` + `sqlalchemy` en `pyproject.toml:11` son el `pg` + `prisma` de Python — `EST-10` hará `SELECT value, filtered_value FROM sensor_events WHERE sensor_type='ultrasonic'` para t-Student real (RNF-2.2).

---

## 13. Referencias cruzadas (para evaluadores)

- `docs/prd.md:51` KPI reducción >85% con `S_t=α·Y_t+(1-α)·S_{t-1}`
- `docs/requirements.md:42-44` RNF-2.1 (EMA HC-SR04/KY-037), RNF-2.2 (histórico), HU-03 α=0.2
- `docs/architecture.md:72-84` flujo `sensor → EMA firmware → MQTT → PostgreSQL → stats → t-Student`
- `docs/roadmap.md:86` Estadística (EMA, t-Student Welch, Pandas/SciPy, FOSS)
- `docs/sprints.md:40` Sprint 4 (EMA, Node-RED, Tuya cancelado), estado `EST-01 ✅ + EST-08 ✅`
- `firmware/rover-uno/rover-uno.ino:66,259` y `firmware/test-ema-uno/test-ema-uno.ino:14,42` — port C de EMA
- `backend/app/models.py` + `routers/events.py` — `sensor_events` (fuente EST-10)
- `AGENTS.md:4` umbral seguridad — cambiar α requiere confirmación humana (justificado en `alpha_sweep.json`)

---

## 14. Glosario (si vienes de web)

| Término stats | Equivalente web | Qué es |
|---|---|---|
| `EMA α=0.2` | `debounce(200ms)` + `useMemo` suavizado | Media móvil exponencial — 20% nuevo, 80% historia |
| `MSE` | `Math.pow(error,2)` mean | Error cuadrático medio vs true |
| `Reducción %` | `1 - mseFilt/mseRaw` | KPI >85% (PRD) |
| `Monte Carlo 100×` | `for(i=0;i<100;i++) test()` | Repetir con seeds distintos para media/mediana/SD |
| `seed 42` | `Math.seedrandom(42)` | Reproducibilidad — mismo ruido cada run |
| `psycopg2` | `pg` en Node | Driver PostgreSQL en Python |
| `Matplotlib` | `Chart.js` / `recharts` | Gráficas `plt.plot`, `axhline`, `bar` |
| `pytest --cov` | `jest --coverage` | Tests + cobertura (90% aquí) |

---

*Documentado como estudiante 6º semestre que prototipa en Python (Pandas/SciPy, `pytest --cov`, `ruff`/`mypy`) lo que luego porto a C en `rover-uno.ino:259`, valido en banco físico UNO con `serial_plot_ema.py` y cierro el ciclo con PostgreSQL (`sensor_events`) para t-Student. Cada `seed` reproducible, cada α justificado con Monte Carlo, cada PNG versionado — FOSS de extremo a extremo (RNF-3.1).*
