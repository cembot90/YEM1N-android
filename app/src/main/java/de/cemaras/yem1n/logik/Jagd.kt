package de.cemaras.yem1n.logik

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Eine Plattform: [x] ist die Mitte, [y] die Oberkante, dort steht der Hase. */
data class JagdPlattform(val x: Float, val y: Float, val breite: Float) {
    val links: Float get() = x - breite / 2
    val rechts: Float get() = x + breite / 2
}

data class JagdPunkt(val x: Float, val y: Float)

class JagdEbene(val plattformen: List<JagdPlattform>, val muenzen: List<JagdPunkt>)

/**
 * Münzjagd: Werte und Ebenen. Die Bühne ist 1024 mal 576 groß, y zählt von unten nach oben.
 * Eigene Datei, damit sich nachrechnen lässt, dass jede Münze erreichbar ist und die Gegner
 * langsamer bleiben als der Hase.
 */
object JagdBalance {
    const val BREITE = 1024f
    const val HOEHE = 576f
    const val BODEN_OBERKANTE = 70f
    const val PLATTFORM_DICKE = 26f

    // Spielgefühl
    const val SCHWERKRAFT = -1900f
    const val SPRUNG_KRAFT = 780f

    /** Beim Halten im Fallen wirkt nur ein Teil der Schwerkraft (Schweben). */
    const val SCHWEB_FAKTOR = 0.22f
    const val SCHWEB_FALL_MAX = -170f
    const val LAUF_TEMPO = 330f
    const val SPIELER_HOEHE = 64f
    const val MAX_LEBEN = 3

    /** Wie hoch ein Sprung kommt. */
    val maxSprungHoehe: Float get() = SPRUNG_KRAFT * SPRUNG_KRAFT / (2 * -SCHWERKRAFT)

    /** Ein Stockwerk darf höchstens so hoch sein, dass der Sprung bequem reicht. */
    val bequemeHoehe: Float get() = maxSprungHoehe * 0.85f

    /** So weit darf es zur nächsten Plattform seitlich sein. */
    const val BEQUEME_WEITE = 200f

    // Gegner
    fun gegnerAnzahl(ebene: Int): Int = min(max(ebene, 1), 5)

    fun gegnerTempo(ebene: Int): Float = min(110f + (max(ebene, 1) - 1) * 18f, 230f)

    /** Ab dieser Ebene verfolgt ein Gegner den Hasen langsam. */
    const val JAEGER_AB_EBENE = 3

    // Punkte
    const val PUNKTE_MUENZE = 1
    const val PUNKTE_LEUCHTEND = 3
    const val PUNKTE_EBENE = 10

    val boden = JagdPlattform(BREITE / 2, BODEN_OBERKANTE, BREITE)

    private val formen: List<List<JagdPlattform>> = listOf(
        // Zwei Türme und eine Mitte
        listOf(
            JagdPlattform(190f, 180f, 250f), JagdPlattform(834f, 180f, 250f), JagdPlattform(512f, 290f, 320f),
            JagdPlattform(170f, 400f, 230f), JagdPlattform(854f, 400f, 230f), JagdPlattform(512f, 500f, 260f),
        ),
        // Treppe
        listOf(
            JagdPlattform(130f, 175f, 240f), JagdPlattform(400f, 255f, 200f), JagdPlattform(670f, 335f, 200f),
            JagdPlattform(900f, 415f, 220f), JagdPlattform(620f, 495f, 240f), JagdPlattform(300f, 375f, 200f),
        ),
        // Burg
        listOf(
            JagdPlattform(110f, 170f, 180f), JagdPlattform(914f, 170f, 180f), JagdPlattform(512f, 190f, 200f),
            JagdPlattform(300f, 300f, 180f), JagdPlattform(724f, 300f, 180f), JagdPlattform(120f, 420f, 200f),
            JagdPlattform(904f, 420f, 200f), JagdPlattform(512f, 430f, 220f),
        ),
    )

    val anzahlFormen: Int get() = formen.size

    /** Ebene 1, 2, 3 ... wechselt durch die Formen. */
    fun ebene(nummer: Int): JagdEbene {
        val platten = formen[(max(nummer, 1) - 1) % formen.size]
        val muenzen = mutableListOf<JagdPunkt>()
        for (p in platten) {
            val n = max(2, (p.breite / 80f).toInt())
            val von = p.links + 30f
            val bis = p.rechts - 30f
            for (i in 0 until n) {
                val t = i.toFloat() / (n - 1)
                muenzen.add(JagdPunkt(von + (bis - von) * t, p.y + 38f))
            }
        }
        for (x in listOf(300f, 420f, 600f, 720f)) muenzen.add(JagdPunkt(x, BODEN_OBERKANTE + 38f))
        return JagdEbene(platten, muenzen)
    }
}

/** Reine Rechnung für den Analogstick der Münzjagd, damit sie sich testen lässt. */
object JagdStick {
    /** So weit lässt sich der Knopf aus der Mitte ziehen. */
    const val RADIUS = 70f

    /** Kleine Bewegungen um die Mitte zählen nicht, sonst läuft der Hase von allein. */
    const val TOTZONE = 0.15f

    /** Ab so weit nach oben springt der Hase. */
    const val SPRUNG_SCHWELLE = 0.6f

    /** Wandelt den Abstand vom Mittelpunkt in Werte von -1 bis 1 um (x, y). */
    fun wert(dx: Float, dy: Float): Pair<Float, Float> {
        val laenge = hypot(dx, dy)
        if (laenge <= 0f) return 0f to 0f
        val faktor = min(laenge, RADIUS) / laenge
        var x = dx * faktor / RADIUS
        val y = dy * faktor / RADIUS
        x = if (abs(x) < TOTZONE) 0f else (abs(x) - TOTZONE) / (1 - TOTZONE) * (if (x < 0) -1f else 1f)
        return x to y
    }

    /** Wo der Knopf gezeichnet wird: nie weiter als der Radius von der Mitte. */
    fun knopf(dx: Float, dy: Float): Pair<Float, Float> {
        val laenge = hypot(dx, dy)
        return if (laenge > RADIUS) (dx / laenge * RADIUS) to (dy / laenge * RADIUS) else dx to dy
    }

    fun springt(wertY: Float): Boolean = wertY >= SPRUNG_SCHWELLE
}

class JagdMuenze(val x: Float, val y: Float)

class JagdGegner(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val tempo: Float, val jaeger: Boolean, val variante: Int, var aktivRest: Float,
)

data class JagdEreignis(val art: Art, val x: Float, val y: Float, val text: String = "") {
    enum class Art { FUNKEL, TREFFER, EBENE }
}

/** Der Ablauf der Münzjagd ohne Grafik. */
class JagdSpiel(private val zufall: Random = Random.Default) {
    enum class Phase { WARTEN, LAEUFT, EBENE_FERTIG, VORBEI }

    private companion object {
        const val START_X = JagdBalance.BREITE / 2
        const val START_Y = JagdBalance.BODEN_OBERKANTE + JagdBalance.SPIELER_HOEHE / 2
    }

    var phase = Phase.WARTEN
        private set
    var x = START_X
        private set
    var y = START_Y
        private set
    var vy = 0f
        private set
    var amBoden = true
        private set
    var blickLinks = false
        private set
    var leben = JagdBalance.MAX_LEBEN
        private set
    var punkte = 0
        private set
    var ebene = 1
        private set
    var unverwundbar = 0f
        private set
    var zeit = 0f
        private set

    /** Wird nur gesetzt, wenn der Hase in der Luft ist und sich waagrecht bewegt. */
    var laeuft = false
        private set

    var plattformen: List<JagdPlattform> = emptyList()
        private set
    val muenzen = mutableListOf<JagdMuenze>()
    var markiert: JagdMuenze? = null
        internal set
    val gegner = mutableListOf<JagdGegner>()
    val ereignisse = mutableListOf<JagdEreignis>()
    var beiEnde: ((Int) -> Unit)? = null

    private var stickX = 0f
    private var haelt = false
    private var sprungNeu = false
    private var wartetRest = 0f

    init {
        baueEbene()
    }

    /** Eine neue Berührung: Spiel starten oder nach dem Ende neu beginnen. */
    fun tippe() {
        when (phase) {
            Phase.VORBEI -> neuStarten()
            Phase.WARTEN -> phase = Phase.LAEUFT
            else -> Unit
        }
    }

    /**
     * Was die Finger gerade tun: [stickX] von -1 bis 1, [taste] für den Sprungknopf,
     * [stickHoch], wenn der Stick weit genug nach oben gezogen ist.
     */
    fun eingabe(stickX: Float, taste: Boolean, stickHoch: Boolean) {
        this.stickX = stickX
        val j = taste || stickHoch
        if (j && !haelt) sprungNeu = true
        if (!j && haelt && vy > 0f) vy *= 0.6f
        haelt = j
    }

    private fun neuStarten() {
        leben = JagdBalance.MAX_LEBEN
        punkte = 0
        ebene = 1
        unverwundbar = 1f
        stickX = 0f
        haelt = false
        sprungNeu = false
        wartetRest = 0f
        baueEbene()
        stelleAufStart()
        phase = Phase.LAEUFT
    }

    private fun stelleAufStart() {
        x = START_X
        y = START_Y
        vy = 0f
        amBoden = true
    }

    private fun baueEbene() {
        gegner.clear()
        muenzen.clear()
        markiert = null
        val aufbau = JagdBalance.ebene(ebene)
        plattformen = aufbau.plattformen
        aufbau.muenzen.forEach { muenzen.add(JagdMuenze(it.x, it.y)) }
        markiert = muenzen.randomOrNull(zufall)
        erzeugeGegner()
    }

    private fun erzeugeGegner() {
        val anzahl = JagdBalance.gegnerAnzahl(ebene)
        val tempo = JagdBalance.gegnerTempo(ebene)
        for (i in 0 until anzahl) {
            val vonLinks = i % 2 == 0
            val richtung = if (vonLinks) 1f else -1f
            gegner.add(
                JagdGegner(
                    x = if (vonLinks) 60f else JagdBalance.BREITE - 60f,
                    y = JagdBalance.HOEHE - 100f - i * 24f,
                    vx = richtung * tempo * 0.82f, vy = -tempo * 0.57f,
                    tempo = tempo,
                    jaeger = ebene >= JagdBalance.JAEGER_AB_EBENE && i == 0,
                    variante = i % 3,
                    aktivRest = 1.4f,
                ),
            )
        }
    }

    fun schritt(dtRoh: Float) {
        val dt = min(dtRoh, 1f / 30f)
        if (dt <= 0f) return
        zeit += dt
        if (phase == Phase.EBENE_FERTIG) {
            wartetRest -= dt
            if (wartetRest <= 0f) {
                ebene += 1
                baueEbene()
                stelleAufStart()
                unverwundbar = 1f
                phase = Phase.LAEUFT
            }
            return
        }
        if (phase != Phase.LAEUFT) return
        bewegeSpieler(dt)
        bewegeGegner(dt)
        pruefe(dt)
    }

    private fun alle(): List<JagdPlattform> = listOf(JagdBalance.boden) + plattformen

    private fun landeplatz(px: Float, alt: Float, neu: Float): JagdPlattform? =
        alle().filter { px >= it.links - 14f && px <= it.rechts + 14f && alt >= it.y - 2f && neu <= it.y }
            .maxByOrNull { it.y }

    private fun stehtAuf(px: Float, py: Float): Boolean {
        val fuss = py - JagdBalance.SPIELER_HOEHE / 2
        return alle().any { px >= it.links - 14f && px <= it.rechts + 14f && abs(fuss - it.y) < 3f }
    }

    private fun bewegeSpieler(dt: Float) {
        val richtung = stickX
        val alteUnterkante = y - JagdBalance.SPIELER_HOEHE / 2
        x = min(max(x + richtung * JagdBalance.LAUF_TEMPO * dt, 28f), JagdBalance.BREITE - 28f)
        laeuft = richtung != 0f
        if (richtung != 0f) blickLinks = richtung < 0f

        if (sprungNeu) {
            sprungNeu = false
            if (amBoden) {
                vy = JagdBalance.SPRUNG_KRAFT
                amBoden = false
            }
        }

        if (!amBoden) {
            var g = JagdBalance.SCHWERKRAFT
            if (haelt && vy < 0f) g *= JagdBalance.SCHWEB_FAKTOR
            vy += g * dt
            if (haelt && vy < JagdBalance.SCHWEB_FALL_MAX) vy = JagdBalance.SCHWEB_FALL_MAX
            y += vy * dt
            val neueUnterkante = y - JagdBalance.SPIELER_HOEHE / 2
            if (vy <= 0f) {
                val p = landeplatz(x, alteUnterkante, neueUnterkante)
                if (p != null) {
                    y = p.y + JagdBalance.SPIELER_HOEHE / 2
                    vy = 0f
                    amBoden = true
                }
            }
        } else if (!stehtAuf(x, y)) {
            amBoden = false
            vy = 0f
        }
    }

    private fun bewegeGegner(dt: Float) {
        val unten = JagdBalance.BODEN_OBERKANTE + 28f
        val oben = JagdBalance.HOEHE - 60f
        for (g in gegner) {
            if (g.aktivRest > 0f) g.aktivRest -= dt
            if (g.jaeger) {
                val dx = x - g.x
                val dy = y - g.y
                val d = max(hypot(dx, dy), 1f)
                val wunschX = dx / d * g.tempo * 0.8f
                val wunschY = dy / d * g.tempo * 0.8f
                val f = min(1f, 1.2f * dt)
                g.vx += (wunschX - g.vx) * f
                g.vy += (wunschY - g.vy) * f
            }
            g.x += g.vx * dt
            g.y += g.vy * dt
            if (g.x < 26f) {
                g.x = 26f
                g.vx = abs(g.vx)
            } else if (g.x > JagdBalance.BREITE - 26f) {
                g.x = JagdBalance.BREITE - 26f
                g.vx = -abs(g.vx)
            }
            if (g.y < unten) {
                g.y = unten
                g.vy = abs(g.vy)
            } else if (g.y > oben) {
                g.y = oben
                g.vy = -abs(g.vy)
            }
        }
    }

    private fun pruefe(dt: Float) {
        if (unverwundbar > 0f) {
            unverwundbar = max(0f, unverwundbar - dt)
        } else {
            for (g in gegner) {
                if (g.aktivRest <= 0f && abs(g.x - x) < 36f && abs(g.y - y) < 40f) {
                    treffer(g)
                    break
                }
            }
        }
        if (phase == Phase.VORBEI) return

        for (m in muenzen.toList()) {
            if (abs(m.x - x) < 36f && abs(m.y - y) < 42f) sammle(m)
        }
        if (muenzen.isEmpty()) ebeneGeschafft()
    }

    private fun sammle(m: JagdMuenze) {
        val leuchtend = m === markiert
        punkte += if (leuchtend) JagdBalance.PUNKTE_LEUCHTEND else JagdBalance.PUNKTE_MUENZE
        ereignisse.add(
            JagdEreignis(JagdEreignis.Art.FUNKEL, m.x, m.y, if (leuchtend) "+${JagdBalance.PUNKTE_LEUCHTEND}" else "✦"),
        )
        muenzen.remove(m)
        if (leuchtend) {
            // Die nächste leuchtende Münze liegt in der Nähe der letzten.
            markiert = muenzen.minByOrNull { hypot(it.x - m.x, it.y - m.y) }
        }
    }

    private fun treffer(g: JagdGegner) {
        leben -= 1
        unverwundbar = 1.6f
        vy = 380f
        amBoden = false
        ereignisse.add(JagdEreignis(JagdEreignis.Art.TREFFER, g.x, g.y))
        if (leben <= 0) {
            phase = Phase.VORBEI
            beiEnde?.invoke(punkte)
        }
    }

    private fun ebeneGeschafft() {
        punkte += JagdBalance.PUNKTE_EBENE
        phase = Phase.EBENE_FERTIG
        wartetRest = 1.8f
        ereignisse.add(JagdEreignis(JagdEreignis.Art.EBENE, JagdBalance.BREITE / 2, JagdBalance.HOEHE * 0.45f))
    }
}
