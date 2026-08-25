# Documentación Académica — Programación Móvil (TS6C3)

**Proyecto:** AetherNet IoT & Autonomous Rover
**Asignatura UTP:** Programación Móvil — Código TS6C3, 3 créditos (`docs/UTP/2361_6. TS6C3 Programación Móvil (1).pdf`)
**Área del proyecto:** Área 1 — Programación Para Móviles (App "AetherControl") · ver `docs/backlog.md` §Área 1 · ítems MOV-01 a MOV-10

> ⚠️ **Nota de adaptación tecnológica importante:** el PDF del programa está redactado sobre la pila **.NET / C# / Xamarin.Forms / XAML**. El proyecto implementa los **mismos conceptos y patrones** con la pila nativa moderna **Kotlin / Jetpack Compose**, según exige el PRD (Android nativo). Este documento mapea cada unidad del PDF a su equivalente real en el proyecto, dejando explícita la equivalencia conceptual.

---

## 1. Contenido académico según el PDF

| Unidad | Contenido (PDF) | Equivalente en el proyecto |
|---|---|---|
| **U1** | Plataforma .NET: Framework, IDE/tools, bibliotecas, compilación/depuración; lenguaje: sintaxis, variables, selección, iteración, operadores, arrays y colecciones | Plataforma Android + Kotlin (equivalente funcional del framework): Android Studio como IDE, Gradle como build system, SDK/librerías Jetpack |
| **U2** | Programación Móvil **Front**: arquitecturas móviles; lenguaje de UI **XAML**; Pages, StackLayout/Grid, Views | UI declarativa con **Jetpack Compose**: composables (≈ Views), `Column`/`Row`/`LazyGrid` (≈ StackLayout/Grid), pantallas como funciones (≈ Pages); theming |
| **U3** | Programación Móvil **Back**: Xamarin.Forms, emuladores iOS/Android, patrón **MVVM**, desarrollo de aplicaciones | Patrón **MVVM** con ViewModel + `StateFlow`/`LiveData`; emulador AVD de Android Studio; arquitectura recomendada por Google |
| **U4** | Administración de datos y entorno: consumo de servicios **JSON–REST**, APIs nativas, conexiones; hardware del dispositivo (**permisos y privacidad**, acceso a hardware); desarrollo nativo iOS/Android; multiplataforma | Consumo vía **MQTT/WebSocket** (+ Retrofit para REST donde aplique); APIs nativas Bluetooth SPP con permisos runtime; persistencia local con Room; Kotlin nativo Android |

---

## 2. Mapa: tema académico → aplicación en el proyecto → área

### U1 — Plataforma y Lenguaje ✅ Aplicado (Kotlin en vez de C#)

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Framework, IDE, compilación y depuración | Android Studio + Gradle; pipeline de build preparado en CI (JDK 17, setup-android, `gradlew assembleDebug`) | `.github/workflows/ci.yml` job `android-build` (plantilla lista) |
| Estructura/sintaxis del lenguaje, tipos | **Kotlin** en lugar de C#: null-safety nativa, data classes para modelos de dominio, sealed interfaces para estados | `app/src/main/java/com/aethernet/aethercontrol/ui/viewmodel/DashboardViewModel.kt` (data classes `RoverTelemetry`, `AccessEvent`; sealed interface `DashboardUiState`) |
| Colecciones | `Map<String, Boolean>` para estados de relés; colecciones inmutables expuestas al estado de UI | `DashboardViewModel.kt:13` (`relayStates: Map<String, Boolean>`) |
| Instrucciones asíncronas (concepto moderno equivalente) | **Coroutines + Flow**: `viewModelScope.launch`, `MutableStateFlow` — sin bloquear el hilo principal | `DashboardViewModel.kt:7-9,43-56` |

### U2 — Front Móvil ✅ Aplicado (Compose en vez de XAML)

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Arquitecturas móviles | App nativa Android (decisión de arquitectura documentada en PRD §4; iOS/web fuera de alcance) | `docs/prd.md` §4 |
| Lenguaje declarativo de UI (XAML) | **Jetpack Compose**: UI declarativa en Kotlin — mismo principio que XAML (se declara QUÉ se ve, no cómo dibujarlo) | `app/src/main/java/com/aethernet/aethercontrol/ui/theme/Theme.kt` (theming Compose) |
| Pages | Pantallas como composables raíz; actividad única (`MainActivity`) con navegación por estado | `MainActivity.kt` |
| StackLayout / Grid | Equivalentes Compose: `Column`/`Row` para layout lineal (dashboard de relés), `LazyVerticalGrid` si la grilla lo requiere (RF-1.1 Dashboard) | Por construir — backlog MOV-02/MOV-08 |
| Views / controles | Controles planificados: switches de relés (RF-1.1), **joystick virtual custom** capturando vectores X,Y (RF-1.2), indicadores de telemetría | Backlog MOV-02, MOV-05, MOV-08 |

### U3 — Back Móvil / MVVM ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Patrón **MVVM** | Implementado literalmente: `View` (Compose) observa `uiState` → `DashboardViewModel` expone estado inmutable → capa de datos (Room + MQTT planeado). El PDF lo enseña con Xamarin.Forms; el proyecto usa el mismo patrón con el stack oficial actual de Google | `DashboardViewModel.kt` completo; convención en `docs/roadmap.md` §1 |
| Estado de UI modelado | `sealed interface DashboardUiState` con 4 estados exhaustivos: `Connected(relayStates, roverTelemetry, lastAccessEvent)`, `Disconnected(reason)`, `Connecting`, `Error(message)` — la UI no puede quedar en un estado indefinido | `DashboardViewModel.kt:11-23` |
| Emuladores | AVD (Android Virtual Device) de Android Studio para pruebas sin hardware físico | Flujo de desarrollo estándar |
| Reactividad View↔ViewModel | Puente `MutableStateFlow` (fuente de verdad interna) → `.asLiveData()` (observación por la vista) — separación estricta: la Vista nunca toca datos directamente | `DashboardViewModel.kt:43-47` |

### U4 — Administración de Datos y Hardware ✅ Aplicado

| Lo que se ve en el contenido | Lo que se aplica en el proyecto | Dónde está aplicado |
|---|---|---|
| Consumo de servicios JSON–REST | **Adaptación clave:** el canal principal es **MQTT/WebSocket** (pub/sub en tiempo real, requisito RF-1.1), no REST puro — decisión de arquitectura IoT. REST/Retrofit queda para operaciones request/response si se requieren. Los topics MQTT transportan payloads JSON igual que un API REST | `docs/roadmap.md` §1 (Paho/HiveMQ, Retrofit/OkHttp); contratos de datos en `backend/app/models.py` |
| Conexiones / tiempo real | Suscripción a topics de telemetría (`aethernet/rover/...`, `aethernet/access/pin`, `aethernet/relay/{id}` definidos como TODOs contractuales) | `DashboardViewModel.kt:65-75` |
| Persistencia local (administración de datos) | **Room Database** definida: entidades, DAOs y base con acceso tipado | `data/local/Entities.kt`, `Daos.kt`, `AppDatabase.kt` |
| Permisos y privacidad (hardware) | Permisos runtime Android 12+ identificados para el fallback Bluetooth: `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` (escaneo BT clásico SPP ≠ BLE, confusión común documentada) | `docs/roadmap.md` §1; requisito RF-1.3 |
| Acceso al hardware del dispositivo | **Bluetooth SPP** para contingencia si cae Wi-Fi (RF-1.3, backlog MOV-07); el "hardware" remoto (Rover, relés, cerrojo) se controla vía MQTT→ESP32 | `docs/requirements.md` RF-1.3; `docs/hardware-inventory.md` (HC-06) |
| Desarrollo nativo Android | 100% nativa en **Kotlin** (el PDF menciona Xamarin como multiplataforma; el PRD excluye iOS y multiplataforma deliberadamente) | `docs/prd.md` §4 out-of-scope |

---

## 3. Estado real de implementación del área (honestidad técnica)

La app está en fase **fundacional**: la arquitectura (MVVM + estados sellados + Room + theming) ya existe, pero los clientes de comunicación son stubs con contrato definido:

| Componente | Estado | Evidencia |
|---|---|---|
| Estructura MVVM + UiState | ✅ Hecho | `DashboardViewModel.kt` |
| Base de datos local Room | ✅ Definida | `data/local/*` |
| Cliente MQTT | ❌ TODO contractual (`connect()`, publicación a topics) | `DashboardViewModel.kt:50-75` |
| Pantallas Compose de relés/dashboard | ❌ Pendiente | Backlog MOV-02/MOV-08 (Sprint 2/4) |
| Joystick virtual | ❌ Pendiente | Backlog MOV-05/06 (Sprint 3) |
| Fallback Bluetooth SPP | ❌ Pendiente | Backlog MOV-07 (Sprint 3) |
| Tests unitarios de ViewModels | ❌ Pendiente | Backlog MOV-10 (Sprint 4, JUnit) |
| Build CI de Android | ⚠️ Plantilla desactivada hasta que exista Gradle | `ci.yml` job `android-build` (`if: false`) |

---

## 4. Pendientes / brechas (trazable al backlog)

MOV-01 (setup base, en curso) · MOV-02..MOV-10 según tabla anterior. Riesgo del área documentado: MOV-06 (latencia joystick <10 ms) depende del enlace RF probado en Sprint 1 — atraso en DevOps/Firmware = atraso en cascada aquí.

## 5. Trazabilidad

- **Requisitos funcionales:** RF-1.1 (Dashboard tiempo real), RF-1.2 (Joystick), RF-1.3 (Bluetooth fallback)
- **HU:** HU-01 (interfaz de desbloqueo PIN desde app)
- **Sprints:** Sprint 2 (pantallas relés) → Sprint 3 (joystick+telemetría) → Sprint 4 (dashboard consolidado + tests)
