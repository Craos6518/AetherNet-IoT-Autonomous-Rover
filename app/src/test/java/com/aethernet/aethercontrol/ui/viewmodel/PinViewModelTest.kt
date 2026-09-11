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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp(){ Dispatchers.setMain(dispatcher)}
    @After fun tearDown(){ Dispatchers.resetMain()}

    private class FakeRepo(
        var mqttState: MqttConnectionState = MqttConnectionState.Connected("tcp://1:1883"),
        val accessFlow: MutableSharedFlow<AccessEventMqtt> = MutableSharedFlow(extraBufferCapacity=32),
        var sendResult: Result<Unit> = Result.Success(Unit)
    ): AetherRepository {
        private val _mqtt = MutableStateFlow(mqttState)
        override val mqttConnectionState: StateFlow<MqttConnectionState> get() = _mqtt
        override val roverTelemetryFlow: SharedFlow<RoverTelemetryMqtt> get() = MutableSharedFlow()
        override val accessEventFlow: SharedFlow<AccessEventMqtt> get() = accessFlow
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
        override suspend fun sendAccessCommand(pin:String): Result<Unit> = sendResult
        override suspend fun sendRoverCommand(leftPwm: Int, rightPwm: Int, mode: Int): Result<Unit> = Result.Success(Unit)
        override suspend fun sendRoverVector(x: Float, y: Float): Result<Unit> = Result.Success(Unit)
        fun setMqtt(s: MqttConnectionState){ mqttState=s; _mqtt.value=s}
    }

    @Test fun `onPinDigit valida 4 a 6`() = runTest {
        val vm = PinViewModel(FakeRepo())
        vm.onPinDigit("1"); vm.onPinDigit("2"); vm.onPinDigit("3")
        assertEquals(false, vm.pinState.value.isValid) // len 3
        vm.onPinDigit("4")
        assertEquals(true, vm.pinState.value.isValid)
        assertEquals("1234", vm.pinState.value.pinInput)
    }

    @Test fun `onPinDigit ignora mayor 6 y no digito`() = runTest {
        val vm = PinViewModel(FakeRepo())
        repeat(7){ vm.onPinDigit("1") }
        assertEquals(6, vm.pinState.value.pinInput.length)
        vm.onPinClear()
        vm.onPinDigit("a")
        assertEquals("", vm.pinState.value.pinInput)
    }

    @Test fun `onPinBackspace y clear`() = runTest {
        val vm = PinViewModel(FakeRepo())
        vm.onPinDigit("1"); vm.onPinDigit("2")
        vm.onPinBackspace()
        assertEquals("1", vm.pinState.value.pinInput)
        vm.onPinClear()
        assertEquals("", vm.pinState.value.pinInput)
    }

    @Test fun `sendPin requiere 4 a 6 digitos`() = runTest {
        val vm = PinViewModel(FakeRepo())
        vm.onPinDigit("1"); vm.onPinDigit("2")
        vm.sendPin()
        assertNotNull(vm.pinState.value.error)
        assertEquals("PIN 4-6 dígitos", vm.pinState.value.error)
    }

    @Test fun `sendPin guarda MQTT no conectado`() = runTest {
        val repo = FakeRepo(mqttState = MqttConnectionState.Disconnected)
        val vm = PinViewModel(repo)
        vm.onPinDigit("1"); vm.onPinDigit("2"); vm.onPinDigit("3"); vm.onPinDigit("4")
        vm.sendPin()
        assertEquals("MQTT no conectado — verifica Wi-Fi/broker", vm.pinState.value.error)
    }

    @Test fun `sendPin success limpia input y muestra Enviado`() = runTest {
        val repo = FakeRepo(mqttState = MqttConnectionState.Connected("tcp://1:1883"), sendResult = Result.Success(Unit))
        val vm = PinViewModel(repo)
        vm.onPinDigit("1"); vm.onPinDigit("2"); vm.onPinDigit("3"); vm.onPinDigit("4")
        vm.sendPin()
        // Unconfined -> inmediato
        assertEquals("", vm.pinState.value.pinInput)
        assertEquals("Enviado, esperando confirmación…", vm.pinState.value.lastMessage)
        assertEquals(1, vm.pinState.value.attemptsInWindow)
    }

    @Test fun `accessEventFlow actualiza lastResultSuccess`() = runTest {
        val flow = MutableSharedFlow<AccessEventMqtt>(extraBufferCapacity=32)
        val repo = FakeRepo(accessFlow=flow)
        val vm = PinViewModel(repo)
        vm.pinState.test {
            // init state
            var s = awaitItem()
            flow.emit(AccessEventMqtt("app_user","hash",true,"app"))
            s = awaitItem()
            assertEquals(true, s.lastResultSuccess)
            assertEquals("✓ Desbloqueado", s.lastMessage)
            flow.emit(AccessEventMqtt("app_user","hash",false,"app"))
            s = awaitItem()
            assertEquals(false, s.lastResultSuccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `throttle 5 intentos en 60s bloquea sexto`() = runTest {
        val repo = FakeRepo(mqttState = MqttConnectionState.Connected("tcp://1:1883"))
        val vm = PinViewModel(repo)
        repeat(5){
            vm.onPinDigit("1"); vm.onPinDigit("2"); vm.onPinDigit("3"); vm.onPinDigit("4")
            vm.sendPin()
        }
        assertEquals(5, vm.pinState.value.attemptsInWindow)
        vm.onPinDigit("1"); vm.onPinDigit("2"); vm.onPinDigit("3"); vm.onPinDigit("4")
        vm.sendPin()
        assertEquals("Demasiados intentos, espera 60s", vm.pinState.value.error)
    }
}
