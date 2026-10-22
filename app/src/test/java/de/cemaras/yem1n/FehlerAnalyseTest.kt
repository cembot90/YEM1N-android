package de.cemaras.yem1n

import de.cemaras.yem1n.logik.FehlerAnalyse
import de.cemaras.yem1n.logik.FehlerMuster
import de.cemaras.yem1n.logik.Rechenausdruck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FehlerAnalyseTest {
    private fun zahl(rechnung: String, soll: String, ist: String) =
        FehlerAnalyse.befund("zahl", rechnung, "", soll, "", ist)?.muster

    @Test fun rechenausdruckLesen() {
        val a = Rechenausdruck.lies("9 · 9 + 8 =")!!
        assertEquals(listOf(9, 9, 8), a.zahlen)
        assertEquals(89, a.wert)
        assertEquals("Punkt vor Strich", 89, Rechenausdruck.lies("8 + 9 · 9 =")!!.wert)
        assertEquals("von links nach rechts", 153, Rechenausdruck.lies("8 + 9 · 9 =")!!.linksNachRechts)
        assertEquals(14, Rechenausdruck.lies("58 : 4 = 14 R 2")!!.wert)
        assertNull(Rechenausdruck.lies("Multipliziere die Zahlen 7 und 8."))
        assertNull(Rechenausdruck.lies("7 ·"))
    }

    @Test fun rechenzeichen() {
        assertEquals(FehlerMuster.PLUSSTATTMAL, zahl("7 · 8 =", "56", "15"))
        assertEquals(FehlerMuster.MALSTATTGETEILT, zahl("32 : 8 =", "4", "256"))
        assertEquals(FehlerMuster.MINUSSTATTPLUS, zahl("20 + 5 =", "25", "15"))
        assertEquals(FehlerMuster.PLUSSTATTMINUS, zahl("20 - 5 =", "15", "25"))
    }

    @Test fun malReiheUndKnapp() {
        assertEquals(FehlerMuster.MALREIHEZUVIEL, zahl("7 · 8 =", "56", "63"))
        assertEquals(FehlerMuster.MALREIHEZUWENIG, zahl("7 · 8 =", "56", "49"))
        assertEquals(FehlerMuster.KNAPPDANEBEN, zahl("3 · 9 =", "27", "26"))
        assertEquals(FehlerMuster.ZIFFERDREHER, zahl("4 · 9 =", "36", "63"))
        assertNull("richtig ist kein Fehler", zahl("7 · 8 =", "56", "56"))
    }

    @Test fun punktVorStrich() {
        assertEquals(FehlerMuster.PUNKTVORSTRICH, zahl("8 + 9 · 9 =", "89", "153"))
    }

    @Test fun teilenMitRest() {
        val b = FehlerAnalyse.befund("rest", "48 : 4 =", "", "12", "0", "10", "8")!!
        assertEquals(FehlerMuster.RESTZUGROSS, b.muster)
        assertEquals(true, b.satz.contains("bei 10 stehen geblieben"))
        assertEquals(FehlerMuster.RESTFALSCH, FehlerAnalyse.befund("rest", "58 : 4 =", "", "14", "2", "14", "1")!!.muster)
    }

    @Test fun vergleichUndText() {
        assertEquals(FehlerMuster.VERGLEICHUMGEDREHT, FehlerAnalyse.befund("vergleich", "5 · 5  ?  9 · 7", "25 < 63", "<", "", ">")!!.muster)
        assertEquals(FehlerMuster.GLEICHUEBERSEHEN, FehlerAnalyse.befund("vergleich", "", "", "=", "", "<")!!.muster)
        assertEquals(FehlerMuster.GROSSKLEIN, FehlerAnalyse.befund("text", "", "", "Hund", "", "hund")!!.muster)
        assertEquals(FehlerMuster.HARTWEICH, FehlerAnalyse.befund("text", "", "", "Hund", "", "Hunt")!!.muster)
        assertEquals(FehlerMuster.DOPPELTERMITLAUT, FehlerAnalyse.befund("text", "", "", "Sonne", "", "Sone")!!.muster)
        assertEquals(FehlerMuster.IESTATTI, FehlerAnalyse.befund("text", "", "", "Biene", "", "Bine")!!.muster)
    }
}
