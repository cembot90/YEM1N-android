package de.cemaras.yem1n.daten

import de.cemaras.yem1n.logik.Mauer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

// Import-Format, so wie Claude es liefert (gleich wie bei der iOS-App).
// Antworten dürfen im JSON als Text oder als Zahl stehen.

@Serializable
data class ArbeitPaket(
    val klasse: String,
    val fach: String,
    val arbeit: String,
    val uebungen: List<UebungPaket>,
)

@Serializable
data class UebungPaket(
    val titel: String,
    val gruppe: String? = null,
    val symbol: String? = null,
    val tipp: String? = null,
    val aufgaben: List<AufgabePaket>,
)

@Serializable
data class AufgabePaket(
    val art: String? = null,
    val frage: String? = null,
    val rechnung: String? = null,
    val hinweis: String? = null,
    val erklaerung: String? = null,
    val antwort: JsonElement? = null,
    val antwort2: JsonElement? = null,
    val reihen: List<List<Int>>? = null,
    // Nur für art "wahl" (Vorschule)
    val bild: String? = null,
    val tr: String? = null,
    val gross: JsonElement? = null,
    val folge: List<String>? = null,
    val optionen: List<String>? = null,
)

/** Text aus einem JSON-Wert, egal ob er als Text oder als Zahl geschrieben wurde. */
fun JsonElement?.alsText(): String? {
    if (this == null || this is JsonNull) return null
    return (this as? JsonPrimitive)?.content
}

/** Eine fertig vorbereitete Aufgabe ohne leere Felder. */
data class Aufgabe(
    val art: String,
    val frage: String,
    val rechnung: String,
    val hinweis: String,
    val erklaerung: String,
    val antwort: String,
    val antwort2: String,
    val reihen: List<List<Int>>,
    val bild: String,
    val tr: String,
    val gross: String,
    val folge: List<String>,
    val optionen: List<String>,
) {
    val istMauer get() = art == ART_MAUER
    val istWahl get() = art == ART_WAHL

    companion object {
        const val ART_ZAHL = "zahl"
        const val ART_REST = "rest"
        const val ART_VERGLEICH = "vergleich"
        const val ART_MAUER = "mauer"
        const val ART_TEXT = "text"
        const val ART_WAHL = "wahl"
    }
}

data class Uebung(
    val titel: String,
    val gruppe: String,
    val symbol: String,
    val tipp: String,
    val aufgaben: List<Aufgabe>,
)

data class Arbeit(
    val klasse: String,
    val fach: String,
    val titel: String,
    val uebungen: List<Uebung>,
) {
    /** Schlüssel, unter dem der Fortschritt dieser Arbeit gespeichert wird. */
    val schluessel get() = "$klasse|$fach|$titel"
    val maxSterne get() = uebungen.size * 3
}

fun AufgabePaket.zuAufgabe() = Aufgabe(
    art = art?.ifBlank { null } ?: Aufgabe.ART_ZAHL,
    frage = frage.orEmpty(),
    rechnung = rechnung.orEmpty(),
    hinweis = hinweis.orEmpty(),
    erklaerung = erklaerung.orEmpty(),
    antwort = antwort.alsText().orEmpty(),
    antwort2 = antwort2.alsText().orEmpty(),
    reihen = reihen.orEmpty(),
    bild = bild.orEmpty(),
    tr = tr.orEmpty(),
    gross = gross.alsText().orEmpty(),
    folge = folge.orEmpty(),
    optionen = optionen.orEmpty(),
)

fun ArbeitPaket.zuArbeit() = Arbeit(
    klasse = klasse,
    fach = fach,
    titel = arbeit,
    uebungen = uebungen.map { u ->
        Uebung(
            titel = u.titel,
            gruppe = u.gruppe.orEmpty(),
            symbol = u.symbol.orEmpty(),
            tipp = u.tipp.orEmpty(),
            aufgaben = u.aufgaben.map { it.zuAufgabe() },
        )
    },
)

/** Ergebnis beim Einlesen: entweder eine Arbeit oder ein Fehlertext für die Eltern. */
data class LeseErgebnis(val arbeit: Arbeit?, val fehler: String?)

object PaketLeser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Entfernt Text und Code-Zäune um das JSON herum, zum Beispiel aus der Zwischenablage. */
    fun bereinigt(roh: String): String {
        val a = roh.indexOf('{')
        val b = roh.lastIndexOf('}')
        return if (a < 0 || b < a) roh.trim() else roh.substring(a, b + 1)
    }

    fun lese(roh: String): LeseErgebnis {
        val paket = try {
            json.decodeFromString(ArbeitPaket.serializer(), bereinigt(roh))
        } catch (e: Exception) {
            return LeseErgebnis(null, "Das ist keine gültige Arbeit. Bitte das JSON komplett kopieren.")
        }
        if (paket.uebungen.isEmpty() || paket.uebungen.any { it.aufgaben.isEmpty() }) {
            return LeseErgebnis(null, "Die Arbeit hat leere Übungen.")
        }
        for (u in paket.uebungen) {
            if (u.aufgaben.any { it.art == Aufgabe.ART_MAUER && !Mauer.gueltig(it.reihen.orEmpty()) }) {
                return LeseErgebnis(null, "In \"${u.titel}\" ist eine Zahlenmauer nicht richtig aufgebaut.")
            }
            for (a in u.aufgaben.filter { it.art == Aufgabe.ART_WAHL }) {
                val optionen = a.optionen
                val antwort = a.antwort.alsText()
                if (optionen == null || optionen.size !in 2..4 || antwort == null ||
                    antwort !in optionen || a.frage.isNullOrEmpty()
                ) {
                    return LeseErgebnis(
                        null,
                        "In \"${u.titel}\" hat eine Auswahl-Aufgabe keine passenden Antwortknöpfe.",
                    )
                }
            }
        }
        return LeseErgebnis(paket.zuArbeit(), null)
    }
}
