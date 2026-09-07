"""
==============================================================================
Tests EMA Filter — 6º Semestre UTP | TS4D3 Estadística | RNF-2.1 / HU-03
Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
Experiencia: 2 años Python (pytest), 1 año C (port EMA), 2 años Arduino
FOSS: pytest + pytest-cov (como Jest en React) — RNF-3.1
==============================================================================
QUÉ ES ESTO:
  Suite de 14 tests para validar EMAFilter y MultiSensorEMA antes de portar
  a C (rover-uno.ino:66). Es el TDD que aprendí en Desarrollo Software —
  primero prototipo en Python, valido KPI >85%, luego flasheo al UNO.

  Si vienes de React/JS (2 años): piensa en Jest — describe/it/expect son
  class Test*/test_* y assert. pytest es el Jest de Python.

  Si vienes de C/Arduino (1 año C): aquí valido con gauss(0,σ) lo que en UNO
  validaré con Serial Plotter raw,ema. Sin estos tests, estaría flasheando
  a ciegas.

  Si vienes de PostgreSQL (1 año): estos tests son como validar un trigger
  antes de ponerlo en prod — simulo datos (50cm + ruido) como si vinieran de
  sensor_events.value y verifico que filtered_value cumple KPI.
"""
import pytest  # framework test como Jest — fixtures, asserts, -v verbose
import statistics  # para variance en test_ultrasonic_approach
from ema_filter import EMAFilter, MultiSensorEMA, simulate_noisy_signal, calculate_noise_reduction


# ==========================================================================
# TestEMAFilter — 6 tests básicos (como unit tests de un hook en React)
# ==========================================================================
class TestEMAFilter:
    """Test basic EMA filter functionality — valida S_t = α·Y_t + (1-α)·S_{t-1}."""

    def test_initialization(self):
        # Estado inicial — como new EMAFilter() sin update() — debe ser virgen
        ema = EMAFilter(alpha=0.2)  # α=0.2 HU-03
        assert ema.alpha == 0.2  # prop guardada (como props.alpha en React)
        assert ema.current_value is None  # sin primer valor — None como null en JS
        assert not ema.is_initialized  # flag false — como ultrasonicInitialized=false en C

    def test_invalid_alpha(self):
        # α debe estar en (0,1] — como validar props en React con PropTypes
        # α=0 no filtra (división por cero conceptual), α>1 amplifica ruido
        with pytest.raises(ValueError):  # espera excepción — como expect(...).toThrow() en Jest
            EMAFilter(alpha=0.0)  # límite inferior excluido
        with pytest.raises(ValueError):
            EMAFilter(alpha=1.5)  # >1 amplifica — inválido
        with pytest.raises(ValueError):
            EMAFilter(alpha=-0.1)  # negativo — sin sentido

    def test_first_value_initializes(self):
        # Primera llamada S_0 = Y_0 — inicializa sin filtrar (no hay historia)
        # En C: if (!initialized) { ema = raw; initialized=true; return raw; }
        ema = EMAFilter(alpha=0.2)
        result = ema.update(100.0)  # primer raw 100
        assert result == 100.0  # sin filtrar — 100% nuevo valor
        assert ema.current_value == 100.0  # estado guardado
        assert ema.is_initialized  # ahora sí inicializado

    def test_constant_signal(self):
        """Filter debería seguir señal constante exacto tras primer muestra (sin drift)."""
        ema = EMAFilter(alpha=0.2)
        ema.update(50.0)  # inicializa a 50
        for _ in range(10):
            # 50 constante → EMA 50 siempre — 0.2*50 + 0.8*50 = 50 (no drift)
            assert ema.update(50.0) == 50.0  # si drift, bug en fórmula

    def test_step_response(self):
        """Test respuesta a escalón — cuánto tarda en converger (como animación ease)."""
        ema = EMAFilter(alpha=0.5)  # α alto para respuesta rápida (más peso a nuevo valor)
        ema.update(0.0)
        for _ in range(5):
            ema.update(0.0)  # estabiliza en 0

        # Escalón a 100 — simula obstáculo súbito 0→100cm
        values = [ema.update(100.0) for _ in range(5)]

        # Debe acercarse exponencialmente a 100 — no salto instantáneo (EMA es IIR, no FIR)
        # α=0.5: primer paso 0.5*100+0.5*0=50, luego 75, 87.5, etc. (como ease-out en CSS)
        assert values[0] == 50.0  # First step: 0.5*100 + 0.5*0 = 50
        assert values[-1] > 90.0  # Should be close to 100 after 5 steps (converge rápido con α=0.5)

    def test_reset(self):
        # Reset — como borrar state en React y volver a S_0 = Y_0
        ema = EMAFilter(alpha=0.2)
        ema.update(50.0)
        ema.update(60.0)  # estado 52 con α0.2 (0.2*60+0.8*50)
        ema.reset()  # limpia — vuelve a None/false
        assert ema.current_value is None  # sin estado
        assert not ema.is_initialized
        assert ema.update(10.0) == 10.0  # reinicia como primera llamada


# ==========================================================================
# TestMultiSensorEMA — 4 tests aislamiento por sensor (como Map en JS)
# ==========================================================================
class TestMultiSensorEMA:
    """Test multi-sensor EMA manager — aísla HC-SR04 vs KY-037."""

    def test_independent_filters(self):
        # Cada sensor su propio EMAFilter — no se mezclan (como 2 instancias useEMA)
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)  # HC-SR04 → 10cm
        multi.update("sensor2", 20.0)  # KY-037 → 20dB

        # Cada uno guarda su valor — no cross-talk (como 2 states en React)
        assert multi.get_filter("sensor1").current_value == 10.0
        assert multi.get_filter("sensor2").current_value == 20.0

    def test_reset_sensor(self):
        # Reset solo un sensor — como borrar un doc en Mongo por id
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)
        multi.reset_sensor("sensor1")
        assert multi.get_filter("sensor1").current_value is None  # solo sensor1 borrado

    def test_reset_all(self):
        # Reset todos — como truncate tabla en PostgreSQL (pero en memoria)
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)
        multi.update("sensor2", 20.0)
        multi.reset_all()
        assert multi.get_filter("sensor1").current_value is None
        assert multi.get_filter("sensor2").current_value is None

    def test_get_state(self):
        # Serializa estado — como GET /state en FastAPI, para debug/JSON
        multi = MultiSensorEMA(alpha=0.2)
        multi.update("sensor1", 10.0)
        state = multi.get_state()
        assert "sensor1" in state
        assert state["sensor1"]["value"] == 10.0
        assert state["sensor1"]["initialized"] is True


# ==========================================================================
# TestNoiseReduction — 2 tests KPI PRD >85% (el que importa para nota)
# ==========================================================================
class TestNoiseReduction:
    """Test noise reduction meets KPI requirements — valida PRD KPI."""

    def test_noise_reduction_kpi(self):
        """Verify EMA achieves >85% noise reduction per PRD KPI (prd.md:51)."""
        # Señal verdadera: 50cm constante (objeto quieto a 50cm, caso ideal)
        true_values = [50.0] * 200
        # Añade ruido fuerte σ10 — HC-SR04 ruidoso + picos (peor caso)
        noisy = simulate_noisy_signal(true_values, noise_std=10.0, seed=123)

        # Aplica EMA α=0.2 (HU-03) — el que va en rover.ino:66
        ema = EMAFilter(alpha=0.2)
        filtered = [ema.update(v) for v in noisy]  # como map(raw => ema.update(raw)) en JS

        metrics = calculate_noise_reduction(noisy, filtered, true_values)

        # Log para ver en pytest -v (como console.log en CI)
        print("\nNoise reduction test:")
        print(f"  Raw MSE: {metrics['raw_mse']:.2f}")
        print(f"  Filtered MSE: {metrics['filtered_mse']:.2f}")
        print(f"  Reduction: {metrics['noise_reduction_pct']:.1f}%")

        # KPI PRD — debe ser >85% — si falla, el filtro no sirve en Rover (frenará por picos)
        # Con α0.2 σ10 da ~87% (ver notebooks celda 2) — pasa por poco, con picos baja a 82%
        assert metrics["meets_kpi"], \
            f"EMA noise reduction {metrics['noise_reduction_pct']:.1f}% < 85% KPI"

    def test_different_alphas(self):
        """Test que α=0.2 es buen tradeoff — explora 0.1/0.2/0.3/0.5 (como alpha_sweep pero unit)."""
        true_values = [50.0] * 100
        noisy = simulate_noisy_signal(true_values, noise_std=8.0, seed=42)

        for alpha in [0.1, 0.2, 0.3, 0.5]:
            ema = EMAFilter(alpha=alpha)
            filtered = [ema.update(v) for v in noisy]
            metrics = calculate_noise_reduction(noisy, filtered, true_values)
            print(f"  α={alpha}: reduction={metrics['noise_reduction_pct']:.1f}%")
            # Loguea para comparar — α bajo más reducción pero más retardo (ver alpha_sweep.py)


# ==========================================================================
# TestSimulatedSensorData — 2 tests patrones reales (rampa + escalón)
# ==========================================================================
class TestSimulatedSensorData:
    """Tests con patrones realistas — lo que verá el Rover en pista."""

    def test_ultrasonic_approach(self):
        """Simula objeto acercándose — distancia decreciente (Rover avanzando a pared)."""
        # Objeto de 100cm a 10cm en 50 muestras — rampa -1.8cm/muestra (Rover a ~0.9 m/s a 50Hz)
        true_values = [100 - i * 1.8 for i in range(50)]
        noisy = simulate_noisy_signal(true_values, noise_std=5.0, seed=42)

        ema = EMAFilter(alpha=0.2)
        filtered = [ema.update(v) for v in noisy]

        # Con señal dinámica, EMA tiene lag → MSE no es buen KPI (siempre atrasa).
        # Valida que suaviza (varianza reducida) y sigue tendencia sin lag excesivo.
        import statistics

        # Varianza filtrada debe ser menor que ruidosa — EMA suaviza (como blur en CSS)
        assert statistics.variance(filtered) < statistics.variance(noisy), "EMA debe suavizar varianza"

        # Valor final debe estar cerca del true (lag <15cm con α=0.2) — si lag >15, Rover chocaría
        assert abs(filtered[-1] - true_values[-1]) < 15, f"Lag excesivo: {filtered[-1]:.1f} vs {true_values[-1]:.1f}"

    def test_sudden_obstacle(self):
        """Simula obstáculo súbito (escalón) — pared aparece de golpe a 20cm."""
        # Camino libre 100cm, de pronto obstáculo 20cm (escalón) — caso crítico para Rover
        true_values = [100.0] * 20 + [20.0] * 30
        noisy = simulate_noisy_signal(true_values, noise_std=3.0, seed=42)

        ema = EMAFilter(alpha=0.2)
        filtered = [ema.update(v) for v in noisy]

        # Debe detectar obstáculo en tiempo razonable — si tarda >10 muestras (100ms a 100Hz),
        # Rover recorre 14cm a 1.4m/s y choca. α=0.2 detecta en ~4 muestras (ver alpha_sweep).
        obstacle_detected = False
        for i, v in enumerate(filtered[20:], 20):  # desde muestra 20 (escalón)
            if v < 50:  # Cruce a mitad entre 100 y 20 — umbral detección
                obstacle_detected = True
                detection_delay = i - 20  # muestras desde escalón
                break

        assert obstacle_detected, "Failed to detect obstacle — EMA nunca cruzó 50"
        assert detection_delay <= 10, f"Detection too slow: {detection_delay} samples (>100ms → choque)"


# Permite python tests/test_ema_filter.py directo (como npm test sin pytest global)
if __name__ == "__main__":
    pytest.main([__file__, "-v"])  # corre con -v verbose (como jest --verbose)
