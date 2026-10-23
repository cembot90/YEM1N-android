package de.cemaras.yem1n

import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.logik.Mauer
import de.cemaras.yem1n.logik.MatheGenerator
import de.cemaras.yem1n.logik.Sonderrunden
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Rechnet "a + b · c" mit Punkt vor Strich, unabhängig vom Generator. */
private fun auswerten(ausdruck: String): Int {
    val teile = ausdruck.trim().split(" ").filter { it.isNotEmpty() }
    // Erst Mal und Geteilt von links nach rechts
    val zahlen = mutableListOf(teile[0].toInt())
    val strich = mutableListOf<String>()
    var i = 1
    while (i < teile.size) {
        val op = teile[i]
        val n = teile[i + 1].toInt()
        when (op) {
            "·" -> zahlen[zahlen.size - 1] = zahlen.last() * n
            ":" -> {
                assertEquals("Geteilt nur ohne Rest: $ausdruck", 0, zahlen.last() % n)
                zahlen[zahlen.size - 1] = zahlen.last() / n
            }
            "+", "-" -> { strich += op; zahlen += n }
            else -> error("Unbekanntes Zeichen $op in $ausdruck")
        }
        i += 2
    }
    var ergebnis = zahlen[0]
    strich.forEachIndexed { k, op -> ergebnis = if (op == "+") ergebnis + zahlen[k + 1] else ergebnis - zahlen[k + 1] }
    return ergebnis
}

private val rechnungMuster = Regex("^[\\d ·:+\\-]+ =$")
private val gleichungMuster = Regex("^([\\d ·:+\\-]+) = (\\d+)$")

/** Prüft eine Aufgabe auf einem zweiten Weg nach. */
private fun pruefeAufgabe(a: Aufgabe) {
    when (a.art) {
        Aufgabe.ART_MAUER -> {
            assertTrue(Mauer.gueltig(a.reihen))
            assertEquals(4, a.reihen[0].size)
            assertTrue(a.reihen[0].all { it in 3..15 })
            assertTrue(a.reihen.last()[0] <= 100)
            return
        }
        Aufgabe.ART_VERGLEICH -> {
            val (l, r) = a.rechnung.split("  ?  ")
            val lw = auswerten(l)
            val rw = auswerten(r)
            val soll = if (lw > rw) ">" else if (lw < rw) "<" else "="
            assertEquals(a.rechnung, soll, a.antwort)
            assertEquals("$lw $soll $rw", a.erklaerung)
            return
        }
        Aufgabe.ART_REST -> {
            val m = Regex("^(\\d+) : (\\d+) =$").matchEntire(a.rechnung)!!
            val dividend = m.groupValues[1].toInt()
            val teiler = m.groupValues[2].toInt()
            val q = a.antwort.toInt()
            val rest = a.antwort2.toInt()
            assertEquals(dividend, q * teiler + rest)
            assertTrue("Rest kleiner als Teiler", rest < teiler)
            assertTrue("Dividend höchstens 100", dividend <= 100)
            return
        }
    }
    val antwort = a.antwort.toInt()
    assertTrue("Antwort 0 bis 999: ${a.rechnung}", antwort in 0..999)
    if (rechnungMuster.matches(a.rechnung)) {
        assertEquals(a.rechnung, antwort, auswerten(a.rechnung.removeSuffix(" =")))
        return
    }
    val gl = gleichungMuster.matchEntire(a.erklaerung)
    if (gl != null) {
        assertEquals(a.erklaerung, antwort, gl.groupValues[2].toInt())
        assertEquals(a.erklaerung, antwort, auswerten(gl.groupValues[1]))
        return
    }
    if (a.frage == "Wie heißt die Zahl?") {
        val z = a.rechnung.split("\n")
        val gerade = z[0].contains("ist gerade")
        val worte = listOf("", "Einer", "Zweier", "Dreier", "Vierer", "Fünfer", "Sechser", "Siebener", "Achter", "Neuner", "Zehner")
        val k = worte.indexOfFirst { it.isNotEmpty() && z[1].contains("eine ${it}zahl") }
        val (start, ende) = Regex("zwischen (\\d+) und (\\d+)").find(z[2])!!.destructured
        val treffer = ((start.toInt() + 1) until ende.toInt()).filter { it % k == 0 && (it % 2 == 0) == gerade }
        assertEquals(a.rechnung, listOf(antwort), treffer)
        return
    }
    error("Aufgabe nicht prüfbar: ${a.rechnung} / ${a.erklaerung}")
}

class GeneratorTest {
    @Test fun alleVorlagenLiefernRichtigeAufgaben() {
        for (seed in 0 until 300) {
            val gen = MatheGenerator(Random(seed))
            for (v in MatheGenerator.vorlagen) {
                repeat(5) {
                    val a = v.mach(gen)
                    try {
                        pruefeAufgabe(a)
                    } catch (e: Throwable) {
                        throw AssertionError("Vorlage ${v.id}, Seed $seed: ${e.message}", e)
                    }
                }
            }
        }
    }

    @Test fun dreizehnVorlagenWieInDerTabelle() {
        val v = MatheGenerator.vorlagen
        assertEquals(13, v.size)
        assertEquals(listOf(10, 10, 10, 10, 12, 10, 10, 10, 10, 4, 8, 8, 12), v.map { it.anzahl })
    }

    @Test fun fuelleLiefertEinmaligeAufgabenUndMerktSich() {
        val gen = MatheGenerator(Random(1))
        val sperre = mutableSetOf<String>()
        val v = MatheGenerator.vorlagen.first { it.id == "sach" }
        val a = gen.fuelle(v, 8, sperre)
        assertEquals(8, a.size)
        assertEquals(8, a.map { it.rechnung }.toSet().size)
        assertEquals(8, sperre.size)
    }

    @Test fun teilenMitRestHatDieHaelfteMitZweistelligemErgebnis() {
        val gen = MatheGenerator(Random(2))
        val v = MatheGenerator.vorlagen.first { it.id == "rest" }
        val a = gen.fuelle(v, 10, mutableSetOf())
        assertEquals(10, a.size)
        assertEquals(5, a.count { it.antwort.toInt() >= 10 })
    }
}

class SonderrundenTest {
    @Test fun probePlan() {
        repeat(100) { seed ->
            val plan = Sonderrunden.probePlan(Random(seed))
            assertEquals(20, plan.sumOf { it.second })
            assertTrue(plan.all { it.second >= 1 })
            assertTrue(plan.first { it.first == "mauer" }.second <= 2)
            assertEquals(Sonderrunden.probeBereiche, plan.map { it.first })
        }
    }

    @Test fun probearbeitHatZwanzigNeueRichtigeAufgaben() {
        val bekannt = listOf(de.cemaras.yem1n.daten.PaketLeser.lese(
            java.io.File("src/main/assets/pakete/mathe-klassenarbeit-nr1.json").readText(),
        ).arbeit!!)
        repeat(20) { seed ->
            val probe = Sonderrunden.probe(bekannt, Random(seed))
            val aufgaben = probe.uebungen.single().aufgaben
            assertEquals(20, aufgaben.size)
            aufgaben.forEach { pruefeAufgabe(it) }
            val schluessel = aufgaben.map { MatheGenerator.schluessel(it) }
            assertEquals("keine Doppelten", schluessel.size, schluessel.toSet().size)
        }
    }

    @Test fun noten() {
        assertEquals(1, Sonderrunden.note(20, 20))
        assertEquals(2, Sonderrunden.note(17, 20))
        assertEquals(3, Sonderrunden.note(14, 20))
        assertEquals(4, Sonderrunden.note(10, 20))
        assertEquals(5, Sonderrunden.note(5, 20))
        assertEquals(6, Sonderrunden.note(4, 20))
        assertEquals(6, Sonderrunden.note(0, 0))
    }

    @Test fun trainingNimmtDieZweiSchwaechstenBereiche() {
        val anteile = mapOf("Einmaleins" to 0.5, "Geteilt" to 0.7, "Kernaufgaben" to 1.0, "Sachaufgaben" to 0.9)
        val t = Sonderrunden.training(emptyList(), anteile, Random(3))
        assertEquals(listOf("Einmaleins", "Geteilt"), t.uebungen.map { it.titel })
        assertTrue(t.uebungen.all { it.aufgaben.size == 8 })
        t.uebungen.flatMap { it.aufgaben }.forEach { pruefeAufgabe(it) }
    }

    @Test fun trainingFuelltOhneDatenZufaelligAuf() {
        val t = Sonderrunden.training(emptyList(), emptyMap(), Random(4))
        assertEquals(2, t.uebungen.size)
        assertEquals(2, t.uebungen.map { it.titel }.toSet().size)
    }

    @Test fun sachaufgabenHabenStimmendeLoesungen() {
        val texte = HashSet<String>()
        for (seed in 1..400) {
            val a = MatheGenerator(Random(seed)).sach()
            val antwort = a.antwort.toInt()
            assertTrue("Antwort im Zahlenraum: ${a.rechnung}", antwort in 0..999)
            assertTrue("Keine Gedankenstriche: ${a.rechnung}", !a.rechnung.contains('–') && !a.rechnung.contains('—'))
            // Die Erklärung endet mit dem Ergebnis, das auch die Antwort ist.
            assertTrue("Erklärung passt zur Antwort: ${a.erklaerung}", a.erklaerung.endsWith("= ${a.antwort}"))
            texte += a.rechnung.lines().last()
        }
        assertTrue("Mindestens zehn verschiedene Fragen", texte.size >= 10)
    }
}
