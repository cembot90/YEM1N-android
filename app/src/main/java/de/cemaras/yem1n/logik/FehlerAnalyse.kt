package de.cemaras.yem1n.logik

import kotlin.math.abs

/**
 * Erkennt typische Denkfehler aus Rechnung, Eingabe und Lösung (Port von `FehlerAnalyse.swift`).
 * Das macht ausschließlich der Code, damit nichts Falsches als Grund genannt wird.
 */
enum class FehlerMuster(val elternText: String, val elternTipp: String? = null) {
    RESTZUGROSS(
        "hört beim Teilen mit Rest zu früh auf, der Rest ist dann zu groß",
        "Gemeinsam prüfen: Ist der Rest kleiner als die Zahl, durch die geteilt wird? Wenn nicht, passt sie noch einmal hinein.",
    ),
    RESTFALSCH(
        "hat beim Teilen mit Rest das Ergebnis richtig, aber den Rest nicht genau ausgerechnet",
        "Den Rest mit einer Malaufgabe prüfen: Ergebnis mal Teiler, dann bis zur Ausgangszahl auffüllen.",
    ),
    GETEILTZUVIEL(
        "nimmt beim Teilen eine zu große Zahl",
        "Nach jeder Geteilt-Aufgabe die Umkehraufgabe sagen lassen: Ergebnis mal Teiler.",
    ),
    PLUSSTATTMAL("verwechselt manchmal die Rechenzeichen"),
    MALSTATTPLUS("verwechselt manchmal die Rechenzeichen"),
    PLUSSTATTMINUS("verwechselt manchmal die Rechenzeichen"),
    MINUSSTATTPLUS("verwechselt manchmal die Rechenzeichen"),
    MALSTATTGETEILT("verwechselt manchmal die Rechenzeichen"),
    MALREIHEZUVIEL(
        "nimmt beim Malrechnen eine Reihe zu viel oder zu wenig",
        "Die Einmaleins-Reihe laut zählen, dabei an den Fingern mitzählen, wie viele es sind.",
    ),
    MALREIHEZUWENIG(
        "nimmt beim Malrechnen eine Reihe zu viel oder zu wenig",
        "Die Einmaleins-Reihe laut zählen, dabei an den Fingern mitzählen, wie viele es sind.",
    ),
    GETEILTDANEBEN(
        "liegt beim Geteilt daneben und prüft nicht mit der Umkehraufgabe",
        "Nach jeder Geteilt-Aufgabe die Umkehraufgabe sagen lassen: Ergebnis mal Teiler.",
    ),
    PUNKTVORSTRICH(
        "rechnet von links nach rechts statt Punkt vor Strich",
        "Einen Bogen unter die Mal- oder Geteiltaufgabe malen und die zuerst ausrechnen.",
    ),
    ZIFFERDREHER(
        "vertauscht manchmal die Ziffern einer Zahl",
        "Zahlen erst laut sagen, dann schreiben: siebenundsechzig, also zuerst die 6.",
    ),
    ZEHNERDANEBEN("verrechnet sich bei den Zehnern"),
    KNAPPDANEBEN("liegt oft nur um 1 daneben, eher Flüchtigkeit"),
    VERGLEICHUMGEDREHT(
        "verwechselt die Zeichen < und >",
        "Merkhilfe: Das Krokodil frisst immer die größere Zahl, das offene Maul zeigt zu ihr.",
    ),
    GLEICHUEBERSEHEN("erkennt nicht sicher, wann beide Seiten gleich sind"),
    NICHTGLEICH("erkennt nicht sicher, wann beide Seiten gleich sind"),
    GROSSKLEIN("achtet nicht immer auf groß und klein"),
    HARTWEICH(
        "verwechselt d und t, g und k oder b und p",
        "Das Wort verlängern: der Hund, die Hunde. Dann hört man, ob d oder t.",
    ),
    AESTATTE("verwechselt e und ä", "Ein verwandtes Wort mit a suchen: Äpfel kommt von Apfel."),
    DOPPELTERMITLAUT(
        "vergisst doppelte Mitlaute wie in Sonne",
        "Das Wort langsam klatschen. Nach einem kurzen Selbstlaut folgen oft zwei gleiche Mitlaute.",
    ),
    IESTATTI("schreibt i statt ie", "Lang gesprochenes i schreibt man meistens ie, wie in Biene."),
    EINBUCHSTABE("macht Flüchtigkeitsfehler bei einzelnen Buchstaben"),
}

data class FehlerBefund(val muster: FehlerMuster, val satz: String)

/** Eine einfache Rechnung ohne Klammern, zum Beispiel 9 · 9 + 8. Zeichen: '+', '-', '*', '/'. */
data class Rechenausdruck(val zahlen: List<Int>, val zeichen: List<Char>) {
    /** Mit Punkt vor Strich, so wie es richtig ist. Null bei Teilen durch 0. */
    val wert: Int? get() = rechne(zahlen, zeichen, true)

    /** Stur von links nach rechts, der typische Fehler. */
    val linksNachRechts: Int? get() = rechne(zahlen, zeichen, false)

    companion object {
        private fun an(a: Int, o: Char, b: Int): Int? = when (o) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b == 0) null else a / b
            else -> null
        }

        private fun rechne(z: List<Int>, op: List<Char>, punktZuerst: Boolean): Int? {
            if (z.size != op.size + 1 || z.isEmpty()) return null
            if (!punktZuerst) {
                var w = z[0]
                for ((i, o) in op.withIndex()) w = an(w, o, z[i + 1]) ?: return null
                return w
            }
            val werte = mutableListOf(z[0])
            val striche = mutableListOf<Char>()
            for ((i, o) in op.withIndex()) {
                if (o == '*' || o == '/') {
                    val letzter = werte.removeAt(werte.lastIndex)
                    werte.add(an(letzter, o, z[i + 1]) ?: return null)
                } else {
                    striche.add(o)
                    werte.add(z[i + 1])
                }
            }
            var w = werte[0]
            for ((i, o) in striche.withIndex()) w = an(w, o, werte[i + 1]) ?: return null
            return w
        }

        private val TEIL = Regex("""\d+|[+\-·:*/×÷]""")

        /** Liest "7 · 8 =" oder "58 : 4 = 14 R 2" (nur bis zum Gleichzeichen). Wörter ergeben null. */
        fun lies(text: String): Rechenausdruck? {
            val erste = text.lines().firstOrNull { it.isNotBlank() } ?: return null
            val links = erste.substringBefore("=").trim()
            if (links.isEmpty()) return null
            val tokens = TEIL.findAll(links).map { it.value }.toList()
            // Alles außer Leerzeichen muss erkannt worden sein
            if (tokens.joinToString("") != links.filter { !it.isWhitespace() }) return null
            val zahlen = mutableListOf<Int>()
            val zeichen = mutableListOf<Char>()
            for ((i, t) in tokens.withIndex()) {
                if (t[0].isDigit()) {
                    if (i % 2 != 0) return null
                    zahlen.add(t.toIntOrNull() ?: return null)
                } else {
                    if (i % 2 != 1) return null
                    zeichen.add(
                        when (t[0]) {
                            '·', '×', '*' -> '*'
                            ':', '÷', '/' -> '/'
                            else -> t[0]
                        },
                    )
                }
            }
            if (zahlen.size < 2 || zahlen.size != zeichen.size + 1) return null
            return Rechenausdruck(zahlen, zeichen)
        }
    }
}

object FehlerAnalyse {

    /** Sucht den Grund für eine falsche Antwort. Null, wenn kein bekanntes Muster passt. */
    fun befund(
        art: String, rechnung: String, erklaerung: String, antwort: String, antwort2: String,
        eingabe: String, eingabe2: String = "",
    ): FehlerBefund? {
        val ausdruck = Rechenausdruck.lies(rechnung) ?: Rechenausdruck.lies(erklaerung)
        return when (art) {
            "rest" -> {
                val q = antwort.trim().toIntOrNull() ?: return null
                val r = antwort2.trim().toIntOrNull() ?: return null
                val qi = eingabe.trim().toIntOrNull() ?: return null
                val ri = eingabe2.trim().toIntOrNull() ?: return null
                rest(ausdruck, q, r, qi, ri)
            }
            "vergleich" -> vergleich(antwort, eingabe, erklaerung)
            "text" -> text(antwort, eingabe)
            "mauer" -> null
            else -> {
                val soll = antwort.trim().toIntOrNull() ?: return null
                val ist = eingabe.trim().toIntOrNull() ?: return null
                zahl(ausdruck, soll, ist)
            }
        }
    }

    /** Dasselbe für einen Eintrag aus dem Antwort-Protokoll. Die Art ergibt sich aus der Lösung. */
    fun befund(d: AntwortDetail): FehlerBefund? {
        if (d.stand != AntwortProtokoll.FALSCH) return null
        if (d.rechnung.startsWith("Zahlenmauer")) return null
        val teile = d.loesung.split(" Rest ")
        if (teile.size == 2) {
            val ein = d.eingabe.split(" Rest ")
            if (ein.size != 2) return null
            return befund("rest", d.rechnung, "", teile[0], teile[1], ein[0], ein[1])
        }
        if (d.loesung in listOf("<", ">", "=")) return befund("vergleich", d.rechnung, "", d.loesung, "", d.eingabe)
        if (d.loesung.trim().toIntOrNull() != null) return befund("zahl", d.rechnung, "", d.loesung, "", d.eingabe)
        return befund("text", d.rechnung, "", d.loesung, "", d.eingabe)
    }

    private fun rest(a: Rechenausdruck?, q: Int, r: Int, qi: Int, ri: Int): FehlerBefund? {
        if (qi == q && ri == r) return null
        val teilen = a != null && a.zahlen.size == 2 && a.zeichen.firstOrNull() == '/'
        if (!teilen || a == null) return null
        val n = a.zahlen[0]
        val t = a.zahlen[1]
        if (t <= 0) return null
        val richtig = "$n : $t = $q Rest $r"
        if (ri >= t && qi < q) {
            var s = "Dein Rest $ri ist nicht kleiner als $t. Dann passt die $t noch hinein."
            if (qi == 10) s += " Du bist bei 10 stehen geblieben."
            s += " Teile weiter, bis der Rest kleiner ist als $t: $richtig."
            return FehlerBefund(FehlerMuster.RESTZUGROSS, s)
        }
        if (qi == q) {
            return FehlerBefund(
                FehlerMuster.RESTFALSCH,
                "Das Ergebnis $q stimmt. Jetzt der Rest: $q · $t = ${q * t}. Bis $n fehlen noch $r. Also $richtig.",
            )
        }
        if (qi * t > n) {
            return FehlerBefund(
                FehlerMuster.GETEILTZUVIEL,
                "$qi · $t = ${qi * t}, das ist schon mehr als $n. Nimm eine kleinere Zahl. Richtig ist $richtig.",
            )
        }
        return null
    }

    private fun zeichenText(o: Char): String = when (o) {
        '*' -> "·"
        '/' -> ":"
        else -> o.toString()
    }

    private fun zahl(a: Rechenausdruck?, soll: Int, ist: Int): FehlerBefund? {
        if (soll == ist) return null
        if (a != null && a.zahlen.size == 2) {
            val op = a.zeichen[0]
            val x = a.zahlen[0]
            val y = a.zahlen[1]
            val richtig = "$x ${zeichenText(op)} $y = $soll"
            when (op) {
                '*' -> {
                    if (ist == x + y) {
                        return FehlerBefund(FehlerMuster.PLUSSTATTMAL, "Du hast plus gerechnet. Der Punkt heißt mal: $richtig.")
                    }
                    for ((reihe, anzahl) in listOf(y to x, x to y)) {
                        if (reihe <= 0) continue
                        if (ist == soll + reihe) {
                            return FehlerBefund(
                                FehlerMuster.MALREIHEZUVIEL,
                                "Du hast eine $reihe zu viel genommen. $anzahl mal die $reihe sind $soll. Also $richtig.",
                            )
                        }
                        if (ist == soll - reihe) {
                            return FehlerBefund(
                                FehlerMuster.MALREIHEZUWENIG,
                                "Dir fehlt noch eine $reihe. $anzahl mal die $reihe sind $soll. Also $richtig.",
                            )
                        }
                    }
                }
                '/' -> {
                    if (ist == x * y) {
                        return FehlerBefund(FehlerMuster.MALSTATTGETEILT, "Du hast mal gerechnet. Der Doppelpunkt heißt geteilt: $richtig.")
                    }
                    if (ist > 0 && y > 0) {
                        return FehlerBefund(
                            FehlerMuster.GETEILTDANEBEN,
                            "Prüf es mit der Umkehraufgabe: $ist · $y = ${ist * y}, das ist nicht $x. Aber $soll · $y = $x. Also $richtig.",
                        )
                    }
                }
                '+' -> {
                    if (ist == x - y) return FehlerBefund(FehlerMuster.MINUSSTATTPLUS, "Du hast minus gerechnet. Hier steht plus: $richtig.")
                    if (ist == x * y) return FehlerBefund(FehlerMuster.MALSTATTPLUS, "Du hast mal gerechnet. Hier steht plus: $richtig.")
                }
                '-' -> {
                    if (ist == x + y) return FehlerBefund(FehlerMuster.PLUSSTATTMINUS, "Du hast plus gerechnet. Hier steht minus: $richtig.")
                }
            }
        }
        if (a != null && a.zahlen.size >= 3) {
            val falsch = a.linksNachRechts
            if (falsch != null && falsch == ist && falsch != soll) {
                return FehlerBefund(
                    FehlerMuster.PUNKTVORSTRICH,
                    "Du hast von links nach rechts gerechnet. Es gilt Punkt vor Strich: Rechne zuerst mal oder geteilt, dann plus oder minus. Dann kommt $soll heraus.",
                )
            }
        }
        if (istZifferDreher(soll, ist)) {
            return FehlerBefund(FehlerMuster.ZIFFERDREHER, "Fast! Du hast die Ziffern vertauscht. Richtig ist $soll, nicht $ist.")
        }
        if (abs(soll - ist) == 10) {
            return FehlerBefund(FehlerMuster.ZEHNERDANEBEN, "Die Einer stimmen, aber bei den Zehnern bist du um 10 daneben. Richtig ist $soll.")
        }
        if (abs(soll - ist) == 1) {
            return FehlerBefund(FehlerMuster.KNAPPDANEBEN, "Ganz knapp! Du bist nur um 1 daneben. Richtig ist $soll.")
        }
        return null
    }

    fun istZifferDreher(a: Int, b: Int): Boolean {
        if (a == b || a < 10 || b < 10) return false
        return a.toString().reversed() == b.toString()
    }

    private fun vergleich(soll: String, ist: String, erklaerung: String): FehlerBefund? {
        if (soll == ist || ist !in listOf("<", ">", "=")) return null
        val beleg = if (erklaerung.isEmpty()) "" else " Richtig ist: $erklaerung."
        if (soll == "=") {
            return FehlerBefund(
                FehlerMuster.GLEICHUEBERSEHEN,
                "Rechne beide Seiten aus. Sie sind genau gleich groß, deshalb gehört = in die Mitte.$beleg",
            )
        }
        if (ist == "=") {
            return FehlerBefund(FehlerMuster.NICHTGLEICH, "Rechne beide Seiten aus. Sie sind nicht gleich groß.$beleg")
        }
        return FehlerBefund(
            FehlerMuster.VERGLEICHUMGEDREHT,
            "Das Zeichen ist umgedreht. Die Spitze zeigt immer zur kleineren Zahl, die offene Seite zur größeren.$beleg",
        )
    }

    private fun text(rohSoll: String, rohIst: String): FehlerBefund? {
        val soll = rohSoll.trim()
        val ist = rohIst.trim()
        if (soll.isEmpty() || ist.isEmpty() || soll == ist) return null
        if (soll.lowercase() == ist.lowercase()) {
            val gross = soll.first().isUpperCase() && !ist.first().isUpperCase()
            val satz = if (gross) "Fast! Das schreibt man groß: $soll." else "Fast! Achte auf groß und klein. Richtig ist: $soll."
            return FehlerBefund(FehlerMuster.GROSSKLEIN, satz)
        }
        val s = soll.lowercase().toList()
        val i = ist.lowercase().toList()
        if (s.size == i.size) {
            val unterschiede = s.indices.filter { s[it] != i[it] }
            if (unterschiede.size == 1) {
                val u = unterschiede[0]
                val paar = setOf(s[u], i[u])
                if (paar in listOf(setOf('d', 't'), setOf('g', 'k'), setOf('b', 'p'))) {
                    return FehlerBefund(
                        FehlerMuster.HARTWEICH,
                        "Am Wortende klingen ${s[u]} und ${i[u]} fast gleich. Verlängere das Wort, dann hörst du es. Richtig ist: $soll.",
                    )
                }
                if (paar == setOf('e', 'ä')) {
                    return FehlerBefund(
                        FehlerMuster.AESTATTE,
                        "Suche ein verwandtes Wort mit a, dann weißt du, ob ä oder e. Richtig ist: $soll.",
                    )
                }
                if (soll.length >= 3) {
                    return FehlerBefund(FehlerMuster.EINBUCHSTABE, "Fast! Nur ein Buchstabe ist anders. Richtig ist: $soll.")
                }
            }
        }
        if (s.size == i.size + 1) {
            for (k in s.indices) {
                val kurz = s.toMutableList().also { it.removeAt(k) }
                if (kurz != i) continue
                if (k > 0 && (s[k] == s[k - 1] || (k + 1 < s.size && s[k] == s[k + 1]))) {
                    return FehlerBefund(
                        FehlerMuster.DOPPELTERMITLAUT,
                        "Hier kommt ein Buchstabe doppelt. Nach einem kurz gesprochenen Selbstlaut folgen oft zwei gleiche Mitlaute. Richtig ist: $soll.",
                    )
                }
                if (s[k] == 'e' && k > 0 && s[k - 1] == 'i') {
                    return FehlerBefund(
                        FehlerMuster.IESTATTI,
                        "Das i klingt hier lang. Lang gesprochenes i schreibt man meistens ie. Richtig ist: $soll.",
                    )
                }
                if (soll.length >= 3) {
                    return FehlerBefund(FehlerMuster.EINBUCHSTABE, "Fast! Da fehlt nur ein Buchstabe. Richtig ist: $soll.")
                }
            }
        }
        return null
    }
}
