package de.cemaras.yem1n.logik

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlin.random.asKotlinRandom

/** Klassencode wie in der iOS-App: 10 Zeichen, gespeichert und abgefragt als XXXXX-XXXXX (mit Strich). Familien nutzen ihn nicht mehr. */
object Familiencode {
    const val ZEICHEN = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** Bringt eine Eingabe in die Form XXXXX-XXXXX. Null, wenn sie nicht passt. */
    fun pruefe(eingabe: String): String? {
        val s = eingabe.uppercase().filter { it != '-' && !it.isWhitespace() }
        return if (s.length == 10 && s.all { it in ZEICHEN }) s.take(5) + "-" + s.drop(5) else null
    }

    /** Zum Anzeigen, auch für einen älteren Code ohne Strich. */
    fun zeige(code: String): String = pruefe(code) ?: code

    /** Der Code ist ein Geheimnis. Deshalb kommt der Zufall vom Sicherheits-Zufallsgenerator des Geräts. */
    private val sicher: kotlin.random.Random by lazy { java.security.SecureRandom().asKotlinRandom() }

    fun neu(zufall: kotlin.random.Random = sicher): String =
        pruefe(String(CharArray(10) { ZEICHEN[zufall.nextInt(ZEICHEN.length)] }))!!
}

/** Eine Frage eines Kindes (Datensatz "JokerAnfrage"). */
data class JokerFrage(
    val id: String,
    val kind: String,
    val fach: String,
    val uebung: String,
    val text: String,
    val erstellt: Long,
    val antworten: List<JokerAntwortEintrag> = emptyList(),
)

/** Eine Antwort der Eltern (Datensatz "JokerAntwort"). [daumen] ist wahr, wenn ein "JokerDaumen" dazu existiert. */
data class JokerAntwortEintrag(
    val id: String,
    val frageId: String,
    val absender: String,
    val emoji: String,
    val text: String,
    val erstellt: Long,
    val daumen: Boolean = false,
)

/** Ein Ergebnis einer Runde (Datensatz "RundenErgebnis"). */
data class RundenErgebnisEintrag(
    val eintragId: String,
    val klasse: String,
    val fach: String,
    val arbeit: String,
    val uebung: String,
    val richtig: Int,
    val gesamt: Int,
    val angesehen: Int,
    val sterne: Int,
    val kind: String,
    val zeitpunkt: Long,
    val details: String = "",
)

/**
 * Baut Leseanfragen an CloudKit und liest die Antworten. Nur noch für öffentliche Klassenpakete.
 * Familien laufen über [FamilienProtokoll]. Reines Kotlin, damit es ohne Gerät testbar ist.
 */
object JokerCloud {

    private val json = Json { ignoreUnknownKeys = true }

    /** Wie `sterneFuer` in der iOS-App. */
    fun sterne(gut: Int, gesamt: Int): Int {
        if (gesamt <= 0) return 0
        val a = gut.toDouble() / gesamt
        return when {
            a >= 0.9 -> 3
            a >= 0.7 -> 2
            a >= 0.5 -> 1
            else -> 0
        }
    }

    // Lesen

    /** [neuesterZuerstNach]: Feld, nach dem absteigend sortiert wird, damit bei vielen Treffern die neuesten kommen. */
    fun abfrage(typ: String, code: String, limit: Int = 200, neuesterZuerstNach: String? = null, fortsetzung: String? = null): String = buildJsonObject {
        put("query", buildJsonObject {
            put("recordType", typ)
            put("filterBy", buildJsonArray {
                add(buildJsonObject {
                    put("comparator", "EQUALS")
                    put("fieldName", "familienCode")
                    put("fieldValue", buildJsonObject { put("value", code) })
                })
            })
        })
        if (neuesterZuerstNach != null) {
            put("sortBy", buildJsonArray {
                add(buildJsonObject {
                    put("fieldName", neuesterZuerstNach)
                    put("ascending", false)
                })
            })
        }
        put("resultsLimit", limit)
        if (fortsetzung != null) put("continuationMarker", fortsetzung)
    }.toString()

    /** Der Hinweis, wo die nächste Seite einer langen Abfrage anfängt. Null, wenn es keine mehr gibt. */
    fun fortsetzung(antwort: String): String? = try {
        json.parseToJsonElement(antwort).jsonObject["continuationMarker"]?.jsonPrimitive?.contentOrNull?.ifEmpty { null }
    } catch (e: Exception) {
        null
    }

    /**
     * Holt alle Seiten einer Abfrage, höchstens [maxSeiten]. [holen] bekommt den Text der Abfrage und liefert die Antwort
     * oder null ohne Verbindung. Gibt null zurück, wenn schon die erste Seite fehlt.
     */
    fun alleSeiten(typ: String, code: String, limit: Int, sortierNach: String?, maxSeiten: Int, holen: (String) -> String?): List<Datensatz>? {
        var marke: String? = null
        val alle = mutableListOf<Datensatz>()
        for (seite in 0 until maxSeiten) {
            val text = holen(abfrage(typ, code, limit, sortierNach, marke))
            if (text == null) return if (seite == 0) null else alle
            alle += datensaetze(text)
            marke = fortsetzung(text) ?: break
        }
        return alle
    }

    /** Ein gelesener Datensatz: Name, Zeit der Erstellung in Millisekunden und seine Felder als Text oder Zahl. */
    data class Datensatz(val name: String, val erstellt: Long, val felder: Map<String, JsonPrimitive>) {
        fun text(k: String, ersatz: String = ""): String = felder[k]?.contentOrNull ?: ersatz
        fun zahl(k: String): Long = felder[k]?.longOrNull ?: 0L
    }

    fun datensaetze(antwort: String): List<Datensatz> {
        val o = try { json.parseToJsonElement(antwort).jsonObject } catch (e: Exception) { return emptyList() }
        val liste: JsonArray = try { o["records"]?.jsonArray ?: return emptyList() } catch (e: Exception) { return emptyList() }
        val aus = mutableListOf<Datensatz>()
        for (el in liste) {
            try {
                val r = el.jsonObject
                val name = r["recordName"]?.jsonPrimitive?.contentOrNull ?: continue
                val erstellt = r["created"]?.jsonObject?.get("timestamp")?.jsonPrimitive?.longOrNull ?: 0L
                val felder = mutableMapOf<String, JsonPrimitive>()
                r["fields"]?.jsonObject?.forEach { (k, v) ->
                    val w = v.jsonObject["value"]
                    if (w is JsonPrimitive) felder[k] = w
                }
                aus.add(Datensatz(name, erstellt, felder))
            } catch (e: Exception) {
                // kaputter Eintrag wird übersprungen
            }
        }
        return aus
    }
}
