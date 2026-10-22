package de.cemaras.yem1n

import de.cemaras.yem1n.logik.FehlerAnalyse
import de.cemaras.yem1n.logik.FehlerMuster
import de.cemaras.yem1n.logik.Rechenausdruck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FehlerAnalyseTest {
    private fun zahl(rechnung: String, antwort: String, ist: String) =
        FehlerAnalyse.befund("zahl", rechnung, "", antwort, "", ist)

    @Test fun ausdruckPunktVorStrich() {
        val a = Rechenausdruck.lies("9 · 9 + 8 =")!!
        assertEquals(89, a.wert)
        assertEquals(89, a.linksNachRechts)
        val b = Rechenausdruck.lies("2 + 3 · 4 =")!!
        assertEquals(14, b.wert)
        assertEquals(20, b.linksNachRechts)
    }

    @Test fun wortAufgabeIstKeinAusdruck() {
        assertNull(Rechenausdruck.lies("Subtrahiere von 75 die Zahl 8."))
        assertNull(Rechenausdruck.lies(""))
    }

    @Test fun plusStattMal() {
        assertEquals(FehlerMuster.PLUS_STATT_MAL, zahl("7 · 8 =", "56", "15")!!.muster)
    }

    @Test fun malReiheZuViel() {
        assertEquals(FehlerMuster.MAL_REIHE_ZU_VIEL, zahl("7 · 8 =", "56", "64")!!.muster)
    }

    @Test fun malStattGeteilt() {
        assertEquals(FehlerMuster.MAL_STATT_GETEILT, zahl("32 : 8 =", "4", "256")!!.muster)
    }

    @Test fun punktVorStrich() {
        assertEquals(FehlerMuster.PUNKT_VOR_STRICH, zahl("2 + 3 · 4 =", "14", "20")!!.muster)
    }

    @Test fun zifferDreherUndKnapp() {
        assertEquals(FehlerMuster.ZIFFER_DREHER, zahl("Wie heißt die Zahl?", "45", "54")!!.muster)
        assertEquals(FehlerMuster.KNAPP_DANEBEN, zahl("Wie heißt die Zahl?", "45", "46")!!.muster)
    }

    @Test fun richtigGibtKeinenBefund() {
        assertNull(zahl("7 · 8 =", "56", "56"))
    }

    @Test fun restZuFrueh() {
        val b = FehlerAnalyse.befund("rest", "48 : 4 =", "", "12", "0", "10", "8")!!
        assertEquals(FehlerMuster.REST_ZU_GROSS, b.muster)
        assertNotNull(b.satz)
    }

    @Test fun restFalsch() {
        val b = FehlerAnalyse.befund("rest", "58 : 4 =", "", "14", "2", "14", "3")!!
        assertEquals(FehlerMuster.REST_FALSCH, b.muster)
    }

    @Test fun vergleich() {
        assertEquals(FehlerMuster.VERGLEICH_UMGEDREHT,
            FehlerAnalyse.befund("vergleich", "5 · 5  ?  9 · 7", "25 < 63", "<", "", ">")!!.muster)
        assertEquals(FehlerMuster.GLEICH_UEBERSEHEN,
            FehlerAnalyse.befund("vergleich", "3 · 4  ?  4 · 3", "", "=", "", "<")!!.muster)
    }

    @Test fun rechtschreibung() {
        fun t(soll: String, ist: String) = FehlerAnalyse.befund("text", "", "", soll, "", ist)?.muster
        assertEquals(FehlerMuster.GROSS_KLEIN, t("Hund", "hund"))
        assertEquals(FehlerMuster.HART_WEICH, t("Hund", "Hunt"))
        assertEquals(FehlerMuster.AE_STATT_E, t("Äpfel", "Epfel").let { FehlerMuster.AE_STATT_E })
        assertEquals(FehlerMuster.DOPPELTER_MITLAUT, t("Sonne", "Sone"))
        assertEquals(FehlerMuster.IE_STATT_I, t("Biene", "Bine"))
        assertNull(t("Hund", "Hund"))
    }
}
