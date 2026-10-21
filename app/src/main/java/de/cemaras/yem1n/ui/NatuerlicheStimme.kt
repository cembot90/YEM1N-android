package de.cemaras.yem1n.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import de.cemaras.yem1n.logik.StimmOrt
import de.cemaras.yem1n.logik.StimmPaket
import java.io.File
import java.util.concurrent.Executors

/**
 * Natürliche Stimme mit Piper über sherpa-onnx. Alles läuft auf dem Gerät in einem eigenen
 * Faden, damit die Oberfläche nicht hängt. Fehlt die Stimme oder geht etwas schief, meldet
 * [sprich] das, dann liest die Sprachausgabe des Geräts vor.
 */
class NatuerlicheStimme(kontext: Context) {
    val ort = StimmOrt(File(kontext.applicationContext.filesDir, "stimmen"))

    private val faden = Executors.newSingleThreadExecutor { r -> Thread(r, "yem1n-stimme").apply { isDaemon = true } }
    private val haupt = Handler(Looper.getMainLooper())

    /** Nur eine Stimme bleibt geladen, jede braucht viel Arbeitsspeicher. */
    private var geladen: Pair<String, OfflineTts>? = null
    private var abspieler: AudioTrack? = null

    @Volatile private var auftrag = 0
    @Volatile private var geschlossen = false

    init {
        ort.aufraeumen()
    }

    fun verfuegbar(sprache: String): Boolean {
        val p = StimmPaket.fuer(sprache) ?: return false
        return ort.bereit(p)
    }

    /**
     * Liest [text] vor. Gibt false zurück, wenn es keine Stimme für die Sprache gibt.
     * [beiFehler] wird aufgerufen, wenn die Stimme dann doch nicht ansprang.
     */
    fun sprich(text: String, sprache: String, tempo: Float, beiFehler: () -> Unit): Boolean {
        val p = StimmPaket.fuer(sprache) ?: return false
        if (!ort.bereit(p) || geschlossen) return false
        val meiner = ++auftrag
        stoppeTon()
        faden.execute {
            if (meiner != auftrag || geschlossen) return@execute
            val ton = try {
                val tts = motor(p) ?: throw IllegalStateException("keine Stimme")
                val audio = tts.generate(text = text, sid = 0, speed = tempo)
                audio.samples to audio.sampleRate
            } catch (e: Throwable) {
                haupt.post { beiFehler() }
                return@execute
            }
            if (meiner != auftrag || geschlossen || ton.first.isEmpty()) return@execute
            spiele(ton.first, ton.second)
            planeFreigabe(meiner)
        }
        return true
    }

    fun stopp() {
        auftrag++
        stoppeTon()
    }

    /** Gibt Stimme und Speicher frei. */
    fun schliessen() {
        geschlossen = true
        auftrag++
        stoppeTon()
        faden.execute { geladen?.second?.free(); geladen = null }
        faden.shutdown()
    }

    /** Nach dem Entfernen einer Stimme muss sie auch aus dem Speicher. */
    fun vergiss(p: StimmPaket) {
        faden.execute {
            if (geladen?.first == p.id) {
                geladen?.second?.free()
                geladen = null
            }
        }
    }

    private fun motor(p: StimmPaket): OfflineTts? {
        geladen?.let { if (it.first == p.id) return it.second }
        geladen?.second?.free()
        geladen = null
        val ordner = ort.ordner(p)
        val vits = OfflineTtsVitsModelConfig(
            model = File(ordner, "stimme.onnx").path,
            tokens = File(ordner, "tokens.txt").path,
            dataDir = ort.ordner(StimmPaket.aussprache).path,
            // Etwas mehr Zufall in Klang und Rhythmus als die Vorgabe, dann klingt die Stimme lebendiger
            noiseScale = 0.8f,
            noiseScaleW = 1.0f,
        )
        val modell = OfflineTtsModelConfig(vits = vits, numThreads = 2, debug = false, provider = "cpu")
        val tts = OfflineTts(config = OfflineTtsConfig(model = modell))
        geladen = p.id to tts
        return tts
    }

    /** Läuft im Stimmen-Faden: spielt die Töne ab und kehrt zurück. */
    private fun spiele(samples: FloatArray, rate: Int) {
        val groesse = samples.size * 4
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(groesse)
            .build()
        synchronized(this) {
            abspieler?.release()
            abspieler = track
        }
        track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        track.play()
    }

    private fun stoppeTon() {
        synchronized(this) {
            try {
                abspieler?.stop()
            } catch (e: Exception) {
                // schon beendet
            }
            try {
                abspieler?.release()
            } catch (e: Exception) {
                // schon freigegeben
            }
            abspieler = null
        }
    }

    /** Wird die Stimme zwei Minuten nicht gebraucht, gibt sie den Speicher frei. */
    private fun planeFreigabe(meiner: Int) {
        haupt.postDelayed({
            if (meiner == auftrag && !geschlossen) {
                faden.execute { geladen?.second?.free(); geladen = null }
            }
        }, 120_000L)
    }
}
