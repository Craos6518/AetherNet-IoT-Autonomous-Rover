package com.aethernet.aethercontrol.ui.viewmodel

// =============================================================================
// BluetoothViewModel.kt — ViewModel fallback Bluetooth SPP | 6º Semestre UTP | MOV-07 RF-1.3
// Autor: Andres Felipe Martinez Henao
// Analogía React: este class es como `function useBluetooth() { const [devices, setDevices] = useState([]); const connect = async (addr) => {...} }`
// — aquí con ViewModel + BluetoothManager (data/bluetooth/BluetoothManager.kt).
// Analogía C: espejo "app-side" del firmware nano-access-compacto (CMD:ACCESS + POLL:PRESENCE).
// FOSS: ViewModel + Coroutines (Apache 2.0) — RNF-3.1.
// Origen: MOV-07 RF-1.3 — contingencia BT cuando cae el Wi-Fi: pantalla "Bluetooth Fallback".
// Flujo: BluetoothScreen pide permiso BLUETOOTH_CONNECT (API 31+) -> loadDevices() ->
// connect(address) -> sendPin() -> Nano responde ACCESS:{success} (flow -> lastResultSuccess).
// Presencia: job sondea POLL:PRESENCE cada 1s mientras Connected (como poll 5s de Dashboard).
// =============================================================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethernet.aethercontrol.data.bluetooth.BluetoothManager
import com.aethernet.aethercontrol.data.bluetooth.BtConnectionState
import com.aethernet.aethercontrol.domain.validator.PinValidator // mismo validator que PIN MQTT (PinViewModel:24)
import com.aethernet.aethercontrol.domain.model.BluetoothUiState
import com.aethernet.aethercontrol.util.Result // Result — como {ok, error} en TS
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay // delay(1000) — poll presencia como poll 5s de DashboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MOV-07 — ViewModel del fallback Bluetooth SPP (RF-1.3, Nodo Acceso Compacto HC-06).
 * Delega en BluetoothManager (like PinViewModel -> AetherRepository). Reusa PinValidator
 * para 4..6 dígitos. Sondea presencia FC-51 cada 1s sólo mientras BT Connected.
 */
class BluetoothViewModel(
    private val bt: BluetoothManager // inyectado via BluetoothViewModelFactory (ViewModelFactory.kt)
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null // job sondeo presencia — como `setInterval(1s)` en JS
    private var cooldownJob: Job? = null // auto-limpia lastMessage tras 3s — como setTimeout

    init {
        // Observa confirmación del Nodo: ACCESS:{success} — como socket.on('ACCESS', cb) en JS
        viewModelScope.launch {
            bt.accessEvents.collect { ev ->
                _uiState.update {
                    it.copy(
                        lastResultSuccess = ev.success,
                        lastMessage = if (ev.success) "✓ Desbloqueado (HC-06)" else "✕ Denegado",
                        error = null
                    )
                }
                // auto-limpia mensaje — como setTimeout en PinViewModel:69
                cooldownJob?.cancel()
                cooldownJob = viewModelScope.launch {
                    delay(3000); _uiState.update { s -> if (s.lastMessage != null) s.copy(lastMessage = null, lastResultSuccess = null) else s }
                }
            }
        }
        // Observa presencia FC-51 (push del Nano) — como `socket.on('presence', ...)`
        viewModelScope.launch {
            bt.presence.collect { p -> _uiState.update { it.copy(presence = p) } }
        }
        // Gestiona sondeo POLL:PRESENCE: arranca al conectar, para al desconectar
        viewModelScope.launch {
            bt.connectionState.collect { state ->
                _uiState.update { it.copy(connection = state) }
                when (state) {
                    is BtConnectionState.Connected -> {
                        pollJob?.cancel()
                        pollJob = viewModelScope.launch {
                            while (true) { bt.requestPresence(); delay(1000) } // 1 Hz — como poll 5s
                        }
                    }
                    else -> pollJob?.cancel()
                }
            }
        }
    }

    /** true si el dispositivo tiene adaptador Bluetooth (bluetoothManager.isSupported()). */
    fun btSupported(): Boolean = bt.isSupported()

    /** Recarga dispositivos emparejados (llamar DESPUÉS de conceder BLUETOOTH_CONNECT). */
    fun loadDevices() {
        _uiState.update { it.copy(devices = bt.listPaired(), error = null) }
    }

    fun connect(address: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = bt.connect(address)) {
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = r.msg) }
                else -> _uiState.update { it.copy(isLoading = false, error = null) }
            }
        }
    }

    fun disconnect() {
        bt.disconnect()
        _uiState.update { it.copy(presence = null, lastMessage = null, lastResultSuccess = null) }
    }

    fun onPinDigit(d: String) {
        if (d.length != 1 || !d[0].isDigit()) return // solo 0-9 — como keypad_control.cpp:51
        val cur = _uiState.value.pinInput
        if (cur.length >= PinValidator.MAX_LEN) return // max 6
        _uiState.update {
            it.copy(pinInput = cur + d, isValid = PinValidator.isValidFormat(cur + d), error = null)
        }
    }

    fun onPinBackspace() {
        val cur = _uiState.value.pinInput
        if (cur.isEmpty()) return
        val next = cur.dropLast(1)
        _uiState.update { it.copy(pinInput = next, isValid = if (next.isEmpty()) false else PinValidator.isValidFormat(next), error = null) }
    }

    fun onPinClear() {
        _uiState.update { it.copy(pinInput = "", isValid = false, error = null) }
    }

    fun sendPin() {
        val s = _uiState.value
        if (s.isLoading) return // evita doble tap
        val pin = s.pinInput
        if (!PinValidator.isValidFormat(pin)) { // 4..6 dígitos — espejo firmware PIN_MAX_LEN
            _uiState.update { it.copy(error = "PIN 4-6 dígitos") }
            return
        }
        if (s.connection !is BtConnectionState.Connected) { // guard BT — como guard MQTT en PinViewModel:120
            _uiState.update { it.copy(error = "Bluetooth no conectado — conecta al HC-06") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, lastMessage = null, lastResultSuccess = null) }
            when (val r = bt.sendAccessCommand(pin)) { // CMD:ACCESS:{"pin":"1234"} -> Nano
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, pinInput = "", isValid = false, lastMessage = "Enviado, esperando confirmación…")
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = r.msg) }
                is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    override fun onCleared() {
        bt.disconnect() // cierra socket al salir — como WebSocket.close() en React cleanup
        pollJob?.cancel()
        cooldownJob?.cancel()
        super.onCleared()
    }
}