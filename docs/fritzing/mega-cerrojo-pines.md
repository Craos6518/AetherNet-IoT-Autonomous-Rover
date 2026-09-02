# Plano de Pines — MEGA Cerrojo RF-2.2 / HU-01

> **Para Fritzing** — `feature/firmware-mega-cerrojo` — sin relés (inventario actual)
> Fuente canónica: `firmware/mega-access/src/config.h` + `docs/hardware-inventory.md:25`
> Exporta este plano como `mega-cerrojo-v1.fzz` y deja el PNG/SVG en `docs/fritzing/`.

---

## 1. Vista rápida (qué vas a dibujar)

```
┌──────────────────── ARDUINO MEGA 2560 ────────────────────┐
│                                                           │
│  22 ──► Keypad R1                                         │
│  24 ──► R2          9  ──► Servo MG90S (señal, naranja)  │
│  26 ──► R3         44 ──► LED R (220Ω)                    │
│  28 ──► R4         45 ──► LED G (220Ω)  ← verde 5000ms   │
│  30 ──► C1         46 ──► LED B (220Ω)                    │
│  32 ──► C2         16 ←── ESP32 GPIO17 (TX2) Serial2 RX  │
│  34 ──► C3         17 ──► ESP32 GPIO16 (RX2) Serial2 TX  │
│  36 ──► C4         GND ── GND común + LED cátodo + Servo  │
│                                                           │
│  7,8 reservados láser KY-008 — NO cablear en esta rama    │
│  40-43,47-49,38 LIBRES — ex-relés eliminados               │
└───────────────────────────────────────────────────────────┘
```

UART: `Serial2 115200` (`GATEWAY_SERIAL` en `config.h:37`). Alimentación servo **externa 5V 2A** con GND común — no uses el 5V del MEGA para el MG90S en Fritzing (pon fuente externa).

---

## 2. Tabla maestra para Fritzing (copia tal cual)

| Función | Pin MEGA | Dirección | Cable Fritzing (color sugerido) | Parte Fritzing |
|---------|----------|-----------|----------------------------------|----------------|
| Keypad Row 0 | **22** | IN | Gris → `R1` | `Keypad 4x4 membrane` R1 |
| Row 1 | **24** | IN | Gris → `R2` | R2 |
| Row 2 | **26** | IN | Gris → `R3` | R3 |
| Row 3 | **28** | IN | Gris → `R4` | R4 |
| Col 0 | **30** | OUT | Amarillo → `C1` | C1 |
| Col 1 | **32** | OUT | Amarillo → `C2` | C2 |
| Col 2 | **34** | OUT | Amarillo → `C3` | C3 |
| Col 3 | **36** | OUT | Amarillo → `C4` | C4 |
| **Servo señal** | **9 PWM** | OUT | Naranja → SIG | `Servo` (MG90S) |
| Servo VCC | **5V EXT** | PWR | Rojo → VCC (fuente ext) | No al 5V MEGA |
| Servo GND | **GND** | GND | Marrón → GND | GND común |
| **LED R** | **44 PWM PL5** | OUT | Rojo 220Ω en serie → anodo R | `RGB LED Cathode` |
| **LED G** | **45 PWM PL4** | OUT | Verde 220Ω → anodo G | idem — **verde = `DOOR_AUTO_LOCK_MS=5000` mientras desbloqueada** |
| **LED B** | **46 PWM PL3** | OUT | Azul 220Ω → anodo B | idem — azul 50ms por dígito |
| LED cátodo | **GND** | GND | Negro → GND | común |
| UART RX2 | **16** | IN | Violeta ← ESP32 `GPIO17` TX2 | `ESP32 DevKit V1` |
| UART TX2 | **17** | OUT | Violeta → ESP32 `GPIO16` RX2 | |
| UART GND | **GND** | GND | Negro ↔ ESP32 GND | imprescindible |

> **Si tu LED es ánodo común:** invierte lógica en `src/led.cpp: analogWrite(255-v)`. El plano físico es idéntico.
> **Resistencias:** 220Ω en cada ánodo (no en cátodo) — Fritzing las pone en protoboard entre MEGA y LED.

---

## 3. Pasos en Fritzing

**Breadboard:**
1. Arrastra `Arduino MEGA 2560` centro, `ESP32` arriba-der (solo referencia UART).
2. Keypad abajo-izq → 8 dupont a 22,24,26,28 / 30,32,34,36.
3. Protoboard central: monta `RGB LED` + 3× `220Ω` → 44/45/46.
4. Servo derecha → SIG a 9, VCC/GND a rail externo 5V (etiqueta “MG90S EXT 5V”).
5. UART: 16↔17 cruzados + GND↔GND. Añade label “Serial2 115200”.
6. Deja pines 7/8 vacíos con nota “Láser KY-008 → feature/firmware-mega-laser”.

**Schematic:**
- MEGA como MCU, keypad como matriz de pulsadores, servo como conector 3p, LED con 3 resistencias, ESP32 como bloque con UART. Añade notas: `VALID_PIN="1234" config.h:45`, `SERVO 0°/90°`, `LED verde sincronizado 5000ms`.

**PCB:** ignorar en Sprint 2.

**Piezas que no vienen por defecto:**
- Keypad 4x4 membrana: `File → Open → keypad.fzpz` (Adafruit Fritzing repo). Fallback: 8× `Generic Header 1p`.
- MG90S: usa `Servo` genérico, en Inspector `pin 9`.

Guarda como `docs/fritzing/mega-cerrojo-v1.fzz` + exporta `File → Export → as Image → PNG/SVG`.

---

## 4. Correspondencia con código

- `config.h:18-19` ROW/COL → tabla arriba
- `config.h:22-24` SERVO_PIN 9, 0°/90°
- `config.h:27-29` LED 44/45/46
- `config.h:49` DOOR_AUTO_LOCK_MS 5000 → LED verde
- `config.h:37-39` Serial2 16/17 115200 → `mega-access.ino:55` / `gateway-esp32.ino`

---

## 5. Checklist pre-export

- [ ] Sin `Relay Module` en ninguna vista
- [ ] 44/45/46 solo LED
- [ ] 22/24/26/28/30/32/34/36 solo keypad
- [ ] 9 solo servo (PWM)
- [ ] 16/17 cruzados + GND
- [ ] 3×220Ω visibles
- [ ] Notas: verde 5000ms / rojo 1s fallo / azul 50ms dígito / `*` borra

---

## 6. Archivo SVG de referencia

Ver `mega-cerrojo-wiring.svg` en esta misma carpeta — diagrama esquemático simplificado listo para incluir en docs o imprimir para cableado físico.
