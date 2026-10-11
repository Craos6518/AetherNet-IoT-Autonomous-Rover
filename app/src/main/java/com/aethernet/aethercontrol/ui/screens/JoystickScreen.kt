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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.ui.viewmodel.JoystickViewModel
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

            // Card estado PWM — como `Rover L={t.left_pwm} R={t.right_pwm}` en DashboardScreen:113 pero para comando enviado
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
                    Text(
                        text = "L=${s.leftPwm} R=${s.rightPwm} mode=${s.mode} ${if (s.isActive) "● activo" else "○ reposo"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (s.isActive) Color(0xFF4CAF50) else Color.Gray
                    )
                    if (s.isSending) {
                        Text("Enviando…", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    s.error?.let {
                        Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Deadband ${com.aethernet.aethercontrol.domain.model.JoystickMapper.MIN_PWM} — throttle ${com.aethernet.aethercontrol.domain.model.JoystickMapper.THROTTLE_MS}ms (MOV-06 espejo rover-uno.ino:91)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Joystick Canvas — base 240dp (radio 120dp) + thumb 40dp
            // Como `<canvas width=240 height=240>` en HTML5 con `arc` y `pointer events` (detectDragGestures ≈ pointermove)
            val baseRadiusDp = 120.dp
            val thumbRadiusDp = 24.dp // thumb 48dp diámetro visual (24dp radio)
            val basePx = with(LocalDensity.current) { baseRadiusDp.toPx() } // radio base en px
            val thumbPx = with(LocalDensity.current) { thumbRadiusDp.toPx() }

            // Offset del thumb respecto al centro — derivado de s.x,s.y normalizados
            // x * basePx, -y * basePx (Y invertido porque Canvas Y+ abajo pero joystick Y+ adelante)
            val thumbOffsetX = s.x * basePx
            val thumbOffsetY = -s.y * basePx

            // Box contenedor 240dp — centra Canvas y maneja pointerInput
            // pointerInput detecta drag dentro del área; calcula delta desde centro, normaliza y llama vm
            var boxCenter by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .size(baseRadiusDp * 2)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // offset es posición del dedo dentro del Box (0..size). Centro = size/2
                                // boxCenter se setea en onSizeChanged; por ahora calculamos con size
                                val center = Offset(size.width / 2f, size.height / 2f)
                                boxCenter = center
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                // Normaliza en círculo con radio basePx, invertY true (Canvas Y-down → joystick Y-up)
                                val (nx, ny) = com.aethernet.aethercontrol.domain.model.JoystickMapper.normalizeInCircle(dx, dy, basePx, invertY = true)
                                vm.onPositionChanged(nx, ny)
                            },
                            onDragEnd = {
                                vm.onReleased()
                            },
                            onDragCancel = {
                                vm.onReleased()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val center = boxCenter
                                // Si aún no hay center (primera vez), usa size/2
                                val cx = if (center == Offset.Zero) size.width / 2f else center.x
                                val cy = if (center == Offset.Zero) size.height / 2f else center.y
                                val dx = change.position.x - cx
                                val dy = change.position.y - cy
                                val (nx, ny) = com.aethernet.aethercontrol.domain.model.JoystickMapper.normalizeInCircle(dx, dy, basePx, invertY = true)
                                vm.onPositionChanged(nx, ny)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    // Base círculo — como `ctx.arc(center.x, center.y, basePx, 0, 2*Math.PI)` en HTML5 Canvas
                    drawCircle(
                        color = Color(0xFFE0E0E0),
                        radius = basePx,
                        center = center
                    )
                    // Anillo activo si isActive — como `strokeStyle = isActive ? 'green' : 'gray'` en Canvas
                    drawCircle(
                        color = if (s.isActive) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                        radius = basePx,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                    // Ejes cruz — como líneas guía en joystick HTML5 (opcional visual)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(center.x - basePx, center.y),
                        end = Offset(center.x + basePx, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(center.x, center.y - basePx),
                        end = Offset(center.x, center.y + basePx),
                        strokeWidth = 1.dp.toPx()
                    )
                    // Thumb — como `ctx.arc(thumbX, thumbY, thumbPx, 0, 2*Math.PI)` con fill
                    val thumbCenter = Offset(center.x + thumbOffsetX, center.y + thumbOffsetY)
                    drawCircle(
                        color = if (s.isActive) Color(0xFF4CAF50) else Color(0xFF757575),
                        radius = thumbPx,
                        center = thumbCenter
                    )
                    // Brillo interno thumb — como highlight en CSS
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = thumbPx * 0.4f,
                        center = Offset(thumbCenter.x - thumbPx * 0.2f, thumbCenter.y - thumbPx * 0.2f)
                    )
                }
            }

            Text(
                "Centro = stop (0,0). Arriba = adelante (Y+), Izq/Der = giro tank. Suelta para frenar (fail-safe 500ms HU-04 rover-uno.ino:80).",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { vm.stop() }, modifier = Modifier.weight(1f)) {
                    Text("STOP (0,0)")
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Volver Dashboard")
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
