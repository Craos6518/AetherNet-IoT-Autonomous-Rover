package com.aethernet.aethercontrol.ui.navigation

// =============================================================================
// NavGraph.kt — Navegación Compose | 6º Semestre UTP | MOV-01 5.3, MOV-04 Plan B
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años JS/React (React Router), 1 año C
// Analogía React: este archivo es como `App.jsx` con `BrowserRouter` + `Routes` + `Route path="dashboard"` en React Router —
// aquí con NavHost + composable(Dest.Dashboard.route) en Jetpack Navigation Compose.
// FOSS: Navigation Compose (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: MOV-01 5.3 (RF-1.1) + MOV-04 Plan B Dest.Pin (HU-01) — 2 pantallas: Dashboard y Pin.
// Pattern: sealed class Dest define rutas (como `enum Route { Dashboard = "/dashboard", Pin = "/pin" }` en TS).
// Uso: MainActivity:25 NavGraph(viewModel, pinViewModel) — NavHost con startDestination Dashboard.
// =============================================================================

import androidx.compose.runtime.Composable // @Composable — como `function Component()` en React
import androidx.navigation.NavHostController // NavHostController — como `useNavigate()` en React Router
import androidx.navigation.compose.NavHost // NavHost — como `<Routes>` en React Router
import androidx.navigation.compose.composable // composable — como `<Route path="dashboard" element={<DashboardScreen />} />` en React Router
import androidx.navigation.compose.rememberNavController // rememberNavController — como `useNavigate()` hook en React Router
import com.aethernet.aethercontrol.ui.screens.DashboardScreen
import com.aethernet.aethercontrol.ui.screens.PinScreen
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel
import com.aethernet.aethercontrol.ui.viewmodel.PinViewModel

/**
 * NavGraph — MOV-01 5.3 (RF-1.1) + MOV-04 Plan B Dest.Pin (HU-01).
 * Define rutas y host de navegación Compose. Start en Dashboard, Pin es `navigate("pin")` desde DashboardScreen "Abrir PIN cerrojo".
 */
sealed class Dest(val route: String) { // sealed — como `enum Route` en TS pero con type safety
    object Dashboard : Dest("dashboard") // ruta "dashboard" — como `path: "/dashboard"` en React Router
    object Pin : Dest("pin") // ruta "pin" — MOV-04 Plan B — como `path: "/pin"` para PinScreen dedicada (no card embebida)
}

@Composable
fun NavGraph(
    viewModel: DashboardViewModel, // Dashboard VM — como prop `dashboardViewModel` en React <NavGraph />
    pinViewModel: PinViewModel, // Pin VM — MOV-04 Plan B (PinScreen dedicada, no comparte Dashboard VM)
    navController: NavHostController = rememberNavController() // controller — como `const navigate = useNavigate()` en React Router
) {
    NavHost( // NavHost — como `<Routes>` en React Router, contenedor de rutas
        navController = navController, // controller — como `router` en React Router
        startDestination = Dest.Dashboard.route // inicio "dashboard" — como `initialRouteName="dashboard"` en React Navigation
    ) {
        composable(Dest.Dashboard.route) { // ruta dashboard — como `<Route path="dashboard" element={<DashboardScreen />}>` en React Router
            DashboardScreen(vm = viewModel, onOpenPin = { navController.navigate(Dest.Pin.route) }) // callback navega a pin — como `onClick={() => navigate('/pin')}` en React
        }
        composable(Dest.Pin.route) { // ruta pin — MOV-04 Plan B
            PinScreen(vm = pinViewModel, onBack = { navController.popBackStack() }) // popBackStack — como `navigate(-1)` en React Router (volver Dashboard)
        }
    }
}
