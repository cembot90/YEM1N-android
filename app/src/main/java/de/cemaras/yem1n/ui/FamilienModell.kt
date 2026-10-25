package de.cemaras.yem1n.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.cemaras.yem1n.logik.ElternPin
import de.cemaras.yem1n.logik.Einladung
import de.cemaras.yem1n.logik.FamilienAntwort
import de.cemaras.yem1n.logik.FamilienClient
import de.cemaras.yem1n.logik.FamilienEintrag
import de.cemaras.yem1n.logik.FamilienGeraet
import de.cemaras.yem1n.logik.FamilienProtokoll
import de.cemaras.yem1n.logik.FamilienZugang
import de.cemaras.yem1n.speicher.FamilienNetz
import de.cemaras.yem1n.speicher.FamilienSpeicher
import de.cemaras.yem1n.speicher.GeraetSchluessel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Die Familie dieses Geräts: wer dazugehört, welche Geräte warten und alles, was über den Familien-Dienst geht
 * (Protokoll 2, `cloud/PROTOKOLL-V2.md`). Die Zustände sind für die Oberfläche lesbar, die Funktionen laufen im Hintergrund.
 */
class FamilienModell(context: Context) {
    private val speicher = FamilienSpeicher(context)
    private val client = FamilienClient(FamilienNetz, GeraetSchluessel).also { it.versatz = speicher.versatz }

    /** Null, solange das Gerät zu keiner Familie gehört. */
    var zugang: FamilienZugang? by mutableStateOf(speicher.laden())
        private set

    /** Die Geräte der Familie (nur für Eltern gefüllt). */
    var geraete: List<FamilienGeraet> by mutableStateOf(emptyList())
        private set

    /** Die letzte Einladung, solange sie noch gilt. */
    var einladung: Einladung? by mutableStateOf(null)
        private set

    var meldung: String by mutableStateOf("")
    var beschaeftigt: Boolean by mutableStateOf(false)
        private set

    /** Das Gerät gehört zur Familie und ist freigegeben. Nur dann gehen Nachrichten hin und her. */
    val aktiv: Boolean get() = zugang?.aktiv == true
    val wartet: Boolean get() = zugang?.wartet == true
    val istEltern: Boolean get() = zugang?.let { it.aktiv && it.istEltern } == true

    private fun setzeZugang(z: FamilienZugang?) {
        zugang = z
        speicher.speichern(z)
        if (z == null) {
            pinGesetzt = false
            entsperrtBis = 0L
        }
    }

    // ---- Eltern-PIN ----

    /** Gibt es auf diesem Gerät eine Eltern-PIN? */
    var pinGesetzt: Boolean by mutableStateOf(speicher.pinHash.isNotEmpty())
        private set

    private var entsperrtBis: Long by mutableLongStateOf(0L)

    /** Der Bereich der Eltern ist gerade offen (nach der richtigen PIN, kurze Zeit). */
    val entsperrt: Boolean get() = System.currentTimeMillis() < entsperrtBis

    /** Legt die PIN fest und öffnet den Bereich. Gibt einen Fehlertext zurück oder null. */
    fun pinSetzen(pin: String, wiederholt: String): String? {
        if (!ElternPin.gueltig(pin)) return "Die PIN besteht aus 4 bis 8 Ziffern."
        if (pin != wiederholt) return "Die beiden PINs sind nicht gleich."
        if (ElternPin.zuLeicht(pin)) return "Bitte keine PIN wie 1234 oder 0000. Das erraten Kinder schnell."
        val salz = ElternPin.salz(java.security.SecureRandom().let { r -> ByteArray(16).also { r.nextBytes(it) } })
        speicher.pinSalz = salz
        speicher.pinHash = ElternPin.hash(pin, salz)
        speicher.pinFehler = 0
        speicher.pinSperreBis = 0L
        pinGesetzt = true
        entsperrtBis = System.currentTimeMillis() + ElternPin.OFFEN_MS
        return null
    }

    /** Prüft die PIN. Gibt einen Text für die Oberfläche zurück oder null, wenn sie stimmt (dann ist der Bereich offen). */
    fun pinPruefen(pin: String): String? {
        val jetzt = System.currentTimeMillis()
        val sperreBis = speicher.pinSperreBis
        if (jetzt < sperreBis) return ElternPin.sperreText(sperreBis - jetzt)
        if (ElternPin.stimmt(pin, speicher.pinSalz, speicher.pinHash)) {
            speicher.pinFehler = 0
            entsperrtBis = jetzt + ElternPin.OFFEN_MS
            return null
        }
        val fehler = speicher.pinFehler + 1
        speicher.pinFehler = fehler
        val warten = ElternPin.sperreMs(fehler)
        if (warten > 0) {
            speicher.pinSperreBis = jetzt + warten
            return "Falsche PIN. " + ElternPin.sperreText(warten)
        }
        return "Falsche PIN."
    }

    /** Schließt den Bereich der Eltern sofort wieder. */
    fun sperren() {
        entsperrtBis = 0L
    }

    private fun versatzMerken() {
        if (speicher.versatz != client.versatz) speicher.versatz = client.versatz
    }

    /** Die Familie ist weg oder dieses Gerät wurde entfernt: alles vergessen, auch den Schlüssel. */
    private fun vergessen(text: String) {
        setzeZugang(null)
        GeraetSchluessel.loeschen()
        geraete = emptyList()
        einladung = null
        meldung = text
    }

    private suspend fun signiert(pfad: String, body: JsonObject = JsonObject(emptyMap())): FamilienAntwort {
        val z = zugang ?: return FamilienAntwort(-3, null)
        val a = withContext(Dispatchers.IO) { client.signiert(z, pfad, body) }
        versatzMerken()
        return a
    }

    // ---- Familie anlegen und beitreten ----

    /** Legt eine neue Familie an. Dieses Gerät wird das erste Eltern-Gerät. Gibt einen Fehlertext zurück oder null. */
    suspend fun anlegen(name: String): String? {
        beschaeftigt = true
        try {
            val a = withContext(Dispatchers.IO) {
                if (GeraetSchluessel.neuErzeugen() == null) FamilienAntwort(-1, null) else client.anlegen(name)
            }
            if (!a.ok || a.text("familie").isEmpty()) {
                withContext(Dispatchers.IO) { GeraetSchluessel.loeschen() }
                return a.fehler
            }
            setzeZugang(FamilienZugang(a.text("familie"), a.text("geraet"), name, "eltern", "aktiv"))
            meldung = ""
            return null
        } finally {
            beschaeftigt = false
        }
    }

    /** Tritt einer Familie mit dem Text aus dem QR-Code bei. Danach wartet das Gerät auf die Freigabe der Eltern. */
    suspend fun koppeln(qrText: String, name: String): String? {
        val (familie, token) = FamilienProtokoll.qrLesen(qrText) ?: return "Das ist kein QR-Code zum Verbinden. Bitte auf dem Gerät der Eltern einen neuen anzeigen."
        beschaeftigt = true
        try {
            val a = withContext(Dispatchers.IO) {
                if (GeraetSchluessel.neuErzeugen() == null) FamilienAntwort(-1, null) else client.koppeln(familie, token, name)
            }
            if (!a.ok || a.text("geraet").isEmpty()) {
                withContext(Dispatchers.IO) { GeraetSchluessel.loeschen() }
                return when (a.status) {
                    403 -> "Dieser QR-Code gilt nicht mehr. Bitte auf dem Gerät der Eltern einen neuen anzeigen."
                    else -> a.fehler
                }
            }
            val rolle = a.text("rolle").ifEmpty { "kind" }
            setzeZugang(FamilienZugang(familie, a.text("geraet"), name, rolle, a.text("status").ifEmpty { "wartet" }))
            meldung = ""
            return null
        } finally {
            beschaeftigt = false
        }
    }

    /** Fragt den Stand dieses Geräts ab (wartet, aktiv, abgelehnt). Gibt true zurück, wenn sich etwas geändert hat. */
    suspend fun statusPruefen(): Boolean {
        val z = zugang ?: return false
        val a = signiert(FamilienProtokoll.PFAD_STATUS)
        when {
            a.status == 401 || a.status == 404 -> {
                vergessen("Dieses Gerät gehört nicht mehr zur Familie. Bitte neu verbinden.")
                return true
            }
            !a.ok -> return false
        }
        val neu = a.text("status").ifEmpty { z.status }
        val rolle = a.text("rolle").ifEmpty { z.rolle }
        if (neu == z.status && rolle == z.rolle) return false
        setzeZugang(z.copy(status = neu, rolle = rolle))
        return true
    }

    // ---- Eltern: Einladung und Geräte ----

    suspend fun einladungErzeugen(rolle: String): String? {
        val z = zugang ?: return FamilienProtokoll.fehlertext(401)
        beschaeftigt = true
        try {
            val a = signiert(FamilienProtokoll.PFAD_EINLADUNG, buildJsonObject { put("rolle", rolle) })
            if (!a.ok) return if (a.status == 429) "Es gibt schon fünf offene Einladungen. Bitte ein paar Minuten warten." else a.fehler
            einladung = Einladung(z.familie, rolle, a.text("token"), a.zahl("bis"))
            return null
        } finally {
            beschaeftigt = false
        }
    }

    fun einladungSchliessen() { einladung = null }

    suspend fun geraeteLaden(): Boolean {
        if (!istEltern) return false
        val a = signiert(FamilienProtokoll.PFAD_GERAETE)
        if (!a.ok) {
            if (a.status == 401 || a.status == 404) statusPruefen()
            return false
        }
        geraete = FamilienProtokoll.geraete(a.json ?: return false)
        return true
    }

    private suspend fun geraetAktion(pfad: String, id: String): String? {
        beschaeftigt = true
        try {
            val a = signiert(pfad, buildJsonObject { put("geraet", id) })
            if (!a.ok) return if (a.status == 409) "Das Gerät wartet nicht mehr auf eine Freigabe." else a.fehler
            geraeteLaden()
            return null
        } finally {
            beschaeftigt = false
        }
    }

    suspend fun erlauben(id: String) = geraetAktion(FamilienProtokoll.PFAD_ERLAUBEN, id)
    suspend fun ablehnen(id: String) = geraetAktion(FamilienProtokoll.PFAD_ABLEHNEN, id)
    suspend fun entfernen(id: String) = geraetAktion(FamilienProtokoll.PFAD_ENTFERNEN, id)

    // ---- Verlassen und löschen ----

    /**
     * Dieses Gerät verlässt die Familie. Ein Kind kann seine Einträge mitnehmen (löschen lassen).
     * Ein Gerät, das noch wartet oder abgelehnt wurde, geht immer: der Dienst räumt solche Geräte selbst auf.
     */
    suspend fun verlassen(eintraegeLoeschen: Boolean): String? {
        val nochNichtAktiv = zugang?.aktiv == false
        beschaeftigt = true
        try {
            val a = signiert(FamilienProtokoll.PFAD_AUSTRETEN, buildJsonObject { put("eintraegeLoeschen", eintraegeLoeschen) })
            if (!a.ok && !nochNichtAktiv && a.status != 401 && a.status != 404) {
                return if (a.status == 409) "Das ist das letzte Eltern-Gerät. Löscht stattdessen die ganze Familie." else a.fehler
            }
            vergessen("")
            return null
        } finally {
            beschaeftigt = false
        }
    }

    /** Löscht die ganze Familie mit allen Einträgen. Nur Eltern. */
    suspend fun familieLoeschen(): String? {
        beschaeftigt = true
        try {
            val a = signiert(FamilienProtokoll.PFAD_LOESCHEN, buildJsonObject { put("bestaetigung", "LOESCHEN") })
            if (!a.ok && a.status != 404) return a.fehler
            vergessen("")
            return null
        } finally {
            beschaeftigt = false
        }
    }

    /** Nur auf diesem Gerät vergessen (zum Beispiel beim Widerruf der Einwilligung). Die Familie bleibt bestehen. */
    fun lokalVergessen() {
        vergessen("")
    }

    // ---- Nachrichten ----

    /** Legt einen Eintrag ab. [body] kommt aus [FamilienProtokoll]. */
    suspend fun senden(body: JsonObject): FamilienAntwort {
        if (!aktiv) return FamilienAntwort(-3, null)
        return signiert(FamilienProtokoll.PFAD_SENDEN, body)
    }

    /** Liest alle Einträge einer Art, in höchstens [maxSeiten] Seiten. Null, wenn schon die erste Seite fehlt. */
    suspend fun lesen(typ: String, maxSeiten: Int = 5): List<FamilienEintrag>? {
        if (!aktiv) return null
        var seit = 0L
        val alle = mutableListOf<FamilienEintrag>()
        for (seite in 0 until maxSeiten) {
            val a = signiert(FamilienProtokoll.PFAD_LESEN, FamilienProtokoll.lesen(typ, seit, 200))
            val j = a.json
            if (!a.ok || j == null) return if (seite == 0) null else alle
            alle += FamilienProtokoll.eintraege(j)
            seit = FamilienProtokoll.weiterAb(j) ?: break
        }
        return alle
    }

    /** Meldet die Push-Kennung dieses Geräts an (Google FCM). */
    suspend fun pushAnmelden(token: String): Boolean = signiert(FamilienProtokoll.PFAD_PUSH, FamilienProtokoll.pushAnmelden(token)).ok

    suspend fun pushAbmelden(): Boolean = signiert(FamilienProtokoll.PFAD_PUSH, FamilienProtokoll.pushAbmelden()).ok
}
