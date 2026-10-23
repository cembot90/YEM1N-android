package de.cemaras.yem1n

import de.cemaras.yem1n.logik.VorleseText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VorleseTextTest {
    @Test
    fun rechtschreibungUndMauerWerdenNichtVorgelesen() {
        assertFalse(VorleseText.vorlesbar("text"))
        assertFalse(VorleseText.vorlesbar("mauer"))
        assertTrue(VorleseText.vorlesbar("zahl"))
        assertTrue(VorleseText.vorlesbar("rest"))
        assertTrue(VorleseText.vorlesbar("vergleich"))
    }

    @Test
    fun zeichenWerdenZuWoertern() {
        assertEquals("Rechne. 7 mal 8 gleich", VorleseText.aufgabe("Rechne", "7 · 8 ="))
        assertEquals("Rechne. 32 geteilt durch 8 gleich", VorleseText.aufgabe("Rechne", "32 : 8 ="))
        assertEquals("Was gehört in die Mitte?. 5 mal 5 und 9 mal 7", VorleseText.aufgabe("Was gehört in die Mitte?", "5 · 5  ?  9 · 7"))
    }

    @Test
    fun emojisUndLeerzeichenWerdenEntfernt() {
        assertEquals("Hallo Welt", VorleseText.gesprochen("🦄 Hallo   Welt\n"))
        assertEquals("Größer ä ß", VorleseText.gesprochen("Größer ä ß"))
    }

    @Test
    fun zeilenumbruecheWerdenZuSaetzen() {
        assertEquals("Lies genau. Meine Zahl ist gerade. Sie ist eine Neunerzahl", VorleseText.aufgabe("Lies genau", "Meine Zahl ist gerade.\nSie ist eine Neunerzahl"))
    }
}
