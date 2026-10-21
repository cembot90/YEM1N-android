package de.cemaras.yem1n.logik

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Münz-Hockey: Werte. Der Computer muss schlagbar bleiben. Seine Geschwindigkeit liegt
 * deshalb immer deutlich unter der des schnellsten Pucks.
 */
object HockeyBalance {
    const val BREITE = 1024f
    const val HOEHE = 576f

    /** Rand um das Spielfeld. */
    const val RAND = 36f

    /** Höhe der Toröffnung. */
    const val TOR_HOEHE = 190f

    const val PUCK_RADIUS = 24f
    const val SCHLAEGER_RADIUS = 42f
    const val PUCK_MAX_TEMPO = 1100f
    const val PUCK_MIN_STOSS = 260f

    /** Wer zuerst so viele Tore hat, gewinnt das Spiel. */
    const val TORE_ZUM_SIEG = 5
    const val HOECHSTES_LEVEL = 8

    fun level(nummer: Int): Int = min(max(nummer, 1), HOECHSTES_LEVEL)

    /** Wie schnell der Computer-Schläger höchstens ist, in Punkten je Sekunde. */
    fun kiTempo(level: Int): Float = 380f + (level(level) - 1) * 55f

    /** Wie ungenau der Computer zielt. Wird mit dem Level besser, bleibt aber über null. */
    fun kiFehler(level: Int): Float = max(70f - (level(level) - 1) * 9f, 10f)

    /** Wie oft der Computer neu überlegt, in Sekunden. */
    fun kiReaktion(level: Int): Float = max(0.28f - (level(level) - 1) * 0.03f, 0.07f)

    /** Sieg im Level bringt Punkte, ein Torvorsprung bringt extra. */
    fun punkteFuerSieg(level: Int, vorsprung: Int): Int = 10 * level(level) + max(vorsprung, 0)
}

data class HockeyEreignis(val art: Art, val x: Float, val y: Float) {
    enum class Art { SCHLAG, TOR_ICH, TOR_KI, SIEG }
}

/** Der Ablauf des Münz-Hockeys ohne Grafik. Die Bühne ist 1024 mal 576 groß, y zählt von unten nach oben. */
class HockeySpiel(private val zufall: Random = Random.Default) {
    enum class Phase { WARTEN, LAEUFT, PAUSE, VORBEI }

    private enum class Danach { NICHTS, ANSTOSS_ICH, ANSTOSS_KI, NAECHSTES_LEVEL }

    companion object {
        const val LINKS = HockeyBalance.RAND
        const val RECHTS = HockeyBalance.BREITE - HockeyBalance.RAND
        const val UNTEN = HockeyBalance.RAND
        const val OBEN = HockeyBalance.HOEHE - HockeyBalance.RAND
        const val MITTE = HockeyBalance.BREITE / 2
        const val TOR_UNTEN = (HockeyBalance.HOEHE - HockeyBalance.TOR_HOEHE) / 2
        const val TOR_OBEN = TOR_UNTEN + HockeyBalance.TOR_HOEHE
        const val TREFFER_RADIUS = HockeyBalance.SCHLAEGER_RADIUS - 2f
        private const val MITTE_Y = HockeyBalance.HOEHE / 2
    }

    var phase = Phase.WARTEN
        private set
    var level = 1
        private set
    var toreIch = 0
        private set
    var toreKi = 0
        private set
    var punkte = 0
        private set

    var puckX = MITTE
        private set
    var puckY = MITTE_Y
        private set
    var puckVx = 0f
        private set
    var puckVy = 0f
        private set
    var puckDreh = 0f
        private set

    var meinX = LINKS + 150f
        private set
    var meinY = MITTE_Y
        private set
    var kiX = RECHTS - 150f
        private set
    var kiY = MITTE_Y
        private set

    /** Nach einem Levelsieg ist hier die Meldung, sonst false. */
    var siegMeldung = false
        private set
    var zeit = 0f
        private set

    val ereignisse = mutableListOf<HockeyEreignis>()
    var beiEnde: ((Int) -> Unit)? = null

    private var meinVx = 0f
    private var meinVy = 0f
    private var kiVx = 0f
    private var kiVy = 0f
    private var zielX = meinX
    private var zielY = meinY
    private var kiVersatzX = 0f
    private var kiVersatzY = 0f
    private var kiNeuRest = 0f
    private var anstossRest = 0f
    private var anstossRichtung = 1f
    private var pauseRest = 0f
    private var danach = Danach.NICHTS

    /** Eine Berührung bei (x, y): starten, neu beginnen oder den Schläger führen. */
    fun beruehre(x: Float, y: Float) {
        when (phase) {
            Phase.VORBEI -> neuStarten()
            Phase.WARTEN -> {
                phase = Phase.LAEUFT
                anstossRichtung = 1f
                anstossRest = 0.8f
            }
            else -> Unit
        }
        fuehre(x, y)
    }

    /** Der Finger bewegt sich: der Schläger folgt, bleibt aber in der eigenen Hälfte. */
    fun fuehre(x: Float, y: Float) {
        zielX = min(max(x, LINKS + TREFFER_RADIUS), MITTE - TREFFER_RADIUS - 4f)
        zielY = min(max(y, UNTEN + TREFFER_RADIUS), OBEN - TREFFER_RADIUS)
    }

    private fun stelleAuf() {
        meinX = LINKS + 150f
        meinY = MITTE_Y
        kiX = RECHTS - 150f
        kiY = MITTE_Y
        zielX = meinX
        zielY = meinY
        meinVx = 0f
        meinVy = 0f
        kiVx = 0f
        kiVy = 0f
        puckX = MITTE
        puckY = MITTE_Y
        puckVx = 0f
        puckVy = 0f
        puckDreh = 0f
    }

    private fun neuStarten() {
        level = 1
        toreIch = 0
        toreKi = 0
        punkte = 0
        siegMeldung = false
        stelleAuf()
        anstossRichtung = 1f
        anstossRest = 0.8f
        pauseRest = 0f
        danach = Danach.NICHTS
        phase = Phase.LAEUFT
    }

    fun schritt(dtRoh: Float) {
        val dt = min(dtRoh, 1f / 30f)
        if (dt <= 0f) return
        zeit += dt
        if (phase == Phase.PAUSE) {
            pauseRest -= dt
            if (pauseRest <= 0f) weiterNachPause()
            return
        }
        if (phase != Phase.LAEUFT) return

        bewegeMeinen(dt)
        bewegeKi(dt)

        if (anstossRest > 0f) {
            anstossRest -= dt
            if (anstossRest <= 0f) anstoss()
            return
        }

        val teile = 3
        for (i in 0 until teile) {
            teilschritt(dt / teile)
            if (phase != Phase.LAEUFT) return
        }
        puckDreh += (puckVx - puckVy) * dt * 0.01f
    }

    private fun weiterNachPause() {
        when (danach) {
            Danach.ANSTOSS_ICH, Danach.ANSTOSS_KI -> {
                stelleAuf()
                anstossRichtung = if (danach == Danach.ANSTOSS_ICH) 1f else -1f
                anstossRest = 0.9f
            }
            Danach.NAECHSTES_LEVEL -> {
                level += 1
                toreIch = 0
                toreKi = 0
                stelleAuf()
                siegMeldung = false
                anstossRichtung = 1f
                anstossRest = 0.9f
            }
            Danach.NICHTS -> Unit
        }
        danach = Danach.NICHTS
        phase = Phase.LAEUFT
    }

    private fun bewegeMeinen(dt: Float) {
        val altX = meinX
        val altY = meinY
        val dx = zielX - altX
        val dy = zielY - altY
        val d = hypot(dx, dy)
        val erlaubt = 1700f * dt
        var neuX = altX
        var neuY = altY
        if (d > 0.5f) {
            val s = min(d, erlaubt)
            neuX = altX + dx / d * s
            neuY = altY + dy / d * s
        }
        meinX = neuX
        meinY = neuY
        meinVx = (neuX - altX) / dt
        meinVy = (neuY - altY) / dt
    }

    private fun bewegeKi(dt: Float) {
        kiNeuRest -= dt
        if (kiNeuRest <= 0f) {
            kiNeuRest = HockeyBalance.kiReaktion(level)
            val f = HockeyBalance.kiFehler(level)
            kiVersatzX = zufall.nextFloat() * 2f * f - f
            kiVersatzY = zufall.nextFloat() * 2f * f - f
        }
        val pr = HockeyBalance.PUCK_RADIUS
        val sr = HockeyBalance.SCHLAEGER_RADIUS
        var wunschX: Float
        var wunschY: Float

        if (anstossRest > 0f) {
            wunschX = RECHTS - 150f
            wunschY = MITTE_Y
        } else if (puckX > kiX - 5f && puckX > MITTE) {
            // Der Puck liegt hinter dem Schläger. Erst außen herum dahinter stellen.
            val seite = if (kiY >= puckY) 1f else -1f
            wunschX = puckX + 70f
            wunschY = puckY + seite * 80f
        } else if (puckX > MITTE - 20f) {
            // Angreifen: hinter den Puck stellen und zum Tor des Spielers schieben.
            val dx = puckX - LINKS
            val dy = puckY - MITTE_Y
            val d = max(hypot(dx, dy), 1f)
            wunschX = puckX + dx / d * (pr + sr - 6f) + kiVersatzX
            wunschY = puckY + dy / d * (pr + sr - 6f) + kiVersatzY
        } else {
            // Verteidigen: vor dem eigenen Tor auf Höhe des Pucks bleiben.
            wunschX = RECHTS - 110f
            wunschY = min(max(puckY, TOR_UNTEN), TOR_OBEN) + kiVersatzY
        }

        wunschX = min(max(wunschX, MITTE + sr + 4f), RECHTS - sr)
        wunschY = min(max(wunschY, UNTEN + sr), OBEN - sr)

        val dx = wunschX - kiX
        val dy = wunschY - kiY
        val d = hypot(dx, dy)
        var neuX = kiX
        var neuY = kiY
        if (d > 0.5f) {
            val s = min(d, HockeyBalance.kiTempo(level) * dt)
            neuX = kiX + dx / d * s
            neuY = kiY + dy / d * s
        }
        kiVx = (neuX - kiX) / dt
        kiVy = (neuY - kiY) / dt
        kiX = neuX
        kiY = neuY
    }

    private fun teilschritt(d: Float) {
        var px = puckX + puckVx * d
        var py = puckY + puckVy * d
        val reibung = max(1f - 0.25f * d, 0f)
        puckVx *= reibung
        puckVy *= reibung
        val r = HockeyBalance.PUCK_RADIUS

        if (py - r < UNTEN) {
            py = UNTEN + r
            puckVy = abs(puckVy) * 0.92f
        } else if (py + r > OBEN) {
            py = OBEN - r
            puckVy = -abs(puckVy) * 0.92f
        }

        val imTor = py > TOR_UNTEN + r * 0.3f && py < TOR_OBEN - r * 0.3f
        if (px - r < LINKS) {
            if (imTor) {
                if (px + r < LINKS) {
                    puckX = px
                    puckY = py
                    torErzielt(ich = false)
                    return
                }
            } else {
                px = LINKS + r
                puckVx = abs(puckVx) * 0.92f
            }
        } else if (px + r > RECHTS) {
            if (imTor) {
                if (px - r > RECHTS) {
                    puckX = px
                    puckY = py
                    torErzielt(ich = true)
                    return
                }
            } else {
                px = RECHTS - r
                puckVx = -abs(puckVx) * 0.92f
            }
        }
        puckX = px
        puckY = py

        stoss(meinX, meinY, meinVx, meinVy, meiner = true)
        stoss(kiX, kiY, kiVx, kiVy, meiner = false)
        begrenze()
    }

    private fun stoss(sx: Float, sy: Float, vx: Float, vy: Float, meiner: Boolean) {
        val pr = HockeyBalance.PUCK_RADIUS
        val sr = TREFFER_RADIUS
        val dx = puckX - sx
        val dy = puckY - sy
        val d = hypot(dx, dy)
        if (d >= pr + sr) return
        val nx = if (d > 0.001f) dx / d else if (meiner) 1f else -1f
        val ny = if (d > 0.001f) dy / d else 0f
        puckX = sx + nx * (pr + sr)
        puckY = sy + ny * (pr + sr)
        val rel = (puckVx - vx) * nx + (puckVy - vy) * ny
        if (rel < -200f && meiner) ereignisse.add(HockeyEreignis(HockeyEreignis.Art.SCHLAG, puckX, puckY))
        if (rel < 0f) {
            puckVx -= 1.9f * rel * nx
            puckVy -= 1.9f * rel * ny
        }
        val tempo = hypot(puckVx, puckVy)
        if (tempo < HockeyBalance.PUCK_MIN_STOSS) {
            puckVx = nx * HockeyBalance.PUCK_MIN_STOSS
            puckVy = ny * HockeyBalance.PUCK_MIN_STOSS
        }
    }

    private fun begrenze() {
        val tempo = hypot(puckVx, puckVy)
        if (tempo > HockeyBalance.PUCK_MAX_TEMPO) {
            val f = HockeyBalance.PUCK_MAX_TEMPO / tempo
            puckVx *= f
            puckVy *= f
        }
    }

    private fun anstoss() {
        val winkel = zufall.nextFloat() - 0.5f
        puckVx = cos(winkel) * 320f * anstossRichtung
        puckVy = sin(winkel) * 320f
    }

    private fun torErzielt(ich: Boolean) {
        if (ich) toreIch += 1 else toreKi += 1
        ereignisse.add(HockeyEreignis(if (ich) HockeyEreignis.Art.TOR_ICH else HockeyEreignis.Art.TOR_KI, if (ich) RECHTS - 60f else LINKS + 60f, MITTE_Y))
        puckVx = 0f
        puckVy = 0f
        when {
            toreIch >= HockeyBalance.TORE_ZUM_SIEG -> siegImLevel()
            toreKi >= HockeyBalance.TORE_ZUM_SIEG -> {
                phase = Phase.VORBEI
                beiEnde?.invoke(punkte)
            }
            else -> {
                // Wer das Tor bekommen hat, bekommt den Puck.
                phase = Phase.PAUSE
                pauseRest = 0.9f
                danach = if (ich) Danach.ANSTOSS_ICH else Danach.ANSTOSS_KI
            }
        }
    }

    private fun siegImLevel() {
        phase = Phase.PAUSE
        punkte += HockeyBalance.punkteFuerSieg(level, toreIch - toreKi)
        siegMeldung = true
        pauseRest = 2.0f
        danach = Danach.NAECHSTES_LEVEL
        ereignisse.add(HockeyEreignis(HockeyEreignis.Art.SIEG, MITTE, HockeyBalance.HOEHE * 0.4f))
    }

    /** Für Tests: setzt den Puck. */
    internal fun setzePuck(x: Float, y: Float, vx: Float, vy: Float) {
        puckX = x
        puckY = y
        puckVx = vx
        puckVy = vy
    }
}
