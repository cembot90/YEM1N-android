package de.cemaras.yem1n.logik

/** Joker pro Tag und Nachschub, gleiche Regeln wie in der iOS-App (3 Joker am Tag, Eltern füllen wieder auf). */
object JokerNachschub {
    const val LIMIT_STANDARD = 3

    /** Wie viele Joker heute noch übrig sind. */
    fun uebrig(limit: Int, tag: Long, genutzt: Int, heute: Long): Int =
        if (tag != heute) limit.coerceAtLeast(0) else (limit - genutzt).coerceAtLeast(0)

    /** Name des Kindes, dessen Anfrage noch offen ist (neuer als die letzte Freigabe), sonst null. */
    fun offeneAnfrage(
        nachschub: List<JokerCloud.Datensatz>,
        freigaben: List<JokerCloud.Datensatz>,
    ): String? {
        val letzte = freigaben.maxOfOrNull { it.erstellt } ?: 0L
        return nachschub.filter { it.erstellt > letzte }.maxByOrNull { it.erstellt }?.text("kind", "Kind")
    }

    /**
     * Freigaben, die dieses Gerät noch nicht angewendet hat. Nur Freigaben, die nach dem ersten Abgleich
     * entstanden sind, damit alte Freigaben auf einem frisch eingerichteten Gerät nichts auffüllen.
     */
    fun neueFreigaben(freigaben: List<JokerCloud.Datensatz>, benutzt: List<String>, ab: Long): List<String> =
        freigaben.filter { it.erstellt > ab && it.name !in benutzt }.map { it.name }
}
