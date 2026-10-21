package de.cemaras.yem1n

import de.cemaras.yem1n.logik.ElfmeterRegeln
import de.cemaras.yem1n.logik.ElfmeterRegeln.Ergebnis
import de.cemaras.yem1n.logik.ElfmeterSpiel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ElfmeterTest {
    private val R = ElfmeterRegeln

    @Test
    fun zielLiegtNachObenWischenImTor() {
        val z = R.ziel(0f, 200f)
        assertEquals(R.TOR_MITTE, z.first, 1e-3f)
        assertEquals(R.TOR_UNTEN + 60f, z.second, 1e-3f)
        assertTrue(R.istImTor(z.first, z.second))
        // Kurzes Wischen zielt tief
        assertEquals(R.TOR_UNTEN, R.ziel(0f, 50f).second, 1e-3f)
        // Zu weit nach oben geht über die Latte
        assertFalse(R.istImTor(R.TOR_MITTE, R.ziel(0f, 500f).second))
    }

    @Test
    fun flugzeitBleibtInGrenzen() {
        assertEquals(0.85, R.flugzeit(0f), 1e-9)
        assertEquals(0.45, R.flugzeit(3000f), 1e-9)
        assertEquals(0.45, R.flugzeit(99999f), 1e-9)
        assertEquals(0.85, R.flugzeit(-5f), 1e-9)
        assertTrue(R.flugzeit(1500f) < R.flugzeit(500f))
    }

    @Test
    fun winkelErkennung() {
        assertTrue(R.istWinkel(300f, 350f))
        assertTrue(R.istWinkel(R.TOR_MITTE, 450f))
        assertFalse(R.istWinkel(R.TOR_MITTE, 350f))
        assertFalse(R.istWinkel(100f, 350f))
    }

    @Test
    fun torwartWirdMitDemLevelBesser() {
        for (l in 1..20) {
            assertTrue(R.reaktion(l) >= 0.08)
            assertTrue(R.tempo(l) <= 950f)
            assertTrue(R.falscheEckeChance(l) >= 0.06)
            assertTrue(R.sprungHoehe(l) < R.TOR_OBEN)
        }
        assertTrue(R.reaktion(5) < R.reaktion(1))
        assertTrue(R.tempo(5) > R.tempo(1))
        assertTrue(R.falscheEckeChance(5) < R.falscheEckeChance(1))
        assertTrue(R.sprungHoehe(9) > R.sprungHoehe(1))
    }

    @Test
    fun urteilDanebenUndGehaltenUndTor() {
        // Über die Latte
        assertEquals(Ergebnis.DANEBEN, R.urteil(R.TOR_MITTE, 600f, R.TOR_MITTE, 0.5, 1, 0.9).ergebnis)
        // Links am Pfosten vorbei
        assertEquals(Ergebnis.DANEBEN, R.urteil(200f, 350f, R.TOR_MITTE, 0.5, 1, 0.9).ergebnis)
        // Genau auf den Torwart
        assertEquals(Ergebnis.GEHALTEN, R.urteil(R.TOR_MITTE, 350f, R.TOR_MITTE, 0.85, 1, 0.9).ergebnis)
        // In die Ecke, schneller Schuss
        assertEquals(Ergebnis.WINKEL, R.urteil(292f, 360f, R.TOR_MITTE, 0.45, 1, 0.9).ergebnis)
        // Sehr hoch kommt der Torwart auf Level 1 nicht hin
        assertEquals(Ergebnis.WINKEL, R.urteil(R.TOR_MITTE, 450f, R.TOR_MITTE, 0.85, 1, 0.9).ergebnis)
    }

    @Test
    fun verschaetzterTorwartSpringtInDieFalscheEcke() {
        val gut = R.urteil(300f, 350f, R.TOR_MITTE, 0.85, 1, 0.9)
        assertTrue(gut.torwartZielX < R.TOR_MITTE)
        val falsch = R.urteil(300f, 350f, R.TOR_MITTE, 0.85, 1, 0.0)
        assertTrue(falsch.torwartZielX > R.TOR_MITTE)
        assertEquals(Ergebnis.WINKEL, falsch.ergebnis)
    }

    @Test
    fun torwartBleibtImTor() {
        for (zufall in listOf(0.0, 0.5, 0.99)) for (x in listOf(260f, 512f, 760f)) {
            val u = R.urteil(x, 350f, R.TOR_MITTE, 0.85, 12, zufall)
            assertTrue(u.torwartZielX >= R.TOR_LINKS + 30f && u.torwartZielX <= R.TOR_RECHTS - 30f)
        }
    }

    @Test
    fun punkteUndLevel() {
        assertEquals(1, R.punkte(false, 1))
        assertEquals(3, R.punkte(true, 2))
        assertEquals(2, R.punkte(false, 3))
        assertEquals(4, R.punkte(true, 5))
        assertEquals(1, R.level(0))
        assertEquals(1, R.level(4))
        assertEquals(2, R.level(5))
        assertEquals(3, R.level(10))
        assertEquals(1, R.level(-3))
    }

    private fun spiel(): ElfmeterSpiel = ElfmeterSpiel(Random(5))

    private fun fertig(s: ElfmeterSpiel) {
        var n = 0
        while ((s.phase == ElfmeterSpiel.Phase.FLUG || s.phase == ElfmeterSpiel.Phase.NACHSPIEL) && n < 1000) {
            s.schritt(0.016f)
            n += 1
        }
    }

    @Test
    fun startUndWippen() {
        val s = spiel()
        assertEquals(ElfmeterSpiel.Phase.WARTEN, s.phase)
        val x0 = s.torwartX
        repeat(40) { s.schritt(0.016f) }
        assertTrue(s.torwartX != x0)
        assertTrue(Math.abs(s.torwartX - R.TOR_MITTE) <= 36.01f)
        s.beruehre(512f, 100f)
        assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
    }

    @Test
    fun wischMussUntenAnfangenUndLangGenugSein() {
        val s = spiel()
        s.beruehre(512f, 450f)
        s.loslassen(512f, 600f, 0.2f)
        assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
        s.beruehre(512f, 100f)
        s.bewege(512f, 200f)
        assertTrue(s.vorschau != null)
        s.loslassen(512f, 120f, 0.2f)
        assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
        assertNull(s.vorschau)
        s.beruehre(512f, 100f)
        s.loslassen(512f, 130f, 0.2f)
        assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
        s.beruehre(512f, 100f)
        s.abbrechen()
        s.loslassen(512f, 400f, 0.2f)
        assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
        s.beruehre(512f, 100f)
        s.loslassen(512f, 400f, 0.2f)
        assertEquals(ElfmeterSpiel.Phase.FLUG, s.phase)
    }

    @Test
    fun toreBringenPunkteUndLevel() {
        val s = spiel()
        // Ecke links, schneller Schuss: kein Torwart erreicht das auf Level 1
        repeat(5) {
            s.beruehre(512f, 100f)
            // Der Torwart braucht einen Moment, bis er wieder in der Mitte wippt
            repeat(30) { s.schritt(0.016f) }
            s.schiesseAuf(-146f, 200f, 0.45)
            fertig(s)
        }
        assertEquals(5, s.tore)
        assertEquals(0, s.fehlschuesse)
        assertEquals(2, s.level)
        // 3 + 3 + 4 + 4 + 4
        assertEquals(18, s.punkte)
        assertTrue(s.ereignisse.any { it.text.startsWith("WINKEL") })
        assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
    }

    @Test
    fun dreiFehlschuesseBeendenDasSpiel() {
        val s = spiel()
        var gemeldet = -1
        s.beiEnde = { gemeldet = it }
        s.beruehre(512f, 100f)
        repeat(3) {
            assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
            // Weit über die Latte
            s.schiesseAuf(0f, 500f, 0.5)
            fertig(s)
        }
        assertEquals(3, s.fehlschuesse)
        assertEquals(ElfmeterSpiel.Phase.VORBEI, s.phase)
        assertEquals(0, gemeldet)
        assertTrue(s.ereignisse.count { it.text == "Daneben" } == 3)
        s.beruehre(512f, 100f)
        assertEquals(ElfmeterSpiel.Phase.BEREIT, s.phase)
        assertEquals(0, s.fehlschuesse)
        assertEquals(0, s.punkte)
        assertEquals(1, s.level)
        assertEquals(1f, s.ballAlpha, 1e-6f)
        assertEquals(R.BALL_X, s.ballX, 1e-3f)
    }

    @Test
    fun ballFliegtZumZielUndFaelltZurueck() {
        val s = spiel()
        s.beruehre(512f, 100f)
        s.schiesseAuf(0f, 200f, 0.5)
        repeat(10) { s.schritt(0.016f) }
        assertTrue(s.ballY > R.BALL_Y)
        assertTrue(s.ballSkala < 1f)
        fertig(s)
        assertEquals(R.BALL_X, s.ballX, 1e-3f)
        assertEquals(R.BALL_Y, s.ballY, 1e-3f)
        assertEquals(1f, s.ballSkala, 1e-6f)
    }

    @Test
    fun serieWirdNachFehlschussZurueckgesetzt() {
        val s = spiel()
        s.beruehre(512f, 100f)
        s.schiesseAuf(-146f, 200f, 0.45)
        fertig(s)
        assertEquals(1, s.serie)
        s.schiesseAuf(0f, 500f, 0.5)
        fertig(s)
        assertEquals(0, s.serie)
        assertEquals(1, s.fehlschuesse)
    }
}
