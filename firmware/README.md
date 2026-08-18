# Firmware Directory Structure

```
firmware/
├── gateway-esp32/          # ESP32 Central Gateway
│   ├── src/
│   │   └── gateway.ino     # Main firmware
│   ├── platformio.ini      # PlatformIO config
│   └── lib/                # Custom libraries (if any)
├── mega-access/            # Arduino MEGA - Access Control
│   ├── src/
│   │   └── access_control.ino
│   ├── platformio.ini
│   └── lib/
└── rover-uno/              # Arduino UNO - Autonomous Rover
    ├── src/
    │   └── rover.ino
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
| nRF24L01 (SPI) | CE=5, CSN=18, SCK=18, MOSI=23, MISO=19 |
| UART to MEGA | RX=16 (GPIO16), TX=17 (GPIO17) |

### MEGA Access Control
| Component | Pins |
|-----------|------|
| Keypad 4x4 | Rows: 22,24,26,28 | Cols: 30,32,34,36 |
| Servo MG90S | 9 (PWM) |
| Laser KY-008 | TX=8, RX=7 |
| LED RGB | R=44, G=45, B=46 (PWM) |
| Relays (8ch) | 40-47 |
| UART to ESP32 | RX=16, TX=17 |

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

**MEGA → Gateway:**
- `ACCESS:{"user_id":"...","pin_hash":"...","success":true,"timestamp":12345,"source":"keypad"}`
- `SECURITY:{"event_type":"intrusion","sensor":"laser-01","location":"entrance","severity":"high","timestamp":12345}`
- `RELAY:{"id":0,"state":true}`
- `STATUS:{"door_locked":true,"laser_armed":true,"laser_beam_intact":true,"relays":[false,...],"uptime_ms":12345}`

**Gateway → MEGA:**
- `CMD:ACCESS:{"pin":"1234"}`
- `CMD:RELAY:0:true`
- `CMD:LASER:true`
- `CMD:STATUS`

### MQTT Topics (Gateway ↔ Backend/App/Node-RED)
| Topic | Direction | Description |
|-------|-----------|-------------|
| `aethernet/rover/command` | App → Rover | Joystick commands |
| `aethernet/rover/telemetry` | Rover → App | Telemetry data |
| `aethernet/access/command` | App → MEGA | PIN commands |
| `aethernet/access/event` | MEGA → App | Access granted/denied |
| `aethernet/seguridad/intrusion` | MEGA → Node-RED | Laser intrusion |
| `aethernet/relay/+` | App ↔ MEGA | Relay control |
| `aethernet/relay/event` | MEGA → App | Relay state changes |
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