package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Haustier
import de.cemaras.yem1n.logik.HaustierErinnerung
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HaustierErinnerungTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    private fun zeit(tag: Int, stunde: Int): Long {
        val k = Calendar.getInstance(zone)
        k.clear()
        k.set(2026, Calendar.OCTOBER, tag, stunde, 0, 0)
        return k.timeInMillis
    }

    private fun stundeVon(ms: Long): Int {
        val k = Calendar.getInstance(zone)
        k.timeInMillis = ms
        return k.get(Calendar.HOUR_OF_DAY)
    }

    @Test fun kommtImmerUmSiebzehnUhr() {
        val jetzt = zeit(21, 9)
        val h = Haustier(xp = 100, satt = 80.0, zuletzt = jetzt)
        val z = HaustierErinnerung.zeitpunkt(h, jetzt, zone)
        assertEquals(17, stundeVon(z))
        assertTrue(z > jetzt)
    }

    @Test fun eiBekommtErstNachZwanzigStundenEineErinnerung() {
        val jetzt = zeit(21, 9)
        val z = HaustierErinnerung.zeitpunkt(Haustier(xp = 0, zuletzt = jetzt), jetzt, zone)
        assertTrue(z - jetzt >= 20 * 3_600_000L)
        assertEquals(17, stundeVon(z))
    }

    @Test fun hungrigesTierBekommtDieErinnerungHeuteNachmittag() {
        val jetzt = zeit(21, 9)
        val h = Haustier(xp = 100, satt = 10.0, zuletzt = jetzt)
        val z = HaustierErinnerung.zeitpunkt(h, jetzt, zone)
        assertTrue(z - jetzt <= 9 * 3_600_000L)
    }

    @Test fun nieSpaeterAlsVierTageUndNieInDerVergangenheit() {
        val jetzt = zeit(21, 9)
        val z = HaustierErinnerung.zeitpunkt(Haustier(xp = 100, satt = 100.0, zuletzt = jetzt), jetzt, zone)
        assertTrue(z > jetzt)
        assertTrue(z - jetzt <= 4 * 24 * 3_600_000L)
    }

    @Test fun texteNennenDenNamen() {
        val h = Haustier(name = "Momo", xp = 100)
        assertTrue(HaustierErinnerung.titel(h).contains("Momo"))
        assertTrue(HaustierErinnerung.text(h).contains("Momo"))
        assertTrue(HaustierErinnerung.titel(Haustier(xp = 0)).contains("Ei"))
    }
}
