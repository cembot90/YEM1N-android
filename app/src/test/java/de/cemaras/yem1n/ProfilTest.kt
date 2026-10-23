package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Profil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilTest {
    @Test
    fun klassenNamenWerdenZuZahlen() {
        assertEquals(0, Profil.lernKlasse("Vorschule"))
        assertEquals(0, Profil.lernKlasse(""))
        assertEquals(3, Profil.lernKlasse("Klasse 3"))
        assertEquals(4, Profil.lernKlasse("Klasse 9"))
    }

    @Test
    fun neueInstallationBekommtDieEinrichtung() {
        assertTrue(Profil.einrichtungNoetig(Fortschritt()))
        assertFalse(Profil.einrichtungNoetig(Fortschritt(name = "Yemin")))
        assertFalse(Profil.einrichtungNoetig(Fortschritt(profilFertig = true)))
    }

    @Test
    fun kindWirdUebernommen() {
        val f = Profil.uebernehmen(Fortschritt(), "kind", "  Yemin ", "Klasse 3", true, true, 9, true)
        assertEquals("Yemin", f.name)
        assertEquals(3, f.lernKlasse)
        assertFalse(f.vorschulAn)
        assertTrue(f.sprachenAn)
        assertTrue(f.rosa)
        assertEquals(5, f.tagesziel)
        assertTrue(f.autoVorlesen)
        assertTrue(f.profilFertig)
    }

    @Test
    fun vorschuleBleibtAn() {
        val f = Profil.uebernehmen(Fortschritt(vorschulAn = false), "kind", "Evîn", "Vorschule", false, false, 2, false)
        assertTrue(f.vorschulAn)
        assertEquals(0, f.lernKlasse)
    }

    @Test
    fun elternWaehlenNurDieRolle() {
        val f = Profil.uebernehmen(Fortschritt(name = "X"), "eltern", "", "", false, false, 1, false)
        assertEquals("eltern", f.jokerRolle)
        assertEquals("X", f.name)
        assertTrue(f.profilFertig)
    }
}
