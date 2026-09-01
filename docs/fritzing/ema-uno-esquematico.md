# Esquemático — Banco de Pruebas EMA Solo Arduino UNO (HU-03 / RNF-2.1)

> **Rama:** `feature/stats-ema-prototipo` — prueba aislada **solo UNO**, **ESP32 en otra prueba** (no se cablea aquí).  
> **Filtro:** `stats/ema_filter.py:15` (`EMAFilter` α=0.2) → `firmware/rover-uno/src/rover.ino:66`, `firmware/test-ema-uno/test-ema-uno.ino:9` (mismo `EMA_ALPHA 0.2f`).  
> **Objetivo:** visualizar `raw` vs `ema` estabilizado para validar KPI `>85%` `prd.md:51` sin depender de RF/MQTT.

---

## 1. Vista general

```
[PC USB] ──115200──> [Arduino UNO] ──D2/D3──> [HC-SR04] ──ultrasonido──> [Objeto 10-100 cm]
       └─ Serial Plotter: raw (rojo, ruidoso) + ema (azul, suave) 20 Hz
```

Sin ESP32, sin nRF24L01, sin L298N, sin MEGA. Solo 4 cables.

---

## 2. Bill of Materials (BoM)

| # | Componente | Cant | Fritzing Part | Nota |
|---|---|---|---|---|
| 1 | Arduino UNO R3 | 1 | `Arduino UNO (Rev3)` | Alimenta todo por USB 5V |
| 2 | HC-SR04 (ultrasonido) | 1 | `HC-SR04` | Rango 2-400 cm, `MAX_DISTANCE_CM 200` `test-ema-uno.ino:12` |
| 3 | Protoboard half-size | 1 | `Breadboard` | Opcional: aloja HC-SR04 |
| 4 | Cables Dupont macho-hembra | 4 | `Wire` | TRIG/ECHO/VCC/GND |
| 5 | Cable USB A-B | 1 | — | 115200 baud `test-ema-uno.ino:21` |

**No incluir:** ESP32, MEGA, nRF24L01, L298N, TCRT5000, KY-037 — quedan para otros bancos.

---

## 3. Esquemático y cableado

### 3.1 Tabla netlist (para Fritzing Breadboard → Schematic)

| HC-SR04 Pin | → | Arduino UNO Pin | Color sugerido | Función |
|---|---|---|---|---|
| **VCC** | → | **5V** | Rojo | Alimentación 5V (no 3.3V) |
| **GND** | → | **GND** | Negro | Masa común |
| **TRIG** | → | **D2** | Verde | Disparo `ULTRASONIC_TRIG 2` `test-ema-uno.ino:10` |
| **ECHO** | → | **D3** | Azul | Eco `ULTRASONIC_ECHO 3` — 5V tolerado en UNO |
| — | — | **USB** → PC | — | Serial 115200 `raw,ema` |

> **Aviso:** HC-SR04 ECHO devuelve 5V. En UNO es seguro; en ESP32 requeriría divisor (no aplica aquí).

### 3.2 Breadboard (Fritzing)

1. Arrastra `Arduino UNO` al centro, USB izquierda.
2. Coloca `Breadboard` arriba. Inserta `HC-SR04` con transductores hacia afuera (fuera de la protoboard).
3. Conecta `HC-SR04 VCC` (rojo) al rail `5V` del UNO, `GND` (negro) a `GND`.
4. `TRIG` (verde) a `D2`, `ECHO` (azul) a `D3`. Grosor `0.7 mm` en Inspector.
5. `Schematic view`: verifica 5V/GND bus compartido, D2/D3 únicos sin junctions fantasmas.

### 3.3 Esquemático (Símbolos)

```
         HC-SR04                Arduino UNO
        ┌───────┐               ┌─────────┐
   5V ──┤VCC    ├─┐         ┌───┤5V       │
  GND ──┤GND    ├─┤         │   │GND      ├── GND
   D2 ──┤TRIG   │ │    D2 ◄─┘   │D2 (TRIG)│
   D3 ──┤ECHO   │ └────D3 ◄─────┤D3 (ECHO)│
        └───────┘               │USB      ├── PC 115200
                                └─────────┘
```

### 3.4 Diagrama Mermaid (flujo de datos)

```mermaid
graph LR
  HC[HC-SR04<br/>ping_cm 0-200] --> RAW[raw<br/>ruidoso ±8cm]
  RAW --> EMA[EMA α=0.2<br/>ema = α·raw + (1-α)·prev<br/>test-ema-uno.ino:31]
  EMA --> SER[Serial 115200<br/>raw,ema 20Hz]
  SER --> PLOT[Serial Plotter<br/>raw rojo / ema azul]
  PLOT --> DEC{ema < 30?}
  DEC -->|aplica en rover.ino:322| GIRO
```

---

## 4. Firmware

**Archivo:** `firmware/test-ema-uno/test-ema-uno.ino:1` (compila 12% flash, verificado `arduino-cli compile --fqbn arduino:avr:uno`)

```cpp
#define EMA_ALPHA 0.2f
float ema = 0; bool inited = false;
unsigned int raw = sonar.ping_cm();
if (!inited) ema = raw; else ema = EMA_ALPHA*raw + (1-EMA_ALPHA)*ema;
Serial.print(raw); Serial.print(","); Serial.println(ema,1);
```

Loop 20 Hz (`millis 50 ms` `test-ema-uno.ino:36`) para Plotter estable.

---

## 5. Cómo probar (visual, sin Python)

1. **Flasheo:**
   ```bash
   arduino-cli compile --fqbn arduino:avr:uno ./firmware/test-ema-uno
   arduino-cli upload -p /dev/ttyACM0 --fqbn arduino:avr:uno ./firmware/test-ema-uno
   ```
2. **Plotter:** Arduino IDE → `Tools → Serial Plotter` → `115200 baud`. Verás 2 trazas (leyenda `raw,ema` `test-ema-uno.ino:22`).
3. **Monitor:** `Tools → Serial Monitor` → líneas `42,45.1`.
4. **Gestos:**
   - Objeto fijo 50 cm → `ema` estable, `raw` ±8 cm.
   - Rampa 100→20 cm → `ema` retrasa 3-4 muestras (lag 6-8 cm real) y cruza 30 cm sin rebote.
   - Spike (mano 5 cm y retira) → `raw` 150, `ema` ≈70 — pico atenuado, no dispara falso obstáculo.

**Sin hardware físico:** Wokwi → proyecto UNO + HC-SR04 virtual → pega `test-ema-uno.ino` → mueve slider distancia.

---

## 6. Relación con prototipo Python

| Capa | Archivo | Qué valida |
|---|---|---|
| Offline | `stats/ema_filter.py:99` + `stats/visualize_ema.py` | `calculate_noise_reduction` 89.3% (`noise_std 8`) / 87.4% (`noise_std 10` KPI) |
| Firmware UNO | `test-ema-uno.ino:31` | Mismo α=0.2 sobre `ping_cm()` real, umbral `30 cm` rover |

Python valida KPI; UNO valida que el KPI se mantiene con ultrasonido real y retardo aceptable.

---

## 7. Checklist

- [ ] `HC-SR04` 5V/GND correctos (no 3.3V)
- [ ] `D2`/`D3` sin conflicto (libres)
- [ ] `firmware/test-ema-uno` compila y sube
- [ ] Plotter muestra `raw` + `ema` a 20 Hz
- [ ] Rampa detecta en ≤4 muestras, sin falsos picos

> **Siguiente:** `docs/fritzing/ema-bench-mapa.md` (bancos A/B/C completos) y `stats/visualize_ema.py --compare` para barrido α antes de cerrar EST-01.
