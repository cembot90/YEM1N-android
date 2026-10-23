package de.cemaras.yem1n.logik

import de.cemaras.yem1n.logik.lehrplan.Vokabel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Ein Wort im Vokabel-Editor. Deutsch, Englisch und Türkisch sind Pflicht, Italienisch ist freiwillig. */
@Serializable
data class EigenWort(
    val emoji: String = "",
    val de: String = "",
    val en: String = "",
    val tr: String = "",
    val it: String = "",
) {
    val gueltig get() = de.isNotBlank() && en.isNotBlank() && tr.isNotBlank()

    fun zuVokabel() = Vokabel(emoji.trim(), de.trim(), en.trim(), tr.trim(), it.trim())
}

/** Ein selbst gemachtes Vokabel-Thema. Es erscheint im Bereich Sprachen neben den fertigen Themen. */
@Serializable
data class EigenThema(
    val id: String,
    val titel: String,
    val emoji: String = "📘",
    val klasse: Int = 3,
    val woerter: List<EigenWort> = emptyList(),
)

/** Die selbst gemachten Themen: Speicherformat, Prüfung und der Weg in die Sprachen-Seite. */
object EigeneThemen {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }

    /** Wird von der App gesetzt, sobald die Themen geladen oder geändert sind. */
    @Volatile
    var liste: List<EigenThema> = emptyList()

    const val VORSATZ = "eigen-"

    fun ist(id: String) = id.startsWith(VORSATZ)

    fun woerter(id: String): List<Vokabel>? =
        liste.firstOrNull { it.id == id }?.woerter?.filter { it.gueltig }?.map { it.zuVokabel() }

    fun neueId(titel: String, vorhanden: List<EigenThema>): String {
        val basis = VORSATZ + titel.lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "thema" }
        var id = basis
        var n = 2
        while (vorhanden.any { it.id == id }) { id = "$basis-$n"; n++ }
        return id
    }

    fun text(themen: List<EigenThema>): String = json.encodeToString(themen)

    fun lese(text: String?): List<EigenThema> =
        if (text.isNullOrBlank()) emptyList()
        else try { json.decodeFromString<List<EigenThema>>(text) } catch (e: Exception) { emptyList() }

    /** Prüft ein Thema vor dem Speichern. Gibt Fehler für die Eltern zurück, leer heißt in Ordnung. */
    fun pruefe(t: EigenThema): List<String> {
        val f = mutableListOf<String>()
        if (t.titel.isBlank()) f += "Das Thema braucht einen Titel."
        val gut = t.woerter.filter { it.gueltig }
        if (gut.size < 3) f += "Mindestens drei vollständige Wörter (Deutsch, Englisch, Türkisch) sind nötig, damit Karten und Paare funktionieren."
        for ((i, w) in t.woerter.withIndex()) {
            val leer = w.de.isBlank() && w.en.isBlank() && w.tr.isBlank() && w.it.isBlank()
            if (!leer && !w.gueltig) f += "Wort ${i + 1} ist nicht vollständig (Deutsch, Englisch und Türkisch fehlen nicht)."
        }
        val alles = t.titel + t.woerter.joinToString(" ") { it.de + it.en + it.tr + it.it }
        if (alles.any { it == '–' || it == '—' }) f += "Bitte keine langen Striche verwenden, nur - ."
        val doppelt = gut.groupBy { it.de.trim().lowercase() }.filter { it.value.size > 1 }.keys
        if (doppelt.isNotEmpty()) f += "Doppelte deutsche Wörter: ${doppelt.joinToString(", ")}."
        return f
    }

    /** Lässt leere Zeilen weg und schneidet Leerzeichen ab. */
    fun bereinigt(t: EigenThema): EigenThema = t.copy(
        titel = t.titel.trim(),
        woerter = t.woerter.filter { it.de.isNotBlank() || it.en.isNotBlank() || it.tr.isNotBlank() || it.it.isNotBlank() }
            .map { EigenWort(it.emoji.trim(), it.de.trim(), it.en.trim(), it.tr.trim(), it.it.trim()) },
    )
}
