package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.JokerCloud
import de.cemaras.yem1n.logik.JokerNachschub
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JokerNachschubTest {
    private fun ds(name: String, erstellt: Long, kind: String = "Yemin") =
        JokerCloud.Datensatz(name, erstellt, mapOf("kind" to JsonPrimitive(kind)))

    @Test fun uebrigRechnetProTag() {
        assertEquals("neuer Tag ist voll", 3, JokerNachschub.uebrig(3, 10, 2, 11))
        assertEquals("heute zwei benutzt", 1, JokerNachschub.uebrig(3, 11, 2, 11))
        assertEquals("nie unter null", 0, JokerNachschub.uebrig(3, 11, 5, 11))
    }

    @Test fun fortschrittZaehltUndFuelltAuf() {
        var f = Fortschritt()
        assertEquals(3, f.jokerUebrig(100))
        f = f.mitJokerGenutzt(100).mitJokerGenutzt(100).mitJokerGenutzt(100)
        assertEquals(0, f.jokerUebrig(100))
        assertEquals("am nächsten Tag wieder voll", 3, f.jokerUebrig(101))
        assertEquals(3, f.mitJokerAufgefuellt(100).jokerUebrig(100))
    }

    @Test fun offeneAnfrageNurNachLetzterFreigabe() {
        val n = listOf(ds("a", 100, "Yemin"), ds("b", 300, "Evîn"))
        assertEquals("Evîn", JokerNachschub.offeneAnfrage(n, emptyList()))
        assertEquals("Evîn", JokerNachschub.offeneAnfrage(n, listOf(ds("f", 200))))
        assertNull(JokerNachschub.offeneAnfrage(n, listOf(ds("f", 400))))
        assertNull(JokerNachschub.offeneAnfrage(emptyList(), emptyList()))
    }

    @Test fun neueFreigabenIgnorierenAlteUndBenutzte() {
        val fr = listOf(ds("alt", 50), ds("benutzt", 150), ds("neu", 250))
        assertEquals(listOf("neu"), JokerNachschub.neueFreigaben(fr, listOf("benutzt"), 100))
        assertEquals(emptyList<String>(), JokerNachschub.neueFreigaben(fr, listOf("neu", "benutzt"), 100))
    }
}
