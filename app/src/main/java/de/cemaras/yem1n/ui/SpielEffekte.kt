package de.cemaras.yem1n.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.HapticFeedbackConstants
import android.view.View
import de.cemaras.yem1n.logik.Klang
import java.util.concurrent.Executors

/** Ton und Vibration für die Spiele. Beides lässt sich in den Einstellungen ausschalten. */
object SpielEffekte {
    @Volatile var ton = true
    @Volatile var haptik = true
    @Volatile var ansicht: View? = null

    private val werkbank = Executors.newSingleThreadExecutor { r -> Thread(r, "spielklang").apply { isDaemon = true } }
    private val vorrat = HashMap<Klang, ShortArray>()
    private var laeuft = 0

    fun klang(k: Klang) {
        if (!ton || laeuft >= 4) return
        laeuft++
        werkbank.execute {
            try {
                val daten = synchronized(vorrat) { vorrat.getOrPut(k) { Klang.pcm(k) } }
                val spur = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(Klang.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                    )
                    .setBufferSizeInBytes(daten.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                spur.write(daten, 0, daten.size)
                spur.play()
                Thread.sleep(k.dauerMs + 40L)
                spur.release()
            } catch (e: Exception) {
                // Kein Ton ist besser als ein Absturz.
            } finally {
                laeuft--
            }
        }
    }

    /** Kurzes Klopfen, zum Beispiel beim Fangen einer Münze. */
    fun tipp() {
        if (haptik) ansicht?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Kräftiger Stoß bei einem Treffer oder Tor. */
    fun stoss() {
        if (haptik) ansicht?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun einhorn(e: de.cemaras.yem1n.logik.EinhornEreignis) {
        when (e.ding) {
            de.cemaras.yem1n.logik.EinhornRegeln.Ding.REGENBOGEN, de.cemaras.yem1n.logik.EinhornRegeln.Ding.HERZ -> { klang(Klang.PLING); tipp() }
            de.cemaras.yem1n.logik.EinhornRegeln.Ding.STERN -> { klang(Klang.KLEIN); tipp() }
            de.cemaras.yem1n.logik.EinhornRegeln.Ding.GEWITTER -> { klang(Klang.AUTSCH); stoss() }
            null -> {}
        }
        if (e.levelHoch) klang(Klang.LEVEL)
    }

    fun lauf(e: de.cemaras.yem1n.logik.LaufEreignis) {
        when (e.art) {
            de.cemaras.yem1n.logik.LaufEreignis.Art.MUENZE -> { klang(Klang.KLEIN); tipp() }
            de.cemaras.yem1n.logik.LaufEreignis.Art.FLUEGEL -> { klang(Klang.PLING); tipp() }
            de.cemaras.yem1n.logik.LaufEreignis.Art.TREFFER -> { klang(Klang.AUTSCH); stoss() }
            de.cemaras.yem1n.logik.LaufEreignis.Art.STUFE -> klang(Klang.LEVEL)
        }
    }

    fun jagd(e: de.cemaras.yem1n.logik.JagdEreignis) {
        when (e.art) {
            de.cemaras.yem1n.logik.JagdEreignis.Art.FUNKEL -> { klang(Klang.KLEIN); tipp() }
            de.cemaras.yem1n.logik.JagdEreignis.Art.TREFFER -> { klang(Klang.AUTSCH); stoss() }
            de.cemaras.yem1n.logik.JagdEreignis.Art.EBENE -> klang(Klang.LEVEL)
        }
    }

    fun elfmeter(e: de.cemaras.yem1n.logik.ElfmeterEreignis) {
        when (e.art) {
            de.cemaras.yem1n.logik.ElfmeterEreignis.Art.TOR -> { klang(Klang.TOR); stoss() }
            de.cemaras.yem1n.logik.ElfmeterEreignis.Art.GEHALTEN -> { klang(Klang.GEHALTEN); tipp() }
            de.cemaras.yem1n.logik.ElfmeterEreignis.Art.DANEBEN -> klang(Klang.GEHALTEN)
            de.cemaras.yem1n.logik.ElfmeterEreignis.Art.LEVEL -> klang(Klang.LEVEL)
        }
    }

    fun hockey(e: de.cemaras.yem1n.logik.HockeyEreignis) {
        when (e.art) {
            de.cemaras.yem1n.logik.HockeyEreignis.Art.SCHLAG -> { klang(Klang.SCHLAG); tipp() }
            de.cemaras.yem1n.logik.HockeyEreignis.Art.TOR_ICH -> { klang(Klang.TOR); stoss() }
            de.cemaras.yem1n.logik.HockeyEreignis.Art.TOR_KI -> { klang(Klang.AUTSCH); stoss() }
            de.cemaras.yem1n.logik.HockeyEreignis.Art.SIEG -> klang(Klang.SIEG)
        }
    }
}
