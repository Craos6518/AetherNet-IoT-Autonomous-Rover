package com.aethernet.aethercontrol.ui.viewmodel

// =============================================================================
// SettingsViewModel.kt — ViewModel Ajustes funcional | 6º Semestre UTP | MOV-08/MOV-09
// Autor: Andres Felipe Martinez Henao
// Secciones: RED (URL backend + broker derivado + reconexión MQTT), Bluetooth (HC-06 live),
// Rover (velocidad máx aplica al joystick; fail-safe solo informativo firmware 500ms HU-04),
// Tema (CLARO/OSCURO/SISTEMA, aplica en vivo en MainActivity).
// Diálogos: logs MQTT (estado + topics) y versiones (app + backend + firmware doc).
// FOSS: ViewModel + Coroutines + DataStore (AndroidX, Apache 2.0) — RNF-3.1.
// =============================================================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethernet.aethercontrol.core.di.ServiceLocator
import com.aethernet.aethercontrol.data.bluetooth.BluetoothManager
import com.aethernet.aethercontrol.data.bluetooth.BtConnectionState
import com.aethernet.aethercontrol.data.local.PreferencesManager
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Normaliza URL de backend (pura, testeable): añade http:// y "/" final.
 * Espejo de ServiceLocator.normalizeUrl — duplicada aquí para no exponerlo y poder testearla.
 * Ej: "192.168.1.50:8000" -> "http://192.168.1.50:8000/".
 */
fun normalizeBackendUrl(raw: String): String {
    var u = raw.trim()
    require(u.isNotBlank()) { "URL vacía" }
    if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
    if (!u.endsWith("/")) u += "/"
    return u
}

/**
 * Deriva etiqueta de broker tcp del backend (pura, testeable).
 * Espejo de MqttManager.getBrokerHostFromHttpUrl — el broker Mosquitto corre en el
 * mismo host que el backend (docker-compose.yml), por eso la URL del backend manda.
 * Ej: "http://192.168.1.14:8000/" -> "tcp://192.168.1.14:1883".
 */
fun deriveBrokerLabel(httpUrl: String): String {
    val host = httpUrl
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore(":")
        .substringBefore("/")
        .trim()
        .ifBlank { "10.0.2.2" }
    return "tcp://$host:1883"
}

/**
 * MOV-08/MOV-09 — ViewModel Ajustes funcional.
 * RED ahora edita la URL del backend (la que realmente mueve HTTP + deriva el broker MQTT),
 * no un campo broker muerto: "Guardar y reconectar" aplica + reconecta MQTT + verifica /health.
 * @param bt canal BT para estado live (nullable = tests sin BT).
 * @param prefs DataStore (nullable = tests sin Android; se resuelve vía ServiceLocator si es null).
 */
class SettingsViewModel(
    private val repo: AetherRepository,
    private val bt: BluetoothManager? = null,
    prefs: PreferencesManager? = null
) : ViewModel() {

    private val prefs: PreferencesManager? =
        prefs ?: runCatching { ServiceLocator.preferencesManager }.getOrNull()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val mqttState: StateFlow<MqttConnectionState> get() = repo.mqttConnectionState

    // Estado BT live (Disconnected si no hay manager) — para la sección Bluetooth en vivo
    private val _btState: StateFlow<BtConnectionState> =
        bt?.connectionState ?: MutableStateFlow<BtConnectionState>(BtConnectionState.Disconnected)
    val btState: StateFlow<BtConnectionState> get() = _btState

    init {
        // Prefill con la URL en uso (memoria, cargada en Application.onCreate) — si falla, DataStore directo
        val current = runCatching { ServiceLocator.getCurrentBaseUrl() }.getOrNull()
        if (current != null) _uiState.update { it.copy(backendUrl = current) }
        viewModelScope.launch {
            val p = prefs ?: return@launch
            _uiState.update {
                it.copy(
                    backendUrl = runCatching { p.getBaseUrlOnce() }.getOrDefault(current ?: ""),
                    themeMode = runCatching { ThemeMode.valueOf(p.getThemeModeOnce()) }
                        .getOrDefault(ThemeMode.SISTEMA),
                    maxSpeedPct = runCatching { p.getMaxSpeedPctOnce() }.getOrDefault(85)
                )
            }
        }
    }

    // ===== RED: URL backend (mueve HTTP + broker MQTT derivado) =====

    fun onBackendUrlChanged(url: String) {
        _uiState.update { it.copy(backendUrl = url, applySuccess = null, applyError = null) }
    }

    /**
     * Guarda la URL, recrea Retrofit, reconecta MQTT y verifica /health.
     * Como "Save & Apply" que además hace reconnect + health-check (no solo persiste).
     */
    fun onSaveAndReconnect() {
        val raw = _uiState.value.backendUrl.trim()
        if (raw.isBlank()) {
            _uiState.update { it.copy(applyError = "URL vacía") }
            return
        }
        _uiState.update { it.copy(isApplying = true, applyError = null, applySuccess = null) }
        viewModelScope.launch {
            val normalized = try {
                normalizeBackendUrl(raw)
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(isApplying = false, applyError = e.message) }
                return@launch
            }
            // 1) Aplica URL (persiste DataStore + recrea Retrofit)
            val applied = try {
                ServiceLocator.updateBaseUrl(normalized)
            } catch (e: Exception) {
                _uiState.update { it.copy(isApplying = false, applyError = e.message ?: "No se pudo guardar") }
                return@launch
            }
            _uiState.update { it.copy(backendUrl = applied) }
            // 2) Reconecta MQTT sobre el host nuevo (disconnect primero: connect() no reconecta si sigue vivo)
            repo.disconnectMqtt()
            repo.connectMqtt(applied)
            // 3) Verifica backend /health para feedback real
            when (val h = repo.getHealth()) {
                is Result.Success -> _uiState.update {
                    it.copy(isApplying = false, applySuccess = "Backend ${h.data.status} v${h.data.version} — MQTT reconectando a ${deriveBrokerLabel(applied)}")
                }
                is Result.Error -> _uiState.update {
                    it.copy(isApplying = false, applySuccess = "URL guardada — MQTT reconectando; backend sin respuesta: ${h.msg}")
                }
                is Result.Loading -> _uiState.update { it.copy(isApplying = true) }
            }
        }
    }

    /** Prueba diagnóstica del broker derivado (cliente temporal, no cambia la conexión activa). */
    fun onTestMqttConnection() {
        val broker = deriveBrokerLabel(_uiState.value.backendUrl.ifBlank { "http://10.0.2.2:8000/" })
        _uiState.update { it.copy(isTestingMqtt = true, mqttTestError = null, mqttTestSuccess = false) }
        viewModelScope.launch {
            when (val r = repo.testMqttConnection(broker)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isTestingMqtt = false, mqttTestSuccess = true, mqttTestError = null) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isTestingMqtt = false, mqttTestSuccess = false, mqttTestError = r.msg) }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isTestingMqtt = true) }
                }
            }
        }
    }

    // ===== Rover: velocidad máx (aplica al joystick en vivo vía JoystickViewModel.setMaxSpeedPct) =====

    fun onMaxSpeedPctChanged(pct: Int) {
        val v = pct.coerceIn(0, 100)
        _uiState.update { it.copy(maxSpeedPct = v) }
        viewModelScope.launch {
            runCatching { prefs?.saveMaxSpeedPct(v) }
        }
    }

    // ===== Tema (aplica en vivo en MainActivity observando DataStore) =====

    fun onThemeModeChanged(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            runCatching { prefs?.saveThemeMode(mode.name) }
        }
    }

    // ===== Diálogos: logs MQTT + versiones =====

    fun onOpenMqttLogs() {
        _uiState.update { it.copy(showMqttLogs = true) }
    }

    fun onCloseMqttLogs() {
        _uiState.update { it.copy(showMqttLogs = false) }
    }

    /** Abre diálogo de versiones y trae la versión del backend en vivo (/health). */
    fun onOpenFwInfo() {
        _uiState.update { it.copy(showFwInfo = true, fwBackendVersion = null, fwError = null, isLoadingFw = true) }
        viewModelScope.launch {
            when (val h = repo.getHealth()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoadingFw = false, fwBackendVersion = "${h.data.status} v${h.data.version} (db ${h.data.database})")
                }
                is Result.Error -> _uiState.update { it.copy(isLoadingFw = false, fwError = h.msg) }
                is Result.Loading -> _uiState.update { it.copy(isLoadingFw = true) }
            }
        }
    }

    fun onCloseFwInfo() {
        _uiState.update { it.copy(showFwInfo = false) }
    }
}

/** MOV-08 — Estado UI Ajustes. Fail-safe es solo informativo (firmware 500ms HU-04, no editable desde app). */
data class SettingsUiState(
    val backendUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SISTEMA,
    val maxSpeedPct: Int = 85,
    val isApplying: Boolean = false,
    val applySuccess: String? = null,
    val applyError: String? = null,
    val isTestingMqtt: Boolean = false,
    val mqttTestSuccess: Boolean = false,
    val mqttTestError: String? = null,
    val showMqttLogs: Boolean = false,
    val showFwInfo: Boolean = false,
    val isLoadingFw: Boolean = false,
    val fwBackendVersion: String? = null,
    val fwError: String? = null
)

enum class ThemeMode {
    CLARO, OSCURO, SISTEMA
}
