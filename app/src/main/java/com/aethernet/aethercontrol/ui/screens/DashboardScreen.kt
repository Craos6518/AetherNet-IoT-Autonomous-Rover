package com.aethernet.aethercontrol.ui.screens

// =============================================================================
// DashboardScreen.kt — Dashboard consolidado | 6º Semestre UTP | MOV-08 RF-1.1
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (dashboard con cards), 2 años electrónica (LED 44/45/46, HC-SR04, TCRT5000),
//              2 años Python (telemetría MQTT), 1 año PostgreSQL (security_events)
// Analogía React: este @Composable es como `function DashboardScreen({ vm }) { const s = useSelector(vm.uiState); return <div><LedCard/><RoverCard/><AlertsCard/></div> }`
// en React con JSX — aquí con Compose (declarativo, scroll vertical como `overflow-y: auto` en CSS).
// Analogía Python: como `def dashboard_screen(s): return [led_card(s.led), rover_card(s.rover), alerts_card(s.alerts)]` en Streamlit.
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: MOV-08 RF-1.1 (consolida MOV-02 LED local + MOV-03 telemetría MQTT + alertas HU-02).
// Consolida en una sola pantalla lo que antes estaba disperso: LedStatusCard (MOV-02), telemetría Rover
// (antes solo en Joystick/MQTT push), y últimas alertas access/security (HU-01/HU-02).
// Flujo: DashboardViewModel colecta MQTT push (rover/telemetry:69, access/event:71, seguridad/intrusion:72)
//        + fallback HTTP poll 5s (MOV-02) → aquí solo pinta state, no fetchea directo (MVVM).
// =============================================================================

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.ui.components.LedStatusCard
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel

/**
 * MOV-08 — Dashboard consolidado (RF-1.1).
 * Cards: [Estado conexión] + [LED local MOV-02] + [Telemetría Rover MOV-03] + [Alertas HU-01/HU-02] + accesos.
 * Si vienes de React: es como `<Dashboard s={state} onRetry={vm.refresh} onOpenX={nav} />` en TS.
 */
@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onOpenPin: () -> Unit = {},
    onOpenJoystick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHistorial: () -> Unit = {},
    onOpenBluetooth: () -> Unit = {}
) {
    val s by vm.uiState.collectAsStateWithLifecycle() // state — como `const s = useSelector(vm.uiState)` en React Redux
    val mqtt = s.mqttState // atajo — como `const mqtt = s.mqttState` en JS

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // scroll — como `overflow-y: auto` en CSS (consolidado no cabe en 1 pantalla)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Inicio", style = MaterialTheme.typography.titleLarge)

            // --- Card Estado conexión: MQTT badge + backend health + última sync (RF-1.1) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Estado", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = when (mqtt) {
                            is MqttConnectionState.Connected -> "MQTT ● ${(mqtt as MqttConnectionState.Connected).broker}"
                            is MqttConnectionState.Connecting -> "MQTT ○ Conectando…"
                            is MqttConnectionState.Error -> "MQTT ✕ ${(mqtt as MqttConnectionState.Error).msg}"
                            is MqttConnectionState.Disconnected -> "MQTT ○ Desconectado (poll HTTP 5s)"
                        },
                        color = if (mqtt is MqttConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (s.isConnected) "Backend ● Conectado${s.health?.let { " v${it.version}" } ?: ""}"
                        else "Backend ○ Desconectado",
                        color = if (s.isConnected) Color(0xFF4CAF50) else Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    s.lastSync?.let { ts ->
                        val ageSec = ((System.currentTimeMillis() - ts) / 1000).coerceAtLeast(0)
                        Text(
                            text = "Última sync hace ${ageSec}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    s.error?.let { err ->
                        Text(text = err, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!s.isConnected || s.error != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.refreshHealth() }) { Text("Reintentar") }
                        }
                    }
                }
            }

            // --- LED local (MOV-02, HU-01/HU-02) — reutiliza LedStatusCard ---
            LedStatusCard(
                state = s.ledState,
                onRetry = { vm.refreshLedState() },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Telemetría Rover (MOV-03, RF-1.1 <50ms) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Rover", style = MaterialTheme.typography.titleSmall)
                    val r = s.lastRover
                    if (r == null) {
                        Text(
                            "Sin telemetría — abre Rover y mueve el joystick.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            "L=${r.left_pwm}  R=${r.right_pwm}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "US=${r.ultrasonic_cm?.let { "$it cm" } ?: "—"}  " +
                                "IR=[${r.ir_left?.let { if (it) "BORDE" else "libre" } ?: "—"}/" +
                                "${r.ir_center?.let { if (it) "BORDE" else "libre" } ?: "—"}/" +
                                "${r.ir_right?.let { if (it) "BORDE" else "libre" } ?: "—"}]  " +
                                "RSSI=${r.rf_rssi?.let { "$it dBm" } ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    OutlinedButton(onClick = onOpenJoystick, modifier = Modifier.fillMaxWidth()) {
                        Text("Abrir Joystick Rover")
                    }
                }
            }

            // --- Alertas (HU-01 accesos + HU-02 intrusión, MOV-08) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Alertas", style = MaterialTheme.typography.titleSmall)
                    val sec = s.lastSecurity
                    val acc = s.lastAccess
                    if (sec == null && acc == null) {
                        Text(
                            "Sin alertas — el LED y Telegram avisarán aquí.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        sec?.let {
                            Text(
                                "🚨 ${it.event_type} (${it.severity ?: "medium"})" +
                                    (it.description?.let { d -> " — $d" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBA1A1A)
                            )
                        }
                        acc?.let {
                            Text(
                                (if (it.success) "✅ Acceso OK" else "⛔ Acceso fallido") +
                                    " ${it.pin_hash} (${it.source})",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it.success) Color(0xFF2E7D32) else Color(0xFFBA1A1A)
                            )
                        }
                        s.lastAlertAt?.let { ts ->
                            val ageSec = ((System.currentTimeMillis() - ts) / 1000).coerceAtLeast(0)
                            Text(
                                "Última alerta hace ${ageSec}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    OutlinedButton(onClick = onOpenHistorial, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver Historial")
                    }
                }
            }

            // --- Accesos a módulos (navegación) ---
            Button(onClick = onOpenJoystick, modifier = Modifier.fillMaxWidth()) {
                Text("Rover")
            }
            Button(onClick = onOpenPin, modifier = Modifier.fillMaxWidth()) {
                Text("Acceso")
            }
            Button(onClick = onOpenHistorial, modifier = Modifier.fillMaxWidth()) {
                Text("Historial")
            }
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Ajustes")
            }
            OutlinedButton(onClick = onOpenBluetooth, modifier = Modifier.fillMaxWidth()) {
                Text("Bluetooth (contingencia)")
            }
        }
    }
}
