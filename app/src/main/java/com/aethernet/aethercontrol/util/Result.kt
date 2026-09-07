package com.aethernet.aethercontrol.util

// =============================================================================
// Result.kt — Wrapper de resultado | 6º Semestre UTP | MOV-01 3.3
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años Python (Result pattern), 2 años JS/React (Promise ok/err),
//              1 año C (códigos de error), 1 año PostgreSQL (transacciones)
// Analogía React: este archivo es como `type Result<T> = {ok:true, data:T} | {ok:false, error:string}`
// en TypeScript — aquí con sealed interface Success/Error/Loading para StateFlow.
// Analogía Python: como `Result` de Rust o `Either` en stats/ema_filter.py safeCall — captura excepciones.
// Analogía C: en C retornas `int` 0=ok, -1=error + `errno`; aquí es type-safe sin códigos mágicos.
// FOSS: clase pura Kotlin, sin dependencia propietaria (RNF-3.1).
// Uso: AetherRepository retorna Result<HealthResponse> para que ViewModel haga when(Success/Error/Loading)
// sin exponer excepciones al UI (como useState con loading/error en React).
// =============================================================================

import java.io.IOException // error de red — como NetworkError en fetch() de JS
import retrofit2.HttpException // error HTTP 4xx/5xx — como response.ok === false en fetch()

/**
 * Wrapper de resultado — MOV-01 3.3.
 * Usado por AetherRepository para propagar Loading/Success/Error sin exponer excepciones al UI.
 * ViewModel hace `when(result) { is Success -> data, is Error -> msg, is Loading -> spinner }`
 * Como `useState<{data, error, loading}>` en React pero type-safe.
 */
sealed interface Result<out T> {
    // Success con data — como {ok: true, data: T} en TS
    data class Success<T>(val data: T) : Result<T>
    // Error con mensaje + causa opcional — como {ok: false, error: string} + stack
    data class Error(val msg: String, val cause: Throwable? = null) : Result<Nothing>
    // Loading — como isLoading true en React Query
    object Loading : Result<Nothing>
}

/**
 * Ejecuta [block] y mapea excepciones conocidas a Result.Error.
 * Captura IOException (red caída, como fetch() network error) y HttpException (HTTP 4xx/5xx).
 * Envuelve el éxito en Success, errores en Error con msg para UI.
 * Si vienes de React: es como `try { const data = await fetch(); return {ok:true,data} } catch(e) { return {ok:false,error:e.message} }`
 * Si vienes de C: es como `if (ret < 0) return Error; else return Success`.
 * Uso: `val result = safeCall { api.getHealth() }` en AetherRepositoryImpl:33 — evita try/catch repetido.
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block()) // éxito — bloque ejecutado sin excepción, como `resolve(data)` en Promise
    } catch (e: IOException) {
        // Error de red — WiFi caído, backend no alcanzable (como NetworkError en fetch)
        // Mensaje para UI: "Network error" o msg de excepción (como e.message en JS)
        Result.Error(msg = e.message ?: "Network error", cause = e)
    } catch (e: HttpException) {
        // Error HTTP — 404, 500, 422 — como `!response.ok` en fetch, pero con código
        Result.Error(msg = "HTTP ${e.code()}: ${e.message()}", cause = e)
    } catch (e: Exception) {
        // Cualquier otro — como catch genérico en JS, para no crashear (ver Result.Error en ViewModel)
        Result.Error(msg = e.message ?: "Unknown error", cause = e)
    }
}
