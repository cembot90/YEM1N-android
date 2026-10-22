package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Eule
import de.cemaras.yem1n.logik.Haustier
import de.cemaras.yem1n.logik.HaustierZeit
import de.cemaras.yem1n.logik.WochenberichtErinnerung
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErinnerungenTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")
    private fun zeit(j: Int, m: Int, t: Int, h: Int, min: Int = 0): Long =
        Calendar.getInstance(zone).apply { clear(); set(j, m - 1, t, h, min) }.timeInMillis

    @Test fun schlafZeiten() {
        assertTrue(HaustierZeit.schlaeft(20))
        assertTrue(HaustierZeit.schlaeft(3))
        assertFalse(HaustierZeit.schlaeft(7))
        assertFalse(HaustierZeit.schlaeft(19))
    }

    @Test fun schlafendesTierSagtZzz() {
        val tier = Haustier(name = "Pip", xp = 60)
        assertTrue(Eule.antippen(tier, 0, schlaeft = true).contains("Zzz"))
        assertTrue(Eule.lageTipps(tier, 0, 5, schlaeft = true).first().contains("schläft"))
        assertFalse(Eule.lageTipps(tier, 0, 5).any { it.contains("schläft") })
    }

    @Test fun sonntagsErinnerung() {
        // Donnerstag 22.10.2026 gibt Sonntag 25.10. um 18 Uhr
        assertEquals(zeit(2026, 10, 25, 18), WochenberichtErinnerung.zeitpunkt(zeit(2026, 10, 22, 14), zone))
        // Sonntag 17 Uhr: heute
        assertEquals(zeit(2026, 10, 25, 18), WochenberichtErinnerung.zeitpunkt(zeit(2026, 10, 25, 17), zone))
        // Sonntag 18:30: erst nächste Woche
        assertEquals(zeit(2026, 11, 1, 18), WochenberichtErinnerung.zeitpunkt(zeit(2026, 10, 25, 18, 30), zone))
    }
}
