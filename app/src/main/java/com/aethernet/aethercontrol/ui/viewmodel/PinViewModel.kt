package com.aethernet.aethercontrol.ui.viewmodel

// =============================================================================
// PinViewModel.kt — ViewModel PIN Cerrojo | 6º Semestre UTP | MOV-04 HU-01, RF-2.2
// Autor: Andres Felipe Martinez Henao
// Experiencia: 2 años JS/React (form state), 2 años electrónica (keypad 4x4), 1 año C (throttle),
//              1 año PostgreSQL (pin_hash), 2 años Python
// Analogía React: este class es como `function usePinForm() { const [pinInput, setPinInput] = useState(""); const sendPin = async () => {...} }`
// en React con hooks — aquí con ViewModel + MutableStateFlow + PinValidator + throttle.
// Analogía Python: como `class PinViewModel` con `def on_pin_digit()` y `async def send_pin()` con validación y rate limit.
// Analogía C: espejo de firmware/mega-access/src/keypad_control.cpp:51 isDigit + :44 processPinAttempt — aquí con PinValidator.
// FOSS: ViewModel + Coroutines (Apache 2.0) — RNF-3.1.
// Origen: MOV-04 Plan B — S — HU-01 RF-2.2 — ViewModel dedicado pantalla PIN (no DashboardViewModel).
// Reusa AetherRepository.sendAccessCommand -> MqttManager.publishAccessCommand -> gateway aethernet/access/command:70
// Valida formato 4..6 dígitos espejo config.h:49 + throttle 5/60s + guard MQTT Connected.
// Observa accessEventFlow para confirmar success/fail (LedUiState ya pinta verde 5s en DashboardViewModel).
// =============================================================================

import android.util.Log // MOV-07 debug — filtrar con `adb logcat -s AetherBT`
import androidx.lifecycle.ViewModel // ViewModel — como `useState` + `useEffect` en React pero con ciclo vida
import androidx.lifecycle.viewModelScope // scope — como `useEffect` con cancel en unmount
import com.aethernet.aethercontrol.data.bluetooth.BluetoothConnectionState
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState // estado MQTT — como connectionState en WebSocket JS
import com.aethernet.aethercontrol.data.repository.AetherRepository // repo — como apiService en React
import com.aethernet.aethercontrol.domain.model.PinUiState // UiState PIN — como `type PinFormState` en TS
import com.aethernet.aethercontrol.domain.validator.PinValidator // validador — como `PinValidator` en JS (regex 4..6 dígitos)
import com.aethernet.aethercontrol.util.Result // Result — como {ok, data, error} en TS
import kotlinx.coroutines.Job // Job — handle coroutine (como setTimeout id en JS)
import kotlinx.coroutines.delay // delay — como `await sleep(ms)` en JS
import kotlinx.coroutines.flow.MutableStateFlow // MutableStateFlow — como useState pero reactivo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update // update { copy(...) } — como setState(prev => ({...prev, pinInput: next})) en React
import kotlinx.coroutines.launch

/**
 * MOV-04 Plan B — S — HU-01 RF-2.2 — ViewModel dedicado pantalla PIN.
 * Reusa AetherRepository.sendAccessCommand -> MqttManager.publishAccessCommand -> gateway aethernet/access/command:70
 * Valida formato 4..6 dígitos espejo config.h:49 PIN_MAX_LEN=6 + throttle 5/60s + guard MQTT Connected.
 * Observa accessEventFlow para confirmar success/fail (LedUiState ya pinta verde 5s en DashboardViewModel).
 * Si vienes de React: es como `function usePinForm(api) { const [pinInput, setPinInput] = useState(""); const [isValid, setIsValid] = useState(false); }`
 */
class PinViewModel(
    private val repo: AetherRepository // inyectado via ViewModelFactory — como props en React
) : ViewModel() {

    private val _pinState = MutableStateFlow(PinUiState()) // state interno — como `const [state, setState] = useState(PinUiState())` en React
    val pinState: StateFlow<PinUiState> = _pinState.asStateFlow() // público read-only — como `state` en React

    // Expone mqtt state para badge "MQTT ●" en PinScreen:138 — como `mqttState` en React props
    val mqttState: StateFlow<MqttConnectionState> get() = repo.mqttConnectionState // delega a repo — como `const mqttState = useMqttState()` en React

    // MOV-07: Bluetooth SPP fallback state
    val bluetoothConnectionState: StateFlow<BluetoothConnectionState> get() = repo.bluetoothConnectionState
    private val _isBluetoothMode = MutableStateFlow(false)
    val isBluetoothMode: StateFlow<Boolean> = _isBluetoothMode.asStateFlow()

    fun setBluetoothMode(enabled: Boolean) {
        _isBluetoothMode.value = enabled
        _pinState.update { it.copy(error = null) }
    }

    fun connectBluetooth(address: String? = null) {
        viewModelScope.launch {
            _pinState.update { it.copy(isLoading = true, error = null) }
            val res = repo.connectBluetooth(address)
            _pinState.update {
                it.copy(
                    isLoading = false,
                    error = if (res is Result.Error) res.msg else null,
                    lastMessage = if (res is Result.Success) "✓ Conectado a HC-06" else null
                )
            }
        }
    }

    fun disconnectBluetooth() {
        repo.disconnectBluetooth()
        _pinState.update { it.copy(lastMessage = "Bluetooth desconectado") }
    }

    private var accessJob: Job? = null // job colecta accessEventFlow — como `socket.on('access', handler)` handle
    private var btJob: Job? = null // job colecta bluetoothMessageFlow
    private var windowStartMs: Long = 0L // inicio ventana throttle 60s — como `windowStart` en rate limiter Express
    private var cooldownJob: Job? = null // job auto-limpia lastMessage tras 3s — como `setTimeout(() => clearMessage(), 3000)` en JS

    init {
        // Observa confirmación del Gateway/MEGA via MQTT — como `socket.on('access/event', ...)` en JS
        accessJob = viewModelScope.launch {
            repo.accessEventFlow.collect { ev ->
                if (!_isBluetoothMode.value) {
                    handleAccessConfirmation(ev.success)
                }
            }
        }
        // Observa mensajes entrantes del HC-06 via Bluetooth SPP (MOV-07 RF-1.3)
        btJob = viewModelScope.launch {
            repo.bluetoothMessageFlow.collect { msg ->
                Log.d("AetherBT", "VM RX: btMode=${_isBluetoothMode.value} msg=$msg") // MOV-07: confirma que el flow entrega a la UI
                if (_isBluetoothMode.value) {
                    val success = msg.contains("success\":true") || msg.contains("✓") || msg.contains("OK") || msg.contains("Desbloqueado")
                    handleAccessConfirmation(success)
                }
            }
        }
    }

    private fun handleAccessConfirmation(success: Boolean) {
        _pinState.update {
            it.copy(
                lastResultSuccess = success,
                lastMessage = if (success) "✓ Desbloqueado" else "✕ Denegado",
                error = null
            )
        }
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            delay(3000)
            _pinState.update { s -> if (s.lastMessage != null) s.copy(lastMessage = null, lastResultSuccess = null) else s }
        }
    }

    fun onPinDigit(d: String) {
        if (d.length != 1 || !d[0].isDigit()) return // solo dígito 0-9 — como `isDigit(key)` en C keypad_control.cpp:51
        val cur = _pinState.value.pinInput
        if (cur.length >= PinValidator.MAX_LEN) return // max 6 — como `if (len >= PIN_MAX_LEN) return` en C keypad_control.cpp:44
        val next = cur + d // concat — como `inputBuffer += key` en C
        _pinState.update {
            it.copy(
                pinInput = next,
                isValid = PinValidator.isValidFormat(next), // valida 4..6 dígitos — como `PinValidator.isValidFormat` en JS (regex)
                error = null // limpia error al teclear — como `setError(null)` en React onChange
            )
        }
    }

    fun onPinClear() {
        _pinState.update { it.copy(pinInput = "", isValid = false, error = null) } // limpia — como `inputBuffer = ""` en C keypad_control.cpp:48
    }

    fun onPinBackspace() {
        val cur = _pinState.value.pinInput
        if (cur.isEmpty()) return
        val next = cur.dropLast(1) // borra último — como `inputBuffer.dropLast(1)` en Kotlin (en C `inputBuffer.remove(len-1)`)
        _pinState.update {
            it.copy(
                pinInput = next,
                isValid = if (next.isEmpty()) false else PinValidator.isValidFormat(next), // valida si no vacío
                error = null
            )
        }
    }

    fun sendPin() {
        val s = _pinState.value
        if (s.isLoading) return // ya enviando — evita doble tap (como debounce en JS)
        val pin = s.pinInput

        // validación formato 4..6 dígitos — espejo MqttManager.publishAccessCommand:171 y config.h:49 PIN_MAX_LEN=6
        if (!PinValidator.isValidFormat(pin)) {
            _pinState.update { it.copy(error = "PIN 4-6 dígitos") } // error UI — como `setError("PIN 4-6 dígitos")` en React form
            return
        }

        val isBt = _isBluetoothMode.value
        if (isBt) {
            val btSt = repo.bluetoothConnectionState.value
            if (btSt !is BluetoothConnectionState.Connected) {
                _pinState.update { it.copy(error = "Bluetooth no conectado al HC-06") }
                return
            }
        } else {
            // guard MQTT — como `if (!socket.connected) return Error` en JS (ver AetherRepositoryImpl:86)
            val mqtt = repo.mqttConnectionState.value
            if (mqtt !is MqttConnectionState.Connected) {
                _pinState.update { it.copy(error = "MQTT no conectado — verifica Wi-Fi/broker") }
                return
            }
        }

        // throttle 5 intentos / 60s — como rate limit en Express `rateLimit({windowMs: 60000, max: 5})` (evita spam cerrojo)
        val now = System.currentTimeMillis()
        if (now - windowStartMs > 60_000) { // ventana expirada — reset (como `windowMs` en Express rate limit)
            windowStartMs = now
            _pinState.update { it.copy(attemptsInWindow = 0) }
        }
        if (_pinState.value.attemptsInWindow >= 5) {
            _pinState.update { it.copy(error = "Demasiados intentos, espera 60s") } // throttle — como `429 Too Many Requests` en HTTP
            return
        }

        viewModelScope.launch {
            _pinState.update { it.copy(isLoading = true, error = null, lastMessage = null, lastResultSuccess = null) } // loading — como `setIsLoading(true)` en React
            val r = if (isBt) repo.sendBluetoothAccessCommand(pin) else repo.sendAccessCommand(pin)
            when (r) {
                is Result.Success -> {
                    _pinState.update {
                        it.copy(
                            isLoading = false,
                            pinInput = "", // limpia input tras envío — como `inputBuffer = ""` en C tras processPinAttempt
                            isValid = false,
                            attemptsInWindow = it.attemptsInWindow + 1, // incrementa contador throttle
                            lastMessage = if (isBt) "Enviado por Bluetooth SPP…" else "Enviado, esperando confirmación…" // feedback
                        )
                    }
                }
                is Result.Error -> {
                    _pinState.update {
                        it.copy(
                            isLoading = false,
                            error = r.msg // error publish — como `setError(r.msg)` en React (ej. "MQTT no conectado")
                        )
                    }
                }
                is Result.Loading -> {
                    _pinState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    override fun onCleared() {
        accessJob?.cancel() // cancela colecta access — como `socket.off('access')` en JS cleanup
        btJob?.cancel()
        cooldownJob?.cancel() // cancela auto-limpia — como `clearTimeout(id)` en JS
        super.onCleared() // super — como `useEffect return () => cleanup` en React
    }
}
