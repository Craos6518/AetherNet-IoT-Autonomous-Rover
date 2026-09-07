package com.aethernet.aethercontrol

import android.app.Application
import com.aethernet.aethercontrol.core.di.ServiceLocator

/**
 * Application class — RF-1.1, RNF-3.1.
 * Instancia ServiceLocator en onCreate para DI manual FOSS (sin Hilt/Koin).
 * ServiceLocator.init() carga la URL guardada en DataStore (PreferencesManager.saveBaseUrl)
 * de forma síncrona para que el primer Retrofit use la IP correcta (ej. 192.168.x.x en
 * dispositivo físico, no solo 10.0.2.2 del emulador).
 */
class AetherControlApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
