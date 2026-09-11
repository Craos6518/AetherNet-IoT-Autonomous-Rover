package com.aethernet.aethercontrol.core.di

// =============================================================================
// ServiceLocator.kt — DI Manual Singleton | 6º Semestre UTP | MOV-01 1.2, MOV-03, RNF-3.1
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (context, singleton), 2 años Python (DI), 1 año C (singleton),
//              1 año PostgreSQL (backend URL)
// Analogía React: este object es como `const ServiceLocator = { repository: createRepository(), updateBaseUrl(url) { ... } }`
// en JS con singleton pattern — DI manual sin Hilt/Koin (como `createContext` pero sin librería, solo object + lazy).
// Analogía Python: como `class ServiceLocator` con `@lru_cache` singleton en backend/app/config.py — aquí con `object` Kotlin (singleton nativo).
// FOSS: DI manual (sin Hilt/Koin, ambos FOSS pero evitados por simplicidad evaluable RNF-3.1) — como `secrets.h` manual en firmware.
// Origen: MOV-01 1.2 (RNF-3.1 FOSS, sin Hilt/Koin). BaseUrl dinámica: updateBaseUrl(url) normaliza, persiste en DataStore y recrea Retrofit.
// =============================================================================

import android.content.Context
import com.aethernet.aethercontrol.BuildConfig // BuildConfig.DEBUG — como `process.env.NODE_ENV !== 'production'` en React
import com.aethernet.aethercontrol.data.local.PreferencesManager
import com.aethernet.aethercontrol.data.local.preferencesManager // extension Context.preferencesManager() — como `createStore()` en Zustand
import com.aethernet.aethercontrol.data.mqtt.MqttManager
import com.aethernet.aethercontrol.data.remote.ApiService
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.data.repository.AetherRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory // converter — como `axios` con JSON parser
import kotlinx.coroutines.flow.first // first() — como `await once` en JS (toma primer valor Flow)
import kotlinx.coroutines.runBlocking // runBlocking — como `await` en sync (solo en init, breve)
import kotlinx.serialization.json.Json // Json — como `JSON` en JS pero con config (ignoreUnknownKeys)
import okhttp3.MediaType.Companion.toMediaType // MediaType — como `Content-Type: application/json` en fetch
import okhttp3.OkHttpClient // OkHttpClient — como `fetch` client en JS (HTTP client)
import okhttp3.logging.HttpLoggingInterceptor // logger — como `morgan` en Express (log cada request)
import retrofit2.Retrofit // Retrofit — como `axios.create({baseURL})` en JS pero tipado con ApiService interface

/**
 * DI manual — MOV-01 1.2 (RNF-3.1 FOSS, sin Hilt/Koin).
 * Singletons via by lazy (como `useMemo` en React) + synchronized (como `Mutex` en C).
 * Inicializado desde AetherControlApp.onCreate() (ver AetherControlApp.kt:16).
 *
 * BaseUrl dinámica: ServiceLocator.updateBaseUrl(url) normaliza (añade http:// y /), persiste
 * en PreferencesManager.saveBaseUrl(url) (DataStore) y recrea Retrofit/ApiService/Repository.
 * La ViewModel existente conserva el repo antiguo; para aplicar la nueva URL recrea la Activity/ViewModel (o reinicia app).
 * Como `localStorage.setItem('apiBaseUrl', url)` + `axios.defaults.baseURL = url` en React, pero con Retrofit.
 *
 * Documentado como manual para evaluación RNF-3.1 (ver README si existe).
 */
object ServiceLocator { // object — singleton Kotlin (como `const ServiceLocator = {}` en JS, nativo)

    private var appContext: Context? = null // Context — como `window` en JS pero para Android (necesario para DataStore)

    fun init(context: Context) {
        appContext = context.applicationContext // guarda applicationContext — como `window` global en JS
        // Carga síncrona de la URL guardada (runBlocking breve, DataStore en memoria tras primer acceso) — como `localStorage.getItem('apiBaseUrl')` sync en JS
        // Evita que el primer Retrofit use DEFAULT 10.0.2.2 si el usuario ya guardó 192.168.x.x (físico) — ver PreferencesManager:28 DEFAULT_BASE_URL
        try {
            runBlocking {
                val saved = preferencesManager.apiBaseUrl.first() // lee DataStore — como `await localStorage.getItem('apiBaseUrl')` en JS pero con Flow
                synchronized(this@ServiceLocator) {
                    currentBaseUrl = saved // guarda en memoria — como `currentBaseUrl = saved` en JS
                    // Invalida cachés para que siguiente acceso use la guardada — como `cachedRetrofit = null` en JS
                    cachedRetrofit = null
                    cachedApi = null
                    cachedRepo = null
                }
            }
        } catch (_: Exception) {
            // Si DataStore aún no está listo (primera vez), se usa DEFAULT y se actualizará en próximo updateBaseUrl
        }
    }

    private fun requireContext(): Context =
        appContext ?: error("ServiceLocator not initialized. Did you register AetherControlApp in AndroidManifest.xml?") // error si no init — como `if (!context) throw new Error('Not initialized')` en JS

    private val json: Json by lazy { // Json lenient — como `JSON.parse` con `ignoreUnknownKeys` (si backend añade campo, no crashea)
        Json {
            ignoreUnknownKeys = true // ignora campos desconocidos — como `Zod passthrough` en JS (tolerante a cambios backend)
            isLenient = true // leniente — como `JSON.parse` tolerante en JS
            coerceInputValues = true // coerces — como `z.coerce` en Zod
        }
    }

    val okHttpClient: OkHttpClient by lazy { // OkHttp — como `axios.create()` en JS con interceptors
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) { // solo en debug — como `if (process.env.NODE_ENV !== 'production')` en React
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // log BODY — como `morgan('dev')` en Express (log cada request/response)
            }
            builder.addInterceptor(logger) // añade logger — como `axios.interceptors.request.use(logger)` en JS
        }
        builder.build() // build — como `axios.create()` en JS
    }

    // BaseUrl extraída a constante computable (no hardcodeada en ViewModel) — como `const DEFAULT_BASE_URL = 'http://10.0.2.2:8000/'` en JS
    // Por defecto emulador 10.0.2.2; sobreescribible vía PreferencesManager.apiBaseUrl (DataStore) — ver DashboardScreen Tip
    private const val DEFAULT_BASE_URL = PreferencesManager.DEFAULT_BASE_URL // 10.0.2.2 emulador — como `DEFAULT_BASE_URL` en JS

    @Volatile // Volatile — como `volatile` en C (garantiza visibilidad entre threads, como `useRef` en React pero thread-safe)
    private var currentBaseUrl: String = DEFAULT_BASE_URL // URL actual — como `let currentBaseUrl = DEFAULT_BASE_URL` en JS

    private var cachedRetrofit: Retrofit? = null // cache Retrofit — como `let cachedRetrofit = null` en JS (singleton)
    private var cachedApi: ApiService? = null // cache ApiService
    private var cachedRepo: AetherRepository? = null
    private var cachedMqtt: MqttManager? = null

    private fun buildRetrofit(url: String): Retrofit =
        Retrofit.Builder() // Builder — como `axios.create({baseURL: url})` en JS pero con Retrofit
            .baseUrl(url) // baseUrl — como `baseURL` en axios
            .client(okHttpClient) // client — como `axios` instance en JS
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())) // converter — como `JSON.parse` auto en axios
            .build()

    val retrofit: Retrofit
        get() = synchronized(this) { // synchronized — como `Mutex` en C o `lock` en Python (thread-safe singleton)
            cachedRetrofit ?: buildRetrofit(currentBaseUrl).also { cachedRetrofit = it } // lazy — como `cachedRetrofit ??= buildRetrofit(url)` en JS
        }

    val apiService: ApiService
        get() = synchronized(this) {
            cachedApi ?: retrofit.create(ApiService::class.java).also { cachedApi = it } // crea ApiService — como `axios.create(ApiService)` en JS
        }

    val preferencesManager: PreferencesManager by lazy { // PreferencesManager — como `localStorage` en JS pero con DataStore
        requireContext().preferencesManager() // crea — como `new PreferencesManager(dataStore)` en JS
    }

    // MOV-03: MQTT Mosquitto tcp://host:1883 derivada de getCurrentBaseUrl() + fallback ws://host:9001 (mosquitto.conf:4)
    val mqttManager: MqttManager
        get() = synchronized(this) {
            cachedMqtt ?: MqttManager(requireContext()).also { cachedMqtt = it } // MqttManager — como `new MqttManager(context)` en JS
        }

    val repository: AetherRepository
        get() = synchronized(this) {
            cachedRepo ?: AetherRepositoryImpl(apiService, mqttManager).also { cachedRepo = it } // repo — como `new AetherRepositoryImpl(api, mqtt)` en JS
        }

    /** MOV-03: reconecta MQTT usando la URL HTTP actual (para DashboardScreen guardar URL). */
    suspend fun reconnectMqtt() {
        mqttManager.connect(getCurrentBaseUrl()) // conecta — como `mqtt.connect(url)` en MQTT.js
    }

    /** URL actual en uso por Retrofit (útil para debug/UI) — como `getCurrentBaseUrl()` en JS. */
    fun getCurrentBaseUrl(): String = synchronized(this) { currentBaseUrl }

    /**
     * Actualiza la dirección del backend.
     * - Normaliza (añade http:// y "/" si faltan) — como `new URL(url).toString()` en JS pero simple
     * - Persiste en DataStore via PreferencesManager.saveBaseUrl (como localStorage.setItem)
     * - Recrea Retrofit/ApiService/Repository atómicamente (synchronized) — como `axios.defaults.baseURL = url` en JS pero con Retrofit
     * @return url normalizada — como `return normalizedUrl` en JS
     * Ejemplo: `ServiceLocator.updateBaseUrl("192.168.1.50:8000")` -> `http://192.168.1.50:8000/`
     *         `ServiceLocator.updateBaseUrl("http://10.0.2.2:8000/")`
     */
    suspend fun updateBaseUrl(url: String): String {
        val normalized = normalizeUrl(url) // normaliza — como `normalizeUrl(url)` en JS (añade http:// y /)
        // Persiste primero — como `localStorage.setItem('apiBaseUrl', normalized)` en JS (DataStore)
        preferencesManager.saveBaseUrl(normalized)
        // Recrea singletons atómicamente (synchronized) — como `cachedRetrofit = new Retrofit(normalized)` en JS pero thread-safe
        synchronized(this) {
            currentBaseUrl = normalized
            cachedRetrofit = buildRetrofit(normalized)
            cachedApi = cachedRetrofit!!.create(ApiService::class.java)
            // reutiliza mqttManager existente para reconexión en próximo reconnectMqtt() — como `mqttManager.reconnect()` en JS
            val mqtt = cachedMqtt ?: MqttManager(requireContext()).also { cachedMqtt = it }
            cachedRepo = AetherRepositoryImpl(cachedApi!!, mqtt)
        }
        return normalized
    }

    /**
     * Variante no-suspend para uso rápido desde UI sin coroutine scope (usa runBlocking breve).
     * Preferible `updateBaseUrl` suspend si ya estás en coroutine (como `await updateBaseUrl()` en JS).
     * Si vienes de React: es como `function updateBaseUrlBlocking(url) { return runBlocking { updateBaseUrl(url) } }` en JS con `await` sync.
     */
    fun updateBaseUrlBlocking(url: String): String = runBlocking { updateBaseUrl(url) }

    /** Para tests: permite sustituir el repositorio (como `jest.mock` en JS para tests). */
    fun setRepositoryForTest(repo: AetherRepository) {
        synchronized(this) { cachedRepo = repo }
    }

    /** Resetea a DEFAULT (útil en tests o "Restaurar" en Ajustes DashboardScreen) — como `localStorage.removeItem('apiBaseUrl')` en JS. */
    suspend fun resetBaseUrl(): String = updateBaseUrl(DEFAULT_BASE_URL)

    private fun normalizeUrl(raw: String): String {
        var u = raw.trim() // trim — como `String.trim()` en JS
        require(u.isNotBlank()) { "URL vacía" } // valida — como `if (!u) throw new Error('URL vacía')` en JS
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u" // añade http:// — como `if (!url.startsWith('http')) url = 'http://' + url` en JS
        }
        if (!u.endsWith("/")) u += "/" // Retrofit exige "/" final — como `if (!url.endsWith('/')) url += '/'` en JS
        return u
    }
}
