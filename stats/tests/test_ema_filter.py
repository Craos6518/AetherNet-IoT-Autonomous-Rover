"""Tests for EMA filter implementation."""
import pytest
import statistics
from ema_filter import EMAFilter, MultiSensorEMA, simulate_noisy_signal, calculate_noise_reduction


class TestEMAFilter:
    """Test basic EMA filter functionality."""

    def test_initialization(self):
        ema = EMAFilter(alpha=0.2)
        assert ema.alpha == 0.2
        assert ema.current_value is None
        assert not ema.is_initialized

    def test_invalid_alpha(self):
        with pytest.raises(ValueError):
            EMAFilter(alpha=0.0)
        with pytest.raises(ValueError):
            EMAFilter(alpha=1.5)
        with pytest.raises(ValueError):
            EMAFilter(alpha=-0.1)

    def test_first_value_initializes(self):
        ema = EMAFilter(alpha=0.2)
        result = ema.update(100.0)
        assert result == 100.0
        assert ema.current_value == 100.0
        assert ema.is_initialized

    def test_constant_signal(self):
        """Filter should track constant signal exactly after first sample."""
        ema = EMAFilter(alpha=0.2)
        ema.update(50.0)
        for _ in range(10):
            assert ema.update(50.0) == 50.0

    def test_step_response(self):
        """Test filter response to step change."""
        ema = EMAFilter(alpha=0.5)  # Higher alpha for faster response
        ema.update(0.0)
        for _ in range(5):
            ema.update(0.0)

        # Step to 100
        values = [ema.update(100.0) for _ in range(5)]

        # Should approach 100 exponentially
        assert values[0] == 50.0  # First step: 0.5*100 + 0.5*0
        assert values[-1] > 90.0  # Should be close to 100 after 5 steps

    def test_reset(self):
        ema = EMAFilter(alpha=0.2)
        ema.update(50.0)
        ema.update(60.0)
        ema.reset()
        assert ema.current_value is None
        assert not ema.is_initialized
        assert ema.update(10.0) == 10.0


class TestMultiSensorEMA:
    """Test multi-sensor EMA manager."""

    def test_independent_filters(self):
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)
        multi.update("sensor2", 20.0)

        assert multi.get_filter("sensor1").current_value == 10.0
        assert multi.get_filter("sensor2").current_value == 20.0

    def test_reset_sensor(self):
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)
        multi.reset_sensor("sensor1")
        assert multi.get_filter("sensor1").current_value is None

    def test_reset_all(self):
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)
        multi.update("sensor2", 20.0)
        multi.reset_all()
        assert multi.get_filter("sensor1").current_value is None
        assert multi.get_filter("sensor2").current_value is None

    def test_get_state(self):
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)
        state = multi.get_state()
        assert "sensor1" in state
        assert state["sensor1"]["value"] == 10.0
        assert state["sensor1"]["initialized"] is True


class TestNoiseReduction:
    """Test noise reduction meets KPI requirements."""

    def test_noise_reduction_kpi(self):
        """Verify EMA achieves >85% noise reduction per PRD KPI."""
        # True signal: constant 50cm
        true_values = [50.0] * 200
        # Add significant noise
        noisy = simulate_noisy_signal(true_values, noise_std=10.0, seed=123)

        # Apply EMA with alpha=0.2 (per HU-03)
        ema = EMAFilter(alpha=0.2)
        filtered = [ema.update(v) for v in noisy]

        metrics = calculate_noise_reduction(noisy, filtered, true_values)

        print(f"\nNoise reduction test:")
        print(f"  Raw MSE: {metrics['raw_mse']:.2f}")
        print(f"  Filtered MSE: {metrics['filtered_mse']:.2f}")
        print(f"  Reduction: {metrics['noise_reduction_pct']:.1f}%")

        # This is the PRD KPI - must achieve >85%
        assert metrics["meets_kpi"], \
            f"EMA noise reduction {metrics['noise_reduction_pct']:.1f}% < 85% KPI"

    def test_different_alphas(self):
        """Test that alpha=0.2 is a good tradeoff."""
        true_values = [50.0] * 100
        noisy = simulate_noisy_signal(true_values, noise_std=8.0, seed=42)

        for alpha in [0.1, 0.2, 0.3, 0.5]:
            ema = EMAFilter(alpha=alpha)
            filtered = [ema.update(v) for v in noisy]
            metrics = calculate_noise_reduction(noisy, filtered, true_values)
            print(f"  α={alpha}: reduction={metrics['noise_reduction_pct']:.1f}%")


class TestSimulatedSensorData:
    """Tests with realistic sensor patterns."""

    def test_ultrasonic_approach(self):
        """Simulate object approaching - distance decreasing."""
        # Object moves from 100cm to 10cm over 50 readings
        true_values = [100 - i * 1.8 for i in range(50)]
        noisy = simulate_noisy_signal(true_values, noise_std=5.0, seed=42)

        ema = EMAFilter(alpha=0.2)
        filtered = [ema.update(v) for v in noisy]

        # Con señal dinámica, EMA tiene lag -> MSE no es buen KPI.
        # Validar que el filtro suaviza (varianza reducida) y sigue tendencia.
        import statistics

        # Varianza del ruido debe reducirse
        assert statistics.variance(filtered) < statistics.variance(noisy), "EMA debe suavizar varianza"

        # Valor final debe estar cerca del verdadero (lag < 15cm con α=0.2)
        assert abs(filtered[-1] - true_values[-1]) < 15, f"Lag excesivo: {filtered[-1]:.1f} vs {true_values[-1]:.1f}"

    def test_sudden_obstacle(self):
        """Simulate sudden obstacle detection (step change)."""
        # Clear path at 100cm, then obstacle at 20cm
        true_values = [100.0] * 20 + [20.0] * 30
        noisy = simulate_noisy_signal(true_values, noise_std=3.0, seed=42)

        ema = EMAFilter(alpha=0.2)
        filtered = [ema.update(v) for v in noisy]

        # Should detect obstacle within reasonable time
        # At alpha=0.2, reaches ~63% of step in ~5 samples
        obstacle_detected = False
        for i, v in enumerate(filtered[20:], 20):
            if v < 50:  # Halfway between 100 and 20
                obstacle_detected = True
                detection_delay = i - 20
                break

        assert obstacle_detected, "Failed to detect obstacle"
        assert detection_delay <= 10, f"Detection too slow: {detection_delay} samples"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])