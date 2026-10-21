package de.cemaras.yem1n

import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import de.cemaras.yem1n.logik.ServerAnfrage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAnfrageTest {
    private fun lies(t: String) = Json.parseToJsonElement(t).jsonObject

    @Test fun frageHatKopfUndDaten() {
        val o = lies(ServerAnfrage.frage("ABCDE23456", " Yemin ", "Mathe", "Geteilt", " Hilfe "))
        assertEquals("ABCDE23456", o["code"]!!.jsonPrimitive.content)
        assertEquals("frage", o["art"]!!.jsonPrimitive.content)
        val d = o["daten"]!!.jsonObject
        assertEquals("Yemin", d["kind"]!!.jsonPrimitive.content)
        assertEquals("Hilfe", d["text"]!!.jsonPrimitive.content)
    }

    @Test fun leererNameWirdZuKind() {
        val d = lies(ServerAnfrage.frage("C", "", "", "", "x"))["daten"]!!.jsonObject
        assertEquals("Kind", d["kind"]!!.jsonPrimitive.content)
    }

    @Test fun langeTexteWerdenGekuerzt() {
        val d = lies(ServerAnfrage.frage("C", "K", "", "", "a".repeat(900)))["daten"]!!.jsonObject
        assertEquals(500, d["text"]!!.jsonPrimitive.content.length)
    }

    @Test fun antwortUndDaumen() {
        val a = lies(ServerAnfrage.antwort("C", "f1", "", "👍", "Rechne 6 mal 7"))["daten"]!!.jsonObject
        assertEquals("Eltern", a["absender"]!!.jsonPrimitive.content)
        assertEquals("f1", a["frageId"]!!.jsonPrimitive.content)
        val d = lies(ServerAnfrage.daumen("C", "f1", "w1", "Yemin"))["daten"]!!.jsonObject
        assertEquals("w1", d["antwortId"]!!.jsonPrimitive.content)
    }

    @Test fun ergebnisMitZahlen() {
        val e = RundenErgebnisEintrag("id", "Klasse 3", "Mathe", "Nr. 1", "Geteilt", 8, 10, 0, 2, "Yemin", 1000L)
        val d = lies(ServerAnfrage.ergebnis("C", e))["daten"]!!.jsonObject
        assertEquals("8", d["richtig"]!!.jsonPrimitive.content)
        assertFalse("details" in d)
        assertTrue(ServerAnfrage.ergebnis("C", e.copy(details = "[]")).contains("details"))
    }

    @Test fun geraet() {
        val o = lies(ServerAnfrage.geraet("C", "eltern", "tok"))
        assertEquals("eltern", o["rolle"]!!.jsonPrimitive.content)
        assertEquals("tok", o["token"]!!.jsonPrimitive.content)
    }
}
