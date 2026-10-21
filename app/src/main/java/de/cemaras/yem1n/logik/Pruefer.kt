package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Aufgabe

/** Prüft Antworten des Kindes. Reine Logik ohne Oberfläche, damit sie sich testen lässt. */
object Pruefer {

    /** Zahlen werden als Zahlen verglichen ("07" gleich "7"), alles andere als Text. */
    fun gleich(x: String, y: String): Boolean {
        val i = x.trim().toIntOrNull()
        val j = y.trim().toIntOrNull()
        return if (i != null && j != null) i == j else x.trim() == y.trim()
    }

    /** Textantworten: Leerzeichen am Rand und doppelte Leerzeichen zählen nicht, Groß und Klein schon. */
    fun normalisiert(t: String): String =
        t.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    /**
     * Prüft eine Antwort. Gibt null zurück, solange die Eingabe nicht vollständig ist
     * (leer, oder bei Aufgaben mit Rest fehlt ein Feld). Zahlenmauern laufen über [Runde].
     */
    fun pruefe(a: Aufgabe, eingabe: String, eingabe2: String = ""): Boolean? = when (a.art) {
        Aufgabe.ART_REST -> {
            if (eingabe.isBlank() || eingabe2.isBlank()) null
            else gleich(eingabe, a.antwort) && gleich(eingabe2, a.antwort2)
        }
        Aufgabe.ART_TEXT -> {
            val e = normalisiert(eingabe)
            if (e.isEmpty()) null else e == normalisiert(a.antwort)
        }
        Aufgabe.ART_WAHL, Aufgabe.ART_VERGLEICH -> {
            if (eingabe.isEmpty()) null else eingabe == a.antwort
        }
        else -> {
            if (eingabe.isBlank()) null else gleich(eingabe, a.antwort)
        }
    }

    /** Was das Kind eingegeben hat, lesbar für die Eltern. */
    fun gegebenText(a: Aufgabe, eingabe: String, eingabe2: String): String =
        if (a.art == Aufgabe.ART_REST) "$eingabe Rest $eingabe2" else normalisiert(eingabe)

    /** Sterne einer Übung: ab 90 Prozent 3, ab 70 Prozent 2, ab 50 Prozent 1. */
    fun sterneFuer(gut: Int, gesamt: Int): Int {
        if (gesamt <= 0) return 0
        val anteil = gut.toDouble() / gesamt
        return when {
            anteil >= 0.9 -> 3
            anteil >= 0.7 -> 2
            anteil >= 0.5 -> 1
            else -> 0
        }
    }
}

/** Zahlenmauern: die unterste Reihe ist gegeben, jede obere Reihe ist die Summe zweier Steine darunter. */
object Mauer {
    /** Reihe 0 ist unten, jede weitere Reihe hat einen Stein weniger, oben steht ein Stein. */
    fun gueltig(reihen: List<List<Int>>): Boolean =
        reihen.size >= 2 && reihen.indices.all { i -> reihen[i].size == reihen[0].size - i } &&
            reihen.last().size == 1

    /** Am Anfang steht nur die unterste Reihe, alles andere ist leer. */
    fun start(reihen: List<List<Int>>): List<List<Int?>> =
        reihen.mapIndexed { i, r -> if (i == 0) r.map { it } else List<Int?>(r.size) { null } }

    /** Anzahl der Steine, die das Kind ausfüllen muss. */
    fun anzahl(reihen: List<List<Int>>): Int = reihen.drop(1).sumOf { it.size }

    /** Der Stein für Schritt n: von unten nach oben, in jeder Reihe von links nach rechts. */
    fun position(reihen: List<List<Int>>, schritt: Int): Pair<Int, Int>? {
        var rest = schritt
        for (r in 1 until reihen.size) {
            if (rest < reihen[r].size) return r to rest
            rest -= reihen[r].size
        }
        return null
    }
}
