package de.cemaras.yem1n

import de.cemaras.yem1n.logik.HockeyBalance
import de.cemaras.yem1n.logik.HockeyEreignis
import de.cemaras.yem1n.logik.HockeySpiel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.random.Random

class HockeyTest {
    @Test
    fun levelWirdBegrenzt() {
        assertEquals(1, HockeyBalance.level(-4))
        assertEquals(1, HockeyBalance.level(1))
        assertEquals(8, HockeyBalance.level(99))
    }

    @Test
    fun computerBleibtSchlagbar() {
        var vorher = 0f
        for (l in 1..12) {
            val t = HockeyBalance.kiTempo(l)
            assertTrue(t >= vorher)
            vorher = t
            // Deutlich langsamer als der schnellste Puck
            assertTrue(t < HockeyBalance.PUCK_MAX_TEMPO * 0.75f)
            assertTrue(HockeyBalance.kiFehler(l) >= 10f)
            assertTrue(HockeyBalance.kiReaktion(l) >= 0.07f)
        }
        assertTrue(HockeyBalance.kiFehler(8) < HockeyBalance.kiFehler(1))
        assertTrue(HockeyBalance.kiReaktion(8) < HockeyBalance.kiReaktion(1))
    }

    @Test
    fun punkteFuerSieg() {
        assertEquals(10, HockeyBalance.punkteFuerSieg(1, 0))
        assertEquals(15, HockeyBalance.punkteFuerSieg(1, 5))
        assertEquals(34, HockeyBalance.punkteFuerSieg(3, 4))
        assertEquals(10, HockeyBalance.punkteFuerSieg(1, -3))
    }

    @Test
    fun startUndAnstoss() {
        val s = HockeySpiel(Random(2))
        assertEquals(HockeySpiel.Phase.WARTEN, s.phase)
        s.beruehre(200f, 288f)
        assertEquals(HockeySpiel.Phase.LAEUFT, s.phase)
        repeat(30) { s.schritt(0.016f) }
        assertEquals(512f, s.puckX, 1e-3f)
        repeat(30) { s.schritt(0.016f) }
        assertTrue(s.puckX > 512f)
        assertEquals(320f, hypot(s.puckVx, s.puckVy), 25f)
    }

    @Test
    fun schlaegerBleibtInDerEigenenHaelfte() {
        val s = HockeySpiel(Random(2))
        s.beruehre(900f, 10f)
        repeat(100) { s.schritt(0.016f) }
        assertTrue(s.meinX <= HockeySpiel.MITTE - HockeySpiel.TREFFER_RADIUS - 4f + 0.01f)
        assertTrue(s.meinY >= HockeySpiel.UNTEN + HockeySpiel.TREFFER_RADIUS - 0.01f)
        s.fuehre(-50f, 900f)
        repeat(100) { s.schritt(0.016f) }
        assertEquals(HockeySpiel.LINKS + HockeySpiel.TREFFER_RADIUS, s.meinX, 0.01f)
        assertEquals(HockeySpiel.OBEN - HockeySpiel.TREFFER_RADIUS, s.meinY, 0.01f)
    }

    @Test
    fun schlaegerSchiebtDenPuck() {
        val s = HockeySpiel(Random(2))
        s.beruehre(300f, 288f)
        repeat(100) { s.schritt(0.016f) }
        s.setzePuck(s.meinX + 70f, s.meinY, 0f, 0f)
        s.fuehre(s.meinX + 150f, s.meinY)
        repeat(10) { s.schritt(0.016f) }
        assertTrue(s.puckVx > 260f)
    }

    private fun tor(s: HockeySpiel, ich: Boolean) {
        // Erst den Anstoß abwarten, vorher steht der Puck still
        repeat(62) { s.schritt(0.016f) }
        if (ich) s.setzePuck(HockeySpiel.RECHTS - 30f, 288f, 600f, 0f) else s.setzePuck(HockeySpiel.LINKS + 30f, 288f, -600f, 0f)
        var n = 0
        while (s.phase == HockeySpiel.Phase.LAEUFT && n < 200) {
            s.schritt(0.016f)
            n += 1
        }
    }

    private fun warteBisLaeuft(s: HockeySpiel) {
        var n = 0
        while (s.phase == HockeySpiel.Phase.PAUSE && n < 400) {
            s.schritt(0.016f)
            n += 1
        }
    }

    @Test
    fun torZaehltUndPuckKommtZumVerlierer() {
        val s = HockeySpiel(Random(2))
        s.beruehre(200f, 288f)
        tor(s, ich = true)
        assertEquals(1, s.toreIch)
        assertEquals(HockeySpiel.Phase.PAUSE, s.phase)
        assertTrue(s.ereignisse.any { it.art == HockeyEreignis.Art.TOR_ICH })
        warteBisLaeuft(s)
        assertEquals(HockeySpiel.Phase.LAEUFT, s.phase)
        assertEquals(512f, s.puckX, 1e-3f)
        // Nach dem Anstoß fliegt der Puck zum Spieler, der das Tor bekommen hat, hier zum Computer, also nach rechts
        repeat(80) { s.schritt(0.016f) }
        assertTrue(s.puckVx != 0f)
    }

    @Test
    fun fuenfToreGebenPunkteUndDasNaechsteLevel() {
        val s = HockeySpiel(Random(2))
        s.beruehre(200f, 288f)
        repeat(5) {
            tor(s, ich = true)
            warteBisLaeuft(s)
        }
        assertEquals(2, s.level)
        assertEquals(0, s.toreIch)
        assertEquals(0, s.toreKi)
        // 10 mal Level 1 plus Vorsprung 5
        assertEquals(15, s.punkte)
        assertTrue(s.ereignisse.any { it.art == HockeyEreignis.Art.SIEG })
        assertEquals(HockeySpiel.Phase.LAEUFT, s.phase)
    }

    @Test
    fun fuenfGegentoreBeendenDasSpiel() {
        val s = HockeySpiel(Random(2))
        var gemeldet = -1
        s.beiEnde = { gemeldet = it }
        s.beruehre(200f, 288f)
        repeat(5) {
            tor(s, ich = false)
            warteBisLaeuft(s)
        }
        assertEquals(HockeySpiel.Phase.VORBEI, s.phase)
        assertEquals(0, gemeldet)
        s.beruehre(200f, 288f)
        assertEquals(HockeySpiel.Phase.LAEUFT, s.phase)
        assertEquals(0, s.toreKi)
        assertEquals(1, s.level)
    }

    @Test
    fun puckBleibtImFeldUndZuSchnell() {
        for (seed in 1..4) {
            val s = HockeySpiel(Random(seed))
            s.beruehre(200f, 288f)
            var t = 0
            while (t < 4000 && s.phase != HockeySpiel.Phase.VORBEI) {
                // Der Spieler wackelt mit dem Schläger hin und her
                s.fuehre(150f + (t % 120).toFloat() * 3f, 100f + (t % 77).toFloat() * 5f)
                s.schritt(0.016f)
                assertTrue(s.puckY >= HockeySpiel.UNTEN - 1f && s.puckY <= HockeySpiel.OBEN + 1f)
                assertTrue(hypot(s.puckVx, s.puckVy) <= HockeyBalance.PUCK_MAX_TEMPO + 1f)
                assertTrue(s.kiX >= HockeySpiel.MITTE && s.kiX <= HockeySpiel.RECHTS)
                t += 1
            }
        }
    }
}
