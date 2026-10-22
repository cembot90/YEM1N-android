package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Ablehnung
import de.cemaras.yem1n.logik.JokerCloud
import de.cemaras.yem1n.logik.Klassensiegel
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.JsonPrimitive
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalEncodingApi::class)
class KlassensiegelTest {
    private val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private val seed = hex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60")
    private val oeffentlich = hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a")
    private val code = "K-ABCDE-23456"

    private fun signiere(json: String): String {
        val e = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
        e.initSign(EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, spec)))
        return Base64.encode(e.signOneShot("$code|$json".toByteArray(Charsets.UTF_8)))
    }

    private fun paket(json: String, sig: String) =
        JokerCloud.Datensatz("p", 0, mapOf("json" to JsonPrimitive(json), "sig" to JsonPrimitive(sig)))

    @Test fun rfc8032Vektor() {
        val sig = hex("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b")
        val e = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
        e.initVerify(net.i2p.crypto.eddsa.EdDSAPublicKey(net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec(oeffentlich, spec)))
        assertTrue(e.verifyOneShot(ByteArray(0), sig))
    }

    @Test fun cloudCode() {
        assertEquals("K-ABCDE-23456", Klassensiegel.cloudCode("abcde23456"))
        assertNull(Klassensiegel.cloudCode("zu kurz"))
    }

    @Test fun gueltigeUnterschrift() {
        val json = """{"klasse":"Klasse 3"}"""
        assertTrue(Klassensiegel.pruefe(code, json, signiere(json), oeffentlich))
        assertFalse(Klassensiegel.pruefe(code, json + " ", signiere(json), oeffentlich))
        assertFalse(Klassensiegel.pruefe("K-ZZZZZ-23456", json, signiere(json), oeffentlich))
        assertFalse(Klassensiegel.pruefe(code, json, "kein base64 !!", oeffentlich))
    }

    @Test fun ersterKontaktMerktSchluessel() {
        val json = "{}"
        val key = Klassensiegel.schluesselText(oeffentlich)
        val r = Klassensiegel.pruefen(code, listOf(paket(json, signiere(json))), null, key)
        assertEquals(listOf(json), r.angenommen)
        assertEquals(key, r.neuerSchluessel)
    }

    @Test fun gewechselterSchluesselWirdAbgelehnt() {
        val json = "{}"
        val key = Klassensiegel.schluesselText(oeffentlich)
        val anderer = Klassensiegel.schluesselText(ByteArray(32) { 1 })
        val r = Klassensiegel.pruefen(code, listOf(paket(json, signiere(json))), anderer, key)
        assertTrue(r.angenommen.isEmpty())
        assertEquals(setOf(Ablehnung.SCHLUESSEL_GEWECHSELT), r.gruende)
    }

    @Test fun ohneUnterschriftUndOhneSchluessel() {
        val key = Klassensiegel.schluesselText(oeffentlich)
        assertEquals(setOf(Ablehnung.OHNE_UNTERSCHRIFT), Klassensiegel.pruefen(code, listOf(paket("{}", "")), null, key).gruende)
        assertEquals(setOf(Ablehnung.KEIN_SCHLUESSEL), Klassensiegel.pruefen(code, listOf(paket("{}", "abc")), null, null).gruende)
        assertEquals(setOf(Ablehnung.UNTERSCHRIFT_FALSCH), Klassensiegel.pruefen(code, listOf(paket("{}", signiere("{x}"))), null, key).gruende)
    }
}
