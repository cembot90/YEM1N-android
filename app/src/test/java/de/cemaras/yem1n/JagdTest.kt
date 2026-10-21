package de.cemaras.yem1n

import de.cemaras.yem1n.logik.JagdBalance
import de.cemaras.yem1n.logik.JagdGegner
import de.cemaras.yem1n.logik.JagdMuenze
import de.cemaras.yem1n.logik.JagdPlattform
import de.cemaras.yem1n.logik.JagdSpiel
import de.cemaras.yem1n.logik.JagdStick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class JagdTest {
    private fun luecke(a: JagdPlattform, b: JagdPlattform): Float =
        maxOf(0f, maxOf(a.links - b.rechts, b.links - a.rechts))

    @Test
    fun jedePlattformIstErreichbar() {
        for (nummer in 1..JagdBalance.anzahlFormen) {
            val e = JagdBalance.ebene(nummer)
            val alle = listOf(JagdBalance.boden) + e.plattformen
            for (p in e.plattformen) {
                val erreichbar = alle.any { q ->
                    q.y < p.y && p.y - q.y <= JagdBalance.bequemeHoehe && luecke(p, q) <= JagdBalance.BEQUEME_WEITE
                }
                assertTrue("Ebene $nummer Plattform $p", erreichbar)
            }
        }
    }

    @Test
    fun jedeMuenzeLiegtAufEinerPlattformUndInDerBuehne() {
        for (nummer in 1..JagdBalance.anzahlFormen * 2) {
            val e = JagdBalance.ebene(nummer)
            val alle = listOf(JagdBalance.boden) + e.plattformen
            assertTrue(e.muenzen.size >= 15)
            for (m in e.muenzen) {
                assertTrue(m.x in 0f..JagdBalance.BREITE && m.y in 0f..JagdBalance.HOEHE)
                assertTrue("Muenze $m", alle.any { m.x >= it.links && m.x <= it.rechts && Math.abs(m.y - (it.y + 38f)) < 0.01f })
            }
        }
    }

    @Test
    fun plattformenUeberlappenNichtUndLiegenImBild() {
        for (nummer in 1..JagdBalance.anzahlFormen) {
            val ps = JagdBalance.ebene(nummer).plattformen
            for (p in ps) {
                assertTrue(p.links >= 0f && p.rechts <= JagdBalance.BREITE)
                assertTrue(p.y + JagdBalance.SPIELER_HOEHE < JagdBalance.HOEHE)
            }
            for (i in ps.indices) for (j in i + 1 until ps.size) {
                val a = ps[i]
                val b = ps[j]
                val gleicheHoehe = Math.abs(a.y - b.y) < JagdBalance.PLATTFORM_DICKE
                assertFalse("$a und $b", gleicheHoehe && luecke(a, b) <= 0f)
            }
        }
    }

    @Test
    fun gegnerSindLangsamerAlsDerHase() {
        for (ebene in 1..30) {
            val tempo = JagdBalance.gegnerTempo(ebene)
            // Auch der Jäger (80 Prozent des Tempos) bleibt langsamer
            assertTrue(tempo * 0.8f < JagdBalance.LAUF_TEMPO)
            assertTrue(JagdBalance.gegnerAnzahl(ebene) in 1..5)
        }
        assertEquals(1, JagdBalance.gegnerAnzahl(0))
        assertEquals(5, JagdBalance.gegnerAnzahl(99))
    }

    @Test
    fun sprungKommtHochGenug() {
        assertTrue(JagdBalance.maxSprungHoehe > 150f)
        assertEquals(JagdBalance.maxSprungHoehe * 0.85f, JagdBalance.bequemeHoehe, 1e-3f)
    }

    @Test
    fun stickTotzoneUndGrenzen() {
        assertEquals(0f to 0f, JagdStick.wert(0f, 0f))
        assertEquals(0f, JagdStick.wert(5f, 0f).first)
        assertEquals(1f, JagdStick.wert(500f, 0f).first, 1e-4f)
        assertEquals(-1f, JagdStick.wert(-500f, 0f).first, 1e-4f)
        assertTrue(JagdStick.wert(40f, 0f).first in 0.1f..0.9f)
        assertTrue(JagdStick.springt(JagdStick.wert(0f, 500f).second))
        assertFalse(JagdStick.springt(JagdStick.wert(0f, 20f).second))
        val k = JagdStick.knopf(300f, 400f)
        assertEquals(JagdStick.RADIUS, Math.hypot(k.first.toDouble(), k.second.toDouble()).toFloat(), 1e-3f)
        assertEquals(10f to 20f, JagdStick.knopf(10f, 20f))
    }

    private fun spiel(): JagdSpiel {
        val s = JagdSpiel(Random(3))
        s.tippe()
        s.gegner.clear()
        return s
    }

    @Test
    fun startMitErstemTippen() {
        val s = JagdSpiel(Random(1))
        assertEquals(JagdSpiel.Phase.WARTEN, s.phase)
        assertNotNull(s.markiert)
        assertEquals(1, s.gegner.size)
        s.tippe()
        assertEquals(JagdSpiel.Phase.LAEUFT, s.phase)
    }

    @Test
    fun laufenUndBleibtImBild() {
        val s = spiel()
        s.eingabe(1f, false, false)
        repeat(400) { s.schritt(0.016f) }
        assertTrue(s.x <= JagdBalance.BREITE - 28f && s.x >= JagdBalance.BREITE - 28f - 1f)
        s.eingabe(-1f, false, false)
        assertTrue(s.laeuft)
        repeat(400) { s.schritt(0.016f) }
        assertEquals(28f, s.x, 1e-3f)
        assertTrue(s.blickLinks)
    }

    @Test
    fun springenUndAufPlattformLanden() {
        val s = spiel()
        // Unter die linke untere Plattform der ersten Form (x 190, Oberkante 180) laufen und springen
        s.eingabe(-1f, false, false)
        while (s.x > 200f) s.schritt(0.016f)
        s.eingabe(0f, true, false)
        s.schritt(0.016f)
        assertTrue(!s.amBoden)
        // Taste gehalten: Schweben beim Fallen
        repeat(120) { s.schritt(0.016f) }
        assertTrue(s.amBoden)
        assertEquals(180f + JagdBalance.SPIELER_HOEHE / 2, s.y, 1e-3f)
    }

    @Test
    fun kurzesTippenSpringtNiedriger() {
        val kurz = spiel()
        val lang = spiel()
        kurz.eingabe(0f, true, false)
        lang.eingabe(0f, true, false)
        kurz.schritt(0.016f)
        lang.schritt(0.016f)
        kurz.schritt(0.016f)
        lang.schritt(0.016f)
        kurz.eingabe(0f, false, false)
        repeat(40) { kurz.schritt(0.016f); lang.schritt(0.016f) }
        assertTrue(lang.y > kurz.y || lang.amBoden && kurz.amBoden)
    }

    @Test
    fun vomRandFallenWirdZumBoden() {
        val s = spiel()
        // Auf eine Plattform und dann seitlich hinauslaufen
        s.eingabe(-1f, false, false)
        while (s.x > 200f) s.schritt(0.016f)
        s.eingabe(0f, true, false)
        repeat(120) { s.schritt(0.016f) }
        assertEquals(180f + 32f, s.y, 1e-3f)
        s.eingabe(1f, false, false)
        repeat(300) { s.schritt(0.016f) }
        assertTrue(s.y < 180f + 32f)
    }

    @Test
    fun muenzeSammelnGibtPunkte() {
        val s = spiel()
        val gesamt = s.muenzen.size
        val m = s.muenzen.first { it !== s.markiert }
        val ziel = JagdMuenze(s.x, s.y)
        s.muenzen.add(ziel)
        s.schritt(0.001f)
        assertEquals(1, s.punkte)
        assertEquals(gesamt, s.muenzen.size)
        assertNotNull(m)
    }

    @Test
    fun leuchtendeMuenzeGibtDreiUndDieNaechsteLeuchtet() {
        val s = spiel()
        s.muenzen.clear()
        val a = JagdMuenze(s.x, s.y)
        val nah = JagdMuenze(s.x + 300f, s.y)
        val fern = JagdMuenze(s.x + 500f, s.y + 200f)
        s.muenzen.addAll(listOf(a, fern, nah))
        s.markiert = a
        s.schritt(0.001f)
        assertEquals(3, s.punkte)
        assertTrue(s.markiert === nah)
    }

    @Test
    fun gegnerTreffenUndEndeNachDreiLeben() {
        val s = spiel()
        var gemeldet = -1
        s.beiEnde = { gemeldet = it }
        repeat(3) {
            repeat(120) { s.schritt(0.016f) }
            s.gegner.clear()
            s.gegner.add(JagdGegner(s.x, s.y, 0f, 0f, 100f, false, 0, 0f))
            s.schritt(0.001f)
        }
        assertEquals(JagdSpiel.Phase.VORBEI, s.phase)
        assertEquals(s.punkte, gemeldet)
        s.tippe()
        assertEquals(JagdSpiel.Phase.LAEUFT, s.phase)
        assertEquals(3, s.leben)
        assertEquals(0, s.punkte)
        assertEquals(1, s.ebene)
    }

    @Test
    fun neueGegnerSindZuerstHarmlos() {
        val s = spiel()
        s.gegner.add(JagdGegner(s.x, s.y, 0f, 0f, 100f, false, 0, 1.4f))
        s.schritt(0.001f)
        assertEquals(3, s.leben)
    }

    @Test
    fun alleMuenzenSammelnBringtNaechsteEbene() {
        val s = spiel()
        s.muenzen.clear()
        s.muenzen.add(JagdMuenze(s.x, s.y))
        val vorher = s.punkte
        s.schritt(0.001f)
        assertEquals(JagdSpiel.Phase.EBENE_FERTIG, s.phase)
        assertTrue(s.punkte >= vorher + 11)
        repeat(130) { s.schritt(0.016f) }
        assertEquals(JagdSpiel.Phase.LAEUFT, s.phase)
        assertEquals(2, s.ebene)
        assertTrue(s.muenzen.size >= 15)
        assertEquals(2, s.gegner.size)
        assertTrue(s.unverwundbar > 0f)
    }

    @Test
    fun jaegerFolgtDemHasen() {
        val s = JagdSpiel(Random(1))
        s.tippe()
        // Ebene 3 erreichen
        repeat(2) {
            s.muenzen.clear()
            s.muenzen.add(JagdMuenze(s.x, s.y))
            s.gegner.clear()
            s.schritt(0.001f)
            repeat(130) { s.schritt(0.016f) }
        }
        assertEquals(3, s.ebene)
        assertTrue(s.gegner.first().jaeger)
        assertEquals(1, s.gegner.count { it.jaeger })
    }
}
