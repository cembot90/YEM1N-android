package de.cemaras.yem1n.logik

/**
 * Meldet ein Kind-Gerät bei den Eltern an, ohne dass schon eine Runde gespielt wurde.
 * Es geht als Ergebnis ohne Aufgaben (gesamt 0) über den gleichen Weg wie Rundenergebnisse.
 * iOS zeigt das Kind damit in der Liste, zählt es aber nicht als Runde.
 */
object Anmeldung {
    const val UEBUNG = "Angemeldet"

    /** Ändert sich, wenn Code, Name oder Gerät wechseln. Dann wird neu angemeldet. */
    fun schluessel(code: String, name: String, geraet: String): String =
        "$code|${name.trim().lowercase()}|$geraet"

    fun eintrag(code: String, name: String, geraet: String, jetzt: Long): RundenErgebnisEintrag {
        val kurz = Integer.toHexString(schluessel(code, name, geraet).hashCode())
        return RundenErgebnisEintrag(
            eintragId = "anmeldung-${geraet.take(36)}-$kurz",
            klasse = "", fach = "", arbeit = "", uebung = UEBUNG,
            richtig = 0, gesamt = 0, angesehen = 0, sterne = 0,
            kind = name.trim().take(40), zeitpunkt = jetzt,
        )
    }

    /** Ob ein Eintrag nur die Anmeldung ist und keine gespielte Runde. */
    fun istAnmeldung(e: RundenErgebnisEintrag): Boolean = e.gesamt == 0 && e.uebung == UEBUNG

    /** Die Namen aller Kinder, die sich angemeldet haben, ohne doppelte Schreibweisen (Groß und Klein, Leerzeichen). */
    fun namen(liste: List<RundenErgebnisEintrag>): List<String> =
        liste.filter { istAnmeldung(it) }.sortedByDescending { it.zeitpunkt }
            .map { it.kind.trim() }.filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

    /** Kinder für die Auswahl: erst die mit Runden, dann die nur angemeldeten. */
    fun kinder(mitRunden: List<String>, angemeldet: List<String>): List<String> {
        val ergebnis = mitRunden.toMutableList()
        for (n in angemeldet) if (ergebnis.none { it.trim().lowercase() == n.trim().lowercase() }) ergebnis += n
        return ergebnis
    }

    /** Eine Zeile der Kinderliste für die Eltern. [runden] ist 0, wenn sich das Kind nur angemeldet hat. */
    data class KindZeile(val name: String, val runden: Int, val zuletzt: Long)

    private fun schluessel(name: String) = name.trim().lowercase().ifEmpty { "kind" }

    /** Kinder mit Zahl der Runden und letzter Aktivität, dazu Kinder, die sich nur angemeldet haben. Nach Name sortiert. */
    fun kinderZeilen(runden: List<RundenErgebnisEintrag>, anmeldungen: List<RundenErgebnisEintrag>): List<KindZeile> {
        val aus = LinkedHashMap<String, KindZeile>()
        for (e in runden.sortedBy { it.zeitpunkt }) {
            val k = schluessel(e.kind)
            val alt = aus[k]
            aus[k] = KindZeile(e.kind.trim().ifEmpty { "Kind" }, (alt?.runden ?: 0) + 1, maxOf(alt?.zuletzt ?: 0L, e.zeitpunkt))
        }
        for (a in anmeldungen.sortedBy { it.zeitpunkt }) {
            val k = schluessel(a.kind)
            val alt = aus[k]
            if (alt == null) aus[k] = KindZeile(a.kind.trim().ifEmpty { "Kind" }, 0, a.zeitpunkt)
        }
        return aus.values.sortedBy { it.name.lowercase() }
    }
}
