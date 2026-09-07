package com.aethernet.aethercontrol.domain.validator

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MOV-04 — PinValidator formato 4-6 dígitos espejo config.h:49 + hash djb2 keypad_control.cpp:99
 */
class PinValidatorTest {

    @Test fun `isValidFormat rechaza len menor 4`() {
        assertEquals(false, PinValidator.isValidFormat("123"))
        assertEquals(false, PinValidator.isValidFormat(""))
    }

    @Test fun `isValidFormat acepta 4-6 digitos`() {
        assertEquals(true, PinValidator.isValidFormat("1234"))
        assertEquals(true, PinValidator.isValidFormat("12345"))
        assertEquals(true, PinValidator.isValidFormat("123456"))
    }

    @Test fun `isValidFormat rechaza mayor 6 y no-digito`() {
        assertEquals(false, PinValidator.isValidFormat("1234567"))
        assertEquals(false, PinValidator.isValidFormat("12a4"))
        assertEquals(false, PinValidator.isValidFormat("12*4"))
        assertEquals(false, PinValidator.isValidFormat("12#4"))
    }

    @Test fun `hashPin djb2 deterministico 32bit`() {
        val h1 = PinValidator.hashPin("1234")
        val h2 = PinValidator.hashPin("1234")
        assertEquals(h1, h2)
        // distinto pin -> distinto hash
        assertEquals(false, h1 == PinValidator.hashPin("0000"))
        // espejo Arduino String(hash,HEX) lowercase sin 0x
        assertEquals(true, h1.matches(Regex("[0-9a-f]+")))
    }

    @Test fun `isCorrectForDemo solo para preview`() {
        assertEquals(true, PinValidator.isCorrectForDemo("1234"))
        assertEquals(false, PinValidator.isCorrectForDemo("0000"))
    }
}
