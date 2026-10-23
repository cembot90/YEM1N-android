package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Anmeldung
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnmeldungTest {
    private fun runde(kind: String, zeit: Long) = RundenErgebnisEintrag(
        eintragId = "r$zeit", klasse = "Klasse 3", fach = "Mathe", arbeit = "A", uebung = "Minus",
        richtig = 8, gesamt = 10, angesehen = 0, sterne = 2, kind = kind, zeitpunkt = zeit,
    )

    @Test fun anmeldungIstKeineRunde() {
        val e = Anmeldung.eintrag("ABCDE-FGHIJ", "Yemin", "geraet-1", 1000)
        assertTrue(Anmeldung.istAnmeldung(e))
        assertEquals(0, e.gesamt)
        assertEquals("Yemin", e.kind)
        assertFalse(Anmeldung.istAnmeldung(runde("Yemin", 5)))
    }

    @Test fun kennungBleibtGleichUndAendertSichMitNamenOderCode() {
        val a = Anmeldung.eintrag("ABCDE-FGHIJ", "Yemin", "g1", 1)
        val b = Anmeldung.eintrag("ABCDE-FGHIJ", " yemin ", "g1", 2)
        assertEquals(a.eintragId, b.eintragId)
        assertNotEquals(a.eintragId, Anmeldung.eintrag("ABCDE-FGHIJ", "Evin", "g1", 1).eintragId)
        assertNotEquals(a.eintragId, Anmeldung.eintrag("KLMNO-PQRST", "Yemin", "g1", 1).eintragId)
        assertTrue(a.eintragId.length <= 80)
    }

    @Test fun namenOhneDoppelte() {
        val liste = listOf(
            Anmeldung.eintrag("C", "Yemin", "g1", 1), Anmeldung.eintrag("C", "yemin ", "g2", 2),
            Anmeldung.eintrag("C", "Evin", "g3", 3), runde("Mia", 4),
        )
        assertEquals(listOf("Evin", "yemin"), Anmeldung.namen(liste).sortedBy { it.lowercase() }.map { it })
    }

    @Test fun kinderAuswahlHaengtNurAngemeldeteAn() {
        assertEquals(listOf("Yemin", "Evin"), Anmeldung.kinder(listOf("Yemin"), listOf("yemin", "Evin")))
    }

    @Test fun kinderZeilenZaehlenRundenUndNehmenAngemeldeteDazu() {
        val runden = listOf(runde("Yemin", 10), runde("yemin ", 30), runde("Mia", 20))
        val anm = listOf(
            Anmeldung.eintrag("C", "Evin", "g3", 5),
            Anmeldung.eintrag("C", "Yemin", "g1", 6),
        )
        val z = Anmeldung.kinderZeilen(runden, anm)
        assertEquals(listOf("Evin", "Mia", "yemin"), z.map { it.name })
        assertEquals(listOf(0, 1, 2), z.map { it.runden })
        assertEquals(30L, z.first { it.name == "yemin" }.zuletzt)
        assertEquals(5L, z.first { it.name == "Evin" }.zuletzt)
    }
}
