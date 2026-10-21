package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Haustier
import de.cemaras.yem1n.logik.WidgetText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetTextTest {
    @Test fun ohneHaustierEinLockruf() {
        val t = WidgetText.von(Fortschritt(), 1000L, 100L)
        assertEquals("🥚", t.emoji)
        assertEquals("Such dir eins aus", t.zeile)
    }

    @Test fun mitHaustierNameUndStimmung() {
        val h = Haustier(name = "Momo", art = "hase", xp = 100, satt = 90.0, laune = 90.0, zuletzt = 1000L)
        val t = WidgetText.von(Fortschritt(haustier = h), 1000L, 100L)
        assertEquals("Momo", t.titel)
        assertEquals("🐰", t.emoji)
        assertTrue(t.zeile.startsWith("froh"))
    }

    @Test fun serieStehtImWidget() {
        val h = Haustier(xp = 100, zuletzt = 1000L)
        val f = Fortschritt(haustier = h, tage = listOf(99L, 100L))
        val t = WidgetText.von(f, 1000L, 100L)
        assertTrue(t.zeile.contains("🔥 2 Tage"))
    }

    @Test fun hutStehtVorDemTier() {
        val h = Haustier(xp = 100, art = "katze", hut = "krone", huete = listOf("krone"), zuletzt = 1000L)
        assertEquals("👑🐱", WidgetText.von(Fortschritt(haustier = h), 1000L, 1L).emoji)
    }
}
