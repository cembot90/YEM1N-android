package de.cemaras.yem1n.logik

/** Was beim Üben vorgelesen wird. Reines Kotlin, gleiche Regeln wie `VorleseText.swift` bei iOS. */
object VorleseText {
    /** Aufgaben mit Lautsprecher. Bei Rechtschreibung würde das Vorlesen die Lösung verraten, eine Zahlenmauer lässt sich nicht sinnvoll vorlesen. */
    fun vorlesbar(art: String): Boolean = art != "text" && art != "mauer"

    private fun istBild(cp: Int): Boolean =
        cp == 0xFE0F || cp == 0x200D ||
            cp in 0x1F000..0x1FAFF || cp in 0x2600..0x27BF || cp in 0x2B00..0x2BFF || cp in 0x25A0..0x25FF

    /** Der Text so, wie er gesprochen wird: ohne Bilder und Emojis, ohne doppelte Leerzeichen und Zeilenumbrüche. */
    fun gesprochen(text: String): String {
        val sb = StringBuilder()
        text.codePoints().forEach { cp -> if (!istBild(cp)) sb.appendCodePoint(cp) }
        return sb.toString().split(' ', '\n', '\t').filter { it.isNotEmpty() }.joinToString(" ").trim()
    }

    /** Frage und Rechnung, die Zeichen als Wörter. */
    fun aufgabe(frage: String, rechnung: String): String {
        var t = "$frage. $rechnung"
        val tausch = listOf(
            "\n" to ". ", " · " to " mal ", " : " to " geteilt durch ", " - " to " minus ", " + " to " plus ",
            "=" to " gleich ", " ? " to " und ", "<" to " kleiner ", ">" to " größer ",
        )
        for ((alt, neu) in tausch) t = t.replace(alt, neu)
        return gesprochen(t)
    }
}
