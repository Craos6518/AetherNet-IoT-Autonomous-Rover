package com.aethernet.aethercontrol.ui.viewmodel

// =============================================================================
// HistorialViewModel.kt — ViewModel Historial | 6º Semestre UTP | MOV-XX
// Autor: Andres Felipe Martinez Henao
// FOSS: ViewModel + Coroutines (Apache 2.0) — RNF-3.1.
// =============================================================================

import androidx.lifecycle.ViewModel
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.repository.AetherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * MOV-XX — ViewModel Historial Telemetría.
 * Sensores: TCRT5000 (3x borde), HC-SR04 (ultrasonido), FC-51 (presencia)
 * Filtro EMA, Reducción de ruido.
 */
class HistorialViewModel(
    private val repo: AetherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    // Expone mqtt state para badge en header
    val mqttState: StateFlow<com.aethernet.aethercontrol.data.mqtt.MqttConnectionState> get() = repo.mqttConnectionState

    // TODO: Implementar carga de histórico con filtros EMA, export CSV, etc.
}

/**
 * MOV-XX — Estado UI Historial.
 */
data class HistorialUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)