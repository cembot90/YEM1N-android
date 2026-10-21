package de.cemaras.yem1n.logik

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Natürliche Stimmen: Pakete, Speicherort, Prüfen und Auspacken.
 * Es sind freie Piper-Stimmen (CC0 oder gemeinfrei). Die App lädt sie auf Wunsch einmal
 * von GitHub (dieselben Pakete wie bei iOS) und liest danach ohne Internet vor.
 */
class StimmPaket(
    val id: String,
    /** "de", "tr" oder "en", leer bei den Aussprachedaten */
    val sprache: String,
    val name: String,
    /** Ordner nach dem Auspacken, so wie er im Paket steht */
    val ordner: String,
    val bytes: Long,
    val sha256: String,
) {
    val url: String get() = BASIS + id + ".tar"
    val megabyte: Int get() = ((bytes + 500_000) / 1_000_000).toInt()

    companion object {
        const val BASIS = "https://github.com/cembot90/YEM1N/releases/download/stimmen-1/"

        /** Wie die Stimmen ausgesprochen werden (espeak-ng). Gehört zu jeder Stimme dazu. */
        val aussprache = StimmPaket(
            "espeak-1", "", "Aussprache", "espeak-ng-data", 1_228_800L,
            "678402ce9945a6f2cbeb05050cfabfaf631c297a06ddc20eebdaad869f20aad0",
        )

        val stimmen: List<StimmPaket> = listOf(
            StimmPaket(
                "de-thorsten-2", "de", "Thorsten", "de-thorsten", 63_160_320L,
                "7efb961d95490934553771c35766dcce579e8449a2bc2616106e98532f328c51",
            ),
            StimmPaket(
                "tr-fahrettin-1", "tr", "Fahrettin", "tr-fahrettin", 63_160_320L,
                "f092809a87d8a1d1d15710963097c273d728550f73d3ff8abcf27d6141b3ad6d",
            ),
            StimmPaket(
                "en-cori-1", "en", "Cori", "en-cori", 63_539_200L,
                "1f1e56c5054f9d055b406c5debed4bf73bd7dfe9d82ee10f97c5b447b765dc97",
            ),
        )

        fun fuer(sprache: String): StimmPaket? = stimmen.firstOrNull { it.sprache == sprache }
    }
}

/** Wo die geladenen Stimmen auf dem Gerät liegen. */
class StimmOrt(val wurzel: File) {
    fun ordner(p: StimmPaket): File = File(wurzel, p.ordner)

    /** Diese kleine Datei entsteht erst, wenn alles fertig ausgepackt ist. */
    fun marke(p: StimmPaket): File = File(wurzel, p.id + ".fertig")

    fun installiert(p: StimmPaket): Boolean = marke(p).exists()

    /** Eine Stimme ist nutzbar, wenn sie und die Aussprachedaten da sind. */
    fun bereit(p: StimmPaket): Boolean = installiert(p) && installiert(StimmPaket.aussprache)

    /** Packt ein geprüftes Paket aus und ersetzt eine ältere Fassung. */
    fun einrichten(p: StimmPaket, datei: File) {
        wurzel.mkdirs()
        val zwischen = File(wurzel, "auspacken-" + p.id)
        zwischen.deleteRecursively()
        zwischen.mkdirs()
        try {
            TarAuspacker.auspacken(datei, zwischen)
            val neu = File(zwischen, p.ordner)
            if (!neu.exists()) throw IOException("Im Paket fehlt der Ordner ${p.ordner}.")
            marke(p).delete()
            val ziel = ordner(p)
            ziel.deleteRecursively()
            if (!neu.renameTo(ziel)) throw IOException("Die Stimme ließ sich nicht ablegen.")
            marke(p).writeBytes(ByteArray(0))
        } finally {
            zwischen.deleteRecursively()
        }
    }

    /** Räumt Marken alter Fassungen weg, zum Beispiel de-thorsten-1. */
    fun aufraeumen() {
        val aktuell = (listOf(StimmPaket.aussprache) + StimmPaket.stimmen).map { it.id + ".fertig" }.toSet()
        wurzel.listFiles()?.forEach { if (it.name.endsWith(".fertig") && it.name !in aktuell) it.delete() }
    }

    fun entfernen(p: StimmPaket) {
        marke(p).delete()
        ordner(p).deleteRecursively()
    }
}

object StimmPruefung {
    fun sha256(datei: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(datei).use { ein ->
            val puffer = ByteArray(1 shl 16)
            while (true) {
                val n = ein.read(puffer)
                if (n < 0) break
                md.update(puffer, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Null, wenn Größe und Prüfsumme stimmen, sonst ein Satz für die Eltern. */
    fun fehler(datei: File, p: StimmPaket): String? {
        if (datei.length() != p.bytes) return "Die Datei ist nicht vollständig. Bitte noch einmal laden."
        if (sha256(datei) != p.sha256) return "Die Datei ist beschädigt. Bitte noch einmal laden."
        return null
    }
}

/** Liest ein einfaches tar-Archiv (ustar, ohne Kompression) Stück für Stück. */
object TarAuspacker {
    /** Packt aus und gibt die Zahl der Dateien zurück. */
    fun auspacken(datei: File, ziel: File): Int {
        var dateien = 0
        BufferedInputStream(FileInputStream(datei), 1 shl 16).use { ein ->
            while (true) {
                val kopf = ByteArray(512)
                if (!lies(ein, kopf)) break
                if (kopf.all { it.toInt() == 0 }) break
                val name = pfad(kopf)
                val groesse = zahl(kopf, 124, 12)
                val art = kopf[156].toInt().toChar()
                val rest = (512 - groesse % 512) % 512
                if (name.isEmpty() || !sicher(name)) throw IOException("Ungültiger Name im Paket: $name")
                val ziele = File(ziel, name)
                when (art) {
                    '5' -> {
                        ziele.mkdirs()
                        ueberspringe(ein, groesse + rest)
                    }
                    '0', '\u0000' -> {
                        ziele.parentFile?.mkdirs()
                        FileOutputStream(ziele).use { aus ->
                            var offen = groesse
                            val puffer = ByteArray(1 shl 16)
                            while (offen > 0) {
                                val n = ein.read(puffer, 0, minOf(offen, puffer.size.toLong()).toInt())
                                if (n < 0) throw IOException("Das Paket ist unvollständig.")
                                aus.write(puffer, 0, n)
                                offen -= n
                            }
                        }
                        ueberspringe(ein, rest)
                        dateien += 1
                    }
                    else -> ueberspringe(ein, groesse + rest)
                }
            }
        }
        return dateien
    }

    private fun lies(ein: BufferedInputStream, ziel: ByteArray): Boolean {
        var gelesen = 0
        while (gelesen < ziel.size) {
            val n = ein.read(ziel, gelesen, ziel.size - gelesen)
            if (n < 0) return false
            gelesen += n
        }
        return true
    }

    /** Name aus dem Kopf, bei langen Pfaden mit dem Vorsatz aus dem ustar-Feld. */
    fun pfad(b: ByteArray): String {
        fun text(von: Int, laenge: Int): String {
            var ende = von
            while (ende < von + laenge && b[ende].toInt() != 0) ende++
            return String(b, von, ende - von, Charsets.UTF_8)
        }
        val name = text(0, 100)
        val ustar = text(257, 5) == "ustar"
        val vorsatz = if (ustar) text(345, 155) else ""
        val ganz = if (vorsatz.isEmpty()) name else "$vorsatz/$name"
        return ganz.removeSuffix("/")
    }

    /** Kein Pfad darf aus dem Zielordner herausführen. */
    fun sicher(name: String): Boolean = !name.startsWith("/") && !name.split("/").contains("..")

    fun zahl(b: ByteArray, von: Int, laenge: Int): Long {
        val teil = String(b, von, laenge, Charsets.US_ASCII).filter { it != '\u0000' && it != ' ' }
        return teil.toLongOrNull(8)?.takeIf { it >= 0 } ?: throw IOException("Ungültige Größe im Paket.")
    }

    private fun ueberspringe(ein: BufferedInputStream, n: Long) {
        var offen = n
        while (offen > 0) {
            val s = ein.skip(offen)
            if (s <= 0) {
                if (ein.read() < 0) throw IOException("Das Paket ist unvollständig.")
                offen -= 1
            } else {
                offen -= s
            }
        }
    }
}
