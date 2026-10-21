package de.cemaras.yem1n.speicher

import android.content.Context
import de.cemaras.yem1n.logik.CloudAdresse
import de.cemaras.yem1n.logik.CloudAntwort
import de.cemaras.yem1n.logik.CloudAuswertung
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Zugang zur CloudKit-Datenbank. Der API-Token ist öffentlich, mit ihm allein darf man nur lesen. */
object CloudKonfig {
    const val API_TOKEN = "1a871a85aea9944bde5f238aead6482a580e523faa6f558879781aa6cd30dac1"

    /** "development" zum Testen. Für die fertige App auf "production" stellen (mit eigenem Token). */
    const val UMGEBUNG = "development"
}

/** Spricht mit den CloudKit-Web-Services und merkt sich den Anmelde-Schlüssel des Nutzers auf dem Gerät. */
class CloudClient(context: Context) {
    private val datei = File(context.filesDir, "cloud-anmeldung.txt")

    @Volatile private var webAuth: String? = try { datei.readText(Charsets.UTF_8).trim().ifEmpty { null } } catch (e: Exception) { null }

    val angemeldet: Boolean get() = webAuth != null

    fun merkeToken(token: String?) {
        webAuth = token?.ifEmpty { null }
        try {
            if (webAuth == null) datei.delete() else datei.writeText(webAuth!!, Charsets.UTF_8)
        } catch (e: Exception) {
            // Dann muss man sich beim nächsten Start neu anmelden.
        }
    }

    /** Eine Anfrage an CloudKit. Läuft im Hintergrund, nie auf dem Haupt-Thread aufrufen. */
    fun anfrage(operation: String, koerper: String? = null): CloudAntwort {
        val adresse = CloudAdresse.url(operation, CloudKonfig.API_TOKEN, webAuth, CloudKonfig.UMGEBUNG)
        return try {
            val c = URL(adresse).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 20000
            if (koerper != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(koerper.toByteArray(Charsets.UTF_8)) }
            }
            val strom = if (c.responseCode in 200..299) c.inputStream else (c.errorStream ?: c.inputStream)
            val text = strom.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val a = CloudAuswertung.lies(text)
            // Apple macht den alten Schlüssel nach jeder Anfrage ungültig und liefert einen neuen.
            if (a.neuerToken != null) merkeToken(a.neuerToken)
            a
        } catch (e: Exception) {
            CloudAntwort(false, "NETZ", "Keine Verbindung zu Apple: ${e.javaClass.simpleName}")
        }
    }

    /** Wer ist angemeldet? Ohne Anmeldung kommt `anmeldungNoetig` mit der Anmelde-Adresse zurück. */
    fun aktuellerNutzer(): CloudAntwort = anfrage("users/current")
}
