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
import de.cemaras.yem1n.daten.zuArbeit
import de.cemaras.yem1n.logik.MatheGenerator
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
import de.cemaras.yem1n.logik.Einwilligung
import de.cemaras.yem1n.logik.Familiencode
import de.cemaras.yem1n.logik.JokerCloud
import de.cemaras.yem1n.logik.Profil
import de.cemaras.yem1n.logik.Klassensiegel
import de.cemaras.yem1n.logik.KlassenAdmin
import de.cemaras.yem1n.logik.PaketJson
import de.cemaras.yem1n.logik.Karteikasten
import de.cemaras.yem1n.logik.Sprachen
import de.cemaras.yem1n.logik.SprachRunde
import de.cemaras.yem1n.logik.JokerFrage
import de.cemaras.yem1n.logik.DuellAntwort
import de.cemaras.yem1n.logik.DuellHerausforderung
import de.cemaras.yem1n.logik.DuellPost
import de.cemaras.yem1n.logik.DuellRegeln
import de.cemaras.yem1n.logik.DuellSpiel
import de.cemaras.yem1n.logik.DuellStand
import de.cemaras.yem1n.logik.DuellStufe
import de.cemaras.yem1n.logik.JokerNachschub
import de.cemaras.yem1n.logik.ServerAnfrage
import de.cemaras.yem1n.logik.FamilienProtokoll
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
    data object Familie : Ansicht
    data object ErgebnisseEltern : Ansicht
    data object Duell : Ansicht
    data object Lernwelt : Ansicht
    data object Sprachen : Ansicht
    data object Editor : Ansicht
    data object VokabelEditor : Ansicht
    data class Spiel(val art: SpielArt) : Ansicht
    data class VorschulSpiel(val art: VorschulArt) : Ansicht
    data class Englisch(val thema: EnglischThema) : Ansicht
    data class InfoSeite(val seite: de.cemaras.yem1n.ui.InfoSeite) : Ansicht
}

/** Runden, die nicht aus einer Arbeit der Liste kommen. */
enum class SonderArt { FEHLERHEFT, PROBE, TRAINING }

/** Platzhalter für die Arbeit, die nicht in der Liste steht (das Fehlerheft). */
private const val SONDER = -1

/** Platzhalter für die Arbeit, die gerade aus der Lernwelt kommt. */
private const val LERNWELT = -2

private const val DATEI_ENTWURF = "entwurf.json"
private const val DATEI_THEMEN = "eigene-themen.json"

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val speicher = Speicher(app)

    private var alleArbeiten: List<Arbeit> = emptyList()
    private var sonderArbeit: Arbeit? = null
    private var sonderArt: SonderArt? = null
    private var lernArbeit: Arbeit? = null
    private var lernThema: de.cemaras.yem1n.logik.LernThema? = null

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
        altesAufraeumen()
        sprecher.an = fortschritt.vorlesen
        SpielEffekte.ton = fortschritt.spielTon
        if (fortschritt.stift) stiftPruefen()
        val schwach = (app.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)?.isLowRamDevice == true
        // "Animationen entfernen" in den Android-Einstellungen (Bedienungshilfen) wie "Bewegung reduzieren" bei iOS
        val ohneAnimation = runCatching {
            android.provider.Settings.Global.getFloat(app.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
        de.cemaras.yem1n.ui.theme.Sparmodus.an = fortschritt.sparmodus || schwach || ohneAnimation
        Erinnerung.planen(app, fortschritt.haustier, fortschritt.haustierErinnerung)
        Erinnerung.planenLernzeit(app, fortschritt.lernzeitAn, fortschritt.lernzeitMinuten, fortschritt.rundenHeute(Tag.heute()) > 0)
        Erinnerung.planenBericht(app, fortschritt.wochenberichtAn && fortschritt.jokerRolle == "eltern")
        offeneErgebnisseSenden()
        Mitteilungen.kanalAnlegen(app)
        if (fortschritt.duellGeraet.isEmpty()) behalte(fortschritt.copy(duellGeraet = java.util.UUID.randomUUID().toString()))
        geraetMelden()
        SpielEffekte.haptik = fortschritt.spielHaptik
        Farbwelt.rosa = fortschritt.rosa
        sichtbarMachen()
    }

    /**
     * Reste aus der Zeit vor dem Familien-Dienst: der alte Familiencode war Passwort und Suchschlüssel zugleich und
     * gilt nicht mehr. Wartende Ergebnisse im alten Format könnten nie ankommen. Beides wird vergessen.
     */
    private fun altesAufraeumen() {
        val f = fortschritt
        val offen = f.offeneErgebnisse.filter { it.contains("\"typ\"") }
        if (f.familiencode.isNotEmpty() || f.angemeldet.isNotEmpty() || offen.size != f.offeneErgebnisse.size) {
            fortschritt = f.copy(familiencode = "", angemeldet = "", offeneErgebnisse = offen)
            speicher.speichereFortschritt(fortschritt)
        }
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
        Erinnerung.planenBericht(getApplication(), neu.wochenberichtAn && neu.jokerRolle == "eltern")
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
            is Ansicht.Ueben -> when (a.arbeit) {
                SONDER -> sonderArbeit
                LERNWELT -> lernArbeit
                else -> arbeiten.getOrNull(a.arbeit)
            }
            Ansicht.Start, Ansicht.Einstellungen, Ansicht.Erfolge, Ansicht.Info, is Ansicht.InfoSeite,
            Ansicht.Vorschule, is Ansicht.VorschulPakete, is Ansicht.VorschulSpiel, is Ansicht.Englisch, Ansicht.Spiele, is Ansicht.Spiel, Ansicht.HaustierSeite, Ansicht.Joker, Ansicht.Familie, Ansicht.ErgebnisseEltern, Ansicht.Duell, Ansicht.Lernwelt, Ansicht.Sprachen, Ansicht.Editor, Ansicht.VokabelEditor -> null
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

    // Cloud: nur noch zum Lesen öffentlicher, unterschriebener Klassenpakete (kein Benutzerkonto, keine Anmeldung)

    private val cloud = CloudClient(app)

    // Familie (Protokoll 2): Schlüssel je Gerät, Koppeln per QR-Code, Freigabe durch die Eltern

    val familie = FamilienModell(app)

    // Joker: Fragen der Kinder und Antworten der Eltern über den Familien-Dienst

    /** Solange die Eltern nicht eingewilligt haben, geht nichts in die Cloud. */
    val einwilligungOffen: Boolean get() = Einwilligung.erforderlich(fortschritt.einwilligung)

    /** Das Gerät gehört zur Familie, ist freigegeben und die Eltern haben eingewilligt. Nur dann geht etwas hin und her. */
    val familieBereit: Boolean get() = !einwilligungOffen && familie.aktiv

    fun einwilligen() = behalte(fortschritt.copy(einwilligung = Einwilligung.AKTUELLE_VERSION, einwilligungDatum = System.currentTimeMillis()))

    /**
     * Widerruf: Die Push-Kennung wird beim Dienst und bei Google gelöscht, danach sendet die App nichts mehr.
     * Zur Familie gehört das Gerät weiter, bis es unter Familie ausdrücklich entfernt wird.
     */
    fun einwilligungWiderrufen() {
        val war = familie.aktiv
        behalte(fortschritt.copy(einwilligung = 0, einwilligungDatum = 0))
        viewModelScope.launch {
            if (war) familie.pushAbmelden()
            Mitteilungen.ausschalten(getApplication<android.app.Application>())
        }
        jokerFragen = emptyList()
        jokerMeldung = ""
    }

    var jokerFragen: List<JokerFrage> by mutableStateOf(emptyList())
        private set
    var jokerMeldung: String by mutableStateOf("")
    /** Name des Kindes mit offener Bitte um neue Joker (nur für Eltern). */
    var jokerNachschubOffen: String? by mutableStateOf(null)
        private set
    var jokerBeschaeftigt: Boolean by mutableStateOf(false)
        private set

    fun oeffneFamilie() {
        ansicht = Ansicht.Familie
        familieAktualisieren()
    }

    fun oeffneJoker() {
        ansicht = Ansicht.Joker
        offeneErgebnisseSenden()
        if (familieBereit) jokerLaden()
    }

    // Sprachen: Karteikasten und Bericht

    fun setzeLernSprache(code: String, an: Boolean) {
        val neu = Sprachen.alle.map { it.code }.filter { if (it == code) an else it in fortschritt.lernSprachen }
        behalte(fortschritt.copy(lernSprachen = neu))
    }

    fun bewerteWort(sprache: String, thema: String, de: String, richtig: Boolean) =
        behalte(fortschritt.copy(wortStand = Karteikasten.bewerte(fortschritt.wortStand, Karteikasten.schluessel(sprache, thema, de), richtig, System.currentTimeMillis())))

    fun kannWort(sprache: String, thema: String, de: String) =
        behalte(fortschritt.copy(wortStand = Karteikasten.kannIch(fortschritt.wortStand, Karteikasten.schluessel(sprache, thema, de), System.currentTimeMillis())))

    fun sprachRunde(sprache: String, thema: String, art: String, richtig: Int, gesamt: Int) =
        behalte(fortschritt.copy(sprachVerlauf = Karteikasten.neuerVerlauf(fortschritt.sprachVerlauf, SprachRunde(System.currentTimeMillis(), sprache, thema, art, richtig, gesamt))))

    // Klassencode als Admin: Klasse anlegen, Pakete unterschreiben und senden

    var adminMeldung: String by mutableStateOf("")
        private set
    var adminBeschaeftigt: Boolean by mutableStateOf(false)
        private set
    /** Pakete dieser Klasse in der Cloud: Name des Datensatzes und Titel. */
    var adminPakete: List<Pair<String, String>> by mutableStateOf(emptyList())
        private set

    val istKlassenAdmin: Boolean
        get() = fortschritt.klassencode.isNotEmpty() && fortschritt.klassenAdmin.containsKey(fortschritt.klassencode)

    fun klasseAnlegen() {
        if (einwilligungOffen || adminBeschaeftigt) return
        val code = Familiencode.neu()
        val seed = KlassenAdmin.neuerSeed()
        adminBeschaeftigt = true
        adminMeldung = ""
        viewModelScope.launch {
            val e = withContext(Dispatchers.IO) { ServerClient.senden(ServerAnfrage.PFAD_KLASSE, KlassenAdmin.schluesselAnfrage(code, seed)) }
            if (e.ok) {
                behalte(fortschritt.copy(klassencode = code, klassenAdmin = fortschritt.klassenAdmin + (code to seed)))
                adminMeldung = "Deine Klasse ist angelegt. Der Klassencode ist $code. Gib ihn an die Geräte der Klasse weiter."
                adminPaketeLaden()
            } else {
                adminMeldung = e.meldung
            }
            adminBeschaeftigt = false
        }
    }

    /** Schickt eine Arbeit unterschrieben an die Klasse. */
    fun klassePaketSenden(arbeit: Arbeit) {
        val code = fortschritt.klassencode
        val seed = fortschritt.klassenAdmin[code] ?: return
        if (adminBeschaeftigt) return
        val json = PaketJson.text(arbeit)
        if (json.length > KlassenAdmin.MAX_JSON) {
            adminMeldung = "\"${arbeit.titel}\" ist zu groß für die Cloud. Bitte in zwei Arbeiten teilen."
            return
        }
        adminBeschaeftigt = true
        viewModelScope.launch {
            val e = withContext(Dispatchers.IO) { ServerClient.senden(ServerAnfrage.PFAD_KLASSE, KlassenAdmin.paketAnfrage(code, seed, json)) }
            adminMeldung = if (e.ok) "\"${arbeit.titel}\" ist bei der Klasse. Alle Geräte mit dem Klassencode laden es von selbst." else e.meldung
            adminBeschaeftigt = false
            if (e.ok) adminPaketeLaden()
        }
    }

    fun klassePaketLoeschen(name: String) {
        val code = fortschritt.klassencode
        val seed = fortschritt.klassenAdmin[code] ?: return
        if (adminBeschaeftigt) return
        adminBeschaeftigt = true
        viewModelScope.launch {
            val e = withContext(Dispatchers.IO) { ServerClient.senden(ServerAnfrage.PFAD_KLASSE, KlassenAdmin.loeschenAnfrage(code, seed, name)) }
            adminMeldung = if (e.ok) "Das Paket wurde bei der Klasse gelöscht." else e.meldung
            adminBeschaeftigt = false
            if (e.ok) adminPaketeLaden()
        }
    }

    fun adminPaketeLaden() {
        val kcode = Klassensiegel.cloudCode(fortschritt.klassencode) ?: return
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) { cloud.lesen("records/query", JokerCloud.abfrage("Lernpaket", kcode, 100)) }
            if (text != null) adminPakete = JokerCloud.datensaetze(text).map { it.name to it.text("titel", it.name) }
        }
    }

    // Klassencode: Aufgabenpakete der Klasse, vom Admin unterschrieben

    var klassenBeschaeftigt: Boolean by mutableStateOf(false)
        private set

    /** Nimmt einen Klassencode an und holt gleich die Pakete. Gibt false zurück, wenn er nicht passt. */
    fun setzeKlassencode(eingabe: String): Boolean {
        val code = Familiencode.pruefe(eingabe) ?: return false
        behalte(fortschritt.copy(klassencode = code))
        klassenPaketeHolen()
        return true
    }

    fun entferneKlassencode() {
        val alt = Klassensiegel.cloudCode(fortschritt.klassencode)
        behalte(fortschritt.copy(klassencode = "", klassenSchluessel = fortschritt.klassenSchluessel - listOfNotNull(alt)))
    }

    /** Holt neue Pakete der Klasse, prüft die Unterschrift und legt sie als Arbeiten ab. */
    fun klassenPaketeHolen() {
        val cloudCode = Klassensiegel.cloudCode(fortschritt.klassencode) ?: return
        if (einwilligungOffen || klassenBeschaeftigt) return
        klassenBeschaeftigt = true
        viewModelScope.launch {
            val (liste, schluessel) = withContext(Dispatchers.IO) {
                val l = cloud.lesen("records/query", JokerCloud.abfrage("Lernpaket", cloudCode, limit = 50))
                val k = cloud.lesen("records/lookup", Klassensiegel.abfrageSchluessel(cloudCode))
                l to k
            }
            if (liste == null) {
                meldung = "Keine Verbindung. Ist das Internet an?"
            } else {
                val geholt = schluessel?.let { JokerCloud.datensaetze(it).firstOrNull()?.text("publicKey") }?.ifEmpty { null }
                val e = Klassensiegel.pruefen(cloudCode, JokerCloud.datensaetze(liste), fortschritt.klassenSchluessel[cloudCode], geholt)
                if (e.neuerSchluessel != null) {
                    behalte(fortschritt.copy(klassenSchluessel = fortschritt.klassenSchluessel + (cloudCode to e.neuerSchluessel)))
                }
                var neu = 0
                for (json in e.angenommen) if (fuegeArbeitEin(json) == null) neu++
                val teile = mutableListOf<String>()
                if (neu > 0) teile += if (neu == 1) "Eine neue Arbeit der Klasse ist da." else "$neu neue Arbeiten der Klasse sind da."
                if (e.abgelehnt > 0) {
                    teile += "${e.abgelehnt} Paket(e) ohne gültige Unterschrift wurden ignoriert."
                    teile += e.gruende.map { it.text }
                }
                if (teile.isEmpty()) teile += "Keine neuen Pakete."
                meldung = teile.joinToString("\n")
            }
            klassenBeschaeftigt = false
        }
    }

    // ---- Familie verbinden (Koppeln per QR-Code, Freigabe durch die Eltern) ----

    /** Nach jeder Änderung an der Familie: Rolle des Geräts angleichen, Push anmelden, Wartendes nachschicken. */
    private fun familieGeaendert() {
        val z = familie.zugang
        if (z != null && z.aktiv && fortschritt.jokerRolle != z.rolle) behalte(fortschritt.copy(jokerRolle = z.rolle))
        if (familieBereit) {
            geraetMelden()
            offeneErgebnisseSenden()
        } else {
            jokerFragen = emptyList()
            jokerNachschubOffen = null
        }
    }

    /** Legt eine neue Familie an. Dieses Gerät wird das erste Eltern-Gerät. */
    fun familieAnlegen(geraeteName: String, fertig: (String?) -> Unit = {}) {
        if (einwilligungOffen) {
            familie.meldung = "Zuerst die Einwilligung der Eltern geben."
            return
        }
        if (familie.beschaeftigt) return
        viewModelScope.launch {
            val fehler = familie.anlegen(geraeteName.trim().take(40).ifEmpty { "Eltern" })
            familie.meldung = fehler.orEmpty()
            familieGeaendert()
            fertig(fehler)
        }
    }

    /** Tritt einer Familie mit dem Text aus dem QR-Code bei. Danach wartet das Gerät auf die Freigabe der Eltern. */
    fun familieBeitreten(qrText: String, geraeteName: String, fertig: (String?) -> Unit = {}) {
        if (einwilligungOffen) {
            familie.meldung = "Zuerst die Einwilligung der Eltern geben."
            return
        }
        if (familie.beschaeftigt) return
        viewModelScope.launch {
            val fehler = familie.koppeln(qrText, geraeteName.trim().take(40).ifEmpty { "Kind" })
            familie.meldung = fehler.orEmpty()
            familieGeaendert()
            fertig(fehler)
        }
    }

    /** Fragt nach, ob die Eltern dieses Gerät schon freigegeben haben, und lädt bei Eltern die Geräteliste. */
    fun familieAktualisieren() {
        if (einwilligungOffen || familie.zugang == null) return
        viewModelScope.launch {
            val geaendert = familie.statusPruefen()
            if (familie.istEltern) familie.geraeteLaden()
            if (geaendert) familieGeaendert()
        }
    }

    fun familieEinladen(rolle: String) {
        viewModelScope.launch { familie.meldung = familie.einladungErzeugen(rolle).orEmpty() }
    }

    fun familieGeraetErlauben(id: String) = viewModelScope.launch { familie.meldung = familie.erlauben(id).orEmpty() }

    fun familieGeraetAblehnen(id: String) = viewModelScope.launch { familie.meldung = familie.ablehnen(id).orEmpty() }

    fun familieGeraetEntfernen(id: String) = viewModelScope.launch { familie.meldung = familie.entfernen(id).orEmpty() }

    /** Dieses Gerät verlässt die Familie. Ein Kind kann seine Einträge dabei löschen lassen. */
    fun familieVerlassen(eintraegeLoeschen: Boolean) {
        viewModelScope.launch {
            val fehler = familie.verlassen(eintraegeLoeschen)
            familie.meldung = fehler.orEmpty()
            if (fehler == null) {
                Mitteilungen.ausschalten(getApplication<android.app.Application>())
                familieGeaendert()
            }
        }
    }

    /** Löscht die ganze Familie mit allen Einträgen (nur Eltern, mit Bestätigung in der Oberfläche). */
    fun familieLoeschen() {
        viewModelScope.launch {
            val fehler = familie.familieLoeschen()
            familie.meldung = fehler.orEmpty()
            if (fehler == null) {
                Mitteilungen.ausschalten(getApplication<android.app.Application>())
                familieGeaendert()
            }
        }
    }

    /**
     * Meldet dieses Gerät (Push-Kennung von Google) beim Dienst an, damit es Mitteilungen bekommt.
     * Nur wenn sich etwas geändert hat. Die Mitteilungen enthalten weder Namen noch Inhalte.
     */
    fun geraetMelden() {
        if (!familieBereit) return
        val app = getApplication<android.app.Application>()
        try {
            Mitteilungen.einschalten()
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (token.isNullOrEmpty()) return@addOnSuccessListener
                Mitteilungen.speichereToken(app, token)
                val z = familie.zugang ?: return@addOnSuccessListener
                val schluessel = "${z.familie}|${z.geraet}|$token"
                if (Mitteilungen.gemeldet(app) == schluessel) return@addOnSuccessListener
                viewModelScope.launch {
                    if (familie.pushAnmelden(token)) Mitteilungen.merkeGemeldet(app, schluessel)
                }
            }
        } catch (e: Exception) {
            // Ohne Google-Dienste gibt es keine Mitteilungen, die App läuft trotzdem.
        }
    }

    fun jokerLaden() {
        if (!familieBereit || jokerBeschaeftigt) return
        jokerBeschaeftigt = true
        viewModelScope.launch {
            val f = familie.lesen(FamilienProtokoll.TYP_FRAGE)
            val a = if (f == null) null else familie.lesen(FamilienProtokoll.TYP_ANTWORT)
            val d = if (a == null) null else familie.lesen(FamilienProtokoll.TYP_DAUMEN)
            if (f == null || a == null || d == null) {
                jokerMeldung = "Keine Verbindung. Ist das Internet an?"
            } else {
                val liste = FamilienProtokoll.fragen(f, a, d)
                jokerFragen = liste
                jokerMeldung = if (liste.isEmpty()) "Noch keine Fragen." else ""
            }
            jokerBeschaeftigt = false
            nachschubPruefen()
        }
    }

    /** Eltern sehen offene Bitten um Joker, ein Kind-Gerät füllt seine Joker bei neuen Freigaben auf. */
    private suspend fun nachschubPruefen() {
        val n = familie.lesen(FamilienProtokoll.TYP_NACHSCHUB) ?: return
        val fr = familie.lesen(FamilienProtokoll.TYP_FREIGABE) ?: return
        val nachschub = n.map { it.alsDatensatz() }
        val freigaben = fr.map { it.alsDatensatz() }
        if (familie.istEltern) {
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

    // ---- Duell ----

    var duelle: List<DuellHerausforderung> by mutableStateOf(emptyList())
    var duellAntworten: List<DuellAntwort> by mutableStateOf(emptyList())
    var duellMeldung: String by mutableStateOf("")
    var duellBeschaeftigt: Boolean by mutableStateOf(false)
    var duellSpiel: DuellSpiel? by mutableStateOf(null)

    /** Wer das Duell spielt: der Name aus den Einstellungen, sonst die Rolle. */
    val duellName: String
        get() = fortschritt.name.trim().take(14).ifEmpty { if (fortschritt.jokerRolle == "eltern") "Eltern" else "Kind" }

    /** Die Stufe zur Klasse des Kindes. Eltern rechnen standardmäßig auf Profi. */
    val duellMeineStufe: DuellStufe
        get() = if (fortschritt.jokerRolle == "eltern") DuellStufe.PROFI else DuellStufe.KLASSE3

    fun oeffneDuell() {
        ansicht = Ansicht.Duell
        duellLaden()
    }

    fun duellLaden() {
        if (!familieBereit || duellBeschaeftigt) return
        duellBeschaeftigt = true
        viewModelScope.launch {
            val d = familie.lesen(FamilienProtokoll.TYP_DUELL)
            val a = if (d == null) null else familie.lesen(FamilienProtokoll.TYP_DUELLANTWORT)
            if (d == null || a == null) {
                duellMeldung = "Die Duelle konnten nicht geladen werden. Ist das Internet an?"
            } else {
                val neu = d.mapNotNull { DuellPost.herausforderung(it.text("json")) }
                val antw = a.mapNotNull { DuellPost.antwort(it.text("json")) }
                // Was gerade erst geschickt wurde, steht vielleicht noch nicht in der Liste.
                val ids = neu.map { it.id }.toSet()
                val antwIds = antw.map { it.id }.toSet()
                duelle = neu + duelle.filter { it.id !in ids && it.vonGeraet == fortschritt.duellGeraet }
                duellAntworten = antw + duellAntworten.filter { it.id !in antwIds && it.geraet == fortschritt.duellGeraet }
                duellMeldung = ""
            }
            duellBeschaeftigt = false
        }
    }

    fun duellNeu(meine: DuellStufe, andere: DuellStufe) {
        duellSpiel = DuellSpiel(meine, DuellRegeln.aufgaben(meine), null, andere, DuellRegeln.aufgaben(andere))
    }

    fun duellAnnehmen(d: DuellHerausforderung) {
        duellSpiel = DuellSpiel(d.anStufe, d.aufgaben.ifEmpty { DuellRegeln.aufgaben(d.anStufe) }, d)
    }

    fun duellSpielBeenden() {
        duellSpiel = null
    }

    /** Alle Aufgaben sind gerechnet: Ergebnis in die Familien-Cloud schicken. */
    fun duellFertig(stand: DuellStand) {
        val spiel = duellSpiel ?: return
        val jetzt = DuellRegeln.zeitpunktJetzt()
        val koerper: kotlinx.serialization.json.JsonObject
        val herausforderung = spiel.herausforderung
        if (herausforderung == null) {
            val d = DuellHerausforderung(
                id = java.util.UUID.randomUUID().toString(), von = duellName, vonGeraet = fortschritt.duellGeraet,
                vonStufe = spiel.stufe, vonStand = stand, anStufe = spiel.anStufe, aufgaben = spiel.fuerAndere, zeitpunkt = jetzt,
            )
            duelle = duelle + d
            koerper = FamilienProtokoll.duell(d.id, DuellPost.text(d))
        } else {
            val a = DuellAntwort(herausforderung.id, duellName, fortschritt.duellGeraet, stand, jetzt)
            duellAntworten = duellAntworten + a
            koerper = FamilienProtokoll.duellAntwort(a.duellID, DuellPost.text(a))
        }
        if (!familieBereit) return
        viewModelScope.launch {
            val e = familie.senden(koerper)
            duellMeldung = if (e.ok) "" else "Das Ergebnis ging nicht raus. ${e.fehler}"
        }
    }

    fun jokerNachschubAnfragen() {
        if (!familieBereit) return
        jokerSenden(FamilienProtokoll.nachschub(neueKennung("nach"), fortschritt.name))
    }

    fun jokerFreigeben() {
        if (!familieBereit) return
        jokerSenden(FamilienProtokoll.freigabe(neueKennung("frei"), fortschritt.jokerLimit))
    }

    private fun neueKennung(vorsatz: String): String =
        FamilienProtokoll.neueKennung(vorsatz, java.util.UUID.randomUUID().toString())

    private fun jokerSenden(koerper: kotlinx.serialization.json.JsonObject, verbraucht: Boolean = false) {
        if (jokerBeschaeftigt) return
        jokerBeschaeftigt = true
        viewModelScope.launch {
            val e = familie.senden(koerper)
            jokerBeschaeftigt = false
            if (e.ok) {
                if (verbraucht) behalte(fortschritt.mitJokerGenutzt(Tag.heute()))
                jokerMeldung = "Gesendet."
                jokerLaden()
            } else {
                jokerMeldung = e.fehler
            }
        }
    }

    fun jokerFrageSenden(fach: String, uebung: String, text: String) {
        if (text.isBlank() || !familieBereit) return
        if (fortschritt.jokerUebrig(Tag.heute()) <= 0) {
            jokerMeldung = "Heute sind keine Joker mehr übrig. Du kannst neue anfragen."
            return
        }
        jokerSenden(FamilienProtokoll.frage(neueKennung("frage"), fortschritt.name, fach, uebung, text), verbraucht = true)
    }

    fun jokerAntwortSenden(frageId: String, emoji: String, text: String) {
        if (text.isBlank() || !familieBereit) return
        jokerSenden(FamilienProtokoll.antwort(neueKennung("antw"), frageId, emoji, text))
    }

    fun jokerDaumen(frageId: String, antwortId: String) {
        if (!familieBereit) return
        jokerSenden(FamilienProtokoll.daumen(frageId, antwortId))
    }

    // Ergebnisse der Kinder für die Eltern

    var ergebnisse: List<de.cemaras.yem1n.logik.RundenErgebnisEintrag> by mutableStateOf(emptyList())
        private set
    /** Namen der Kinder-Geräte der Familie, auch wenn noch keine Runde angekommen ist (aus der Geräteliste der Eltern). */
    val angemeldeteKinder: List<String>
        get() = familie.geraete.filter { it.rolle == "kind" && it.status == "aktiv" }
            .map { it.name.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
    var ergebnisseMeldung: String by mutableStateOf("")
        private set
    var ergebnisseBeschaeftigt: Boolean by mutableStateOf(false)
        private set

    fun oeffneErgebnisse() {
        ansicht = Ansicht.ErgebnisseEltern
        ergebnisseLaden()
    }

    fun ergebnisseLaden() {
        if (!familieBereit || ergebnisseBeschaeftigt) return
        ergebnisseBeschaeftigt = true
        viewModelScope.launch {
            val daten = familie.lesen(FamilienProtokoll.TYP_ERGEBNIS, 10)
            familie.geraeteLaden()
            if (daten == null) {
                ergebnisseMeldung = "Keine Verbindung. Ist das Internet an?"
            } else {
                ergebnisse = FamilienProtokoll.ergebnisse(daten)
                ergebnisseMeldung = when {
                    ergebnisse.isNotEmpty() -> ""
                    angemeldeteKinder.isNotEmpty() -> "Verbunden: ${angemeldeteKinder.joinToString(", ")}. Noch keine Runde angekommen."
                    else -> "Noch keine Runden angekommen."
                }
            }
            ergebnisseBeschaeftigt = false
        }
    }

    fun euleEinfuehrungGesehen() = behalte(fortschritt.copy(euleEinfuehrung = true))

    fun setzeErinnerung(an: Boolean) = behalte(fortschritt.copy(haustierErinnerung = an))

    fun setzeWochenbericht(an: Boolean) = behalte(fortschritt.copy(wochenberichtAn = an))

    fun setzeLernzeit(an: Boolean) = behalte(fortschritt.copy(lernzeitAn = an))

    fun setzeLernzeitUhrzeit(minuten: Int) = behalte(fortschritt.copy(lernzeitMinuten = minuten.coerceIn(0, 24 * 60 - 1)))

    /** Nach der Frage nach der Erlaubnis noch einmal planen. */
    fun erinnerungNeuPlanen() {
        Erinnerung.planen(getApplication(), fortschritt.haustier, fortschritt.haustierErinnerung)
    }

    fun setzeVorschulAn(an: Boolean) = behalte(fortschritt.copy(vorschulAn = an))

    fun setzeSprachenAn(an: Boolean) = behalte(fortschritt.copy(sprachenAn = an))

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

    fun setzeAutoVorlesen(an: Boolean) {
        if (!an) sprecher.stopp()
        behalte(fortschritt.copy(autoVorlesen = an))
    }

    /** Die Aufgabe beim Üben vorlesen (Lautsprecher-Knopf oder automatisch). */
    fun aufgabeVorlesen(a: de.cemaras.yem1n.daten.Aufgabe) {
        if (!de.cemaras.yem1n.logik.VorleseText.vorlesbar(a.art)) return
        sprecher.sprich(de.cemaras.yem1n.logik.VorleseText.aufgabe(a.frage, a.rechnung))
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

    // Lernwelt

    val bundesland: de.cemaras.yem1n.logik.Bundesland
        get() = de.cemaras.yem1n.logik.Bundesland.aus(fortschritt.bundesland)

    // Editoren

    /** Der angefangene Entwurf einer Arbeit (bleibt auf dem Gerät, bis er gespeichert oder verworfen wird). */
    var entwurf: Arbeit? by mutableStateOf(
        speicher.ladeText(DATEI_ENTWURF)?.let {
            try {
                PaketLeser.json.decodeFromString(de.cemaras.yem1n.daten.ArbeitPaket.serializer(), it).zuArbeit()
            } catch (e: Exception) { null }
        },
    )
        private set

    /** 0 = Arbeit, 1 = Übung, 2 = Aufgabe */
    var editorEbene: Int by mutableStateOf(0)
    var editorUebung: Int by mutableStateOf(0)
    var editorAufgabe: Int by mutableStateOf(-1)

    fun oeffneEditor() {
        editorEbene = 0
        ansicht = Ansicht.Editor
    }

    fun setzeEntwurf(neu: Arbeit?) {
        entwurf = neu
        speicher.speichereText(DATEI_ENTWURF, neu?.let { de.cemaras.yem1n.logik.PaketJson.text(it) })
        if (neu == null) editorEbene = 0
    }

    /** Alle Aufgaben, die es schon gibt, damit Vorlagen nichts doppelt erzeugen. */
    fun vorhandeneAufgaben(): MutableSet<String> {
        val s = MatheGenerator.vorhandeneSchluessel(alleArbeiten)
        entwurf?.let { s += MatheGenerator.vorhandeneSchluessel(listOf(it)) }
        return s
    }

    /** Prüft den Entwurf und legt ihn als Arbeit ab. Gibt einen Fehlertext zurück oder null. */
    fun entwurfSpeichern(): String? {
        val e = entwurf ?: return "Es gibt keinen Entwurf."
        val schwer = de.cemaras.yem1n.logik.EntwurfPruefer.pruefe(e).firstOrNull { it.schwer }
        if (schwer != null) return schwer.text
        val fehler = fuegeArbeitEin(de.cemaras.yem1n.logik.PaketJson.text(e))
        if (fehler == null) setzeEntwurf(null)
        return fehler
    }

    var eigeneThemen: List<de.cemaras.yem1n.logik.EigenThema> by mutableStateOf(
        de.cemaras.yem1n.logik.EigeneThemen.lese(speicher.ladeText(DATEI_THEMEN)).also {
            de.cemaras.yem1n.logik.EigeneThemen.liste = it
        },
    )
        private set

    /** Das Thema, das im Vokabel-Editor gerade bearbeitet wird (null = Liste). */
    var vokabelThema: de.cemaras.yem1n.logik.EigenThema? by mutableStateOf(null)

    fun oeffneVokabelEditor() {
        vokabelThema = null
        ansicht = Ansicht.VokabelEditor
    }

    fun setzeEigeneThemen(neu: List<de.cemaras.yem1n.logik.EigenThema>) {
        eigeneThemen = neu
        de.cemaras.yem1n.logik.EigeneThemen.liste = neu
        speicher.speichereText(DATEI_THEMEN, if (neu.isEmpty()) null else de.cemaras.yem1n.logik.EigeneThemen.text(neu))
    }

    fun oeffneSprachen() {
        ansicht = Ansicht.Sprachen
    }

    fun oeffneLernwelt() {
        ansicht = Ansicht.Lernwelt
    }

    fun waehleBundesland(land: de.cemaras.yem1n.logik.Bundesland) {
        behalte(fortschritt.copy(bundesland = land.schluessel))
    }

    /** 0 heißt: keine Klasse gewählt. */
    fun waehleLernKlasse(klasse: Int) {
        behalte(fortschritt.copy(lernKlasse = klasse))
    }

    /** Startet ein Thema der Lernwelt mit frischen Aufgaben. Sterne und Bestwert bleiben erhalten. */
    fun startLernThema(thema: de.cemaras.yem1n.logik.LernThema) {
        val arbeit = de.cemaras.yem1n.logik.Lernwelt.arbeit(thema, bundesland)
        lernArbeit = arbeit
        lernThema = thema
        runde = Runde.start(arbeit.uebungen[0].aufgaben)
        verdient = 0
        haustierText = null
        neuePokale = emptyList()
        ansicht = Ansicht.Ueben(LERNWELT, 0)
    }

    /** Die Sterne eines Themas, 0 bis 3. */
    fun lernSterne(thema: de.cemaras.yem1n.logik.LernThema): Int {
        val k = "Klasse ${thema.klasse(bundesland)}"
        return fortschritt.stand("$k|${thema.fach.anzeige}|${de.cemaras.yem1n.logik.Lernwelt.ARBEIT_TITEL}", thema.titel).sterne
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
        if (a.arbeit == LERNWELT) {
            lernThema?.let { startLernThema(it) }
            return
        }
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
                ansicht = if (a.arbeit == LERNWELT) {
                    Ansicht.Lernwelt
                } else if (a.arbeit != SONDER || sonderArt == SonderArt.TRAINING) {
                    Ansicht.Uebungen(a.arbeit)
                } else {
                    Ansicht.Start
                }
            }
            Ansicht.Editor -> if (editorEbene > 0) editorEbene-- else ansicht = Ansicht.Start
            Ansicht.VokabelEditor -> if (vokabelThema != null) vokabelThema = null else ansicht = Ansicht.Start
            is Ansicht.Uebungen, Ansicht.Einstellungen, Ansicht.Erfolge, Ansicht.Vorschule, Ansicht.Spiele, Ansicht.HaustierSeite, Ansicht.Joker, Ansicht.Familie, Ansicht.ErgebnisseEltern, Ansicht.Duell, Ansicht.Lernwelt, Ansicht.Sprachen -> ansicht = Ansicht.Start
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
        if ((ansicht as? Ansicht.Ueben)?.arbeit == LERNWELT) return
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
     * Merkt das Ergebnis der Runde vor und schickt es an die Eltern (über den Familien-Dienst), wenn dieses Gerät
     * zur Familie gehört und dem Kind gehört. Was nicht ankommt, bleibt gemerkt und wird später noch einmal geschickt.
     */
    private fun ergebnisMelden(klasse: String, fach: String, arbeit: String, uebung: String, s: RundenStand) {
        if (!familieBereit) return
        if (!familie.aktiv || familie.zugang?.rolle != "kind") {
            ergebnisStatus = "Dieses Gerät gehört zu den Eltern, darum wird die Runde nicht gesendet."
            return
        }
        val e = de.cemaras.yem1n.logik.RundenErgebnisEintrag(
            eintragId = FamilienProtokoll.neueKennung("erg", java.util.UUID.randomUUID().toString()),
            klasse = klasse, fach = fach, arbeit = arbeit,
            uebung = uebung, richtig = s.richtig, gesamt = s.aufgaben.size, angesehen = s.angeschaut,
            sterne = s.sterne, kind = fortschritt.name.trim(), zeitpunkt = System.currentTimeMillis(),
            details = de.cemaras.yem1n.logik.AntwortProtokoll.json(de.cemaras.yem1n.logik.AntwortProtokoll.eintraege(s)),
        )
        val offen = (fortschritt.offeneErgebnisse + FamilienProtokoll.ergebnis(e).toString()).takeLast(50)
        behalte(fortschritt.copy(offeneErgebnisse = offen))
        offeneErgebnisseSenden()
    }

    /** Handschrift: Erkennung von Google, das Modell lädt einmal nach Wunsch. */
    val stiftLeser = de.cemaras.yem1n.speicher.StiftLeser()
    var stiftStand: de.cemaras.yem1n.speicher.StiftLeser.Stand by mutableStateOf(de.cemaras.yem1n.speicher.StiftLeser.Stand.UNBEKANNT)

    fun setzeStift(an: Boolean) {
        behalte(fortschritt.copy(stift = an))
        if (an) stiftPruefen()
    }

    fun stiftPruefen() = stiftLeser.pruefen { stiftStand = it }

    fun stiftLaden() = stiftLeser.laden { stiftStand = it }

    /** Soll der Einrichtungsassistent gezeigt werden? Beim ersten Start, oder wenn er in Technik noch einmal gewünscht ist. */
    var einrichtungNochmal: Boolean by mutableStateOf(false)
    val zeigeEinrichtung: Boolean get() = einrichtungNochmal || Profil.einrichtungNoetig(fortschritt)

    fun einrichtungAbschliessen(rolle: String, name: String, klasse: String, sprachen: Boolean, rosa: Boolean, ziel: Int, auto: Boolean) {
        behalte(Profil.uebernehmen(fortschritt, rolle, name, klasse, sprachen, rosa, ziel, auto))
        einrichtungNochmal = false
        offeneErgebnisseSenden()
    }

    fun einrichtungUeberspringen() {
        behalte(fortschritt.copy(profilFertig = true))
        einrichtungNochmal = false
    }

    /** Ergebnis des letzten Verbindungstests (Einstellungen > Technik). */
    var diagnose: List<String> by mutableStateOf(emptyList())
    var diagnoseLaeuft: Boolean by mutableStateOf(false)

    fun diagnoseStarten() {
        if (diagnoseLaeuft) return
        diagnoseLaeuft = true
        diagnose = listOf("Ich teste die Verbindung ...")
        viewModelScope.launch {
            val zeilen = mutableListOf<String>()
            val ping = withContext(Dispatchers.IO) { de.cemaras.yem1n.speicher.FamilienNetz.post("/v2/ping", emptyMap(), "{}").first }
            zeilen += if (ping == 200) "✅ Familien-Dienst erreichbar" else "❌ Familien-Dienst nicht erreichbar (Internet prüfen)"
            zeilen += when {
                familie.aktiv -> "✅ Dieses Gerät gehört zur Familie (${if (familie.istEltern) "Eltern" else "Kind"})"
                familie.wartet -> "ℹ️ Dieses Gerät wartet auf die Freigabe der Eltern"
                else -> "ℹ️ Noch keine Familie verbunden"
            }
            zeilen += if (!einwilligungOffen) "✅ Einwilligung ist erteilt" else "ℹ️ Einwilligung fehlt, es wird nichts gesendet"
            zeilen += "ℹ️ Noch nicht gesendet: ${fortschritt.offeneErgebnisse.size}"
            diagnose = zeilen
            diagnoseLaeuft = false
        }
    }

    /** Schickt alle gemerkten Ergebnisse. Ein Fehler hält die Reihe an, der Rest bleibt für später. */
    fun offeneErgebnisseSenden() {
        if (ergebnisSendet || fortschritt.offeneErgebnisse.isEmpty() || !familieBereit) return
        ergebnisSendet = true
        viewModelScope.launch {
            var gesendet = 0
            var fehler: String? = null
            for (koerper in fortschritt.offeneErgebnisse) {
                val body = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(koerper) as? kotlinx.serialization.json.JsonObject
                } catch (e: Exception) {
                    null
                }
                // Reste aus der Zeit vor dem Familien-Dienst oder kaputte Einträge fliegen raus, damit sie nicht alles blockieren.
                if (body == null || !body.containsKey("typ")) {
                    behalte(fortschritt.copy(offeneErgebnisse = fortschritt.offeneErgebnisse - koerper))
                    continue
                }
                val r = familie.senden(body)
                if (r.ok) {
                    gesendet++
                    behalte(fortschritt.copy(offeneErgebnisse = fortschritt.offeneErgebnisse - koerper))
                } else {
                    fehler = r.fehler
                    // Eine abgelehnte Anfrage (400) würde nie ankommen, sie fliegt raus.
                    if (r.status == 400) behalte(fortschritt.copy(offeneErgebnisse = fortschritt.offeneErgebnisse - koerper))
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

    /** Belohnung zu Hause für das Wochenziel. Das Kind sieht den Text auf der Wochenziel-Karte. */
    fun setzeFamilienBelohnung(text: String) =
        behalte(fortschritt.copy(familienBelohnung = de.cemaras.yem1n.logik.Erfolge.familienBelohnungText(text)))

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
