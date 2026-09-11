# Notebooks Diarios de Campo por Materia — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convertir la documentación dispersa en 7 diarios de campo estilo notebook (uno por materia + Firmware/Hardware) con estructura uniforme, logs reales, fotos/fritzing, roadmaps actualizados con temas y búsquedas Google, y gráficos nuevos — todo versionado en `docs/` y `notebooks/`.

**Architecture:** Scaffold primero (`docs/<Materia>/` + `notebooks/Diario_*.ipynb`), luego poblar cada diario con celdas markdown/code que leen fuentes canónicas (`firmware/`, `app/`, `backend/`, `docs/logs/`, `stats/Dataset/`, `docs/fritzing/`). Roadmaps se actualizan en paralelo con tablas Tema→Profundidad→Requisito→Búsqueda Google. Fotos faltantes quedan como placeholders `![FOTO PENDIENTE]` con checklist de captura.

**Tech Stack:** Jupyter Notebooks (Python 3 + Kotlin-info), Markdown, Mermaid, Fritzing (.fzz/.svg/.png), Docker/Mosquitto/PostgreSQL/FastAPI, arduino-cli logs, Pandas/SciPy/Matplotlib, Jetpack Compose screenshots.

**Spec:** Request del usuario 2026-09-11 (este chat) + `AGENTS.md:1` + `docs/prd.md` + `docs/requirements.md` (HU-01..04, RF-1.1..4.2, RNF-1.1..3.1) + `docs/roadmap.md` + `docs/materias/README.md` + `docs/materias/*.md` + `docs/hardware-inventory.md` + `docs/sprints.md` + `docs/backlog.md` + `notebooks/README.md` + `stats/Dataset/README.md`.

## Global Constraints

- 100% FOSS — ninguna dependencia de nube propietaria (RNF-3.1, `prd.md:53`). No introducir SDK de pago.
- No cambiar protocolo inter-subsistema (RF/UART/MQTT) sin confirmación humana (`AGENTS.md:4`).
- No modificar umbrales de seguridad (láser, PIN `1234`, EMA `α=0.2`) sin confirmación.
- Cada cambio cita HU/RF que implementa (`AGENTS.md:3`).
- Criterios BDD de `requirements.md` son fuente de verdad para tests.
- Notebooks canónicos viven en `notebooks/`; `stats/notebooks/` es espejo y `docs/notebooks/README.md` es redirección — no editar espejos directamente.
- Firmware compila con `arduino-cli 1.5.1` en CI; todo push firmware debe pasar CI.
- Fotos/fritzing: si no existe, dejar placeholder explícito con instrucción de captura, no inventar.

---

## File Structure

```
docs/
├── materias/                          # existe — se amplía, no se borra
│   ├── README.md                      # MODIFICAR: añadir filas Firmware/Hardware/LowCode + mapa diarios
│   ├── estadistica.md                 # MODIFICAR: § brechas con nuevos gráficos/datasets 36.5k
│   ├── programacion-movil.md          # MODIFICAR: estado real MOV-05/06 + screenshots
│   ├── devops.md                      # MODIFICAR: T6 monitoreo con logs reales
│   ├── administracion-proyectos.md    # MODIFICAR: Gantt/tablero enlaces a diarios
│   ├── firmware.md                    # CREAR: mapeo académico Firmware (nuevo, sin PDF propio)
│   ├── hardware.md                    # CREAR: mapeo Hardware/Inventario
│   ├── roadmap-estadistica.md         # MODIFICAR: + tabla búsquedas Google
│   ├── roadmap-movil.md               # MODIFICAR: + búsquedas Google
│   ├── roadmap-devops.md              # MODIFICAR: + búsquedas Google
│   ├── roadmap-administracion.md      # MODIFICAR: + búsquedas Google
│   ├── roadmap-firmware.md            # CREAR
│   ├── roadmap-hardware.md            # CREAR
│   └── roadmap-lowcode.md             # CREAR (ex backlog-lowcode)
├── Estadistica/                       # existe — mantener, añadir subcarpeta Fotos si falta
│   └── Datasets/README.md             # MODIFICAR: link a nuevos gráficos
├── Firmware/                          # CREAR subcarpeta docs/Firmware/
│   ├── README.md                      # CREAR: índice firmware + fotos placeholders
│   ├── fotos/                         # CREAR
│   └── fritzing/ -> ../fritzing/      # symlink o README con referencia
├── Hardware/                          # CREAR docs/Hardware/
│   ├── README.md                      # CREAR
│   └── fotos/                         # CREAR
├── Programacion-Movil/                # CREAR docs/Programacion-Movil/
│   ├── README.md                      # CREAR
│   └── capturas/                      # CREAR
├── DevOps/                            # CREAR docs/DevOps/
│   ├── README.md                      # CREAR
│   └── logs/ -> ../logs/              # referencia
├── Administracion-Proyectos/          # CREAR docs/Administracion-Proyectos/
│   └── README.md                      # CREAR
├── Automatizacion-LowCode/            # CREAR docs/Automatizacion-LowCode/
│   └── README.md                      # CREAR
├── notebooks/README.md                # MODIFICAR: ampliar índice 3→7 diarios
├── roadmap.md                         # MODIFICAR: añadir §6 Firmware y §7 Hardware + búsquedas
└── hardware-inventory.md              # MODIFICAR: añadir tabla fotos por subsistema

notebooks/                              # CANÓNICO
├── AetherControl_Notebook.ipynb        # MODIFICAR: añadir § capturas + logs MQTT
├── EMA_Estadistica.ipynb               # MODIFICAR: añadir gráficos nuevos 36.5k
├── Firmware_Notebook.ipynb             # MODIFICAR: añadir fotos placeholders + checklist
├── Diario_Hardware.ipynb               # CREAR — diario Hardware (fotos, inventario, fritzing)
├── Diario_DevOps.ipynb                 # CREAR — diario DevOps (Docker, CI, Mosquitto)
├── Diario_Administracion.ipynb         # CREAR — diario Administración (Scrum, riesgos, Gantt)
└── Diario_LowCode.ipynb                # CREAR — diario Automatización (Telegram, Node-RED deuda)
├── README.md                          # MODIFICAR: tabla 7 notebooks

stats/
├── water_turbidity_analysis.py         # MODIFICAR: generar 2 PNGs nuevos si faltan
├── data/water_turbidity_report.json    # REFERENCIADO (no tocar sin datos nuevos)
└── notebooks/EMA_Estadistica.ipynb     # ESPEJO: cp tras editar canónico

docs/fritzing/
├── compendio-planos.md                # MODIFICAR: añadir placeholders fotos reales
└── *.png/*.fzz/*.svg                  # REFERENCIADOS

docs/logs/firmware_sprint3/
└── live_2026-09-11/T_telemetry_invertida.log  # REFERENCIADO
```

---

### Task 1: Scaffold — subcarpetas por materia + auditoría fotos/logs existentes

**Files:**
- Create: `docs/Firmware/README.md`, `docs/Hardware/README.md`, `docs/Programacion-Movil/README.md`, `docs/DevOps/README.md`, `docs/Administracion-Proyectos/README.md`, `docs/Automatizacion-LowCode/README.md`
- Create dirs: `docs/Firmware/fotos/`, `docs/Hardware/fotos/`, `docs/Programacion-Movil/capturas/`
- Modify: `docs/materias/README.md:1-21` (añadir 3 filas)
- Modify: `docs/hardware-inventory.md:1-52` (añadir § Fotos por subsistema)

**Interfaces:**
- Consumes: inventario `docs/hardware-inventory.md`, `docs/fritzing/*.png`, `docs/logs/firmware_sprint3/*`
- Produces: estructura de carpetas que todas las tareas siguientes asumen existente; `docs/materias/README.md` actualizado con 7 materias.

- [ ] **Step 1: Verificar estado actual de docs y notebooks**

```bash
ls -R docs/materias docs/fritzing notebooks stats/Dataset 2>/dev/null | head -n 120
ls -l docs/logs/firmware_sprint3/live_2026-09-11/ 2>/dev/null
cat docs/materias/README.md
```

- [ ] **Step 2: Crear scaffold de carpetas y READMEs placeholder**

```bash
mkdir -p docs/Firmware/fotos docs/Hardware/fotos docs/Programacion-Movil/capturas docs/DevOps docs/Administracion-Proyectos docs/Automatizacion-LowCode
```

Contenido `docs/Firmware/README.md` (crear):

```markdown
# Diario de Campo — Firmware

> Fotos del hardware real van en `fotos/` (ver checklist). Fritzing en `docs/fritzing/`.

## Checklist fotos pendientes
- [ ] MEGA 2560 + Keypad 4x4 + Servo MG90S + LED RGB 44/45/46 (vista panel) — `fotos/mega-panel.jpg`
- [ ] KY-008 + LDR discreta con tubo negro — `fotos/laser-ldr.jpg`
- [ ] Gateway ESP32 + nRF24L01 + divisor 5V→3.3V — `fotos/gateway-esp32.jpg`
- [ ] Rover UNO + L298N + HC-SR04 + TCRT×3 — `fotos/rover-uno.jpg`
- [ ] Chasis TT 6V 1:48 ×4 (calce 2026-09-11) — `fotos/chasis-tt.jpg`

Si falta: deja `![FOTO PENDIENTE](fotos/mega-panel.jpg)` en el notebook y marca [ ] arriba.
```

Repetir patrón para `docs/Hardware/README.md` (inventario visual), `docs/Programacion-Movil/README.md` (capturas APP), `docs/DevOps/README.md` (logs Docker), `docs/Administracion-Proyectos/README.md` (Gantt/tablero), `docs/Automatizacion-LowCode/README.md` (Telegram).

- [ ] **Step 3: Actualizar `docs/materias/README.md` — añadir 3 filas**

```markdown
| Firmware (C++/arduino-cli) | — | `firmware/*` + `docs/Firmware/` | [firmware.md](firmware.md) | [roadmap-firmware.md](roadmap-firmware.md) | — |
| Hardware (Electrónica) | — | `docs/Hardware/` + `docs/hardware-inventory.md` + `docs/fritzing/` | [hardware.md](hardware.md) | [roadmap-hardware.md](roadmap-hardware.md) | — |
| Automatización LowCode | — | `automation/` + `docs/Automatizacion-LowCode/` | — | [roadmap-lowcode.md](roadmap-lowcode.md) | [backlog-lowcode.md](backlog-lowcode.md) |
```

- [ ] **Step 4: Añadir § Fotos por subsistema a `docs/hardware-inventory.md`**

```markdown
## Fotos por subsistema (diario de campo)

| Subsistema | Fritzing | Foto real | Estado |
|---|---|---|---|
| MEGA Cerrojo | `docs/fritzing/AetherNet-P3-MEGA-Cerrojo-v1-breadboard.png` | `docs/Firmware/fotos/mega-panel.jpg` | ![FOTO PENDIENTE] si no existe |
| Rover | `docs/fritzing/AetherNet-P4-Rover-v1-breadboard.png` | `docs/Hardware/fotos/rover-uno.jpg` | ![FOTO PENDIENTE] |
| Gateway | `docs/fritzing/AetherNet-P2-RF-Link-v1-breadboard.png` | `docs/Firmware/fotos/gateway-esp32.jpg` | ![FOTO PENDIENTE] |
```

- [ ] **Step 5: Commit scaffold**

```bash
git add docs/Firmware docs/Hardware docs/Programacion-Movil docs/DevOps docs/Administracion-Proyectos docs/Automatizacion-LowCode docs/materias/README.md docs/hardware-inventory.md
git commit -m "docs: scaffold diarios campo por materia + carpetas fotos/Firmware/Hardware (Task 1)"
```

---

### Task 2: Diario Hardware — `notebooks/Diario_Hardware.ipynb` + `docs/materias/hardware.md`

**Files:**
- Create: `notebooks/Diario_Hardware.ipynb`
- Create: `docs/materias/hardware.md`
- Modify: `docs/materias/roadmap-hardware.md` (crear si no existe)
- Test: abrir notebook, ejecutar celda 1 verifica `docs/hardware-inventory.md` existe

**Interfaces:**
- Consumes: `docs/hardware-inventory.md:1-52`, `docs/fritzing/*.png`, `docs/Firmware/fotos/*`, `firmware/*/src/config.h`
- Produces: `Diario_Hardware.ipynb` con índice diario de campo; `hardware.md` mapeo académico.

- [ ] **Step 1: Crear `docs/materias/hardware.md` (mapeo sin PDF propio, basado en inventario)**

```markdown
# Documentación Académica — Hardware
Área: inventario físico + electrónica. Fuente: `docs/hardware-inventory.md` + `docs/fritzing/`.
Mapa: Componente → Función → Pines → Foto → Fritzing
Incluir tabla por subsistema (Gateway/MEGA/Rover/Nodos) y § Brechas (fotos pendientes).
```

Incluir trazabilidad a RF-2.2/2.3/3.1/3.2 y HU-01/02/03.

- [ ] **Step 2: Crear `notebooks/Diario_Hardware.ipynb` — 8 celdas mínimo**

Estructura:
- Celda 0 markdown: título, autor, fecha, spec links
- Celda 1 code: `Path("docs/hardware-inventory.md").read_text()[:500]` + lista `docs/fritzing/*.png`
- Celda 2 markdown: Tabla inventario con `![Fritzing](../docs/fritzing/AetherNet-P3-MEGA-Cerrojo-v1-breadboard.png)` + `![FOTO PENDIENTE](../docs/Hardware/fotos/mega-panel.jpg)` con checklist
- Celda 3 markdown: Detalle pines MEGA (44/45/46 LED, 22/24/26/28 ROW, 30/32/34/36 COL, 9 servo, 8 láser TX, 7 LDR, 16/17 UART) — cita `firmware/mega-access/src/config.h`
- Celda 4 code: parse `config.h` con regex `VALID_PIN|LED|LASER|ROW_PINS`
- Celda 5 markdown: Calce Rover TT 6V 1:48 tabla impacto + foto placeholder `fotos/chasis-tt.jpg`
- Celda 6 markdown: Fritzing compendio link + `!FOTO PENDIENTE` instrucciones: "Tomar foto cenital panel MEGA con regla, luz natural, 12MP"
- Celda 7 code: verifica fotos existen `for p in Path("docs/Hardware/fotos").glob("*"): print(p, p.stat().st_size)`
- Celda 8 markdown: Próximos pasos + referencia `docs/Hardware/README.md`

```python
# Celda 1 — verifica inventario
from pathlib import Path
base = Path("docs/hardware-inventory.md")
print(base.read_text()[:800])
for p in sorted(Path("docs/fritzing").glob("*.png")):
    print(p.name, f"{p.stat().st_size/1024:.0f}kB")
```

- [ ] **Step 3: Verificar notebook se abre sin error**

```bash
python3 -c "import json; nb=json.load(open('notebooks/Diario_Hardware.ipynb')); print(len(nb['cells']), [c['cell_type'] for c in nb['cells']])"
# esperado: 9+ celdas, sin JSON error
```

- [ ] **Step 4: Commit**

```bash
git add notebooks/Diario_Hardware.ipynb docs/materias/hardware.md
git commit -m "docs: diario Hardware + hardware.md con fotos/fritzing placeholders (Task 2)"
```

---

### Task 3: Diario Firmware — ampliar `Firmware_Notebook.ipynb` + crear `roadmap-firmware.md`

**Files:**
- Modify: `notebooks/Firmware_Notebook.ipynb` (añadir § fotos + logs completos de todos los firmware)
- Create: `docs/materias/firmware.md`
- Create: `docs/materias/roadmap-firmware.md`
- Test: celdas nuevas parsean `docs/logs/firmware_sprint3/*.log` y `firmware/test-*/`

**Interfaces:**
- Consumes: `firmware/mega-access/src/*`, `firmware/gateway-esp32/gateway-esp32.ino`, `firmware/rover-uno/rover-uno.ino`, `docs/logs/firmware_sprint3/*`, `firmware/test-laser-uno/*`, `firmware/test-ema-uno/*`
- Produces: Firmware_Notebook con diario completo + roadmaps

- [ ] **Step 1: Crear `docs/materias/firmware.md`**

Mapa: T1 C++ Arduino (setup/loop, interrupciones, PWM) → dónde en `mega-access/src/door.cpp`, `rover-uno.ino:80 FAILSAFE`; T2 UART/RF/SPI → `gateway-esp32.ino:59`, `rover-uno.ino:372 verifyChecksum`; T3 sensores → TCRT/HC-SR04/LDR; T4 EMA en firmware → `rover-uno.ino:259`.

- [ ] **Step 2: Crear `docs/materias/roadmap-firmware.md` con tabla búsquedas Google**

```markdown
| Tema | Profundidad | Para qué RF | Búsqueda Google sugerida |
|---|---|---|---|
| Arduino C++ millis() no bloqueante | Fluido | RF-2.2/2.3 | `arduino millis non blocking delay without delay tutorial` |
| nRF24L01 SPI RF24 library | Operativo | RF-2.1/3.1 | `nRF24L01 RF24 library arduino datasheet 2.4GHz` |
| HC-SR04 + EMA α=0.2 | Operativo | HU-03 | `HC-SR04 ultrasonic datasheet timing EMA exponential moving average` |
| TCRT5000 analogRead threshold | Operativo | RF-3.2 | `TCRT5000 datasheet analogRead threshold infrared line tracking` |
| L298N vs TB6612FNG | Conceptual | Rover | `L298N voltage drop 2V vs TB6612FNG datasheet` |
| Servo MG90S PWM | Operativo | RF-2.2 | `MG90S servo datasheet PWM 0-90 degrees Arduino` |
```

- [ ] **Step 3: Añadir 3 celdas a `Firmware_Notebook.ipynb` (sin borrar existentes 454 líneas)**

Nueva § 2b "Fotos hardware — placeholders":
```markdown
![FOTO PENDIENTE](../../docs/Firmware/fotos/mega-panel.jpg) — tomar foto panel MEGA con Keypad+Servo+LED
![FOTO PENDIENTE](../../docs/Firmware/fotos/gateway-esp32.jpg)
```

Nueva celda code 7b:
```python
# Lista fotos firmware — marca pendiente
from pathlib import Path
for p in Path("docs/Firmware/fotos").glob("*"):
    print(p, "OK")
if not list(Path("docs/Firmware/fotos").glob("*.jpg")):
    print("FOTOS PENDIENTES — ver docs/Firmware/README.md checklist")
```

Nueva § 9 "Logs de todos los firmware" con tabla ampliada incluyendo `test-laser-uno`, `test-ema-uno`, `test-nrf24-*` y link `docs/logs/firmware_sprint3/`.

- [ ] **Step 4: Verificar JSON válido**

```bash
python3 -c "import json; json.load(open('notebooks/Firmware_Notebook.ipynb')); print('OK')"
```

- [ ] **Step 5: Commit**

```bash
git add notebooks/Firmware_Notebook.ipynb docs/materias/firmware.md docs/materias/roadmap-firmware.md
git commit -m "docs: diario Firmware — fotos placeholders + logs completos + roadmaps (Task 3)"
```

---

### Task 4: Diario Programación Móvil — ampliar `AetherControl_Notebook.ipynb` + `roadmap-movil` con búsquedas

**Files:**
- Modify: `notebooks/AetherControl_Notebook.ipynb`
- Modify: `docs/materias/roadmap-movil.md:1-71` (añadir § Búsquedas Google)
- Modify: `docs/materias/programacion-movil.md:1-90` (actualizar §3 Estado real con MOV-05/06)
- Create: `docs/Programacion-Movil/capturas/README.md`

**Interfaces:**
- Consumes: `app/src/main/java/com/aethernet/aethercontrol/` (ViewModels, Compose), `docs/logs/firmware_sprint3/*` (MQTT), `app/build.gradle.kts`
- Produces: Notebook Móvil con capturas APP placeholders + roadmap con búsquedas.

- [ ] **Step 1: Crear `docs/Programacion-Movil/capturas/README.md` checklist**

```markdown
# Capturas APP — checklist
- [ ] Dashboard LED local (LedStatusCard verde/rojo) — `capturas/dashboard-led.png` (tomar: Android Studio → Run → screenshot 1080x2400)
- [ ] PinScreen 4×3 dots + throttle — `capturas/pin-screen.png`
- [ ] Joystick Canvas 120dp drag — `capturas/joystick-drag.png`
- [ ] MQTT ●/○/✕ indicador — `capturas/mqtt-status.png`
- [ ] Si falta: `![CAPTURA PENDIENTE](capturas/dashboard-led.png)` en notebook
```

- [ ] **Step 2: Añadir § Capturas a `AetherControl_Notebook.ipynb`**

Dos celdas markdown nuevas tras índice:

```markdown
## 📸 Capturas APP (diario visual)

| Pantalla | Captura | Estado |
|---|---|---|
| Dashboard LED | `![Dashboard](../../docs/Programacion-Movil/capturas/dashboard-led.png)` | ![CAPTURA PENDIENTE] si no existe — tomar en SM-X620 |
| Joystick | `![Joystick](../../docs/Programacion-Movil/capturas/joystick-drag.png)` | ![CAPTURA PENDIENTE] |
```

Celda code:

```python
from pathlib import Path
for p in sorted(Path("docs/Programacion-Movil/capturas").glob("*.png")):
    print(p.name, p.stat().st_size)
if not list(Path("docs/Programacion-Movil/capturas").glob("*.png")):
    print("CAPTURAS PENDIENTES — ver docs/Programacion-Movil/capturas/README.md")
```

- [ ] **Step 3: Actualizar `docs/materias/roadmap-movil.md` — añadir tabla búsquedas Google**

Añadir al final antes de "Orden crítico":

```markdown
## Búsquedas Google por bloque

| Bloque | Búsqueda sugerida (copiar a Google) |
|---|---|
| Compose Canvas joystick | `Jetpack Compose Canvas pointerInput detectDragGestures joystick` |
| Paho MQTT Android | `Eclipse Paho Android MQTT client tutorial 2024 StateFlow` |
| Coroutines StateFlow | `Kotlin StateFlow ViewModel collectAsState lifecycle` |
| Room offline cache | `Android Room database DAO MVVM tutorial` |
| Bluetooth SPP | `Android Bluetooth Classic SPP HC-06 BluetoothSocket tutorial` |
| Retrofit + kotlinx.serialization | `Retrofit kotlinx serialization Android tutorial` |
```

- [ ] **Step 4: Actualizar `docs/materias/programacion-movil.md` §3 Estado real: marcar MOV-05/06 ✅**

Tabla fila Joystick: `✅ Done feature/app-joystick-virtual 2026-09-11 JoystickScreen Canvas 120dp + MqttManager QoS0`.

- [ ] **Step 5: Commit**

```bash
git add notebooks/AetherControl_Notebook.ipynb docs/materias/roadmap-movil.md docs/materias/programacion-movil.md docs/Programacion-Movil/capturas/README.md
git commit -m "docs: diario Movil — capturas APP placeholders + roadmap búsquedas Google (Task 4)"
```

---

### Task 5: Diario Estadística — ampliar `EMA_Estadistica.ipynb` + nuevos gráficos 36.5k

**Files:**
- Modify: `notebooks/EMA_Estadistica.ipynb` (añadir § datasets 36.5k + 2 gráficos nuevos)
- Modify: `stats/water_turbidity_analysis.py` (generar PNGs `water_us_vs_true.png`, `water_ir_by_angle.png` si faltan — ya existen, verificar)
- Modify: `docs/materias/roadmap-estadistica.md`
- Modify: `docs/materias/estadistica.md`
- Test: `pytest stats/tests/test_ema_filter.py -v`

**Interfaces:**
- Consumes: `stats/ema_filter.py:15`, `stats/Dataset/` (31.5k + 5k), `stats/data/water_turbidity_report.json`, `stats/water_turbidity_analysis.py:1`, `docs/fritzing/alpha_sweep.png`, `docs/logs/firmware_sprint3/live_2026-09-11/T_telemetry_invertida.log`
- Produces: Notebook Estadística diario completo con gráficos versionados + roadmap búsquedas.

- [ ] **Step 1: Verificar datasets y gráficos existentes**

```bash
ls -lh stats/Dataset/ stats/data/ docs/fritzing/*.png 2>/dev/null
python3 -c "import json; print(json.load(open('stats/data/water_turbidity_report.json')).keys())" 2>/dev/null | head -n 20
pytest stats/tests/test_ema_filter.py -v 2>&1 | tail -n 20
```

- [ ] **Step 2: Generar 2 gráficos nuevos si faltan (usando `water_turbidity_analysis.py` existente)**

```bash
python3 stats/water_turbidity_analysis.py 2>&1 | tail -n 20
ls -lh docs/fritzing/water_*.png stats/data/*.json 2>/dev/null
```

Si el script no genera PNGs, añadir celdas notebook que sí lo hagan:

```python
# Celda nueva — genera water_us_vs_true.png desde stats/data/
import pandas as pd, matplotlib.pyplot as plt, json
from pathlib import Path
df = pd.read_csv("stats/Dataset/water_turbidity.csv")  # o el path real
# ... plot us_value vs water_level por turbidez
plt.savefig("docs/fritzing/water_us_vs_true.png", dpi=150)
print("PNG generado")
```

- [ ] **Step 3: Añadir § Diario de campo a `EMA_Estadistica.ipynb`**

3 celdas:

- Markdown: `## 📓 Diario de campo — bitácora 36.5k` con tabla `| Fecha | Actividad | Dataset | Gráfico |` y `![EMA UNo](../docs/fritzing/AetherNet-P1-EMA-UNO-v1-breadboard.png)` + `![water_us_vs_true](../docs/fritzing/water_us_vs_true.png)` + `![water_ir_by_angle](../docs/fritzing/water_ir_by_angle.png)` + placeholders `![GRAFICO PENDIENTE](ema-ky037.png)` para KY-037.
- Code: `print(Path("stats/Dataset").stat())` + `len(open("stats/data/water_turbidity_report.json").read())`
- Markdown: Checklist fotos: `docs/Estadistica/Datasets/fotos/` si aplica.

- [ ] **Step 4: Actualizar `docs/materias/roadmap-estadistica.md` — tabla búsquedas Google**

```markdown
## Búsquedas Google por tema (copiar tal cual)

| Tema | Búsqueda sugerida |
|---|---|
| EMA α=0.2 trade-off | `exponential moving average alpha smoothing factor 0.2 tutorial` |
| HC-SR04 ruido gaussiano | `HC-SR04 ultrasonic sensor noise gaussian distribution datasheet` |
| Welch t-Student | `Welch t-test unequal variances SciPy tutorial` |
| ANOVA turbidez | `ANOVA water turbidity sensor angle Python` |
| Kalman vs EMA | `Kalman filter vs exponential moving average Arduino` |
| Pandas describe | `Pandas descriptive statistics water quality dataset Kaggle` |
```

- [ ] **Step 5: Actualizar `docs/materias/estadistica.md` §4 Pendientes — marcar EST-05/06 parcial con gráficos**

- [ ] **Step 6: Commit**

```bash
git add notebooks/EMA_Estadistica.ipynb docs/materias/roadmap-estadistica.md docs/materias/estadistica.md docs/fritzing/water_*.png 2>/dev/null
git cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb 2>/dev/null; git add stats/notebooks/EMA_Estadistica.ipynb 2>/dev/null
git commit -m "docs: diario Estadistica — datasets 36.5k + graficos + búsquedas Google (Task 5)"
```

---

### Task 6: Diario DevOps — `Diario_DevOps.ipynb` + `roadmap-devops.md` con logs reales

**Files:**
- Create: `notebooks/Diario_DevOps.ipynb`
- Modify: `docs/materias/roadmap-devops.md`
- Modify: `docs/materias/devops.md`
- Test: `docker compose config` parse + `cat backend/mosquitto/config/acl.conf`

**Interfaces:**
- Consumes: `docker-compose.yml`, `backend/mosquitto/config/mosquitto.conf`, `backend/mosquitto/config/acl.conf`, `.github/workflows/ci.yml`, `docs/logs/firmware_sprint3/T1_docker*.log`, `backend/app/*`
- Produces: Diario DevOps con logs Docker/Mosquitto/CI + roadmaps búsquedas.

- [ ] **Step 1: Crear `notebooks/Diario_DevOps.ipynb` — 10 celdas**

Índice: 1 Mapa infra, 2 Docker Compose, 3 Mosquitto, 4 CI/CD, 5 Logs T1, 6 Fotos infra, 7 Búsquedas.

Celdas clave:

```python
# Celda — parse docker-compose
import yaml, pathlib
txt = pathlib.Path("docker-compose.yml").read_text()
print(txt[:800])
# acl.conf
print(pathlib.Path("backend/mosquitto/config/acl.conf").read_text())
```

```python
# Celda — logs T1
from pathlib import Path
for p in sorted(Path("docs/logs/firmware_sprint3").glob("T1*.log")):
    print(f"--- {p.name} ---")
    print(p.read_text()[:600])
```

Markdown fotos:

```markdown
![FOTO PENDIENTE](../../docs/DevOps/fotos/rack-docker.jpg) — foto rack/servidor con `docker ps`
![Captura CI](../../docs/DevOps/capturas/ci-green.png) — screenshot GitHub Actions verde
```

- [ ] **Step 2: Actualizar `docs/materias/roadmap-devops.md` — búsquedas Google**

```markdown
| Tema | Búsqueda |
|---|---|
| Docker Compose multi-servicio | `docker compose FastAPI PostgreSQL Mosquitto tutorial` |
| Mosquitto ACL aethernet/# | `Mosquitto MQTT ACL aethernet topic access control` |
| arduino-cli CI | `arduino-cli GitHub Actions compile ESP32 MEGA UNO tutorial` |
| Trivy SARIF | `Trivy security scan GitHub Actions SARIF` |
| Prometheus Grafana (post) | `Prometheus Grafana Mosquitto MQTT monitoring docker` |
```

- [ ] **Step 3: Verificar notebook**

```bash
python3 -c "import json; nb=json.load(open('notebooks/Diario_DevOps.ipynb')); print([c['cell_type'] for c in nb['cells']])"
```

- [ ] **Step 4: Commit**

```bash
git add notebooks/Diario_DevOps.ipynb docs/materias/roadmap-devops.md docs/materias/devops.md
git commit -m "docs: diario DevOps — Docker/Mosquitto/CI logs + búsquedas Google (Task 6)"
```

---

### Task 7: Diario Administración — `Diario_Administracion.ipynb` + `roadmap-administracion.md`

**Files:**
- Create: `notebooks/Diario_Administracion.ipynb`
- Modify: `docs/materias/roadmap-administracion.md`
- Modify: `docs/materias/administracion-proyectos.md`
- Test: parse `docs/sprints.md`, `docs/backlog.md`, `docs/risk-register.md`, `docs/gantt.md`

**Interfaces:**
- Consumes: `docs/sprints.md`, `docs/backlog.md`, `docs/risk-register.md`, `docs/gantt.md`, `docs/tablero-scrum.md`, `docs/UTP/*.pdf`
- Produces: Diario Administración (Scrum, riesgos, Gantt, costos)

- [ ] **Step 1: Crear `notebooks/Diario_Administracion.ipynb` — 8 celdas**

Celdas:

- Markdown: Portada + WBS 40 paquetes + costos COP 176k/1.176M
- Code:

```python
from pathlib import Path
for f in ["docs/sprints.md","docs/backlog.md","docs/risk-register.md","docs/gantt.md"]:
    p = Path(f)
    print(f"{f}: {p.stat().st_size} bytes — {len(p.read_text().splitlines())} líneas")
```

- Markdown: Gantt Mermaid embed `![Gantt](../docs/gantt.md)` + `docs/tablero-scrum.md` 6 columnas Kanban + placeholder `![CAPTURA PENDIENTE](../../docs/Administracion-Proyectos/capturas/tablero-kanban.png)` — instrucción: screenshot `github.com/users/Craos6518/projects/14`.
- Markdown: Matriz riesgos R-01..R-13 con foto placeholder `fotos/reunion-riesgos.jpg`.
- Code: parse backlog MOV/DEVOPS/EST counts.

- [ ] **Step 2: Actualizar `docs/materias/roadmap-administracion.md` — búsquedas Google**

```markdown
| Tema | Búsqueda |
|---|---|
| Scrum backlog MoSCoW | `Scrum backlog MoSCoW prioritization tutorial` |
| WBS 40 paquetes | `WBS work breakdown structure software project example` |
| Matriz riesgos R-01..R-13 | `risk register matrix software project probability impact` |
| Gantt Mermaid | `Mermaid Gantt diagram GitHub markdown tutorial` |
| Planning Poker | `Planning Poker estimation story points tutorial` |
```

- [ ] **Step 3: Commit**

```bash
git add notebooks/Diario_Administracion.ipynb docs/materias/roadmap-administracion.md docs/materias/administracion-proyectos.md
git commit -m "docs: diario Administracion — Scrum/riesgos/Gantt + búsquedas Google (Task 7)"
```

---

### Task 8: Diario LowCode/Automatización — `Diario_LowCode.ipynb` + `roadmap-lowcode.md`

**Files:**
- Create: `notebooks/Diario_LowCode.ipynb`
- Create: `docs/materias/roadmap-lowcode.md`
- Modify: `docs/materias/backlog-lowcode.md` (añadir § diario)
- Test: `cat automation/flows/intrusion_alert.json`, `cat backend/app/routers/events.py | grep telegram`

**Interfaces:**
- Consumes: `automation/flows/intrusion_alert.json`, `backend/app/routers/events.py`, `docs/architecture.md:55`, `docs/backlog.md` LOW-02 deuda
- Produces: Diario LowCode con flujo Telegram + placeholders Node-RED

- [ ] **Step 1: Crear `docs/materias/roadmap-lowcode.md`**

```markdown
# Roadmap — Automatizacion LowCode
| Tema | Profundidad | Búsqueda Google |
|---|---|---|
| Telegram Bot API sendMessage | Implementar | `Telegram Bot API sendMessage BotFather tutorial` |
| Node-RED mqtt in/out (deuda) | Conceptual | `Node-RED MQTT subscribe publish tutorial Mosquitto` |
| Tuya cancelado ADR-001 | Referencia | `Tuya local_key API policy alternative LED RGB` |
| HTTP POST FastAPI security-events | Operativo | `FastAPI POST security events Telegram notification` |
```

- [ ] **Step 2: Crear `notebooks/Diario_LowCode.ipynb` — 7 celdas**

Celdas:
- Markdown: Estado LOW-02 deuda 2026-09-09 + diagrama flujo HU-02 (MEGA→UART→Gateway→MQTT→Telegram+LED)
- Code:

```python
import json, pathlib
flow = json.load(open("automation/flows/intrusion_alert.json"))
print(json.dumps(flow, indent=2)[:800])
# backend telegram
import re
txt = pathlib.Path("backend/app/routers/events.py").read_text()
for l in txt.splitlines():
    if "telegram" in l.lower() or "security" in l.lower():
        print(l)
```

- Markdown: `![FOTO PENDIENTE](../../docs/Automatizacion-LowCode/capturas/telegram-alert.png)` — captura chat BotFather + mensaje intrusión real; `![Node-RED PENDIENTE](../../docs/Automatizacion-LowCode/capturas/node-red-flow.png)` — screenshot flujo si se retoma Sprint 4.
- Markdown: Payloads JSON `aethernet/seguridad/intrusion` + `SECURITY:` UART.

- [ ] **Step 3: Commit**

```bash
git add notebooks/Diario_LowCode.ipynb docs/materias/roadmap-lowcode.md
git commit -m "docs: diario LowCode — Telegram directo + Node-RED deuda + búsquedas (Task 8)"
```

---

### Task 9: Actualizar Roadmaps globales — `docs/roadmap.md` + índices por materia con listas Google + datasheets

**Files:**
- Modify: `docs/roadmap.md:1-120` (añadir §6 Firmware, §7 Hardware, §8 LowCode + tabla consolidada búsquedas)
- Modify: `docs/materias/roadmap-estadistica.md`, `roadmap-movil.md`, `roadmap-devops.md`, `roadmap-administracion.md` (añadir § Datasheets)
- Test: `grep -r "Búsqueda Google" docs/materias/ docs/roadmap.md | wc -l` >= 30

**Interfaces:**
- Consumes: todos los roadmaps previos + `docs/hardware-inventory.md` (datasheets), `stats/Dataset/README.md`
- Produces: Roadmap global coherente con 7 materias y listas buscables.

- [ ] **Step 1: Añadir §6 Firmware y §7 Hardware y §8 LowCode a `docs/roadmap.md`**

```markdown
## 6. Firmware — C++ Embebido (MEGA/Gateway/Rover)
Conocimientos a adquirir: millis() no bloqueante, RF24, HC-SR04 EMA α=0.2, TCRT threshold, L298N, Servo MG90S...
Datasheets: `nRF24L01+` (Nordic), `HC-SR04`, `TCRT5000`, `L298N`, `MG90S`, `ESP32-WROOM-32U`
Búsquedas: ver `docs/materias/roadmap-firmware.md`

## 7. Hardware — Electrónica
Datasheets: `KY-008 laser`, `LDR GL5528`, `nRF24L01`, `TP4056`, `StepUp MT3608`
Búsquedas: ver `docs/materias/roadmap-hardware.md`

## 8. LowCode — Telegram Bot
Búsquedas: ver `docs/materias/roadmap-lowcode.md`
```

- [ ] **Step 2: Añadir § Datasheets a cada roadmap por materia**

Ejemplo `roadmap-firmware.md`:

```markdown
## Datasheets (buscar tal cual)

- `nRF24L01+ datasheet Nordic Semiconductor pdf`
- `HC-SR04 datasheet ultrasonic distance sensor timing diagram`
- `TCRT5000 Vishay datasheet reflective optical sensor`
- `L298N datasheet STMicroelectronics dual H-bridge`
- `ESP32-WROOM-32U datasheet Espressif`
```

Repetir para Hardware (KY-008, LDR, TP4056, MT3608), Estadística (Kaggle water_turbidity, SciPy docs), DevOps (Mosquitto, Docker, arduino-cli), Móvil (Paho, Compose).

- [ ] **Step 3: Verificar cobertura búsquedas**

```bash
grep -c "Búsqueda\|datasheet\|Dataset" docs/materias/roadmap-*.md docs/roadmap.md
# esperado: cada archivo >=3 hits
```

- [ ] **Step 4: Commit**

```bash
git add docs/roadmap.md docs/materias/roadmap-*.md
git commit -m "docs: roadmaps — Temas + búsquedas Google + datasheets/datasets por materia (Task 9)"
```

---

### Task 10: Índices maestros + espejos + verificación final

**Files:**
- Modify: `notebooks/README.md:1-53` (tabla 3→7 notebooks)
- Modify: `docs/notebooks/README.md:1-53` (redirección ampliada)
- Modify: `docs/gantt.md`, `docs/backlog.md` (refs a diarios)
- Test: `ls notebooks/*.ipynb`, `jupyter nbconvert --to markdown` dry-run, `pytest stats/tests/ -q`

**Interfaces:**
- Consumes: todos los diarios creados Tasks 2-8
- Produces: Índices coherentes + verificación

- [ ] **Step 1: Actualizar `notebooks/README.md` — tabla 7 notebooks**

```markdown
| Notebook | Descripción | Materia / Sprint | Ejecutar |
|---|---|---|---|
| `Diario_Hardware.ipynb` | Hardware — inventario, fritzing, fotos, calce TT 6V | Hardware Sprint 1-3 | `jupyter notebook notebooks/Diario_Hardware.ipynb` |
| `Diario_DevOps.ipynb` | DevOps — Docker, Mosquitto, CI/CD, logs T1 | DevOps Sprint 1 | ... |
| `Diario_Administracion.ipynb` | Admin — Scrum, WBS, riesgos, Gantt, costos | Admin transversal | ... |
| `Diario_LowCode.ipynb` | LowCode — Telegram directo, Node-RED deuda | LowCode Sprint 4 | ... |
| (mantener 3 existentes) | ... | ... | ... |
```

```markdown
## Estructura por materia (docs/)

- `docs/Firmware/` — fotos MEGA/Gateway/Rover
- `docs/Hardware/` — fotos chasis/sensores
- `docs/Programacion-Movil/capturas/` — screenshots APP
- `docs/Estadistica/Datasets/` — 36.5k + PNGs
- `docs/DevOps/` — logs Docker
- `docs/Administracion-Proyectos/` — Gantt/tablero
- `docs/Automatizacion-LowCode/` — Telegram
```

- [ ] **Step 2: Actualizar `docs/notebooks/README.md` espejo de compatibilidad**

Añadir tabla 7 notebooks, mantener nota `stats/notebooks/` espejo: `cp notebooks/Diario_*.ipynb` no aplica (solo EMA se espeja), explicar.

- [ ] **Step 3: Sincronizar espejo EMA y verificar notebooks válidos**

```bash
cp notebooks/EMA_Estadistica.ipynb stats/notebooks/EMA_Estadistica.ipynb
python3 -c "import json, pathlib; [json.load(open(str(p))) for p in pathlib.Path('notebooks').glob('*.ipynb')]; print('all notebooks JSON OK')"
pytest stats/tests/test_ema_filter.py -q 2>&1 | tail -n 5
ls -lh notebooks/*.ipynb docs/Firmware/fotos docs/Hardware/fotos docs/Programacion-Movil/capturas 2>&1 | head -n 40
```

- [ ] **Step 4: Verificación Gantt + backlog refs**

```bash
grep -n "Diario\|notebooks/" docs/materias/README.md docs/roadmap.md docs/gantt.md 2>/dev/null | head -n 20
```

Añadir a `docs/gantt.md` nota: "Diarios en `notebooks/Diario_*.ipynb` — ver `notebooks/README.md`".

- [ ] **Step 5: Commit final**

```bash
git add notebooks/README.md docs/notebooks/README.md stats/notebooks/EMA_Estadistica.ipynb docs/gantt.md
git commit -m "docs: índices maestros 7 diarios + espejo EMA + verificación final (Task 10)"
```

---

## Self-Review

**1. Spec coverage:**
- ✅ Subcarpetas `docs/<Materia>` con Firmware/Hardware incluidos → Task 1
- ✅ NOTEBOOK por materia como diario campo → Tasks 2-8 (7 diarios: Hardware, Firmware, Móvil, Estadística, DevOps, Administración, LowCode)
- ✅ Logs firmware/APP si necesitas (solicitados y referenciados) → Tasks 3,4,6 (logs T1-T4, MQTT, `test-*`)
- ✅ Roadmaps con temas necesarios → Task 9
- ✅ Lista temas para buscar en Google (Documentación, datasheets, dataset) → Tasks 3,5,6,7,8,9 (tablas Búsquedas Google por roadmap)
- ✅ Datasets ya implementados + generar nuevos gráficos → Task 5 (36.5k + water_us_vs_true/water_ir_by_angle + alpha_sweep)
- ✅ Fotos y si no están placeholder con espacio para tomarla/crear fritzing o captura APP → Tasks 1-4,6,8 (checklists + `![FOTO PENDIENTE]`/`![CAPTURA PENDIENTE]`)

**2. Placeholder scan:** Ningún "TBD/TODO sin código" — todos los placeholders son intencionales `![FOTO PENDIENTE]` con instrucción de captura, no deuda de implementación.

**3. Type consistency:** Paths `docs/Firmware/fotos/` vs `notebooks/Diario_*.ipynb` relativos `../../docs/...` consistentes; `notebooks/` canónico, `stats/notebooks/` espejo, `docs/notebooks/README.md` redirección — coherente con `notebooks/README.md:13` existente.

---

**Plan completo y guardado en `docs/superpowers/plans/2026-09-11-notebooks-diarios-campo-materias.md`. Dos opciones de ejecución:**

**1. Subagent-Driven (recomendado)** — despacho un subagente fresco por tarea, reviso entre tareas, iteración rápida

**2. Inline Execution** — ejecuto tareas en esta sesión usando executing-plans, batch con checkpoints

**¿Cuál eliges?**
