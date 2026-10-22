package de.cemaras.yem1n

import de.cemaras.yem1n.logik.SprachTipp
import de.cemaras.yem1n.logik.Tipp
import org.junit.Assert.assertEquals
import org.junit.Test

class SprachTippTest {
    @Test fun englischOhneArtikel() {
        assertEquals(Tipp.RICHTIG, SprachTipp.pruefe("dog", "The Dog", "en"))
        assertEquals(Tipp.RICHTIG, SprachTipp.pruefe("to run", "run!", "en"))
        assertEquals(Tipp.FALSCH, SprachTipp.pruefe("dog", "cat", "en"))
        assertEquals(Tipp.FALSCH, SprachTipp.pruefe("dog", "  ", "en"))
    }

    @Test fun tuerkischGrossKleinMitPunktlosemI() {
        assertEquals(Tipp.RICHTIG, SprachTipp.pruefe("kırmızı", "KIRMIZI", "tr"))
        assertEquals(Tipp.FAST, SprachTipp.pruefe("kırmızı", "kirmizi", "tr"))
        assertEquals(Tipp.RICHTIG, SprachTipp.pruefe("ışık", "IŞIK", "tr"))
    }

    @Test fun italienischArtikelUndApostroph() {
        assertEquals(Tipp.RICHTIG, SprachTipp.pruefe("amico", "l’amico", "it"))
        assertEquals(Tipp.RICHTIG, SprachTipp.pruefe("gatto", "il gatto", "it"))
        assertEquals(Tipp.FAST, SprachTipp.pruefe("perché", "perche", "it"))
    }

    @Test fun sonderzeichen() {
        assertEquals(7, SprachTipp.sonderzeichen("tr").size)
        assertEquals(6, SprachTipp.sonderzeichen("it").size)
        assertEquals(0, SprachTipp.sonderzeichen("en").size)
    }
}
