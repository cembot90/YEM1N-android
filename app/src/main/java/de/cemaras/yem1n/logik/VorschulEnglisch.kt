package de.cemaras.yem1n.logik

import kotlin.random.Random

/**
 * Englisch für Kleine: so, wie es in der Kita läuft, hören und antippen, ohne Lesen.
 * Das Bild ist die Bedeutung, die Stimme sagt das englische Wort. Erst hört das Kind die Wörter
 * in Ruhe an, dann kommt ein Spiel: "Tap the dog!" und drei Bilder zur Wahl.
 */
data class EnglischWort(
    val bild: String,
    val de: String,
    val en: String,
    /** Mehrzahl wie "trousers": "Where are the trousers?" */
    val mehrzahl: Boolean = false,
    /** Farben und Zahlen stehen ohne "the": "Tap red!" */
    val ohneArtikel: Boolean = false,
)

enum class EnglischThema(val titel: String, val titelEn: String, val emoji: String) {
    FARBEN("Farben", "Colours", "🎨"),
    ZAHLEN("Zahlen", "Numbers", "🔢"),
    TIERE("Tiere", "Animals", "🐶"),
    ESSEN("Essen", "Food", "🍎"),
    KOERPER("Mein Körper", "My body", "👃"),
    KLEIDUNG("Kleidung", "Clothes", "👕"),
    DINGE("Spielen und draußen", "Play and outside", "⚽"),
    GEMISCHT("Alles gemischt", "Everything mixed", "🎲");

    /** Wörter dieses Themas. "Alles gemischt" hat keine eigenen. */
    val woerter: List<EnglischWort>
        get() = when (this) {
            FARBEN -> VorschulEnglisch.farben
            ZAHLEN -> VorschulEnglisch.zahlen
            TIERE -> VorschulEnglisch.tiere
            ESSEN -> VorschulEnglisch.essen
            KOERPER -> VorschulEnglisch.koerper
            KLEIDUNG -> VorschulEnglisch.kleidung
            DINGE -> VorschulEnglisch.dinge
            GEMISCHT -> mitWoertern.flatMap { it.woerter }
        }

    /** Name für Ergebnisse und Eltern-Übersicht. */
    val uebungName: String get() = "🇬🇧 $titel"

    companion object {
        /** Alle Themen mit eigenen Wörtern. */
        val mitWoertern: List<EnglischThema> = listOf(FARBEN, ZAHLEN, TIERE, ESSEN, KOERPER, KLEIDUNG, DINGE)
    }
}

object VorschulEnglisch {
    private fun f(bild: String, de: String, en: String) = EnglischWort(bild, de, en, ohneArtikel = true)
    private fun w(bild: String, de: String, en: String, mehrzahl: Boolean = false) = EnglischWort(bild, de, en, mehrzahl)

    val farben = listOf(
        f("🔴", "rot", "red"), f("🔵", "blau", "blue"), f("🟡", "gelb", "yellow"),
        f("🟢", "grün", "green"), f("🟠", "orange", "orange"), f("🟣", "lila", "purple"),
        f("⚫", "schwarz", "black"), f("⚪", "weiß", "white"), f("🟤", "braun", "brown"),
    )

    val zahlen = listOf(
        f("1", "eins", "one"), f("2", "zwei", "two"), f("3", "drei", "three"), f("4", "vier", "four"),
        f("5", "fünf", "five"), f("6", "sechs", "six"), f("7", "sieben", "seven"), f("8", "acht", "eight"),
        f("9", "neun", "nine"), f("10", "zehn", "ten"),
    )

    val tiere = listOf(
        w("🐶", "der Hund", "dog"), w("🐱", "die Katze", "cat"), w("🐦", "der Vogel", "bird"),
        w("🐟", "der Fisch", "fish"), w("🐴", "das Pferd", "horse"), w("🐮", "die Kuh", "cow"),
        w("🐷", "das Schwein", "pig"), w("🦆", "die Ente", "duck"), w("🐭", "die Maus", "mouse"),
        w("🐰", "der Hase", "rabbit"), w("🦁", "der Löwe", "lion"), w("🐘", "der Elefant", "elephant"),
        w("🐸", "der Frosch", "frog"), w("🐻", "der Bär", "bear"),
    )

    val essen = listOf(
        w("🍎", "der Apfel", "apple"), w("🍌", "die Banane", "banana"), w("🍐", "die Birne", "pear"),
        w("🍓", "die Erdbeere", "strawberry"), w("🍇", "die Weintrauben", "grapes", true),
        w("🥕", "die Karotte", "carrot"), w("🍞", "das Brot", "bread"), w("🧀", "der Käse", "cheese"),
        w("🥚", "das Ei", "egg"), w("🥛", "die Milch", "milk"), w("🍰", "der Kuchen", "cake"),
        w("🍦", "das Eis", "ice cream"),
    )

    val koerper = listOf(
        w("👃", "die Nase", "nose"), w("👂", "das Ohr", "ear"), w("👁️", "das Auge", "eye"),
        w("👄", "der Mund", "mouth"), w("✋", "die Hand", "hand"), w("🦶", "der Fuß", "foot"),
        w("🦵", "das Bein", "leg"), w("🦷", "der Zahn", "tooth"),
    )

    val kleidung = listOf(
        w("👕", "das T-Shirt", "T-shirt"), w("👖", "die Hose", "trousers", true), w("👗", "das Kleid", "dress"),
        w("🧥", "die Jacke", "coat"), w("🧦", "die Socken", "socks", true), w("👟", "die Schuhe", "shoes", true),
        w("🧢", "die Mütze", "cap"), w("🧣", "der Schal", "scarf"), w("🧤", "die Handschuhe", "gloves", true),
    )

    val dinge = listOf(
        w("⚽", "der Ball", "ball"), w("🚗", "das Auto", "car"), w("🚌", "der Bus", "bus"),
        w("🚂", "der Zug", "train"), w("✈️", "das Flugzeug", "plane"), w("📖", "das Buch", "book"),
        w("🏠", "das Haus", "house"), w("☀️", "die Sonne", "sun"), w("🌙", "der Mond", "moon"),
        w("⭐", "der Stern", "star"), w("🌳", "der Baum", "tree"), w("🌸", "die Blume", "flower"),
    )

    /** Was die Stimme beim Anhören sagt: nur das Wort, damit es gut hängen bleibt. */
    fun anhoeren(w: EnglischWort): String = w.en

    /** Die Aufforderung im Spiel. [art] wählt eine von drei Formen. */
    fun aufforderung(w: EnglischWort, art: Int): String {
        val ding = if (w.ohneArtikel) w.en else "the ${w.en}"
        return when (art % 3) {
            0 -> "Tap $ding!"
            1 -> "Where ${if (w.mehrzahl) "are" else "is"} $ding?"
            else -> "Find $ding!"
        }
    }

    /** Was die Eltern unter der Aufforderung lesen: "dog: der Hund". */
    fun bedeutung(w: EnglischWort): String = "${w.en}: ${w.de}"

    /** Eine Runde: jedes Wort höchstens einmal gesucht, drei Bilder zur Wahl. */
    fun fragen(thema: EnglischThema, anzahl: Int = 8, zufall: Random = Random.Default): List<VorschulFrage> {
        val ziele = mutableListOf<Pair<EnglischWort, List<EnglischWort>>>()
        if (thema == EnglischThema.GEMISCHT) {
            // Aus jedem Thema etwas, die anderen Bilder kommen aus demselben Thema
            var themen = EnglischThema.mitWoertern.shuffled(zufall).toMutableList()
            var runden = 0
            val gesehen = mutableSetOf<String>()
            while (ziele.size < anzahl && runden < 200) {
                runden += 1
                val t = themen.removeAt(0)
                val wort = t.woerter.randomOrNull(zufall)
                if (wort != null && gesehen.add(wort.en)) ziele.add(wort to t.woerter)
                if (themen.isEmpty() && ziele.size < anzahl) themen = EnglischThema.mitWoertern.shuffled(zufall).toMutableList()
            }
        } else {
            val liste = thema.woerter
            liste.shuffled(zufall).take(anzahl).forEach { ziele.add(it to liste) }
        }
        return ziele.mapIndexed { i, paar -> frage(paar.first, paar.second, i, zufall) }
    }

    fun frage(w: EnglischWort, liste: List<EnglischWort>, art: Int, zufall: Random = Random.Default): VorschulFrage {
        val optionen = mutableListOf(w.bild)
        for (a in liste.shuffled(zufall)) {
            if (optionen.size < 3 && a.bild !in optionen) optionen.add(a.bild)
        }
        optionen.shuffle(zufall)
        return VorschulFrage(
            text = VorschulText(de = bedeutung(w), tr = null, en = aufforderung(w, art)),
            optionen = optionen,
            richtig = optionen.indexOf(w.bild).coerceAtLeast(0),
        )
    }
}
