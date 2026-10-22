package de.cemaras.yem1n

import de.cemaras.yem1n.logik.Familiencode
import de.cemaras.yem1n.logik.QrPaket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrPaketTest {
    @Test fun hinUndZurueck() {
        val code = Familiencode.neu()
        val gelesen = QrPaket.lese(QrPaket.inhalt("familie", code))
        assertEquals("familie" to code, gelesen)
    }

    @Test fun fremdesWirdAbgelehnt() {
        assertNull(QrPaket.lese("https://example.com"))
        assertNull(QrPaket.lese("yem1n://familie/zukurz"))
        assertNull(QrPaket.lese("yem1n://etwas/ABCDE-FGHJK"))
        assertNull(QrPaket.lese("yem1n://familie"))
    }

    @Test fun grossUndKleinEgal() {
        val code = Familiencode.neu()
        assertEquals("familie" to code, QrPaket.lese("  YEM1N://FAMILIE/${code.lowercase()}  "))
    }
}
