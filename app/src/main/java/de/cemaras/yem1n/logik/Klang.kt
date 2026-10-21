package de.cemaras.yem1n.logik

import kotlin.math.PI
import kotlin.math.sin

/** Kurze Klänge für die Spiele. Sie werden selbst erzeugt, es braucht keine Tondateien. */
enum class Klang(val noten: List<Pair<Int, Int>>, val lautstaerke: Float = 0.5f) {
    PLING(listOf(880 to 70, 1320 to 110)),
    KLEIN(listOf(990 to 60)),
    AUTSCH(listOf(220 to 110, 150 to 170), 0.55f),
    SPRUNG(listOf(420 to 50, 620 to 70), 0.35f),
    TOR(listOf(523 to 90, 659 to 90, 784 to 90, 1047 to 220)),
    GEHALTEN(listOf(300 to 120, 220 to 200), 0.5f),
    SCHLAG(listOf(180 to 40), 0.6f),
    LEVEL(listOf(659 to 90, 784 to 90, 988 to 150)),
    SIEG(listOf(523 to 100, 659 to 100, 784 to 100, 1047 to 140, 784 to 90, 1047 to 300)),
    NIEDERLAGE(listOf(392 to 160, 330 to 160, 262 to 320));

    /** Sendedauer in Millisekunden. */
    val dauerMs: Int get() = noten.sumOf { it.second }

    companion object {
        const val RATE = 22050

        /** Die Wellenform als 16-Bit-Zahlen. Jede Note klingt weich ein und aus, damit nichts knackt. */
        fun pcm(k: Klang): ShortArray {
            val gesamt = k.noten.sumOf { it.second * RATE / 1000 }
            val aus = ShortArray(gesamt)
            var pos = 0
            for ((hz, ms) in k.noten) {
                val n = ms * RATE / 1000
                val rampe = minOf(n / 4, RATE / 200)
                for (i in 0 until n) {
                    val huelle = when {
                        rampe > 0 && i < rampe -> i.toFloat() / rampe
                        rampe > 0 && i > n - rampe -> (n - i).toFloat() / rampe
                        else -> 1f
                    }
                    val abkling = 1f - 0.4f * i / n
                    val w = sin(2.0 * PI * hz * i / RATE).toFloat()
                    aus[pos + i] = (w * huelle * abkling * k.lautstaerke * Short.MAX_VALUE).toInt().toShort()
                }
                pos += n
            }
            return aus
        }
    }
}
