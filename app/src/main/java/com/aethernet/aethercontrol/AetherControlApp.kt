package com.aethernet.aethercontrol

// =============================================================================
// AetherControlApp.kt — Application Class | 6º Semestre UTP | RF-1.1, RNF-3.1
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años JS/React (App root), 2 años Python (service locator), 1 año C
// Analogía React: este class es como `function App() { useEffect(() => { ServiceLocator.init() }, []) }` en React
// — punto de entrada de la app Android, se ejecuta antes que MainActivity (como `index.js` en React).
// FOSS: Application (Android, Apache 2.0) — RNF-3.1.
// Origen: RF-1.1 (Dashboard tiempo real), RNF-3.1 FOSS (sin Hilt/Koin). ServiceLocator.init() carga URL guardada
// en DataStore (PreferencesManager.saveBaseUrl) de forma síncrona para que el primer Retrofit use la IP correcta
// (ej. 192.168.x.x en dispositivo físico, no solo 10.0.2.2 del emulador).
// =============================================================================

import android.app.Application // Application — como `App` en React Native (clase base de la app Android)
import com.aethernet.aethercontrol.core.di.ServiceLocator // ServiceLocator — DI manual (como createContext en React, pero singleton)

/**
 * Application class — RF-1.1, RNF-3.1.
 * Instancia ServiceLocator en onCreate para DI manual FOSS (sin Hilt/Koin, ver ServiceLocator.kt:30).
 * ServiceLocator.init() carga la URL guardada en DataStore (PreferencesManager.saveBaseUrl)
 * de forma síncrona (runBlocking) para que el primer Retrofit use la IP correcta (ej. 192.168.x.x en
 * dispositivo físico, no solo 10.0.2.2 del emulador — ver PreferencesManager.DEFAULT_BASE_URL y DashboardScreen Tip).
 * Si vienes de React: es como `function App() { useEffect(() => { initServiceLocator() }, []) }` en React
 * pero en Android nativo (se ejecuta una vez al iniciar la app, antes que cualquier Activity).
 */
class AetherControlApp : Application() {
    override fun onCreate() {
        super.onCreate() // super — como `super()` en JS class
        ServiceLocator.init(this) // init DI — como `ServiceLocator.init(context)` en JS (carga DataStore, crea Retrofit con URL guardada)
    }
}
