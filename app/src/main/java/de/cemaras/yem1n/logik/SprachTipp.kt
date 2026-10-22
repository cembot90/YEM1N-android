package de.cemaras.yem1n.logik

import java.util.Locale

enum class Tipp { RICHTIG, FAST, FALSCH }

/** Schreibweise prüfen beim Tippen, gleiche Regeln wie bei iOS (SprachHelfer). */
object SprachTipp {
    private val weg = ".,!?;:…\"'“”‘’".toSet()

    fun norm(eingabe: String, sp: String): String {
        var t = eingabe.trim()
        t = if (sp == "tr") t.lowercase(Locale("tr")) else t.lowercase()
        if (sp == "it") {
            t = t.replace('’', '\'').replace('‘', '\'')
            for (p in listOf("l'", "un'")) if (t.startsWith(p)) { t = t.removePrefix(p); break }
        }
        t = t.filter { it !in weg }
        t = t.replace('-', ' ').replace('–', ' ').replace('—', ' ')
        t = t.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
        if (sp == "en") for (p in listOf("to ", "the ", "a ", "an ")) if (t.startsWith(p)) { t = t.removePrefix(p); break }
        if (sp == "it") for (p in listOf("il ", "lo ", "la ", "i ", "gli ", "le ", "un ", "uno ", "una ")) if (t.startsWith(p)) { t = t.removePrefix(p); break }
        return t
    }

    /** Die Zusatztasten über der Tastatur beim Tippen. */
    fun sonderzeichen(sp: String): List<String> = when (sp) {
        "tr" -> listOf("ç", "ğ", "ı", "ö", "ş", "ü", "İ")
        "it" -> listOf("à", "è", "é", "ì", "ò", "ù")
        else -> emptyList()
    }

    private val falten = listOf(
        "ı" to "i", "İ" to "i", "ğ" to "g", "ü" to "u", "ş" to "s", "ö" to "o", "ç" to "c", "â" to "a", "ä" to "a",
        "î" to "i", "û" to "u", "ß" to "ss", "à" to "a", "è" to "e", "é" to "e", "ì" to "i", "ò" to "o", "ù" to "u",
    )

    /** Sonderzeichen vereinfachen, damit "kirmizi" als fast richtig für "kırmızı" gilt. */
    fun falte(s: String): String {
        var t = s
        for ((a, b) in falten) t = t.replace(a, b)
        return t.replace(" ", "")
    }

    fun pruefe(richtig: String, eingabe: String, sp: String): Tipp {
        val e = norm(eingabe, sp)
        if (e.isEmpty()) return Tipp.FALSCH
        val r = norm(richtig, sp)
        if (r == e) return Tipp.RICHTIG
        if (falte(r) == falte(e)) return Tipp.FAST
        return Tipp.FALSCH
    }
}
