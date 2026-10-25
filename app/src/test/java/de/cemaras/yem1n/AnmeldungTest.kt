package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Anmeldung
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import org.junit.Assert.assertEquals
import org.junit.Test

class AnmeldungTest {
    private fun runde(kind: String, zeit: Long) = RundenErgebnisEintrag(
        eintragId = "r$zeit", klasse = "Klasse 3", fach = "Mathe", arbeit = "A", uebung = "Minus",
        richtig = 8, gesamt = 10, angesehen = 0, sterne = 2, kind = kind, zeitpunkt = zeit,
    )

    @Test fun kinderAuswahlHaengtNurAngemeldeteAn() {
        assertEquals(listOf("Yemin", "Evin"), Anmeldung.kinder(listOf("Yemin"), listOf("yemin", "Evin")))
    }

    @Test fun kinderZeilenZaehlenRundenUndNehmenGeraeteOhneRundeDazu() {
        val runden = listOf(runde("Yemin", 10), runde("yemin ", 30), runde("Mia", 20))
        val z = Anmeldung.kinderZeilen(runden, listOf("Evin", "Yemin"))
        assertEquals(listOf("Evin", "Mia", "yemin"), z.map { it.name })
        assertEquals(listOf(0, 1, 2), z.map { it.runden })
        assertEquals(30L, z.first { it.name == "yemin" }.zuletzt)
        assertEquals(0L, z.first { it.name == "Evin" }.zuletzt)
    }

    @Test fun leererNameWirdZuKind() {
        val z = Anmeldung.kinderZeilen(emptyList(), listOf("  "))
        assertEquals(listOf("Kind"), z.map { it.name })
    }
}
