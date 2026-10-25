package de.cemaras.yem1n.logik

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Was dieses Gerät über seine Familie weiß. Der private Schlüssel liegt nicht hier, sondern im Android Keystore. */
@Serializable
data class FamilienZugang(
    val familie: String,
    val geraet: String,
    val name: String,
    /** "eltern" oder "kind", bestimmt durch die Einladung. */
    val rolle: String,
    /** "aktiv", "wartet" (auf die Freigabe der Eltern) oder "abgelehnt". */
    val status: String,
) {
    val aktiv: Boolean get() = status == "aktiv"
    val wartet: Boolean get() = status == "wartet"
    val istEltern: Boolean get() = rolle == "eltern"

    /** Kurzer Prüfcode, den das neue Gerät und die Liste der Eltern gleich anzeigen, damit man das richtige Gerät erlaubt. */
    val pruefcode: String get() = FamilienProtokoll.pruefcode(geraet)
}

/** Ein Eintrag aus `/v2/lesen`. [id] ist die Kennung der Sache (Frage, Antwort ...), [von] das Gerät, das ihn angelegt hat. */
data class FamilienEintrag(
    val nr: Long,
    val typ: String,
    val id: String,
    val von: String,
    val absender: String,
    val erstellt: Long,
    val daten: JsonObject,
) {
    fun text(k: String, ersatz: String = ""): String = (daten[k] as? JsonPrimitive)?.contentOrNull ?: ersatz
    fun zahl(k: String): Long = (daten[k] as? JsonPrimitive)?.longOrNull ?: 0L

    /** Für die vorhandene Auswertung von Nachschub und Freigaben (Name = Kennung, Zeit = Anlegezeit). */
    fun alsDatensatz(): JokerCloud.Datensatz {
        val felder = mutableMapOf<String, JsonPrimitive>()
        for ((k, v) in daten) if (v is JsonPrimitive) felder[k] = v
        return JokerCloud.Datensatz(id, erstellt, felder)
    }
}

/** Ein Gerät in der Liste der Eltern (`/v2/geraete`). */
data class FamilienGeraet(
    val id: String,
    val name: String,
    val rolle: String,
    val status: String,
    val erstellt: Long,
    val zuletzt: Long,
    val ich: Boolean,
) {
    val pruefcode: String get() = FamilienProtokoll.pruefcode(id)
}

/** Eine frische Einladung, die als QR-Code gezeigt wird. [bis] ist die Ablaufzeit in Millisekunden. */
data class Einladung(val familie: String, val rolle: String, val token: String, val bis: Long) {
    val qr: String get() = FamilienProtokoll.qrInhalt(familie, token)
}

/** Alles, was zum Sprechen mit dem Familien-Dienst gehört und sich ohne Netz und Gerät prüfen lässt. */
object FamilienProtokoll {
    const val PFAD_FAMILIE = "/v2/familie"
    const val PFAD_KOPPELN = "/v2/koppeln"
    const val PFAD_STATUS = "/v2/status"
    const val PFAD_PUSH = "/v2/push"
    const val PFAD_SENDEN = "/v2/senden"
    const val PFAD_LESEN = "/v2/lesen"
    const val PFAD_EINLADUNG = "/v2/einladung"
    const val PFAD_GERAETE = "/v2/geraete"
    const val PFAD_ERLAUBEN = "/v2/geraet/erlauben"
    const val PFAD_ABLEHNEN = "/v2/geraet/ablehnen"
    const val PFAD_ENTFERNEN = "/v2/geraet/entfernen"
    const val PFAD_AUSTRETEN = "/v2/geraet/austreten"
    const val PFAD_LOESCHEN = "/v2/familie/loeschen"

    const val TYP_FRAGE = "frage"
    const val TYP_ANTWORT = "antwort"
    const val TYP_DAUMEN = "daumen"
    const val TYP_NACHSCHUB = "nachschub"
    const val TYP_FREIGABE = "freigabe"
    const val TYP_ERGEBNIS = "ergebnis"
    const val TYP_DUELL = "duell"
    const val TYP_DUELLANTWORT = "duellantwort"

    private val FAMILIE = Regex("^[0-9a-f]{64}$")
    private val TOKEN = Regex("^[A-Za-z0-9_-]{16,64}$")
    private val KENNUNG = Regex("^[A-Za-z0-9_-]{6,64}$")

    // ---- QR-Code zum Koppeln ----

    fun qrInhalt(familie: String, token: String): String = "yem1n://koppeln?f=$familie&t=$token"

    /** Gibt Familie und Einmal-Schlüssel aus einem gescannten Text zurück, sonst null. */
    fun qrLesen(text: String): Pair<String, String>? {
        val t = text.trim()
        val vorsatz = "yem1n://koppeln?"
        if (!t.lowercase().startsWith(vorsatz)) return null
        var familie: String? = null
        var token: String? = null
        for (teil in t.substring(vorsatz.length).split("&")) {
            val i = teil.indexOf('=')
            if (i < 0) continue
            when (teil.substring(0, i)) {
                "f" -> familie = teil.substring(i + 1)
                "t" -> token = teil.substring(i + 1)
            }
        }
        if (familie == null || token == null) return null
        return if (FAMILIE.matches(familie) && TOKEN.matches(token)) familie to token else null
    }

    /** Sechs Zeichen aus der Gerätekennung, groß geschrieben. Beide Geräte zeigen dasselbe, damit die Eltern das richtige erlauben. */
    fun pruefcode(geraet: String): String = geraet.take(6).uppercase()

    // ---- Kennungen ----

    /** Neue Kennung für eine Frage, Antwort und so weiter. Der Dienst erlaubt 6 bis 64 Zeichen aus A-Z, a-z, 0-9, _ und -. */
    fun neueKennung(vorsatz: String, zufall: String): String = (vorsatz + "-" + zufall).take(64)

    fun kennungOk(k: String): Boolean = KENNUNG.matches(k)

    // ---- Körper der Anfragen ----

    private fun senden(typ: String, daten: JsonObject): JsonObject = buildJsonObject {
        put("typ", typ)
        put("daten", daten)
    }

    fun frage(id: String, kind: String, fach: String, uebung: String, text: String): JsonObject = senden(TYP_FRAGE, buildJsonObject {
        put("id", id)
        put("kind", kind.trim().take(40).ifEmpty { "Kind" })
        put("fach", fach.take(40))
        put("uebung", uebung.take(80))
        put("text", text.trim().take(500))
    })

    fun antwort(id: String, frageId: String, emoji: String, text: String): JsonObject = senden(TYP_ANTWORT, buildJsonObject {
        put("id", id)
        put("frageId", frageId)
        put("emoji", emoji.take(16))
        put("text", text.trim().take(500))
    })

    fun daumen(frageId: String, antwortId: String): JsonObject = senden(TYP_DAUMEN, buildJsonObject {
        put("frageId", frageId)
        put("antwortId", antwortId)
    })

    fun nachschub(id: String, kind: String): JsonObject = senden(TYP_NACHSCHUB, buildJsonObject {
        put("id", id)
        put("kind", kind.trim().take(40).ifEmpty { "Kind" })
    })

    fun freigabe(id: String, anzahl: Int = 3): JsonObject = senden(TYP_FREIGABE, buildJsonObject {
        put("id", id)
        put("anzahl", anzahl.coerceIn(1, 10))
    })

    fun ergebnis(e: RundenErgebnisEintrag): JsonObject = senden(TYP_ERGEBNIS, buildJsonObject {
        put("id", e.eintragId)
        put("klasse", e.klasse.take(40))
        put("fach", e.fach.take(40))
        put("arbeit", e.arbeit.take(120))
        put("uebung", e.uebung.take(120))
        put("richtig", e.richtig.coerceIn(0, 1000))
        put("gesamt", e.gesamt.coerceIn(0, 1000))
        put("angesehen", e.angesehen.coerceIn(0, 1000))
        put("sterne", e.sterne.coerceIn(0, 3))
        put("kind", e.kind.trim().take(40))
        put("zeitpunkt", e.zeitpunkt)
        if (e.details.isNotEmpty() && e.details.length <= 20000) put("details", e.details)
    })

    fun duell(id: String, json: String): JsonObject = senden(TYP_DUELL, buildJsonObject { put("id", id); put("json", json) })

    fun duellAntwort(duellId: String, json: String): JsonObject =
        senden(TYP_DUELLANTWORT, buildJsonObject { put("duellId", duellId); put("json", json) })

    fun lesen(typ: String, seit: Long = 0, limit: Int = 100): JsonObject = buildJsonObject {
        put("typ", typ)
        put("seit", seit)
        put("limit", limit.coerceIn(1, 200))
    }

    /** Meldet die Push-Kennung dieses Geräts an. */
    fun pushAnmelden(token: String): JsonObject = buildJsonObject {
        put("art", "fcm")
        put("token", token)
    }

    fun pushAbmelden(): JsonObject = buildJsonObject { put("art", "") }

    // ---- Antworten lesen ----

    /** Die Einträge aus der Antwort von `/v2/lesen`. Kaputte Einträge werden übersprungen. */
    fun eintraege(antwort: JsonObject): List<FamilienEintrag> {
        val liste: JsonArray = try { antwort["eintraege"]?.jsonArray ?: return emptyList() } catch (e: Exception) { return emptyList() }
        val aus = mutableListOf<FamilienEintrag>()
        for (el in liste) {
            try {
                val o = el.jsonObject
                aus += FamilienEintrag(
                    nr = (o["nr"] as? JsonPrimitive)?.longOrNull ?: continue,
                    typ = (o["typ"] as? JsonPrimitive)?.contentOrNull ?: continue,
                    id = (o["id"] as? JsonPrimitive)?.contentOrNull ?: continue,
                    von = (o["von"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                    absender = (o["absender"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                    erstellt = (o["erstellt"] as? JsonPrimitive)?.longOrNull ?: 0L,
                    daten = o["daten"]?.jsonObject ?: continue,
                )
            } catch (e: Exception) {
                // kaputter Eintrag wird übersprungen
            }
        }
        return aus
    }

    /** Ob nach dieser Antwort noch mehr zu holen ist, und ab welcher Nummer. */
    fun weiterAb(antwort: JsonObject): Long? =
        if ((antwort["mehr"] as? JsonPrimitive)?.booleanOrNull == true) (antwort["letzte"] as? JsonPrimitive)?.longOrNull else null

    fun geraete(antwort: JsonObject): List<FamilienGeraet> {
        val liste: JsonArray = try { antwort["geraete"]?.jsonArray ?: return emptyList() } catch (e: Exception) { return emptyList() }
        val aus = mutableListOf<FamilienGeraet>()
        for (el in liste) {
            try {
                val o = el.jsonObject
                aus += FamilienGeraet(
                    id = (o["id"] as? JsonPrimitive)?.contentOrNull ?: continue,
                    name = (o["name"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                    rolle = (o["rolle"] as? JsonPrimitive)?.contentOrNull ?: "kind",
                    status = (o["status"] as? JsonPrimitive)?.contentOrNull ?: "wartet",
                    erstellt = (o["erstellt"] as? JsonPrimitive)?.longOrNull ?: 0L,
                    zuletzt = (o["zuletzt"] as? JsonPrimitive)?.longOrNull ?: 0L,
                    ich = (o["ich"] as? JsonPrimitive)?.booleanOrNull == true,
                )
            } catch (e: Exception) {
                // kaputter Eintrag wird übersprungen
            }
        }
        return aus
    }

    // ---- Auswertung für die Oberfläche ----

    /** Setzt Fragen, Antworten und Daumen zusammen. Neueste Frage zuerst. Den Namen der Antwortenden setzt der Dienst. */
    fun fragen(fragen: List<FamilienEintrag>, antworten: List<FamilienEintrag>, daumen: List<FamilienEintrag>): List<JokerFrage> {
        val daumenIds = daumen.map { it.text("antwortId") }.toSet()
        val alle = antworten.map {
            JokerAntwortEintrag(
                id = it.id, frageId = it.text("frageId"), absender = it.absender.ifEmpty { "?" },
                emoji = it.text("emoji", "🙂"), text = it.text("text"), erstellt = it.erstellt,
                daumen = it.id in daumenIds,
            )
        }
        return fragen.map { f ->
            JokerFrage(
                id = f.id, kind = f.text("kind", "Kind"), fach = f.text("fach"),
                uebung = f.text("uebung"), text = f.text("text"), erstellt = f.erstellt,
                antworten = alle.filter { it.frageId == f.id }.sortedBy { it.erstellt },
            )
        }.sortedByDescending { it.erstellt }
    }

    fun ergebnisse(daten: List<FamilienEintrag>): List<RundenErgebnisEintrag> = daten.map {
        RundenErgebnisEintrag(
            eintragId = it.id, klasse = it.text("klasse"), fach = it.text("fach"),
            arbeit = it.text("arbeit"), uebung = it.text("uebung"), richtig = it.zahl("richtig").toInt(),
            gesamt = it.zahl("gesamt").toInt(), angesehen = it.zahl("angesehen").toInt(),
            sterne = it.zahl("sterne").toInt(), kind = it.text("kind"),
            zeitpunkt = it.zahl("zeitpunkt").takeIf { z -> z > 0 } ?: it.erstellt,
            details = it.text("details"),
        )
    }.sortedByDescending { it.zeitpunkt }

    // ---- Fehlertexte ----

    /** Ein Satz für Kinder und Eltern. [status] 0 heißt: keine Verbindung, -1: der Schlüssel dieses Geräts fehlt. */
    fun fehlertext(status: Int): String = when (status) {
        0 -> "Keine Verbindung. Ist das Internet an?"
        -1 -> "Dieses Gerät kann keinen Schlüssel anlegen oder lesen."
        400 -> "Das konnte nicht gesendet werden. Bitte den Text prüfen."
        401 -> "Die Familie kennt dieses Gerät nicht (mehr). Bitte neu verbinden. Auch eine falsche Uhrzeit kann das auslösen."
        403 -> "Das darf dieses Gerät nicht, oder die Einladung gilt nicht mehr."
        404 -> "Diese Familie gibt es nicht (mehr)."
        409 -> "Das geht gerade nicht. Vielleicht ist das Gerät schon in der Familie oder die Familie ist voll."
        429 -> "Das ging zu schnell. Bitte kurz warten."
        else -> "Der Dienst hat nicht geantwortet (Code $status)."
    }
}
