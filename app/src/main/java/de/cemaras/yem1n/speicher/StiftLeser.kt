package de.cemaras.yem1n.speicher

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink

/** Ein Punkt eines Strichs: Ort und Zeit in Millisekunden. */
class StiftPunkt(val x: Float, val y: Float, val t: Long)

/**
 * Liest Ziffern aus Strichen mit der Handschrifterkennung von Google (ML Kit).
 * Das Modell (etwa 20 MB) wird einmal geladen, danach läuft alles auf dem Gerät.
 */
class StiftLeser {
    enum class Stand { UNBEKANNT, FEHLT, LAEDT, BEREIT, FEHLER }

    @Volatile var stand: Stand = Stand.UNBEKANNT
        private set

    private val modell: DigitalInkRecognitionModel? = runCatching {
        DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")?.let { DigitalInkRecognitionModel.builder(it).build() }
    }.getOrNull()
    private var erkenner: DigitalInkRecognizer? = null

    /** Schaut nach, ob das Modell schon auf dem Gerät liegt. */
    fun pruefen(fertig: (Stand) -> Unit) {
        val m = modell ?: run { stand = Stand.FEHLER; fertig(stand); return }
        runCatching {
            RemoteModelManager.getInstance().isModelDownloaded(m)
                .addOnSuccessListener { da -> stand = if (da) Stand.BEREIT else Stand.FEHLT; fertig(stand) }
                .addOnFailureListener { stand = Stand.FEHLER; fertig(stand) }
        }.onFailure { stand = Stand.FEHLER; fertig(stand) }
    }

    /** Lädt das Modell, nur nach Wunsch des Nutzers. */
    fun laden(fertig: (Stand) -> Unit) {
        val m = modell ?: run { stand = Stand.FEHLER; fertig(stand); return }
        stand = Stand.LAEDT
        fertig(stand)
        runCatching {
            RemoteModelManager.getInstance().download(m, DownloadConditions.Builder().build())
                .addOnSuccessListener { stand = Stand.BEREIT; fertig(stand) }
                .addOnFailureListener { stand = Stand.FEHLER; fertig(stand) }
        }.onFailure { stand = Stand.FEHLER; fertig(stand) }
    }

    /** Liest die Striche. [ergebnis] bekommt die Vorschläge, bester zuerst, oder eine leere Liste. */
    fun lesen(striche: List<List<StiftPunkt>>, ergebnis: (List<String>) -> Unit) {
        val m = modell
        if (m == null || stand != Stand.BEREIT || striche.isEmpty()) { ergebnis(emptyList()); return }
        val e = erkenner ?: DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(m).build()).also { erkenner = it }
        val ink = Ink.builder().apply {
            for (strich in striche) {
                if (strich.isEmpty()) continue
                addStroke(Ink.Stroke.builder().apply { for (p in strich) addPoint(Ink.Point.create(p.x, p.y, p.t)) }.build())
            }
        }.build()
        runCatching {
            e.recognize(ink)
                .addOnSuccessListener { r -> ergebnis(r.candidates.map { it.text }) }
                .addOnFailureListener { ergebnis(emptyList()) }
        }.onFailure { ergebnis(emptyList()) }
    }

    fun schliessen() {
        runCatching { erkenner?.close() }
        erkenner = null
    }
}
