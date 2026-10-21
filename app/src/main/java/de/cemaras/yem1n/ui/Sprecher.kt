package de.cemaras.yem1n.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Liest Fragen und Lob mit der Sprachausgabe des Geräts vor. Fehlt die deutsche Stimme oder
 * lässt sich die Sprachausgabe nicht starten, bleibt die App einfach still.
 */
class Sprecher(kontext: Context) {
    private var tts: TextToSpeech? = null
    private var bereit = false
    private var wartend: Pair<String, Boolean>? = null

    /** Ausgeschaltet bleibt die App still (Einstellung der Eltern). */
    var an: Boolean = true

    init {
        tts = try {
            TextToSpeech(kontext.applicationContext) { status ->
                val t = tts
                if (status == TextToSpeech.SUCCESS && t != null) {
                    val r = t.setLanguage(Locale.GERMANY)
                    bereit = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                    val w = wartend
                    wartend = null
                    if (bereit && w != null) sprich(w.first, w.second)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** [froh] klingt etwas höher und lebhafter, zum Beispiel beim Loben. */
    fun sprich(text: String, froh: Boolean = false) {
        if (!an || text.isBlank()) return
        val t = tts ?: return
        if (!bereit) {
            wartend = text to froh
            return
        }
        try {
            t.setSpeechRate(if (froh) 1.0f else 0.9f)
            t.setPitch(if (froh) 1.15f else 1.05f)
            t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yem1n")
        } catch (e: Exception) {
            // Die Sprachausgabe ist nicht erreichbar: still bleiben.
        }
    }

    fun stopp() {
        wartend = null
        try {
            tts?.stop()
        } catch (e: Exception) {
            // nichts zu tun
        }
    }

    fun schliessen() {
        stopp()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            // nichts zu tun
        }
        tts = null
        bereit = false
    }
}
