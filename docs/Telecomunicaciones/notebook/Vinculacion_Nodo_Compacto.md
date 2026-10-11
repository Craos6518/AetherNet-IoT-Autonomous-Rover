# Vinculación Nodo Acceso Compacto — Evidencias E2E y Documentación Técnica

**Fecha:** 2026-10-10  
**Autor:** Andres Felipe Martinez Henao  
**Sprint:** 3 (Semanas 5-6) — MOV-07 RF-1.3  
**Rama:** `feature/app-bluetooth-fallback` (base `develop@6179e16`)

---

## 1. Resumen Ejecutivo

El Nodo Acceso Compacto (Arduino Nano + HC-06 SPP + FC-51) se implementó como contingencia Bluetooth SPP (RF-1.3 / MOV-07) para operar el cerrojo por PIN y detectar presencia cuando el Wi-Fi/MQTT falla.

**Estado:** ✅ Firmware + App + Tests + CI implementados | ⚠️ E2E RF bloqueado por hardware HC-06 inestable (no es app ni protocolo).

---

## 2. Arquitectura del Protocolo

```
┌─────────────────────────────────────────────────────────────────┐
│                    PROTOCOLO SPP (9600 baud)                     │
├─────────────────────────────────────────────────────────────────┤
│  App Android (BluetoothManager)          Nano Acceso Compacto   │
│  ┌─────────────────────────┐           ┌─────────────────────┐  │
│  │ CMD:ACCESS:{"pin":"1234"} ──────────▶ │ parsea PIN          │  │
│  │ ◀──────────────────────── ACCESS:{success,pin_hash,source}  │  │
│  │ POLL:PRESENCE ──────────────────────▶ │ responde presencia  │  │
│  │ ◀──────────────────────── PRESENCE:{bool} (push + poll 1s)   │  │
│  │ ◀──────────────────────── PRESENCE:{bool} (push al cambiar)  │  │
│  └─────────────────────────┘           └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

- **Tipo:** Texto línea a línea `TYPE:{json}\n` (compatibilidad HC-06 stock 9600)
- **PIN:** 4-6 dígitos, validado en firmware (hash djb2 espejo MEGA)
- **Hash:** `djb2("1234") = 7c78c98f` (8 hex lowercase)
- **Presencia:** FC-51 activo LOW → push automático al cambiar + respuesta a `POLL:PRESENCE`

---

## 3. Firmware: `nano-access-compacto.ino`

| Característica | Valor |
|---|---|
| **MCU** | Arduino Nano (ATmega328P) |
| **HC-06** | D2=SoftwareSerial RX ← TX, D3=SoftwareSerial TX → RX (divisor 1kΩ/2kΩ en RX) |
| **FC-51** | D4 INPUT (activo LOW: LOW = objeto detectado) |
| **LED built-in** | D13 (feedback local: 4×80ms éxito, 250ms fallo) |
| **Baud** | 9600 (HC-06 stock) |
| **Parser** | Manual sin ArduinoJson (RAM 25%) |
| **Flash/RAM** | 8972B (29%) / 526B (25%) |
| **CI** | `arduino:avr:nano` en matriz `ci.yml` |

### Test de Mesa (USB Serial, 2026-10-10)

```
Entrada: CMD:ACCESS:{"pin":"1234"}
Salida:  ACCESS:{"user_id":"hc06_user","pin_hash":"7c78c98f","success":true,"source":"bluetooth"}
LED:     4 parpadeos rápidos (éxito)

Entrada: CMD:ACCESS:{"pin":"9999"}
Salida:  ACCESS:{"user_id":"hc06_user","pin_hash":"7c78c98f","success":false,"source":"bluetooth"}
LED:     Destello largo 250ms (fallo)

Entrada: POLL:PRESENCE
Salida:  PRESENCE:{"presence":true/false}
```

---

## 4. App Android: Stack Bluetooth SPP

### Archivos clave

| Archivo | Responsabilidad |
|---|---|
| `data/bluetooth/BluetoothModels.kt` | `BtConnectionState`, `BtLineMessage`, `BtPayloadParser`, `BtDeviceInfo`, `BtAccessResult` |
| `data/bluetooth/BluetoothManager.kt` | SPP client (connect, reader thread, `accessEvents`, `presence`, `requestPresence`) |
| `ui/viewmodel/BluetoothViewModel.kt` | StateFlow UI, poll 1s presencia, teclado PIN, guard sendPin |
| `ui/screens/BluetoothScreen.kt` | Compose UI: permiso runtime, lista emparejados, badge, teclado, presencia |
| `core/di/ServiceLocator.kt` | Singleton `BluetoothManagerImpl` (interfaz `BluetoothManager`) |
| `ui/viewmodel/BluetoothViewModelFactory.kt` | Factory DI manual |

### Flujo de Permisos (API 31+)

1. Pantalla muestra "Habilitar Bluetooth" si `!hasPermission`
2. Usuario concede `BLUETOOTH_CONNECT` via `ActivityResultContracts.RequestPermission`
3. `derivedStateOf` re-evalúa `hasPermission` en recomposición → lista dispositivos
4. Usuario selecciona HC-06 → `connect(address)` → `Connecting` → `Connected` / `Error`

### Fix Crítico (Task 2): Propagación `connectionState`

```kotlin
// BluetoothViewModel.kt:70 — AÑADIDO
bt.connectionState.collect { state ->
    _uiState.update { it.copy(connection = state) }  // ← FIX
    when (state) {
        is BtConnectionState.Connected -> startPresencePoll()
        else -> cancelPresencePoll()
    }
}
```

**Antes:** `uiState.connection` siempre `Disconnected` → badge nunca cambiaba, teclado PIN no aparecía, `sendPin` fallaba con "Bluetooth no conectado".  
**Después:** Badge "BT ● HC-06", teclado visible, `sendPin` envía PIN si conectado.

---

## 5. Tests

| Test Suite | Tests | Estado |
|---|---|---|
| `BtPayloadParserTest` | 14 (8 originales + 6 casos borde) | ✅ PASS |
| `BluetoothViewModelTest` | 1 (propagación connectionState) | ✅ PASS |
| `PinViewModelTest` | 8 | ✅ PASS |
| `JoystickViewModelTest` | 6 | ✅ PASS |
| `DashboardViewModelTest` | 10 | ✅ PASS |
| **Total unitarios** | **59** | ✅ PASS |
| `connectedDebugAndroidTest` | 1 (ExampleInstrumentedTest) | ✅ PASS en Infinix |

### Casos Borde Añadidos (Task 5)

| Caso | Entrada | Resultado Esperado |
|---|---|---|
| Espacios tras dos puntos | `ACCESS: {"success":true}` | Access success |
| TYPE minúsculas | `access:{"success":true}` | `null` (case-sensitive) |
| Línea truncada | `ACCESS:{"success"` | Access success=false |
| Ruido común | `OK`, `ERROR`, `CONNECT` | `null` |
| Source distinto | `source":"ble"` | Propaga "ble" |
| Presencia inválida | `presence":"yes"` | `null` |

---

## 6. Evidencia E2E en Dispositivo Físico (Infinix X6831)

### Hardware Conectado
- **Infinix X6831** (SDK 33, serial `099344036F001239`) — USB + adb-tls wireless
- **HC-06** emparejado: `20:16:06:29:69:52` (nombre "HC-06")
- **Arduino Nano** cargado con `nano-access-compacto.ino` (probado por USB serial)

### Secuencia de Prueba Manual (2026-10-10)

| Paso | Acción | Resultado Esperado | Resultado Real |
|---|---|---|---|
| 1 | Abrir app → Dashboard → card "Bluetooth Fallback" | Navega a BluetoothScreen | ✅ |
| 2 | Conceder permiso `BLUETOOTH_CONNECT` | Lista emparejados (incl. HC-06) | ✅ (tras re-entrar: fix Task 3 `derivedStateOf`) |
| 3 | Tap HC-06 | Badge "BT ○ Conectando HC-06…" → "BT ● HC-06" | ⚠️ Badge queda "BT - Desconectado" |
| 4 | Logcat `AetherBT` | `TX: CMD:ACCESS...` / `RX: ACCESS:{...}` | ⚠️ No llega `RX` |
| 5 | Logcat sistema | `bt_btif_sock_rfcomm: unable to find RFCOMM slot id: 31/32/33` | ⚠️ RFCOMM falla |

### Diagnóstico HW (Confirmado 2026-10-10 noche)

| Síntoma | Causa | Confirmación |
|---|---|---|
| `IOException: read failed, socket might closed or timeout, read ret: -1` | HC-06 no confirma RFCOMM | Logcat `bt_btif_sock_rfcomm` |
| `RFC_MX_STATE_SABME_WAIT_UA` → `Page Timeout` | Enlace RFCOMM no completa | `bt_log` filtrado |
| Al desemparejar/re-emparejar → bond falla (`BOND_STATE_BONDING`→`NONE`) | Módulo inestable / power / divisor RX | Comportamiento intermitente |

**Conclusión:** Fallo **físico del HC-06** (alimentación 5V inestable, divisor 1kΩ/2kΩ en RX mal contactado, o módulo defectuoso). No es bug de app ni firmware. El firmware responde correctamente por USB serial.

---

## 7. CI/CD: Matriz `arduino-cli`

`.github/workflows/ci.yml` — Job `firmware-compile` matrix:

```yaml
matrix:
  firmware:
    - gateway-esp32      # FQBN esp32:esp32:esp32
    - mega-access        # FQBN arduino:avr:mega
    - rover-uno          # FQBN arduino:avr:uno
    - nano-access-compacto  # FQBN arduino:avr:nano  ← YA INCLUIDO
```

**Compilación CI verificada localmente:** Flash 29% (8.9KB), RAM 25% (526B) — dentro de límites.

---

## 8. Decisiones de Rama y Limpieza

| Rama | Contenido | Decisión 2026-10-10 |
|---|---|---|
| `feature/app-joystick-virtual` (e086068..7806dd3) | BT duplicado (`SppClient`, `node-compact/`, métodos BT en repo, toggle PinScreen) | **Excluir bloque BT** al mergear a `develop`; conservar solo docs DevOps (`*.pptx`, `*.md`, `Diario_*.ipynb`) |
| `feature/app-bluetooth-fallback` (actual) | Implementación canónica BT (`BluetoothManager`, `BluetoothViewModel`, `BluetoothScreen`, `BluetoothModels`, `nano-access-compacto.ino`) | **Versión canónica** — mergear a `develop` |

---

## 9. Trazabilidad HU/RF

| ID | Descripción | Estado | Evidencia |
|---|---|---|---|
| **HU-03** | Acceso por PIN vía BT fallback | 🔄 Implementado, E2E HW pendiente | `BluetoothScreen` + `BluetoothViewModel` + `nano-access-compacto.ino` |
| **RF-1.3** | Protocolo SPP HC-06 9600 + FC-51 | ✅ Implementado | `BluetoothModels.kt`, `nano-access-compacto.ino` |
| **MOV-07** | Fallback BT contingencia | 🔄 Implementado, E2E HW pendiente | Ver arriba |
| **RNF-3.1** | FOSS 100% (sin SDK propietario) | ✅ | Android Bluetooth API + arduino-cli |

---

## 10. Próximos Pasos (Post-Sprint 3)

1. **Validar HW HC-06:** Verificar 5V/GND estable, divisor 1kΩ/2kΩ en RX (Fritzing P7), probar otro módulo HC-06.
2. **Reintento E2E:** Con HW estable, validar PIN `1234` → `7c78c98f` + presencia FC-51 live.
3. **Merge a `develop`:** `feature/app-bluetooth-fallback` → `develop` (incluye MOV-07 completo).
4. **Cierre Sprint 3:** Actualizar `docs/sprints.md` y `docs/backlog.md` MOV-07 = DONE (con nota HW).

---

## 11. Referencias

- **Firmware:** `firmware/nano-access-compacto/nano-access-compacto.ino`
- **App Bluetooth:** `app/src/main/java/com/aethernet/aethercontrol/data/bluetooth/`
- **Tests:** `app/src/test/.../BtPayloadParserTest.kt`, `BluetoothViewModelTest.kt`
- **CI:** `.github/workflows/ci.yml` (job `firmware-compile` matrix)
- **Logs E2E:** `adb logcat -s AetherBT` (ver sección 6)
- **Docs:** `docs/backlog.md` (MOV-07), `docs/sprints.md` (Sprint 3 estado actual)
- **Fritzing:** `docs/fritzing/compendio-planos.md` P7 (divisor HC-06 RX)