package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Einwilligung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EinwilligungTest {
    @Test fun ohneEinwilligungErforderlich() {
        assertTrue(Einwilligung.erforderlich(0))
        assertFalse(Einwilligung.erforderlich(Einwilligung.AKTUELLE_VERSION))
        assertFalse(Einwilligung.erforderlich(Einwilligung.AKTUELLE_VERSION + 1))
    }

    @Test fun datumText() {
        assertEquals("noch nicht erteilt", Einwilligung.datumText(0))
        assertTrue(Einwilligung.datumText(1_700_000_000_000L).matches(Regex("\\d\\d\\.\\d\\d\\.\\d{4}")))
    }

    @Test fun keineGedankenstriche() {
        assertFalse(Einwilligung.punkte.any { it.contains("—") || it.contains("–") })
    }
}
