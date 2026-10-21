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
import de.cemaras.yem1n.logik.Sonderrunden
import de.cemaras.yem1n.logik.Stand
import de.cemaras.yem1n.ui.theme.Farbwelt
import de.cemaras.yem1n.speicher.Speicher

/** Wo die App gerade ist. */
sealed interface Ansicht {
    data object Start : Ansicht
    data class Uebungen(val arbeit: Int) : Ansicht
    data class Ueben(val arbeit: Int, val uebung: Int) : Ansicht
    data object Einstellungen : Ansicht
}

/** Runden, die nicht aus einer Arbeit der Liste kommen. */
enum class SonderArt { FEHLERHEFT, PROBE, TRAINING }

/** Platzhalter für die Arbeit, die nicht in der Liste steht (das Fehlerheft). */
private const val SONDER = -1

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val speicher = Speicher(app)

    private var alleArbeiten: List<Arbeit> = emptyList()
    private var sonderArbeit: Arbeit? = null
    private var sonderArt: SonderArt? = null

    /** Beginn der Probearbeit in Millisekunden, für die Uhr. */
    var probeStart: Long by mutableStateOf(0L)
        private set

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
        Farbwelt.rosa = fortschritt.rosa
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

    /** Die Runde kommt nicht aus der Arbeitenliste (Fehlerheft, Probearbeit oder Training). */
    val imSonder: Boolean
        get() = when (val a = ansicht) {
            is Ansicht.Ueben -> a.arbeit == SONDER
            is Ansicht.Uebungen -> a.arbeit == SONDER
            else -> false
        }
    val istFehlerheft: Boolean get() = imSonder && sonderArt == SonderArt.FEHLERHEFT
    val istProbe: Boolean get() = imSonder && sonderArt == SonderArt.PROBE

    /** (fällig, wartend) für den Knopf auf der Startseite. */
    val fehlerStand: Pair<Int, Int>
        get() = Fehlerheft.stand(arbeiten, fortschritt.fehler, Tag.heute())

    val aktuelleArbeit: Arbeit?
        get() = when (val a = ansicht) {
            is Ansicht.Uebungen -> if (a.arbeit == SONDER) sonderArbeit else arbeiten.getOrNull(a.arbeit)
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
        sonderArt = SonderArt.FEHLERHEFT
        runde = Runde.start(arbeit.uebungen[0].aufgaben)
        verdient = 0
        ansicht = Ansicht.Ueben(SONDER, 0)
    }

    /** Zwanzig neue gemischte Aufgaben mit Uhr, am Ende gibt es eine Note. */
    fun startProbe() {
        val arbeit = Sonderrunden.probe(arbeiten)
        sonderArbeit = arbeit
        sonderArt = SonderArt.PROBE
        runde = Runde.start(arbeit.uebungen[0].aufgaben)
        verdient = 0
        probeStart = System.currentTimeMillis()
        ansicht = Ansicht.Ueben(SONDER, 0)
    }

    /** Neue Aufgaben zu den zwei Bereichen, die am schwersten fallen. */
    fun startTraining() {
        sonderArbeit = Sonderrunden.training(arbeiten, fortschritt.anteile())
        sonderArt = SonderArt.TRAINING
        ansicht = Ansicht.Uebungen(SONDER)
    }

    fun startUebung(uebung: Int) {
        val a = ansicht as? Ansicht.Uebungen ?: return
        val u = aktuelleArbeit?.uebungen?.getOrNull(uebung) ?: return
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
                ansicht = if (a.arbeit != SONDER || sonderArt == SonderArt.TRAINING) {
                    Ansicht.Uebungen(a.arbeit)
                } else {
                    Ansicht.Start
                }
            }
            is Ansicht.Uebungen, Ansicht.Einstellungen -> ansicht = Ansicht.Start
            Ansicht.Start -> Unit
        }
    }

    // Üben

    fun antworte(eingabe: String, eingabe2: String = "") {
        val alt = runde ?: return
        val neu = Runde.antworte(alt, eingabe, eingabe2)
        runde = neu
        vermerke(alt, neu)
    }

    fun zeigeLoesung() {
        runde = runde?.let { Runde.zeigeLoesung(it) }
    }

    /** Merkt sich Fehler sofort, damit sie auch bei einer abgebrochenen Runde ins Fehlerheft kommen. */
    private fun vermerke(alt: RundenStand, neu: RundenStand) {
        val i = alt.index
        val vorher = alt.staende.getOrNull(i) ?: return
        val nachher = neu.staende.getOrNull(i) ?: return
        if (vorher != Stand.OFFEN || (nachher != Stand.RICHTIG && nachher != Stand.FALSCH)) return
        val key = schluesselVon(neu, i) ?: return
        behalte(fortschritt.nachAntwort(istFehlerheft, key, nachher, Tag.heute()))
    }

    private fun schluesselVon(s: RundenStand, i: Int): String? {
        if (imSonder) return s.aufgaben.getOrNull(i)?.quellKey?.ifEmpty { null }
        val arbeit = aktuelleArbeit ?: return null
        val uebung = aktuelleUebung ?: return null
        return Fehlerheft.schluessel(arbeit.schluessel, uebung.titel, i)
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
        val (neu, muenzen) = fortschritt.nachRundeMit(
            imSonder, arbeit.schluessel, uebung.titel, s.staende, Tag.heute(),
        )
        verdient = muenzen
        behalte(neu)
    }

    // Einstellungen

    fun setzeName(name: String) = behalte(fortschritt.copy(name = name.trim().take(30)))

    fun setzeFarbwelt(rosa: Boolean) {
        Farbwelt.rosa = rosa
        behalte(fortschritt.copy(rosa = rosa))
    }

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
