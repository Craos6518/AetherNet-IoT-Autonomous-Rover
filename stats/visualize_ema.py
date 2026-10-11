"""
Visualización del filtro EMA — Sprint 1-2.
Reutiliza exactamente EMAFilter / MultiSensorEMA / simulate_noisy_signal / calculate_noise_reduction
de ema_filter.py. Sin duplicar lógica.

Uso:
  python stats/visualize_ema.py
  python stats/visualize_ema.py --alpha 0.5 --seed 42
  python stats/visualize_ema.py --compare
  python stats/visualize_ema.py --live-obstacle
  python stats/visualize_ema.py --save docs/fritzing/ema-demo.png
  python stats/visualize_ema.py --csv stats/data/ema_demo.csv
"""
import argparse
import sys
from pathlib import Path

# Asegurar import desde stats/
sys.path.insert(0, str(Path(__file__).parent))

from ema_filter import EMAFilter, simulate_noisy_signal, calculate_noise_reduction

try:
    import matplotlib.pyplot as plt
except ImportError:
    print("matplotlib no instalado. Instala: pip install matplotlib>=3.8.0  (ya en stats/requirements.txt)")
    sys.exit(1)


def run_once(true_values, noise_std, alpha, seed):
    noisy = simulate_noisy_signal(true_values, noise_std=noise_std, seed=seed)
    ema = EMAFilter(alpha=alpha)
    filt = [ema.update(v) for v in noisy]
    metrics = calculate_noise_reduction(noisy, filt, true_values)
    return noisy, filt, metrics


def plot_single(alpha, noise_std, seed, obstacle, save, csv):
    # Escenario
    if obstacle:
        # Rampa 100 -> 10 cm (50 muestras): simula objeto acercándose
        true_vals = [100 - i * 1.8 for i in range(50)]
    else:
        true_vals = [50.0] * 100

    noisy, filt, metrics = run_once(true_vals, noise_std, alpha, seed)

    # Consola
    print(f"=== EMA α={alpha} noise_std={noise_std} seed={seed} obstacle={obstacle} ===")
    print(f"Raw MSE: {metrics['raw_mse']:.2f}  Filtered MSE: {metrics['filtered_mse']:.2f}  "
          f"Reducción: {metrics['noise_reduction_pct']:.1f}%  KPI>85%: {metrics['meets_kpi']}")
    if csv:
        Path(csv).parent.mkdir(parents=True, exist_ok=True)
        with open(csv, "w") as f:
            f.write("idx,true,raw,filtered\n")
            for i, (t, r, fl) in enumerate(zip(true_vals, noisy, filt)):
                f.write(f"{i},{t:.2f},{r:.2f},{fl:.2f}\n")
        print(f"CSV guardado en {csv}")

    # Plot
    fig, ax = plt.subplots(figsize=(10, 4.2))
    x = range(len(true_vals))
    ax.plot(x, true_vals, label="True (sin ruido)", color="#9CA3AF", linewidth=2, linestyle="--")
    ax.plot(x, noisy, label="Raw (ruidoso)", color="#F87171", alpha=0.6, linewidth=1)
    ax.plot(x, filt, label=f"EMA α={alpha}", color="#3B82F6", linewidth=2)
    ax.set_xlabel("Muestra (tiempo →)")
    ax.set_ylabel("Distancia cm")
    ax.set_title(f"EMA filtro α={alpha} — Reducción {metrics['noise_reduction_pct']:.1f}% "
                 f"{'✓ KPI>85%' if metrics['meets_kpi'] else '✗ KPI no alcanzado'}")
    ax.legend(loc="best")
    ax.grid(True, alpha=0.2)
    # Umbral de obstáculo del rover
    ax.axhline(30, color="#F59E0B", linestyle=":", linewidth=1, label=None)
    ax.text(len(true_vals)*0.02, 31, "umbral rover 30 cm", color="#92400E", fontsize=8)
    # Banda de ruido
    # Marcadores de detección para caso rampa
    if obstacle:
        for i, (f, t) in enumerate(zip(filt, true_vals)):
            if t <= 30 and f <= 30:
                ax.axvline(i, color="#10B981", alpha=0.3, linewidth=1)
                ax.text(i, ax.get_ylim()[1]*0.92, f"detect {i}", rotation=90, fontsize=7, color="#065F46")
                break

    plt.tight_layout()
    if save:
        Path(save).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(save, dpi=180)
        print(f"PNG guardado en {save}")
    plt.show()


def plot_compare(noise_std, seed, save):
    # Compara α = 0.1 / 0.2 / 0.5 / 0.8 sobre misma señal ruidosa
    true_vals = [50.0] * 100
    alphas = [0.1, 0.2, 0.3, 0.5, 0.8]
    noisy = simulate_noisy_signal(true_vals, noise_std=noise_std, seed=seed)

    fig, ax = plt.subplots(figsize=(10, 4.5))
    x = range(len(true_vals))
    ax.plot(x, true_vals, label="True 50 cm", color="#9CA3AF", linewidth=2, linestyle="--")
    ax.plot(x, noisy, label="Raw", color="#F87171", alpha=0.35, linewidth=1)

    for alpha in alphas:
        ema = EMAFilter(alpha=alpha)
        filt = [ema.update(v) for v in noisy]
        m = calculate_noise_reduction(noisy, filt, true_vals)
        ax.plot(x, filt, label=f"α={alpha} ({m['noise_reduction_pct']:.0f}%)", linewidth=1.8)

    ax.set_xlabel("Muestra")
    ax.set_ylabel("Distancia cm")
    ax.set_title(f"Comparativa α — noise_std={noise_std} seed={seed} (KPI>85% ideal)")
    ax.legend(ncol=2, fontsize=8)
    ax.grid(True, alpha=0.2)
    plt.tight_layout()
    if save:
        Path(save).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(save, dpi=180)
        print(f"PNG guardado en {save}")
    plt.show()


def main():
    ap = argparse.ArgumentParser(description="Visualiza EMA α=0.2 (HU-03 / RNF-2.1)")
    ap.add_argument("--alpha", type=float, default=0.2, help="α en (0,1] (default 0.2)")
    ap.add_argument("--noise-std", type=float, default=8.0, help="sigma ruido (default 8.0)")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--live-obstacle", action="store_true", help="rampa 100→10 cm en vez de 50 constante")
    ap.add_argument("--compare", action="store_true", help="compara 5 alphas superpuestos")
    ap.add_argument("--save", type=str, help="ruta PNG a guardar")
    ap.add_argument("--csv", type=str, help="ruta CSV a exportar")
    args = ap.parse_args()

    if args.compare:
        plot_compare(args.noise_std, args.seed, args.save)
    else:
        plot_single(args.alpha, args.noise_std, args.seed, args.live_obstacle, args.save, args.csv)


if __name__ == "__main__":
    main()
