package de.cemaras.yem1n

import de.cemaras.yem1n.logik.DuellAntwort
import de.cemaras.yem1n.logik.DuellAufgabe
import de.cemaras.yem1n.logik.DuellHerausforderung
import de.cemaras.yem1n.logik.DuellLauf
import de.cemaras.yem1n.logik.DuellPost
import de.cemaras.yem1n.logik.DuellRegeln
import de.cemaras.yem1n.logik.DuellStand
import de.cemaras.yem1n.logik.DuellStufe
import de.cemaras.yem1n.logik.ServerAnfrage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DuellTest {
    private val stand = DuellStand(8, 2, 40.0)

    private fun duell(id: String, geraet: String, zeitMs: Long) = DuellHerausforderung(
        id, "Yemin", geraet, DuellStufe.KLASSE3, stand, DuellStufe.PROFI, listOf(DuellAufgabe("2 · 3", 6)), DuellRegeln.zeitpunktJetzt(zeitMs),
    )

    @Test fun aufgabenSindRichtigUndVerschieden() {
        for (stufe in DuellStufe.entries) {
            repeat(50) { n ->
                val liste = DuellRegeln.aufgaben(stufe, zufall = Random(n))
                assertEquals(stufe.name, 10, liste.size)
                assertEquals(10, liste.map { it.rechnung }.toSet().size)
                liste.forEach { a ->
                    assertTrue("${stufe.name} ${a.rechnung} = ${a.antwort}", a.antwort in 1..999)
                    val t = a.rechnung.split(" ")
                    val x = t[0].toInt()
                    val y = t[2].toInt()
                    val soll = when (t[1]) {
                        "+" -> x + y
                        "-" -> x - y
                        "·" -> x * y
                        ":" -> x / y
                        else -> -1
                    }
                    assertEquals(a.rechnung, soll, a.antwort)
                    if (t[1] == ":") assertEquals("ohne Rest: ${a.rechnung}", 0, x % y)
                }
            }
        }
    }

    @Test fun laufZaehltFehlerMitStrafe() {
        var l = DuellLauf(listOf(DuellAufgabe("2 · 3", 6), DuellAufgabe("4 + 4", 8)))
        assertNull("keine Zahl", l.antworte("abc"))
        l = l.antworte("6")!!.first
        val (neu, ok) = l.antworte("9")!!
        assertEquals(false, ok)
        assertTrue(neu.fertig)
        val s = neu.stand(20.0)
        assertEquals(1, s.richtig)
        assertEquals(1, s.falsch)
        assertEquals(23.0, s.sekunden, 0.001)
    }

    @Test fun siegerNachRichtigenDannZeit() {
        assertEquals(0, DuellRegeln.sieger(DuellStand(9, 1, 50.0), DuellStand(8, 2, 20.0)))
        assertEquals(1, DuellRegeln.sieger(DuellStand(8, 2, 50.0), DuellStand(8, 2, 40.0)))
        assertNull(DuellRegeln.sieger(DuellStand(8, 2, 40.0), DuellStand(8, 2, 40.05)))
        assertEquals("48,3 s", DuellRegeln.zeitText(48.3))
    }

    @Test fun jsonIstDasVonIos() {
        // So schreibt die iOS-App die Herausforderung: Stufe als Text, Datum in Sekunden seit 2001
        val ios = """{"id":"A1","von":"Papa","vonGeraet":"G1","vonStufe":"profi","vonStand":{"richtig":9,"falsch":1,"sekunden":41.5},""" +
            """"anStufe":"klasse3","aufgaben":[{"rechnung":"7 · 8","antwort":56}],"zeitpunkt":782000000.5}"""
        val d = DuellPost.herausforderung(ios)!!
        assertEquals(DuellStufe.PROFI, d.vonStufe)
        assertEquals(56, d.aufgaben[0].antwort)
        val zurueck = DuellPost.text(d)
        assertTrue(zurueck, zurueck.contains("\"vonStufe\":\"profi\""))
        assertEquals(d, DuellPost.herausforderung(zurueck))
        val a = DuellAntwort("A1", "Yemin", "G2", stand, 782000100.0)
        assertEquals(a, DuellPost.antwort(DuellPost.text(a)))
        assertNull(DuellPost.herausforderung("kaputt"))
    }

    @Test fun datumUmrechnung() {
        val ms = 1_792_000_000_000L
        assertEquals(ms, DuellRegeln.zeitpunktMs(DuellRegeln.zeitpunktJetzt(ms)))
    }

    @Test fun offenWartendEntschieden() {
        val jetzt = 1_792_000_000_000L
        val tag = 86_400_000L
        val meins = duell("M", "ICH", jetzt - tag)
        val fremd = duell("F", "ANDERE", jetzt - tag)
        val alt = duell("A", "ANDERE", jetzt - 20 * tag)
        val duelle = listOf(meins, fremd, alt)
        assertEquals(listOf("F"), DuellPost.offen(duelle, emptyList(), "ICH", jetzt).map { it.id })
        assertEquals(listOf("M"), DuellPost.wartend(duelle, emptyList(), "ICH", jetzt).map { it.id })
        val antwort = DuellAntwort("F", "Evîn", "ICH", DuellStand(10, 0, 30.0), DuellRegeln.zeitpunktJetzt(jetzt))
        assertEquals("angenommen verschwindet aus offen", emptyList<String>(), DuellPost.offen(duelle, listOf(antwort), "ICH", jetzt).map { it.id })
        val p = DuellPost.entschieden(duelle, listOf(antwort)).single()
        assertEquals("Evîn", p.siegerName)
    }
}
