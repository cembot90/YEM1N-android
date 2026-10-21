package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.daten.Uebung
import kotlin.random.Random

/** Macht aus den Aufgaben eines eigenen Vorschul-Pakets Fragen für eine Runde (wie `VorschulPaket` bei iOS). */
object VorschulPaket {
    /** Nur Aufgaben der Art "wahl" mit gültigen Antwortknöpfen zählen. Die Reihenfolge wird gemischt. */
    fun fragen(u: Uebung, zufall: Random = Random.Default): List<VorschulFrage> {
        val liste = ArrayList<VorschulFrage>()
        for (a in u.aufgaben) {
            if (a.art != Aufgabe.ART_WAHL) continue
            val opt = a.optionen
            if (opt.size < 2 || a.antwort !in opt) continue
            val gemischt = opt.shuffled(zufall)
            val richtig = gemischt.indexOf(a.antwort)
            val tuerkisch = a.tr.ifEmpty { a.hinweis }.ifEmpty { null }
            val teile = a.rechnung.split("+")
            val plus = teile.size == 2
            liste.add(
                VorschulFrage(
                    text = VorschulText(de = a.frage, tr = tuerkisch),
                    optionen = gemischt,
                    richtig = richtig,
                    zeige = if (plus) zeichen(teile[0]) else zeichen(a.rechnung),
                    zeigeZwei = if (plus) zeichen(teile[1]) else emptyList(),
                    plus = plus,
                    gross = a.gross.ifEmpty { a.erklaerung },
                    folge = a.folge.ifEmpty { a.antwort2.split(" ").filter { it.isNotEmpty() } },
                    gruppenOptionen = opt.any { it.length > 2 },
                ),
            )
        }
        return liste.shuffled(zufall)
    }

    /** Jedes sichtbare Zeichen einzeln, auch ein Emoji aus zwei Code-Einheiten bleibt ganz. */
    private fun zeichen(t: String): List<String> {
        val aus = ArrayList<String>()
        var i = 0
        val s = t.filter { !it.isWhitespace() }
        while (i < s.length) {
            val n = Character.charCount(s.codePointAt(i))
            aus.add(s.substring(i, i + n))
            i += n
        }
        return aus
    }
}
