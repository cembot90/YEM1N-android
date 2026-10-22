package de.cemaras.yem1n.logik

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Die Anfragen an den eigenen Server (`server/`). Der Server prüft alles noch einmal und schreibt dann in CloudKit. */
object ServerAnfrage {
    const val PFAD_SENDEN = "/v1/senden"
    const val PFAD_GERAET = "/v1/geraet"

    private fun kopf(code: String, art: String, daten: JsonObject): String = buildJsonObject {
        put("code", code)
        put("art", art)
        put("daten", daten)
    }.toString()

    fun frage(code: String, kind: String, fach: String, uebung: String, text: String): String =
        kopf(code, "frage", buildJsonObject {
            put("kind", kind.trim().take(40).ifEmpty { "Kind" })
            put("fach", fach.take(40))
            put("uebung", uebung.take(80))
            put("text", text.trim().take(500))
        })

    fun antwort(code: String, frageId: String, absender: String, emoji: String, text: String): String =
        kopf(code, "antwort", buildJsonObject {
            put("frageId", frageId)
            put("absender", absender.trim().take(40).ifEmpty { "Eltern" })
            put("emoji", emoji.take(16))
            put("text", text.trim().take(500))
        })

    fun daumen(code: String, frageId: String, antwortId: String, absender: String): String =
        kopf(code, "daumen", buildJsonObject {
            put("frageId", frageId)
            put("antwortId", antwortId)
            put("absender", absender.trim().take(40).ifEmpty { "Kind" })
        })

    fun ergebnis(code: String, e: RundenErgebnisEintrag): String =
        kopf(code, "ergebnis", buildJsonObject {
            put("eintragId", e.eintragId)
            put("klasse", e.klasse)
            put("fach", e.fach)
            put("arbeit", e.arbeit)
            put("uebung", e.uebung)
            put("richtig", e.richtig)
            put("gesamt", e.gesamt)
            put("angesehen", e.angesehen)
            put("sterne", e.sterne)
            put("kind", e.kind)
            put("zeitpunkt", e.zeitpunkt)
            if (e.details.isNotEmpty()) put("details", e.details)
        })

    /** Das Kind bittet um neue Joker. */
    fun nachschub(code: String, kind: String): String =
        kopf(code, "nachschub", buildJsonObject { put("kind", kind.trim().take(40).ifEmpty { "Kind" }) })

    /** Die Eltern geben Joker frei (wie bei iOS: wieder auf das Tageslimit auffüllen). */
    fun freigabe(code: String, anzahl: Int = 3): String =
        kopf(code, "freigabe", buildJsonObject { put("anzahl", anzahl.coerceIn(1, 10)) })

    /** Meldet dieses Gerät für Mitteilungen an. [rolle] ist "kind" oder "eltern". */
    fun geraet(code: String, rolle: String, token: String): String = buildJsonObject {
        put("code", code)
        put("rolle", rolle)
        put("token", token)
    }.toString()
}
