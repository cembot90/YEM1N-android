package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Runde
import de.cemaras.yem1n.logik.Stand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RundeTest {
    private val mauer = aufgabe(
        art = "mauer", rechnung = "",
        reihen = listOf(listOf(3, 4, 5, 6), listOf(7, 9, 11), listOf(16, 20), listOf(36)),
    )

    @Test fun richtigUndFalschWerdenGezaehlt() {
        var s = Runde.start(listOf(aufgabe(), aufgabe(antwort = "42", rechnung = "6 · 7 =")))
        s = Runde.antworte(s, "56")
        assertEquals(Stand.RICHTIG, s.staende[0])
        assertTrue(s.bewertet)
        s = Runde.weiter(s)
        s = Runde.antworte(s, "40")
        assertEquals(Stand.FALSCH, s.staende[1])
        assertEquals("6 · 7 = 42", s.erklaerung)
        s = Runde.weiter(s)
        assertTrue(s.fertig)
        assertEquals(1, s.richtig)
        assertEquals(1, s.falsch)
        assertEquals(0, s.angeschaut)
    }

    @Test fun leereEingabeAendertNichts() {
        val s = Runde.start(listOf(aufgabe()))
        assertEquals(s, Runde.antworte(s, ""))
    }

    @Test fun nachDerBewertungZaehltEineZweiteAntwortNicht() {
        var s = Runde.start(listOf(aufgabe()))
        s = Runde.antworte(s, "1")
        s = Runde.antworte(s, "56")
        assertEquals(Stand.FALSCH, s.staende[0])
    }

    @Test fun weiterGehtErstNachDerBewertung() {
        val s = Runde.start(listOf(aufgabe(), aufgabe()))
        assertEquals(0, Runde.weiter(s).index)
    }

    @Test fun loesungZeigenZaehltWederRichtigNochFalsch() {
        var s = Runde.start(listOf(aufgabe(erklaerung = "7 · 8 = 56")))
        s = Runde.zeigeLoesung(s)
        assertEquals(Stand.ANGESCHAUT, s.staende[0])
        assertEquals(0, s.richtig)
        assertEquals(0, s.falsch)
        assertEquals(1, s.angeschaut)
        assertEquals("", s.gegeben[0])
        assertEquals("7 · 8 = 56", s.erklaerung)
    }

    @Test fun restAufgabeMitBeidenFeldern() {
        var s = Runde.start(listOf(aufgabe(art = "rest", rechnung = "58 : 4 =", antwort = "14", antwort2 = "2")))
        assertEquals(s, Runde.antworte(s, "14", ""))
        s = Runde.antworte(s, "14", "2")
        assertEquals(Stand.RICHTIG, s.staende[0])
        assertEquals("14 Rest 2", s.gegeben[0])
    }

    @Test fun zahlenmauerKomplettRichtig() {
        var s = Runde.start(listOf(mauer))
        assertEquals(6, s.aufgaben[0].reihen.drop(1).sumOf { it.size })
        for ((i, zahl) in listOf("7", "9", "11", "16", "20", "36").withIndex()) {
            s = Runde.antworte(s, zahl)
            if (i < 5) {
                assertFalse(s.bewertet)
                assertEquals("Richtig. Weiter nach oben.", s.kurzerHinweis)
            }
        }
        assertEquals(Stand.RICHTIG, s.staende[0])
        assertEquals(36, s.mauerWerte[3][0])
    }

    @Test fun zahlenmauerFalscheZahlZeigtDieRechnung() {
        var s = Runde.start(listOf(mauer))
        s = Runde.antworte(s, "7")
        s = Runde.antworte(s, "10")
        assertEquals(Stand.FALSCH, s.staende[0])
        assertEquals("Hier gehört 9 hin: 4 + 5 = 9", s.erklaerung)
        assertEquals("10", s.gegeben[0])
        assertEquals(9, s.mauerWerte[1][1])
    }

    @Test fun zahlenmauerLoesungZeigen() {
        var s = Runde.start(listOf(mauer))
        s = Runde.zeigeLoesung(s)
        assertEquals(Stand.ANGESCHAUT, s.staende[0])
        assertEquals(listOf<Int?>(16, 20), s.mauerWerte[2])
    }

    @Test fun nachDerLetztenAufgabeIstDieRundeFertig() {
        var s = Runde.start(listOf(aufgabe()))
        assertNull(s.aktuell.takeIf { s.fertig })
        s = Runde.weiter(Runde.antworte(s, "56"))
        assertTrue(s.fertig)
        assertNull(s.aktuell)
        assertEquals(3, s.sterne)
    }
}
