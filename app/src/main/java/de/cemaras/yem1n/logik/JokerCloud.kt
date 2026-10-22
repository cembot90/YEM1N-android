package de.cemaras.yem1n.logik

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Familiencode wie in der iOS-App: 10 Zeichen, gespeichert und abgefragt als XXXXX-XXXXX (mit Strich). */
object Familiencode {
    const val ZEICHEN = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** Bringt eine Eingabe in die Form XXXXX-XXXXX. Null, wenn sie nicht passt. */
    fun pruefe(eingabe: String): String? {
        val s = eingabe.uppercase().filter { it != '-' && !it.isWhitespace() }
        return if (s.length == 10 && s.all { it in ZEICHEN }) s.take(5) + "-" + s.drop(5) else null
    }

    /** Zum Anzeigen, auch für einen älteren Code ohne Strich. */
    fun zeige(code: String): String = pruefe(code) ?: code

    fun neu(zufall: kotlin.random.Random = kotlin.random.Random.Default): String =
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

/** Baut die Anfragen an CloudKit und liest die Antworten. Reines Kotlin, damit es ohne Gerät testbar ist. */
object JokerCloud {
    const val TYP_FRAGE = "JokerAnfrage"
    const val TYP_ANTWORT = "JokerAntwort"
    const val TYP_DAUMEN = "JokerDaumen"
    const val TYP_ERGEBNIS = "RundenErgebnis"
    const val TYP_NACHSCHUB = "JokerNachschub"
    const val TYP_FREIGABE = "JokerFreigabe"

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

    private fun feld(wert: JsonElement) = buildJsonObject { put("value", wert) }

    private fun anlegen(typ: String, name: String?, felder: Map<String, JsonElement>): JsonObject =
        buildJsonObject {
            put("operationType", "create")
            put("record", buildJsonObject {
                put("recordType", typ)
                if (name != null) put("recordName", name)
                put("fields", buildJsonObject { felder.forEach { (k, v) -> put(k, feld(v)) } })
            })
        }

    private fun modify(vararg op: JsonObject): String =
        buildJsonObject { put("operations", buildJsonArray { op.forEach { add(it) } }) }.toString()

    private fun s(t: String) = JsonPrimitive(t)
    private fun n(z: Number) = JsonPrimitive(z)

    // Schreiben (nur neu anlegen, wie bei iOS)

    fun frageSenden(code: String, kind: String, fach: String, uebung: String, text: String): String =
        modify(anlegen(TYP_FRAGE, null, mapOf(
            "familienCode" to s(code), "kind" to s(kind), "fach" to s(fach),
            "uebung" to s(uebung), "text" to s(text), "status" to s("offen"),
        )))

    fun antwortSenden(code: String, frageId: String, absender: String, emoji: String, text: String): String =
        modify(anlegen(TYP_ANTWORT, null, mapOf(
            "familienCode" to s(code), "anfrageID" to s(frageId), "absender" to s(absender),
            "emoji" to s(emoji), "text" to s(text),
        )))

    fun daumenSenden(code: String, frageId: String, antwortId: String, absender: String): String =
        modify(anlegen(TYP_DAUMEN, "daumen-$antwortId", mapOf(
            "familienCode" to s(code), "anfrageID" to s(frageId), "antwortID" to s(antwortId),
            "absender" to s(absender),
        )))

    fun ergebnisSenden(code: String, e: RundenErgebnisEintrag): String {
        val felder = linkedMapOf<String, JsonElement>(
            "familienCode" to s(code), "eintragID" to s(e.eintragId), "klasse" to s(e.klasse),
            "fach" to s(e.fach), "arbeit" to s(e.arbeit), "uebung" to s(e.uebung),
            "richtig" to n(e.richtig), "gesamt" to n(e.gesamt), "angesehen" to n(e.angesehen),
            "sterne" to n(e.sterne), "kind" to s(e.kind), "zeitpunkt" to n(e.zeitpunkt),
        )
        if (e.details.isNotEmpty()) felder["details"] = s(e.details)
        return modify(anlegen(TYP_ERGEBNIS, "erg-" + e.eintragId, felder))
    }

    // Lesen

    /** [neuesterZuerstNach]: Feld, nach dem absteigend sortiert wird, damit bei vielen Treffern die neuesten kommen. */
    fun abfrage(typ: String, code: String, limit: Int = 200, neuesterZuerstNach: String? = null): String = buildJsonObject {
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
    }.toString()

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

    /** Setzt Fragen, Antworten und Daumen wie `ladeJoker` in iOS zusammen. Neueste Frage zuerst. */
    fun fragen(fragen: List<Datensatz>, antworten: List<Datensatz>, daumen: List<Datensatz>): List<JokerFrage> {
        val daumenIds = daumen.map { it.text("antwortID") }.toSet()
        val alle = antworten.map {
            JokerAntwortEintrag(
                id = it.name, frageId = it.text("anfrageID"), absender = it.text("absender", "?"),
                emoji = it.text("emoji", "🙂"), text = it.text("text"), erstellt = it.erstellt,
                daumen = it.name in daumenIds,
            )
        }
        return fragen.map { f ->
            JokerFrage(
                id = f.name, kind = f.text("kind", "Kind"), fach = f.text("fach"),
                uebung = f.text("uebung"), text = f.text("text"), erstellt = f.erstellt,
                antworten = alle.filter { it.frageId == f.name }.sortedBy { it.erstellt },
            )
        }.sortedByDescending { it.erstellt }
    }

    fun ergebnisse(daten: List<Datensatz>): List<RundenErgebnisEintrag> = daten.map {
        RundenErgebnisEintrag(
            eintragId = it.text("eintragID", it.name), klasse = it.text("klasse"), fach = it.text("fach"),
            arbeit = it.text("arbeit"), uebung = it.text("uebung"), richtig = it.zahl("richtig").toInt(),
            gesamt = it.zahl("gesamt").toInt(), angesehen = it.zahl("angesehen").toInt(),
            sterne = it.zahl("sterne").toInt(), kind = it.text("kind"),
            zeitpunkt = it.zahl("zeitpunkt").takeIf { z -> z > 0 } ?: it.erstellt,
            details = it.text("details"),
        )
    }.sortedByDescending { it.zeitpunkt }
}
