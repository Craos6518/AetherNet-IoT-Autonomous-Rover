package com.aethernet.aethercontrol.ui.components

// =============================================================================
// LedStatusCard.kt — Card LED Local | 6º Semestre UTP | MOV-02 RF-1.1, HU-01/HU-02
// Autor: Est. Tecnología en Desarrollo Software + Ing. Sistemas (UTP)
// Experiencia: 2 años HTML/CSS/JS/React (Card como <div class="card">), 2 años electrónica (LED RGB 44/45/46),
//              2 años Python (LedStateMapper)
// Analogía React: este @Composable es como `function LedStatusCard({ state, onRetry }) { return <Card><Circle color={state.color} /><Text>{state.label}</Text></Card> }`
// en React con JSX — aquí con Compose (declarativo, como React pero para Android).
// Analogía Python: como `def led_status_card(state, on_retry): return Card(...)` en Python con Flet/Streamlit.
// Analogía C: colores 0xFF4CAF50 verde, 0xFFF44336 rojo, 0xFF9E9E9E off son como `setLedColor(0,255,0)` en firmware/mega-access/src/led.cpp:22
// pero en Kotlin con `Color(0xFF4CAF50)` (mismo RGB, distinta API).
// FOSS: Material3 (AndroidX, Apache 2.0) — RNF-3.1, sin UI kit propietario.
// Origen: MOV-02 RF-1.1 HU-01/HU-02 solo lectura — Card LED [círculo 64dp + etiqueta + timestamp relativo + error + Reintentar].
// Wireframe: Card LED [círculo 64dp color + texto estado + timestamp relativo] — ver Figma si existe.
// No envía comandos (solo lectura, como GET /api/access-events), Reintentar delega a ViewModel.refreshLedState().
// =============================================================================

import androidx.compose.foundation.background // background — como `backgroundColor` en CSS/React
import androidx.compose.foundation.layout.Arrangement // Arrangement — como `gap` en CSS Flex
import androidx.compose.foundation.layout.Box // Box — como `<div>` con `position: relative` en React
import androidx.compose.foundation.layout.Column // Column — como `<div flexDirection="column">` en React
import androidx.compose.foundation.layout.Row // Row — como `<div flexDirection="row">` en React
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth — como `width: 100%` en CSS
import androidx.compose.foundation.layout.padding // padding — como `padding: 16px` en CSS
import androidx.compose.foundation.layout.size // size — como `width: 64px; height: 64px` en CSS
import androidx.compose.foundation.shape.CircleShape // CircleShape — como `borderRadius: 50%` en CSS
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape — como `borderRadius: 16px` en CSS
import androidx.compose.material3.Button // Button — como `<button>` en HTML
import androidx.compose.material3.Card // Card — como `<div class="card" elevation>` en Material UI React
import androidx.compose.material3.CardDefaults // CardDefaults — como `cardElevation` en MUI
import androidx.compose.material3.CircularProgressIndicator // spinner — como `<CircularProgress>` en MUI React
import androidx.compose.material3.MaterialTheme // MaterialTheme — como `ThemeProvider` en React
import androidx.compose.material3.Text // Text — como `<span>` en React
import androidx.compose.runtime.Composable // @Composable — como `function Component()` en React
import androidx.compose.runtime.LaunchedEffect // LaunchedEffect — como `useEffect` en React
import androidx.compose.runtime.getValue // by remember — como `const [value, setValue] = useState()` en React
import androidx.compose.runtime.mutableLongStateOf // mutableLongStateOf — como `useState<number>` en React
import androidx.compose.runtime.remember // remember — como `useMemo` en React (memoriza)
import androidx.compose.runtime.setValue // setValue — como `setState` en React
import androidx.compose.ui.Alignment // Alignment — como `alignItems` en CSS Flex
import androidx.compose.ui.Modifier // Modifier — como `style` + `className` en React (chainable)
import androidx.compose.ui.draw.clip // clip — como `overflow: hidden + borderRadius` en CSS
import androidx.compose.ui.graphics.Color // Color — como `color: #4CAF50` en CSS
import androidx.compose.ui.tooling.preview.Preview // @Preview — como Storybook en React (preview en Android Studio)
import androidx.compose.ui.unit.dp // dp — como `px` en CSS pero density-independent (16dp = 16px en 160dpi)
import com.aethernet.aethercontrol.domain.model.LedColor
import com.aethernet.aethercontrol.domain.model.LedState
import com.aethernet.aethercontrol.domain.model.LedUiState
import kotlinx.coroutines.delay // delay — como `await sleep(ms)` en JS

/**
 * LedStatusCard — MOV-02 (RF-1.1 HU-01/HU-02 solo lectura).
 * Wireframe: Card LED [círculo 64dp color + texto estado + timestamp relativo] — como Figma Card.
 * Colores: 0xFF4CAF50 verde (desbloqueado 5s HU-01), 0xFFF44336 rojo (intrusión 10s HU-02 / fallo 1s), 0xFF9E9E9E off, Color.Red error / UNKNOWN.
 * No envía comandos (solo lectura, como GET). Reintentar delega a ViewModel.refreshLedState() — como `onClick={refreshLedState}` en React.
 */
@Composable
fun LedStatusCard(
    state: LedUiState, // estado LED — como prop `state` en React `<LedStatusCard state={ledState} />`
    onRetry: () -> Unit, // callback reintentar — como `onRetry` prop en React (llama ViewModel.refreshLedState)
    modifier: Modifier = Modifier
) {
    // Mapeo color enum → Color Compose — como `const circleColor = {GREEN: '#4CAF50', RED: '#F44336'}[state.color]` en JS
    val circleColor = when (state.color) {
        LedColor.GREEN -> Color(0xFF4CAF50) // verde — como `setLedColor(0,255,0)` en led.cpp:22 (0xFF4CAF50 = RGB 76,175,80)
        LedColor.RED -> Color(0xFFF44336) // rojo — como `setLedColor(255,0,0)` en led.cpp (0xFFF44336 = RGB 244,67,54)
        LedColor.OFF -> Color(0xFF9E9E9E) // gris off — como `setLedColor(0,0,0)` apagado en led.cpp (0xFF9E9E9E = gris)
        LedColor.UNKNOWN -> Color(0xFFBDBDBD) // gris claro unknown — como `UNKNOWN` en LedState.kt:19 (sin datos)
    }

    // Timestamp relativo "hace Xs / Xm" que tickea cada segundo — como `useEffect(() => { const id = setInterval(() => setNow(Date.now()), 1000) }, [])` en React
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) } // nowTick — como `const [now, setNow] = useState(Date.now())` en React
    LaunchedEffect(state.lastEventAt) { // reinicia timer si cambia lastEventAt — como `useEffect(() => { setInterval }, [lastEventAt])` en React
        while (true) {
            delay(1000) // cada segundo — como `setInterval(() => setNow(Date.now()), 1000)` en JS
            nowTick = System.currentTimeMillis()
        }
    }
    val relative = state.lastEventAt?.let { ts -> // calcula "hace 3s" — como `formatDistanceToNow(ts)` en date-fns (JS)
        val deltaSec = ((nowTick - ts) / 1000).coerceAtLeast(0) // segundos desde evento — como `Math.floor((Date.now() - ts)/1000)` en JS
        when {
            deltaSec < 60 -> "hace ${deltaSec}s" // <60s — como "hace 5s" en React
            deltaSec < 3600 -> "hace ${deltaSec / 60}m ${deltaSec % 60}s" // <1h — como "hace 2m 30s"
            else -> "hace ${deltaSec / 3600}h" // >1h — como "hace 2h"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(), // width 100% — como `style={{width: '100%'}}` en React
        shape = RoundedCornerShape(16.dp), // bordes 16dp — como `borderRadius: 16px` en CSS
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // sombra 2dp — como `boxShadow` en CSS (Material elevation)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { // padding 16 + gap 8 — como `padding: 16px; gap: 8px` en CSS Flex column
            Text(text = "LED Local", style = MaterialTheme.typography.titleMedium) // título — como `<h3>LED Local</h3>` en HTML
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { // row con círculo + texto — como `display: flex; align-items: center; gap: 12px` en CSS
                Box(
                    modifier = Modifier
                        .size(64.dp) // 64dp círculo — como `width: 64px; height: 64px` en CSS
                        .clip(CircleShape) // círculo — como `borderRadius: 50%` en CSS
                        .background(circleColor), // color — como `backgroundColor: circleColor` en React style
                    contentAlignment = Alignment.Center // centro — como `display: flex; justify-content: center` en CSS
                ) {
                    if (state.isLoading) { // loading — como `if (isLoading) return <Spinner />` en React
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), // spinner 24dp — como `size: 24px` en CSS
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // columna texto — como `flexDirection: column; gap: 4px` en CSS
                    Text(text = state.label, style = MaterialTheme.typography.titleSmall) // label — "Verde desbloqueado" etc. — como `{state.label}` en React
                    state.source?.let { src ->
                        Text(text = "Origen: $src", style = MaterialTheme.typography.bodySmall, color = Color.Gray) // origen — como `Origen: access` en React
                    }
                    relative?.let { rel ->
                        Text(text = rel, style = MaterialTheme.typography.bodySmall, color = Color.Gray) // "hace 3s" — como `{formatDistance(ts)}` en React
                    }
                    if (state.lastEventAt == null && state.state == LedState.UNKNOWN) {
                        Text(text = "Sin eventos", style = MaterialTheme.typography.bodySmall, color = Color.Gray) // sin eventos — como `Sin eventos` en React empty state
                    }
                }
            }

            state.error?.let { err ->
                Text(text = err, color = Color.Red, style = MaterialTheme.typography.bodySmall) // error — como `error && <Text color="red">{error}</Text>` en React
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // botón reintentar — como `<button onClick={onRetry}>Reintentar LED</button>` en React
                Button(onClick = onRetry) { Text("Reintentar LED") }
            }
        }
    }
}

// Previews — como Storybook stories en React (ver cada estado en Android Studio Preview sin correr app)
@Preview(name = "Verde desbloqueado")
@Composable private fun PreviewGreen() {
    LedStatusCard(state = LedUiState(color = LedColor.GREEN, state = LedState.GREEN_UNLOCKED, label = "Verde desbloqueado", lastEventAt = System.currentTimeMillis() - 2000, source = "access"), onRetry = {})
}

@Preview(name = "Rojo intrusión")
@Composable private fun PreviewRedIntrusion() {
    LedStatusCard(state = LedUiState(color = LedColor.RED, state = LedState.RED_INTRUSION, label = "Rojo intrusión", lastEventAt = System.currentTimeMillis() - 1000, source = "security"), onRetry = {})
}

@Preview(name = "Rojo fallo PIN")
@Composable private fun PreviewRedFail() {
    LedStatusCard(state = LedUiState(color = LedColor.RED, state = LedState.RED_FAIL, label = "Rojo fallo PIN", lastEventAt = System.currentTimeMillis() - 500, source = "access"), onRetry = {})
}

@Preview(name = "Apagado")
@Composable private fun PreviewOff() {
    LedStatusCard(state = LedUiState(color = LedColor.OFF, state = LedState.OFF, label = "Apagado", lastEventAt = System.currentTimeMillis() - 6000, source = "access"), onRetry = {})
}

@Preview(name = "Desconocido / Desconectado")
@Composable private fun PreviewUnknown() {
    LedStatusCard(state = LedUiState(color = LedColor.UNKNOWN, state = LedState.UNKNOWN, label = "Desconocido", error = "Network error"), onRetry = {})
}
