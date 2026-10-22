package de.cemaras.yem1n.logik.lehrplan

import de.cemaras.yem1n.logik.Bundesland
import de.cemaras.yem1n.logik.Lern
import de.cemaras.yem1n.logik.LernAufgabe
import de.cemaras.yem1n.logik.LernThema
import de.cemaras.yem1n.logik.LehrFach

// Lernwelt Mathe, Klasse 1 bis 4. Port von LehrplanMathe.swift.
//
// Klasse 1: Zahlen bis 20, Plus und Minus, Zerlegen, Nachbarzahlen
// Klasse 2: Zahlen bis 100, Zehner und Einer, Einmaleins, Geteilt, Geld, Uhr
// Klasse 3: Zahlen bis 1000, schriftlich Plus und Minus, Teilen mit Rest, Längen, Gewicht
// Klasse 4: Zahlen bis 1 Million, Runden, schriftlich Mal und Geteilt, Zeitspannen, Brüche bei Größen
// Das ganze Einmaleins ist in Bayern und Hessen Ende Klasse 2 verlangt, in NRW Ende Klasse 4
// (dort hier in Klasse 3). Gewichte (g, kg) stehen in NRW erst bei Klasse 4.

object LehrplanMathe {
    private val M = LehrFach.MATHE

    val themen: List<LernThema> by lazy {
        listOf(
            // Klasse 1
            LernThema("m1-plus10", M, "Plus bis 10", "➕", "Zwei Zahlen zusammenrechnen", 1, erzeuge = ::plus10),
            LernThema("m1-minus10", M, "Minus bis 10", "➖", "Etwas wegnehmen", 1, erzeuge = ::minus10),
            LernThema("m1-zerlegen", M, "Zahlen zerlegen", "✂️", "7 = 3 + ?", 1, erzeuge = ::zerlegen),
            LernThema(
                "m1-zwanzig", M, "Plus und Minus bis 20", "🔟", "Über den Zehner rechnen", 1,
                tipp = "Rechne erst bis zur 10, dann weiter. 8 + 5: erst 8 + 2 = 10, dann noch 3.",
                erzeuge = ::zwanzig,
            ),
            LernThema("m1-nachbar", M, "Nachbarzahlen", "🏘️", "Welche Zahl kommt davor und danach?", 1, erzeuge = ::nachbarzahl),
            // Klasse 2
            LernThema(
                "m2-hundert", M, "Plus und Minus bis 100", "💯", "Mit Zehnern und Einern rechnen", 2,
                tipp = "Rechne erst die Zehner, dann die Einer.", erzeuge = ::hundert,
            ),
            LernThema("m2-stellen", M, "Zehner und Einer", "🧱", "Wie ist eine Zahl gebaut?", 2, erzeuge = ::zehnerEiner),
            LernThema("m2-kern", M, "Kernaufgaben Einmaleins", "⭐️", "Mal 1, 2, 5, 10 und Quadratzahlen", 2, erzeuge = ::kern),
            LernThema(
                "m2-einmaleins", M, "Das ganze Einmaleins", "✖️", "Alle Reihen von 2 bis 10", 2,
                abweichend = mapOf(Bundesland.NRW to 3),
                tipp = "Nutze die Kernaufgaben: 9 · 8 ist 10 · 8 minus 8.", erzeuge = ::einmaleins,
            ),
            LernThema(
                "m2-geteilt", M, "Geteilt mit Umkehraufgabe", "➗", "Gerecht verteilen", 2,
                abweichend = mapOf(Bundesland.NRW to 3), erzeuge = ::geteilt,
            ),
            LernThema("m2-geld", M, "Euro und Cent", "💶", "Mit Geld rechnen", 2, tipp = "1 € sind 100 ct.", erzeuge = ::geld),
            LernThema(
                "m2-uhr", M, "Uhr und Minuten", "🕐", "Stunden, halbe und Viertelstunden", 2,
                tipp = "Eine Stunde hat 60 Minuten.", erzeuge = ::uhr,
            ),
            // Klasse 3
            LernThema("m3-tausend", M, "Rechnen bis 1000", "🔢", "Mit Hundertern und Zehnern", 3, erzeuge = ::tausend),
            LernThema(
                "m3-schriftplus", M, "Schriftlich addieren", "📝", "Untereinander schreiben und rechnen", 3,
                tipp = "Fang bei den Einern an. Ist es mehr als 9, schreib den Übertrag zur nächsten Stelle.",
                erzeuge = ::schriftPlus,
            ),
            LernThema(
                "m3-schriftminus", M, "Schriftlich subtrahieren", "📝", "Untereinander schreiben und abziehen", 3,
                tipp = "Fang bei den Einern an. Reicht es nicht, nimm dir einen Zehner von der nächsten Stelle.",
                erzeuge = ::schriftMinus,
            ),
            LernThema(
                "m3-rest", M, "Teilen mit Rest", "🍪", "Was übrig bleibt, ist der Rest", 3,
                tipp = "Der Rest ist immer kleiner als die Zahl, durch die du teilst.", erzeuge = ::rest,
            ),
            LernThema(
                "m3-laengen", M, "Längen: m, cm und km", "📏", "Umrechnen von Längen", 3,
                tipp = "1 m sind 100 cm. 1 km sind 1000 m.", erzeuge = ::laengen,
            ),
            LernThema(
                "m3-gewicht", M, "Gewicht: kg und g", "⚖️", "Umrechnen von Gewichten", 3,
                abweichend = mapOf(Bundesland.NRW to 4), tipp = "1 kg sind 1000 g.", erzeuge = ::gewicht,
            ),
            // Klasse 4
            LernThema("m4-million", M, "Zahlen bis 1 Million", "🚀", "Große Zahlen lesen und verstehen", 4, erzeuge = ::million),
            LernThema(
                "m4-runden", M, "Runden", "🎯", "Auf Zehner, Hunderter und Tausender", 4,
                tipp = "Schau auf die Ziffer rechts daneben. Bei 0 bis 4 abrunden, bei 5 bis 9 aufrunden.",
                erzeuge = ::runden,
            ),
            LernThema(
                "m4-schriftmal", M, "Schriftlich multiplizieren", "✍️", "Große Malaufgaben", 4,
                tipp = "Rechne Stelle für Stelle von rechts und denk an den Übertrag.", erzeuge = ::schriftMal,
            ),
            LernThema(
                "m4-schriftgeteilt", M, "Schriftlich dividieren", "✍️", "Große Zahlen durch eine Ziffer teilen", 4,
                tipp = "Teile Stelle für Stelle von links. Prüfe mit der Malaufgabe.", erzeuge = ::schriftGeteilt,
            ),
            LernThema(
                "m4-zeit", M, "Zeitspannen", "⏱️", "Wie lange dauert es?", 4,
                tipp = "Rechne erst bis zur vollen Stunde, dann weiter.", erzeuge = ::zeitspanne,
            ),
            LernThema("m4-brueche", M, "Brüche bei Größen", "🍕", "Halbe, Viertel und Dreiviertel", 4, erzeuge = ::brueche),
        )
    }

    private const val RECHNE = "Rechne."
    private fun z(r: IntRange) = Lern.z(r)

    // Klasse 1

    fun plus10(): LernAufgabe {
        val a = z(1..9)
        val b = z(1..(10 - a))
        return Lern.zahl(RECHNE, "$a + $b =", a + b, erklaerung = "$a + $b = ${a + b}")
    }

    fun minus10(): LernAufgabe {
        val a = z(2..10)
        val b = z(1..a)
        return Lern.zahl(RECHNE, "$a - $b =", a - b, erklaerung = "$a - $b = ${a - b}")
    }

    fun zerlegen(): LernAufgabe {
        val ganz = z(5..10)
        val teil = z(1..(ganz - 1))
        return Lern.zahl(
            "Zerlege die Zahl. Was fehlt?", "$ganz = $teil + ?", ganz - teil,
            erklaerung = "$teil und ${ganz - teil} sind zusammen $ganz.",
        )
    }

    fun zwanzig(): LernAufgabe {
        if (Lern.wahr(0.5)) {
            val a = z(2..9)
            val b = z((11 - a)..9)
            return Lern.zahl(
                RECHNE, "$a + $b =", a + b,
                erklaerung = "$a + ${10 - a} = 10, dann noch ${a + b - 10} dazu: ${a + b}",
            )
        }
        val a = z(11..18)
        val b = z((a % 10 + 1)..9)
        return Lern.zahl(
            RECHNE, "$a - $b =", a - b,
            erklaerung = "$a - ${a - 10} = 10, dann noch ${b - (a - 10)} weg: ${a - b}",
        )
    }

    fun nachbarzahl(): LernAufgabe {
        val n = z(2..19)
        return when (z(0..2)) {
            0 -> Lern.zahl("Welche Zahl kommt danach?", "$n, ?", n + 1, erklaerung = "Nach $n kommt ${n + 1}.")
            1 -> Lern.zahl("Welche Zahl kommt davor?", "?, $n", n - 1, erklaerung = "Vor $n kommt ${n - 1}.")
            else -> {
                val m = minOf(n, 18)
                Lern.zahl(
                    "Welche Zahl liegt dazwischen?", "${m - 1}, ?, ${m + 1}", m,
                    erklaerung = "Zwischen ${m - 1} und ${m + 1} liegt $m.",
                )
            }
        }
    }

    // Klasse 2

    fun hundert(): LernAufgabe = when (z(0..3)) {
        0 -> {
            val a = z(2..8) * 10
            val b = z(1..(9 - a / 10)) * 10
            Lern.zahl(RECHNE, "$a + $b =", a + b, erklaerung = "$a + $b = ${a + b}")
        }
        1 -> {
            val a = z(11..89)
            val b = z(2..(99 - a))
            Lern.zahl(RECHNE, "$a + $b =", a + b, erklaerung = "$a + $b = ${a + b}")
        }
        2 -> {
            val a = z(20..99)
            val b = z(2..(a - 5))
            Lern.zahl(RECHNE, "$a - $b =", a - b, erklaerung = "$a - $b = ${a - b}")
        }
        else -> {
            val a = z(3..9) * 10
            val b = z(1..(a / 10 - 1)) * 10
            Lern.zahl(RECHNE, "$a - $b =", a - b, erklaerung = "$a - $b = ${a - b}")
        }
    }

    fun zehnerEiner(): LernAufgabe {
        val zehner = z(1..9)
        val e = z(0..9)
        val n = 10 * zehner + e
        return when (z(0..2)) {
            0 -> Lern.zahl(
                "Welche Zahl ist das?", "$zehner Zehner und $e Einer", n,
                erklaerung = "$zehner Zehner sind ${10 * zehner}, dazu $e Einer: $n",
            )
            1 -> Lern.zahl(
                "Wie viele Zehner hat die Zahl?", "$n", zehner,
                erklaerung = "$n = ${10 * zehner} + $e. Das sind $zehner Zehner.",
            )
            else -> Lern.zahl(
                "Wie viele Einer hat die Zahl?", "$n", e,
                erklaerung = "$n = ${10 * zehner} + $e. Das sind $e Einer.",
            )
        }
    }

    fun kern(): LernAufgabe {
        if (Lern.wahr(0.2)) {
            val q = z(2..10)
            return Lern.zahl("Das ist eine Quadratzahl.", "$q · $q =", q * q, erklaerung = "$q · $q = ${q * q}")
        }
        val k = Lern.eins(listOf(0, 1, 2, 5, 10))
        val n = z(1..10)
        val vorn = Lern.wahr(0.5)
        val r = if (vorn) "$k · $n =" else "$n · $k ="
        val e = if (k == 0) "Mal 0 ist immer 0." else (if (vorn) "$k · $n" else "$n · $k") + " = ${k * n}"
        return Lern.zahl("Das ist eine Kernaufgabe.", r, k * n, erklaerung = e)
    }

    fun einmaleins(): LernAufgabe {
        val a = z(2..10)
        val b = z(2..10)
        return Lern.zahl(RECHNE, "$a · $b =", a * b, erklaerung = "$a · $b = ${a * b}")
    }

    fun geteilt(): LernAufgabe {
        val t = z(2..10)
        val q = z(1..10)
        return Lern.zahl(
            "Rechne und denk an die Umkehraufgabe.", "${t * q} : $t =", q,
            erklaerung = "${t * q} : $t = $q, denn $q · $t = ${t * q}",
            hinweis = "Denn ___ · $t = ${t * q}",
        )
    }

    fun geld(): LernAufgabe = when (z(0..2)) {
        0 -> {
            val e = z(1..9)
            val c = z(1..19) * 5
            Lern.zahl(
                "Wie viel Cent sind das?", "$e € $c ct = ? ct", e * 100 + c,
                erklaerung = "$e € sind ${e * 100} ct, dazu $c ct: ${e * 100 + c} ct",
            )
        }
        1 -> {
            val c = z(1..19) * 5
            Lern.zahl(
                "Wie viel Cent fehlen bis 1 €?", "$c ct + ? ct = 1 €", 100 - c,
                erklaerung = "1 € sind 100 ct. $c ct + ${100 - c} ct = 100 ct",
            )
        }
        else -> {
            val e = z(2..9)
            Lern.zahl(
                "Wie viel Cent sind das?", "$e € = ? ct", e * 100,
                erklaerung = "1 € sind 100 ct, also sind $e € ${e * 100} ct.",
            )
        }
    }

    private fun gross1(s: String) = s.replaceFirstChar { it.uppercase() }

    fun uhr(): LernAufgabe = when (z(0..3)) {
        0 -> {
            val teil = Lern.eins(
                listOf(
                    "eine halbe Stunde" to 30, "eine Viertelstunde" to 15,
                    "eine Dreiviertelstunde" to 45, "eine Stunde" to 60,
                ),
            )
            Lern.zahl(
                "Wie viele Minuten sind das?", gross1(teil.first), teil.second,
                erklaerung = "${gross1(teil.first)} hat ${teil.second} Minuten.",
            )
        }
        1 -> {
            val m = z(1..11) * 5
            Lern.zahl(
                "Wie viele Minuten sind das?", "1 Stunde $m Minuten", 60 + m,
                erklaerung = "60 Minuten + $m Minuten = ${60 + m} Minuten",
            )
        }
        2 -> {
            val h = z(2..3)
            Lern.zahl(
                "Wie viele Minuten sind das?", "$h Stunden", 60 * h,
                erklaerung = "$h · 60 Minuten = ${60 * h} Minuten",
            )
        }
        else -> {
            val von = z(7..15)
            val dauer = z(1..5)
            Lern.zahl(
                "Wie viele Stunden sind das?", "Von $von Uhr bis ${von + dauer} Uhr", dauer,
                erklaerung = "Von $von bis ${von + dauer} Uhr sind es $dauer ${if (dauer == 1) "Stunde" else "Stunden"}.",
            )
        }
    }

    // Klasse 3

    fun tausend(): LernAufgabe = when (z(0..3)) {
        0 -> {
            val a = z(1..8) * 100
            val b = z(1..(9 - a / 100)) * 100
            Lern.zahl(RECHNE, "$a + $b =", a + b, erklaerung = "$a + $b = ${a + b}")
        }
        1 -> {
            val a = z(10..80) * 10
            val b = z(1..(99 - a / 10)) * 10
            Lern.zahl(RECHNE, "$a + $b =", a + b, erklaerung = "$a + $b = ${a + b}")
        }
        2 -> {
            val a = z(20..99) * 10
            val b = z(1..(a / 10 - 1)) * 10
            Lern.zahl(RECHNE, "$a - $b =", a - b, erklaerung = "$a - $b = ${a - b}")
        }
        else -> {
            val a = z(101..989)
            val b = z(2..9)
            Lern.zahl(RECHNE, "$a + $b =", a + b, erklaerung = "$a + $b = ${a + b}")
        }
    }

    fun schriftPlus(): LernAufgabe {
        val a = z(100..799)
        val b = z(100..(999 - a))
        return Lern.zahl(
            "Rechne schriftlich.", "$a + $b =", a + b,
            erklaerung = "$a + $b = ${a + b}", hinweis = "Einer, dann Zehner, dann Hunderter.",
        )
    }

    fun schriftMinus(): LernAufgabe {
        val a = z(300..999)
        val b = z(100..(a - 50))
        return Lern.zahl(
            "Rechne schriftlich.", "$a - $b =", a - b,
            erklaerung = "$a - $b = ${a - b}. Probe: ${a - b} + $b = $a",
            hinweis = "Einer, dann Zehner, dann Hunderter.",
        )
    }

    fun rest(): LernAufgabe {
        var t: Int
        var q: Int
        var r: Int
        do {
            t = z(2..9)
            q = z(2..12)
            r = z(1..(t - 1))
        } while (t * q + r > 100)
        val n = t * q + r
        return LernAufgabe(
            art = "rest", frage = "Teile mit Rest. Rechne bis zum Ende.", rechnung = "$n : $t =",
            antwort = q.toString(), antwort2 = r.toString(),
            erklaerung = "$n : $t = $q Rest $r, denn $q · $t = ${q * t} und ${q * t} + $r = $n",
        )
    }

    fun laengen(): LernAufgabe = when (z(0..4)) {
        0 -> {
            val m = z(1..9)
            val cm = z(1..19) * 5
            Lern.zahl(
                "Wie viele Zentimeter sind das?", "$m m $cm cm = ? cm", m * 100 + cm,
                erklaerung = "$m m sind ${m * 100} cm, dazu $cm cm: ${m * 100 + cm} cm",
            )
        }
        1 -> {
            val m = z(2..9)
            Lern.zahl(
                "Wie viele Zentimeter sind das?", "$m m = ? cm", m * 100,
                erklaerung = "1 m sind 100 cm, also $m m = ${m * 100} cm",
            )
        }
        2 -> {
            val m = z(2..9)
            Lern.zahl(
                "Wie viele Meter sind das?", "${m * 100} cm = ? m", m,
                erklaerung = "100 cm sind 1 m, also ${m * 100} cm = $m m",
            )
        }
        3 -> {
            val km = z(2..9)
            Lern.zahl(
                "Wie viele Meter sind das?", "$km km = ? m", km * 1000,
                erklaerung = "1 km sind 1000 m, also $km km = ${km * 1000} m",
            )
        }
        else -> {
            val km = z(1..5)
            val m = z(1..9) * 100
            Lern.zahl(
                "Wie viele Meter sind das?", "$km km $m m = ? m", km * 1000 + m,
                erklaerung = "$km km sind ${km * 1000} m, dazu $m m: ${km * 1000 + m} m",
            )
        }
    }

    fun gewicht(): LernAufgabe = when (z(0..3)) {
        0 -> {
            val kg = z(2..9)
            Lern.zahl(
                "Wie viele Gramm sind das?", "$kg kg = ? g", kg * 1000,
                erklaerung = "1 kg sind 1000 g, also $kg kg = ${kg * 1000} g",
            )
        }
        1 -> {
            val kg = z(1..5)
            val g = z(1..9) * 100
            Lern.zahl(
                "Wie viele Gramm sind das?", "$kg kg $g g = ? g", kg * 1000 + g,
                erklaerung = "$kg kg sind ${kg * 1000} g, dazu $g g: ${kg * 1000 + g} g",
            )
        }
        2 -> {
            val kg = z(2..9)
            Lern.zahl(
                "Wie viele Kilogramm sind das?", "${kg * 1000} g = ? kg", kg,
                erklaerung = "1000 g sind 1 kg, also ${kg * 1000} g = $kg kg",
            )
        }
        else -> {
            val g = z(1..9) * 100
            Lern.zahl(
                "Wie viel Gramm fehlen bis 1 kg?", "$g g + ? g = 1 kg", 1000 - g,
                erklaerung = "1 kg sind 1000 g. $g g + ${1000 - g} g = 1000 g",
            )
        }
    }

    // Klasse 4

    fun million(): LernAufgabe = when (z(0..3)) {
        0 -> {
            val n = z(10..999) * 1000 - 1
            Lern.zahl(
                "Welche Zahl kommt danach?", "${Lern.gross(n)}, ?", n + 1,
                erklaerung = "Nach ${Lern.gross(n)} kommt ${Lern.gross(n + 1)}.",
            )
        }
        1 -> {
            val n = z(10..999) * 1000
            Lern.zahl(
                "Welche Zahl kommt davor?", "?, ${Lern.gross(n)}", n - 1,
                erklaerung = "Vor ${Lern.gross(n)} kommt ${Lern.gross(n - 1)}.",
            )
        }
        2 -> {
            val n = z(100_000..999_999)
            val stelle = Lern.eins(
                listOf("Hunderterstelle" to 100, "Tausenderstelle" to 1000, "Zehntausenderstelle" to 10_000),
            )
            val ziffer = (n / stelle.second) % 10
            Lern.zahl(
                "Welche Ziffer steht an der ${stelle.first}?", Lern.gross(n), ziffer,
                erklaerung = "In ${Lern.gross(n)} steht an der ${stelle.first} eine $ziffer.",
            )
        }
        else -> {
            val n = z(100..9_000) * 100
            val plus = Lern.eins(listOf(1000, 10_000))
            Lern.zahl(
                RECHNE, "${Lern.gross(n)} + ${Lern.gross(plus)} =", n + plus,
                erklaerung = "${Lern.gross(n)} + ${Lern.gross(plus)} = ${Lern.gross(n + plus)}",
            )
        }
    }

    /** Kaufmännisch runden wie in der Schule: ab 5 aufrunden */
    fun gerundet(n: Int, stelle: Int): Int = ((n + stelle / 2) / stelle) * stelle

    fun runden(): LernAufgabe {
        val ziel = Lern.eins(listOf("Zehner" to 10, "Hunderter" to 100, "Tausender" to 1000))
        val n = z(1_001..99_999)
        val g = gerundet(n, ziel.second)
        return Lern.zahl(
            "Runde auf ${ziel.first}.", Lern.gross(n), g,
            erklaerung = "${Lern.gross(n)} wird gerundet zu ${Lern.gross(g)}.",
        )
    }

    fun schriftMal(): LernAufgabe {
        if (Lern.wahr(0.6)) {
            val a = z(101..999)
            val b = z(2..9)
            return Lern.zahl("Rechne schriftlich.", "$a · $b =", a * b, erklaerung = "$a · $b = ${Lern.gross(a * b)}")
        }
        val a = z(12..49)
        val b = z(12..29)
        return Lern.zahl(
            "Rechne schriftlich.", "$a · $b =", a * b,
            erklaerung = "$a · $b = ${a * 10 * (b / 10)} + ${a * (b % 10)} = ${a * b}",
        )
    }

    fun schriftGeteilt(): LernAufgabe {
        val t = z(2..9)
        val q = z(101..999)
        val n = t * q
        return Lern.zahl("Rechne schriftlich.", "$n : $t =", q, erklaerung = "$n : $t = $q. Probe: $q · $t = $n")
    }

    fun uhrzeit(minuten: Int): String {
        val h = (minuten / 60) % 24
        val m = minuten % 60
        return "%d:%02d".format(h, m)
    }

    fun zeitspanne(): LernAufgabe {
        val start = z((7 * 12)..(16 * 12)) * 5
        val dauer = z(4..30) * 5
        return Lern.zahl(
            "Wie viele Minuten sind das?", "Von ${uhrzeit(start)} Uhr bis ${uhrzeit(start + dauer)} Uhr", dauer,
            erklaerung = "Von ${uhrzeit(start)} Uhr bis ${uhrzeit(start + dauer)} Uhr sind es $dauer Minuten.",
        )
    }

    private val bruchListe: List<Triple<String, String, Int>> = listOf(
        Triple("¼ Stunde", "min", 15), Triple("½ Stunde", "min", 30), Triple("¾ Stunde", "min", 45),
        Triple("1 ½ Stunden", "min", 90),
        Triple("½ kg", "g", 500), Triple("¼ kg", "g", 250), Triple("¾ kg", "g", 750), Triple("1 ½ kg", "g", 1500),
        Triple("½ m", "cm", 50), Triple("¼ m", "cm", 25), Triple("¾ m", "cm", 75),
        Triple("½ l", "ml", 500), Triple("¼ l", "ml", 250), Triple("1 ½ l", "ml", 1500),
        Triple("½ km", "m", 500), Triple("¼ km", "m", 250),
    )

    fun brueche(): LernAufgabe {
        val b = Lern.eins(bruchListe)
        return Lern.zahl("Rechne um.", "${b.first} = ? ${b.second}", b.third, erklaerung = "${b.first} sind ${b.third} ${b.second}.")
    }
}
