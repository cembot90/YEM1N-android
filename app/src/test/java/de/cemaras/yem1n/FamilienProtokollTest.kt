package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Einladung
import de.cemaras.yem1n.logik.FamilienProtokoll
import de.cemaras.yem1n.logik.FamilienZugang
import de.cemaras.yem1n.logik.JokerNachschub
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilienProtokollTest {
    private val familie = "0123456789abcdef".repeat(4)
    private val token = "AbCdEfGhIjKlMnOpQrStUv"

    private fun lies(t: String) = Json.parseToJsonElement(t).jsonObject

    // ---- QR-Code ----

    @Test fun qrCodeGehtHinUndZurueck() {
        val e = Einladung(familie, "kind", token, 1L)
        assertEquals("yem1n://koppeln?f=$familie&t=$token", e.qr)
        assertEquals(familie to token, FamilienProtokoll.qrLesen(e.qr))
        assertEquals(familie to token, FamilienProtokoll.qrLesen("  YEM1N://koppeln?t=$token&f=$familie \n"))
    }

    @Test fun kaputteQrCodesWerdenAbgelehnt() {
        assertNull(FamilienProtokoll.qrLesen("yem1n://familie/ABCDE-FGHIJ"))
        assertNull(FamilienProtokoll.qrLesen("https://koppeln?f=$familie&t=$token"))
        assertNull(FamilienProtokoll.qrLesen("yem1n://koppeln?f=$familie"))
        assertNull(FamilienProtokoll.qrLesen("yem1n://koppeln?f=${familie.take(63)}&t=$token"))
        assertNull(FamilienProtokoll.qrLesen("yem1n://koppeln?f=${familie.uppercase()}&t=$token"))
        assertNull(FamilienProtokoll.qrLesen("yem1n://koppeln?f=$familie&t=kurz"))
        assertNull(FamilienProtokoll.qrLesen("yem1n://koppeln?f=$familie&t=$token%20x"))
        assertNull(FamilienProtokoll.qrLesen(""))
    }

    @Test fun pruefcodeIstDerAnfangDerKennung() {
        assertEquals("35880C", FamilienProtokoll.pruefcode("35880c633aaf87fd6ff9d04b"))
        val z = FamilienZugang(familie, "35880c633aaf87fd6ff9d04b", "Yemin", "kind", "wartet")
        assertEquals("35880C", z.pruefcode)
        assertTrue(z.wartet)
        assertFalse(z.aktiv)
        assertFalse(z.istEltern)
    }

    // ---- Körper ----

    @Test fun frageHatDasFormatDesDienstes() {
        val o = FamilienProtokoll.frage("frage-0001", " Yemin ", "Mathe", "Geteilt", " Hilfe ")
        assertEquals("frage", o["typ"]!!.jsonPrimitive.content)
        val d = o["daten"]!!.jsonObject
        assertEquals("frage-0001", d["id"]!!.jsonPrimitive.content)
        assertEquals("Yemin", d["kind"]!!.jsonPrimitive.content)
        assertEquals("Hilfe", d["text"]!!.jsonPrimitive.content)
        assertEquals("Kind", FamilienProtokoll.frage("frage-0002", "", "", "", "x")["daten"]!!.jsonObject["kind"]!!.jsonPrimitive.content)
        assertEquals(500, FamilienProtokoll.frage("frage-0003", "K", "", "", "a".repeat(900))["daten"]!!.jsonObject["text"]!!.jsonPrimitive.content.length)
    }

    @Test fun antwortDaumenNachschubFreigabe() {
        val a = FamilienProtokoll.antwort("antw-0001", "frage-0001", "👍", "Rechne 6 mal 7")["daten"]!!.jsonObject
        assertEquals("frage-0001", a["frageId"]!!.jsonPrimitive.content)
        assertFalse(a.containsKey("absender")) // den Namen setzt der Dienst
        val d = FamilienProtokoll.daumen("frage-0001", "antw-0001")
        assertEquals("daumen", d["typ"]!!.jsonPrimitive.content)
        assertEquals("antw-0001", d["daten"]!!.jsonObject["antwortId"]!!.jsonPrimitive.content)
        assertEquals("nachschub", FamilienProtokoll.nachschub("nach-0001", "Yemin")["typ"]!!.jsonPrimitive.content)
        assertEquals("10", FamilienProtokoll.freigabe("frei-0001", 99)["daten"]!!.jsonObject["anzahl"]!!.jsonPrimitive.content)
        assertEquals("1", FamilienProtokoll.freigabe("frei-0001", 0)["daten"]!!.jsonObject["anzahl"]!!.jsonPrimitive.content)
    }

    @Test fun ergebnisWirdAufDieGrenzenDesDienstesGekuerzt() {
        val e = RundenErgebnisEintrag(
            "erg-0001", "k".repeat(60), "f".repeat(60), "a".repeat(200), "u".repeat(200),
            8, 10, 1, 2, " Yemin ", 1800000000000L, "x".repeat(25000),
        )
        val d = FamilienProtokoll.ergebnis(e)["daten"]!!.jsonObject
        assertEquals(40, d["klasse"]!!.jsonPrimitive.content.length)
        assertEquals(40, d["fach"]!!.jsonPrimitive.content.length)
        assertEquals(120, d["arbeit"]!!.jsonPrimitive.content.length)
        assertEquals(120, d["uebung"]!!.jsonPrimitive.content.length)
        assertEquals("Yemin", d["kind"]!!.jsonPrimitive.content)
        assertFalse(d.containsKey("details")) // zu lang für den Dienst, lieber ohne Einzelheiten als gar nicht
        assertEquals("8", d["richtig"]!!.jsonPrimitive.content)
    }

    @Test fun duellUndLesenUndPush() {
        assertEquals("duell-0001", FamilienProtokoll.duell("duell-0001", "{}")["daten"]!!.jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals("duell-0001", FamilienProtokoll.duellAntwort("duell-0001", "{}")["daten"]!!.jsonObject["duellId"]!!.jsonPrimitive.content)
        val l = FamilienProtokoll.lesen("frage", 7, 999)
        assertEquals("7", l["seit"]!!.jsonPrimitive.content)
        assertEquals("200", l["limit"]!!.jsonPrimitive.content)
        assertEquals("fcm", FamilienProtokoll.pushAnmelden("tok")["art"]!!.jsonPrimitive.content)
        assertEquals("", FamilienProtokoll.pushAbmelden()["art"]!!.jsonPrimitive.content)
    }

    @Test fun kennungenSindFuerDenDienstGueltig() {
        assertTrue(FamilienProtokoll.kennungOk(FamilienProtokoll.neueKennung("frage", java.util.UUID.randomUUID().toString())))
        assertTrue(FamilienProtokoll.neueKennung("frage", "x".repeat(100)).length <= 64)
        assertFalse(FamilienProtokoll.kennungOk("zu kurz"))
        assertFalse(FamilienProtokoll.kennungOk("mit:Doppelpunkt"))
    }

    // ---- Auswerten ----

    private val antwort = lies(
        """{"ok":true,"letzte":9,"mehr":true,"eintraege":[
        {"nr":3,"typ":"frage","id":"frage-0001","von":"g1","absender":"Yemin","erstellt":1000,"daten":{"id":"frage-0001","kind":"Yemin","fach":"Mathe","uebung":"Geteilt","text":"Was ist 56 : 7?"}},
        {"nr":5,"typ":"frage","id":"frage-0002","von":"g1","absender":"Yemin","erstellt":3000,"daten":{"id":"frage-0002","kind":"Yemin","text":"Noch eine"}},
        {"nr":6,"typ":"antwort","id":"antw-0001","von":"g2","absender":"Mama","erstellt":2000,"daten":{"id":"antw-0001","frageId":"frage-0001","emoji":"😀","text":"8"}},
        {"nr":7,"typ":"daumen","id":"antw-0001","von":"g1","absender":"Yemin","erstellt":2500,"daten":{"frageId":"frage-0001","antwortId":"antw-0001"}},
        {"nr":8,"typ":"frage"},
        {"typ":"frage","id":"x","daten":{}}]}""",
    )

    @Test fun eintraegeWerdenGelesenKaputteUebersprungen() {
        val e = FamilienProtokoll.eintraege(antwort)
        assertEquals(4, e.size)
        assertEquals("Mama", e[2].absender)
        assertEquals(1000L, e[0].erstellt)
        assertEquals("Mathe", e[0].text("fach"))
        assertEquals(emptyList<Any>(), FamilienProtokoll.eintraege(lies("""{"ok":true}""")))
    }

    @Test fun weiterLesenNurWennNochMehrDa() {
        assertEquals(9L, FamilienProtokoll.weiterAb(antwort))
        assertNull(FamilienProtokoll.weiterAb(lies("""{"ok":true,"letzte":9,"mehr":false,"eintraege":[]}""")))
    }

    @Test fun fragenAntwortenUndDaumenWerdenZusammengesetzt() {
        val alle = FamilienProtokoll.eintraege(antwort)
        val fragen = FamilienProtokoll.fragen(alle.filter { it.typ == "frage" }, alle.filter { it.typ == "antwort" }, alle.filter { it.typ == "daumen" })
        assertEquals(listOf("frage-0002", "frage-0001"), fragen.map { it.id }) // neueste zuerst
        val erste = fragen[1]
        assertEquals("Geteilt", erste.uebung)
        assertEquals(1, erste.antworten.size)
        assertEquals("Mama", erste.antworten[0].absender)
        assertTrue(erste.antworten[0].daumen)
        assertTrue(fragen[0].antworten.isEmpty())
    }

    @Test fun ergebnisseNeuesteZuerst() {
        val l = FamilienProtokoll.eintraege(lies(
            """{"eintraege":[
            {"nr":1,"typ":"ergebnis","id":"erg-0001","von":"g1","absender":"Yemin","erstellt":10,"daten":{"id":"erg-0001","fach":"Mathe","uebung":"Geteilt","richtig":8,"gesamt":10,"angesehen":1,"sterne":2,"kind":"Yemin","zeitpunkt":500}},
            {"nr":2,"typ":"ergebnis","id":"erg-0002","von":"g1","absender":"Yemin","erstellt":2000,"daten":{"id":"erg-0002","richtig":3,"gesamt":4,"kind":"Evin"}}]}""",
        ))
        val e = FamilienProtokoll.ergebnisse(l)
        assertEquals(listOf("erg-0002", "erg-0001"), e.map { it.eintragId })
        assertEquals(2000L, e[0].zeitpunkt) // ohne eigene Zeit gilt die Zeit des Anlegens
        assertEquals(500L, e[1].zeitpunkt)
        assertEquals(8, e[1].richtig)
    }

    @Test fun nachschubUndFreigabeLaufenDurchDieVorhandeneAuswertung() {
        val n = FamilienProtokoll.eintraege(lies(
            """{"eintraege":[{"nr":1,"typ":"nachschub","id":"nach-0001","von":"g1","absender":"Yemin","erstellt":100,"daten":{"id":"nach-0001","kind":"Yemin"}}]}""",
        )).map { it.alsDatensatz() }
        val f = FamilienProtokoll.eintraege(lies(
            """{"eintraege":[{"nr":2,"typ":"freigabe","id":"frei-0001","von":"g2","absender":"Mama","erstellt":50,"daten":{"id":"frei-0001","anzahl":3}}]}""",
        )).map { it.alsDatensatz() }
        assertEquals("Yemin", JokerNachschub.offeneAnfrage(n, f)) // die Bitte ist neuer als die letzte Freigabe
        assertEquals(listOf("frei-0001"), JokerNachschub.neueFreigaben(f, emptyList(), 10))
    }

    @Test fun geraeteListeOhneSchluessel() {
        val l = FamilienProtokoll.geraete(lies(
            """{"ok":true,"geraete":[
            {"id":"aaaaaaaaaaaaaaaaaaaaaaaa","name":"Mama","rolle":"eltern","status":"aktiv","erstellt":1,"zuletzt":2,"ich":true},
            {"id":"bbbbbbbbbbbbbbbbbbbbbbbb","name":"Yemin","rolle":"kind","status":"wartet","erstellt":3,"zuletzt":3,"ich":false},
            {"name":"ohne id"}]}""",
        ))
        assertEquals(2, l.size)
        assertTrue(l[0].ich)
        assertEquals("BBBBBB", l[1].pruefcode)
        assertEquals("wartet", l[1].status)
    }

    @Test fun fehlertexteSindFreundlich() {
        for (s in listOf(0, -1, 400, 401, 403, 404, 409, 429, 500, 503)) assertTrue(FamilienProtokoll.fehlertext(s).isNotBlank())
        assertTrue(FamilienProtokoll.fehlertext(500).contains("500"))
        assertTrue(FamilienProtokoll.fehlertext(503).contains("Pause"))
        for (s in listOf(0, 401, 429, 503)) {
            val t = FamilienProtokoll.fehlertext(s)
            assertFalse(t.contains("\u2013") || t.contains("\u2014")) // keine Gedankenstriche
        }
    }
}
