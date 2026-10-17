# Nodo Compacto Vinculación — Deep Review Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolver la vinculación completa del Nodo Acceso Compacto (Arduino Nano + HC-06 + FC-51) con la App Android vía Bluetooth SPP, cubriendo firmware, protocolo, stack BT app, bugs de UI, y pruebas E2E.

**Architecture:** Enfoque en capas: (1) Hardware/firmware verificación y estabilidad, (2) Protocolo JSON TYPE:PAYLOAD validado, (3) Android BluetoothManager/SPP robusto con StateFlow tipado, (4) ViewModel/UI sincronizado, (5) Tests automatizados + validación en dispositivo físico.

**Tech Stack:** Arduino Nano (ATmega328P), HC-06 SPP 9600, FC-51, Android Kotlin + Coroutines + Flow, BluetoothAdapter/BluetoothSocket (RFCOMM), arduino-cli CI, JUnit + BtPayloadParserTest.

**Spec:** `docs/requirements.md` (HU-03, RF-1.3, MOV-07), `docs/hardware-inventory.md:12` (Nodo Acceso Compacto), `docs/backlog.md:24` (MOV-07 RF-1.3 fallback), `docs/sprints.md` (Sprint 3).

## Global Constraints
- FOSS estricto: sin SDK propietarios (RNF-3.1).
- HC-06 usa divisor 1k/2k en RX y 5V/GND estables (ADR-001, commit 8152c2b).
- PIN 4..6 dígitos, hash djb2 espejo MEGA (`keypad_control.cpp:130`).
- Protocolo: `CMD:ACCESS:{"pin":"1234"}\n` → `ACCESS:{...}` + `PRESENCE:{...}` push + `POLL:PRESENCE` sondeo 1 Hz.
- `BtPayloadParser` puro, testeable (8 tests verdes).
- CI: matriz arduino-cli compila `nano-access-compacto` (FQBN `arduino:avr:nano`).
- Cambios de protocolo entre subsistemas requieren confirmación humana (AGENTS.md §4).

---

### Task 1: Auditoría y estabilización del firmware Nodo Compacto

**Files:**
- Read: `firmware/nano-access-compacto/nano-access-compacto.ino`
- Read: `firmware/node-compact/node-compact.ino` (rama `feature/app-joystick-virtual:e086068` — duplicado histórico)
- Modify: `firmware/nano-access-compacto/nano-access-compacto.ino`
- Test: `firmware/nano-access-compacto/` (compilación arduino-cli)

**Interfaces:**
- Consumes: hardware-inventory.md pines (HC-06 TX→D2, RX→D3, FC-51→D4), PIN 1234, hash djb2.
- Produces: sketches estables que responden ACCESS/PRESENCE/POLL a 9600 baud.

- [ ] **Step 1.1: Comparar los dos sketches y consolidar canónico**

```bash
# En worktree e086068
diff -u firmware/node-compact/node-compact.ino firmware/nano-access-compacto/nano-access-compacto.ino
```
Diferencias clave a reconciliar:
- Parser: ArduinoJson (v6) vs manual sin dependencias.
- FC-51 pin: D5 vs D4.
- JSON response: `"node":"compact_access"` vs sin campo.
- `test-bluetooth-uno/` sketch de prueba — evaluar si conservar.

Decisión: mantener `nano-access-compacto.ino` (parser manual, djb2, sin ArduinoJson, FC-51 D4) como canónico; archivar `node-compact/` en `docs/archivo/`.

- [ ] **Step 1.2: Verificar pines y timings en el canónico**

```cpp
// nano-access-compacto.ino — confirmar:
const int BT_RX_PIN = 2;  // HC-06 TX
const int BT_TX_PIN = 3;  // HC-06 RX (con divisor 1k/2k)
const int FC_PIN   = 4;   // FC-51 INPUT_PULLUP
// Baud 9600 fijo (HC-06 stock)
// parseCommand(): extractPin manual → djb2 → ACCESS:{success,pin_hash,source}
// loop(): POLL:PRESENCE → PRESENCE:{presence,bool}
// presencia: envío automático solo en cambio + debounce 2s
```

- [ ] **Step 1.3: Compilar y validar tamaño**

```bash
cd firmware/nano-access-compacto
arduino-cli compile --fqbn arduino:avr:nano .
```
Expected: Flash ≤ 70% (≈21 KB), RAM ≤ 50% (≈1 KB). Sin warnings.

- [ ] **Step 1.4: Test de mesa con Arduino Nano físico (si disponible)**

1. Cargar sketch.
2. Conectar HC-06 (D2/D3 + divisor RX) + FC-51 (D4).
3. Serial Monitor 9600: enviar `CMD:ACCESS:{"pin":"1234"}\n` → esperar `ACCESS:{"success":true,"pin_hash":"7c78c98f","source":"bluetooth"}`.
4. Enviar `POLL:PRESENCE\n` → `PRESENCE:{"presence":true/false}`.
5. Tapar FC-51 → push `PRESENCE:{"presence":true}` automático.

- [ ] **Step 1.5: Commit**

```bash
git add firmware/nano-access-compacto/nano-access-compacto.ino
git commit -m "firmware(nano-access-compacto): consolida sketch canónico, parser manual djb2, FC-51 D4"
```

---

### Task 2: Fix crítico — BluetoothViewModel propaga connectionState a UI

**Files:**
- Modify: `app/src/main/java/com/aethernet/aethercontrol/ui/viewmodel/BluetoothViewModel.kt:70`
- Test: `app/src/test/java/com/aethernet/aethercontrol/ui/viewmodel/BluetoothViewModelTest.kt`

**Interfaces:**
- Consumes: `BluetoothManager.connectionState: StateFlow<BtConnectionState>`
- Produces: `BluetoothUiState.connection` refleja estado real (Disconnected/Connecting/Connected/Error).

- [ ] **Step 2.1: Escribir test que falle sin el fix**

```kotlin
// BluetoothViewModelTest.kt
@Test
fun `connectionState se propaga a uiState`() = runTest {
    val bt = FakeBluetoothManager()
    val vm = BluetoothViewModel(bt)
    val states = vm.uiState.map { it.connection }.toList()
    bt.simulateConnecting("HC-06")
    assertEquals(BtConnectionState.Connecting("HC-06"), states.last())
    bt.simulateConnected("HC-06", "20:16:06:29:69:52")
    assertEquals(BtConnectionState.Connected("HC-06"), vm.uiState.value.connection)
    bt.simulateError("read failed")
    assertTrue(vm.uiState.value.connection is BtConnectionState.Error)
}
```

- [ ] **Step 2.2: Ejecutar test → FAIL**

```bash
./gradlew testDebugUnitTest --tests "com.aethernet.aethercontrol.ui.viewmodel.BluetoothViewModelTest.connectionState se propaga a uiState"
```

- [ ] **Step 2.3: Implementar fix (1 línea en el collect)**

```kotlin
// BluetoothViewModel.kt:70-80
bt.connectionState.collect { state ->
    _uiState.update { it.copy(connection = state) } // ← ADD
    when (state) {
        is BtConnectionState.Connected -> { pollJob?.cancel(); pollJob = viewModelScope.launch { while (true) { bt.requestPresence(); delay(1000) } } }
        else -> pollJob?.cancel()
    }
}
```

- [ ] **Step 2.4: Ejecutar test → PASS**

```bash
./gradlew testDebugUnitTest --tests "com.aethernet.aethercontrol.ui.viewmodel.BluetoothViewModelTest.connectionState se propaga a uiState"
```

- [ ] **Step 2.5: Verificar que no rompe tests existentes**

```bash
./gradlew testDebugUnitTest --tests "com.aethernet.aethercontrol.ui.viewmodel.BluetoothViewModelTest"
```

- [ ] **Step 2.6: Commit**

```bash
git add app/src/main/java/com/aethernet/aethercontrol/ui/viewmodel/BluetoothViewModel.kt
git add app/src/test/java/com/aethernet/aethercontrol/ui/viewmodel/BluetoothViewModelTest.kt
git commit -m "fix(viewmodel): propaga bt.connectionState a uiState — habilita badge, teclado PIN, guard sendPin"
```

---

### Task 3: Fix UX — Re-check de permiso Bluetooth al resume de pantalla

**Files:**
- Modify: `app/src/main/java/com/aethernet/aethercontrol/ui/screens/BluetoothScreen.kt:59-62`
- Test: `app/src/androidTest/java/.../BluetoothScreenTest.kt` (nuevo)

**Interfaces:**
- Consumes: `ContextCompat.checkSelfPermission` en recomposición.
- Produces: pantalla muestra "Actualizar dispositivos" inmediatamente tras conceder permiso sin navegar fuera.

- [ ] **Step 3.1: Escribir test instrumentado que falle**

```kotlin
// app/src/androidTest/.../BluetoothScreenTest.kt
@Test
fun permisoConcedido_refrescaListaSinNavegar() {
    val scenario = launchActivity<MainActivity>()
    scenario.onActivity { navigateToBluetooth() }
    // concede permiso via UIAutomator o grantPermissionRule
    // verifica que desaparece "Habilitar Bluetooth" y aparece "Actualizar dispositivos"
}
```

- [ ] **Step 3.2: Fix — mover check a `derivedStateOf` o `LaunchedEffect`**

```kotlin
// BluetoothScreen.kt
val hasPermission by remember {
    derivedStateOf {
        !needsRuntimePerm ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }
}
// o LaunchedEffect(needsRuntimePerm) { vm.loadDevices() if granted }
```

- [ ] **Step 3.3: Ejecutar test → PASS**

- [ ] **Step 3.4: Commit**

```bash
git add app/src/main/java/com/aethernet/aethercontrol/ui/screens/BluetoothScreen.kt
git commit -m "ux(bluetooth): re-check permiso en resume — elimina necesidad de re-entrar tras conceder"
```

---

### Task 4: Validación del stack BluetoothManager — manejo de errores y timeouts

**Files:**
- Read/Modify: `app/src/main/java/com/aethernet/aethercontrol/data/bluetooth/BluetoothManager.kt`
- Test: `app/src/test/.../BluetoothManagerTest.kt` (nuevo)

**Interfaces:**
- Consumes: `BluetoothSocket`, `BluetoothAdapter`, `BluetoothDevice`.
- Produces: `Result<Unit>` en connect/send/disconnect, `StateFlow<BtConnectionState>`, `SharedFlow<BtAccessResult>` + `SharedFlow<Boolean>`.

- [ ] **Step 4.1: Revisar connect() — orden de error/disconnect (commit 8152c2b fix)**

```kotlin
// BluetoothManager.kt:102-122 — ya tiene:
catch (e: Exception) {
    disconnect()          // 1) limpia socket
    _connectionState.value = BtConnectionState.Error(...) // 2) error DESPUÉS
    Result.Error(...)
}
```
Verificar: no hay race donde `Error` se pise por `Disconnected` del `finally` del reader thread.

- [ ] **Step 4.2: Añadir timeout de conexión (actual: bloquea indefinido en `socket.connect()`)**

```kotlin
// BluetoothManager.kt connect()
val connectJob = scope.launch { socket.connect() }
val timeoutJob = scope.launch { delay(10_000); if (socket?.isConnected != true) socket?.close() }
joinAll(connectJob, timeoutJob)
```

- [ ] **Step 4.3: Harden reader thread — evitar NPE y leaks**

```kotlin
// startReader(): ya usa `if (socket === s)` en finally — OK.
// Añadir: catch específico IOException → _connectionState.postValue(Disconnected)
```

- [ ] **Step 4.4: Tests unitarios BluetoothManager (mock BluetoothSocket)**

- [ ] **Step 4.5: Commit**

---

### Task 5: Protocolo — Validar BtPayloadParser y casos borde

**Files:**
- Read: `app/src/main/java/com/aethernet/aethercontrol/data/bluetooth/BluetoothModels.kt:49-103`
- Test: `app/src/test/.../BtPayloadParserTest.kt` (existente, 8 tests)

**Interfaces:**
- Consumes: líneas crudas `TYPE:{json}\n` del HC-06.
- Produces: `BtLineMessage.Access(BtAccessResult)` | `BtLineMessage.Presence(Boolean)` | `null`.

- [ ] **Step 5.1: Casos de prueba extra (añadir a BtPayloadParserTest)**

```kotlin
// ACCESS con espacios: "ACCESS: { \"success\": true }"
assertNotNull(parseLine("ACCESS: {\"success\":true}"))
// PRESENCE minúsculas: "presence:{\"presence\":true}" → null (case sensitive TYPE)
// Línea truncada: "ACCESS:{"success" → null
// Ruido: "OK\n" → null
// JSON con source distinto: "source":"ble" → BtAccessResult(source="ble")
```

- [ ] **Step 5.2: Ejecutar tests extendidos → PASS**

```bash
./gradlew testDebugUnitTest --tests "com.aethernet.aethercontrol.data.bluetooth.BtPayloadParserTest"
```

- [ ] **Step 5.3: Verificar firmware emite exactamente formato esperado**

```cpp
// nano-access-compacto.ino — confirmar serial.print:
btSerial.print(F("ACCESS:")); btSerial.println(payload); // payload = JSON compacto sin espacios
btSerial.print(F("PRESENCE:")); btSerial.println(payload);
```

- [ ] **Step 5.4: Commit** (si hay cambios en parser o tests)

---

### Task 6: Integración E2E en dispositivo físico (Infinix + HC-06 real)

**Files:**
- No cambios de código — validación manual automatizada.
- Test: `app/src/androidTest/.../BluetoothE2ETest.kt` (nuevo, opcional).

**Interfaces:**
- Consumes: App instalada, HC-06 emparejado (`20:16:06:29:69:52`), Nano cargado con sketch canónico.
- Produces: E2E verde = conectar → PIN 1234 → ACCESS success → presencia FC-51 live.

- [ ] **Step 6.1: Preparar hardware**
1. Arduino Nano + HC-06 (D2/D3, divisor 1k/2k en RX) + FC-51 (D4, PULLUP).
2. Alimentar Nano (5V/GND estable — ¡evitar USB solo si HC-06 pide >100mA!).
3. Verificar HC-06 LED parpadea (no vinculado) → emparejar desde Android Settings → LED fijo.

- [ ] **Step 6.2: Script de validación manual (checklist)**

```bash
# 1. App: Dashboard → card "Bluetooth Fallback" → lista muestra HC-06
# 2. Tap HC-06 → badge "BT ○ Conectando HC-06…" → "BT ● HC-06" (≤10s timeout)
# 3. Si Error: logcat | grep AetherBT → diagnosticar RFCOMM slot / HW
# 4. Conectado: teclado PIN aparece (4-6 dígitos)
# 5. PIN 1234 → "Enviar" → logcat: "TX: CMD:ACCESS:{"pin":"1234"}"
# 6. Nano responde: "RX: ACCESS:{"success":true,"pin_hash":"7c78c98f","source":"bluetooth"}"; UI: "✓ Desbloqueado"
# 7. FC-51: tapar sensor → UI "Presencia: SI (objeto detectado)" en ≤1s (poll + push)
# 8. Destapar → "Presencia: NO"
# 9. "Desconectar" → badge "BT - Desconectado", teclado oculto
```

- [ ] **Step 6.3: Capturar logcat AetherBT completo para evidencias**

```bash
adb -s 099344036F001239 logcat -s AetherBT > logs/e2e-bluetooth-$(date +%s).txt
```

- [ ] **Step 6.4: Documentar resultados en `docs/backlog.md` (MOV-07) y `docs/sprints.md` (Sprint 3)**

---

### Task 7: Limpieza rama histórica `feature/app-joystick-virtual` — evitar duplicados

**Files:**
- Read: `firmware/node-compact/` (rama `feature/app-joystick-virtual:e086068`)
- Modify: `docs/backlog.md`, `docs/sprints.md` (actualizar estado MOV-07)

**Interfaces:**
- Consumes: hallazgo de duplicado SppClient vs BluetoothManager (Task 2 review).
- Produces: backlog limpio, una sola implementación canónica.

- [ ] **Step 7.1: Confirmar que `feature/app-joystick-virtual` no aporta nada único a develop**

```bash
# diff develop..feature/app-joystick-virtual → solo: SppClient (obsoleto), node-compact (duplicado), docs DevOps (conservar)
```

- [ ] **Step 7.2: Si merge a develop → excluir bloque BT (SppClient, repo methods, Pin toggle, node-compact, test-bluetooth-uno); conservar solo docs DevOps pptx/md**

- [ ] **Step 7.3: Actualizar backlog/sprints: MOV-07 = DONE en app-bluetooth-fallback, bloque HW documentado**

- [ ] **Step 7.4: Commit**

---

### Task 8: CI — Añadir `nano-access-compacto` a matriz arduino-cli

**Files:**
- Modify: `.github/workflows/ci.yml` (buscar matriz `arduino-cli`)

**Interfaces:**
- Consumes: FQBN `arduino:avr:nano`, sketch path `firmware/nano-access-compacto`.
- Produces: build verde en PR que toque firmware nano.

- [ ] **Step 8.1: Verificar matriz actual**

```yaml
# .github/workflows/ci.yml — buscar strategy.matrix.fqbn
```

- [ ] **Step 8.2: Añadir entrada**

```yaml
nano-access-compacto:
  fqbn: arduino:avr:nano
  path: firmware/nano-access-compacto
```

- [ ] **Step 8.3: Probar localmente**

```bash
arduino-cli compile --fqbn arduino:avr:nano firmware/nano-access-compacto
```

- [ ] **Step 8.4: Commit**

---

### Task 9: Documentación final y cierre

**Files:**
- Modify: `docs/backlog.md` (MOV-07), `docs/sprints.md` (Sprint 3), `docs/requirements.md` (RF-1.3), `docs/hardware-inventory.md` (Nodo Acceso Compacto).
- Create: `docs/Telecomunicaciones/notebook/Vinculacion_Nodo_Compacto.md` (evidencias E2E, logcat, fotos).

**Interfaces:**
- Consumes: resultados Tasks 1–8.
- Produces: trazabilidad completa HU-03/RF-1.3 → código → tests → HW.

- [ ] **Step 9.1: Actualizar backlog.md — MOV-07 a DONE con notas HW**

- [ ] **Step 9.2: Actualizar sprints.md — Sprint 3 cierre**

- [ ] **Step 9.3: Notebook evidencias — fotos setup, logcat E2E, tabla pines**

- [ ] **Step 9.4: Commit final**

```bash
git add docs/backlog.md docs/sprints.md docs/requirements.md docs/hardware-inventory.md docs/Telecomunicaciones/notebook/Vinculacion_Nodo_Compacto.md
git commit -m "docs: cierre MOV-07 RF-1.3 — Nodo Compacto vinculado (E2E validado, HW documentado)"
```

---

## Checklist de Verificación Final (Self-Review)

| Requisito Spec | Task | Verificado |
|---|---|---|
| HU-03: Acceso por PIN vía BT fallback | 1,2,6 | ☐ |
| RF-1.3: Protocolo SPP HC-06 9600 | 1,4,5 | ☐ |
| PIN 4-6 dígitos + hash djb2 | 1,5 | ☐ |
| FC-51 presencia push + poll 1s | 1,2,6 | ☐ |
| BtPayloadParser 8 tests + casos borde | 5 | ☐ |
| BluetoothManager connect timeout + error order | 4 | ☐ |
| ViewModel propaga connectionState | 2 | ☐ |
| UI refresca permiso sin navegar | 3 | ☐ |
| CI compila nano-access-compacto | 8 | ☐ |
| E2E en Infinix + HC-06 real | 6 | ☐ |
| Rama histórica limpiada | 7 | ☐ |
| Trazabilidad docs completa | 9 | ☐ |

---

**Plan guardado en** `docs/superpowers/plans/2026-10-10-nodo-compacto-vinculacion.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration. REQUIRED SUB-SKILL: superpowers:subagent-driven-development.

2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints. REQUIRED SUB-SKILL: superpowers:executing-plans.

**Which approach?**