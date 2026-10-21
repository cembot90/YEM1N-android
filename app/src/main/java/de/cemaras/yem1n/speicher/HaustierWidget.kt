package de.cemaras.yem1n.speicher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.cemaras.yem1n.MainActivity
import de.cemaras.yem1n.R
import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Tag
import de.cemaras.yem1n.logik.WidgetText

/** Kleines Widget für den Startbildschirm: Haustier und Serie. Es liest nur die Datei auf dem Gerät. */
class HaustierWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        zeichne(context, manager, ids, Speicher(context).ladeFortschritt())
    }

    companion object {
        /** Nach jeder Änderung am Fortschritt aufrufen. Ohne Widget auf dem Bildschirm passiert nichts. */
        fun aktualisieren(context: Context, f: Fortschritt) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, HaustierWidget::class.java))
                if (ids.isNotEmpty()) zeichne(context, manager, ids, f)
            } catch (e: Exception) {
                // Das Widget ist nur eine Zugabe.
            }
        }

        private fun zeichne(context: Context, manager: AppWidgetManager, ids: IntArray, f: Fortschritt) {
            val t = WidgetText.von(f, System.currentTimeMillis(), Tag.heute())
            val oeffnen = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            for (id in ids) {
                val v = RemoteViews(context.packageName, R.layout.widget_haustier)
                v.setTextViewText(R.id.widget_emoji, t.emoji)
                v.setTextViewText(R.id.widget_titel, t.titel)
                v.setTextViewText(R.id.widget_zeile, t.zeile)
                v.setOnClickPendingIntent(R.id.widget_wurzel, oeffnen)
                manager.updateAppWidget(id, v)
            }
        }
    }
}
