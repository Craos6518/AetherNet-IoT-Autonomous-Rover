package com.aethernet.aethercontrol.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.domain.model.DashboardUiState
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel — MOV-01 5.2 (RF-1.1, RNF-3.1).
 * Solo StateFlow, no LiveData. init { refreshHealth() } sin TODO MQTT (docs/cierre-mov01.md:22).
 */
class DashboardViewModel(
    private val repo: AetherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshHealth()
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
}
