package de.cemaras.yem1n

import de.cemaras.yem1n.logik.KiPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KiPromptTest {
    private val ersatz = "Richtig ist 42. 6 · 7 = 42"

    @Test fun gutenTextAnnehmen() {
        val (t, ki) = KiPrompt.pruefe("Fast! 6 · 7 sind 42.", listOf("6 · 7 =", "42"), ersatz)
        assertTrue(ki); assertEquals("Fast! 6 · 7 sind 42.", t)
    }

    @Test fun neueZahlAblehnen() {
        val (t, ki) = KiPrompt.pruefe("6 · 7 sind 43.", listOf("6 · 7 =", "42"), ersatz)
        assertFalse(ki); assertEquals(ersatz, t)
    }

    @Test fun leerOderZuLang() {
        assertFalse(KiPrompt.pruefe(null, listOf("42"), ersatz).second)
        assertFalse(KiPrompt.pruefe("42 " + "a".repeat(400), listOf("42"), ersatz).second)
    }

    @Test fun markdownUndStricheWerdenBereinigt() {
        val (t, ki) = KiPrompt.pruefe("**Super** — 42", listOf("42"), ersatz)
        assertTrue(ki); assertEquals("Super - 42", t)
    }

    @Test fun anweisungOhneGrund() {
        assertTrue(KiPrompt.anweisung(false).contains("Nenne keinen Grund"))
        assertFalse(KiPrompt.anweisung(true).contains("Nenne keinen Grund"))
    }
}
