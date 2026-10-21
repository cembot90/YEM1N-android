package de.cemaras.yem1n

import de.cemaras.yem1n.logik.LaufBalance
import de.cemaras.yem1n.logik.LaufObjekt
import de.cemaras.yem1n.logik.LaufSpiel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LaufTest {
    private fun laufendes(seed: Int = 1): LaufSpiel {
        val s = LaufSpiel(Random(seed))
        s.druecke()
        s.loslassen()
        return s
    }

    @Test
    fun stufenUndTempo() {
        assertEquals(1, LaufBalance.stufe(0))
        assertEquals(1, LaufBalance.stufe(119))
        assertEquals(2, LaufBalance.stufe(120))
        assertEquals(8, LaufBalance.stufe(99999))
        assertEquals(320f, LaufBalance.tempo(1))
        assertEquals(684f, LaufBalance.tempo(8))
        assertEquals(684f, LaufBalance.tempo(80))
    }

    @Test
    fun immerGenugZeitZwischenHindernissen() {
        for (stufe in 1..LaufBalance.HOECHSTE_STUFE) {
            assertTrue(
                "Stufe $stufe",
                LaufBalance.zeitZwischenHindernissen(stufe) >= LaufBalance.KUERZESTE_REAKTION,
            )
            val (klein, gross) = LaufBalance.abstand(stufe)
            assertTrue(klein <= gross)
        }
    }

    @Test
    fun fliegerErstAbStufeZwei() {
        assertEquals(0.0, LaufBalance.fliegerAnteil(1), 1e-9)
        assertEquals(0.18, LaufBalance.fliegerAnteil(2), 1e-9)
        assertEquals(0.42, LaufBalance.fliegerAnteil(8), 1e-9)
    }

    @Test
    fun ersterTippStartetNochKeinSprung() {
        val s = LaufSpiel(Random(1))
        s.druecke()
        assertEquals(LaufSpiel.Phase.LAEUFT, s.phase)
        assertEquals(0f, s.vy)
    }

    @Test
    fun springenUndLandenMitDoppelsprung() {
        val s = laufendes()
        s.druecke()
        s.loslassen()
        assertTrue(s.vy > 0f)
        assertTrue(!s.amBoden)
        repeat(5) { s.schritt(0.016f) }
        val vorher = s.vy
        s.druecke()
        s.loslassen()
        assertTrue(s.vy > 0f)
        // Ein dritter Sprung ohne Flügel geht nicht
        repeat(3) { s.schritt(0.016f) }
        val vyVorDritten = s.vy
        s.druecke()
        s.loslassen()
        assertEquals(vyVorDritten * 0.55f, s.vy, 1e-3f)
        assertTrue(vorher > 0f)
        repeat(200) {
            s.objekte.clear()
            s.schritt(0.016f)
        }
        assertTrue(s.amBoden)
        assertEquals(LaufSpiel.RUHE_Y, s.y)
    }

    @Test
    fun halbeSprungKraftBeiKurzemTippen() {
        val kurz = laufendes()
        kurz.druecke()
        kurz.loslassen()
        val lang = laufendes()
        lang.druecke()
        assertTrue(kurz.vy < lang.vy)
        repeat(30) {
            kurz.objekte.clear()
            lang.objekte.clear()
            kurz.schritt(0.016f)
            lang.schritt(0.016f)
        }
        assertTrue(lang.y > kurz.y)
    }

    @Test
    fun hindernisAmBodenKostetEinLeben() {
        val s = laufendes()
        s.objekte.add(LaufObjekt(LaufObjekt.Art.HINDERNIS, LaufSpiel.SPIELER_X, LaufSpiel.BODEN_Y + 29f))
        s.schritt(0.001f)
        assertEquals(2, s.leben)
        assertTrue(s.unverwundbarRest > 1f)
        assertTrue(s.auaRest > 0f)
    }

    @Test
    fun uebersprungenesHindernisGibtZweiPunkte() {
        val s = laufendes()
        s.objekte.add(LaufObjekt(LaufObjekt.Art.HINDERNIS, -89f, LaufSpiel.BODEN_Y + 29f))
        s.schritt(0.016f)
        assertEquals(2, s.punkte)
        assertEquals(3, s.leben)
    }

    @Test
    fun muenzeGibtZehnPunkte() {
        val s = laufendes()
        s.objekte.add(LaufObjekt(LaufObjekt.Art.MUENZE, LaufSpiel.SPIELER_X, s.y))
        s.schritt(0.001f)
        assertEquals(10, s.punkte)
        assertEquals(1, s.ereignisse.size)
    }

    @Test
    fun federSchleudertHochUndFluegelGebenDritteSprung() {
        val s = laufendes()
        s.objekte.add(LaufObjekt(LaufObjekt.Art.FEDER, LaufSpiel.SPIELER_X, s.y))
        s.schritt(0.001f)
        assertTrue(s.vy > 1000f)
        assertTrue(!s.amBoden)

        val f = laufendes(2)
        f.objekte.add(LaufObjekt(LaufObjekt.Art.FLUEGEL, LaufSpiel.SPIELER_X, f.y))
        f.schritt(0.001f)
        assertTrue(f.fluegelRest > 7f)
        f.druecke(); f.loslassen()
        repeat(3) { f.schritt(0.016f) }
        f.druecke(); f.loslassen()
        repeat(3) { f.schritt(0.016f) }
        val vor = f.vy
        f.druecke()
        assertTrue(f.vy > vor)
    }

    @Test
    fun nachDreiTreffernIstSchluss() {
        val s = laufendes()
        var gemeldet = -1
        s.beiEnde = { gemeldet = it }
        repeat(3) {
            repeat(110) {
                s.objekte.clear()
                s.schritt(0.016f)
            }
            s.objekte.add(LaufObjekt(LaufObjekt.Art.HINDERNIS, LaufSpiel.SPIELER_X, LaufSpiel.BODEN_Y + 29f))
            s.schritt(0.001f)
        }
        assertEquals(LaufSpiel.Phase.VORBEI, s.phase)
        assertEquals(s.punkte, gemeldet)
        s.druecke()
        assertEquals(LaufSpiel.Phase.LAEUFT, s.phase)
        assertEquals(3, s.leben)
        assertEquals(0, s.punkte)
        assertTrue(s.objekte.isEmpty())
    }

    @Test
    fun stufeSteigtMitPunkten() {
        val s = laufendes()
        repeat(12) { s.objekte.add(LaufObjekt(LaufObjekt.Art.MUENZE, LaufSpiel.SPIELER_X, s.y)) }
        s.schritt(0.001f)
        assertEquals(120, s.punkte)
        assertEquals(2, s.stufe)
    }

    @Test
    fun einKindDasNichtSpringtKommtTrotzdemNichtDurchDieWelt() {
        // Dauerlauf ohne Eingabe: das Spiel endet irgendwann, ohne Absturz
        val s = laufendes(5)
        repeat(20000) { s.schritt(0.016f) }
        assertEquals(LaufSpiel.Phase.VORBEI, s.phase)
    }

    @Test
    fun gutesSpringenSchafftLangeStrecken() {
        // Ein einfacher Bot springt vor jedem Hindernis. Er darf auf Stufe 1 mindestens 40 Hindernisse schaffen.
        val s = laufendes(7)
        repeat(6000) {
            val naechstes = s.objekte
                .filter { it.art == LaufObjekt.Art.HINDERNIS && it.x > LaufSpiel.SPIELER_X }
                .minByOrNull { it.x }
            if (naechstes != null && naechstes.x - LaufSpiel.SPIELER_X < 150f && s.amBoden) {
                s.druecke()
            } else if (s.vy > 0f) {
                // halten, bis oben
            } else {
                s.loslassen()
            }
            s.schritt(0.016f)
        }
        assertTrue("Leben ${s.leben}, Punkte ${s.punkte}", s.punkte > 100)
    }
}
