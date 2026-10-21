package de.cemaras.yem1n.logik

import kotlin.random.Random

/** Die Spiele der Vorschule. Die Reihenfolge entspricht der iOS-App. */
enum class VorschulArt(val titel: String, val emoji: String) {
    MIX("Alles gemischt", "🎲"),
    ZAEHLEN("Zählen", "🍎"),
    ZAHLEN("Zahlen", "🔢"),
    PLUS("Plus", "➕"),
    MEHR("Mehr oder weniger", "⚖️"),
    NACHBAR("Davor und danach", "↔️"),
    FORMEN("Formen und Farben", "🎨"),
    ANLAUT("Anfangsbuchstaben", "🔤"),
    ANDERS("Was passt nicht?", "🧐"),
    MUSTER("Muster", "🧩"),
    MEMORY("Memory", "🃏"),
    WUERFEL("Würfelbilder", "🎯"),
    MINUS("Wegnehmen", "🎈"),
    ZAHLBILD("Zahl und Menge", "🔟"),
    TEILEN("Gerecht teilen", "🍪"),
    BUCHSTABE("Buchstaben finden", "🅰️"),
    REIME("Reime", "🐭"),
    SILBEN("Silben klatschen", "👏"),
    LAGE("Links, Mitte, rechts", "👈"),
    GEFUEHLE("Gefühle", "😊"),
    REIHENFOLGE("Was kommt danach?", "🐣"),
    TIERWELT("Wo lebt das Tier?", "🌍"),
    VERKEHR("Sicher unterwegs", "🚦"),
    UNTERSCHIED("Finde das andere Bild", "🔎");

    /** Name für Fortschritt und Ergebnisse, wie bei iOS "🍎 Zählen". */
    val uebungName: String get() = "$emoji $titel"

    companion object {
        val rechnen = listOf(ZAEHLEN, WUERFEL, ZAHLEN, ZAHLBILD, PLUS, MINUS, MEHR, NACHBAR, TEILEN)
        val sprache = listOf(ANLAUT, BUCHSTABE, REIME, SILBEN)
        val spielen = listOf(FORMEN, LAGE, MUSTER, UNTERSCHIED, ANDERS, REIHENFOLGE, GEFUEHLE, TIERWELT, VERKEHR, MEMORY, MIX)

        /** Alle Stationen für den Schulstart-Pass: jedes Spiel außer "Alles gemischt". */
        val stationen: List<VorschulArt> get() = entries.filter { it != MIX }
    }
}

/** Eine Frage auf Deutsch. Türkisch und Englisch sind für später vorbereitet. */
data class VorschulText(val de: String, val tr: String? = null, val en: String? = null)

/**
 * Eine Frage der Vorschule. [zeige] und [zeigeZwei] sind Bilder zum Zählen, [gross] ein großes Zeichen,
 * [folge] eine Reihe mit Fragezeichen am Ende. Bei [gruppenOptionen] sind die Antworten kleine Bildgruppen.
 */
data class VorschulFrage(
    val text: VorschulText,
    val optionen: List<String>,
    val richtig: Int,
    val zeige: List<String> = emptyList(),
    val zeigeZwei: List<String> = emptyList(),
    val plus: Boolean = false,
    val gross: String = "",
    val folge: List<String> = emptyList(),
    val gruppenOptionen: Boolean = false,
) {
    val schluessel: String
        get() = text.de + "|" + zeige.joinToString("") + "|" + zeigeZwei.joinToString("") + "|" + gross + "|" +
            folge.joinToString("") + "|" + optionen.joinToString(",")
}

/** Der Zustand einer Runde in der Vorschule. Unveränderlich wie [RundenStand]. */
data class VorschulStand(
    val fragen: List<VorschulFrage>,
    val nr: Int = 0,
    /** Richtig beim ersten Versuch. */
    val perfekt: Int = 0,
    /** Richtig beim ersten Versuch, so oft hintereinander. */
    val serie: Int = 0,
    val falsch: Set<Int> = emptySet(),
    val richtigGewaehlt: Int? = null,
) {
    val fertig: Boolean get() = nr >= fragen.size
    val aktuelle: VorschulFrage? get() = fragen.getOrNull(nr)
    val sterne: Int get() = Pruefer.sterneFuer(perfekt, fragen.size)

    /** Das Kind tippt auf Antwort [i]. Schon Gewähltes und Tippen nach dem Treffer zählen nicht. */
    fun wahl(i: Int): VorschulStand {
        val f = aktuelle ?: return this
        if (richtigGewaehlt != null || i in falsch || i !in f.optionen.indices) return this
        return if (i == f.richtig) {
            val ersterVersuch = falsch.isEmpty()
            copy(
                richtigGewaehlt = i,
                perfekt = if (ersterVersuch) perfekt + 1 else perfekt,
                serie = if (ersterVersuch) serie + 1 else 0,
            )
        } else {
            copy(falsch = falsch + i, serie = 0)
        }
    }

    /** True, wenn die letzte Wahl richtig war und ohne Fehlversuch davor. */
    val ersterVersuch: Boolean get() = richtigGewaehlt != null && falsch.isEmpty()

    fun weiter(): VorschulStand =
        if (richtigGewaehlt == null) this else copy(nr = nr + 1, falsch = emptySet(), richtigGewaehlt = null)

    /** Für Fortschritt und Münzen: gleich richtig zählt als richtig, sonst als falsch. */
    fun staende(): List<Stand> = List(fragen.size) { if (it < perfekt) Stand.RICHTIG else Stand.FALSCH }
}

object VorschulGenerator {
    val dinge = listOf("🍎", "⭐", "🐶", "🎈", "🚗", "🐟", "🌸", "🍪", "🐱", "🍓")

    private val formen = listOf(
        VorschulText("Wo ist der Kreis?", "Daire hangisi?", "Where is the circle?") to "🔵",
        VorschulText("Wo ist das Quadrat?", "Kare hangisi?", "Where is the square?") to "🟩",
        VorschulText("Wo ist das Dreieck?", "Üçgen hangisi?", "Where is the triangle?") to "🔺",
        VorschulText("Wo ist der Stern?", "Yıldız hangisi?", "Where is the star?") to "⭐",
        VorschulText("Wo ist das Herz?", "Kalp hangisi?", "Where is the heart?") to "💜",
    )

    private val farben = listOf(
        VorschulText("Tippe auf Rot.", "Kırmızı olanı bul.", "Tap the red one.") to "🔴",
        VorschulText("Tippe auf Blau.", "Mavi olanı bul.", "Tap the blue one.") to "🔵",
        VorschulText("Tippe auf Gelb.", "Sarı olanı bul.", "Tap the yellow one.") to "🟡",
        VorschulText("Tippe auf Grün.", "Yeşil olanı bul.", "Tap the green one.") to "🟢",
        VorschulText("Tippe auf Lila.", "Mor olanı bul.", "Tap the purple one.") to "🟣",
        VorschulText("Tippe auf Orange.", "Turuncu olanı bul.", "Tap the orange one.") to "🟠",
    )

    /** Bild, Wort, Anfangsbuchstabe. */
    val anlaute = listOf(
        Triple("🍎", "Apfel", "A"), Triple("🚗", "Auto", "A"), Triple("🐻", "Bär", "B"), Triple("🍌", "Banane", "B"),
        Triple("🎈", "Ballon", "B"), Triple("🪁", "Drachen", "D"), Triple("🐘", "Elefant", "E"), Triple("🦆", "Ente", "E"),
        Triple("🐟", "Fisch", "F"), Triple("🐸", "Frosch", "F"), Triple("🐶", "Hund", "H"), Triple("🏠", "Haus", "H"),
        Triple("🦔", "Igel", "I"), Triple("🐱", "Katze", "K"), Triple("🐄", "Kuh", "K"), Triple("🦁", "Löwe", "L"),
        Triple("🐭", "Maus", "M"), Triple("🌙", "Mond", "M"), Triple("👃", "Nase", "N"), Triple("🍊", "Orange", "O"),
        Triple("🐴", "Pferd", "P"), Triple("🐧", "Pinguin", "P"), Triple("🌹", "Rose", "R"), Triple("🌈", "Regenbogen", "R"),
        Triple("🌞", "Sonne", "S"), Triple("🧦", "Socke", "S"), Triple("🐯", "Tiger", "T"), Triple("🍅", "Tomate", "T"),
        Triple("⏰", "Uhr", "U"), Triple("🐳", "Wal", "W"), Triple("☁️", "Wolke", "W"), Triple("🦓", "Zebra", "Z"),
        Triple("🚂", "Zug", "Z"),
    )

    private val gruppen = listOf(
        listOf("🐶", "🐱", "🐭", "🐰", "🐻", "🦁"),
        listOf("🍎", "🍌", "🍓", "🍇", "🍊", "🍐"),
        listOf("🚗", "🚌", "🚲", "✈️", "🚂", "🚢"),
        listOf("🌹", "🌻", "🌷", "🌼"),
        listOf("⚽", "🏀", "🎾", "🏐"),
    )

    /** Größte Zahl je Zahlenraum: Stufe 1 bis 5, Stufe 2 bis 10, Stufe 3 bis 20. */
    fun grenze(stufe: Int) = when (stufe) { 1 -> 5; 2 -> 10; else -> 20 }

    /** Drei verschiedene Zahlen, eine davon ist richtig. Gibt die Texte und die Stelle der richtigen zurück. */
    fun zahlenOptionen(richtig: Int, von: Int, bis: Int, zufall: Random): Pair<List<String>, Int> {
        val menge = linkedSetOf(richtig)
        var versuche = 0
        while (menge.size < 3 && versuche < 200) {
            versuche += 1
            val abstand = zufall.nextInt(1, 4) * (if (zufall.nextBoolean()) 1 else -1)
            val z = richtig + abstand
            if (z in von..bis) menge.add(z)
        }
        if (menge.size < 3) {
            for (z in von..bis) if (menge.size < 3) menge.add(z)
        }
        val liste = menge.toList().shuffled(zufall)
        return liste.map { it.toString() } to liste.indexOf(richtig).coerceAtLeast(0)
    }

    /** Bildgruppe als Text, nach fünf Stück ein Zeilenumbruch. */
    fun gruppe(n: Int, emoji: String): String {
        val sb = StringBuilder()
        for (i in 0 until n) {
            sb.append(emoji)
            if ((i + 1) % 5 == 0 && i + 1 < n) sb.append("\n")
        }
        return sb.toString()
    }

    /** Optionen mischen und die richtige finden. */
    fun gemischt(richtig: String, andere: List<String>, zufall: Random, anzahl: Int = 3): Pair<List<String>, Int> {
        val liste = mutableListOf(richtig)
        for (a in andere.shuffled(zufall)) {
            if (a != richtig && a !in liste && liste.size < anzahl) liste.add(a)
        }
        liste.shuffle(zufall)
        return liste to liste.indexOf(richtig).coerceAtLeast(0)
    }

    fun fragen(art: VorschulArt, stufe: Int, anzahl: Int = 8, zufall: Random = Random.Default): List<VorschulFrage> {
        val arten = VorschulArt.entries.filter { it != VorschulArt.MIX && it != VorschulArt.MEMORY }
        val liste = mutableListOf<VorschulFrage>()
        val gesehen = mutableSetOf<String>()
        repeat(anzahl) {
            fun neu() = eine(if (art == VorschulArt.MIX) arten.random(zufall) else art, stufe, zufall)
            var f = neu()
            var versuche = 0
            while (f.schluessel in gesehen && versuche < 10) {
                versuche += 1
                f = neu()
            }
            gesehen.add(f.schluessel)
            liste.add(f)
        }
        return liste
    }

    fun eine(art: VorschulArt, stufe: Int, zufall: Random = Random.Default): VorschulFrage = when (art) {
        VorschulArt.ZAEHLEN -> {
            val m = if (stufe == 1) 5 else 10
            val n = zufall.nextInt(1, m + 1)
            val e = dinge.random(zufall)
            val (o, idx) = zahlenOptionen(n, 1, m, zufall)
            VorschulFrage(
                VorschulText("Wie viele siehst du?", "Kaç tane görüyorsun?", "How many can you see?"),
                o, idx, zeige = List(n) { e },
            )
        }
        VorschulArt.ZAHLEN -> {
            val m = grenze(stufe)
            val n = zufall.nextInt(1, m + 1)
            val (o, idx) = zahlenOptionen(n, 1, m, zufall)
            VorschulFrage(VorschulText("Tippe auf die $n.", "$n sayısına dokun.", "Tap the $n."), o, idx)
        }
        VorschulArt.PLUS -> {
            val m = grenze(stufe)
            val x: Int
            val y: Int
            if (stufe == 3) {
                x = zufall.nextInt(2, 11)
                y = zufall.nextInt(1, minOf(10, m - x) + 1)
            } else {
                x = zufall.nextInt(1, m)
                y = zufall.nextInt(1, m - x + 1)
            }
            val paar = dinge.shuffled(zufall)
            val (o, idx) = zahlenOptionen(x + y, 1, m, zufall)
            VorschulFrage(
                VorschulText("$x plus $y. Wie viel ist das?", "$x artı $y kaç eder?", "$x plus $y. How many is that?"),
                o, idx, zeige = List(x) { paar[0] }, zeigeZwei = List(y) { paar[1] }, plus = true,
            )
        }
        VorschulArt.MEHR -> {
            val m = if (stufe == 1) 5 else 10
            val x = zufall.nextInt(1, m + 1)
            var y = zufall.nextInt(1, m + 1)
            while (y == x) y = zufall.nextInt(1, m + 1)
            val e = dinge.random(zufall)
            val fragtMehr = zufall.nextInt(10) < 7
            val tausch = zufall.nextBoolean()
            val a = if (tausch) y else x
            val b = if (tausch) x else y
            val idx = if (fragtMehr) (if (a > b) 0 else 1) else (if (a < b) 0 else 1)
            val text = if (fragtMehr) VorschulText("Wo sind mehr?", "Hangisi daha fazla?", "Which side has more?")
            else VorschulText("Wo sind weniger?", "Hangisi daha az?", "Which side has fewer?")
            VorschulFrage(text, listOf(gruppe(a, e), gruppe(b, e)), idx, gruppenOptionen = true)
        }
        VorschulArt.NACHBAR -> {
            val m = grenze(stufe)
            val danach = zufall.nextBoolean()
            val n = if (danach) zufall.nextInt(1, m) else zufall.nextInt(2, m + 1)
            val ziel = if (danach) n + 1 else n - 1
            val (o, idx) = zahlenOptionen(ziel, 0, m, zufall)
            val text = if (danach) {
                VorschulText("Welche Zahl kommt nach der $n?", "$n sayısından sonra hangi sayı gelir?", "Which number comes after $n?")
            } else {
                VorschulText("Welche Zahl kommt vor der $n?", "$n sayısından önce hangi sayı gelir?", "Which number comes before $n?")
            }
            VorschulFrage(text, o, idx, gross = n.toString())
        }
        VorschulArt.FORMEN -> {
            val quelle = if (zufall.nextBoolean()) formen else farben
            val drei = quelle.shuffled(zufall).take(3)
            val r = zufall.nextInt(3)
            VorschulFrage(drei[r].first, drei.map { it.second }, r)
        }
        VorschulArt.ANLAUT -> {
            val item = anlaute.random(zufall)
            val buchstaben = anlaute.map { it.third }.distinct().filter { it != item.third }.shuffled(zufall)
            val optionen = (buchstaben.take(2) + item.third).shuffled(zufall)
            VorschulFrage(VorschulText("Womit fängt ${item.second} an?"), optionen, optionen.indexOf(item.third), gross = item.first)
        }
        VorschulArt.ANDERS -> {
            val g = gruppen.shuffled(zufall)
            val eigene = g[0].shuffled(zufall).take(3)
            val fremd = g[1].random(zufall)
            val optionen = (eigene + fremd).shuffled(zufall)
            VorschulFrage(
                VorschulText("Was passt nicht dazu?", "Hangisi diğerlerine uymuyor?", "Which one does not belong?"),
                optionen, optionen.indexOf(fremd),
            )
        }
        VorschulArt.WUERFEL -> wuerfel(zufall)
        VorschulArt.MINUS -> minus(stufe, zufall)
        VorschulArt.ZAHLBILD -> zahlbild(stufe, zufall)
        VorschulArt.TEILEN -> teilen(stufe, zufall)
        VorschulArt.BUCHSTABE -> buchstabe(zufall)
        VorschulArt.REIME -> reim(zufall)
        VorschulArt.SILBEN -> silben(zufall)
        VorschulArt.LAGE -> lage(zufall)
        VorschulArt.GEFUEHLE -> gefuehl(zufall)
        VorschulArt.REIHENFOLGE -> reihenfolge(zufall)
        VorschulArt.TIERWELT -> tierwelt(zufall)
        VorschulArt.VERKEHR -> verkehr(zufall)
        VorschulArt.UNTERSCHIED -> unterschied(zufall)
        // Muster und das Mischen: Muster hängen nicht vom Zahlenraum ab, die Art wechselt zufällig
        VorschulArt.MUSTER, VorschulArt.MEMORY, VorschulArt.MIX -> muster(zufall)
    }

    private fun muster(zufall: Random): VorschulFrage {
        val p = listOf("🔴", "🔵", "🟡", "🟢", "🟣", "🟠", "⭐", "❤️", "🍎", "🐟").shuffled(zufall)
        val (a, b, c) = Triple(p[0], p[1], p[2])
        val m = when (zufall.nextInt(1, 5)) {
            1 -> listOf(a, b)
            2 -> listOf(a, a, b)
            3 -> listOf(a, b, b)
            else -> listOf(a, b, c)
        }
        val l = m.size
        val anzahl = zufall.nextInt(l + 1, 2 * l + 2)
        val folge = List(anzahl) { m[it % l] }
        val richtig = m[anzahl % l]
        val optionen = mutableListOf(richtig)
        for (e in p) if (optionen.size < 3 && e != richtig) optionen.add(e)
        optionen.shuffle(zufall)
        return VorschulFrage(VorschulText("Wie geht es weiter?", "Sıradaki hangisi?", "What comes next?"), optionen, optionen.indexOf(richtig), folge = folge)
    }

    // Zahlen

    private val wuerfelBilder = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

    private fun wuerfel(zufall: Random): VorschulFrage {
        val n = zufall.nextInt(1, 7)
        val (o, idx) = zahlenOptionen(n, 1, 6, zufall)
        return VorschulFrage(
            VorschulText("Wie viele Punkte hat der Würfel?", "Zarda kaç nokta var?", "How many dots are on the dice?"),
            o, idx, gross = wuerfelBilder[n - 1],
        )
    }

    private fun minus(stufe: Int, zufall: Random): VorschulFrage {
        val m = if (stufe == 1) 5 else 10
        val n = zufall.nextInt(2, m + 1)
        val weg = zufall.nextInt(1, n)
        val e = listOf("🎈", "🐦", "🦋", "🐝").random(zufall)
        val (o, idx) = zahlenOptionen(n - weg, 0, m, zufall)
        val wegDe = if (weg == 1) "Einer ist" else "$weg sind"
        val wegEn = if (weg == 1) "One" else "$weg"
        return VorschulFrage(
            VorschulText(
                "Es waren $n. $wegDe weggeflogen. Wie viele sind noch da?",
                "$n tane vardı. $weg tanesi uçtu. Kaç tane kaldı?",
                "There were $n. $wegEn flew away. How many are left?",
            ),
            o, idx, zeige = List(n - weg) { e } + List(weg) { "💨" },
        )
    }

    private fun zahlbild(stufe: Int, zufall: Random): VorschulFrage {
        val m = if (stufe == 1) 5 else 10
        val n = zufall.nextInt(1, m + 1)
        val e = dinge.random(zufall)
        val (o, idx) = zahlenOptionen(n, 1, m, zufall)
        return VorschulFrage(
            VorschulText("Wo sind es $n?", "Hangisinde $n tane var?", "Which one has $n?"),
            o.map { gruppe(it.toInt(), e) }, idx, gross = n.toString(), gruppenOptionen = true,
        )
    }

    private fun teilen(stufe: Int, zufall: Random): VorschulFrage {
        val kinder = zufall.nextInt(2, 4)
        val je = zufall.nextInt(1, (if (stufe == 3) 6 else if (stufe == 1) 2 else 4) + 1)
        val e = listOf("🍪", "🍓", "🍬", "🍎").random(zufall)
        val (o, idx) = zahlenOptionen(je, 1, 10, zufall)
        return VorschulFrage(
            VorschulText(
                "Teile gerecht auf $kinder Kinder auf. Wie viele bekommt jedes Kind?",
                "$kinder çocuğa eşit paylaştır. Her çocuk kaç tane alır?",
                "Share fairly between $kinder children. How many does each child get?",
            ),
            o, idx, zeige = List(kinder * je) { e },
        )
    }

    // Sprache und Laute

    val buchstaben = listOf("A", "B", "D", "E", "F", "H", "I", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "Z")

    private fun buchstabe(zufall: Random): VorschulFrage {
        val b = buchstaben.random(zufall)
        val (o, idx) = gemischt(b, buchstaben, zufall)
        return VorschulFrage(VorschulText("Tippe auf das $b.", "$b harfine dokun.", "Tap the letter $b."), o, idx)
    }

    /** Bild und Wort, das zweite reimt sich auf das erste. */
    val reimPaare: List<Pair<Pair<String, String>, Pair<String, String>>> = listOf(
        ("🐭" to "Maus") to ("🏠" to "Haus"), ("🌹" to "Rose") to ("👖" to "Hose"), ("🐰" to "Hase") to ("👃" to "Nase"),
        ("🦵" to "Bein") to ("🐷" to "Schwein"), ("🫖" to "Kanne") to ("🌲" to "Tanne"), ("🐶" to "Hund") to ("👄" to "Mund"),
        ("🐱" to "Katze") to ("🐾" to "Tatze"), ("👜" to "Tasche") to ("🍾" to "Flasche"), ("✋" to "Hand") to ("🏖️" to "Sand"),
    )

    private fun reim(zufall: Random): VorschulFrage {
        val p = reimPaare.random(zufall)
        val tausch = zufall.nextBoolean()
        val frage = if (tausch) p.second else p.first
        val antwort = if (tausch) p.first else p.second
        val andere = reimPaare.filter { it.first.second != p.first.second }.flatMap { listOf(it.first.first, it.second.first) }
        val (o, idx) = gemischt(antwort.first, andere, zufall)
        return VorschulFrage(VorschulText("Was reimt sich auf ${frage.second}?"), o, idx, gross = frage.first)
    }

    /** Bild, Wort, Anzahl der Silben. */
    val silbenBilder = listOf(
        Triple("🍌", "Banane", 3), Triple("🍎", "Apfel", 2), Triple("⚽", "Ball", 1), Triple("🐘", "Elefant", 3),
        Triple("🚗", "Auto", 2), Triple("🐶", "Hund", 1), Triple("🍅", "Tomate", 3), Triple("🐱", "Katze", 2),
        Triple("🍫", "Schokolade", 4), Triple("🦒", "Giraffe", 3), Triple("🐟", "Fisch", 1), Triple("🚀", "Rakete", 3),
        Triple("☀️", "Sonne", 2), Triple("🌙", "Mond", 1), Triple("🐊", "Krokodil", 3), Triple("🌸", "Blume", 2),
    )

    private fun silben(zufall: Random): VorschulFrage {
        val (bild, wort, n) = silbenBilder.random(zufall)
        return VorschulFrage(VorschulText("Wie viele Silben hat $wort? Klatsche mit!"), listOf("1", "2", "3", "4"), n - 1, gross = bild)
    }

    // Entdecken

    private val tierReihe = listOf("🐶", "🐱", "🐰", "🐻", "🦊", "🐸", "🐼", "🐵")

    private fun lage(zufall: Random): VorschulFrage {
        val drei = tierReihe.shuffled(zufall).take(3)
        val wahl = zufall.nextInt(3)
        val texte = listOf(
            VorschulText("Wer ist ganz links?", "En solda kim var?", "Who is on the far left?"),
            VorschulText("Wer ist in der Mitte?", "Ortada kim var?", "Who is in the middle?"),
            VorschulText("Wer ist ganz rechts?", "En sağda kim var?", "Who is on the far right?"),
        )
        val (o, idx) = gemischt(drei[wahl], drei, zufall)
        return VorschulFrage(texte[wahl], o, idx, zeige = drei)
    }

    /** Bild, deutsches, türkisches und englisches Wort. */
    val gefuehle = listOf(
        listOf("😀", "fröhlich", "mutlu", "happy"), listOf("😢", "traurig", "üzgün", "sad"),
        listOf("😠", "wütend", "kızgın", "angry"), listOf("😨", "ängstlich", "korkmuş", "scared"),
        listOf("😲", "überrascht", "şaşırmış", "surprised"), listOf("😴", "müde", "uykulu", "tired"),
    )

    private fun gefuehl(zufall: Random): VorschulFrage {
        val g = gefuehle.random(zufall)
        val (o, idx) = gemischt(g[0], gefuehle.map { it[0] }, zufall)
        return VorschulFrage(VorschulText("Wer ist ${g[1]}?", "Hangisi ${g[2]}?", "Who is ${g[3]}?"), o, idx)
    }

    /** Was davor passiert, was als Nächstes kommt. */
    val ablaeufe: List<Pair<List<String>, String>> = listOf(
        listOf("🥚", "🐣") to "🐔", listOf("🐛") to "🦋", listOf("🌱", "🌿") to "🌻", listOf("🌅", "☀️") to "🌙",
        listOf("❄️", "🌷") to "☀️", listOf("🌷", "☀️") to "🍂", listOf("☀️", "🍂") to "❄️", listOf("🍂", "❄️") to "🌷",
        listOf("👶", "🧒") to "🧑", listOf("🌧️") to "🌈", listOf("🧊") to "💧",
    )

    private fun reihenfolge(zufall: Random): VorschulFrage {
        val a = ablaeufe.random(zufall)
        val (o, idx) = gemischt(a.second, ablaeufe.map { it.second }, zufall)
        return VorschulFrage(VorschulText("Was kommt als Nächstes?", "Sırada ne var?", "What happens next?"), o, idx, folge = a.first)
    }

    /** Tier und wo es lebt. */
    val lebensraeume = listOf(
        "🐟" to "🌊", "🐬" to "🌊", "🐙" to "🌊", "🦉" to "🌳", "🦊" to "🌳", "🐿️" to "🌳",
        "🐪" to "🏜️", "🦂" to "🏜️", "🐧" to "🧊", "🐻‍❄️" to "🧊", "🐄" to "🏡", "🐖" to "🏡", "🐑" to "🏡",
    )

    private fun tierwelt(zufall: Random): VorschulFrage {
        val (tier, ort) = lebensraeume.random(zufall)
        val (o, idx) = gemischt(ort, listOf("🌊", "🌳", "🏜️", "🧊", "🏡"), zufall)
        return VorschulFrage(VorschulText("Wo lebt dieses Tier?", "Bu hayvan nerede yaşar?", "Where does this animal live?"), o, idx, gross = tier)
    }

    private val verkehrsFragen: List<Triple<VorschulText, String, List<String>>> = listOf(
        Triple(VorschulText("Bei welcher Ampelfarbe darfst du gehen?", "Hangi ışıkta karşıya geçebilirsin?", "Which light means you may cross?"), "🟢", listOf("🔴", "🟡")),
        Triple(VorschulText("Bei welcher Ampelfarbe bleibst du stehen?", "Hangi ışıkta durursun?", "Which light means you stop?"), "🔴", listOf("🟢", "🔵")),
        Triple(VorschulText("Was machst du, bevor du über die Straße gehst?", "Karşıya geçmeden önce ne yaparsın?", "What do you do before you cross the road?"), "👀", listOf("🏃", "🙈")),
        Triple(VorschulText("Was setzt du beim Fahrradfahren auf den Kopf?", "Bisiklete binerken başına ne takarsın?", "What do you wear on your head on a bike?"), "⛑️", listOf("🎩", "👑")),
        Triple(VorschulText("Was macht dich im Dunkeln gut sichtbar?", "Karanlıkta seni ne görünür yapar?", "What helps others see you in the dark?"), "🦺", listOf("🕶️", "🧢")),
        Triple(VorschulText("Wo gehst du sicher über die Straße?", "Karşıya nerede güvenle geçersin?", "Where can you cross the road safely?"), "🚸", listOf("🌳", "🚧")),
    )

    private fun verkehr(zufall: Random): VorschulFrage {
        val (text, richtig, falsch) = verkehrsFragen.random(zufall)
        val (o, idx) = gemischt(richtig, falsch, zufall)
        return VorschulFrage(text, o, idx)
    }

    /** Zwei Bilder, die sich ähnlich sehen. */
    val aehnlich = listOf(
        "🐶" to "🐕", "🐱" to "🐈", "🙂" to "🙃", "⭐" to "🌟", "🍎" to "🍏", "🚗" to "🚙",
        "🐟" to "🐠", "❤️" to "🧡", "🔵" to "🟦", "🌲" to "🌳", "😀" to "😃", "🐭" to "🐹",
    )

    private fun unterschied(zufall: Random): VorschulFrage {
        val (gleich, anders) = aehnlich.random(zufall)
        val bilder = MutableList(3) { gleich }
        val stelle = zufall.nextInt(4)
        bilder.add(stelle, anders)
        return VorschulFrage(VorschulText("Welches Bild ist anders?", "Hangi resim farklı?", "Which picture is different?"), bilder, stelle)
    }
}

/** Ein Kärtchen im Memory. */
data class MemoryKarte(val paar: Int, val emoji: String, val offen: Boolean = false, val weg: Boolean = false)

/** Memory mit vier Paaren, unabhängig vom Zahlenraum. */
data class MemoryStand(
    val karten: List<MemoryKarte>,
    val erste: Int? = null,
    val zuege: Int = 0,
    /** Zwei verschiedene Karten sind offen und müssen erst zugedeckt werden. */
    val sperre: Boolean = false,
) {
    val fertig: Boolean get() = karten.isNotEmpty() && karten.all { it.weg }
    val paare: Int get() = karten.size / 2

    /** Für Sterne und Münzen: alle Paare richtig, mehr Züge als Paare zählen als Fehlversuche. */
    val gesamt: Int get() = maxOf(paare, zuege)

    fun tippe(i: Int): MemoryStand {
        if (sperre || i !in karten.indices || karten[i].offen || karten[i].weg) return this
        val offen = karten.mapIndexed { k, x -> if (k == i) x.copy(offen = true) else x }
        val e = erste ?: return copy(karten = offen, erste = i)
        return if (offen[e].paar == offen[i].paar) {
            copy(
                karten = offen.mapIndexed { k, x -> if (k == e || k == i) x.copy(weg = true, offen = false) else x },
                erste = null, zuege = zuege + 1,
            )
        } else {
            copy(karten = offen, erste = null, zuege = zuege + 1, sperre = true)
        }
    }

    /** Deckt zwei nicht passende Karten wieder zu. */
    fun zudecken(): MemoryStand =
        copy(karten = karten.map { if (it.offen && !it.weg) it.copy(offen = false) else it }, sperre = false)

    companion object {
        const val PAARE = 4

        fun neu(zufall: Random = Random.Default): MemoryStand {
            val pool = (VorschulGenerator.dinge + listOf("🐰", "🦁", "🐸", "🍌", "🚌", "🌻")).shuffled(zufall)
            val liste = mutableListOf<MemoryKarte>()
            pool.take(PAARE).forEachIndexed { i, e ->
                liste.add(MemoryKarte(i, e))
                liste.add(MemoryKarte(i, e))
            }
            return MemoryStand(liste.shuffled(zufall))
        }
    }
}
