package de.cemaras.yem1n

import de.cemaras.yem1n.logik.StimmOrt
import de.cemaras.yem1n.logik.StimmPaket
import de.cemaras.yem1n.logik.StimmPruefung
import de.cemaras.yem1n.logik.TarAuspacker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class StimmenTest {
    private fun tempOrdner(): File = java.nio.file.Files.createTempDirectory("stimmen").toFile()

    private fun oktal(n: Long, stellen: Int): ByteArray =
        (n.toString(8).padStart(stellen - 1, '0') + "\u0000").toByteArray(Charsets.US_ASCII)

    /** Baut ein ustar-Archiv wie packen.py: Einträge sind (Name, Inhalt). */
    private fun tar(eintraege: List<Pair<String, ByteArray>>, art: Char = '0'): ByteArray {
        val aus = ByteArrayOutputStream()
        for ((name, daten) in eintraege) {
            val kopf = ByteArray(512)
            val n = name.toByteArray(Charsets.UTF_8)
            if (n.size <= 100) {
                System.arraycopy(n, 0, kopf, 0, n.size)
            } else {
                // Langer Pfad: letzter Teil im Namen, Rest im Vorsatz
                val teile = name.split("/")
                val nameTeil = teile.last().toByteArray()
                val vorsatz = teile.dropLast(1).joinToString("/").toByteArray()
                System.arraycopy(nameTeil, 0, kopf, 0, nameTeil.size)
                System.arraycopy(vorsatz, 0, kopf, 345, vorsatz.size)
            }
            System.arraycopy(oktal(daten.size.toLong(), 12), 0, kopf, 124, 12)
            kopf[156] = art.code.toByte()
            System.arraycopy("ustar\u0000".toByteArray(), 0, kopf, 257, 6)
            aus.write(kopf)
            aus.write(daten)
            aus.write(ByteArray((512 - daten.size % 512) % 512))
        }
        aus.write(ByteArray(1024))
        return aus.toByteArray()
    }

    @Test
    fun pakeleHabenEindeutigeDaten() {
        val alle = listOf(StimmPaket.aussprache) + StimmPaket.stimmen
        assertEquals(alle.size, alle.map { it.id }.toSet().size)
        for (p in alle) {
            assertEquals(64, p.sha256.length)
            assertTrue(p.bytes > 0)
            assertTrue(p.url.startsWith("https://github.com/cembot90/YEM1N/releases/download/stimmen-1/"))
            assertTrue(p.url.endsWith(p.id + ".tar"))
        }
        assertEquals("Thorsten", StimmPaket.fuer("de")?.name)
        assertEquals("Fahrettin", StimmPaket.fuer("tr")?.name)
        assertEquals("Cori", StimmPaket.fuer("en")?.name)
        assertNull(StimmPaket.fuer("fr"))
        assertEquals(63, StimmPaket.stimmen[0].megabyte)
        assertEquals(1, StimmPaket.aussprache.megabyte)
    }

    @Test
    fun auspackenMitOrdnernUndLangenPfaden() {
        val inhalt = ByteArray(1300) { (it % 251).toByte() }
        val lang = "a".repeat(60) + "/" + "b".repeat(60) + "/datei.txt"
        val arch = tar(listOf("de-thorsten/stimme.onnx" to inhalt, "de-thorsten/tokens.txt" to "x y".toByteArray(), lang to "lang".toByteArray()))
        val d = tempOrdner()
        val datei = File(d, "t.tar").also { it.writeBytes(arch) }
        val ziel = File(d, "aus")
        assertEquals(3, TarAuspacker.auspacken(datei, ziel))
        assertTrue(inhalt.contentEquals(File(ziel, "de-thorsten/stimme.onnx").readBytes()))
        assertEquals("x y", File(ziel, "de-thorsten/tokens.txt").readText())
        assertEquals("lang", File(ziel, lang).readText())
    }

    @Test
    fun gefaehrlichePfadeWerdenAbgelehnt() {
        assertFalse(TarAuspacker.sicher("../x"))
        assertFalse(TarAuspacker.sicher("a/../../x"))
        assertFalse(TarAuspacker.sicher("/etc/x"))
        assertTrue(TarAuspacker.sicher("de/stimme.onnx"))
        val d = tempOrdner()
        val datei = File(d, "t.tar").also { it.writeBytes(tar(listOf("../boese.txt" to "x".toByteArray()))) }
        var fehler = false
        try {
            TarAuspacker.auspacken(datei, File(d, "aus"))
        } catch (e: IOException) {
            fehler = true
        }
        assertTrue(fehler)
        assertFalse(File(d, "boese.txt").exists())
    }

    @Test
    fun abgeschnittenesPaketIstEinFehler() {
        val arch = tar(listOf("a/b.bin" to ByteArray(5000)))
        val d = tempOrdner()
        val datei = File(d, "t.tar").also { it.writeBytes(arch.copyOf(2000)) }
        var fehler = false
        try {
            TarAuspacker.auspacken(datei, File(d, "aus"))
        } catch (e: IOException) {
            fehler = true
        }
        assertTrue(fehler)
    }

    @Test
    fun einrichtenSetztDieMarkeErstAmEnde() {
        val d = tempOrdner()
        val p = StimmPaket("test-1", "de", "Test", "de-test", 0L, "")
        val ort = StimmOrt(File(d, "stimmen"))
        assertFalse(ort.installiert(p))
        val datei = File(d, "t.tar").also { it.writeBytes(tar(listOf("de-test/stimme.onnx" to "modell".toByteArray(), "de-test/tokens.txt" to "t".toByteArray()))) }
        ort.einrichten(p, datei)
        assertTrue(ort.installiert(p))
        assertEquals("modell", File(ort.ordner(p), "stimme.onnx").readText())
        assertFalse(File(ort.wurzel, "auspacken-test-1").exists())
        // Neu einrichten ersetzt die alte Fassung
        File(ort.ordner(p), "alt.txt").writeText("alt")
        ort.einrichten(p, datei)
        assertFalse(File(ort.ordner(p), "alt.txt").exists())
        ort.entfernen(p)
        assertFalse(ort.installiert(p))
        assertFalse(ort.ordner(p).exists())
    }

    @Test
    fun fehlenderOrdnerImPaketMeldetFehler() {
        val d = tempOrdner()
        val p = StimmPaket("test-2", "de", "Test", "de-test", 0L, "")
        val ort = StimmOrt(File(d, "stimmen"))
        val datei = File(d, "t.tar").also { it.writeBytes(tar(listOf("anders/stimme.onnx" to "m".toByteArray()))) }
        var fehler = false
        try {
            ort.einrichten(p, datei)
        } catch (e: IOException) {
            fehler = true
        }
        assertTrue(fehler)
        assertFalse(ort.installiert(p))
    }

    @Test
    fun bereitBrauchtStimmeUndAussprache() {
        val d = tempOrdner()
        val ort = StimmOrt(File(d, "s"))
        val p = StimmPaket.stimmen[0]
        ort.wurzel.mkdirs()
        ort.marke(p).writeBytes(ByteArray(0))
        assertTrue(ort.installiert(p))
        assertFalse(ort.bereit(p))
        ort.marke(StimmPaket.aussprache).writeBytes(ByteArray(0))
        assertTrue(ort.bereit(p))
    }

    @Test
    fun aufraeumenEntferntAlteMarken() {
        val d = tempOrdner()
        val ort = StimmOrt(File(d, "s"))
        ort.wurzel.mkdirs()
        File(ort.wurzel, "de-thorsten-1.fertig").writeBytes(ByteArray(0))
        File(ort.wurzel, "de-thorsten-2.fertig").writeBytes(ByteArray(0))
        ort.aufraeumen()
        assertFalse(File(ort.wurzel, "de-thorsten-1.fertig").exists())
        assertTrue(File(ort.wurzel, "de-thorsten-2.fertig").exists())
    }

    @Test
    fun pruefungErkenntFalscheGroesseUndPruefsumme() {
        val d = tempOrdner()
        val datei = File(d, "x.bin").also { it.writeText("hello") }
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", StimmPruefung.sha256(datei))
        val richtig = StimmPaket("x", "de", "X", "x", 5L, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")
        assertNull(StimmPruefung.fehler(datei, richtig))
        val falscheGroesse = StimmPaket("x", "de", "X", "x", 6L, richtig.sha256)
        assertTrue(StimmPruefung.fehler(datei, falscheGroesse) != null)
        val falscheSumme = StimmPaket("x", "de", "X", "x", 5L, "0".repeat(64))
        assertTrue(StimmPruefung.fehler(datei, falscheSumme) != null)
    }
}
