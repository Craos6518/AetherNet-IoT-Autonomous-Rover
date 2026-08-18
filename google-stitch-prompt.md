# Google Stitch Prompt — AetherControl Android App Mockups

## Project Context
**App Name:** AetherControl  
**Platform:** Android (Kotlin, Jetpack Compose, MVVM)  
**Project:** AetherNet IoT & Autonomous Rover — 100% FOSS distributed home automation, access control & mobile robotics platform  
**Target Users:** Home administrators/occupants + university evaluators  
**Network:** Local LAN only (no cloud dependencies) — MQTT/WebSocket + Bluetooth SPP fallback  

---

## Core Screens Required

### 1. **Splash / Onboarding Screen**
- App logo "AetherControl" with subtle animation
- Connection status indicator (Wi-Fi / Bluetooth / Offline)
- Auto-connect to last known MQTT broker
- "Reconnect" button if connection fails
- Navigate to Main Dashboard on success

---

### 2. **Main Dashboard (Home) — *Primary Screen***
**Real-time telemetry dashboard consuming MQTT/WebSocket streams**

**Top Bar:**
- App title "AetherControl"
- Connection status chip: 🟢 Connected / 🟡 Bluetooth Fallback / 🔴 Disconnected
- Alarm mode toggle: 🔓 Disarmed / 🔒 Armed (controls laser trap + notifications)

**Grid Layout (2 columns, scrollable):**

| Card | Content | Data Source |
|------|---------|-------------|
| **Access Status** | Large lock icon (🔒/🔓), last access timestamp, PIN entry button | MEGA UART → ESP32 → MQTT `aethernet/access/status` |
| **Laser Trap** | Laser icon + status (✅ Intact / 🚨 BREACHED), arm/disarm button | MEGA KY-008 → ESP32 → MQTT `aethernet/security/laser` |
| **Room Light (Tuya)** | Color picker + brightness slider + power toggle, "Intrusion Flash" indicator | Node-RED → `tuya-local` → MQTT `aethernet/light/tuya` |
| **Local LED (MEGA)** | RGB color indicator (Green=unlocked, Red=intrusion), read-only | MEGA PWM → ESP32 → MQTT `aethernet/light/local` |
| **Sound Level** | Real-time VU meter bar (0-100%), peak hold | ESP8266 KY-037 → MQTT `aethernet/env/sound` |
| **Relay Bank** | 4-8 toggle switches with labels (configurable), all-on/off | MEGA Relays → ESP32 → MQTT `aethernet/relay/{1..8}` |

**Bottom Navigation Bar (5 tabs):**
1. 🏠 Dashboard (this screen)
2. 🎮 Rover Control
3. 🔐 Access Control
4. 📊 Telemetry History
5. ⚙️ Settings

---

### 3. **Rover Control Screen — *Critical Screen***
**Virtual joystick for tank-drive rover with <10ms RF latency requirement**

**Layout:**
- **Top:** Connection quality indicator (RF signal strength bars, latency ms, packet loss %)
- **Center:** Large virtual joystick (dual-thumb friendly)
  - Outer ring: dead zone visualization
  - Inner knob: draggable, returns to center on release
  - Vector display: X: -1.00 to 1.00, Y: -1.00 to 1.00 (real-time)
  - Haptic feedback on movement
- **Bottom Controls Row:**
  - [◀ Auto] [■ Stop] [▶ Manual] mode selector (segmented control)
  - Speed slider: 0% — 100% (PWM scaling)
  - Emergency Stop button (large, red, always visible)

**Telemetry Overlay (collapsible bottom sheet):**
- Ultrasonic distance (HC-SR04): "Front: 42 cm" + EMA filtered trend sparkline
- Edge sensors (3× TCRT5000): [⬜][⬜][⬜] → [🟩][🟩][🟥] visual
- Battery voltage: 11.2V / 12.6V + percentage
- Motor current draw (L298N): Left 1.2A, Right 1.1A
- RF link quality: RSSI, SNR, last packet age (ms)

**Fail-Safe Visual Warning:**
- Banner at top if RF timeout > 300ms: "⚠️ RF LINK LOST — ROVER STOPPED (fail-safe active)"
- Auto-dismisses when packets resume

---

### 4. **Access Control Screen**
**PIN management & door control (complements physical 4×4 keypad on MEGA)**

**Sections:**
- **Door Status Card:** Large animated lock (locked/unlocked), servo angle indicator
- **Remote Unlock:** 
  - PIN entry field (4-6 digits, masked, numeric keyboard)
  - "Unlock" button (disabled until valid PIN + # entered)
  - Biometric prompt option (fingerprint/Face ID) for app-level auth
- **Access Log:** Scrollable list (last 50 entries)
  - Timestamp | PIN Hash (partial) | Result (✅/❌) | Source (App/Keypad/Bluetooth)
- **PIN Management (Admin only):**
  - Add/remove PINs, set temporary codes, view audit trail
  - Sync button → pushes to MEGA via MQTT `aethernet/access/pins`

**Visual Feedback:**
- Success: Green flash on door card + local LED RGB → Green (2s)
- Failure: Red shake animation + local LED RGB → Red (3 blinks)

---

### 5. **Telemetry History / Analytics Screen**
**Historical data visualization for statistical analysis (t-Student, EMA validation)**

**Tabs:**
- **Real-time Charts:** Live updating line charts (WebSocket)
  - HC-SR04 distance (raw vs EMA filtered, α=0.2)
  - KY-037 sound level
  - RF latency (Wi-Fi vs nRF24L01 comparison)
  - Motor PWM commands
- **Historical Queries:** Date range picker + sensor selector → PostgreSQL query
  - Export CSV button (for Python/R analysis)
  - Pre-built views: "Last 24h", "Last Week", "Custom Range"
- **Statistical Validation:** 
  - EMA noise reduction % (target >85%)
  - t-Test results: RF vs Wi-Fi latency (H₀: μ₁ = μ₂)
  - Packet loss rate over time

---

### 6. **Settings Screen**
**Configuration & diagnostics**

**Sections:**
- **Network:** MQTT broker IP/port, WebSocket endpoint, auto-reconnect toggle
- **Bluetooth Fallback:** Paired HC-06 devices, auto-fallback threshold
- **Rover Tuning:** Joystick sensitivity, dead zone, max speed, fail-safe timeout (300-500ms)
- **Alarm:** Arming delay, notification channels (Telegram test button)
- **Display:** Theme (Light/Dark/System), units (Metric/Imperial), language
- **Diagnostics:** 
  - MQTT topic monitor (live log)
  - Firmware versions (ESP32, MEGA, UNO, ESP8266)
  - Ping test to all nodes
  - Export logs button
- **About:** Version, FOSS licenses, GitHub link

---

## Design System Requirements

### Color Palette (FOSS-friendly, accessible)
```
Primary:    #1A73E8  (Material Blue 600)
PrimaryVar: #155BB5  (Blue 700)
Secondary:  #00C853  (Green A400) — success/unlocked
Error:      #D32F2F  (Red 700) — intrusion/locked/fail-safe
Warning:    #F57C00  (Orange 700) — Bluetooth fallback
Surface:    #FFFFFF / #121212 (Light/Dark)
Background: #F5F5F5 / #000000
OnPrimary:  #FFFFFF
OnSurface:  #1D1D1D / #E6E6E6
```

### Semantic Colors for Hardware States
```
Laser Intact:      #00C853 (Green)
Laser Breached:    #D32F2F (Red) + pulsing animation
RF Connected:      #1A73E8 (Blue)
RF Degraded:       #F57C00 (Orange) — latency >50ms
RF Lost:           #D32F2F (Red) — fail-safe active
Bluetooth Active:  #6A1B9A (Purple 800)
Tuya Bulb Online:  #00C853
Tuya Bulb Offline: #9E9E9E (Grey 500)
```

### Typography
- **Headline:** Roboto Medium / 24sp
- **Body:** Roboto Regular / 16sp
- **Monospace (telemetry values):** Roboto Mono / 14sp
- **Button:** Roboto Medium / 14sp, all caps

### Spacing & Layout
- Base unit: 8dp
- Screen margins: 16dp
- Card elevation: 2dp (Light) / 8dp (Dark)
- Border radius: 12dp cards, 28dp joystick
- Touch targets: minimum 48×48dp

### Motion & Feedback
- Joystick: spring physics, 150ms return-to-center
- Screen transitions: 200ms shared element
- State changes (lock/unlock): 300ms morph + haptic
- Critical alerts (intrusion): persistent until dismissed, system notification

---

## Component Specifications

### Virtual Joystick (Custom Composable)
```
Props:
- onVectorChange: (x: Float, y: Float) -> Unit
- maxRadius: Dp = 120dp
- deadZone: Float = 0.15
- hapticEnabled: Boolean = true
- autoCenter: Boolean = true

Visual:
- Base ring: 240dp, stroke 4dp, color Primary/20%
- Active ring: follows knob, color Primary
- Knob: 80dp circle, elevation 8dp, shadow
- Vector label: "X: 0.42  Y: -0.71" below joystick
```

### Telemetry Card (Reusable)
```
Props:
- title: String
- value: String (monospace)
- unit: String?
- trend: List<Float>? (sparkline data)
- status: Enum { Normal, Warning, Critical, Unknown }
- onTap: () -> Unit?

Layout: Title top-left, value large center, unit small right, sparkline bottom
```

### Connection Status Chip (Top Bar)
```
States: Connected (green), Bluetooth Fallback (purple), Disconnected (red), Connecting (grey)
Shows: Icon + label + latency (ms) when connected
Tap: opens Network Diagnostics bottom sheet
```

---

## User Flows to Illustrate

1. **Cold Start → Dashboard:** Splash → auto-connect → Dashboard loaded with live data
2. **Intrusion Alert:** Laser breached → Dashboard alarm banner → Tuya bulb flashes red → Telegram notification → User taps → Access Control screen shows log
3. **Rover Manual Drive:** Dashboard → Rover tab → Joystick active → RF latency <10ms → Telemetry overlay updates real-time
4. **RF Fail-Safe:** Rover moving → RF packets stop → 300ms timeout → Rover stops → Red banner "FAIL-SAFE ACTIVE" → Joystick disabled until reconnect
5. **Bluetooth Fallback:** Wi-Fi lost → Auto-switch to HC-06 → Purple chip "Bluetooth Fallback" → Limited control (relays, unlock only)
6. **Remote Unlock:** Access screen → Enter PIN → Biometric confirm → MQTT unlock command → MEGA servo rotates → Local LED green → Success toast

---

## Accessibility Requirements
- TalkBack labels on all interactive elements
- Color-blind safe palette (tested with deuteranopia/protanopia)
- Large text support (up to 200%)
- High contrast mode compatible
- Haptic feedback as non-visual confirmation

---

## Deliverables for Google Stitch
Generate **high-fidelity mockups** for:
1. Splash / Onboarding (light & dark)
2. Main Dashboard — Armed state (light & dark)
3. Main Dashboard — Disarmed + Intrusion Alert active (light & dark)
4. Rover Control — Manual mode with telemetry overlay expanded (light & dark)
5. Rover Control — Fail-safe banner visible (light & dark)
6. Access Control — PIN entry with biometric prompt (light & dark)
7. Access Control — Access log list (light & dark)
8. Telemetry History — Real-time charts tab (light & dark)
9. Settings — Network & Rover Tuning sections (light & dark)
10. Component library sheet: Joystick states, Telemetry Card variants, Status Chips, Buttons, Cards

**Format:** Figma-compatible frames, organized in pages by screen, with component variants and design tokens exported.

---

## Technical Notes for Developers (Reference)
- MQTT Topics: `aethernet/{domain}/{node}/{metric}` (see `docs/architecture.md`)
- QoS 1 for commands, QoS 0 for high-frequency telemetry
- WebSocket fallback for HTTP-only networks
- Bluetooth SPP UUID: `00001101-0000-1000-8000-00805F9B34FB`
- EMA α = 0.2 implemented in firmware (UNO) + mirrored in app for display
- Fail-safe timeout: configurable 300-500ms (default 400ms)
- All timestamps ISO8601 UTC, convert to local for display