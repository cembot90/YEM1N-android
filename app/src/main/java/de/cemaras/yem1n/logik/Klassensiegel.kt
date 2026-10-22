package de.cemaras.yem1n.logik

import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec

/** Warum ein Paket der Klasse abgelehnt wurde, in Worten für die Eltern (gleiche Texte wie bei iOS). */
enum class Ablehnung(val text: String) {
    OHNE_UNTERSCHRIFT("Das Paket hat keine Unterschrift. Der Admin der Klasse sollte es noch einmal an den Klassencode senden."),
    KEIN_SCHLUESSEL("In der Cloud liegt kein Schlüssel für diesen Klassencode. Der Admin der Klasse sollte ein Paket neu senden."),
    SCHLUESSEL_GEWECHSELT("Der Schlüssel der Klasse hat sich geändert. Klassencode in den Einstellungen entfernen und neu eintragen."),
    UNTERSCHRIFT_FALSCH("Die Unterschrift passt nicht zum Schlüssel der Klasse. Der Admin der Klasse sollte das Paket neu senden."),
}

/** Ergebnis der Prüfung aller Pakete einer Klasse. */
class KlassenErgebnis(val angenommen: List<String>, val abgelehnt: Int, val gruende: Set<Ablehnung>, val neuerSchluessel: String?)

/**
 * Pakete einer Klasse müssen vom Admin mit Ed25519 unterschrieben sein, genau wie in der iOS-App.
 * Unterschrieben wird der Text "K-XXXXX-XXXXX|<json>". Der öffentliche Schlüssel wird beim ersten Mal gemerkt.
 */
object Klassensiegel {
    /** In der Cloud trägt der Klassencode ein "K-", damit er nie mit einem Familiencode verwechselt wird. */
    fun cloudCode(code: String): String? = Familiencode.pruefe(code)?.let { "K-$it" }

    /** Name des Datensatzes mit dem öffentlichen Schlüssel. */
    fun schluesselName(cloudCode: String): String = "klassenkey-$cloudCode"

    @OptIn(ExperimentalEncodingApi::class)
    private fun b64(s: String): ByteArray? = try { Base64.decode(s.trim()) } catch (e: Exception) { null }

    @OptIn(ExperimentalEncodingApi::class)
    fun pruefe(cloudCode: String, json: String, sig: String, oeffentlich: ByteArray): Boolean {
        val sd = b64(sig) ?: return false
        return try {
            val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
            val pk = EdDSAPublicKey(EdDSAPublicKeySpec(oeffentlich, spec))
            val engine = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
            engine.initVerify(pk)
            engine.verifyOneShot("$cloudCode|$json".toByteArray(Charsets.UTF_8), sd)
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun schluesselText(b: ByteArray): String = Base64.encode(b)

    /**
     * Prüft die Pakete. [gemerkt] ist der beim ersten Mal gemerkte Schlüssel (Base64), [geholt] der aus der Cloud.
     * Ein anderer Schlüssel als der gemerkte wird abgelehnt.
     */
    fun pruefen(cloudCode: String, pakete: List<JokerCloud.Datensatz>, gemerkt: String?, geholt: String?): KlassenErgebnis {
        val gruende = mutableSetOf<Ablehnung>()
        var neu: String? = null
        val verwendet: String? = when {
            gemerkt != null && geholt != null && gemerkt != geholt -> { gruende += Ablehnung.SCHLUESSEL_GEWECHSELT; null }
            gemerkt != null -> gemerkt
            geholt != null -> { neu = geholt; geholt }
            else -> null
        }
        val key = verwendet?.let { b64(it) }
        val gut = mutableListOf<String>()
        var schlecht = 0
        for (p in pakete) {
            val json = p.text("json")
            val sig = p.text("sig")
            val grund = when {
                json.isEmpty() || sig.isEmpty() -> Ablehnung.OHNE_UNTERSCHRIFT
                key == null -> if (Ablehnung.SCHLUESSEL_GEWECHSELT in gruende) Ablehnung.SCHLUESSEL_GEWECHSELT else Ablehnung.KEIN_SCHLUESSEL
                !pruefe(cloudCode, json, sig, key) -> Ablehnung.UNTERSCHRIFT_FALSCH
                else -> null
            }
            if (grund == null) gut += json else { schlecht++; gruende += grund }
        }
        // Der Schlüssel wird nur gemerkt, wenn er wirklich gebraucht wurde und etwas daran hing.
        return KlassenErgebnis(gut, schlecht, gruende, if (key != null) neu else null)
    }

    fun abfrageSchluessel(cloudCode: String): String = """{"records":[{"recordName":"${schluesselName(cloudCode)}"}]}"""
}
