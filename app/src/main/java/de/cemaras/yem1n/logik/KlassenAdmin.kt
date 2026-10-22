package de.cemaras.yem1n.logik

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec

/**
 * Admin einer Klasse: Der Schlüssel entsteht auf dem Gerät, der private Teil verlässt es nie.
 * Der Server bekommt nur den öffentlichen Schlüssel und die Unterschriften, so wie bei iOS (Klassensiegel).
 */
@OptIn(ExperimentalEncodingApi::class)
object KlassenAdmin {
    const val MAX_JSON = 38000
    private val kurve = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)

    fun neuerSeed(zufall: SecureRandom = SecureRandom()): String = Base64.encode(ByteArray(32).also { zufall.nextBytes(it) })

    private fun spec(seed: String) = EdDSAPrivateKeySpec(Base64.decode(seed), kurve)

    fun oeffentlich(seed: String): String = Base64.encode(spec(seed).a.toByteArray())

    private fun signiere(seed: String, text: String): String {
        val e = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
        e.initSign(EdDSAPrivateKey(spec(seed)))
        return Base64.encode(e.signOneShot(text.toByteArray(Charsets.UTF_8)))
    }

    /** Gleicher Name wie bei iOS und auf dem Server: erneutes Senden ersetzt das Paket statt es zu verdoppeln. */
    fun paketName(klassencode: String, klasse: String, fach: String, titel: String): String {
        val sb = StringBuilder()
        var strich = false
        for (z in "$klasse-$fach-$titel".lowercase()) {
            if (z in 'a'..'z' || z in '0'..'9') { sb.append(z); strich = false } else if (!strich) { sb.append('-'); strich = true }
        }
        return "paket-K-$klassencode-$sb"
    }

    private fun kopf(code: String, art: String, daten: JsonObject) = buildJsonObject {
        put("code", code); put("art", art); put("daten", daten)
    }.toString()

    fun schluesselAnfrage(code: String, seed: String): String =
        kopf(code, "schluessel", buildJsonObject { put("publicKey", oeffentlich(seed)) })

    fun paketAnfrage(code: String, seed: String, json: String): String =
        kopf(code, "paket", buildJsonObject { put("json", json); put("sig", signiere(seed, "K-$code|$json")) })

    fun loeschenAnfrage(code: String, seed: String, name: String): String =
        kopf(code, "loeschen", buildJsonObject { put("name", name); put("sig", signiere(seed, "K-$code|loeschen|$name")) })
}
