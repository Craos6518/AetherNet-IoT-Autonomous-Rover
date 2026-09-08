# Backlog por Materia — Programación Móvil (App AetherControl)

Backlog operativo detallado del Área 1. IDs MOV-01..10 provienen de `docs/backlog.md`; nuevos continúan la serie (MOV-11+). Cada tarea define qué hacer exactamente, alcance IN/OUT y criterios de aceptación.

> Estado real: `DashboardViewModel.kt` es esqueleto con TODOs; Room definido sin usar; Gradle existe; cero pantallas funcionales.

---

## Sprint 2 — Base conectada

### MOV-01 — Setup proyecto Android ✅ PARCIAL (cerrar)
| M · Sprint 1-2 · Origen RF-1.1 |
**Hecho:** estructura Gradle KTS, paquete `com.aethernet.aethercontrol`, Application class, Theme, MainActivity.
**Falta exactamente:** verificar que `app/build.gradle.kts` declara dependencias necesarias ANTES de escribir código: `androidx.lifecycle:lifecycle-viewmodel-compose`, `org.eclipse.paho:org.eclipse.paho.client.mqttv3` (+ `paho.android.service` si se usa el servicio), `kotlinx-serialization-json`, Room runtime/kapt ya presente. Compilar en CI habilitando job android (ver DEVOPS-10).
**Criterios:**
- [ ] `./gradlew assembleDebug` verde local y en CI
- [ ] Dependencias MQTT/serialización presentes y resueltas

### MOV-11 — Cliente MQTT reusable (MqttClientManager)
| M · Sprint 2 · Depende de MOV-01 + DEVOPS-02 (broker con ACL) · Origen RF-1.1 |
**Qué hacer exactamente:**
1. Crear `data/mqtt/MqttClientManager.kt`: singleton inyectado desde `AetherControlApplication`; configura conexión (host/broker desde `BuildConfig` o settings screen mínima), credenciales usuario `appbackend`.
2. Exponer API reactiva: `val connectionState: StateFlow<ConnectionState>` (mapea a `DashboardUiState.Connecting/Disconnected/Error` existentes), `fun subscribe(topic: String): Flow<Pair<String,String>>`, `fun publish(topic, payload, qos=1)`.
3. Reconexión automática con backoff y callback `connectComplete`.

**Alcance IN:** gestión de una sola conexión compartida por toda la app.
**Alcance OUT:** UI de configuración avanzada, TLS.
**Criterios:**
- [ ] App conecta al broker local y muestra "Conectado" en el estado
- [ ] Mensaje publicado desde `mosquitto_pub -t aethernet/app/test` llega al Flow suscrito
- [ ] Apagar broker 10 s → app pasa a Disconnected y se reconecta sola

### MOV-02 — Pantallas de control: bombillo Tuya + LED local
| M · Sprint 2 · Depende de MOV-11, LOW-02/MEGA básico · Origen RF-1.1 |
**Qué hacer exactamente:**
1. `ui/screens/LightControlScreen.kt`: control bombillo Tuya vía `aethernet/tuya/command` (color/brillo) + indicador solo-lectura LED RGB local (`aethernet/access/event` → verde/rojo).
2. ViewModel `LightControlViewModel`: expone `TuyaState` y `LedLocalState` desde suscripciones MQTT retained.
3. Optimistic update + rollback si el gateway confirma error (o timeout 2 s).

**Alcance OUT:** escenas/agrupados, programación horaria.
**Criterios:**
- [ ] Control Tuya refleja estado real del bombillo (validado vía `tuya-local`)
- [ ] LED local refleja verde (HU-01) / rojo (HU-02) desde MEGA

### MOV-03 — Suscripción telemetría en dashboard ✅ ESQUELETO → completar
| M · Sprint 2 · Depende de MOV-11 · Origen RF-1.1 |
**Qué hacer exactamente:** reemplazar los stubs de `DashboardViewModel` (hoy `Disconnected("MQTT no configurado")` fijo): conectar `init{}` al MqttClientManager; suscribir `aethernet/rover/telemetry` y mapear JSON→`RoverTelemetry` (data class YA existe); exponer `lastAccessEvent` desde `aethernet/access/event`. Implementar los métodos vacíos `sendJoystickCommand/sendPinCommand` delegando al manager.
**Criterios:**
- [ ] Ningún TODO restante en DashboardViewModel
- [ ] Telemetría simulada con mosquitto_pub se ve en el estado de UI (verificado con test MOV-12)

## Sprint 3 — Joystick y contingencia

### MOV-05 — Joystick virtual en Compose
| M · Sprint 3 · Origen RF-1.2 |
**Qué hacer exactamente:**
1. `ui/components/JoystickPad.kt`: composable con Canvas circular base + knob; `pointerInput(Unit){ detectDragGestures }` calcula offset normalizado X,Y ∈ [-1,1] clampeado al radio; al soltar, animar knob al centro y emitir (0,0).
2. Callback `onVector:(Float,Float)->Unit` hacia el ViewModel.

**Alcance IN:** control táctil single-pointer.
**Alcance OUT:** vibración háptica, multi-touch, gamepad externo.
**Criterios:**
- [ ] Vector correcto en las 8 direcciones cardinales ±diagonales (prueba manual documentada)
- [ ] Al soltar siempre emite centro exacto (0,0)

### MOV-06 — Envío de comandos con baja latencia
| M · Sprint 3 · Depende de MOV-05 + RF operativo (DEVOPS-05) · Origen RF-1.2 |
**Qué hacer exactamente:** en `RoverControlViewModel`, throttling del stream del joystick a 20 Hz (50 ms) usando `sample()` de coroutines-flows antes de publicar a `aethernet/rover/command` con QoS 0 (latencia > fiabilidad aquí); payload idéntico al struct `RoverCommand` del firmware vía JSON acordado; mapear vector→PWM: `pwm = round(v * 255)` con deadband ±0.15 (bajo eso = 0, evita jitter).
**Alcance IN:** modo manual únicamente.
**Alcance OUT:** modo autónomo (decisión vive en el Rover).
**Criterios:**
- [ ] ≤20 msg/s medidos en broker (suscriptor contador)
- [ ] KPI PRD: percepción inmediata (<50 ms comando→motor, validar junto DevOps con timestamps)

### MOV-07 — Fallback Bluetooth SPP
| S · Sprint 3 · Depende de DEVOPS-13 (tramas UART) · Origen RF-1.3 |
**Qué hacer exactamente:**
1. Permisos runtime: en Manifest declarar `BLUETOOTH_CONNECT` (API 31+), `BLUETOOTH` legacy (≤30), `ACCESS_FINE_LOCATION`; solicitar con `rememberLauncherForActivityResult` antes de escanear.
2. `data/bluetooth/SppClient.kt`: bond con HC-06 (UUID SPP estándar `00001101-...`), socket bloqueo en `Dispatchers.IO`, loop lectura→Flow<String>, escritura de tramas del protocolo DEVOPS-13.
3. `AccessScreen` con toggle "Wi-Fi / Bluetooth": misma lógica de negocio, transporte distinto (Repository pattern).

**Alcance IN:** desbloqueo por PIN vía BT cuando Wi-Fi cae.
**Alcance OUT:** control del Rover por BT (solo nodo crítico de acceso, según RF-1.3).
**Criterios:**
- [ ] Con Wi-Fi apagado, PIN enviado por BT abre cerrojo (HU-01 completa por canal alterno)
- [ ] Denegación de permisos muestra explicación y no crashea

## Sprint 4 — Consolidación

### MOV-08 — Dashboard consolidado
| S · Sprint 4 · Depende de MOV-02/03 · Origen RF-1.1 |
**Qué hacer exactamente:** navegación inferior (Compose Navigation) con 3 pestañas: Luces | Rover | Acceso; cabecera persistente con estado de conexión (usa `connectionStatus` ya expuesto); pestaña Acceso lista últimos eventos desde Room (offline-first: insertar cada evento recibido en BD vía repository).
**Criterios:**
- [ ] Rotación de pantalla no pierde estado (ViewModel sobrevive)
- [ ] Eventos vistos siguen disponibles sin conexión (Room)

### MOV-09 — Reconexión y errores de red UX
| C · Sprint 4 · Depende de MOV-11 |
**Qué hacer exactamente:** banner global según ConnectionState (amarillo reconectando/rojo caído); snackbars para fallos de publicación con reintento; timeout de comandos 2 s → estado Error(message) del UiState existente.

### MOV-10 — Tests unitarios de ViewModels
| S · Sprint 4 · Depende de MOV-03/06 · Origen calidad |
**Qué hacer exactamente:** `app/src/test/`: fake `MqttClientManager` (interface extraída para permitir mock); tests con `kotlinx-coroutines-test`: (1) telemetría entrante actualiza UiState.Connected, (2) throttling del joystick emite máx 20/s, (3) desconexión → Disconnected(reason), (4) comando luces optimista + rollback en timeout.
**Criterios:**
- [ ] `./gradlew test` verde en CI (job android-build activo)
- [ ] ≥4 casos correspondientes a la lista

### MOV-12 — Habilitar build Android en CI *(nuevo)*
| S · Sprint 2 · Sin dependencias técnicas |
**Qué hacer exactamente:** coordinar con DevOps (DEVOPS-10): quitar `if: false` del job, caché Gradle, subir APK debug como artifact del run.
**Criterios:**
- [ ] Pipeline verde incluye job Android
