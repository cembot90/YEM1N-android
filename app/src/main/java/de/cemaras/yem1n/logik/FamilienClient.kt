package de.cemaras.yem1n.logik

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Die Antwort des Dienstes. [status] 0 heißt: keine Verbindung, -1: der Schlüssel dieses Geräts fehlt, -3: das Gerät gehört zu keiner Familie. */
class FamilienAntwort(val status: Int, val json: JsonObject?) {
    val ok: Boolean get() = status in 200..299
    val keineVerbindung: Boolean get() = status == 0
    fun text(k: String): String = (json?.get(k) as? JsonPrimitive)?.contentOrNull.orEmpty()
    fun zahl(k: String): Long = (json?.get(k) as? JsonPrimitive)?.longOrNull ?: 0L
    val fehler: String get() = FamilienProtokoll.fehlertext(status)
}

/** Die Leitung zum Dienst. Liefert Status und Text der Antwort, Status 0 ohne Verbindung. Läuft im Hintergrund. */
interface FamilienLeitung {
    fun post(pfad: String, kopf: Map<String, String>, body: String): Pair<Int, String>
}

/** Der Schlüssel dieses Geräts. Auf Android liegt er im Keystore und verlässt das Gerät nie. */
interface GeraetSignierer {
    /** Die 65 Bytes des öffentlichen Schlüssels (`04 X Y`), oder null, wenn es (noch) keinen gibt. */
    fun oeffentlich(): ByteArray?

    /** Unterschreibt mit ECDSA über SHA-256. Das Ergebnis ist im DER-Format, so wie Android es liefert. */
    fun signieren(daten: ByteArray): ByteArray?
}

/**
 * Spricht Protokoll 2 (`cloud/PROTOKOLL-V2.md`): unterschreibt jede Anfrage mit dem Schlüssel des Geräts und gleicht
 * eine falsch gehende Uhr aus. Nie auf dem Haupt-Thread aufrufen.
 */
class FamilienClient(
    private val leitung: FamilienLeitung,
    private val signierer: GeraetSignierer,
    private val jetzt: () -> Long = { System.currentTimeMillis() },
) {
    /** Unterschied zwischen der Uhr des Dienstes und der dieses Geräts in Millisekunden. */
    @Volatile var versatz: Long = 0L

    private val json = Json { ignoreUnknownKeys = true }

    private fun lies(text: String): JsonObject? = try { json.parseToJsonElement(text).jsonObject } catch (e: Exception) { null }

    private fun roh(pfad: String, kopf: Map<String, String>, body: String): FamilienAntwort {
        val (status, text) = leitung.post(pfad, kopf + ("Content-Type" to "application/json"), body)
        return FamilienAntwort(status, lies(text))
    }

    /** Legt eine neue Familie an. Dieses Gerät wird das erste Eltern-Gerät. */
    fun anlegen(name: String): FamilienAntwort {
        val pub = signierer.oeffentlich() ?: return FamilienAntwort(-1, null)
        val body = buildJsonObject {
            put("pub", FamilienKrypto.base64url(pub))
            put("name", name)
        }
        return roh(FamilienProtokoll.PFAD_FAMILIE, emptyMap(), body.toString())
    }

    /** Meldet dieses Gerät mit dem Einmal-Schlüssel aus dem QR-Code. Es wartet danach auf die Freigabe der Eltern. */
    fun koppeln(familie: String, token: String, name: String): FamilienAntwort {
        val pub = signierer.oeffentlich() ?: return FamilienAntwort(-1, null)
        val body = buildJsonObject {
            put("token", token)
            put("pub", FamilienKrypto.base64url(pub))
            put("name", name)
        }
        return roh(FamilienProtokoll.PFAD_KOPPELN, mapOf("Y-Familie" to familie), body.toString())
    }

    /** Eine unterschriebene Anfrage. Ist die Uhr des Geräts falsch, gleicht der Client sie aus und versucht es einmal neu. */
    fun signiert(zugang: FamilienZugang, pfad: String, body: JsonObject = JsonObject(emptyMap())): FamilienAntwort {
        val bodyText = body.toString()
        val bodyHash = FamilienKrypto.sha256Hex(bodyText)
        var antwort = FamilienAntwort(-1, null)
        for (versuch in 0..1) {
            val zeit = jetzt() + versatz
            val text = FamilienKrypto.unterschriftText("POST", pfad, zugang.familie, zugang.geraet, zeit, bodyHash)
            val der = signierer.signieren(text.toByteArray(Charsets.UTF_8)) ?: return FamilienAntwort(-1, null)
            val sig = FamilienKrypto.derZuRoh(der) ?: return FamilienAntwort(-1, null)
            antwort = roh(
                pfad,
                mapOf(
                    "Y-Familie" to zugang.familie,
                    "Y-Geraet" to zugang.geraet,
                    "Y-Zeit" to zeit.toString(),
                    "Y-Signatur" to FamilienKrypto.base64url(sig),
                ),
                bodyText,
            )
            if (antwort.status != 401 || versuch == 1) break
            val serverZeit = (antwort.json?.get("serverZeit") as? JsonPrimitive)?.longOrNull ?: break
            // Nur nachstellen, wenn die Uhr wirklich abweicht. Sonst liegt es an etwas anderem und ein zweiter Versuch hilft nicht.
            if (kotlin.math.abs(serverZeit - zeit) < 30_000) break
            versatz = serverZeit - jetzt()
        }
        return antwort
    }
}
