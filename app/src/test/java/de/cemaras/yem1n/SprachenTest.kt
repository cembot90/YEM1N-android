package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Sprachen
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SprachenTest {
    @Test fun woerterDa() {
        for (s in Sprachen.alle) assertTrue(s.code, Sprachen.woerter("farben", s.code).size >= 6)
    }

    @Test fun paareSindEindeutig() {
        for (s in Sprachen.alle) for (t in Sprachen.themen) {
            val p = Sprachen.paare(Sprachen.woerter(t.id, s.code), s.code, 6, Random(7))
            assertEquals(p.size, p.map { it.de }.distinct().size)
            assertEquals(p.size, p.map { it.fremd }.distinct().size)
            assertTrue("${s.code} ${t.id}", p.size >= 6)
        }
    }

    @Test fun jedesThemaHatFragen() {
        for (s in Sprachen.alle) for (t in Sprachen.themen) {
            if (s.code == "en" && t.id !in de.cemaras.yem1n.logik.lehrplan.LehrplanEnglisch.katalogZuThemaId) continue
            assertNotNull("${s.code} ${t.id}", Sprachen.lernThema(t.id, s.code))
        }
    }
}
