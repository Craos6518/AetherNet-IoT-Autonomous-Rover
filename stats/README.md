# AetherNet Stats Module

Statistical analysis for sensor data: EMA filtering, noise reduction validation, and t-Student hypothesis testing.

## Structure

```
stats/
├── ema_filter.py           # EMA filter implementation (α=0.2)
├── requirements.txt        # Python dependencies
├── pyproject.toml          # Project config
├── tests/
│   └── test_ema_filter.py  # Unit tests + KPI validation
├── data/                   # Generated data files
│   └── ema_demo.json       # Demo output
└── notebooks/              # Jupyter notebooks for analysis (future)
```

## Installation

```bash
cd stats
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## EMA Filter (HU-03 / RNF-2.1)

The Exponential Moving Average filter is implemented in `ema_filter.py`:

```python
from ema_filter import EMAFilter

ema = EMAFilter(alpha=0.2)  # Per PRD requirements
filtered_value = ema.update(raw_sensor_reading)
```

**KPI Target**: >85% noise reduction (validated in tests)

## Running Tests

```bash
cd stats
pytest tests/ -v
```

Tests validate:
- Basic filter functionality
- Noise reduction >85% (PRD KPI)
- Step response characteristics
- Dynamic signal tracking (approaching obstacle)
- Sudden obstacle detection latency

## Statistical Analysis (Sprint 4 / EST-04 to EST-07)

Planned scripts for Sprint 4:

1. **Data Extraction** (`extract_historical.py`): Pull sensor events from PostgreSQL
2. **Descriptive Analysis** (`descriptive_analysis.py`): Means, variances, distributions
3. **t-Student Test** (`t_student_test.py`): RF vs Wi-Fi latency comparison
4. **Report Generation** (`generate_report.py`): Final PDF with results

## Usage in Firmware

The EMA algorithm is ported to C++ in `firmware/rover-uno/src/rover.ino` for real-time ultrasonic filtering:

```cpp
#define EMA_ALPHA 0.2f
float ultrasonicEma = 0;
bool ultrasonicInitialized = false;

// In sensor reading loop:
unsigned int rawDistance = sonar.ping_cm();
if (rawDistance > 0) {
    if (!ultrasonicInitialized) {
        ultrasonicEma = rawDistance;
        ultrasonicInitialized = true;
    } else {
        ultrasonicEma = EMA_ALPHA * rawDistance + (1.0 - EMA_ALPHA) * ultrasonicEma;
    }
}
```

## References

- PRD KPI: "Precisión del Filtro Estadístico > 85%"
- HU-03: Filtrado Estadístico de Telemetría (α = 0.2)
- RNF-2.1: EMA para HC-SR04 y KY-037