package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Bundesland
import de.cemaras.yem1n.logik.Lern
import de.cemaras.yem1n.logik.LehrFach
import de.cemaras.yem1n.logik.Lernwelt
import de.cemaras.yem1n.logik.Schuljahr
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LernweltTest {

    @Before
    fun feststehenderZufall() {
        Lern.zufall = Random(42)
    }

    @Test
    fun jedesThemaLiefertZehnVerschiedeneAufgaben() {
        for (t in Lernwelt.themen) {
            val liste = Lernwelt.aufgaben(t)
            assertEquals("Thema ${t.id}", 10, liste.size)
            assertEquals("Thema ${t.id} doppelt", 10, liste.map { it.schluessel }.toSet().size)
        }
    }

    @Test
    fun themenIdsUndTitelSindEindeutig() {
        assertEquals("ids", Lernwelt.themen.size, Lernwelt.themen.map { it.id }.toSet().size)
        for (land in Bundesland.entries) {
            val schluessel = Lernwelt.themen.map { Triple(it.fach, it.klasse(land), it.titel) }
            assertEquals("Titel je Fach und Klasse in ${land.kurz}", schluessel.size, schluessel.toSet().size)
        }
    }

    @Test
    fun jedeAufgabeHatFrageUndAntwort() {
        repeat(30) {
            for (t in Lernwelt.themen) {
                val a = t.erzeuge()
                assertTrue("${t.id}: Frage leer", a.frage.isNotBlank())
                if (a.art != "mauer") assertTrue("${t.id}: Antwort leer", a.antwort.isNotBlank())
                if (a.art == "wahl") {
                    assertTrue("${t.id}: richtige Antwort fehlt in den Knöpfen", a.antwort in a.optionen)
                    assertTrue("${t.id}: zu wenige Knöpfe", a.optionen.size >= 2)
                    assertEquals("${t.id}: doppelte Knöpfe", a.optionen.size, a.optionen.toSet().size)
                }
                if (a.art == "rest") assertTrue("${t.id}: Rest fehlt", a.antwort2.isNotBlank())
                assertTrue("${t.id}: Gedankenstrich", !(a.frage + a.rechnung + a.erklaerung + a.hinweis).contains('–') &&
                    !(a.frage + a.rechnung + a.erklaerung + a.hinweis).contains('—'))
            }
        }
    }

    /** Rechnet einfache Aufgaben der Form "a op b =" nach und vergleicht mit der Antwort. */
    @Test
    fun einfacheRechnungenStimmen() {
        val muster = Regex("""^(\d+) ([+\-·:]) (\d+) =$""")
        var geprueft = 0
        repeat(80) {
            for (t in Lernwelt.themen) {
                val a = t.erzeuge()
                if (a.art != "zahl") continue
                val m = muster.matchEntire(a.rechnung) ?: continue
                val x = m.groupValues[1].toInt()
                val y = m.groupValues[3].toInt()
                val soll = when (m.groupValues[2]) {
                    "+" -> x + y
                    "-" -> x - y
                    "·" -> x * y
                    else -> { assertEquals("${t.id}: Rest bei geteilt", 0, x % y); x / y }
                }
                assertEquals("${t.id}: ${a.rechnung}", soll.toString(), a.antwort)
                assertTrue("${t.id}: negatives Ergebnis", soll >= 0)
                geprueft++
            }
        }
        assertTrue("zu wenige geprüft: $geprueft", geprueft > 500)
    }

    @Test
    fun restAufgabenStimmen() {
        val muster = Regex("""^(\d+) : (\d+) =$""")
        repeat(200) {
            val a = de.cemaras.yem1n.logik.lehrplan.LehrplanMathe.rest()
            val m = muster.matchEntire(a.rechnung)!!
            val n = m.groupValues[1].toInt()
            val t = m.groupValues[2].toInt()
            assertEquals(n / t, a.antwort.toInt())
            assertEquals(n % t, a.antwort2.toInt())
            assertTrue(n <= 100)
        }
    }

    @Test
    fun arbeitHatEineUebungMitDemThemaTitel() {
        val t = Lernwelt.themen.first { it.id == "m3-rest" }
        val a = Lernwelt.arbeit(t, Bundesland.HESSEN)
        assertEquals("Klasse 3", a.klasse)
        assertEquals("Mathe", a.fach)
        assertEquals(1, a.uebungen.size)
        assertEquals("Teilen mit Rest", a.uebungen[0].titel)
        assertEquals(10, a.uebungen[0].aufgaben.size)
    }

    @Test
    fun nrwVerschiebtEinmaleinsUndGewicht() {
        val einmaleins = Lernwelt.themen.first { it.id == "m2-einmaleins" }
        assertEquals(2, einmaleins.klasse(Bundesland.HESSEN))
        assertEquals(3, einmaleins.klasse(Bundesland.NRW))
        assertTrue(Lernwelt.themenFuer(LehrFach.MATHE, 3, Bundesland.NRW).any { it.id == "m2-einmaleins" })
        assertEquals(1, Schuljahr.abschnitt("m2-einmaleins", Bundesland.NRW))
        assertEquals(3, Schuljahr.abschnitt("m2-einmaleins", Bundesland.BAYERN))
    }

    @Test
    fun klasseAusProfil() {
        assertEquals(3, Lernwelt.klasse("Klasse 3"))
        assertEquals(1, Lernwelt.klasse("Vorschule"))
        assertEquals(null, Lernwelt.klasse("Klasse 7"))
        assertEquals(null, Lernwelt.klasse(""))
        assertEquals(listOf(3, 1, 2, 4), Lernwelt.klassenReihenfolge(3))
        assertEquals(listOf(1, 2, 3, 4), Lernwelt.klassenReihenfolge(null))
    }

    @Test
    fun grosseZahlenMitAbstand() {
        assertEquals("999", Lern.gross(999))
        assertEquals("9999", Lern.gross(9999))
        assertEquals("45 300", Lern.gross(45300))
        assertEquals("123 456", Lern.gross(123456))
    }

    @Test
    fun vergleichUndWahl() {
        val v = Lern.vergleich("5 · 5", 25, "9 · 7", 63)
        assertEquals("<", v.antwort)
        assertEquals("5 · 5  ?  9 · 7", v.rechnung)
        val w = Lern.wahl("Frage", richtig = "a", falsch = listOf("a", "b", "b", "c", "d"))
        assertEquals(3, w.optionen.size)
        assertTrue("a" in w.optionen)
    }

    @Test
    fun gruppiertNachSchuljahr() {
        val liste = Lernwelt.themenFuer(LehrFach.MATHE, 3, Bundesland.HESSEN)
        val g = Schuljahr.gruppiert(liste, Bundesland.HESSEN)
        assertEquals(liste.size, g.sumOf { it.themen.size })
        assertEquals(listOf(1, 2, 3, 4), g.map { it.abschnitt }.take(4))
    }
}

class LernweltWeitereTest {
    @org.junit.Test
    fun sprachkatalogWirdGeladen() {
        val w = de.cemaras.yem1n.logik.lehrplan.LehrplanEnglisch.woerter
        org.junit.Assert.assertTrue(w["farben"].orEmpty().size >= 3)
        org.junit.Assert.assertTrue(w["wochentage"].orEmpty().size >= 7)
    }

    @org.junit.Test
    fun englischMusikKunstHabenThemen() {
        for (f in listOf(de.cemaras.yem1n.logik.LehrFach.ENGLISCH, de.cemaras.yem1n.logik.LehrFach.MUSIK,
            de.cemaras.yem1n.logik.LehrFach.KUNST)) {
            org.junit.Assert.assertTrue("$f", de.cemaras.yem1n.logik.Lernwelt.themen.count { it.fach == f } >= 9)
        }
    }
}
