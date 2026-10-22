package de.cemaras.yem1n.logik.lehrplan

import de.cemaras.yem1n.logik.LehrFach
import de.cemaras.yem1n.logik.Lern
import de.cemaras.yem1n.logik.LernAufgabe
import de.cemaras.yem1n.logik.LernThema

// Lernwelt Deutsch, weitere Themen. Port von LehrplanDeutschMehr.swift.
//
// Klasse 1: Wörter lesen, fehlende Buchstaben, große und kleine Buchstaben
// Klasse 2: Satzzeichen, Gegenteile, doppelte Mitlaute, zusammengesetzte Nomen
// Klasse 3: Satzarten, ä und äu ableiten, s, ss oder ß, Wortfamilien, Wortfelder, Steigern
// Klasse 4: Subjekt und Prädikat, Pronomen, Wörter trennen, Wortarten
// Jede Liste ist so gewählt, dass es genau eine richtige Antwort gibt.
// Wörter, bei denen auch eine andere Schreibung ein echtes Wort wäre (Wende und Wände,
// Rassen und Rasen, Geste und Gäste), stehen bewusst nicht drin.

object LehrplanDeutschMehr {
    private val D = LehrFach.DEUTSCH
    private fun z(r: IntRange) = Lern.z(r)

    val themen: List<LernThema> by lazy {
        listOf(
            LernThema("d1-lesen", D, "Wörter lesen", "👀", "Welches Bild passt zum Wort?", 1, erzeuge = ::woerterLesen),
            LernThema(
                "d1-luecke", D, "Welcher Buchstabe fehlt?", "🔍", "a, e, i, o oder u?", 1,
                tipp = "Hör dir das Wort an und sprich es langsam mit.", erzeuge = ::vokalLuecke,
            ),
            LernThema("d1-buchstaben", D, "Groß und klein", "🔡", "A und a gehören zusammen", 1, erzeuge = ::buchstabenPaare),
            LernThema(
                "d2-satzzeichen", D, "Punkt, Fragezeichen, Ausrufezeichen", "❗", "Welches Zeichen kommt ans Ende?", 2,
                tipp = "Fragen enden mit ?, Ausrufe und Aufforderungen mit !, alle anderen Sätze mit einem Punkt.",
                erzeuge = ::satzzeichen,
            ),
            LernThema("d2-gegenteil", D, "Gegenteile", "🔄", "groß und klein, hell und dunkel", 2, erzeuge = ::gegenteil),
            LernThema(
                "d2-doppelt", D, "Doppelte Mitlaute", "👥", "Sonne mit nn, Hose mit s", 2,
                tipp = "Klingt der Selbstlaut davor kurz, kommt oft ein doppelter Mitlaut: Sonne. Klingt er lang, nur einer: Hose.",
                erzeuge = ::doppelt,
            ),
            LernThema(
                "d2-zusammen", D, "Zusammengesetzte Nomen", "🔗", "Haus + Tür = Haustür", 2,
                tipp = "Das letzte Wort bestimmt den Artikel: die Tür, also die Haustür.", erzeuge = ::zusammengesetzt,
            ),
            LernThema("d3-satzarten", D, "Satzarten", "💬", "Aussage, Frage oder Aufforderung?", 3, erzeuge = ::satzart),
            LernThema(
                "d3-ableiten", D, "ä oder e, äu oder eu?", "🌳", "Ableiten: Baum, also Bäume", 3,
                tipp = "Suche ein verwandtes Wort mit a oder au: Hände kommt von Hand.", erzeuge = ::ableiten,
            ),
            LernThema(
                "d3-sss", D, "s, ss oder ß?", "🐍", "Fuß, Fluss und Hase", 3,
                tipp = "Nach kurzem Selbstlaut ss: Fluss. Nach langem Selbstlaut oder ei, au ß: Fuß, heiß. Weich gesprochen s: Hase.",
                erzeuge = ::sss,
            ),
            LernThema(
                "d3-wortfamilie", D, "Wortfamilien", "👨‍👩‍👧‍👦", "Welches Wort gehört nicht dazu?", 3,
                tipp = "Wörter einer Familie haben den gleichen Wortstamm: fahren, Fahrer, Fahrrad.", erzeuge = ::wortfamilie,
            ),
            LernThema("d3-wortfeld", D, "Wortfelder", "🌾", "Andere Wörter für sagen, gehen, sehen", 3, erzeuge = ::wortfeld),
            LernThema("d3-steigern", D, "Adjektive steigern", "📈", "groß, größer, am größten", 3, erzeuge = ::steigern),
            LernThema(
                "d4-satzglieder", D, "Subjekt und Prädikat", "🧱", "Wer tut etwas? Was tut er?", 4,
                tipp = "Das Subjekt findest du mit Wer oder was?, das Prädikat mit Was tut er?", erzeuge = ::satzglied,
            ),
            LernThema("d4-pronomen", D, "Pronomen", "👉", "er, sie, es, wir statt Nomen", 4, erzeuge = ::pronomen),
            LernThema(
                "d4-trennen", D, "Wörter trennen", "✂️", "Nach Silben trennen", 4,
                tipp = "Trenne nach Sprechsilben. ck bleibt zusammen: Zu-cker. Zwei gleiche Mitlaute werden getrennt: Was-ser.",
                erzeuge = ::trennen,
            ),
            LernThema("d4-wortarten", D, "Alle Wortarten", "🗂️", "Auch Artikel, Pronomen und Präpositionen", 4, erzeuge = ::wortartenAlle),
        )
    }

    private fun wahlFest(frage: String, rechnung: String, antwort: String, optionen: List<String>, erklaerung: String) =
        LernAufgabe(art = "wahl", frage = frage, rechnung = rechnung, antwort = antwort, erklaerung = erklaerung, optionen = optionen)

    // Klasse 1

    fun woerterLesen(): LernAufgabe {
        val (bild, wort) = Lern.eins(LehrplanDeutsch.anlaute)
        return Lern.wahl(
            frage = "Lies das Wort. Welches Bild passt?", rechnung = wort, richtig = bild,
            falsch = LehrplanDeutsch.anlaute.map { it.first }, erklaerung = "$wort: $bild",
        )
    }

    val vokale: List<Char> = listOf('a', 'e', 'i', 'o', 'u')

    /** Stellen im Wort mit einem einzelnen Selbstlaut, nicht ei, au, ie oder Ähnliches */
    fun vokalStellen(wort: String): List<Int> {
        val zeichen = wort.toList()
        val stellen = mutableListOf<Int>()
        for (i in 1 until zeichen.size) {
            if (zeichen[i] !in vokale) continue
            val davor = i > 0 && zeichen[i - 1].lowercaseChar() in vokale
            val danach = i + 1 < zeichen.size && zeichen[i + 1] in vokale
            if (!davor && !danach) stellen += i
        }
        return stellen
    }

    fun vokalLuecke(): LernAufgabe {
        var eintrag = Lern.eins(LehrplanDeutsch.anlaute)
        while (vokalStellen(eintrag.second).isEmpty()) eintrag = Lern.eins(LehrplanDeutsch.anlaute)
        val (bild, wort) = eintrag
        val i = Lern.eins(vokalStellen(wort))
        val zeichen = wort.toCharArray()
        val richtig = zeichen[i].toString()
        zeichen[i] = '?'
        return Lern.wahl(
            frage = "Welcher Buchstabe fehlt?", rechnung = String(zeichen), richtig = richtig,
            falsch = vokale.map { it.toString() }, bild = "wort:$bild|$wort",
            erklaerung = "Richtig geschrieben: $wort",
        )
    }

    /** Buchstaben, die leicht verwechselt werden */
    val aehnlich: Map<String, List<String>> = mapOf(
        "a" to listOf("o", "e"), "b" to listOf("d", "p"), "c" to listOf("e", "o"), "d" to listOf("b", "q"),
        "e" to listOf("a", "c"), "f" to listOf("t", "k"), "g" to listOf("q", "j"), "h" to listOf("n", "k"),
        "i" to listOf("l", "j"), "j" to listOf("i", "g"), "k" to listOf("h", "x"), "l" to listOf("i", "t"),
        "m" to listOf("n", "w"), "n" to listOf("m", "h"), "o" to listOf("a", "c"), "p" to listOf("q", "b"),
        "q" to listOf("p", "g"), "r" to listOf("n", "v"), "s" to listOf("z", "c"), "t" to listOf("f", "l"),
        "u" to listOf("n", "v"), "v" to listOf("w", "u"), "w" to listOf("m", "v"), "x" to listOf("k", "z"),
        "y" to listOf("v", "j"), "z" to listOf("s", "x"),
    )

    fun buchstabenPaare(): LernAufgabe {
        val klein = Lern.eins(aehnlich.keys.toList())
        val gross = klein.uppercase()
        val falsch = aehnlich[klein] ?: emptyList()
        if (Lern.wahr(0.5)) {
            return Lern.wahl(
                frage = "Welcher kleine Buchstabe gehört zu diesem großen?", rechnung = gross,
                richtig = klein, falsch = falsch, erklaerung = "$gross und $klein gehören zusammen.",
            )
        }
        return Lern.wahl(
            frage = "Welcher große Buchstabe gehört zu diesem kleinen?", rechnung = klein,
            richtig = gross, falsch = falsch.map { it.uppercase() },
            erklaerung = "$klein und $gross gehören zusammen.",
        )
    }

    // Klasse 2

    val saetzeOhneZeichen: List<Pair<String, String>> = listOf(
        "Wie heißt du" to "?", "Ich gehe in die Schule" to ".", "Pass auf" to "!", "Hast du einen Hund" to "?",
        "Der Ball ist rot" to ".", "Komm schnell her" to "!", "Wo wohnst du" to "?", "Heute scheint die Sonne" to ".",
        "Magst du Eis" to "?", "Mia malt ein Bild" to ".", "Sei bitte leise" to "!", "Wann beginnt die Pause" to "?",
        "Mein Bruder ist sieben Jahre alt" to ".", "Kannst du schwimmen" to "?", "Wir spielen im Garten" to ".",
        "Räum bitte dein Zimmer auf" to "!", "Was isst du gern" to "?", "Die Katze schläft auf dem Sofa" to ".",
        "Halt, stopp" to "!", "Gib mir bitte den Stift" to "!",
    )

    fun satzzeichen(): LernAufgabe {
        val (satz, zeichen) = Lern.eins(saetzeOhneZeichen)
        val namen = mapOf("." to "Punkt", "?" to "Fragezeichen", "!" to "Ausrufezeichen")
        return wahlFest(
            "Welches Zeichen gehört ans Ende?", "$satz ___", zeichen, listOf(".", "?", "!"),
            "$satz$zeichen Hier gehört ein ${namen[zeichen] ?: ""} hin.",
        )
    }

    val gegenteile: List<Pair<String, String>> = listOf(
        "groß" to "klein", "hell" to "dunkel", "laut" to "leise", "schnell" to "langsam", "warm" to "kalt",
        "nass" to "trocken", "hart" to "weich", "dick" to "dünn", "lang" to "kurz", "voll" to "leer",
        "schwer" to "leicht", "süß" to "sauer", "früh" to "spät", "oben" to "unten", "fröhlich" to "traurig",
        "viel" to "wenig", "hoch" to "tief", "sauber" to "schmutzig",
    )

    fun gegenteil(): LernAufgabe {
        val p = Lern.eins(gegenteile)
        val umgekehrt = Lern.wahr(0.5)
        val wort = if (umgekehrt) p.second else p.first
        val richtig = if (umgekehrt) p.first else p.second
        val falsch = gegenteile.flatMap { listOf(it.first, it.second) }.filter { it != wort && it != richtig }
        return Lern.wahl(
            frage = "Was ist das Gegenteil?", rechnung = wort, richtig = richtig, falsch = falsch,
            erklaerung = "Das Gegenteil von $wort ist $richtig.",
        )
    }

    /** Lückenwort, einfacher Mitlaut, doppelter Mitlaut, richtig, ganzes Wort */
    class Doppelt(val luecke: String, val einfach: String, val doppel: String, val richtig: String, val ganz: String)

    private fun dp(l: String, e: String, d: String, r: String, g: String) = Doppelt(l, e, d, r, g)

    val doppeltWoerter: List<Doppelt> = listOf(
        dp("So?e", "n", "nn", "nn", "Sonne"), dp("Ka?e", "n", "nn", "nn", "Kanne"), dp("Pu?e", "p", "pp", "pp", "Puppe"),
        dp("Su?e", "p", "pp", "pp", "Suppe"), dp("Te?er", "l", "ll", "ll", "Teller"), dp("Mu?er", "t", "tt", "tt", "Mutter"),
        dp("Bu?er", "t", "tt", "tt", "Butter"), dp("Wa?er", "s", "ss", "ss", "Wasser"), dp("Ta?e", "s", "ss", "ss", "Tasse"),
        dp("Hi?el", "m", "mm", "mm", "Himmel"), dp("Zi?er", "m", "mm", "mm", "Zimmer"), dp("Ko?er", "f", "ff", "ff", "Koffer"),
        dp("A?e", "f", "ff", "ff", "Affe"), dp("Bri?e", "l", "ll", "ll", "Brille"), dp("So?er", "m", "mm", "mm", "Sommer"),
        dp("Ha?e", "s", "ss", "s", "Hase"), dp("Ho?e", "s", "ss", "s", "Hose"), dp("Ta?el", "f", "ff", "f", "Tafel"),
        dp("O?en", "f", "ff", "f", "Ofen"), dp("To?ate", "m", "mm", "m", "Tomate"), dp("Ga?el", "b", "bb", "b", "Gabel"),
        dp("E?el", "s", "ss", "s", "Esel"), dp("Ki?o", "n", "nn", "n", "Kino"), dp("Na?e", "s", "ss", "s", "Nase"),
    )

    fun doppelt(): LernAufgabe {
        val w = Lern.eins(doppeltWoerter)
        val kurz = w.richtig == w.doppel
        return wahlFest(
            "Ein Mitlaut oder zwei?", w.luecke, w.richtig, listOf(w.einfach, w.doppel),
            if (kurz) "${w.ganz}: Der Selbstlaut davor klingt kurz." else "${w.ganz}: Der Selbstlaut davor klingt lang.",
        )
    }

    /** Erstes Wort, zweites Wort, zusammengesetztes Wort, Artikel */
    class Kompositum(val a: String, val b: String, val ganz: String, val artikel: String)

    private fun kp(a: String, b: String, g: String, art: String) = Kompositum(a, b, g, art)

    val komposita: List<Kompositum> = listOf(
        kp("Haus", "Tür", "Haustür", "die"), kp("Schnee", "Mann", "Schneemann", "der"), kp("Apfel", "Baum", "Apfelbaum", "der"),
        kp("Sonne", "Blume", "Sonnenblume", "die"), kp("Hand", "Schuh", "Handschuh", "der"), kp("Fuß", "Ball", "Fußball", "der"),
        kp("Zahn", "Bürste", "Zahnbürste", "die"), kp("Blume", "Topf", "Blumentopf", "der"), kp("Regen", "Schirm", "Regenschirm", "der"),
        kp("Bett", "Decke", "Bettdecke", "die"), kp("Wasser", "Glas", "Wasserglas", "das"), kp("Spiel", "Zeug", "Spielzeug", "das"),
        kp("Eis", "Bär", "Eisbär", "der"), kp("Tisch", "Decke", "Tischdecke", "die"), kp("Schule", "Tasche", "Schultasche", "die"),
        kp("Garten", "Haus", "Gartenhaus", "das"), kp("Kinder", "Zimmer", "Kinderzimmer", "das"), kp("Auto", "Bahn", "Autobahn", "die"),
        kp("Feuer", "Wehr", "Feuerwehr", "die"), kp("Buch", "Seite", "Buchseite", "die"),
    )

    fun zusammengesetzt(): LernAufgabe {
        val k = Lern.eins(komposita)
        return wahlFest(
            "Welcher Artikel passt zum neuen Wort?", "${k.a} + ${k.b} = ${k.ganz}", k.artikel, listOf("der", "die", "das"),
            "Das letzte Wort bestimmt den Artikel: ${k.artikel} ${k.b}, also ${k.artikel} ${k.ganz}.",
        )
    }

    // Klasse 3

    val satzartListe: List<Pair<String, String>> = listOf(
        "Der Hund schläft im Körbchen." to "Aussagesatz", "Heute regnet es." to "Aussagesatz",
        "Ich habe zwei Schwestern." to "Aussagesatz", "Im Sommer fahren wir ans Meer." to "Aussagesatz",
        "Wann beginnt die Pause?" to "Fragesatz", "Hast du deine Hausaufgaben gemacht?" to "Fragesatz",
        "Wie spät ist es?" to "Fragesatz", "Kommst du morgen mit?" to "Fragesatz",
        "Komm bitte her!" to "Aufforderungssatz", "Mach das Fenster zu!" to "Aufforderungssatz",
        "Hör mir bitte zu!" to "Aufforderungssatz", "Räum deine Sachen auf!" to "Aufforderungssatz",
        "Wo ist mein Turnbeutel?" to "Fragesatz", "Die Blumen blühen schon." to "Aussagesatz",
        "Gib mir bitte das Salz!" to "Aufforderungssatz",
    )

    fun satzart(): LernAufgabe {
        val (satz, art) = Lern.eins(satzartListe)
        return wahlFest(
            "Welche Satzart ist das?", satz, art, listOf("Aussagesatz", "Fragesatz", "Aufforderungssatz"),
            "„$satz“ ist ein $art.",
        )
    }

    /** Lückenwort, richtig, falsch, verwandtes Wort oder Hinweis */
    class Ableit(val luecke: String, val richtig: String, val falsch: String, val hilfe: String)

    private fun ab(l: String, r: String, f: String, h: String) = Ableit(l, r, f, h)

    private const val KEIN_AU = "Es gibt kein verwandtes Wort mit au."
    private const val KEIN_A = "Es gibt kein verwandtes Wort mit a."

    val ableitWoerter: List<Ableit> = listOf(
        ab("B?me", "äu", "eu", "der Baum"), ab("H?ser", "äu", "eu", "das Haus"), ab("tr?men", "äu", "eu", "der Traum"),
        ab("M?se", "äu", "eu", "die Maus"), ab("Z?ne", "äu", "eu", "der Zaun"), ab("R?ber", "äu", "eu", "der Raub"),
        ab("h?te", "eu", "äu", KEIN_AU), ab("F?er", "eu", "äu", KEIN_AU), ab("Fr?nd", "eu", "äu", KEIN_AU),
        ab("t?er", "eu", "äu", KEIN_AU), ab("Kr?z", "eu", "äu", KEIN_AU),
        ab("H?nde", "ä", "e", "die Hand"), ab("z?hlen", "ä", "e", "die Zahl"), ab("B?cker", "ä", "e", "backen"),
        ab("Bl?tter", "ä", "e", "das Blatt"), ab("D?cher", "ä", "e", "das Dach"), ab("st?rker", "ä", "e", "stark"),
        ab("F?lder", "e", "ä", "das Feld"), ab("N?st", "e", "ä", KEIN_A),
        ab("B?tt", "e", "ä", KEIN_A), ab("l?ben", "e", "ä", KEIN_A),
    )

    fun ableiten(): LernAufgabe {
        val w = Lern.eins(ableitWoerter)
        val ganz = w.luecke.replace("?", w.richtig)
        val erklaerung = if (w.hilfe.startsWith("Es gibt")) "$ganz. ${w.hilfe} Darum ${w.richtig}." else "$ganz kommt von ${w.hilfe}."
        return LernAufgabe(
            art = "wahl", frage = "Welche Buchstaben fehlen?", rechnung = w.luecke, antwort = w.richtig,
            erklaerung = erklaerung, optionen = listOf(w.richtig, w.falsch).sorted(),
        )
    }

    /** Lückenwort, richtig, Begründung */
    val sssWoerter: List<Triple<String, String, String>> = listOf(
        Triple("Fu?", "ß", "der Fuß: das u klingt lang."), Triple("Flu?", "ss", "der Fluss: das u klingt kurz."),
        Triple("Ha?e", "s", "der Hase: das s klingt weich."), Triple("Gru?", "ß", "der Gruß: das u klingt lang."),
        Triple("Ku?", "ss", "der Kuss: das u klingt kurz."), Triple("Stra?e", "ß", "die Straße: das a klingt lang."),
        Triple("Wa?er", "ss", "das Wasser: das a klingt kurz."), Triple("Ta?e", "ss", "die Tasse: das a klingt kurz."),
        Triple("drau?en", "ß", "draußen: nach au kommt ß."), Triple("Spa?", "ß", "der Spaß: das a klingt lang."),
        Triple("Na?e", "s", "die Nase: das s klingt weich."), Triple("Me?er", "ss", "das Messer: das e klingt kurz."),
        Triple("Klö?e", "ß", "die Klöße: das ö klingt lang."), Triple("gro?", "ß", "groß: das o klingt lang."),
        Triple("Schlo?", "ss", "das Schloss: das o klingt kurz."), Triple("hei?", "ß", "heiß: nach ei kommt ß."),
        Triple("Glä?er", "s", "die Gläser: das s klingt weich."), Triple("wei?", "ß", "weiß: nach ei kommt ß."),
        Triple("Gra?", "s", "das Gras: verlängere zu die Gräser, dann hörst du ein weiches s."),
    )

    fun sss(): LernAufgabe {
        val (luecke, richtig, warum) = Lern.eins(sssWoerter)
        return wahlFest("s, ss oder ß?", luecke, richtig, listOf("s", "ss", "ß"), warum)
    }

    /** Stamm, drei Wörter der Familie */
    val familien: List<Pair<String, List<String>>> = listOf(
        "fahren" to listOf("Fahrrad", "Fahrer", "Fahrt"), "spielen" to listOf("Spielplatz", "Spieler", "Spielzeug"),
        "Haus" to listOf("Häuschen", "Hausaufgabe", "Haustür"), "backen" to listOf("Bäcker", "Backofen", "Gebäck"),
        "schlafen" to listOf("Schlafanzug", "schläfrig", "Schlafzimmer"), "lesen" to listOf("Leser", "Lesebuch", "Lesezeichen"),
        "fliegen" to listOf("Flieger", "Flugzeug", "Flughafen"), "trinken" to listOf("Getränk", "Trinkflasche", "Trinkhalm"),
        "rechnen" to listOf("Rechnung", "Rechenheft", "Rechner"), "malen" to listOf("Maler", "Malkasten", "Gemälde"),
    )

    fun wortfamilie(): LernAufgabe {
        val i = z(0..(familien.size - 1))
        var j = z(0..(familien.size - 1))
        while (j == i) j = z(0..(familien.size - 1))
        val (stamm, woerter) = familien[i]
        val fremd = Lern.eins(familien[j].second)
        return LernAufgabe(
            art = "wahl", frage = "Welches Wort gehört nicht zur Wortfamilie $stamm?",
            antwort = fremd,
            erklaerung = "$fremd gehört zu ${familien[j].first}. Die anderen kommen von $stamm.",
            optionen = (woerter + fremd).shuffled(Lern.zufall),
        )
    }

    val wortfelder: List<Pair<String, List<String>>> = listOf(
        "sagen" to listOf("flüstern", "rufen", "schreien", "erzählen", "antworten", "murmeln", "brüllen"),
        "gehen" to listOf("rennen", "schleichen", "hüpfen", "wandern", "spazieren", "stolpern", "schlendern"),
        "sehen" to listOf("schauen", "gucken", "beobachten", "betrachten", "starren", "blicken"),
    )

    fun wortfeld(): LernAufgabe {
        val i = z(0..2)
        val (feld, woerter) = wortfelder[i]
        val richtig = Lern.eins(woerter)
        val falsch = wortfelder.filterIndexed { k, _ -> k != i }.flatMap { it.second }
        return Lern.wahl(
            frage = "Welches Wort gehört zum Wortfeld $feld?", richtig = richtig, falsch = falsch,
            erklaerung = "$richtig ist eine Art zu $feld.",
        )
    }

    /** Grundform, Vergleichsform, Höchstform */
    val steigerung: List<Triple<String, String, String>> = listOf(
        Triple("klein", "kleiner", "am kleinsten"), Triple("groß", "größer", "am größten"),
        Triple("schnell", "schneller", "am schnellsten"), Triple("alt", "älter", "am ältesten"),
        Triple("jung", "jünger", "am jüngsten"), Triple("lang", "länger", "am längsten"),
        Triple("kurz", "kürzer", "am kürzesten"), Triple("warm", "wärmer", "am wärmsten"),
        Triple("kalt", "kälter", "am kältesten"), Triple("hoch", "höher", "am höchsten"),
        Triple("gut", "besser", "am besten"), Triple("viel", "mehr", "am meisten"),
        Triple("laut", "lauter", "am lautesten"), Triple("schön", "schöner", "am schönsten"),
        Triple("stark", "stärker", "am stärksten"), Triple("hell", "heller", "am hellsten"),
        Triple("süß", "süßer", "am süßesten"), Triple("nah", "näher", "am nächsten"),
    )

    fun steigern(): LernAufgabe {
        val (grund, mehr, meist) = Lern.eins(steigerung)
        if (Lern.wahr(0.5)) {
            return Lern.wahl(
                frage = "Wie heißt die Vergleichsform?", rechnung = "$grund, ?",
                richtig = mehr, falsch = listOf(grund + "er", "mehr $grund", grund + "ster"),
                erklaerung = "$grund, $mehr, $meist",
            )
        }
        return Lern.wahl(
            frage = "Wie heißt die Höchstform?", rechnung = "$grund, $mehr, ?",
            richtig = meist, falsch = listOf("am " + grund + "sten", "am " + mehr + "sten", "am meisten $grund"),
            erklaerung = "$grund, $mehr, $meist",
        )
    }

    // Klasse 4

    /** Satz, Subjekt, Prädikat. Das Subjekt ist hier immer ein einzelnes Wort. */
    val gliederSaetze: List<Triple<String, String, String>> = listOf(
        Triple("Ben spielt Fußball.", "Ben", "spielt"), Triple("Mia liest ein Buch.", "Mia", "liest"),
        Triple("Tom isst einen Apfel.", "Tom", "isst"), Triple("Wir singen ein Lied.", "Wir", "singen"),
        Triple("Lena malt ein Bild.", "Lena", "malt"), Triple("Ali baut eine Burg.", "Ali", "baut"),
        Triple("Jonas fährt Fahrrad.", "Jonas", "fährt"), Triple("Ida schreibt einen Brief.", "Ida", "schreibt"),
        Triple("Emil wirft den Ball.", "Emil", "wirft"), Triple("Nele kocht Suppe.", "Nele", "kocht"),
        Triple("Sara füttert die Hasen.", "Sara", "füttert"), Triple("Opa erzählt eine Geschichte.", "Opa", "erzählt"),
        Triple("Morgen besucht Jule ihre Oma.", "Jule", "besucht"), Triple("Im Garten spielt Luis.", "Luis", "spielt"),
    )

    fun woerter(satz: String): List<String> {
        val liste = mutableListOf<String>()
        for (roh in satz.split(" ")) {
            val w = roh.trim('.', '!', '?', ',')
            if (w.isNotEmpty() && w !in liste) liste += w
        }
        return liste
    }

    fun satzglied(): LernAufgabe {
        val (satz, subjekt, praedikat) = Lern.eins(gliederSaetze)
        val teile = woerter(satz)
        if (Lern.wahr(0.5)) {
            return LernAufgabe(
                art = "wahl", frage = "Welches Wort ist das Subjekt? Frag: Wer oder was?", rechnung = satz,
                antwort = subjekt, erklaerung = "Wer $praedikat? $subjekt. Das ist das Subjekt.", optionen = teile,
            )
        }
        return LernAufgabe(
            art = "wahl", frage = "Welches Wort ist das Prädikat? Frag: Was tut jemand?", rechnung = satz,
            antwort = praedikat, erklaerung = "Was tut $subjekt? $subjekt $praedikat. Das ist das Prädikat.", optionen = teile,
        )
    }

    val pronomenSaetze: List<Triple<String, String, String>> = listOf(
        Triple("Tom spielt.", "___ spielt Fußball.", "Er"), Triple("Das Pferd läuft.", "___ ist schnell.", "Es"),
        Triple("Mia und Ben lachen.", "___ sind froh.", "Sie"), Triple("Der Hund bellt.", "___ ist laut.", "Er"),
        Triple("Die Blume blüht.", "___ ist rot.", "Sie"), Triple("Das Baby schläft.", "___ ist müde.", "Es"),
        Triple("Ali und ich spielen.", "___ haben Spaß.", "Wir"), Triple("Die Kinder rennen.", "___ sind schnell.", "Sie"),
        Triple("Der Ball rollt.", "___ ist rund.", "Er"), Triple("Das Auto ist neu.", "___ glänzt.", "Es"),
        Triple("Oma backt.", "___ backt einen Kuchen.", "Sie"), Triple("Mein Bruder und ich lesen.", "___ lesen ein Buch.", "Wir"),
    )

    fun pronomen(): LernAufgabe {
        val (eins, zwei, richtig) = Lern.eins(pronomenSaetze)
        return wahlFest(
            "Welches Pronomen passt?", "$eins\n$zwei", richtig, listOf("Er", "Sie", "Es", "Wir"),
            "$eins ${zwei.replace("___", richtig)}",
        )
    }

    /** Richtig getrennt, mit Bindestrich */
    val trennWoerter = listOf(
        "Fens-ter", "Schwes-ter", "Ham-mer", "Kat-ze", "Zu-cker", "Was-ser", "Blu-men", "Kis-te", "Mon-tag",
        "Ta-fel", "Gar-ten", "Wol-ke", "Ap-fel", "Kin-der", "Te-le-fon", "Ba-na-ne", "Ka-rot-te", "Hand-schuh",
        "Pin-sel", "Tel-ler", "Fahr-rad", "Mut-ter",
    )

    /** Falsche Trennungen: ein Strich rutscht um einen Buchstaben nach links oder rechts */
    fun falscheTrennungen(richtig: String): List<String> {
        val wort = richtig.replace("-", "")
        val stellen = mutableListOf<Int>()
        var i = 0
        for (ch in richtig) {
            if (ch == '-') stellen += i else i++
        }
        val ergebnis = mutableListOf<String>()
        for ((k, s) in stellen.withIndex()) {
            for (d in listOf(-1, 1)) {
                val neu = stellen.toMutableList()
                neu[k] = s + d
                if (!neu.all { it >= 1 && it < wort.length } || neu.toSet().size != neu.size || neu != neu.sorted()) continue
                val zeichen = wort.toMutableList()
                for (p in neu.sortedDescending()) zeichen.add(p, '-')
                val text = zeichen.joinToString("")
                if (text != richtig && text !in ergebnis) ergebnis += text
            }
        }
        return ergebnis
    }

    fun trennen(): LernAufgabe {
        val richtig = Lern.eins(trennWoerter)
        val wort = richtig.replace("-", "")
        return Lern.wahl(
            frage = "Wie trennt man das Wort richtig?", rechnung = wort, richtig = richtig,
            falsch = falscheTrennungen(richtig), erklaerung = "Richtig getrennt: $richtig",
        )
    }

    val artikelWoerter = listOf("der", "die", "das", "ein", "eine")
    val pronomenWoerter = listOf("ich", "du", "er", "sie", "es", "wir", "ihr")
    val praepositionen = listOf("auf", "unter", "neben", "hinter", "vor", "in", "über", "zwischen", "mit")

    fun wortartenAlle(): LernAufgabe {
        val arten = listOf("Nomen", "Verb", "Adjektiv", "Artikel", "Pronomen", "Präposition")
        val wahl = z(0..5)
        val wort = when (wahl) {
            0 -> Lern.eins(LehrplanDeutsch.nomen).wort
            1 -> Lern.eins(LehrplanDeutsch.verben).infinitiv
            2 -> Lern.eins(LehrplanDeutsch.adjektive)
            3 -> Lern.eins(artikelWoerter)
            4 -> Lern.eins(pronomenWoerter)
            else -> Lern.eins(praepositionen)
        }
        // "die" und "sie" können auch anders gebraucht werden, hier sind sie eindeutig zugeordnet
        return Lern.wahl(
            frage = "Welche Wortart ist das?", rechnung = wort, richtig = arten[wahl], falsch = arten,
            anzahl = 4, erklaerung = "$wort ist ein${if (arten[wahl] == "Präposition") "e" else ""} ${arten[wahl]}.",
        )
    }
}
