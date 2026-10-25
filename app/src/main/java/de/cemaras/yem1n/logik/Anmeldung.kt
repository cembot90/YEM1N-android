package de.cemaras.yem1n.logik

/**
 * Die Kinder einer Familie für die Elternansicht. Wer zur Familie gehört, sagt die Geräteliste des Familien-Dienstes:
 * jedes freigegebene Kinder-Gerät ist ein Kind, auch wenn es noch keine Runde gespielt hat.
 */
object Anmeldung {
    /** Kinder für die Auswahl: erst die mit Runden, dann die nur angemeldeten. */
    fun kinder(mitRunden: List<String>, angemeldet: List<String>): List<String> {
        val ergebnis = mitRunden.toMutableList()
        for (n in angemeldet) if (ergebnis.none { it.trim().lowercase() == n.trim().lowercase() }) ergebnis += n
        return ergebnis
    }

    /** Eine Zeile der Kinderliste für die Eltern. [runden] ist 0, wenn das Kind noch keine Runde gespielt hat. */
    data class KindZeile(val name: String, val runden: Int, val zuletzt: Long)

    private fun schluessel(name: String) = name.trim().lowercase().ifEmpty { "kind" }

    /** Kinder mit Zahl der Runden und letzter Aktivität, dazu Kinder-Geräte ohne Runde. Nach Name sortiert. */
    fun kinderZeilen(runden: List<RundenErgebnisEintrag>, angemeldet: List<String>): List<KindZeile> {
        val aus = LinkedHashMap<String, KindZeile>()
        for (e in runden.sortedBy { it.zeitpunkt }) {
            val k = schluessel(e.kind)
            val alt = aus[k]
            aus[k] = KindZeile(e.kind.trim().ifEmpty { "Kind" }, (alt?.runden ?: 0) + 1, maxOf(alt?.zuletzt ?: 0L, e.zeitpunkt))
        }
        for (name in angemeldet) {
            val k = schluessel(name)
            if (aus[k] == null) aus[k] = KindZeile(name.trim().ifEmpty { "Kind" }, 0, 0L)
        }
        return aus.values.sortedBy { it.name.lowercase() }
    }
}
