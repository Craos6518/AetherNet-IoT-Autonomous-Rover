"""
==============================================================================
AetherNet — EMA Filter (Python) | 6º Semestre UTP | RNF-2.1 / HU-03 / KPI >85%
Autor: Est. Tecnología en Desarrollo de Software + Ing. Sistemas (UTP)
Experiencia: 2 años electrónica/Arduino, 1 año C, 2 años Python/JS/React, 1 año PostgreSQL
Materia: TS4D3 Estadística — Series temporales, suavizado exponencial (U3)
FOSS: Python stdlib + SciPy/Pandas (no R propietario) — RNF-3.1
==============================================================================
QUÉ ES ESTO:
  Gemelo en Python del filtro EMA que corre en C en rover-uno.ino:66 y
  test-ema-uno.ino:14. Misma fórmula S_t = α·Y_t + (1-α)·S_{t-1} con α=0.2,
  pero aquí puedo simular 100 Monte Carlo, medir KPI >85% y graficar sin
  flashear el UNO cada vez. Es el prototipo offline EST-01.

  Si vienes de C/Arduino (1 año C, 2 años electrónica): piensa en este
  EMAFilter como el float ultrasonicEma + bool ultrasonicInitialized de
  rover.ino, pero con clase y validación. En C es manual, aquí es @dataclass.

  Si vienes de React/JS (2 años): EMA es como un useState con suavizado —
  alpha 0.2 es 20% nuevo valor + 80% historia, como un debounce suave.

  Si vienes de PostgreSQL (1 año): los sensor_events de FastAPI/Postgres
  se leerán con EST-10 y se filtrarán con este mismo EMA para t-Student.

Fórmula (prd.md:51, architecture.md:75, requirements.md:42):
  S_t = α * Y_t + (1 - α) * S_{t-1}  con α = 0.2 (HU-03)

Usado en AetherNet para:
  - HC-SR04 ultrasónico (distancia cm, ruido ±8 cm, picos 150 cm)
  - KY-037 micrófono (futuro EST-11, mismo α salvo análisis previo)
"""

from dataclasses import dataclass  # como struct con __init__ auto en C, pero Pythonico
from typing import Optional, List  # tipos como TypeScript — Optional = puede ser None
import json  # para guardar demo en data/ema_demo.json


# ==========================================================================
# EMAFilter — Filtro individual (un sensor, un α)
# ==========================================================================
@dataclass
class EMAFilter:
    """Exponential Moving Average filter con α configurable (como props en React)."""
    # alpha es como una prop — 0.2 por HU-03, pero testeable con 0.1/0.3/0.5 en alpha_sweep.py
    alpha: float = 0.2  # default HU-03 — 20% muestra actual, 80% historia (balance KPI/retardo)
    # _state es el S_{t-1} — como useRef en React, persiste entre update() calls
    _state: Optional[float] = None  # None = no inicializado (como null en JS)
    _initialized: bool = False  # flag como ultrasonicInitialized en rover.ino:108 (C bool)

    def __post_init__(self):
        # Valida α en (0,1] — como propTypes en React o assert en C
        # α=0 no filtra nada (división por cero conceptual), α>1 amplifica ruido
        if not 0 < self.alpha <= 1:
            raise ValueError("Alpha must be in (0, 1]")  # falla rápido si α malo (fail-fast)

    def update(self, value: float) -> float:
        """Actualiza filtro con nueva medición y retorna valor filtrado (como setState)."""
        # Primera llamada: S_0 = Y_0 — inicializa con raw (no hay historia)
        # En C: if (!ultrasonicInitialized) { ultrasonicEma = raw; initialized=true; }
        if not self._initialized:
            self._state = value  # guarda primer raw como estado
            self._initialized = True
            return value  # sin filtrar la primera (no hay previa para promediar)

        # S_t = α·Y_t + (1-α)·S_{t-1} — el corazón, idéntico a rover.ino:259
        # En C: ultrasonicEma = 0.2*raw + 0.8*ultrasonicEma
        # En Python: self._state = 0.2*value + 0.8*self._state
        self._state = self.alpha * value + (1 - self.alpha) * self._state
        return self._state  # retorna filtrado — como filtered_value en sensor_events (PostgreSQL)

    def reset(self) -> None:
        """Resetea estado — como resetear useState a null (útil entre experimentos)."""
        self._state = None
        self._initialized = False  # vuelve a S_0 = Y_0 en próximo update

    @property
    def current_value(self) -> Optional[float]:
        """Get valor filtrado actual sin actualizar — como leer state sin setState."""
        return self._state  # None si no inicializado, float si hay historia

    @property
    def is_initialized(self) -> bool:
        """¿Ya recibió primer valor? — como ultrasonicInitialized en C."""
        return self._initialized


# ==========================================================================
# MultiSensorEMA — Gestor para múltiples sensores (HC-SR04 + KY-037)
# ==========================================================================
class MultiSensorEMA:
    """Maneja múltiples EMAFilters por sensor_id (como Map<sensorId, EMA> en JS)."""

    def __init__(self, alpha: float = 0.2):
        self.alpha = alpha  # α compartido por defecto — todos 0.2 salvo EST-11 KY-037
        # dict[str, EMAFilter] — como { "hc-sr04": EMAFilter(0.2), "ky-037": EMAFilter(0.2) } en JS
        self.filters: dict[str, EMAFilter] = {}

    def get_filter(self, sensor_id: str) -> EMAFilter:
        """Get o crea filtro para sensor — lazy init (como getOrCreate en backend)."""
        if sensor_id not in self.filters:
            self.filters[sensor_id] = EMAFilter(alpha=self.alpha)  # crea con α del gestor
        return self.filters[sensor_id]

    def update(self, sensor_id: str, value: float) -> float:
        """Actualiza filtro de sensor y retorna filtrado — API principal."""
        # Delega a EMAFilter.update() — cada sensor su propia historia, no se mezclan
        return self.get_filter(sensor_id).update(value)

    def reset_sensor(self, sensor_id: str) -> None:
        """Resetea filtro específico — como borrar un doc en Mongo por id."""
        if sensor_id in self.filters:
            self.filters[sensor_id].reset()

    def reset_all(self) -> None:
        """Resetea todos — como truncate tabla en PostgreSQL (pero en memoria)."""
        for f in self.filters.values():
            f.reset()

    def get_state(self) -> dict:
        """Get estado serializable de todos los filtros — para JSON/debug (como GET /state)."""
        return {
            sensor_id: {
                "value": f.current_value,  # filtrado actual o None
                "initialized": f.is_initialized  # bool
            }
            for sensor_id, f in self.filters.items()
        }


# ==========================================================================
# Simulación — Genera señal ruidosa reproducible (como mock data en tests React)
# ==========================================================================
def simulate_noisy_signal(
    true_values: List[float],  # señal verdadera (ej. [50.0]*100 = objeto a 50cm quieto)
    noise_std: float = 5.0,    # σ ruido gaussiano — HC-SR04 real σ~8 cm (ver data/ema-real-531.csv)
    seed: int = 42             # seed para reproducibilidad — mismo seed = mismos picos (determinístico)
) -> List[float]:
    """Genera mediciones ruidosas para testing (como factory de datos en Jest)."""
    import random  # stdlib gauss — no necesita numpy/scipy (FOSS, liviano)

    random.seed(seed)  # fija RNG — CI determinístico, no flaky (como seed en faker.js)
    # Cada true + gauss(0, σ) — simula HC-SR04: true 50 + ruido ±8 con picos ocasionales
    # En electrónica real, el HC-SR04 mete picos 150 cm por eco falso — aquí gauss los simula
    return [v + random.gauss(0, noise_std) for v in true_values]


# ==========================================================================
# Métricas — Calcula KPI PRD >85% (como aserción de cobertura en Jest)
# ==========================================================================
def calculate_noise_reduction(
    raw_values: List[float],       # ruidoso — lo que mide HC-SR04 crudo (como raw en UNO Serial)
    filtered_values: List[float],  # filtrado — salida EMA (como ema en UNO Serial)
    true_values: List[float]       # verdadero — ground truth simulado (50 cm)
) -> dict:
    """Calcula métricas reducción ruido — KPI cuantitativo (como coverage en pytest)."""
    import statistics  # stdlib mean — no necesita numpy (FOSS, suficiente para n=100)

    # Valida longitudes iguales — como validar que 3 arrays vienen del mismo fetch
    if len(raw_values) != len(filtered_values) or len(raw_values) != len(true_values):
        raise ValueError("All lists must have same length")

    # MSE = mean((valor - true)²) — Mean Squared Error, métrica estándar U4
    # raw_errors: error del ruidoso vs true; filtered_errors: error del filtrado vs true
    raw_errors = [(r - t) ** 2 for r, t in zip(raw_values, true_values)]
    filtered_errors = [(f - t) ** 2 for f, t in zip(filtered_values, true_values)]

    raw_mse = statistics.mean(raw_errors)  # MSE raw — grande por ruido
    filtered_mse = statistics.mean(filtered_errors)  # MSE filtrado — debe ser menor

    # Reducción % = (1 - MSE_filt/MSE_raw)*100 — cuánto atenúa EMA
    # Si raw_mse=49 y filt=5 → reducción 89% (meets_kpi True) — ver data/ema_demo.json:72
    reduction_pct = (1 - filtered_mse / raw_mse) * 100 if raw_mse > 0 else 0

    return {
        "raw_mse": raw_mse,
        "filtered_mse": filtered_mse,
        "noise_reduction_pct": reduction_pct,  # 89.3% con α0.2 σ8 (demo)
        "meets_kpi": reduction_pct > 85  # PRD KPI: >85% — si <85, el filtro no sirve en Rover
    }


# ==========================================================================
# Demo — Ejecutable directo (como npm run demo o python -m stats.ema_filter)
# ==========================================================================
if __name__ == "__main__":
    # Demo con HC-SR04 simulado — objeto a 50cm quieto con ruido σ8 (realista)
    print("=== EMA Filter Demo (α=0.2) ===\n")

    # Señal verdadera: 50cm constante ×100 (como tener cartón a 50cm fijo en banco)
    true_distance = [50.0] * 100
    # Ruidosa: 50 + gauss(0,8) — simula HC-SR04 real (ver test-ema-uno + serial_plot_ema.py)
    noisy_readings = simulate_noisy_signal(true_distance, noise_std=8.0)

    # Aplica EMA α=0.2 — idéntico a rover.ino:259 y test-ema-uno.ino:42
    ema = EMAFilter(alpha=0.2)
    filtered = [ema.update(r) for r in noisy_readings]  # lista filtrada (como map en JS)

    # Métricas KPI — ¿cumple PRD >85%?
    metrics = calculate_noise_reduction(noisy_readings, filtered, true_distance)

    print(f"Raw MSE:          {metrics['raw_mse']:.2f}")
    print(f"Filtered MSE:     {metrics['filtered_mse']:.2f}")
    print(f"Noise reduction:  {metrics['noise_reduction_pct']:.1f}%")
    print(f"Meets KPI (>85%): {metrics['meets_kpi']}")  # True con α0.2 σ8 (si picos, baja a 82%)

    print("\nFirst 10 readings:")
    print(f"{'Raw':>8} {'Filtered':>10} {'True':>8}")
    for r, f, t in zip(noisy_readings[:10], filtered[:10], true_distance[:10]):
        print(f"{r:8.1f} {f:10.1f} {t:8.1f}")  # tabla como console.table en JS

    # Guarda demo para referencia — ruta robusta al cwd (fix Sprint 1: Path(__file__).parent)
    # Antes usaba open("data/ema_demo.json") y fallaba si cwd != stats/ — Path lo arregla
    demo_data = {
        "alpha": 0.2,
        "true_values": true_distance[:20],  # solo 20 para JSON liviano
        "noisy_values": noisy_readings[:20],
        "filtered_values": filtered[:20],
        "metrics": metrics
    }
    from pathlib import Path  # para ruta robusta (como path.join en Node)

    out = Path(__file__).parent / "data" / "ema_demo.json"  # stats/data/ema_demo.json desde cualquier cwd
    out.parent.mkdir(parents=True, exist_ok=True)  # crea stats/data si no existe (como fs.mkdir)
    with open(out, "w") as f:
        json.dump(demo_data, f, indent=2)  # JSON pretty — como JSON.stringify(demo, null, 2) en JS
    print(f"\nDemo data saved to {out}")  # ver data/ema_demo.json:1
