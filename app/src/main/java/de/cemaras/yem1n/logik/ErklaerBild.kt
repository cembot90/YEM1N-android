package de.cemaras.yem1n.logik

import kotlin.math.abs

/**
 * Erklärbilder von Olli. Port von ErklaerBild.swift (ohne Oberfläche).
 * Das Bild entsteht nur aus der Rechnung selbst und wird gegen die Lösung geprüft.
 * Passt etwas nicht zusammen, gibt es kein Bild. Ein falsches Bild wäre schlimmer als keins.
 */
sealed class BildArt {
    data class Punkte(val reihen: Int, val spalten: Int) : BildArt()

    /** [kern] Reihen sind bekannt, [reihen] ist die gesuchte Aufgabe (eine mehr oder weniger) */
    data class Nachbar(val kern: Int, val reihen: Int, val spalten: Int) : BildArt()
    data class Teller(val gesamt: Int, val teller: Int, val jeder: Int, val rest: Int) : BildArt()

    /** Schritte mit Vorzeichen: +3 springt nach rechts, -5 nach links */
    data class Spruenge(val start: Int, val schritte: List<Int>) : BildArt()
    data class Rechenweg(val zeilen: List<String>) : BildArt()
}

data class ErklaerBild(val art: BildArt, val saetze: List<String>)

object ErklaerBildPlan {

    fun bild(art: String, rechnung: String, erklaerung: String, antwort: String, antwort2: String): ErklaerBild? {
        return when (art) {
            "vergleich" -> vergleich(rechnung, antwort)
            "rest" -> {
                val q = antwort.trim().toIntOrNull() ?: return null
                val r = antwort2.trim().toIntOrNull() ?: return null
                val t = ausdruck(listOf(rechnung, erklaerung)) ?: return null
                if (t.zahlen.size != 2 || t.zeichen != listOf(":")) return null
                val a = t.zahlen[0]; val b = t.zahlen[1]
                if (b <= 0 || a / b != q || a % b != r) return null
                teller(a, b)
            }
            "zahl" -> {
                val soll = antwort.trim().toIntOrNull() ?: return null
                val t = ausdruck(listOf(rechnung, erklaerung)) ?: return null
                val ist = wert(t) ?: return null
                if (ist != soll) null else passendesBild(t)
            }
            else -> null
        }
    }

    data class Term(val zahlen: List<Int>, val zeichen: List<String>) {
        val text: String
            get() {
                val sb = StringBuilder((zahlen.firstOrNull() ?: 0).toString())
                for ((i, z) in zeichen.withIndex()) if (i + 1 < zahlen.size) sb.append(" $z ${zahlen[i + 1]}")
                return sb.toString()
            }
    }

    /** Liest "75 - 8 =" oder "75 - 8 = 67" als Rechnung. Text mit Wörtern ist keine Rechnung. */
    fun term(roh: String): Term? {
        val r = Rechenausdruck.lies(roh) ?: return null
        return Term(r.zahlen, r.zeichen.map { when (it) { '*' -> "·"; '/' -> ":"; else -> it.toString() } })
    }

    /** Die erste der Texte, die eine Rechnung ist. Bei Wortaufgaben steht sie in der Erklärung. */
    fun ausdruck(texte: List<String>): Term? = texte.firstNotNullOfOrNull { term(it) }

    class Schritt(val neu: Term, val satz: String, val punkt: Boolean)

    /** Erst Mal und Geteilt von links, dann Plus und Minus. null, wenn etwas nicht aufgeht oder negativ wird. */
    fun schritt(t: Term): Schritt? {
        if (t.zeichen.isEmpty()) return null
        val punktStelle = t.zeichen.indexOfFirst { it == "·" || it == ":" }
        val i = if (punktStelle >= 0) punktStelle else 0
        val a = t.zahlen[i]; val b = t.zahlen[i + 1]; val z = t.zeichen[i]
        val e: Int = when (z) {
            "+" -> a + b
            "-" -> if (a >= b) a - b else return null
            "·" -> {
                val p = a.toLong() * b
                if (p > 1_000_000) return null
                p.toInt()
            }
            else -> if (b != 0 && a % b == 0) a / b else return null
        }
        val zahlen = t.zahlen.toMutableList().also { it.subList(i, i + 2).clear(); it.add(i, e) }
        val zeichen = t.zeichen.toMutableList().also { it.removeAt(i) }
        return Schritt(Term(zahlen, zeichen), "$a $z $b = $e", punktStelle >= 0)
    }

    fun wert(t: Term): Int? {
        var x = t
        while (x.zeichen.isNotEmpty()) x = schritt(x)?.neu ?: return null
        return x.zahlen.firstOrNull()
    }

    private fun passendesBild(t: Term): ErklaerBild? {
        if (t.zeichen.size == 1) {
            val a = t.zahlen[0]; val b = t.zahlen[1]
            return when (t.zeichen[0]) {
                "·" -> malBild(a, b)
                ":" -> if (b > 0) teller(a, b) else null
                "+" -> spruengeBild(a, true, b)
                else -> spruengeBild(a, false, b)
            }
        }
        return rechenweg(t)
    }

    val reihenWort = mapOf(1 to "Einer", 2 to "Zweier", 3 to "Dreier", 4 to "Vierer", 5 to "Fünfer",
        6 to "Sechser", 7 to "Siebener", 8 to "Achter", 9 to "Neuner", 10 to "Zehner")

    /** Nachbaraufgaben: 3 neben 2, 4 und 6 neben 5, 9 neben 10 */
    fun kern(reihen: Int): Int? = when (reihen) { 3 -> 2; 4, 6 -> 5; 9 -> 10; else -> null }

    fun malBild(a: Int, b: Int): ErklaerBild? {
        if (a !in 1..10 || b !in 1..10) return null
        kern(a)?.let { k ->
            val mehr = a > k
            return ErklaerBild(BildArt.Nachbar(k, a, b), listOf(
                "Die Kernaufgabe kennst du: $k · $b = ${k * b}.",
                if (mehr) "Eine Reihe mehr: ${k * b} + $b = ${a * b}." else "Eine Reihe weniger: ${k * b} - $b = ${a * b}.",
                "Also ist $a · $b = ${a * b}.",
            ))
        }
        val liste = (1..a).joinToString(", ") { (it * b).toString() }
        val saetze = mutableListOf(
            "$a · $b heißt: $a ${if (a == 1) "Reihe" else "Reihen"} mit je $b ${if (b == 1) "Punkt" else "Punkten"}.",
        )
        val wort = reihenWort[b]
        if (a > 1 && wort != null) saetze += "Zähle in ${wort}schritten: $liste."
        saetze += "Zusammen sind es ${a * b}."
        return ErklaerBild(BildArt.Punkte(a, b), saetze)
    }

    fun teller(a: Int, b: Int): ErklaerBild? {
        if (b !in 2..10 || a < 0) return null
        val q = a / b; val r = a % b
        if (q > 25) return null
        val saetze = mutableListOf(
            "Verteile $a Kekse gerecht auf $b Teller.",
            "Jeder Teller bekommt $q ${if (q == 1) "Keks" else "Kekse"}.",
        )
        if (r > 0) {
            saetze += if (r == 1) "1 Keks bleibt übrig. Das ist der Rest." else "$r Kekse bleiben übrig. Das ist der Rest."
            saetze += "$a : $b = $q Rest $r"
        } else {
            saetze += "Es bleibt nichts übrig. $a : $b = $q"
        }
        return ErklaerBild(BildArt.Teller(a, b, q, r), saetze)
    }

    /** Erst die Hunderter, dann die Zehner, dann die Einer. Die Einer gehen zuerst bis zum nächsten Zehner. */
    fun spruenge(a: Int, plus: Boolean, b: Int): List<Int> {
        val teile = mutableListOf<Int>()
        var x = a
        var rest = b
        for (stelle in listOf(100, 10)) {
            val teil = (rest / stelle) * stelle
            if (teil > 0) {
                teile += teil
                x += if (plus) teil else -teil
                rest -= teil
            }
        }
        if (rest > 0) {
            val bisZehner = if (plus) (10 - x % 10) % 10 else x % 10
            if (bisZehner > 0 && rest > bisZehner) { teile += bisZehner; teile += rest - bisZehner } else teile += rest
        }
        return teile.map { if (plus) it else -it }
    }

    fun spruengeBild(a: Int, plus: Boolean, b: Int): ErklaerBild? {
        if (b <= 0 || a < 0) return null
        val ziel = if (plus) a + b else a - b
        if (ziel !in 0..1000 || a > 1000) return null
        val schritte = spruenge(a, plus, b)
        if (schritte.size > 5) return null
        val saetze = mutableListOf("Rechne in Sprüngen auf dem Zahlenstrahl.")
        var x = a
        var zumZehner = false
        for ((i, s) in schritte.withIndex()) {
            val neu = x + s
            saetze += "$x ${if (s > 0) "+" else "-"} ${abs(s)} = $neu"
            if (neu % 10 == 0 && abs(s) < 10 && i < schritte.size - 1) zumZehner = true
            x = neu
        }
        if (zumZehner) saetze.add(1, "Zuerst bis zum Zehner, dann weiter.")
        return ErklaerBild(BildArt.Spruenge(a, schritte), saetze)
    }

    fun rechenweg(t: Term): ErklaerBild? {
        var x = t
        val zeilen = mutableListOf(t.text)
        val saetze = mutableListOf<String>()
        val gemischt = t.zeichen.any { it == "·" || it == ":" } && t.zeichen.any { it == "+" || it == "-" }
        if (gemischt) saetze += "Punkt vor Strich: Mal und Geteilt kommen zuerst."
        while (x.zeichen.isNotEmpty()) {
            val s = schritt(x) ?: return null
            saetze += (if (saetze.size <= (if (gemischt) 1 else 0)) "Zuerst: " else "Dann: ") + s.satz
            x = s.neu
            zeilen += x.text
        }
        return ErklaerBild(BildArt.Rechenweg(zeilen), saetze)
    }

    fun vergleich(rechnung: String, antwort: String): ErklaerBild? {
        val seiten = rechnung.split("?")
        if (seiten.size != 2) return null
        class Seite(val text: String, val wert: Int, val rechnen: Boolean)
        fun seite(s: String): Seite? {
            val roh = s.trim()
            roh.toIntOrNull()?.let { return Seite(roh, it, false) }
            val t = term(roh) ?: return null
            val w = wert(t) ?: return null
            return Seite(t.text, w, true)
        }
        val l = seite(seiten[0]) ?: return null
        val r = seite(seiten[1]) ?: return null
        if (!l.rechnen && !r.rechnen) return null
        val zeichen = if (l.wert < r.wert) "<" else if (l.wert > r.wert) ">" else "="
        if (zeichen != antwort.trim()) return null
        val wort = when (zeichen) { "<" -> "kleiner als"; ">" -> "größer als"; else -> "genauso groß wie" }
        return ErklaerBild(
            BildArt.Rechenweg(listOf("${l.text}  ?  ${r.text}", "${l.wert}  ?  ${r.wert}", "${l.wert}  $zeichen  ${r.wert}")),
            listOf("Rechne zuerst beide Seiten aus.", "${l.wert} ist $wort ${r.wert}.", "Darum gehört $zeichen in die Mitte."),
        )
    }

    /** Wie viele Schritte das Bild hat. Danach steht es fertig da. */
    fun schrittZahl(art: BildArt): Int = when (art) {
        is BildArt.Punkte -> art.reihen
        is BildArt.Nachbar -> 2
        is BildArt.Teller -> art.jeder + if (art.rest > 0) 1 else 0
        is BildArt.Spruenge -> art.schritte.size
        is BildArt.Rechenweg -> maxOf(art.zeilen.size - 1, 1)
    }
}
