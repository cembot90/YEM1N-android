package de.cemaras.yem1n.speicher

import de.cemaras.yem1n.logik.FamilienLeitung
import java.net.HttpURLConnection
import java.net.URL

/** Die echte Leitung zum Familien-Dienst bei Cloudflare (Adresse in [ServerKonfig]). */
object FamilienNetz : FamilienLeitung {
    override fun post(pfad: String, kopf: Map<String, String>, body: String): Pair<Int, String> = try {
        val c = URL(ServerKonfig.ADRESSE + pfad).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.requestMethod = "POST"
        c.doOutput = true
        for ((k, v) in kopf) c.setRequestProperty(k, v)
        c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val status = c.responseCode
        val strom = if (status in 200..299) c.inputStream else (c.errorStream ?: c.inputStream)
        status to strom.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (e: Exception) {
        0 to ""
    }
}
