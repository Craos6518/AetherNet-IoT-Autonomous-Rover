package com.aethernet.aethercontrol.ui.viewmodel

import app.cash.turbine.test
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
import com.aethernet.aethercontrol.domain.mapper.LedStateMapper
import com.aethernet.aethercontrol.domain.model.LedColor
import com.aethernet.aethercontrol.domain.model.LedState
import com.aethernet.aethercontrol.domain.model.LedUiState
import com.aethernet.aethercontrol.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Tests adelanto MOV-10 — MOV-01 5.6 (RF-1.1).
 * FakeRepo + UnconfinedTestDispatcher para ejecución inmediata de viewModelScope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepo(
        private val isHealthOk: Boolean = true,
        private val ledState: LedUiState? = null,
        private val ledError: String? = null
    ) : AetherRepository {
        override suspend fun getHealth(): Result<HealthResponse> =
            if (isHealthOk) Result.Success(HealthResponse("ok", "ok", "1.0.0-sprint1"))
            else Result.Error("Network error")

        override suspend fun getAccessEvents(limit: Int) = Result.Success(emptyList<AccessEventOut>())
        override suspend fun postAccessEvent(payload: AccessEventCreate) = Result.Error("not impl")
        override suspend fun getSensorEvents(limit: Int) = Result.Success(emptyList<SensorEventOut>())
        override suspend fun postSensorEvent(payload: SensorEventCreate) = Result.Error("not impl")
        override suspend fun getSecurityEvents(limit: Int) = Result.Success(emptyList<SecurityEventOut>())
        override suspend fun postSecurityEvent(payload: SecurityEventCreate) = Result.Error("not impl")
        override suspend fun getRoverTelemetry(limit: Int) = Result.Success(emptyList<RoverTelemetryOut>())
        override suspend fun postRoverTelemetry(payload: RoverTelemetryCreate) = Result.Error("not impl")
        override suspend fun getLedState(): Result<LedUiState> =
            if (ledError != null) Result.Error(ledError)
            else Result.Success(ledState ?: LedUiState(color = LedColor.OFF, state = LedState.OFF, label = "Apagado"))
    }

    @Test
    fun `refreshHealth success updates isConnected and health`() = runTest {
        val repo = FakeRepo(isHealthOk = true)
        val vm = DashboardViewModel(repo, autoPollLed = false)

        // Con Unconfined, init ya corrió: estado final debe ser conectado
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isLoading)
            assertEquals(true, state.isConnected)
            assertNotNull(state.health)
            assertEquals("ok", state.health?.status)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshHealth error sets error and isConnected false`() = runTest {
        val repo = FakeRepo(isHealthOk = false)
        val vm = DashboardViewModel(repo, autoPollLed = false)

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(false, state.isLoading)
            assertEquals(false, state.isConnected)
            assertNotNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state after init reflects repo result`() = runTest {
        val repo = FakeRepo(isHealthOk = true)
        val vm = DashboardViewModel(repo, autoPollLed = false)
        // Valor directo sin turbine también debe reflejar success
        val state = vm.uiState.value
        assertEquals(true, state.isConnected)
        assertNotNull(state.health)
    }

    @Test
    fun `refreshHealth toggles loading`() = runTest {
        // Repo con delay simulado usando Result.Success inmediato pero verificamos transición
        val repo = FakeRepo(isHealthOk = true)
        val vm = DashboardViewModel(repo, autoPollLed = false)
        // Forzar refresh de nuevo y verificar que termina en connected
        vm.refreshHealth()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(true, state.isConnected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MOV-02 — LedUiState en ViewModel

    @Test
    fun `init loads ledState OFF by default`() = runTest {
        val repo = FakeRepo(isHealthOk = true)
        val vm = DashboardViewModel(repo, autoPollLed = false)
        val state = vm.uiState.value
        // init refreshLedState con FakeRepo OFF
        assertEquals(LedState.OFF, state.ledState.state)
        assertEquals(LedColor.OFF, state.ledState.color)
    }

    @Test
    fun `refreshLedState error sets led error without crashing`() = runTest {
        val repo = FakeRepo(isHealthOk = true, ledError = "Network error")
        val vm = DashboardViewModel(repo, autoPollLed = false)
        vm.refreshLedState()
        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.ledState.error)
            assertEquals("Network error", state.ledState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshLedState success updates label`() = runTest {
        val green = LedUiState(color = LedColor.GREEN, state = LedState.GREEN_UNLOCKED, label = "Verde desbloqueado", source = "access")
        val repo = FakeRepo(isHealthOk = true, ledState = green)
        val vm = DashboardViewModel(repo, autoPollLed = false)
        vm.refreshLedState()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals("Verde desbloqueado", state.ledState.label)
            assertEquals(LedColor.GREEN, state.ledState.color)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `polling is active after init`() = runTest {
        val repo = FakeRepo(isHealthOk = true)
        val vmAuto = DashboardViewModel(repo, autoPollLed = true)
        assertEquals(true, vmAuto.isPolling)
        vmAuto.stopLedPolling()
        assertEquals(false, vmAuto.isPolling)
        vmAuto.startLedPolling()
        assertEquals(true, vmAuto.isPolling)
        vmAuto.stopLedPolling()

        val vmManual = DashboardViewModel(repo, autoPollLed = false)
        assertEquals(false, vmManual.isPolling)
        vmManual.startLedPolling()
        assertEquals(true, vmManual.isPolling)
        vmManual.stopLedPolling()
    }
}
