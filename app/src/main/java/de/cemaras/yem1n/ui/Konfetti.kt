package de.cemaras.yem1n.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private class Stueck(val x: Float, val tempo: Float, val dreh: Float, val groesse: Float, val farbe: Color, val start: Float)

private val KonfettiFarben = listOf(
    Color(0xFFFFED00), Color(0xFFFF6B6B), Color(0xFF73F29E), Color(0xFF5BC0FF), Color(0xFFFF9EDD), Color.White,
)

/** Bunte Papierschnipsel, die einmal von oben nach unten fallen. Kein Eingriff in die Bedienung. */
@Composable
fun Konfetti(modifier: Modifier = Modifier.fillMaxSize(), dauer: Float = 3.2f) {
    // Im Sparmodus und bei "Animationen entfernen" fällt kein Konfetti
    if (de.cemaras.yem1n.ui.theme.Sparmodus.an) return
    val stuecke = remember {
        val z = Random(System.nanoTime())
        List(70) {
            Stueck(
                x = z.nextFloat(), tempo = 0.45f + z.nextFloat() * 0.5f, dreh = (z.nextFloat() - 0.5f) * 720f,
                groesse = 8f + z.nextFloat() * 10f, farbe = KonfettiFarben[z.nextInt(KonfettiFarben.size)],
                start = z.nextFloat() * 0.8f,
            )
        }
    }
    var zeit by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var erster = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { n ->
                if (erster == 0L) erster = n
                zeit = n - erster
            }
            if (zeit / 1e9f > dauer + 1.5f) break
        }
    }
    val t = zeit / 1e9f
    if (t > dauer + 1.5f) return
    Canvas(modifier) {
        stuecke.forEach { s ->
            val lokal = t - s.start
            if (lokal > 0f) {
                val y = -20f + lokal * s.tempo * size.height * 0.55f
                if (y < size.height + 20f) {
                    val x = s.x * size.width + kotlin.math.sin(lokal * 3f + s.x * 9f) * 24f
                    rotate(s.dreh * lokal, Offset(x, y)) {
                        drawRect(s.farbe, Offset(x, y), Size(s.groesse, s.groesse * 0.6f))
                    }
                }
            }
        }
    }
}
