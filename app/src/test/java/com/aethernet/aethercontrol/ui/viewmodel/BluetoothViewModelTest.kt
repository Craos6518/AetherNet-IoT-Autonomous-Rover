package com.aethernet.aethercontrol.ui.viewmodel

import app.cash.turbine.test
import com.aethernet.aethercontrol.data.bluetooth.BluetoothManager
import com.aethernet.aethercontrol.data.bluetooth.BtConnectionState
import com.aethernet.aethercontrol.data.bluetooth.BluetoothDeviceInfo
import com.aethernet.aethercontrol.data.bluetooth.BtAccessResult
import com.aethernet.aethercontrol.domain.model.BluetoothUiState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private class FakeBluetoothManager : BluetoothManager {
        private val _conn = MutableStateFlow<BtConnectionState>(BtConnectionState.Disconnected)
        override val connectionState: StateFlow<BtConnectionState> = _conn
        private val _access = MutableSharedFlow<BtAccessResult>()
        override val accessEvents: SharedFlow<BtAccessResult> = _access
        private val _presence = MutableSharedFlow<Boolean>()
        override val presence: SharedFlow<Boolean> = _presence

        override fun isSupported(): Boolean = true
        override fun listPaired() = emptyList<BluetoothDeviceInfo>()
        override suspend fun connect(address: String): Result<Unit> = Result.Success(Unit)
        override fun disconnect() { _conn.value = BtConnectionState.Disconnected }
        override suspend fun sendAccessCommand(pin: String): Result<Unit> = Result.Success(Unit)
        override suspend fun requestPresence(): Result<Unit> = Result.Success(Unit)

        fun simulateConnecting(deviceName: String = "HC-06") {
            _conn.value = BtConnectionState.Connecting(deviceName)
        }
        fun simulateConnected(deviceName: String = "HC-06") {
            _conn.value = BtConnectionState.Connected(deviceName)
        }
        fun simulateError(msg: String) {
            _conn.value = BtConnectionState.Error(msg)
        }
    }

    @Test fun `connectionState se propaga a uiState`() = runTest {
        val bt = FakeBluetoothManager()
        val vm = BluetoothViewModel(bt)

        assertEquals(BtConnectionState.Disconnected, vm.uiState.value.connection)

        bt.simulateConnecting("HC-06")
        assertEquals(BtConnectionState.Connecting("HC-06"), vm.uiState.value.connection)

        bt.simulateConnected("HC-06")
        assertEquals(BtConnectionState.Connected("HC-06"), vm.uiState.value.connection)

        bt.simulateError("read failed")
        val err = vm.uiState.value.connection
        assertTrue(err is BtConnectionState.Error)
    }
}