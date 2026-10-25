package de.cemaras.yem1n.logik

import java.security.MessageDigest

/**
 * Die Rechnerei für Protokoll 2 (siehe `cloud/PROTOKOLL-V2.md`): Kennungen, Text zur Unterschrift, Umrechnung der Unterschrift.
 * Reines Kotlin ohne Android, damit es auf dem PC gegen die Testvektoren (`cloud/testvektoren-v2.json`) geprüft werden kann.
 */
object FamilienKrypto {
    private const val ZEICHEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /** base64url ohne Auffüllzeichen. Eigene Umsetzung, weil `java.util.Base64` erst ab Android 8 da ist. */
    fun base64url(bytes: ByteArray): String {
        val aus = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 255
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 255 else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 255 else 0
            val n = (b0 shl 16) or (b1 shl 8) or b2
            aus.append(ZEICHEN[(n shr 18) and 63])
            aus.append(ZEICHEN[(n shr 12) and 63])
            if (i + 1 < bytes.size) aus.append(ZEICHEN[(n shr 6) and 63])
            if (i + 2 < bytes.size) aus.append(ZEICHEN[n and 63])
            i += 3
        }
        return aus.toString()
    }

    /** Das Gegenstück. Null bei unerlaubten Zeichen oder unmöglicher Länge. */
    fun base64urlLesen(text: String): ByteArray? {
        if (text.length % 4 == 1) return null
        val werte = IntArray(text.length)
        for (i in text.indices) {
            val w = ZEICHEN.indexOf(text[i])
            if (w < 0) return null
            werte[i] = w
        }
        val aus = ByteArray(text.length * 6 / 8)
        var o = 0
        var i = 0
        while (i < werte.size) {
            val n = (werte[i] shl 18) or
                ((if (i + 1 < werte.size) werte[i + 1] else 0) shl 12) or
                ((if (i + 2 < werte.size) werte[i + 2] else 0) shl 6) or
                (if (i + 3 < werte.size) werte[i + 3] else 0)
            if (o < aus.size) aus[o++] = (n shr 16).toByte()
            if (o < aus.size) aus[o++] = (n shr 8).toByte()
            if (o < aus.size) aus[o++] = n.toByte()
            i += 4
        }
        return aus
    }

    fun hex(bytes: ByteArray): String {
        val aus = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            aus.append("0123456789abcdef"[(b.toInt() shr 4) and 15])
            aus.append("0123456789abcdef"[b.toInt() and 15])
        }
        return aus.toString()
    }

    fun sha256Hex(daten: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(daten))

    fun sha256Hex(text: String): String = sha256Hex(text.toByteArray(Charsets.UTF_8))

    /** Die Kennung eines Geräts: die ersten 24 Hex-Zeichen von SHA-256 über die 65 Bytes des öffentlichen Schlüssels. */
    fun geraetId(pub: ByteArray): String = sha256Hex(pub).take(24)

    /**
     * Der Keystore liefert den öffentlichen Schlüssel als X.509 (SPKI). Die letzten 65 Bytes sind der unkomprimierte
     * Punkt `04 X Y`, den der Dienst erwartet. Null, wenn das nicht so aussieht.
     */
    fun pubAusSpki(spki: ByteArray): ByteArray? {
        if (spki.size < 65) return null
        val pub = spki.copyOfRange(spki.size - 65, spki.size)
        return if (pub[0].toInt() == 4) pub else null
    }

    /** Der Text, der unterschrieben wird: sieben Zeilen, getrennt durch Zeilenumbruch. */
    fun unterschriftText(methode: String, pfad: String, familie: String, geraet: String, zeit: Long, bodyHash: String): String =
        listOf("YEM1N-V2", methode, pfad, familie, geraet, zeit.toString(), bodyHash).joinToString("\n")

    /**
     * Rechnet eine Unterschrift im DER-Format (so liefert sie Android) in rohes `r` und `s` um, je 32 Bytes.
     * Null, wenn die Folge nicht stimmt.
     */
    fun derZuRoh(der: ByteArray): ByteArray? {
        try {
            var i = 0
            if (der[i++].toInt() != 0x30) return null
            var laenge = der[i++].toInt() and 255
            if (laenge and 0x80 != 0) {
                val n = laenge and 0x7f
                if (n < 1 || n > 2) return null
                laenge = 0
                repeat(n) { laenge = (laenge shl 8) or (der[i++].toInt() and 255) }
            }
            if (i + laenge != der.size) return null
            val aus = ByteArray(64)
            for (teil in 0..1) {
                if (der[i++].toInt() != 0x02) return null
                val n = der[i++].toInt() and 255
                if (n == 0 || i + n > der.size) return null
                var von = i
                var bis = i + n
                while (bis - von > 32 && der[von].toInt() == 0) von++
                if (bis - von > 32) return null
                System.arraycopy(der, von, aus, teil * 32 + 32 - (bis - von), bis - von)
                i += n
            }
            return if (i == der.size) aus else null
        } catch (e: IndexOutOfBoundsException) {
            return null
        }
    }
}
