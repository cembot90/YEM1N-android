package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.Uebung
import kotlin.random.Random

/** Das Fehlerheft: falsch beantwortete Aufgaben kommen als eigene Runde wieder. */
object Fehlerheft {
    const val MAX_AUFGABEN = 15

    /** Eindeutiger Schlüssel einer Originalaufgabe. */
    fun schluessel(arbeitSchluessel: String, uebungTitel: String, index: Int) =
        "$arbeitSchluessel#$uebungTitel#$index"

    /** Nur Fächer mit Fehlerheft. Die Vorschule hat keins, dort gibt es kein richtig oder falsch. */
    private fun zaehlt(a: Arbeit) = a.fach != "Vorschule"

    /** Wie viele Fehler jetzt dran sind und wie viele erst in einigen Tagen wiederkommen. */
    fun stand(arbeiten: List<Arbeit>, fehler: Map<String, FehlerEintrag>, heute: Long): Pair<Int, Int> {
        var faellig = 0
        var wartend = 0
        for (arbeit in arbeiten.filter(::zaehlt)) {
            for (u in arbeit.uebungen) {
                for (i in u.aufgaben.indices) {
                    val e = fehler[schluessel(arbeit.schluessel, u.titel, i)] ?: continue
                    if (Fehlerplan.istFaellig(e, heute)) faellig++ else wartend++
                }
            }
        }
        return faellig to wartend
    }

    /** Bis zu 15 fällige Fehler als neue Runde, oder null, wenn gerade keiner dran ist. */
    fun baue(
        arbeiten: List<Arbeit>, fehler: Map<String, FehlerEintrag>, heute: Long,
        zufall: Random = Random.Default,
    ): Arbeit? {
        val faellige = mutableListOf<Pair<String, de.cemaras.yem1n.daten.Aufgabe>>()
        val faecher = linkedSetOf<String>()
        val auswahlFach = mutableMapOf<String, String>()
        for (arbeit in arbeiten.filter(::zaehlt)) {
            for (u in arbeit.uebungen) {
                u.aufgaben.forEachIndexed { i, a ->
                    val key = schluessel(arbeit.schluessel, u.titel, i)
                    val e = fehler[key]
                    if (e != null && Fehlerplan.istFaellig(e, heute)) {
                        faellige += key to a
                        auswahlFach[key] = arbeit.fach
                    }
                }
            }
        }
        if (faellige.isEmpty()) return null
        val auswahl = faellige.shuffled(zufall).take(MAX_AUFGABEN)
        auswahl.forEach { faecher += auswahlFach.getValue(it.first) }
        val fach = if (faecher.size == 1) faecher.first() else "Gemischt"
        val uebung = Uebung(
            titel = "Fehlerheft",
            gruppe = "Nochmal versuchen",
            symbol = "✎",
            tipp = "Schau dir die Erklärung genau an.",
            aufgaben = auswahl.map { (key, a) -> a.copy(quellKey = key) },
        )
        return Arbeit(klasse = arbeiten.first().klasse, fach = fach, titel = "Fehlerheft", uebungen = listOf(uebung))
    }
}
