package de.cemaras.yem1n.logik

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Einhorn-Flug: Regeln und Zahlen. Für Vorschulkinder gemacht: großzügig, langsam, nie gemein.
 * Die Bühne ist 1024 mal 576 groß, y zählt von unten nach oben (wie bei iOS).
 */
object EinhornRegeln {
    const val BREITE = 1024f
    const val HOEHE = 576f

    const val SCHWERKRAFT = 820f
    const val FLUG_STOSS = 360f
    const val MAX_LEBEN = 3

    /** Ab so vielen Punkten steigt das Level. */
    const val PUNKTE_PRO_LEVEL = 15

    /** So nah muss das Einhorn an einem Ding sein, um es zu fangen. Bewusst großzügig. */
    const val FANG_ABSTAND = 62f

    /** Das Einhorn bleibt auf dieser Höhe der Bühne von links. */
    const val EINHORN_X = 220f

    enum class Ding(val bild: String, val punkte: Int) {
        STERN("⭐", 1), REGENBOGEN("🌈", 3), HERZ("💖", 0), GEWITTER("⛈️", 0)
    }

    fun level(punkte: Int): Int = max(0, punkte) / PUNKTE_PRO_LEVEL + 1

    /** Wie schnell die Dinge von rechts kommen (Punkte pro Sekunde). */
    fun tempo(level: Int): Float = min(460f, 210f + 25f * (level - 1))

    /** Abstand zwischen zwei neuen Dingen in Sekunden. */
    fun abstand(level: Int): Double = max(0.7, 1.35 - 0.06 * (level - 1))

    /** Anteil der Gewitterwolken. Am Anfang selten, später etwas öfter. */
    fun gewitterAnteil(level: Int): Double = min(0.32, 0.12 + 0.03 * (level - 1))

    /** Wählt das nächste Ding. [zufall] liegt zwischen 0 und 1. Herzen gibt es nur, wenn ein Leben fehlt. */
    fun naechstes(zufall: Double, level: Int, leben: Int): Ding {
        val gewitter = gewitterAnteil(level)
        if (zufall < gewitter) return Ding.GEWITTER
        if (leben < MAX_LEBEN && zufall < gewitter + 0.08) return Ding.HERZ
        if (zufall > 0.8) return Ding.REGENBOGEN
        return Ding.STERN
    }

    /** Ein gefangenes Ding verändert Punkte und Leben. */
    fun fange(ding: Ding, punkte: Int, leben: Int): Pair<Int, Int> = when (ding) {
        Ding.HERZ -> punkte to min(MAX_LEBEN, leben + 1)
        Ding.GEWITTER -> punkte to max(0, leben - 1)
        else -> (punkte + ding.punkte) to leben
    }
}

/** Etwas, das im Spiel passiert ist und einen Effekt auf dem Bildschirm bekommt. */
data class EinhornEreignis(val ding: EinhornRegeln.Ding?, val x: Float, val y: Float, val levelHoch: Boolean = false)

/** Ein Ding, das von rechts nach links fliegt. */
class EinhornObjekt(val ding: EinhornRegeln.Ding, var x: Float, val y: Float)

/** Der Ablauf des Spiels ohne Grafik. Die Oberfläche ruft [schritt] und [tippe] auf und zeichnet den Zustand. */
class EinhornSpiel(private val zufall: Random = Random.Default) {
    enum class Phase { WARTEN, LAEUFT, VORBEI }

    var phase = Phase.WARTEN
        private set
    var y = EinhornRegeln.HOEHE / 2
        private set
    var vy = 0f
        private set
    var punkte = 0
        private set
    var leben = EinhornRegeln.MAX_LEBEN
        private set
    var level = 1
        private set

    /** Restzeit, in der nach einer Gewitterwolke nichts passieren kann (Blinken). */
    var schutz = 0f
        private set
    val objekte = mutableListOf<EinhornObjekt>()

    /** Neue Ereignisse, die die Oberfläche abholt und dann leert. */
    val ereignisse = mutableListOf<EinhornEreignis>()

    /** Wird einmal aufgerufen, wenn das letzte Leben weg ist. */
    var beiEnde: ((Int) -> Unit)? = null

    private var bisNaechstes = 1.0

    fun tippe() {
        when (phase) {
            Phase.VORBEI -> neuStarten()
            Phase.WARTEN -> {
                phase = Phase.LAEUFT
                vy = EinhornRegeln.FLUG_STOSS
            }
            Phase.LAEUFT -> vy = EinhornRegeln.FLUG_STOSS
        }
    }

    private fun neuStarten() {
        objekte.clear()
        punkte = 0
        leben = EinhornRegeln.MAX_LEBEN
        level = 1
        schutz = 0f
        bisNaechstes = 1.0
        y = EinhornRegeln.HOEHE / 2
        phase = Phase.LAEUFT
        vy = EinhornRegeln.FLUG_STOSS
    }

    /** [dtRoh] Sekunden seit dem letzten Bild, [zeit] die Laufzeit für das Schweben vor dem Start. */
    fun schritt(dtRoh: Float, zeit: Float) {
        val dt = min(dtRoh, 1f / 30f)
        if (phase != Phase.LAEUFT) {
            // Vor dem Start und danach schwebt das Einhorn sanft
            y = EinhornRegeln.HOEHE / 2 + sin(zeit * 2) * 14
            return
        }
        vy -= EinhornRegeln.SCHWERKRAFT * dt
        y += vy * dt
        if (y < 60f) {
            y = 60f
            vy = EinhornRegeln.FLUG_STOSS * 0.6f
        }
        if (y > EinhornRegeln.HOEHE - 70f) {
            y = EinhornRegeln.HOEHE - 70f
            vy = min(vy, 0f)
        }
        if (schutz > 0f) schutz = max(0f, schutz - dt)

        bisNaechstes -= dt
        if (bisNaechstes <= 0.0) {
            bisNaechstes = EinhornRegeln.abstand(level) * (0.8 + 0.4 * zufall.nextDouble())
            erzeuge()
        }

        val tempo = EinhornRegeln.tempo(level)
        val iterator = objekte.iterator()
        while (iterator.hasNext()) {
            val o = iterator.next()
            o.x -= tempo * dt
            val abstand = hypot(o.x - EinhornRegeln.EINHORN_X, o.y - y)
            if (abstand < EinhornRegeln.FANG_ABSTAND) {
                if (o.ding == EinhornRegeln.Ding.GEWITTER && schutz > 0f) continue
                iterator.remove()
                fange(o)
                if (phase == Phase.VORBEI) return
                continue
            }
            if (o.x < -80f) iterator.remove()
        }
    }

    private fun erzeuge() {
        val ding = EinhornRegeln.naechstes(zufall.nextDouble(), level, leben)
        val hoehe = 90f + (EinhornRegeln.HOEHE - 190f) * zufall.nextFloat()
        objekte.add(EinhornObjekt(ding, EinhornRegeln.BREITE + 60f, hoehe))
    }

    private fun fange(o: EinhornObjekt) {
        val vorher = level
        val (p, l) = EinhornRegeln.fange(o.ding, punkte, leben)
        punkte = p
        leben = l
        if (o.ding == EinhornRegeln.Ding.GEWITTER) schutz = 1.6f
        level = EinhornRegeln.level(punkte)
        ereignisse.add(EinhornEreignis(o.ding, o.x, o.y, levelHoch = level > vorher))
        if (leben <= 0) {
            phase = Phase.VORBEI
            schutz = 0f
            objekte.clear()
            beiEnde?.invoke(punkte)
        }
    }
}
