package com.aethernet.aethercontrol.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Espejo de backend/app/schemas.py:17 HealthResponse (RF-1.1).
 */
@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
    val version: String
)

@Serializable
data class RootResponse(
    val name: String,
    val version: String,
    val docs: String,
    val health: String
)
