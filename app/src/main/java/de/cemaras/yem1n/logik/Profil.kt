package de.cemaras.yem1n.logik

/** Einrichtung beim ersten Start. Reines Kotlin, gleiche Stufen wie `ProfilDaten` bei iOS. */
object Profil {
    val klassen = listOf("Vorschule", "Klasse 1", "Klasse 2", "Klasse 3", "Klasse 4")

    /** 0 heißt keine Klasse oder Vorschule, sonst 1 bis 4. */
    fun lernKlasse(name: String): Int = if (name.startsWith("Klasse ")) name.removePrefix("Klasse ").toIntOrNull()?.coerceIn(1, 4) ?: 0 else 0

    /** Zeigt der erste Start die Einrichtung? Nicht, wenn schon geübt wurde oder ein Name da ist (ältere Installation). */
    fun einrichtungNoetig(f: Fortschritt): Boolean = !f.profilFertig && f.name.isBlank() && f.runden.isEmpty()

    /** Übernimmt die Antworten des Assistenten in den Fortschritt. */
    fun uebernehmen(
        f: Fortschritt, rolle: String, name: String, klasse: String, sprachen: Boolean,
        rosa: Boolean, tagesziel: Int, autoVorlesen: Boolean,
    ): Fortschritt {
        if (rolle == "eltern") return f.copy(jokerRolle = "eltern", profilFertig = true)
        return f.copy(
            jokerRolle = "kind",
            name = name.trim().take(30),
            lernKlasse = lernKlasse(klasse),
            vorschulAn = if (klasse == "Vorschule") true else if (klasse.isNotEmpty()) false else f.vorschulAn,
            sprachenAn = sprachen,
            rosa = rosa,
            tagesziel = tagesziel.coerceIn(1, 5),
            autoVorlesen = autoVorlesen,
            profilFertig = true,
        )
    }
}
