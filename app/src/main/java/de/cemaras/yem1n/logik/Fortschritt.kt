package de.cemaras.yem1n.logik

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Bestes Ergebnis einer Übung. */
@Serializable
data class UebungsStand(val beste: Int = 0, val sterne: Int = 0, val runden: Int = 0)

/** Wie viele Aufgaben eines Bereichs bisher richtig waren, für das Training. */
@Serializable
data class Leistung(val richtig: Int = 0, val gesamt: Int = 0)

/** Eine fertige Runde im Verlauf, für Abzeichen und Wochenziel. */
@Serializable
data class RundenEintrag(
    val fach: String = "",
    val uebung: String = "",
    val richtig: Int = 0,
    val gesamt: Int = 0,
    val angesehen: Int = 0,
    val tag: Long = 0,
    val stunde: Int = 12,
) {
    val sterne: Int get() = Pruefer.sterneFuer(richtig, gesamt)
}

/** Alles, was die App über den Lernfortschritt auf diesem Gerät weiß. */
@Serializable
data class Fortschritt(
    val eintraege: Map<String, UebungsStand> = emptyMap(),
    /** Fehler, die noch geübt werden müssen. Schlüssel siehe [Fehlerheft.schluessel]. */
    val fehler: Map<String, FehlerEintrag> = emptyMap(),
    val muenzen: Int = 0,
    /** Tagesnummern (siehe [Tag]), an denen mindestens eine Runde fertig wurde. */
    val tage: List<Long> = emptyList(),
    val letzterTag: Long = 0,
    val rundenAmLetztenTag: Int = 0,
    val name: String = "",
    val tagesziel: Int = 1,
    /** Arbeiten, die das Kind nicht mehr sehen soll (mitgelieferte lassen sich nicht löschen). */
    val versteckt: List<String> = emptyList(),
    /** Richtig und gesamt je Übungsname (zum Beispiel "Einmaleins"), über alle Runden. */
    val leistung: Map<String, Leistung> = emptyMap(),
    val rosa: Boolean = false,
    /** Die letzten fertigen Runden (höchstens [MAX_RUNDEN]). */
    val runden: List<RundenEintrag> = emptyList(),
    /** Anfangstag der Woche, für die die Wochenbelohnung schon abgeholt wurde. */
    val belohnungWoche: Long = -1,
    /** Zahlenraum in der Vorschule: 1 bis 5, 2 bis 10, 3 bis 20. */
    val vorschulStufe: Int = 2,
    /** Sprache der Vorschule ("de", "en" oder "tr") und ob Englisch und Türkisch angeboten werden. */
    val vorschulSprache: String = "de",
    val vorschulEnglisch: Boolean = false,
    val vorschulTuerkisch: Boolean = false,
    /** Liest die App Fragen und Lob vor? */
    val vorlesen: Boolean = true,
    /** Bester Punktestand je Spiel, Schlüssel siehe [SpielArt.schluessel]. */
    val bestwerte: Map<String, Int> = emptyMap(),
    /** Tag, an dem zuletzt ein Spiel bezahlt wurde, und wie viele es an dem Tag waren. */
    val spieleTag: Long = 0,
    val spieleAnzahl: Int = 0,
    /** Eltern können die Spiele ausschalten oder die Zahl pro Tag begrenzen. */
    val spieleErlaubt: Boolean = true,
    val spieleProTag: Int = 3,
    /** Töne und Vibration in den Spielen. */
    val spielTon: Boolean = true,
    val spielHaptik: Boolean = true,
    /** Das Haustier, null solange noch keins gewählt wurde. */
    val haustier: Haustier? = null,
) {

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

    /**
     * Merkt sich sofort, wenn eine Aufgabe richtig oder falsch beantwortet wurde, auch wenn die
     * Runde danach nicht zu Ende gespielt wird. [key] ist der Schlüssel der Originalaufgabe.
     * Im Fehlerheft ([fehlerheft] = true) zählt eine richtige Antwort erst nach der zweiten Wiederholung.
     */
    fun nachAntwort(fehlerheft: Boolean, key: String, stand: Stand, heute: Long): Fortschritt {
        if (key.isEmpty() || (stand != Stand.FALSCH && stand != Stand.RICHTIG)) return this
        val offen = fehler.toMutableMap()
        if (stand == Stand.FALSCH) {
            if (fehlerheft) {
                offen[key] = Fehlerplan.falsch(heute)
            } else if (key !in offen) {
                offen[key] = FehlerEintrag()
            }
        } else if (fehlerheft) {
            val neu = Fehlerplan.richtig(offen[key] ?: FehlerEintrag(), heute)
            if (neu == null) offen.remove(key) else offen[key] = neu
        } else {
            offen.remove(key)
        }
        return copy(fehler = offen)
    }

    /**
     * Trägt eine fertige Runde ein: Sterne, Münzen, Serie und Tagesziel. Bei Sonderrunden
     * (Fehlerheft, Probearbeit, Training, [fehlerheft] = true) gibt es keine Sterne. Die Fehler sind schon
     * bei jeder Antwort eingetragen worden. Gibt den neuen Stand und die verdienten Münzen zurück.
     */
    fun nachRundeMit(
        fehlerheft: Boolean, arbeitSchluessel: String, uebung: String,
        staende: List<Stand>, heute: Long, fach: String = "", stunde: Int = 12,
        jetzt: Long = System.currentTimeMillis(),
    ): Pair<Fortschritt, Int> {
        val richtig = staende.count { it == Stand.RICHTIG }
        val angeschaut = staende.count { it == Stand.ANGESCHAUT }
        val verdient = Muenzen.verdient(richtig, staende.size, angeschaut)
        val basis = if (fehlerheft) this else nachRunde(arbeitSchluessel, uebung, richtig, staende.size)
        val alt = leistung[uebung] ?: Leistung()
        return basis.copy(
            leistung = leistung + (uebung to Leistung(alt.richtig + richtig, alt.gesamt + staende.size)),
            runden = (runden + RundenEintrag(fach, uebung, richtig, staende.size, angeschaut, heute, stunde))
                .takeLast(MAX_RUNDEN),
            muenzen = muenzen + verdient,
            tage = if (heute in tage) tage else tage + heute,
            letzterTag = heute,
            rundenAmLetztenTag = rundenHeute(heute) + 1,
            haustier = haustier?.lernrunde(richtig, staende.size, angeschaut, jetzt)?.first,
        ) to verdient
    }

    /** Anteil richtiger Aufgaben je Übungsname von 0.0 bis 1.0. */
    fun anteile(): Map<String, Double> =
        leistung.filter { it.value.gesamt > 0 }.mapValues { it.value.richtig.toDouble() / it.value.gesamt }

    /** Wie viele Spiele der Vorschule schon mit mindestens einem Stern geschafft sind. */
    fun vorschulStempel(): Set<VorschulArt> =
        VorschulArt.stationen.filter { stand(VORSCHUL_ARBEIT, it.uebungName).sterne >= 1 }.toSet()

    fun spieleHeute(heute: Long): Int = if (spieleTag == heute) spieleAnzahl else 0

    /** Mindestens eine Übungsrunde heute fertig? Erst dann darf gespielt werden. */
    fun heuteGeuebt(heute: Long): Boolean = rundenHeute(heute) > 0

    /** Zieht den Preis für ein Spiel ab und zählt es für heute mit. */
    fun spielBezahlt(heute: Long): Fortschritt =
        copy(muenzen = muenzen - SpielRegeln.PREIS, spieleTag = heute, spieleAnzahl = spieleHeute(heute) + 1)

    /** Merkt sich den Punktestand, wenn er der beste ist. Zweiter Wert: neuer Rekord? */
    fun mitBestwert(spiel: String, punkte: Int): Pair<Fortschritt, Boolean> {
        if (punkte <= (bestwerte[spiel] ?: 0)) return this to false
        return copy(bestwerte = bestwerte + (spiel to punkte)) to true
    }

    fun rundenHeute(heute: Long): Int = if (letzterTag == heute) rundenAmLetztenTag else 0

    fun serie(heute: Long): Pair<Int, Int> = Serie.mitSchutz(tage.toSet(), heute)

    fun sterneDerArbeit(arbeitSchluessel: String, uebungen: List<String>): Int =
        uebungen.sumOf { stand(arbeitSchluessel, it).sterne }

    fun zuJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val MAX_RUNDEN = 2000

        /** Schlüssel der Vorschul-Spiele im Fortschritt, wie "klasse|fach|titel" einer Arbeit. */
        const val VORSCHUL_ARBEIT = "Vorschule|Vorschule|Übungen"

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
