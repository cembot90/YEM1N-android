package de.cemaras.yem1n.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spart Akku und Leistung auf schwachen Geräten: keine bewegten Lichtflecken, keine Federn. */
object Sparmodus {
    var an by mutableStateOf(false)
}

private val GlasFlaeche: Brush
    get() = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.06f)))

private val GlasKante: Brush
    get() = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.07f)))

/**
 * Glas-Karte: helle, leicht verlaufende Fläche mit feiner Kante. Echte Unschärfe des Hintergrunds
 * braucht zu viel Leistung für ein Kinder-Tablet, deshalb sieht die Karte nur wie Glas aus.
 */
fun Modifier.glas(radius: Dp = 20.dp): Modifier {
    val form = RoundedCornerShape(radius)
    return this.clip(form).background(GlasFlaeche).border(1.dp, GlasKante, form)
}

/**
 * Auswählbare Glas-Karte (Haustier, Hut, Abzeichen). Die gewählte Karte hat eine gelbe Kante und einen
 * warmen Schimmer, eine gesperrte ist blasser.
 */
fun Modifier.auswahl(gewaehlt: Boolean, radius: Dp = 18.dp, aktiv: Boolean = true): Modifier {
    val form = RoundedCornerShape(radius)
    val flaeche = if (gewaehlt) Brush.verticalGradient(listOf(Gelb.copy(alpha = 0.34f), Gelb.copy(alpha = 0.14f))) else GlasFlaeche
    val kante = if (gewaehlt) Brush.verticalGradient(listOf(Gelb, Gelb.copy(alpha = 0.55f))) else GlasKante
    return this.clip(form).background(flaeche).border(if (gewaehlt) 2.dp else 1.dp, kante, form)
        .graphicsLayer { alpha = if (aktiv) 1f else 0.5f }
}

/** Lässt eine Fläche beim Drücken kurz einfedern. [quelle] gehört auch dem `clickable` der Fläche. */
@Composable
fun Modifier.feder(quelle: MutableInteractionSource): Modifier {
    val gedrueckt by quelle.collectIsPressedAsState()
    val ziel = if (gedrueckt && !Sparmodus.an) 0.95f else 1f
    val skala by animateFloatAsState(
        ziel, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "feder",
    )
    return this.graphicsLayer { scaleX = skala; scaleY = skala }
}

/** Weiche Lichtflecken hinter allem. Sie wandern ganz langsam, im Sparmodus stehen sie still. */
@Composable
fun LichtFlecken(modifier: Modifier = Modifier.fillMaxSize()) {
    val phase = if (Sparmodus.an) 0f else {
        val t = rememberInfiniteTransition(label = "licht")
        val p by t.animateFloat(
            0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse), label = "lichtphase",
        )
        p
    }
    Canvas(modifier) {
        val b = size.width
        val h = size.height
        fun fleck(farbe: Color, cx: Float, cy: Float, r: Float) {
            drawCircle(
                Brush.radialGradient(listOf(farbe.copy(alpha = 0.32f), Color.Transparent), Offset(cx, cy), r),
                r, Offset(cx, cy),
            )
        }
        fleck(Glanz, b * (0.15f + 0.25f * phase), h * 0.12f, b * 0.75f)
        fleck(Himmel, b * (0.95f - 0.2f * phase), h * (0.55f + 0.05f * phase), b * 0.6f)
        fleck(Gelb, b * 0.2f, h * (1.0f - 0.08f * phase), b * 0.5f)
    }
}

/** Ring, der sich zum Anteil (0 bis 1) füllt. In der Mitte steht [inhalt]. */
@Composable
fun Fortschrittsring(
    anteil: Float,
    modifier: Modifier = Modifier,
    groesse: Dp = 64.dp,
    strich: Dp = 7.dp,
    farbe: Color = Gelb,
    inhalt: @Composable () -> Unit = {},
) {
    val gezeigt by animateFloatAsState(anteil.coerceIn(0f, 1f), tween(if (Sparmodus.an) 0 else 700), label = "ring")
    Box(modifier.size(groesse), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = strich.toPx()
            val d = size.minDimension - s
            val oben = Offset(s / 2, s / 2)
            drawArc(Color.White.copy(alpha = 0.16f), 0f, 360f, false, oben, Size(d, d), style = Stroke(s))
            drawArc(farbe, -90f, 360f * gezeigt, false, oben, Size(d, d), style = Stroke(s, cap = StrokeCap.Round))
        }
        inhalt()
    }
}
