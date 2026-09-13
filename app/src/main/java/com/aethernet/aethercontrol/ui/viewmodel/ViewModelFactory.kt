package com.aethernet.aethercontrol.ui.viewmodel

// =============================================================================
// ViewModelFactory.kt — Factory Manual MVVM | 6º Semestre UTP | MOV-01 1.3
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (context/provider), 2 años Python (DI), 1 año C
// Analogía React: este class es como `const createViewModel = (repo) => { if (type === 'dashboard') return new DashboardViewModel(repo) }`
// en JS — factory que crea ViewModels con repo inyectado (DI manual, sin Hilt/Koin).
// Analogía Python: como `def create_viewmodel(repo, cls): if cls is DashboardViewModel: return DashboardViewModel(repo)` en Python.
// FOSS: ViewModelProvider.Factory (AndroidX, Apache 2.0) — RNF-3.1, sin Hilt/Koin propietario.
// Origen: MOV-01 1.3 (RNF-3.1 sin Hilt/Koin) — Uso en MainActivity: viewModel(factory = DashboardViewModelFactory(ServiceLocator.repository))
// (como `const vm = useViewModel(factory)` en React). MOV-04 Plan B añade PinViewModel.
// Pattern: Factory manual — ServiceLocator es el DI container (como createContext en React, pero sin librería).
// =============================================================================

import androidx.lifecycle.ViewModel // ViewModel — como `useState` + `useEffect` en React
import androidx.lifecycle.ViewModelProvider // Factory — como `createContext` en React
import com.aethernet.aethercontrol.data.repository.AetherRepository // repo — como apiService en React props

/**
 * Factory manual — MOV-01 1.3 (RNF-3.1 sin Hilt/Koin FOSS).
 * Inyecta AetherRepository en ViewModels sin librería DI (Hilt/Koin son FOSS pero evitados por simplicidad evaluable).
 * Uso en MainActivity: viewModel(factory = DashboardViewModelFactory(ServiceLocator.repository))
 * MOV-04 Plan B añade PinViewModel al mismo factory (reusa repo).
 * Si vienes de React: es como `function createViewModel(repo, type) { switch(type) { case 'dashboard': return new DashboardViewModel(repo) } }`
 */
class DashboardViewModelFactory(
    private val repo: AetherRepository // repo inyectado — como `const repo = useRepo()` en React, pero via constructor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // cast `as T` es seguro porque verificamos isAssignableFrom — como `as unknown as T` en TS
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Crea DashboardViewModel — usado en MainActivity:22 `val vm: DashboardViewModel = viewModel(factory = vmFactory)` (como `useViewModel('dashboard')` en React)
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repo) as T
        }
        // Crea PinViewModel — usado en MainActivity:24 `val pinVm: PinViewModel = viewModel(factory = vmFactory)` (MOV-04 Plan B)
        if (modelClass.isAssignableFrom(PinViewModel::class.java)) {
            return PinViewModel(repo) as T
        }
        // Crea JoystickViewModel — usado en MainActivity:26 `val joyVm: JoystickViewModel = viewModel(factory = vmFactory)` (MOV-05 RF-1.2)
        if (modelClass.isAssignableFrom(JoystickViewModel::class.java)) {
            return JoystickViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}") // error si piden otro ViewModel no registrado (como `throw new Error('Unknown type')` en JS)
    }
}
