package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Runde
import de.cemaras.yem1n.logik.Stand
import de.cemaras.yem1n.logik.ZweiterVersuch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZweiterVersuchTest {
    @Test fun regelnWieIos() {
        assertFalse(ZweiterVersuch.erlaubt("zahl", 0, probe = true))
        assertTrue(ZweiterVersuch.erlaubt("zahl", 0, probe = false))
        assertFalse(ZweiterVersuch.erlaubt("mauer", 0, probe = false))
        assertFalse(ZweiterVersuch.erlaubt("vergleich", 3, probe = false))
        assertFalse(ZweiterVersuch.erlaubt("wahl", 3, probe = false))
        assertTrue(ZweiterVersuch.erlaubt("wahl", 4, probe = false))
        assertTrue(ZweiterVersuch.erlaubt("text", 0, probe = false))
    }

    @Test fun eigenerTippKommtDazu() {
        assertTrue(ZweiterVersuch.text(" Denk an die Zehner. ", 0).endsWith("Tipp: Denk an die Zehner."))
        assertEquals(ZweiterVersuch.saetze[1], ZweiterVersuch.text("", 7))
        assertTrue(ZweiterVersuch.text("", -3).isNotEmpty())
        for (satz in ZweiterVersuch.saetze) assertFalse(satz.contains("–") || satz.contains("—"))
    }

    @Test fun ersterFehlerGibtHinweisUndNochEinenVersuch() {
        var s = Runde.start(listOf(aufgabe()))
        s = Runde.antworte(s, "50", zweiterVersuch = true)
        assertFalse(s.bewertet)
        assertTrue(s.zweiterVersuch)
        assertTrue(s.kurzerHinweis.isNotEmpty())
        s = Runde.antworte(s, "56", zweiterVersuch = true)
        assertEquals(Stand.RICHTIG, s.staende[0])
        assertTrue(s.nachFehler)
        assertFalse(s.zweiterVersuch)
    }

    @Test fun zweiterFehlerZeigtDieLoesung() {
        var s = Runde.start(listOf(aufgabe(erklaerung = "7 · 8 = 56")))
        s = Runde.antworte(s, "50", zweiterVersuch = true)
        s = Runde.antworte(s, "54", zweiterVersuch = true)
        assertEquals(Stand.FALSCH, s.staende[0])
        assertEquals("7 · 8 = 56", s.erklaerung)
    }

    @Test fun ohneSchalterWieBisher() {
        var s = Runde.start(listOf(aufgabe()))
        s = Runde.antworte(s, "50")
        assertEquals(Stand.FALSCH, s.staende[0])
    }

    @Test fun naechsteAufgabeBeginntOhneZweitenVersuch() {
        var s = Runde.start(listOf(aufgabe(), aufgabe()))
        s = Runde.antworte(s, "50", zweiterVersuch = true)
        s = Runde.antworte(s, "56", zweiterVersuch = true)
        s = Runde.weiter(s)
        assertFalse(s.zweiterVersuch)
        assertFalse(s.nachFehler)
    }

    @Test fun loesungZeigenBeendetDenZweitenVersuch() {
        var s = Runde.start(listOf(aufgabe()))
        s = Runde.antworte(s, "50", zweiterVersuch = true)
        s = Runde.zeigeLoesung(s)
        assertEquals(Stand.ANGESCHAUT, s.staende[0])
        assertFalse(s.zweiterVersuch)
    }

    @Test fun auswahlMitZweiKnoepfenOhneZweitenVersuch() {
        var s = Runde.start(listOf(aufgabe(art = "wahl", antwort = "ie", optionen = listOf("i", "ie"))))
        s = Runde.antworte(s, "i", zweiterVersuch = true)
        assertEquals(Stand.FALSCH, s.staende[0])
    }
}
