package de.cemaras.yem1n.logik.lehrplan

import de.cemaras.yem1n.logik.Bundesland
import de.cemaras.yem1n.logik.LehrFach
import de.cemaras.yem1n.logik.Lern
import de.cemaras.yem1n.logik.LernAufgabe
import de.cemaras.yem1n.logik.LernThema
import de.cemaras.yem1n.logik.mitBild
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Lernwelt Mathe, weitere Themen. Port von LehrplanMatheMehr.swift.
// Sachaufgaben, Vergleichen, Verdoppeln, Zahlenmauern, Geometrie, Uhr, Daten und Zufall.
// Bayern (LehrplanPLUS 1/2) nennt Körperformen, Uhrzeit, Kombinieren und sicher, möglich,
// unmöglich schon für Klasse 1 und 2, NRW und Hessen ebenso bis Ende Klasse 2.
// Umfang und Flächeninhalt bestimmen steht in NRW bei Ende Klasse 4.

object LehrplanMatheMehr {
    private val M = LehrFach.MATHE
    private fun z(r: IntRange) = Lern.z(r)

    val themen: List<LernThema> by lazy {
        listOf(
            // Klasse 1
            LernThema(
                "m1-verdoppeln", M, "Verdoppeln und Halbieren", "👯", "Doppelt so viel, halb so viel", 1,
                tipp = "Verdoppeln heißt: die Zahl plus dieselbe Zahl. 6 + 6 = 12.", erzeuge = ::verdoppeln20,
            ),
            LernThema(
                "m1-vergleich", M, "Größer, kleiner, gleich", "🐊", "<, > und = bis 20", 1,
                tipp = "Das Krokodil frisst immer die größere Zahl: 9 > 4.", erzeuge = ::vergleich20,
            ),
            LernThema("m1-geld", M, "Geld bis 20 €", "🪙", "Münzen und Scheine zusammenzählen", 1, erzeuge = ::geld20),
            LernThema("m1-sach", M, "Rechengeschichten", "📖", "Kleine Geschichten zum Rechnen", 1, erzeuge = ::sach20),
            LernThema(
                "m1-strich", M, "Strichlisten", "✏️", "Striche zählen, immer fünf zusammen", 1,
                tipp = "Ein Bündel mit Querstrich sind 5. Zähle erst die Bündel: 5, 10, 15.", erzeuge = ::strichliste,
            ),
            // Klasse 2
            LernThema("m2-mauer", M, "Zahlenmauern", "🧱", "Zwei Steine ergeben den Stein darüber", 2, erzeuge = ::mauer100),
            LernThema(
                "m2-ergaenzen", M, "Ergänzen bis 100", "🧩", "Wie viel fehlt noch?", 2,
                tipp = "Erst bis zum nächsten Zehner, dann bis 100.", erzeuge = ::ergaenzen,
            ),
            LernThema("m2-verdoppeln", M, "Verdoppeln und Halbieren bis 100", "👯", "Mit Zehnern und Einern", 2, erzeuge = ::verdoppeln100),
            LernThema("m2-vergleich", M, "Vergleichen bis 100", "🐊", "Erst rechnen, dann vergleichen", 2, erzeuge = ::vergleich100),
            LernThema(
                "m2-sach", M, "Sachaufgaben bis 100", "🛒", "Lesen, rechnen, antworten", 2,
                tipp = "Lies genau: Was weißt du, was ist gesucht?", erzeuge = ::sach100,
            ),
            LernThema("m2-meter", M, "Meter und Zentimeter", "📏", "1 m sind 100 cm", 2, erzeuge = ::meter),
            LernThema("m2-koerper", M, "Körper", "🎲", "Würfel, Quader, Kugel und mehr", 2, erzeuge = ::koerper),
            LernThema(
                "m2-uhrlesen", M, "Die Uhr lesen", "🕰️", "Volle, halbe und Viertelstunden", 2,
                tipp = "Der kurze Zeiger zeigt die Stunde, der lange die Minuten.", erzeuge = ::uhrLesen,
            ),
            LernThema(
                "m2-kombi", M, "Kombinieren", "👕", "Wie viele Möglichkeiten gibt es?", 2,
                tipp = "Zu jedem T-Shirt passt jede Hose. Also T-Shirts mal Hosen.", erzeuge = ::kombinieren2,
            ),
            LernThema("m2-zufall", M, "Sicher, möglich, unmöglich", "🎲", "Kann das passieren?", 2, erzeuge = ::zufall2),
            // Klasse 3
            LernThema("m3-mauer", M, "Zahlenmauern bis 1000", "🏰", "Große Mauern mit vier Steinen unten", 3, erzeuge = ::mauer1000),
            LernThema(
                "m3-halbschriftlich", M, "Mal mit großen Zahlen", "🧮", "13 · 6 in Schritten rechnen", 3,
                tipp = "Zerlege die große Zahl: 13 · 6 = 10 · 6 + 3 · 6.", erzeuge = ::halbschriftlich,
            ),
            LernThema(
                "m3-geldkomma", M, "Euro mit Komma", "💶", "3,45 € sind 345 ct", 3,
                tipp = "Vor dem Komma stehen die Euro, dahinter die Cent.", erzeuge = ::geldKomma,
            ),
            LernThema(
                "m3-ueberschlag", M, "Überschlagen", "🔭", "Ungefähr wie viel?", 3,
                tipp = "Runde beide Zahlen auf Hunderter und rechne dann.", erzeuge = ::ueberschlag,
            ),
            LernThema(
                "m3-nachbarn", M, "Nachbarzehner und Nachbarhunderter", "🏘️", "Zwischen welchen Zahlen liegt sie?", 3,
                erzeuge = ::nachbarHunderter,
            ),
            LernThema(
                "m3-sach", M, "Sachaufgaben bis 1000", "🚌", "Mit Geld, Seiten und Plätzen", 3,
                tipp = "Schreib dir auf: Was weiß ich? Was ist gesucht?", erzeuge = ::sach1000,
            ),
            LernThema(
                "m3-kaestchen", M, "Fläche und Umfang in Kästchen", "🟨", "Kästchen zählen und Ränder messen", 3,
                abweichend = mapOf(Bundesland.NRW to 4),
                tipp = "Die Fläche sind alle Kästchen. Der Umfang ist der Weg einmal ganz außen herum.",
                erzeuge = ::kaestchen,
            ),
            LernThema("m3-tabelle", M, "Tabellen lesen", "📊", "Daten finden und rechnen", 3, erzeuge = ::tabelle),
            // Klasse 4
            LernThema(
                "m4-flaeche", M, "Umfang und Fläche", "📐", "Rechtecke in cm und cm²", 4,
                tipp = "Fläche: Länge mal Breite. Umfang: alle vier Seiten zusammen.", erzeuge = ::flaeche,
            ),
            LernThema(
                "m4-groessen", M, "Größen umrechnen", "🧪", "mm, km, ml, t, Sekunden", 4,
                tipp = "1 cm = 10 mm, 1 l = 1000 ml, 1 t = 1000 kg, 1 min = 60 s.", erzeuge = ::groessen,
            ),
            LernThema(
                "m4-vergleich", M, "Große Zahlen vergleichen", "🐊", "<, > und = bis 1 Million", 4,
                tipp = "Vergleiche von links: zuerst die höchste Stelle.", erzeuge = ::vergleichGross,
            ),
            LernThema("m4-sach", M, "Sachaufgaben mit großen Zahlen", "✈️", "Auch mit zwei Rechenschritten", 4, erzeuge = ::sachGross),
            LernThema("m4-kombi", M, "Kombinieren und Zählen", "🤝", "Möglichkeiten geschickt zählen", 4, erzeuge = ::kombinieren4),
            LernThema("m4-zufall", M, "Wahrscheinlichkeit", "🍀", "Was ist wahrscheinlicher?", 4, erzeuge = ::zufall4),
        )
    }

    // Namen und Dinge für Geschichten

    /** Name und ob "sie" oder "er" */
    val kinder: List<Pair<String, Boolean>> = listOf(
        "Jule" to true, "Ali" to false, "Mia" to true, "Tom" to false, "Lena" to true, "Ben" to false,
        "Sara" to true, "Emil" to false, "Nele" to true, "Jonas" to false, "Ida" to true, "Luis" to false,
    )

    /** Mehrzahl der Dinge, die gesammelt werden */
    val dinge = listOf("Murmeln", "Sticker", "Bonbons", "Buntstifte", "Karten", "Muscheln", "Kekse", "Perlen")

    class KindName(val name: String, val pron: String, val Pron: String)

    fun kind(): KindName {
        val k = Lern.eins(kinder)
        return KindName(k.first, if (k.second) "sie" else "er", if (k.second) "Sie" else "Er")
    }

    fun zweiKinder(): Pair<String, String> {
        val a = Lern.eins(kinder).first
        var b = Lern.eins(kinder).first
        while (b == a) b = Lern.eins(kinder).first
        return a to b
    }

    private const val LIES = "Lies genau. Rechne und antworte."

    // Klasse 1

    fun verdoppeln20(): LernAufgabe {
        if (Lern.wahr(0.5)) {
            val n = z(1..10)
            return Lern.zahl("Verdopple die Zahl.", "$n", 2 * n, erklaerung = "$n + $n = ${2 * n}")
        }
        val n = z(1..10) * 2
        return Lern.zahl("Halbiere die Zahl.", "$n", n / 2, erklaerung = "${n / 2} + ${n / 2} = $n, also ist die Hälfte ${n / 2}.")
    }

    fun vergleich20(): LernAufgabe {
        if (Lern.wahr(0.5)) {
            val a = z(0..20)
            var b = z(0..20)
            if (Lern.wahr(0.15)) b = a
            return Lern.vergleich(a.toString(), a, b.toString(), b)
        }
        val x = z(1..9)
        val y = z(1..(10 - x))
        val c = z(max(1, x + y - 2)..min(20, x + y + 2))
        return Lern.vergleich("$x + $y", x + y, c.toString(), c)
    }

    fun geld20(): LernAufgabe = when (z(0..2)) {
        0 -> {
            val werte = listOf(1, 2, 5, 10)
            var teile: List<Int>
            do {
                teile = (0 until z(2..3)).map { Lern.eins(werte) }
            } while (teile.sum() > 20)
            val summe = teile.sum()
            val text = teile.joinToString(" + ") { "$it €" }
            Lern.zahl(
                "Wie viel Geld ist das?", "$text = ? €", summe,
                erklaerung = teile.joinToString(" + ") + " = $summe",
            )
        }
        1 -> {
            val k = kind()
            val preis = z(2..18)
            Lern.zahl(
                LIES, "${k.name} hat 20 €. Ein Spiel kostet $preis €. Wie viel Geld bleibt übrig?", 20 - preis,
                erklaerung = "20 - $preis = ${20 - preis}",
            )
        }
        else -> {
            val a = z(2..9)
            val b = z(2..(20 - a))
            Lern.zahl(
                LIES, "Ein Ball kostet $a €, ein Buch $b €. Wie viel kostet beides zusammen?", a + b,
                erklaerung = "$a + $b = ${a + b}",
            )
        }
    }

    fun sach20(): LernAufgabe {
        val k = kind()
        val ding = Lern.eins(dinge)
        return when (z(0..2)) {
            0 -> {
                val a = z(2..12)
                val b = z(2..(20 - a))
                Lern.zahl(
                    LIES, "${k.name} hat $a $ding. ${k.Pron} bekommt $b dazu. Wie viele $ding hat ${k.pron} jetzt?",
                    a + b, erklaerung = "$a + $b = ${a + b}",
                )
            }
            1 -> {
                val a = z(5..20)
                val b = z(2..(a - 2))
                Lern.zahl(
                    LIES, "${k.name} hat $a $ding. ${k.Pron} verschenkt $b. Wie viele $ding hat ${k.pron} noch?",
                    a - b, erklaerung = "$a - $b = ${a - b}",
                )
            }
            else -> {
                val (n1, n2) = zweiKinder()
                val a = z(2..14)
                val b = z((a + 2)..20)
                Lern.zahl(
                    LIES, "$n1 hat $a $ding. $n2 hat $b $ding. Wie viele $ding hat $n2 mehr?",
                    b - a, erklaerung = "$b - $a = ${b - a}",
                )
            }
        }
    }

    fun strichliste(): LernAufgabe {
        val n = z(3..23)
        val ding = Lern.eins(listOf("Vögel", "Autos", "Blumen", "Kinder", "Hunde"))
        return Lern.zahl(
            "Wie viele $ding sind das? Zähle die Striche.", "", n,
            erklaerung = "${n / 5} Bündel mit je 5 und noch ${n % 5}: $n",
        ).mitBild("strichliste:$n")
    }

    // Klasse 2

    /** Eine Zahlenmauer aus den Steinen der untersten Reihe */
    fun mauer(unten: List<Int>): List<List<Int>> {
        val reihen = mutableListOf(unten)
        while (reihen.last().size > 1) {
            val l = reihen.last()
            reihen += l.zipWithNext { a, b -> a + b }
        }
        return reihen
    }

    fun mauer100(): LernAufgabe {
        var r: List<List<Int>>
        do {
            r = mauer(listOf(z(2..30), z(2..30), z(2..30)))
        } while (r.last().first() > 100)
        return LernAufgabe(art = "mauer", frage = "Fülle die Mauer von unten nach oben.", antwort = "", reihen = r)
    }

    fun ergaenzen(): LernAufgabe {
        if (Lern.wahr(0.4)) {
            val n = z(11..89)
            if (n % 10 == 0) return ergaenzen()
            val ziel = (n / 10 + 1) * 10
            return Lern.zahl(
                "Ergänze bis zum nächsten Zehner.", "$n + ? = $ziel", ziel - n,
                erklaerung = "$n + ${ziel - n} = $ziel",
            )
        }
        val n = z(11..95)
        return Lern.zahl("Ergänze bis 100.", "$n + ? = 100", 100 - n, erklaerung = "$n + ${100 - n} = 100")
    }

    fun verdoppeln100(): LernAufgabe {
        if (Lern.wahr(0.5)) {
            val n = z(11..50)
            return Lern.zahl("Verdopple die Zahl.", "$n", 2 * n, erklaerung = "$n + $n = ${2 * n}")
        }
        val n = z(11..50) * 2
        return Lern.zahl(
            "Halbiere die Zahl.", "$n", n / 2,
            erklaerung = "Die Hälfte von $n ist ${n / 2}, denn ${n / 2} + ${n / 2} = $n.",
        )
    }

    fun vergleich100(): LernAufgabe = when (z(0..2)) {
        0 -> {
            val a = z(10..99)
            var b = z(10..99)
            if (Lern.wahr(0.15)) b = a
            Lern.vergleich(a.toString(), a, b.toString(), b)
        }
        1 -> {
            val x = z(10..80)
            val y = z(2..(99 - x))
            val c = z(max(10, x + y - 5)..min(100, x + y + 5))
            Lern.vergleich("$x + $y", x + y, c.toString(), c)
        }
        else -> {
            val a = z(2..10)
            val b = z(2..10)
            val c = z(2..10)
            val d = z(2..10)
            Lern.vergleich("$a · $b", a * b, "$c · $d", c * d)
        }
    }

    fun sach100(): LernAufgabe {
        val k = kind()
        val ding = Lern.eins(dinge)
        return when (z(0..4)) {
            0 -> {
                val a = z(20..70)
                val b = z(5..(99 - a))
                Lern.zahl(
                    LIES, "${k.name} hat $a $ding. ${k.Pron} bekommt $b dazu. Wie viele $ding hat ${k.pron} jetzt?",
                    a + b, erklaerung = "$a + $b = ${a + b}",
                )
            }
            1 -> {
                val je = z(2..10)
                val tueten = z(2..10)
                Lern.zahl(
                    LIES, "In einer Tüte sind $je $ding. ${k.name} kauft $tueten Tüten. Wie viele $ding sind das?",
                    je * tueten, erklaerung = "$tueten · $je = ${je * tueten}",
                )
            }
            2 -> {
                val kinder = z(2..10)
                val je = z(2..10)
                Lern.zahl(
                    LIES, "${k.name} verteilt ${kinder * je} $ding gerecht an $kinder Kinder. Wie viele bekommt jedes Kind?",
                    je, erklaerung = "${kinder * je} : $kinder = $je",
                )
            }
            3 -> {
                val ziel = z(5..10) * 10
                val schon = z(5..(ziel - 3))
                Lern.zahl(
                    LIES, "${k.name} möchte $ziel $ding sammeln. ${k.Pron} hat schon $schon. Wie viele fehlen noch?",
                    ziel - schon, erklaerung = "$ziel - $schon = ${ziel - schon}",
                )
            }
            else -> {
                val a = z(30..99)
                val b = z(5..(a - 5))
                Lern.zahl(
                    LIES, "Im Bus sitzen $a Kinder. An der Schule steigen $b aus. Wie viele Kinder sitzen noch im Bus?",
                    a - b, erklaerung = "$a - $b = ${a - b}",
                )
            }
        }
    }

    fun meter(): LernAufgabe = when (z(0..2)) {
        0 -> {
            val cm = z(1..19) * 5
            Lern.zahl(
                "Wie viele Zentimeter sind das?", "1 m $cm cm = ? cm", 100 + cm,
                erklaerung = "1 m sind 100 cm, dazu $cm cm: ${100 + cm} cm",
            )
        }
        1 -> {
            val cm = z(1..19) * 5
            Lern.zahl(
                "Wie viele Zentimeter fehlen bis 1 m?", "$cm cm + ? cm = 1 m", 100 - cm,
                erklaerung = "1 m sind 100 cm. $cm cm + ${100 - cm} cm = 100 cm",
            )
        }
        else -> {
            val a = z(10..60)
            val b = z(5..(99 - a))
            Lern.zahl("Rechne mit Zentimetern.", "$a cm + $b cm = ? cm", a + b, erklaerung = "$a + $b = ${a + b}")
        }
    }

    val koerperListe: List<Triple<String, String, String>> = listOf(
        Triple("Würfel", "der", "6 gleich große Quadrate als Flächen"),
        Triple("Quader", "der", "6 Rechtecke als Flächen, wie ein Schuhkarton"),
        Triple("Kugel", "die", "ganz rund, ohne Ecken und Kanten"),
        Triple("Zylinder", "der", "2 Kreise als Flächen, wie eine Dose"),
        Triple("Kegel", "der", "unten ein Kreis, oben eine Spitze, wie eine Eiswaffel"),
        Triple("Pyramide", "die", "unten ein Quadrat, dazu 4 Dreiecke, die oben spitz zusammenkommen"),
    )

    fun koerper(): LernAufgabe {
        if (Lern.wahr(0.4)) {
            val f = Lern.eins(
                listOf(
                    "Wie viele Ecken hat ein Würfel?" to 8, "Wie viele Kanten hat ein Würfel?" to 12,
                    "Wie viele Flächen hat ein Würfel?" to 6, "Wie viele Ecken hat ein Quader?" to 8,
                    "Wie viele Flächen hat ein Quader?" to 6, "Wie viele Ecken hat eine Kugel?" to 0,
                    "Wie viele Ecken hat eine Pyramide mit quadratischem Boden?" to 5,
                ),
            )
            return Lern.zahl(f.first, "", f.second, erklaerung = "Richtig sind ${f.second}.")
        }
        val (name, artikel, text) = Lern.eins(koerperListe)
        return Lern.wahl(
            frage = "Welcher Körper ist das?", rechnung = text, richtig = name,
            falsch = koerperListe.map { it.first }, erklaerung = "Das ist $artikel $name.",
        )
    }

    fun zeitText(h: Int, m: Int): String = String.format(Locale.ROOT, "%d:%02d Uhr", h, m)

    fun uhrLesen(): LernAufgabe {
        val h = z(1..12)
        val m = Lern.eins(listOf(0, 15, 30, 45))
        val richtig = zeitText(h, m)
        // Typische Verwechslungen: Zeiger vertauscht, eine Stunde daneben, halb und voll verwechselt
        val naechste = h % 12 + 1
        val vorige = (h + 10) % 12 + 1
        val falsch = mutableListOf(zeitText(naechste, m), zeitText(vorige, m), zeitText(h, (m + 30) % 60))
        if (m == 0) falsch += zeitText(12, h * 5 % 60)
        return Lern.wahl(
            frage = "Wie spät ist es?", richtig = richtig, falsch = falsch, bild = "uhr:$h:$m",
            erklaerung = "Der kurze Zeiger zeigt die Stunde, der lange die Minuten: $richtig.",
        )
    }

    fun kombinieren2(): LernAufgabe {
        val a = z(2..4)
        val b = z(2..4)
        val f = Lern.eins(
            listOf(
                Triple("T-Shirts", "Hosen", "anziehen"), Triple("Sorten Eis", "Waffeln", "aussuchen"),
                Triple("Mützen", "Schals", "anziehen"),
            ),
        )
        return Lern.zahl(
            LIES, "Im Schrank liegen $a ${f.first} und $b ${f.second}. Wie viele verschiedene Paare kannst du ${f.third}?",
            a * b, erklaerung = "Zu jedem der $a passen alle $b: $a · $b = ${a * b}",
        )
    }

    val zufallFaelle2: List<Pair<String, String>> = listOf(
        "Im Beutel sind nur rote Kugeln. Du ziehst eine rote Kugel." to "sicher",
        "Im Beutel sind nur rote Kugeln. Du ziehst eine blaue Kugel." to "unmöglich",
        "Im Beutel sind rote und blaue Kugeln. Du ziehst eine blaue Kugel." to "möglich",
        "Du würfelst mit einem normalen Würfel eine 7." to "unmöglich",
        "Du würfelst mit einem normalen Würfel eine 6." to "möglich",
        "Du würfelst mit einem normalen Würfel eine Zahl kleiner als 7." to "sicher",
        "Nach dem Montag kommt der Dienstag." to "sicher",
        "Morgen regnet es." to "möglich",
        "Eine Münze zeigt Kopf." to "möglich",
        "Ein Hund fliegt allein zum Mond." to "unmöglich",
        "Du ziehst aus einem Beutel mit 5 grünen Kugeln eine grüne." to "sicher",
    )

    fun zufall2(): LernAufgabe {
        val (satz, richtig) = Lern.eins(zufallFaelle2)
        return LernAufgabe(
            art = "wahl", frage = "Ist das sicher, möglich oder unmöglich?", rechnung = satz,
            antwort = richtig, erklaerung = "Das ist $richtig.", optionen = listOf("sicher", "möglich", "unmöglich"),
        )
    }

    // Klasse 3

    fun mauer1000(): LernAufgabe {
        var r: List<List<Int>>
        do {
            r = mauer(listOf(z(5..30) * 5, z(5..30) * 5, z(5..30) * 5, z(5..30) * 5))
        } while (r.last().first() > 999)
        return LernAufgabe(art = "mauer", frage = "Fülle die Mauer von unten nach oben.", antwort = "", reihen = r)
    }

    fun halbschriftlich(): LernAufgabe {
        val a = z(12..29)
        val b = z(3..9)
        val zehner = a / 10 * 10
        val e = a % 10
        return Lern.zahl(
            "Rechne in Schritten.", "$a · $b =", a * b,
            erklaerung = "$zehner · $b = ${zehner * b}, $e · $b = ${e * b}, zusammen ${a * b}",
            hinweis = "$zehner · $b + $e · $b",
        )
    }

    fun komma(cent: Int): String = String.format(Locale.ROOT, "%d,%02d €", cent / 100, cent % 100)

    fun geldKomma(): LernAufgabe = when (z(0..2)) {
        0 -> {
            val c = z(101..999)
            Lern.zahl(
                "Wie viel Cent sind das?", "${komma(c)} = ? ct", c,
                erklaerung = "${komma(c)} sind ${c / 100} € und ${c % 100} ct, also $c ct.",
            )
        }
        1 -> {
            val c = z(5..99)
            Lern.zahl("Wie viel Cent sind das?", "${komma(c)} = ? ct", c, erklaerung = "${komma(c)} sind $c ct.")
        }
        else -> {
            val schein = Lern.eins(listOf(5, 10))
            val preis = z(1..(schein * 20 - 1)) * 5
            val zurueck = schein * 100 - preis
            Lern.zahl(
                LIES, "Du zahlst mit $schein €. Es kostet ${komma(preis)}. Wie viel Cent bekommst du zurück?",
                zurueck, erklaerung = "${schein * 100} ct - $preis ct = $zurueck ct",
            )
        }
    }

    fun ueberschlag(): LernAufgabe {
        val plus = Lern.wahr(0.6)
        val h1 = z(2..6)
        val h2 = z(1..3)
        val a = h1 * 100 + z(-12..12)
        val b = h2 * 100 + z(-12..12)
        val ungefaehr = if (plus) (h1 + h2) * 100 else (h1 - h2) * 100
        if (ungefaehr <= 0) return ueberschlag()
        val r = if (plus) "$a + $b" else "$a - $b"
        val richtig = "etwa $ungefaehr"
        return Lern.wahl(
            frage = "Überschlage. Ungefähr wie viel ist das?", rechnung = r, richtig = richtig,
            falsch = listOf("etwa ${ungefaehr + 100}", "etwa ${max(ungefaehr - 100, 100)}", "etwa ${ungefaehr + 200}"),
            erklaerung = "$a ist etwa ${h1 * 100}, $b ist etwa ${h2 * 100}. Also etwa $ungefaehr. Genau: ${if (plus) a + b else a - b}",
        )
    }

    fun nachbarHunderter(): LernAufgabe {
        val n = z(101..989)
        if (n % 10 == 0) return nachbarHunderter()
        return when (z(0..3)) {
            0 -> Lern.zahl(
                "Welcher Zehner kommt davor?", "?, $n", n / 10 * 10,
                erklaerung = "$n liegt zwischen ${n / 10 * 10} und ${n / 10 * 10 + 10}.",
            )
            1 -> Lern.zahl(
                "Welcher Zehner kommt danach?", "$n, ?", n / 10 * 10 + 10,
                erklaerung = "$n liegt zwischen ${n / 10 * 10} und ${n / 10 * 10 + 10}.",
            )
            2 -> Lern.zahl(
                "Welcher Hunderter kommt davor?", "?, $n", n / 100 * 100,
                erklaerung = "$n liegt zwischen ${n / 100 * 100} und ${n / 100 * 100 + 100}.",
            )
            else -> Lern.zahl(
                "Welcher Hunderter kommt danach?", "$n, ?", n / 100 * 100 + 100,
                erklaerung = "$n liegt zwischen ${n / 100 * 100} und ${n / 100 * 100 + 100}.",
            )
        }
    }

    fun sach1000(): LernAufgabe {
        val k = kind()
        return when (z(0..4)) {
            0 -> {
                val ziel = z(20..90) * 10
                val schon = z(10..(ziel / 10 - 2)) * 10
                Lern.zahl(
                    LIES, "Die Klasse spart für einen Ausflug. Er kostet $ziel €. $schon € sind schon da. Wie viel Euro fehlen noch?",
                    ziel - schon, erklaerung = "$ziel - $schon = ${ziel - schon}",
                )
            }
            1 -> {
                val reihen = z(8..15)
                val je = z(4..6)
                Lern.zahl(
                    LIES, "Ein Bus hat $reihen Reihen mit je $je Plätzen. Wie viele Plätze hat der Bus?",
                    reihen * je, erklaerung = "$reihen · $je = ${reihen * je}",
                )
            }
            2 -> {
                val alle = z(150..480)
                val maedchen = z(60..(alle - 60))
                Lern.zahl(
                    LIES, "In der Schule sind $alle Kinder. $maedchen davon sind Mädchen. Wie viele Jungen sind es?",
                    alle - maedchen, erklaerung = "$alle - $maedchen = ${alle - maedchen}",
                )
            }
            3 -> {
                val seiten = z(120..480)
                val gelesen = z(20..(seiten - 20))
                Lern.zahl(
                    LIES, "Ein Buch hat $seiten Seiten. ${k.name} hat schon $gelesen Seiten gelesen. Wie viele Seiten fehlen noch?",
                    seiten - gelesen, erklaerung = "$seiten - $gelesen = ${seiten - gelesen}",
                )
            }
            else -> {
                val hefte = z(2..5)
                val preis = z(2..4)
                val stift = z(3..9)
                Lern.zahl(
                    LIES, "${k.name} kauft $hefte Hefte für je $preis € und einen Füller für $stift €. Wie viel Euro bezahlt ${k.pron}?",
                    hefte * preis + stift,
                    erklaerung = "$hefte · $preis = ${hefte * preis}, dann ${hefte * preis} + $stift = ${hefte * preis + stift}",
                )
            }
        }
    }

    fun kaestchen(): LernAufgabe {
        val b = z(2..8)
        val h = z(2..5)
        if (Lern.wahr(0.5)) {
            return Lern.zahl(
                "Wie viele Kästchen hat die Fläche?", "", b * h,
                erklaerung = "$h Reihen mit je $b Kästchen: $h · $b = ${b * h}",
            ).mitBild("rechteck:${b}x$h")
        }
        return Lern.zahl(
            "Wie lang ist der Umfang in Kästchenlängen?", "", 2 * (b + h),
            erklaerung = "$b + $h + $b + $h = ${2 * (b + h)}",
        ).mitBild("rechteck:${b}x$h")
    }

    fun tabelle(): LernAufgabe {
        val tiere = listOf("Hund", "Katze", "Pferd", "Hase")
        val zahlen = tiere.map { z(2..12) }
        val klasse = Lern.eins(listOf("3a", "3b", "3c"))
        val text = "Lieblingstiere der Klasse $klasse:\n" + tiere.zip(zahlen).joinToString("\n") { "${it.first}: ${it.second}" }
        return when (z(0..2)) {
            0 -> {
                val i = z(0..3)
                var j = z(0..3)
                while (j == i) j = z(0..3)
                Lern.zahl(
                    "Wie viele Kinder mögen ${tiere[i]} oder ${tiere[j]} am liebsten?", text, zahlen[i] + zahlen[j],
                    erklaerung = "${zahlen[i]} + ${zahlen[j]} = ${zahlen[i] + zahlen[j]}",
                )
            }
            1 -> {
                val summe = zahlen.sum()
                Lern.zahl(
                    "Wie viele Kinder sind es zusammen?", text, summe,
                    erklaerung = zahlen.joinToString(" + ") + " = $summe",
                )
            }
            else -> {
                val gross = zahlen.max()
                val klein = zahlen.min()
                Lern.zahl(
                    "Wie viele Kinder sind es beim beliebtesten Tier mehr als beim am wenigsten beliebten?",
                    text, gross - klein, erklaerung = "$gross - $klein = ${gross - klein}",
                )
            }
        }
    }

    // Klasse 4

    fun flaeche(): LernAufgabe {
        val l = z(3..15)
        val b = z(2..9)
        if (Lern.wahr(0.5)) {
            return Lern.zahl(
                "Wie groß ist die Fläche in cm²?", "Ein Rechteck ist $l cm lang und $b cm breit.", l * b,
                erklaerung = "$l · $b = ${l * b}, also ${l * b} cm²",
            )
        }
        return Lern.zahl(
            "Wie lang ist der Umfang in cm?", "Ein Rechteck ist $l cm lang und $b cm breit.", 2 * (l + b),
            erklaerung = "$l + $b + $l + $b = ${2 * (l + b)}, also ${2 * (l + b)} cm",
        )
    }

    fun groessen(): LernAufgabe = when (z(0..5)) {
        0 -> {
            val cm = z(2..25)
            Lern.zahl("Rechne um.", "$cm cm = ? mm", cm * 10, erklaerung = "1 cm sind 10 mm, also ${cm * 10} mm.")
        }
        1 -> {
            val l = z(2..9)
            Lern.zahl("Rechne um.", "$l l = ? ml", l * 1000, erklaerung = "1 l sind 1000 ml, also ${l * 1000} ml.")
        }
        2 -> {
            val t = z(2..9)
            Lern.zahl("Rechne um.", "$t t = ? kg", t * 1000, erklaerung = "1 t sind 1000 kg, also ${t * 1000} kg.")
        }
        3 -> {
            val minuten = z(2..5)
            Lern.zahl("Rechne um.", "$minuten min = ? s", minuten * 60, erklaerung = "1 min sind 60 s, also ${minuten * 60} s.")
        }
        4 -> {
            val km = z(2..9)
            val m = z(1..9) * 100 + Lern.eins(listOf(0, 50))
            Lern.zahl(
                "Rechne um.", "$km km $m m = ? m", km * 1000 + m,
                erklaerung = "$km km sind ${km * 1000} m, dazu $m m: ${km * 1000 + m} m",
            )
        }
        else -> {
            val kg = z(2..9)
            val g = z(1..19) * 50
            Lern.zahl(
                "Rechne um.", "$kg kg $g g = ? g", kg * 1000 + g,
                erklaerung = "$kg kg sind ${kg * 1000} g, dazu $g g: ${kg * 1000 + g} g",
            )
        }
    }

    fun vergleichGross(): LernAufgabe {
        val a = z(10_000..999_999)
        val b: Int = when (z(0..3)) {
            0 -> a + Lern.eins(listOf(-1, 1)) * Lern.eins(listOf(10, 100, 1000, 9000))
            1 -> {
                // Ziffern vertauscht, das ist die typische Falle
                val ziffern = a.toString().toCharArray()
                val i = z(0..(ziffern.size - 2))
                val t = ziffern[i]
                ziffern[i] = ziffern[i + 1]
                ziffern[i + 1] = t
                String(ziffern).toIntOrNull() ?: a
            }
            2 -> a
            else -> z(10_000..999_999)
        }
        if (b < 10_000 || b > 1_000_000) return vergleichGross()
        return Lern.vergleich(Lern.gross(a), a, Lern.gross(b), b)
    }

    fun sachGross(): LernAufgabe = when (z(0..4)) {
        0 -> {
            val km = z(150..950)
            Lern.zahl(
                LIES, "Ein Flugzeug fliegt $km km hin und $km km zurück. Wie viele km sind das zusammen?",
                2 * km, erklaerung = "$km + $km = ${2 * km}",
            )
        }
        1 -> {
            val a = z(1_200..9_800)
            val b = z(1_200..9_800)
            Lern.zahl(
                LIES, "Am Samstag kommen $a Menschen in den Zoo, am Sonntag $b. Wie viele sind es zusammen?",
                a + b, erklaerung = "$a + $b = ${a + b}",
            )
        }
        2 -> {
            val je = Lern.eins(listOf(6, 8, 12))
            val packungen = z(25..150)
            Lern.zahl(
                LIES, "Eine Bäckerei packt ${je * packungen} Kekse in Packungen mit je $je Keksen. Wie viele Packungen werden voll?",
                packungen, erklaerung = "${je * packungen} : $je = $packungen",
            )
        }
        3 -> {
            val raten = Lern.eins(listOf(3, 4, 5, 6))
            val rate = z(40..180)
            Lern.zahl(
                LIES, "Ein Fahrrad kostet ${raten * rate} €. Die Familie zahlt in $raten gleich großen Raten. Wie viel Euro ist eine Rate?",
                rate, erklaerung = "${raten * rate} : $raten = $rate",
            )
        }
        else -> {
            val karten = z(3..6)
            val preis = z(7..12)
            val popcorn = z(4..9)
            Lern.zahl(
                LIES, "Mama kauft $karten Kinokarten für je $preis € und Popcorn für $popcorn €. Wie viel Euro bezahlt sie?",
                karten * preis + popcorn,
                erklaerung = "$karten · $preis = ${karten * preis}, dann ${karten * preis} + $popcorn = ${karten * preis + popcorn}",
            )
        }
    }

    fun kombinieren4(): LernAufgabe = when (z(0..2)) {
        0 -> {
            val n = z(3..6)
            val schlaege = n * (n - 1) / 2
            Lern.zahl(
                LIES, "$n Kinder geben sich alle einmal die Hand. Wie viele Handschläge sind das?", schlaege,
                erklaerung = "Das erste Kind gibt ${n - 1} Kindern die Hand, das nächste noch ${n - 2} und so weiter: $schlaege",
            )
        }
        1 -> {
            val a = z(3..5)
            val b = z(2..4)
            val c = z(2..3)
            Lern.zahl(
                LIES, "Zum Frühstück gibt es $a Brotsorten, $b Aufstriche und $c Getränke. Wie viele verschiedene Frühstücke mit je einem davon gibt es?",
                a * b * c, erklaerung = "$a · $b · $c = ${a * b * c}",
            )
        }
        else -> {
            val n = Lern.eins(listOf(3, 4))
            val anzahl = if (n == 3) 6 else 12
            val ziffern = if (n == 3) "1, 2 und 3" else "1, 2, 3 und 4"
            Lern.zahl(
                LIES, "Wie viele zweistellige Zahlen kannst du aus den Ziffern $ziffern bilden, wenn keine Ziffer doppelt vorkommt?",
                anzahl, erklaerung = "Für die Zehnerstelle gibt es $n Möglichkeiten, für die Einerstelle noch ${n - 1}: $n · ${n - 1} = $anzahl",
            )
        }
    }

    fun zufall4(): LernAufgabe {
        val rot = z(1..9)
        var blau = z(1..9)
        while (blau == rot) blau = z(1..9)
        if (Lern.wahr(0.6)) {
            val richtig = if (rot > blau) "Rot" else "Blau"
            return LernAufgabe(
                art = "wahl", frage = "Welche Farbe ziehst du wahrscheinlicher?",
                rechnung = "Im Beutel sind $rot rote und $blau blaue Kugeln.", antwort = richtig,
                erklaerung = "Es gibt mehr ${if (richtig == "Rot") "rote" else "blaue"} Kugeln, darum ist $richtig wahrscheinlicher.",
                optionen = listOf("Rot", "Blau"),
            )
        }
        val mehr = if (rot > blau) "rote" else "blaue"
        val weniger = if (rot > blau) "blaue" else "rote"
        val d = abs(rot - blau)
        return Lern.zahl(
            "Wie viele $weniger Kugeln musst du dazulegen, damit beide Farben gleich wahrscheinlich sind?",
            "Im Beutel sind $rot rote und $blau blaue Kugeln.", d,
            erklaerung = "${max(rot, blau)} - ${min(rot, blau)} = $d. Mit $d dazu sind es gleich viele $mehr und $weniger.",
        )
    }
}
