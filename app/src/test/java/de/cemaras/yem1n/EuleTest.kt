package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Eule
import de.cemaras.yem1n.logik.Haustier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EuleTest {
    private val ei = Haustier(name = "Pip", xp = 0)
    private val gross = Haustier(name = "Pip", xp = 60, satt = 90.0, laune = 90.0)

    @Test fun eiSagtSchluepfen() {
        assertTrue(Eule.lageTipps(ei, 0, heute = 5).first().contains("schlüpft"))
    }

    @Test fun geschenkNurWennOffen() {
        val abgeholt = gross.copy(geschenkTag = 5)
        assertFalse(Eule.lageTipps(abgeholt, 0, heute = 5).any { it.contains("Tagesgeschenk") })
        assertTrue(Eule.lageTipps(abgeholt, 0, heute = 6).any { it.contains("Tagesgeschenk") })
    }

    @Test fun hungrigesTierBekommtTipp() {
        val hungrig = gross.copy(satt = 10.0)
        assertTrue(Eule.lageTipps(hungrig, 0, heute = 5).first().contains("Hunger"))
    }

    @Test fun keineDoppeltenTipps() {
        val t = Eule.tipps(gross, 100, true, 5)
        assertEquals(t.size, t.distinct().size)
    }

    @Test fun huteNurWennBezahlbarUndNochNichtDa() {
        assertEquals(null, Eule.guenstigsterHut(gross, 0))
        val hut = Eule.guenstigsterHut(gross, 1000)!!
        assertEquals(null, Eule.guenstigsterHut(gross.copy(huete = listOf(hut.id)), 0))
    }

    @Test fun antippenKreistDurchDieSprueche() {
        val a = Eule.antippen(gross, 0)
        val b = Eule.antippen(gross, 1)
        assertTrue(a != b)
        assertTrue(Eule.antippen(gross, -3).isNotEmpty())
    }

    @Test fun sprechbarOhneEmoji() {
        assertEquals("Hallo Welt", Eule.sprechbar("Hallo 🦉 Welt 🎉"))
    }

    @Test fun einfuehrungHatSchritte() {
        assertTrue(Eule.einfuehrung(ei).size >= 6)
        assertFalse(Eule.einfuehrung(ei).any { it.contains("—") || it.contains("–") })
    }
}
