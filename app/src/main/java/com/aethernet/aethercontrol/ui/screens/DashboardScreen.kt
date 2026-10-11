package com.aethernet.aethercontrol.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.core.di.ServiceLocator
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

/**
 * DashboardScreen — MOV-01 5.4 (RF-1.1).
 * Maneja isConnected=false con banner Desconectado — base para MOV-09 reconexión.
 */
@Composable
fun DashboardScreen(vm: DashboardViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Muestra y edita la baseUrl persistida (PreferencesManager.saveBaseUrl)
    val savedUrl by ServiceLocator.preferencesManager.apiBaseUrl.collectAsStateWithLifecycle(
        initialValue = ServiceLocator.getCurrentBaseUrl()
    )
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var saveMsg by remember { mutableStateOf<String?>(null) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner desconectado (MOV-09 base)
            if (!state.isConnected && !state.isLoading) {
                Text(
                    text = "Desconectado",
                    color = Color.Red,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = state.health?.status ?: if (state.isLoading) "..." else "Sin datos",
                style = MaterialTheme.typography.headlineSmall
            )

            state.health?.let { h ->
                Text(text = "DB: ${h.database} | v${h.version}", style = MaterialTheme.typography.bodyMedium)
            }

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            state.error?.let { err ->
                Text(text = err, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = { vm.refreshHealth() }) {
                Text("Reintentar")
            }

            state.lastSync?.let { ts ->
                Text(text = "Última sync: $ts", style = MaterialTheme.typography.bodySmall)
            }

            // --- Ajuste de dirección backend (MOV-01 3.1) ---
            Text(
                text = "Backend URL",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Actual: $savedUrl",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; saveMsg = null },
                label = { Text("http://10.0.2.2:8000/ o 192.168.1.x:8000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        try {
                            val normalized = ServiceLocator.updateBaseUrl(urlInput)
                            saveMsg = "Guardado: $normalized — reintentando..."
                            vm.refreshHealth()
                        } catch (e: Exception) {
                            saveMsg = "Error: ${e.message}"
                        }
                    }
                }) {
                    Text("Guardar URL")
                }
                Button(onClick = {
                    scope.launch {
                        val def = ServiceLocator.resetBaseUrl()
                        urlInput = def
                        saveMsg = "Restaurado: $def"
                        vm.refreshHealth()
                    }
                }) {
                    Text("Restaurar")
                }
            }
            saveMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
            Text(
                text = "Tip: emulador=10.0.2.2:8000, físico=IP del PC (ipconfig/ifconfig) + :8000 . Asegúrate docker compose up.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
