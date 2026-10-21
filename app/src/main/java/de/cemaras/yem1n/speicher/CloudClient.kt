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
    const val API_TOKEN = "866221c746d62d9f4f42bc7572da920249c950ef0a0b5164d91d22fca855d5d6"

    /**
     * "production" ist die echte Datenbank, die auch die iOS-App aus TestFlight und dem App Store benutzt.
     * Der API-Token gilt nur für diese Umgebung. Zum Testen in "development" braucht es den Token von dort.
     */
    const val UMGEBUNG = "production"
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

    /**
     * Liest öffentliche Daten, nur mit dem API-Token und ohne Anmeldung. Gibt den rohen Text der Antwort zurück,
     * oder null, wenn keine Verbindung zustande kam. Nie auf dem Haupt-Thread aufrufen.
     */
    fun lesen(operation: String, koerper: String): String? {
        val adresse = CloudAdresse.url(operation, CloudKonfig.API_TOKEN, null, CloudKonfig.UMGEBUNG)
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

    /** Wer ist angemeldet? Ohne Anmeldung kommt `anmeldungNoetig` mit der Anmelde-Adresse zurück. */
    fun aktuellerNutzer(): CloudAntwort = anfrage("users/current")
}
