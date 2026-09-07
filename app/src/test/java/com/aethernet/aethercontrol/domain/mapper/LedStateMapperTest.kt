package com.aethernet.aethercontrol.domain.mapper

import com.aethernet.aethercontrol.data.remote.dto.AccessEventOut
import com.aethernet.aethercontrol.data.remote.dto.SecurityEventOut
import com.aethernet.aethercontrol.domain.model.LedColor
import com.aethernet.aethercontrol.domain.model.LedState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Mapper determinista LED local — MOV-02 BDD HU-01/HU-02.
 * Espejo led.cpp:64 + config.h:52 DOOR_AUTO_LOCK_MS=5000 / :62 LED_RED_FAIL_MS=1000
 */
class LedStateMapperTest {

    private fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    private fun access(success: Boolean, tsMs: Long) = AccessEventOut(
        id = "00000000-0000-0000-0000-000000000001",
        user_id = "keypad_user",
        pin_hash = "hash",
        success = success,
        timestamp = iso(tsMs),
        source = "keypad"
    )

    private fun security(type: String, tsMs: Long) = SecurityEventOut(
        id = "00000000-0000-0000-0000-000000000002",
        event_type = type,
        severity = "high",
        description = "test",
        timestamp = iso(tsMs),
        acknowledged = false
    )

    @Test
    fun `empty collections to UNKNOWN`() {
        val r = LedStateMapper.map(emptyList(), emptyList(), nowMs = 10000L)
        assertEquals(LedState.UNKNOWN, r.state)
        assertEquals(LedColor.UNKNOWN, r.color)
    }

    @Test
    fun `HU-01 Dado MEGA verde tras PIN 1234 Cuando poll less 5s Entonces verde desbloqueado`() {
        val now = 10_000L
        val ts = now - 2000L // 2s atrás to vigente
        val r = LedStateMapper.map(listOf(access(true, ts)), emptyList(), nowMs = now)
        assertEquals(LedState.GREEN_UNLOCKED, r.state)
        assertEquals(LedColor.GREEN, r.color)
        assertEquals("Verde desbloqueado", r.label)
        assertEquals("access", r.source)
    }

    @Test
    fun `HU-01 verde expirado 5s to OFF`() {
        val now = 10_000L
        val ts = now - 6000L // 6s atrás to expirado
        val r = LedStateMapper.map(listOf(access(true, ts)), emptyList(), nowMs = now)
        assertEquals(LedState.OFF, r.state)
        assertEquals(LedColor.OFF, r.color)
        assertEquals("Apagado", r.label)
    }

    @Test
    fun `HU-02 Dado MEGA rojo tras intrusion Entonces roja`() {
        val now = 20_000L
        val ts = now - 3000L
        val r = LedStateMapper.map(emptyList(), listOf(security("intrusion", ts)), nowMs = now)
        assertEquals(LedState.RED_INTRUSION, r.state)
        assertEquals(LedColor.RED, r.color)
        assertEquals("Rojo intrusión", r.label)
    }

    @Test
    fun `HU-02 intrusión expirada 10s to OFF`() {
        val now = 20_000L
        val ts = now - 11_000L
        val r = LedStateMapper.map(emptyList(), listOf(security("intrusion", ts)), nowMs = now)
        assertEquals(LedState.OFF, r.state)
    }

    @Test
    fun `access fail dentro 1s to RED_FAIL`() {
        val now = 30_000L
        val ts = now - 500L
        val r = LedStateMapper.map(listOf(access(false, ts)), emptyList(), nowMs = now)
        assertEquals(LedState.RED_FAIL, r.state)
        assertEquals(LedColor.RED, r.color)
    }

    @Test
    fun `access fail expirado 1s to OFF`() {
        val now = 30_000L
        val ts = now - 1500L
        val r = LedStateMapper.map(listOf(access(false, ts)), emptyList(), nowMs = now)
        assertEquals(LedState.OFF, r.state)
    }

    @Test
    fun `intrusion prevalece sobre access aunque security timestamp sea tie`() {
        val now = 40_000L
        val ts = now - 1000L
        val r = LedStateMapper.map(
            listOf(access(true, ts)),
            listOf(security("intrusion", ts)),
            nowMs = now
        )
        // tie to prioriza security intrusión
        assertEquals(LedState.RED_INTRUSION, r.state)
    }

    @Test
    fun `security no-intrusion no pinta rojo to OFF`() {
        val now = 50_000L
        val ts = now - 1000L
        val r = LedStateMapper.map(emptyList(), listOf(security("access_denied", ts)), nowMs = now)
        assertEquals(LedState.OFF, r.state)
    }

    @Test
    fun `backend caido no aplicable en mapper — UNKNOWN manejado por ViewModel error`() {
        // Mapper no conoce backend caído; si no hay eventos es UNKNOWN
        val r = LedStateMapper.map(emptyList(), emptyList(), nowMs = 0L)
        assertEquals(LedState.UNKNOWN, r.state)
    }

    @Test
    fun `ventana borde exacto 5000ms to OFF (exclusive upper bound)`() {
        val now = 100_000L
        val ts = now - 5000L
        val r = LedStateMapper.map(listOf(access(true, ts)), emptyList(), nowMs = now)
        assertEquals(LedState.OFF, r.state)
    }
}
