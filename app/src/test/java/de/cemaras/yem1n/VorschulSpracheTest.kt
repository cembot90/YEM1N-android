package de.cemaras.yem1n

import de.cemaras.yem1n.logik.VorschulGenerator
import de.cemaras.yem1n.logik.VorschulSprache
import de.cemaras.yem1n.logik.VorschulText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VorschulSpracheTest {
    @Test
    fun angebotUndAktiveSprache() {
        assertEquals(listOf("de"), VorschulSprache.angebot(false, false))
        assertEquals(listOf("de", "en"), VorschulSprache.angebot(true, false))
        assertEquals(listOf("de", "tr"), VorschulSprache.angebot(false, true))
        assertEquals(listOf("de", "en", "tr"), VorschulSprache.angebot(true, true))
        assertEquals("en", VorschulSprache.aktiv("en", true, false))
        // Ausgeschaltete Sprache fällt auf Deutsch zurück
        assertEquals("de", VorschulSprache.aktiv("tr", true, false))
        assertEquals("de", VorschulSprache.aktiv("xx", true, true))
    }

    @Test
    fun textFaelltAufDeutschZurueck() {
        val t = VorschulText("Wo ist der Kreis?", "Daire hangisi?", "Where is the circle?")
        assertEquals("Daire hangisi?", t.text("tr"))
        assertEquals("tr", t.sprache("tr"))
        assertEquals("Where is the circle?", t.text("en"))
        assertEquals("Wo ist der Kreis?", t.text("de"))
        val nur = VorschulText("Womit fängt Apfel an?")
        assertEquals("Womit fängt Apfel an?", nur.text("tr"))
        assertEquals("de", nur.sprache("en"))
    }

    @Test
    fun namenDerSprachen() {
        assertEquals("Deutsch", VorschulSprache.name("de"))
        assertEquals("English", VorschulSprache.name("en"))
        assertEquals("Türkçe", VorschulSprache.name("tr"))
    }

    @Test
    fun vieleFragenHabenEineUebersetzung() {
        val fragen = (0 until 40).flatMap { VorschulGenerator.fragen(de.cemaras.yem1n.logik.VorschulArt.MIX, 2, 8, kotlin.random.Random(it)) }
        val mitBeiden = fragen.count { it.text.tr != null && it.text.en != null }
        assertTrue(mitBeiden > fragen.size / 4)
        for (f in fragen) {
            assertTrue(f.text.text("tr").isNotBlank())
            assertTrue(f.text.text("en").isNotBlank())
        }
    }
}
