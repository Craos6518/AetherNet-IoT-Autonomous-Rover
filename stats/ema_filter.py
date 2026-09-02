"""
Exponential Moving Average (EMA) filter for sensor noise reduction.
Used in AetherNet for HC-SR04 ultrasonic sensor and KY-037 sound sensor.

Formula: S_t = α * Y_t + (1 - α) * S_{t-1}
Where α = 0.2 (as specified in HU-03 / RNF-2.1)
"""

from dataclasses import dataclass
from typing import Optional, List
import json


@dataclass
class EMAFilter:
    """Exponential Moving Average filter with configurable alpha."""
    alpha: float = 0.2
    _state: Optional[float] = None
    _initialized: bool = False

    def __post_init__(self):
        if not 0 < self.alpha <= 1:
            raise ValueError("Alpha must be in (0, 1]")

    def update(self, value: float) -> float:
        """Update filter with new measurement and return filtered value."""
        if not self._initialized:
            self._state = value
            self._initialized = True
            return value

        self._state = self.alpha * value + (1 - self.alpha) * self._state
        return self._state

    def reset(self) -> None:
        """Reset filter state."""
        self._state = None
        self._initialized = False

    @property
    def current_value(self) -> Optional[float]:
        """Get current filtered value without updating."""
        return self._state

    @property
    def is_initialized(self) -> bool:
        return self._initialized


class MultiSensorEMA:
    """Manages multiple EMA filters for different sensors."""

    def __init__(self, alpha: float = 0.2):
        self.alpha = alpha
        self.filters: dict[str, EMAFilter] = {}

    def get_filter(self, sensor_id: str) -> EMAFilter:
        """Get or create filter for sensor."""
        if sensor_id not in self.filters:
            self.filters[sensor_id] = EMAFilter(alpha=self.alpha)
        return self.filters[sensor_id]

    def update(self, sensor_id: str, value: float) -> float:
        """Update sensor filter and return filtered value."""
        return self.get_filter(sensor_id).update(value)

    def reset_sensor(self, sensor_id: str) -> None:
        """Reset specific sensor filter."""
        if sensor_id in self.filters:
            self.filters[sensor_id].reset()

    def reset_all(self) -> None:
        """Reset all filters."""
        for f in self.filters.values():
            f.reset()

    def get_state(self) -> dict:
        """Get current state of all filters for serialization."""
        return {
            sensor_id: {
                "value": f.current_value,
                "initialized": f.is_initialized
            }
            for sensor_id, f in self.filters.items()
        }


def simulate_noisy_signal(
    true_values: List[float],
    noise_std: float = 5.0,
    seed: int = 42
) -> List[float]:
    """Generate noisy measurements for testing."""
    import random
    random.seed(seed)
    return [v + random.gauss(0, noise_std) for v in true_values]


def calculate_noise_reduction(
    raw_values: List[float],
    filtered_values: List[float],
    true_values: List[float]
) -> dict:
    """Calculate noise reduction metrics."""
    import statistics

    if len(raw_values) != len(filtered_values) or len(raw_values) != len(true_values):
        raise ValueError("All lists must have same length")

    raw_errors = [(r - t) ** 2 for r, t in zip(raw_values, true_values)]
    filtered_errors = [(f - t) ** 2 for f, t in zip(filtered_values, true_values)]

    raw_mse = statistics.mean(raw_errors)
    filtered_mse = statistics.mean(filtered_errors)

    reduction_pct = (1 - filtered_mse / raw_mse) * 100 if raw_mse > 0 else 0

    return {
        "raw_mse": raw_mse,
        "filtered_mse": filtered_mse,
        "noise_reduction_pct": reduction_pct,
        "meets_kpi": reduction_pct > 85  # PRD KPI: >85% reduction
    }


if __name__ == "__main__":
    # Demo with simulated HC-SR04 data
    print("=== EMA Filter Demo (α=0.2) ===\n")

    # Simulate distance readings: object at 50cm with noise
    true_distance = [50.0] * 100
    noisy_readings = simulate_noisy_signal(true_distance, noise_std=8.0)

    # Apply EMA filter
    ema = EMAFilter(alpha=0.2)
    filtered = [ema.update(r) for r in noisy_readings]

    # Calculate metrics
    metrics = calculate_noise_reduction(noisy_readings, filtered, true_distance)

    print(f"Raw MSE:          {metrics['raw_mse']:.2f}")
    print(f"Filtered MSE:     {metrics['filtered_mse']:.2f}")
    print(f"Noise reduction:  {metrics['noise_reduction_pct']:.1f}%")
    print(f"Meets KPI (>85%): {metrics['meets_kpi']}")

    print("\nFirst 10 readings:")
    print(f"{'Raw':>8} {'Filtered':>10} {'True':>8}")
    for r, f, t in zip(noisy_readings[:10], filtered[:10], true_distance[:10]):
        print(f"{r:8.1f} {f:10.1f} {t:8.1f}")

    # Save demo data for reference (ruta robusta al cwd)
    demo_data = {
        "alpha": 0.2,
        "true_values": true_distance[:20],
        "noisy_values": noisy_readings[:20],
        "filtered_values": filtered[:20],
        "metrics": metrics
    }
    from pathlib import Path
    out = Path(__file__).parent / "data" / "ema_demo.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    with open(out, "w") as f:
        json.dump(demo_data, f, indent=2)
    print(f"\nDemo data saved to {out}")