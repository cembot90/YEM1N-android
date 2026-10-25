package de.cemaras.yem1n.logik

import java.security.MessageDigest

/**
 * Die Eltern-PIN schützt den Bereich der Eltern (Familie, Geräte erlauben, Ergebnisse, Joker beantworten),
 * damit ein Kind ihn nicht aus Versehen oder aus Neugier öffnet. Sie ist eine Sperre für Kinder, kein Tresor:
 * Die Daten der Familie schützt der Schlüssel des Geräts, nicht die PIN.
 * Reines Kotlin, damit es ohne Gerät testbar ist.
 */
object ElternPin {
    const val MIN_LAENGE = 4
    const val MAX_LAENGE = 8

    /** So lange bleibt der Bereich nach der richtigen PIN offen. */
    const val OFFEN_MS = 5 * 60 * 1000L

    private const val RUNDEN = 5000

    /** Nur Ziffern, 4 bis 8 Stück. */
    fun gueltig(pin: String): Boolean = pin.length in MIN_LAENGE..MAX_LAENGE && pin.all { it in '0'..'9' }

    /** Einfache Folgen und Wiederholungen sind zu leicht zu erraten (1234, 0000, 4321). */
    fun zuLeicht(pin: String): Boolean {
        if (pin.length < MIN_LAENGE) return true
        if (pin.all { it == pin[0] }) return true
        val schritte = (1 until pin.length).map { pin[it] - pin[it - 1] }
        return schritte.all { it == 1 } || schritte.all { it == -1 }
    }

    /** Einmal pro PIN, damit gleiche PINs auf verschiedenen Geräten verschiedene Werte ergeben. [zufall] muss 16 Bytes haben. */
    fun salz(zufall: ByteArray): String = FamilienKrypto.hex(zufall)

    fun hash(pin: String, salz: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        var h = md.digest((salz + ":" + pin).toByteArray(Charsets.UTF_8))
        repeat(RUNDEN) {
            md.reset()
            md.update(h)
            md.update(salz.toByteArray(Charsets.UTF_8))
            h = md.digest()
        }
        return FamilienKrypto.hex(h)
    }

    fun stimmt(pin: String, salz: String, hash: String): Boolean {
        if (salz.isEmpty() || hash.isEmpty()) return false
        // Gleich lange Prüfung, damit die Dauer nichts verrät.
        return MessageDigest.isEqual(hash(pin, salz).toByteArray(Charsets.UTF_8), hash.toByteArray(Charsets.UTF_8))
    }

    /** Wartezeit nach so vielen falschen Eingaben in Folge. Die ersten zwei sind frei. */
    fun sperreMs(fehlversuche: Int): Long = when {
        fehlversuche < 3 -> 0L
        fehlversuche == 3 -> 30_000L
        fehlversuche == 4 -> 60_000L
        fehlversuche == 5 -> 5 * 60_000L
        else -> 15 * 60_000L
    }

    /** Freundlicher Text für die Wartezeit. */
    fun sperreText(ms: Long): String {
        val s = ((ms + 999) / 1000).coerceAtLeast(1)
        return when {
            s == 1L -> "Bitte noch 1 Sekunde warten."
            s < 90 -> "Bitte noch $s Sekunden warten."
            else -> "Bitte noch ${(s + 59) / 60} Minuten warten."
        }
    }
}
