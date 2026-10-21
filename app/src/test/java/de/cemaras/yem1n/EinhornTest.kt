package de.cemaras.yem1n

import de.cemaras.yem1n.logik.EinhornObjekt
import de.cemaras.yem1n.logik.EinhornRegeln
import de.cemaras.yem1n.logik.EinhornRegeln.Ding
import de.cemaras.yem1n.logik.EinhornSpiel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EinhornTest {
    @Test
    fun levelUndTempo() {
        assertEquals(1, EinhornRegeln.level(0))
        assertEquals(1, EinhornRegeln.level(14))
        assertEquals(2, EinhornRegeln.level(15))
        assertEquals(1, EinhornRegeln.level(-5))
        assertEquals(210f, EinhornRegeln.tempo(1))
        assertEquals(460f, EinhornRegeln.tempo(50))
        assertEquals(0.7, EinhornRegeln.abstand(99), 1e-9)
        assertEquals(0.32, EinhornRegeln.gewitterAnteil(99), 1e-9)
    }

    @Test
    fun fangenVeraendertPunkteUndLeben() {
        assertEquals(1 to 3, EinhornRegeln.fange(Ding.STERN, 0, 3))
        assertEquals(6 to 3, EinhornRegeln.fange(Ding.REGENBOGEN, 3, 3))
        assertEquals(5 to 3, EinhornRegeln.fange(Ding.HERZ, 5, 2))
        assertEquals(3, EinhornRegeln.fange(Ding.HERZ, 5, 3).second)
        assertEquals(0, EinhornRegeln.fange(Ding.GEWITTER, 5, 0).second)
        assertEquals(1, EinhornRegeln.fange(Ding.GEWITTER, 5, 2).second)
    }

    @Test
    fun herzenNurWennEinLebenFehlt() {
        for (i in 0..100) {
            assertTrue(EinhornRegeln.naechstes(i / 100.0, 1, 3) != Ding.HERZ)
        }
        assertEquals(Ding.HERZ, EinhornRegeln.naechstes(0.15, 1, 2))
        assertEquals(Ding.GEWITTER, EinhornRegeln.naechstes(0.0, 1, 3))
        assertEquals(Ding.REGENBOGEN, EinhornRegeln.naechstes(0.9, 1, 3))
        assertEquals(Ding.STERN, EinhornRegeln.naechstes(0.5, 1, 3))
    }

    @Test
    fun spielStartetMitDemErstenTippen() {
        val s = EinhornSpiel(Random(1))
        assertEquals(EinhornSpiel.Phase.WARTEN, s.phase)
        s.schritt(0.016f, 1f)
        assertEquals(EinhornSpiel.Phase.WARTEN, s.phase)
        s.tippe()
        assertEquals(EinhornSpiel.Phase.LAEUFT, s.phase)
        assertEquals(EinhornRegeln.FLUG_STOSS, s.vy)
    }

    @Test
    fun einhornFaelltUndWirdAmBodenWeichAufgefangen() {
        val s = EinhornSpiel(Random(1))
        s.tippe()
        repeat(600) {
            s.schritt(0.016f, 0f)
            assertTrue(s.y >= 60f && s.y <= EinhornRegeln.HOEHE - 70f)
        }
    }

    @Test
    fun sternFangenGibtPunkt() {
        val s = EinhornSpiel(Random(1))
        s.tippe()
        s.objekte.add(EinhornObjekt(Ding.STERN, EinhornRegeln.EINHORN_X + 10f, s.y))
        s.schritt(0.001f, 0f)
        assertEquals(1, s.punkte)
        assertEquals(1, s.ereignisse.size)
        assertEquals(Ding.STERN, s.ereignisse[0].ding)
    }

    @Test
    fun gewitterKostetEinLebenUndGibtSchutz() {
        val s = EinhornSpiel(Random(1))
        s.tippe()
        s.objekte.add(EinhornObjekt(Ding.GEWITTER, EinhornRegeln.EINHORN_X, s.y))
        s.schritt(0.001f, 0f)
        assertEquals(2, s.leben)
        assertTrue(s.schutz > 1f)
        // Im Schutz geht die nächste Wolke einfach vorbei
        s.objekte.add(EinhornObjekt(Ding.GEWITTER, EinhornRegeln.EINHORN_X, s.y))
        s.schritt(0.001f, 0f)
        assertEquals(2, s.leben)
    }

    @Test
    fun spielEndetBeimLetztenLebenUndMeldetPunkte() {
        val s = EinhornSpiel(Random(1))
        var gemeldet = -1
        s.beiEnde = { gemeldet = it }
        s.tippe()
        repeat(3) {
            // Den Schutz ablaufen lassen, ohne dass andere Dinge dazukommen
            repeat(120) {
                s.objekte.clear()
                s.schritt(0.016f, 0f)
            }
            s.objekte.clear()
            s.objekte.add(EinhornObjekt(Ding.GEWITTER, EinhornRegeln.EINHORN_X, s.y))
            s.schritt(0.001f, 0f)
        }
        assertEquals(EinhornSpiel.Phase.VORBEI, s.phase)
        assertEquals(s.punkte, gemeldet)
        assertTrue(s.objekte.isEmpty())
        // Noch einmal tippen startet neu
        s.tippe()
        assertEquals(EinhornSpiel.Phase.LAEUFT, s.phase)
        assertEquals(3, s.leben)
        assertEquals(0, s.punkte)
    }

    @Test
    fun levelAufstiegWirdGemeldet() {
        val s = EinhornSpiel(Random(1))
        s.tippe()
        repeat(14) { s.objekte.add(EinhornObjekt(Ding.STERN, EinhornRegeln.EINHORN_X, s.y)) }
        s.schritt(0.001f, 0f)
        assertEquals(14, s.punkte)
        assertFalse(s.ereignisse.any { it.levelHoch })
        s.objekte.add(EinhornObjekt(Ding.STERN, EinhornRegeln.EINHORN_X, s.y))
        s.schritt(0.001f, 0f)
        assertEquals(2, s.level)
        assertTrue(s.ereignisse.last().levelHoch)
    }

    @Test
    fun neueDingeKommenVonRechtsUndVerschwindenLinks() {
        val s = EinhornSpiel(Random(2))
        s.tippe()
        var gesehen = false
        repeat(2000) {
            if (it % 20 == 0) s.tippe()
            s.schritt(0.016f, 0f)
            if (s.phase == EinhornSpiel.Phase.VORBEI) return
            if (s.objekte.isNotEmpty()) gesehen = true
            assertTrue(s.objekte.all { o -> o.x >= -80f - 500f * 0.04f })
        }
        assertTrue(gesehen)
    }
}
