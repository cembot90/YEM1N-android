package de.cemaras.yem1n.logik

/** Aufträge und Prüfung für die KI auf dem Gerät. Die KI formuliert nur um, die Zahlen kommen aus dem Code. */
object KiPrompt {
    fun anweisung(grundBekannt: Boolean): String {
        var a = "Du bist eine freundliche Eule, die einem Kind der 3. Klasse in Deutschland beim Lernen hilft. " +
            "Formuliere die Erklärung neu, damit das Kind sie gut versteht: höchstens drei kurze Sätze, " +
            "einfache Wörter, ermutigend, in der Du-Form. Ändere keine Zahl und erfinde keine neuen Zahlen. " +
            "Benutze für mal das Zeichen · und für geteilt das Zeichen :. " +
            "Kein Markdown, keine Aufzählung, keine Gedankenstriche. Schreibe nur den Text."
        if (!grundBekannt) a += " Nenne keinen Grund für den Fehler, erkläre nur den richtigen Weg."
        return a
    }

    fun fakten(rechnung: String, gegeben: String, loesung: String, ersatz: String): String =
        "Aufgabe: ${rechnung.replace("\n", " ")}\nDas Kind hat geschrieben: $gegeben\nRichtig ist: $loesung\nErklärung: $ersatz"

    fun prompt(anweisung: String, fakten: String): String = "$anweisung\n\n$fakten"

    private val zahl = Regex("\\d+")

    /**
     * Nimmt den Text der KI nur an, wenn er brauchbar ist: nicht leer, nicht zu lang und ohne neue Zahlen.
     * Sonst gilt der feste Satz [ersatz], der genauso stimmt.
     */
    fun pruefe(roh: String?, erlaubteTexte: List<String>, ersatz: String, maxZeichen: Int = 320): Pair<String, Boolean> {
        var t = roh.orEmpty().trim()
            .replace("**", "").replace("__", "").replace("`", "")
            .replace('–', '-').replace('—', '-')
            .replace(Regex("\\s*\\n+\\s*"), " ")
            .trim()
        if (t.isEmpty() || t.length > maxZeichen) return ersatz to false
        val erlaubt = erlaubteTexte.flatMap { zahl.findAll(it).map { m -> m.value.trimStart('0').ifEmpty { "0" } }.toList() }.toSet()
        val gesagt = zahl.findAll(t).map { it.value.trimStart('0').ifEmpty { "0" } }.toList()
        if (gesagt.any { it !in erlaubt }) return ersatz to false
        return t to true
    }
}
