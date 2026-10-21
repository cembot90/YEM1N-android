package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Familiencode
import de.cemaras.yem1n.logik.JokerCloud
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class JokerCloudTest {
    private fun satz(text: String) = Json.parseToJsonElement(text).jsonObject["operations"]!!
        .jsonArray[0].jsonObject["record"]!!.jsonObject

    @Test fun codePruefen() {
        assertEquals("ABCDE-23456", Familiencode.pruefe("abcde-23456"))
        assertEquals("ABCDE-23456", Familiencode.pruefe(" ABCDE 23456 "))
        assertEquals("ABCDE-23456", Familiencode.pruefe("ABCDE23456"))
        assertNull(Familiencode.pruefe("ABCDE-2345"))
        assertNull(Familiencode.pruefe("ABCDE-2345O"))
        assertEquals("ABCDE-23456", Familiencode.zeige("ABCDE23456"))
        assertEquals("ABCDE-23456", Familiencode.zeige("ABCDE-23456"))
    }

    @Test fun codeNeuIstGueltig() {
        repeat(50) {
            val c = Familiencode.neu(Random(it))
            assertNotNull(Familiencode.pruefe(c))
            assertEquals(11, c.length)
            assertEquals('-', c[5])
        }
    }

    @Test fun sterneWieIos() {
        assertEquals(3, JokerCloud.sterne(9, 10))
        assertEquals(2, JokerCloud.sterne(7, 10))
        assertEquals(1, JokerCloud.sterne(5, 10))
        assertEquals(0, JokerCloud.sterne(4, 10))
        assertEquals(0, JokerCloud.sterne(0, 0))
    }

    @Test fun frageEnthaeltAlleFelder() {
        val r = satz(JokerCloud.frageSenden("ABCDE23456", "Yemin", "Mathe", "Einmaleins", "Hilfe"))
        assertEquals("JokerAnfrage", r["recordType"]!!.jsonPrimitive.content)
        val f = r["fields"]!!.jsonObject
        assertEquals("offen", f["status"]!!.jsonObject["value"]!!.jsonPrimitive.content)
        assertEquals("ABCDE23456", f["familienCode"]!!.jsonObject["value"]!!.jsonPrimitive.content)
    }

    @Test fun daumenHatFestenNamen() {
        assertEquals("daumen-w9", satz(JokerCloud.daumenSenden("C", "a1", "w9", "Mama"))["recordName"]!!.jsonPrimitive.content)
    }

    @Test fun ergebnisOhneDetailsLaesstFeldWeg() {
        val e = RundenErgebnisEintrag("id1", "Klasse 3", "Mathe", "Nr. 1", "Geteilt", 8, 10, 1, 2, "Yemin", 1000L)
        val r = satz(JokerCloud.ergebnisSenden("C", e))
        assertEquals("erg-id1", r["recordName"]!!.jsonPrimitive.content)
        assertTrue("details" !in r["fields"]!!.jsonObject)
        assertEquals("8", r["fields"]!!.jsonObject["richtig"]!!.jsonObject["value"]!!.jsonPrimitive.content)
    }

    @Test fun abfrageFiltertNachCode() {
        val o = Json.parseToJsonElement(JokerCloud.abfrage("JokerAnfrage", "ABC")).jsonObject
        val f = o["query"]!!.jsonObject["filterBy"]!!.jsonArray[0].jsonObject
        assertEquals("familienCode", f["fieldName"]!!.jsonPrimitive.content)
        assertEquals("200", o["resultsLimit"]!!.jsonPrimitive.content)
    }

    private fun rec(name: String, zeit: Long, vararg f: Pair<String, String>): String {
        val felder = f.joinToString(",") { "\"${it.first}\":{\"value\":\"${it.second}\",\"type\":\"STRING\"}" }
        return "{\"recordName\":\"$name\",\"created\":{\"timestamp\":$zeit},\"fields\":{$felder}}"
    }

    @Test fun jokerZusammensetzen() {
        val fragen = JokerCloud.datensaetze("{\"records\":[" +
            rec("a1", 100, "kind" to "Yemin", "text" to "alt") + "," +
            rec("a2", 200, "kind" to "Yemin", "text" to "neu") + "]}")
        val antworten = JokerCloud.datensaetze("{\"records\":[" +
            rec("w2", 400, "anfrageID" to "a2", "absender" to "Papa") + "," +
            rec("w1", 300, "anfrageID" to "a2", "absender" to "Mama", "emoji" to "😀") + "]}")
        val daumen = JokerCloud.datensaetze("{\"records\":[" + rec("daumen-w1", 500, "antwortID" to "w1") + "]}")
        val liste = JokerCloud.fragen(fragen, antworten, daumen)
        assertEquals(listOf("a2", "a1"), liste.map { it.id })
        assertEquals(listOf("w1", "w2"), liste[0].antworten.map { it.id })
        assertTrue(liste[0].antworten[0].daumen)
        assertTrue(!liste[0].antworten[1].daumen)
        assertEquals("🙂", liste[0].antworten[1].emoji)
        assertTrue(liste[1].antworten.isEmpty())
    }

    @Test fun kaputteAntwortGibtLeereListe() {
        assertTrue(JokerCloud.datensaetze("kein json").isEmpty())
        assertTrue(JokerCloud.datensaetze("{\"x\":1}").isEmpty())
    }

    @Test fun ergebnisseLesen() {
        val text = "{\"records\":[{\"recordName\":\"erg-x\",\"created\":{\"timestamp\":50},\"fields\":{" +
            "\"eintragID\":{\"value\":\"x\"},\"richtig\":{\"value\":7},\"gesamt\":{\"value\":10}," +
            "\"sterne\":{\"value\":2},\"kind\":{\"value\":\"Evîn\"}}}]}"
        val e = JokerCloud.ergebnisse(JokerCloud.datensaetze(text)).single()
        assertEquals(7, e.richtig)
        assertEquals(10, e.gesamt)
        assertEquals("Evîn", e.kind)
        assertEquals(50L, e.zeitpunkt)
    }
}
