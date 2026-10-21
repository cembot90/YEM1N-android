package de.cemaras.yem1n.logik

import kotlin.math.abs

/** Münzen verdienen, wie in der iOS-App. */
object Muenzen {
    const val PRO_RICHTIG = 1
    const val BONUS_ZWEI_STERNE = 2
    const val BONUS_DREI_STERNE = 5

    /** Angeschaute Lösungen zählen nicht als richtig, Spicken bringt also nichts. */
    fun verdient(richtig: Int, gesamt: Int, angesehen: Int): Int {
        if (richtig <= 0 || gesamt <= 0) return 0
        var summe = richtig * PRO_RICHTIG
        if (angesehen == 0) {
            val sterne = Pruefer.sterneFuer(richtig, gesamt)
            if (sterne >= 3) summe += BONUS_DREI_STERNE else if (sterne == 2) summe += BONUS_ZWEI_STERNE
        }
        return summe
    }
}

/** Serie: Tage hintereinander mit mindestens einer Runde. */
object Serie {
    /** Ein einzelner verpasster Tag unterbricht die Serie nicht, aber nur einmal pro Woche. */
    const val FREIER_TAG_ABSTAND = 7

    /**
     * [laenge] zählt nur Tage, an denen geübt wurde. [gerettet] ist die Zahl der freien Tage,
     * die die Serie überbrückt haben. Heute ist noch offen und zählt nicht als verpasst.
     */
    fun mitSchutz(tage: Set<Long>, heute: Long): Pair<Int, Int> {
        var tag = heute
        if (tag !in tage) tag -= 1
        var laenge = 0
        var gerettet = 0
        var letzteLuecke: Long? = null
        var schutz = 0
        while (schutz < 4000) {
            schutz++
            if (tag in tage) {
                laenge++
                tag -= 1
                continue
            }
            val davor = tag - 1
            val frei = letzteLuecke?.let { abs(it - tag) >= FREIER_TAG_ABSTAND } ?: true
            if (davor !in tage || !frei) break
            gerettet++
            letzteLuecke = tag
            tag = davor
        }
        return laenge to gerettet
    }
}
