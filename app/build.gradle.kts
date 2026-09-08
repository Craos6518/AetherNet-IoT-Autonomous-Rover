// =============================================================================
// build.gradle.kts — Build App Android | 6º Semestre UTP | MOV-01, MOV-03, RF-1.1, RNF-3.1
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años JS/React (package.json, Vite), 2 años Python (pyproject.toml),
//              2 años electrónica (platformio.ini)
// Analogía React: este archivo es como `package.json` + `vite.config.ts` en React — declara plugins, SDK y dependencias.
// Analogía Python: como `pyproject.toml` en stats/ — aquí con `plugins` y `dependencies` para Android.
// Analogía C: como `platformio.ini` en firmware/ — define `compileSdk`, `minSdk`, `lib_deps` pero para Android.
// FOSS: Gradle + Compose + Retrofit + Paho — RNF-3.1, sin SDK propietario (como Tuya).
// Origen: MOV-01 (MVVM, Compose, Retrofit), MOV-03 (Paho MQTT), RF-1.1 Dashboard tiempo real.
// =============================================================================
plugins {
    alias(libs.plugins.android.application) // Android Application — como `react-scripts` en React (build APK)
    alias(libs.plugins.kotlin.compose) // Kotlin Compose — como `vite-plugin-react` en Vite (Compose compiler)
    alias(libs.plugins.kotlin.serialization) // Kotlin Serialization — como `babel-plugin` para JSON (genera serializers)
}

android {
    namespace = "com.aethernet.aethercontrol" // namespace — como `name` en package.json (`com.aethernet.aethercontrol`)
    compileSdk {
        version = release(37) // compileSdk 37 (Android 15) — como `target: es2022` en TS (máximo que compilas)
    }

    defaultConfig {
        applicationId = "com.aethernet.aethercontrol" // appId — como `name` en package.json + `bundleId` en React Native
        minSdk = 24 // minSdk 24 (Android 7.0) — como `browserslist: ["chrome >= 60"]` en React (mínimo soportado)
        targetSdk = 37 // targetSdk 37 — como `targetSdk` en Android (optimizado para 15)
        versionCode = 1 // versionCode 1 — como `versionCode` en React Native (entero para Play Store)
        versionName = "1.0" // versionName "1.0" — como `version: "1.0.0"` en package.json (visible usuario)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // runner tests instrumented — como `jest` runner en React
    }

    buildTypes {
        release {
            optimization {
                enable = false // sin minify en release Sprint 1 — como `minify: false` en Vite (para debug, luego true)
            }
        }
        debug {
            // Permite BuildConfig.DEBUG para HttpLoggingInterceptor (ServiceLocator.kt:68) — como `process.env.NODE_ENV !== 'production'` en React
            isMinifyEnabled = false // sin minify en debug — como `minify: false` en dev
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Java 11 — como `target: es11` en TS (compatibilidad)
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true // Compose — como `jsx: react` en tsconfig.json (habilita @Composable)
        buildConfig = true // BuildConfig — como `process.env` en React (genera BuildConfig.DEBUG)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom)) // BOM Compose — como `react@18` BOM en React (versiones alineadas)
    implementation(libs.androidx.activity.compose) // Activity Compose — como `react-dom` en React (setContent)
    implementation(libs.androidx.compose.material3) // Material3 — como `mui/material` en React (Card, Button, Text)
    implementation(libs.androidx.compose.ui) // Compose UI — como `react` core (Column, Row, Box)
    implementation(libs.androidx.compose.ui.graphics) // graphics — como `canvas` en React (background, Color)
    implementation(libs.androidx.compose.ui.tooling.preview) // preview — como Storybook en React (Preview)
    implementation(libs.androidx.core.ktx) // core-ktx — como `core-js` en JS (extensiones Kotlin para Android)
    implementation(libs.androidx.lifecycle.runtime.ktx) // lifecycle runtime — como `react-lifecycle` en React (viewModelScope)
    implementation(libs.androidx.lifecycle.runtime.compose) // lifecycle compose — como `react-hooks` para Compose (collectAsStateWithLifecycle)
    // MOV-01: MVVM + Navigation + DI manual (RNF-3.1 FOSS, RF-1.1) — como `react-router` + `zustand` en React
    implementation(libs.androidx.lifecycle.viewmodel.compose) // viewmodel-compose — como `zustand` en React (ViewModel + viewModel() hook)
    implementation(libs.androidx.navigation.compose) // navigation-compose — como `react-router-dom` en React (NavHost, NavController)
    // Retrofit + OkHttp + kotlinx.serialization (FOSS) — como `axios` + `zod` en React (HTTP tipado)
    implementation(libs.retrofit.core) // Retrofit — como `axios` en JS (HTTP client tipado con ApiService)
    implementation(libs.retrofit.kotlinx.serialization) // Retrofit serialization — como `axios` con `zod` parser (JSON)
    implementation(libs.okhttp.core) // OkHttp — como `fetch` en JS (HTTP client bajo Retrofit)
    implementation(libs.okhttp.logging) // OkHttp logging — como `morgan` en Express (log HTTP BODY en debug)
    implementation(libs.kotlinx.serialization.json) // kotlinx serialization — como `zod` en JS (parse JSON DTOs)
    // Coroutines — como `async/await` en JS (suspend, Flow)
    implementation(libs.kotlinx.coroutines.android) // coroutines android — como `async` en React Native (Dispatchers.Main)
    implementation(libs.kotlinx.coroutines.core) // coroutines core — como `Promise` en JS (launch, async, Flow)
    // DataStore Preferences — como `localStorage` en web pero con Flow (ver PreferencesManager.kt:20)
    implementation(libs.datastore.preferences) // DataStore — como `localForage` en JS (key-value async con Flow)
    // MOV-03: MQTT Paho FOSS (EPL 1.0, RNF-3.1) — tcp://host:1883 + fallback ws://host:9001 (Mosquitto 1883/9001)
    implementation(libs.paho.mqttv3) // Paho MQTTv3 — como `mqtt.js` en Node (client MQTT para aethernet/#)
    testImplementation(libs.junit) // JUnit — como `jest` en React (unit tests)
    testImplementation(libs.kotlinx.coroutines.core) // coroutines test — como `jest` con async/await
    testImplementation(libs.kotlinx.coroutines.test) // coroutines test — como `jest` con `runTest` (test coroutines)
    testImplementation(libs.turbine) // turbine — como `jest` con `turbine` para Flow (test StateFlow/SharedFlow)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4) // Compose test — como `@testing-library/react` en React (test UI)
    androidTestImplementation(libs.androidx.espresso.core) // Espresso — como `cypress` en React (UI tests instrumented)
    androidTestImplementation(libs.androidx.junit) // AndroidX JUnit — como `jest` para instrumented
    debugImplementation(libs.androidx.compose.ui.test.manifest) // test manifest debug — como Storybook manifest
    debugImplementation(libs.androidx.compose.ui.tooling) // tooling debug — como `react-devtools` en React
}
