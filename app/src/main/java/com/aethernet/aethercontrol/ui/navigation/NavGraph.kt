package com.aethernet.aethercontrol.ui.navigation

// =============================================================================
// NavGraph.kt — Navegación Compose con barra inferior | 6º Semestre UTP
// Autor: Andres Felipe Martinez Henao
// Barra inferior: Inicio, Rover, Acceso, Historial, Ajustes.
// Rutas: inicio, rover, acceso, historial, ajustes (+ bluetooth fuera de la barra).
// FOSS: Navigation Compose + Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// =============================================================================

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aethernet.aethercontrol.ui.screens.BluetoothScreen
import com.aethernet.aethercontrol.ui.screens.DashboardScreen
import com.aethernet.aethercontrol.ui.screens.HistorialScreen
import com.aethernet.aethercontrol.ui.screens.JoystickScreen
import com.aethernet.aethercontrol.ui.screens.PinScreen
import com.aethernet.aethercontrol.ui.screens.SettingsScreen
import com.aethernet.aethercontrol.ui.viewmodel.BluetoothViewModel
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel
import com.aethernet.aethercontrol.ui.viewmodel.HistorialViewModel
import com.aethernet.aethercontrol.ui.viewmodel.JoystickViewModel
import com.aethernet.aethercontrol.ui.viewmodel.PinViewModel
import com.aethernet.aethercontrol.ui.viewmodel.SettingsViewModel

/**
 * Destinos del NavGraph inferior.
 * Bluetooth queda como ruta secundaria (fuera de la barra).
 */
sealed class Dest(val route: String, val label: String, val emoji: String) {
    object Inicio : Dest("inicio", "Inicio", "🏠")
    object Rover : Dest("rover", "Rover", "🤖")
    object Acceso : Dest("acceso", "Acceso", "🔒")
    object Historial : Dest("historial", "Historial", "📊")
    object Ajustes : Dest("ajustes", "Ajustes", "⚙️")
    object Bluetooth : Dest("bluetooth", "Bluetooth", "📶")
}

private val bottomTabs = listOf(
    Dest.Inicio,
    Dest.Rover,
    Dest.Acceso,
    Dest.Historial,
    Dest.Ajustes
)

@Composable
fun NavGraph(
    viewModel: DashboardViewModel,
    pinViewModel: PinViewModel,
    joystickViewModel: JoystickViewModel,
    bluetoothViewModel: BluetoothViewModel,
    settingsViewModel: SettingsViewModel,
    historialViewModel: HistorialViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Text(tab.emoji) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Inicio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Dest.Inicio.route) {
                DashboardScreen(
                    vm = viewModel,
                    onOpenPin = { navController.navigate(Dest.Acceso.route) },
                    onOpenJoystick = { navController.navigate(Dest.Rover.route) },
                    onOpenSettings = { navController.navigate(Dest.Ajustes.route) },
                    onOpenHistorial = { navController.navigate(Dest.Historial.route) },
                    onOpenBluetooth = { navController.navigate(Dest.Bluetooth.route) }
                )
            }
            composable(Dest.Acceso.route) {
                PinScreen(
                    vm = pinViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenBluetooth = { navController.navigate(Dest.Bluetooth.route) }
                )
            }
            composable(Dest.Rover.route) {
                JoystickScreen(vm = joystickViewModel, onBack = { navController.popBackStack() })
            }
            composable(Dest.Historial.route) {
                HistorialScreen(vm = historialViewModel, onBack = { navController.popBackStack() })
            }
            composable(Dest.Ajustes.route) {
                SettingsScreen(
                    vm = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenBluetooth = { navController.navigate(Dest.Bluetooth.route) }
                )
            }
            composable(Dest.Bluetooth.route) {
                BluetoothScreen(
                    vm = bluetoothViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenPin = { navController.navigate(Dest.Acceso.route) }
                )
            }
        }
    }
}
