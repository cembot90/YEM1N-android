package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.SpielArt
import de.cemaras.yem1n.logik.SpielRegeln
import de.cemaras.yem1n.logik.Stand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpieleTest {
    private val heute = 500L

    private fun geuebtMitMuenzen(muenzen: Int) =
        Fortschritt(muenzen = muenzen, letzterTag = heute, rundenAmLetztenTag = 1)

    @Test
    fun ohneUebungKeinSpiel() {
        val f = Fortschritt(muenzen = 100)
        assertTrue(SpielRegeln.sperre(f, heute)!!.startsWith("Erst eine Übung"))
    }

    @Test
    fun uebungVonGesternZaehltNicht() {
        val f = Fortschritt(muenzen = 100, letzterTag = heute - 1, rundenAmLetztenTag = 3)
        assertTrue(SpielRegeln.sperre(f, heute)!!.startsWith("Erst eine Übung"))
    }

    @Test
    fun zuWenigMuenzen() {
        val grund = SpielRegeln.sperre(geuebtMitMuenzen(14), heute)!!
        assertTrue(grund.contains("15 Münzen"))
        assertTrue(grund.contains("14"))
        assertNull(SpielRegeln.sperre(geuebtMitMuenzen(15), heute))
    }

    @Test
    fun elternKoennenSpieleAusschalten() {
        val f = geuebtMitMuenzen(100).copy(spieleErlaubt = false)
        assertTrue(SpielRegeln.sperre(f, heute)!!.contains("ausgeschaltet"))
    }

    @Test
    fun tageslimit() {
        var f = geuebtMitMuenzen(100)
        repeat(3) {
            assertNull(SpielRegeln.sperre(f, heute))
            f = f.spielBezahlt(heute)
        }
        assertEquals(55, f.muenzen)
        assertEquals(3, f.spieleHeute(heute))
        assertTrue(SpielRegeln.sperre(f, heute)!!.contains("3 von 3"))
        // Am nächsten Tag zählt wieder von vorn
        assertEquals(0, f.spieleHeute(heute + 1))
    }

    @Test
    fun bestwertNurWennBesser() {
        val (a, rekord1) = Fortschritt().mitBestwert("einhorn", 10)
        assertTrue(rekord1)
        val (b, rekord2) = a.mitBestwert("einhorn", 10)
        assertFalse(rekord2)
        val (c, rekord3) = b.mitBestwert("einhorn", 4)
        assertFalse(rekord3)
        assertEquals(10, c.bestwerte["einhorn"])
        val (d, rekord4) = c.mitBestwert("einhorn", 11)
        assertTrue(rekord4)
        assertEquals(11, d.bestwerte["einhorn"])
        assertEquals(0, Fortschritt().mitBestwert("lauf", 0).second.let { if (it) 1 else 0 })
    }

    @Test
    fun eineRundeMachtDenTagZumUebetag() {
        val f = Fortschritt().nachRundeMit(false, "a", "u", listOf(Stand.RICHTIG), heute, "Mathe").first
        assertTrue(f.heuteGeuebt(heute))
        assertFalse(f.heuteGeuebt(heute + 1))
    }

    @Test
    fun spieleHabenEindeutigeSchluessel() {
        assertEquals(SpielArt.entries.size, SpielArt.entries.map { it.schluessel }.toSet().size)
        assertTrue(SpielArt.entries.all { it.verfuegbar })
    }

    @Test
    fun alterSpeicherstandLaedtMitStandardwerten() {
        val alt = Fortschritt.ausJson("{\"muenzen\":30}")
        assertTrue(alt.spieleErlaubt)
        assertEquals(3, alt.spieleProTag)
        assertTrue(alt.bestwerte.isEmpty())
    }
}
