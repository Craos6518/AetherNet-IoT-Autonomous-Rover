package com.aethernet.aethercontrol.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BtPayloadParserTest {

    @Test fun `access success parsea hash y source`() {
        val msg = BtPayloadParser.parseLine("""ACCESS:{"user_id":"hc06_user","pin_hash":"7c78c98f","success":true,"source":"bluetooth"}""")
        assertTrue(msg is BtLineMessage.Access)
        val a = (msg as BtLineMessage.Access).result
        assertTrue(a.success)
        assertEquals("7c78c98f", a.pinHash)
        assertEquals("bluetooth", a.source)
    }

    @Test fun `access fail success false`() {
        val msg = BtPayloadParser.parseLine("""ACCESS:{"success":false,"source":"bluetooth"}""")
        assertTrue(msg is BtLineMessage.Access)
        val a = (msg as BtLineMessage.Access).result
        assertEquals(false, a.success)
        assertNull(a.pinHash)
    }

    @Test fun `presence true parsea`() {
        val msg = BtPayloadParser.parseLine("""PRESENCE:{"presence":true}""")
        assertTrue(msg is BtLineMessage.Presence)
        assertEquals(true, (msg as BtLineMessage.Presence).presence)
    }

    @Test fun `presence false parsea`() {
        val msg = BtPayloadParser.parseLine("""PRESENCE:{"presence":false}""")
        assertTrue(msg is BtLineMessage.Presence)
        assertEquals(false, (msg as BtLineMessage.Presence).presence)
    }

    @Test fun `permite espacios entre clave y valor`() {
        // nano-access-compacto.ino edita con formato compacto, pero toleramos espacios
        val msg = BtPayloadParser.parseLine("""ACCESS:{"success" : true}""")
        assertTrue(msg is BtLineMessage.Access)
        assertTrue((msg as BtLineMessage.Access).result.success)
    }

    @Test fun `linea basura se ignora null`() {
        assertNull(BtPayloadParser.parseLine("foo"))
        assertNull(BtPayloadParser.parseLine(""))
        assertNull(BtPayloadParser.parseLine("   "))
        assertNull(BtPayloadParser.parseLine("UNKNOWN:{}"))
    }

    @Test fun `trim quita CR LF real del reader`() {
        // reader.readLine() no incluye \r\n, pero si llegara, trim los quita
        val msg = BtPayloadParser.parseLine("""ACCESS:{"success":true}""" + "\r\n")
        assertTrue(msg is BtLineMessage.Access)
        assertTrue((msg as BtLineMessage.Access).result.success)
    }

    @Test fun `access sin pin_hash tolera campos ausentes`() {
        val msg = BtPayloadParser.parseLine("""ACCESS:{"user_id":"hc06_user","success":true}""")
        assertTrue(msg is BtLineMessage.Access)
        val a = (msg as BtLineMessage.Access).result
        assertTrue(a.success)
        assertNull(a.pinHash)
        assertNull(a.source)
    }

    // --- Casos borde añadidos Task 5 ---

    @Test fun `access con espacios tras dos puntos`() {
        val msg = BtPayloadParser.parseLine("""ACCESS: {"success":true}""")
        assertTrue(msg is BtLineMessage.Access)
        assertTrue((msg as BtLineMessage.Access).result.success)
    }

    @Test fun `type minusculas devuelve null`() {
        // TYPE es case-sensitive (solo ACCESS/PRESENCE mayúsculas)
        assertNull(BtPayloadParser.parseLine("""access:{"success":true}"""))
        assertNull(BtPayloadParser.parseLine("""presence:{"presence":true}"""))
    }

    @Test fun `linea truncada devuelve access con success=false`() {
        // Parser es leniente: JSON truncado produce Access con success=false y campos null
        val msg1 = BtPayloadParser.parseLine("""ACCESS:{"success""")
        assertTrue(msg1 is BtLineMessage.Access)
        val a1 = (msg1 as BtLineMessage.Access).result
        assertEquals(false, a1.success)
        assertNull(a1.pinHash)
        assertNull(a1.source)

        val msg2 = BtPayloadParser.parseLine("""ACCESS:""")
        assertTrue(msg2 is BtLineMessage.Access)
        val a2 = (msg2 as BtLineMessage.Access).result
        assertEquals(false, a2.success)
        assertNull(a2.pinHash)
        assertNull(a2.source)
    }

    @Test fun `ruido comun se ignora`() {
        assertNull(BtPayloadParser.parseLine("OK"))
        assertNull(BtPayloadParser.parseLine("ERROR"))
        assertNull(BtPayloadParser.parseLine("CONNECT"))
    }

    @Test fun `source distinto se propaga`() {
        val msg = BtPayloadParser.parseLine("""ACCESS:{"success":true,"source":"ble"}""")
        assertTrue(msg is BtLineMessage.Access)
        val a = (msg as BtLineMessage.Access).result
        assertTrue(a.success)
        assertEquals("ble", a.source)
    }

    @Test fun `presencia valor invalido devuelve null`() {
        assertNull(BtPayloadParser.parseLine("""PRESENCE:{"presence":"yes"}"""))
        assertNull(BtPayloadParser.parseLine("""PRESENCE:{"presence":1}"""))
    }
}