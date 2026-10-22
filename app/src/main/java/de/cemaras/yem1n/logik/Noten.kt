package de.cemaras.yem1n.logik

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Noten im Violinschlüssel für Musik in der Lernwelt. Port von `Noten` aus AufgabenBild.swift. */
object Noten {
    /** Position auf der Notenzeile: 0 ist die unterste Linie (e'), jeder Schritt eine halbe Linie höher */
    val stufe: Map<String, Int> = mapOf(
        "c1" to -2, "d1" to -1, "e1" to 0, "f1" to 1, "g1" to 2, "a1" to 3,
        "h1" to 4, "c2" to 5, "d2" to 6, "e2" to 7, "f2" to 8,
    )

    /** Wie die Note im Unterricht heißt */
    val name: Map<String, String> = mapOf(
        "c1" to "c", "d1" to "d", "e1" to "e", "f1" to "f", "g1" to "g", "a1" to "a",
        "h1" to "h", "c2" to "c", "d2" to "d", "e2" to "e", "f2" to "f",
    )

    val frequenz: Map<String, Double> = mapOf(
        "c1" to 261.63, "d1" to 293.66, "e1" to 329.63, "f1" to 349.23,
        "g1" to 392.00, "a1" to 440.00, "h1" to 493.88, "c2" to 523.25,
        "d2" to 587.33, "e2" to 659.26, "f2" to 698.46,
    )

    val alle = listOf("c1", "d1", "e1", "f1", "g1", "a1", "h1", "c2", "d2", "e2", "f2")

    const val RATE = 44_100

    /** Ein weicher Ton wie von einem Glockenspiel: schnell an, langsam aus. */
    fun pcm(frequenz: Double, dauer: Double = 1.2): ShortArray {
        val anzahl = (RATE * dauer).toInt()
        return ShortArray(anzahl) { i ->
            val t = i.toDouble() / RATE
            val an = min(t / 0.015, 1.0)
            val aus = exp(-3.2 * t)
            val welle = sin(2 * PI * frequenz * t) + 0.25 * sin(4 * PI * frequenz * t)
            (0.32 * an * aus * welle * Short.MAX_VALUE).toInt().toShort()
        }
    }
}

/** Die Farben aus dem Malkasten (Namen wie in den Aufgaben). */
object Malfarbe {
    /** Als ARGB-Zahl. */
    fun argb(name: String): Long = when (name) {
        "rot" -> 0xFFE62626
        "gelb" -> 0xFFFFD91A
        "blau" -> 0xFF1F59E6
        "gruen" -> 0xFF26A640
        "orange" -> 0xFFFF8C1A
        "violett" -> 0xFF8C40BF
        "weiss" -> 0xFFFFFFFF
        "schwarz" -> 0xFF141414
        "rosa" -> 0xFFFFB3CC
        "hellblau" -> 0xFF8CC7FF
        "grau" -> 0xFF8C8C94
        "braun" -> 0xFF804D26
        "tuerkis" -> 0xFF1AC0C0
        else -> 0xFF808080
    }

    fun anzeige(name: String): String = when (name) {
        "gruen" -> "Grün"
        "weiss" -> "Weiß"
        "tuerkis" -> "Türkis"
        else -> name.replaceFirstChar { it.uppercase() }
    }
}
