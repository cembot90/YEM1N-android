package de.cemaras.yem1n.logik

/**
 * Macht aus gelesener Handschrift eine Zahl. Reines Kotlin, gleiche Regeln wie `Handschrift` in der iOS-App.
 * Die Zahlentasten bleiben immer als Ausweg.
 */
object Handschrift {
    /** Zeichen, die beim Lesen von handgeschriebenen Ziffern oft herauskommen. Nur eindeutige Fälle. */
    private val ersatz: Map<Char, Char> = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0', '°' to '0',
        'l' to '1', 'I' to '1', 'i' to '1', '|' to '1', '!' to '1', '/' to '1', ']' to '1', '[' to '1',
        'Z' to '2', 'z' to '2',
        'S' to '5', 's' to '5',
        'b' to '6', 'G' to '6',
        'T' to '7',
        'B' to '8',
        'g' to '9', 'q' to '9',
    )

    /** Zeichen ohne Bedeutung, zum Beispiel ein Punkt, wenn der Stift kurz aufkommt. */
    private val ohneBedeutung: Set<Char> = setOf('.', ',', '\'', '`', '_', '-', '~', '´')

    /** Macht aus einem gelesenen Text eine Zahl. Null, wenn der Text keine Zahl ist. */
    fun zahl(gelesen: String, maxLaenge: Int): String? {
        val ziffern = StringBuilder()
        for (ch in gelesen) {
            if (ch.isWhitespace() || ch in ohneBedeutung) continue
            if (ch in '0'..'9') ziffern.append(ch)
            else ersatz[ch]?.let { ziffern.append(it) } ?: return null
        }
        var s = ziffern.toString()
        while (s.length > 1 && s.first() == '0') s = s.drop(1)
        return if (s.isEmpty() || s.length > maxLaenge) null else s
    }

    /** Nimmt die Vorschläge der Erkennung (bester zuerst) und gibt den ersten, der eine Zahl ist. */
    fun lies(vorschlaege: List<String>, maxLaenge: Int): String? =
        vorschlaege.firstNotNullOfOrNull { zahl(it, maxLaenge) }
}
