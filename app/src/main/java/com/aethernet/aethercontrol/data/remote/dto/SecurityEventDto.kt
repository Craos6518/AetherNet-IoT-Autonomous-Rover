package com.aethernet.aethercontrol.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Espejo backend/app/schemas.py:73 SecurityEventCreate y :79 SecurityEventOut (HU-02, RF-2.3).
 */
@Serializable
data class SecurityEventCreate(
    val event_type: String,
    val severity: String = "medium",
    val description: String? = null
)

@Serializable
data class SecurityEventOut(
    val id: String,
    val event_type: String,
    val severity: String,
    val description: String? = null,
    val timestamp: String,
    val acknowledged: Boolean,
    val acknowledged_at: String? = null,
    val acknowledged_by: String? = null
)
