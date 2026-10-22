package de.cemaras.yem1n.logik

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Einwilligung der Eltern, bevor Daten über den Familiencode in die Cloud gehen.
 * Wird [AKTUELLE_VERSION] erhöht, fragt die App noch einmal.
 */
object Einwilligung {
    const val AKTUELLE_VERSION = 1

    fun erforderlich(gespeicherteVersion: Int): Boolean = gespeicherteVersion < AKTUELLE_VERSION

    fun datumText(ms: Long): String =
        if (ms <= 0) "noch nicht erteilt" else SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(ms))

    val punkte = listOf(
        "Die App speichert Übungen, Ergebnisse und Einstellungen auf diesem Gerät.",
        "Nur mit einem Familiencode gehen Ergebnisse, der Name des Kindes (freiwillig), Joker-Nachrichten und Duelle an die Familie. " +
            "Das läuft über einen kleinen Server (Cloudflare) in die iCloud-Datenbank CloudKit. Mitteilungen kommen über Google (FCM). Für Namen reicht ein Vorname oder Spitzname.",
        "Es gibt keine Werbung, keine Analysedienste und kein Benutzerkonto.",
        "Du kannst die Einwilligung jederzeit in den Einstellungen widerrufen. Dann hört die App sofort auf, Daten zu senden.",
    )
}
