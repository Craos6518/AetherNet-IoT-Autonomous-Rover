package com.aethernet.aethercontrol.util

import java.io.IOException
import retrofit2.HttpException

/**
 * Wrapper de resultado — MOV-01 3.3.
 * Usado por AetherRepository para propagar Loading/Success/Error sin exponer excepciones al UI.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val msg: String, val cause: Throwable? = null) : Result<Nothing>
    object Loading : Result<Nothing>
}

/**
 * Ejecuta [block] y mapea excepciones conocidas a Result.Error.
 * Captura IOException (red) y HttpException (HTTP 4xx/5xx).
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: IOException) {
        Result.Error(msg = e.message ?: "Network error", cause = e)
    } catch (e: HttpException) {
        Result.Error(msg = "HTTP ${e.code()}: ${e.message()}", cause = e)
    } catch (e: Exception) {
        Result.Error(msg = e.message ?: "Unknown error", cause = e)
    }
}
