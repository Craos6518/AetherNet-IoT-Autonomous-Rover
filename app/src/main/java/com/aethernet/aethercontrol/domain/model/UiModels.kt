package com.aethernet.aethercontrol.domain.model

import com.aethernet.aethercontrol.data.remote.dto.HealthResponse

/**
 * UiState para Dashboard — MOV-01 5.1 (RF-1.1).
 * MOV-02 extiende con ledState: LedUiState (RF-1.1 LED local solo lectura, HU-01/HU-02).
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val health: HealthResponse? = null,
    val error: String? = null,
    val lastSync: Long? = null,
    val ledState: LedUiState = LedUiState()
)
