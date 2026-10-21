package de.cemaras.yem1n.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.cemaras.yem1n.R
import de.cemaras.yem1n.logik.HockeyBalance
import de.cemaras.yem1n.logik.HockeyEreignis
import de.cemaras.yem1n.logik.HockeySpiel
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Himmel
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TiefNavy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Ein Text, der aufsteigt und verblasst. */
private class HockeyEffekt(val text: String, val x: Float, val y: Float, val groesse: Float, val farbe: Int, var alter: Float = 0f)

/** Ein Konfetti- oder Funkenstück. */
private class HockeyStueck(var x: Float, var y: Float, var vx: Float, var vy: Float, val farbe: Color, val dauer: Float, var alter: Float = 0f)

/**
 * Münz-Hockey mit den Bildern von Kenney (www.kenney.nl, Lizenz CC0).
 * Die Regeln stecken in [HockeySpiel], hier wird nur gezeichnet.
 */
@Composable
fun HockeyScreen(onEnde: (Int) -> Unit) {
    val spiel = remember { HockeySpiel() }
    val ende by rememberUpdatedState(onEnde)
    val effekte = remember { ArrayList<HockeyEffekt>() }
    val stuecke = remember { ArrayList<HockeyStueck>() }
    var bild by remember { mutableStateOf(0) }
    var blitz by remember { mutableStateOf(0f) }

    val muenze = ImageBitmap.imageResource(R.drawable.spiel_muenze)
    val held = ImageBitmap.imageResource(R.drawable.spiel_held_sprung)
    val kugel = ImageBitmap.imageResource(R.drawable.spiel_hindernis_kugel)

    val gelbArgb = android.graphics.Color.argb(255, (Gelb.red * 255).toInt(), (Gelb.green * 255).toInt(), (Gelb.blue * 255).toInt())

    fun streue(x: Float, y: Float, anzahl: Int, farben: List<Color>, tempo: Float, dauer: Float) {
        repeat(anzahl) {
            val winkel = Random.nextFloat() * 2f * PI.toFloat()
            val t = tempo * (0.4f + Random.nextFloat() * 0.6f)
            stuecke.add(HockeyStueck(x, y, cos(winkel) * t, sin(winkel) * t, farben.random(), dauer))
        }
    }

    LaunchedEffect(spiel) {
        spiel.beiEnde = { punkte -> ende(punkte) }
        var letzte = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (letzte == 0L) 0f else min(0.1f, (t - letzte) / 1_000_000_000f)
                letzte = t
                spiel.schritt(dt)
                for (e in spiel.ereignisse) {
                    SpielEffekte.hockey(e)
                    when (e.art) {
                        HockeyEreignis.Art.SCHLAG -> streue(e.x, e.y, 8, listOf(Himmel, Color.White), 220f, 0.4f)
                        HockeyEreignis.Art.TOR_ICH -> {
                            streue(e.x, e.y, 60, listOf(Gelb, Koralle, Himmel, Color.White, Color(0xFF73F29E)), 380f, 1.2f)
                            effekte.add(HockeyEffekt("Tor!", e.x, e.y, 30f, gelbArgb))
                            effekte.add(HockeyEffekt("TOR!", HockeyBalance.BREITE / 2, HockeyBalance.HOEHE / 2, 52f, gelbArgb))
                        }
                        HockeyEreignis.Art.TOR_KI -> {
                            blitz = 0.25f
                            effekte.add(HockeyEffekt("Aua", e.x, e.y, 30f, android.graphics.Color.WHITE))
                        }
                        HockeyEreignis.Art.SIEG -> streue(e.x, e.y, 70, listOf(Gelb, Koralle, Himmel, Color.White), 420f, 1.4f)
                    }
                }
                spiel.ereignisse.clear()
                effekte.forEach { it.alter += dt }
                effekte.removeAll { it.alter > 1.0f }
                for (k in stuecke) {
                    k.alter += dt
                    k.x += k.vx * dt
                    k.y += k.vy * dt
                    k.vx *= 0.98f
                    k.vy *= 0.98f
                }
                stuecke.removeAll { it.alter > it.dauer }
                if (blitz > 0f) blitz = maxOf(0f, blitz - dt)
                bild += 1
            }
        }
    }

    val farbe = remember { Paint().apply { isAntiAlias = true } }

    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(spiel) {
                val breite = HockeyBalance.BREITE
                val hoehe = HockeyBalance.HOEHE
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val s = min(size.width / breite, size.height / hoehe)
                        val ox = (size.width - breite * s) / 2f
                        val oy = (size.height - hoehe * s) / 2f
                        spiel.beruehre((down.position.x - ox) / s, hoehe - (down.position.y - oy) / s)
                        down.consume()
                        var fertig = false
                        while (!fertig) {
                            val ereignis = awaitPointerEvent()
                            val c = ereignis.changes.firstOrNull { it.id == down.id }
                            if (c == null || !c.pressed) {
                                fertig = true
                            } else {
                                spiel.fuehre((c.position.x - ox) / s, hoehe - (c.position.y - oy) / s)
                            }
                            c?.consume()
                        }
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val abonniert = bild
        val breite = HockeyBalance.BREITE
        val hoehe = HockeyBalance.HOEHE
        val s = min(size.width / breite, size.height / hoehe)
        val ox = (size.width - breite * s) / 2f
        val oy = (size.height - hoehe * s) / 2f
        fun sx(x: Float) = ox + x * s
        fun sy(y: Float) = oy + (hoehe - y) * s

        fun zeichne(b: ImageBitmap, cx: Float, cy: Float, h: Float) {
            val pxH = h * s
            val pxB = pxH * b.width / b.height
            drawImage(
                b,
                dstOffset = IntOffset((sx(cx) - pxB / 2f).roundToInt(), (sy(cy) - pxH / 2f).roundToInt()),
                dstSize = IntSize(pxB.roundToInt().coerceAtLeast(1), pxH.roundToInt().coerceAtLeast(1)),
            )
        }

        drawRect(TiefNavy)

        // Spielfeld
        val links = HockeySpiel.LINKS
        val rechts = HockeySpiel.RECHTS
        val unten = HockeySpiel.UNTEN
        val oben = HockeySpiel.OBEN
        drawRoundRect(Navy, topLeft = Offset(sx(links), sy(oben)), size = Size((rechts - links) * s, (oben - unten) * s), cornerRadius = CornerRadius(16f * s))
        drawRoundRect(
            Color.White.copy(alpha = 0.7f), topLeft = Offset(sx(links), sy(oben)), size = Size((rechts - links) * s, (oben - unten) * s),
            cornerRadius = CornerRadius(16f * s), style = Stroke(5f * s),
        )
        drawLine(Color.White.copy(alpha = 0.35f), Offset(sx(HockeySpiel.MITTE), sy(unten)), Offset(sx(HockeySpiel.MITTE), sy(oben)), 3f * s)
        drawCircle(Color.White.copy(alpha = 0.35f), radius = 74f * s, center = Offset(sx(HockeySpiel.MITTE), sy(hoehe / 2)), style = Stroke(3f * s))
        drawLine(Himmel, Offset(sx(links), sy(HockeySpiel.TOR_UNTEN)), Offset(sx(links), sy(HockeySpiel.TOR_OBEN)), 12f * s, StrokeCap.Butt)
        drawLine(Koralle, Offset(sx(rechts), sy(HockeySpiel.TOR_UNTEN)), Offset(sx(rechts), sy(HockeySpiel.TOR_OBEN)), 12f * s, StrokeCap.Butt)

        // Funken und Konfetti
        for (k in stuecke) {
            drawRect(
                k.farbe.copy(alpha = (1f - k.alter / k.dauer).coerceIn(0f, 1f)),
                topLeft = Offset(sx(k.x) - 4f * s, sy(k.y) - 4f * s), size = Size(8f * s, 8f * s),
            )
        }

        // Puck
        withTransform({ rotate(-spiel.puckDreh * 180f / PI.toFloat(), Offset(sx(spiel.puckX), sy(spiel.puckY))) }) {
            zeichne(muenze, spiel.puckX, spiel.puckY, HockeyBalance.PUCK_RADIUS * 2f)
        }

        // Schläger mit Ring
        val sr = HockeyBalance.SCHLAEGER_RADIUS
        drawCircle(Himmel.copy(alpha = 0.18f), radius = sr * s, center = Offset(sx(spiel.meinX), sy(spiel.meinY)))
        drawCircle(Himmel, radius = sr * s, center = Offset(sx(spiel.meinX), sy(spiel.meinY)), style = Stroke(5f * s))
        zeichne(held, spiel.meinX, spiel.meinY, sr * 1.9f)
        drawCircle(Koralle.copy(alpha = 0.18f), radius = sr * s, center = Offset(sx(spiel.kiX), sy(spiel.kiY)))
        drawCircle(Koralle, radius = sr * s, center = Offset(sx(spiel.kiX), sy(spiel.kiY)), style = Stroke(5f * s))
        zeichne(kugel, spiel.kiX, spiel.kiY, sr * 1.9f)

        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            fun text(text: String, x: Float, y: Float, groesse: Float, farbeArgb: Int, ausrichtung: Paint.Align, fett: Boolean = true, alpha: Float = 1f) {
                farbe.typeface = if (fett) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                farbe.color = farbeArgb
                farbe.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
                farbe.textSize = groesse * s
                farbe.textAlign = ausrichtung
                nc.drawText(text, sx(x), sy(y) - (farbe.descent() + farbe.ascent()) / 2f, farbe)
            }
            val weiss = android.graphics.Color.WHITE
            text("Du ${spiel.toreIch} : ${spiel.toreKi} Computer", HockeySpiel.MITTE, hoehe - 18f, 22f, gelbArgb, Paint.Align.CENTER)
            text("Level ${spiel.level}   Punkte ${spiel.punkte}", HockeySpiel.MITTE, 18f, 16f, weiss, Paint.Align.CENTER)

            for (e in effekte) {
                val anteil = e.alter / 1.0f
                text(e.text, e.x, e.y + 36f * anteil, e.groesse, e.farbe, Paint.Align.CENTER, alpha = 1f - anteil)
            }

            when {
                spiel.phase == HockeySpiel.Phase.WARTEN -> {
                    text("Münz-Hockey", HockeySpiel.MITTE, hoehe / 2 + 70f, 36f, weiss, Paint.Align.CENTER)
                    text("Tippen zum Start. Mit dem Finger den Schläger bewegen.", HockeySpiel.MITTE, hoehe / 2 + 32f, 17f, gelbArgb, Paint.Align.CENTER, fett = false)
                }
                spiel.siegMeldung -> {
                    text("Level ${spiel.level} geschafft!", HockeySpiel.MITTE, hoehe / 2 + 70f, 36f, weiss, Paint.Align.CENTER)
                    text("Der Computer wird jetzt besser", HockeySpiel.MITTE, hoehe / 2 + 32f, 17f, gelbArgb, Paint.Align.CENTER, fett = false)
                }
                spiel.phase == HockeySpiel.Phase.VORBEI -> {
                    text("${spiel.punkte} Punkte", HockeySpiel.MITTE, hoehe / 2 + 70f, 36f, weiss, Paint.Align.CENTER)
                    text("Level ${spiel.level}. Tippen für noch einen Versuch", HockeySpiel.MITTE, hoehe / 2 + 32f, 17f, gelbArgb, Paint.Align.CENTER, fett = false)
                }
            }
        }

        if (blitz > 0f) drawRect(Color.Red.copy(alpha = (blitz / 0.25f) * 0.25f))
    }
}
