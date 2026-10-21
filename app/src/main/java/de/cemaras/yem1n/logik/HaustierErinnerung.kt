package de.cemaras.yem1n.logik

import java.util.Calendar
import java.util.TimeZone

/**
 * Eine einzige Erinnerung am späten Nachmittag, und nur dann, wenn das Tier bis dahin hungrig wäre.
 * Wer vorher übt, hört nichts davon (die Erinnerung wird nach jeder Runde neu geplant). Wie bei iOS.
 */
object HaustierErinnerung {
    const val UHRZEIT = 17
    const val MAX_STUNDEN = 72.0

    /** Wann die Erinnerung kommen soll, in Millisekunden, und zwar immer um [UHRZEIT] Uhr Ortszeit. */
    fun zeitpunkt(h: Haustier, jetzt: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val frisch = h.aktualisiert(jetzt)
        val stunden = if (frisch.stufe == HaustierStufe.EI) 20.0
        else maxOf(0.0, (frisch.satt - Haustier.HUNGRIG_GRENZE) / Haustier.SATT_PRO_STUNDE)
        val ziel = jetzt + (minOf(stunden, MAX_STUNDEN) * 3_600_000.0).toLong()
        val k = Calendar.getInstance(zone)
        k.timeInMillis = ziel
        k.set(Calendar.HOUR_OF_DAY, UHRZEIT)
        k.set(Calendar.MINUTE, 0)
        k.set(Calendar.SECOND, 0)
        k.set(Calendar.MILLISECOND, 0)
        if (k.timeInMillis <= ziel) k.add(Calendar.DAY_OF_MONTH, 1)
        return k.timeInMillis
    }

    fun titel(h: Haustier): String =
        if (h.stufe == HaustierStufe.EI) "🥚 Dein Ei wartet" else "${h.emoji} ${h.name} hat Hunger"

    fun text(h: Haustier): String =
        if (h.stufe == HaustierStufe.EI) "Löse eine Runde, damit es schlüpft." else "Eine Übungsrunde macht ${h.name} wieder satt."
}
