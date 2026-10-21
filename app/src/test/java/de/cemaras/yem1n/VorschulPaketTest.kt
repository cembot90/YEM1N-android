package de.cemaras.yem1n

import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.daten.Uebung
import de.cemaras.yem1n.logik.VorschulPaket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VorschulPaketTest {
    private fun wahl(frage: String, antwort: String, opt: List<String>, rechnung: String = "", tr: String = "", antwort2: String = "") =
        Aufgabe("wahl", frage, rechnung, "", "", antwort, antwort2, emptyList(), "", tr, "", emptyList(), opt)

    private fun uebung(vararg a: Aufgabe) = Uebung("Test", "Gruppe", "🧸", "", a.toList())

    @Test fun richtigeAntwortBleibtNachDemMischenRichtig() {
        val u = uebung(wahl("Wie viele?", "3", listOf("2", "3", "4"), "🍎🍎🍎"))
        repeat(20) {
            val f = VorschulPaket.fragen(u, Random(it)).single()
            assertEquals("3", f.optionen[f.richtig])
            assertEquals(listOf("🍎", "🍎", "🍎"), f.zeige)
        }
    }

    @Test fun plusAufgabeZeigtZweiGruppen() {
        val f = VorschulPaket.fragen(uebung(wahl("Zusammen?", "5", listOf("4", "5"), "🍎🍎+🍌🍌🍌")), Random(1)).single()
        assertTrue(f.plus)
        assertEquals(2, f.zeige.size)
        assertEquals(3, f.zeigeZwei.size)
    }

    @Test fun ungueltigeAufgabenFallenWeg() {
        val u = uebung(
            wahl("Ohne Treffer", "9", listOf("1", "2")),
            wahl("Zu wenig", "1", listOf("1")),
            Aufgabe("zahl", "Rechne", "1 + 1 =", "", "", "2", "", emptyList(), "", "", "", emptyList(), emptyList()),
            wahl("Gut", "a", listOf("a", "b")),
        )
        val f = VorschulPaket.fragen(u, Random(2))
        assertEquals(1, f.size)
        assertEquals("Gut", f[0].text.de)
    }

    @Test fun uebersetzungUndFolge() {
        val f = VorschulPaket.fragen(uebung(wahl("Was kommt?", "3", listOf("3", "4"), "", "Sırada ne var?", "1 2")), Random(3)).single()
        assertEquals("Sırada ne var?", f.text.tr)
        assertEquals(listOf("1", "2"), f.folge)
        assertEquals(false, f.gruppenOptionen)
    }
}
