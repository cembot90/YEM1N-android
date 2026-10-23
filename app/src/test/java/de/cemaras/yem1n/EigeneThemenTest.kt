package de.cemaras.yem1n

import de.cemaras.yem1n.logik.EigenThema
import de.cemaras.yem1n.logik.EigenWort
import de.cemaras.yem1n.logik.EigeneThemen
import de.cemaras.yem1n.logik.Sprachen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EigeneThemenTest {
    private fun w(de: String, en: String = "x", tr: String = "y", it: String = "") = EigenWort("", de, en, tr, it)

    @After
    fun aufraeumen() { EigeneThemen.liste = emptyList() }

    @Test
    fun speichernUndLesen() {
        val t = EigenThema("eigen-a", "Mein Thema", "🐞", 2, listOf(w("Käfer", "beetle", "böcek", "scarabeo")))
        val neu = EigeneThemen.lese(EigeneThemen.text(listOf(t)))
        assertEquals(listOf(t), neu)
        assertTrue(EigeneThemen.lese("kaputt").isEmpty())
        assertTrue(EigeneThemen.lese(null).isEmpty())
    }

    @Test
    fun idsSindEindeutigUndOhneSonderzeichen() {
        val a = EigenThema("eigen-tiere-im-wald", "Tiere im Wald")
        assertEquals("eigen-tiere-im-wald", EigeneThemen.neueId("Tiere im Wald", emptyList()))
        assertEquals("eigen-tiere-im-wald-2", EigeneThemen.neueId("Tiere im Wald", listOf(a)))
        assertEquals("eigen-aepfel-und-oel", EigeneThemen.neueId("Äpfel und Öl!", emptyList()))
        assertEquals("eigen-thema", EigeneThemen.neueId("???", emptyList()))
    }

    @Test
    fun pruefenFindetProbleme() {
        assertTrue(EigeneThemen.pruefe(EigenThema("eigen-a", "", woerter = emptyList())).size >= 2)
        val gut = EigenThema("eigen-a", "Tiere", woerter = listOf(w("Hund"), w("Katze"), w("Maus")))
        assertTrue(EigeneThemen.pruefe(gut).isEmpty())
        val halb = gut.copy(woerter = gut.woerter + EigenWort("", "Pferd", "", "", ""))
        assertTrue(EigeneThemen.pruefe(halb).any { it.contains("Wort 4") })
        val doppelt = gut.copy(woerter = gut.woerter + w("hund"))
        assertTrue(EigeneThemen.pruefe(doppelt).any { it.contains("Doppelte") })
        assertTrue(EigeneThemen.pruefe(gut.copy(titel = "A – B")).any { it.contains("Striche") })
    }

    @Test
    fun bereinigenLaesstLeereZeilenWeg() {
        val t = EigenThema("eigen-a", "  Tiere ", woerter = listOf(w(" Hund "), EigenWort()))
        val b = EigeneThemen.bereinigt(t)
        assertEquals("Tiere", b.titel)
        assertEquals(1, b.woerter.size)
        assertEquals("Hund", b.woerter[0].de)
    }

    @Test
    fun themaErscheintInDenSprachen() {
        EigeneThemen.liste = listOf(EigenThema("eigen-a", "Tiere", "🐞", 2, listOf(w("Hund", "dog", "köpek", "cane"), w("Katze", "cat", "kedi"))))
        assertTrue(Sprachen.themen.any { it.id == "eigen-a" })
        assertEquals(2, Sprachen.woerter("eigen-a", "en").size)
        assertEquals(1, Sprachen.woerter("eigen-a", "it").size)
        assertTrue(Sprachen.lernThema("eigen-a", "en") == null)
    }
}
