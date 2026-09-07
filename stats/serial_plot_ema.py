"""
==============================================================================
Serial Plot EMA — Puente UNO → Python | 6º Semestre UTP | HU-03 / EST bench
Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
Experiencia: 2 años Python (pyserial/matplotlib), 2 años electrónica/Arduino
             (HC-SR04, Serial 115200), 2 años JS/React (Chart vivo), 1 año C
Materia: TS4D3 Estadística — Validación banco físico UNO (firmware/test-ema-uno)
FOSS: pyserial + matplotlib — RNF-3.1 (no Serial Plotter propietario solo)
==============================================================================
QUÉ ES ESTO:
  Lee Serial 115200 de firmware/test-ema-uno/test-ema-uno.ino que imprime
  "raw,ema" (ej. "16.0,16.6") y grafica en vivo. Es el Serial Plotter de
  Arduino IDE pero con superpoderes: guarda CSV + PNG, modo captura 30s
  para data/ema-real-531.csv, y ventana viva con FuncAnimation.

  Si vienes de React/JS (2 años): piensa en un WebSocket que lee un stream
  y lo grafica con Chart.js en vivo — aquí el stream es Serial y el chart es
  matplotlib con deque como ring buffer (como circular buffer en JS).

  Si vienes de electrónica/C (2 años Arduino, 1 año C): este script es el
  osciloscopio del HC-SR04 — ves raw ruidoso vs ema estable sin tocar Rover.
  test-ema-uno.ino hace S_t = 0.2*raw + 0.8*previa (C), aquí solo graficas.

Uso:
  /tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0
  /tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0 --save /tmp/captura.csv --png /tmp/ema-live.png
  /tmp/venv-ema/bin/python stats/serial_plot_ema.py -p /dev/ttyACM0 --seconds 30
Requiere: pip install pyserial matplotlib (ya en stats/requirements.txt + venv)
"""
import argparse  # CLI — como yargs en Node
import csv  # para guardar CSV time_s,raw,ema (como export en React)
import sys  # para exit
import time  # para time.time() y sleep (como Date.now() en JS)
from pathlib import Path  # rutas — como path.join en Node
from collections import deque  # ring buffer — como circular array en JS (maxlen)


# --------------------------------------------------------------------------
# CLI args — como props de un componente <SerialPlot port="/dev/ttyACM0" />
# --------------------------------------------------------------------------
def parse_args():
    ap = argparse.ArgumentParser(description="Plot raw vs ema desde UNO (test-ema-uno.ino)")
    # Puerto serie — arduino-cli board list te dice /dev/ttyACM0 o /dev/ttyUSB0 (como ls /dev/tty*)
    ap.add_argument("-p", "--port", default="/dev/ttyACM0", help="puerto serie (arduino-cli board list)")
    ap.add_argument("-b", "--baud", type=int, default=115200, help="baud — debe coincidir con Serial.begin(115200) en test-ema-uno.ino:22")
    # Modo captura vs vivo — como polling vs streaming en web
    ap.add_argument("--seconds", type=float, default=0, help="0=infinito (vivo), >0 captura N segundos y genera PNG (como record)")
    ap.add_argument("--save", type=str, default="", help="CSV donde guardar raw,ema,time_s (como download CSV en React)")
    ap.add_argument("--png", type=str, default="", help="PNG final (si no se da, muestra ventana interactiva)")
    ap.add_argument("--window", type=int, default=200, help="muestras visibles en vivo (ring buffer, como ventana deslizante en chart)")
    return ap.parse_args()


def main():
    args = parse_args()
    # Imports tardíos con mensaje accionable — como try { require('pyserial') } catch en Node
    try:
        import serial  # pyserial — como WebSerial API en JS, pero Python
    except ImportError:
        print("Falta pyserial. Instala: /tmp/venv-ema/bin/pip install pyserial")
        sys.exit(1)
    try:
        import matplotlib.pyplot as plt  # graficas — como Chart.js
        from matplotlib.animation import FuncAnimation  # animación viva — como requestAnimationFrame en JS
    except ImportError:
        print("Falta matplotlib — pip install matplotlib>=3.8.0")
        sys.exit(1)

    # Abre puerto serie — como new WebSocket('serial:///dev/ttyACM0') en web (pero bloqueante)
    print(f"Abriendo {args.port} @ {args.baud} ... (Ctrl+C para salir)")
    try:
        ser = serial.Serial(args.port, args.baud, timeout=1)  # timeout 1s — no bloquea infinito si no hay datos
    except Exception as e:
        print(f"No se pudo abrir {args.port}: {e}")
        print("Prueba: arduino-cli board list / ls /dev/ttyACM* / dmesg | grep tty")
        sys.exit(1)
    time.sleep(2)  # espera 2s — el UNO resetea al abrir Serial (como esperar a que server reinicie)
    ser.reset_input_buffer()  # limpia basura del reset (como clear cache)

    # Buffers — deque con maxlen es ring buffer (cuando llega 201, saca la 0) — como circular buffer en JS
    raw_q = deque(maxlen=args.window)  # últimas 200 muestras raw (ventana visible)
    ema_q = deque(maxlen=args.window)  # últimas 200 ema
    all_rows = []  # todas las muestras capturadas — para CSV (como log en PostgreSQL)
    t0 = time.time()  # tiempo inicio — como Date.now() en JS

    # ----------------------------------------------------------------------
    # Modo CAPTURA — bloquea N segundos, luego genera PNG+CSV (como record)
    # ----------------------------------------------------------------------
    if args.seconds > 0:
        print(f"Capturando {args.seconds}s ... mueve objeto 10-100 cm frente al HC-SR04 (TRIG D2 ECHO D3)")
        end = t0 + args.seconds  # timestamp fin
        while time.time() < end:  # loop bloqueante N segundos
            line = ser.readline().decode(errors="ignore").strip()  # lee línea Serial — como socket.on('data')
            # Ignora header y separadores — test-ema-uno imprime "raw,ema" y "=== Test EMA ==="
            if not line or line.startswith("raw") or line.startswith("="):
                continue
            try:
                parts = line.split(",")  # "16.0,16.6" → ["16.0","16.6"] (como line.split(',') en JS)
                if len(parts) < 2:
                    continue  # línea incompleta
                raw = float(parts[0]); ema = float(parts[1])  # parsea — como parseFloat en JS
                elapsed = time.time() - t0  # segundos desde inicio
                all_rows.append((elapsed, raw, ema))  # guarda para CSV
                print(f"{elapsed:5.1f}s raw={raw:5.1f} ema={ema:5.1f}")  # log vivo — como console.log
            except ValueError:
                continue  # línea corrupta — ignora (como try/catch en fetch)
        ser.close()  # cierra puerto — como socket.close()
        if not all_rows:
            print("No se capturó nada — verifica baud 115200 y que test-ema-uno imprime raw,ema (Serial.println(raw + ',' + ema))")
            sys.exit(0)
        # Guarda CSV — time_s,raw,ema (como export a PostgreSQL COPY)
        if args.save:
            Path(args.save).parent.mkdir(parents=True, exist_ok=True)
            with open(args.save, "w", newline="") as f:
                w = csv.writer(f); w.writerow(["time_s","raw","ema"]); w.writerows(all_rows)
            print(f"CSV guardado {args.save} ({len(all_rows)} filas) — ver stats/data/ema-real-531.csv:1")
        # Genera PNG — como export chart a PNG en React (html2canvas)
        times = [r[0] for r in all_rows]; raws = [r[1] for r in all_rows]; emas = [r[2] for r in all_rows]
        plt.figure(figsize=(10,4))  # figura 10×4 — como <Chart width={1000} height={400}>
        plt.plot(times, raws, label="Raw (real)", color="#F87171", alpha=0.6)  # rojo claro — ruidoso
        plt.plot(times, emas, label="EMA α0.2 (suavizado)", color="#3B82F6", linewidth=2)  # azul — filtrado
        plt.axhline(30, color="#F59E0B", linestyle=":", label="umbral 30 cm")  # umbral Rover OBSTACLE_DISTANCE_CM
        plt.xlabel("Tiempo s"); plt.ylabel("cm")
        plt.title(f"UNO raw vs EMA — {len(all_rows)} muestras (test-ema-uno.ino α0.2)")
        plt.legend(); plt.grid(alpha=0.2)
        out = args.png or "/tmp/ema-live.png"  # default /tmp si no se da --png
        Path(out).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(out, dpi=180); print(f"PNG guardado {out} — ver docs/fritzing/ema-real-531.png")
        plt.show()  # muestra ventana (como window.open(chart))
        return  # fin modo captura

    # ----------------------------------------------------------------------
    # Modo VIVO — ventana deslizante con FuncAnimation (como streaming en React)
    # ----------------------------------------------------------------------
    plt.style.use("default")
    fig, ax = plt.subplots(figsize=(10,4))
    # Líneas vacías — se actualizarán cada 50ms (como <Line data={[]} /> en recharts)
    line_raw, = ax.plot([], [], label="Raw (real)", color="#F87171", alpha=0.6)
    line_ema, = ax.plot([], [], label="EMA α0.2 (suavizado)", color="#3B82F6", linewidth=2)
    ax.axhline(30, color="#F59E0B", linestyle=":", label="umbral 30")  # referencia 30cm
    ax.set_ylim(0, 200); ax.set_xlim(0, args.window)  # rango HC-SR04 0-200cm, 200 muestras ventana
    ax.set_xlabel("Muestra (tiempo →)"); ax.set_ylabel("cm")
    ax.set_title(f"UNO vivo {args.port} @ {args.baud} — Ctrl+C para salir (mueve cartón 10-100 cm)")
    ax.legend(loc="upper right"); ax.grid(alpha=0.2)
    # Texto overlay con último valor — como HUD en dashboard React
    txt = ax.text(0.02, 0.95, "", transform=ax.transAxes, fontsize=9, color="#374151", va="top")

    def init():
        """Init animación — limpia líneas (como initial state en React)."""
        line_raw.set_data([], []); line_ema.set_data([], [])
        return line_raw, line_ema, txt

    def update(frame):
        """Update cada 50ms — lee Serial disponible y actualiza líneas (como useEffect con interval)."""
        # Lee TODO lo disponible sin bloquear — while ser.in_waiting (como socket drain)
        while ser.in_waiting:
            line = ser.readline().decode(errors="ignore").strip()
            if not line or line.startswith("raw") or line.startswith("="):
                continue
            try:
                a,b = line.split(",")[:2]  # primeros 2 campos — ignora si hay más (como split en JS)
                raw = float(a); ema = float(b)
                elapsed = time.time() - t0
                raw_q.append(raw); ema_q.append(ema)  # deque auto-rotates si >200
                all_rows.append((elapsed, raw, ema))  # log completo para CSV final
            except:
                pass  # línea corrupta — ignora (como catch en promise)
        # Actualiza líneas con ventana actual — como setData(newData) en Chart.js
        x = list(range(len(raw_q)))
        line_raw.set_data(x, list(raw_q))
        line_ema.set_data(x, list(ema_q))
        if raw_q:
            # Auto-escala Y según datos — min-5 a max+10 (como domain auto en recharts)
            lo = min(min(raw_q), min(ema_q)) -5; hi = max(max(raw_q), max(ema_q)) +10
            ax.set_ylim(max(0, lo), hi)
            # Ventana deslizante X — últimos 200 (como scroll en log viewer)
            ax.set_xlim(max(0, len(raw_q)-args.window), max(args.window, len(raw_q)))
            # HUD con último valor y conteo — como badge en dashboard
            txt.set_text(f"raw={raw_q[-1]:.1f} ema={ema_q[-1]:.1f} n={len(all_rows)}")
        return line_raw, line_ema, txt

    # Animación 50ms — como setInterval(update, 50) en JS, pero con matplotlib
    ani = FuncAnimation(fig, update, init_func=init, interval=50, blit=False, cache_frame_data=False)
    try:
        plt.show()  # bloquea hasta cerrar ventana (como app.listen en Node)
    except KeyboardInterrupt:
        pass  # Ctrl+C — sale limpio
    finally:
        ser.close()  # siempre cierra puerto — como finally { socket.close() } en JS
        # Guarda al salir si se pidió --save/--png (como auto-save en React)
        if args.save and all_rows:
            Path(args.save).parent.mkdir(parents=True, exist_ok=True)
            with open(args.save, "w", newline="") as f:
                w=csv.writer(f); w.writerow(["time_s","raw","ema"]); w.writerows(all_rows)
            print(f"\nCSV guardado {args.save} ({len(all_rows)} filas)")
        if args.png and all_rows:
            Path(args.png).parent.mkdir(parents=True, exist_ok=True)
            fig.savefig(args.png, dpi=180); print(f"PNG guardado {args.png}")

if __name__ == "__main__":
    main()  # entry — como ReactDOM.render
