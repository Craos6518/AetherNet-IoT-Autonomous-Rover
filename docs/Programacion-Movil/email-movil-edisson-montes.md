# Email — Programación Móvil TS6C3 (Edisson Montes) — Versión final

**Asunto:** Propuesta y avances del componente móvil (AetherControl) — TS6C3 Gr. 401

**Repositorio:** https://github.com/Craos6518/AetherNet-IoT-Autonomous-Rover

---

Estimado profesor Edisson:

Espero que usted y sus seres queridos se encuentren bien tras el sismo del 10 de agosto (7:34 AM, mag. 7.4). Por mi parte, mi familia está a salvo y he podido apoyar labores de voluntariado local sin descuidar el trabajo académico desde casa. Solo tuvimos una semana de clases antes del evento.

Atendiendo a su confirmación en clase de que trabajaremos con **Kotlin + Android Studio + Jetpack Compose**, estructuré la propuesta formal del componente móvil de **AetherNet IoT & Autonomous Rover** — App nativa **AetherControl**, interfaz humana del sistema — y avancé en la arquitectura base MVVM validada en dispositivo físico.

**Aclaración tecnológica (trazable):** El PDF del programa describe la pila .NET/C#/Xamarin.Forms/XAML. El proyecto implementa los **mismos conceptos y patrones** con la pila nativa moderna **Kotlin/Compose** (estándar Android 2024-2025, 100% FOSS según RNF-3.1). La equivalencia está mapeada en `docs/materias/programacion-movil.md:7`.

**Alineación con el syllabus TS6C3 (U1–U4):**

* **U1 Plataforma y Lenguaje (.NET → Kotlin):** Android Studio + Gradle como plataforma/IDE/build system; Kotlin con null-safety, data classes (`RoverTelemetry`, `AccessEvent`) y sealed interfaces (`DashboardUiState`), Flow/Coroutines para asincronía — equivalente funcional a C#/colecciones del PDF.
* **U2 Front Móvil (XAML → Compose):** UI declarativa en Compose — `Column`/`Row`/`LazyColumn` ≈ StackLayout/Grid, pantallas como `@Composable` ≈ Pages, theming Material3; recomposición automática por estado.
* **U3 Back Móvil (MVVM + Emuladores):** MVVM nativo: `View` (Compose) observa `uiState` inmutable expuesto por `DashboardViewModel` vía `StateFlow`; `ViewModel` + `Repository` con `viewModelScope`; AVD + dispositivo físico SM-X620 para validación.
* **U4 Datos y Entorno (REST/JSON, Hardware, Permisos):** Canal principal **MQTT/WebSocket** pub/sub (RF-1.1) con payloads JSON — Retrofit/OkHttp reservado para REST puntual; persistencia local **Room** (Entities/DAOs/AppDatabase); hardware vía **Bluetooth SPP (RFCOMM)** como contingencia si cae Wi-Fi (RF-1.3, `BLUETOOTH_CONNECT` en Android 12+).

**Funcionalidades núcleo de AetherControl (criterios BDD):**

| Feature | Usuario | Tecnología clave | Criterio BDD |
|---|---|---|---|
| Dashboard Telemetría | Ve distancia, estado puerta, alertas en vivo | Compose + `StateFlow` ← `Repository` (MQTT) | Dado conectado, cuando hay lectura, entonces UI <200 ms |
| Joystick Virtual | Arrastra → vector X/Y [-1,1] → throttle 50 ms → `control/rover/cmd` | `PointerInput` + drag → MQTT | Comando enviado cada 50 ms máx |
| PIN Cerrojo | Teclado numérico → `access/door/unlock` → servo 90° + LED verde | Compose + MQTT | HU-01: Dado cerrada, cuando PIN correcto + #, entonces servo + LED |
| Alerta Intrusión | Notificación HIGH + banner rojo si láser KY-008 se corta | `NotificationManager` | HU-02: Dado Armado, cuando láser interrumpido, entonces notificación <2 s |
| Fallback Bluetooth | Wi-Fi caído → Modo BT → HC-06 (Nano) → control básico | `BluetoothSocket` RFCOMM/SPP | RF-1.3: Dado Wi-Fi caído, cuando activa BT, entonces comandos llegan |

**Entregables específicos propuestos:**

| Aplicación | Método / Stack real | Resultado esperado |
|---|---|---|
| Arquitectura base | MVVM + `StateFlow` + `ServiceLocator` DI manual + `Retrofit`/`kotlinx.serialization` + `DataStore` | Build `assembleDebug` verde y `testDebugUnitTest` verde (verificado) |
| Dashboard + Telemetría | Compose + `PreferencesManager.updateBaseUrl()` + `network_security_config` cleartext | App conecta a `192.168.1.14:8000/health` ok en SM-X620 |
| Joystick + BT (progresivo) | Compose `PointerInput` + MQTT throttling + `BluetoothSocket` SPP | MOV-05/06/07 — Sprints 3-4, depende de RF probado |

**Estado actual (honestidad técnica — `docs/materias/programacion-movil.md:65`):** MOV-01 ✅ Done `f03190b` `feature/app-setup-mvvm` 2026-09-01 — `AetherControlApp`, `ServiceLocator` DI manual, DTOs espejo `schemas.py`, `DashboardViewModel` con `StateFlow`/`NavGraph`/`DashboardScreen` editor URL, verificado `curl 192.168.1.14:8000/health` + `assembleDebug`. MOV-02..10 pendientes por sprints (MQTT client, pantallas, joystick, BT SPP, tests JUnit). `ci.yml` job `android-build` en plantilla condicional hasta existir Gradle completo — no se presenta como hecho.

**Documentación de referencia en el repositorio:**

* Mapa académico U1–U4 (.NET → Kotlin/Compose): `docs/materias/programacion-movil.md`
* Backlog móvil (MOV-01..10, MoSCoW, sprints): `docs/backlog.md:14`
* Roadmap por materia (despliegue por sprint): `docs/materias/roadmap-movil.md` + `docs/roadmap.md:0`
* Requisitos y HU BDD: `docs/requirements.md:14` (RF-1.1..1.3) y `docs/requirements.md:51` (HU-01..04)
* Sprints (Sprint 2 pantallas, Sprint 3 joystick/telemetría): `docs/sprints.md:19` y `docs/sprints.md:32`
* App verificada: `app/src/main/java/.../AetherControlApp.kt`, `ServiceLocator`, `DashboardViewModel.kt:11` (`DashboardUiState`), `hardware-inventory.md:9` (LED RGB local — bombillo Tuya cancelado ADR-001)
* Estado Sprint 1 y deuda saldada: `docs/cierre-mov01.md:4` y `docs/deuda-sprint1-sprint2.md:36`

Quedo atento a sus indicaciones sobre cronograma, entregables (APK firmado + código + demo) y modalidad de evaluación.

Un saludo cordial,

**Andres Felipe Martinez Henao**
Estudiante TS6C3 Programación Móvil — Grupo 401
Proyecto Integrador: AetherNet IoT & Autonomous Rover
