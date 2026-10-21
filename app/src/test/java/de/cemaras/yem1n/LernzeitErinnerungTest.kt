package de.cemaras.yem1n

import de.cemaras.yem1n.logik.LernzeitErinnerung
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LernzeitErinnerungTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    private fun zeit(tag: Int, h: Int, m: Int): Long {
        val k = Calendar.getInstance(zone)
        k.clear()
        k.set(2026, Calendar.OCTOBER, tag, h, m, 0)
        return k.timeInMillis
    }

    @Test fun nochHeuteWennNichtGeuebtUndZeitNochNichtDa() {
        assertEquals(zeit(21, 16, 0), LernzeitErinnerung.zeitpunkt(zeit(21, 10, 0), 16 * 60, false, zone))
    }

    @Test fun morgenWennZeitVorbei() {
        assertEquals(zeit(22, 16, 0), LernzeitErinnerung.zeitpunkt(zeit(21, 17, 0), 16 * 60, false, zone))
    }

    @Test fun morgenWennHeuteSchonGeuebt() {
        assertEquals(zeit(22, 16, 0), LernzeitErinnerung.zeitpunkt(zeit(21, 10, 0), 16 * 60, true, zone))
    }

    @Test fun genauJetztGiltAlsVorbei() {
        assertEquals(zeit(22, 16, 0), LernzeitErinnerung.zeitpunkt(zeit(21, 16, 0), 16 * 60, false, zone))
    }

    @Test fun uhrzeitText() {
        assertEquals("07:05", LernzeitErinnerung.uhrzeitText(7 * 60 + 5))
        assertEquals("16:00", LernzeitErinnerung.uhrzeitText(LernzeitErinnerung.STANDARD_MINUTEN))
    }
}
