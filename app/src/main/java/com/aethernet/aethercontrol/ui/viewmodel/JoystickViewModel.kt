package com.aethernet.aethercontrol.ui.viewmodel

// =============================================================================
// JoystickViewModel.kt — ViewModel Joystick Virtual | 6º Semestre UTP | MOV-05 RF-1.2, MOV-06
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (joystick state, throttle), 2 años electrónica (L298N, nRF24L01, rover-uno.ino:70 MIN_PWM),
//              2 años Python (EMA), 1 año C (RoverCommand)
// Analogía React: este class es como `function useJoystick(api) { const [pos, setPos] = useState({x:0,y:0}); const send = throttle((x,y)=>api.publish({left,right}), 50) }`
// en React con hooks + throttle (como lodash.throttle) — aquí con ViewModel + StateFlow + 50ms guard.
// Analogía C: espejo de `RoverCommand {left_pwm,right_pwm,mode}` en rover-uno.ino:106 pero con throttle 50ms para no saturar RF.
// Analogía Python: como `class JoystickViewModel` con `def on_position(x,y): if now-last<0.05: return; await mqtt.publish(...)` en asyncio.
// FOSS: ViewModel + Coroutines (Apache 2.0) — RNF-3.1.
// Origen: MOV-05 RF-1.2 (captura X,Y -1..1 → left/right -255..255 tank-steering) + MOV-06 throttle 50ms + deadband 60.
// Flujo: JoystickScreen drag → onPositionChanged(x,y) → JoystickMapper.vectorToPwm → repo.sendRoverCommand → MqttManager.publishRoverCommand
//        → aethernet/rover/command:69 → Gateway handleRoverCommand:303 → radio.write → rover-uno.ino:219 handleRfCommands → setMotorSpeeds.
// Latencia objetivo: <50ms MQTT prd.md:49 / <10ms RF prd.md:50 — throttle 50ms no añade lag perceptible, evita 100Hz flood.
// =============================================================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.domain.model.JoystickMapper
import com.aethernet.aethercontrol.domain.model.JoystickUiState
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MOV-05 RF-1.2 + MOV-06 — ViewModel joystick virtual.
 * Captura vectores X,Y normalizados -1..1 (JoystickScreen Canvas) → tank-steering PWM → MQTT aethernet/rover/command.
 * Throttle 50ms (MOV-06) evita saturar broker/RF: si llegan 100 eventos/s del drag, solo envía 20/s.
 * Deadband 60 espejo rover-uno.ino:91 MIN_PWM_FOR_MOVEMENT — cog en ViewModel + firmware (doble guard).
 * Si vienes de React: es como `useJoystick` hook con `throttle(send, 50)` y `useState(pos)`.
 */
class JoystickViewModel(
    private val repo: AetherRepository,
    private val throttleMs: Long = JoystickMapper.THROTTLE_MS // 50ms MOV-06 — inyectable para tests (0 = sin throttle)
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoystickUiState())
    val uiState: StateFlow<JoystickUiState> = _uiState.asStateFlow()

    // Expone mqtt state para badge "MQTT ●" en JoystickScreen — como `mqttState` en PinViewModel
    val mqttState: StateFlow<MqttConnectionState> get() = repo.mqttConnectionState

    private var lastSendMs: Long = 0L
    private var sendJob: Job? = null

    /**
     * Llamado por JoystickScreen en drag/move con x,y ya normalizados -1..1 (invertY ya aplicado).
     * Actualiza UI inmediato (x,y,leftPwm,rightPwm) y dispara publish throttled.
     * Si throttleMs=0 (tests), envía cada vez; si 50ms, ignora si pasaron <50ms (excepto stop 0,0 que siempre envía).
     */
    fun onPositionChanged(x: Float, y: Float) {
        val cx = x.coerceIn(-1f, 1f)
        val cy = y.coerceIn(-1f, 1f)
        val (left, right) = JoystickMapper.vectorToPwm(cx, cy, applyDeadband = true)
        val mode = if (left == 0 && right == 0) 0 else 1
        val active = !(cx == 0f && cy == 0f)
        _uiState.update {
            it.copy(x = cx, y = cy, leftPwm = left, rightPwm = right, mode = mode, isActive = active, error = null)
        }
        throttledSend(left, right, mode, force = !active) // force true si stop (0,0) — no throttlear el stop (fail-safe inmediato)
    }

    /**
     * Llamado al soltar joystick (drag end) — centra a 0,0 y envía stop inmedito (mode 0).
     * Fuerza envío sin throttle para fail-stop rápido (rover debe detenerse ya, no en 50ms).
     */
    fun onReleased() {
        _uiState.update { it.copy(x = 0f, y = 0f, leftPwm = 0, rightPwm = 0, mode = 0, isActive = false) }
        throttledSend(0, 0, 0, force = true)
    }

    /**
     * Alias para botonera STOP — como onReleased pero explícito.
     */
    fun stop() = onReleased()

    private fun throttledSend(left: Int, right: Int, mode: Int, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && throttleMs > 0 && now - lastSendMs < throttleMs) {
            // throttled — no envía, solo actualiza UI (ya hecho). Próximo drag >50ms sí enviará.
            return
        }
        lastSendMs = now
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            when (val r = repo.sendRoverCommand(left, right, mode)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSending = false, error = null) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSending = false, error = r.msg) }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isSending = true) }
                }
            }
        }
    }

    override fun onCleared() {
        sendJob?.cancel()
        super.onCleared()
        // Fail-stop al salir: envía 0,0 por si ViewModel se destruye con joystick activo (seguridad)
        // No bloquea porque onCleared no puede ser suspend; se pierde si app muere, pero rover tiene fail-safe 500ms HU-04
    }
}
