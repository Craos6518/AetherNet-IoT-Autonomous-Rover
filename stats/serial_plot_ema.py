"""
Grafica valor real (raw) vs suavizado (ema) del UNO en tiempo real.
Lee Serial 115200 de firmware/test-ema-uno/test-ema-uno.ino (raw,ema) y grafica.

Uso:
  /tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0
  /tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0 --save /tmp/captura.csv --png /tmp/ema-live.png
  /tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0 --seconds 30

Requiere: pip install pyserial matplotlib
"""
import argparse, csv, sys, time
from pathlib import Path
from collections import deque

def parse_args():
    ap = argparse.ArgumentParser(description="Plot raw vs ema desde UNO (test-ema-uno.ino)")
    ap.add_argument("-p", "--port", default="/dev/ttyACM0", help="puerto serie (arduino-cli board list)")
    ap.add_argument("-b", "--baud", type=int, default=115200)
    ap.add_argument("--seconds", type=float, default=0, help="0=infinito, >0 captura N segundos y genera PNG")
    ap.add_argument("--save", type=str, default="", help="CSV donde guardar raw,ema,time_s")
    ap.add_argument("--png", type=str, default="", help="PNG final (si no se da, muestra ventana)")
    ap.add_argument("--window", type=int, default=200, help="muestras visibles en vivo")
    return ap.parse_args()

def main():
    args = parse_args()
    try:
        import serial
    except ImportError:
        print("Falta pyserial. Instala: /tmp/venv-ema/bin/pip install pyserial")
        sys.exit(1)
    try:
        import matplotlib.pyplot as plt
        from matplotlib.animation import FuncAnimation
    except ImportError:
        print("Falta matplotlib")
        sys.exit(1)

    print(f"Abriendo {args.port} @ {args.baud} ... (Ctrl+C para salir)")
    try:
        ser = serial.Serial(args.port, args.baud, timeout=1)
    except Exception as e:
        print(f"No se pudo abrir {args.port}: {e}")
        print("Prueba: arduino-cli board list / ls /dev/ttyACM*")
        sys.exit(1)
    time.sleep(2)
    ser.reset_input_buffer()

    raw_q = deque(maxlen=args.window)
    ema_q = deque(maxlen=args.window)
    all_rows = []
    t0 = time.time()

    if args.seconds > 0:
        print(f"Capturando {args.seconds}s ... mueve objeto 10-100 cm")
        end = t0 + args.seconds
        while time.time() < end:
            line = ser.readline().decode(errors="ignore").strip()
            if not line or line.startswith("raw") or line.startswith("="):
                continue
            try:
                parts = line.split(",")
                if len(parts) < 2:
                    continue
                raw = float(parts[0]); ema = float(parts[1])
                elapsed = time.time() - t0
                all_rows.append((elapsed, raw, ema))
                print(f"{elapsed:5.1f}s raw={raw:5.1f} ema={ema:5.1f}")
            except ValueError:
                continue
        ser.close()
        if not all_rows:
            print("No se capturó nada — verifica baud y que el sketch imprime raw,ema")
            sys.exit(0)
        if args.save:
            Path(args.save).parent.mkdir(parents=True, exist_ok=True)
            with open(args.save, "w", newline="") as f:
                w = csv.writer(f); w.writerow(["time_s","raw","ema"]); w.writerows(all_rows)
            print(f"CSV guardado {args.save} ({len(all_rows)} filas)")
        times = [r[0] for r in all_rows]; raws = [r[1] for r in all_rows]; emas = [r[2] for r in all_rows]
        plt.figure(figsize=(10,4))
        plt.plot(times, raws, label="Raw (real)", color="#F87171", alpha=0.6)
        plt.plot(times, emas, label="EMA α0.2 (suavizado)", color="#3B82F6", linewidth=2)
        plt.axhline(30, color="#F59E0B", linestyle=":", label="umbral 30 cm")
        plt.xlabel("Tiempo s"); plt.ylabel("cm")
        plt.title(f"UNO raw vs EMA — {len(all_rows)} muestras")
        plt.legend(); plt.grid(alpha=0.2)
        out = args.png or "/tmp/ema-live.png"
        Path(out).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(out, dpi=180); print(f"PNG guardado {out}")
        plt.show()
        return

    plt.style.use("default")
    fig, ax = plt.subplots(figsize=(10,4))
    line_raw, = ax.plot([], [], label="Raw (real)", color="#F87171", alpha=0.6)
    line_ema, = ax.plot([], [], label="EMA α0.2 (suavizado)", color="#3B82F6", linewidth=2)
    ax.axhline(30, color="#F59E0B", linestyle=":", label="umbral 30")
    ax.set_ylim(0, 200); ax.set_xlim(0, args.window)
    ax.set_xlabel("Muestra (tiempo →)"); ax.set_ylabel("cm")
    ax.set_title(f"UNO vivo {args.port} @ {args.baud} — Ctrl+C para salir")
    ax.legend(loc="upper right"); ax.grid(alpha=0.2)
    txt = ax.text(0.02, 0.95, "", transform=ax.transAxes, fontsize=9, color="#374151", va="top")

    def init():
        line_raw.set_data([], []); line_ema.set_data([], [])
        return line_raw, line_ema, txt

    def update(frame):
        while ser.in_waiting:
            line = ser.readline().decode(errors="ignore").strip()
            if not line or line.startswith("raw") or line.startswith("="):
                continue
            try:
                a,b = line.split(",")[:2]
                raw = float(a); ema = float(b)
                elapsed = time.time() - t0
                raw_q.append(raw); ema_q.append(ema)
                all_rows.append((elapsed, raw, ema))
            except:
                pass
        x = list(range(len(raw_q)))
        line_raw.set_data(x, list(raw_q))
        line_ema.set_data(x, list(ema_q))
        if raw_q:
            lo = min(min(raw_q), min(ema_q)) -5; hi = max(max(raw_q), max(ema_q)) +10
            ax.set_ylim(max(0, lo), hi)
            ax.set_xlim(max(0, len(raw_q)-args.window), max(args.window, len(raw_q)))
            txt.set_text(f"raw={raw_q[-1]:.1f} ema={ema_q[-1]:.1f} n={len(all_rows)}")
        return line_raw, line_ema, txt

    ani = FuncAnimation(fig, update, init_func=init, interval=50, blit=False, cache_frame_data=False)
    try:
        plt.show()
    except KeyboardInterrupt:
        pass
    finally:
        ser.close()
        if args.save and all_rows:
            Path(args.save).parent.mkdir(parents=True, exist_ok=True)
            with open(args.save, "w", newline="") as f:
                w=csv.writer(f); w.writerow(["time_s","raw","ema"]); w.writerows(all_rows)
            print(f"\nCSV guardado {args.save}")
        if args.png and all_rows:
            Path(args.png).parent.mkdir(parents=True, exist_ok=True)
            fig.savefig(args.png, dpi=180); print(f"PNG guardado {args.png}")

if __name__ == "__main__":
    main()
