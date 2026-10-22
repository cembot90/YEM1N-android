package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Karteikasten
import de.cemaras.yem1n.logik.SprachRunde
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KarteikastenTest {
    private val k = Karteikasten.schluessel("tr", "farben", "rot")
    private val t0 = 1_000_000L

    @Test fun richtigSteigtBisDrei() {
        var s = emptyMap<String, de.cemaras.yem1n.logik.WortEintrag>()
        repeat(5) { s = Karteikasten.bewerte(s, k, true, t0) }
        assertEquals(3, s[k]!!.b)
        assertEquals(5, s[k]!!.r)
        assertEquals(t0 + 7 * 86_400_000L, s[k]!!.n)
    }

    @Test fun falschGehtZurueck() {
        var s = Karteikasten.bewerte(emptyMap(), k, true, t0)
        s = Karteikasten.bewerte(s, k, true, t0)
        assertTrue(Karteikasten.gelernt(s[k]))
        s = Karteikasten.bewerte(s, k, false, t0)
        assertEquals(0, s[k]!!.b)
        assertEquals(1, s[k]!!.f)
        assertFalse(Karteikasten.gelernt(s[k]))
    }

    @Test fun kannIchUndFaellig() {
        val s = Karteikasten.kannIch(emptyMap(), k, t0)
        assertEquals(1, s[k]!!.b)
        assertFalse(Karteikasten.faellig(s[k], t0))
        assertTrue(Karteikasten.faellig(s[k], t0 + 86_400_000L))
        assertFalse(Karteikasten.faellig(null, t0))
    }

    @Test fun verlaufBleibtBegrenzt() {
        var v = emptyList<SprachRunde>()
        repeat(150) { v = Karteikasten.neuerVerlauf(v, SprachRunde(it.toLong(), "en", "t", "karten", 1, 1)) }
        assertEquals(100, v.size)
        assertEquals(149L, v.last().ts)
    }
}
