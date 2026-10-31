package de.cemaras.yem1n.logik

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Beim Wechsel auf eine neue Familie schickt das Kind-Gerät seine bisherigen Runden noch einmal an die neue Familie,
 * wie bei iOS (`ErgebnisUmzug.swift`). So behalten die Eltern ihre Übersicht und den Wochenbericht.
 *
 * Dafür merkt sich das Gerät jede gesendete Runde in einem Archiv (eine Zeile je Runde, fertige Anfrage).
 * Doppelt gesendet schadet nicht: Jeder Eintrag hat eine feste Kennung, der Dienst nimmt ihn nur einmal an.
 * Bricht das Senden ab, geht es beim nächsten Mal hinter der zuletzt angekommenen Runde weiter.
 */
object ErgebnisUmzug {
    /** So lange hält der Dienst Ergebnisse. Ältere schicken wir nicht mehr hoch. */
    const val TAGE = 365
    /** Höchstens so viele Runden bleiben im Archiv, damit die Datei klein bleibt. */
    const val MAX = 3000
    /** Pause zwischen zwei Einträgen bei vielen Runden, damit die Bremse des Dienstes (120 Anfragen pro Minute) nicht anschlägt. */
    const val PAUSE_MS = 700L
    const val AB = 50

    private const val TAG_MS = 86_400_000L

    fun grenze(jetzt: Long): Long = jetzt - TAGE * TAG_MS

    /** Zeitpunkt der Runde aus einer fertigen Anfrage, 0 wenn er fehlt. */
    fun zeitpunkt(koerper: String): Long = try {
        val o = Json.parseToJsonElement(koerper) as? JsonObject
        (o?.get("daten") as? JsonObject)?.get("zeitpunkt")?.jsonPrimitive?.longOrNull ?: 0
    } catch (e: Exception) {
        0
    }

    /** Archiv als Text: eine Anfrage je Zeile. Leere oder kaputte Zeilen fallen weg. */
    fun lese(text: String?): List<String> = text.orEmpty().lines().filter { it.isNotBlank() && zeitpunkt(it) > 0 }

    fun text(archiv: List<String>): String = archiv.joinToString("\n")

    /** Nimmt eine gesendete Runde ins Archiv auf. Zu alte und zu viele fallen vorne heraus. */
    fun merke(archiv: List<String>, koerper: String, jetzt: Long): List<String> {
        if (zeitpunkt(koerper) <= 0 || koerper in archiv) return archiv
        val g = grenze(jetzt)
        return (archiv + koerper).filter { zeitpunkt(it) >= g }.takeLast(MAX)
    }

    /**
     * Was noch an [familie] geschickt werden muss, älteste zuerst.
     * [erledigt]: Familie, für die schon alles gesendet ist. [laeuft] und [bis]: Familie, an die gerade gesendet wird,
     * und der Zeitpunkt der zuletzt angekommenen Runde.
     */
    fun offen(archiv: List<String>, familie: String, erledigt: String, laeuft: String, bis: Long, jetzt: Long): List<String> {
        if (familie.isEmpty() || erledigt == familie) return emptyList()
        val ab = maxOf(grenze(jetzt) - 1, if (laeuft == familie) bis else 0)
        return archiv.filter { zeitpunkt(it) > ab }.sortedBy { zeitpunkt(it) }
    }

    /** Bei vielen offenen Runden kurz warten, bei wenigen gar nicht. */
    fun pause(anzahl: Int): Long? = if (anzahl > AB) PAUSE_MS else null
}
