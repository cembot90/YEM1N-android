package de.cemaras.yem1n.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.PaketLeser
import de.cemaras.yem1n.daten.Uebung
import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Runde
import de.cemaras.yem1n.logik.RundenStand
import de.cemaras.yem1n.speicher.Speicher

/** Wo die App gerade ist. */
sealed interface Ansicht {
    data object Start : Ansicht
    data class Uebungen(val arbeit: Int) : Ansicht
    data class Ueben(val arbeit: Int, val uebung: Int) : Ansicht
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val speicher = Speicher(app)

    var arbeiten: List<Arbeit> by mutableStateOf(emptyList())
        private set
    var fortschritt: Fortschritt by mutableStateOf(Fortschritt())
        private set
    var ansicht: Ansicht by mutableStateOf(Ansicht.Start)
        private set
    var runde: RundenStand? by mutableStateOf(null)
        private set

    init {
        arbeiten = speicher.ladeArbeiten()
        fortschritt = speicher.ladeFortschritt()
    }

    val aktuelleArbeit: Arbeit?
        get() = when (val a = ansicht) {
            is Ansicht.Uebungen -> arbeiten.getOrNull(a.arbeit)
            is Ansicht.Ueben -> arbeiten.getOrNull(a.arbeit)
            Ansicht.Start -> null
        }

    val aktuelleUebung: Uebung?
        get() = (ansicht as? Ansicht.Ueben)?.let { arbeiten.getOrNull(it.arbeit)?.uebungen?.getOrNull(it.uebung) }

    // Navigation

    fun oeffne(arbeit: Int) {
        ansicht = Ansicht.Uebungen(arbeit)
    }

    fun startUebung(uebung: Int) {
        val a = ansicht as? Ansicht.Uebungen ?: return
        val u = arbeiten.getOrNull(a.arbeit)?.uebungen?.getOrNull(uebung) ?: return
        runde = Runde.start(u.aufgaben)
        ansicht = Ansicht.Ueben(a.arbeit, uebung)
    }

    /** Dieselbe Übung noch einmal von vorn. */
    fun nochmal() {
        val a = ansicht as? Ansicht.Ueben ?: return
        val u = aktuelleUebung ?: return
        runde = Runde.start(u.aufgaben)
        ansicht = a
    }

    fun zurueck() {
        when (val a = ansicht) {
            is Ansicht.Ueben -> {
                runde = null
                ansicht = Ansicht.Uebungen(a.arbeit)
            }
            is Ansicht.Uebungen -> ansicht = Ansicht.Start
            Ansicht.Start -> Unit
        }
    }

    // Üben

    fun antworte(eingabe: String, eingabe2: String = "") {
        runde = runde?.let { Runde.antworte(it, eingabe, eingabe2) }
    }

    fun zeigeLoesung() {
        runde = runde?.let { Runde.zeigeLoesung(it) }
    }

    fun weiter() {
        val neu = runde?.let { Runde.weiter(it) } ?: return
        runde = neu
        if (neu.fertig) rundeSpeichern(neu)
    }

    private fun rundeSpeichern(s: RundenStand) {
        val arbeit = aktuelleArbeit ?: return
        val uebung = aktuelleUebung ?: return
        fortschritt = fortschritt.nachRunde(arbeit.schluessel, uebung.titel, s.richtig, s.aufgaben.size)
        speicher.speichereFortschritt(fortschritt)
    }

    // Arbeiten einfügen

    /** Liest eingefügtes JSON. Gibt null zurück, wenn es geklappt hat, sonst den Fehlertext. */
    fun fuegeArbeitEin(text: String): String? {
        val ergebnis = PaketLeser.lese(text)
        val arbeit = ergebnis.arbeit ?: return ergebnis.fehler
        if (arbeiten.any { it.schluessel == arbeit.schluessel }) {
            return "\"${arbeit.titel}\" ist schon vorhanden."
        }
        speicher.speichereArbeit(arbeit, text)
        arbeiten = arbeiten + arbeit
        return null
    }
}
