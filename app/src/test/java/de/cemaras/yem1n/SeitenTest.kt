package de.cemaras.yem1n

import de.cemaras.yem1n.logik.JokerCloud
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeitenTest {
    private fun seite(von: Int, bis: Int, weiter: String?): String {
        val rec = (von until bis).joinToString(",") { """{"recordName":"r$it","fields":{}}""" }
        return """{"records":[$rec]""" + (if (weiter != null) ""","continuationMarker":"$weiter"""" else "") + "}"
    }

    @Test fun holtAlleSeiten() {
        val seiten = mapOf<String?, String>(null to seite(0, 2, "m1"), "m1" to seite(2, 4, "m2"), "m2" to seite(4, 5, null))
        val abfragen = mutableListOf<String>()
        val r = JokerCloud.alleSeiten("RundenErgebnis", "ABCDE-23456", 2, "zeitpunkt", 10) { q ->
            abfragen += q
            seiten[Regex("\"continuationMarker\":\"([^\"]+)\"").find(q)?.groupValues?.get(1)]
        }
        assertEquals(5, r!!.size)
        assertEquals(3, abfragen.size)
    }

    @Test fun begrenztDieSeiten() {
        val r = JokerCloud.alleSeiten("T", "C", 1, null, 3) { seite(0, 1, "weiter") }
        assertEquals(3, r!!.size)
    }

    @Test fun ohneVerbindung() {
        assertNull(JokerCloud.alleSeiten("T", "C", 1, null, 3) { null })
        val teil = JokerCloud.alleSeiten("T", "C", 1, null, 3) { q -> if ("continuationMarker" in q) null else seite(0, 1, "m") }
        assertEquals(1, teil!!.size)
    }
}
