package de.cemaras.yem1n.logik.lehrplan

import de.cemaras.yem1n.logik.LehrFach
import de.cemaras.yem1n.logik.Lern
import de.cemaras.yem1n.logik.LernAufgabe
import de.cemaras.yem1n.logik.LernThema
import de.cemaras.yem1n.logik.Noten
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private fun LernAufgabe.mit(code: String) = copy(bild = code)

/** Ein Wort aus dem Sprachkatalog: Bild, Deutsch, Englisch */
class Vokabel(
    val emoji: String, val de: String, val en: String, val tr: String = "", val it: String = "",
) {
    fun text(sprache: String): String = when (sprache) { "en" -> en; "tr" -> tr; "it" -> it; else -> de }
}

/** Englisch ab Klasse 3. Klasse 1 und 2 sind Extras zum Reinschnuppern. */
object LehrplanEnglisch {
    private const val TIPP = "Tippe auf den Lautsprecher und sprich das englische Wort laut nach."

    private fun thema(
        id: String, titel: String, emoji: String, text: String,
        klasse: Int, katalog: String, lehrplan: Boolean = true,
    ) = LernThema(
        id = id, fach = LehrFach.ENGLISCH, titel = titel, emoji = emoji, beschreibung = text,
        klasse = klasse, lehrplan = lehrplan, tipp = TIPP, erzeuge = { vokabel(katalog) },
    )

    val themen: List<LernThema> by lazy {
        listOf(
            thema("e1-farben", "Colours: Farben", "🎨", "red, blue, green ...", 1, "farben", false),
            thema("e1-zahlen", "Numbers: Zahlen bis 10", "🔢", "one, two, three ...", 1, "zahl0", false),
            thema("e2-tiere", "Animals: Haustiere", "🐶", "dog, cat, horse ...", 2, "haustiere", false),
            thema("e2-obst", "Fruit: Obst und Gemüse", "🍎", "apple, banana ...", 2, "obst", false),
            thema("e3-familie", "Family: Familie", "👨‍👩‍👧", "mother, father, sister ...", 3, "familie"),
            thema("e3-kleidung", "Clothes: Kleidung", "👕", "T-shirt, shoes, hat ...", 3, "kleidung"),
            thema("e3-schule", "School: Schule", "🏫", "pencil, book, teacher ...", 3, "schule"),
            thema("e3-zahlen", "Numbers: 11 bis 20", "🔢", "eleven, twelve ...", 3, "zahl20"),
            thema("e3-tage", "Days: Wochentage", "📅", "Monday, Tuesday ...", 3, "wochentage"),
            thema("e4-wetter", "Weather: Wetter", "☀️", "sun, rain, snow ...", 4, "wetter"),
            thema("e4-monate", "Months: Monate", "🗓️", "January, February ...", 4, "monate"),
            thema("e4-zeit", "Seasons: Jahreszeiten", "🌷", "spring, summer ...", 4, "zeit"),
            thema("e4-wildtiere", "Wild animals: Wilde Tiere", "🦁", "lion, tiger, monkey ...", 4, "wildtiere"),
            thema("e4-freizeit", "Hobbies: Freizeit", "⚽", "ball, game, music ...", 4, "freizeit"),
            thema("e3-koerper", "Body: Körper", "🧍", "head, hand, foot ...", 3, "koerper"),
            thema("e3-essen", "Food: Essen und Trinken", "🍞", "bread, milk, water ...", 3, "essen"),
            thema("e3-hallo", "Hello! Begrüßen und Danken", "👋", "hello, thank you, sorry ...", 3, "hallo"),
            thema("e4-zuhause", "Home: Zuhause", "🏠", "house, bed, kitchen ...", 4, "zuhause"),
            thema("e4-fahrzeuge", "Transport: Fahrzeuge", "🚗", "car, bus, train ...", 4, "fahrzeuge"),
            thema("e4-stadt", "Town: Stadt und Orte", "🏙️", "shop, park, school ...", 4, "stadt"),
            thema("e4-tun", "Actions: Tätigkeiten", "🏃", "run, swim, read ...", 4, "taetigkeiten"),
            thema("e4-gegenteile", "Opposites: Gegenteile", "↔️", "big, small, hot, cold ...", 4, "gegenteile"),
        )
    }

    /** Katalog-Thema zu Thema der Englisch-Lernwelt, für den Weg von den Sprachen in die Fragen. */
    val katalogZuThemaId = mapOf(
        "farben" to "e1-farben", "zahl0" to "e1-zahlen", "haustiere" to "e2-tiere", "obst" to "e2-obst",
        "familie" to "e3-familie", "kleidung" to "e3-kleidung", "schule" to "e3-schule", "zahl20" to "e3-zahlen",
        "wochentage" to "e3-tage", "wetter" to "e4-wetter", "monate" to "e4-monate", "zeit" to "e4-zeit",
        "wildtiere" to "e4-wildtiere", "freizeit" to "e4-freizeit", "koerper" to "e3-koerper", "essen" to "e3-essen",
        "hallo" to "e3-hallo", "zuhause" to "e4-zuhause", "fahrzeuge" to "e4-fahrzeuge", "stadt" to "e4-stadt",
        "taetigkeiten" to "e4-tun", "gegenteile" to "e4-gegenteile",
    )

    /** Wörter je Katalog-Thema, aus der mitgelieferten Datei sprachkatalog.json */
    val woerter: Map<String, List<Vokabel>> by lazy {
        try {
            val text = LehrplanEnglisch::class.java.classLoader!!
                .getResourceAsStream("sprachkatalog.json")!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
            laden(text)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun laden(text: String): Map<String, List<Vokabel>> {
        val wurzel = Json.parseToJsonElement(text).jsonObject
        val erg = HashMap<String, List<Vokabel>>()
        for (t in wurzel["themen"]?.jsonArray ?: JsonArray(emptyList())) {
            val o = t.jsonObject
            val id = (o["id"] as? JsonPrimitive)?.content ?: continue
            erg[id] = (o["woerter"]?.jsonArray ?: JsonArray(emptyList())).mapNotNull { w ->
                val wo: JsonObject = w.jsonObject
                val de = (wo["de"] as? JsonPrimitive)?.content.orEmpty()
                val en = (wo["en"] as? JsonPrimitive)?.content.orEmpty()
                val tr = (wo["tr"] as? JsonPrimitive)?.content.orEmpty()
                val it = (wo["it"] as? JsonPrimitive)?.content.orEmpty()
                if (de.isEmpty() || en.isEmpty()) null
                else Vokabel((wo["emoji"] as? JsonPrimitive)?.content.orEmpty(), de, en, tr, it)
            }
        }
        return erg
    }

    fun nurZiffern(e: String) = e.isNotEmpty() && e.all { it.isDigit() || it == '️' || it == '⃣' }

    fun vokabel(katalog: String): LernAufgabe {
        val liste = woerter[katalog].orEmpty()
        if (liste.size < 3) {
            return Lern.wahl("Wie heißt das auf Englisch?", "Hallo", "hello", listOf("goodbye", "thank you"))
        }
        val w = Lern.eins(liste)
        val mitBild = liste.filter { it.emoji.isNotEmpty() && !nurZiffern(it.emoji) }
        if (mitBild.size >= 3 && w.emoji.isNotEmpty() && mitBild.any { it.en == w.en } && Lern.wahr(0.33)) {
            return Lern.wahl(
                "Hör gut zu! Welches Bild passt?", richtig = w.emoji, falsch = mitBild.map { it.emoji },
                bild = "englisch:${w.en}", erklaerung = "${w.en} heißt ${w.de} ${w.emoji}.",
            )
        }
        if (Lern.wahr(0.5)) {
            val vorne = if (w.emoji.isEmpty()) "" else w.emoji + " "
            return Lern.wahl(
                "Wie heißt das auf Englisch?", vorne + w.de, w.en, liste.map { it.en },
                erklaerung = "${w.de} heißt auf Englisch ${w.en}.",
            )
        }
        return Lern.wahl(
            "Was bedeutet das englische Wort?", w.en, w.de, liste.map { it.de },
            bild = "englisch:${w.en}", erklaerung = "${w.en} heißt ${w.de}.",
        )
    }
}

/** Musik: Instrumente, Noten, Takt, Fachbegriffe, Komponisten */
object LehrplanMusik {
    private fun t(id: String, titel: String, emoji: String, text: String, klasse: Int, tipp: String = "",
                  f: () -> LernAufgabe) =
        LernThema(id = id, fach = LehrFach.MUSIK, titel = titel, emoji = emoji, beschreibung = text,
            klasse = klasse, tipp = tipp, erzeuge = f)

    val themen: List<LernThema> by lazy {
        listOf(
            t("mu1-instrumente", "Instrumente erkennen", "🥁", "Wie heißt das Instrument?", 1) { instrument() },
            t("mu1-laut", "Laut oder leise?", "🔊", "Geräusche um uns herum", 1) { lautLeise() },
            t("mu2-notenwerte", "Notenwerte", "🎼", "Ganze, halbe und Viertelnoten", 2,
                "Eine ganze Note dauert 4 Schläge, eine halbe 2 und eine Viertelnote 1.") { notenwert() },
            t("mu3-noten", "Noten lesen", "🎵", "c, d, e, f, g, a, h im Violinschlüssel", 3,
                "Die unterste Linie ist das e. Das c hat eine eigene kleine Hilfslinie.") { noteLesen() },
            t("mu3-familien", "Instrumentenfamilien", "🎻", "Streichen, zupfen, blasen, schlagen, Tasten", 3) { familie() },
            t("mu4-takt", "Takt", "🥢", "Wie viele Noten passen in einen Takt?", 4,
                "Im 3/4-Takt passen 3 Viertelnoten in einen Takt.") { takt() },
            t("mu4-begriffe", "piano, forte und mehr", "🎚️", "Wie laut und wie schnell?", 4) { begriff() },
            t("mu4-komponisten", "Berühmte Komponisten", "🎹", "Wer hat das komponiert?", 4) { komponist() },
            t("mu1-hochtief", "Hoch oder tief?", "🐦", "Zwei Töne anhören und vergleichen", 1,
                "Ein hoher Ton klingt wie ein Vogel, ein tiefer wie ein Bär.") { hochTief() },
            t("mu3-tonleiter", "Die Tonleiter", "🪜", "c, d, e, f, g, a, h, c", 3,
                "Die Tonleiter geht c, d, e, f, g, a, h und dann wieder c.") { tonleiter() },
            t("mu4-stimmen", "Singstimmen", "🎤", "Sopran, Alt, Tenor und Bass", 4) { stimmen() },
        )
    }

    fun hochTief(): LernAufgabe {
        var a = Lern.eins(Noten.alle)
        var b = Lern.eins(Noten.alle)
        while (Math.abs((Noten.stufe[a] ?: 0) - (Noten.stufe[b] ?: 0)) < 3) {
            a = Lern.eins(Noten.alle)
            b = Lern.eins(Noten.alle)
        }
        val ersterHoeher = (Noten.stufe[a] ?: 0) > (Noten.stufe[b] ?: 0)
        val fragHoeher = Lern.wahr(0.5)
        val richtig = if (ersterHoeher == fragHoeher) "Ton 1" else "Ton 2"
        return LernAufgabe(
            art = "wahl",
            frage = if (fragHoeher) "Hör dir beide Töne an. Welcher ist höher?" else "Hör dir beide Töne an. Welcher ist tiefer?",
            antwort = richtig, erklaerung = "$richtig klingt ${if (fragHoeher) "höher" else "tiefer"}.",
            optionen = listOf("Ton 1", "Ton 2"), bild = "toene:$a|$b",
        )
    }

    val leiter = listOf("c", "d", "e", "f", "g", "a", "h")

    fun tonleiter(): LernAufgabe {
        if (Lern.wahr(0.1)) {
            return Lern.zahl("Wie viele Töne hat die Tonleiter von c bis zum nächsten c?", "", 8,
                erklaerung = "c, d, e, f, g, a, h, c: das sind 8 Töne.")
        }
        val i = Lern.z(0..6)
        if (Lern.wahr(0.5)) {
            val richtig = leiter[(i + 1) % 7]
            return Lern.wahl("Welcher Ton kommt danach?", "${leiter[i]}, ?", richtig, leiter,
                erklaerung = "Nach ${leiter[i]} kommt $richtig.")
        }
        val richtig = leiter[(i + 6) % 7]
        return Lern.wahl("Welcher Ton kommt davor?", "?, ${leiter[i]}", richtig, leiter,
            erklaerung = "Vor ${leiter[i]} kommt $richtig.")
    }

    val stimmlagen = listOf(
        "Sopran" to "die hohe Frauenstimme", "Alt" to "die tiefe Frauenstimme",
        "Tenor" to "die hohe Männerstimme", "Bass" to "die tiefe Männerstimme",
    )

    fun stimmen(): LernAufgabe {
        return when (Lern.z(0..2)) {
            0 -> {
                val (name, text) = Lern.eins(stimmlagen)
                Lern.wahl("Wie heißt $text?", richtig = name, falsch = stimmlagen.map { it.first },
                    erklaerung = "$name ist $text.")
            }
            1 -> {
                val (name, text) = Lern.eins(stimmlagen)
                Lern.wahl("Was ist ein $name?", richtig = text, falsch = stimmlagen.map { it.second },
                    erklaerung = "$name ist $text.")
            }
            else -> {
                val hoch = Lern.wahr(0.5)
                Lern.wahl(if (hoch) "Welche Stimme singt am höchsten?" else "Welche Stimme singt am tiefsten?",
                    richtig = if (hoch) "Sopran" else "Bass", falsch = stimmlagen.map { it.first },
                    erklaerung = if (hoch) "Der Sopran singt am höchsten." else "Der Bass singt am tiefsten.")
            }
        }
    }

    val instrumente = listOf(
        "🥁" to "Trommel", "🎻" to "Geige", "🎺" to "Trompete", "🎹" to "Klavier", "🎸" to "Gitarre",
        "🪈" to "Flöte", "🎷" to "Saxofon", "🪗" to "Akkordeon", "🔔" to "Glocke", "🪇" to "Rassel",
    )

    fun instrument(): LernAufgabe {
        val (bild, name) = Lern.eins(instrumente)
        return Lern.wahl("Wie heißt das Instrument?", richtig = name, falsch = instrumente.map { it.second },
            bild = "emoji:$bild", erklaerung = "Das Instrument heißt $name.")
    }

    val geraeusche = listOf(
        "Eine Maus piepst." to "leise", "Ein Gewitter donnert." to "laut", "Ein Flugzeug startet." to "laut",
        "Jemand flüstert." to "leise", "Eine Feder fällt zu Boden." to "leise",
        "Ein Feuerwehrauto fährt mit Sirene." to "laut", "Der Wecker klingelt." to "laut",
        "Ein Baby schläft und atmet." to "leise", "Blätter rascheln im Wind." to "leise",
        "Ein Presslufthammer hämmert." to "laut", "Eine Katze schnurrt." to "leise",
        "Die Kirchenglocken läuten." to "laut", "Ein Löwe brüllt." to "laut", "Schnee fällt." to "leise",
    )

    fun lautLeise(): LernAufgabe {
        val (satz, richtig) = Lern.eins(geraeusche)
        return LernAufgabe(art = "wahl", frage = "Ist das laut oder leise?", rechnung = satz, antwort = richtig,
            erklaerung = "Das ist $richtig.", optionen = listOf("laut", "leise"))
    }

    val notenNamen = mapOf("ganze" to "ganze Note", "halbe" to "halbe Note", "viertel" to "Viertelnote",
        "achtel" to "Achtelnote")

    fun notenwert(): LernAufgabe {
        val alleNamen = listOf("ganze Note", "halbe Note", "Viertelnote", "Achtelnote")
        return when (Lern.z(0..3)) {
            0 -> {
                val w = Lern.eins(listOf("ganze", "halbe", "viertel", "achtel"))
                val name = notenNamen[w].orEmpty()
                LernAufgabe(art = "wahl", frage = "Wie heißt diese Note?", antwort = name,
                    erklaerung = "Das ist eine $name.", optionen = alleNamen, bild = "notenwert:$w")
            }
            1 -> {
                val w = Lern.eins(listOf(Triple("ganze", 4, 0), Triple("halbe", 2, 0), Triple("viertel", 1, 0)))
                val name = notenNamen[w.first].orEmpty()
                Lern.zahl("Wie viele Schläge dauert diese Note?", "", w.second,
                    erklaerung = "Eine $name dauert ${w.second} ${if (w.second == 1) "Schlag" else "Schläge"}.")
                    .mit("notenwert:${w.first}")
            }
            2 -> {
                val f = Lern.eins(listOf(
                    "Wie viele Viertelnoten dauern so lang wie eine halbe Note?" to 2,
                    "Wie viele Viertelnoten dauern so lang wie eine ganze Note?" to 4,
                    "Wie viele halbe Noten dauern so lang wie eine ganze Note?" to 2,
                    "Wie viele Achtelnoten dauern so lang wie eine Viertelnote?" to 2,
                ))
                Lern.zahl(f.first, "", f.second, erklaerung = "Richtig sind ${f.second}.")
            }
            else -> {
                val f = Lern.eins(listOf(
                    "Zwei Viertelnoten dauern so lang wie eine ..." to "halbe Note",
                    "Zwei halbe Noten dauern so lang wie eine ..." to "ganze Note",
                    "Zwei Achtelnoten dauern so lang wie eine ..." to "Viertelnote",
                ))
                LernAufgabe(art = "wahl", frage = f.first, antwort = f.second,
                    erklaerung = "${f.first.dropLast(4)} ${f.second}.", optionen = alleNamen)
            }
        }
    }

    fun noteLesen(): LernAufgabe {
        val note = Lern.eins(Noten.alle)
        val name = Noten.name[note].orEmpty()
        return Lern.wahl("Wie heißt die Note?", richtig = name, falsch = listOf("c", "d", "e", "f", "g", "a", "h"),
            bild = "note:$note", erklaerung = "Das ist ein $name.")
    }

    val familien = listOf(
        "Geige" to "Streichinstrument", "Cello" to "Streichinstrument", "Kontrabass" to "Streichinstrument",
        "Bratsche" to "Streichinstrument", "Gitarre" to "Zupfinstrument", "Harfe" to "Zupfinstrument",
        "Trompete" to "Blasinstrument", "Posaune" to "Blasinstrument", "Tuba" to "Blasinstrument",
        "Blockflöte" to "Blasinstrument", "Klarinette" to "Blasinstrument", "Querflöte" to "Blasinstrument",
        "Trommel" to "Schlaginstrument", "Pauke" to "Schlaginstrument", "Xylofon" to "Schlaginstrument",
        "Becken" to "Schlaginstrument", "Triangel" to "Schlaginstrument",
        "Klavier" to "Tasteninstrument", "Orgel" to "Tasteninstrument",
    )

    fun familie(): LernAufgabe {
        val (instrument, art) = Lern.eins(familien)
        val alle = listOf("Streichinstrument", "Zupfinstrument", "Blasinstrument", "Schlaginstrument", "Tasteninstrument")
        return Lern.wahl("Zu welcher Familie gehört das Instrument?", instrument, art, alle,
            erklaerung = "$instrument ist ein $art.")
    }

    fun takt(): LernAufgabe {
        val t = Lern.z(2..4)
        return when (Lern.z(0..3)) {
            0 -> Lern.zahl("Wie viele Viertelnoten passen in einen $t/4-Takt?", "", t,
                erklaerung = "In einen $t/4-Takt passen $t Viertelnoten.")
            1 -> Lern.zahl("Wie viele Achtelnoten passen in einen $t/4-Takt?", "", 2 * t,
                erklaerung = "Eine Viertelnote sind 2 Achtelnoten. $t · 2 = ${2 * t}")
            2 -> Lern.zahl("Im $t/4-Takt steht schon eine Viertelnote. Wie viele Viertelnoten fehlen noch?", "", t - 1,
                erklaerung = "$t - 1 = ${t - 1}")
            else -> {
                val tt = maxOf(t, 3)
                Lern.zahl("Im $tt/4-Takt steht schon eine halbe Note. Wie viele Viertelnoten fehlen noch?", "", tt - 2,
                    erklaerung = "Eine halbe Note sind 2 Viertel. $tt - 2 = ${tt - 2}")
            }
        }
    }

    val begriffe = listOf(
        "piano" to "leise", "forte" to "laut", "pianissimo" to "sehr leise", "fortissimo" to "sehr laut",
        "crescendo" to "lauter werden", "decrescendo" to "leiser werden", "allegro" to "schnell und fröhlich",
        "adagio" to "langsam", "presto" to "sehr schnell",
    )

    fun begriff(): LernAufgabe {
        val (wort, bedeutung) = Lern.eins(begriffe)
        if (Lern.wahr(0.5)) {
            return Lern.wahl("Was bedeutet das in der Musik?", wort, bedeutung, begriffe.map { it.second },
                erklaerung = "$wort heißt $bedeutung.")
        }
        return Lern.wahl("Welches Wort steht in den Noten dafür?", bedeutung, wort, begriffe.map { it.first },
            erklaerung = "$bedeutung heißt in der Musik $wort.")
    }

    val werke = listOf(
        "Die Zauberflöte" to "Mozart", "Eine kleine Nachtmusik" to "Mozart", "Für Elise" to "Beethoven",
        "die Melodie der Ode an die Freude" to "Beethoven", "Die vier Jahreszeiten" to "Vivaldi",
        "Der Nussknacker" to "Tschaikowsky", "Schwanensee" to "Tschaikowsky", "Peter und der Wolf" to "Prokofjew",
        "Karneval der Tiere" to "Saint-Saëns", "Die Moldau" to "Smetana",
        "In der Halle des Bergkönigs" to "Grieg", "Die Wassermusik" to "Händel",
        "die Oper Hänsel und Gretel" to "Humperdinck",
    )

    fun komponist(): LernAufgabe {
        val (werk, wer) = Lern.eins(werke)
        return Lern.wahl("Wer hat das komponiert?", werk, wer, werke.map { it.second },
            erklaerung = "$werk: von $wer.")
    }
}

/** Kunst: Farben, Formen, Farbkreis, Techniken, Künstler */
object LehrplanKunst {
    private fun t(id: String, titel: String, emoji: String, text: String, klasse: Int, tipp: String = "",
                  f: () -> LernAufgabe) =
        LernThema(id = id, fach = LehrFach.KUNST, titel = titel, emoji = emoji, beschreibung = text,
            klasse = klasse, tipp = tipp, erzeuge = f)

    val themen: List<LernThema> by lazy {
        listOf(
            t("ku1-farben", "Farben und Grundfarben", "🖍️", "Rot, Gelb und Blau sind die Grundfarben", 1) { grundfarben() },
            t("ku1-formen", "Formen", "🔺", "Kreis, Dreieck, Quadrat und mehr", 1) { formen() },
            t("ku2-mischen", "Farben mischen", "🎨", "Aus zwei Farben wird eine neue", 2,
                "Blau und Gelb ergibt Grün. Mit Weiß wird eine Farbe heller.") { mischen() },
            t("ku3-farbkreis", "Farbkreis", "🌈", "Warme, kalte und Gegenfarben", 3,
                "Gegenfarben liegen sich im Farbkreis gegenüber: Rot und Grün, Blau und Orange, Gelb und Violett.") { farbkreis() },
            t("ku3-techniken", "Techniken und Bildarten", "✂️", "Collage, Aquarell, Porträt ...", 3) { technik() },
            t("ku4-kuenstler", "Berühmte Künstler", "🖼️", "Wer hat das gemacht?", 4) { kuenstler() },
            t("ku2-helldunkel", "Heller und dunkler", "🌗", "Farben mit Weiß und Schwarz mischen", 2) { hellDunkel() },
            t("ku1-werkzeuge", "Malwerkzeuge", "🖌️", "Pinsel, Schere, Lineal und mehr", 1) { werkzeug() },
            t("ku4-bildaufbau", "Bildaufbau", "🏞️", "Vordergrund, Hintergrund und Formate", 4) { bildaufbau() },
        )
    }

    val farbNamen = mapOf(
        "rot" to "Rot", "gelb" to "Gelb", "blau" to "Blau", "gruen" to "Grün", "orange" to "Orange",
        "violett" to "Violett", "braun" to "Braun", "rosa" to "Rosa", "schwarz" to "Schwarz", "weiss" to "Weiß",
        "hellblau" to "Hellblau", "grau" to "Grau", "tuerkis" to "Türkis",
    )

    private fun n(code: String) = farbNamen[code] ?: code

    fun hellDunkel(): LernAufgabe {
        val code = Lern.eins(listOf("rot", "blau", "gruen", "violett", "orange", "braun"))
        val heller = Lern.wahr(0.5)
        return LernAufgabe(art = "wahl", frage = "Womit machst du ${n(code)} ${if (heller) "heller" else "dunkler"}?",
            antwort = if (heller) "Weiß" else "Schwarz",
            erklaerung = if (heller) "Mit Weiß wird eine Farbe heller." else "Mit Schwarz wird eine Farbe dunkler.",
            optionen = listOf("Weiß", "Schwarz"), bild = "farbe:$code")
    }

    val werkzeuge = listOf(
        "🖌️" to "Pinsel", "✂️" to "Schere", "🖍️" to "Wachsmalstift", "✏️" to "Bleistift",
        "📏" to "Lineal", "🧽" to "Schwamm", "🎨" to "Farbpalette",
    )

    fun werkzeug(): LernAufgabe {
        if (Lern.wahr(0.5)) {
            val (bild, name) = Lern.eins(werkzeuge)
            val e = if (name == "Schere" || name == "Farbpalette") "e" else ""
            return Lern.wahl("Wie heißt das?", richtig = name, falsch = werkzeuge.map { it.second },
                bild = "emoji:$bild", erklaerung = "Das ist ein$e $name.")
        }
        val f = Lern.eins(listOf(
            "Womit schneidest du Papier?" to "Schere", "Womit malst du mit Wasserfarben?" to "Pinsel",
            "Womit ziehst du gerade Linien?" to "Lineal", "Worauf mischst du Farben?" to "Farbpalette",
            "Womit wischst du Farbe vom Tisch?" to "Schwamm",
        ))
        return Lern.wahl(f.first, richtig = f.second, falsch = werkzeuge.map { it.second },
            erklaerung = "Dafür brauchst du: ${f.second}.")
    }

    val aufbauFragen = listOf(
        Triple("Was ganz vorne im Bild ist, liegt im ...", "Vordergrund", listOf("Hintergrund", "Mittelgrund")),
        Triple("Was ganz hinten im Bild ist, liegt im ...", "Hintergrund", listOf("Vordergrund", "Mittelgrund")),
        Triple("Was zwischen vorne und hinten liegt, ist der ...", "Mittelgrund", listOf("Vordergrund", "Hintergrund")),
        Triple("Wie malt man Dinge, die weit weg sind?", "kleiner", listOf("größer", "genauso groß")),
        Triple("Wie wirken Farben in der Ferne oft?", "heller und blasser", listOf("dunkler und kräftiger", "ganz schwarz")),
        Triple("Ein Bild, das breiter als hoch ist, hat ein ...", "Querformat", listOf("Hochformat", "Rundformat")),
        Triple("Ein Bild, das höher als breit ist, hat ein ...", "Hochformat", listOf("Querformat", "Rundformat")),
        Triple("Wo ist bei einem Landschaftsbild meist der Himmel?", "oben", listOf("unten", "in der Mitte")),
        Triple("Die Linie, an der Himmel und Erde sich treffen, heißt ...", "Horizont", listOf("Rahmen", "Kante")),
        Triple("Was ist bei einem Porträt die Hauptsache?", "ein Mensch", listOf("eine Landschaft", "eine Vase")),
    )

    fun bildaufbau(): LernAufgabe {
        val (frage, richtig, falsch) = Lern.eins(aufbauFragen)
        return Lern.wahl(frage, richtig = richtig, falsch = falsch, erklaerung = "Richtig ist: $richtig.")
    }

    fun grundfarben(): LernAufgabe {
        val grund = listOf("Rot", "Gelb", "Blau")
        val gemischt = listOf("Grün", "Orange", "Violett", "Braun")
        return when (Lern.z(0..2)) {
            0 -> Lern.wahl("Welche Farbe ist eine Grundfarbe?", richtig = Lern.eins(grund), falsch = gemischt,
                erklaerung = "Die Grundfarben sind Rot, Gelb und Blau.")
            1 -> Lern.wahl("Welche Farbe ist keine Grundfarbe?", richtig = Lern.eins(gemischt), falsch = grund,
                erklaerung = "Die Grundfarben sind Rot, Gelb und Blau. Alle anderen kann man mischen.")
            else -> {
                val code = Lern.eins(listOf("rot", "gelb", "blau", "gruen", "orange", "violett", "braun", "rosa", "schwarz", "weiss"))
                Lern.wahl("Welche Farbe siehst du?", richtig = n(code),
                    falsch = listOf("Rot", "Gelb", "Blau", "Grün", "Orange", "Violett", "Braun", "Rosa", "Schwarz", "Weiß"),
                    bild = "farbe:$code", erklaerung = "Das ist ${n(code)}.")
            }
        }
    }

    fun formen(): LernAufgabe {
        val namen = linkedMapOf("kreis" to "Kreis", "dreieck" to "Dreieck", "quadrat" to "Quadrat",
            "rechteck" to "Rechteck", "oval" to "Oval", "stern" to "Stern")
        if (Lern.wahr(0.5)) {
            val f = Lern.eins(namen.keys.toList())
            val name = namen[f].orEmpty()
            return Lern.wahl("Wie heißt die Form?", richtig = name, falsch = namen.values.toList(),
                bild = "form:$f", erklaerung = "Das ist ein $name.")
        }
        val f = Lern.eins(listOf(
            Triple("Wie viele Ecken hat ein Dreieck?", 3, "dreieck"),
            Triple("Wie viele Ecken hat ein Quadrat?", 4, "quadrat"),
            Triple("Wie viele Ecken hat ein Rechteck?", 4, "rechteck"),
            Triple("Wie viele Ecken hat ein Kreis?", 0, "kreis"),
            Triple("Wie viele Seiten hat ein Dreieck?", 3, "dreieck"),
            Triple("Wie viele Seiten hat ein Quadrat?", 4, "quadrat"),
            Triple("Wie viele Zacken hat der Stern?", 5, "stern"),
        ))
        return Lern.zahl(f.first, "", f.second, erklaerung = "Richtig sind ${f.second}.").mit("form:${f.third}")
    }

    val mischungen = listOf(
        Triple("blau", "gelb", "Grün"), Triple("rot", "gelb", "Orange"), Triple("rot", "blau", "Violett"),
        Triple("rot", "weiss", "Rosa"), Triple("blau", "weiss", "Hellblau"), Triple("schwarz", "weiss", "Grau"),
    )

    fun mischen(): LernAufgabe {
        val m = Lern.eins(mischungen)
        val tausch = Lern.wahr(0.5)
        val a = if (tausch) m.second else m.first
        val b = if (tausch) m.first else m.second
        return Lern.wahl("Welche Farbe entsteht, wenn du die beiden mischst?", richtig = m.third,
            falsch = mischungen.map { it.third } + "Braun", bild = "mischen:$a+$b",
            erklaerung = "${n(a)} und ${n(b)} ergibt ${m.third}.")
    }

    fun farbkreis(): LernAufgabe {
        if (Lern.wahr(0.5)) {
            val f = Lern.eins(listOf("rot" to "warm", "orange" to "warm", "gelb" to "warm",
                "blau" to "kalt", "tuerkis" to "kalt", "hellblau" to "kalt"))
            return LernAufgabe(art = "wahl", frage = "Wirkt diese Farbe warm oder kalt?", antwort = f.second,
                erklaerung = if (f.second == "warm") "${n(f.first)} erinnert an Sonne und Feuer: warm."
                else "${n(f.first)} erinnert an Wasser und Eis: kalt.",
                optionen = listOf("warm", "kalt"), bild = "farbe:${f.first}")
        }
        val p = Lern.eins(listOf("Rot" to "Grün", "Blau" to "Orange", "Gelb" to "Violett"))
        val umgekehrt = Lern.wahr(0.5)
        val von = if (umgekehrt) p.second else p.first
        val zu = if (umgekehrt) p.first else p.second
        return Lern.wahl("Welche Farbe liegt im Farbkreis gegenüber?", von, zu,
            listOf("Rot", "Grün", "Blau", "Orange", "Gelb", "Violett").filter { it != von },
            erklaerung = "$von und $zu sind Gegenfarben.")
    }

    val techniken = listOf(
        "Collage" to "Ein Bild aus aufgeklebten Papierstücken",
        "Aquarell" to "Ein Bild mit Wasserfarben, die durchscheinen",
        "Mosaik" to "Ein Bild aus vielen kleinen Steinchen",
        "Frottage" to "Eine Struktur mit dem Stift durchreiben",
        "Druck" to "Farbe mit einem Stempel aufs Papier bringen",
        "Skulptur" to "Eine Figur aus Stein, Holz oder Ton",
        "Porträt" to "Ein Bild von einem Menschen",
        "Selbstporträt" to "Ein Bild, auf dem man sich selbst malt",
        "Stillleben" to "Ein Bild von Dingen wie Obst und Vasen",
        "Landschaftsbild" to "Ein Bild von Wiesen, Bergen und Himmel",
    )

    fun technik(): LernAufgabe {
        val (name, text) = Lern.eins(techniken)
        if (Lern.wahr(0.5)) {
            return Lern.wahl("Wie nennt man das?", text, name, techniken.map { it.first }, erklaerung = "$text: $name.")
        }
        return Lern.wahl("Was ist das?", name, text, techniken.map { it.second }, erklaerung = "$name: $text.")
    }

    val kunstwerke = listOf(
        "Wer malte die Mona Lisa?" to "Leonardo da Vinci",
        "Wer malte die Sonnenblumen und die Sternennacht?" to "Vincent van Gogh",
        "Wer malte viele Bilder von Seerosen in seinem Garten?" to "Claude Monet",
        "Wer malte den Feldhasen?" to "Albrecht Dürer",
        "Wer baute bunte Häuser fast ohne gerade Linien?" to "Friedensreich Hundertwasser",
        "Wer malte Gesichter gleichzeitig von vorn und von der Seite?" to "Pablo Picasso",
        "Wer malte bunte Kreise und Linien und dachte dabei an Musik?" to "Wassily Kandinsky",
        "Wer malte das Bild Burg und Sonne aus bunten Formen?" to "Paul Klee",
        "Wer malte viele Selbstporträts mit Blumen im Haar?" to "Frida Kahlo",
        "Wer schnitt für seine Bilder bunte Formen aus Papier aus?" to "Henri Matisse",
        "Wer malte das goldene Bild Der Kuss?" to "Gustav Klimt",
        "Wer baute die großen, bunten Frauenfiguren Nanas?" to "Niki de Saint Phalle",
    )

    fun kuenstler(): LernAufgabe {
        val (frage, wer) = Lern.eins(kunstwerke)
        return Lern.wahl("Wer war das?", frage, wer, kunstwerke.map { it.second }, erklaerung = "Das war $wer.")
    }
}

/**
 * Türkisch und Italienisch aus dem Sprachkatalog. Sie stehen nicht im Lehrplan,
 * darum sind es Extras. Die Stufe des Katalogs bestimmt die Klasse (1 bis 3).
 */
object LehrplanSprachen {
    class Thema(val id: String, val titel: String, val emoji: String, val klasse: Int)

    val themenListe = listOf(
        Thema("hallo", "Hallo und Danke", "👋", 1), Thema("zahl0", "Zahlen 0 bis 10", "🔢", 1),
        Thema("farben", "Farben", "🎨", 1), Thema("haustiere", "Haustiere und Bauernhof", "🐶", 1),
        Thema("familie", "Familie", "👨‍👩‍👧", 1), Thema("koerper", "Körper", "🧍", 1),
        Thema("obst", "Obst und Gemüse", "🍎", 1), Thema("essen", "Essen und Trinken", "🍞", 1),
        Thema("wildtiere", "Wilde Tiere", "🦁", 2), Thema("zuhause", "Zuhause", "🏠", 2),
        Thema("schule", "Schule", "🏫", 2), Thema("kleidung", "Kleidung", "👕", 2),
        Thema("wetter", "Wetter und Natur", "☀️", 2), Thema("fahrzeuge", "Fahrzeuge", "🚗", 2),
        Thema("stadt", "Stadt und Orte", "🏙️", 2), Thema("freizeit", "Spielen und Freizeit", "⚽", 2),
        Thema("wochentage", "Wochentage", "📅", 2), Thema("zahl20", "Zahlen 11 bis 20", "🔢", 2),
        Thema("monate", "Monate", "🗓️", 3), Thema("zeit", "Jahreszeiten und Zeit", "🌷", 3),
        Thema("taetigkeiten", "Tätigkeiten", "🏃", 3), Thema("gegenteile", "Gegenteile", "↔️", 3),
        Thema("zehner", "Zehnerzahlen bis 100", "💯", 3), Thema("saetze", "Kleine Sätze", "💬", 3),
    )

    private fun themen(fach: LehrFach, kuerzel: String, adjektiv: String): List<LernThema> =
        themenListe.map { t ->
            LernThema(
                id = "$kuerzel-${t.id}", fach = fach, titel = t.titel, emoji = t.emoji,
                beschreibung = "Wörter anhören und erkennen", klasse = t.klasse, lehrplan = false,
                tipp = "Tippe auf den Lautsprecher und sprich das Wort laut nach.",
                erzeuge = { vokabel(t.id, kuerzel, adjektiv) },
            )
        }

    val tuerkisch: List<LernThema> by lazy { themen(LehrFach.TUERKISCH, "tr", "türkische") }
    val italienisch: List<LernThema> by lazy { themen(LehrFach.ITALIENISCH, "it", "italienische") }

    private fun sprachName(sprache: String) = if (sprache == "tr") "Türkisch" else "Italienisch"

    fun vokabel(katalog: String, sprache: String, adjektiv: String): LernAufgabe {
        val liste = LehrplanEnglisch.woerter[katalog].orEmpty().filter { it.text(sprache).isNotEmpty() }
        if (liste.size < 3) {
            return if (sprache == "tr") Lern.wahl("Wie heißt das auf Türkisch?", "Hallo", "merhaba", listOf("hoşça kal", "teşekkürler"))
            else Lern.wahl("Wie heißt das auf Italienisch?", "Hallo", "ciao", listOf("arrivederci", "grazie"))
        }
        val w = Lern.eins(liste)
        val fremd = w.text(sprache)
        val mitBild = liste.filter { it.emoji.isNotEmpty() && !LehrplanEnglisch.nurZiffern(it.emoji) }
        if (mitBild.size >= 3 && w.emoji.isNotEmpty() && mitBild.any { it.text(sprache) == fremd } && Lern.wahr(0.33)) {
            return Lern.wahl(
                "Hör gut zu! Welches Bild passt?", richtig = w.emoji, falsch = mitBild.map { it.emoji },
                bild = "sprich:$sprache|$fremd", erklaerung = "$fremd heißt ${w.de} ${w.emoji}.",
            )
        }
        if (Lern.wahr(0.5)) {
            val vorne = if (w.emoji.isEmpty()) "" else w.emoji + " "
            return Lern.wahl(
                "Wie heißt das auf ${sprachName(sprache)}?", vorne + w.de, fremd, liste.map { it.text(sprache) },
                erklaerung = "${w.de} heißt auf ${sprachName(sprache)} $fremd.",
            )
        }
        return Lern.wahl(
            "Was bedeutet das $adjektiv Wort?", fremd, w.de, liste.map { it.de },
            bild = "sprich:$sprache|$fremd", erklaerung = "$fremd heißt ${w.de}.",
        )
    }
}
