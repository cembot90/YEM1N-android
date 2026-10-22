package de.cemaras.yem1n.logik

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Eine einzelne Antwort in einer Runde. Das Format ist dasselbe wie in der iOS-App (`Antworten.swift`),
 * damit Eltern auf beiden Systemen die Antworten beider Systeme sehen: Kurzschlüssel n, f, r, e, l, s.
 */
@Serializable
data class AntwortDetail(
    @SerialName("n") val nr: Int,
    @SerialName("f") val frage: String = "",
    @SerialName("r") val rechnung: String = "",
    @SerialName("e") val eingabe: String = "",
    @SerialName("l") val loesung: String = "",
    @SerialName("s") val stand: String = AntwortProtokoll.OFFEN,
) {
    val istProblem: Boolean get() = stand == AntwortProtokoll.FALSCH || stand == AntwortProtokoll.ANGESCHAUT
}

/** Eine Aufgabe, bei der das Kind über mehrere Runden Probleme hatte. */
data class ProblemAufgabe(
    val uebung: String,
    val frage: String,
    val rechnung: String,
    val loesung: String,
    val falsch: Int = 0,
    val angeschaut: Int = 0,
    val richtig: Int = 0,
    /** Falsche Eingaben des Kindes, neueste zuerst, ohne Wiederholungen. */
    val falscheEingaben: List<String> = emptyList(),
    val zuletzt: Long = 0,
    val zuletztRichtig: Boolean = false,
) {
    val problemAnzahl: Int get() = falsch + angeschaut
    val schluessel: String get() = "$uebung|$frage|$rechnung"
}

/** Eine Übung im Schwächen-Radar. [anteil] ist der Anteil richtiger Antworten von 0 bis 1. */
data class Schwaeche(
    val titel: String,
    val anteil: Double,
    val runden: Int,
    val angesehen: Int,
    /** 1 wird besser, -1 zuletzt schlechter, 0 gleich. */
    val trend: Int,
)

object AntwortProtokoll {
    const val RICHTIG = "richtig"
    const val FALSCH = "falsch"
    const val ANGESCHAUT = "angeschaut"
    const val OFFEN = "offen"

    /** Obergrenze für einen Text, damit lange Sachaufgaben den Eintrag nicht aufblähen. */
    const val MAX_TEXT = 240

    /** Der Server nimmt höchstens 20000 Zeichen an. */
    private const val MAX_JSON = 19000

    private val json = Json { ignoreUnknownKeys = true }

    fun kuerze(text: String): String {
        val t = text.trim()
        return if (t.length > MAX_TEXT) t.take(MAX_TEXT) + " ..." else t
    }

    /** Bei Zahlenmauern gibt es keine Rechnung, dann steht die unterste Reihe da. */
    fun rechnungText(art: String, rechnung: String, reihen: List<List<Int>>): String {
        if (art == "mauer") {
            val unten = reihen.firstOrNull().orEmpty().joinToString(", ")
            return if (unten.isEmpty()) "Zahlenmauer" else "Zahlenmauer, unten: $unten"
        }
        return rechnung
    }

    fun loesungText(art: String, antwort: String, antwort2: String, reihen: List<List<Int>>): String = when (art) {
        "rest" -> "$antwort Rest $antwort2"
        "mauer" -> reihen.lastOrNull()?.firstOrNull()?.let { "Spitze $it" } ?: ""
        else -> antwort
    }

    /** Die Antworten einer fertigen Runde. */
    fun eintraege(s: RundenStand): List<AntwortDetail> = s.aufgaben.mapIndexed { i, a ->
        val stand = when (s.staende.getOrNull(i)) {
            Stand.RICHTIG -> RICHTIG
            Stand.FALSCH -> FALSCH
            Stand.ANGESCHAUT -> ANGESCHAUT
            else -> OFFEN
        }
        AntwortDetail(
            nr = i + 1,
            frage = kuerze(a.frage),
            rechnung = kuerze(rechnungText(a.art, a.rechnung, a.reihen)),
            eingabe = if (stand == ANGESCHAUT) "" else kuerze(s.gegeben.getOrNull(i).orEmpty()),
            loesung = kuerze(loesungText(a.art, a.antwort, a.antwort2, a.reihen)),
            stand = stand,
        )
    }

    /** Das Protokoll als JSON-Text, leer wenn es nichts gibt oder zu groß wäre. */
    fun json(liste: List<AntwortDetail>): String {
        if (liste.isEmpty()) return ""
        val t = json.encodeToString(ListSerializer(AntwortDetail.serializer()), liste)
        return if (t.length > MAX_JSON) "" else t
    }

    fun liste(text: String): List<AntwortDetail> {
        if (text.isBlank()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(AntwortDetail.serializer()), text).sortedBy { it.nr }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Mehrzeilige Rechnungen (Rätsel, Sachaufgaben) werden einzeilig. */
    fun einzeilig(text: String): String = text.lines().filter { it.isNotBlank() }.joinToString(" ") { it.trim() }

    /**
     * Sammelt über alle Runden die Aufgaben, die falsch waren oder deren Lösung angeschaut wurde.
     * Gleiche Aufgaben aus verschiedenen Runden werden zusammengefasst. Zuerst kommen die, die noch nicht sitzen,
     * dann die mit den meisten Problemen.
     */
    fun problemAufgaben(runden: List<RundenErgebnisEintrag>, limit: Int = 8): List<ProblemAufgabe> {
        val sammlung = LinkedHashMap<String, ProblemAufgabe>()
        for (runde in runden.sortedBy { it.zeitpunkt }) {
            for (d in liste(runde.details)) {
                if (d.stand == OFFEN) continue
                val key = "${runde.uebung}|${d.frage}|${d.rechnung}"
                var p = sammlung[key] ?: ProblemAufgabe(runde.uebung, d.frage, d.rechnung, d.loesung)
                p = when (d.stand) {
                    FALSCH -> {
                        val e = einzeilig(d.eingabe)
                        val eingaben = if (e.isEmpty()) p.falscheEingaben else listOf(e) + p.falscheEingaben.filter { it != e }
                        p.copy(falsch = p.falsch + 1, falscheEingaben = eingaben)
                    }
                    ANGESCHAUT -> p.copy(angeschaut = p.angeschaut + 1)
                    else -> p.copy(richtig = p.richtig + 1)
                }
                sammlung[key] = p.copy(zuletztRichtig = d.stand == RICHTIG, zuletzt = runde.zeitpunkt)
            }
        }
        return sammlung.values.filter { it.problemAnzahl > 0 }
            .sortedWith(
                compareBy<ProblemAufgabe> { it.zuletztRichtig }
                    .thenByDescending { it.problemAnzahl }
                    .thenByDescending { it.zuletzt },
            )
            .take(limit)
    }

    /** Runden der letzten [tage] Tage (heute mitgezählt), 0 heißt alle. */
    fun imZeitraum(runden: List<RundenErgebnisEintrag>, tage: Int, heute: Long): List<RundenErgebnisEintrag> =
        if (tage <= 0) runden else runden.filter { Tag.heute(it.zeitpunkt) >= heute - (tage - 1) }

    /** Die schwächsten Übungen: nicht ganz sicher gekonnt oder Lösung angeschaut, schwächste zuerst. */
    fun schwaechste(runden: List<RundenErgebnisEintrag>, limit: Int = 5): List<Schwaeche> {
        val alle = runden.groupBy { it.uebung }.map { (titel, liste) ->
            val r = liste.sumOf { it.richtig }
            val g = liste.sumOf { it.gesamt }
            val sortiert = liste.sortedBy { it.zeitpunkt }
            var trend = 0
            val letzte = sortiert.lastOrNull()
            if (sortiert.size >= 2 && letzte != null && letzte.gesamt > 0) {
                val frueher = sortiert.dropLast(1)
                val fg = frueher.sumOf { it.gesamt }
                if (fg > 0) {
                    val alt = frueher.sumOf { it.richtig }.toDouble() / fg
                    val neu = letzte.richtig.toDouble() / letzte.gesamt
                    trend = if (neu > alt + 0.05) 1 else if (neu < alt - 0.05) -1 else 0
                }
            }
            Schwaeche(titel, if (g > 0) r.toDouble() / g else 0.0, liste.size, liste.sumOf { it.angesehen }, trend)
        }
        return alle.filter { it.anteil < 0.95 || it.angesehen > 0 }.sortedBy { it.anteil }.take(limit)
    }

    /** Wann jedes Kind zuletzt geübt hat, das jüngste zuerst. */
    fun zuletztGeuebt(runden: List<RundenErgebnisEintrag>): List<Pair<String, Long>> =
        runden.groupBy { it.kind.ifBlank { "Kind" } }
            .map { (kind, liste) -> kind to liste.maxOf { it.zeitpunkt } }
            .sortedByDescending { it.second }
}
