package de.cemaras.yem1n.speicher

import java.net.HttpURLConnection
import java.net.URL

/** Wo der eigene Server läuft (Cloudflare Worker, siehe `server/EINRICHTEN.md`). */
object ServerKonfig {
    const val ADRESSE = "https://yem1n-server.cemaras-yem1n.workers.dev"
}

data class ServerErgebnis(val ok: Boolean, val meldung: String = "")

/** Schickt Joker, Antworten und Ergebnisse an den Server. Nie auf dem Haupt-Thread aufrufen. */
object ServerClient {
    fun senden(pfad: String, koerper: String): ServerErgebnis {
        return try {
            val c = URL(ServerKonfig.ADRESSE + pfad).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 20000
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(koerper.toByteArray(Charsets.UTF_8)) }
            val ok = c.responseCode in 200..299
            val strom = if (ok) c.inputStream else (c.errorStream ?: c.inputStream)
            val text = strom.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (ok) ServerErgebnis(true) else ServerErgebnis(false, fehlertext(c.responseCode, text))
        } catch (e: Exception) {
            ServerErgebnis(false, "Keine Verbindung. Ist das Internet an?")
        }
    }

    internal fun fehlertext(status: Int, text: String): String = when (status) {
        429 -> "Das ging zu schnell. Bitte kurz warten."
        400 -> "Das konnte nicht gesendet werden. Bitte Text und Familiencode prüfen."
        else -> "Der Server hat nicht geantwortet (Code $status)."
    }
}
