package com.aethernet.aethercontrol.ui.screens

// =============================================================================
// BluetoothScreen.kt — Pantalla Bluetooth Fallback SPP | 6º Semestre UTP | MOV-07 RF-1.3
// Autor: Andres Felipe Martinez Henao
// Analogía React: como `function BluetoothScreen({ vm }) { return <View><List>devices</List><Pin pad/></View> }`
// en React Native — lista de HC-06 emparejados + teclado PIN + presencia FC-51.
// Analogía C: lado app del protocolo nano-access-compacto.ino (CMD:ACCESS + POLL:PRESENCE).
// FOSS: Compose Material3 + Activity Result API (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: MOV-07 RF-1.3 — contingencia BT si cae el Wi-Fi (docs/backlog.md:24).
// Permisos: Android 12+ exige BLUETOOTH_CONNECT runtime — botón "Habilitar Bluetooth"
// pide con ActivityResultContracts.RequestPermission y luego vm.loadDevices().
// =============================================================================

import android.Manifest // Manifest.permission.BLUETOOTH_CONNECT
import android.content.pm.PackageManager // checkSelfPermission — como navigator.permissions
import android.os.Build // VERSION.SDK_INT — para exigir runtime permiso solo API 31+
import androidx.activity.compose.rememberLauncherForActivityResult // launcher de permiso — como useEffect+ask
import androidx.activity.result.contract.ActivityResultContracts // RequestPermission — como askPermission()
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState // scroll — como overflow-y auto
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // by collectAsStateWithLifecycle
import androidx.compose.runtime.remember // remember { permission } — como useMemo en React
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // context — para checkSelfPermission
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat // checkSelfPermission — como navigator.permissions.query
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.data.bluetooth.BtConnectionState // estado SPP
import com.aethernet.aethercontrol.ui.viewmodel.BluetoothViewModel

/**
 * MOV-07 — Pantalla fallback Bluetooth SPP (RF-1.3).
 * 1) Si API 31+ y sin permiso BLUETOOTH_CONNECT -> botón "Habilitar Bluetooth".
 * 2) Lista HC-06 emparejados (vm.loadDevices después de permiso).
 * 3) Conectado: presencia FC-51 en vivo (poll 1s en VM) + teclado PIN (espejo PinScreen).
 * Feedback: lastResultSuccess ✓/✕ como PinScreen, badge de conexión "BT ● HC-06".
 */
@Composable
fun BluetoothScreen(vm: BluetoothViewModel, onBack: () -> Unit) {
    val s by vm.uiState.collectAsStateWithLifecycle() // state — como useSelector en React
    val context = LocalContext.current // para checkSelfPermission
    val needsRuntimePerm = Build.VERSION.SDK_INT >= 31 // BLUETOOTH_CONNECT solo API 31+ (Android 12+)
    // permiso ya otorgado? — como `navigator.permissions.query({name:'bluetooth'})` en JS
    val hasPermission = !needsRuntimePerm || // API 24..30: BLUETOOTH es normal, ya instalado
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    // Launcher de permiso — como `const [granted, request] = usePromision()` en React
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.loadDevices() else Unit // si acepta -> lista emparejados
    }

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
            Text("Bluetooth Fallback (HC-06)", style = MaterialTheme.typography.titleLarge)
            Text("Contingencia RF-1.3: PIN + presencia por Bluetooth SPP si cae el Wi-Fi", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            // --- Permiso runtime API 31+ (Android 12+) ---
            if (needsRuntimePerm && !hasPermission) {
                Button(onClick = { requestPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Habilitar Bluetooth")
                }
            } else if (!vm.btSupported()) { // sin adaptador BT
                Text("Este dispositivo no tiene Bluetooth", color = Color.Red, style = MaterialTheme.typography.bodyMedium)
            } else {
                // --- Lista dispositivos emparejados (HC-06) ---
                Button(onClick = { vm.loadDevices() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Actualizar dispositivos")
                }
                if (s.devices.isEmpty()) {
                    Text("Sin BT emparejados — pareja el HC-06 (PIN 1234) desde ajustes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                s.devices.forEach { d ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.connect(d.address) },
                            enabled = s.connection !is BtConnectionState.Connected && !s.isLoading,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (d.name.isBlank()) d.address else d.name) }
                        Text(d.address, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
                if (s.connection is BtConnectionState.Connected) {
                    OutlinedButton(onClick = { vm.disconnect() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Desconectar ${(s.connection as BtConnectionState.Connected).deviceName}")
                    }
                }
            }

            // --- Badge estado BT (como badge MQTT de PinScreen:154) ---
            Text(
                text = when (val c = s.connection) {
                    is BtConnectionState.Connected -> "BT ● ${c.deviceName}"
                    is BtConnectionState.Connecting -> "BT ○ Conectando ${c.deviceName}…"
                    is BtConnectionState.Error -> "BT ✕ ${c.msg}"
                    is BtConnectionState.Disconnected -> "BT - Desconectado"
                },
                color = if (s.connection is BtConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray
            )

            // --- Presencia FC-51 en vivo (poll 1s + push) ---
            Text(
                text = when (s.presence) {
                    null -> "Presencia: —"
                    true -> "Presencia: SI (objeto detectado)"
                    false -> "Presencia: NO"
                },
                color = when (s.presence) { true -> Color(0xFFF57C00); false -> Color(0xFF4CAF50); null -> Color.Gray },
                style = MaterialTheme.typography.bodyMedium
            )
            s.error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodyMedium) }
            s.lastMessage?.let {
                val c = if (s.lastResultSuccess == true) Color(0xFF4CAF50) else if (s.lastResultSuccess == false) Color.Red else Color.Gray
                Text(it, color = c, style = MaterialTheme.typography.bodyMedium)
            }

            // --- Teclado PIN (mismo grid que PinScreen:104) — solo cuando conectado ---
            if (s.connection is BtConnectionState.Connected) {
                Text("PIN del cerrojo (${s.pinInput.length}/6)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#") // * borrar, # enviar — espejo keypad MEGA 4x4
                )
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            val isSend = k == "#"
                            val isClear = k == "*"
                            Button(
                                onClick = {
                                    when (k) {
                                        "*" -> vm.onPinClear()
                                        "#" -> vm.sendPin()
                                        else -> vm.onPinDigit(k)
                                    }
                                },
                                enabled = !s.isLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = when (k) { "#" -> "Enviar"; "*" -> "Borrar"; else -> k }, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.onPinBackspace() }, enabled = !s.isLoading && s.pinInput.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Text("←")
                    }
                    OutlinedButton(onClick = { vm.onPinClear() }, enabled = !s.isLoading && s.pinInput.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Text("Limpiar")
                    }
                }
            }

            if (s.isLoading) CircularProgressIndicator() // spinner — como PinScreen:151
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver Dashboard")
            }
            Text("Nodo Acceso Compacto: HC-06 BT (9600) + FC-51. PIN 1234. Falta Wi-Fi? Usa BT (nano-access-compacto.ino)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}