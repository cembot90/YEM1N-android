package de.cemaras.yem1n

import de.cemaras.yem1n.logik.ErgebnisUmzug
import de.cemaras.yem1n.logik.FamilienProtokoll
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErgebnisUmzugTest {
    private val tag = 86_400_000L
    private val jetzt = 1_800_000_000_000L

    private fun runde(id: String, zeitpunkt: Long): String = FamilienProtokoll.ergebnis(
        RundenErgebnisEintrag(
            eintragId = id, klasse = "3", fach = "Mathe", arbeit = "Arbeit 1", uebung = "Plus",
            richtig = 8, gesamt = 10, angesehen = 0, sterne = 2, kind = "Yemin", zeitpunkt = zeitpunkt,
        ),
    ).toString()

    @Test fun zeitpunktKommtAusDerAnfrage() {
        assertEquals(jetzt, ErgebnisUmzug.zeitpunkt(runde("a", jetzt)))
        assertEquals(0L, ErgebnisUmzug.zeitpunkt("kaputt"))
    }

    @Test fun archivMerktJedeRundeNurEinmal() {
        val r = runde("a", jetzt)
        val a = ErgebnisUmzug.merke(ErgebnisUmzug.merke(emptyList(), r, jetzt), r, jetzt)
        assertEquals(1, a.size)
    }

    @Test fun zuAlteRundenFallenHeraus() {
        val alt = runde("alt", jetzt - 400 * tag)
        val neu = runde("neu", jetzt)
        val a = ErgebnisUmzug.merke(listOf(alt), neu, jetzt)
        assertEquals(listOf(neu), a)
    }

    @Test fun archivAlsTextHinUndZurueck() {
        val a = listOf(runde("a", jetzt - tag), runde("b", jetzt))
        assertEquals(a, ErgebnisUmzug.lese(ErgebnisUmzug.text(a) + "\n\nkaputt\n"))
        assertTrue(ErgebnisUmzug.lese(null).isEmpty())
    }

    @Test fun neueFamilieBekommtAllesAeltesteZuerst() {
        val a = listOf(runde("b", jetzt - tag), runde("a", jetzt - 2 * tag))
        val offen = ErgebnisUmzug.offen(a, "fam2", erledigt = "fam1", laeuft = "", bis = 0, jetzt = jetzt)
        assertEquals(listOf(a[1], a[0]), offen)
    }

    @Test fun erledigteFamilieBekommtNichtsMehr() {
        val a = listOf(runde("a", jetzt))
        assertTrue(ErgebnisUmzug.offen(a, "fam1", erledigt = "fam1", laeuft = "", bis = 0, jetzt = jetzt).isEmpty())
        assertTrue(ErgebnisUmzug.offen(a, "", erledigt = "", laeuft = "", bis = 0, jetzt = jetzt).isEmpty())
    }

    @Test fun nachAbbruchGehtEsDahinterWeiter() {
        val a = listOf(runde("a", jetzt - 2 * tag), runde("b", jetzt - tag), runde("c", jetzt))
        val offen = ErgebnisUmzug.offen(a, "fam2", erledigt = "", laeuft = "fam2", bis = jetzt - tag, jetzt = jetzt)
        assertEquals(listOf(a[2]), offen)
        // Stand einer anderen Familie zählt nicht.
        assertEquals(3, ErgebnisUmzug.offen(a, "fam3", erledigt = "", laeuft = "fam2", bis = jetzt - tag, jetzt = jetzt).size)
    }

    @Test fun pauseNurBeiVielenRunden() {
        assertNull(ErgebnisUmzug.pause(3))
        assertNull(ErgebnisUmzug.pause(ErgebnisUmzug.AB))
        assertNotNull(ErgebnisUmzug.pause(ErgebnisUmzug.AB + 1))
    }
}
