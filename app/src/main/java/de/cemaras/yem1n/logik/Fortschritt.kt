package de.cemaras.yem1n.logik

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Bestes Ergebnis einer Übung. */
@Serializable
data class UebungsStand(val beste: Int = 0, val sterne: Int = 0, val runden: Int = 0)

/** Alles, was die App über den Lernfortschritt auf diesem Gerät weiß. */
@Serializable
data class Fortschritt(val eintraege: Map<String, UebungsStand> = emptyMap()) {

    fun stand(arbeitSchluessel: String, uebung: String): UebungsStand =
        eintraege[schluessel(arbeitSchluessel, uebung)] ?: UebungsStand()

    /** Trägt das Ergebnis einer fertigen Runde ein. Gespeichert wird immer das beste. */
    fun nachRunde(arbeitSchluessel: String, uebung: String, richtig: Int, gesamt: Int): Fortschritt {
        val key = schluessel(arbeitSchluessel, uebung)
        val alt = eintraege[key] ?: UebungsStand()
        val neu = UebungsStand(
            beste = maxOf(alt.beste, richtig),
            sterne = maxOf(alt.sterne, Pruefer.sterneFuer(richtig, gesamt)),
            runden = alt.runden + 1,
        )
        return copy(eintraege = eintraege + (key to neu))
    }

    fun sterneDerArbeit(arbeitSchluessel: String, uebungen: List<String>): Int =
        uebungen.sumOf { stand(arbeitSchluessel, it).sterne }

    fun zuJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun schluessel(arbeitSchluessel: String, uebung: String) = "$arbeitSchluessel#$uebung"

        /** Liest gespeicherten Fortschritt. Bei kaputtem oder leerem Text beginnt es von vorn. */
        fun ausJson(text: String?): Fortschritt =
            if (text.isNullOrBlank()) Fortschritt()
            else try {
                json.decodeFromString(serializer(), text)
            } catch (e: Exception) {
                Fortschritt()
            }
    }
}
