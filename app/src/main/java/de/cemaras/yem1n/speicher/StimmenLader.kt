package de.cemaras.yem1n.speicher

import de.cemaras.yem1n.logik.StimmOrt
import de.cemaras.yem1n.logik.StimmPaket
import de.cemaras.yem1n.logik.StimmPruefung
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Lädt ein Stimmenpaket von GitHub, prüft es und packt es aus. Läuft im Hintergrund. */
object StimmenLader {
    /**
     * Gibt null zurück, wenn alles geklappt hat, sonst einen Satz für die Eltern.
     * [fortschritt] bekommt Werte von 0 bis 1.
     */
    fun lade(p: StimmPaket, ort: StimmOrt, zwischenordner: File, fortschritt: (Float) -> Unit): String? {
        val datei = File(zwischenordner, p.id + ".tar.laden")
        try {
            zwischenordner.mkdirs()
            val verbindung = URL(p.url).openConnection() as HttpURLConnection
            verbindung.connectTimeout = 20_000
            verbindung.readTimeout = 30_000
            verbindung.instanceFollowRedirects = true
            try {
                if (verbindung.responseCode != 200) return "Der Server antwortet nicht (${verbindung.responseCode}). Bitte später noch einmal."
                var gelesen = 0L
                verbindung.inputStream.use { ein ->
                    FileOutputStream(datei).use { aus ->
                        val puffer = ByteArray(1 shl 16)
                        var letzte = -1
                        while (true) {
                            val n = ein.read(puffer)
                            if (n < 0) break
                            aus.write(puffer, 0, n)
                            gelesen += n
                            val prozent = (gelesen * 100 / p.bytes.coerceAtLeast(1)).toInt().coerceIn(0, 100)
                            if (prozent != letzte) {
                                letzte = prozent
                                fortschritt(prozent / 100f)
                            }
                        }
                    }
                }
            } finally {
                verbindung.disconnect()
            }
            StimmPruefung.fehler(datei, p)?.let { return it }
            ort.einrichten(p, datei)
            return null
        } catch (e: Exception) {
            return "Das Laden hat nicht geklappt. Ist das Gerät im Internet?"
        } finally {
            datei.delete()
        }
    }
}
