package de.cemaras.yem1n

import de.cemaras.yem1n.daten.PaketLeser
import de.cemaras.yem1n.logik.KlassenAdmin
import de.cemaras.yem1n.logik.Klassensiegel
import de.cemaras.yem1n.logik.PaketJson
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalEncodingApi::class)
class KlassenAdminTest {
    private val code = "ABCDE-23456"
    private val kcode = "K-ABCDE-23456"
    private val arbeitText = """{"klasse":"Klasse 3","fach":"Mathe","arbeit":"Nr. 1","uebungen":[
        {"titel":"Rest","gruppe":"G","symbol":"R","tipp":"T","aufgaben":[
          {"art":"rest","frage":"f","rechnung":"58 : 4 =","antwort":"14","antwort2":"2","erklaerung":"e"}]},
        {"titel":"Mauer","aufgaben":[{"art":"mauer","frage":"m","reihen":[[3,4,5,6],[7,9,11],[16,20],[36]]}]}]}"""

    @Test fun unterschriftPasstZumKlassensiegel() {
        val seed = KlassenAdmin.neuerSeed(SecureRandom())
        val pub = Base64.decode(KlassenAdmin.oeffentlich(seed))
        assertEquals(32, pub.size)
        val anfrage = Json.parseToJsonElement(KlassenAdmin.paketAnfrage(code, seed, arbeitText)).jsonObject
        val daten = anfrage["daten"]!!.jsonObject
        assertEquals("paket", anfrage["art"]!!.jsonPrimitive.content)
        val sig = daten["sig"]!!.jsonPrimitive.content
        assertTrue(Klassensiegel.pruefe(kcode, arbeitText, sig, pub))
        assertTrue(!Klassensiegel.pruefe(kcode, arbeitText + " ", sig, pub))
    }

    @Test fun schluesselIstStabil() {
        val seed = KlassenAdmin.neuerSeed()
        assertEquals(KlassenAdmin.oeffentlich(seed), KlassenAdmin.oeffentlich(seed))
        assertTrue(KlassenAdmin.neuerSeed() != seed)
    }

    @Test fun loeschenWirdUnterschrieben() {
        val seed = KlassenAdmin.neuerSeed()
        val name = KlassenAdmin.paketName(code, "Klasse 3", "Mathe", "Klassenarbeit Nr. 1")
        assertEquals("paket-K-ABCDE-23456-klasse-3-mathe-klassenarbeit-nr-1", name)
        val daten = Json.parseToJsonElement(KlassenAdmin.loeschenAnfrage(code, seed, name)).jsonObject["daten"]!!.jsonObject
        val pub = Base64.decode(KlassenAdmin.oeffentlich(seed))
        // Der Text ist "K-CODE|loeschen|Name", pruefe setzt "K-CODE|" davor.
        assertTrue(Klassensiegel.pruefe(kcode, "loeschen|$name", daten["sig"]!!.jsonPrimitive.content, pub))
    }

    @Test fun arbeitWirdZuJsonUndZurueck() {
        val arbeit = PaketLeser.lese(arbeitText).arbeit!!
        val text = PaketJson.text(arbeit)
        val nochmal = PaketLeser.lese(text).arbeit!!
        assertEquals(arbeit, nochmal)
        assertTrue(text.contains("antwort2"))
        assertTrue(text.contains("reihen"))
    }
}
