package de.cemaras.yem1n.logik

import java.util.Calendar
import java.util.TimeZone

/** Tägliche Erinnerung ans Üben zu einer festen Uhrzeit. Wer heute schon geübt hat, wird erst morgen erinnert. */
object LernzeitErinnerung {
    const val STANDARD_MINUTEN = 16 * 60

    /** Wann die nächste Erinnerung kommen soll, in Millisekunden. [minuten] ist die Uhrzeit seit Mitternacht. */
    fun zeitpunkt(
        jetzt: Long, minuten: Int, heuteGeuebt: Boolean, zone: TimeZone = TimeZone.getDefault(),
    ): Long {
        val m = minuten.coerceIn(0, 24 * 60 - 1)
        val k = Calendar.getInstance(zone)
        k.timeInMillis = jetzt
        k.set(Calendar.HOUR_OF_DAY, m / 60)
        k.set(Calendar.MINUTE, m % 60)
        k.set(Calendar.SECOND, 0)
        k.set(Calendar.MILLISECOND, 0)
        if (k.timeInMillis <= jetzt || heuteGeuebt) {
            if (k.timeInMillis <= jetzt || heuteGeuebt) k.add(Calendar.DAY_OF_MONTH, 1)
        }
        return k.timeInMillis
    }

    fun uhrzeitText(minuten: Int): String = "%02d:%02d".format(minuten / 60, minuten % 60)

    const val TITEL = "📚 Zeit zum Üben"
    const val TEXT = "Eine kurze Runde reicht, dann ist der Tag geschafft."
}
