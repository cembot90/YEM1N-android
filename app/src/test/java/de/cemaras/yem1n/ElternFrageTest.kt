package de.cemaras.yem1n

import de.cemaras.yem1n.logik.ElternFrage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ElternFrageTest {
    @Test fun zahlenImBereich() {
        val r = Random(7)
        repeat(200) {
            val f = ElternFrage.neu(r)
            assertTrue(f.a in ElternFrage.BEREICH && f.b in ElternFrage.BEREICH)
            assertTrue(f.ergebnis >= 144)
        }
    }

    @Test fun nurDasErgebnisStimmt() {
        val f = ElternFrage(13, 17)
        assertTrue(f.stimmt("221"))
        assertTrue(f.stimmt(" 221 "))
        assertFalse(f.stimmt("220"))
        assertFalse(f.stimmt(""))
        assertFalse(f.stimmt("abc"))
    }

    @Test fun textWieIos() {
        val t = ElternFrage(12, 19).text
        assertTrue(t.contains("12 · 19"))
        assertFalse(t.contains("–") || t.contains("—"))
    }
}
