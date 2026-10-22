package de.cemaras.yem1n.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.cemaras.yem1n.logik.StimmPaket
import de.cemaras.yem1n.speicher.StimmenLader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.daten.PaketLeser
import de.cemaras.yem1n.daten.Uebung
import de.cemaras.yem1n.logik.EnglischThema
import de.cemaras.yem1n.logik.EnglischWort
import de.cemaras.yem1n.logik.Erfolge
import de.cemaras.yem1n.logik.VorschulEnglisch
import de.cemaras.yem1n.logik.Fehlerheft
import de.cemaras.yem1n.logik.Pokal
import de.cemaras.yem1n.logik.Fortschritt
import de.cemaras.yem1n.logik.Haustier
import de.cemaras.yem1n.speicher.CloudClient
import de.cemaras.yem1n.speicher.ServerClient
import de.cemaras.yem1n.logik.Familiencode
import de.cemaras.yem1n.logik.JokerCloud
import de.cemaras.yem1n.logik.JokerFrage
import de.cemaras.yem1n.logik.ServerAnfrage
import de.cemaras.yem1n.logik.CloudAdresse
import de.cemaras.yem1n.speicher.Erinnerung
import de.cemaras.yem1n.speicher.Mitteilungen
import de.cemaras.yem1n.speicher.HaustierWidget
import de.cemaras.yem1n.logik.HaustierHut
import de.cemaras.yem1n.logik.Lob
import de.cemaras.yem1n.logik.MemoryStand
import de.cemaras.yem1n.logik.SpielArt
import de.cemaras.yem1n.logik.SpielRegeln
import de.cemaras.yem1n.logik.VorschulArt
import de.cemaras.yem1n.logik.VorschulGenerator
import de.cemaras.yem1n.logik.VorschulPaket
import de.cemaras.yem1n.logik.VorschulSprache
import de.cemaras.yem1n.logik.VorschulStand
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
    data object Erfolge : Ansicht
    data object Info : Ansicht
    data object Vorschule : Ansicht
    data class VorschulPakete(val arbeit: Int) : Ansicht
    data object Spiele : Ansicht
    data object HaustierSeite : Ansicht
    data object Joker : Ansicht
    data object ErgebnisseEltern : Ansicht
    data class Spiel(val art: SpielArt) : Ansicht
    data class VorschulSpiel(val art: VorschulArt) : Ansicht
    data class Englisch(val thema: EnglischThema) : Ansicht
    data class InfoSeite(val seite: de.cemaras.yem1n.ui.InfoSeite) : Ansicht
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
    /** Abzeichen, die die letzte fertige Runde freigeschaltet hat. */
    var neuePokale: List<Pokal> by mutableStateOf(emptyList())
        private set
    /** Meldung zu einer Arbeit, die von einer anderen App hereingeschickt wurde. */
    var meldung: String? by mutableStateOf(null)
        private set
    var fortschritt: Fortschritt by mutableStateOf(Fortschritt())
        private set
    var ansicht: Ansicht by mutableStateOf(Ansicht.Start)
        private set
    var runde: RundenStand? by mutableStateOf(null)
        private set

    /** Die laufende Runde in der Vorschule (Fragen) oder Memory, sonst null. */
    var vorschul: VorschulStand? by mutableStateOf(null)
        private set
    var memory: MemoryStand? by mutableStateOf(null)
        private set
    private var vorschulArt: VorschulArt = VorschulArt.MIX
    /** Vorlesen mit der Sprachausgabe des Geräts. */
    val sprecher = Sprecher(app)

    /** Stand der Stimmenpakete: Prozent beim Laden (0 bis 100), -1 bei einem Fehler. Fehlt der Eintrag, ist nichts im Gange. */
    val stimmenLaden = mutableStateMapOf<String, Int>()
    var stimmenFehler: String? by mutableStateOf(null)
        private set
    private var stimmenZaehler by mutableStateOf(0)

    fun stimmeDa(sprache: String): Boolean {
        @Suppress("UNUSED_VARIABLE") val abonniert = stimmenZaehler
        return sprecher.natuerlich.verfuegbar(sprache)
    }

    /** Lädt die Aussprachedaten (falls nötig) und die Stimme für die Sprache. */
    fun ladeStimme(sprache: String) {
        val paket = StimmPaket.fuer(sprache) ?: return
        if (stimmenLaden.containsKey(paket.id)) return
        stimmenFehler = null
        stimmenLaden[paket.id] = 0
        viewModelScope.launch {
            val ort = sprecher.natuerlich.ort
            val zwischen = java.io.File(getApplication<Application>().cacheDir, "stimmen")
            val fehler = withContext(Dispatchers.IO) {
                var f: String? = null
                val noetig = listOf(StimmPaket.aussprache, paket).filter { !ort.installiert(it) }
                val gesamt = noetig.sumOf { it.bytes }.coerceAtLeast(1)
                var davor = 0L
                for (p in noetig) {
                    f = StimmenLader.lade(p, ort, zwischen) { anteil ->
                        stimmenLaden[paket.id] = ((davor + p.bytes * anteil) * 100 / gesamt).toInt()
                    }
                    if (f != null) break
                    davor += p.bytes
                }
                f
            }
            stimmenLaden.remove(paket.id)
            stimmenFehler = fehler
            stimmenZaehler += 1
        }
    }

    fun entferneStimme(sprache: String) {
        val paket = StimmPaket.fuer(sprache) ?: return
        sprecher.natuerlich.stopp()
        sprecher.natuerlich.vergiss(paket)
        sprecher.natuerlich.ort.entfernen(paket)
        // Sind keine Stimmen mehr da, braucht es auch die Aussprachedaten nicht
        if (StimmPaket.stimmen.none { sprecher.natuerlich.ort.installiert(it) }) {
            sprecher.natuerlich.ort.entfernen(StimmPaket.aussprache)
        }
        stimmenZaehler += 1
    }

    fun probeStimme(sprache: String) {
        val text = when (sprache) {
            "tr" -> "Merhaba! Sana böyle kitap okurum."
            "en" -> "Hello! This is how I sound when I read to you."
            else -> "Hallo! So klinge ich, wenn ich dir vorlese."
        }
        sprecher.probe(text, sprache)
    }

    init {
        alleArbeiten = speicher.ladeArbeiten()
        fortschritt = speicher.ladeFortschritt()
        sprecher.an = fortschritt.vorlesen
        SpielEffekte.ton = fortschritt.spielTon
        val schwach = (app.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)?.isLowRamDevice == true
        de.cemaras.yem1n.ui.theme.Sparmodus.an = fortschritt.sparmodus || schwach
        Erinnerung.planen(app, fortschritt.haustier, fortschritt.haustierErinnerung)
        Erinnerung.planenLernzeit(app, fortschritt.lernzeitAn, fortschritt.lernzeitMinuten, fortschritt.rundenHeute(Tag.heute()) > 0)
        offeneErgebnisseSenden()
        Mitteilungen.kanalAnlegen(app)
        geraetMelden()
        SpielEffekte.haptik = fortschritt.spielHaptik
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
        Erinnerung.planen(getApplication(), neu.haustier, neu.haustierErinnerung)
        Erinnerung.planenLernzeit(getApplication(), neu.lernzeitAn, neu.lernzeitMinuten, neu.rundenHeute(Tag.heute()) > 0)
        HaustierWidget.aktualisieren(getApplication(), neu)
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
            Ansicht.Start, Ansicht.Einstellungen, Ansicht.Erfolge, Ansicht.Info, is Ansicht.InfoSeite,
            Ansicht.Vorschule, is Ansicht.VorschulPakete, is Ansicht.VorschulSpiel, is Ansicht.Englisch, Ansicht.Spiele, is Ansicht.Spiel, Ansicht.HaustierSeite, Ansicht.Joker, Ansicht.ErgebnisseEltern -> null
        }

    val aktuelleUebung: Uebung?
        get() = (ansicht as? Ansicht.Ueben)?.let { aktuelleArbeit?.uebungen?.getOrNull(it.uebung) }

    // Navigation

    fun oeffne(arbeit: Int) {
        ansicht = Ansicht.Uebungen(arbeit)
    }

    fun oeffneErfolge() {
        ansicht = Ansicht.Erfolge
    }

    // Spiele

    /** True, wenn das Spiel gerade einen neuen Bestwert geschafft hat. */
    var rekord: Boolean by mutableStateOf(false)
        private set

    /** Grund, warum gerade nicht gespielt werden kann, sonst null. */
    val spielSperre: String? get() = SpielRegeln.sperre(fortschritt, Tag.heute())

    fun oeffneSpiele() {
        ansicht = Ansicht.Spiele
    }

    /** Bezahlt ein Spiel mit Münzen und öffnet es, wenn gerade gespielt werden darf. */
    fun startSpiel(art: SpielArt) {
        if (!art.verfuegbar || spielSperre != null) return
        behalte(fortschritt.spielBezahlt(Tag.heute()))
        rekord = false
        ansicht = Ansicht.Spiel(art)
    }

    fun spielEnde(art: SpielArt, punkte: Int) {
        val (neu, istRekord) = fortschritt.mitBestwert(art.schluessel, punkte)
        rekord = istRekord
        if (istRekord) behalte(neu)
    }

    fun setzeSpieleErlaubt(erlaubt: Boolean) = behalte(fortschritt.copy(spieleErlaubt = erlaubt))

    fun setzeSpieleProTag(anzahl: Int) = behalte(fortschritt.copy(spieleProTag = anzahl.coerceIn(1, 10)))

    // Vorschule

    fun oeffneVorschule() {
        ansicht = Ansicht.Vorschule
    }

    fun setzeVorschulStufe(stufe: Int) = behalte(fortschritt.copy(vorschulStufe = stufe.coerceIn(1, 3)))

    /** Die Sprache der Vorschule, die gerade gilt. */
    val vorschulSpracheAktiv: String
        get() = VorschulSprache.aktiv(fortschritt.vorschulSprache, fortschritt.vorschulEnglisch, fortschritt.vorschulTuerkisch)

    fun setzeVorschulSprache(sprache: String) = behalte(fortschritt.copy(vorschulSprache = sprache))

    fun setzeVorschulEnglisch(an: Boolean) = behalte(fortschritt.copy(vorschulEnglisch = an))

    fun setzeVorschulTuerkisch(an: Boolean) = behalte(fortschritt.copy(vorschulTuerkisch = an))

    /** Das Thema, wenn gerade eine Runde "Englisch für Kleine" läuft, sonst null. */
    var englischThema: EnglischThema? by mutableStateOf(null)
        private set

    fun oeffneEnglisch(thema: EnglischThema) {
        sprecher.sprich(thema.titelEn, sprache = "en")
        ansicht = Ansicht.Englisch(thema)
    }

    /** Ein Wort anhören und nachsprechen. */
    fun sageWort(w: EnglischWort) = sprecher.sprich(VorschulEnglisch.anhoeren(w), sprache = "en")

    fun startEnglisch(thema: EnglischThema) {
        paketStand = null
        englischThema = thema
        verdient = 0
        haustierText = null
        neuePokale = emptyList()
        memory = null
        val s = VorschulStand(VorschulEnglisch.fragen(thema))
        vorschul = s
        s.aktuelle?.let { sageFrage(it.text) }
        ansicht = Ansicht.VorschulSpiel(VorschulArt.MIX)
    }

    /** Liest eine Frage in der gewählten Sprache vor. */
    private fun sageFrage(t: de.cemaras.yem1n.logik.VorschulText) {
        if (englischThema != null) {
            sprecher.sprich(t.en ?: t.de, sprache = "en")
            return
        }
        val wunsch = vorschulSpracheAktiv
        sprecher.sprich(t.text(wunsch), sprache = t.sprache(wunsch))
    }

    // Cloud (Apple-ID)

    private val cloud = CloudClient(app)

    /** Kurzer Text für die Einstellungen: was der letzte Cloud-Test ergeben hat. */
    var cloudText: String by mutableStateOf(if (cloud.angemeldet) "Mit Apple-ID angemeldet." else "Noch nicht angemeldet.")
        private set
    var cloudAngemeldet: Boolean by mutableStateOf(cloud.angemeldet)
        private set
    var cloudBeschaeftigt: Boolean by mutableStateOf(false)
        private set

    /** Adresse, die im Browser geöffnet werden soll (Apple-Anmeldung), sonst null. Die Oberfläche öffnet sie einmal. */
    var cloudBrowserUrl: String? by mutableStateOf(null)
        private set

    fun cloudBrowserGeoeffnet() {
        cloudBrowserUrl = null
    }

    /** Prüft die Verbindung. Ist eine Anmeldung nötig, kommt die Anmelde-Seite von Apple in `cloudBrowserUrl`. */
    fun cloudPruefen() {
        cloudBeschaeftigt = true
        viewModelScope.launch {
            val a = withContext(Dispatchers.IO) { cloud.aktuellerNutzer() }
            cloudAngemeldet = cloud.angemeldet && a.ok
            when {
                a.ok -> cloudText = "Angemeldet bei Apple. Nutzerkennung: ${a.benutzer?.take(12) ?: "?"}..."
                a.anmeldungNoetig && a.anmeldeUrl != null -> {
                    cloudText = "Bitte bei Apple anmelden."
                    cloudBrowserUrl = a.anmeldeUrl
                }
                else -> cloudText = "Fehler: ${a.fehlerCode ?: "?"}. ${a.grund.orEmpty()}"
            }
            cloudBeschaeftigt = false
        }
    }

    /** Die App wurde von Apple über `yem1n://cloudkit?ckWebAuthToken=...` zurückgerufen. */
    fun cloudRueckkehr(adresse: String) {
        val token = CloudAdresse.tokenAusRueckkehr(adresse)
        if (token == null) {
            cloudText = "Die Anmeldung hat keinen Schlüssel gebracht."
            return
        }
        cloud.merkeToken(token)
        cloudAngemeldet = true
        cloudText = "Schlüssel erhalten, prüfe die Verbindung..."
        cloudPruefen()
    }

    fun cloudAbmelden() {
        cloud.merkeToken(null)
        cloudAngemeldet = false
        cloudText = "Abgemeldet."
    }

    // Joker: Fragen der Kinder und Antworten der Eltern über den eigenen Server

    /** Der Familiencode in der Form XXXXX-XXXXX, leer ohne gültigen Code (auch ältere Codes ohne Strich gehen). */
    val jokerCode: String get() = Familiencode.pruefe(fortschritt.familiencode) ?: ""

    var jokerFragen: List<JokerFrage> by mutableStateOf(emptyList())
        private set
    var jokerMeldung: String by mutableStateOf("")
    /** Name des Kindes mit offener Bitte um neue Joker (nur für Eltern). */
    var jokerNachschubOffen: String? by mutableStateOf(null)
        private set
    var jokerBeschaeftigt: Boolean by mutableStateOf(false)
        private set

    fun oeffneJoker() {
        ansicht = Ansicht.Joker
        offeneErgebnisseSenden()
        if (jokerCode.isNotEmpty()) jokerLaden()
    }

    /** Nimmt einen eingegebenen Familiencode an. Gibt false zurück, wenn er nicht passt. */
    fun setzeFamiliencode(eingabe: String): Boolean {
        val code = Familiencode.pruefe(eingabe) ?: return false
        behalte(fortschritt.copy(familiencode = code))
        jokerFragen = emptyList()
        jokerLaden()
        geraetMelden()
        return true
    }

    fun neuerFamiliencode() {
        behalte(fortschritt.copy(familiencode = Familiencode.neu()))
        jokerFragen = emptyList()
        jokerMeldung = "Neuer Familiencode. Gib ihn auch auf dem Gerät der Eltern ein."
    }

    fun vergissFamiliencode() {
        behalte(fortschritt.copy(familiencode = ""))
        jokerFragen = emptyList()
        jokerMeldung = ""
    }

    fun setzeJokerRolle(rolle: String) {
        if (rolle == "kind" || rolle == "eltern") {
            behalte(fortschritt.copy(jokerRolle = rolle))
            geraetMelden()
        }
    }

    /**
     * Meldet dieses Gerät (Familiencode, Rolle, Push-Kennung von Google) beim Server an,
     * damit es Mitteilungen bekommt. Nur wenn sich etwas geändert hat.
     */
    fun geraetMelden() {
        val code = jokerCode
        if (code.isEmpty()) return
        val app = getApplication<android.app.Application>()
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token.isNullOrEmpty()) return@addOnSuccessListener
                Mitteilungen.speichereToken(app, token)
                val rolle = fortschritt.jokerRolle
                val schluessel = "$code|$rolle|$token"
                if (Mitteilungen.gemeldet(app) == schluessel) return@addOnSuccessListener
                viewModelScope.launch {
                    val e = withContext(Dispatchers.IO) {
                        ServerClient.senden(ServerAnfrage.PFAD_GERAET, ServerAnfrage.geraet(code, rolle, token))
                    }
                    if (e.ok) Mitteilungen.merkeGemeldet(app, schluessel)
                }
            }
        } catch (e: Exception) {
            // Ohne Google-Dienste gibt es keine Mitteilungen, die App läuft trotzdem.
        }
    }

    fun jokerLaden() {
        val code = jokerCode
        if (code.isEmpty() || jokerBeschaeftigt) return
        jokerBeschaeftigt = true
        viewModelScope.launch {
            val liste = withContext(Dispatchers.IO) {
                val f = cloud.lesen("records/query", JokerCloud.abfrage(JokerCloud.TYP_FRAGE, code))
                val a = cloud.lesen("records/query", JokerCloud.abfrage(JokerCloud.TYP_ANTWORT, code))
                val d = cloud.lesen("records/query", JokerCloud.abfrage(JokerCloud.TYP_DAUMEN, code))
                if (f == null) null
                else JokerCloud.fragen(
                    JokerCloud.datensaetze(f), JokerCloud.datensaetze(a.orEmpty()), JokerCloud.datensaetze(d.orEmpty()),
                )
            }
            if (liste == null) {
                jokerMeldung = "Keine Verbindung. Ist das Internet an?"
            } else {
                jokerFragen = liste
                jokerMeldung = if (liste.isEmpty()) "Noch keine Fragen." else ""
            }
            jokerBeschaeftigt = false
            nachschubPruefen(code)
        }
    }

    /** Eltern sehen offene Bitten um Joker, ein Kind-Gerät füllt seine Joker bei neuen Freigaben auf. */
    private suspend fun nachschubPruefen(code: String) {
        val (n, fr) = withContext(Dispatchers.IO) {
            cloud.lesen("records/query", JokerCloud.abfrage(JokerCloud.TYP_NACHSCHUB, code)) to
                cloud.lesen("records/query", JokerCloud.abfrage(JokerCloud.TYP_FREIGABE, code))
        }
        if (n == null || fr == null) return
        val nachschub = JokerCloud.datensaetze(n)
        val freigaben = JokerCloud.datensaetze(fr)
        if (fortschritt.jokerRolle == "eltern") {
            jokerNachschubOffen = JokerNachschub.offeneAnfrage(nachschub, freigaben)
            return
        }
        jokerNachschubOffen = null
        if (fortschritt.jokerFreigabeAb == 0L) {
            behalte(fortschritt.copy(jokerFreigabeAb = System.currentTimeMillis()))
            return
        }
        val neu = JokerNachschub.neueFreigaben(freigaben, fortschritt.jokerFreigaben, fortschritt.jokerFreigabeAb)
        if (neu.isNotEmpty()) {
            behalte(
                fortschritt.mitJokerAufgefuellt(Tag.heute()).copy(jokerFreigaben = (fortschritt.jokerFreigaben + neu).takeLast(100)),
            )
            jokerMeldung = "🎁 Deine Eltern haben dir neue Joker geschenkt!"
        }
    }

    fun jokerNachschubAnfragen() {
        if (jokerCode.isEmpty()) return
        jokerSenden(ServerAnfrage.nachschub(jokerCode, fortschritt.name))
    }

    fun jokerFreigeben() {
        if (jokerCode.isEmpty()) return
        jokerSenden(ServerAnfrage.freigabe(jokerCode, fortschritt.jokerLimit))
    }

    private fun jokerSenden(koerper: String, verbraucht: Boolean = false) {
        if (jokerBeschaeftigt) return
        jokerBeschaeftigt = true
        viewModelScope.launch {
            val e = withContext(Dispatchers.IO) { ServerClient.senden(ServerAnfrage.PFAD_SENDEN, koerper) }
            jokerBeschaeftigt = false
            if (e.ok) {
                if (verbraucht) behalte(fortschritt.mitJokerGenutzt(Tag.heute()))
                jokerMeldung = "Gesendet."
                // CloudKit braucht einen Moment, bis neue Einträge abfragbar sind.
                kotlinx.coroutines.delay(1500)
                jokerLaden()
            } else {
                jokerMeldung = e.meldung
            }
        }
    }

    fun jokerFrageSenden(fach: String, uebung: String, text: String) {
        if (text.isBlank() || jokerCode.isEmpty()) return
        if (fortschritt.jokerUebrig(Tag.heute()) <= 0) {
            jokerMeldung = "Heute sind keine Joker mehr übrig. Du kannst neue anfragen."
            return
        }
        jokerSenden(ServerAnfrage.frage(jokerCode, fortschritt.name, fach, uebung, text), verbraucht = true)
    }

    fun jokerAntwortSenden(frageId: String, emoji: String, text: String) {
        if (text.isBlank() || jokerCode.isEmpty()) return
        jokerSenden(ServerAnfrage.antwort(jokerCode, frageId, fortschritt.name.ifBlank { "Eltern" }, emoji, text))
    }

    fun jokerDaumen(frageId: String, antwortId: String) {
        if (jokerCode.isEmpty()) return
        jokerSenden(ServerAnfrage.daumen(jokerCode, frageId, antwortId, fortschritt.name))
    }

    // Ergebnisse der Kinder für die Eltern

    var ergebnisse: List<de.cemaras.yem1n.logik.RundenErgebnisEintrag> by mutableStateOf(emptyList())
        private set
    var ergebnisseMeldung: String by mutableStateOf("")
        private set
    var ergebnisseBeschaeftigt: Boolean by mutableStateOf(false)
        private set

    fun oeffneErgebnisse() {
        ansicht = Ansicht.ErgebnisseEltern
        ergebnisseLaden()
    }

    fun ergebnisseLaden() {
        val code = jokerCode
        if (code.isEmpty() || ergebnisseBeschaeftigt) return
        ergebnisseBeschaeftigt = true
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                cloud.lesen("records/query", JokerCloud.abfrage(JokerCloud.TYP_ERGEBNIS, code, 300, "zeitpunkt"))
            }
            if (text == null) {
                ergebnisseMeldung = "Keine Verbindung. Ist das Internet an?"
            } else {
                ergebnisse = JokerCloud.ergebnisse(JokerCloud.datensaetze(text))
                ergebnisseMeldung = if (ergebnisse.isEmpty()) "Noch keine Runden angekommen." else ""
            }
            ergebnisseBeschaeftigt = false
        }
    }

    fun setzeErinnerung(an: Boolean) = behalte(fortschritt.copy(haustierErinnerung = an))

    fun setzeLernzeit(an: Boolean) = behalte(fortschritt.copy(lernzeitAn = an))

    fun setzeLernzeitUhrzeit(minuten: Int) = behalte(fortschritt.copy(lernzeitMinuten = minuten.coerceIn(0, 24 * 60 - 1)))

    /** Nach der Frage nach der Erlaubnis noch einmal planen. */
    fun erinnerungNeuPlanen() {
        Erinnerung.planen(getApplication(), fortschritt.haustier, fortschritt.haustierErinnerung)
    }

    fun setzeSparmodus(an: Boolean) {
        de.cemaras.yem1n.ui.theme.Sparmodus.an = an
        behalte(fortschritt.copy(sparmodus = an))
    }

    fun setzeSpielTon(an: Boolean) {
        SpielEffekte.ton = an
        behalte(fortschritt.copy(spielTon = an))
    }

    fun setzeSpielHaptik(an: Boolean) {
        SpielEffekte.haptik = an
        behalte(fortschritt.copy(spielHaptik = an))
    }

    fun setzeVorlesen(an: Boolean) {
        sprecher.an = an
        if (!an) sprecher.stopp()
        behalte(fortschritt.copy(vorlesen = an))
    }

    fun startVorschul(art: VorschulArt) {
        paketStand = null
        vorschulArt = art
        englischThema = null
        verdient = 0
        haustierText = null
        neuePokale = emptyList()
        if (art == VorschulArt.MEMORY) {
            vorschul = null
            memory = MemoryStand.neu()
        } else {
            memory = null
            val s = VorschulStand(VorschulGenerator.fragen(art, fortschritt.vorschulStufe))
            vorschul = s
            s.aktuelle?.let { sageFrage(it.text) }
        }
        ansicht = Ansicht.VorschulSpiel(art)
    }

    fun vorschulNochmal() {
        val thema = englischThema
        val paket = paketStand
        when {
            thema != null -> startEnglisch(thema)
            paket != null -> startVorschulPaket(paket.first, paket.second)
            else -> startVorschul(vorschulArt)
        }
    }

    /** Eigene Vorschul-Pakete (Arbeiten mit dem Fach "Vorschule"), mit ihrer Stelle in der Arbeitenliste. */
    val vorschulPakete: List<Pair<Int, Arbeit>>
        get() = arbeiten.withIndex().filter { it.value.fach == "Vorschule" }.map { it.index to it.value }

    /** Laufende Runde aus einem eigenen Paket: Stelle der Arbeit und der Übung, sonst null. */
    var paketStand: Pair<Int, Int>? by mutableStateOf(null)
        private set

    fun oeffneVorschulPaket(arbeit: Int) {
        ansicht = Ansicht.VorschulPakete(arbeit)
    }

    /** Titel der laufenden Paket-Übung, sonst null. */
    val paketTitel: String?
        get() = paketStand?.let { (a, u) -> arbeiten.getOrNull(a)?.uebungen?.getOrNull(u)?.titel }

    fun startVorschulPaket(arbeit: Int, uebung: Int) {
        val u = arbeiten.getOrNull(arbeit)?.uebungen?.getOrNull(uebung) ?: return
        val fragen = VorschulPaket.fragen(u)
        if (fragen.isEmpty()) {
            meldung = "In dieser Übung gibt es keine Fragen zum Antippen."
            return
        }
        paketStand = arbeit to uebung
        vorschulArt = VorschulArt.MIX
        englischThema = null
        verdient = 0
        haustierText = null
        neuePokale = emptyList()
        memory = null
        val s = VorschulStand(fragen)
        vorschul = s
        s.aktuelle?.let { sageFrage(it.text) }
        ansicht = Ansicht.VorschulSpiel(VorschulArt.MIX)
    }

    /** Die aktuelle Frage noch einmal vorlesen (Lautsprecher-Knopf). */
    fun frageVorlesen() {
        vorschul?.aktuelle?.let { sageFrage(it.text) }
    }

    fun vorschulWahl(i: Int) {
        val s = vorschul ?: return
        val neu = s.wahl(i)
        if (neu == s) return
        vorschul = neu
        if (neu.richtigGewaehlt != null) {
            sprecher.sprich(Lob.fuerRichtig(neu.serie, !neu.ersterVersuch, fortschritt.name), true)
        } else {
            sprecher.sprich(Lob.fuerNochmal())
        }
    }

    /** Nach dem Treffer zur nächsten Frage, nach der letzten wird die Runde gespeichert. */
    fun vorschulWeiter() {
        val s = vorschul ?: return
        val neu = s.weiter()
        if (neu == s) return
        vorschul = neu
        val frage = neu.aktuelle
        if (frage != null) {
            sageFrage(frage.text)
        } else {
            speichereVorschulRunde(neu.staende())
            sprecher.sprich(Lob.fuerEnde(neu.perfekt, neu.fragen.size, fortschritt.name), true)
        }
    }

    fun memoryTippe(i: Int) {
        val s = memory ?: return
        val neu = s.tippe(i)
        if (neu == s) return
        memory = neu
        if (neu.fertig) {
            val staende = List(neu.gesamt) { if (it < neu.paare) Stand.RICHTIG else Stand.FALSCH }
            speichereVorschulRunde(staende)
            sprecher.sprich(Lob.fuerEnde(neu.paare, neu.gesamt, fortschritt.name), true)
        }
    }

    fun memoryZudecken() {
        memory = memory?.zudecken()
    }

    private fun speichereVorschulRunde(staende: List<Stand>) {
        val stunde = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val vorher = Erfolge.pokale(fortschritt).filter { it.erreicht }.map { it.id }.toSet()
        val paket = paketStand?.let { (a, u) -> arbeiten.getOrNull(a)?.let { it to it.uebungen.getOrNull(u) } }
        val (neu, muenzen) = if (paket?.second != null) {
            fortschritt.nachRundeMit(false, paket.first.schluessel, paket.second!!.titel, staende, Tag.heute(), "Vorschule", stunde)
        } else {
            fortschritt.nachRundeMit(
                false, Fortschritt.VORSCHUL_ARBEIT, englischThema?.uebungName ?: vorschulArt.uebungName, staende, Tag.heute(), "Vorschule", stunde,
            )
        }
        neuePokale = Erfolge.pokale(neu).filter { it.erreicht && it.id !in vorher }
        verdient = muenzen
        haustierText = Haustier.ereignis(fortschritt.haustier, neu.haustier)?.let { neu.haustier?.meldung(it) }
        behalte(neu)
    }

    override fun onCleared() {
        sprecher.schliessen()
        super.onCleared()
    }

    /** Meldung vom Haustier für die Ergebnisseite, null ohne Haustier. */
    var haustierText: String? by mutableStateOf(null)

    fun oeffneHaustier() {
        ansicht = Ansicht.HaustierSeite
    }

    /** Das Tier mit dem heutigen Hunger und der heutigen Laune. */
    val haustier: Haustier? get() = fortschritt.haustier?.aktualisiert(System.currentTimeMillis())

    private fun setzeHaustier(h: Haustier, muenzen: Int = fortschritt.muenzen) {
        behalte(fortschritt.copy(haustier = h, muenzen = muenzen))
    }

    fun neuesHaustier(name: String, art: String) {
        setzeHaustier(Haustier.neu(name, art, System.currentTimeMillis()))
    }

    /** Das Haustier geht, ein neues Ei kann ausgesucht werden. Münzen bleiben. */
    fun haustierWechseln() {
        behalte(fortschritt.copy(haustier = null))
    }

    fun haustierLeckerli() {
        val h = haustier ?: return
        h.leckerli(fortschritt.muenzen)?.let { setzeHaustier(it, fortschritt.muenzen - Haustier.LECKERLI_PREIS) }
    }

    fun haustierSpielen() {
        val h = haustier ?: return
        h.spielen(fortschritt.muenzen)?.let { setzeHaustier(it, fortschritt.muenzen - Haustier.SPIELZEUG_PREIS) }
    }

    fun haustierKaufen(id: String) {
        val h = haustier ?: return
        val preis = HaustierHut.finde(id)?.preis ?: return
        h.kaufe(id, fortschritt.muenzen)?.let { setzeHaustier(it, fortschritt.muenzen - preis) }
    }

    fun haustierTragen(id: String) {
        val h = haustier ?: return
        setzeHaustier(h.trage(id))
    }

    /** Tagesgeschenk abholen. Gibt die Münzen zurück, 0 wenn heute schon abgeholt. */
    fun haustierGeschenk(): Int {
        val h = haustier ?: return 0
        val neu = h.holeGeschenk(Tag.heute()) ?: return 0
        setzeHaustier(neu, fortschritt.muenzen + Haustier.GESCHENK_MUENZEN)
        return Haustier.GESCHENK_MUENZEN
    }

    fun oeffneInfo() {
        ansicht = Ansicht.Info
    }

    fun oeffneInfoSeite(seite: InfoSeite) {
        ansicht = Ansicht.InfoSeite(seite)
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
        haustierText = null
        ansicht = Ansicht.Ueben(SONDER, 0)
    }

    /** Zwanzig neue gemischte Aufgaben mit Uhr, am Ende gibt es eine Note. */
    fun startProbe() {
        val arbeit = Sonderrunden.probe(arbeiten)
        sonderArbeit = arbeit
        sonderArt = SonderArt.PROBE
        runde = Runde.start(arbeit.uebungen[0].aufgaben)
        verdient = 0
        haustierText = null
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
        haustierText = null
        neuePokale = emptyList()
        ansicht = Ansicht.Ueben(a.arbeit, uebung)
    }

    /**
     * Die nächste Übung derselben Arbeit: bevorzugt eine, die noch nicht geübt wurde, sonst einfach die folgende.
     * Null im Fehlerheft, in der Probearbeit und bei der letzten Übung.
     */
    val naechsteUebung: Pair<Int, Uebung>?
        get() {
            val a = ansicht as? Ansicht.Ueben ?: return null
            if (imSonder) return null
            val arbeit = aktuelleArbeit ?: return null
            val danach = arbeit.uebungen.withIndex().filter { it.index > a.uebung }
            val wahl = danach.firstOrNull { fortschritt.stand(arbeit.schluessel, it.value.titel).beste == 0 } ?: danach.firstOrNull()
            return wahl?.let { it.index to it.value }
        }

    fun startNaechsteUebung() {
        val a = ansicht as? Ansicht.Ueben ?: return
        val (index, u) = naechsteUebung ?: return
        runde = Runde.start(u.aufgaben)
        verdient = 0
        haustierText = null
        neuePokale = emptyList()
        ansicht = Ansicht.Ueben(a.arbeit, index)
    }

    /** Dieselbe Übung noch einmal von vorn. */
    fun nochmal() {
        val a = ansicht as? Ansicht.Ueben ?: return
        val u = aktuelleUebung ?: return
        runde = Runde.start(u.aufgaben)
        verdient = 0
        haustierText = null
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
            is Ansicht.Uebungen, Ansicht.Einstellungen, Ansicht.Erfolge, Ansicht.Vorschule, Ansicht.Spiele, Ansicht.HaustierSeite, Ansicht.Joker, Ansicht.ErgebnisseEltern -> ansicht = Ansicht.Start
            is Ansicht.VorschulPakete -> ansicht = Ansicht.Vorschule
            is Ansicht.Spiel -> ansicht = Ansicht.Spiele
            is Ansicht.VorschulSpiel -> {
                sprecher.stopp()
                vorschul = null
                memory = null
                val thema = englischThema
                englischThema = null
                val paket = paketStand
                paketStand = null
                ansicht = when {
                    thema != null -> Ansicht.Englisch(thema)
                    paket != null -> Ansicht.VorschulPakete(paket.first)
                    else -> Ansicht.Vorschule
                }
            }
            is Ansicht.Englisch -> {
                sprecher.stopp()
                ansicht = Ansicht.Vorschule
            }
            Ansicht.Info -> ansicht = Ansicht.Einstellungen
            is Ansicht.InfoSeite -> ansicht = Ansicht.Info
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
        val stunde = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val vorher = Erfolge.pokale(fortschritt).filter { it.erreicht }.map { it.id }.toSet()
        val (neu, muenzen) = fortschritt.nachRundeMit(
            imSonder, arbeit.schluessel, uebung.titel, s.staende, Tag.heute(), arbeit.fach, stunde,
        )
        neuePokale = Erfolge.pokale(neu).filter { it.erreicht && it.id !in vorher }
        verdient = muenzen
        haustierText = Haustier.ereignis(fortschritt.haustier, neu.haustier)?.let { neu.haustier?.meldung(it) }
        behalte(neu)
        if (!imSonder) ergebnisMelden(arbeit.klasse, arbeit.fach, arbeit.titel, uebung.titel, s)
    }

    /** Was beim Senden der Ergebnisse zuletzt passiert ist, für die Joker-Seite. */
    var ergebnisStatus: String by mutableStateOf("")
        private set
    private var ergebnisSendet = false

    /**
     * Merkt das Ergebnis der Runde vor und schickt es an die Eltern (über den Server), wenn ein Familiencode gesetzt ist
     * und dieses Gerät dem Kind gehört. Was nicht ankommt, bleibt gemerkt und wird später noch einmal geschickt.
     */
    private fun ergebnisMelden(klasse: String, fach: String, arbeit: String, uebung: String, s: RundenStand) {
        val code = jokerCode
        if (code.isEmpty()) return
        if (fortschritt.jokerRolle != "kind") {
            ergebnisStatus = "Dieses Gerät steht auf Eltern, darum wird die Runde nicht gesendet."
            return
        }
        val e = de.cemaras.yem1n.logik.RundenErgebnisEintrag(
            eintragId = java.util.UUID.randomUUID().toString(), klasse = klasse, fach = fach, arbeit = arbeit,
            uebung = uebung, richtig = s.richtig, gesamt = s.aufgaben.size, angesehen = s.angeschaut,
            sterne = s.sterne, kind = fortschritt.name.trim(), zeitpunkt = System.currentTimeMillis(),
        )
        val offen = (fortschritt.offeneErgebnisse + ServerAnfrage.ergebnis(code, e)).takeLast(50)
        behalte(fortschritt.copy(offeneErgebnisse = offen))
        offeneErgebnisseSenden()
    }

    /** Schickt alle gemerkten Ergebnisse. Ein Fehler hält die Reihe an, der Rest bleibt für später. */
    fun offeneErgebnisseSenden() {
        if (ergebnisSendet || fortschritt.offeneErgebnisse.isEmpty()) return
        ergebnisSendet = true
        viewModelScope.launch {
            var gesendet = 0
            var fehler: String? = null
            for (koerper in fortschritt.offeneErgebnisse) {
                val r = withContext(Dispatchers.IO) { ServerClient.senden(ServerAnfrage.PFAD_SENDEN, koerper) }
                if (r.ok) {
                    gesendet++
                    behalte(fortschritt.copy(offeneErgebnisse = fortschritt.offeneErgebnisse - koerper))
                } else {
                    fehler = r.meldung
                    // Eine abgelehnte Anfrage (400) würde nie ankommen, sie fliegt raus, damit sie nicht alles blockiert.
                    if (r.abgelehnt) behalte(fortschritt.copy(offeneErgebnisse = fortschritt.offeneErgebnisse - koerper))
                    else break
                }
            }
            val warten = fortschritt.offeneErgebnisse.size
            ergebnisStatus = when {
                fehler != null && warten > 0 -> "$warten Runde(n) warten noch auf das Senden. $fehler"
                fehler != null -> "Eine Runde konnte nicht gesendet werden. $fehler"
                gesendet > 0 -> "Letzte Runde an die Eltern gesendet."
                else -> ergebnisStatus
            }
            ergebnisSendet = false
        }
    }

    /** Wochenbelohnung abholen. Gibt die Münzen zurück, 0 wenn es nichts abzuholen gab. */
    fun holeWochenBelohnung(): Int {
        val neu = Erfolge.belohnungHolen(fortschritt, Tag.heute())
        val plus = neu.muenzen - fortschritt.muenzen
        if (plus > 0) behalte(neu)
        return plus
    }

    /** Eine Arbeit, die eine andere App geschickt hat (Teilen oder Datei öffnen). */
    fun importiereVonAussen(text: String) {
        meldung = fuegeArbeitEin(text) ?: "Die Arbeit wurde importiert."
    }

    fun meldungGelesen() {
        meldung = null
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
