package com.aethernet.aethercontrol.ui.screens

// =============================================================================
// HistorialScreen.kt — Pantalla Historial Telemetría | 6º Semestre UTP | MOV-XX
// Autor: Andres Felipe Martinez Henao
// Sensores: TCRT5000 (3x borde), HC-SR04 (ultrasonido), FC-51 (presencia).
// Filtro EMA + reducción de ruido. Vacía por ahora.
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// =============================================================================

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.ui.viewmodel.HistorialViewModel

/**
 * MOV-XX — Pantalla Historial (vacía por ahora).
 * Panel histórico de telemetría: TCRT5000, HC-SR04, FC-51. Filtro EMA + reducción de ruido.
 */
@Composable
fun HistorialScreen(
    vm: HistorialViewModel,
    onBack: () -> Unit
) {
    val s by vm.uiState.collectAsStateWithLifecycle()
    val mqtt by vm.mqttState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Historial", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when (mqtt) {
                    is MqttConnectionState.Connected -> "MQTT conectado"
                    is MqttConnectionState.Connecting -> "MQTT conectando..."
                    is MqttConnectionState.Error -> "MQTT error"
                    else -> "MQTT desconectado"
                },
                color = if (mqtt is MqttConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Sensores", style = MaterialTheme.typography.titleSmall)
                    Text("TCRT5000 x3 (borde) — sin datos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("HC-SR04 (ultrasonido) — sin datos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("FC-51 (presencia) — sin datos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        "Filtro EMA + reducción de ruido (pendiente).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    s.error?.let {
                        Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }
    }
}
