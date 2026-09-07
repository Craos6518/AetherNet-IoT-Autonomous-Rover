package com.aethernet.aethercontrol.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aethernet_prefs")

/**
 * PreferencesManager — MOV-01 3.1 (RF-1.1).
 * Almacena configuración local via DataStore. Sin Room aún.
 * BaseUrl extraída aquí, no hardcodeada en ViewModel (cf. 2.3.2).
 */
class PreferencesManager(private val dataStore: DataStore<Preferences>) {

    object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val USER_ID = stringPreferencesKey("user_id")
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
    }

    val apiBaseUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.API_BASE_URL] ?: DEFAULT_BASE_URL
    }

    /**
     * Guarda nueva baseUrl normalizada.
     * - trim, añade http:// si falta esquema
     * - garantiza "/" final (Retrofit exige terminar en "/")
     * - lanza IllegalArgumentException si queda vacía
     * Uso: `ServiceLocator.updateBaseUrl(url)` para que además recree Retrofit.
     * Directo: `preferencesManager.saveBaseUrl("192.168.1.50:8000")` -> `http://192.168.1.50:8000/`
     */
    suspend fun saveBaseUrl(url: String) {
        val normalized = normalizeUrl(url)
        dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = normalized
        }
    }

    /** Lectura suspendida útil para ServiceLocator.loadSavedBaseUrl() en Application.onCreate */
    suspend fun getBaseUrlOnce(): String = apiBaseUrl.first()

    val userId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.USER_ID]
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
        }
    }

    private fun normalizeUrl(raw: String): String {
        var u = raw.trim()
        require(u.isNotBlank()) { "URL vacía" }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        if (!u.endsWith("/")) u += "/"
        // Validación mínima: debe tener host
        require(u.length > "http://".length + 1) { "URL inválida: $raw" }
        return u
    }
}

/** Helper para crear PreferencesManager desde Context (usado por ServiceLocator). */
fun Context.preferencesManager(): PreferencesManager = PreferencesManager(dataStore)
