package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Handschrift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HandschriftTest {
    @Test
    fun ziffernWerdenGelesen() {
        assertEquals("42", Handschrift.zahl("42", 3))
        assertEquals("42", Handschrift.zahl(" 4 2 ", 3))
    }

    @Test
    fun fuehrendeNullenFallenWeg() {
        assertEquals("7", Handschrift.zahl("007", 3))
        assertEquals("0", Handschrift.zahl("0", 3))
        assertEquals("0", Handschrift.zahl("00", 3))
    }

    @Test
    fun aehnlicheZeichenWerdenZuZiffern() {
        assertEquals("10", Handschrift.zahl("lO", 3))
        assertEquals("25", Handschrift.zahl("Zs", 3))
        assertEquals("68", Handschrift.zahl("bB", 3))
    }

    @Test
    fun punktUndKommaStoerenNicht() {
        assertEquals("56", Handschrift.zahl("5.6", 3))
        assertEquals("56", Handschrift.zahl("5,6.", 3))
    }

    @Test
    fun buchstabenUndZuLangeZahlenSindKeineZahl() {
        assertNull(Handschrift.zahl("Hallo", 3))
        assertNull(Handschrift.zahl("1234", 3))
        assertNull(Handschrift.zahl("", 3))
        assertNull(Handschrift.zahl("-", 3))
    }

    @Test
    fun ersterBrauchbarerVorschlagZaehlt() {
        assertEquals("42", Handschrift.lies(listOf("Hy", "42", "41"), 3))
        assertNull(Handschrift.lies(listOf("Hy", "Ab"), 3))
        assertEquals("1234567", Handschrift.lies(listOf("1234567"), 7))
    }
}
