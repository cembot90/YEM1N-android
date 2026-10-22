package de.cemaras.yem1n.logik

import java.util.Calendar
import java.util.TimeZone

/** Sonntags um 18 Uhr erinnert die App die Eltern an den Wochenbericht. */
object WochenberichtErinnerung {
    const val STUNDE = 18
    const val TITEL = "📊 Wochenbericht"
    const val TEXT = "Schau nach, wie die Woche für dein Kind gelaufen ist."

    /** Der nächste Sonntag um 18 Uhr, auch heute, wenn es noch davor ist. */
    fun zeitpunkt(jetzt: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val k = Calendar.getInstance(zone)
        k.timeInMillis = jetzt
        k.set(Calendar.HOUR_OF_DAY, STUNDE)
        k.set(Calendar.MINUTE, 0)
        k.set(Calendar.SECOND, 0)
        k.set(Calendar.MILLISECOND, 0)
        while (k.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || k.timeInMillis <= jetzt) {
            k.add(Calendar.DAY_OF_MONTH, 1)
        }
        return k.timeInMillis
    }
}
