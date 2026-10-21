package de.cemaras.yem1n

import de.cemaras.yem1n.daten.PaketLeser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PaketTest {
    private fun asset(name: String) = File("src/main/assets/pakete/$name").readText()

    @Test fun codeZaeuneUndTextDrumherumWerdenEntfernt() {
        val roh = "Hier ist die Arbeit:\n```json\n{\"klasse\":\"Klasse 3\"}\n```\nViel Spaß!"
        assertEquals("{\"klasse\":\"Klasse 3\"}", PaketLeser.bereinigt(roh))
    }

    @Test fun antwortenDuerfenZahlenSein() {
        val r = PaketLeser.lese(
            """{"klasse":"Klasse 3","fach":"Mathe","arbeit":"A","uebungen":[
              {"titel":"T","aufgaben":[{"art":"zahl","frage":"Rechne.","rechnung":"7 · 8 =","antwort":56}]}]}""",
        )
        assertNull(r.fehler)
        val a = r.arbeit!!.uebungen[0].aufgaben[0]
        assertEquals("56", a.antwort)
        assertEquals("zahl", a.art)
    }

    @Test fun fehlendeArtIstZahl() {
        val r = PaketLeser.lese(
            """{"klasse":"K","fach":"F","arbeit":"A","uebungen":[{"titel":"T","aufgaben":[{"antwort":"1"}]}]}""",
        )
        assertEquals("zahl", r.arbeit!!.uebungen[0].aufgaben[0].art)
    }

    @Test fun keinJsonGibtEinenFehler() {
        assertNotNull(PaketLeser.lese("Hallo").fehler)
        assertNotNull(PaketLeser.lese("{\"klasse\":\"K\"}").fehler)
    }

    @Test fun leereUebungWirdAbgelehnt() {
        val r = PaketLeser.lese("""{"klasse":"K","fach":"F","arbeit":"A","uebungen":[{"titel":"T","aufgaben":[]}]}""")
        assertNotNull(r.fehler)
    }

    @Test fun auswahlAufgabeBrauchtPassendeKnoepfe() {
        val gut = """{"klasse":"V","fach":"V","arbeit":"A","uebungen":[{"titel":"T","aufgaben":[
            {"art":"wahl","frage":"Wie viele?","optionen":["2","3","5"],"antwort":"3"}]}]}"""
        assertNull(PaketLeser.lese(gut).fehler)
        val schlecht = gut.replace("\"antwort\":\"3\"", "\"antwort\":\"4\"")
        assertNotNull(PaketLeser.lese(schlecht).fehler)
        val zuWenig = gut.replace("[\"2\",\"3\",\"5\"]", "[\"3\"]")
        assertNotNull(PaketLeser.lese(zuWenig).fehler)
    }

    @Test fun kaputteZahlenmauerWirdAbgelehnt() {
        val r = PaketLeser.lese(
            """{"klasse":"K","fach":"F","arbeit":"A","uebungen":[{"titel":"T","aufgaben":[
            {"art":"mauer","frage":"Fülle.","reihen":[[1,2,3],[3,5,8]]}]}]}""",
        )
        assertNotNull(r.fehler)
    }

    @Test fun alleBeispielPaketeLassenSichLesen() {
        val ordner = File("src/main/assets/pakete")
        val dateien = ordner.listFiles { f -> f.extension == "json" }!!
        assertTrue(dateien.size >= 3)
        for (f in dateien) {
            val r = PaketLeser.lese(f.readText())
            assertNull("${f.name}: ${r.fehler}", r.fehler)
            assertTrue(r.arbeit!!.uebungen.isNotEmpty())
        }
    }

    @Test fun matheArbeitHatDreizehnUebungenMitDenRichtigenAnzahlen() {
        val a = PaketLeser.lese(asset("mathe-klassenarbeit-nr1.json")).arbeit!!
        assertEquals("Klasse 3", a.klasse)
        assertEquals("Mathe", a.fach)
        assertEquals(
            listOf(10, 10, 10, 10, 12, 10, 10, 10, 10, 4, 8, 8, 12),
            a.uebungen.map { it.aufgaben.size },
        )
        assertEquals(39, a.maxSterne)
    }

    @Test fun matheAntwortenStimmen() {
        val a = PaketLeser.lese(asset("mathe-klassenarbeit-nr1.json")).arbeit!!
        val mal = Regex("""^(\d+) · (\d+) =$""")
        val geteilt = Regex("""^(\d+) : (\d+) =$""")
        var gepruefte = 0
        for (u in a.uebungen) for (x in u.aufgaben) {
            when (x.art) {
                "rest" -> {
                    val m = geteilt.matchEntire(x.rechnung)!!.groupValues
                    val (d, t) = m[1].toInt() to m[2].toInt()
                    assertEquals(x.rechnung, (d / t).toString(), x.antwort)
                    assertEquals(x.rechnung, (d % t).toString(), x.antwort2)
                    assertTrue(x.antwort2.toInt() < t)
                    gepruefte++
                }
                "mauer" -> {
                    for (r in 1 until x.reihen.size) for (c in x.reihen[r].indices) {
                        assertEquals(x.reihen[r - 1][c] + x.reihen[r - 1][c + 1], x.reihen[r][c])
                    }
                    assertTrue(x.reihen.last()[0] <= 100)
                    gepruefte++
                }
                "vergleich" -> {
                    val teile = x.rechnung.split("  ?  ")
                    fun wert(t: String): Int = t.split(" · ").map { it.toInt() }.reduce { p, q -> p * q }
                    val (l, r) = wert(teile[0]) to wert(teile[1])
                    val sym = if (l < r) "<" else if (l > r) ">" else "="
                    assertEquals(x.rechnung, sym, x.antwort)
                    gepruefte++
                }
                else -> {
                    mal.matchEntire(x.rechnung)?.let {
                        assertEquals(x.rechnung, (it.groupValues[1].toInt() * it.groupValues[2].toInt()).toString(), x.antwort)
                        gepruefte++
                    }
                    geteilt.matchEntire(x.rechnung)?.let {
                        assertEquals(x.rechnung, (it.groupValues[1].toInt() / it.groupValues[2].toInt()).toString(), x.antwort)
                        assertEquals(0, it.groupValues[1].toInt() % it.groupValues[2].toInt())
                        gepruefte++
                    }
                }
            }
        }
        assertTrue("zu wenige geprüft: $gepruefte", gepruefte > 60)
    }
}
