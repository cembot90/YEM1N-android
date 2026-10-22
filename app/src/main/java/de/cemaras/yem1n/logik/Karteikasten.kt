package de.cemaras.yem1n.logik

import kotlinx.serialization.Serializable

/** Stand eines Wortes im Karteikasten: Fach b (0 bis 3), nächste Wiederholung, richtig und falsch gezählt. */
@Serializable
data class WortEintrag(val b: Int = 0, val n: Long = 0, val r: Int = 0, val f: Int = 0)

/** Eine geübte Runde in Sprachen, für den Bericht. */
@Serializable
data class SprachRunde(val ts: Long, val sp: String, val thema: String, val art: String, val richtig: Int, val gesamt: Int)

/** Wie bei iOS: richtig steigt ein Fach, falsch geht zurück auf 0. Fach 2 und höher gilt als gelernt. */
object Karteikasten {
    private const val TAG = 86_400_000L
    val dauer = longArrayOf(0, TAG, 3 * TAG, 7 * TAG)
    const val MAX_VERLAUF = 100

    fun schluessel(sprache: String, thema: String, de: String) = "$sprache|$thema|$de"

    fun bewerte(stand: Map<String, WortEintrag>, key: String, richtig: Boolean, jetzt: Long): Map<String, WortEintrag> {
        val e = stand[key] ?: WortEintrag()
        val neu = if (richtig) e.copy(b = minOf(e.b + 1, 3), r = e.r + 1) else e.copy(b = 0, f = e.f + 1)
        return stand + (key to neu.copy(n = jetzt + dauer[neu.b]))
    }

    /** "Kann ich schon": mindestens Fach 1. */
    fun kannIch(stand: Map<String, WortEintrag>, key: String, jetzt: Long): Map<String, WortEintrag> {
        val e = stand[key] ?: WortEintrag()
        val b = maxOf(e.b, 1)
        return stand + (key to e.copy(b = b, n = jetzt + dauer[b]))
    }

    fun gelernt(e: WortEintrag?) = (e?.b ?: 0) >= 2
    fun gesehen(e: WortEintrag?) = e != null && (e.r + e.f > 0 || e.b > 0)
    fun faellig(e: WortEintrag?, jetzt: Long) = gesehen(e) && e!!.n <= jetzt

    fun neuerVerlauf(alt: List<SprachRunde>, runde: SprachRunde): List<SprachRunde> = (alt + runde).takeLast(MAX_VERLAUF)
}
