package de.cemaras.yem1n

import de.cemaras.yem1n.logik.FamilienKrypto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Prüft die Rechnung gegen die gemeinsamen Testvektoren (`cloud/testvektoren-v2.json`), die auch Worker und iPhone-App bestehen müssen. */
class FamilienKryptoTest {
    private fun vektoren(): JsonObject {
        val kandidaten = listOf("../cloud/testvektoren-v2.json", "cloud/testvektoren-v2.json")
        val datei = kandidaten.map { File(it) }.first { it.exists() }
        return Json.parseToJsonElement(datei.readText(Charsets.UTF_8)).jsonObject
    }

    private fun s(o: JsonObject, k: String) = o[k]!!.jsonPrimitive.content

    @Test fun schluesselUndKennungStimmen() {
        val v = vektoren()
        val g = v["geraet"]!!.jsonObject
        val pub = FamilienKrypto.base64urlLesen(s(g, "pub"))!!
        assertEquals(65, pub.size)
        assertEquals(s(g, "pubHex"), FamilienKrypto.hex(pub))
        assertEquals(s(g, "geraetId"), FamilienKrypto.geraetId(pub))
        val spki = Base64.getDecoder().decode(s(g, "spkiBase64"))
        assertArrayEquals(pub, FamilienKrypto.pubAusSpki(spki))
    }

    @Test fun unterschriftTextUndHashStimmen() {
        val a = vektoren()["anfrage"]!!.jsonObject
        assertEquals(s(a, "bodyHash"), FamilienKrypto.sha256Hex(s(a, "body")))
        val text = FamilienKrypto.unterschriftText(
            s(a, "methode"), s(a, "pfad"), s(a, "familie"), s(a, "geraet"), a["zeit"]!!.jsonPrimitive.content.toLong(), s(a, "bodyHash"),
        )
        assertEquals(s(a, "nachricht"), text)
    }

    @Test fun derUnterschriftWirdZuRohemRundS() {
        val v = vektoren()
        val spki = Base64.getDecoder().decode(s(v["geraet"]!!.jsonObject, "spkiBase64"))
        val nachricht = s(v["anfrage"]!!.jsonObject, "nachricht")
        val d = v["derUmrechnung"]!!.jsonObject
        val der = Base64.getDecoder().decode(s(d, "derBase64"))
        // Die DER-Unterschrift gilt wirklich für diese Nachricht ...
        val pruefer = Signature.getInstance("SHA256withECDSA")
        pruefer.initVerify(KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(spki)))
        pruefer.update(nachricht.toByteArray(Charsets.UTF_8))
        assertTrue(pruefer.verify(der))
        // ... und die Umrechnung liefert genau die erwarteten 64 Bytes.
        assertEquals(s(d, "rohBase64url"), FamilienKrypto.base64url(FamilienKrypto.derZuRoh(der)!!))
    }

    @Test fun kaputteDerWirdAbgelehnt() {
        val der = Base64.getDecoder().decode(s(vektoren()["derUmrechnung"]!!.jsonObject, "derBase64"))
        assertNull(FamilienKrypto.derZuRoh(der.copyOf(der.size - 1)))
        assertNull(FamilienKrypto.derZuRoh(byteArrayOf()))
        assertNull(FamilienKrypto.derZuRoh(byteArrayOf(0x31, 0x00)))
        assertNull(FamilienKrypto.derZuRoh(der + byteArrayOf(0)))
    }

    @Test fun kleineZahlenWerdenAufDreiunddreissigAufgefuellt() {
        val roh = FamilienKrypto.derZuRoh(byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02))!!
        assertEquals(64, roh.size)
        assertEquals(1, roh[31].toInt())
        assertEquals(2, roh[63].toInt())
        assertEquals(0, roh[0].toInt())
    }

    @Test fun fuehrendesNullbyteWirdGestrichen() {
        // r = 0x00 0x81 ... (33 Bytes, weil das erste Bit gesetzt ist), s = 1
        val r = ByteArray(33).also { it[1] = 0x81.toByte(); it[32] = 7 }
        val der = byteArrayOf(0x30, (2 + 33 + 3).toByte(), 0x02, 33) + r + byteArrayOf(0x02, 0x01, 0x01)
        val roh = FamilienKrypto.derZuRoh(der)!!
        assertEquals(0x81.toByte(), roh[0])
        assertEquals(7, roh[31].toInt())
        assertEquals(1, roh[63].toInt())
    }

    @Test fun base64urlGehtHinUndZurueck() {
        val v = vektoren()["base64url"]!!.jsonObject
        fun bytes(hex: String) = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        assertEquals(s(v, "kodiert"), FamilienKrypto.base64url(bytes(s(v, "hex"))))
        assertEquals(s(v, "kodiertZwei"), FamilienKrypto.base64url(bytes(s(v, "hexZwei"))))
        assertArrayEquals(bytes(s(v, "hexZwei")), FamilienKrypto.base64urlLesen(s(v, "kodiertZwei")))
        for (n in 0..40) {
            val b = ByteArray(n) { (it * 37 + n).toByte() }
            assertArrayEquals(b, FamilienKrypto.base64urlLesen(FamilienKrypto.base64url(b)))
        }
        assertNull(FamilienKrypto.base64urlLesen("ab+/"))
        assertNull(FamilienKrypto.base64urlLesen("a"))
        assertNotNull(FamilienKrypto.base64urlLesen(""))
    }

    @Test fun ungueltigeBeispieleSindFuerDieAnderenPlattformenDa() {
        // Die Liste muss da sein, damit Swift und Worker sie benutzen können.
        assertTrue(vektoren()["ungueltig"]!!.jsonArray.size >= 4)
    }
}
