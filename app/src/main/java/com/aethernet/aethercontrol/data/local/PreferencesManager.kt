package com.aethernet.aethercontrol.data.local

// =============================================================================
// PreferencesManager.kt — DataStore Preferences | 6º Semestre UTP | MOV-01 3.1
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años Python (dotenv), 2 años JS/React (localStorage), 1 año PostgreSQL,
//              2 años electrónica (secrets.h)
// Analogía React: este archivo es `localStorage` en web — guarda `apiBaseUrl` como
// `localStorage.setItem('apiBaseUrl', 'http://192.168.1.50:8000/')` pero con Flow reactivo.
// Analogía Python: como `python-dotenv` + `DATABASE_URL` en backend/app/config.py — aquí con DataStore.
// Analogía C/Arduino: como `EEPROM` del MEGA para guardar `VALID_PIN` — aquí guarda URL base.
// Analogía PostgreSQL: no es tabla, es key-value local (como SharedPreferences, pero con DataStore async).
// FOSS: DataStore Preferences (AndroidX, Apache 2.0) — RNF-3.1, reemplaza SharedPreferences (menos bugs).
// Origen: MOV-01 3.1 (RF-1.1) — BaseUrl extraída aquí, no hardcodeada en ViewModel (cf. 2.3.2).
// Usado por: ServiceLocator.init() carga runBlocking, ServiceLocator.updateBaseUrl() persiste.
// =============================================================================

import android.content.Context // para dataStore extension
import androidx.datastore.core.DataStore // DataStore — como localForage en JS pero con Flow
import androidx.datastore.preferences.core.Preferences // Preferences — key-value (como Map<string, string>)
import androidx.datastore.preferences.core.edit // edit { prefs[key] = value } — como localStorage.setItem
import androidx.datastore.preferences.core.stringPreferencesKey // key tipada — como z.string() key en TS
import androidx.datastore.preferences.preferencesDataStore // delegate dataStore — como createStore en Zustand
import kotlinx.coroutines.flow.Flow // Flow — como Observable en RxJS / StateFlow en Compose
import kotlinx.coroutines.flow.first // first() — suspend que toma primer valor (como await once en JS)
import kotlinx.coroutines.flow.map // map — transforma Flow (como .map en JS array)

// Extension Context.dataStore — singleton DataStore "aethernet_prefs" (como `const store = createStore()` en Zustand)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aethernet_prefs")

/**
 * PreferencesManager — MOV-01 3.1 (RF-1.1).
 * Almacena configuración local via DataStore (async, type-safe, sin blocking I/O como SharedPreferences).
 * Sin Room aún — solo Preferences (key-value). BaseUrl extraída aquí, no hardcodeada en ViewModel (cf. 2.3.2).
 * Si vienes de React: es `localStorage` pero con `Flow` (reactivo) en vez de `getItem` sync.
 * Si vienes de Python: es `dotenv` pero persistente (guarda, no solo lee).
 */
class PreferencesManager(private val dataStore: DataStore<Preferences>) {

    object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url") // key para URL base — como localStorage key
        val USER_ID = stringPreferencesKey("user_id") // key para user_id — futuro multiusuario MOV-04
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/" // emulador Android — 10.0.2.2 mapea a localhost del host (como 127.0.0.1 en Docker, pero para emulador)
        // En físico: 192.168.1.x:8000 (IP del PC con docker compose up) — ver DashboardScreen Tip
    }

    // Flow<String> — reactivo, emite cada vez que cambia (como useState + useEffect en React)
    // Collect en Compose: `val url by preferencesManager.apiBaseUrl.collectAsStateWithLifecycle()`
    val apiBaseUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.API_BASE_URL] ?: DEFAULT_BASE_URL // si no hay guardado, default emulador
    }

    /**
     * Guarda nueva baseUrl normalizada.
     * - trim, añade http:// si falta esquema (como `https://` en fetch)
     * - garantiza "/" final (Retrofit exige terminar en "/")
     * - lanza IllegalArgumentException si queda vacía (como Zod error)
     * Uso: `ServiceLocator.updateBaseUrl(url)` para que además recree Retrofit (ver ServiceLocator:138).
     * Directo: `preferencesManager.saveBaseUrl("192.168.1.50:8000")` -> `http://192.168.1.50:8000/`
     */
    suspend fun saveBaseUrl(url: String) {
        val normalized = normalizeUrl(url) // valida y normaliza (como Zod parse)
        dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = normalized // guarda — como localStorage.setItem con Flow update (reactivo)
        }
    }

    /** Lectura suspendida útil para ServiceLocator.loadSavedBaseUrl() en Application.onCreate (ver AetherControlApp.kt:16). */
    suspend fun getBaseUrlOnce(): String = apiBaseUrl.first() // toma primer valor Flow (suspend, como await)

    val userId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.USER_ID] // nullable — si no hay user, null (como localStorage.getItem que puede ser null)
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId // guarda user_id — futuro MOV-04 multiusuario
        }
    }

    // Normaliza URL — como `new URL(url).toString()` en JS pero simple (añade http:// y /)
    private fun normalizeUrl(raw: String): String {
        var u = raw.trim() // trim espacios (como String.trim en JS)
        require(u.isNotBlank()) { "URL vacía" } // valida no vacía — como Zod min(1)
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u" // añade esquema si falta — como `https://` default en fetch
        }
        if (!u.endsWith("/")) u += "/" // Retrofit exige "/" final (ver ServiceLocator:89 baseUrl)
        // Validación mínima: debe tener host (no solo http://)
        require(u.length > "http://".length + 1) { "URL inválida: $raw" }
        return u
    }
}

/** Helper para crear PreferencesManager desde Context (usado por ServiceLocator:106). */
fun Context.preferencesManager(): PreferencesManager = PreferencesManager(dataStore) // extension — como `createStore()` en Zustand
