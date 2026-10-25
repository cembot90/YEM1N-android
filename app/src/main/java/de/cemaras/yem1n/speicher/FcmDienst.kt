package de.cemaras.yem1n.speicher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.cemaras.yem1n.MainActivity

/** Mitteilungen der Familie (Frage, Antwort, Ergebnis ...), geschickt vom Server über Googles FCM. */
object Mitteilungen {
    const val KANAL = "mitteilungen"
    private const val ANFRAGE = 31

    fun kanalAnlegen(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val zentrale = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        zentrale.createNotificationChannel(NotificationChannel(KANAL, "Familie", NotificationManager.IMPORTANCE_HIGH))
    }

    fun zeigen(ctx: Context, titel: String, text: String) {
        if (titel.isEmpty() || !Erinnerung.erlaubt(ctx)) return
        kanalAnlegen(ctx)
        val oeffnen = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, KANAL)
            .setSmallIcon(ctx.applicationInfo.icon)
            .setContentTitle(titel)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(oeffnen)
            .build()
        // Jede Mitteilung bekommt ihre eigene Nummer, damit sich mehrere nicht überschreiben.
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(ANFRAGE + (System.currentTimeMillis() % 1000).toInt(), n)
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("push", Context.MODE_PRIVATE)

    fun token(ctx: Context): String = prefs(ctx).getString("token", "").orEmpty()
    fun speichereToken(ctx: Context, t: String) = prefs(ctx).edit().putString("token", t).apply()

    /**
     * Die Push-Kennung von Google gibt es erst nach der Einwilligung der Eltern (im Manifest ist die automatische
     * Anmeldung bei Google aus). Vorher hat die App keinen Kontakt zu Google.
     */
    fun einschalten() {
        try { FirebaseMessaging.getInstance().isAutoInitEnabled = true } catch (e: Exception) { /* ohne Google-Dienste geht es auch */ }
    }

    /** Widerruf: Die Push-Kennung wird bei Google gelöscht und nicht mehr neu angelegt. */
    fun ausschalten(ctx: Context) {
        try {
            val fm = FirebaseMessaging.getInstance()
            fm.isAutoInitEnabled = false
            fm.deleteToken()
        } catch (e: Exception) { /* ohne Google-Dienste gibt es nichts zu löschen */ }
        prefs(ctx).edit().clear().apply()
    }

    /** Was dem Server zuletzt gemeldet wurde (Code, Rolle, Kennung), damit nicht bei jedem Start neu gemeldet wird. */
    fun gemeldet(ctx: Context): String = prefs(ctx).getString("gemeldet", "").orEmpty()
    fun merkeGemeldet(ctx: Context, s: String) = prefs(ctx).edit().putString("gemeldet", s).apply()
}

class FcmDienst : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Mitteilungen.speichereToken(this, token)
        // Gemeldet wird beim nächsten Start der App, dann kennt sie Familie und Rolle.
        Mitteilungen.merkeGemeldet(this, "")
    }

    /** Kommt nur an, wenn die App offen ist. Sonst zeigt das System die Mitteilung selbst. */
    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        Mitteilungen.zeigen(this, n.title.orEmpty(), n.body.orEmpty())
    }
}
