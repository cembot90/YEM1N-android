package de.cemaras.yem1n.logik

/** Zusammenfassung der Rundenergebnisse für die Eltern. Reines Kotlin, ohne Android. */
data class ErgebnisUebersicht(
    val runden: Int,
    val richtig: Int,
    val gesamt: Int,
    val sterne: Int,
    /** Wie viele Runden je Kind, größte Zahl zuerst. */
    val proKind: List<Pair<String, Int>>,
) {
    /** Anteil richtiger Antworten in Prozent, 0 ohne Aufgaben. */
    val prozent: Int get() = if (gesamt > 0) Math.round(richtig * 100f / gesamt) else 0

    companion object {
        fun von(liste: List<RundenErgebnisEintrag>): ErgebnisUebersicht {
            val proKind = liste.groupingBy { it.kind.ifBlank { "Kind" } }.eachCount()
                .toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            return ErgebnisUebersicht(
                runden = liste.size,
                richtig = liste.sumOf { it.richtig },
                gesamt = liste.sumOf { it.gesamt },
                sterne = liste.sumOf { it.sterne },
                proKind = proKind,
            )
        }

        /** Nur die Runden eines Kindes, null heißt alle. */
        fun fuerKind(liste: List<RundenErgebnisEintrag>, kind: String?): List<RundenErgebnisEintrag> =
            if (kind == null) liste else liste.filter { it.kind.ifBlank { "Kind" } == kind }
    }
}
