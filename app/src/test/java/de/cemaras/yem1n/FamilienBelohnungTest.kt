package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Erfolge
import org.junit.Assert.assertEquals
import org.junit.Test

class FamilienBelohnungTest {
    @Test fun leerzeichenWerdenGeglaettet() {
        assertEquals("Eis essen gehen", Erfolge.familienBelohnungText("  Eis   essen\n gehen "))
    }

    @Test fun leerBleibtLeer() {
        assertEquals("", Erfolge.familienBelohnungText("   "))
    }

    @Test fun langerTextWirdGekuerzt() {
        assertEquals(60, Erfolge.familienBelohnungText("x".repeat(200)).length)
    }
}
