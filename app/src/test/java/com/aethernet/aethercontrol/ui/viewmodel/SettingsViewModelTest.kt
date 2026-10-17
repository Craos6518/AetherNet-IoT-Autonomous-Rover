package com.aethernet.aethercontrol.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MOV-08 — Tests puros de Ajustes (sin Android/ServiceLocator).
 * normalizeBackendUrl y deriveBrokerLabel son top-level sin dependencias.
 */
class SettingsViewModelTest {

    @Test fun `normalizeBackendUrl añade esquema y barra`() {
        assertEquals("http://192.168.1.50:8000/", normalizeBackendUrl("192.168.1.50:8000"))
        assertEquals("http://192.168.1.50:8000/", normalizeBackendUrl("http://192.168.1.50:8000"))
        assertEquals("http://10.0.2.2:8000/", normalizeBackendUrl("  http://10.0.2.2:8000/  "))
        assertEquals("https://host:8000/", normalizeBackendUrl("https://host:8000"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `normalizeBackendUrl rechaza vacia`() {
        normalizeBackendUrl("   ")
    }

    @Test fun `deriveBrokerLabel extrae host y fija 1883`() {
        assertEquals("tcp://192.168.1.14:1883", deriveBrokerLabel("http://192.168.1.14:8000/"))
        assertEquals("tcp://10.0.2.2:1883", deriveBrokerLabel("http://10.0.2.2:8000/"))
        assertEquals("tcp://host:1883", deriveBrokerLabel("https://host:8000/api"))
        assertEquals("tcp://10.0.2.2:1883", deriveBrokerLabel(""))
    }
}
