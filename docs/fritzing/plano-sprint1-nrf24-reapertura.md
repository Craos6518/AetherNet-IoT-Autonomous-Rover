# Plano Sprint 1 Reabierto — Prueba nRF24L01 DEVOPS-05

> **Reapertura 2026-09-01** — Cierre pendiente `DEVOPS-05` `docs/testing-rf-sprint1.md:1` y `docs/sprints.md:13`. Sprint 1 no se cierra sin `radio.begin()=true` en ambos nodos + round-trip + fail-safe <500ms + latencia <10ms.
> Fuente pines: `firmware/gateway-esp32/gateway-esp32.ino:60` / `firmware/rover-uno/rover-uno.ino:52` / `docs/fritzing/sprint1-mapa.md:30`

## 1. BoM Mínimo (solo Sprint 1)

| # | Componente | Cant | Rol | Nota |
|---|---|---|---|---|
| 1 | ESP32-WROOM DevKit V1 30p | 1 | Gateway | `firmware/gateway-esp32/gateway-esp32.ino:5` |
| 2 | Arduino UNO R3 | 1 | Rover | `firmware/rover-uno/rover-uno.ino:6` |
| 3 | nRF24L01 2.4GHz + antena PCB | 2 | RF1/RF2 | Canal 76, 2Mbps, PA_HIGH `gateway-esp32.ino:129` / `rover-uno.ino:140` |
| 4 | Condensador electrolítico **10µF ideal** / **22µF válido** | 2 | C1,C2 VCC/GND nRF | **Obligatorio** `docs/testing-rf-sprint1.md:9` — **10µF ideal** (tienes 4× `10µF`, menor ESR, ref. RF24) / **22µF válido** (tienes 2× `22µF`, mejor bulk pico 115mA `firmware/rover-uno/rover-uno.ino:138`). Elegir uno por nodo, no mezclar valores en el mismo banco. |
| 5 | Condensador electrolítico 47µF / 100µF | — | Reserva Sprint 3 | **No usar en nRF** — reservados para bulk `L298N`/`TP4056`/`StepUp 5V` `docs/hardware-inventory.md:10` (tienes 2× 47µF + 3× 100µF). Sobredimensionados para nRF (más ESR, más inrush). |
| 6 | Condensador cerámico 100nF | 2 | C3,C4 opcional | Paralelo a 10µF/22µF para HF (si disponible) |
| 7 | Breadboard half-size | 2 | BB1,BB2 | Uno por nRF, no compartir 3.3V |
| 8 | Regulador AMS1117-3.3 (opcional) | 1 | Backup 3.3V | Si `ERROR: nRF24L01 not detected!` por 3.3V ruidoso del UNO |
| 9 | Cables Dupont M-H + USB A/C | — | SPI + monitores | 2 monitores serie 115200 |

**Excluido Sprint 1:** MEGA 2560, teclado 4x4, MG90S, KY-008, LED RGB 44/45/46, L298N, HC-SR04, TCRT5000, Node-RED.

## 2. Cableado — Tablas para Fritzing

### 2.1 Gateway ESP32 ↔ nRF24L01 #1 (HSPI)

| nRF24L01 #1 | → | ESP32 | Color | Origen |
|---|---|---|---|---|
| VCC 3.3V | → | 3V3 | Rojo | `gateway-esp32.ino:30` + C1 **10µF ideal (o 22µF válido)** a GND ≤5mm del nRF (+ 100nF si hay) |
| GND | → | GND | Negro | — |
| CE | → | GPIO5 | Naranja | `gateway-esp32.ino:60` `NRF_CE_PIN 5` |
| **CSN** | → | **GPIO15*** | Amarillo | **Corrección: `15`, no `18`** — `gateway-esp32.ino:61` actual `18` colisiona con SCK |
| SCK | → | GPIO18 | Verde | SPI HW SCK 18 |
| MOSI | → | GPIO23 | Azul | SPI HW MOSI 23 |
| MISO | → | GPIO19 | Violeta | SPI HW MISO 19 |
| IRQ | → | NC | — | No usado |

> **(*) Errata a corregir antes de flashear:** `firmware/gateway-esp32/gateway-esp32.ino:61` y `firmware/README.md:71` definen `CSN=18` y `SCK=18` mismo pin → `radio.begin()` siempre falla (`gateway-esp32.ino:124` WARN). Cambiar a `#define NRF_CSN_PIN 15` (o 2/21) y recompilar `arduino-cli compile --fqbn esp32:esp32:esp32`. `docs/fritzing/sprint1-mapa.md:43` documenta el conflicto.

### 2.2 Rover UNO ↔ nRF24L01 #2 (SPI HW)

| nRF24L01 #2 | → | Arduino UNO | Color | Origen |
|---|---|---|---|---|
| VCC | → | 3.3V | Rojo | `rover-uno.ino:53` + C2 **10µF ideal (o 22µF válido)** a GND ≤5mm (+ 100nF si hay) |
| GND | → | GND | Negro | — |
| CE | → | D4 | Naranja | `rover-uno.ino:53` `CE=4` |
| CSN | → | D10 | Amarillo | `rover-uno.ino:54` `CSN=10` (SS HW, libre tras mover ENB a 11 `rover-uno.ino:39`) |
| SCK | → | D13 | Verde | SPI HW 13 |
| MOSI | → | D11 | Azul | SPI HW 11 |
| MISO | → | D12 | Violeta | SPI HW 12 |
| IRQ | → | NC | — | — |

Opcional sin interferir: HC-SR04 `TRIG 2 / ECHO 3` para verificar `ultrasonicEma` EMA α=0.2 `rover-uno.ino:66`, pero puede quedar NC en esta prueba.

### 2.3 Reservado Sprint 2 (punteado, NO cablear)

| Señal | ESP32 | ↔ | MEGA | Baud |
|---|---|---|---|---|
| UART2 | TX GPIO17 → RX2 16 | | 115200 | `gateway-esp32.ino:64` |
| UART2 | RX GPIO16 ← TX2 17 | | — | — |

## 3. Diagrama Visual (Mermaid)

```mermaid
graph TB
  subgraph S1 [Sprint 1 Reabierto — DEVOPS-05 Banco RF]
    ESP32[ESP32 Gateway<br/>CE5 CSN15* SCK18 MOSI23 MISO19<br/>115200]
    RF1[nRF24L01 #1<br/>C76 2Mbps PA_HIGH<br/>GATEW / ROVER<br/>+10µF ideal / 22µF válido]
    UNO[Arduino UNO Rover<br/>CE4 CSN10 SCK13 MOSI11 MISO12<br/>ENB11 115200]
    RF2[nRF24L01 #2<br/>+10µF ideal / 22µF válido<br/>FAIL-SAFE 500ms]
    ESP32 ---|SPI HSPI| RF1
    UNO ---|SPI HW| RF2
    RF1 <-->|RF 2.4GHz<br/>RoverCommand 7B {L,R,mode,chk}<br/>Telemetry {ultra,IR,RSSI}| RF2
  end
  MEGA[(MEGA2560<br/>Sprint 2)] -.->|UART 115200<br/>TX17↔RX16<br/>NO CONECTAR| ESP32
  style MEGA stroke-dasharray:6 4,fill:#eee
  style RF1 fill:#d0ebff
  style RF2 fill:#d0ebff
```

## 4. Procedimiento de Cierre (copiado `docs/testing-rf-sprint1.md:12`)

1. Corregir `NRF_CSN_PIN 15` y compilar:
   ```bash
   arduino-cli compile --fqbn esp32:esp32:esp32 ./firmware/gateway-esp32
   arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno
   ```
2. Monitores 115200:
   ```bash
   arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=115200 # ESP32
   arduino-cli monitor -p /dev/ttyUSB1 -c baudrate=115200 # UNO
   ```
3. Test SPI: `nRF24L01 initialized` en ambos. Si `ERROR/WARN not detected` → revisar 3.3V + cap + CSN 15.
4. Round-trip: Gateway `handleRoverCommand` publica `{"left_pwm":120,"right_pwm":120,"mode":1}` → UNO `RF RX: L=120 R=120 mode=1` `rover-uno.ino:202` → ESP32 `RF TX: L=120 R=120` `gateway-esp32.ino:261` + telemetría `aethernet/rover/telemetry`.
5. Fail-safe: cortar alimentación nRF Gateway con Rover en `mode=1` → en <500ms UNO `!!! FAIL-SAFE ACTIVATED !!!` `rover-uno.ino:220` y `stopMotors()`.

## 5. Criterios de Cierre Sprint 1

- [ ] Condensador verificado: `C1/C2 10µF ideal` (o `22µF válido`) + `100nF` opcional pegado VCC/GND ≤5mm — `47/100µF` NO en nRF, reservados `Sprint 3`
- [ ] `gateway-esp32.ino:61` corregido a `15` + `firmware/README.md:71` alineado
- [ ] `radio.begin()=true` ambos lados
- [ ] Round-trip OK con checksum `calculateChecksum`/`verifyChecksum` verde
- [ ] Fail-safe <500ms medido con `millis()` + `FAILSAFE_TIMEOUT_MS 500` `rover-uno.ino:63`
- [ ] Latencia RF <10ms KPI `docs/prd.md:50` (RTT/2 desde `millis()` Gateway `docs/test-plan.md:61`)
- [ ] Export Fritzing `.fzz` → `docs/fritzing/sprint1-rf-link.fzz` + SVG breadboard + PDF schematic

## 6. Fritzing — Pasos

1. `File → New` → guarda `docs/fritzing/sprint1-rf-link.fzz`
2. Breadboard: ESP32 arriba USB izq, UNO abajo, 2 breadboards mini con nRF encima + C **10µF ideal (o 22µF válido)** pegado VCC-GND + `100nF` en paralelo si hay.
3. Cablea §2.1/2.2 con colores indicados, `wire 0.7mm`.
4. Schematic: verificar bus SPI sin junctions fantasma, label `Sprint 1 DEVOPS-05 Reabierto (*) CSN15`.
5. Export `Breadboard SVG + Schematic PDF` + commitear.
