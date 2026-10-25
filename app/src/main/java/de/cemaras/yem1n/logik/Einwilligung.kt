package de.cemaras.yem1n.logik

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Einwilligung der Eltern, bevor Daten an die Familie in die Cloud gehen.
 * Wird [AKTUELLE_VERSION] erhöht, fragt die App noch einmal.
 */
object Einwilligung {
    /** 2: Die Familie läuft über ein eigenes, abgetrenntes Fach in der EU (Protokoll 2), nicht mehr über den Familiencode in CloudKit. */
    const val AKTUELLE_VERSION = 2

    fun erforderlich(gespeicherteVersion: Int): Boolean = gespeicherteVersion < AKTUELLE_VERSION

    fun datumText(ms: Long): String =
        if (ms <= 0) "noch nicht erteilt" else SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(ms))

    val punkte = listOf(
        "Die App speichert Übungen, Ergebnisse und Einstellungen auf diesem Gerät.",
        "Nur wenn dieses Gerät zu einer Familie gehört, gehen Ergebnisse, der Name des Kindes (freiwillig), Joker-Nachrichten und Duelle an die Familie. " +
            "Jede Familie hat ein eigenes, abgetrenntes Fach bei Cloudflare in der EU. Neue Geräte kommen nur mit einem QR-Code dazu und müssen von den Eltern erlaubt werden. " +
            "Für Namen reicht ein Vorname oder Spitzname.",
        "Mitteilungen kommen über Google (FCM). Sie enthalten keine Namen und keine Inhalte, nur einen kurzen festen Satz.",
        "Es gibt keine Werbung, keine Analysedienste und kein Benutzerkonto.",
        "Du kannst die Einwilligung jederzeit in den Einstellungen widerrufen. Dann hört die App sofort auf, Daten zu senden. Die Familie kannst du unter Familie ganz löschen.",
    )
}
