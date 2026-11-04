package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Aufgabe

/**
 * Nach dem ersten Fehler bekommt das Kind einen Hinweis und darf es noch einmal probieren.
 * Erst danach kommt die Lösung mit Erklärung. So schreibt das Kind die richtige Antwort selbst.
 * Gleich wie bei iOS (`ZweiterVersuch` in `Lob.swift`).
 */
object ZweiterVersuch {
    /**
     * Nicht in der Probearbeit (wie in der Schule), nicht bei der Zahlenmauer (sie zeigt die richtige Zahl sofort)
     * und nicht bei Auswahl mit weniger als vier Knöpfen oder beim Vergleichen (dann wäre der zweite Versuch nur Raten).
     */
    fun erlaubt(art: String, optionen: Int, probe: Boolean): Boolean = when {
        probe -> false
        art == Aufgabe.ART_MAUER || art == Aufgabe.ART_VERGLEICH -> false
        art == Aufgabe.ART_WAHL -> optionen >= 4
        else -> true
    }

    val saetze = listOf(
        "Fast! Schau noch einmal genau hin und probier es nochmal.",
        "Noch nicht ganz. Rechne in Ruhe noch einmal.",
        "Das passt noch nicht. Du hast noch einen Versuch.",
        "Kleiner Fehler. Lies die Aufgabe noch einmal und versuch es nochmal.",
        "Nicht schlimm! Probier es gleich noch einmal.",
        "Hmm, knapp daneben. Noch ein Versuch, du schaffst das.",
    )

    /** Der Satz nach dem ersten Fehler. Hat die Aufgabe einen eigenen Tipp, kommt er dazu. */
    fun text(eigenerTipp: String, nr: Int): String {
        val satz = saetze[Math.floorMod(nr, saetze.size)]
        val tipp = eigenerTipp.trim()
        return if (tipp.isEmpty()) satz else "$satz Tipp: $tipp"
    }
}
