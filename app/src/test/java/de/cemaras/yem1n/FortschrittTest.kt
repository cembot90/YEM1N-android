package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Fortschritt
import org.junit.Assert.assertEquals
import org.junit.Test

class FortschrittTest {
    private val arbeit = "Klasse 3|Mathe|Klassenarbeit Nr. 1"

    @Test fun beginntLeer() {
        assertEquals(0, Fortschritt().stand(arbeit, "Einmaleins").sterne)
    }

    @Test fun gespeichertWirdDasBeste() {
        var f = Fortschritt().nachRunde(arbeit, "Einmaleins", 9, 12)
        assertEquals(2, f.stand(arbeit, "Einmaleins").sterne)
        f = f.nachRunde(arbeit, "Einmaleins", 5, 12)
        val st = f.stand(arbeit, "Einmaleins")
        assertEquals(9, st.beste)
        assertEquals(2, st.sterne)
        assertEquals(2, st.runden)
        f = f.nachRunde(arbeit, "Einmaleins", 12, 12)
        assertEquals(3, f.stand(arbeit, "Einmaleins").sterne)
    }

    @Test fun sterneDerArbeitZaehlenAlleUebungen() {
        val f = Fortschritt()
            .nachRunde(arbeit, "A", 10, 10)
            .nachRunde(arbeit, "B", 7, 10)
        assertEquals(5, f.sterneDerArbeit(arbeit, listOf("A", "B", "C")))
    }

    @Test fun jsonHinUndZurueck() {
        val f = Fortschritt().nachRunde(arbeit, "Einmaleins", 10, 12)
        assertEquals(f, Fortschritt.ausJson(f.zuJson()))
    }

    @Test fun kaputterTextBeginntVonVorn() {
        assertEquals(Fortschritt(), Fortschritt.ausJson("{kaputt"))
        assertEquals(Fortschritt(), Fortschritt.ausJson(""))
        assertEquals(Fortschritt(), Fortschritt.ausJson(null))
    }

    @Test fun ohneHaustierBleibtEsLeer() {
        val (neu, _) = Fortschritt().nachRundeMit(false, arbeit, "Einmaleins", List(3) { de.cemaras.yem1n.logik.Stand.RICHTIG }, 100L, "Mathe", 12, 0L)
        assertEquals(null, neu.haustier)
    }

    @Test fun haustierWaechstNachRundeUndBleibtImJson() {
        val ei = de.cemaras.yem1n.logik.Haustier.neu("Momo", "hase", 0L)
        val start = Fortschritt(haustier = ei)
        val (neu, _) = start.nachRundeMit(false, arbeit, "Einmaleins", List(5) { de.cemaras.yem1n.logik.Stand.RICHTIG }, 100L, "Mathe", 12, 0L)
        val h = neu.haustier!!
        assertEquals(true, h.xp > 0)
        assertEquals(true, h.stufe != de.cemaras.yem1n.logik.HaustierStufe.EI)
        val ereignis = de.cemaras.yem1n.logik.Haustier.ereignis(ei, h)!!
        assertEquals(true, ereignis.geschluepft)
        assertEquals(h, Fortschritt.ausJson(neu.zuJson()).haustier)
    }

    @Test fun altesJsonOhneHaustierWirdGelesen() {
        assertEquals(null, Fortschritt.ausJson("{\"muenzen\": 5}").haustier)
    }
}
