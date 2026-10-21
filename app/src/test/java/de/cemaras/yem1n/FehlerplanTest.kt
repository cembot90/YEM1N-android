package de.cemaras.yem1n

import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.Uebung
import de.cemaras.yem1n.logik.FehlerEintrag
import de.cemaras.yem1n.logik.Fehlerheft
import de.cemaras.yem1n.logik.Fehlerplan
import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Lob
import de.cemaras.yem1n.logik.Muenzen
import de.cemaras.yem1n.logik.Serie
import de.cemaras.yem1n.logik.Stand
import de.cemaras.yem1n.logik.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun arbeit(fach: String = "Mathe", anzahl: Int = 3) = Arbeit(
    klasse = "Klasse 3", fach = fach, titel = "Test",
    uebungen = listOf(Uebung("Eins", "G", "+", "", List(anzahl) { aufgabe(antwort = "$it") })),
)

class FehlerplanTest {
    @Test fun ersteRichtigeAntwortKommtNachDreiTagenWieder() {
        val e = Fehlerplan.richtig(FehlerEintrag(), heute = 10)
        assertEquals(FehlerEintrag(13, 1), e)
    }

    @Test fun zweiteRichtigeAntwortMeistertDenFehler() {
        assertNull(Fehlerplan.richtig(FehlerEintrag(13, 1), heute = 13))
    }

    @Test fun falscheAntwortKommtMorgenWieder() {
        assertEquals(FehlerEintrag(11, 0), Fehlerplan.falsch(10))
    }

    @Test fun faelligkeit() {
        assertTrue(Fehlerplan.istFaellig(FehlerEintrag(10), 10))
        assertFalse(Fehlerplan.istFaellig(FehlerEintrag(11), 10))
    }

    @Test fun tagesnummerUeberMitternacht() {
        val tag = 86_400_000L * 100
        assertEquals(100L, Tag.heute(tag + 1000, 0))
        assertEquals(101L, Tag.heute(tag + 86_400_000L - 1000, 3_600_000))
    }
}

class FehlerheftTest {
    @Test fun nurFaelligeFehlerKommenInsHeft() {
        val a = arbeit()
        val k = a.schluessel
        val fehler = mapOf(
            Fehlerheft.schluessel(k, "Eins", 0) to FehlerEintrag(0, 0),
            Fehlerheft.schluessel(k, "Eins", 1) to FehlerEintrag(20, 1),
        )
        assertEquals(1 to 1, Fehlerheft.stand(listOf(a), fehler, heute = 10))
        val heft = Fehlerheft.baue(listOf(a), fehler, 10)
        assertNotNull(heft)
        val aufgaben = heft!!.uebungen.single().aufgaben
        assertEquals(1, aufgaben.size)
        assertEquals(Fehlerheft.schluessel(k, "Eins", 0), aufgaben[0].quellKey)
    }

    @Test fun keinFehlerGibtKeinHeft() {
        assertNull(Fehlerheft.baue(listOf(arbeit()), emptyMap(), 10))
    }

    @Test fun vorschuleHatKeinFehlerheft() {
        val a = arbeit(fach = "Vorschule")
        val fehler = mapOf(Fehlerheft.schluessel(a.schluessel, "Eins", 0) to FehlerEintrag())
        assertNull(Fehlerheft.baue(listOf(a), fehler, 10))
    }

    @Test fun hoechstensFuenfzehnAufgaben() {
        val a = arbeit(anzahl = 20)
        val fehler = (0 until 20).associate { Fehlerheft.schluessel(a.schluessel, "Eins", it) to FehlerEintrag() }
        assertEquals(15, Fehlerheft.baue(listOf(a), fehler, 10)!!.uebungen.single().aufgaben.size)
    }
}

class RundeAbschlussTest {
    private val a = arbeit()
    private val keys = List(3) { Fehlerheft.schluessel(a.schluessel, "Eins", it) }

    @Test fun falscheAufgabeKommtInsFehlerheftUndRichtigeNimmtSieHeraus() {
        val (f1, _) = Fortschritt().nachRundeMit(
            false, a.schluessel, "Eins", keys, listOf(Stand.RICHTIG, Stand.FALSCH, Stand.ANGESCHAUT), heute = 5,
        )
        assertEquals(setOf(keys[1]), f1.fehler.keys)
        val (f2, _) = f1.nachRundeMit(
            false, a.schluessel, "Eins", keys, listOf(Stand.RICHTIG, Stand.RICHTIG, Stand.RICHTIG), heute = 6,
        )
        assertTrue(f2.fehler.isEmpty())
    }

    @Test fun fehlerheftMeistertErstNachZweiMalen() {
        val start = Fortschritt(fehler = mapOf(keys[0] to FehlerEintrag()))
        val (f1, _) = start.nachRundeMit(true, "Fehlerheft", "Fehlerheft", listOf(keys[0]), listOf(Stand.RICHTIG), 10)
        assertEquals(FehlerEintrag(13, 1), f1.fehler[keys[0]])
        val (f2, _) = f1.nachRundeMit(true, "Fehlerheft", "Fehlerheft", listOf(keys[0]), listOf(Stand.RICHTIG), 13)
        assertTrue(f2.fehler.isEmpty())
        assertTrue(f2.eintraege.isEmpty())
    }

    @Test fun fehlerheftFalschStelltAufAnfang() {
        val start = Fortschritt(fehler = mapOf(keys[0] to FehlerEintrag(13, 1)))
        val (f, _) = start.nachRundeMit(true, "x", "x", listOf(keys[0]), listOf(Stand.FALSCH), 13)
        assertEquals(FehlerEintrag(14, 0), f.fehler[keys[0]])
    }

    @Test fun muenzenTagesZielUndSerie() {
        val alle = List(3) { Stand.RICHTIG }
        val (f1, m1) = Fortschritt().nachRundeMit(false, a.schluessel, "Eins", keys, alle, 7)
        assertEquals(3 + 5, m1)
        assertEquals(8, f1.muenzen)
        assertEquals(1, f1.rundenHeute(7))
        val (f2, _) = f1.nachRundeMit(false, a.schluessel, "Eins", keys, alle, 7)
        assertEquals(2, f2.rundenHeute(7))
        assertEquals(listOf(7L), f2.tage)
        assertEquals(0, f2.rundenHeute(8))
    }

    @Test fun fortschrittBleibtBeiAltemJsonLesbar() {
        val f = Fortschritt.ausJson("""{"eintraege":{"a#b":{"beste":3,"sterne":2,"runden":1}}}""")
        assertEquals(2, f.eintraege.getValue("a#b").sterne)
        assertEquals(0, f.muenzen)
        val rund = Fortschritt.ausJson(Fortschritt(muenzen = 9, fehler = mapOf("k" to FehlerEintrag(3, 1))).zuJson())
        assertEquals(9, rund.muenzen)
        assertEquals(FehlerEintrag(3, 1), rund.fehler["k"])
    }
}

class ErfolgeTest {
    @Test fun muenzen() {
        assertEquals(0, Muenzen.verdient(0, 10, 0))
        assertEquals(10 + 5, Muenzen.verdient(10, 10, 0))
        assertEquals(10, Muenzen.verdient(10, 10, 1))
        assertEquals(8 + 2, Muenzen.verdient(8, 10, 0))
        assertEquals(6, Muenzen.verdient(6, 10, 0))
    }

    @Test fun serieZaehltTageHintereinander() {
        assertEquals(3 to 0, Serie.mitSchutz(setOf(8L, 9L, 10L), heute = 10))
        // Heute noch nichts geübt: gestern zählt noch
        assertEquals(2 to 0, Serie.mitSchutz(setOf(8L, 9L), heute = 10))
        assertEquals(0 to 0, Serie.mitSchutz(emptySet(), heute = 10))
        assertEquals(0 to 0, Serie.mitSchutz(setOf(5L), heute = 10))
    }

    @Test fun einFreierTagRettetDieSerieNurEinmalProWoche() {
        // 10 geübt, 9 frei, 8 und 7 geübt
        assertEquals(3 to 1, Serie.mitSchutz(setOf(10L, 8L, 7L), heute = 10))
        // zwei Lücken innerhalb von 7 Tagen: nur die erste wird gerettet
        assertEquals(2 to 1, Serie.mitSchutz(setOf(10L, 8L, 6L), heute = 10))
        // zwei verpasste Tage hintereinander beenden die Serie
        assertEquals(1 to 0, Serie.mitSchutz(setOf(10L, 7L), heute = 10))
    }
}

class LobTest {
    @Test fun serienLob() {
        assertNotNull(Lob.serienLob(3))
        assertNull(Lob.serienLob(4))
        assertTrue(Lob.serienLob(20)!!.contains("20"))
    }

    @Test fun vorname() {
        assertEquals("Jonas", Lob.vorname("jonas"))
        assertEquals("Jonas", Lob.vorname("JONAS müller"))
        assertEquals("LeAnn", Lob.vorname("LeAnn"))
        assertEquals("", Lob.vorname("  "))
    }

    @Test fun nameWirdEingesetztOderFaelltWeg() {
        assertEquals("Super, Ida!", Lob.einsetzen("Super, {name}!", "Ida"))
        assertEquals("Super!", Lob.einsetzen("Super, {name}!", ""))
        assertEquals("Du bist spitze!", Lob.einsetzen("{name}, du bist spitze!", ""))
    }

    @Test fun lobHatNieUnbenutztePlatzhalterUndIstNichtLeer() {
        repeat(200) {
            val t = Lob.fuerRichtig(it % 4 + 1, it % 3 == 0, if (it % 2 == 0) "Yemin" else "")
            assertTrue(t.isNotBlank())
            assertFalse(t.contains("{name}"))
            assertFalse(Lob.fuerEnde(it % 11, 10, "").contains("{name}"))
        }
    }
}
