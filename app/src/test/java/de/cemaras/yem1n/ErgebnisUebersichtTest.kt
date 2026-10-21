package de.cemaras.yem1n

import de.cemaras.yem1n.logik.ErgebnisUebersicht
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import org.junit.Assert.assertEquals
import org.junit.Test

class ErgebnisUebersichtTest {
    private fun e(kind: String, richtig: Int, gesamt: Int, sterne: Int) =
        RundenErgebnisEintrag("id$kind$richtig", "Klasse 3", "Mathe", "Nr. 1", "Geteilt", richtig, gesamt, 0, sterne, kind, 1L)

    @Test fun summenUndProzent() {
        val u = ErgebnisUebersicht.von(listOf(e("Yemin", 8, 10, 2), e("Yemin", 9, 10, 3), e("Evîn", 5, 10, 1)))
        assertEquals(3, u.runden)
        assertEquals(22, u.richtig)
        assertEquals(30, u.gesamt)
        assertEquals(73, u.prozent)
        assertEquals(6, u.sterne)
        assertEquals(listOf("Yemin" to 2, "Evîn" to 1), u.proKind)
    }

    @Test fun leereListeIstNull() {
        val u = ErgebnisUebersicht.von(emptyList())
        assertEquals(0, u.prozent)
        assertEquals(0, u.runden)
    }

    @Test fun filterNachKind() {
        val l = listOf(e("Yemin", 8, 10, 2), e("Evîn", 5, 10, 1), e("", 7, 10, 2))
        assertEquals(1, ErgebnisUebersicht.fuerKind(l, "Yemin").size)
        assertEquals(1, ErgebnisUebersicht.fuerKind(l, "Kind").size)
        assertEquals(3, ErgebnisUebersicht.fuerKind(l, null).size)
    }
}
