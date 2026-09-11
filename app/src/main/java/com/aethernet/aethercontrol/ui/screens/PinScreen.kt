package com.aethernet.aethercontrol.ui.screens

// =============================================================================
// PinScreen.kt — Pantalla PIN Cerrojo | 6º Semestre UTP | MOV-04 HU-01, RF-2.2
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años HTML/CSS/JS/React (form Grid), 2 años electrónica (keypad 4x4),
//              2 años Python (validator), 1 año C (PIN buffer)
// Analogía React: este @Composable es como `function PinScreen({ vm, onBack }) { const {pinInput, isValid, error} = useSelector(vm.pinState); return <div><Dots count={6} filled={pinInput.length} /><Grid rows={rows} /></div> }`
// en React con JSX — aquí con Compose (declarativo, como React pero para Android).
// FOSS: Compose Material3 (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: MOV-04 Plan B — S — HU-01 RF-2.2 — Pantalla dedicada PIN cerrojo (no card embebida en Dashboard).
// Grid 4x3: 1 2 3 / 4 5 6 / 7 8 9 / * 0 # (# = enviar, * = borrar) — espejo keypad físico MEGA 4x4 keypad_control.cpp:51.
// Dots 6x CircleShape reflejan pinInput.length sin mostrar PIN claro (seguridad, como password dots en React).
// Observa PinViewModel.pinState + mqttState para badge "MQTT ●" (ver PinViewModel.kt:32).
// Navega desde DashboardScreen "Abrir PIN cerrojo" -> NavGraph Dest.Pin (NavGraph.kt:35).
// =============================================================================

import androidx.compose.foundation.background // background — como `backgroundColor` en CSS
import androidx.compose.foundation.layout.Arrangement // gap — como `gap` en CSS Flex
import androidx.compose.foundation.layout.Box // Box — como `<div>` con `position: relative` en React
import androidx.compose.foundation.layout.Column // Column — como `<div flexDirection="column">` en React
import androidx.compose.foundation.layout.Row // Row — como `<div flexDirection="row">` en React
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape // círculo — como `borderRadius: 50%` en CSS
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold // layout base — como `<div class="scaffold">` en MUI
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // by collectAsStateWithLifecycle — como `const s = useSelector(...)` en React Redux
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // sp — como `px` pero scaled (accesibilidad)
import androidx.lifecycle.compose.collectAsStateWithLifecycle // collectAsStateWithLifecycle — como `useSelector` con lifecycle
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.ui.viewmodel.PinViewModel

/**
 * MOV-04 Plan B — S — HU-01 RF-2.2 — Pantalla dedicada PIN cerrojo.
 * Grid 4x3: 1 2 3 / 4 5 6 / 7 8 9 / * 0 # (# = enviar, * = borrar) — espejo keypad MEGA.
 * Dots 6x CircleShape reflejan pinInput.length sin mostrar PIN claro (seguridad).
 * Observa PinViewModel.pinState + mqttState para badge "MQTT ●".
 * Navega desde DashboardScreen "Abrir PIN cerrojo" -> NavGraph Dest.Pin.
 */
@Composable
fun PinScreen(
    vm: PinViewModel, // ViewModel PIN — como `pinViewModel` prop en React
    onBack: () -> Unit // callback volver — como `onBack` prop en React (NavGraph popBackStack)
) {
    val s by vm.pinState.collectAsStateWithLifecycle() // pin state — como `const s = useSelector(vm.pinState)` en React Redux
    val mqtt by vm.mqttState.collectAsStateWithLifecycle() // mqtt state — como `const mqtt = useSelector(vm.mqttState)` en React

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // centro — como `alignItems: center` en CSS Flex
            verticalArrangement = Arrangement.spacedBy(12.dp) // gap 12 — como `gap: 12px` en CSS Flex
        ) {
            Text("PIN cerrojo", style = MaterialTheme.typography.titleLarge) // título — como `<h1>PIN cerrojo</h1>` en HTML
            Text(
                "Ingresa 4-6 dígitos y presiona # para enviar", // ayuda — como placeholder en React form
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Dots 6x — sin exponer PIN (seguridad) — como `Array(6).fill(0).map((_, i) => <Dot filled={i < pin.length} />)` en React
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { i -> // 6 dots — como `Array(6).map` en JS
                    Box(
                        modifier = Modifier
                            .size(16.dp) // 16dp círculo — como `width: 16px; height: 16px` en CSS
                            .clip(CircleShape) // círculo — como `borderRadius: 50%` en CSS
                            .background(if (i < s.pinInput.length) Color.Black else Color.LightGray) // lleno si i < len — como `filled ? 'black' : 'lightgray'` en React style
                    )
                }
            }

            Text(
                text = "${s.pinInput.length} / 6", // contador — como `${pin.length} / 6` en React
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            s.error?.let { // error validación/throttle/MQTT — como `{error && <Text color="red">{error}</Text>}` en React
                Text(it, color = Color.Red, style = MaterialTheme.typography.bodyMedium)
            }
            s.lastMessage?.let { // mensaje confirmación — como `{lastMessage && <Text color={success ? 'green' : 'red'}>{lastMessage}</Text>}` en React
                val c = if (s.lastResultSuccess == true) Color(0xFF4CAF50) else if (s.lastResultSuccess == false) Color.Red else Color.Gray
                Text(it, color = c, style = MaterialTheme.typography.bodyMedium)
            }

            // Grid 4x3 sin Lazy — 4 filas x 3 cols — como `rows.map(row => <Row>{row.map(k => <Button>{k}</Button>)}</Row>)` en React
            val rows = listOf(
                listOf("1", "2", "3"), // fila 1 — como `["1","2","3"]` en JS array
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "#") // fila 4: * borrar, 0, # enviar — como `["*","0","#"]` en JS (MEGA * borra, # envía)
            )
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // gap 8 — como `gap: 8px` en CSS Flex row
                ) {
                    row.forEach { k ->
                        val isSend = k == "#" // # enviar — como `k === '#'` en JS
                        val isClear = k == "*" // * borrar — como `k === '*'` en JS
                        Button(
                            onClick = {
                                when (k) { // switch(k) — como `switch(k)` en JS/C
                                    "*" -> vm.onPinClear() // borrar — como `onPinClear()` en React handler
                                    "#" -> vm.sendPin() // enviar — como `sendPin()` en React (valida y publish MQTT)
                                    else -> vm.onPinDigit(k) // dígito — como `onPinDigit(k)` en React (añade a pinInput)
                                }
                            },
                            enabled = !s.isLoading, // disabled si loading — como `disabled={isLoading}` en React <button>
                            modifier = Modifier.weight(1f) // flex 1 — como `flex: 1` en CSS (3 botones iguales por fila)
                        ) {
                            Text(
                                text = when (k) {
                                    "#" -> "Enviar" // # muestra "Enviar" — como `k === '#' ? 'Enviar' : k` en JS
                                    "*" -> "Borrar" // * muestra "Borrar"
                                    else -> k
                                },
                                fontSize = 20.sp // 20sp — como `fontSize: 20px` en CSS pero scaled
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // row backspace/limpiar — como `<div><Button>←</Button><Button>Limpiar</Button></div>` en React
                OutlinedButton(onClick = { vm.onPinBackspace() }, enabled = !s.isLoading && s.pinInput.isNotEmpty()) { // backspace — como `onClick={onPinBackspace}` en React
                    Text("←") // flecha — como `←` en HTML
                }
                OutlinedButton(onClick = { vm.onPinClear() }, enabled = !s.isLoading && s.pinInput.isNotEmpty()) {
                    Text("Limpiar") // limpiar — como `onClick={onPinClear}` en React
                }
            }

            if (s.isLoading) CircularProgressIndicator() // spinner si enviando — como `{isLoading && <CircularProgress />}` en React MUI

            Text(
                text = when (mqtt) { // badge MQTT — como `mqtt === 'connected' ? 'MQTT ●' : 'MQTT ○'` en React
                    is MqttConnectionState.Connected -> "MQTT ● ${(mqtt as MqttConnectionState.Connected).broker}" // conectado — como `MQTT ● tcp://host:1883`
                    is MqttConnectionState.Connecting -> "MQTT ○ Conectando…" // conectando
                    is MqttConnectionState.Error -> "MQTT ✕ ${(mqtt as MqttConnectionState.Error).msg}" // error
                    is MqttConnectionState.Disconnected -> "MQTT - Desconectado" // desconectado
                },
                color = if (mqtt is MqttConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray, // verde si conectado — como `color: isConnected ? 'green' : 'gray'` en React style
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { // volver — como `<Button onClick={onBack}>Volver Dashboard</Button>` en React Router
                Text("Volver Dashboard")
            }

            Text(
                "Teclado físico MEGA 4x4 keypad_control.cpp:51 sigue activo (* borra, A-D ignoradas). App envía aethernet/access/command:70", // nota — como ayuda en React UI
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
