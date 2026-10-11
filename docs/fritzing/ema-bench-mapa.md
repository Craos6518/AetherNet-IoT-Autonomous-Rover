# Mapa Esquemático — Banco de Pruebas Filtro EMA (HU-03 / RNF-2.1)

> **Rama:** `feature/stats-ema-prototipo` — aislado Sprint 1-2. El filtro **no es solo Python**: es dual. `stats/ema_filter.py:15` es el **prototipo offline** (validación KPI >85% `prd.md:51`), y `firmware/rover-uno/src/rover.ino:66`, `251` es el **mismo algoritmo en C++** para decisión en tiempo real (evasión `<30 cm`). Ambos usan `α=0.2` (`stats/ema_filter.py:17`, `rover.ino:66`).

---

## 1. Respuesta corta

- **¿Solo Python? No.** Python sirve para: simular ruido, medir `calculate_noise_reduction:99` (`noise_reduction_pct>85`), y barrer α (EST-08). Firmware sirve para: filtrar `sonar.ping_cm()` en el loop 100 Hz antes de `executeAutoMode:322`.
- **¿Qué necesito para probarlo físicamente?** Un **Arduino UNO + HC-SR04** basta. El resto (L298N, TCRT5000, nRF24L01) es opcional para probar el efecto sobre navegación. Abajo tienes 3 bancos, del mínimo al completo.

---

## 2. Bancos de prueba

### Banco A — Solo Python (0 hardware, KPI offline)

```
PC → python stats/ema_filter.py → stats/data/ema_demo.json → matplotlib
     pytest stats/tests/test_ema_filter.py -v  (14 tests, KPI >85%)
```

Nada que cablear. Ideal para validar la matemática antes de flashear. Ver §5 para visualización.

### Banco B — Mínimo físico para ver EMA real (recomendado para Sprint 1)

**Objetivo:** ver `rawDistance` vs `ultrasonicEma` estabilizado en Serial Plotter.

| Componente | Fritzing | Conexión | Origen |
|---|---|---|---|
| Arduino UNO R3 | `Arduino UNO (Rev3)` | USB a PC (115200 baud `rover.ino:115`) | `rover.ino:42` |
| HC-SR04 | `HC-SR04` | **VCC→5V**, **GND→GND**, **TRIG→D2**, **ECHO→D3** | `rover.ino:42` |
| Protoboard + cables Dupont | — | — | — |
| Opcional: LED RGB (feedback visual) | `LED RGB Common Cathode` | **R→44**, **G→45**, **B→46** vía 220Ω a GND | `mega-access` pinout, útil para mapear distancia→color |
| Opcional: KY-037 (para EST-11) | `KY-037` | **VCC→5V**, **GND→GND**, **AO→A3** (libre) | `RNF-2.1` |

> No alimentes HC-SR04 a 3.3V: el ECHO da 5V y puede dañar ESP32. En UNO es seguro.

**Fritzing (Breadboard view):**
1. Arrastra UNO al centro, USB izquierda.
2. Coloca HC-SR04 arriba del UNO, cara de los transductores hacia afuera de la protoboard.
3. Cablea TRIG→D2 (verde), ECHO→D3 (azul), VCC rojo 5V, GND negro.
4. Si añades LED RGB: `R→D44` (no existe en UNO, usa D9/D10/D11 en su lugar si pruebas solo en UNO; si usas MEGA para Banco B-extendido, respeta 44-46).
5. En *Schematic view* verifica bus: 5V/GND compartidos, D2/D3 únicos.

**Código mínimo para Banco B** (extracto ya integrado en `rover.ino:251`):
```cpp
#define EMA_ALPHA 0.2f
float ultrasonicEma = 0; bool inited = false;
unsigned int raw = sonar.ping_cm();
if (raw>0 && raw<=200) {
  if (!inited) { ultrasonicEma = raw; inited=true; }
  else ultrasonicEma = EMA_ALPHA*raw + (1-EMA_ALPHA)*ultrasonicEma;
  // Para Serial Plotter (ver §5.2):
  Serial.print(raw); Serial.print(','); Serial.println(ultrasonicEma);
}
```

### Banco C — Rover completo (validación de impacto en navegación)

Añade al Banco B:
- **L298N** `ENA5 IN1 6 IN2 7 IN3 8 IN4 9 ENB 11` (`rover.ino:34`)
- **3× TCRT5000** `A0 A1 A2` (`rover.ino:47`), potenciómetro umbral `500`
- **nRF24L01** `CE4 CSN10 SCK13 MOSI11 MISO12` (`rover.ino:53`) + condensador 10µF en VCC/GND

Misma lógica EMA, pero ahora `ultrasonicEma<30` dispara `executeAutoMode:337` (giro). Permite medir lag real (6-8 cm) vs reducción ruido.

---

## 3. Mermaid — Flujo de datos del filtro

```mermaid
graph LR
  subgraph PY [Python offline]
    SIM[simulate_noisy_signal] --> EMA_PY[EMAFilter alpha 0.2]
    EMA_PY --> KPI[calculate_noise_reduction >85%]
  end
  subgraph FW [Firmware UNO 100Hz]
    HC[HC-SR04 ping_cm] --> EMA_FW[ultrasonicEma = α·raw + (1-α)·prev]
    EMA_FW --> DEC{ultrasonicEma < 30?}
    DEC -->|sí| GIRO[executeAutoMode giro]
    DEC -->|no| AVANCE[avance BASE 120]
  end
  KPI -. comparte α .-> EMA_FW
```

---

## 4. Por qué no es “solo Python” — trazabilidad

| Capa | Archivo | α | Qué valida | Cuándo |
|---|---|---|---|---|
| Offline | `stats/ema_filter.py:17` | 0.2 | `test_noise_reduction_kpi:78` (87.4% con σ=10) | Sprint 1-2 sin hardware |
| Firmware | `rover.ino:66`, `258` | 0.2f | Lectura real 0-200 cm, `OBSTACLE_DISTANCE_CM 30` | Sprint 2-3 banco B/C |
| Informe | `docs/materias/informe-estadistica.md` (Sprint 4) | — | Gráficas `filtered vs raw` desde `stats/data/*.csv` | EST-07 |

---

## 5. Cómo probarlo visualmente (3 formas, de más fácil a más completa)

### 5.1 Python + matplotlib (sin hardware, 30 segundos)

```bash
cd stats
python -m pip install -r requirements.txt   # ya incluye matplotlib>=3.8.0
python ema_filter.py                        # genera data/ema_demo.json
python visualize_ema.py                     # ← nuevo helper (ver §6)
# O directo:
pytest tests/test_ema_filter.py -v         # valida KPI
```

**Qué verás:** ventana con 3 curvas: `True 50 cm` (gris), `Raw noisy` (rojo picos), `Filtered EMA α0.2` (azul suave) + KPI `% reducción` en título. Prueba `python visualize_ema.py --alpha 0.1 --alpha 0.5 --live-obstacle` para ver lag vs suavizado.

### 5.2 Arduino Serial Plotter (con hardware Banco B, 1 minuto)

1. Flashea `firmware/rover-uno`:
   ```bash
   arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno
   arduino-cli upload -p /dev/ttyACM0 --fqbn arduino:avr:uno ./firmware/rover-uno
   ```
   o `pio run -t upload -d firmware/rover-uno`
2. Abre **Tools → Serial Plotter** (Arduino IDE) a **115200 baud**.
3. Mueve una carpeta/cartón frente al HC-SR04 de 100→20 cm.

**Qué verás:** dos trazas en tiempo real: `raw` (dentada) y `ultrasonicEma` (suave, retraso 3-4 muestras). Con el código de `§2 Banco B` la línea es `raw,filtered` — el Plotter las superpone automáticamente. Haz una palmada cerca del sensor: el pico crudo desaparece en la traza filtrada.

> **Wokwi sin hardware:** https://wokwi.com → nuevo proyecto UNO + HC-SR04 → pega `rover.ino` (solo `readSensors()`+Serial) → corre y mueve el slider de distancia del HC-SR04 virtual.

### 5.3 LED RGB como indicador físico (sin PC)

En `mega-access` o UNO con LED:
```cpp
// Mapea distancia filtrada a color:
if (ultrasonicEma < 15) setLedColor(255,0,0);      // rojo crítico
else if (ultrasonicEma < 30) setLedColor(255,255,0); // amarillo obstáculo
else setLedColor(0,255,0);                           // verde libre
```
Verás el LED cambiar con retardo y sin parpadeo (filtrado), vs raw que parpadearía.

---

## 6. Script `stats/visualize_ema.py` (creado en esta rama)

Uso:
```bash
python stats/visualize_ema.py                 # demo 50cm + σ8, α0.2
python stats/visualize_ema.py --alpha 0.5     # más nervioso
python stats/visualize_ema.py --compare       # 0.1 / 0.2 / 0.5 / 0.8 superpuestos
python stats/visualize_ema.py --live-obstacle # rampa 100→10 cm (simula acercamiento)
python stats/visualize_ema.py --save docs/fritzing/ema-demo.png
```

El script reutiliza `EMAFilter`/`MultiSensorEMA` y `simulate_noisy_signal` — no duplica lógica.

---

## 7. Checklist banco EMA

- [ ] `pytest` verde + `ema_demo.json` generado
- [ ] `visualize_ema.py` muestra >85% reducción
- [ ] UNO flasheado, Serial Plotter con dos trazas estables
- [ ] Obstáculo 100→20 cm: filtrada detecta en ≤4 muestras, sin falsos picos

**Siguiente:** EST-08 `stats/experiments/alpha_sweep.py` (100 Monte Carlo × 6 α) genera `alpha_sweep.json` + PNG para decidir si α=0.2 se mantiene (requiere `AGENTS.md:4` confirmación humana si cambias umbral).
