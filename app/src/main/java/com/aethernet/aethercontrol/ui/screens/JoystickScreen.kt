package com.aethernet.aethercontrol.ui.screens

// =============================================================================
// JoystickScreen.kt — Pantalla Joystick Virtual | 6º Semestre UTP | MOV-05 RF-1.2, MOV-06
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (Canvas joystick, gamepad API), 2 años electrónica (L298N, nRF24L01, rover-uno.ino:316),
//              1 año C (RoverCommand), 2 años HTML/CSS (Compose Canvas ≈ HTML5 Canvas)
// Analogía React: este @Composable es como `function JoystickScreen({ vm }) { const {x,y,leftPwm,rightPwm} = useSelector(vm.uiState); return <Canvas onPointerMove={e=>vm.onPositionChanged(nx,ny)} /> }`
// en React con `<canvas>` + `pointer events` — aquí con Compose Canvas + pointerInput + detectDragGestures.
// Analogía HTML5 Canvas: `Canvas(modifier.pointerInput{detectDragGestures(...) { drawCircle(...) } })` ≈ `canvas.addEventListener('pointermove', handler)` + `ctx.arc()`.
// FOSS: Compose Material3 + Canvas (AndroidX, Apache 2.0) — RNF-3.1.
// Origen: MOV-05 RF-1.2 — Joystick virtual Compose Canvas captura vectores X,Y normalizados -1..1 → JoystickMapper → PWM tank-steering.
// MOV-06 throttle 50ms + deadband 60 ya en JoystickViewModel/JoystickMapper (no aquí) — aquí solo pinta y normaliza drag.
// Flujo: drag en círculo base 120dp → normalizeInCircle (radius) → vm.onPositionChanged(x,y) → repo.sendRoverCommand → MQTT aethernet/rover/command:69
//        → Gateway handleRoverCommand:303 → radio.write → rover-uno.ino:219 handleRfCommands → rover-uno.ino:364 deadband + setMotorSpeeds.
// Fail-safe: onReleased envía 0,0 mode 0 inmediato; rover también hace fail-stop 500ms HU-04 si RF se pierde.
// =============================================================================

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.core.di.ServiceLocator
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.ui.viewmodel.JoystickViewModel
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * MOV-05 RF-1.2 + MOV-06 — Pantalla joystick virtual.
 * Círculo base 240dp (radio 120dp) + thumb 40dp arrastrable. Fundamento: pointerInput + Canvas ≈ HTML5 Canvas joystick.
 * Vectores X,Y normalizados -1..1 se calculan con (delta / radius) y clamp círculo (JoystickViewModel también hace mapper).
 * Si vienes de React: es como `<Joystick onMove={(x,y)=>vm.onPositionChanged(x,y)} onEnd={()=>vm.onReleased()} />` en TS.
 */
@Composable
fun JoystickScreen(
    vm: JoystickViewModel,
    onBack: () -> Unit
) {
    val s by vm.uiState.collectAsStateWithLifecycle()
    val mqtt by vm.mqttState.collectAsStateWithLifecycle()

    // Cap velocidad Ajustes → Rover (MOV-08): observa DataStore y lo empuja al VM en vivo
    val prefs = remember { runCatching { ServiceLocator.preferencesManager }.getOrNull() }
    val speedCap: Int = if (prefs != null) {
        val c by prefs.maxSpeedPct.collectAsStateWithLifecycle(initialValue = 100)
        c
    } else 100
    LaunchedEffect(speedCap) { vm.setMaxSpeedPct(speedCap) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Joystick Rover", style = MaterialTheme.typography.titleLarge)
            Text(
                "Arrastra el círculo para mover el Rover (X,Y → PWM tank-steering)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Badge MQTT — como en PinScreen:138
            Text(
                text = when (mqtt) {
                    is MqttConnectionState.Connected -> "MQTT ● ${(mqtt as MqttConnectionState.Connected).broker}"
                    is MqttConnectionState.Connecting -> "MQTT ○ Conectando…"
                    is MqttConnectionState.Error -> "MQTT ✕ ${(mqtt as MqttConnectionState.Error).msg}"
                    is MqttConnectionState.Disconnected -> "MQTT - Desconectado"
                },
                color = if (mqtt is MqttConnectionState.Connected) Color(0xFF4CAF50) else Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            // Card estado X,Y — coordenadas joystick
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "X=${String.format("%.2f", s.x)} Y=${String.format("%.2f", s.y)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Badge modo actual — RF-1.2/RF-3.2: 0=STOP 1=MANUAL 2=AUTO (+ enclavamiento emergencia HU-04)
            Text(
                text = "Modo: " + when {
                    s.emergencyLatched -> "⛔ EMERGENCIA (bloqueado)"
                    s.mode == 2 -> "AUTO (evasión HC-SR04 + TCRT)"
                    s.mode == 1 -> "MANUAL (joystick)"
                    else -> "STOP (detenido)"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    s.emergencyLatched -> Color(0xFFBA1A1A)
                    s.mode == 2 -> Color(0xFF1976D2)
                    s.mode == 1 -> Color(0xFF4CAF50)
                    else -> Color.Gray
                }
            )

            // Banner emergencia enclavada — requiere reset deliberado MANUAL/AUTO (seguridad)
            if (s.emergencyLatched) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDAD6))
                ) {
                    Text(
                        text = "⛔ PARADA DE EMERGENCIA ACTIVA — joystick bloqueado. Pulsa MANUAL (o AUTO) para reanudar.",
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFBA1A1A),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Joystick Canvas — Stitch dimensions: base 192dp (radio 96dp) + thumb 80dp (radio 40dp)
            // Como `<canvas width=192 height=192>` en HTML5 con `arc` y `pointer events`
            val baseSize = 192.dp
            val thumbSize = 80.dp
            val baseRadiusPx = with(LocalDensity.current) { (baseSize / 2).toPx() } // radio base 96dp en px
            val thumbRadiusPx = with(LocalDensity.current) { (thumbSize / 2).toPx() } // radio thumb 40dp en px
            val maxThumbTravelPx = baseRadiusPx - thumbRadiusPx

            // Estado local del thumb para renderizado inmediato (anti-vibración)
            var thumbOffset by remember { mutableStateOf(Offset.Zero) }

            // Reset visual al soltar o STOP
            if (!s.isActive && thumbOffset != Offset.Zero) {
                thumbOffset = Offset.Zero
            }

            // Box contenedor 192dp — centra Canvas y maneja pointerInput
            // pointerInput detecta drag dentro del área; calcula delta desde centro, normaliza y llama vm

            Box(
                modifier = Modifier
                    .size(baseSize)
                    .pointerInput(s.emergencyLatched) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (s.emergencyLatched) return@detectDragGestures
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                val (nx, ny) = com.aethernet.aethercontrol.domain.model.JoystickMapper.normalizeInCircle(dx, dy, baseRadiusPx, invertY = true)
                                thumbOffset = Offset(nx * maxThumbTravelPx, -ny * maxThumbTravelPx)
                                vm.onPositionChanged(nx, ny)
                            },
                            onDragEnd = {
                                thumbOffset = Offset.Zero
                                vm.onReleased()
                            },
                            onDragCancel = {
                                thumbOffset = Offset.Zero
                                vm.onReleased()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (s.emergencyLatched) return@detectDragGestures
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                val (nx, ny) = com.aethernet.aethercontrol.domain.model.JoystickMapper.normalizeInCircle(dx, dy, baseRadiusPx, invertY = true)
                                thumbOffset = Offset(nx * maxThumbTravelPx, -ny * maxThumbTravelPx)
                                vm.onPositionChanged(nx, ny)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(baseSize)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = baseRadiusPx
                    // Base background
                    drawCircle(color = Color(0xFFE0E0E0), radius = radius, center = center)
                    // Active ring
                    drawCircle(
                        color = if (s.isActive) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                        radius = radius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                    // Directional indicators (N/S/E/W)
                    val indicatorColor = Color.Gray.copy(alpha = 0.3f)
                    val indW = 1.dp.toPx()
                    val indH = 8.dp.toPx()
                    val indOffset = 16.dp.toPx()
                    drawRect(color = indicatorColor, topLeft = Offset(center.x - indW / 2, center.y - radius + indOffset), size = Size(indW, indH))
                    drawRect(color = indicatorColor, topLeft = Offset(center.x - indW / 2, center.y + radius - indOffset - indH), size = Size(indW, indH))
                    drawRect(color = indicatorColor, topLeft = Offset(center.x - radius + indOffset, center.y - indH / 2), size = Size(indH, indW))
                    drawRect(color = indicatorColor, topLeft = Offset(center.x + radius - indOffset - indH, center.y - indH / 2), size = Size(indH, indW))
                    // Thumb at local thumbOffset
                    val thumbCenter = Offset(center.x + thumbOffset.x, center.y + thumbOffset.y)
                    drawCircle(
                        color = if (s.isActive) Color(0xFF4CAF50) else Color(0xFF757575),
                        radius = thumbRadiusPx,
                        center = thumbCenter
                    )
                    // Inner highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = thumbRadiusPx * 0.4f,
                        center = Offset(thumbCenter.x - thumbRadiusPx * 0.2f, thumbCenter.y - thumbRadiusPx * 0.2f)
                    )
                }
            }

            Text(
                "Centro = stop (0,0). Arriba = adelante (Y+), Izq/Der = giro tank. Suelta para frenar (fail-safe 500ms HU-04 rover-uno.ino:80).",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Botonera modos RF-1.2/RF-3.2 — AUTO=2 (autónomo UNO), MANUAL=1 (joystick), PARAR=0
            // maxLines=1 + 12sp: en 3 botones weight(1f) "MANUAL" a 14sp se recorta ("MANUA")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { vm.setMode(2) },
                    modifier = Modifier.weight(1f),
                    enabled = !s.emergencyLatched,
                    colors = if (s.mode == 2 && !s.emergencyLatched)
                        androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                    else androidx.compose.material3.ButtonDefaults.buttonColors()
                ) {
                    Text("AUTO", maxLines = 1, fontSize = 12.sp)
                }
                Button(
                    onClick = { vm.setMode(1) },
                    modifier = Modifier.weight(1f),
                    colors = if (s.mode == 1 && !s.emergencyLatched)
                        androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    else androidx.compose.material3.ButtonDefaults.buttonColors()
                ) {
                    Text("MANUAL", maxLines = 1, fontSize = 12.sp)
                }
                OutlinedButton(onClick = { vm.stop() }, modifier = Modifier.weight(1f)) {
                    Text("PARAR", maxLines = 1, fontSize = 12.sp)
                }
            }

// Emergency button — HU-04: stop inmediato + enclavamiento (bloquea drag hasta MANUAL/AUTO)
            Button(
                onClick = { vm.emergencyStop() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(vertical = 12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBA1A1A),
                    contentColor = Color(0xFFFFFFFF)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠", style = MaterialTheme.typography.titleLarge)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(
                        "PARADA EMERGENCIA"
                    )
                }
            }

            Text(
                "Topic aethernet/rover/command:69 JSON {left_pwm,right_pwm,mode} → Gateway handleRoverCommand:303 → nRF24L01 RF → rover-uno.ino:219 (Tank L298N ENA5/IN6/IN7 ENB11). Latencia <50ms MQTT / <10ms RF (prd.md:49-50).",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
