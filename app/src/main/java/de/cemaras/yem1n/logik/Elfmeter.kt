package de.cemaras.yem1n.logik

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Elfmeterschießen: Regeln und Zahlen ohne Grafik, damit sie sich testen lassen.
 * Das Spielfeld ist 1024 mal 576 Punkte groß, y zählt von unten nach oben, unten liegt der Ball.
 */
object ElfmeterRegeln {
    const val BREITE = 1024f
    const val HOEHE = 576f

    // Das Tor
    const val TOR_LINKS = 262f
    const val TOR_RECHTS = 762f
    const val TOR_UNTEN = 300f
    const val TOR_OBEN = 470f
    const val TOR_MITTE = (TOR_LINKS + TOR_RECHTS) / 2

    /** Hier liegt der Ball vor dem Schuss. */
    const val BALL_X = 512f
    const val BALL_Y = 110f

    /** So weit kommt der Torwart seitlich mit den Händen. */
    const val REICHWEITE = 64f

    /** Nach so vielen Fehlschüssen ist das Spiel vorbei. */
    const val FEHLSCHUESSE_ERLAUBT = 3

    /** Alle so viele Tore steigt das Level. */
    const val TORE_PRO_LEVEL = 5

    /** Wohin der Ball fliegt, abhängig vom Wischen. Nach oben wischen zielt aufs Tor. */
    fun ziel(wischX: Float, wischY: Float): Pair<Float, Float> =
        (TOR_MITTE + wischX * 1.5f) to (TOR_UNTEN + max(0f, wischY - 120f) * 0.75f)

    /** Flugzeit in Sekunden: schnelles Wischen gibt einen schnellen Schuss. */
    fun flugzeit(wischTempo: Float): Double {
        val t = 0.85 - min(max(wischTempo, 0f), 3000f).toDouble() / 3000.0 * 0.4
        return max(0.42, min(0.85, t))
    }

    fun istImTor(x: Float, y: Float): Boolean =
        x > TOR_LINKS + 14f && x < TOR_RECHTS - 14f && y >= TOR_UNTEN && y < TOR_OBEN - 10f

    /** Schüsse nah an Pfosten oder Latte zählen als Winkel und bringen mehr Punkte. */
    fun istWinkel(x: Float, y: Float): Boolean {
        if (!istImTor(x, y)) return false
        val seitlich = abs(x - TOR_MITTE) > (TOR_RECHTS - TOR_LINKS) / 2f * 0.72f
        val oben = y > TOR_UNTEN + (TOR_OBEN - TOR_UNTEN) * 0.72f
        return seitlich || oben
    }

    /** Wie lange der Torwart braucht, bis er losspringt (Sekunden). */
    fun reaktion(level: Int): Double = max(0.08, 0.32 - 0.03 * (level - 1))

    /** Wie schnell der Torwart zur Seite kommt (Punkte pro Sekunde). */
    fun tempo(level: Int): Float = min(950f, 380f + 70f * (level - 1))

    /** Wie oft der Torwart in die falsche Ecke springt. */
    fun falscheEckeChance(level: Int): Double = max(0.06, 0.35 - 0.04 * (level - 1))

    /** Bis zu dieser Höhe kommt der Torwart mit einem Sprung. */
    fun sprungHoehe(level: Int): Float = min(TOR_OBEN - 6f, TOR_UNTEN + 128f + 6f * (level - 1))

    enum class Ergebnis { TOR, WINKEL, GEHALTEN, DANEBEN }

    class Urteil(val ergebnis: Ergebnis, val torwartZielX: Float)

    /**
     * Entscheidet den Schuss. [zufall] liegt zwischen 0 und 1 und bestimmt, ob der Torwart
     * sich verschätzt. Gibt auch zurück, wohin der Torwart springt.
     */
    fun urteil(zielX: Float, zielY: Float, torwartX: Float, flugzeit: Double, level: Int, zufall: Double): Urteil {
        val verschaetzt = zufall < falscheEckeChance(level)
        val gewollt = if (verschaetzt) 2 * TOR_MITTE - zielX else zielX
        val weg = (max(0.0, flugzeit - reaktion(level)) * tempo(level)).toFloat()
        val schritt = min(max(gewollt - torwartX, -weg), weg)
        val endX = min(max(torwartX + schritt, TOR_LINKS + 30f), TOR_RECHTS - 30f)
        if (!istImTor(zielX, zielY)) return Urteil(Ergebnis.DANEBEN, endX)
        val erreicht = abs(zielX - endX) <= REICHWEITE && zielY <= sprungHoehe(level)
        if (erreicht) return Urteil(Ergebnis.GEHALTEN, endX)
        return Urteil(if (istWinkel(zielX, zielY)) Ergebnis.WINKEL else Ergebnis.TOR, endX)
    }

    /** Ein Tor bringt 1 Punkt, in den Winkel 3. Ab dem dritten Treffer in Folge gibt es einen Extrapunkt. */
    fun punkte(winkel: Boolean, serie: Int): Int = (if (winkel) 3 else 1) + (if (serie >= 3) 1 else 0)

    fun level(tore: Int): Int = max(0, tore) / TORE_PRO_LEVEL + 1
}

data class ElfmeterEreignis(val art: Art, val x: Float, val y: Float, val text: String) {
    enum class Art { TOR, GEHALTEN, DANEBEN, LEVEL }
}

/** Der Ablauf des Elfmeterschießens ohne Grafik. */
class ElfmeterSpiel(private val zufall: Random = Random.Default) {
    enum class Phase { WARTEN, BEREIT, FLUG, NACHSPIEL, VORBEI }

    companion object {
        const val TORWART_Y = ElfmeterRegeln.TOR_UNTEN + 64f
        const val NACHSPIEL_DAUER = 1.3f
        const val RUECKLAUF = 0.3f
        private const val WIPPEN_DAUER = 2.8f
        private const val WIPPEN_WEITE = 36f
    }

    var phase = Phase.WARTEN
        private set
    var punkte = 0
        private set
    var tore = 0
        private set
    var serie = 0
        private set
    var fehlschuesse = 0
        private set
    var level = 1
        private set

    var ballX = ElfmeterRegeln.BALL_X
        private set
    var ballY = ElfmeterRegeln.BALL_Y
        private set
    var ballSkala = 1f
        private set
    var ballAlpha = 1f
        private set
    var torwartX = ElfmeterRegeln.TOR_MITTE
        private set
    var torwartY = TORWART_Y
        private set

    /** Neigung des Torwarts in Bogenmaß, positiv ist gegen den Uhrzeigersinn. */
    var torwartDreh = 0f
        private set

    /** Zielvorschau beim Wischen, sonst null. */
    var vorschau: Pair<Float, Float>? = null
        private set

    /** Wackeln des Netzes nach einem Tor: 0 bis 1. */
    var netz = 0f
        private set
    var zeit = 0f
        private set

    val ereignisse = mutableListOf<ElfmeterEreignis>()
    var beiEnde: ((Int) -> Unit)? = null

    private var wischStartX = 0f
    private var wischStartY = 0f
    private var wischt = false
    private var wippZeit = 0f
    private var rueck = 0f
    private var rueckX = 0f
    private var rueckY = 0f
    private var schussZeit = 0f
    private var nachZeit = 0f
    private var flug = 0.5
    private var zielX = 0f
    private var zielY = 0f
    private var urteil: ElfmeterRegeln.Urteil? = null
    private var startX = 0f
    private var startY = 0f
    private var levelMeldung = 0f

    /** Eine neue Berührung bei (x, y) in Bühnenpunkten: Spiel starten, neu beginnen oder einen Wisch anfangen. */
    fun beruehre(x: Float, y: Float) {
        if (phase == Phase.VORBEI) {
            neuStarten()
            return
        }
        if (phase == Phase.WARTEN) phase = Phase.BEREIT
        if (phase != Phase.BEREIT) return
        // Der Wisch muss in der unteren Hälfte beginnen
        if (y >= ElfmeterRegeln.TOR_UNTEN - 20f) return
        wischStartX = x
        wischStartY = y
        wischt = true
    }

    fun bewege(x: Float, y: Float) {
        if (phase != Phase.BEREIT || !wischt) return
        vorschau = ElfmeterRegeln.ziel(x - wischStartX, y - wischStartY)
    }

    fun abbrechen() {
        wischt = false
        vorschau = null
    }

    /** Finger weg bei (x, y). [dauer] ist die Zeit des Wischens in Sekunden. */
    fun loslassen(x: Float, y: Float, dauer: Float) {
        if (phase != Phase.BEREIT || !wischt) return
        wischt = false
        vorschau = null
        val dx = x - wischStartX
        val dy = y - wischStartY
        val laenge = hypot(dx, dy)
        if (laenge <= 50f || dy <= 30f) return
        val tempo = laenge / max(dauer, 0.05f)
        schiesse(dx, dy, ElfmeterRegeln.flugzeit(tempo))
    }

    private fun schiesse(dx: Float, dy: Float, flugzeit: Double) {
        val z = ElfmeterRegeln.ziel(dx, dy)
        zielX = z.first
        zielY = z.second
        flug = flugzeit
        startX = torwartX
        startY = torwartY
        urteil = ElfmeterRegeln.urteil(zielX, zielY, torwartX, flugzeit, level, zufall.nextDouble())
        schussZeit = 0f
        nachZeit = 0f
        phase = Phase.FLUG
    }

    /** Für Tests: schießt direkt auf ein Ziel. */
    internal fun schiesseAuf(wischX: Float, wischY: Float, flugzeit: Double) {
        if (phase == Phase.WARTEN) phase = Phase.BEREIT
        if (phase == Phase.BEREIT) schiesse(wischX, wischY, flugzeit)
    }

    private fun neuStarten() {
        punkte = 0
        tore = 0
        serie = 0
        fehlschuesse = 0
        level = 1
        levelMeldung = 0f
        legeBallHin()
        torwartX = ElfmeterRegeln.TOR_MITTE
        torwartY = TORWART_Y
        torwartDreh = 0f
        wippZeit = 0f
        rueck = 0f
        phase = Phase.BEREIT
    }

    private fun legeBallHin() {
        ballX = ElfmeterRegeln.BALL_X
        ballY = ElfmeterRegeln.BALL_Y
        ballSkala = 1f
        ballAlpha = 1f
    }

    private fun leicht(u: Float): Float {
        val k = min(max(u, 0f), 1f)
        return 1f - (1f - k) * (1f - k)
    }

    fun schritt(dtRoh: Float) {
        val dt = min(dtRoh, 1f / 30f)
        if (dt <= 0f) return
        zeit += dt
        if (netz > 0f) netz = max(0f, netz - dt / 0.28f)
        when (phase) {
            Phase.WARTEN, Phase.BEREIT -> wippe(dt)
            Phase.FLUG -> {
                schussZeit += dt
                bewegeTorwart()
                val u = leicht((schussZeit / flug).toFloat())
                ballX = ElfmeterRegeln.BALL_X + (zielX - ElfmeterRegeln.BALL_X) * u
                ballY = ElfmeterRegeln.BALL_Y + (zielY - ElfmeterRegeln.BALL_Y) * u
                ballSkala = 1f - 0.45f * u
                if (schussZeit >= flug) landet()
            }
            Phase.NACHSPIEL -> {
                schussZeit += dt
                nachZeit += dt
                bewegeTorwart()
                bewegeBallNach()
                if (levelMeldung > 0f) {
                    levelMeldung -= dt
                    if (levelMeldung <= 0f) {
                        ereignisse.add(ElfmeterEreignis(ElfmeterEreignis.Art.LEVEL, ElfmeterRegeln.BREITE / 2, 180f, "Level $level"))
                    }
                }
                if (nachZeit >= NACHSPIEL_DAUER) naechsterSchuss()
            }
            Phase.VORBEI -> Unit
        }
    }

    private fun wippe(dt: Float) {
        if (rueck > 0f) {
            rueck = max(0f, rueck - dt)
            val u = 1f - rueck / RUECKLAUF
            torwartX = rueckX + (ElfmeterRegeln.TOR_MITTE - rueckX) * u
            torwartY = rueckY + (TORWART_Y - rueckY) * u
            return
        }
        wippZeit += dt
        torwartX = ElfmeterRegeln.TOR_MITTE - WIPPEN_WEITE * sin(2.0 * PI * wippZeit / WIPPEN_DAUER).toFloat()
        torwartY = TORWART_Y
    }

    private fun bewegeTorwart() {
        val u = urteil ?: return
        val wartet = ElfmeterRegeln.reaktion(level).toFloat()
        val t = schussZeit - wartet
        if (t <= 0f) return
        val e = leicht(t / (flug * 0.9).toFloat())
        val sprungY = TORWART_Y + min(max(zielY - ElfmeterRegeln.TOR_UNTEN - 40f, 0f), 120f)
        torwartX = startX + (u.torwartZielX - startX) * e
        torwartY = startY + (sprungY - startY) * e
        val richtung = if (u.torwartZielX < startX) 1f else -1f
        val weit = abs(u.torwartZielX - startX) > 90f
        torwartDreh = if (weit) 0.7f * richtung * leicht(t / (flug * 0.6).toFloat()) else 0f
    }

    private fun bewegeBallNach() {
        val u = urteil ?: return
        ballSkala = 0.55f
        when (u.ergebnis) {
            ElfmeterRegeln.Ergebnis.TOR, ElfmeterRegeln.Ergebnis.WINKEL -> {
                ballX = zielX
                ballY = zielY + 14f * min(1f, nachZeit / 0.15f)
                ballAlpha = 1f
            }
            ElfmeterRegeln.Ergebnis.GEHALTEN -> {
                val k = min(1f, nachZeit / 0.45f)
                val weg = if (zielX < ElfmeterRegeln.TOR_MITTE) -170f else 170f
                ballX = zielX + weg * k
                ballY = zielY - 150f * k
                ballAlpha = 1f - k
            }
            ElfmeterRegeln.Ergebnis.DANEBEN -> {
                val k = min(1f, nachZeit / 0.4f)
                ballX = zielX + (zielX - ElfmeterRegeln.BALL_X) * 0.3f * k
                ballY = zielY + 80f * k
                ballAlpha = 1f - k
            }
        }
    }

    private fun landet() {
        val u = urteil ?: return
        val mitteX = ElfmeterRegeln.BREITE / 2
        phase = Phase.NACHSPIEL
        nachZeit = 0f
        when (u.ergebnis) {
            ElfmeterRegeln.Ergebnis.TOR, ElfmeterRegeln.Ergebnis.WINKEL -> {
                val winkel = u.ergebnis == ElfmeterRegeln.Ergebnis.WINKEL
                tore += 1
                serie += 1
                val neu = ElfmeterRegeln.punkte(winkel, serie)
                punkte += neu
                netz = 1f
                ereignisse.add(ElfmeterEreignis(ElfmeterEreignis.Art.TOR, zielX, zielY, if (winkel) "WINKEL! +$neu" else "TOR! +$neu"))
                val neuesLevel = ElfmeterRegeln.level(tore)
                if (neuesLevel > level) {
                    level = neuesLevel
                    levelMeldung = 0.9f
                }
            }
            ElfmeterRegeln.Ergebnis.GEHALTEN -> {
                serie = 0
                fehlschuesse += 1
                ereignisse.add(ElfmeterEreignis(ElfmeterEreignis.Art.GEHALTEN, mitteX, 228f, "Gehalten!"))
            }
            ElfmeterRegeln.Ergebnis.DANEBEN -> {
                serie = 0
                fehlschuesse += 1
                ereignisse.add(ElfmeterEreignis(ElfmeterEreignis.Art.DANEBEN, mitteX, 228f, "Daneben"))
            }
        }
    }

    private fun naechsterSchuss() {
        if (fehlschuesse >= ElfmeterRegeln.FEHLSCHUESSE_ERLAUBT) {
            phase = Phase.VORBEI
            beiEnde?.invoke(punkte)
            return
        }
        legeBallHin()
        torwartDreh = 0f
        rueckX = torwartX
        rueckY = torwartY
        rueck = RUECKLAUF
        wippZeit = 0f
        urteil = null
        phase = Phase.BEREIT
    }
}
