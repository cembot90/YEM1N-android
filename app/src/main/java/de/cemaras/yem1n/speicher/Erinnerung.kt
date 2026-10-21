package de.cemaras.yem1n.speicher

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import de.cemaras.yem1n.MainActivity
import de.cemaras.yem1n.logik.Haustier
import de.cemaras.yem1n.logik.HaustierErinnerung

/** Plant die Haustier-Erinnerung und zeigt sie an. Nichts davon verlässt das Gerät. */
object Erinnerung {
    private const val KANAL = "haustier"
    private const val ANFRAGE = 17

    private fun alarm(ctx: Context) = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun absicht(ctx: Context, titel: String = "", text: String = ""): PendingIntent {
        val i = Intent(ctx, ErinnerungEmpfaenger::class.java)
            .putExtra("titel", titel).putExtra("text", text)
        return PendingIntent.getBroadcast(ctx, ANFRAGE, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun erlaubt(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Alte Erinnerung weg, neue planen, wenn es ein Haustier gibt und der Schalter an ist. */
    fun planen(ctx: Context, haustier: Haustier?, an: Boolean, jetzt: Long = System.currentTimeMillis()) {
        try {
            val a = alarm(ctx)
            a.cancel(absicht(ctx))
            if (!an || haustier == null || !erlaubt(ctx)) return
            val h = haustier.aktualisiert(jetzt)
            val zeit = HaustierErinnerung.zeitpunkt(h, jetzt)
            a.setAndAllowWhileIdle(
                AlarmManager.RTC, zeit,
                absicht(ctx, HaustierErinnerung.titel(h), HaustierErinnerung.text(h)),
            )
        } catch (e: Exception) {
            // Ohne Erinnerung läuft die App ganz normal weiter.
        }
    }

    internal fun zeigen(ctx: Context, titel: String, text: String) {
        if (!erlaubt(ctx) || titel.isEmpty()) return
        val zentrale = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            zentrale.createNotificationChannel(NotificationChannel(KANAL, "Haustier", NotificationManager.IMPORTANCE_DEFAULT))
        }
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
        zentrale.notify(ANFRAGE, n)
    }
}

class ErinnerungEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Erinnerung.zeigen(context, intent.getStringExtra("titel").orEmpty(), intent.getStringExtra("text").orEmpty())
    }
}
