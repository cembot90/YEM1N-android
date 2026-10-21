package de.cemaras.yem1n.logik

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Adressen der CloudKit-Web-Services. Dieselbe Datenbank wie in der iOS-App. */
object CloudAdresse {
    const val CONTAINER = "iCloud.de.cemaras.yem1n"
    const val HOST = "https://api.apple-cloudkit.com"

    /** Der Anmelde-Schlüssel muss so kodiert werden, wie Apple es verlangt. */
    fun kodiere(token: String): String = token.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D")

    /**
     * Adresse für eine Operation, zum Beispiel "users/current" oder "records/query".
     * [umgebung] ist "development" oder "production", [datenbank] meist "public".
     */
    fun url(
        operation: String, apiToken: String, webAuth: String? = null,
        umgebung: String = "development", datenbank: String = "public",
    ): String {
        val basis = "$HOST/database/1/$CONTAINER/$umgebung/$datenbank/$operation?ckAPIToken=$apiToken"
        return if (webAuth.isNullOrEmpty()) basis else "$basis&ckWebAuthToken=${kodiere(webAuth)}"
    }

    /** Liest den Schlüssel aus der Rückkehr-Adresse der App, zum Beispiel "yem1n://cloudkit?ckWebAuthToken=abc". */
    fun tokenAusRueckkehr(adresse: String): String? {
        val q = adresse.substringAfter('?', "").substringBefore('#')
        for (teil in q.split('&')) {
            val name = teil.substringBefore('=')
            if (name == "ckWebAuthToken") {
                val wert = teil.substringAfter('=', "")
                if (wert.isEmpty()) return null
                return entkodiere(wert)
            }
        }
        return null
    }

    /** Wie `decodeURIComponent`: ein Plus bleibt ein Plus. */
    internal fun entkodiere(s: String): String {
        val aus = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null) { aus.write(hex); i += 3; continue }
            }
            aus.write(c.toString().toByteArray(Charsets.UTF_8))
            i++
        }
        return aus.toString(Charsets.UTF_8.name())
    }
}

/** Was eine Antwort von CloudKit gesagt hat. */
data class CloudAntwort(
    val ok: Boolean,
    val fehlerCode: String? = null,
    val grund: String? = null,
    /** Bei AUTHENTICATION_REQUIRED: die Seite, auf der man sich bei Apple anmeldet. */
    val anmeldeUrl: String? = null,
    /** Neuer Schlüssel, wenn die Antwort einen mitgebracht hat. */
    val neuerToken: String? = null,
    val benutzer: String? = null,
) {
    val anmeldungNoetig: Boolean get() = fehlerCode == "AUTHENTICATION_REQUIRED"
}

object CloudAuswertung {
    private val json = Json { ignoreUnknownKeys = true }

    /** Liest eine Antwort von CloudKit. Kaputter Text gilt als Fehler, nicht als Absturz. */
    fun lies(text: String): CloudAntwort {
        val o: JsonObject = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return CloudAntwort(false, "UNGUELTIG", "Die Antwort von Apple war nicht lesbar.")
        }
        fun s(k: String): String? = try { o[k]?.jsonPrimitive?.contentOrNull } catch (e: Exception) { null }
        val fehler = s("serverErrorCode")
        return CloudAntwort(
            ok = fehler == null,
            fehlerCode = fehler,
            grund = s("reason"),
            anmeldeUrl = s("redirectURL"),
            neuerToken = s("ckWebAuthToken"),
            benutzer = s("userRecordName"),
        )
    }
}
