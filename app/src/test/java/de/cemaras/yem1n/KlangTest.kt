package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Klang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KlangTest {
    @Test fun jederKlangHatDieRichtigeLaenge() {
        for (k in Klang.entries) {
            val pcm = Klang.pcm(k)
            assertEquals(k.name, (k.dauerMs * Klang.RATE / 1000).toLong(), pcm.size.toLong())
        }
    }

    @Test fun klangIstNichtStummUndNichtUebersteuert() {
        for (k in Klang.entries) {
            val pcm = Klang.pcm(k)
            val hoechster = pcm.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue(k.name, hoechster > 3000)
            assertTrue(k.name, hoechster < Short.MAX_VALUE)
        }
    }

    @Test fun anfangUndEndeSindLeise() {
        val pcm = Klang.pcm(Klang.TOR)
        assertTrue(kotlin.math.abs(pcm.first().toInt()) < 500)
        assertTrue(kotlin.math.abs(pcm.last().toInt()) < 500)
    }

    @Test fun tonUndVibrationSindStandardmaessigAn() {
        val f = Fortschritt()
        assertTrue(f.spielTon && f.spielHaptik)
        assertEquals(false, Fortschritt.ausJson(Fortschritt(spielTon = false).zuJson()).spielTon)
    }
}
