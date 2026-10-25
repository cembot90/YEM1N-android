package de.cemaras.yem1n

import de.cemaras.yem1n.logik.ElternPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElternPinTest {
    @Test fun nurZiffernVierBisAcht() {
        assertTrue(ElternPin.gueltig("4821"))
        assertTrue(ElternPin.gueltig("48213579"))
        assertFalse(ElternPin.gueltig("482"))
        assertFalse(ElternPin.gueltig("482135791"))
        assertFalse(ElternPin.gueltig("48a1"))
        assertFalse(ElternPin.gueltig(""))
    }

    @Test fun leichtePinsWerdenErkannt() {
        assertTrue(ElternPin.zuLeicht("0000"))
        assertTrue(ElternPin.zuLeicht("1234"))
        assertTrue(ElternPin.zuLeicht("98765"))
        assertTrue(ElternPin.zuLeicht("123"))
        assertFalse(ElternPin.zuLeicht("4821"))
        assertFalse(ElternPin.zuLeicht("1357"))
    }

    @Test fun hashPasstNurZurRichtigenPin() {
        val salz = ElternPin.salz(ByteArray(16) { it.toByte() })
        assertEquals(32, salz.length)
        val h = ElternPin.hash("4821", salz)
        assertEquals(64, h.length)
        assertTrue(ElternPin.stimmt("4821", salz, h))
        assertFalse(ElternPin.stimmt("4822", salz, h))
        assertFalse(ElternPin.stimmt("4821", "anderes", h))
        assertFalse(ElternPin.stimmt("4821", "", h))
        assertFalse(ElternPin.stimmt("4821", salz, ""))
    }

    @Test fun anderesSalzGibtAnderenHash() {
        val a = ElternPin.salz(ByteArray(16) { 1 })
        val b = ElternPin.salz(ByteArray(16) { 2 })
        assertNotEquals(ElternPin.hash("4821", a), ElternPin.hash("4821", b))
        assertEquals(ElternPin.hash("4821", a), ElternPin.hash("4821", a))
    }

    @Test fun wartezeitWaechstMitFehlversuchen() {
        assertEquals(0L, ElternPin.sperreMs(0))
        assertEquals(0L, ElternPin.sperreMs(2))
        assertEquals(30_000L, ElternPin.sperreMs(3))
        assertEquals(60_000L, ElternPin.sperreMs(4))
        assertEquals(300_000L, ElternPin.sperreMs(5))
        assertEquals(900_000L, ElternPin.sperreMs(6))
        assertEquals(900_000L, ElternPin.sperreMs(50))
    }

    @Test fun wartezeitText() {
        assertEquals("Bitte noch 30 Sekunden warten.", ElternPin.sperreText(30_000))
        assertEquals("Bitte noch 1 Sekunde warten.", ElternPin.sperreText(1))
        assertEquals("Bitte noch 5 Minuten warten.", ElternPin.sperreText(300_000))
        assertFalse(ElternPin.sperreText(30_000).contains("\u2014"))
    }
}
