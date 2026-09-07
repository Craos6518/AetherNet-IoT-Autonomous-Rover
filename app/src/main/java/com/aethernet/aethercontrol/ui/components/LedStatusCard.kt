package com.aethernet.aethercontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aethernet.aethercontrol.domain.model.LedColor
import com.aethernet.aethercontrol.domain.model.LedState
import com.aethernet.aethercontrol.domain.model.LedUiState
import kotlinx.coroutines.delay

/**
 * LedStatusCard — MOV-02 (RF-1.1 HU-01/HU-02 solo lectura).
 * Wireframe: Card LED [círculo 64dp color + texto estado + timestamp relativo]
 * Colores: 0xFF4CAF50 verde, 0xFFF44336 rojo, 0xFF9E9E9E off, Color.Red error / UNKNOWN.
 * No envía comandos (solo lectura). Reintentar delega a ViewModel.refreshLedState().
 */
@Composable
fun LedStatusCard(
    state: LedUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val circleColor = when (state.color) {
        LedColor.GREEN -> Color(0xFF4CAF50)
        LedColor.RED -> Color(0xFFF44336)
        LedColor.OFF -> Color(0xFF9E9E9E)
        LedColor.UNKNOWN -> Color(0xFFBDBDBD)
    }

    // Timestamp relativo "hace Xs / Xm" que tickea cada segundo
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.lastEventAt) {
        while (true) {
            delay(1000)
            nowTick = System.currentTimeMillis()
        }
    }
    val relative = state.lastEventAt?.let { ts ->
        val deltaSec = ((nowTick - ts) / 1000).coerceAtLeast(0)
        when {
            deltaSec < 60 -> "hace ${deltaSec}s"
            deltaSec < 3600 -> "hace ${deltaSec / 60}m ${deltaSec % 60}s"
            else -> "hace ${deltaSec / 3600}h"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "LED Local", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = state.label, style = MaterialTheme.typography.titleSmall)
                    state.source?.let { src ->
                        Text(text = "Origen: $src", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    relative?.let { rel ->
                        Text(text = rel, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (state.lastEventAt == null && state.state == LedState.UNKNOWN) {
                        Text(text = "Sin eventos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            state.error?.let { err ->
                Text(text = err, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) { Text("Reintentar LED") }
            }
        }
    }
}

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
