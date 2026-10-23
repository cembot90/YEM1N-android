package de.cemaras.yem1n

import de.cemaras.yem1n.logik.AufgabenBauer
import de.cemaras.yem1n.logik.EntwurfListe
import de.cemaras.yem1n.logik.EntwurfPruefer
import de.cemaras.yem1n.logik.FormArt
import de.cemaras.yem1n.logik.FormEingabe
import de.cemaras.yem1n.logik.MatheRechner
import de.cemaras.yem1n.logik.PaketJson
import de.cemaras.yem1n.daten.PaketLeser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTest {
    @Test
    fun rechnerPunktVorStrichUndKlammern() {
        assertEquals(26, MatheRechner.werte("2 + 3 · 8"))
        assertEquals(40, MatheRechner.werte("(2 + 3) * 8"))
        assertEquals(6, MatheRechner.werte("48 : 8 ="))
        assertEquals(3, MatheRechner.werte("10 − 7"))
    }

    @Test
    fun rechnerMeldetFehlerFreundlich() {
        for (s in listOf("", "5 +", "7 : 2", "4 : 0", "(3 + 4", "3 a 4", "99999999999")) {
            try {
                MatheRechner.werte(s)
                throw AssertionError("Kein Fehler bei '$s'")
            } catch (e: MatheRechner.Fehler) {
                assertTrue(e.text.isNotEmpty())
            }
        }
    }

    @Test
    fun anzeigeSchreibtSchoen() {
        assertEquals("9 · 9 + 8", MatheRechner.anzeige("9*9+8"))
        assertEquals("48 : 4", MatheRechner.anzeige("48/4"))
    }

    @Test
    fun rechnungWirdBerechnet() {
        val e = AufgabenBauer.baue(FormEingabe(FormArt.RECHNUNG, ausdruck = "7*8"))
        assertEquals("7 · 8 =", e.aufgabe!!.rechnung)
        assertEquals("56", e.aufgabe!!.antwort)
        assertEquals("Rechne.", e.aufgabe!!.frage)
    }

    @Test
    fun ergebnisAusserhalbWirdAbgelehnt() {
        assertNotNull(AufgabenBauer.baue(FormEingabe(FormArt.RECHNUNG, ausdruck = "10-20")).fehler)
        assertNotNull(AufgabenBauer.baue(FormEingabe(FormArt.RECHNUNG, ausdruck = "100*100")).fehler)
    }

    @Test
    fun restUndVergleich() {
        val r = AufgabenBauer.baue(FormEingabe(FormArt.REST, zahlA = "58", zahlB = "4")).aufgabe!!
        assertEquals("14", r.antwort); assertEquals("2", r.antwort2); assertEquals("58 : 4 = 14 R 2", r.erklaerung)
        assertNotNull(AufgabenBauer.baue(FormEingabe(FormArt.REST, zahlA = "58", zahlB = "1")).fehler)
        val v = AufgabenBauer.baue(FormEingabe(FormArt.VERGLEICH, links = "5*5", rechts = "9*7")).aufgabe!!
        assertEquals("5 · 5  ?  9 · 7", v.rechnung); assertEquals("<", v.antwort); assertEquals("25 < 63", v.erklaerung)
        assertEquals("=", AufgabenBauer.baue(FormEingabe(FormArt.VERGLEICH, links = "3*4", rechts = "4*3")).aufgabe!!.antwort)
    }

    @Test
    fun mauerWirdAufgebaut() {
        val a = AufgabenBauer.baue(FormEingabe(FormArt.MAUER, mauer = listOf("14", "11", "11", "9"))).aufgabe!!
        assertEquals(listOf(listOf(14, 11, 11, 9), listOf(25, 22, 20), listOf(47, 42), listOf(89)), a.reihen)
        assertNotNull(AufgabenBauer.baue(FormEingabe(FormArt.MAUER, mauer = listOf("1", "2", "", "4"))).fehler)
    }

    @Test
    fun raetselMussEindeutigSein() {
        val ok = AufgabenBauer.baue(FormEingabe(FormArt.RAETSEL, gerade = true, xer = 9, von = "21", bis = "44")).aufgabe!!
        assertEquals("36", ok.antwort)
        assertEquals("Meine Zahl ist gerade.\nSie ist eine Neunerzahl.\nSie liegt zwischen 21 und 44.", ok.rechnung)
        val viele = AufgabenBauer.baue(FormEingabe(FormArt.RAETSEL, gerade = true, xer = 2, von = "1", bis = "20"))
        assertNull(viele.aufgabe); assertTrue(viele.fehler!!.contains("eindeutig"))
    }

    @Test
    fun sachUndWortUndFrei() {
        val s = AufgabenBauer.baue(FormEingabe(FormArt.SACH, text = "Jule hat 4 Kisten mit je 6 Äpfeln.", ausdruck = "4*6")).aufgabe!!
        assertEquals("24", s.antwort); assertEquals("4 · 6 = 24", s.erklaerung)
        val w = AufgabenBauer.baue(FormEingabe(FormArt.WORT, zahlA = "75", zahlB = "8", wortMinus = true)).aufgabe!!
        assertEquals("67", w.antwort); assertEquals("Subtrahiere von 75 die Zahl 8.", w.rechnung)
        val m = AufgabenBauer.baue(FormEingabe(FormArt.WORT, zahlA = "7", zahlB = "8", wortMinus = false, wortVariante = 1)).aufgabe!!
        assertEquals("56", m.antwort)
        assertNotNull(AufgabenBauer.baue(FormEingabe(FormArt.WORT, zahlA = "5", zahlB = "8", wortMinus = true)).fehler)
        val f = AufgabenBauer.baue(FormEingabe(FormArt.FREI, freiRechnung = "Wie viele Ecken hat ein Würfel?", freiAntwort = "8"))
        assertEquals("8", f.aufgabe!!.antwort); assertNotNull(f.hinweis)
    }

    @Test
    fun prueferFindetFalscheLoesungen() {
        val gut = AufgabenBauer.baue(FormEingabe(FormArt.RECHNUNG, ausdruck = "7*8")).aufgabe!!
        val schlecht = gut.copy(antwort = "54", rechnung = "7 · 8 =  ", erklaerung = "x")
        val u = EntwurfListe.uebungNeu("Test").copy(aufgaben = listOf(schlecht))
        val b = EntwurfPruefer.pruefe(EntwurfListe.neu("Klasse 3", "Mathe", "Nr. 1").copy(uebungen = listOf(u)))
        assertTrue(b.any { it.schwer && it.text.contains("ergibt 56") })
    }

    @Test
    fun prueferFindetGedankenstricheUndLeeres() {
        val a = AufgabenBauer.baue(FormEingabe(FormArt.RECHNUNG, ausdruck = "7*8")).aufgabe!!.copy(hinweis = "7 – 1")
        val arbeit = EntwurfListe.neu("Klasse 3", "Mathe", "").copy(uebungen = listOf(EntwurfListe.uebungNeu("A").copy(aufgaben = listOf(a)), EntwurfListe.uebungNeu("B")))
        val t = EntwurfPruefer.pruefe(arbeit).map { it.text }
        assertTrue(t.any { it.contains("Titel") })
        assertTrue(t.any { it.contains("Gedankenstrich") })
        assertTrue(t.any { it.contains("keine Aufgaben") })
    }

    @Test
    fun komplettErzeugteArbeitIstOhneBefundUndLaesstSichLesen() {
        val vorhanden = mutableSetOf<String>()
        val arbeit = EntwurfListe.ausVorlagen("Klasse 3", "Mathe", "Klassenarbeit Nr. 9", vorhanden)
        assertEquals(13, arbeit.uebungen.size)
        val schwer = EntwurfPruefer.pruefe(arbeit).filter { it.schwer }
        assertTrue(schwer.joinToString { it.text }, schwer.isEmpty())
        val gelesen = PaketLeser.lese(PaketJson.text(arbeit)).arbeit!!
        assertEquals(arbeit.uebungen.sumOf { it.aufgaben.size }, gelesen.uebungen.sumOf { it.aufgaben.size })
    }

    @Test
    fun verschiebenUndUebungAusVorlage() {
        assertEquals(listOf(2, 1, 3), EntwurfListe.verschiebe(listOf(1, 2, 3), 1, 0))
        assertEquals(listOf(1, 2, 3), EntwurfListe.verschiebe(listOf(1, 2, 3), 5, 0))
    }
}
