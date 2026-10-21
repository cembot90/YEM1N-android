package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Lob
import de.cemaras.yem1n.logik.Stand
import de.cemaras.yem1n.logik.VorschulArt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VorschulFortschrittTest {
    private fun runde(f: Fortschritt, art: VorschulArt, richtig: Int, gesamt: Int): Pair<Fortschritt, Int> {
        val staende = List(gesamt) { if (it < richtig) Stand.RICHTIG else Stand.FALSCH }
        return f.nachRundeMit(false, Fortschritt.VORSCHUL_ARBEIT, art.uebungName, staende, 100L, "Vorschule")
    }

    @Test
    fun einSternGibtEinenStempel() {
        val (f, _) = runde(Fortschritt(), VorschulArt.ZAEHLEN, 5, 8)
        assertTrue(VorschulArt.ZAEHLEN in f.vorschulStempel())
    }

    @Test
    fun zuWenigRichtigGibtKeinenStempel() {
        val (f, _) = runde(Fortschritt(), VorschulArt.PLUS, 3, 8)
        assertFalse(VorschulArt.PLUS in f.vorschulStempel())
    }

    @Test
    fun einmalGeschafftBleibtGestempelt() {
        var f = runde(Fortschritt(), VorschulArt.MEMORY, 4, 4).first
        f = runde(f, VorschulArt.MEMORY, 1, 8).first
        assertTrue(VorschulArt.MEMORY in f.vorschulStempel())
    }

    @Test
    fun muenzenUndRundenWerdenGezaehlt() {
        val (f, muenzen) = runde(Fortschritt(), VorschulArt.FORMEN, 8, 8)
        assertEquals(8 + 5, muenzen)
        assertEquals(muenzen, f.muenzen)
        assertEquals(1, f.rundenHeute(100L))
        assertEquals("Vorschule", f.runden.last().fach)
    }

    @Test
    fun neueEinstellungenHabenSinnvolleStandardwerte() {
        val f = Fortschritt()
        assertEquals(2, f.vorschulStufe)
        assertTrue(f.vorlesen)
        // Ein alter Speicherstand ohne die neuen Felder lädt weiter
        val alt = Fortschritt.ausJson("{\"muenzen\":7}")
        assertEquals(7, alt.muenzen)
        assertEquals(2, alt.vorschulStufe)
        assertTrue(alt.vorlesen)
    }

    @Test
    fun nochmalLobGibtImmerEinenSatz() {
        repeat(30) { assertTrue(Lob.fuerNochmal().isNotBlank()) }
    }
}
