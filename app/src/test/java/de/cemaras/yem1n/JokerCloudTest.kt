package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Familiencode
import de.cemaras.yem1n.logik.JokerCloud
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

    @Test fun abfrageFiltertNachCode() {
        val o = Json.parseToJsonElement(JokerCloud.abfrage("JokerAnfrage", "ABC")).jsonObject
        val f = o["query"]!!.jsonObject["filterBy"]!!.jsonArray[0].jsonObject
        assertEquals("familienCode", f["fieldName"]!!.jsonPrimitive.content)
        assertEquals("200", o["resultsLimit"]!!.jsonPrimitive.content)
    }

    @Test fun abfrageKannNeuesteZuerstSortieren() {
        val o = Json.parseToJsonElement(JokerCloud.abfrage("RundenErgebnis", "ABC", 300, "zeitpunkt")).jsonObject
        val sortierung = o["sortBy"]!!.jsonArray[0].jsonObject
        assertEquals("zeitpunkt", sortierung["fieldName"]!!.jsonPrimitive.content)
        assertEquals("false", sortierung["ascending"]!!.jsonPrimitive.content)
        assertTrue("sortBy" !in Json.parseToJsonElement(JokerCloud.abfrage("JokerAnfrage", "ABC")).jsonObject)
    }

    private fun rec(name: String, zeit: Long, vararg f: Pair<String, String>): String {
        val felder = f.joinToString(",") { "\"${it.first}\":{\"value\":\"${it.second}\",\"type\":\"STRING\"}" }
        return "{\"recordName\":\"$name\",\"created\":{\"timestamp\":$zeit},\"fields\":{$felder}}"
    }

    @Test fun datensaetzeWerdenGelesenKaputteUebersprungen() {
        val text = "{\"records\":[" + rec("a1", 100, "titel" to "Mathe") + ",{\"kaputt\":true}," + rec("a2", 200) + "]}"
        val liste = JokerCloud.datensaetze(text)
        assertEquals(listOf("a1", "a2"), liste.map { it.name })
        assertEquals("Mathe", liste[0].text("titel"))
        assertEquals(100L, liste[0].erstellt)
        assertEquals("ohne", liste[1].text("titel", "ohne"))
    }

    @Test fun kaputteAntwortGibtLeereListe() {
        assertTrue(JokerCloud.datensaetze("kein json").isEmpty())
        assertTrue(JokerCloud.datensaetze("{\"x\":1}").isEmpty())
    }
}
