package com.aethernet.aethercontrol.data.local

// =============================================================================
// PreferencesManager.kt — DataStore Preferences | 6º semestre UTP | MOV-01 3.1 + MOV-XX
// Autor: Andres Felipe Martínez Henao
// Experiencia: 2 años Python (dotenv), 2 años JS/React (localStorage), 1 año PostgreSQL,
//              2 años electrónica (secrets.h)
// Analogía React: este archivo es `localStorage` en web — guarda `apiBaseUrl` como
// `localStorage.setItem('apiBaseUrl', 'http://192.168.1.50:8000/')`, pero con Flow reactivo.
// Analogía Python: como `python-dotenv` + `DATABASE_URL` en backend/app/config.py — aquí con DataStore.
// Analogía C/Arduino: como `EEPROM` del MEGA para guardar `VALID_PIN` — aquí guarda URL base.
// Analogía PostgreSQL: no es tabla, es key-value local (como SharedPreferences, pero con DataStore async).
// FOSS: DataStore Preferences (AndroidX, Apache 2.0) — RNF-3.1, reemplaza SharedPreferences (menos bugs).
// Origen: MOV-01 3.1 (RF-1.1) — BaseUrl extraída aquí, no hardcodeada en ViewModel (cf. 2.3.2).
// MOV-XX añade: MQTT broker, Theme mode, Fail-safe timeout, Max speed %.
// Usado por: ServiceLocator.init() carga runBlocking, ServiceLocator.updateBaseUrl() persiste.
// =============================================================================

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// Extension Context.dataStore — singleton DataStore "aethernet_prefs"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aethernet_prefs")

/**
 * PreferencesManager — MOV-01 3.1 (RF-1.1) + MOV-XX (Ajustes).
 * Almacena configuración local via DataStore (async, type-safe, sin blocking I/O como SharedPreferences).
 * Sin Room aún — solo Preferences (key-value). BaseUrl extraída aquí, no hardcodeada en ViewModel.
 * Si vienes de React: es `localStorage` pero con `Flow` (reactivo) en vez de `getItem` sync.
 * Si vienes de Python: es `dotenv` pero persistente (guarda, no solo lee).
 */
class PreferencesManager(private val dataStore: DataStore<Preferences>) {

    object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val USER_ID = stringPreferencesKey("user_id")
        val MQTT_BROKER = stringPreferencesKey("mqtt_broker")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "CLARO" | "OSCURO" | "SISTEMA"
        val FAIL_SAFE_MS = intPreferencesKey("fail_safe_ms")
        val MAX_SPEED_PCT = intPreferencesKey("max_speed_pct")
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
        const val DEFAULT_MQTT_BROKER = "192.168.1.50:1883"
        const val DEFAULT_THEME_MODE = "SISTEMA"
        const val DEFAULT_FAIL_SAFE_MS = 1500
        const val DEFAULT_MAX_SPEED_PCT = 85
    }

    // Flow<String> — reactivo, emite cada vez que cambia
    val apiBaseUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.API_BASE_URL] ?: DEFAULT_BASE_URL
    }

    val mqttBroker: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.MQTT_BROKER] ?: DEFAULT_MQTT_BROKER
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "SISTEMA"
    }

    val failSafeMs: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.FAIL_SAFE_MS] ?: 1500
    }

    val maxSpeedPct: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.MAX_SPEED_PCT] ?: 85
    }

    suspend fun saveBaseUrl(url: String) {
        val normalized = normalizeUrl(url)
        dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = normalized
        }
    }

    suspend fun getBaseUrlOnce(): String = apiBaseUrl.first()

    suspend fun saveMqttBroker(broker: String) {
        dataStore.edit { prefs ->
            prefs[Keys.MQTT_BROKER] = broker.trim()
        }
    }

    suspend fun getMqttBrokerOnce(): String = mqttBroker.first()

    suspend fun saveThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun getThemeModeOnce(): String = themeMode.first()

    suspend fun saveFailSafeMs(ms: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.FAIL_SAFE_MS] = ms.coerceIn(500, 5000)
        }
    }

    suspend fun getFailSafeMsOnce(): Int = failSafeMs.first()

    suspend fun saveMaxSpeedPct(pct: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.MAX_SPEED_PCT] = pct.coerceIn(0, 100)
        }
    }

    suspend fun getMaxSpeedPctOnce(): Int = maxSpeedPct.first()

    val userId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.USER_ID]
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
        }
    }

    // Normaliza URL — como `new URL(url).toString()` en JS pero simple
    private fun normalizeUrl(raw: String): String {
        var u = raw.trim()
        require(u.isNotBlank()) { "URL vacía" }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        if (!u.endsWith("/")) u += "/"
        require(u.length > "http://".length + 1) { "URL inválida: $raw" }
        return u
    }
}

/** Helper para crear PreferencesManager desde Context (usado por ServiceLocator:106). */
fun Context.preferencesManager(): PreferencesManager = PreferencesManager(dataStore)