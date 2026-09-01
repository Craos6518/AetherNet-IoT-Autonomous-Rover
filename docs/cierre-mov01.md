# Cierre Formal MOV-01 — Setup Android MVVM Base (Sprint 1)

**ID backlog:** MOV-01 | **Sprint:** 1 (Must) | **Rama:** `feature/firmware-mega-cerrojo` (deuda Sprint 1→2) | **Fecha:** 2026-08-29 — **Sincerado 2026-08-31**
**Estado:** ⚠️ Plantilla Studio — sin código Kotlin propio (sincerado a petición del dueño 2026-08-31)

> Formaliza el cierre pendiente listado en `docs/deuda-sprint1-sprint2.md:13` (— Cierre formal de MOV-01). MOV-12 queda fusionado con este cierre (primer build verde, no fix).
> **Sinceramiento 2026-08-31:** el dueño confirma "solo abrí el proyecto en Android Studio, no he lanzado una línea de código Kotlin". `app/app/src/main/java/com/aethernet/aethercontrol/MainActivity.kt:22` (`DashboardScreen`) y `app/.../DashboardViewModel.kt:32` (`DashboardUiState` + `TODO MQTT`) son scaffold del wizard `Empty Activity + Compose`, no lógica de negocio propia. Este doc pasa de `✅ Cerrado` a `⚠️ Plantilla-only`. Infra del setup (Gradle/KSP/CI) sí está verde; la lógica MVVM real queda para `feature/app-setup-mvvm` en Sprint 2.

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

## Pendiente (fuera de deuda, va a Sprint 2/3 — sincerado 2026-08-31)
- **MOV-01 real:** escribir Kotlin propio — MVVM con `StateFlow`, `Repository` MQTT/Retrofit, navegación Compose, inyección — en `feature/app-setup-mvvm` (Sprint 2). La plantilla actual no cuenta como Done.
- Pantallas `MOV-02` (bombillo Tuya + LED), `MOV-03` MQTT, `MOV-04` PIN — backlog `docs/backlog.md:19-21`.
- Tests `MOV-10` JUnit ViewModels.

## Siguiente
Marcar `docs/deuda-sprint1-sprint2.md:13-14` como `[x]` y continuar con PM-02/PM-04.
