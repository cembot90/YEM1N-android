package de.cemaras.yem1n.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.cemaras.yem1n.R
import de.cemaras.yem1n.logik.LaufBalance
import de.cemaras.yem1n.logik.LaufEreignis
import de.cemaras.yem1n.logik.LaufObjekt
import de.cemaras.yem1n.logik.LaufSpiel
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.TiefNavy
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Ein kleiner Text, der aufsteigt und verblasst. */
private class LaufEffekt(val text: String, val x: Float, val y: Float, val groesse: Float, var alter: Float = 0f)

/**
 * Zahlenlauf mit den Bildern von Kenney (www.kenney.nl, Lizenz CC0).
 * Die Regeln stecken in [LaufSpiel], hier wird nur gezeichnet.
 */
@Composable
fun LaufScreen(onEnde: (Int) -> Unit) {
    val spiel = remember { LaufSpiel() }
    val ende by rememberUpdatedState(onEnde)
    val effekte = remember { ArrayList<LaufEffekt>() }
    var bild by remember { mutableStateOf(0) }
    var blitz by remember { mutableStateOf(0f) }
    val sterne = remember { List(46) { Triple(Random.nextFloat(), Random.nextFloat() * 0.8f + 0.2f, Random.nextFloat()) } }

    val held1 = ImageBitmap.imageResource(R.drawable.spiel_held_lauf1)
    val held2 = ImageBitmap.imageResource(R.drawable.spiel_held_lauf2)
    val heldSprung = ImageBitmap.imageResource(R.drawable.spiel_held_sprung)
    val heldAua = ImageBitmap.imageResource(R.drawable.spiel_held_aua)
    val hindernisse = listOf(
        ImageBitmap.imageResource(R.drawable.spiel_hindernis_kaktus),
        ImageBitmap.imageResource(R.drawable.spiel_hindernis_kugel),
        ImageBitmap.imageResource(R.drawable.spiel_hindernis_pilz),
    )
    val flieger = listOf(
        ImageBitmap.imageResource(R.drawable.spiel_flieger_robo),
        ImageBitmap.imageResource(R.drawable.spiel_flieger_vogel),
    )
    val muenze = ImageBitmap.imageResource(R.drawable.spiel_muenze)
    val feder = ImageBitmap.imageResource(R.drawable.spiel_feder)
    val fluegel = ImageBitmap.imageResource(R.drawable.spiel_fluegel)
    val herz = ImageBitmap.imageResource(R.drawable.spiel_leben)
    val boden = ImageBitmap.imageResource(R.drawable.spiel_boden)
    val huegel = ImageBitmap.imageResource(R.drawable.spiel_huegel)

    LaunchedEffect(spiel) {
        spiel.beiEnde = { punkte -> ende(punkte) }
        var letzte = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (letzte == 0L) 0f else min(0.1f, (t - letzte) / 1_000_000_000f)
                letzte = t
                spiel.schritt(dt)
                for (e in spiel.ereignisse) {
                    SpielEffekte.lauf(e)
                    when (e.art) {
                        LaufEreignis.Art.MUENZE -> effekte.add(LaufEffekt("✦", e.x, e.y, 26f))
                        LaufEreignis.Art.FLUEGEL -> effekte.add(LaufEffekt("✦", e.x, e.y, 34f))
                        LaufEreignis.Art.TREFFER -> blitz = 0.25f
                        LaufEreignis.Art.STUFE -> effekte.add(LaufEffekt("Stufe ${spiel.stufe}!", LaufBalance.BREITE / 2, LaufBalance.HOEHE * 0.7f, 44f))
                    }
                }
                spiel.ereignisse.clear()
                effekte.forEach { it.alter += dt }
                effekte.removeAll { it.alter > 0.8f }
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
                detectTapGestures(onPress = {
                    spiel.druecke()
                    tryAwaitRelease()
                    spiel.loslassen()
                })
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val abonniert = bild
        val breite = LaufBalance.BREITE
        val hoehe = LaufBalance.HOEHE
        val s = min(size.width / breite, size.height / hoehe)
        val ox = (size.width - breite * s) / 2f
        val oy = (size.height - hoehe * s) / 2f
        fun sx(x: Float) = ox + x * s
        fun sy(y: Float) = oy + (hoehe - y) * s

        /** Zeichnet ein Bild mit der Mitte bei (cx, cy) in Bühnenpunkten und einer Höhe in Bühnenpunkten. */
        fun zeichne(b: ImageBitmap, cx: Float, cy: Float, h: Float, alpha: Float = 1f, breiteFaktor: Float = 1f, filter: ColorFilter? = null) {
            val pxH = h * s
            val pxB = pxH * b.width / b.height * breiteFaktor
            drawImage(
                b,
                dstOffset = IntOffset((sx(cx) - pxB / 2f).roundToInt(), (sy(cy) - pxH / 2f).roundToInt()),
                dstSize = IntSize(pxB.roundToInt().coerceAtLeast(1), pxH.roundToInt().coerceAtLeast(1)),
                alpha = alpha,
                colorFilter = filter,
            )
        }

        // Nachthimmel mit Sternen
        drawRect(TiefNavy)
        for ((a, b, c) in sterne) {
            val blinken = 0.35f + 0.5f * (0.5f + 0.5f * kotlin.math.sin(spiel.zeit * 2f + c * 6f))
            drawCircle(Color.White.copy(alpha = blinken), radius = (1.2f + c * 1.6f) * s, center = Offset(a * size.width, (1f - b) * size.height))
        }

        // Hügel ziehen langsam vorbei
        val berg = maxOf(breite * 1.2f, 600f)
        val versatz = (spiel.zeit * 46f) % berg
        for (i in 0..1) {
            val x0 = i * berg - versatz
            drawImage(
                huegel,
                dstOffset = IntOffset(sx(x0).roundToInt(), sy(LaufSpiel.BODEN_Y - 10f + berg * 0.5f).roundToInt()),
                dstSize = IntSize((berg * s).roundToInt(), (berg * 0.5f * s).roundToInt()),
                alpha = 0.16f,
            )
        }

        // Boden aus Kacheln, über die ganze Bildschirmbreite
        val kachel = 220f * s
        val kachelHoehe = 56f * s
        var kx = ox - (spiel.weg % 220f) * s
        while (kx > -kachel) kx -= kachel
        while (kx < size.width) {
            drawImage(
                boden,
                dstOffset = IntOffset(kx.roundToInt(), sy(LaufSpiel.BODEN_Y).roundToInt()),
                dstSize = IntSize((kachel + 1f).roundToInt(), kachelHoehe.roundToInt()),
            )
            kx += kachel
        }

        // Münzen, Hindernisse, Flieger, Federn und Flügel
        val t = (spiel.zeit / 0.5f) % 2f
        val drehung = if (t < 1f) 1f - 0.75f * t else 0.25f + 0.75f * (t - 1f)
        for (o in spiel.objekte) {
            when (o.art) {
                LaufObjekt.Art.HINDERNIS -> zeichne(hindernisse[o.variante % hindernisse.size], o.x, o.y, LaufSpiel.HINDERNIS_HOEHE)
                LaufObjekt.Art.FLIEGER -> zeichne(flieger[o.variante % flieger.size], o.x, o.y, 52f)
                LaufObjekt.Art.MUENZE -> zeichne(muenze, o.x, o.y, 38f, breiteFaktor = drehung)
                LaufObjekt.Art.FEDER -> zeichne(feder, o.x, o.y, LaufSpiel.FEDER_HOEHE)
                LaufObjekt.Art.FLUEGEL -> zeichne(fluegel, o.x, o.y, 44f)
            }
        }

        // Der Held
        val figur = when {
            spiel.phase == LaufSpiel.Phase.VORBEI || spiel.auaRest > 0f -> heldAua
            !spiel.amBoden -> heldSprung
            ((spiel.zeit / 0.13f).toInt() % 2) == 0 -> held1
            else -> held2
        }
        val blinkt = spiel.unverwundbarRest > 0f && (spiel.unverwundbarRest * 12f).toInt() % 2 == 0
        val gefluegelt = if (spiel.fluegelRest > 0f) ColorFilter.tint(Gelb.copy(alpha = 0.35f), BlendMode.SrcAtop) else null
        zeichne(figur, LaufSpiel.SPIELER_X, spiel.y, LaufSpiel.SPIELER_HOEHE, if (blinkt) 0.3f else 1f, filter = gefluegelt)

        // Leben
        for (i in 0 until LaufSpiel.MAX_LEBEN) {
            zeichne(herz, 30f + i * 30f, hoehe - 74f, 32f, if (i < spiel.leben) 1f else 0.18f)
        }

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
            text("Stufe ${spiel.stufe}", 24f, hoehe - 112f, 17f, weiss, Paint.Align.LEFT)

            for (e in effekte) {
                val anteil = e.alter / 0.8f
                text(e.text, min(e.x, breite - 60f), e.y + 40f * anteil, e.groesse, gelb, Paint.Align.CENTER, alpha = 1f - anteil)
            }

            when (spiel.phase) {
                LaufSpiel.Phase.WARTEN -> {
                    text("Tippen und losrennen", breite / 2, hoehe * 0.62f, 34f, weiss, Paint.Align.CENTER)
                    text("Tippen zum Springen, zweites Tippen für den Doppelsprung", breite / 2, hoehe * 0.62f - 38f, 17f, gelb, Paint.Align.CENTER, fett = false)
                }
                LaufSpiel.Phase.VORBEI -> {
                    text("${spiel.punkte} Punkte", breite / 2, hoehe * 0.62f, 34f, weiss, Paint.Align.CENTER)
                    text("Tippen für noch einen Versuch", breite / 2, hoehe * 0.62f - 38f, 17f, gelb, Paint.Align.CENTER, fett = false)
                }
                LaufSpiel.Phase.LAEUFT -> Unit
            }
        }

        // Kurzes rotes Aufblitzen nach einem Treffer
        if (blitz > 0f) drawRect(Color.Red.copy(alpha = (blitz / 0.25f) * 0.25f))
    }
}
