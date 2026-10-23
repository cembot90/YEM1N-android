package de.cemaras.yem1n.logik

import de.cemaras.yem1n.logik.lehrplan.LehrplanEnglisch
import de.cemaras.yem1n.logik.lehrplan.LehrplanSprachen
import de.cemaras.yem1n.logik.lehrplan.Vokabel
import kotlin.random.Random

class SprachInfo(val code: String, val name: String, val flagge: String)

/** Der Bereich Sprachen: Karten zum Umdrehen, Paare finden, Fragen (aus der Lernwelt). */
object Sprachen {
    val alle = listOf(
        SprachInfo("en", "Englisch", "🇬🇧"),
        SprachInfo("tr", "Türkisch", "🇹🇷"),
        SprachInfo("it", "Italienisch", "🇮🇹"),
    )

    fun info(code: String): SprachInfo = alle.firstOrNull { it.code == code } ?: alle[0]

    val themen: List<LehrplanSprachen.Thema>
        get() = LehrplanSprachen.themenListe +
            EigeneThemen.liste.map { LehrplanSprachen.Thema(it.id, it.titel, it.emoji, it.klasse) }

    /** Alle Wörter eines Themas, die es in der Sprache gibt. */
    fun woerter(katalog: String, sprache: String): List<Vokabel> =
        (EigeneThemen.woerter(katalog) ?: LehrplanEnglisch.woerter[katalog].orEmpty()).filter { it.text(sprache).isNotEmpty() }

    /** Das Thema der Lernwelt zu einem Katalog-Thema, für den Knopf "Fragen". */
    fun lernThema(katalog: String, sprache: String): LernThema? {
        val id = if (sprache == "en") LehrplanEnglisch.katalogZuThemaId[katalog] else "$sprache-$katalog"
        return Lernwelt.themen.firstOrNull { it.id == id }
    }

    class Paar(val de: String, val fremd: String, val emoji: String)

    /** Bis zu [anzahl] Paare, deren deutsche und fremde Wörter jeweils nur einmal vorkommen. */
    fun paare(liste: List<Vokabel>, sprache: String, anzahl: Int = 6, zufall: Random = Random.Default): List<Paar> {
        val aus = mutableListOf<Paar>()
        val de = mutableSetOf<String>()
        val fremd = mutableSetOf<String>()
        for (w in liste.shuffled(zufall)) {
            val f = w.text(sprache)
            if (w.de in de || f in fremd) continue
            de += w.de; fremd += f
            aus += Paar(w.de, f, w.emoji)
            if (aus.size >= anzahl) break
        }
        return aus
    }
}
