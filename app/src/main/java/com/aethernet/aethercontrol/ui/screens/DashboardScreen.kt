package com.aethernet.aethercontrol.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.core.di.ServiceLocator
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.ui.components.LedStatusCard
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

/**
 * DashboardScreen — MOV-01 5.4 + MOV-02 (RF-1.1, HU-01/HU-02).
 * Maneja isConnected=false con banner Desconectado — base para MOV-09 reconexión.
 * MOV-02 añade Card LED [círculo 64dp + etiqueta + lastSync + error] solo lectura,
 * polling 5s en ViewModel, barra acciones [Reintentar] [Poll toggle].
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner desconectado (MOV-09 base) -> app/src/main/java/com/aethernet/aethercontrol/ui/screens/DashboardScreen.kt:55
            if (!state.isConnected && !state.isLoading) {
                Text(
                    text = "Desconectado",
                    color = Color.Red,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // MOV-03: Estado MQTT vivo <50ms prd.md:50 — tcp://host:1883 (mosquitto.conf:4) o ws://host:9001 fallback
            val mqttState = state.mqttState
            Text(
                text = when (mqttState) {
                    is MqttConnectionState.Connected -> "MQTT ● ${mqttState.broker}"
                    is MqttConnectionState.Connecting -> "MQTT ○ Conectando..."
                    is MqttConnectionState.Error -> "MQTT ✕ ${mqttState.msg}"
                    is MqttConnectionState.Disconnected -> "MQTT - Desconectado"
                },
                color = if (mqttState is MqttConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            // MOV-03: Telemetría viva Rover (push MQTT, no poll)
            state.lastRover?.let { t ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Rover L=${t.left_pwm} R=${t.right_pwm} US=${t.ultrasonic_cm ?: "-"}cm", style = MaterialTheme.typography.bodyMedium)
                        if (t.ir_left != null || t.ir_center != null || t.ir_right != null) {
                            Text(text = "IR L=${t.ir_left} C=${t.ir_center} R=${t.ir_right}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        // barra progreso PWM -255..255
                        LinearProgressIndicator(progress = { (t.left_pwm.coerceIn(-255, 255) + 255) / 510f }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // MOV-02: Card LED local solo lectura — círculo 64dp + etiqueta + lastSync + error
            LedStatusCard(
                state = state.ledState,
                onRetry = { vm.refreshLedState() }
            )

            // Card Health existente
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

            // Barra acciones [Reintentar] [Poll toggle]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.refreshHealth() }) {
                    Text("Reintentar")
                }
                Button(onClick = { vm.refreshLedState() }) {
                    Text("Refrescar LED")
                }
                if (vm.isPolling) {
                    OutlinedButton(onClick = { vm.stopLedPolling() }) { Text("Pausar poll") }
                } else {
                    OutlinedButton(onClick = { vm.startLedPolling() }) { Text("Reanudar poll") }
                }
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
