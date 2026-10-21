package de.cemaras.yem1n.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.cemaras.yem1n.R
import de.cemaras.yem1n.logik.JagdBalance
import de.cemaras.yem1n.logik.JagdEreignis
import de.cemaras.yem1n.logik.JagdSpiel
import de.cemaras.yem1n.logik.JagdStick
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.TiefNavy
import kotlin.math.min
import kotlin.math.roundToInt

/** Ein kleiner Text, der aufsteigt und verblasst. */
private class JagdEffekt(val text: String, val x: Float, val y: Float, val groesse: Float, var alter: Float = 0f)

/** Wo die Finger gerade sind, in Bildschirmpunkten. */
private class JagdFinger {
    var stickId: PointerId? = null
    var stickStart = Offset.Zero
    var stickJetzt = Offset.Zero
    var sprungId: PointerId? = null
}

/**
 * Münzjagd mit den Bildern von Kenney (www.kenney.nl, Lizenz CC0).
 * Links ein Stick zum Laufen und Springen, rechts ein großer Sprungknopf.
 * Die Regeln stecken in [JagdSpiel], hier wird nur gezeichnet.
 */
@Composable
fun JagdScreen(onEnde: (Int) -> Unit) {
    val spiel = remember { JagdSpiel() }
    val finger = remember { JagdFinger() }
    val ende by rememberUpdatedState(onEnde)
    val effekte = remember { ArrayList<JagdEffekt>() }
    var bild by remember { mutableStateOf(0) }
    var blitz by remember { mutableStateOf(0f) }

    val bereit = ImageBitmap.imageResource(R.drawable.spiel_held_bereit)
    val held1 = ImageBitmap.imageResource(R.drawable.spiel_held_lauf1)
    val held2 = ImageBitmap.imageResource(R.drawable.spiel_held_lauf2)
    val heldSprung = ImageBitmap.imageResource(R.drawable.spiel_held_sprung)
    val heldAua = ImageBitmap.imageResource(R.drawable.spiel_held_aua)
    val muenze = ImageBitmap.imageResource(R.drawable.spiel_muenze)
    val herz = ImageBitmap.imageResource(R.drawable.spiel_leben)
    val boden = ImageBitmap.imageResource(R.drawable.spiel_boden)
    val gegnerBilder = listOf(
        ImageBitmap.imageResource(R.drawable.spiel_flieger_robo),
        ImageBitmap.imageResource(R.drawable.spiel_flieger_vogel),
        ImageBitmap.imageResource(R.drawable.spiel_hindernis_kugel),
    )

    LaunchedEffect(spiel) {
        spiel.beiEnde = { punkte -> ende(punkte) }
        var letzte = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (letzte == 0L) 0f else min(0.1f, (t - letzte) / 1_000_000_000f)
                letzte = t
                val st = if (finger.stickId != null) {
                    JagdStick.wert(finger.stickJetzt.x - finger.stickStart.x, -(finger.stickJetzt.y - finger.stickStart.y))
                } else {
                    0f to 0f
                }
                spiel.eingabe(st.first, finger.sprungId != null, JagdStick.springt(st.second))
                spiel.schritt(dt)
                for (e in spiel.ereignisse) {
                    SpielEffekte.jagd(e)
                    when (e.art) {
                        JagdEreignis.Art.FUNKEL -> effekte.add(JagdEffekt(e.text, e.x, e.y, if (e.text.length > 1) 34f else 26f))
                        JagdEreignis.Art.TREFFER -> blitz = 0.25f
                        JagdEreignis.Art.EBENE -> effekte.add(JagdEffekt("Ebene ${spiel.ebene} geschafft!", e.x, e.y, 40f))
                    }
                }
                spiel.ereignisse.clear()
                effekte.forEach { it.alter += dt }
                effekte.removeAll { it.alter > 1.0f }
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
                awaitPointerEventScope {
                    while (true) {
                        val ereignis = awaitPointerEvent()
                        for (c in ereignis.changes) {
                            if (c.pressed && !c.previousPressed) {
                                spiel.tippe()
                                if (c.position.x > size.width * 0.55f) {
                                    if (finger.sprungId == null) finger.sprungId = c.id
                                } else if (finger.stickId == null) {
                                    finger.stickId = c.id
                                    finger.stickStart = c.position
                                    finger.stickJetzt = c.position
                                }
                            } else if (c.pressed) {
                                if (c.id == finger.stickId) finger.stickJetzt = c.position
                            } else {
                                if (c.id == finger.stickId) finger.stickId = null
                                if (c.id == finger.sprungId) finger.sprungId = null
                            }
                            c.consume()
                        }
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val abonniert = bild
        val breite = JagdBalance.BREITE
        val hoehe = JagdBalance.HOEHE
        val s = min(size.width / breite, size.height / hoehe)
        val ox = (size.width - breite * s) / 2f
        val oy = (size.height - hoehe * s) / 2f
        fun sx(x: Float) = ox + x * s
        fun sy(y: Float) = oy + (hoehe - y) * s

        fun zeichne(b: ImageBitmap, cx: Float, cy: Float, h: Float, alpha: Float = 1f, breiteFaktor: Float = 1f, gespiegelt: Boolean = false) {
            val pxH = h * s
            val pxB = pxH * b.width / b.height * breiteFaktor
            withTransform({ if (gespiegelt) scale(-1f, 1f, Offset(sx(cx), sy(cy))) }) {
                drawImage(
                    b,
                    dstOffset = IntOffset((sx(cx) - pxB / 2f).roundToInt(), (sy(cy) - pxH / 2f).roundToInt()),
                    dstSize = IntSize(pxB.roundToInt().coerceAtLeast(1), pxH.roundToInt().coerceAtLeast(1)),
                    alpha = alpha,
                )
            }
        }

        // Hintergrund
        drawRect(TiefNavy)
        drawRect(Color.White.copy(alpha = 0.05f), topLeft = Offset(ox, oy), size = Size(breite * s, hoehe * s))

        // Boden über die ganze Breite
        val kachel = 160f * s
        var kx = ox
        while (kx > -kachel) kx -= kachel
        while (kx < size.width) {
            drawImage(
                boden,
                dstOffset = IntOffset(kx.roundToInt(), sy(JagdBalance.BODEN_OBERKANTE).roundToInt()),
                dstSize = IntSize((kachel + 1f).roundToInt(), (JagdBalance.PLATTFORM_DICKE * 1.4f * s).roundToInt()),
            )
            kx += kachel
        }
        drawRect(
            Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(0f, sy(JagdBalance.BODEN_OBERKANTE) + JagdBalance.PLATTFORM_DICKE * 1.4f * s),
            size = Size(size.width, size.height),
        )

        // Plattformen
        for (p in spiel.plattformen) {
            val breitePx = p.breite * s
            var x = sx(p.links)
            val stuecke = maxOf(1, (p.breite / 80f).roundToInt())
            val einzel = breitePx / stuecke
            for (i in 0 until stuecke) {
                drawImage(
                    boden,
                    dstOffset = IntOffset((x).roundToInt(), sy(p.y).roundToInt()),
                    dstSize = IntSize((einzel + 1f).roundToInt(), (JagdBalance.PLATTFORM_DICKE * s).roundToInt()),
                )
                x += einzel
            }
        }

        // Münzen, die markierte leuchtet
        val t = (spiel.zeit / 0.6f) % 2f
        val drehung = if (t < 1f) 1f - 0.75f * t else 0.25f + 0.75f * (t - 1f)
        val puls = 0.5f + 0.5f * kotlin.math.sin(spiel.zeit * 6f)
        for (m in spiel.muenzen) {
            if (m === spiel.markiert) {
                drawCircle(Gelb.copy(alpha = 0.18f + 0.22f * puls), radius = (30f + 6f * puls) * s, center = Offset(sx(m.x), sy(m.y)))
                zeichne(muenze, m.x, m.y, 44f, breiteFaktor = drehung)
            } else {
                zeichne(muenze, m.x, m.y, 32f, breiteFaktor = drehung)
            }
        }

        // Gegner
        for (g in spiel.gegner) {
            val alpha = if (g.aktivRest > 0f) 0.3f else 1f
            if (g.jaeger && g.aktivRest <= 0f) {
                drawCircle(Color.Red.copy(alpha = 0.18f), radius = 34f * s, center = Offset(sx(g.x), sy(g.y)))
            }
            zeichne(gegnerBilder[g.variante % gegnerBilder.size], g.x, g.y, 46f, alpha, gespiegelt = g.vx < 0f)
        }

        // Der Hase
        val figur = when {
            spiel.phase == JagdSpiel.Phase.VORBEI -> heldAua
            spiel.phase == JagdSpiel.Phase.WARTEN -> bereit
            !spiel.amBoden -> heldSprung
            spiel.laeuft -> if (((spiel.zeit / 0.13f).toInt() % 2) == 0) held1 else held2
            else -> bereit
        }
        val blinkt = spiel.unverwundbar > 0f && (spiel.unverwundbar * 12f).toInt() % 2 == 0
        zeichne(figur, spiel.x, spiel.y, JagdBalance.SPIELER_HOEHE, if (blinkt) 0.3f else 1f, gespiegelt = spiel.blickLinks)

        // Leben
        for (i in 0 until JagdBalance.MAX_LEBEN) {
            zeichne(herz, 36f + i * 30f, hoehe - 70f, 30f, if (i < spiel.leben) 1f else 0.18f)
        }

        // Stick links und Sprungknopf rechts
        val mitteStick = if (finger.stickId != null) finger.stickStart else Offset(sx(120f), sy(110f))
        val ring = JagdStick.RADIUS * s
        drawCircle(Color.White.copy(alpha = 0.12f), radius = ring, center = mitteStick)
        drawCircle(Color.White.copy(alpha = 0.4f), radius = ring, center = mitteStick, style = Stroke(width = 2f * s))
        val k = if (finger.stickId != null) {
            val v = JagdStick.knopf(finger.stickJetzt.x - finger.stickStart.x, finger.stickJetzt.y - finger.stickStart.y)
            Offset(mitteStick.x + v.first, mitteStick.y + v.second)
        } else {
            mitteStick
        }
        drawCircle(Color.White.copy(alpha = 0.55f), radius = 30f * s, center = k)
        val knopf = Offset(sx(breite - 120f), sy(110f))
        val gedrueckt = finger.sprungId != null
        drawCircle(Gelb.copy(alpha = if (gedrueckt) 0.75f else 0.4f), radius = 60f * s, center = knopf)
        drawCircle(Gelb.copy(alpha = 0.9f), radius = 60f * s, center = knopf, style = Stroke(width = 2f * s))

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
            val gelb = android.graphics.Color.argb(255, (Gelb.red * 255).toInt(), (Gelb.green * 255).toInt(), (Gelb.blue * 255).toInt())
            val weiss = android.graphics.Color.WHITE
            text("${spiel.punkte}", 24f, hoehe - 36f, 30f, gelb, Paint.Align.LEFT)
            text("Ebene ${spiel.ebene}", breite / 2, hoehe - 36f, 22f, weiss, Paint.Align.CENTER)
            text("Hoch", breite - 120f, 110f, 22f, android.graphics.Color.argb(255, 0, 32, 91), Paint.Align.CENTER)

            for (e in effekte) {
                val anteil = e.alter / 1.0f
                text(e.text, min(max2(e.x, 80f), breite - 80f), e.y + 40f * anteil, e.groesse, gelb, Paint.Align.CENTER, alpha = 1f - anteil)
            }

            when (spiel.phase) {
                JagdSpiel.Phase.WARTEN -> {
                    text("Sammle alle Münzen", breite / 2, hoehe * 0.62f, 34f, weiss, Paint.Align.CENTER)
                    text("Links laufen, rechts springen. Tippen zum Starten", breite / 2, hoehe * 0.62f - 38f, 17f, gelb, Paint.Align.CENTER, fett = false)
                }
                JagdSpiel.Phase.VORBEI -> {
                    text("${spiel.punkte} Punkte", breite / 2, hoehe * 0.62f, 34f, weiss, Paint.Align.CENTER)
                    text("Ebene ${spiel.ebene}. Tippen für noch einen Versuch", breite / 2, hoehe * 0.62f - 38f, 17f, gelb, Paint.Align.CENTER, fett = false)
                }
                else -> Unit
            }
        }

        if (blitz > 0f) drawRect(Color.Red.copy(alpha = (blitz / 0.25f) * 0.25f))
    }
}

private fun max2(a: Float, b: Float): Float = if (a > b) a else b
