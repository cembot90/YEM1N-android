package de.cemaras.yem1n.logik

import kotlinx.serialization.Serializable

/**
 * Ein Fehler, der noch geübt werden muss. Tage sind fortlaufende Tagesnummern (siehe [Tag]).
 * Wer einen Fehler einmal richtig wiederholt, hat ihn noch nicht sicher: Er kommt nach 3 Tagen
 * noch einmal. Erst die zweite richtige Antwort macht ihn zu einem gemeisterten Fehler.
 * Eine falsche Antwort stellt ihn auf Anfang, er kommt dann schon morgen wieder.
 */
@Serializable
data class FehlerEintrag(val faelligTag: Long = 0, val stufe: Int = 0)

object Fehlerplan {
    /** So oft muss ein Fehler richtig wiederholt werden. */
    const val NOETIG = 2

    /** Tage bis zur nächsten Wiederholung nach einer richtigen Antwort. */
    val abstandNachRichtig = listOf(3, 7)
    const val ABSTAND_NACH_FEHLER = 1

    fun istFaellig(e: FehlerEintrag, heute: Long): Boolean = e.faelligTag <= heute

    /** Neuer Stand nach einer richtigen Wiederholung, oder null, wenn der Fehler damit gemeistert ist. */
    fun richtig(e: FehlerEintrag, heute: Long): FehlerEintrag? {
        val neu = e.stufe + 1
        if (neu >= NOETIG) return null
        val index = minOf(neu - 1, abstandNachRichtig.size - 1)
        return FehlerEintrag(heute + abstandNachRichtig[index], neu)
    }

    fun falsch(heute: Long): FehlerEintrag = FehlerEintrag(heute + ABSTAND_NACH_FEHLER, 0)
}

/** Fortlaufende Tagesnummer nach der Ortszeit des Geräts. */
object Tag {
    fun heute(
        jetzt: Long = System.currentTimeMillis(),
        offset: Int = java.util.TimeZone.getDefault().getOffset(jetzt),
    ): Long = Math.floorDiv(jetzt + offset, 86_400_000L)
}
