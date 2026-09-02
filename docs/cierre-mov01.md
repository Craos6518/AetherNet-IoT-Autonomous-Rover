# Cierre Formal MOV-01 — Setup Android MVVM Base (Sprint 1→2)

**ID backlog:** MOV-01 | **Sprint:** 1 (Must) → cerrado Sprint 2 | **Rama:** `feature/app-setup-mvvm` | **Fecha:** 2026-09-01 — **✅ Done `f03190b`**
**Estado:** ✅ Done — MVVM base real verificada `f03190b` (cierra `⚠️ Plantilla-only 2026-08-31`)

> Formaliza el cierre pendiente listado en `docs/deuda-sprint1-sprint2.md:13` (— Cierre formal de MOV-01). MOV-12 queda fusionado con este cierre (primer build verde, no fix).
> **Sinceramiento 2026-08-31:** el dueño confirma "solo abrí el proyecto en Android Studio, no he lanzado una línea de código Kotlin". `app/app/src/main/java/com/aethernet/aethercontrol/MainActivity.kt:22` (`DashboardScreen`) y `app/.../DashboardViewModel.kt:32` (`DashboardUiState` + `TODO MQTT`) son scaffold del wizard `Empty Activity + Compose`, no lógica de negocio propia. Este doc pasa de `✅ Cerrado` a `⚠️ Plantilla-only`. Infra del setup (Gradle/KSP/CI) sí está verde; la lógica MVVM real queda para `feature/app-setup-mvvm` en Sprint 2.
> **Cierre real 2026-09-01 `f03190b`:** implementado y verificado en `SM-X620` + `docker 0.0.0.0:8000` — ver Evidencia 2026-09-01. `ACT-05` pasa a ✅ Done, deuda saldada.

## Criterio de aceptación BDD (requirements.md RF-1.1)

- Proyecto Android en `app/` con arquitectura MVVM base identificable, sin dependencia de nube propietaria (RNF-3.1).

## Evidencia verificada 2026-08-29

### Estructura existente (no generada en esta deuda, ya estaba tras "abrir en Studio" — PLANTILLA, sin código propio 2026-08-31)
- `app/build.gradle.kts:1` (project, `com.android.application 8.4.0 apply false`) + `app/settings.gradle.kts:12` (`include(":app")`, `rootProject.name = "AetherControl"`) — wizard
- `app/app/build.gradle.kts:1` (module, `namespace com.aethernet.aethercontrol`, `compileSdk 34`, `minSdk 24`, `buildFeatures.compose true`, KSP + Room + Retrofit/Moshi/Paho) — wizard
- `app/src/main/AndroidManifest.xml:1` permisos `INTERNET/BLUETOOTH/FOREGROUND_SERVICE` — wizard
- `app/src/main/java/com/aethernet/aethercontrol/MainActivity.kt:22` `DashboardScreen(viewModel)` con `AetherControlTheme` — scaffold, `DashboardViewModel.kt:32` solo `TODO MQTT` sin implementación
- `app/src/main/java/com/aethernet/aethercontrol/AetherControlApplication.kt:10` `Room.databaseBuilder(..., "aethernet-db")` — scaffold
- `app/src/main/java/com/aethernet/aethercontrol/data/local/AppDatabase.kt:11` `@Database(entities=[...])` + DAOs en `Daos.kt` — scaffold generado
- `app/src/main/java/com/aethernet/aethercontrol/ui/viewmodel/DashboardViewModel.kt:32` `DashboardUiState` (`Connected/Disconnected/Connecting/Error`) + `viewModelScope` + `MutableStateFlow` — sin lógica, `connect()`/`sendJoystickCommand()` son stubs
- `app/src/main/java/com/aethernet/aethercontrol/data/{remote,repository}` carpetas base (vacías, esqueleto para MOV-03 MQTT) — vacías

### Correcciones aplicadas en esta deuda (ACT-05/06)
- `.gitignore:72-73` removido `*.gradle`/`*.gradle.kts` que ignoraba `app/build.gradle.kts` (causa MOV-12)
- `app/build.gradle.kts:4` KSP `1.9.20-1.0.14` → `1.9.22-1.0.17` (alineado a Kotlin `1.9.22`, elimina warning `ksp too old`)
- `.gitignore:90-93` whitelist `!app/gradlew`, `!app/gradle/wrapper/gradle-wrapper.jar` para CI reproducible
- `app/gradlew` + `app/gradle/wrapper/gradle-wrapper.jar` (Gradle 9.5.0, copiado de template Helloworld 2026-08-06) + `chmod +x`
- `/.github/workflows/ci.yml:241` `android-build` de `if: false` → `hashFiles('app/build.gradle.kts',...) != ''` (se skipea hasta que wrapper + código Compose existan, sin bloquear pipeline)

### Verificación local 2026-08-29
```bash
bash -c "cd app && ./gradlew tasks"  # SUCCESS: tasks runnable, `packaging` deprecation única advertencia
arduino-cli version 1.5.1, backend ruff/pytest/mypy OK (ver ACT-03)
```
`assembleDebug` completo requiere Android SDK (no presente en runner actual) — se valida en CI con `android-actions/setup-android@v3` cuando se active MOV-02.

## Evidencia verificada 2026-09-01 `f03190b` — MOV-01 real ✅ Done

### Código implementado `feature/app-setup-mvvm` `f03190b` (26 files, +1057 -32)
- `app/src/main/java/com/aethernet/aethercontrol/AetherControlApp.kt:1` `Application.onCreate` → `ServiceLocator.init(this)` + `AndroidManifest.xml:5` `android:name=".AetherControlApp"`
- `app/src/main/java/com/aethernet/aethercontrol/core/di/ServiceLocator.kt:1` `object ServiceLocator` `by lazy` `OkHttpClient`+`Retrofit`(`Json {ignoreUnknownKeys,isLenient}`)+`ApiService`+`PreferencesManager`+`Repository`, `currentBaseUrl` dinámico, `updateBaseUrl(url)` normaliza + recrea `Retrofit`, `runBlocking` carga `DataStore` en `init`
- `app/src/main/java/com/aethernet/aethercontrol/ui/viewmodel/ViewModelFactory.kt:1` `DashboardViewModelFactory`
- `gradle/libs.versions.toml:1` `retrofit 2.11.0 okhttp 4.12.0 serialization 1.8.0 coroutines 1.8.1 lifecycle-viewmodel-compose 2.11.0 navigation 2.8.4 datastore 1.1.1 turbine 1.1.0` + `app/build.gradle.kts:1` `kotlin-serialization` plugin
- `app/src/main/java/com/aethernet/aethercontrol/data/remote/dto/*.kt:1` `HealthDto` `Access/Sensor/Security/RoverTelemetryDto` `@Serializable` espejo `backend/app/schemas.py:17,26,33,47,56,73,79,95,106`
- `app/src/main/java/com/aethernet/aethercontrol/data/remote/ApiService.kt:1` `GET /health / GET / POST api/access-events` etc. espejo `backend/app/main.py:48` `routers/events.py`
- `app/src/main/java/com/aethernet/aethercontrol/data/local/PreferencesManager.kt:34` `saveBaseUrl()` normaliza `http://.../` + `Flow`, `AppDatabase.kt:1` esqueleto `// TODO MOV-08`, `util/Result.kt:1` `safeCall`
- `app/src/main/java/com/aethernet/aethercontrol/data/repository/AetherRepository.kt:1` + `Impl.kt:1`, `domain/model/UiModels.kt:1` `DashboardUiState`, `ui/viewmodel/DashboardViewModel.kt:32` `StateFlow` `refreshHealth()` `init`, `ui/navigation/NavGraph.kt:1`, `ui/screens/DashboardScreen.kt:27` `collectAsStateWithLifecycle` + editor `Backend URL` `Guardar/Restaurar`, `MainActivity.kt:19` `viewModel(factory=...)` + `NavGraph`
- `app/src/main/res/xml/network_security_config.xml:1` `cleartext` `10.0.2.2`/`192.168.1.x` + `AndroidManifest.xml:17` `usesCleartextTraffic`+`networkSecurityConfig`
- `app/src/test/java/com/aethernet/aethercontrol/ui/viewmodel/DashboardViewModelTest.kt:1` `FakeRepo` `UnconfinedTestDispatcher` `turbine` 4 tests

### Verificación local 2026-09-01
```bash
./gradlew :app:assembleDebug              # BUILD SUCCESSFUL 38 tasks (15s/31s)
./gradlew :app:testDebugUnitTest          # BUILD SUCCESSFUL 4 tests verde
./gradlew :app:dependencies | grep retrofit # retrofit 2.11.0 + converter 1.0.0
docker compose up -d && curl -s http://192.168.1.14:8000/health | grep ok # {"status":"ok","database":"ok"} 0.0.0.0:8000
adb shell ping -c 3 192.168.1.14           # 0% loss
adb shell "curl -v http://192.168.1.14:8000/health" # 200 OK en SM-X620 (Chrome y App ok tras Guardar URL)
```

## Pendiente (fuera de deuda, va a Sprint 2/3 — sincerado 2026-08-31) — Actualizado 2026-09-01 ✅ Done
- **MOV-01 real:** ✅ Done `f03190b` — MVVM con `StateFlow`, `Repository` Retrofit, navegación Compose, DI manual — en `feature/app-setup-mvvm` (Sprint 2).
- Pantallas `MOV-02` (bombillo Tuya + LED), `MOV-03` MQTT, `MOV-04` PIN — backlog `docs/backlog.md:19-21` (siguiente).
- Tests `MOV-10` JUnit ViewModels — base `DashboardViewModelTest` en `f03190b`, ampliar en `MOV-02..06`.

## Siguiente
Marcar `docs/deuda-sprint1-sprint2.md:13-14` como `[x]` ✅ Done `f03190b` y continuar con PM-02/PM-04. `feature/app-setup-mvvm` lista para PR a `develop`.
