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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import de.cemaras.yem1n.logik.ElfmeterEreignis
import de.cemaras.yem1n.logik.ElfmeterRegeln
import de.cemaras.yem1n.logik.ElfmeterSpiel
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Himmel
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.TiefNavy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Ein Text, der aufsteigt und verblasst. */
private class ElfEffekt(val text: String, val x: Float, val y: Float, val groesse: Float, val farbe: Int, var alter: Float = 0f)

/** Ein Konfettistück. */
private class ElfKonfetti(var x: Float, var y: Float, var vx: Float, var vy: Float, val farbe: Color, var alter: Float = 0f)

/**
 * Elfmeterschießen: vom Ball zum Tor wischen. Die Regeln stecken in [ElfmeterSpiel],
 * hier wird nur gezeichnet. Der Ball und die Handschuhe sind Emoji.
 */
@Composable
fun ElfmeterScreen(onEnde: (Int) -> Unit) {
    val spiel = remember { ElfmeterSpiel() }
    val ende by rememberUpdatedState(onEnde)
    val effekte = remember { ArrayList<ElfEffekt>() }
    val konfetti = remember { ArrayList<ElfKonfetti>() }
    var bild by remember { mutableStateOf(0) }
    var blitz by remember { mutableStateOf(0f) }

    val gelbArgb = android.graphics.Color.argb(255, (Gelb.red * 255).toInt(), (Gelb.green * 255).toInt(), (Gelb.blue * 255).toInt())
    val himmelArgb = android.graphics.Color.argb(255, (Himmel.red * 255).toInt(), (Himmel.green * 255).toInt(), (Himmel.blue * 255).toInt())
    val koralleArgb = android.graphics.Color.argb(255, (Koralle.red * 255).toInt(), (Koralle.green * 255).toInt(), (Koralle.blue * 255).toInt())

    fun regnen(x: Float, y: Float) {
        val farben = listOf(Gelb, Koralle, Himmel, Color.White, Color(0xFF73F29E))
        repeat(26) {
            val winkel = Random.nextFloat() * 2f * PI.toFloat()
            val tempo = 120f + Random.nextFloat() * 280f
            konfetti.add(ElfKonfetti(x, y, cos(winkel) * tempo, sin(winkel) * tempo + 140f, farben.random()))
        }
    }

    LaunchedEffect(spiel) {
        spiel.beiEnde = { punkte ->
            regnen(ElfmeterRegeln.BREITE / 2, 240f)
            ende(punkte)
        }
        var letzte = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (letzte == 0L) 0f else min(0.1f, (t - letzte) / 1_000_000_000f)
                letzte = t
                spiel.schritt(dt)
                for (e in spiel.ereignisse) {
                    when (e.art) {
                        ElfmeterEreignis.Art.TOR -> {
                            regnen(e.x, e.y)
                            effekte.add(ElfEffekt(e.text, ElfmeterRegeln.BREITE / 2, 228f, 40f, gelbArgb))
                        }
                        ElfmeterEreignis.Art.GEHALTEN -> {
                            blitz = 0.25f
                            effekte.add(ElfEffekt(e.text, e.x, e.y, 40f, koralleArgb))
                        }
                        ElfmeterEreignis.Art.DANEBEN -> effekte.add(ElfEffekt(e.text, e.x, e.y, 40f, himmelArgb))
                        ElfmeterEreignis.Art.LEVEL -> effekte.add(ElfEffekt(e.text, e.x, e.y, 36f, android.graphics.Color.WHITE))
                    }
                }
                spiel.ereignisse.clear()
                effekte.forEach { it.alter += dt }
                effekte.removeAll { it.alter > 1.0f }
                for (k in konfetti) {
                    k.alter += dt
                    k.vy -= 520f * dt
                    k.x += k.vx * dt
                    k.y += k.vy * dt
                }
                konfetti.removeAll { it.alter > 1.2f }
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
                val breite = ElfmeterRegeln.BREITE
                val hoehe = ElfmeterRegeln.HOEHE
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val s = min(size.width / breite, size.height / hoehe)
                        val ox = (size.width - breite * s) / 2f
                        val oy = (size.height - hoehe * s) / 2f
                        fun bx(p: Offset) = (p.x - ox) / s
                        fun by(p: Offset) = hoehe - (p.y - oy) / s
                        val start = down.uptimeMillis
                        spiel.beruehre(bx(down.position), by(down.position))
                        down.consume()
                        var letzte = down.position
                        var fertig = false
                        while (!fertig) {
                            val ereignis = awaitPointerEvent()
                            val c = ereignis.changes.firstOrNull { it.id == down.id }
                            if (c == null) {
                                spiel.abbrechen()
                                fertig = true
                            } else {
                                letzte = c.position
                                if (c.pressed) {
                                    spiel.bewege(bx(c.position), by(c.position))
                                } else {
                                    spiel.loslassen(bx(c.position), by(c.position), (c.uptimeMillis - start) / 1000f)
                                    fertig = true
                                }
                                c.consume()
                            }
                        }
                        @Suppress("UNUSED_VALUE")
                        letzte = letzte
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val abonniert = bild
        val breite = ElfmeterRegeln.BREITE
        val hoehe = ElfmeterRegeln.HOEHE
        val s = min(size.width / breite, size.height / hoehe)
        val ox = (size.width - breite * s) / 2f
        val oy = (size.height - hoehe * s) / 2f
        fun sx(x: Float) = ox + x * s
        fun sy(y: Float) = oy + (hoehe - y) * s

        // Nachthimmel und Flutlicht
        drawRect(TiefNavy)
        for (x in listOf(120f, breite - 120f)) {
            drawCircle(Color.White.copy(alpha = 0.12f), radius = 130f * s, center = Offset(sx(x), sy(hoehe - 40f)))
        }

        // Rasen mit hellen und dunklen Streifen
        val rasenHoehe = ElfmeterRegeln.TOR_UNTEN + 8f
        val streifen = 8
        val h = rasenHoehe / streifen
        for (i in 0 until streifen) {
            val c = if (i % 2 == 0) Color(0xFF299E4D) else Color(0xFF218C45)
            drawRect(c, topLeft = Offset(0f, sy((i + 1) * h)), size = Size(size.width, h * s + 1f))
        }
        // Rasen unter dem Spielfeldrand, falls der Bildschirm höher ist
        drawRect(Color(0xFF218C45), topLeft = Offset(0f, sy(0f)), size = Size(size.width, size.height))

        // Linien und Elfmeterpunkt
        val linie = Color.White.copy(alpha = 0.55f)
        val lw = 4f * s
        drawLine(linie, Offset(sx(150f), sy(ElfmeterRegeln.TOR_UNTEN)), Offset(sx(70f), sy(40f)), lw)
        drawLine(linie, Offset(sx(breite - 150f), sy(ElfmeterRegeln.TOR_UNTEN)), Offset(sx(breite - 70f), sy(40f)), lw)
        drawLine(linie, Offset(0f, sy(ElfmeterRegeln.TOR_UNTEN)), Offset(size.width, sy(ElfmeterRegeln.TOR_UNTEN)), lw)
        drawOval(Color.White, topLeft = Offset(sx(ElfmeterRegeln.BALL_X - 11f), sy(ElfmeterRegeln.BALL_Y - 30f) - 5f * s), size = Size(22f * s, 10f * s))

        // Das Netz wackelt nach einem Tor, es sitzt unten am Tor
        withTransform({ scale(1f, 1f + 0.03f * spiel.netz, Offset(sx(ElfmeterRegeln.TOR_MITTE), sy(ElfmeterRegeln.TOR_UNTEN))) }) {
            val gitter = Color.White.copy(alpha = 0.25f)
            var x = ElfmeterRegeln.TOR_LINKS
            while (x <= ElfmeterRegeln.TOR_RECHTS) {
                drawLine(gitter, Offset(sx(x), sy(ElfmeterRegeln.TOR_UNTEN)), Offset(sx(x), sy(ElfmeterRegeln.TOR_OBEN)), 1.5f * s)
                x += 26f
            }
            var y = ElfmeterRegeln.TOR_UNTEN
            while (y <= ElfmeterRegeln.TOR_OBEN) {
                drawLine(gitter, Offset(sx(ElfmeterRegeln.TOR_LINKS), sy(y)), Offset(sx(ElfmeterRegeln.TOR_RECHTS), sy(y)), 1.5f * s)
                y += 22f
            }
        }

        // Torwart
        withTransform({ rotate(-spiel.torwartDreh * 180f / PI.toFloat(), Offset(sx(spiel.torwartX), sy(spiel.torwartY))) }) {
            val tx = spiel.torwartX
            val ty = spiel.torwartY
            drawOval(Color.Black.copy(alpha = 0.25f), topLeft = Offset(sx(tx - 45f), sy(ty - 66f) - 8f * s), size = Size(90f * s, 16f * s))
            drawRoundRect(Koralle, topLeft = Offset(sx(tx - 29f), sy(ty - 10f + 46f)), size = Size(58f * s, 92f * s), cornerRadius = CornerRadius(24f * s))
            drawRoundRect(
                Color.White.copy(alpha = 0.6f), topLeft = Offset(sx(tx - 29f), sy(ty - 10f + 46f)), size = Size(58f * s, 92f * s),
                cornerRadius = CornerRadius(24f * s), style = Stroke(3f * s),
            )
            drawCircle(Color(0xFFFFD6AD), radius = 24f * s, center = Offset(sx(tx), sy(ty + 56f)))
            drawCircle(Color.Black, radius = 3.5f * s, center = Offset(sx(tx - 8f), sy(ty + 60f)))
            drawCircle(Color.Black, radius = 3.5f * s, center = Offset(sx(tx + 8f), sy(ty + 60f)))
            drawIntoCanvas { c ->
                val nc = c.nativeCanvas
                farbe.typeface = Typeface.DEFAULT_BOLD
                farbe.color = android.graphics.Color.WHITE
                farbe.alpha = 255
                farbe.textAlign = Paint.Align.CENTER
                farbe.textSize = 30f * s
                nc.drawText("1", sx(tx), sy(ty - 8f) - (farbe.descent() + farbe.ascent()) / 2f, farbe)
                farbe.textSize = 36f * s
                nc.drawText("🧤", sx(tx - 48f), sy(ty + 24f) - (farbe.descent() + farbe.ascent()) / 2f, farbe)
                nc.save()
                nc.scale(-1f, 1f, sx(tx + 48f), sy(ty + 24f))
                nc.drawText("🧤", sx(tx + 48f), sy(ty + 24f) - (farbe.descent() + farbe.ascent()) / 2f, farbe)
                nc.restore()
            }
        }

        // Pfosten und Latte
        val pfosten = Color.White
        val pw = 12f * s
        drawLine(pfosten, Offset(sx(ElfmeterRegeln.TOR_LINKS), sy(ElfmeterRegeln.TOR_UNTEN)), Offset(sx(ElfmeterRegeln.TOR_LINKS), sy(ElfmeterRegeln.TOR_OBEN)), pw, StrokeCap.Round)
        drawLine(pfosten, Offset(sx(ElfmeterRegeln.TOR_LINKS), sy(ElfmeterRegeln.TOR_OBEN)), Offset(sx(ElfmeterRegeln.TOR_RECHTS), sy(ElfmeterRegeln.TOR_OBEN)), pw, StrokeCap.Round)
        drawLine(pfosten, Offset(sx(ElfmeterRegeln.TOR_RECHTS), sy(ElfmeterRegeln.TOR_OBEN)), Offset(sx(ElfmeterRegeln.TOR_RECHTS), sy(ElfmeterRegeln.TOR_UNTEN)), pw, StrokeCap.Round)

        // Zielvorschau beim Wischen
        spiel.vorschau?.let { v ->
            drawLine(
                Color.White.copy(alpha = 0.7f), Offset(sx(ElfmeterRegeln.BALL_X), sy(ElfmeterRegeln.BALL_Y)), Offset(sx(v.first), sy(v.second)),
                4f * s, StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(14f * s, 12f * s)),
            )
        }

        // Konfetti
        for (k in konfetti) {
            drawRect(k.farbe.copy(alpha = (1f - k.alter / 1.2f).coerceIn(0f, 1f)), topLeft = Offset(sx(k.x) - 4f * s, sy(k.y) - 4f * s), size = Size(8f * s, 8f * s))
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
            val weiss = android.graphics.Color.WHITE

            // Der Ball
            text("⚽", spiel.ballX, spiel.ballY, 64f * spiel.ballSkala, weiss, Paint.Align.CENTER, alpha = spiel.ballAlpha)

            // Anzeige
            text("⭐ ${spiel.punkte}", 28f, hoehe - 46f, 30f, gelbArgb, Paint.Align.LEFT)
            text("Level ${spiel.level}", 30f, hoehe - 76f, 18f, weiss, Paint.Align.LEFT)
            if (spiel.serie >= 2) text("🔥 ${spiel.serie} in Folge", breite / 2, hoehe - 50f, 22f, koralleArgb, Paint.Align.CENTER)
            for (i in 0 until ElfmeterRegeln.FEHLSCHUESSE_ERLAUBT) {
                val frei = i < ElfmeterRegeln.FEHLSCHUESSE_ERLAUBT - spiel.fehlschuesse
                text("⚽", 46f + i * 34f, hoehe - 106f, 26f, weiss, Paint.Align.CENTER, alpha = if (frei) 1f else 0.2f)
            }

            for (e in effekte) {
                val anteil = e.alter / 1.0f
                text(e.text, e.x, e.y + 36f * anteil, e.groesse, e.farbe, Paint.Align.CENTER, alpha = 1f - anteil)
            }

            when (spiel.phase) {
                ElfmeterSpiel.Phase.WARTEN -> {
                    text("Elfmeterschießen", breite / 2, 228f, 40f, weiss, Paint.Align.CENTER)
                    text("Wische vom Ball zum Tor", breite / 2, 190f, 19f, gelbArgb, Paint.Align.CENTER, fett = false)
                }
                ElfmeterSpiel.Phase.VORBEI -> {
                    text("${spiel.punkte} Punkte", breite / 2, 228f, 40f, weiss, Paint.Align.CENTER)
                    text("${spiel.tore} Tore. Tippen für noch einen Versuch", breite / 2, 190f, 19f, gelbArgb, Paint.Align.CENTER, fett = false)
                }
                else -> Unit
            }
        }

        if (blitz > 0f) drawRect(Color.Red.copy(alpha = (blitz / 0.25f) * 0.25f))
    }
}
