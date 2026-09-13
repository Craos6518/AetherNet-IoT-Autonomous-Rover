package com.aethernet.aethercontrol.ui.screens

// =============================================================================
// DashboardScreen.kt — Pantalla Dashboard | 6º Semestre UTP | MOV-01 5.4 + MOV-02 + MOV-03 + MOV-04
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años HTML/CSS/JS/React (Dashboard como <Dashboard> en React),
//              2 años electrónica (LED 44/45/46, MqttManager), 1 año C, 1 año PostgreSQL
// Analogía React: este @Composable es como `function DashboardScreen({ vm, onOpenPin }) { const state = useSelector(vm.uiState); return <Scaffold><Card><Text>{state.health.status}</Text></Scaffold> }`
// en React con Redux — aquí con Compose + StateFlow + collectAsStateWithLifecycle.
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: MOV-01 5.4 + MOV-02 (RF-1.1, HU-01/HU-02) + MOV-03 (RF-1.1 <50ms MQTT) + MOV-04 Plan B nav a PinScreen.
// Maneja isConnected=false con banner Desconectado — base para MOV-09 reconexión.
// MOV-02 añade Card LED [círculo 64dp + etiqueta + lastSync + error] solo lectura, polling 5s en ViewModel.
// MOV-03 añade estado MQTT vivo <50ms prd.md:50 — tcp://host:1883 derivada de getCurrentBaseUrl() + fallback ws://host:9001, telemetría Rover push.
// MOV-04: botón "Abrir PIN cerrojo" navega a Dest.Pin (PinScreen dedicada, no card embebida).
// =============================================================================

import androidx.compose.foundation.layout.Arrangement // gap — como `gap: 12px` en CSS Flex
import androidx.compose.foundation.layout.Column // Column — como `<div flexDirection="column">` en React
import androidx.compose.foundation.layout.Row // Row — como `<div flexDirection="row">` en React
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize — como `width: 100%; height: 100%` en CSS
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth — como `width: 100%` en CSS
import androidx.compose.foundation.layout.padding // padding — como `padding: 16px` en CSS
import androidx.compose.foundation.rememberScrollState // scroll — como `overflow: auto` en CSS
import androidx.compose.foundation.verticalScroll // verticalScroll — como `overflow-y: auto` en CSS
import androidx.compose.material3.Button // Button — como `<button>` en HTML
import androidx.compose.material3.Card // Card — como `<div class="card">` en MUI React
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator // spinner — como `<CircularProgress>` en MUI
import androidx.compose.material3.LinearProgressIndicator // progress — como `<LinearProgress value={...} />` en MUI (para PWM Rover)
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton // OutlinedButton — como `<button variant="outlined">` en MUI
import androidx.compose.material3.OutlinedTextField // TextField — como `<input>` en HTML
import androidx.compose.material3.Scaffold // Scaffold — como `<div class="scaffold">` layout base en MUI (con innerPadding)
import androidx.compose.material3.Text // Text — como `<span>` en React
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // by collectAsStateWithLifecycle — como `const state = useSelector(...)` en React Redux
import androidx.compose.runtime.mutableStateOf // mutableStateOf — como `useState` en React
import androidx.compose.runtime.remember // remember — como `useMemo` en React
import androidx.compose.runtime.rememberCoroutineScope // scope — como `useCallback` con coroutine
import androidx.compose.runtime.setValue // setValue — como `setState` en React
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Color — como `color: #4CAF50` en CSS
import androidx.compose.ui.unit.dp // dp — como `px` pero density-independent
import androidx.lifecycle.compose.collectAsStateWithLifecycle // collectAsStateWithLifecycle — como `useSelector` con lifecycle (auto unsubscribe)
import com.aethernet.aethercontrol.core.di.ServiceLocator // ServiceLocator — DI manual (como createContext en React)
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState // estado MQTT — como connectionState en WebSocket JS
import com.aethernet.aethercontrol.ui.components.LedStatusCard // LedStatusCard — como `<LedStatusCard state={state.ledState} />` en React
import com.aethernet.aethercontrol.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch // launch — como `async () => { await updateBaseUrl() }` en JS

/**
 * DashboardScreen — MOV-01 5.4 + MOV-02 (RF-1.1, HU-01/HU-02) + MOV-04 Plan B nav a PinScreen.
 * Maneja isConnected=false con banner Desconectado — base para MOV-09 reconexión (retry con backoff).
 * MOV-02 añade Card LED [círculo 64dp + etiqueta + lastSync + error] solo lectura,
 * polling 5s en ViewModel (isActive), barra acciones [Reintentar] [Poll toggle].
 * MOV-03 añade MQTT vivo y telemetría Rover push.
 * MOV-04: botón "Abrir PIN cerrojo" navega a Dest.Pin (PinScreen dedicada).
 */
@Composable
fun DashboardScreen(vm: DashboardViewModel, onOpenPin: () -> Unit = {}, onOpenJoystick: () -> Unit = {}) {
    val state by vm.uiState.collectAsStateWithLifecycle() // state — como `const state = useSelector(vm.uiState)` en React Redux
    val scope = rememberCoroutineScope() // scope para launch — como `const scope = useCoroutineScope()` en React (coroutine)
    // Muestra y edita la baseUrl persistida (PreferencesManager.saveBaseUrl) — como `localStorage.getItem('apiBaseUrl')` en React
    val savedUrl by ServiceLocator.preferencesManager.apiBaseUrl.collectAsStateWithLifecycle(
        initialValue = ServiceLocator.getCurrentBaseUrl() // initial — como `localStorage.getItem('apiBaseUrl') ?? 'http://10.0.2.2:8000/'` en JS
    )
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) } // input editable — como `const [urlInput, setUrlInput] = useState(savedUrl)` en React
    var saveMsg by remember { mutableStateOf<String?>(null) } // mensaje guardado — como `const [saveMsg, setSaveMsg] = useState(null)` en React

    Scaffold { innerPadding -> // Scaffold — layout base con innerPadding (como `padding` de AppBar en MUI)
        Column(
            modifier = Modifier
                .fillMaxSize() // 100% — como `width: 100%; height: 100%` en CSS
                .padding(innerPadding) // padding Scaffold — como `padding: innerPadding` en MUI
                .padding(16.dp) // padding 16 — como `padding: 16px` en CSS
                .verticalScroll(rememberScrollState()), // scroll — como `overflow-y: auto` en CSS
            verticalArrangement = Arrangement.spacedBy(12.dp) // gap 12 — como `gap: 12px` en CSS Flex
        ) {
            // Banner desconectado (MOV-09 base) — como `{!isConnected && <Alert>Desconectado</Alert>}` en React
            if (!state.isConnected && !state.isLoading) {
                Text(
                    text = "Desconectado", // banner rojo — como `<Alert severity="error">Desconectado</Alert>` en MUI React
                    color = Color.Red,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // MOV-03: Estado MQTT vivo <50ms prd.md:50 — tcp://host:1883 (mosquitto.conf:4) o ws://host:9001 fallback
            // Como `{mqttState === 'connected' ? 'MQTT ●' : 'MQTT ○'}` en React con WebSocket status
            val mqttState = state.mqttState
            Text(
                text = when (mqttState) {
                    is MqttConnectionState.Connected -> "MQTT ● ${mqttState.broker}" // conectado — como `MQTT ● tcp://192.168.1.14:1883` en JS
                    is MqttConnectionState.Connecting -> "MQTT ○ Conectando..." // conectando — como `MQTT ○ Conectando...`
                    is MqttConnectionState.Error -> "MQTT ✕ ${mqttState.msg}" // error — como `MQTT ✕ lost`
                    is MqttConnectionState.Disconnected -> "MQTT - Desconectado" // desconectado — como `MQTT - Desconectado`
                },
                color = if (mqttState is MqttConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray, // verde si conectado, gris si no — como `color: isConnected ? 'green' : 'gray'` en React style
                style = MaterialTheme.typography.bodySmall
            )

            // MOV-03: Telemetría viva Rover (push MQTT, no poll) — como `lastRover && <Card><Text>Rover L={lastRover.left_pwm}</Text></Card>` en React
            state.lastRover?.let { t ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Rover L=${t.left_pwm} R=${t.right_pwm} US=${t.ultrasonic_cm ?: "-"}cm", style = MaterialTheme.typography.bodyMedium) // PWM -255..255 y US cm — como `Rover L={t.left_pwm} R={t.right_pwm}` en React
                        if (t.ir_left != null || t.ir_center != null || t.ir_right != null) {
                            Text(text = "IR L=${t.ir_left} C=${t.ir_center} R=${t.ir_right}", style = MaterialTheme.typography.bodySmall, color = Color.Gray) // IR boolean — como `IR L={t.ir_left}` en React
                        }
                        // barra progreso PWM -255..255 mapeado a 0..1 para LinearProgressIndicator — como `progress = (pwm + 255)/510` en JS (normaliza)
                        LinearProgressIndicator(progress = { (t.left_pwm.coerceIn(-255, 255) + 255) / 510f }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // MOV-04 Plan B: acceso a PinScreen dedicada (no embebida) — como `<Button onClick={() => navigate('/pin')}>Abrir PIN</Button>` en React Router
            Button(
                onClick = onOpenPin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir PIN cerrojo")
            }

            // MOV-05 RF-1.2: acceso a JoystickScreen (mando virtual Rover) — como `<Button onClick={() => navigate('/joystick')}>Joystick Rover</Button>` en React Router
            Button(
                onClick = onOpenJoystick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Joystick Rover — control tanque")
            }

            // MOV-02: Card LED local solo lectura — círculo 64dp + etiqueta + lastSync + error — como `<LedStatusCard state={state.ledState} />` en React
            LedStatusCard(
                state = state.ledState,
                onRetry = { vm.refreshLedState() } // reintentar — como `onClick={refreshLedState}` en React
            )

            // Card Health existente — como `<Text>{health.status}</Text>` en React
            Text(
                text = state.health?.status ?: if (state.isLoading) "..." else "Sin datos", // status o "..." loading — como `{health?.status ?? (isLoading ? '...' : 'Sin datos')}` en JS
                style = MaterialTheme.typography.headlineSmall
            )

            state.health?.let { h ->
                Text(text = "DB: ${h.database} | v${h.version}", style = MaterialTheme.typography.bodyMedium) // DB status — como `DB: ok | v1.0.0-sprint1` en React
            }

            if (state.isLoading) {
                CircularProgressIndicator() // spinner — como `<CircularProgress />` en MUI React
            }

            state.error?.let { err ->
                Text(text = err, color = Color.Red, style = MaterialTheme.typography.bodyMedium) // error — como `{error && <Text color="red">{error}</Text>}` en React
            }

            // Barra acciones [Reintentar] [Poll toggle] — como `<div><Button>Reintentar</Button><Button>Poll</Button></div>` en React
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.refreshHealth() }) { // reintentar health — como `onClick={refreshHealth}` en React
                    Text("Reintentar")
                }
                Button(onClick = { vm.refreshLedState() }) {
                    Text("Refrescar LED")
                }
                if (vm.isPolling) { // polling activo — como `isPolling ? 'Pausar poll' : 'Reanudar poll'` en React
                    OutlinedButton(onClick = { vm.stopLedPolling() }) { Text("Pausar poll") }
                } else {
                    OutlinedButton(onClick = { vm.startLedPolling() }) { Text("Reanudar poll") }
                }
            }

            state.lastSync?.let { ts ->
                Text(text = "Última sync: $ts", style = MaterialTheme.typography.bodySmall) // timestamp — como `Última sync: ${new Date(ts).toLocaleString()}` en React
            }

            // --- Ajuste de dirección backend (MOV-01 3.1) — como `localStorage` editor en React ---
            Text(
                text = "Backend URL",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Actual: $savedUrl", // actual — como `Actual: ${savedUrl}` en React
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            OutlinedTextField(
                value = urlInput, // input — como `value={urlInput}` en React <input>
                onValueChange = { urlInput = it; saveMsg = null }, // onChange — como `onChange={e => setUrlInput(e.target.value)}` en React
                label = { Text("http://10.0.2.2:8000/ o 192.168.1.x:8000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { // guardar — como `onClick={async () => { await updateBaseUrl(urlInput); refreshHealth() }}` en React
                    scope.launch {
                        try {
                            val normalized = ServiceLocator.updateBaseUrl(urlInput) // normaliza y persiste — como `localStorage.setItem('apiBaseUrl', normalizeUrl(urlInput))` en JS
                            saveMsg = "Guardado: $normalized — reintentando..." // feedback — como `setSaveMsg('Guardado')` en React
                            vm.refreshHealth() // refresca health con nueva URL — como `fetchHealth()` en React
                        } catch (e: Exception) {
                            saveMsg = "Error: ${e.message}"
                        }
                    }
                }) {
                    Text("Guardar URL")
                }
                Button(onClick = { // restaurar — como `onClick={async () => { await resetBaseUrl(); refreshHealth() }}` en React
                    scope.launch {
                        val def = ServiceLocator.resetBaseUrl() // resetea a 10.0.2.2:8000 — como `localStorage.removeItem('apiBaseUrl')` en JS
                        urlInput = def
                        saveMsg = "Restaurado: $def"
                        vm.refreshHealth()
                    }
                }) {
                    Text("Restaurar")
                }
            }
            saveMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray) } // mensaje guardado — como `{saveMsg && <Text>{saveMsg}</Text>}` en React
            Text(
                text = "Tip: emulador=10.0.2.2:8000, físico=IP del PC (ipconfig/ifconfig) + :8000 . Asegúrate docker compose up.", // tip — como ayuda en React UI
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
