package de.cemaras.yem1n

import de.cemaras.yem1n.logik.FamilienClient
import de.cemaras.yem1n.logik.FamilienKrypto
import de.cemaras.yem1n.logik.FamilienLeitung
import de.cemaras.yem1n.logik.FamilienProtokoll
import de.cemaras.yem1n.logik.FamilienZugang
import de.cemaras.yem1n.logik.GeraetSignierer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/** Ein Schlüssel in Software, der sich wie der Keystore verhält (öffentlicher Teil als 65 Bytes, Unterschrift im DER-Format). */
private class SoftwareSchluessel : GeraetSignierer {
    private val paar = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    override fun oeffentlich(): ByteArray? = FamilienKrypto.pubAusSpki(paar.public.encoded)
    override fun signieren(daten: ByteArray): ByteArray? =
        Signature.getInstance("SHA256withECDSA").run { initSign(paar.private); update(daten); sign() }
}

/** Ein Dienst, der so prüft wie der Worker: Unterschrift, Gerät, Zeitfenster von 5 Minuten. */
private class FalscherDienst(val serverUhr: () -> Long) : FamilienLeitung {
    class Aufruf(val pfad: String, val kopf: Map<String, String>, val body: String, val status: Int)
    val aufrufe = mutableListOf<Aufruf>()
    var bekanntePub: ByteArray? = null
    var offline = false

    private fun rohZuDer(roh: ByteArray): ByteArray {
        fun ganz(b: ByteArray): ByteArray {
            var i = 0
            while (i < b.size - 1 && b[i].toInt() == 0) i++
            val k = b.copyOfRange(i, b.size)
            return if (k[0] < 0) byteArrayOf(0) + k else k
        }
        val r = ganz(roh.copyOfRange(0, 32))
        val s = ganz(roh.copyOfRange(32, 64))
        val inhalt = byteArrayOf(2, r.size.toByte()) + r + byteArrayOf(2, s.size.toByte()) + s
        return byteArrayOf(0x30, inhalt.size.toByte()) + inhalt
    }

    /** Der feste Vorspann einer X.509-Hülle für einen P-256-Schlüssel, danach folgen die 65 Bytes des Punkts. */
    private fun spki(pub: ByteArray): ByteArray {
        val vorspann = "3059301306072a8648ce3d020106082a8648ce3d030107034200"
        return ByteArray(vorspann.length / 2) { vorspann.substring(it * 2, it * 2 + 2).toInt(16).toByte() } + pub
    }

    override fun post(pfad: String, kopf: Map<String, String>, body: String): Pair<Int, String> {
        if (offline) { aufrufe += Aufruf(pfad, kopf, body, 0); return 0 to "" }
        var status = 200
        var antwort = """{"ok":true}"""
        if (pfad == FamilienProtokoll.PFAD_FAMILIE || pfad == FamilienProtokoll.PFAD_KOPPELN) {
            val pub = Json.parseToJsonElement(body).jsonObject["pub"]!!.jsonPrimitive.content
            bekanntePub = FamilienKrypto.base64urlLesen(pub)
        } else {
            val zeit = kopf["Y-Zeit"]!!.toLong()
            val text = FamilienKrypto.unterschriftText("POST", pfad, kopf["Y-Familie"]!!, kopf["Y-Geraet"]!!, zeit, FamilienKrypto.sha256Hex(body))
            val pub = bekanntePub!!
            val pruefer = Signature.getInstance("SHA256withECDSA")
            pruefer.initVerify(KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(spki(pub))))
            pruefer.update(text.toByteArray(Charsets.UTF_8))
            val sig = FamilienKrypto.base64urlLesen(kopf["Y-Signatur"]!!)!!
            val geraetOk = FamilienKrypto.geraetId(pub) == kopf["Y-Geraet"]
            val jetzt = serverUhr()
            when {
                !geraetOk || sig.size != 64 || !pruefer.verify(rohZuDer(sig)) -> { status = 401; antwort = """{"fehler":"Unterschrift passt nicht"}""" }
                kotlin.math.abs(jetzt - zeit) > 5 * 60_000 -> { status = 401; antwort = """{"fehler":"Uhr","serverZeit":$jetzt}""" }
            }
        }
        aufrufe += Aufruf(pfad, kopf, body, status)
        return status to antwort
    }
}

class FamilienClientTest {
    private val jetzt = 1_800_000_000_000L
    private val schluessel = SoftwareSchluessel()
    private val dienst = FalscherDienst { jetzt }

    private fun client(uhr: () -> Long = { jetzt }) = FamilienClient(dienst, schluessel, uhr)
    private fun zugang(c: FamilienClient): FamilienZugang {
        val pub = schluessel.oeffentlich()!!
        c.anlegen("Mama")
        return FamilienZugang("a".repeat(64), FamilienKrypto.geraetId(pub), "Mama", "eltern", "aktiv")
    }

    @Test fun anlegenSendetSchluesselUndNameOhneUnterschrift() {
        val c = client()
        val a = c.anlegen("Mama")
        assertTrue(a.ok)
        val aufruf = dienst.aufrufe.single()
        assertEquals("/v2/familie", aufruf.pfad)
        assertFalse(aufruf.kopf.containsKey("Y-Signatur"))
        val o = Json.parseToJsonElement(aufruf.body).jsonObject
        assertEquals("Mama", o["name"]!!.jsonPrimitive.content)
        assertEquals(65, FamilienKrypto.base64urlLesen(o["pub"]!!.jsonPrimitive.content)!!.size)
    }

    @Test fun koppelnSchicktDieFamilieImKopf() {
        val c = client()
        c.koppeln("b".repeat(64), "T".repeat(22), "Yemin")
        val aufruf = dienst.aufrufe.single()
        assertEquals("b".repeat(64), aufruf.kopf["Y-Familie"])
        assertEquals("T".repeat(22), Json.parseToJsonElement(aufruf.body).jsonObject["token"]!!.jsonPrimitive.content)
    }

    @Test fun signierteAnfrageWirdVomDienstAkzeptiert() {
        val c = client()
        val z = zugang(c)
        val a = c.signiert(z, FamilienProtokoll.PFAD_SENDEN, FamilienProtokoll.frage("frage-0001", "Yemin", "Mathe", "Geteilt", "Was ist 56 : 7?"))
        assertTrue(a.ok)
        val k = dienst.aufrufe.last().kopf
        assertEquals(z.geraet, k["Y-Geraet"])
        assertEquals(jetzt.toString(), k["Y-Zeit"])
    }

    @Test fun jedeAnfrageHatEineNeueUnterschrift() {
        val c = client()
        val z = zugang(c)
        c.signiert(z, FamilienProtokoll.PFAD_STATUS)
        c.signiert(z, FamilienProtokoll.PFAD_STATUS)
        val sig = dienst.aufrufe.takeLast(2).map { it.kopf["Y-Signatur"] }
        assertTrue(sig[0] != sig[1])
    }

    @Test fun falscheUhrWirdEinmalAusgeglichen() {
        val c = client(uhr = { jetzt - 3_600_000 }) // das Gerät geht eine Stunde nach
        val z = zugang(c)
        val a = c.signiert(z, FamilienProtokoll.PFAD_STATUS)
        assertTrue(a.ok)
        val letzte = dienst.aufrufe.takeLast(2)
        assertEquals(401, letzte[0].status)
        assertEquals(200, letzte[1].status)
        assertTrue(kotlin.math.abs(c.versatz - 3_600_000) < 5)
        // Die nächste Anfrage braucht keinen zweiten Versuch mehr.
        val vorher = dienst.aufrufe.size
        assertTrue(c.signiert(z, FamilienProtokoll.PFAD_STATUS).ok)
        assertEquals(vorher + 1, dienst.aufrufe.size)
    }

    @Test fun beiAndererUrsacheKeinZweiterVersuch() {
        val c = client()
        val z = zugang(c).copy(geraet = "0".repeat(24)) // ein Gerät, das der Dienst nicht kennt
        val vorher = dienst.aufrufe.size
        val a = c.signiert(z, FamilienProtokoll.PFAD_STATUS)
        assertEquals(401, a.status)
        assertEquals(vorher + 1, dienst.aufrufe.size)
    }

    @Test fun ohneVerbindungKommtStatusNull() {
        val c = client()
        val z = zugang(c)
        dienst.offline = true
        val a = c.signiert(z, FamilienProtokoll.PFAD_STATUS)
        assertTrue(a.keineVerbindung)
        assertEquals("Keine Verbindung. Ist das Internet an?", a.fehler)
    }

    @Test fun ohneSchluesselKommtMinusEins() {
        val leer = object : GeraetSignierer {
            override fun oeffentlich(): ByteArray? = null
            override fun signieren(daten: ByteArray): ByteArray? = null
        }
        val c = FamilienClient(dienst, leer) { jetzt }
        assertEquals(-1, c.anlegen("Mama").status)
        assertEquals(-1, c.signiert(FamilienZugang("a".repeat(64), "b".repeat(24), "x", "kind", "aktiv"), "/v2/status").status)
    }
}
