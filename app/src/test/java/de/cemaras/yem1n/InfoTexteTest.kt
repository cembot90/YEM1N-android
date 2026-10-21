package de.cemaras.yem1n

import de.cemaras.yem1n.daten.InfoTexte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoTexteTest {
    private val alle: List<String>
        get() = (InfoTexte.kurzAnleitung + InfoTexte.impressum + InfoTexte.beta + InfoTexte.datenschutz)
            .flatMap { listOf(it.first, it.second) }

    @Test
    fun keineGedankenstriche() {
        alle.forEach { assertFalse(it, it.contains('–') || it.contains('—')) }
    }

    @Test
    fun alleAbschnitteHabenInhalt() {
        alle.forEach { assertTrue(it.isNotBlank()) }
    }

    @Test
    fun anleitungAdresseMitFarbwelt() {
        assertEquals("https://cembot90.github.io/YEM1N/anleitung.html", InfoTexte.anleitungAdresse(false))
        assertEquals("https://cembot90.github.io/YEM1N/anleitung.html?farbwelt=rosa", InfoTexte.anleitungAdresse(true))
    }
}
