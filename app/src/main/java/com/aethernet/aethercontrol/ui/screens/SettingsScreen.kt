package com.aethernet.aethercontrol.ui.screens

// =============================================================================
// SettingsScreen.kt — Pantalla Ajustes funcional | 6º Semestre UTP | MOV-08/MOV-09
// Autor: Andres Felipe Martinez Henao
// Secciones: RED (URL backend + Guardar y reconectar + Probar broker),
// Bluetooth (estado HC-06 live + abrir gestión), Rover (velocidad máx aplica al
// joystick; fail-safe solo informativo firmware 500ms HU-04), Tema (aplica en vivo).
// Diálogos: logs MQTT (estado + broker + topics) y versiones (app + backend + FW doc).
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// =============================================================================

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.core.di.ServiceLocator
import com.aethernet.aethercontrol.data.bluetooth.BtConnectionState
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.ui.viewmodel.SettingsViewModel
import com.aethernet.aethercontrol.ui.viewmodel.ThemeMode
import com.aethernet.aethercontrol.ui.viewmodel.deriveBrokerLabel

/**
 * MOV-08/MOV-09 — Pantalla Ajustes funcional.
 * RED edita la URL del backend (mueve HTTP + deriva el broker); Bluetooth muestra el
 * estado SPP live; Rover aplica el cap de velocidad al joystick; Tema aplica en vivo.
 * Sin iconos externos ni TopAppBar experimental: header con Text para máxima compatibilidad.
 */
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onOpenBluetooth: () -> Unit = {}
) {
    val s by vm.uiState.collectAsStateWithLifecycle()
    val mqtt by vm.mqttState.collectAsStateWithLifecycle()
    val btSt by vm.btState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ajustes", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when (mqtt) {
                    is MqttConnectionState.Connected -> "MQTT ● ${(mqtt as MqttConnectionState.Connected).broker}"
                    is MqttConnectionState.Connecting -> "MQTT ○ conectando..."
                    is MqttConnectionState.Error -> "MQTT ✕ ${(mqtt as MqttConnectionState.Error).msg}"
                    else -> "MQTT - desconectado"
                },
                color = if (mqtt is MqttConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            // ===== RED: URL backend (mueve HTTP + deriva broker MQTT) =====
            SettingsSection(title = "RED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Backend (HTTP + MQTT)", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = s.backendUrl,
                            onValueChange = { vm.onBackendUrlChanged(it) },
                            label = { Text("URL del backend") },
                            placeholder = { Text("http://192.168.1.50:8000/") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            "Broker derivado: " + if (s.backendUrl.isBlank()) "—" else deriveBrokerLabel(s.backendUrl),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.onSaveAndReconnect() },
                                enabled = !s.isApplying,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (s.isApplying) "Aplicando…" else "Aplicar", maxLines = 1, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { vm.onTestMqttConnection() },
                                enabled = !s.isTestingMqtt,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (s.isTestingMqtt) "Probando..." else "Probar broker", maxLines = 1, fontSize = 13.sp)
                            }
                        }
                        if (s.isApplying || s.isTestingMqtt) {
                            CircularProgressIndicator()
                        }
                        s.applySuccess?.let {
                            Text(it, color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                        }
                        s.applyError?.let {
                            Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                        if (s.mqttTestSuccess) {
                            Text("Broker alcanzable", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                        }
                        s.mqttTestError?.let {
                            Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ===== BLUETOOTH: estado SPP live + acceso a gestión =====
            SettingsSection(title = "Bluetooth") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (val c = btSt) {
                                is BtConnectionState.Connected -> "HC-06: ● ${c.deviceName}"
                                is BtConnectionState.Connecting -> "HC-06: ○ conectando ${c.deviceName}…"
                                is BtConnectionState.Error -> "HC-06: ✕ ${c.msg}"
                                is BtConnectionState.Disconnected -> "HC-06: - Desconectado"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (btSt is BtConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray
                        )
                        Text(
                            "Nodo Acceso Compacto (Nano + HC-06 + FC-51). El PIN se envía desde Acceso (auto BT/MQTT).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        OutlinedButton(onClick = onOpenBluetooth, modifier = Modifier.fillMaxWidth()) {
                            Text("Abrir Bluetooth")
                        }
                    }
                }
            }

            // ===== ROVER: cap velocidad funcional + fail-safe informativo =====
            SettingsSection(title = "Rover") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Velocidad máx: ${s.maxSpeedPct}% (≈${s.maxSpeedPct * 255 / 100} PWM)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                        Slider(
                            value = s.maxSpeedPct.toFloat() / 100f,
                            onValueChange = { vm.onMaxSpeedPctChanged((it * 100).toInt()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Aplica al joystick en vivo (solo reduce). Fail-safe: 500 ms en firmware (HU-04, no editable desde app).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // ===== TEMA (aplica en vivo) =====
            SettingsSection(title = "Tema") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeButton("Claro", s.themeMode == ThemeMode.CLARO, { vm.onThemeModeChanged(ThemeMode.CLARO) })
                        ThemeModeButton("Oscuro", s.themeMode == ThemeMode.OSCURO, { vm.onThemeModeChanged(ThemeMode.OSCURO) })
                        ThemeModeButton("Sistema", s.themeMode == ThemeMode.SISTEMA, { vm.onThemeModeChanged(ThemeMode.SISTEMA) })
                    }
                    Text(
                        "Se aplica en vivo al elegir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ===== DIÁLOGOS =====
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { vm.onOpenMqttLogs() }, modifier = Modifier.weight(1f)) {
                    Text("Ver logs MQTT", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { vm.onOpenFwInfo() }, modifier = Modifier.weight(1f)) {
                    Text("Versión FW", fontSize = 12.sp)
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        }

        // --- Diálogo logs MQTT: estado live + broker + topics suscritos ---
        if (s.showMqttLogs) {
            val topics = remember {
                runCatching { ServiceLocator.mqttManager.subscribedTopics }.getOrDefault(
                    listOf(
                        "aethernet/rover/telemetry",
                        "aethernet/access/event",
                        "aethernet/seguridad/intrusion",
                        "aethernet/system/status"
                    )
                )
            }
            AlertDialog(
                onDismissRequest = { vm.onCloseMqttLogs() },
                title = { Text("MQTT") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Estado: " + when (val m = mqtt) {
                                is MqttConnectionState.Connected -> "● ${m.broker}"
                                is MqttConnectionState.Connecting -> "○ conectando…"
                                is MqttConnectionState.Error -> "✕ ${m.msg}"
                                else -> "- desconectado"
                            },
                            fontFamily = FontFamily.Monospace
                        )
                        Text("Topics:", style = MaterialTheme.typography.titleSmall)
                        topics.forEach { t ->
                            Text(t, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { vm.onCloseMqttLogs() }) { Text("Cerrar") } }
            )
        }

        // --- Diálogo versiones: app (PackageManager) + backend (/health live) + firmware doc ---
        if (s.showFwInfo) {
            @Suppress("DEPRECATION")
            val appVer = remember {
                runCatching {
                    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
                }.getOrDefault("?")
            }
            AlertDialog(
                onDismissRequest = { vm.onCloseFwInfo() },
                title = { Text("Versiones") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("App: $appVer", fontFamily = FontFamily.Monospace)
                        when {
                            s.isLoadingFw -> Text("Backend: cargando…")
                            s.fwBackendVersion != null -> Text(
                                "Backend: ${s.fwBackendVersion}",
                                fontFamily = FontFamily.Monospace
                            )
                            s.fwError != null -> Text("Backend: ✕ ${s.fwError}", color = Color.Red)
                        }
                        Text("Firmware (doc):", style = MaterialTheme.typography.titleSmall)
                        Text("gateway-esp32 + rover-uno (fail-safe 500ms HU-04) + mega-access (servo 90°)", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = { TextButton(onClick = { vm.onCloseFwInfo() }) { Text("Cerrar") } }
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ThemeModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Button(onClick = onClick, modifier = Modifier.weight(1f)) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
            Text(label)
        }
    }
}
