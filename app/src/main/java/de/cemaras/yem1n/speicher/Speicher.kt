package de.cemaras.yem1n.speicher

import android.content.Context
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.PaketLeser
import de.cemaras.yem1n.logik.Fortschritt
import java.io.File

/**
 * Liest und schreibt alles, was auf dem Gerät liegt: die mitgelieferten Beispiel-Arbeiten (assets),
 * eingefügte Arbeiten (Ordner "pakete" im App-Speicher) und den Fortschritt.
 */
class Speicher(private val context: Context) {

    private val ordnerPakete: File get() = File(context.filesDir, "pakete").also { it.mkdirs() }
    private val dateiFortschritt: File get() = File(context.filesDir, "fortschritt.json")

    /** Alle Arbeiten, doppelte (gleiche Klasse, Fach, Titel) nur einmal. */
    fun ladeArbeiten(): List<Arbeit> {
        val texte = mutableListOf<String>()
        val namen = context.assets.list("pakete").orEmpty().filter { it.endsWith(".json") }.sorted()
        for (name in namen) {
            context.assets.open("pakete/$name").bufferedReader(Charsets.UTF_8).use { texte += it.readText() }
        }
        val eigene = ordnerPakete.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        for (datei in eigene) {
            texte += try { datei.readText(Charsets.UTF_8) } catch (e: Exception) { continue }
        }
        val gesehen = mutableSetOf<String>()
        return texte.mapNotNull { PaketLeser.lese(it).arbeit }.filter { gesehen.add(it.schluessel) }
    }

    /** Legt eine eingefügte Arbeit im App-Speicher ab. */
    fun speichereArbeit(arbeit: Arbeit, json: String) {
        val name = arbeit.titel.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "arbeit" }
        val datei = File(ordnerPakete, "$name-${Integer.toHexString(json.hashCode())}.json")
        datei.writeText(PaketLeser.bereinigt(json), Charsets.UTF_8)
    }

    fun ladeFortschritt(): Fortschritt =
        Fortschritt.ausJson(
            try { dateiFortschritt.readText(Charsets.UTF_8) } catch (e: Exception) { null },
        )

    /** Erst in eine Hilfsdatei schreiben, dann umbenennen, damit nie ein halber Stand liegen bleibt. */
    fun speichereFortschritt(fortschritt: Fortschritt) {
        try {
            val hilfe = File(context.filesDir, "fortschritt.json.neu")
            hilfe.writeText(fortschritt.zuJson(), Charsets.UTF_8)
            if (!hilfe.renameTo(dateiFortschritt)) {
                dateiFortschritt.writeText(fortschritt.zuJson(), Charsets.UTF_8)
                hilfe.delete()
            }
        } catch (e: Exception) {
            // Speichern ist nicht lebenswichtig, die Runde geht trotzdem weiter.
        }
    }
}
