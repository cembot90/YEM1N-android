package de.cemaras.yem1n

import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import de.cemaras.yem1n.logik.Wochenbericht
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WochenberichtTest {
    private val tag = 86_400_000L
    private val jetzt = 100 * tag + 12 * 3_600_000L

    private fun runde(kind: String, uebung: String, tageZurueck: Long, richtig: Int, gesamt: Int, details: String = "", angesehen: Int = 0) =
        RundenErgebnisEintrag("e$kind$uebung$tageZurueck$richtig", "K3", "Mathe", "Nr 1", uebung, richtig, gesamt, angesehen, 2, kind, jetzt - tageZurueck * tag, details)

    private fun falsch(n: Int) =
        """{"n":$n,"f":"Rechne.","r":"7 · 8 =","e":"63","l":"56","s":"falsch"}"""

    @Test fun nurLetzteSiebenTage() {
        val alle = listOf(runde("Yemin", "A", 1, 8, 10), runde("Yemin", "A", 6, 9, 10), runde("Yemin", "A", 7, 1, 10))
        val b = Wochenbericht.berichte(alle, jetzt, 0).single()
        assertEquals(2, b.runden)
        assertEquals(17, b.richtig)
        assertEquals(2, b.tage)
    }

    @Test fun getrenntNachKindern() {
        val alle = listOf(runde("Yemin", "A", 0, 5, 10), runde("Evîn", "B", 0, 10, 10))
        val b = Wochenbericht.berichte(alle, jetzt, 0)
        assertEquals(setOf("Yemin", "Evîn"), b.map { it.name }.toSet())
    }

    @Test fun starkUndUebenUndMuster() {
        val d = "[" + falsch(1) + "]"
        val alle = listOf(
            runde("Yemin", "Kernaufgaben", 0, 10, 10),
            runde("Yemin", "Geteilt", 1, 2, 10, d),
            runde("Yemin", "Geteilt", 2, 3, 10, d),
        )
        val b = Wochenbericht.berichte(alle, jetzt, 0).single()
        assertEquals(listOf("Kernaufgaben"), b.stark)
        assertEquals(listOf("Geteilt"), b.ueben)
        assertEquals(1, b.muster.size)
        assertEquals(2, b.muster[0].anzahl)
        val text = Wochenbericht.text(b)
        assertTrue(text, text.contains("Sicher sitzt: Kernaufgaben."))
        assertTrue(text, text.contains("Mehr Übung braucht: Geteilt."))
        assertTrue(text, text.contains("Tipp für zu Hause"))
        assertTrue(text, text.contains("an 3 Tagen"))
    }

    @Test fun ohneRundenSagtEsDas() {
        val b = Wochenbericht.berichte(emptyList(), jetzt, 0).single()
        assertEquals("Euer Kind hat in den letzten 7 Tagen nicht in YEM1N geübt.", Wochenbericht.text(b))
    }

    @Test fun starkeWoche() {
        val alle = (0L..2L).map { runde("Yemin", "A", it, 10, 10) }
        assertTrue(Wochenbericht.text(Wochenbericht.berichte(alle, jetzt, 0).single()).endsWith("Eine starke Woche!"))
    }
}
