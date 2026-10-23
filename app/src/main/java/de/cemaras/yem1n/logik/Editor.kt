package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.daten.Uebung

/** Rechner für ganze Zahlen mit Punkt vor Strich und Klammern. Port von MatheRechner (iOS). */
object MatheRechner {
    class Fehler(val text: String) : Exception(text)

    private sealed interface Zeichen {
        data class Zahl(val n: Long) : Zeichen
        data class Op(val c: Char) : Zeichen
        data object Auf : Zeichen
        data object Zu : Zeichen
    }

    private fun zerlege(roh: String): List<Zeichen> {
        val t = mutableListOf<Zeichen>()
        val zahl = StringBuilder()
        fun abschluss() {
            if (zahl.isEmpty()) return
            val n = zahl.toString().toLongOrNull()
            if (n == null || n > 1_000_000) throw Fehler("Eine Zahl ist zu groß.")
            t += Zeichen.Zahl(n)
            zahl.clear()
        }
        for (ch in roh) {
            if (ch in '0'..'9') { zahl.append(ch); continue }
            abschluss()
            when (ch) {
                ' ', '=', '\n' -> {}
                '+' -> t += Zeichen.Op('+')
                '-', '−', '–', '—' -> t += Zeichen.Op('-')
                '·', '*', '×', 'x', 'X', '•' -> t += Zeichen.Op('*')
                ':', '/', '÷' -> t += Zeichen.Op('/')
                '(' -> t += Zeichen.Auf
                ')' -> t += Zeichen.Zu
                else -> throw Fehler("Das Zeichen „$ch“ kennt der Rechner nicht.")
            }
        }
        abschluss()
        return t
    }

    private class Parser(val t: List<Zeichen>) {
        var i = 0

        fun ausdruck(): Long {
            var w = term()
            while (i < t.size) {
                val z = t[i]
                if (z is Zeichen.Op && (z.c == '+' || z.c == '-')) {
                    i++
                    val r = term()
                    w = if (z.c == '+') w + r else w - r
                } else break
            }
            return w
        }

        fun term(): Long {
            var w = faktor()
            while (i < t.size) {
                val z = t[i]
                if (z is Zeichen.Op && (z.c == '*' || z.c == '/')) {
                    i++
                    val r = faktor()
                    if (z.c == '*') {
                        w *= r
                        if (kotlin.math.abs(w) > 1_000_000_000L) throw Fehler("Die Zahlen werden zu groß.")
                    } else {
                        if (r == 0L) throw Fehler("Durch null teilen geht nicht.")
                        if (w % r != 0L) throw Fehler("$w : $r geht nicht auf.")
                        w /= r
                    }
                } else break
            }
            return w
        }

        fun faktor(): Long {
            if (i >= t.size) throw Fehler("Die Rechnung ist unvollständig.")
            return when (val z = t[i]) {
                is Zeichen.Zahl -> { i++; z.n }
                Zeichen.Auf -> {
                    i++
                    val w = ausdruck()
                    if (i >= t.size || t[i] != Zeichen.Zu) throw Fehler("Eine Klammer fehlt.")
                    i++
                    w
                }
                else -> throw Fehler("Hier fehlt eine Zahl.")
            }
        }
    }

    fun werte(roh: String): Int {
        val t = zerlege(roh)
        if (t.isEmpty()) throw Fehler("Bitte eine Rechnung eingeben.")
        val p = Parser(t)
        val w = p.ausdruck()
        if (p.i != t.size) throw Fehler("Die Rechnung ist nicht ganz verständlich.")
        return w.coerceIn(-2_000_000_000L, 2_000_000_000L).toInt()
    }

    /** Schöne Schreibweise: Zeichen mit Leerzeichen, Mal als Punkt, Geteilt als Doppelpunkt. */
    fun anzeige(roh: String): String {
        val t = try { zerlege(roh) } catch (e: Fehler) { return roh }
        val s = StringBuilder()
        for (z in t) when (z) {
            is Zeichen.Zahl -> s.append(z.n)
            Zeichen.Auf -> s.append('(')
            Zeichen.Zu -> s.append(')')
            is Zeichen.Op -> s.append(when (z.c) { '+' -> " + "; '-' -> " - "; '*' -> " · "; else -> " : " })
        }
        return s.toString()
    }
}

/** Die Arten von Aufgaben im Formular des Editors. */
enum class FormArt(val titel: String, val standardFrage: String) {
    RECHNUNG("Rechnung (Lösung wird berechnet)", "Rechne."),
    REST("Teilen mit Rest", "Teile mit Rest. Rechne bis zum Ende."),
    VERGLEICH("Größer oder kleiner", "Was gehört in die Mitte?"),
    MAUER("Zahlenmauer", "Fülle die Mauer von unten nach oben."),
    RAETSEL("Zahlenrätsel", "Wie heißt die Zahl?"),
    SACH("Sachaufgabe", "Lies genau. Rechne und antworte."),
    WORT("Rechnung mit Wörtern", "Notiere die passende Rechnung."),
    FREI("Freie Aufgabe (Lösung selbst eingeben)", "Rechne."),
}

/** Alles, was im Formular eingegeben sein kann. Nicht benutzte Felder bleiben leer. */
data class FormEingabe(
    val art: FormArt,
    val frage: String = "",
    val hinweis: String = "",
    val ausdruck: String = "",
    val zahlA: String = "",
    val zahlB: String = "",
    val links: String = "",
    val rechts: String = "",
    val mauer: List<String> = listOf("", "", "", ""),
    val gerade: Boolean = true,
    val xer: Int = 5,
    val von: String = "",
    val bis: String = "",
    val text: String = "",
    val wortMinus: Boolean = true,
    val wortVariante: Int = 0,
    val freiRechnung: String = "",
    val freiAntwort: String = "",
)

data class FormErgebnis(val aufgabe: Aufgabe? = null, val fehler: String? = null, val hinweis: String? = null)

object AufgabenBauer {
    private val ZAHLWORT = listOf("", "Einer", "Zweier", "Dreier", "Vierer", "Fünfer", "Sechser", "Siebener", "Achter", "Neuner", "Zehner")

    private fun neu(
        art: String, frage: String, rechnung: String = "", antwort: String = "", antwort2: String = "",
        hinweis: String = "", erklaerung: String = "", reihen: List<List<Int>> = emptyList(),
    ) = Aufgabe(
        art = art, frage = frage, rechnung = rechnung, hinweis = hinweis, erklaerung = erklaerung,
        antwort = antwort, antwort2 = antwort2, reihen = reihen, bild = "", tr = "", gross = "",
        folge = emptyList(), optionen = emptyList(),
    )

    private fun zahl(s: String): Int? = s.trim().toIntOrNull()

    private fun rechne(roh: String): Int = MatheRechner.werte(roh)

    fun baue(e: FormEingabe): FormErgebnis {
        val f = e.frage.trim().ifEmpty { e.art.standardFrage }
        try {
            return when (e.art) {
                FormArt.RECHNUNG -> {
                    val w = rechne(e.ausdruck)
                    if (w !in 0..999) return FormErgebnis(fehler = "Das Ergebnis muss zwischen 0 und 999 liegen (hier: $w).")
                    val anz = MatheRechner.anzeige(e.ausdruck)
                    FormErgebnis(neu(Aufgabe.ART_ZAHL, f, "$anz =", w.toString(), hinweis = e.hinweis.trim(), erklaerung = "$anz = $w"))
                }
                FormArt.REST -> {
                    val a = zahl(e.zahlA); val b = zahl(e.zahlB)
                    if (a == null || b == null) return FormErgebnis(fehler = "Bitte beide Zahlen eingeben.")
                    if (a !in 0..999 || b !in 2..99) {
                        return FormErgebnis(fehler = "Die erste Zahl darf höchstens 999 sein, der Teiler muss zwischen 2 und 99 liegen.")
                    }
                    val q = a / b; val r = a % b
                    val erkl = "$a : $b = $q" + if (r > 0) " R $r" else " (kein Rest)"
                    FormErgebnis(
                        neu(Aufgabe.ART_REST, f, "$a : $b =", q.toString(), r.toString(), erklaerung = erkl),
                        hinweis = if (r == 0) "Hinweis: Diese Aufgabe geht ohne Rest auf." else null,
                    )
                }
                FormArt.VERGLEICH -> {
                    val lw = rechne(e.links); val rw = rechne(e.rechts)
                    val z = if (lw > rw) ">" else if (lw < rw) "<" else "="
                    FormErgebnis(
                        neu(Aufgabe.ART_VERGLEICH, f, "${MatheRechner.anzeige(e.links)}  ?  ${MatheRechner.anzeige(e.rechts)}", z,
                            erklaerung = "$lw $z $rw"),
                    )
                }
                FormArt.MAUER -> {
                    val basis = e.mauer.map { zahl(it) }
                    if (basis.size != 4 || basis.any { it == null }) {
                        return FormErgebnis(fehler = "Bitte alle vier Zahlen der untersten Reihe eingeben.")
                    }
                    val b = basis.filterNotNull()
                    if (b.any { it !in 1..99 }) return FormErgebnis(fehler = "Die Zahlen müssen zwischen 1 und 99 liegen.")
                    val reihen = mutableListOf(b)
                    while (reihen.last().size > 1) {
                        val l = reihen.last()
                        reihen += List(l.size - 1) { l[it] + l[it + 1] }
                    }
                    val spitze = reihen.last()[0]
                    if (spitze > 999) return FormErgebnis(fehler = "Die Spitze ($spitze) darf höchstens 999 sein.")
                    FormErgebnis(
                        neu(Aufgabe.ART_MAUER, f, reihen = reihen),
                        hinweis = if (spitze > 100) "Hinweis: Die Spitze ist $spitze. In Klasse 3 bleibt man meist bis 100." else null,
                    )
                }
                FormArt.RAETSEL -> {
                    val v = zahl(e.von); val bs = zahl(e.bis)
                    if (v == null || bs == null) return FormErgebnis(fehler = "Bitte „zwischen“ und „und“ eingeben.")
                    if (bs - v < 2 || v < 0 || bs > 1000) {
                        return FormErgebnis(fehler = "Der Bereich muss mindestens drei Zahlen umfassen und unter 1000 liegen.")
                    }
                    if (e.xer !in 2..10) return FormErgebnis(fehler = "Die Reihe muss zwischen 2 und 10 liegen.")
                    val treffer = (v + 1 until bs).filter { n ->
                        n % e.xer == 0 && (if (e.gerade) n % 2 == 0 else n % 2 == 1)
                    }
                    if (treffer.size != 1) {
                        val liste = if (treffer.isEmpty()) "keine" else treffer.joinToString(", ")
                        return FormErgebnis(fehler = "Die Lösung muss eindeutig sein. Passende Zahlen: $liste.")
                    }
                    val par = if (e.gerade) "gerade" else "ungerade"
                    val t = "Meine Zahl ist $par.\nSie ist eine ${ZAHLWORT[e.xer]}zahl.\nSie liegt zwischen $v und $bs."
                    FormErgebnis(neu(Aufgabe.ART_ZAHL, f, t, treffer[0].toString()))
                }
                FormArt.SACH -> {
                    val t = e.text.trim()
                    if (t.isEmpty()) return FormErgebnis(fehler = "Bitte den Text der Aufgabe eingeben.")
                    val w = rechne(e.ausdruck)
                    if (w !in 0..999) return FormErgebnis(fehler = "Das Ergebnis muss zwischen 0 und 999 liegen (hier: $w).")
                    FormErgebnis(neu(Aufgabe.ART_ZAHL, f, t, w.toString(), erklaerung = "${MatheRechner.anzeige(e.ausdruck)} = $w"))
                }
                FormArt.WORT -> {
                    val a = zahl(e.zahlA); val b = zahl(e.zahlB)
                    if (a == null || b == null) return FormErgebnis(fehler = "Bitte beide Zahlen eingeben.")
                    if (e.wortMinus) {
                        if (a <= b || b < 0 || a > 999) {
                            return FormErgebnis(fehler = "Die erste Zahl muss größer sein als die zweite und höchstens 999.")
                        }
                        val t = if (e.wortVariante == 0) "Subtrahiere von $a die Zahl $b." else "Wie heißt die Differenz der Zahlen $a und $b?"
                        FormErgebnis(neu(Aufgabe.ART_ZAHL, f, t, (a - b).toString(), erklaerung = "$a - $b = ${a - b}"))
                    } else {
                        if (a < 0 || b < 0 || a.toLong() * b > 999) return FormErgebnis(fehler = "Das Produkt darf höchstens 999 sein.")
                        val t = if (e.wortVariante == 0) "Multipliziere die Zahlen $a und $b." else "Wie heißt das Produkt der Zahlen $a und $b?"
                        FormErgebnis(neu(Aufgabe.ART_ZAHL, f, t, (a * b).toString(), erklaerung = "$a · $b = ${a * b}"))
                    }
                }
                FormArt.FREI -> {
                    val r = e.freiRechnung.trim()
                    if (r.isEmpty()) return FormErgebnis(fehler = "Bitte die Aufgabe eingeben.")
                    val w = zahl(e.freiAntwort)
                    if (w == null || w !in 0..999) return FormErgebnis(fehler = "Die Lösung muss eine ganze Zahl von 0 bis 999 sein.")
                    FormErgebnis(
                        neu(Aufgabe.ART_ZAHL, f, r, w.toString(), hinweis = e.hinweis.trim()),
                        hinweis = "Bei freien Aufgaben prüft die App die Lösung nicht. Bitte noch einmal selbst nachrechnen.",
                    )
                }
            }
        } catch (x: MatheRechner.Fehler) {
            return FormErgebnis(fehler = x.text)
        }
    }
}

/** Ein Befund beim Prüfen eines Entwurfs. */
data class Befund(val text: String, val schwer: Boolean)

/** Prüft einen Entwurf, bevor er gespeichert wird: Aufbau, Zahlenräume, Nachrechnen, Doppelte, Gedankenstriche. */
object EntwurfPruefer {
    private val GEDANKENSTRICHE = Regex("[–—]")
    private val NUR_RECHNUNG = Regex("^[0-9 +\\-·:()=x*/×÷]+$")

    fun pruefe(arbeit: Arbeit): List<Befund> {
        val b = mutableListOf<Befund>()
        if (arbeit.titel.isBlank()) b += Befund("Die Arbeit braucht einen Titel.", true)
        if (arbeit.uebungen.isEmpty()) b += Befund("Es gibt noch keine Übung.", true)
        val gesehen = mutableSetOf<String>()
        for (u in arbeit.uebungen) {
            if (u.titel.isBlank()) b += Befund("Eine Übung hat keinen Titel.", true)
            if (u.aufgaben.isEmpty()) b += Befund("„${u.titel}“ hat keine Aufgaben.", true)
            for ((i, a) in u.aufgaben.withIndex()) {
                val wo = "„${u.titel}“ Aufgabe ${i + 1}"
                val alles = listOf(a.frage, a.rechnung, a.antwort, a.hinweis, a.erklaerung).joinToString(" ")
                if (GEDANKENSTRICHE.containsMatchIn(alles)) b += Befund("$wo: enthält einen Gedankenstrich. Bitte durch - ersetzen.", true)
                when (a.art) {
                    Aufgabe.ART_MAUER -> if (!Mauer.gueltig(a.reihen)) b += Befund("$wo: Die Zahlenmauer ist nicht richtig aufgebaut.", true)
                    Aufgabe.ART_VERGLEICH -> {
                        if (a.antwort !in listOf("<", ">", "=")) b += Befund("$wo: Die Antwort muss <, > oder = sein.", true)
                        else nachVergleich(a)?.let { b += Befund("$wo: $it", true) }
                    }
                    Aufgabe.ART_REST -> {
                        val q = a.antwort.toIntOrNull(); val r = a.antwort2.toIntOrNull()
                        if (q == null || r == null) b += Befund("$wo: Ergebnis und Rest müssen Zahlen sein.", true)
                        else nachRest(a, q, r)?.let { b += Befund("$wo: $it", true) }
                    }
                    else -> {
                        if (a.antwort.isBlank()) b += Befund("$wo: Es fehlt die Antwort.", true)
                        else if (a.art == Aufgabe.ART_ZAHL) {
                            val n = a.antwort.toIntOrNull()
                            if (n == null || n !in 0..999) b += Befund("$wo: Die Antwort muss eine ganze Zahl von 0 bis 999 sein.", true)
                            else nachZahl(a, n)?.let { b += Befund("$wo: $it", true) }
                        }
                    }
                }
                if (a.rechnung.isBlank() && !a.istMauer) b += Befund("$wo: Es fehlt die Aufgabe.", true)
                if (!gesehen.add(a.rechnung + "|" + a.reihen)) b += Befund("$wo ist doppelt (${a.rechnung.take(30)}).", false)
            }
        }
        return b
    }

    private fun nachZahl(a: Aufgabe, n: Int): String? {
        if (!NUR_RECHNUNG.matches(a.rechnung.trim()) || !a.rechnung.trim().endsWith("=")) return null
        return try {
            val w = MatheRechner.werte(a.rechnung)
            if (w != n) "${a.rechnung.trim()} ergibt $w, nicht $n." else null
        } catch (e: MatheRechner.Fehler) { null }
    }

    private fun nachRest(a: Aufgabe, q: Int, r: Int): String? {
        val m = Regex("^\\s*(\\d+)\\s*:\\s*(\\d+)\\s*=?\\s*$").matchEntire(a.rechnung) ?: return null
        val x = m.groupValues[1].toIntOrNull() ?: return null
        val y = m.groupValues[2].toIntOrNull() ?: return null
        if (y == 0) return "Durch null teilen geht nicht."
        return if (x / y != q || x % y != r) "$x : $y ist ${x / y} Rest ${x % y}, nicht $q Rest $r." else null
    }

    private fun nachVergleich(a: Aufgabe): String? {
        val teile = a.rechnung.split(Regex("\\s+\\?\\s+"))
        if (teile.size != 2) return "Die Aufgabe braucht das Format „5 · 5  ?  9 · 7“."
        return try {
            val l = MatheRechner.werte(teile[0]); val r = MatheRechner.werte(teile[1])
            val z = if (l > r) ">" else if (l < r) "<" else "="
            if (z != a.antwort) "$l $z $r, nicht ${a.antwort}." else null
        } catch (e: MatheRechner.Fehler) { null }
    }
}

/** Entwurf einer Arbeit: Bearbeiten der Liste ohne Seiteneffekte. */
object EntwurfListe {
    fun neu(klasse: String, fach: String, titel: String) = Arbeit(klasse, fach, titel, emptyList())

    fun uebungNeu(titel: String, gruppe: String = "Eigene Übungen", symbol: String = "✎", tipp: String = "") =
        Uebung(titel, gruppe, symbol, tipp, emptyList())

    fun <T> verschiebe(liste: List<T>, von: Int, nach: Int): List<T> {
        if (von !in liste.indices || nach !in liste.indices || von == nach) return liste
        val m = liste.toMutableList()
        m.add(nach, m.removeAt(von))
        return m
    }

    /** Eine komplette Arbeit aus allen 13 Vorlagen, ohne Aufgaben aus [vorhanden]. */
    fun ausVorlagen(klasse: String, fach: String, titel: String, vorhanden: MutableSet<String>,
                    generator: MatheGenerator = MatheGenerator()): Arbeit {
        val uebungen = MatheGenerator.vorlagen.map { v -> uebungAusVorlage(v, vorhanden, generator) }
        return Arbeit(klasse, fach, titel, uebungen)
    }

    fun uebungAusVorlage(v: MatheVorlage, vorhanden: MutableSet<String>, generator: MatheGenerator = MatheGenerator()): Uebung =
        Uebung(v.name, v.gruppe, v.symbol, v.tipp, generator.fuelle(v, v.anzahl, vorhanden))
}
