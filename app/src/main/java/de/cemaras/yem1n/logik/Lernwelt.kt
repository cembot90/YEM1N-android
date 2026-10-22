package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.daten.Uebung
import de.cemaras.yem1n.logik.lehrplan.LehrplanMathe
import de.cemaras.yem1n.logik.lehrplan.LehrplanMatheMehr
import kotlin.random.Random

// Lernwelt: fertige Aufgaben nach dem Lehrplan. Port von Lehrplan.swift der iOS-App.
// Grundlage sind die Lehrpläne von Hessen, Bayern und Nordrhein-Westfalen.
// Jedes Thema erzeugt seine Aufgaben selbst, jedes Mal neue.

enum class Bundesland(val schluessel: String, val anzeige: String, val kurz: String, val lehrplan: String) {
    HESSEN("hessen", "Hessen", "Hessen", "Kerncurriculum Hessen"),
    BAYERN("bayern", "Bayern", "Bayern", "LehrplanPLUS Bayern"),
    NRW("nrw", "Nordrhein-Westfalen", "NRW", "Lehrplan NRW");

    companion object {
        /** Ohne Auswahl gilt Hessen. */
        fun aus(roh: String): Bundesland = entries.firstOrNull { it.schluessel == roh } ?: HESSEN
    }
}

enum class LehrFach(val anzeige: String, val emoji: String) {
    MATHE("Mathe", "🔢"),
    DEUTSCH("Deutsch", "📖"),
    ENGLISCH("Englisch", "🇬🇧"),
    MUSIK("Musik", "🎵"),
    KUNST("Kunst", "🎨"),
}

/** Eine Aufgabe, bevor sie in die Übung kommt. */
data class LernAufgabe(
    /** "zahl", "rest", "vergleich", "mauer", "text" oder "wahl" (Antwort antippen) */
    val art: String = "zahl",
    val frage: String,
    val rechnung: String = "",
    val antwort: String,
    val antwort2: String = "",
    val hinweis: String = "",
    val erklaerung: String = "",
    /** Bei "wahl": alle Antworten zum Antippen, die richtige ist dabei */
    val optionen: List<String> = emptyList(),
    /** Ein kleines Bild über der Aufgabe, siehe AufgabenBild */
    val bild: String = "",
    /** Nur bei "mauer": die Steine von unten nach oben */
    val reihen: List<List<Int>> = emptyList(),
) {
    /** Zwei Aufgaben mit gleichem Schlüssel gelten als dieselbe. */
    val schluessel: String
        get() = listOf(art, frage, rechnung, bild, antwort, reihen.joinToString(";") { it.joinToString(",") })
            .joinToString("|")

    fun zuAufgabe() = Aufgabe(
        art = art, frage = frage, rechnung = rechnung, hinweis = hinweis, erklaerung = erklaerung,
        antwort = antwort, antwort2 = antwort2, reihen = reihen, bild = bild, tr = "", gross = "",
        folge = emptyList(), optionen = optionen,
    )
}

/** Dieselbe Aufgabe mit einem Bild darüber, siehe AufgabenBild. */
fun LernAufgabe.mitBild(bild: String): LernAufgabe = copy(bild = bild)

class LernThema(
    val id: String,
    val fach: LehrFach,
    val titel: String,
    val emoji: String,
    val beschreibung: String,
    /** In dieser Klasse steht das Thema im Lehrplan */
    val klasse: Int,
    /** Länder, die das Thema in einer anderen Klasse haben */
    val abweichend: Map<Bundesland, Int> = emptyMap(),
    /** false bei Extras außerhalb des Lehrplans, etwa Englisch in Klasse 1 und 2 */
    val lehrplan: Boolean = true,
    val tipp: String = "",
    val erzeuge: () -> LernAufgabe,
) {
    fun klasse(land: Bundesland): Int = abweichend[land] ?: klasse
}

/**
 * Mathe und Deutsch in der Reihenfolge, in der die meisten Schulbücher sie bringen.
 * Das Schuljahr hat vier Abschnitte. 0 heißt: das ganze Jahr über, etwa Lesen.
 */
object Schuljahr {
    fun titel(abschnitt: Int): String = when (abschnitt) {
        1 -> "Nach den Sommerferien"
        2 -> "Bis Weihnachten"
        3 -> "Bis Ostern"
        4 -> "Bis zu den Sommerferien"
        else -> "Das ganze Jahr"
    }

    fun monate(abschnitt: Int): String = when (abschnitt) {
        1 -> "August bis Oktober"
        2 -> "November und Dezember"
        3 -> "Januar bis März"
        4 -> "April bis Juli"
        else -> ""
    }

    val plan: Map<String, Int> = mapOf(
        // Mathe Klasse 1
        "m1-vergleich" to 1, "m1-nachbar" to 1, "m1-strich" to 1,
        "m1-zerlegen" to 2, "m1-plus10" to 2, "m1-minus10" to 2,
        "m1-verdoppeln" to 3, "m1-zwanzig" to 3, "m1-sach" to 3,
        "m1-geld" to 4,
        // Mathe Klasse 2
        "m2-stellen" to 1, "m2-vergleich" to 1, "m2-hundert" to 1,
        "m2-ergaenzen" to 2, "m2-mauer" to 2, "m2-verdoppeln" to 2, "m2-geld" to 2,
        "m2-kern" to 3, "m2-einmaleins" to 3, "m2-geteilt" to 3, "m2-meter" to 3, "m2-sach" to 3,
        "m2-uhr" to 4, "m2-uhrlesen" to 4, "m2-koerper" to 4, "m2-kombi" to 4, "m2-zufall" to 4,
        // Mathe Klasse 3
        "m3-tausend" to 1, "m3-nachbarn" to 1, "m3-mauer" to 1,
        "m3-ueberschlag" to 2, "m3-geldkomma" to 2, "m3-laengen" to 2, "m3-sach" to 2,
        "m3-halbschriftlich" to 3, "m3-rest" to 3, "m3-schriftplus" to 3, "m3-kaestchen" to 3,
        "m3-schriftminus" to 4, "m3-gewicht" to 4, "m3-tabelle" to 4,
        // Mathe Klasse 4
        "m4-million" to 1, "m4-vergleich" to 1, "m4-runden" to 1,
        "m4-schriftmal" to 2, "m4-groessen" to 2, "m4-sach" to 2,
        "m4-schriftgeteilt" to 3, "m4-flaeche" to 3, "m4-zeit" to 3,
        "m4-brueche" to 4, "m4-kombi" to 4, "m4-zufall" to 4,
        // Deutsch Klasse 1
        "d1-anlaut" to 1, "d1-silben" to 1, "d1-reime" to 1,
        "d1-buchstaben" to 2, "d1-luecke" to 2,
        "d1-lesen" to 3,
        // Deutsch Klasse 2
        "d2-abc" to 1, "d2-gross" to 1,
        "d2-artikel" to 2, "d2-mehrzahl" to 2, "d2-satzzeichen" to 2,
        "d2-gegenteil" to 3, "d2-zusammen" to 3,
        "d2-doppelt" to 4,
        // Deutsch Klasse 3
        "d3-wortarten" to 1, "d3-verlaengern" to 1, "d3-satzarten" to 1,
        "d3-ie" to 2, "d3-wortfamilie" to 2, "d3-vergangen" to 2,
        "d3-ableiten" to 3, "d3-wortfeld" to 3, "d3-steigern" to 3,
        "d3-sss" to 4,
        // Deutsch Klasse 4
        "d4-wortarten" to 1, "d4-satzglieder" to 1,
        "d4-zeitformen" to 2, "d4-pronomen" to 2,
        "d4-faelle" to 3, "d4-trennen" to 3,
        "d4-dass" to 4,
        // Lesen übt man das ganze Jahr
        "l1-lesen" to 0, "l2-lesen" to 0, "l3-lesen" to 0, "l4-lesen" to 0,
    )

    /** In NRW kommt das Einmaleins erst in Klasse 3, dort gleich am Anfang zum Festigen */
    val planNRW: Map<String, Int> = mapOf("m2-einmaleins" to 1, "m2-geteilt" to 1)

    fun abschnitt(id: String, land: Bundesland = Bundesland.HESSEN): Int? {
        if (land == Bundesland.NRW) planNRW[id]?.let { return it }
        return plan[id]
    }

    /** Welcher Abschnitt gerade läuft. In den Sommerferien gilt schon der Anfang. */
    fun jetzt(monat: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1): Int = when (monat) {
        8, 9, 10 -> 1
        11, 12 -> 2
        1, 2, 3 -> 3
        else -> 4
    }

    class Gruppe(val abschnitt: Int?, val themen: List<LernThema>)

    /**
     * Die Themen nach Abschnitten, in jedem Abschnitt in der alten Reihenfolge.
     * Hat kein Thema einen Abschnitt (Englisch, Musik, Kunst), kommt eine einzige Gruppe ohne Abschnitt.
     */
    fun gruppiert(liste: List<LernThema>, land: Bundesland): List<Gruppe> {
        if (liste.none { abschnitt(it.id, land) != null }) {
            return if (liste.isEmpty()) emptyList() else listOf(Gruppe(null, liste))
        }
        val gruppen = mutableListOf<Gruppe>()
        for (a in listOf(1, 2, 3, 4, 0)) {
            val teil = liste.filter { abschnitt(it.id, land) == a }
            if (teil.isNotEmpty()) gruppen += Gruppe(a, teil)
        }
        val ohne = liste.filter { abschnitt(it.id, land) == null }
        if (ohne.isNotEmpty()) gruppen += Gruppe(null, ohne)
        return gruppen
    }
}

/** Helfer zum Erzeugen von Aufgaben. */
object Lern {
    /** Für Tests austauschbar. */
    var zufall: Random = Random.Default

    fun z(r: IntRange): Int = zufall.nextInt(r.first, r.last + 1)

    /** Ein zufälliges Element. Die Listen hier sind nie leer. */
    fun <T> eins(liste: List<T>): T = liste[zufall.nextInt(liste.size)]

    fun wahr(anteil: Double): Boolean = zufall.nextDouble() < anteil

    /** Große Zahlen mit Abstand wie im Schulbuch: 45 300 */
    fun gross(n: Int): String {
        if (n < 10_000) return n.toString()
        val teile = mutableListOf<String>()
        var rest = n.toString()
        while (rest.length > 3) {
            teile.add(0, rest.takeLast(3))
            rest = rest.dropLast(3)
        }
        teile.add(0, rest)
        return teile.joinToString(" ")
    }

    /**
     * Eine Aufgabe zum Antippen. Doppelte und falsche Antworten, die gleich der richtigen
     * sind, fallen weg. Die Reihenfolge der Knöpfe ist jedes Mal anders.
     */
    fun wahl(
        frage: String, rechnung: String = "", richtig: String, falsch: List<String>,
        anzahl: Int = 3, bild: String = "", hinweis: String = "", erklaerung: String = "",
    ): LernAufgabe {
        val andere = mutableListOf<String>()
        for (f in falsch.shuffled(zufall)) {
            if (f != richtig && f !in andere && andere.size < anzahl - 1) andere += f
        }
        return LernAufgabe(
            art = "wahl", frage = frage, rechnung = rechnung, antwort = richtig,
            hinweis = hinweis, erklaerung = erklaerung,
            optionen = (listOf(richtig) + andere).shuffled(zufall), bild = bild,
        )
    }

    /** Größer, kleiner oder gleich. Die Seiten stehen als Text da, die Werte rechnet der Aufrufer. */
    fun vergleich(links: String, a: Int, rechts: String, b: Int): LernAufgabe {
        val zeichen = if (a < b) "<" else if (a > b) ">" else "="
        val schlicht = links == a.toString() && rechts == b.toString()
        val l = if (links == a.toString()) links else "$links = $a"
        val r = if (rechts == b.toString()) rechts else "$rechts = $b"
        return LernAufgabe(
            art = "vergleich", frage = "Was gehört in die Mitte?",
            rechnung = "$links  ?  $rechts", antwort = zeichen,
            erklaerung = if (schlicht) "$links $zeichen $rechts" else "$l und $r, also $a $zeichen $b",
        )
    }

    fun zahl(
        frage: String, rechnung: String, antwort: Int, erklaerung: String = "", hinweis: String = "",
    ) = LernAufgabe(
        art = "zahl", frage = frage, rechnung = rechnung, antwort = antwort.toString(),
        hinweis = hinweis, erklaerung = erklaerung,
    )
}

/** Alle Themen und das Zusammenstellen einer Übung. */
object Lernwelt {
    const val ARBEIT_TITEL = "Lernwelt"
    const val ANZAHL = 10

    /** Einmal zusammengestellt und dann behalten. */
    val themen: List<LernThema> by lazy { LehrplanMathe.themen + LehrplanMatheMehr.themen }

    fun themenFuer(fach: LehrFach, klasse: Int, land: Bundesland): List<LernThema> =
        themen.filter { it.fach == fach && it.klasse(land) == klasse }

    /** Zehn verschiedene Aufgaben eines Themas */
    fun aufgaben(t: LernThema, anzahl: Int = ANZAHL): List<LernAufgabe> {
        val liste = mutableListOf<LernAufgabe>()
        val gesehen = mutableSetOf<String>()
        var versuche = 0
        while (liste.size < anzahl && versuche < 600) {
            versuche++
            val a = t.erzeuge()
            if (gesehen.add(a.schluessel)) liste += a
        }
        return liste
    }

    /** Die Klasse aus dem Profil als Zahl. Vorschule übt mit Klasse 1, ohne Klasse gibt es null. */
    fun klasse(profil: String): Int? {
        if (profil == "Vorschule") return 1
        val n = profil.filter { it.isDigit() }.toIntOrNull() ?: return null
        return if (n in 1..4) n else null
    }

    /** Die Klasse als Zahl, 0 und alles Ungültige ergibt null. */
    fun klasse(zahl: Int): Int? = if (zahl in 1..4) zahl else null

    /** Zuerst die eigene Klasse, dann die anderen */
    fun klassenReihenfolge(eigene: Int?): List<Int> {
        if (eigene == null) return listOf(1, 2, 3, 4)
        return listOf(eigene) + listOf(1, 2, 3, 4).filter { it != eigene }
    }

    /**
     * Die Arbeit mit der einen Übung zu einem Thema, mit neuen Aufgaben.
     * Sterne und Bestwert hängen am Namen der Übung und bleiben darum erhalten.
     */
    fun arbeit(t: LernThema, land: Bundesland): Arbeit = Arbeit(
        klasse = "Klasse ${t.klasse(land)}",
        fach = t.fach.anzeige,
        titel = ARBEIT_TITEL,
        uebungen = listOf(
            Uebung(
                titel = t.titel, gruppe = t.id, symbol = t.emoji, tipp = t.tipp,
                aufgaben = aufgaben(t).map { it.zuAufgabe() },
            ),
        ),
    )
}
