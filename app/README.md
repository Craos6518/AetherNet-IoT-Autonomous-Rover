# AetherControl — App Android Kotlin (Jetpack Compose, MVVM)

> **Autor:** Andres Felipe Martinez Henao
> **Experiencia:** 2 años electrónica y Arduino | 1 año Programación C | 2 años Python | 2 años HTML/CSS/JavaScript/React | 1 año PostgreSQL  
> **Stack:** Kotlin, Jetpack Compose (Material3), MVVM, Coroutines + StateFlow/SharedFlow, Retrofit + OkHttp + kotlinx.serialization, DataStore Preferences, Paho MQTTv3, Navigation Compose — 100% FOSS (RNF-3.1)  
> **Sprint:** 1 ✅ (MOV-01 Dashboard), 2 ✅ (MOV-02 LED + MOV-03 MQTT), 4 ✅ (MOV-04 Pin cerrojo) — `docs/sprints.md:78`  
> **Package:** `com.aethernet.aethercontrol` · `minSdk 24` (Android 7.0) · `targetSdk 37` (Android 15) · `version 1.0 (1)`

---

## 0. Cómo leer esta app si vienes de web / electrónica / Python

Si vienes de **React (2 años)**: esta `app/` es un `create-react-app` pero en Kotlin.  
`Compose` es `React` (`@Composable` = `function Component()`), `StateFlow` es `useState` + `Observable`, `ViewModel` es `useReducer` con ciclo de vida, `Navigation Compose` es `React Router`, `Material3` es `MUI`, `Retrofit` es `axios` tipado, `Paho MQTT` es `mqtt.js`, `DataStore` es `localStorage` pero con `Flow`. Lo que en React es `fetch('/api/access-events').then(setState)` aquí es `repo.getAccessEvents()` → `Result.Success` → `_uiState.update { copy(...) }`.

Si vienes de **electrónica / C (1 año C, 2 años Arduino)**: la app es el “control remoto” del `MEGA` y del `Rover`. El `MEGA` tiene `keypad_control.cpp:51 isDigit` + `PIN_MAX_LEN=6` + `hashPin djb2` y un `LED RGB 44/45/46` con `DOOR_AUTO_LOCK_MS=5000`; la app replica esa lógica en `PinValidator.kt:15` y `LedStateMapper.kt:33` pero sin tocar hardware — envía `{"pin":"1234"}` por `aethernet/access/command` (MQTT) y el `Gateway ESP32` lo convierte en `CMD:ACCESS` → `MEGA processPinAttempt`. Ver el flujo te ahorra depurar wiring.

Si vienes de **Python / PostgreSQL (2 años Python, 1 año PG)**: los `DTOs` son `Pydantic` del backend (`backend/app/schemas.py`) pero en Kotlin con `@Serializable`. `AccessEventCreate` es el `INSERT INTO access_events(user_id, pin_hash, success)` tipado; `HealthResponse` es el `SELECT 1` del `GET /health`. `stats/` y la app leen la misma `PostgreSQL` — la app para pintar Dashboard, `stats/` para `t-Student`.

Este README cubre **todo** lo que hay en `app/` carpeta por carpeta con `ruta:línea` para ir directo en Android Studio.

---

## 1. Mapa de carpetas

```
app/
├── build.gradle.kts                 # 80 líneas — plugins, SDK 37, deps (Compose/Retrofit/OkHttp/Paho/DataStore)
├── src/main/
│   ├── AndroidManifest.xml          # 33 líneas — INTERNET, AetherControlApp, MainActivity, cleartext HTTP
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml          # app_name = AetherControl
│   │   │   ├── colors.xml           # Purple/Pink (Material baseline)
│   │   │   └── themes.xml           # Theme.AetherControl → AetherControlTheme
│   │   ├── xml/
│   │   │   ├── network_security_config.xml  # cleartext 10.0.2.2 / 192.168.1.x (Android 9+)
│   │   │   ├── backup_rules.xml / data_extraction_rules.xml
│   │   └── mipmap-*/ic_launcher*    # iconos (foreground/background, round)
│   └── java/com/aethernet/aethercontrol/
│       ├── AetherControlApp.kt      # 18 líneas — Application.onCreate → ServiceLocator.init()
│       ├── MainActivity.kt          # 29 líneas — ComponentActivity, edge-to-edge, Theme + 2 ViewModels + NavGraph
│       ├── core/di/
│       │   └── ServiceLocator.kt    # 177 líneas — DI manual singleton (Retrofit/ApiService/MqttManager/Repository)
│       ├── util/
│       │   └── Result.kt            # 30 líneas — sealed Success/Error/Loading + safeCall
│       ├── data/
│       │   ├── remote/
│       │   │   ├── ApiService.kt    # 72 líneas — contrato Retrofit espejo backend FastAPI
│       │   │   └── dto/
│       │   │       ├── HealthDto.kt          # Root + Health (GET / y /health)
│       │   │       ├── AccessEventDto.kt     # HU-01 — AccessEventCreate/Out
│       │   │       ├── SensorEventDto.kt     # HU-03 — SensorEventCreate/Out + metadata alias
│       │   │       ├── SecurityEventDto.kt   # HU-02 — SecurityEventCreate/Out
│       │   │       └── RoverTelemetryDto.kt  # RF-3.1 — RoverTelemetryCreate/Out
│       │   ├── local/
│       │   │   ├── AppDatabase.kt            # 16 líneas — esqueleto Room MOV-08
│       │   │   └── PreferencesManager.kt     # 77 líneas — DataStore api_base_url (10.0.2.2 default)
│       │   ├── mqtt/
│       │   │   ├── MqttManager.kt   # 190 líneas — Paho 1883/9001, SharedFlow, publishAccessCommand
│       │   │   └── MqttModels.kt    # 46 líneas — RoverTelemetryMqtt/AccessEventMqtt/SecurityEventMqtt + State
│       │   └── repository/
│       │       ├── AetherRepository.kt      # 55 líneas — interface contrato
│       │       └── AetherRepositoryImpl.kt  # 92 líneas — safeCall + getLedState + MQTT delegación
│       ├── domain/
│       │   ├── validator/
│       │   │   └── PinValidator.kt  # 39 líneas — 4..6 dígitos, djb2 HEX espejo firmware
│       │   ├── mapper/
│       │   │   └── LedStateMapper.kt # 184 líneas — GREEN 5s / RED 1s / RED intrusión 10s / OFF
│       │   └── model/
│       │       ├── LedState.kt      # 46 líneas — LedColor/LedState/LedUiState
│       │       ├── PinUiState.kt    # 19 líneas — pinInput/isValid/error/throttle
│       │       └── UiModels.kt      # 21 líneas — DashboardUiState (health + LED + MQTT + Rover)
│       └── ui/
│           ├── viewmodel/
│           │   ├── DashboardViewModel.kt # 255 líneas — health + LED polling 5s + MQTT push + expiry
│           │   ├── PinViewModel.kt       # 154 líneas — onPinDigit/clear/backspace + sendPin + accessEventFlow
│           │   └── ViewModelFactory.kt   # 26 líneas — Factory manual para 2 ViewModels
│           ├── navigation/
│           │   └── NavGraph.kt      # 38 líneas — sealed Dest Dashboard/Pin + NavHost
│           ├── screens/
│           │   ├── DashboardScreen.kt # 207 líneas — Banner, MQTT ●, Rover Card, LedStatusCard, Health, URL editor
│           │   └── PinScreen.kt     # 160 líneas — Dots 6x + Grid 4x3 + MQTT badge
│           ├── components/
│           │   └── LedStatusCard.kt # 143 líneas — círculo 64dp + relative "hace Xs" + Previews
│           └── theme/
│               ├── Color.kt         # 11 líneas — Purple80/40 etc. (baseline)
│               ├── Theme.kt         # 58 líneas — AetherControlTheme (dynamicColor Android 12+)
│               └── Type.kt          # 34 líneas — Typography bodyLarge 16sp
└── src/test|androidTest/java/       # 7 tests unit + 1 instrumented (MOV-01..MOV-04)
    └── PinValidatorTest / LedStateMapperTest / DashboardViewModelTest / PinViewModelTest
```

Root aporta `settings.gradle.kts` + `gradle/libs.versions.toml` (version catalog) y CI `.github/workflows/ci.yml:250 android-build`.

---

## 2. Arquitectura — MVVM sin Hilt (RNF-3.1 FOSS)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  UI (Compose)                                                                │
│  DashboardScreen.kt + PinScreen.kt + LedStatusCard.kt + NavGraph.kt          │
│  collectAsStateWithLifecycle()  ←  StateFlow<DashboardUiState | PinUiState>   │
├──────────────────────────────────────────────────────────────────────────────┤
│  ViewModel                                                                   │
│  DashboardViewModel.kt (health, LED poll 5s, MQTT push, expiry)              │
│  PinViewModel.kt (pinInput, validator, throttle 5/60s, accessEventFlow)      │
│  ViewModelFactory.kt (manual, no Hilt)                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│  Domain                                                                      │
│  PinValidator.kt (4..6 dígitos, djb2 HEX) + LedStateMapper.kt (5s/1s/10s)    │
│  LedState.kt / PinUiState.kt / UiModels.kt                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│  Data                                                                        │
│  AetherRepository (interface) → AetherRepositoryImpl (safeCall, getLedState) │
│  ApiService.kt (Retrofit) → DTOs (@Serializable, espejo backend FastAPI)     │
│  MqttManager.kt (Paho 1883/9001, SharedFlow 32) → MqttModels.kt               │
│  PreferencesManager.kt (DataStore api_base_url) + AppDatabase.kt (Room MOV-08)│
├──────────────────────────────────────────────────────────────────────────────┤
│  Core / DI                                                                   │
│  ServiceLocator.kt (object, lazy, synchronized, runBlocking init)            │
│  AetherControlApp.kt (init) + MainActivity.kt (Theme + 2 VMs + NavGraph)     │
│  Result.kt (Success/Error/Loading, safeCall IOException/HttpException)       │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Por qué MVVM y por qué sin Hilt/Koin:** `docs/requirements.md:16 RF-1.1` pide Dashboard en tiempo real con `MVVM`; `docs/prd.md:53 RNF-3.1` pide 100% FOSS sin depender de SDK propietario. Elegimos `ViewModel` + `StateFlow` (estándar) y `ServiceLocator` manual (object + `by lazy` + `synchronized`) porque es evaluable, sin magia de Hilt, y refleja lo que harías en `stats/` con `sqlalchemy` sin ORM mágico. El costo es escribir `DashboardViewModelFactory` a mano, la ganancia es control total y trazabilidad `MainActivity:20 → ServiceLocator:116 → AetherRepositoryImpl`.

---

## 3. `core/di/ServiceLocator.kt:1` + `AetherControlApp.kt:1` + `MainActivity.kt:1`

### `AetherControlApp.kt:13` — 18 líneas
`class AetherControlApp : Application()` con `onCreate() { ServiceLocator.init(this) }`. Punto de entrada Android (como `index.js` en React). `init` carga `DataStore` `apiBaseUrl` con `runBlocking { first() }` para que el primer `Retrofit` use `192.168.1.x:8000` guardado, no solo `10.0.2.2` del emulador.

### `ServiceLocator.kt:31` — 177 líneas, object singleton
`by lazy` para `json`, `okHttpClient` (con `HttpLoggingInterceptor BODY` solo en `DEBUG`), y `synchronized` para `retrofit/apiService/repository/mqttManager`. `DEFAULT_BASE_URL = PreferencesManager.DEFAULT_BASE_URL` (`10.0.2.2:8000/`). `getBrokerHostFromHttpUrl(httpUrl)` convierte `http://192.168.1.14:8000/` → `tcp://192.168.1.14:1883` (Mosquitto `1883`, fallback `ws://9001` en `MqttManager:75`). `updateBaseUrl(url)` normaliza (`http://` + `/`), persiste en `DataStore` y recrea `Retrofit`/`ApiService`/`Repository` atómicamente; `updateBaseUrlBlocking` envuelve con `runBlocking` para UI sin scope.

### `MainActivity.kt:14` — 29 líneas
`ComponentActivity` con `enableEdgeToEdge()` (como `viewport-fit=cover` en web) y `setContent { AetherControlTheme { val repo = ServiceLocator.repository; val factory = DashboardViewModelFactory(repo); val vm: DashboardViewModel = viewModel(factory); val pinVm: PinViewModel = viewModel(factory); NavGraph(vm, pinVm) } }`. Crea 2 VMs con el mismo repo/factory (reuso).

### `util/Result.kt:10` — 30 líneas
`sealed interface Result<T> { Success(data), Error(msg, cause), Loading }` + `suspend fun safeCall(block)` que mapea `IOException` (red) y `HttpException` (4xx/5xx) a `Error`. Cada método de `AetherRepositoryImpl` lo usa: `safeCall { api.getHealth() }`. En `ViewModel` es `when (repo.getHealth()) { is Success -> ..., is Error -> ..., is Loading -> ... }` (como `switch` de `Promise` `ok/error` en JS).

---

## 4. `data/remote` — ApiService + DTOs (espejo backend)

### `ApiService.kt:22` — 72 líneas
`interface ApiService` con Retrofit:
```kotlin
@GET("/") suspend fun getRoot(): RootResponse
@GET("health") suspend fun getHealth(): HealthResponse
@POST("api/access-events") suspend fun createAccessEvent(@Body payload: AccessEventCreate): AccessEventOut
@GET("api/access-events") suspend fun getAccessEvents(@Query("limit") limit: Int = 50, @Query("offset") Int = 0): List<AccessEventOut>
@POST("api/sensor-events") ...
@GET("api/sensor-events") suspend fun getSensorEvents(@Query("sensor_type") sensorType: String? = null, ...): List<SensorEventOut>
@POST("api/security-events") ...
@GET("api/security-events") ...
@POST("api/rover/telemetry") ...
@GET("api/rover/telemetry") suspend fun getRoverTelemetry(@Query("session_id") sessionId: String? = null, ...): List<RoverTelemetryOut>
```
Espejo 1:1 de `backend/app/main.py:68` y `routers/events.py:31,58,92,123`. Cada `DTO` es `@Serializable`.

### DTOs — espejo `backend/app/schemas.py` + `models.py` + `init.sql`

| DTO `data/remote/dto` | Backend `schemas.py` | Tabla `init.sql` / `models.py` | Uso |
|---|---|---|---|
| `AccessEventCreate/Out:9` | `:26`/`:33` | `access_events` `user_id VARCHAR(64)`, `pin_hash 128`, `success BOOLEAN`, `source` | `HU-01` — `MEGA keypad 1234#` → `Gateway aethernet/access/event` → `POST` → `App GET` |
| `SensorEventCreate/Out:12` | `:47`/`:56` | `sensor_events` `value Numeric(10,4)`, `filtered_value`, `unit`, `event_metadata JSONB` | `RNF-2.1 HU-03` — `HC-SR04 42.5 cm` + `EMA 41.2` (`stats/ema_filter.py:32`) |
| `SecurityEventCreate/Out:9` | `:73`/`:79` | `security_events` `event_type`, `severity medium`, `acknowledged` | `HU-02` — `KY-008 intrusion` → `Telegram` |
| `RoverTelemetryCreate/Out:9` | `:95`/`:106` | `rover_telemetry` `session_id UUID`, `left/right_motor_pwm SmallInteger -255..255`, `ultrasonic_distance_cm Numeric(6,2)`, `ir_* BOOLEAN` | `RF-3.1` — `nRF24L01` → `Gateway` → `MQTT` |
| `HealthDto:9` `RootResponse:16` | `:17` `main.py:68` | — | `GET /health` `SELECT 1` / `GET /` |

Notas: `id: String` (no `UUID` type) para evitar serializer custom Sprint 1; `timestamp: String` ISO-8601 con TZ (`server_default=func.now()`), parse en `LedStateMapper:167` con `Instant.parse`. `SensorEventDto:30` lleva `@SerialName("metadata")` + fallback `eventMetadata` por si backend envía `event_metadata`.

---

## 5. `data/local` — DataStore + Room esqueleto

### `PreferencesManager.kt:20` — 77 líneas
`class PreferencesManager(private val dataStore: DataStore<Preferences>)` con `by preferencesDataStore(name="aethernet_prefs")` (extension `Context.dataStore`, como `localForage` en JS). `Keys.API_BASE_URL` / `USER_ID` (`stringPreferencesKey`), `DEFAULT_BASE_URL = "http://10.0.2.2:8000/"` (emulador mapea a `localhost` del host; físico usa `192.168.1.x:8000` ver `DashboardScreen:201` tip). `apiBaseUrl: Flow<String>` emite con `data.map { prefs[key] ?: DEFAULT }` y se colecta con `collectAsStateWithLifecycle`. `saveBaseUrl(url)` normaliza (`http://` + `/`, `require` no vacío) y `dataStore.edit { prefs[key]=normalized }`. `saveBaseUrl` se llama desde `ServiceLocator.updateBaseUrl` (persiste) y `DashboardScreen` “Guardar URL”.

### `AppDatabase.kt:12` — 16 líneas esqueleto
`class AppDatabase private constructor()` con `TODO MOV-08` comentado `@Database(entities = [AccessEventEntity::class], version = 1) abstract class : RoomDatabase()`. Sin `@Database` aún para no romper `ksp`. Futura `MOV-08` cache offline (como `init.sql` local del teléfono, ver `prd.md:61` Contingencia Edge — MEGA sigue sin red, la app también debería cachear).

---

## 6. `data/mqtt` — Paho Mosquitto push <50 ms (prd.md:50)

### `MqttModels.kt:13` — 46 líneas
```kotlin
@Serializable data class RoverTelemetryMqtt(val left_pwm: Int = 0, val right_pwm: Int = 0, val ultrasonic_cm: Int? = null, val ir_left: Boolean? = null, ...)
@Serializable data class AccessEventMqtt(val user_id: String, val pin_hash: String, val success: Boolean, val source: String = "keypad")
@Serializable data class SecurityEventMqtt(val event_type: String, val severity: String? = null, ...)
sealed interface MqttConnectionState { Disconnected, Connecting, Connected(broker: String), Error(msg: String) }
```
Espejo `firmware/gateway-esp32/gateway-esp32.ino:69/71/72/73` topics y `MqttManager` payloads.

### `MqttManager.kt:34` — 190 líneas
Envuelve `org.eclipse.paho.client.mqttv3.MqttClient` (EPL 1.0 FOSS, como `mqtt.js`). `MemoryPersistence` (sin disco). `MutableStateFlow<MqttConnectionState>` (`Disconnected` → `Connecting` → `Connected(brokerUri)`) y 3 `MutableSharedFlow(extraBufferCapacity=32)` (`roverTelemetry`, `accessEvents`, `securityEvents`) con `tryEmit` (no pierde si UI rota). `topics = ["aethernet/rover/telemetry", "aethernet/access/event", "aethernet/seguridad/intrusion", "aethernet/system/status"]` (ver `gateway.ino:69` + `acl.conf:7`). `jsonLenient = Json { ignoreUnknownKeys=true; isLenient=true; coerceInputValues=true }`.

`getBrokerHostFromHttpUrl(httpUrl)` / `getBrokerWsFromHttpUrl(httpUrl)` convierten `http://192.168.1.14:8000/` → `tcp://192.168.1.14:1883` / `ws://...:9001` (fallback `mosquitto.conf:8` si firewall bloquea 1883). `suspend fun connect(httpBaseUrl) = withContext(Dispatchers.IO)` verifica `isConnected`, crea `MqttClient(brokerUri, "aethernet-app-${UUID...take(8)}", MemoryPersistence())`, `setCallback(MqttCallbackExtended { connectComplete { topics.forEach subscribe(qos=0) } ; messageArrived { parse } ; connectionLost { Error } })`, `MqttConnectOptions { isAutomaticReconnect=true; isCleanSession=true; connectionTimeout=5; keepAliveInterval=15 }`, `client.connect(opts)` (bloquea, por eso `Dispatchers.IO`), y fuerza `Connected` si Paho no invocó callback. `parse(topic, payload)` hace `when { startsWith rover/telemetry → decode → tryEmit }`.

`publishAccessCommand(pin): Result<Unit>` valida `4..6 dígitos` (`PinValidator` espejo), construye `"""{"pin":"$pin"}"""`, `MqttMessage(qos=0, retained=false)`, `c.publish("aethernet/access/command", msg)` (esperado por `gateway-esp32.ino:70/332 handleAccessCommand` → `CMD:ACCESS` → `MEGA processPinAttempt`). QoS 0 LAN `<50ms` `prd.md:50`.

`disconnect()` hace `disconnect() + close()` + `Disconnected`.

---

## 7. `data/repository` — Contrato + Impl (MOV-01 4.1/4.2)

### `AetherRepository.kt:26` — 55 líneas, interface contrato
`getHealth(): Result<HealthResponse>`, `get/postAccessEvents`, `get/postSensorEvents`, `get/postSecurityEvents`, `get/postRoverTelemetry`, `getLedState(): Result<LedUiState>` (deriva, no existe `/api/led`), `mqttConnectionState: StateFlow`, `roverTelemetryFlow/accessEventFlow/securityEventFlow: SharedFlow`, `connectMqtt(httpBaseUrl)`, `disconnectMqtt()`, `sendAccessCommand(pin): Result<Unit>` (MQTT `aethernet/access/command`, no HTTP).

### `AetherRepositoryImpl.kt:28` — 92 líneas, `safeCall { api.* }` para HTTP, delegación MQTT
Cada HTTP envuelto en `safeCall { api.getHealth() }` (como `try fetch`). `getLedState()` hace `coroutineScope { async { api.getAccessEvents(1) }; async { api.getSecurityEvents(1) }; await ambos; LedStateMapper.map(...) }` — fetch paralelo como `Promise.all`. MQTT getters delegan a `mqtt?.connectionState ?: MutableStateFlow(Disconnected)` (fallback flows vacíos para tests sin `Context`). `sendAccessCommand(pin)` valida `mqtt != null` y `Connected`, retorna `Error("MQTT no conectado …")` si no, si no `m.publishAccessCommand(pin)`.

---

## 8. `domain` — Validator + Mapper + Models

### `PinValidator.kt:15` — 39 líneas, `object` con `isValidFormat 4..6`, `hashPin djb2 HEX`, `isCorrectForDemo`
`MIN_LEN 4 MAX_LEN 6`, `isValidFormat(pin) = pin.length in 4..6 && pin.all { isDigit() }` (como `isDigit(key)` en `keypad_control.cpp:51` + `PIN_MAX_LEN=6`). `hashPin(pin): String` implementa `djb2 5381 → hash*33 + c.code → & 0xFFFFFFFFL → toString(16)` idéntico a `keypad_control.cpp:99` (Arduino `unsigned long` 32-bit, aquí `Long` + máscara). Usado en `PinViewModel:96` y `MqttManager:170` (validación), y en tests para no loguear PIN claro.

### `LedState.kt:15` — 46 líneas
```kotlin
enum class LedColor { GREEN (0xFF4CAF50), RED (0xFFF44336), OFF (0xFF9E9E9E), UNKNOWN (0xFFBDBDBD) }
enum class LedState { OFF, GREEN_UNLOCKED, RED_INTRUSION, RED_FAIL, UNKNOWN }
data class LedUiState(val color: LedColor = UNKNOWN, val state: LedState = UNKNOWN, val label: String = "Desconocido", val lastEventAt: Long? = null, val source: String? = null, val isLoading: Boolean = false, val error: String? = null)
```
Espejo `firmware/mega-access/src/led.h:15 LedMode` y `config.h:30 LED_COMMON_ANODE` (app solo refleja, no escribe `255-valor`), pero con colores `Material3` para `LedStatusCard`.

### `PinUiState.kt:11` — 19 líneas
`data class PinUiState(val pinInput: String = "", val isLoading: Boolean = false, val isValid: Boolean = false, val error: String? = null, val lastResultSuccess: Boolean? = null, val lastMessage: String? = null, val attemptsInWindow: Int = 0)` — estado formulario PIN como `useState` en React, con throttling `5/60s`.

### `UiModels.kt:12` — 21 líneas
`data class DashboardUiState(val isLoading: Boolean = false, val isConnected: Boolean = false, val health: HealthResponse? = null, val error: String? = null, val lastSync: Long? = null, val ledState: LedUiState = LedUiState(), val mqttState: MqttConnectionState = Disconnected, val lastRover: RoverTelemetryMqtt? = null)` — junta todo lo que `DashboardScreen` necesita (como `Redux` state).

### `LedStateMapper.kt:33` — 184 líneas, `object` determinista, ventanas `5000/10000/1000`
Fuente `backend/app/models.py:20 AccessEvent.success + :55 SecurityEvent.event_type + :61 timestamp` y `routers/events.py:31/92`. Solo lectura, no existe `/api/led`. Ventanas: `GREEN_WINDOW_MS 5000` (`config.h:52 DOOR_AUTO_LOCK_MS` HU-01), `RED_INTRUSION_WINDOW_MS 10000` (HU-02 más visible), `RED_FAIL_WINDOW_MS 1000` (`config.h:62`). `fun map(accessEvents, securityEvents, nowMs = currentTimeMillis()): LedUiState` toma `maxByOrNull { parseTs }` (último evento), compara `securityIsLatest` vs `accessIsLatest` (tie prioriza `security` intrusión), decide: `intrusion && age<10000 → RED_INTRUSION`, `access success && age<5000 → GREEN_UNLOCKED`, `!success && age<1000 → RED_FAIL`, resto `OFF`, vacío `UNKNOWN`. Fallback verifica intrusión vigente aunque no sea el último. `parseTs(iso)` tolera `Instant.parse` con `Z`/`+00:00` o sin zona (asume `UTC`, agrega `Z`), reemplaza `" "` por `"T"` (Postgres sin `T`).

---

## 9. `ui/viewmodel` — Dashboard (poll + MQTT) + Pin (form) + Factory

### `ViewModelFactory.kt:12` — 26 líneas
`class DashboardViewModelFactory(private val repo: AetherRepository) : ViewModelProvider.Factory` con `create(modelClass)` que retorna `DashboardViewModel(repo)` o `PinViewModel(repo)` (`isAssignableFrom`). Usado en `MainActivity:21 viewModel(factory = vmFactory)`.

### `DashboardViewModel.kt:28` — 255 líneas
`class DashboardViewModel(repo, autoPollLed=true, autoConnectMqtt=true) : ViewModel()` con `_uiState: MutableStateFlow<DashboardUiState>` y 6 `Job` (`ledPollingJob`, `mqttJob`, `mqttStateJob`, `mqttAccessJob`, `mqttSecurityJob`, `ledExpiryJob`). `init { refreshHealth(); refreshLedState(); if(autoPollLed) startLedPolling(); if(autoConnectMqtt) connectMqttAndCollect() }`.

`refreshHealth()` y `refreshLedState()` hacen `viewModelScope.launch { _uiState.update { copy(isLoading) } ; when (repo.getHealth()/getLedState()) { Success -> copy(isConnected, health, lastSync); Error -> copy(error); Loading -> ... } }`.

`startLedPolling(intervalMs=5000)` lanza `while(isActive) { delay(intervalMs); refreshLedState() }` (como `setInterval`); `stopLedPolling()` cancela. `isPolling` getter.

`connectMqttAndCollect(httpBaseUrl?)` obtiene `url` de `ServiceLocator.getCurrentBaseUrl()` (fallback `10.0.2.2`), `repo.connectMqtt(url)`, colecta `roverTelemetryFlow` → `lastRover`, `accessEventFlow` → pinta `GREEN 5000` o `RED_FAIL 1000` con `scheduleLedExpiry`, `securityEventFlow` → `RED_INTRUSION 10000`, `mqttConnectionState` → `mqttState` + ahorro batería: `Connected → stopLedPolling()`, `Disconnected/Error → startLedPolling()` (fallback HTTP).

`scheduleLedExpiry(delayMs)` cancela previo, `delay(delayMs)` y si `age >= delayMs-100` revierte a `OFF` (no pisa evento más reciente).

`disconnectMqtt()` cancela todos los `Job` y `Disconnected`; `onCleared()` limpia.

### `PinViewModel.kt:24` — 154 líneas
`class PinViewModel(repo) : ViewModel()` con `_pinState: MutableStateFlow<PinUiState>`, `mqttState` delegado, `accessJob`/`windowStartMs`/`cooldownJob`. `init` colecta `accessEventFlow` → `lastResultSuccess` + `lastMessage "✓/✕"` y `delay(3000)` auto-limpia.

`onPinDigit(d)` valida `isDigit` y `MAX_LEN 6`, `pinInput+ d` + `isValidFormat`; `onPinClear()` vacía; `onPinBackspace()` `dropLast(1)`.

`sendPin()` valida `isLoading`, `isValidFormat` (`"PIN 4-6 dígitos"`), `mqtt Connected` (`"MQTT no conectado — verifica Wi-Fi/broker"`), throttle `5/60s` (`windowStartMs`, `attemptsInWindow`, `"Demasiados intentos, espera 60s"`), luego `viewModelScope.launch { isLoading true; when(repo.sendAccessCommand(pin)) { Success -> pinInput="" isValid false attempts+1 lastMessage="Enviado, esperando confirmación…"; Error -> error=r.msg } }`. Confirmación real llega por `accessEventFlow`.

---

## 10. `ui/navigation` + `ui/screens` + `ui/components` + `ui/theme`

### `NavGraph.kt:15` — 38 líneas
`sealed class Dest(val route: String) { Dashboard("dashboard"), Pin("pin") }` y `@Composable fun NavGraph(viewModel, pinViewModel, navController=rememberNavController()) { NavHost(startDestination=Dashboard) { composable(Dashboard) { DashboardScreen(vm, onOpenPin={ navigate(Pin) }) }; composable(Pin) { PinScreen(vm, onBack={ popBackStack() }) } } }` (como `React Router`).

### `DashboardScreen.kt:45` — 207 líneas
`@Composable fun DashboardScreen(vm, onOpenPin)` con `val state by vm.uiState.collectAsStateWithLifecycle()`, `val savedUrl by ServiceLocator.preferencesManager.apiBaseUrl.collectAsStateWithLifecycle(ServiceLocator.getCurrentBaseUrl())`, `var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }`. `Scaffold { Column.fillMaxSize.padding(innerPadding).padding(16).verticalScroll { ... } }` con `Arrangement.spacedBy(12)`.

Secciones: banner `"Desconectado"` si `!isConnected`, `MQTT ●/○/✕` con color `0xFF4CAF50` si `Connected`, `Card` Rover `L/R US cm` + `IR` + `LinearProgressIndicator (pwm+255)/510f`, `Button "Abrir PIN cerrojo"` → `onOpenPin`, `LedStatusCard(state=ledState, onRetry=refreshLedState)`, `health` + `DB | v`, `CircularProgressIndicator`, `error` rojo, `Row` `[Reintentar][Refrescar LED][Pausar/Reanudar poll]`, `lastSync`, editor `Backend URL` `Actual: $savedUrl` `OutlinedTextField` + `[Guardar URL]` `ServiceLocator.updateBaseUrl(urlInput) + refreshHealth` y `[Restaurar]` `resetBaseUrl`, tip `emulador=10.0.2.2:8000, físico=IP del PC`.

### `PinScreen.kt:38` — 160 líneas
`@Composable fun PinScreen(vm, onBack)` con `val s by vm.pinState.collectAsStateWithLifecycle()` y `val mqtt by vm.mqttState`. `Scaffold + Column center + SpacedBy 12` con `Text "PIN cerrojo"`, `Row` `repeat(6) { Box CircleShape background Black/LightGray }` (dots), `Text "${len}/6"`, `error` rojo + `lastMessage` verde/rojo, `rows = 1/2/3 / 4/5/6 / 7/8/9 / */0/#` con `Row weight(1f)` `Button` `onClick { "*": onPinClear, "#": sendPin, else onPinDigit }` (Grid 4×3), `Row [←][Limpiar]`, `CircularProgressIndicator`, `MQTT badge`, `OutlinedButton "Volver Dashboard"`, nota `keypad_control.cpp:51` + `aethernet/access/command:70`.

### `LedStatusCard.kt:42` — 143 líneas
`@Composable fun LedStatusCard(state, onRetry)` con `circleColor = GREEN 0xFF4CAF50 / RED 0xFFF44336 / OFF 0xFF9E9E9E / UNKNOWN 0xFFBDBDBD` (espejo `led.cpp:22` `0,255,0` etc. pero en Compose). `var nowTick by mutableLongStateOf(currentTimeMillis()); LaunchedEffect(lastEventAt) { while(true) { delay(1000); nowTick = currentTimeMillis() } }` + `relative = "hace Xs/m/s/h"` (tick cada segundo). `Card(RoundedCornerShape 16, elevation 2) { Column 16 padding { Text "LED Local"; Row { Box 64dp CircleShape background circleColor (con CircularProgressIndicator 24dp si isLoading) + Column { label + Origen: source + relative + Sin eventos si UNKNOWN } }; error rojo; Row { Button "Reintentar LED" } } }` + `@Preview` 5 estados `Green/RedIntrusion/RedFail/Off/Unknown`.

### `Theme` — `Color.kt:5` (Purple80/40 etc. baseline), `Theme.kt:36` `AetherControlTheme(darkTheme, dynamicColor)` con `dynamicDarkColorScheme(dynamicLightColorScheme)` en `Android 12+` + fallback `DarkColorScheme/LightColorScheme`, `Type.kt:10` `Typography bodyLarge 16sp`.

---

## 11. `build.gradle.kts:1` (80 líneas) + `AndroidManifest.xml:1` + `res/xml/network_security_config.xml:1`

**`build.gradle.kts`** `plugins { android.application, kotlin.compose, kotlin.serialization }`, `android { namespace com.aethernet.aethercontrol, compileSdk 37, defaultConfig { applicationId, minSdk 24, targetSdk 37, versionCode 1, versionName 1.0, testInstrumentationRunner }, buildTypes { release optimization enable false; debug isMinifyEnabled false (BuildConfig.DEBUG para HttpLoggingInterceptor) }, compileOptions Java 11, buildFeatures { compose true, buildConfig true } }`, `dependencies` BOM Compose + `activity-compose` + `material3` + `core-ktx` + `lifecycle runtime/compose + viewmodel.compose` + `navigation.compose` + `retrofit core + kotlinx.serialization` + `okhttp core + logging` + `kotlinx.serialization.json` + `coroutines android/core` + `datastore preferences` + `paho mqttv3` — todos FOSS (RNF-3.1).

**`AndroidManifest.xml:1`** `INTERNET` (Retrofit + Paho 1883), `application android:name=".AetherControlApp"` + `allowBackup` + `usesCleartextTraffic true` + `networkSecurityConfig @xml/network_security_config`, `activity MainActivity exported true` con `MAIN/LAUNCHER`.

**`network_security_config.xml:1`** `domain-config cleartextTrafficPermitted true` para `10.0.2.2`, `10.0.0.2`, `localhost`, `127.0.0.1`, `192.168.1.0` + `base-config true` (fallback AVD; endurecer a `false` en prod).

---

## 12. Integración con Backend y Firmware (flujo HU-01 cerrado)

```
MEGA keypad 4×4 "1234#" (keypad_control.cpp:51 isDigit, processPinAttempt)
  → uart_protocol.cpp:60 ACCESS:{"pin_hash":djb2, "success":true} 38400 Serial2
  → Gateway ESP32 gateway-esp32.ino:286 handleMegaUart → forwardAccessToBackend:343 HTTP POST
    http://192.168.1.14:8000/api/access-events (201, ver backend/README.md §6)
    → FastAPI routers/events.py:31 → SQLAlchemy AccessEvent → PostgreSQL access_events
      → App Retrofit ApiService.getAccessEvents(limit=1) (ApiService.kt:34) polling 5s
        o push MQTT aethernet/access/event (MqttManager.kt:44) <50ms prd.md:50
        → AetherRepositoryImpl.getLedState() + LedStateMapper.map() → LedUiState GREEN 5s
          → DashboardViewModel LedStatusCard "Verde desbloqueado" + LedStatusCard círculo 0xFF4CAF50

App PinScreen "1234" → PinViewModel.sendPin() → PinValidator.isValidFormat 4..6 → MqttManager.publishAccessCommand {"pin":"1234"} QoS 0
  → Mosquitto 1883 aethernet/access/command (acl.conf:7) → Gateway subscribe :207 handleAccessCommand → publish CMD:ACCESS
  → MEGA Serial2 RX CMD:ACCESS → keypad_control.cpp processPinAttempt("1234") → unlockDoor() servo 90° → LED verde 5s
  → MEGA ACCESS: success true (hash djb2) → Gateway → MQTT aethernet/access/event → App accessEventFlow → PinViewModel lastResultSuccess true "✓ Desbloqueado"
```

Mismo `djb2` en `PinValidator.kt:27` y `keypad_control.cpp:99` (`5381 → shl5+hash + c.code & 0xFFFFFFFFL → HEX`) valida que hash en `PostgreSQL` `pin_hash` coincide aunque App y MEGA calculen por separado.

---

## 13. Testing — 34 tests (unit)

| Suite `src/test/java` | Qué valida |
|---|---|
| `PinValidatorTest 6` | `isValidFormat 4..6`, `hashPin djb2` `1234→a6d2...`, `isCorrectForDemo` |
| `LedStateMapperTest 9` | `GREEN 5s`, `RED_FAIL 1s`, `RED_INTRUSION 10s`, `OFF`, `UNKNOWN`, `parseTs` tolerante |
| `DashboardViewModelTest 9` | `refreshHealth Success/Error`, `refreshLedState`, `polling start/stop`, `MQTT Connected → stopPolling`, `expiry` |
| `PinViewModelTest 8` | `onPinDigit 4..6`, `backspace/clear`, `sendPin requiere 4..6`, `MQTT no conectado`, `success limpia`, `accessEventFlow → lastResultSuccess`, `throttle 5/60s` |
| `ExampleUnitTest / Instrumented` | template |

**Ejecución:**
```bash
./gradlew :app:testDebugUnitTest --tests "*.PinViewModelTest"  # 8 passed
./gradlew :app:testDebugUnitTest                               # 34 passed, BUILD SUCCESSFUL
```

`Result.kt` + `FakeRepo` con `MutableSharedFlow` + `UnconfinedTestDispatcher` permiten test sin `PostgreSQL` ni `Mosquitto` real (como `backend/tests/test_health.py` mock).

---

## 14. Instalación y Uso

```bash
# Requisitos: Android Studio Ladybug+ (AGP 8.7, Kotlin 2.0), JDK 17, SDK 37
# 1. Backend
docker compose up -d  # postgres 5432 + mosquitto 1883/9001 + fastapi 8000 (ver backend/README.md)
# 2. App — emulador
./gradlew :app:installDebug
# Emulador usa 10.0.2.2:8000 (DEFAULT_BASE_URL). Si no llega: Dashboard → Backend URL → 10.0.2.2:8000 → Guardar → Reintentar
# 3. App — físico en misma LAN 192.168.1.x
# En Dashboard → Backend URL → 192.168.1.50:8000 (IP del PC `ipconfig`/`ifconfig`) → Guardar
# 4. Verificar
# Dashboard debe mostrar health DB: ok, MQTT ● tcp://192.168.1.14:1883, LED Apagado/Desconocido (vacío)
# Probar MEGA: teclear 1234# físico → LedStatusCard verde 5s + "hace 0s" (MQTT push) o 5s (polling)
# Probar App: PinScreen 1-2-3-4 → Enviar → "Enviado, esperando…" → "✓ Desbloqueado" (MP)
# 5. Tests
./gradlew :app:testDebugUnitTest  # 34 passed
./gradlew :app:assembleDebug      # APK debug/app-debug.apk
```

**Debug:** `ServiceLocator.kt:68 HttpLoggingInterceptor.Level.BODY` solo en `DEBUG` loguea cada `Retrofit` `GET/POST` en Logcat (como `morgan` en Express). `usesCleartextTraffic` + `network_security_config.xml` permiten `http://` sin `https` en LAN (Android 9+ bloquea cleartext por defecto).

---

## 15. Roadmap App

| Área | Estado | Origen |
|---|---|---|
| `Dashboard RF-1.1` `health` + `LED` `GET /health` `/api/access|security-events` | ✅ `MOV-01/02` `DashboardViewModel:51` |
| `MQTT Paho` `1883/9001` `aethernet/#` `tcp://host:1883` `ws://9001` | ✅ `MOV-03` `MqttManager:34` `mosquitto.conf:4` |
| `Pin cerrojo` `aethernet/access/command {"pin":"1234"}` `HU-01` | ✅ `MOV-04` `PinScreen/MqttManager:162` |
| `Validación 4..6` `djb2` `throttle 5/60s` | ✅ `PinValidator:15` `PinViewModel:114` |
| `LED 5s/1s/10s` `OFF/UNKNOWN` `OFF intrusión expirada` | ✅ `LedStateMapper:33` `LedStatusCard:42` |
| `DataStore api_base_url` `10.0.2.2` editor `DashboardScreen:157` | ✅ `PreferencesManager:20` |
| `Room` cache offline `MOV-08` | ⏳ `AppDatabase.kt:12` esqueleto |
| `Joystick Rover` `aethernet/rover/command` `RF-1.2` `RF-3.1` | ⏳ `MqttManager:50` `rover/telemetry` ya push, falta `command` publish |
| `Bluetooth SPP` fallback `RF-1.3` | ⏳ `docs/requirements.md:18` |
| `Autenticación` `JWT` `multiusuario` `MOV-04 → DB` | ⏳ `pin_hash` ya, falta `user_id` real |

---

## 16. Referencias cruzadas (para evaluadores)

- `docs/prd.md:50` KPI `MQTT <50ms` (aquí `MQTT` `QoS 0` + `polling 5s` fallback) y `prd.md:30` In-Scope `Kotlin MVVM Compose` + `FOSS 53` + `prd.md:61` Contingencia `Edge` (MEGA sigue sin red, app `Room` futuro)
- `docs/requirements.md:15` `RF-1.1` Dashboard `Compose` + `RF-1.2` Joystick (futuro) + `RF-2.2` cerrojo `HU-01` (`PinScreen`) + `RF-2.3` intrusión `HU-02` (`LedStatusCard` rojo 10s)
- `docs/architecture.md:60` protocolos `MQTT 1883` `HTTP POST` `UART 38400` + `docs/hardware-inventory.md:9` LED local `44/45/46` (aquí `0xFF4CAF50` etc.)
- `docs/sprints.md:78` `MOV-01..MOV-04` `sprints.md:53` estado `192.168.1.14:8000` + `backend/app/main.py:68` `GET /` + `routers/events.py:31` `POST /api/access-events` (aquí `ApiService:31`)
- `firmware/mega-access/src/config.h:48 VALID_PIN 1234` `PIN_MAX_LEN 6` `keypad_control.cpp:99 hashPin` ↔ `PinValidator:27 hashPin` `djb2` idéntico + `firmware/mega-access/src/led.cpp:64` `DOOR_AUTO_LOCK_MS 5000` ↔ `LedStateMapper:35` + `DashboardViewModel:155`
- `backend/mosquitto/config/mosquitto.conf:4` `1883` `9001` `acl.conf:14 aethernet/#` ↔ `MqttManager:50` topics + `backend/init.sql:6` `access_events` (`models.py:20`) ↔ `AccessEventDto:9`
- `stats/ema_filter.py:32` `EMA α0.2` ↔ `RoverTelemetryMqtt.ultrasonic_cm` (ya filtrado en `rover-uno.ino:259`) aquí solo refleja `DashboardScreen:96`

---

## 17. Glosario (si vienes de web / Arduino)

| Término app | Equiv. React | Equiv. Arduino/C | Qué es |
|---|---|---|---|
| `@Composable` `StateFlow` `collectAsStateWithLifecycle` | `function Component()` `useState` `useSelector` | `loop()` + `isDoorUnlocked()` `millis()` | UI declarativa reactiva con ciclo de vida |
| `ViewModel` `viewModelScope.launch` | `useReducer` `useEffect` + `async` | `handleDoorAutoLock()` `delay()` no bloqueante | Lógica UI con coroutines (no bloquea hilo) |
| `ServiceLocator` `object` `by lazy` | `createContext` `useContext` | `secrets.h` `EEPROM` `static` singleton | DI manual singleton |
| `Retrofit` `ApiService` `suspend` `DTO` | `axios` `fetch` `interface ApiService` `Promise` | `HTTPClient` `POST` `ArduinoJson` `StaticJsonDocument` | HTTP tipado con DTOs `@Serializable` |
| `DataStore` `Flow` `stringPreferencesKey` | `localStorage` `Observable` | `EEPROM` `Preferences` | Persistencia key-value reactiva |
| `Paho` `MqttClient` `SharedFlow` `tryEmit` | `mqtt.js` `socket.on` `EventEmitter` | `PubSubClient` `mqttCallback` `RF24` `radio.available` | MQTT push `aethernet/#` |
| `PinValidator` `4..6` `djb2` | `Zod regex 4..6` `hash` | `isDigit` `PIN_MAX_LEN 6` `hashPin` `5381` | Validador + hash espejo firmware |
| `LedStateMapper` `GREEN 5s` `OFF` | `selector` `mapStateToProps` | `led.cpp:64 updateLed` `DOOR_AUTO_LOCK_MS` | Mapper determinista LED |

---

*Documentado como estudiante 6º semestre que conecta `React` (`Compose` como `React`, `StateFlow` como `useState`, `Navigation` como `React Router`) con `C` (`isDigit` `PIN_MAX_LEN` `djb2 5381` `DOOR_AUTO_LOCK_MS` `44/45/46` ánodo común) y `PostgreSQL` (`asyncpg` `TIMESTAMPTZ` `JSONB` `SELECT 1`) — probado con `34 tests` `./gradlew testDebugUnitTest` y `docker compose up` `192.168.1.14:8000` `aethernet/#`, FOSS de extremo a extremo (`RNF-3.1`).*
