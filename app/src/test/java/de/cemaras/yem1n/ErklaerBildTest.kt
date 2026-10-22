package de.cemaras.yem1n

import de.cemaras.yem1n.logik.BildArt
import de.cemaras.yem1n.logik.ErklaerBildPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErklaerBildTest {
    private fun zahl(r: String, a: String, e: String = "") = ErklaerBildPlan.bild("zahl", r, e, a, "")

    @Test fun malGibtPunktefeld() {
        val b = zahl("7 · 8 =", "56")!!
        assertEquals(BildArt.Punkte(7, 8), b.art)
        assertTrue(b.saetze.last().contains("56"))
    }

    @Test fun nachbarMitKern() {
        val b = zahl("6 · 7 =", "42")!!
        assertEquals(BildArt.Nachbar(5, 6, 7), b.art)
        val neun = zahl("9 · 4 =", "36")!!
        assertTrue(neun.saetze[1].contains("weniger"))
    }

    @Test fun falscheLoesungGibtKeinBild() {
        assertNull(zahl("7 · 8 =", "55"))
    }

    @Test fun wortAufgabeNutztErklaerung() {
        val b = zahl("Subtrahiere von 75 die Zahl 8.", "67", "75 - 8 = 67")
        assertNotNull(b)
        assertTrue(b!!.art is BildArt.Spruenge)
    }

    @Test fun spruengeGehenZuerstZumZehner() {
        assertEquals(listOf(3, 5), ErklaerBildPlan.spruenge(47, true, 8))
        assertEquals(listOf(-5, -3), ErklaerBildPlan.spruenge(75, false, 8))
        assertEquals(listOf(30, 4), ErklaerBildPlan.spruenge(10, true, 34))
    }

    @Test fun geteiltMitTellern() {
        val b = zahl("32 : 8 =", "4")!!
        assertEquals(BildArt.Teller(32, 8, 4, 0), b.art)
    }

    @Test fun restMitTellern() {
        val b = ErklaerBildPlan.bild("rest", "58 : 4 =", "", "14", "2")!!
        assertEquals(BildArt.Teller(58, 4, 14, 2), b.art)
        assertNull(ErklaerBildPlan.bild("rest", "58 : 4 =", "", "14", "3"))
    }

    @Test fun punktVorStrichRechenweg() {
        val b = zahl("9 · 9 + 8 =", "89")!!
        assertEquals(BildArt.Rechenweg(listOf("9 · 9 + 8", "81 + 8", "89")), b.art)
        assertTrue(b.saetze.first().contains("Punkt vor Strich"))
    }

    @Test fun vergleich() {
        val b = ErklaerBildPlan.bild("vergleich", "5 · 5  ?  9 · 7", "", "<", "")!!
        assertEquals(BildArt.Rechenweg(listOf("5 · 5  ?  9 · 7", "25  ?  63", "25  <  63")), b.art)
        assertNull(ErklaerBildPlan.bild("vergleich", "5 · 5  ?  9 · 7", "", ">", ""))
    }

    @Test fun keinNegativesErgebnis() {
        assertNull(ErklaerBildPlan.term("3 - 5 =")?.let { ErklaerBildPlan.wert(it) })
    }
}
