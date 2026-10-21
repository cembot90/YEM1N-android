package de.cemaras.yem1n.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.PaketLeser
import de.cemaras.yem1n.daten.Uebung
import de.cemaras.yem1n.logik.Fehlerheft
import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Tag
import de.cemaras.yem1n.logik.Runde
import de.cemaras.yem1n.logik.RundenStand
import de.cemaras.yem1n.speicher.Speicher

/** Wo die App gerade ist. */
sealed interface Ansicht {
    data object Start : Ansicht
    data class Uebungen(val arbeit: Int) : Ansicht
    data class Ueben(val arbeit: Int, val uebung: Int) : Ansicht
    data object Einstellungen : Ansicht
}

/** Platzhalter für die Arbeit, die nicht in der Liste steht (das Fehlerheft). */
private const val SONDER = -1

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val speicher = Speicher(app)

    private var alleArbeiten: List<Arbeit> = emptyList()
    private var sonderArbeit: Arbeit? = null

    /** Die sichtbaren Arbeiten (ohne die, die das Kind gelöscht hat). */
    var arbeiten: List<Arbeit> by mutableStateOf(emptyList())
        private set
    /** Fach, nach dem die Startseite gefiltert ist, oder null für alle. */
    var fachFilter: String? by mutableStateOf(null)
        private set
    /** Münzen, die die letzte fertige Runde gebracht hat. */
    var verdient: Int by mutableStateOf(0)
        private set
    var fortschritt: Fortschritt by mutableStateOf(Fortschritt())
        private set
    var ansicht: Ansicht by mutableStateOf(Ansicht.Start)
        private set
    var runde: RundenStand? by mutableStateOf(null)
        private set

    init {
        alleArbeiten = speicher.ladeArbeiten()
        fortschritt = speicher.ladeFortschritt()
        sichtbarMachen()
    }

    private fun sichtbarMachen() {
        arbeiten = alleArbeiten.filter { it.schluessel !in fortschritt.versteckt }
        if (fachFilter != null && arbeiten.none { it.fach == fachFilter }) fachFilter = null
    }

    private fun behalte(neu: Fortschritt) {
        fortschritt = neu
        speicher.speichereFortschritt(neu)
    }

    val faecher: List<String> get() = arbeiten.map { it.fach }.distinct()

    fun waehleFach(fach: String?) {
        fachFilter = fach
    }

    val istFehlerheft: Boolean get() = (ansicht as? Ansicht.Ueben)?.arbeit == SONDER

    /** (fällig, wartend) für den Knopf auf der Startseite. */
    val fehlerStand: Pair<Int, Int>
        get() = Fehlerheft.stand(arbeiten, fortschritt.fehler, Tag.heute())

    val aktuelleArbeit: Arbeit?
        get() = when (val a = ansicht) {
            is Ansicht.Uebungen -> arbeiten.getOrNull(a.arbeit)
            is Ansicht.Ueben -> if (a.arbeit == SONDER) sonderArbeit else arbeiten.getOrNull(a.arbeit)
            Ansicht.Start, Ansicht.Einstellungen -> null
        }

    val aktuelleUebung: Uebung?
        get() = (ansicht as? Ansicht.Ueben)?.let { aktuelleArbeit?.uebungen?.getOrNull(it.uebung) }

    // Navigation

    fun oeffne(arbeit: Int) {
        ansicht = Ansicht.Uebungen(arbeit)
    }

    fun oeffneEinstellungen() {
        ansicht = Ansicht.Einstellungen
    }

    /** Startet das Fehlerheft mit den Fehlern, die jetzt dran sind. */
    fun startFehlerheft() {
        val arbeit = Fehlerheft.baue(arbeiten, fortschritt.fehler, Tag.heute()) ?: return
        sonderArbeit = arbeit
        runde = Runde.start(arbeit.uebungen[0].aufgaben)
        verdient = 0
        ansicht = Ansicht.Ueben(SONDER, 0)
    }

    fun startUebung(uebung: Int) {
        val a = ansicht as? Ansicht.Uebungen ?: return
        val u = arbeiten.getOrNull(a.arbeit)?.uebungen?.getOrNull(uebung) ?: return
        runde = Runde.start(u.aufgaben)
        verdient = 0
        ansicht = Ansicht.Ueben(a.arbeit, uebung)
    }

    /** Dieselbe Übung noch einmal von vorn. */
    fun nochmal() {
        val a = ansicht as? Ansicht.Ueben ?: return
        val u = aktuelleUebung ?: return
        runde = Runde.start(u.aufgaben)
        verdient = 0
        ansicht = a
    }

    fun zurueck() {
        when (val a = ansicht) {
            is Ansicht.Ueben -> {
                runde = null
                ansicht = if (a.arbeit == SONDER) Ansicht.Start else Ansicht.Uebungen(a.arbeit)
            }
            is Ansicht.Uebungen, Ansicht.Einstellungen -> ansicht = Ansicht.Start
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
        val alt = runde ?: return
        if (alt.fertig) return
        val neu = Runde.weiter(alt)
        runde = neu
        if (neu.fertig) rundeSpeichern(neu)
    }

    private fun rundeSpeichern(s: RundenStand) {
        val arbeit = aktuelleArbeit ?: return
        val uebung = aktuelleUebung ?: return
        val sonder = istFehlerheft
        val keys = if (sonder) s.aufgaben.map { it.quellKey }
        else s.aufgaben.indices.map { Fehlerheft.schluessel(arbeit.schluessel, uebung.titel, it) }
        val (neu, muenzen) = fortschritt.nachRundeMit(
            sonder, arbeit.schluessel, uebung.titel, keys, s.staende, Tag.heute(),
        )
        verdient = muenzen
        behalte(neu)
    }

    // Einstellungen

    fun setzeName(name: String) = behalte(fortschritt.copy(name = name.trim().take(30)))

    fun setzeTagesziel(ziel: Int) = behalte(fortschritt.copy(tagesziel = ziel.coerceIn(1, 5)))

    /** Blendet eine Arbeit aus und löscht ihre Datei, falls sie eingefügt wurde. */
    fun loescheArbeit(arbeit: Arbeit) {
        speicher.loescheArbeit(arbeit.schluessel)
        behalte(fortschritt.copy(versteckt = (fortschritt.versteckt + arbeit.schluessel).distinct()))
        sichtbarMachen()
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
        if (arbeit.schluessel in fortschritt.versteckt) {
            behalte(fortschritt.copy(versteckt = fortschritt.versteckt - arbeit.schluessel))
        }
        if (alleArbeiten.none { it.schluessel == arbeit.schluessel }) alleArbeiten = alleArbeiten + arbeit
        sichtbarMachen()
        return null
    }
}
