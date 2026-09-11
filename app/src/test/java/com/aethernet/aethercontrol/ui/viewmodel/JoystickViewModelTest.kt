package com.aethernet.aethercontrol.ui.viewmodel

import app.cash.turbine.test
import com.aethernet.aethercontrol.data.mqtt.AccessEventMqtt
import com.aethernet.aethercontrol.data.mqtt.MqttConnectionState
import com.aethernet.aethercontrol.data.mqtt.RoverTelemetryMqtt
import com.aethernet.aethercontrol.data.mqtt.SecurityEventMqtt
import com.aethernet.aethercontrol.data.remote.dto.AccessEventCreate
import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.HealthResponse
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryCreate
import com.aethernet.aethercontrol.data.remote.dto.RoverTelemetryOut
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.data.remote.dto.SensorEventCreate
import com.aethernet.aethercontrol.data.remote.dto.SensorEventOut
import com.aethernet.aethercontrol.data.repository.AetherRepository
import com.aethernet.aethercontrol.domain.model.LedUiState
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoystickViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    @Before fun setUp(){ Dispatchers.setMain(dispatcher)}
    @After fun tearDown(){ Dispatchers.resetMain()}

    private class FakeRepo(
        var mqttState: MqttConnectionState = MqttConnectionState.Connected("tcp://1:1883"),
        var sendResult: Result<Unit> = Result.Success(Unit),
        var lastLeft: Int? = null,
        var lastRight: Int? = null,
        var lastMode: Int? = null,
        var sendCount: Int = 0
    ): AetherRepository {
        private val _mqtt = MutableStateFlow(mqttState)
        override val mqttConnectionState: StateFlow<MqttConnectionState> get() = _mqtt
        override val roverTelemetryFlow: SharedFlow<RoverTelemetryMqtt> get() = MutableSharedFlow()
        override val accessEventFlow: SharedFlow<AccessEventMqtt> get() = MutableSharedFlow()
        override val securityEventFlow: SharedFlow<SecurityEventMqtt> get() = MutableSharedFlow()
        override suspend fun connectMqtt(httpBaseUrl: String){ _mqtt.value = MqttConnectionState.Connected(httpBaseUrl)}
        override fun disconnectMqtt(){ _mqtt.value = MqttConnectionState.Disconnected}
        override suspend fun getHealth(): Result<HealthResponse> = Result.Success(HealthResponse("ok","ok","1.0.0"))
        override suspend fun getAccessEvents(limit:Int) = Result.Success(emptyList<AccessEventOut>())
        override suspend fun postAccessEvent(payload: AccessEventCreate) = Result.Error("not impl")
        override suspend fun getSensorEvents(limit:Int) = Result.Success(emptyList<SensorEventOut>())
        override suspend fun postSensorEvent(payload: SensorEventCreate) = Result.Error("not impl")
        override suspend fun getSecurityEvents(limit:Int) = Result.Success(emptyList<SecurityEventOut>())
        override suspend fun postSecurityEvent(payload: SecurityEventCreate) = Result.Error("not impl")
        override suspend fun getRoverTelemetry(limit:Int) = Result.Success(emptyList<RoverTelemetryOut>())
        override suspend fun postRoverTelemetry(payload: RoverTelemetryCreate) = Result.Error("not impl")
        override suspend fun getLedState(): Result<LedUiState> = Result.Success(LedUiState())
        override suspend fun sendAccessCommand(pin:String): Result<Unit> = Result.Success(Unit)
        override suspend fun sendRoverCommand(leftPwm:Int, rightPwm:Int, mode:Int): Result<Unit> {
            lastLeft = leftPwm; lastRight = rightPwm; lastMode = mode; sendCount++
            return sendResult
        }
        override suspend fun sendRoverVector(x: Float, y: Float): Result<Unit> {
            // mirror mapper
            val l = ((y + x)*255).toInt().coerceIn(-255,255)
            val r = ((y - x)*255).toInt().coerceIn(-255,255)
            lastLeft = l; lastRight = r; lastMode = if(l==0&&r==0)0 else 1; sendCount++
            return sendResult
        }
        fun setMqtt(s: MqttConnectionState){ mqttState=s; _mqtt.value=s}
    }

    @Test fun `onPositionChanged adelante mapea 255 255 y mode 1`() = runTest {
        val repo = FakeRepo()
        val vm = JoystickViewModel(repo, throttleMs = 0) // sin throttle para test
        vm.onPositionChanged(0f, 1f)
        // Unconfined -> inmediato
        assertEquals(0f, vm.uiState.value.x, 0.01f)
        assertEquals(1f, vm.uiState.value.y, 0.01f)
        assertEquals(255, vm.uiState.value.leftPwm)
        assertEquals(255, vm.uiState.value.rightPwm)
        assertEquals(1, vm.uiState.value.mode)
        assertEquals(255, repo.lastLeft)
        assertEquals(255, repo.lastRight)
        assertEquals(1, repo.lastMode)
    }

    @Test fun `onPositionChanged deadband centro 0 0 stop mode 0`() = runTest {
        val repo = FakeRepo()
        val vm = JoystickViewModel(repo, throttleMs = 0)
        vm.onPositionChanged(0f, 0f)
        assertEquals(0, vm.uiState.value.leftPwm)
        assertEquals(0, vm.uiState.value.rightPwm)
        assertEquals(0, vm.uiState.value.mode)
        assertEquals(0, repo.lastLeft)
        assertEquals(0, repo.lastMode)
    }

    @Test fun `onReleased centra y envia stop`() = runTest {
        val repo = FakeRepo()
        val vm = JoystickViewModel(repo, throttleMs = 0)
        vm.onPositionChanged(0f, 1f) // adelante
        vm.onReleased()
        assertEquals(0f, vm.uiState.value.x, 0.01f)
        assertEquals(0f, vm.uiState.value.y, 0.01f)
        assertEquals(0, vm.uiState.value.leftPwm)
        assertEquals(0, vm.uiState.value.mode)
        assertEquals(0, repo.lastLeft)
    }

    @Test fun `throttle 50ms bloquea segundo envio rapido`() = runTest {
        val repo = FakeRepo()
        val vm = JoystickViewModel(repo, throttleMs = 50)
        vm.onPositionChanged(0f, 1f) // primer envío
        assertEquals(1, repo.sendCount)
        vm.onPositionChanged(0f, 0.9f) // <50ms después, debería throttlear
        assertEquals(1, repo.sendCount) // sigue 1
    }

    @Test fun `error MQTT se refleja en uiState`() = runTest {
        val repo = FakeRepo(mqttState = MqttConnectionState.Connected("tcp://1:1883"), sendResult = Result.Error("MQTT no conectado"))
        val vm = JoystickViewModel(repo, throttleMs = 0)
        vm.onPositionChanged(0f, 1f)
        // espera a que coroutine de send termine (Unconfined inmediato)
        assertEquals("MQTT no conectado", vm.uiState.value.error)
    }

    @Test fun `giro derecha mapea tank 255 -255`() = runTest {
        val repo = FakeRepo()
        val vm = JoystickViewModel(repo, throttleMs = 0)
        vm.onPositionChanged(1f, 0f)
        assertEquals(255, vm.uiState.value.leftPwm)
        assertEquals(-255, vm.uiState.value.rightPwm)
    }
}
