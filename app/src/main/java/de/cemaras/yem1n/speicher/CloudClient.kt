package de.cemaras.yem1n.speicher

import android.content.Context
import de.cemaras.yem1n.logik.CloudAdresse
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Zugang zur CloudKit-Datenbank. Der API-Token ist öffentlich, mit ihm allein darf man nur lesen. */
object CloudKonfig {
    const val API_TOKEN = "866221c746d62d9f4f42bc7572da920249c950ef0a0b5164d91d22fca855d5d6"

    /**
     * "production" ist die echte Datenbank, die auch die iOS-App aus TestFlight und dem App Store benutzt.
     * Der API-Token gilt nur für diese Umgebung. Zum Testen in "development" braucht es den Token von dort.
     */
    const val UMGEBUNG = "production"
}

/** Liest öffentliche Klassenpakete aus CloudKit. Es gibt keine Anmeldung bei Apple mehr. */
class CloudClient(context: Context) {
    init {
        // Der Anmelde-Schlüssel aus früheren Versionen wird nicht mehr gebraucht und gehört nicht auf das Gerät.
        try { File(context.filesDir, "cloud-anmeldung.txt").delete() } catch (e: Exception) { /* egal */ }
    }

    /**
     * Liest öffentliche Daten, nur mit dem API-Token. Gibt den rohen Text der Antwort zurück,
     * oder null, wenn keine Verbindung zustande kam. Nie auf dem Haupt-Thread aufrufen.
     */
    fun lesen(operation: String, koerper: String): String? {
        val adresse = CloudAdresse.url(operation, CloudKonfig.API_TOKEN, CloudKonfig.UMGEBUNG)
        return try {
            val c = URL(adresse).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 20000
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(koerper.toByteArray(Charsets.UTF_8)) }
            val strom = if (c.responseCode in 200..299) c.inputStream else (c.errorStream ?: c.inputStream)
            strom.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
