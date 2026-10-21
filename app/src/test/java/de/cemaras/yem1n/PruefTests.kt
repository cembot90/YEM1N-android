package de.cemaras.yem1n

import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.logik.Mauer
import de.cemaras.yem1n.logik.Pruefer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun aufgabe(
    art: String = "zahl", rechnung: String = "7 · 8 =", antwort: String = "56", antwort2: String = "",
    erklaerung: String = "", reihen: List<List<Int>> = emptyList(), optionen: List<String> = emptyList(),
) = Aufgabe(
    art = art, frage = "Rechne.", rechnung = rechnung, hinweis = "", erklaerung = erklaerung,
    antwort = antwort, antwort2 = antwort2, reihen = reihen, bild = "", tr = "", gross = "",
    folge = emptyList(), optionen = optionen,
)

class PrueferTest {
    @Test fun zahlenWerdenAlsZahlenVerglichen() {
        assertTrue(Pruefer.gleich("56", "56"))
        assertTrue(Pruefer.gleich("056", "56"))
        assertTrue(Pruefer.gleich(" 56 ", "56"))
        assertFalse(Pruefer.gleich("55", "56"))
    }

    @Test fun zahlAufgabe() {
        val a = aufgabe()
        assertEquals(true, Pruefer.pruefe(a, "56"))
        assertEquals(false, Pruefer.pruefe(a, "54"))
        assertNull(Pruefer.pruefe(a, ""))
    }

    @Test fun restAufgabeBrauchtBeideFelder() {
        val a = aufgabe(art = "rest", rechnung = "58 : 4 =", antwort = "14", antwort2 = "2")
        assertNull(Pruefer.pruefe(a, "14", ""))
        assertEquals(true, Pruefer.pruefe(a, "14", "2"))
        assertEquals(false, Pruefer.pruefe(a, "10", "18"))
        assertEquals(false, Pruefer.pruefe(a, "14", "3"))
        assertEquals("14 Rest 2", Pruefer.gegebenText(a, "14", "2"))
    }

    @Test fun textUnterscheidetGrossUndKleinAberNichtLeerzeichen() {
        val a = aufgabe(art = "text", rechnung = "Hun?", antwort = "Hund")
        assertEquals(true, Pruefer.pruefe(a, "Hund"))
        assertEquals(true, Pruefer.pruefe(a, "  Hund "))
        assertEquals(false, Pruefer.pruefe(a, "hund"))
        val satz = aufgabe(art = "text", antwort = "mit dem Auto")
        assertEquals(true, Pruefer.pruefe(satz, "mit   dem Auto"))
        assertEquals(false, Pruefer.pruefe(aufgabe(art = "text", antwort = "Ähre"), "Aehre"))
        assertNull(Pruefer.pruefe(a, "   "))
    }

    @Test fun vergleichUndAuswahl() {
        val v = aufgabe(art = "vergleich", rechnung = "5 · 5  ?  9 · 7", antwort = "<")
        assertEquals(true, Pruefer.pruefe(v, "<"))
        assertEquals(false, Pruefer.pruefe(v, ">"))
        val w = aufgabe(art = "wahl", antwort = "3", optionen = listOf("2", "3", "5"))
        assertEquals(true, Pruefer.pruefe(w, "3"))
        assertEquals(false, Pruefer.pruefe(w, "5"))
    }

    @Test fun sterne() {
        assertEquals(3, Pruefer.sterneFuer(9, 10))
        assertEquals(3, Pruefer.sterneFuer(10, 10))
        assertEquals(2, Pruefer.sterneFuer(8, 10))
        assertEquals(2, Pruefer.sterneFuer(7, 10))
        assertEquals(1, Pruefer.sterneFuer(5, 10))
        assertEquals(0, Pruefer.sterneFuer(4, 10))
        assertEquals(0, Pruefer.sterneFuer(0, 0))
    }
}

class MauerTest {
    private val reihen = listOf(listOf(14, 11, 11, 9), listOf(25, 22, 20), listOf(47, 42), listOf(89))

    @Test fun aufbauWirdGeprueft() {
        assertTrue(Mauer.gueltig(reihen))
        assertFalse(Mauer.gueltig(listOf(listOf(1, 2, 3), listOf(3, 5))))
        assertFalse(Mauer.gueltig(listOf(listOf(1, 2))))
        assertFalse(Mauer.gueltig(emptyList()))
    }

    @Test fun startZeigtNurDieUntersteReihe() {
        val s = Mauer.start(reihen)
        assertEquals(listOf<Int?>(14, 11, 11, 9), s[0])
        assertEquals(listOf<Int?>(null, null, null), s[1])
        assertEquals(listOf<Int?>(null), s[3])
    }

    @Test fun reihenfolgeVonUntenNachObenUndVonLinksNachRechts() {
        assertEquals(6, Mauer.anzahl(reihen))
        assertEquals(1 to 0, Mauer.position(reihen, 0))
        assertEquals(1 to 2, Mauer.position(reihen, 2))
        assertEquals(2 to 0, Mauer.position(reihen, 3))
        assertEquals(3 to 0, Mauer.position(reihen, 5))
        assertNull(Mauer.position(reihen, 6))
    }
}
