package de.cemaras.yem1n

import de.cemaras.yem1n.logik.FamilienEintrag
import de.cemaras.yem1n.logik.FamilienPakete
import de.cemaras.yem1n.logik.FamilienProtokoll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilienPaketeTest {
    private fun arbeit(titel: String, klasse: String = "Klasse 3") =
        """{"klasse":"$klasse","fach":"Mathe","arbeit":"$titel","uebungen":[{"titel":"Plus","aufgaben":[{"art":"zahl","frage":"2 + 3","antwort":5}]}]}"""

    private fun eintrag(id: String, json: String, erstellt: Long, typ: String = FamilienProtokoll.TYP_PAKET) =
        FamilienEintrag(1, typ, id, "geraet", "Eltern", erstellt, buildJsonObject { put("id", id); put("json", json) })

    @Test fun kennungWieBeiIos() {
        // "p-" und die ersten 20 Zeichen von SHA-256 über "klasse|fach|titel", wie PaketJSON.familienKennung
        assertEquals("p-b31b20f0af5180c0e187", FamilienProtokoll.paketKennung("Klasse 3", "Mathe", "Plus bis 100"))
        assertTrue(FamilienProtokoll.kennungOk(FamilienProtokoll.paketKennung("Klasse 3", "Mathe", "Plus bis 100")))
    }

    @Test fun koerperZumSenden() {
        val k = FamilienProtokoll.paket("p-1234567890", arbeit("Test"))
        assertEquals("paket", k["typ"]!!.jsonPrimitive.content)
        val d = k["daten"]!!.jsonObject
        assertEquals("p-1234567890", d["id"]!!.jsonPrimitive.content)
        assertEquals(arbeit("Test"), d["json"]!!.jsonPrimitive.content)
    }

    @Test fun lesenUeberspringtKaputteUndSortiert() {
        val liste = FamilienPakete.lesen(
            listOf(
                eintrag("p-bbbbbbbb", arbeit("Zwei"), 20),
                eintrag("p-kaputt00", "{\"klasse\":1}", 15),
                eintrag("p-aaaaaaaa", arbeit("Eins"), 10),
                eintrag("frage-1234", arbeit("Falscher Typ"), 5, typ = FamilienProtokoll.TYP_FRAGE),
            ),
        )
        assertEquals(listOf("Eins", "Zwei"), liste.map { it.titel })
        assertEquals("Klasse 3|Mathe|Eins", liste.first().schluessel)
    }

    @Test fun nurNeueArbeitenKommenDazu() {
        val pakete = FamilienPakete.lesen(
            listOf(
                eintrag("p-aaaaaaaa", arbeit("Eins"), 1),
                eintrag("p-bbbbbbbb", arbeit("Zwei"), 2),
                eintrag("p-cccccccc", arbeit("Drei"), 3),
                eintrag("p-dddddddd", arbeit("Drei"), 4),
            ),
        )
        val neu = FamilienPakete.neue(pakete, vorhanden = setOf("Klasse 3|Mathe|Eins"), versteckt = setOf("Klasse 3|Mathe|Zwei"))
        assertEquals(listOf("p-cccccccc"), neu.map { it.id })
    }

    @Test fun zuGrossWirdAbgelehnt() {
        assertNull(FamilienPakete.pruefeGroesse(arbeit("Klein")))
        assertNotNull(FamilienPakete.pruefeGroesse("x".repeat(FamilienProtokoll.MAX_PAKET + 1)))
    }
}
