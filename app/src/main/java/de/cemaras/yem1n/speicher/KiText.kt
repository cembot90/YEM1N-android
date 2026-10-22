package de.cemaras.yem1n.speicher

import android.os.Build
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.collect

/**
 * Gemini Nano auf dem Gerät (ML Kit GenAI Prompt API, noch Beta). Es geht nichts ins Internet.
 * Auf Geräten ohne das Modell oder vor Android 8 gibt es null, dann bleibt der feste Satz.
 */
object KiText {
    @Volatile private var ladeVersucht = false

    suspend fun formuliere(prompt: String): String? {
        if (Build.VERSION.SDK_INT < 26) return null
        return try {
            withTimeoutOrNull(20_000) {
                withContext(Dispatchers.Default) {
                    val modell = Generation.getClient()
                    when (modell.checkStatus()) {
                        FeatureStatus.AVAILABLE -> modell.generateContent(prompt).candidates.firstOrNull()?.text
                        FeatureStatus.DOWNLOADABLE -> {
                            // Das Modell wird einmal im Hintergrund geladen, diesmal gilt noch der feste Satz.
                            if (!ladeVersucht) {
                                ladeVersucht = true
                                modell.download().collect { }
                            }
                            null
                        }
                        else -> null
                    }
                }
            }
        } catch (e: Throwable) {
            null
        }
    }
}
