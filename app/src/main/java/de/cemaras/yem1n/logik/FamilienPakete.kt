package de.cemaras.yem1n.logik

import de.cemaras.yem1n.daten.PaketLeser

/** Pakete der Familie: Eltern legen Arbeiten in das Familien-Fach, die Kind-Geräte holen sie von selbst. */
object FamilienPakete {
    /** Ein Paket aus dem Familien-Fach, schon gelesen. [json] ist der Text der Arbeit. */
    data class Kopf(val id: String, val erstellt: Long, val schluessel: String, val titel: String, val klasse: String, val fach: String, val json: String)

    /** Liest die Einträge vom Typ `paket`. Kaputte Einträge werden übersprungen, die ältesten kommen zuerst. */
    fun lesen(eintraege: List<FamilienEintrag>): List<Kopf> = eintraege
        .filter { it.typ == FamilienProtokoll.TYP_PAKET }
        .mapNotNull { e ->
            val json = e.text("json")
            val a = PaketLeser.lese(json).arbeit ?: return@mapNotNull null
            Kopf(e.id, e.erstellt, a.schluessel, a.titel, a.klasse, a.fach, json)
        }
        .sortedBy { it.erstellt }

    /**
     * Was ein Kind-Gerät neu einfügen soll: nur Arbeiten, die es noch nicht hat. Vorhandene bleiben mit Fortschritt
     * unverändert (wie bei iOS), und gelöschte (versteckte) Arbeiten kommen nicht von selbst zurück.
     */
    fun neue(pakete: List<Kopf>, vorhanden: Set<String>, versteckt: Set<String>): List<Kopf> =
        pakete.filter { it.schluessel !in vorhanden && it.schluessel !in versteckt }
            .distinctBy { it.schluessel }

    /** Gibt einen Fehler für die Eltern zurück, wenn die Arbeit zu groß für das Familien-Fach ist, sonst null. */
    fun pruefeGroesse(json: String): String? =
        if (json.length > FamilienProtokoll.MAX_PAKET) "Die Arbeit ist zu groß für die Familie. Bitte in zwei kleinere Arbeiten aufteilen." else null
}
