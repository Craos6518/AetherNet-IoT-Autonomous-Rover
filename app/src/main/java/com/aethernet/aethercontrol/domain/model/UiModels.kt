package com.aethernet.aethercontrol.domain.model

import com.aethernet.aethercontrol.data.remote.dto.HealthResponse

/**
 * UiState para Dashboard — MOV-01 5.1 (RF-1.1).
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val health: HealthResponse? = null,
    val error: String? = null,
    val lastSync: Long? = null
)
