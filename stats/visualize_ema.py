"""
==============================================================================
Visualización EMA — Sprint 1-2 | 6º Semestre UTP | HU-03 / RNF-2.1 / KPI >85%
Autor: Andres Felipe Martinez Henao
Experiencia: 2 años Python (matplotlib), 2 años HTML/CSS/JS/React (Chart.js),
             2 años electrónica/Arduino (HC-SR04), 1 año C (port EMA a rover.ino)
FOSS: matplotlib (como Chart.js en web, pero Python) — RNF-3.1
==============================================================================
QUÉ ES ESTO:
  CLI que grafica raw vs filtrado para validar visualmente que EMA α=0.2
  suaviza sin lag excesivo. Reutiliza exactamente EMAFilter de ema_filter.py
  — sin duplicar lógica (DRY). Es el Storybook del filtro: ves 5 variantes
  de α y eliges.

  Si vienes de React/JS (2 años): piensa en un LineChart con recharts —
  fig, ax = plt.subplots() es <ResponsiveContainer>, ax.plot es <Line>,
  ax.axhline es referencia como <ReferenceLine>.

  Si vienes de electrónica/C (2 años Arduino, 1 año C): este script es el
  Serial Plotter potenciado — ves true --, raw rojo, ema azul, como en
  Arduino IDE pero con CSV/PNG y Monte Carlo.

Uso:
  python stats/visualize_ema.py                          # single α0.2 50cm σ8
  python stats/visualize_ema.py --alpha 0.5 --seed 42    # otro α
  python stats/visualize_ema.py --compare                # 5 α superpuestos
  python stats/visualize_ema.py --live-obstacle          # rampa 100→10 (Rover avanzando)
  python stats/visualize_ema.py --save docs/fritzing/ema-demo.png
  python stats/visualize_ema.py --csv stats/data/ema_demo.csv
"""
import argparse  # CLI args — como yargs en Node o commander en JS
import sys  # para exit y path
from pathlib import Path  # rutas robustas — como path.join en Node

# Asegurar import desde stats/ — cuando corres python stats/visualize_ema.py,
# el cwd puede ser repo raíz, no stats/. Este insert arregla import ema_filter
# (como configurar baseUrl en tsconfig.json)
sys.path.insert(0, str(Path(__file__).parent))

# Reutiliza núcleo — sin copiar fórmula EMA (DRY, single source of truth)
from ema_filter import EMAFilter, simulate_noisy_signal, calculate_noise_reduction

# Matplotlib — Chart.js de Python (FOSS). Si no está, guía instalación
try:
    import matplotlib.pyplot as plt  # plt es el entry — como import { Chart } from 'chart.js'
except ImportError:
    print("matplotlib no instalado. Instala: pip install matplotlib>=3.8.0  (ya en stats/requirements.txt)")
    sys.exit(1)  # falla rápido con mensaje accionable (como throw en JS)


# --------------------------------------------------------------------------
# Helper: ejecuta un trial completo (simula + filtra + métrica)
# --------------------------------------------------------------------------
def run_once(true_values, noise_std, alpha, seed):
    """Un trial: true + gauss → noisy → EMA → métrica (como un test unitario)."""
    noisy = simulate_noisy_signal(true_values, noise_std=noise_std, seed=seed)  # raw ruidoso
    ema = EMAFilter(alpha=alpha)  # filtro con α dado
    filt = [ema.update(v) for v in noisy]  # filtra — como map(raw => ema.update(raw)) en JS
    metrics = calculate_noise_reduction(noisy, filt, true_values)  # KPI
    return noisy, filt, metrics  # tupla (como return {noisy, filt, metrics} en JS)


# --------------------------------------------------------------------------
# plot_single — Un α, una señal (50 constante o rampa 100→10)
# --------------------------------------------------------------------------
def plot_single(alpha, noise_std, seed, obstacle, save, csv):
    """Grafica single α — como Storybook para un componente."""
    # Escenario: rampa simula Rover avanzando a pared (100→10 en 50 muestras)
    # constante 50 simula objeto quieto (caso ideal KPI)
    if obstacle:
        # Rampa 100 -> 10 cm (50 muestras): simula objeto acercándose a 1.8cm/muestra
        # 1.8cm * 50Hz = 90cm/s ≈ 0.9 m/s — velocidad Rover moderada
        true_vals = [100 - i * 1.8 for i in range(50)]
    else:
        true_vals = [50.0] * 100  # 50cm quieto ×100 — caso KPI prd.md:51

    noisy, filt, metrics = run_once(true_vals, noise_std, alpha, seed)

    # Consola — como console.log en JS, útil en CI y para copiar a informe
    print(f"=== EMA α={alpha} noise_std={noise_std} seed={seed} obstacle={obstacle} ===")
    print(f"Raw MSE: {metrics['raw_mse']:.2f}  Filtered MSE: {metrics['filtered_mse']:.2f}  "
          f"Reducción: {metrics['noise_reduction_pct']:.1f}%  KPI>85%: {metrics['meets_kpi']}")
    # MSE y reducción — números para tabla en informe EST-07 (como coverage report)

    # Export CSV opcional — para Excel/Pandas/PostgreSQL import (como export JSON en web)
    if csv:
        Path(csv).parent.mkdir(parents=True, exist_ok=True)  # crea carpeta si no existe (fs.mkdir)
        with open(csv, "w") as f:
            f.write("idx,true,raw,filtered\n")  # header CSV — como header en DataFrame pandas
            for i, (t, r, fl) in enumerate(zip(true_vals, noisy, filt)):
                f.write(f"{i},{t:.2f},{r:.2f},{fl:.2f}\n")  # fila por muestra
        print(f"CSV guardado en {csv}")

    # Plot — como <LineChart> en recharts: 3 series + referencia
    fig, ax = plt.subplots(figsize=(10, 4.2))  # figura 10×4.2 pulgadas — como <ResponsiveContainer width={1000} height={420}>
    x = range(len(true_vals))  # eje X muestras (tiempo →)
    # True -- gris punteado — ground truth (como baseline en benchmark)
    ax.plot(x, true_vals, label="True (sin ruido)", color="#9CA3AF", linewidth=2, linestyle="--")
    # Raw rojo semitransparente — ruidoso (como datos crudos de sensor)
    ax.plot(x, noisy, label="Raw (ruidoso)", color="#F87171", alpha=0.6, linewidth=1)
    # EMA azul sólido — filtrado (como filtered_value en sensor_events)
    ax.plot(x, filt, label=f"EMA α={alpha}", color="#3B82F6", linewidth=2)
    ax.set_xlabel("Muestra (tiempo →)")  # eje X — tiempo
    ax.set_ylabel("Distancia cm")  # eje Y — cm (unidad HC-SR04)
    # Título con métrica — como badge de coverage en README
    ax.set_title(f"EMA filtro α={alpha} — Reducción {metrics['noise_reduction_pct']:.1f}% "
                 f"{'✓ KPI>85%' if metrics['meets_kpi'] else '✗ KPI no alcanzado'}")
    ax.legend(loc="best")  # leyenda auto — como <Legend /> en recharts
    ax.grid(True, alpha=0.2)  # grilla suave — como <CartesianGrid strokeOpacity={0.2} />
    # Umbral Rover 30cm — OBSTACLE_DISTANCE_CM rover.ino:73 — si filt cruza, Rover gira
    ax.axhline(30, color="#F59E0B", linestyle=":", linewidth=1, label=None)  # línea punteada naranja
    ax.text(len(true_vals)*0.02, 31, "umbral rover 30 cm", color="#92400E", fontsize=8)
    # Marcador detección para rampa — cuándo filt detecta obstáculo (true<=30 y filt<=30)
    # Como marcar en gráfica cuándo se dispara evento de seguridad
    if obstacle:
        for i, (f, t) in enumerate(zip(filt, true_vals)):
            if t <= 30 and f <= 30:  # ambos cruzan 30 — detección
                ax.axvline(i, color="#10B981", alpha=0.3, linewidth=1)  # línea verde vertical
                ax.text(i, ax.get_ylim()[1]*0.92, f"detect {i}", rotation=90, fontsize=7, color="#065F46")
                break  # solo primera detección (lag)

    plt.tight_layout()  # ajusta márgenes — como CSS margin auto (evita labels cortados)
    if save:
        Path(save).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(save, dpi=180)  # PNG 180dpi — como export chart a PNG (alta res para informe)
        print(f"PNG guardado en {save}")
    plt.show()  # muestra ventana interactiva (como abrir Chart en browser)


# --------------------------------------------------------------------------
# plot_compare — 5 α superpuestos sobre MISMA señal ruidosa
# --------------------------------------------------------------------------
def plot_compare(noise_std, seed, save):
    """Compara α 0.1/0.2/0.3/0.5/0.8 — como comparar 5 variantes de componente en Storybook."""
    true_vals = [50.0] * 100
    alphas = [0.1, 0.2, 0.3, 0.5, 0.8]  # rango útil — 0.05 muy lento, 0.8 casi no filtra
    # Misma noisy para todos — para comparar justo (mismo seed, misma señal)
    noisy = simulate_noisy_signal(true_vals, noise_std=noise_std, seed=seed)

    fig, ax = plt.subplots(figsize=(10, 4.5))
    x = range(len(true_vals))
    ax.plot(x, true_vals, label="True 50 cm", color="#9CA3AF", linewidth=2, linestyle="--")
    ax.plot(x, noisy, label="Raw", color="#F87171", alpha=0.35, linewidth=1)  # raw tenue — fondo

    for alpha in alphas:
        ema = EMAFilter(alpha=alpha)
        filt = [ema.update(v) for v in noisy]
        m = calculate_noise_reduction(noisy, filt, true_vals)
        # Label con reducción — ves de golpe que α bajo = más reducción pero más lag
        ax.plot(x, filt, label=f"α={alpha} ({m['noise_reduction_pct']:.0f}%)", linewidth=1.8)

    ax.set_xlabel("Muestra")
    ax.set_ylabel("Distancia cm")
    ax.set_title(f"Comparativa α — noise_std={noise_std} seed={seed} (KPI>85% ideal)")
    ax.legend(ncol=2, fontsize=8)  # 2 columnas — como legend compacto en dashboard
    ax.grid(True, alpha=0.2)
    plt.tight_layout()
    if save:
        Path(save).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(save, dpi=180)
        print(f"PNG guardado en {save}")
    plt.show()


# --------------------------------------------------------------------------
# CLI — Argumentos (como yargs en Node)
# --------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description="Visualiza EMA α=0.2 (HU-03 / RNF-2.1)")
    ap.add_argument("--alpha", type=float, default=0.2, help="α en (0,1] (default 0.2 HU-03)")
    ap.add_argument("--noise-std", type=float, default=8.0, help="sigma ruido (default 8.0, HC-SR04 σ~8)")
    ap.add_argument("--seed", type=int, default=42, help="seed RNG (default 42 reproducible)")
    ap.add_argument("--live-obstacle", action="store_true", help="rampa 100→10 cm en vez de 50 constante (simula Rover)")
    ap.add_argument("--compare", action="store_true", help="compara 5 alphas superpuestos (Storybook)")
    ap.add_argument("--save", type=str, help="ruta PNG a guardar (como export chart)")
    ap.add_argument("--csv", type=str, help="ruta CSV a exportar (como download data)")
    args = ap.parse_args()

    if args.compare:
        plot_compare(args.noise_std, args.seed, args.save)
    else:
        plot_single(args.alpha, args.noise_std, args.seed, args.live_obstacle, args.save, args.csv)


if __name__ == "__main__":
    main()  # entry point — como ReactDOM.render(<App />) en web
