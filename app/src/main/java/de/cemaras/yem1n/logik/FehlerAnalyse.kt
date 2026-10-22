package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Aufgabe
import kotlin.math.abs

/** Typische Denkfehler. Port von FehlerAnalyse.swift. */
enum class FehlerMuster {
    REST_ZU_GROSS, REST_FALSCH, GETEILT_ZU_VIEL,
    PLUS_STATT_MAL, MAL_STATT_PLUS, PLUS_STATT_MINUS, MINUS_STATT_PLUS, MAL_STATT_GETEILT,
    MAL_REIHE_ZU_VIEL, MAL_REIHE_ZU_WENIG, GETEILT_DANEBEN,
    PUNKT_VOR_STRICH, ZIFFER_DREHER, ZEHNER_DANEBEN, KNAPP_DANEBEN,
    VERGLEICH_UMGEDREHT, GLEICH_UEBERSEHEN, NICHT_GLEICH,
    GROSS_KLEIN, HART_WEICH, AE_STATT_E, DOPPELTER_MITLAUT, IE_STATT_I, EIN_BUCHSTABE,
}

/** Kindgerechte Erklärung. Sie ist aus den Zahlen der Aufgabe gebaut und stimmt deshalb immer. */
data class FehlerBefund(val muster: FehlerMuster, val satz: String)

/** Eine einfache Rechnung ohne Klammern, zum Beispiel 9 · 9 + 8. */
class Rechenausdruck(val zahlen: List<Int>, val zeichen: List<Char>) {
    /** Mit Punkt vor Strich. null bei Teilen durch 0. */
    val wert: Int? get() = rechne(punktZuerst = true)

    /** Stur von links nach rechts, der typische Fehler. */
    val linksNachRechts: Int? get() = rechne(punktZuerst = false)

    private fun an(a: Int, o: Char, b: Int): Int? = when (o) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> if (b == 0) null else a / b
        else -> null
    }

    private fun rechne(punktZuerst: Boolean): Int? {
        if (zahlen.size != zeichen.size + 1 || zahlen.isEmpty()) return null
        if (!punktZuerst) {
            var w = zahlen[0]
            for ((i, o) in zeichen.withIndex()) w = an(w, o, zahlen[i + 1]) ?: return null
            return w
        }
        val werte = mutableListOf(zahlen[0])
        val striche = mutableListOf<Char>()
        for ((i, o) in zeichen.withIndex()) {
            if (o == '*' || o == '/') {
                val letzter = werte.removeAt(werte.size - 1)
                werte.add(an(letzter, o, zahlen[i + 1]) ?: return null)
            } else {
                striche.add(o)
                werte.add(zahlen[i + 1])
            }
        }
        var w = werte[0]
        for ((i, o) in striche.withIndex()) w = an(w, o, werte[i + 1]) ?: return null
        return w
    }

    companion object {
        /** Liest "7 · 8 =" oder "58 : 4 = 14 R 2" bis zum Gleichzeichen. Wörter ergeben null. */
        fun lies(text: String): Rechenausdruck? {
            val erste = text.lines().firstOrNull().orEmpty()
            val links = erste.substringBefore('=')
            if (links.isBlank()) return null
            val zahlen = mutableListOf<Int>()
            val zeichen = mutableListOf<Char>()
            var zahl = ""
            var erwarteZahl = true
            fun abschliessen(): Boolean {
                if (zahl.isEmpty()) return true
                if (!erwarteZahl) return false
                zahlen.add(zahl.toIntOrNull() ?: return false)
                zahl = ""
                erwarteZahl = false
                return true
            }
            for (c in links) {
                when {
                    c.isDigit() -> zahl += c
                    c == ' ' -> if (!abschliessen()) return null
                    c in "+-*/·:×÷" -> {
                        if (!abschliessen() || erwarteZahl) return null
                        zeichen.add(when (c) { '·', '×' -> '*'; ':', '÷' -> '/'; else -> c })
                        erwarteZahl = true
                    }
                    else -> return null
                }
            }
            if (!abschliessen() || erwarteZahl) return null
            if (zahlen.size < 2 || zahlen.size != zeichen.size + 1) return null
            return Rechenausdruck(zahlen, zeichen)
        }
    }
}

object FehlerAnalyse {

    /**
     * Sucht den Grund für eine falsche Antwort. null, wenn kein Muster passt.
     * [eingabe2] ist der eingegebene Rest bei Teilen mit Rest.
     */
    fun befund(
        art: String, rechnung: String, erklaerung: String, antwort: String, antwort2: String,
        eingabe: String, eingabe2: String = "",
    ): FehlerBefund? {
        val ausdruck = Rechenausdruck.lies(rechnung) ?: Rechenausdruck.lies(erklaerung)
        return when (art) {
            "rest" -> {
                val q = antwort.toIntOrNull(); val r = antwort2.toIntOrNull()
                val qi = eingabe.trim().toIntOrNull(); val ri = eingabe2.trim().toIntOrNull()
                if (q == null || r == null || qi == null || ri == null) null else rest(ausdruck, q, r, qi, ri)
            }
            "vergleich" -> vergleich(antwort, eingabe, erklaerung)
            "text" -> text(antwort, eingabe)
            "mauer" -> null
            else -> {
                val soll = antwort.toIntOrNull(); val ist = eingabe.trim().toIntOrNull()
                if (soll == null || ist == null) null else zahl(ausdruck, soll, ist)
            }
        }
    }

    private fun rest(a: Rechenausdruck?, q: Int, r: Int, qi: Int, ri: Int): FehlerBefund? {
        if (qi == q && ri == r) return null
        val zwei = a != null && a.zahlen.size == 2 && a.zeichen.first() == '/'
        val n = if (zwei) a!!.zahlen[0] else return null
        val t = a!!.zahlen[1]
        if (t <= 0) return null
        val richtig = "$n : $t = $q Rest $r"
        if (ri >= t && qi < q) {
            var s = "Dein Rest $ri ist nicht kleiner als $t. Dann passt die $t noch hinein."
            if (qi == 10) s += " Du bist bei 10 stehen geblieben."
            s += " Teile weiter, bis der Rest kleiner ist als $t: $richtig."
            return FehlerBefund(FehlerMuster.REST_ZU_GROSS, s)
        }
        if (qi == q) {
            return FehlerBefund(FehlerMuster.REST_FALSCH,
                "Das Ergebnis $q stimmt. Jetzt der Rest: $q · $t = ${q * t}. Bis $n fehlen noch $r. Also $richtig.")
        }
        if (qi * t > n) {
            return FehlerBefund(FehlerMuster.GETEILT_ZU_VIEL,
                "$qi · $t = ${qi * t}, das ist schon mehr als $n. Nimm eine kleinere Zahl. Richtig ist $richtig.")
        }
        return null
    }

    private fun zeichenText(o: Char) = when (o) { '*' -> "·"; '/' -> ":"; else -> o.toString() }

    private fun zahl(a: Rechenausdruck?, soll: Int, ist: Int): FehlerBefund? {
        if (soll == ist) return null
        if (a != null && a.zahlen.size == 2) {
            val op = a.zeichen.first()
            val x = a.zahlen[0]; val y = a.zahlen[1]
            val richtig = "$x ${zeichenText(op)} $y = $soll"
            when (op) {
                '*' -> {
                    if (ist == x + y) return FehlerBefund(FehlerMuster.PLUS_STATT_MAL,
                        "Du hast plus gerechnet. Der Punkt heißt mal: $richtig.")
                    for ((reihe, anzahl) in listOf(y to x, x to y)) {
                        if (reihe <= 0) continue
                        if (ist == soll + reihe) return FehlerBefund(FehlerMuster.MAL_REIHE_ZU_VIEL,
                            "Du hast eine $reihe zu viel genommen. $anzahl mal die $reihe sind $soll. Also $richtig.")
                        if (ist == soll - reihe) return FehlerBefund(FehlerMuster.MAL_REIHE_ZU_WENIG,
                            "Dir fehlt noch eine $reihe. $anzahl mal die $reihe sind $soll. Also $richtig.")
                    }
                }
                '/' -> {
                    if (ist == x * y) return FehlerBefund(FehlerMuster.MAL_STATT_GETEILT,
                        "Du hast mal gerechnet. Der Doppelpunkt heißt geteilt: $richtig.")
                    if (ist > 0 && y > 0) return FehlerBefund(FehlerMuster.GETEILT_DANEBEN,
                        "Prüf es mit der Umkehraufgabe: $ist · $y = ${ist * y}, das ist nicht $x. Aber $soll · $y = $x. Also $richtig.")
                }
                '+' -> {
                    if (ist == x - y) return FehlerBefund(FehlerMuster.MINUS_STATT_PLUS,
                        "Du hast minus gerechnet. Hier steht plus: $richtig.")
                    if (ist == x * y) return FehlerBefund(FehlerMuster.MAL_STATT_PLUS,
                        "Du hast mal gerechnet. Hier steht plus: $richtig.")
                }
                '-' -> {
                    if (ist == x + y) return FehlerBefund(FehlerMuster.PLUS_STATT_MINUS,
                        "Du hast plus gerechnet. Hier steht minus: $richtig.")
                }
            }
        }
        if (a != null && a.zahlen.size >= 3) {
            val falsch = a.linksNachRechts
            if (falsch != null && falsch == ist && falsch != soll) {
                return FehlerBefund(FehlerMuster.PUNKT_VOR_STRICH,
                    "Du hast von links nach rechts gerechnet. Es gilt Punkt vor Strich: Rechne zuerst mal oder geteilt, dann plus oder minus. Dann kommt $soll heraus.")
            }
        }
        if (istZifferDreher(soll, ist)) return FehlerBefund(FehlerMuster.ZIFFER_DREHER,
            "Fast! Du hast die Ziffern vertauscht. Richtig ist $soll, nicht $ist.")
        if (abs(soll - ist) == 10) return FehlerBefund(FehlerMuster.ZEHNER_DANEBEN,
            "Die Einer stimmen, aber bei den Zehnern bist du um 10 daneben. Richtig ist $soll.")
        if (abs(soll - ist) == 1) return FehlerBefund(FehlerMuster.KNAPP_DANEBEN,
            "Ganz knapp! Du bist nur um 1 daneben. Richtig ist $soll.")
        return null
    }

    fun istZifferDreher(a: Int, b: Int): Boolean =
        a != b && a >= 10 && b >= 10 && a.toString().reversed() == b.toString()

    private fun vergleich(soll: String, ist: String, erklaerung: String): FehlerBefund? {
        if (soll == ist || ist !in listOf("<", ">", "=")) return null
        val beleg = if (erklaerung.isEmpty()) "" else " Richtig ist: $erklaerung."
        if (soll == "=") return FehlerBefund(FehlerMuster.GLEICH_UEBERSEHEN,
            "Rechne beide Seiten aus. Sie sind genau gleich groß, deshalb gehört = in die Mitte.$beleg")
        if (ist == "=") return FehlerBefund(FehlerMuster.NICHT_GLEICH,
            "Rechne beide Seiten aus. Sie sind nicht gleich groß.$beleg")
        return FehlerBefund(FehlerMuster.VERGLEICH_UMGEDREHT,
            "Das Zeichen ist umgedreht. Die Spitze zeigt immer zur kleineren Zahl, die offene Seite zur größeren.$beleg")
    }

    private fun text(rohSoll: String, rohIst: String): FehlerBefund? {
        val soll = rohSoll.trim(); val ist = rohIst.trim()
        if (soll.isEmpty() || ist.isEmpty() || soll == ist) return null
        if (soll.lowercase() == ist.lowercase()) {
            val gross = soll.first().isUpperCase() && !ist.first().isUpperCase()
            return FehlerBefund(FehlerMuster.GROSS_KLEIN,
                if (gross) "Fast! Das schreibt man groß: $soll." else "Fast! Achte auf groß und klein. Richtig ist: $soll.")
        }
        val s = soll.lowercase().toList(); val i = ist.lowercase().toList()
        if (s.size == i.size) {
            val unterschiede = s.indices.filter { s[it] != i[it] }
            if (unterschiede.size == 1) {
                val u = unterschiede.first()
                val paar = setOf(s[u], i[u])
                if (paar in listOf(setOf('d', 't'), setOf('g', 'k'), setOf('b', 'p'))) {
                    return FehlerBefund(FehlerMuster.HART_WEICH,
                        "Am Wortende klingen ${s[u]} und ${i[u]} fast gleich. Verlängere das Wort, dann hörst du es. Richtig ist: $soll.")
                }
                if (paar == setOf('e', 'ä')) return FehlerBefund(FehlerMuster.AE_STATT_E,
                    "Suche ein verwandtes Wort mit a, dann weißt du, ob ä oder e. Richtig ist: $soll.")
                if (soll.length >= 3) return FehlerBefund(FehlerMuster.EIN_BUCHSTABE,
                    "Fast! Nur ein Buchstabe ist anders. Richtig ist: $soll.")
            }
        }
        if (s.size == i.size + 1) {
            for (k in s.indices) {
                val kurz = s.toMutableList().also { it.removeAt(k) }
                if (kurz != i) continue
                if (k > 0 && s[k] == s[k - 1] || (k + 1 < s.size && s[k] == s[k + 1])) {
                    return FehlerBefund(FehlerMuster.DOPPELTER_MITLAUT,
                        "Hier kommt ein Buchstabe doppelt. Nach einem kurz gesprochenen Selbstlaut folgen oft zwei gleiche Mitlaute. Richtig ist: $soll.")
                }
                if (s[k] == 'e' && k > 0 && s[k - 1] == 'i') return FehlerBefund(FehlerMuster.IE_STATT_I,
                    "Das i klingt hier lang. Lang gesprochenes i schreibt man meistens ie. Richtig ist: $soll.")
                if (soll.length >= 3) return FehlerBefund(FehlerMuster.EIN_BUCHSTABE,
                    "Fast! Da fehlt nur ein Buchstabe. Richtig ist: $soll.")
            }
        }
        return null
    }
}

/** Der Satz der Eule nach einer falschen Antwort. Ohne KI: der feste Satz, der genauso stimmt. */
object ErklaerFreund {
    fun loesung(a: Aufgabe) = if (a.art == Aufgabe.ART_REST) "${a.antwort} Rest ${a.antwort2}" else a.antwort

    private fun teile(gegeben: String): Pair<String, String> {
        val t = gegeben.split(" Rest ")
        return if (t.size == 2) t[0] to t[1] else gegeben to ""
    }

    fun befund(a: Aufgabe, gegeben: String): FehlerBefund? {
        val (e1, e2) = teile(gegeben)
        return FehlerAnalyse.befund(a.art, a.rechnung, a.erklaerung, a.antwort, a.antwort2, e1, e2)
    }

    /** Bei Zahlenmauern und ohne Eingabe gibt es nichts zu erklären. */
    fun lohntSich(a: Aufgabe, gegeben: String): Boolean = a.art != "mauer" && gegeben.isNotBlank()

    fun satz(a: Aufgabe, gegeben: String): String {
        befund(a, gegeben)?.let { return it.satz }
        return if (a.erklaerung.isEmpty()) "Richtig ist ${loesung(a)}." else "Richtig ist ${loesung(a)}. ${a.erklaerung}"
    }
}
