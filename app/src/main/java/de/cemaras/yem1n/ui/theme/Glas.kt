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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

/** Glas-Karte zum Antippen, die beim Drücken federt. */
fun Modifier.federKarte(radius: Dp = 20.dp, onClick: () -> Unit): Modifier = composed {
    val quelle = remember { MutableInteractionSource() }
    this
        .feder(quelle)
        .glas(radius)
        .clickable(interactionSource = quelle, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
}

/** Kleinste Tippfläche: 48 dp, wie Android es empfiehlt. Kinderfinger treffen kleine Knöpfe schlecht. */
val Tippziel: Dp = 48.dp

/**
 * Text zum Antippen (Vorlesen, Nochmal). Die Tippfläche ist mindestens 48 dp hoch,
 * der Text steht in der Mitte und sieht so klein aus wie vorher.
 */
fun Modifier.tippText(onClick: () -> Unit): Modifier = this
    .heightIn(min = Tippziel)
    .clip(RoundedCornerShape(50))
    .clickable(role = Role.Button, onClick = onClick)
    .wrapContentHeight(Alignment.CenterVertically)
    .padding(horizontal = 8.dp)

/** Runder Knopf mit einem Zeichen (Vorlesen, Schließen). [name] ist der Text für die Sprachausgabe TalkBack. */
@Composable
fun RundKnopf(
    symbol: String,
    modifier: Modifier = Modifier,
    betont: Boolean = false,
    name: String = symbol,
    groesse: Dp = Tippziel,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(groesse)
            .clip(CircleShape)
            .background(if (betont) Gelb else Color.White.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 20.sp, color = if (betont) Navy else Color.White, fontWeight = FontWeight.Black)
    }
}

/**
 * Der runde gelbe Vorlese-Knopf. Der Lautsprecher ist gezeichnet (Navy auf Gelb, wie bei iOS),
 * denn das Emoji ist auf Gelb kaum zu erkennen.
 */
@Composable
fun VorleseKnopf(modifier: Modifier = Modifier, groesse: Dp = Tippziel, onClick: () -> Unit) {
    val farbe = Navy
    Box(
        modifier
            .size(groesse)
            .clip(CircleShape)
            .background(Gelb)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Vorlesen" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(groesse * 0.5f)) {
            // Gezeichnet auf einem Raster von 24 mal 24
            val e = size.minDimension / 24f
            val box = Path().apply {
                moveTo(3f * e, 9f * e); lineTo(7.5f * e, 9f * e); lineTo(13f * e, 4f * e)
                lineTo(13f * e, 20f * e); lineTo(7.5f * e, 15f * e); lineTo(3f * e, 15f * e); close()
            }
            drawPath(box, farbe)
            val strich = Stroke(2.2f * e, cap = StrokeCap.Round)
            for (r in listOf(4.2f, 8f)) {
                drawArc(
                    farbe, -45f, 90f, false,
                    topLeft = Offset((13.5f - r) * e, (12f - r) * e), size = Size(2 * r * e, 2 * r * e), style = strich,
                )
            }
        }
    }
}

/** Fortschrittsbalken mit runden Enden, überall gleich. [anteil] geht von 0 bis 1. */
@Composable
fun Fortschrittsbalken(anteil: Float, modifier: Modifier = Modifier, hoehe: Dp = 8.dp, farbe: Color = Gelb) {
    val ziel = if (anteil.isNaN()) 0f else anteil.coerceIn(0f, 1f)
    val gezeigt by animateFloatAsState(ziel, tween(if (Sparmodus.an) 0 else 400), label = "balken")
    Box(
        modifier
            .fillMaxWidth()
            .height(hoehe)
            .progressSemantics(ziel)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f)),
    ) {
        Box(Modifier.fillMaxWidth(gezeigt).fillMaxHeight().clip(CircleShape).background(farbe))
    }
}
