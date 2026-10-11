package com.aethernet.aethercontrol.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Espejo backend/app/schemas.py:26 AccessEventCreate y :33 AccessEventOut (HU-01, RF-2.2).
 */
@Serializable
data class AccessEventCreate(
    val user_id: String,
    val pin_hash: String,
    val success: Boolean,
    val source: String = "keypad"
)

@Serializable
data class AccessEventOut(
    val id: String, // UUID como String para evitar serializador UUID custom en este sprint
    val user_id: String,
    val pin_hash: String,
    val success: Boolean,
    val timestamp: String,
    val source: String
)
