package com.aethernet.aethercontrol.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoystickMapperTest {

    @Test fun `center 0 0 gives 0 0 stop`() {
        val (l, r) = JoystickMapper.vectorToPwm(0f, 0f)
        assertEquals(0, l)
        assertEquals(0, r)
        assertEquals(0, JoystickMapper.vectorToMode(0f, 0f))
    }

    @Test fun `adelante 0 1 da 255 255`() {
        val (l, r) = JoystickMapper.vectorToPwm(0f, 1f)
        assertEquals(255, l)
        assertEquals(255, r)
        assertEquals(1, JoystickMapper.vectorToMode(0f, 1f))
    }

    @Test fun `atras 0 -1 da -255 -255`() {
        val (l, r) = JoystickMapper.vectorToPwm(0f, -1f)
        assertEquals(-255, l)
        assertEquals(-255, r)
    }

    @Test fun `giro derecha 1 0 tank turn`() {
        val (l, r) = JoystickMapper.vectorToPwm(1f, 0f)
        assertEquals(255, l)
        assertEquals(-255, r)
    }

    @Test fun `giro izquierda -1 0 tank turn`() {
        val (l, r) = JoystickMapper.vectorToPwm(-1f, 0f)
        assertEquals(-255, l)
        assertEquals(255, r)
    }

    @Test fun `diagonal clamp 1 1 no excede 255`() {
        val (l, r) = JoystickMapper.vectorToPwm(1f, 1f)
        assertTrue(l in -255..255)
        assertTrue(r in -255..255)
        assertEquals(255, l) // (1+1)*255=510 -> clamp 255
        assertEquals(0, r)   // (1-1)*255=0
    }

    @Test fun `deadband 60 bloquea pwm bajo`() {
        val (l, r) = JoystickMapper.vectorToPwm(0.1f, 0.1f, applyDeadband = true)
        // (0.1+0.1)*255=51 -> deadband <60 => 0
        assertEquals(0, l)
        assertEquals(0, r)
    }

    @Test fun `sin deadband permite pwm bajo`() {
        val (l, r) = JoystickMapper.vectorToPwm(0.1f, 0.1f, applyDeadband = false)
        assertTrue(l != 0 || r != 0)
    }

    @Test fun `normalizeInCircle clamp fuera`() {
        val (nx, ny) = JoystickMapper.normalizeInCircle(200f, 0f, 100f, invertY = false)
        assertEquals(1f, nx, 0.01f)
        assertEquals(0f, ny, 0.01f)
    }

    @Test fun `normalizeInCircle invertY`() {
        val (nx, ny) = JoystickMapper.normalizeInCircle(0f, 100f, 100f, invertY = true)
        // Canvas Y+ abajo 100px, invertY true => ny -1 (adelante)
        assertEquals(0f, nx, 0.01f)
        assertEquals(-1f, ny, 0.01f)
    }
}
