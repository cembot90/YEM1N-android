package de.cemaras.yem1n.logik

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Schwierigkeit im Zahlenlauf. Eigene Datei, damit sich die Kurve nachrechnen und testen lässt.
 * Die Bühne ist 1024 mal 576 groß, y zählt von unten nach oben.
 */
object LaufBalance {
    const val BREITE = 1024f
    const val HOEHE = 576f

    /** Ab so vielen Punkten beginnt die nächste Stufe. */
    const val PUNKTE_PRO_STUFE = 120
    const val HOECHSTE_STUFE = 8

    /** Ab dieser Stufe kommen fliegende Gegner dazu. */
    const val FLIEGER_AB_STUFE = 2

    /** Ab dieser Stufe gibt es Trampoline. */
    const val FEDER_AB_STUFE = 3

    /** Weniger Zeit als das soll zwischen zwei Hindernissen nie herauskommen. */
    const val KUERZESTE_REAKTION = 0.38f

    fun stufe(punkte: Int): Int = min(max(punkte / PUNKTE_PRO_STUFE + 1, 1), HOECHSTE_STUFE)

    private fun s(stufe: Int): Float = (min(max(stufe, 1), HOECHSTE_STUFE) - 1).toFloat()

    /** Wie schnell die Welt vorbeizieht, in Punkten je Sekunde. */
    fun tempo(stufe: Int): Float = 320f + s(stufe) * 52f

    /** Kleinster und größter Abstand zwischen zwei Hindernissen. Wird enger, aber nie unfair. */
    fun abstand(stufe: Int): Pair<Float, Float> =
        max(360f - s(stufe) * 16f, 280f) to max(620f - s(stufe) * 30f, 420f)

    /** Wie wahrscheinlich ein Hindernis ein Flieger ist. */
    fun fliegerAnteil(stufe: Int): Double =
        if (stufe < FLIEGER_AB_STUFE) 0.0 else min(0.18 + (stufe - FLIEGER_AB_STUFE) * 0.05, 0.42)

    /** Wie viel Zeit im ungünstigsten Fall zwischen zwei Hindernissen bleibt. */
    fun zeitZwischenHindernissen(stufe: Int): Float = abstand(stufe).first / tempo(stufe)
}

/** Etwas, das im Lauf passiert ist und einen Effekt bekommt. */
data class LaufEreignis(val art: Art, val x: Float, val y: Float) {
    enum class Art { MUENZE, FLUEGEL, TREFFER, STUFE }
}

class LaufObjekt(val art: Art, var x: Float, var y: Float, val variante: Int = 0, val grundY: Float = y) {
    enum class Art { HINDERNIS, FLIEGER, MUENZE, FEDER, FLUEGEL }
}

/** Der Ablauf des Zahlenlaufs ohne Grafik. */
class LaufSpiel(private val zufall: Random = Random.Default) {
    enum class Phase { WARTEN, LAEUFT, VORBEI }

    companion object {
        const val SCHWERKRAFT = -2500f
        /** Beim Halten fällt der Hase langsamer. */
        const val HALT_FAKTOR = 0.42f
        const val SPRUNG_KRAFT = 830f
        const val DOPPEL_KRAFT = 720f
        const val FEDER_KRAFT = 1450f
        const val MAX_LEBEN = 3

        const val SPIELER_HOEHE = 70f
        const val HINDERNIS_HOEHE = 58f
        const val FEDER_HOEHE = 46f
        const val BODEN_Y = LaufBalance.HOEHE * 0.24f
        const val RUHE_Y = BODEN_Y + SPIELER_HOEHE / 2
        const val SPIELER_X = LaufBalance.BREITE * 0.22f
        const val UNVERWUNDBAR = 1.6f
        const val AUA_DAUER = 0.3f
        const val FLUEGEL_DAUER = 8f
    }

    var phase = Phase.WARTEN
        private set
    var y = RUHE_Y
        private set
    var vy = 0f
        private set
    var amBoden = true
        private set
    var leben = MAX_LEBEN
        private set
    var punkte = 0
        private set
    var stufe = 1
        private set
    var unverwundbarRest = 0f
        private set
    var fluegelRest = 0f
        private set
    var auaRest = 0f
        private set

    /** Wie weit die Welt gelaufen ist, für den Boden. */
    var weg = 0f
        private set

    /** Laufzeit in Sekunden, für Animationen. */
    var zeit = 0f
        private set

    val objekte = mutableListOf<LaufObjekt>()
    val ereignisse = mutableListOf<LaufEreignis>()
    var beiEnde: ((Int) -> Unit)? = null

    private var spruenge = 0
    private var haltend = false
    private var bisHindernis = 500f
    private var bisMuenze = 320f
    private var bisFeder = 1600f

    private val maxSpruenge: Int get() = if (fluegelRest > 0f) 3 else 2

    /** Finger drauf: starten, springen oder neu beginnen. */
    fun druecke() {
        haltend = true
        when (phase) {
            Phase.VORBEI -> neuStarten()
            Phase.WARTEN -> phase = Phase.LAEUFT
            Phase.LAEUFT -> springen()
        }
    }

    /** Kurz tippen heißt kleiner Sprung, Halten heißt hoher Sprung. */
    fun loslassen() {
        haltend = false
        if (vy > 0f) vy *= 0.55f
    }

    private fun springen() {
        if (spruenge >= maxSpruenge) return
        vy = if (spruenge == 0) SPRUNG_KRAFT else DOPPEL_KRAFT
        spruenge += 1
        amBoden = false
    }

    private fun neuStarten() {
        objekte.clear()
        punkte = 0
        stufe = 1
        leben = MAX_LEBEN
        vy = 0f
        spruenge = 0
        amBoden = true
        phase = Phase.LAEUFT
        unverwundbarRest = 0f
        fluegelRest = 0f
        auaRest = 0f
        bisHindernis = 500f
        bisMuenze = 320f
        bisFeder = 1600f
        y = RUHE_Y
    }

    fun schritt(dtRoh: Float) {
        val dt = min(dtRoh, 1f / 30f)
        if (dt <= 0f) return
        zeit += dt
        if (phase != Phase.LAEUFT) return

        if (unverwundbarRest > 0f) unverwundbarRest = max(0f, unverwundbarRest - dt)
        if (fluegelRest > 0f) fluegelRest = max(0f, fluegelRest - dt)
        if (auaRest > 0f) auaRest = max(0f, auaRest - dt)
        bewegeSpieler(dt)

        val strecke = LaufBalance.tempo(stufe) * dt
        weg += strecke
        nachschub(strecke)
        bewegeUndPruefe(strecke)
        if (phase != Phase.LAEUFT) return
        val neu = LaufBalance.stufe(punkte)
        if (neu != stufe) {
            stufe = neu
            ereignisse.add(LaufEreignis(LaufEreignis.Art.STUFE, SPIELER_X, y))
        }
    }

    private fun bewegeSpieler(dt: Float) {
        // Beim Halten wirkt die Schwerkraft schwächer, solange es aufwärts geht
        val g = if (haltend && vy > 0f) SCHWERKRAFT * HALT_FAKTOR else SCHWERKRAFT
        vy += g * dt
        y += vy * dt
        val decke = LaufBalance.HOEHE - 40f
        if (y > decke) {
            y = decke
            vy = min(vy, 0f)
        }
        if (y <= RUHE_Y) {
            y = RUHE_Y
            vy = 0f
            if (!amBoden) {
                amBoden = true
                spruenge = 0
            }
        }
    }

    private fun nachschub(strecke: Float) {
        bisHindernis -= strecke
        if (bisHindernis <= 0f) {
            if (zufall.nextDouble() < LaufBalance.fliegerAnteil(stufe)) setzeFlieger() else setzeHindernis()
            val (klein, gross) = LaufBalance.abstand(stufe)
            bisHindernis = klein + (gross - klein) * zufall.nextFloat()
        }
        bisMuenze -= strecke
        if (bisMuenze <= 0f) {
            setzeMuenzBogen()
            bisMuenze = 620f + 380f * zufall.nextFloat()
        }
        bisFeder -= strecke
        if (bisFeder <= 0f) {
            if (stufe >= LaufBalance.FEDER_AB_STUFE) {
                objekte.add(LaufObjekt(LaufObjekt.Art.FEDER, LaufBalance.BREITE + 70f, BODEN_Y + FEDER_HOEHE / 2))
            }
            bisFeder = 1500f + 1100f * zufall.nextFloat()
        }
    }

    private fun setzeHindernis() {
        objekte.add(
            LaufObjekt(LaufObjekt.Art.HINDERNIS, LaufBalance.BREITE + 80f, BODEN_Y + HINDERNIS_HOEHE / 2, zufall.nextInt(3)),
        )
    }

    private fun setzeFlieger() {
        val hoehe = BODEN_Y + 120f + 70f * zufall.nextFloat()
        objekte.add(LaufObjekt(LaufObjekt.Art.FLIEGER, LaufBalance.BREITE + 80f, hoehe, zufall.nextInt(2)))
    }

    private fun setzeMuenzBogen() {
        val anzahl = 5
        for (i in 0 until anzahl) {
            val bogen = sin(i.toFloat() / (anzahl - 1) * PI.toFloat())
            objekte.add(
                LaufObjekt(LaufObjekt.Art.MUENZE, LaufBalance.BREITE + 60f + i * 52f, BODEN_Y + 56f + bogen * 112f),
            )
        }
        // Gelegentlich hängen Flügel am Ende des Bogens
        if (stufe >= LaufBalance.FEDER_AB_STUFE && zufall.nextDouble() < 0.22) {
            objekte.add(LaufObjekt(LaufObjekt.Art.FLUEGEL, LaufBalance.BREITE + 60f + anzahl * 52f, BODEN_Y + 140f))
        }
    }

    private fun trifft(o: LaufObjekt, breite: Float, hoehe: Float): Boolean =
        abs(o.x - SPIELER_X) < breite && abs(o.y - y) < hoehe

    private fun bewegeUndPruefe(strecke: Float) {
        val iterator = objekte.iterator()
        while (iterator.hasNext()) {
            val o = iterator.next()
            o.x -= strecke
            when (o.art) {
                LaufObjekt.Art.HINDERNIS, LaufObjekt.Art.FLIEGER -> {
                    if (o.art == LaufObjekt.Art.FLIEGER) {
                        // Flieger wippen 18 hoch und wieder runter, alle 1,2 Sekunden
                        val t = (zeit / 0.6f) % 2f
                        o.y = o.grundY + 18f * (if (t < 1f) t else 2f - t)
                    }
                    if (unverwundbarRest <= 0f && trifft(o, 38f, 46f)) {
                        iterator.remove()
                        treffer(o)
                        if (phase == Phase.VORBEI) return
                        continue
                    }
                    if (o.x < -90f) {
                        iterator.remove()
                        punkte += 2
                        continue
                    }
                }
                LaufObjekt.Art.MUENZE -> if (trifft(o, 36f, 40f)) {
                    punkte += 10
                    ereignisse.add(LaufEreignis(LaufEreignis.Art.MUENZE, o.x, o.y))
                    iterator.remove()
                    continue
                }
                LaufObjekt.Art.FLUEGEL -> if (trifft(o, 40f, 44f)) {
                    fluegelRest = FLUEGEL_DAUER
                    ereignisse.add(LaufEreignis(LaufEreignis.Art.FLUEGEL, o.x, o.y))
                    iterator.remove()
                    continue
                }
                LaufObjekt.Art.FEDER -> if (trifft(o, 40f, 44f) && vy <= 0f) {
                    vy = FEDER_KRAFT
                    spruenge = 1
                    amBoden = false
                }
            }
            if (o.x < -120f) iterator.remove()
        }
    }

    private fun treffer(o: LaufObjekt) {
        leben -= 1
        unverwundbarRest = UNVERWUNDBAR
        auaRest = AUA_DAUER
        ereignisse.add(LaufEreignis(LaufEreignis.Art.TREFFER, o.x, o.y))
        if (leben <= 0) {
            phase = Phase.VORBEI
            fluegelRest = 0f
            unverwundbarRest = 0f
            beiEnde?.invoke(punkte)
        }
    }
}
