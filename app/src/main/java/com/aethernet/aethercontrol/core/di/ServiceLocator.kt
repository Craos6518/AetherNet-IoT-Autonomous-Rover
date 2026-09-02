package com.aethernet.aethercontrol.core.di

import android.content.Context
import com.aethernet.aethercontrol.BuildConfig
import com.aethernet.aethercontrol.data.local.PreferencesManager
import com.aethernet.aethercontrol.data.local.preferencesManager
import com.aethernet.aethercontrol.data.remote.ApiService
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.data.repository.AetherRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * DI manual — MOV-01 1.2 (RNF-3.1 FOSS, sin Hilt/Koin).
 * Singletons via by lazy. Inicializado desde AetherControlApp.onCreate().
 *
 * BaseUrl dinámica: ServiceLocator.updateBaseUrl(url) normaliza, persiste
 * en PreferencesManager.saveBaseUrl(url) y recrea Retrofit/ApiService/Repository.
 * La ViewModel existente conserva el repo antiguo; para aplicar la nueva URL
 * recrea la Activity/ViewModel (o reinicia la app).
 *
 * Documentado como manual para evaluación RNF-3.1 (ver README si existe).
 */
object ServiceLocator {

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        // Carga síncrona de la URL guardada (bloqueo breve, DataStore en memoria tras primer acceso).
        // Evita que el primer Retrofit use DEFAULT si el usuario ya guardó otra IP (ej. 192.168.x.x).
        try {
            runBlocking {
                val saved = preferencesManager.apiBaseUrl.first()
                synchronized(this@ServiceLocator) {
                    currentBaseUrl = saved
                    // Invalida cachés para que siguiente acceso use la guardada
                    cachedRetrofit = null
                    cachedApi = null
                    cachedRepo = null
                }
            }
        } catch (_: Exception) {
            // Si DataStore aún no está listo, se usa DEFAULT y se actualizará en próximo updateBaseUrl
        }
    }

    private fun requireContext(): Context =
        appContext ?: error("ServiceLocator not initialized. Did you register AetherControlApp in AndroidManifest.xml?")

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logger)
        }
        builder.build()
    }

    // BaseUrl extraída a constante computable (no hardcodeada en ViewModel).
    // Por defecto emulador 10.0.2.2; sobreescribible vía PreferencesManager.apiBaseUrl
    private const val DEFAULT_BASE_URL = PreferencesManager.DEFAULT_BASE_URL

    @Volatile
    private var currentBaseUrl: String = DEFAULT_BASE_URL

    private var cachedRetrofit: Retrofit? = null
    private var cachedApi: ApiService? = null
    private var cachedRepo: AetherRepository? = null

    private fun buildRetrofit(url: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val retrofit: Retrofit
        get() = synchronized(this) {
            cachedRetrofit ?: buildRetrofit(currentBaseUrl).also { cachedRetrofit = it }
        }

    val apiService: ApiService
        get() = synchronized(this) {
            cachedApi ?: retrofit.create(ApiService::class.java).also { cachedApi = it }
        }

    val preferencesManager: PreferencesManager by lazy {
        requireContext().preferencesManager()
    }

    val repository: AetherRepository
        get() = synchronized(this) {
            cachedRepo ?: AetherRepositoryImpl(apiService).also { cachedRepo = it }
        }

    /** URL actual en uso por Retrofit (útil para debug/UI). */
    fun getCurrentBaseUrl(): String = synchronized(this) { currentBaseUrl }

    /**
     * Actualiza la dirección del backend.
     * - Normaliza (añade http:// y "/" si faltan)
     * - Persiste en DataStore via PreferencesManager.saveBaseUrl
     * - Recrea Retrofit/ApiService/Repository atómicamente
     * @return url normalizada
     * Ejemplo: `ServiceLocator.updateBaseUrl("192.168.1.50:8000")` -> `http://192.168.1.50:8000/`
     *         `ServiceLocator.updateBaseUrl("http://10.0.2.2:8000/")`
     */
    suspend fun updateBaseUrl(url: String): String {
        val normalized = normalizeUrl(url)
        // Persiste primero
        preferencesManager.saveBaseUrl(normalized)
        // Recrea singletons
        synchronized(this) {
            currentBaseUrl = normalized
            cachedRetrofit = buildRetrofit(normalized)
            cachedApi = cachedRetrofit!!.create(ApiService::class.java)
            cachedRepo = AetherRepositoryImpl(cachedApi!!)
        }
        return normalized
    }

    /**
     * Variante no-suspend para uso rápido desde UI sin coroutine scope (usa runBlocking breve).
     * Preferible `updateBaseUrl` suspend si ya estás en coroutine.
     */
    fun updateBaseUrlBlocking(url: String): String = runBlocking { updateBaseUrl(url) }

    /** Para tests: permite sustituir el repositorio. */
    fun setRepositoryForTest(repo: AetherRepository) {
        synchronized(this) { cachedRepo = repo }
    }

    /** Resetea a DEFAULT (útil en tests o "Restaurar" en Ajustes). */
    suspend fun resetBaseUrl(): String = updateBaseUrl(DEFAULT_BASE_URL)

    private fun normalizeUrl(raw: String): String {
        var u = raw.trim()
        require(u.isNotBlank()) { "URL vacía" }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://$u"
        }
        if (!u.endsWith("/")) u += "/"
        return u
    }
}
