package de.cemaras.yem1n

import de.cemaras.yem1n.logik.EnglischThema
import de.cemaras.yem1n.logik.EnglischWort
import de.cemaras.yem1n.logik.VorschulEnglisch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VorschulEnglischTest {
    @Test
    fun jedesThemaHatGenugWoerterMitEindeutigenBildern() {
        for (t in EnglischThema.mitWoertern) {
            val ws = t.woerter
            assertTrue("${t.titel} hat zu wenige Wörter", ws.size >= 8)
            assertEquals("Doppelte Bilder in ${t.titel}", ws.size, ws.map { it.bild }.toSet().size)
            assertEquals("Doppelte Wörter in ${t.titel}", ws.size, ws.map { it.en }.toSet().size)
            for (w in ws) {
                assertTrue(w.en.isNotBlank() && w.de.isNotBlank() && w.bild.isNotBlank())
            }
        }
        assertEquals(EnglischThema.mitWoertern.sumOf { it.woerter.size }, EnglischThema.GEMISCHT.woerter.size)
    }

    @Test
    fun aufforderungenFolgenDerGrammatik() {
        val hund = EnglischWort("🐶", "der Hund", "dog")
        val hose = EnglischWort("👖", "die Hose", "trousers", mehrzahl = true)
        val rot = EnglischWort("🔴", "rot", "red", ohneArtikel = true)
        assertEquals("Tap the dog!", VorschulEnglisch.aufforderung(hund, 0))
        assertEquals("Where is the dog?", VorschulEnglisch.aufforderung(hund, 1))
        assertEquals("Find the dog!", VorschulEnglisch.aufforderung(hund, 2))
        assertEquals("Where are the trousers?", VorschulEnglisch.aufforderung(hose, 1))
        assertEquals("Tap red!", VorschulEnglisch.aufforderung(rot, 3))
        assertEquals("dog: der Hund", VorschulEnglisch.bedeutung(hund))
        assertEquals("dog", VorschulEnglisch.anhoeren(hund))
    }

    @Test
    fun fragenSindGueltig() {
        for (t in EnglischThema.entries) for (seed in 0 until 30) {
            val fragen = VorschulEnglisch.fragen(t, 8, Random(seed))
            assertEquals(8, fragen.size)
            val ziele = fragen.map { it.text.en!! }
            assertEquals("Doppelte Ziele in ${t.titel}", ziele.size, ziele.toSet().size)
            for (f in fragen) {
                assertEquals(3, f.optionen.size)
                assertEquals(3, f.optionen.toSet().size)
                assertTrue(f.richtig in 0..2)
                assertTrue(f.text.de.contains(": "))
                assertEquals("en", f.text.sprache("en"))
            }
        }
    }

    @Test
    fun richtigesBildPasstZumWort() {
        val fragen = VorschulEnglisch.fragen(EnglischThema.TIERE, 8, Random(1))
        for (f in fragen) {
            val wort = f.text.de.substringBefore(":")
            val erwartet = EnglischThema.TIERE.woerter.first { it.en == wort }.bild
            assertEquals(erwartet, f.optionen[f.richtig])
        }
    }

    @Test
    fun gemischtZiehtAusVielenThemen() {
        val fragen = VorschulEnglisch.fragen(EnglischThema.GEMISCHT, 8, Random(4))
        val themen = fragen.map { f ->
            val wort = f.text.de.substringBefore(":")
            EnglischThema.mitWoertern.first { t -> t.woerter.any { it.en == wort } }
        }.toSet()
        assertTrue(themen.size >= 5)
    }

    @Test
    fun uebungName() {
        assertEquals("🇬🇧 Tiere", EnglischThema.TIERE.uebungName)
    }
}
