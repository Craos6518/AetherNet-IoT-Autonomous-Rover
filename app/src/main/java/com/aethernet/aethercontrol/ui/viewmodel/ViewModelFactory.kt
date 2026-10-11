package com.aethernet.aethercontrol.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aethernet.aethercontrol.data.repository.AetherRepository

/**
 * Factory manual — MOV-01 1.3 (RNF-3.1 sin Hilt/Koin).
 * Uso en MainActivity: viewModel(factory = DashboardViewModelFactory(ServiceLocator.repository))
 */
class DashboardViewModelFactory(
    private val repo: AetherRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
