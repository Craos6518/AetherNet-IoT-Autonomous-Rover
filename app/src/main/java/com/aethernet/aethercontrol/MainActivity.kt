package com.aethernet.aethercontrol

// =============================================================================
// MainActivity.kt — Activity Principal | 6º Semestre UTP | RF-1.1
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (App.jsx, Router), 2 años HTML/CSS (edge-to-edge),
//              2 años electrónica (Dashboard)
// Analogía React: este class es como `function MainActivity() { return <AetherControlTheme><NavGraph /></AetherControlTheme> }`
// en React con JSX — Activity es el `index.html` + `App.jsx` de Android (punto de entrada UI).
// FOSS: ComponentActivity + Compose (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: RF-1.1 Dashboard tiempo real — MainActivity monta Theme, ServiceLocator.repository, ViewModels y NavGraph.
// Pattern: MVVM — Activity crea ViewModels (DashboardViewModel, PinViewModel) via ViewModelFactory y pasa a NavGraph (como props en React).
// =============================================================================

import android.os.Bundle // Bundle — como `props` en React pero para estado Android (onCreate savedInstanceState)
import androidx.activity.ComponentActivity // ComponentActivity — como `React.Component` en React (base para Compose)
import androidx.activity.compose.setContent // setContent — como `ReactDOM.render(<App />, document.getElementById('root'))` en React
import androidx.activity.enableEdgeToEdge // edge-to-edge — como `viewport-fit=cover` en CSS para pantallas con notch
import androidx.lifecycle.viewmodel.compose.viewModel // viewModel — como `useViewModel()` hook en React (crea/reusa ViewModel con Factory)
import com.aethernet.aethercontrol.core.di.ServiceLocator // ServiceLocator — DI manual (como `useContext(ServiceLocatorContext)` en React)
import com.aethernet.aethercontrol.ui.navigation.NavGraph // NavGraph — como `<Router><Routes>...</Routes></Router>` en React Router
import com.aethernet.aethercontrol.ui.theme.AetherControlTheme // Theme — como `<ThemeProvider theme={theme}>` en MUI React
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModelFactory // Factory — como `createViewModelFactory(repo)` en React
import com.aethernet.aethercontrol.ui.viewmodel.JoystickViewModel
import com.aethernet.aethercontrol.ui.viewmodel.PinViewModel

class MainActivity : ComponentActivity() { // ComponentActivity — Activity base para Compose (como `class App extends Component` en React)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // super — como `super(props)` en React class
        enableEdgeToEdge() // edge-to-edge — como `edgeToEdge: true` en CSS (app usa toda la pantalla, incluido notch)
        setContent { // setContent — como `ReactDOM.render` en React (monta Compose UI)
            AetherControlTheme { // Theme — como `<ThemeProvider>` en MUI React (envuelve toda la app con MaterialTheme)
                val repo = ServiceLocator.repository // repo — como `const repo = useRepo()` en React (singleton ServiceLocator)
                val vmFactory = DashboardViewModelFactory(repo) // factory — como `const factory = createFactory(repo)` en React (DI para ViewModels)
                val vm: com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel =
                    viewModel(factory = vmFactory) // DashboardVM — como `const vm = useViewModel(factory, 'dashboard')` en React (crea/reusa ViewModel)
                val pinVm: PinViewModel = viewModel(factory = vmFactory) // PinVM — MOV-04 Plan B, mismo factory reusa repo (como `useViewModel(factory, 'pin')`)
                val joyVm: JoystickViewModel = viewModel(factory = vmFactory) // JoystickVM — MOV-05 RF-1.2 joystick virtual Rover (tank-steering)
                NavGraph(viewModel = vm, pinViewModel = pinVm, joystickViewModel = joyVm) // NavGraph — como `<NavGraph ... />` en React Router
            }
        }
    }
}
