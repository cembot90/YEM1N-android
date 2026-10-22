package de.cemaras.yem1n.logik.lehrplan

import de.cemaras.yem1n.logik.LehrFach
import de.cemaras.yem1n.logik.Lern
import de.cemaras.yem1n.logik.LernAufgabe
import de.cemaras.yem1n.logik.LernThema

// Lernwelt Deutsch, Klasse 1 bis 4. Port von LehrplanDeutsch.swift.
//
// Klasse 1: Anlaute, Silben, Reimwörter
// Klasse 2: der, die, das, Einzahl und Mehrzahl, ABC, Groß oder klein
// Klasse 3: Wortarten, Vergangenheit, Verlängern (d oder t), i oder ie
// Klasse 4: Die vier Fälle, das oder dass, Zeitformen
// Die Wörter kommen aus dem Grundwortschatz der Grundschule. Jedes Wort hier ist eindeutig:
// keine Wörter mit zwei möglichen Artikeln oder zwei richtigen Mehrzahlformen.

object LehrplanDeutsch {
    private val D = LehrFach.DEUTSCH

    val themen: List<LernThema> by lazy {
        listOf(
            LernThema(
                "d1-anlaut", D, "Anlaute", "🍎", "Mit welchem Buchstaben fängt das Wort an?", 1,
                tipp = "Sprich das Wort langsam und hör auf den ersten Laut.", erzeuge = ::anlaut,
            ),
            LernThema(
                "d1-silben", D, "Silben klatschen", "👏", "Wie viele Silben hat das Wort?", 1,
                tipp = "Sprich das Wort und klatsche bei jeder Silbe.", erzeuge = ::silben,
            ),
            LernThema(
                "d1-reime", D, "Reimwörter", "🎶", "Was reimt sich?", 1,
                tipp = "Wörter reimen sich, wenn sie am Ende gleich klingen: Haus und Maus.", erzeuge = ::reim,
            ),
            LernThema("d2-artikel", D, "der, die oder das", "🏷️", "Den richtigen Artikel finden", 2, erzeuge = ::artikel),
            LernThema("d2-mehrzahl", D, "Einzahl und Mehrzahl", "👯", "Ein Hund, viele Hunde", 2, erzeuge = ::mehrzahl),
            LernThema("d2-abc", D, "Das ABC", "🔤", "Buchstaben und Wörter ordnen", 2, erzeuge = ::abc),
            LernThema(
                "d2-gross", D, "Groß oder klein?", "🔠", "Nomen schreibt man groß", 2,
                tipp = "Kannst du der, die oder das davor sagen, schreibst du das Wort groß.", erzeuge = ::gross,
            ),
            LernThema(
                "d3-wortarten", D, "Wortarten", "🧩", "Nomen, Verb oder Adjektiv?", 3,
                tipp = "Nomen: der, die, das. Verb: ich ... Adjektiv: wie ist etwas?", erzeuge = ::wortart,
            ),
            LernThema("d3-vergangen", D, "Vergangenheit", "⏪", "Was gestern war: er ging, sie lachte", 3, erzeuge = ::vergangenheit),
            LernThema(
                "d3-verlaengern", D, "Verlängern: d oder t?", "↔️", "Auch g oder k, b oder p", 3,
                tipp = "Verlängere das Wort: Hund, die Hunde. Jetzt hörst du das d.", erzeuge = ::verlaengern,
            ),
            LernThema(
                "d3-ie", D, "i oder ie?", "🐝", "Lang oder kurz gesprochen", 3,
                tipp = "Klingt das i lang, schreibst du meistens ie: Biene. Klingt es kurz, nur i: Kind.",
                erzeuge = ::ieOderI,
            ),
            LernThema(
                "d4-faelle", D, "Die vier Fälle", "❓", "Wer, wessen, wem, wen?", 4,
                tipp = "Frag nach dem Wort: Wer oder was? Wessen? Wem? Wen oder was?", erzeuge = ::faelle,
            ),
            LernThema(
                "d4-dass", D, "das oder dass?", "🤔", "Mit einem oder zwei s", 4,
                tipp = "Kannst du dieses, jenes oder welches einsetzen, schreibst du das.", erzeuge = ::dasDass,
            ),
            LernThema("d4-zeitformen", D, "Zeitformen", "🕰️", "Präsens, Präteritum, Perfekt, Futur", 4, erzeuge = ::zeitform),
        )
    }

    // Wortlisten

    /** Bild, Wort. Nur Wörter, die mit einem einfachen Laut anfangen (kein Sch, Ei, Pf, Qu, St). */
    val anlaute: List<Pair<String, String>> = listOf(
        "🍎" to "Apfel", "🐒" to "Affe", "🚗" to "Auto", "⚽" to "Ball", "🍌" to "Banane", "🐻" to "Bär",
        "🐝" to "Biene", "📕" to "Buch", "🦆" to "Ente", "🐘" to "Elefant", "🐟" to "Fisch", "🦊" to "Fuchs",
        "🐸" to "Frosch", "🦒" to "Giraffe", "🎸" to "Gitarre", "🥒" to "Gurke", "🐶" to "Hund", "🐰" to "Hase",
        "🏠" to "Haus", "🦔" to "Igel", "🍰" to "Kuchen", "🐄" to "Kuh", "🧀" to "Käse", "🦁" to "Löwe",
        "🎈" to "Luftballon", "🐭" to "Maus", "🌙" to "Mond", "🍉" to "Melone", "👃" to "Nase", "🐙" to "Oktopus",
        "🍕" to "Pizza", "🐧" to "Pinguin", "🚀" to "Rakete", "🌹" to "Rose", "🌈" to "Regenbogen",
        "☀️" to "Sonne", "🍅" to "Tomate", "🧸" to "Teddy", "🌷" to "Tulpe", "⌚" to "Uhr", "☁️" to "Wolke",
        "🍋" to "Zitrone", "🦓" to "Zebra", "🦷" to "Zahn",
    )

    /** Silben, mit Bindestrich getrennt, so wie man sie spricht und klatscht */
    val silbenWoerter = listOf(
        "Ap-fel", "Ba-na-ne", "Ball", "Ro-se", "Ta-sche", "Ki-wi", "Me-lo-ne", "Ha-se", "Hund", "Kat-ze",
        "Scho-ko-la-de", "Te-le-fon", "Au-to", "Ra-ke-te", "Kro-ko-dil", "E-le-fant", "Son-ne", "Mond",
        "Blu-me", "Fisch", "Tisch", "Gi-raf-fe", "Ta-fel", "To-ma-te", "Ku-chen", "Brot", "Ka-mel",
        "Pin-gu-in", "Ze-bra", "Re-gen-bo-gen", "Kin-der-gar-ten", "Ka-rot-te", "Schmet-ter-ling", "Baum",
        "Pa-pa-gei", "Gur-ke", "Lö-we", "Am-pel", "Fahr-rad", "Le-se-buch",
    )

    val reimPaare: List<Pair<String, String>> = listOf(
        "Haus" to "Maus", "Hand" to "Sand", "Baum" to "Traum", "Topf" to "Kopf", "Hose" to "Rose",
        "Kanne" to "Tanne", "Bein" to "Schwein", "Fisch" to "Tisch", "Nase" to "Hase", "Hund" to "Mund",
        "Katze" to "Tatze", "Sonne" to "Tonne", "Wiese" to "Riese", "Glas" to "Gras", "Ball" to "Stall",
        "Hecke" to "Schnecke", "Kuchen" to "suchen", "Maler" to "Taler", "Rabe" to "Gabe", "Zug" to "Krug",
    )

    class Nomen(val artikel: String, val wort: String, val mehrzahl: String)

    private fun n(a: String, w: String, m: String) = Nomen(a, w, m)

    val nomen: List<Nomen> = listOf(
        n("der", "Hund", "Hunde"), n("die", "Katze", "Katzen"), n("das", "Haus", "Häuser"),
        n("der", "Baum", "Bäume"), n("die", "Blume", "Blumen"), n("das", "Kind", "Kinder"),
        n("der", "Ball", "Bälle"), n("die", "Maus", "Mäuse"), n("das", "Buch", "Bücher"),
        n("der", "Tisch", "Tische"), n("die", "Tasche", "Taschen"), n("das", "Auto", "Autos"),
        n("der", "Vogel", "Vögel"), n("die", "Schule", "Schulen"), n("das", "Bett", "Betten"),
        n("der", "Apfel", "Äpfel"), n("die", "Uhr", "Uhren"), n("das", "Bild", "Bilder"),
        n("der", "Stuhl", "Stühle"), n("die", "Hand", "Hände"), n("das", "Ei", "Eier"),
        n("der", "Fisch", "Fische"), n("die", "Banane", "Bananen"), n("das", "Glas", "Gläser"),
        n("der", "Stift", "Stifte"), n("die", "Tür", "Türen"), n("das", "Fenster", "Fenster"),
        n("der", "Zug", "Züge"), n("die", "Kuh", "Kühe"), n("das", "Pferd", "Pferde"),
        n("der", "Hase", "Hasen"), n("die", "Nase", "Nasen"), n("das", "Schaf", "Schafe"),
        n("der", "Schuh", "Schuhe"), n("die", "Gabel", "Gabeln"), n("das", "Rad", "Räder"),
        n("der", "Bär", "Bären"), n("die", "Birne", "Birnen"), n("das", "Heft", "Hefte"),
        n("der", "Igel", "Igel"), n("die", "Ente", "Enten"), n("das", "Boot", "Boote"),
        n("der", "Löwe", "Löwen"), n("die", "Wolke", "Wolken"), n("das", "Spiel", "Spiele"),
        n("der", "Freund", "Freunde"), n("die", "Tomate", "Tomaten"), n("das", "Zimmer", "Zimmer"),
        n("der", "Teller", "Teller"), n("die", "Kerze", "Kerzen"), n("das", "Tier", "Tiere"),
        n("der", "Wurm", "Würmer"), n("die", "Brille", "Brillen"), n("das", "Dach", "Dächer"),
        n("der", "Hut", "Hüte"), n("die", "Lampe", "Lampen"), n("der", "Finger", "Finger"),
        n("die", "Frau", "Frauen"), n("der", "Mann", "Männer"),
    )

    val adjektive = listOf(
        "groß", "klein", "schnell", "langsam", "laut", "leise", "rot", "blau", "schön", "lustig",
        "traurig", "warm", "kalt", "hell", "dunkel", "nett", "müde", "stark", "weich", "hart",
        "alt", "neu", "jung", "süß", "sauer", "nass", "trocken", "frech", "mutig", "wild",
    )

    /** er-Form im Präsens, im Präteritum, Partizip, Hilfsverb "hat" oder "ist" */
    class Verb(val infinitiv: String, val praesens: String, val praeteritum: String, val partizip: String, val hilfsverb: String) {
        /** true, wenn das Präteritum mit -te gebildet wird (spielte) */
        val schwach: Boolean get() = praeteritum.endsWith("te")
    }

    private fun v(i: String, p: String, t: String, pp: String, h: String) = Verb(i, p, t, pp, h)

    val verben: List<Verb> = listOf(
        v("spielen", "spielt", "spielte", "gespielt", "hat"),
        v("lachen", "lacht", "lachte", "gelacht", "hat"),
        v("malen", "malt", "malte", "gemalt", "hat"),
        v("kochen", "kocht", "kochte", "gekocht", "hat"),
        v("lernen", "lernt", "lernte", "gelernt", "hat"),
        v("tanzen", "tanzt", "tanzte", "getanzt", "hat"),
        v("bauen", "baut", "baute", "gebaut", "hat"),
        v("kaufen", "kauft", "kaufte", "gekauft", "hat"),
        v("fragen", "fragt", "fragte", "gefragt", "hat"),
        v("hüpfen", "hüpft", "hüpfte", "gehüpft", "ist"),
        v("gehen", "geht", "ging", "gegangen", "ist"),
        v("laufen", "läuft", "lief", "gelaufen", "ist"),
        v("fahren", "fährt", "fuhr", "gefahren", "ist"),
        v("springen", "springt", "sprang", "gesprungen", "ist"),
        v("kommen", "kommt", "kam", "gekommen", "ist"),
        v("fliegen", "fliegt", "flog", "geflogen", "ist"),
        v("fallen", "fällt", "fiel", "gefallen", "ist"),
        v("schlafen", "schläft", "schlief", "geschlafen", "hat"),
        v("essen", "isst", "aß", "gegessen", "hat"),
        v("trinken", "trinkt", "trank", "getrunken", "hat"),
        v("lesen", "liest", "las", "gelesen", "hat"),
        v("schreiben", "schreibt", "schrieb", "geschrieben", "hat"),
        v("sehen", "sieht", "sah", "gesehen", "hat"),
        v("singen", "singt", "sang", "gesungen", "hat"),
        v("finden", "findet", "fand", "gefunden", "hat"),
        v("rufen", "ruft", "rief", "gerufen", "hat"),
        v("helfen", "hilft", "half", "geholfen", "hat"),
        v("geben", "gibt", "gab", "gegeben", "hat"),
        v("nehmen", "nimmt", "nahm", "genommen", "hat"),
        v("werfen", "wirft", "warf", "geworfen", "hat"),
        v("bringen", "bringt", "brachte", "gebracht", "hat"),
        v("denken", "denkt", "dachte", "gedacht", "hat"),
    )

    class Verlaenger(val luecke: String, val richtig: String, val falsch: String, val lang: String)

    private fun vl(l: String, r: String, f: String, lang: String) = Verlaenger(l, r, f, lang)

    /** Wort mit Lücke, richtiger Buchstabe, der andere Buchstabe, verlängerte Form */
    val verlaengerWoerter: List<Verlaenger> = listOf(
        vl("Hun?", "d", "t", "die Hunde"), vl("Kin?", "d", "t", "die Kinder"), vl("Lan?", "d", "t", "die Länder"),
        vl("Han?", "d", "t", "die Hände"), vl("Wal?", "d", "t", "die Wälder"),
        vl("Bil?", "d", "t", "die Bilder"), vl("Mun?", "d", "t", "die Münder"), vl("Fel?", "d", "t", "die Felder"),
        vl("Freun?", "d", "t", "die Freunde"), vl("Pfer?", "d", "t", "die Pferde"), vl("Klei?", "d", "t", "die Kleider"),
        vl("Hu?", "t", "d", "die Hüte"), vl("Bro?", "t", "d", "die Brote"), vl("Zel?", "t", "d", "die Zelte"),
        vl("Hef?", "t", "d", "die Hefte"), vl("Gas?", "t", "d", "die Gäste"), vl("Stif?", "t", "d", "die Stifte"),
        vl("Nes?", "t", "d", "die Nester"),
        vl("Ber?", "g", "k", "die Berge"), vl("Zwer?", "g", "k", "die Zwerge"), vl("Zu?", "g", "k", "die Züge"),
        vl("Ta?", "g", "k", "die Tage"), vl("We?", "g", "k", "die Wege"), vl("Flu?", "g", "k", "die Flüge"),
        vl("Schran?", "k", "g", "die Schränke"), vl("Geschen?", "k", "g", "die Geschenke"),
        vl("Kor?", "b", "p", "die Körbe"), vl("Die?", "b", "p", "die Diebe"), vl("Kal?", "b", "p", "die Kälber"),
        vl("Sie?", "b", "p", "die Siebe"),
        vl("run?", "d", "t", "runde"), vl("wil?", "d", "t", "wilde"), vl("gesun?", "d", "t", "gesunde"),
        vl("lau?", "t", "d", "laute"), vl("bun?", "t", "d", "bunte"), vl("klu?", "g", "k", "kluge"),
        vl("star?", "k", "g", "starke"), vl("gel?", "b", "p", "gelbe"),
    )

    /** Wort mit Lücke, richtig "ie" oder "i", ganzes Wort */
    val ieWoerter: List<Triple<String, String, String>> = listOf(
        Triple("B?ne", "ie", "Biene"), Triple("T?r", "ie", "Tier"), Triple("Sp?l", "ie", "Spiel"),
        Triple("W?se", "ie", "Wiese"), Triple("Br?f", "ie", "Brief"), Triple("R?se", "ie", "Riese"),
        Triple("Z?ge", "ie", "Ziege"), Triple("fl?gen", "ie", "fliegen"), Triple("s?ben", "ie", "sieben"),
        Triple("l?b", "ie", "lieb"), Triple("D?b", "ie", "Dieb"),
        Triple("K?nd", "i", "Kind"), Triple("F?sch", "i", "Fisch"), Triple("T?sch", "i", "Tisch"),
        Triple("W?nd", "i", "Wind"), Triple("R?ng", "i", "Ring"), Triple("K?ste", "i", "Kiste"),
        Triple("M?lch", "i", "Milch"), Triple("B?ld", "i", "Bild"), Triple("L?ppe", "i", "Lippe"),
        Triple("F?lm", "i", "Film"), Triple("S?lber", "i", "Silber"), Triple("K?rsche", "i", "Kirsche"),
    )

    class FallSatz(val satz: String, val teil: String, val fall: Int)

    private fun fs(s: String, t: String, f: Int) = FallSatz(s, t, f)

    /** Satz, markierter Teil, Fall 1 bis 4 */
    val fallSaetze: List<FallSatz> = listOf(
        fs("Der Hund bellt laut.", "Der Hund", 1), fs("Der Lehrer erklärt die Aufgabe.", "Der Lehrer", 1),
        fs("Mein Bruder spielt Fußball.", "Mein Bruder", 1), fs("Der Ball rollt ins Tor.", "Der Ball", 1),
        fs("Der Vogel singt im Baum.", "Der Vogel", 1), fs("Am Morgen scheint die Sonne.", "die Sonne", 1),
        fs("Das Fell des Hundes ist weich.", "des Hundes", 2), fs("Die Tasche des Lehrers ist schwer.", "des Lehrers", 2),
        fs("Das Dach des Hauses ist rot.", "des Hauses", 2), fs("Die Farbe des Balles ist blau.", "des Balles", 2),
        fs("Der Schwanz der Katze ist lang.", "der Katze", 2),
        fs("Ich helfe dem Opa.", "dem Opa", 3), fs("Lena gibt dem Hund einen Knochen.", "dem Hund", 3),
        fs("Wir danken dem Lehrer.", "dem Lehrer", 3), fs("Tom schenkt seiner Schwester ein Buch.", "seiner Schwester", 3),
        fs("Das Fahrrad gehört dem Nachbarn.", "dem Nachbarn", 3), fs("Mia winkt ihrer Freundin.", "ihrer Freundin", 3),
        fs("Ich sehe den Vogel.", "den Vogel", 4), fs("Ben wirft den Ball.", "den Ball", 4),
        fs("Wir besuchen den Zoo.", "den Zoo", 4), fs("Die Katze fängt eine Maus.", "eine Maus", 4),
        fs("Lena liest ein Buch.", "ein Buch", 4), fs("Mama kauft einen Kuchen.", "einen Kuchen", 4),
        fs("Der Hund sucht seinen Knochen.", "seinen Knochen", 4),
    )

    val fallNamen: Map<Int, String> = mapOf(
        1 to "1. Fall: Wer oder was?", 2 to "2. Fall: Wessen?",
        3 to "3. Fall: Wem?", 4 to "4. Fall: Wen oder was?",
    )

    /** Satz mit Lücke (___), richtig "das" oder "dass" */
    val dassSaetze: List<Pair<String, String>> = listOf(
        "Ich weiß, ___ du gut rechnen kannst." to "dass", "Mama sagt, ___ wir bald essen." to "dass",
        "Ich hoffe, ___ es morgen schneit." to "dass", "Tom freut sich, ___ die Ferien beginnen." to "dass",
        "Es ist schön, ___ du da bist." to "dass", "Ich glaube, ___ der Bus gleich kommt." to "dass",
        "Wir wissen, ___ Katzen gern schlafen." to "dass", "Lena merkt, ___ es regnet." to "dass",
        "Ben findet, ___ Mathe Spaß macht." to "dass", "Papa erzählt, ___ er früher Fußball gespielt hat." to "dass",
        "Ich lese ___ Buch über Dinos." to "das", "Wo ist ___ Fahrrad von Ali?" to "das",
        "Siehst du ___ Haus dort drüben?" to "das", "Mia hat ein Bild, ___ sie selbst gemalt hat." to "das",
        "Ich mag ___ Lied sehr." to "das", "Das Spiel, ___ wir spielen, ist lustig." to "das",
        "Gib mir bitte ___ Heft." to "das", "Wir füttern ___ Kaninchen." to "das",
        "Das Mädchen, ___ dort steht, heißt Ida." to "das", "Ich finde ___ Wetter heute toll." to "das",
    )

    private fun z(r: IntRange) = Lern.z(r)

    /** Eine Aufgabe zum Antippen mit festen Knöpfen. */
    private fun wahlFest(frage: String, rechnung: String, antwort: String, optionen: List<String>, erklaerung: String) =
        LernAufgabe(art = "wahl", frage = frage, rechnung = rechnung, antwort = antwort, erklaerung = erklaerung, optionen = optionen)

    // Klasse 1

    fun anlaut(): LernAufgabe {
        val (bild, wort) = Lern.eins(anlaute)
        val richtig = wort.take(1)
        val andere = anlaute.map { it.second.take(1) }
        return Lern.wahl(
            frage = "Mit welchem Buchstaben fängt das Wort an?", richtig = richtig,
            falsch = andere, bild = "wort:$bild|$wort",
            erklaerung = "$wort fängt mit $richtig an.",
        )
    }

    fun silbenZahl(w: String): Int = w.split("-").size

    fun silben(): LernAufgabe {
        val w = Lern.eins(silbenWoerter)
        val ganz = w.replace("-", "")
        val n = silbenZahl(w)
        return Lern.zahl(
            "Wie viele Silben hat das Wort? Klatsche mit!", ganz, n,
            erklaerung = "$w: $n ${if (n == 1) "Silbe" else "Silben"}",
        )
    }

    /** Wörter reimen sich hier, wenn die letzten drei Buchstaben gleich sind. */
    fun reimtSich(a: String, b: String): Boolean = a.lowercase().takeLast(3) == b.lowercase().takeLast(3)

    fun reim(): LernAufgabe {
        val (a, b) = Lern.eins(reimPaare)
        val falsch = reimPaare.flatMap { listOf(it.first, it.second) }.filter { it != a && it != b && !reimtSich(it, a) }
        return Lern.wahl(
            frage = "Was reimt sich auf das Wort?", rechnung = a, richtig = b, falsch = falsch,
            erklaerung = "$a und $b reimen sich.",
        )
    }

    // Klasse 2

    fun artikel(): LernAufgabe {
        val n = Lern.eins(nomen)
        return wahlFest(
            "Welcher Artikel passt?", "? ${n.wort}", n.artikel, listOf("der", "die", "das"), "${n.artikel} ${n.wort}",
        )
    }

    fun umlaut(w: String): String {
        // Bei eu, ei und ie gibt es keinen sinnvollen Umlaut
        if (w.contains("eu") || w.contains("ei") || w.contains("ie")) return w
        for ((alt, neu) in listOf("au" to "äu", "a" to "ä", "o" to "ö", "u" to "ü")) {
            val i = w.lastIndexOf(alt)
            if (i >= 0) return w.substring(0, i) + neu + w.substring(i + alt.length)
        }
        return w
    }

    /** Falsche Mehrzahlformen, die es nicht gibt */
    fun falscheMehrzahl(n: Nomen): List<String> {
        val w = n.wort
        val u = umlaut(w)
        val kandidaten = listOf(
            w + "e", w + "er", w + "s", u + "e", u + "er",
            if (w.endsWith("e")) w + "n" else w + "en", w,
        )
        return kandidaten.filter { it != n.mehrzahl && !it.contains("ee") }
    }

    fun mehrzahl(): LernAufgabe {
        val n = Lern.eins(nomen)
        return Lern.wahl(
            frage = "Wie heißt die Mehrzahl?", rechnung = "${n.artikel} ${n.wort}, die ?",
            richtig = n.mehrzahl, falsch = falscheMehrzahl(n),
            erklaerung = "${n.artikel} ${n.wort}, die ${n.mehrzahl}",
        )
    }

    val alphabet: List<String> = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".map { it.toString() }

    fun abc(): LernAufgabe {
        if (Lern.wahr(0.5)) {
            val i = z(1..24)
            val nach = Lern.wahr(0.5)
            val richtig = alphabet[if (nach) i + 1 else i - 1]
            val falsch = alphabet.filter { it != richtig && it != alphabet[i] }
            return Lern.wahl(
                frage = if (nach) "Welcher Buchstabe kommt danach?" else "Welcher Buchstabe kommt davor?",
                rechnung = if (nach) "${alphabet[i]}, ?" else "?, ${alphabet[i]}",
                richtig = richtig, falsch = falsch,
                erklaerung = if (nach) "Nach ${alphabet[i]} kommt $richtig." else "Vor ${alphabet[i]} kommt $richtig.",
            )
        }
        // Drei Wörter mit verschiedenen Anfangsbuchstaben: welches steht zuerst im ABC?
        val woerter = mutableListOf<String>()
        for (n in nomen.shuffled(Lern.zufall)) {
            if (woerter.size < 3 && woerter.none { it.take(1) == n.wort.take(1) }) woerter += n.wort
        }
        val erstes = woerter.minByOrNull { abcIndex(it) } ?: woerter[0]
        return LernAufgabe(
            art = "wahl", frage = "Welches Wort steht im ABC zuerst?",
            antwort = erstes, erklaerung = "$erstes fängt mit ${erstes.take(1)} an. Das kommt im ABC zuerst.",
            optionen = woerter.shuffled(Lern.zufall),
        )
    }

    /** Stelle des ersten Buchstabens im ABC. Ä zählt wie A. */
    fun abcIndex(w: String): Int {
        val erster = w.take(1).uppercase().replace("Ä", "A").replace("Ö", "O").replace("Ü", "U")
        val i = alphabet.indexOf(erster)
        return if (i < 0) 99 else i
    }

    fun gross(): LernAufgabe {
        val n = Lern.eins(nomen)
        val andere = (adjektive + verben.map { it.infinitiv }).shuffled(Lern.zufall).take(2)
        val optionen = (listOf(n.wort.lowercase()) + andere).shuffled(Lern.zufall)
        return LernAufgabe(
            art = "wahl", frage = "Ein Wort schreibt man groß. Welches?",
            antwort = n.wort.lowercase(),
            erklaerung = "${n.wort} ist ein Nomen: ${n.artikel} ${n.wort}. Nomen schreibt man groß.",
            optionen = optionen,
        )
    }

    // Klasse 3

    fun wortart(): LernAufgabe {
        val optionen = listOf("Nomen", "Verb", "Adjektiv")
        return when (z(0..2)) {
            0 -> {
                val n = Lern.eins(nomen)
                wahlFest(
                    "Welche Wortart ist das?", n.wort, "Nomen", optionen,
                    "${n.artikel} ${n.wort}: ein Nomen, mit Artikel und groß geschrieben.",
                )
            }
            1 -> {
                val v = Lern.eins(verben)
                wahlFest(
                    "Welche Wortart ist das?", v.infinitiv, "Verb", optionen,
                    "${v.infinitiv}: ein Verb. Man kann es tun: er ${v.praesens}.",
                )
            }
            else -> {
                val a = Lern.eins(adjektive)
                wahlFest("Welche Wortart ist das?", a, "Adjektiv", optionen, "$a: ein Adjektiv. Es sagt, wie etwas ist.")
            }
        }
    }

    /** Ein Präteritum mit -te, das es bei starken Verben nicht gibt: gehte */
    fun falschSchwach(v: Verb): String = v.infinitiv.dropLast(2) + "te"

    fun vergangenheit(): LernAufgabe {
        val v = Lern.eins(verben)
        val falsch = if (v.schwach) listOf(v.praesens, v.partizip) else listOf(v.praesens, falschSchwach(v), v.partizip)
        return Lern.wahl(
            frage = "Wie heißt die Vergangenheit?",
            rechnung = "Heute: er ${v.praesens}.\nGestern: er ?",
            richtig = v.praeteritum, falsch = falsch,
            erklaerung = "Heute er ${v.praesens}, gestern er ${v.praeteritum}.",
        )
    }

    fun verlaengern(): LernAufgabe {
        val w = Lern.eins(verlaengerWoerter)
        val ganz = w.luecke.replace("?", w.richtig)
        return LernAufgabe(
            art = "wahl", frage = "Welcher Buchstabe fehlt? Verlängere das Wort.",
            rechnung = w.luecke, antwort = w.richtig,
            erklaerung = "${w.lang}, also $ganz.",
            optionen = listOf(w.richtig, w.falsch).shuffled(Lern.zufall),
        )
    }

    fun ieOderI(): LernAufgabe {
        val (luecke, richtig, ganz) = Lern.eins(ieWoerter)
        return wahlFest(
            "i oder ie?", luecke, richtig, listOf("i", "ie"),
            if (richtig == "ie") "$ganz: das i klingt lang." else "$ganz: das i klingt kurz.",
        )
    }

    // Klasse 4

    fun faelle(): LernAufgabe {
        val f = Lern.eins(fallSaetze)
        val optionen = listOf(1, 2, 3, 4).mapNotNull { fallNamen[it] }
        val richtig = fallNamen[f.fall] ?: ""
        return wahlFest("In welchem Fall steht „${f.teil}“?", f.satz, richtig, optionen, "„${f.teil}“ steht im $richtig")
    }

    fun dasDass(): LernAufgabe {
        val (satz, richtig) = Lern.eins(dassSaetze)
        val ganz = satz.replace("___", richtig)
        return wahlFest(
            "das oder dass?", satz, richtig, listOf("das", "dass"),
            if (richtig == "das") "$ganz Hier passt auch dieses oder welches." else "$ganz Dieses oder welches passt hier nicht.",
        )
    }

    /** wir-Form im Präteritum: ging wird gingen, spielte wird spielten */
    fun wirPraeteritum(v: Verb): String = if (v.praeteritum.endsWith("e")) v.praeteritum + "n" else v.praeteritum + "en"

    fun zeitform(): LernAufgabe {
        val v = Lern.eins(verben)
        val wir = Lern.wahr(0.5)
        val zeit = z(0..3)
        val satz: String = if (wir) {
            val hilf = if (v.hilfsverb == "ist") "sind" else "haben"
            listOf(
                "Wir ${v.infinitiv}.", "Wir ${wirPraeteritum(v)}.",
                "Wir $hilf ${v.partizip}.", "Wir werden ${v.infinitiv}.",
            )[zeit]
        } else {
            listOf(
                "Er ${v.praesens}.", "Er ${v.praeteritum}.",
                "Er ${v.hilfsverb} ${v.partizip}.", "Er wird ${v.infinitiv}.",
            )[zeit]
        }
        val namen = listOf("Präsens (Gegenwart)", "Präteritum (Vergangenheit)", "Perfekt (Vergangenheit)", "Futur (Zukunft)")
        return wahlFest("In welcher Zeitform steht der Satz?", satz, namen[zeit], namen, "„$satz“ steht im ${namen[zeit]}.")
    }
}
