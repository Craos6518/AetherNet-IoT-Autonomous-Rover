# Firmware Directory Structure

```
firmware/
├── gateway-esp32/          # ESP32 Central Gateway
│   ├── gateway-esp32.ino   # Main firmware (HTTP POST access a backend)
│   ├── platformio.ini      # ArduinoJson 6.21.3 pinneado
│   └── src/                # vacío en Sprint 2 (reservado)
├── mega-access/            # Arduino MEGA — Cerrojo RF-2.2/HU-01
│   ├── mega-access.ino     # Wrapper setup/loop
│   ├── src/config.h        # VALID_PIN="1234", pins, timeouts (sin relés)
│   ├── src/led.{h,cpp}     # LED no bloqueante, verde 5s sincronizado
│   ├── src/door.{h,cpp}    # Servo MG90S, auto-lock 5000
│   ├── src/keypad_control.{h,cpp} # Buffer 6, isDigit ignora A-D
│   ├── src/uart_protocol.{h,cpp} # ACCESS/STATUS, throttled 5s
│   ├── platformio.ini      # Keypad, Servo, ArduinoJson@6.21.3
│   └── lib/
└── rover-uno/              # Arduino UNO - Autonomous Rover
    ├── rover-uno.ino
    ├── src/rover.ino       # duplicado legacy
    ├── platformio.ini
    └── lib/
```

## Building Firmware

### Using PlatformIO (Recommended)

```bash
# Gateway ESP32
cd firmware/gateway-esp32
pio run

# MEGA Access Control
cd firmware/mega-access
pio run

# Rover UNO
cd firmware/rover-uno
pio run
```

### Using Arduino CLI (CI/CD)

```bash
# Gateway ESP32
arduino-cli compile --fqbn esp32:esp32:esp32 ./firmware/gateway-esp32

# MEGA Access Control
arduino-cli compile --fqbn arduino:avr:mega ./firmware/mega-access

# Rover UNO
arduino-cli compile --fqbn arduino:avr:uno ./firmware/rover-uno
```

## Flashing

```bash
# ESP32
pio run -t upload -d firmware/gateway-esp32

# MEGA
pio run -t upload -d firmware/mega-access

# UNO
pio run -t upload -d firmware/rover-uno
```

## Pin Mapping Summary

### Gateway ESP32
| Component | Pins |
|-----------|------|
| nRF24L01 (SPI) | CE=5, CSN=15 (corrige 18 colisión SCK), SCK=18, MOSI=23, MISO=19 |
| UART to MEGA | RX=16 (GPIO16), TX=17 (GPIO17) |

### MEGA Access Control (RF-2.2 / HU-01 — `feature/firmware-mega-cerrojo`)
| Component | Pins | Estado |
|-----------|------|--------|
| Keypad 4x4 | Rows: 22,24,26,28 | Cols: 30,32,34,36 | Activo |
| Servo MG90S | 9 (PWM) `0°` lock / `90°` unlock | Activo |
| LED RGB | R=44, G=45, B=46 (PWM) verde sincronizado a `DOOR_AUTO_LOCK_MS=5000` | Activo |
| Láser KY-008 | TX=8, RX=7 | Reservado — implementado en `feature/firmware-mega-laser` |
| Relés 8ch | — | **Eliminado** (sin hardware inventario 2026-08-26) |
| UART a ESP32 | RX=16, TX=17, 115200 `Serial2` | Activo |

### Rover UNO
| Component | Pins |
|-----------|------|
| L298N | ENA=5, IN1=6, IN2=7, IN3=8, IN4=9, ENB=11 |
| HC-SR04 | TRIG=2, ECHO=3 |
| TCRT5000 x3 | A0, A1, A2 |
| nRF24L01 | CE=4, CSN=10 |

## Communication Protocol

### RF (nRF24L01) - Gateway ↔ Rover
- **Channel**: 76
- **Data Rate**: 2 Mbps
- **PA Level**: HIGH
- **Addresses**: Gateway="GATEW", Rover="ROVER"

**Command Packet (Gateway → Rover):**
```c
struct RoverCommand {
    int16_t left_pwm;    // -255 to 255
    int16_t right_pwm;   // -255 to 255
    uint8_t mode;        // 0=stop, 1=manual, 2=auto
    uint16_t checksum;   // Simple sum of preceding bytes
};
```

**Telemetry Packet (Rover → Gateway, via ACK payload):**
```c
struct RoverTelemetry {
    int16_t left_pwm;
    int16_t right_pwm;
    uint16_t ultrasonic_cm;
    bool ir_left;
    bool ir_center;
    bool ir_right;
    int8_t rf_rssi;
    uint16_t checksum;
};
```

### UART (Gateway ↔ MEGA)
- **Baud**: 115200
- **Format**: `TYPE:JSON_PAYLOAD\n`

**MEGA → Gateway (RF-2.2/HU-01, Sprint 2):**
- `ACCESS:{"user_id":"keypad_user","pin_hash":"<djb2 hex>","success":bool,"timestamp":millis,"source":"keypad"}` → Gateway hace `HTTP POST http://<backend>:8000/api/access-events`
- `STATUS:{"door_locked":bool,"laser_armed":false,"free_ram":int,"uptime_ms":ulong}` (cada `STATUS_INTERVAL_MS=5000`, sin `relays`)
- `SECURITY:` reservado para `feature/firmware-mega-laser` (HU-02)

**Gateway → MEGA:**
- `CMD:ACCESS:{"pin":"1234"}` (validado, reenvía a `processPinAttempt`)
- `CMD:STATUS` (poll)
- `CMD:RELAY` / `RELAY:` / `CMD:LASER` **eliminados en esta rama**

### MQTT Topics (Gateway ↔ Backend/App/Node-RED)
| Topic | Direction | Description |
|-------|-----------|-------------|
| `aethernet/rover/command` | App → Rover | Joystick commands |
| `aethernet/rover/telemetry` | Rover → App | Telemetry data |
| `aethernet/access/command` | App → MEGA | PIN commands |
| `aethernet/access/event` | MEGA → App (fallback) | Access granted/denied — flujo principal ahora HTTP POST directo a `POST /api/access-events` |
| `aethernet/seguridad/intrusion` | MEGA → Node-RED | Laser intrusion (reservado laser) |
| `aethernet/system/status` | Gateway → All | System health |
| `aethernet/system/command` | All → Gateway | System commands |

## Fail-Safe Behavior

**Rover RF Fail-Safe (HU-04):**
- Timeout: 500ms (configurable via `FAILSAFE_TIMEOUT_MS`)
- Action: Immediately stop both motors (PWM=0)
- Recovery: Wait for new valid RF packet, then resume

**MEGA Edge Processing:**
- Keypad/servo/laser/LED operate independently of network
- Events reported to Gateway when UART available
- No cloud dependency for core access control

## EMA Filter (HU-03 / RNF-2.1)

Implemented in Rover firmware for HC-SR04 ultrasonic sensor:
- Formula: `S_t = α * Y_t + (1-α) * S_{t-1}`
- α = 0.2 (per requirements)
- Applied in real-time on UNO before obstacle avoidance decisions