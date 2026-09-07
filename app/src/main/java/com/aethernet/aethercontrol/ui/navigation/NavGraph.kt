package com.aethernet.aethercontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aethernet.aethercontrol.ui.screens.DashboardScreen
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel

/**
 * NavGraph — MOV-01 5.3 (RF-1.1).
 */
sealed class Dest(val route: String) {
    object Dashboard : Dest("dashboard")
}

@Composable
fun NavGraph(
    viewModel: DashboardViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Dest.Dashboard.route
    ) {
        composable(Dest.Dashboard.route) {
            DashboardScreen(vm = viewModel)
        }
    }
}
