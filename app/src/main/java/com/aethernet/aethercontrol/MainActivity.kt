package com.aethernet.aethercontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aethernet.aethercontrol.core.di.ServiceLocator
import com.aethernet.aethercontrol.ui.navigation.NavGraph
import com.aethernet.aethercontrol.ui.theme.AetherControlTheme
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AetherControlTheme {
                val vmFactory = DashboardViewModelFactory(ServiceLocator.repository)
                val vm: com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel =
                    viewModel(factory = vmFactory)
                NavGraph(viewModel = vm)
            }
        }
    }
}
