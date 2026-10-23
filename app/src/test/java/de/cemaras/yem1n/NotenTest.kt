package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Noten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotenTest {
    @Test
    fun alleTabellenHabenDieselbenNoten() {
        assertEquals(Noten.alle.toSet(), Noten.stufe.keys)
        assertEquals(Noten.alle.toSet(), Noten.name.keys)
        assertEquals(Noten.alle.toSet(), Noten.frequenz.keys)
    }

    @Test
    fun toeneSteigenMitDerStufe() {
        val nachStufe = Noten.alle.sortedBy { Noten.stufe.getValue(it) }
        assertEquals(Noten.alle, nachStufe)
        val frequenzen = Noten.alle.map { Noten.frequenz.getValue(it) }
        assertEquals(frequenzen.sorted(), frequenzen)
    }

    @Test
    fun pcmHatDieRichtigeLaengeUndBleibtImBereich() {
        val daten = Noten.pcm(440.0, 0.5)
        assertEquals(Noten.RATE / 2, daten.size)
        assertTrue(daten.any { it.toInt() != 0 })
        assertTrue(daten.all { it in Short.MIN_VALUE..Short.MAX_VALUE })
    }
}
