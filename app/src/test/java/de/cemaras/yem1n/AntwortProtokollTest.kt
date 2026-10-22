package de.cemaras.yem1n

import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.logik.AntwortProtokoll
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import de.cemaras.yem1n.logik.Runde
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AntwortProtokollTest {
    private val tag = 86_400_000L

    private fun runde(uebung: String, zeit: Long, richtig: Int, gesamt: Int, details: String = "", angesehen: Int = 0) =
        RundenErgebnisEintrag("e$zeit", "Klasse 3", "Mathe", "Nr. 1", uebung, richtig, gesamt, angesehen, 2, "Yemin", zeit, details)

    private fun detail(n: Int, rechnung: String, e: String, l: String, s: String) =
        """{"n":$n,"f":"Rechne.","r":"$rechnung","e":"$e","l":"$l","s":"$s"}"""

    @Test fun formatIstDasVonIos() {
        val text = """[{"n":2,"f":"Rechne.","r":"7 · 8 =","e":"54","l":"56","s":"falsch"},{"n":1,"f":"x","r":"1+1","e":"2","l":"2","s":"richtig"}]"""
        val l = AntwortProtokoll.liste(text)
        assertEquals("sortiert nach Nummer", listOf(1, 2), l.map { it.nr })
        assertTrue(l[1].istProblem)
        assertEquals("kaputtes JSON gibt eine leere Liste", 0, AntwortProtokoll.liste("kein json").size)
    }

    @Test fun rundeWirdZumProtokoll() {
        fun a(r: String, l: String) = Aufgabe("zahl", "Rechne.", r, "", "", l, "", emptyList(), "", "", "", emptyList(), emptyList())
        var s = Runde.start(listOf(a("2 · 3 =", "6"), a("7 · 8 =", "56"), a("5 · 5 =", "25")))
        s = Runde.antworte(s, "6")
        s = Runde.weiter(s)
        s = Runde.antworte(s, "54")
        s = Runde.weiter(s)
        s = Runde.zeigeLoesung(s)
        val e = AntwortProtokoll.eintraege(s)
        assertEquals(listOf("richtig", "falsch", "angeschaut"), e.map { it.stand })
        assertEquals("54", e[1].eingabe)
        assertEquals("56", e[1].loesung)
        assertEquals("Lösung gezeigt: keine Eingabe", "", e[2].eingabe)
        val json = AntwortProtokoll.json(e)
        assertEquals(e, AntwortProtokoll.liste(json))
        assertTrue("Kurzschlüssel", json.contains("\"n\":1") && json.contains("\"s\":\"falsch\""))
    }

    @Test fun problemAufgabenFassenRundenZusammen() {
        val r1 = runde("Einmaleins", 1 * tag, 0, 2, "[" + detail(1, "7 · 8 =", "54", "56", "falsch") + "," + detail(2, "2 · 2 =", "4", "4", "richtig") + "]")
        val r2 = runde("Einmaleins", 2 * tag, 0, 2, "[" + detail(1, "7 · 8 =", "48", "56", "falsch") + "," + detail(2, "2 · 2 =", "4", "4", "richtig") + "]")
        val r3 = runde("Geteilt", 3 * tag, 0, 1, "[" + detail(1, "32 : 8 =", "", "4", "angeschaut") + "]")
        val p = AntwortProtokoll.problemAufgaben(listOf(r1, r2, r3))
        assertEquals(2, p.size)
        assertEquals("7 · 8 =", p[0].rechnung)
        assertEquals(2, p[0].falsch)
        assertEquals("neueste falsche Eingabe zuerst", listOf("48", "54"), p[0].falscheEingaben)
        assertEquals(1, p[1].angeschaut)
    }

    @Test fun zuletztRichtigRutschtNachHinten() {
        val r1 = runde("A", 1 * tag, 0, 1, "[" + detail(1, "1 · 1 =", "2", "1", "falsch") + "]")
        val r2 = runde("A", 2 * tag, 1, 1, "[" + detail(1, "1 · 1 =", "1", "1", "richtig") + "]")
        val r3 = runde("B", 3 * tag, 0, 1, "[" + detail(1, "2 · 2 =", "5", "4", "falsch") + "]")
        val p = AntwortProtokoll.problemAufgaben(listOf(r1, r2, r3))
        assertEquals("2 · 2 =", p[0].rechnung)
        assertTrue(p[1].zuletztRichtig)
    }

    @Test fun schwaechsteUndZeitraum() {
        val heute = 100L
        val alt = runde("Geteilt", (heute - 40) * tag + 1000, 1, 10)
        val a = runde("Geteilt", (heute - 1) * tag + 1000, 5, 10)
        val b = runde("Einmaleins", heute * tag + 1000, 10, 10)
        val c = runde("Kernaufgaben", heute * tag + 2000, 8, 10)
        assertEquals(3, AntwortProtokoll.imZeitraum(listOf(alt, a, b, c), 7, heute).size)
        assertEquals(4, AntwortProtokoll.imZeitraum(listOf(alt, a, b, c), 0, heute).size)
        val s = AntwortProtokoll.schwaechste(listOf(a, b, c))
        assertEquals("Einmaleins ist sicher und fehlt", listOf("Geteilt", "Kernaufgaben"), s.map { it.titel })
        assertEquals(0.5, s[0].anteil, 0.001)
        val gut = AntwortProtokoll.schwaechste(listOf(runde("X", tag, 10, 10, "", angesehen = 1)))
        assertEquals("angeschaute Lösung zählt als Schwäche", 1, gut.size)
    }

    @Test fun zuletztGeuebt() {
        val l = AntwortProtokoll.zuletztGeuebt(listOf(runde("A", 5, 1, 1), runde("A", 9, 1, 1)))
        assertEquals(listOf("Yemin" to 9L), l)
    }
}
