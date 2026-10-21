package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Haustier
import de.cemaras.yem1n.logik.HaustierStimmung
import de.cemaras.yem1n.logik.HaustierStufe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HaustierTest {
    private val stunde = 3_600_000L

    private fun tier(xp: Int = 100, satt: Double = 80.0, laune: Double = 80.0) =
        Haustier(name = "Pip", art = "hund", xp = xp, satt = satt, laune = laune, zuletzt = 0)

    @Test
    fun stufenNachWachstumspunkten() {
        assertEquals(HaustierStufe.EI, HaustierStufe.fuer(0))
        assertEquals(HaustierStufe.BABY, HaustierStufe.fuer(1))
        assertEquals(HaustierStufe.BABY, HaustierStufe.fuer(59))
        assertEquals(HaustierStufe.KIND, HaustierStufe.fuer(60))
        assertEquals(HaustierStufe.GROSS, HaustierStufe.fuer(250))
        assertEquals(HaustierStufe.MEISTER, HaustierStufe.fuer(5000))
        assertNull(HaustierStufe.MEISTER.naechste)
        assertEquals(HaustierStufe.BABY, HaustierStufe.EI.naechste)
    }

    @Test
    fun namenWerdenSauber() {
        assertEquals("Pip", Haustier.sauber("   "))
        assertEquals("Luna", Haustier.sauber("  Luna "))
        assertEquals(14, Haustier.sauber("A".repeat(40)).length)
        assertEquals("Pip", Haustier.neu("", "katze", 5).name)
        assertEquals(5L, Haustier.neu("", "katze", 5).zuletzt)
    }

    @Test
    fun zeitMachtHungrigUndTraurig() {
        val t = tier().aktualisiert(10 * stunde)
        // 10 Stunden: satt sinkt um 25, noch über der Grenze (55 > 30)
        assertEquals(55.0, t.satt, 1e-9)
        assertEquals(80.0 - 12.0, t.laune, 1e-9)
        // Nach langer Zeit ist alles unten, aber nie unter 0
        val lange = tier().aktualisiert(1000 * stunde)
        assertEquals(0.0, lange.satt, 1e-9)
        assertEquals(0.0, lange.laune, 1e-9)
        assertEquals(HaustierStimmung.HUNGRIG, lange.stimmung)
    }

    @Test
    fun hungerBeschleunigtDenLauneVerlust() {
        // satt 32.5: eine Stunde bis zur Grenze, danach hungrig
        val t = tier(satt = 32.5, laune = 80.0).aktualisiert(3 * stunde)
        assertEquals(80.0 - (1 * 1.2 + 2 * 3.0), t.laune, 1e-9)
    }

    @Test
    fun einEiHatKeinenHunger() {
        val ei = tier(xp = 0).aktualisiert(100 * stunde)
        assertEquals(80.0, ei.satt, 1e-9)
        assertEquals(100 * stunde, ei.zuletzt)
    }

    @Test
    fun uhrZurueckStelltNurDenZeitpunkt() {
        val t = tier().copy(zuletzt = 10 * stunde).aktualisiert(5 * stunde)
        assertEquals(5 * stunde, t.zuletzt)
        assertEquals(80.0, t.satt, 1e-9)
    }

    @Test
    fun stimmungen() {
        assertEquals(HaustierStimmung.FROH, tier(satt = 80.0, laune = 80.0).stimmung)
        assertEquals(HaustierStimmung.ZUFRIEDEN, tier(satt = 50.0, laune = 80.0).stimmung)
        assertEquals(HaustierStimmung.HUNGRIG, tier(satt = 10.0, laune = 80.0).stimmung)
        assertEquals(HaustierStimmung.TRAURIG, tier(satt = 80.0, laune = 10.0).stimmung)
    }

    @Test
    fun rundeLaesstDasEiSchluepfen() {
        val ei = Haustier.neu("Pip", "katze", 0)
        val (neu, e) = ei.lernrunde(richtig = 5, gesamt = 10, angesehen = 0, jetzt = 0)
        // 5 richtig, 1 Stern: 5 + 2
        assertEquals(7, neu.xp)
        assertTrue(e.geschluepft)
        assertFalse(e.aufgestiegen)
        assertEquals(HaustierStufe.BABY, e.stufe)
        assertTrue(neu.meldung(e).contains("geschlüpft"))
    }

    @Test
    fun lernrundeFuettertUndFreut() {
        val t = tier(xp = 10, satt = 40.0, laune = 50.0)
        val (neu, e) = t.lernrunde(10, 10, 0, 0)
        // 3 Sterne: 10 + 6 Punkte, satt + 30, laune + 25
        assertEquals(26, neu.xp)
        assertEquals(70.0, neu.satt, 1e-9)
        assertEquals(75.0, neu.laune, 1e-9)
        assertFalse(e.geschluepft)
        val (kaum, _) = t.lernrunde(0, 10, 0, 0)
        assertEquals(11, kaum.xp)
        assertEquals(60.0, kaum.laune, 1e-9)
    }

    @Test
    fun angesehenGibtKeinenSternbonus() {
        val t = tier(xp = 10)
        val (neu, _) = t.lernrunde(10, 10, 2, 0)
        assertEquals(20, neu.xp)
    }

    @Test
    fun aufstieg() {
        val t = tier(xp = 58)
        val (neu, e) = t.lernrunde(10, 10, 0, 0)
        assertEquals(HaustierStufe.KIND, neu.stufe)
        assertTrue(e.aufgestiegen)
        assertTrue(neu.meldung(e).contains("Kind"))
    }

    @Test
    fun leckerliUndSpielen() {
        val hungrig = tier(satt = 40.0, laune = 50.0)
        assertTrue(hungrig.kannLeckerli(5))
        assertFalse(hungrig.kannLeckerli(4))
        val satt = hungrig.leckerli(5)!!
        assertEquals(65.0, satt.satt, 1e-9)
        assertEquals(53.0, satt.laune, 1e-9)
        assertNull(tier(satt = 96.0).leckerli(100))
        assertNull(tier(xp = 0).leckerli(100))
        val froh = hungrig.spielen(8)!!
        assertEquals(80.0, froh.laune, 1e-9)
        assertNull(tier(laune = 96.0).spielen(100))
        assertNull(hungrig.spielen(7))
        assertNull(tier(xp = 0, laune = 10.0).spielen(100))
    }

    @Test
    fun huete() {
        val t = tier()
        assertFalse(t.kannKaufen("krone", 79))
        assertTrue(t.kannKaufen("krone", 80))
        assertFalse(t.kannKaufen("gibtsnicht", 999))
        val mit = t.kaufe("krone", 80)!!
        assertEquals("krone", mit.hut)
        assertEquals(listOf("krone"), mit.huete)
        // Doppelt kaufen geht nicht
        assertNull(mit.kaufe("krone", 999))
        assertNull(tier(xp = 0).kaufe("krone", 999))
        // Tragen
        assertEquals("", mit.trage("").hut)
        assertEquals("krone", mit.trage("").trage("krone").hut)
        assertEquals("", mit.trage("").trage("brille").hut)
    }

    @Test
    fun tagesgeschenk() {
        val t = tier()
        assertTrue(t.geschenkOffen(100))
        val geholt = t.holeGeschenk(100)
        assertNotNull(geholt)
        assertFalse(geholt!!.geschenkOffen(100))
        assertNull(geholt.holeGeschenk(100))
        assertNotNull(geholt.holeGeschenk(101))
        assertEquals(2, Haustier.GESCHENK_MUENZEN)
    }

    @Test
    fun fortschrittZurNaechstenStufe() {
        assertEquals(0.0, tier(xp = 60).fortschritt, 1e-9)
        assertEquals(0.5, tier(xp = 155).fortschritt, 1e-9)
        assertEquals(1.0, tier(xp = 9999).fortschritt, 1e-9)
    }

    @Test
    fun sprucheKommenZurStimmung() {
        assertTrue(tier(xp = 0).spruch.contains("schlüpfe"))
        assertTrue(tier(satt = 10.0).spruch.contains("Hunger") || tier(satt = 10.0).spruch.contains("Bauch"))
        for (xp in 1..30) assertTrue(tier(xp = xp).spruch.isNotBlank())
    }
}
