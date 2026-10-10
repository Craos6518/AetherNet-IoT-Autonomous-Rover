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
}