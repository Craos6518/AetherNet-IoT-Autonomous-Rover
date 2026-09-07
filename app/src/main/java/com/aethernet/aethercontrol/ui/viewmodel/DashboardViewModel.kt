package com.aethernet.aethercontrol.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.domain.model.DashboardUiState
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel — MOV-01 5.2 + MOV-02 (RF-1.1, RNF-3.1).
 * Solo StateFlow, no LiveData. init { refreshHealth(); refreshLedState(); startLedPolling() }
 * Polling 5s alineado a STATUS_INTERVAL_MS=5000 config.h:55 — cancelable en onCleared().
 * MOV-03 migrará a MQTT Mosquitto suscribiéndose en repo sin romper esta UI.
 */
class DashboardViewModel(
    private val repo: AetherRepository,
    private val autoPollLed: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var ledPollingJob: Job? = null

    init {
        refreshHealth()
        refreshLedState()
        if (autoPollLed) startLedPolling()
    }

    fun refreshHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = repo.getHealth()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = true,
                            health = r.data,
                            lastSync = System.currentTimeMillis(),
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = false,
                            error = r.msg
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /** MOV-02: refresh LED solo lectura — deriva de /api/access-events + /api/security-events. */
    fun refreshLedState() {
        viewModelScope.launch {
            _uiState.update { it.copy(ledState = it.ledState.copy(isLoading = true, error = null)) }
            when (val r = repo.getLedState()) {
                is Result.Success -> {
                    _uiState.update { it.copy(ledState = r.data.copy(isLoading = false, error = null)) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(ledState = it.ledState.copy(isLoading = false, error = r.msg)) }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(ledState = it.ledState.copy(isLoading = true)) }
                }
            }
        }
    }

    /** Polling 5s — STATUS_INTERVAL_MS=5000 config.h:55 evita spam; cancelable en onCleared(). */
    fun startLedPolling(intervalMs: Long = 5000L) {
        if (ledPollingJob?.isActive == true) return
        ledPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalMs)
                refreshLedState()
            }
        }
    }

    fun stopLedPolling() {
        ledPollingJob?.cancel()
        ledPollingJob = null
    }

    val isPolling: Boolean get() = ledPollingJob?.isActive == true

    override fun onCleared() {
        stopLedPolling()
        super.onCleared()
    }
}
