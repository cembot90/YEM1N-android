package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Aufgabe

enum class Stand { OFFEN, RICHTIG, FALSCH, ANGESCHAUT }

/**
 * Der Zustand einer Übungsrunde. Unveränderlich: jede Aktion liefert einen neuen Zustand,
 * die Oberfläche zeigt immer den aktuellen.
 */
data class RundenStand(
    val aufgaben: List<Aufgabe>,
    val index: Int = 0,
    val staende: List<Stand> = List(aufgaben.size) { Stand.OFFEN },
    val gegeben: List<String> = List(aufgaben.size) { "" },
    val mauerWerte: List<List<Int?>> = emptyList(),
    val mauerSchritt: Int = 0,
    val erklaerung: String = "",
    val kurzerHinweis: String = "",
) {
    val aktuell: Aufgabe? get() = aufgaben.getOrNull(index)
    val fertig: Boolean get() = index >= aufgaben.size
    val bewertet: Boolean get() = staende.getOrNull(index)?.let { it != Stand.OFFEN } ?: false
    val richtig: Int get() = staende.count { it == Stand.RICHTIG }
    val falsch: Int get() = staende.count { it == Stand.FALSCH }
    val angeschaut: Int get() = staende.count { it == Stand.ANGESCHAUT }
    val sterne: Int get() = Pruefer.sterneFuer(richtig, aufgaben.size)

    /** So viele Aufgaben hintereinander waren richtig, die aktuelle mitgezählt. */
    val serie: Int
        get() {
            var n = 0
            var i = minOf(index, staende.size - 1)
            while (i >= 0 && staende[i] == Stand.RICHTIG) { n++; i-- }
            return n
        }
}

object Runde {

    fun start(aufgaben: List<Aufgabe>): RundenStand = vorbereitet(RundenStand(aufgaben))

    /** Stellt bei Zahlenmauern die Steine für die aktuelle Aufgabe auf. */
    private fun vorbereitet(s: RundenStand): RundenStand {
        val a = s.aktuell
        return if (a != null && a.istMauer) {
            s.copy(mauerWerte = Mauer.start(a.reihen), mauerSchritt = 0, kurzerHinweis = "")
        } else {
            s.copy(mauerWerte = emptyList(), mauerSchritt = 0, kurzerHinweis = "")
        }
    }

    /** Das Kind gibt eine Antwort ab. Unvollständige Eingaben ändern nichts. */
    fun antworte(s: RundenStand, eingabe: String, eingabe2: String = ""): RundenStand {
        val a = s.aktuell ?: return s
        if (s.bewertet) return s
        if (a.istMauer) return mauerAntwort(s, a, eingabe)
        val ok = Pruefer.pruefe(a, eingabe, eingabe2) ?: return s
        return bewertet(s, a, ok, a.erklaerung, Pruefer.gegebenText(a, eingabe, eingabe2))
    }

    private fun mauerAntwort(s: RundenStand, a: Aufgabe, eingabe: String): RundenStand {
        if (eingabe.isBlank() || !Mauer.gueltig(a.reihen)) return s
        val (r, c) = Mauer.position(a.reihen, s.mauerSchritt) ?: return s
        val soll = a.reihen[r][c]
        val neu = s.mauerWerte.mapIndexed { zeile, werte ->
            if (zeile == r) werte.mapIndexed { spalte, w -> if (spalte == c) soll else w } else werte
        }
        if (eingabe.trim().toIntOrNull() == soll) {
            val schritt = s.mauerSchritt + 1
            return if (schritt >= Mauer.anzahl(a.reihen)) {
                bewertet(
                    s.copy(mauerWerte = neu, mauerSchritt = schritt), a, true,
                    "Die Mauer stimmt komplett.", "alles richtig",
                )
            } else {
                s.copy(mauerWerte = neu, mauerSchritt = schritt, kurzerHinweis = "Richtig. Weiter nach oben.")
            }
        }
        val unten = a.reihen[r - 1]
        val rechnung = "${unten[c]} + ${unten[c + 1]} = $soll"
        return bewertet(
            s.copy(mauerWerte = neu), a, false,
            "Hier gehört $soll hin: $rechnung", eingabe.trim(),
        )
    }

    private fun bewertet(
        s: RundenStand, a: Aufgabe, ok: Boolean, erklaerung: String, gegeben: String,
    ): RundenStand {
        val text = erklaerung.ifEmpty {
            when (a.art) {
                Aufgabe.ART_ZAHL -> a.rechnung.replace("\n", " ") + " " + a.antwort
                Aufgabe.ART_TEXT -> "Richtig: ${a.antwort}"
                else -> ""
            }
        }
        return s.copy(
            staende = s.staende.set(s.index, if (ok) Stand.RICHTIG else Stand.FALSCH),
            gegeben = s.gegeben.set(s.index, gegeben),
            erklaerung = text,
            kurzerHinweis = "",
        )
    }

    /** "Lösung zeigen": zählt weder als richtig noch als falsch. */
    fun zeigeLoesung(s: RundenStand): RundenStand {
        val a = s.aktuell ?: return s
        if (s.bewertet) return s
        val text = when {
            a.istMauer -> "Die Mauer ist jetzt fertig ausgefüllt."
            a.erklaerung.isNotEmpty() -> a.erklaerung
            a.art == Aufgabe.ART_REST -> "${a.antwort} Rest ${a.antwort2}"
            else -> a.antwort
        }
        val werte = if (a.istMauer) a.reihen.map { r -> r.map<Int, Int?> { it } } else s.mauerWerte
        return s.copy(
            staende = s.staende.set(s.index, Stand.ANGESCHAUT),
            gegeben = s.gegeben.set(s.index, ""),
            mauerWerte = werte,
            erklaerung = text,
            kurzerHinweis = "",
        )
    }

    /** Nächste Aufgabe. Nach der letzten ist die Runde fertig. */
    fun weiter(s: RundenStand): RundenStand {
        if (!s.bewertet) return s
        return vorbereitet(s.copy(index = s.index + 1, erklaerung = ""))
    }

    private fun <T> List<T>.set(i: Int, wert: T): List<T> =
        mapIndexed { j, x -> if (j == i) wert else x }
}
