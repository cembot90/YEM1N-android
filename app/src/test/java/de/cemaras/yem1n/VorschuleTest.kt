package de.cemaras.yem1n

import de.cemaras.yem1n.logik.MemoryStand
import de.cemaras.yem1n.logik.Stand
import de.cemaras.yem1n.logik.VorschulArt
import de.cemaras.yem1n.logik.VorschulFrage
import de.cemaras.yem1n.logik.VorschulGenerator
import de.cemaras.yem1n.logik.VorschulStand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VorschulGeneratorTest {
    private val spielArten = VorschulArt.entries.filter { it != VorschulArt.MEMORY && it != VorschulArt.MIX }

    private fun zahl(text: String, regex: String): List<Int> =
        Regex(regex).find(text)!!.groupValues.drop(1).map { it.toInt() }

    @Test
    fun grundregelnFuerJedeFrage() {
        for (seed in 0 until 200) {
            val z = Random(seed)
            for (stufe in 1..3) for (art in spielArten) {
                val f = VorschulGenerator.eine(art, stufe, z)
                val ctx = "$art Stufe $stufe Seed $seed ${f.text.de}"
                assertTrue(ctx, f.richtig in f.optionen.indices)
                assertTrue(ctx, f.optionen.size in 2..4)
                assertTrue(ctx, f.text.de.isNotBlank())
                if (art != VorschulArt.UNTERSCHIED) {
                    assertEquals(ctx, f.optionen.size, f.optionen.toSet().size)
                } else {
                    // drei gleiche Bilder und ein anderes, das andere ist die richtige Antwort
                    val anders = f.optionen[f.richtig]
                    assertEquals(ctx, 1, f.optionen.count { it == anders })
                    assertEquals(ctx, 2, f.optionen.toSet().size)
                }
            }
        }
    }

    @Test
    fun rechenfragenStimmen() {
        for (seed in 0 until 300) {
            val z = Random(seed)
            for (stufe in 1..3) {
                val grenze = VorschulGenerator.grenze(stufe)
                // Zählen
                var f = VorschulGenerator.eine(VorschulArt.ZAEHLEN, stufe, z)
                assertEquals(f.zeige.size.toString(), f.optionen[f.richtig])
                assertTrue(f.zeige.size in 1..(if (stufe == 1) 5 else 10))
                // Zahlen
                f = VorschulGenerator.eine(VorschulArt.ZAHLEN, stufe, z)
                assertEquals(zahl(f.text.de, "die (\\d+)")[0].toString(), f.optionen[f.richtig])
                assertTrue(f.optionen.all { it.toInt() in 1..grenze })
                // Plus
                f = VorschulGenerator.eine(VorschulArt.PLUS, stufe, z)
                val (x, y) = zahl(f.text.de, "(\\d+) plus (\\d+)")
                assertEquals((x + y).toString(), f.optionen[f.richtig])
                assertEquals(x, f.zeige.size)
                assertEquals(y, f.zeigeZwei.size)
                assertTrue(x + y <= grenze)
                assertTrue(f.plus)
                // Nachbar
                f = VorschulGenerator.eine(VorschulArt.NACHBAR, stufe, z)
                val n = f.gross.toInt()
                val erwartet = if (f.text.de.contains("nach der")) n + 1 else n - 1
                assertEquals(erwartet.toString(), f.optionen[f.richtig])
                assertTrue(erwartet in 0..grenze)
                // Wegnehmen
                f = VorschulGenerator.eine(VorschulArt.MINUS, stufe, z)
                val rest = f.zeige.count { it != "💨" }
                val weg = f.zeige.count { it == "💨" }
                assertTrue(weg >= 1 && rest >= 1)
                assertEquals(rest.toString(), f.optionen[f.richtig])
                assertTrue(f.text.de.startsWith("Es waren ${rest + weg}."))
                // Teilen
                f = VorschulGenerator.eine(VorschulArt.TEILEN, stufe, z)
                val kinder = zahl(f.text.de, "auf (\\d+) Kinder")[0]
                assertEquals(0, f.zeige.size % kinder)
                assertEquals((f.zeige.size / kinder).toString(), f.optionen[f.richtig])
                // Zahl und Menge
                f = VorschulGenerator.eine(VorschulArt.ZAHLBILD, stufe, z)
                val gesucht = f.gross.toInt()
                val bild = f.optionen[f.richtig].replace("\n", "")
                assertEquals(gesucht, bild.codePointCount(0, bild.length))
            }
        }
    }

    @Test
    fun wuerfelMitDemRichtigenBild() {
        val bilder = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")
        for (seed in 0 until 100) {
            val f = VorschulGenerator.eine(VorschulArt.WUERFEL, 2, Random(seed))
            assertEquals((bilder.indexOf(f.gross) + 1).toString(), f.optionen[f.richtig])
        }
    }

    @Test
    fun mehrOderWeniger() {
        for (seed in 0 until 300) {
            val f = VorschulGenerator.eine(VorschulArt.MEHR, 2, Random(seed))
            val a = f.optionen[0].replace("\n", "").let { it.codePointCount(0, it.length) }
            val b = f.optionen[1].replace("\n", "").let { it.codePointCount(0, it.length) }
            assertTrue(a != b)
            val mehr = f.text.de == "Wo sind mehr?"
            val erwartet = if (mehr) (if (a > b) 0 else 1) else (if (a < b) 0 else 1)
            assertEquals(erwartet, f.richtig)
            assertTrue(f.gruppenOptionen)
        }
    }

    @Test
    fun musterGehtRichtigWeiter() {
        for (seed in 0 until 300) {
            val f = VorschulGenerator.eine(VorschulArt.MUSTER, 2, Random(seed))
            // Die Folge muss zu einem Muster der Länge 2 oder 3 passen, die Antwort ist dessen nächstes Zeichen
            val folge = f.folge
            val richtig = f.optionen[f.richtig]
            var gefunden = false
            for (l in 2..3) {
                if (folge.indices.all { folge[it] == folge[it % l] } && folge.size > l) {
                    // Der Anfang der Folge enthält das ganze Muster, also gibt es nach der Folge nur eine Fortsetzung
                    assertEquals(folge[folge.size % l], richtig)
                    gefunden = true
                    break
                }
            }
            assertTrue("Keine Periode in $folge", gefunden)
        }
    }

    @Test
    fun spracheUndLaute() {
        for (seed in 0 until 300) {
            val z = Random(seed)
            var f = VorschulGenerator.eine(VorschulArt.ANLAUT, 2, z)
            val wort = Regex("Womit fängt (.+) an\\?").find(f.text.de)!!.groupValues[1]
            val eintrag = VorschulGenerator.anlaute.first { it.second == wort }
            assertEquals(eintrag.third, f.optionen[f.richtig])
            assertEquals(eintrag.first, f.gross)

            f = VorschulGenerator.eine(VorschulArt.BUCHSTABE, 2, z)
            assertEquals(f.text.de.removePrefix("Tippe auf das ").removeSuffix("."), f.optionen[f.richtig])

            f = VorschulGenerator.eine(VorschulArt.SILBEN, 2, z)
            val w = Regex("Wie viele Silben hat (.+)\\? Klatsche mit!").find(f.text.de)!!.groupValues[1]
            val silben = VorschulGenerator.silbenBilder.first { it.second == w }.third
            assertEquals(silben.toString(), f.optionen[f.richtig])

            f = VorschulGenerator.eine(VorschulArt.REIME, 2, z)
            val frageWort = Regex("Was reimt sich auf (.+)\\?").find(f.text.de)!!.groupValues[1]
            val paar = VorschulGenerator.reimPaare.first { it.first.second == frageWort || it.second.second == frageWort }
            val erwartet = if (paar.first.second == frageWort) paar.second.first else paar.first.first
            assertEquals(erwartet, f.optionen[f.richtig])
            assertFalse(f.optionen.any { it == f.gross })
        }
    }

    @Test
    fun entdecken() {
        for (seed in 0 until 300) {
            val z = Random(seed)
            var f = VorschulGenerator.eine(VorschulArt.LAGE, 2, z)
            val stelle = listOf("ganz links", "in der Mitte", "ganz rechts").indexOfFirst { f.text.de.contains(it) }
            assertEquals(f.zeige[stelle], f.optionen[f.richtig])

            f = VorschulGenerator.eine(VorschulArt.GEFUEHLE, 2, z)
            val wort = Regex("Wer ist (.+)\\?").find(f.text.de)!!.groupValues[1]
            assertEquals(VorschulGenerator.gefuehle.first { it[1] == wort }[0], f.optionen[f.richtig])

            f = VorschulGenerator.eine(VorschulArt.TIERWELT, 2, z)
            assertEquals(VorschulGenerator.lebensraeume.first { it.first == f.gross }.second, f.optionen[f.richtig])

            f = VorschulGenerator.eine(VorschulArt.REIHENFOLGE, 2, z)
            assertEquals(VorschulGenerator.ablaeufe.first { it.first == f.folge }.second, f.optionen[f.richtig])
        }
    }

    @Test
    fun anderesBildIstEinMalDa() {
        val z = Random(7)
        repeat(100) {
            val f = VorschulGenerator.eine(VorschulArt.UNTERSCHIED, 2, z)
            assertEquals(4, f.optionen.size)
        }
    }

    @Test
    fun rundeHatAchtVerschiedeneFragen() {
        for (seed in 0 until 50) {
            for (art in listOf(VorschulArt.ZAHLEN, VorschulArt.FORMEN, VorschulArt.MIX, VorschulArt.GEFUEHLE)) {
                val fragen = VorschulGenerator.fragen(art, 2, 8, Random(seed))
                assertEquals(8, fragen.size)
            }
        }
    }

    @Test
    fun mischenNimmtAlleArtenAberKeinMemory() {
        val z = Random(1)
        val texte = mutableSetOf<String>()
        repeat(200) { VorschulGenerator.fragen(VorschulArt.MIX, 2, 8, z).forEach { texte.add(it.text.de) } }
        assertTrue(texte.size > 40)
    }
}

class VorschulStandTest {
    private fun frage(richtig: Int) = VorschulFrage(
        text = de.cemaras.yem1n.logik.VorschulText("Test"), optionen = listOf("a", "b", "c"), richtig = richtig,
    )

    private fun stand() = VorschulStand(listOf(frage(1), frage(2), frage(0)))

    @Test
    fun ersterVersuchRichtigZaehlt() {
        val s = stand().wahl(1)
        assertEquals(1, s.perfekt)
        assertEquals(1, s.serie)
        assertTrue(s.ersterVersuch)
        assertEquals(1, s.richtigGewaehlt)
    }

    @Test
    fun nachFehlerZaehltNichtAlsPerfekt() {
        var s = stand().wahl(0)
        assertEquals(setOf(0), s.falsch)
        assertEquals(0, s.serie)
        s = s.wahl(1)
        assertEquals(0, s.perfekt)
        assertFalse(s.ersterVersuch)
    }

    @Test
    fun gleicheFalscheAntwortUndTippenNachTrefferZaehlenNicht() {
        var s = stand().wahl(0).wahl(0)
        assertEquals(setOf(0), s.falsch)
        s = s.wahl(1).wahl(2)
        assertEquals(1, s.richtigGewaehlt)
        assertEquals(0, s.perfekt)
    }

    @Test
    fun weiterNurNachTreffer() {
        val s = stand()
        assertEquals(0, s.weiter().nr)
        assertEquals(1, s.wahl(1).weiter().nr)
        assertNull(s.wahl(1).weiter().richtigGewaehlt)
    }

    @Test
    fun serieBrichtBeiFehlerAb() {
        var s = stand().wahl(1).weiter().wahl(2).weiter()
        assertEquals(2, s.serie)
        s = s.wahl(1).wahl(0)
        assertEquals(0, s.serie)
        assertEquals(2, s.perfekt)
    }

    @Test
    fun rundeEndetUndGibtSterne() {
        var s = stand()
        s = s.wahl(1).weiter().wahl(2).weiter().wahl(0).weiter()
        assertTrue(s.fertig)
        assertNull(s.aktuelle)
        assertEquals(3, s.sterne)
        assertEquals(listOf(Stand.RICHTIG, Stand.RICHTIG, Stand.RICHTIG), s.staende())
    }

    @Test
    fun staendeFuerGemischteRunde() {
        var s = stand().wahl(1).weiter().wahl(0).wahl(2).weiter().wahl(0).weiter()
        assertEquals(listOf(Stand.RICHTIG, Stand.RICHTIG, Stand.FALSCH), s.staende())
    }
}

class MemoryTest {
    private fun stand() = MemoryStand(
        listOf(
            de.cemaras.yem1n.logik.MemoryKarte(0, "🍎"), de.cemaras.yem1n.logik.MemoryKarte(1, "⭐"),
            de.cemaras.yem1n.logik.MemoryKarte(0, "🍎"), de.cemaras.yem1n.logik.MemoryKarte(1, "⭐"),
        ),
    )

    @Test
    fun neuHatVierPaare() {
        val s = MemoryStand.neu(Random(3))
        assertEquals(8, s.karten.size)
        for (p in 0 until 4) assertEquals(2, s.karten.count { it.paar == p })
        assertEquals(4, s.karten.map { it.emoji }.toSet().size)
        assertFalse(s.fertig)
    }

    @Test
    fun gleichesPaarVerschwindet() {
        val s = stand().tippe(0).tippe(2)
        assertTrue(s.karten[0].weg && s.karten[2].weg)
        assertEquals(1, s.zuege)
        assertFalse(s.sperre)
    }

    @Test
    fun falschesPaarBleibtOffenBisZudecken() {
        var s = stand().tippe(0).tippe(1)
        assertTrue(s.sperre)
        assertTrue(s.karten[0].offen && s.karten[1].offen)
        // Während der Sperre zählt kein Tippen
        assertEquals(s, s.tippe(2))
        s = s.zudecken()
        assertFalse(s.sperre)
        assertTrue(s.karten.none { it.offen })
        assertEquals(1, s.zuege)
    }

    @Test
    fun dieselbeKarteZweimalTippenZaehltNicht() {
        val s = stand().tippe(0)
        assertEquals(s, s.tippe(0))
    }

    @Test
    fun fertigWennAllesWeg() {
        var s = stand().tippe(0).tippe(2).tippe(1).tippe(3)
        assertTrue(s.fertig)
        assertEquals(2, s.zuege)
        assertEquals(2, s.gesamt)
    }

    @Test
    fun fehlzuegeSenkenDieWertung() {
        var s = stand().tippe(0).tippe(1).zudecken().tippe(0).tippe(2).tippe(1).tippe(3)
        assertTrue(s.fertig)
        assertEquals(3, s.zuege)
        assertEquals(3, s.gesamt)
    }

    @Test
    fun arbeitsnamen() {
        assertEquals("🍎 Zählen", VorschulArt.ZAEHLEN.uebungName)
        assertNotNull(VorschulArt.stationen.find { it == VorschulArt.MEMORY })
        assertFalse(VorschulArt.stationen.contains(VorschulArt.MIX))
        assertEquals(VorschulArt.entries.size - 1, VorschulArt.stationen.size)
    }
}
