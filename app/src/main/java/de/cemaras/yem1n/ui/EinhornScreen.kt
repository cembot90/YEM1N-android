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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import de.cemaras.yem1n.logik.EinhornRegeln
import de.cemaras.yem1n.logik.EinhornSpiel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val BREITE = EinhornRegeln.BREITE
private const val HOEHE = EinhornRegeln.HOEHE

/** Wolken, die langsam vorbeiziehen, auch vor dem Start. */
private class WolkenFeld {
    val x = FloatArray(6) { Random.nextFloat() * BREITE }
    val y = FloatArray(6) { 120f + Random.nextFloat() * (HOEHE - 180f) }

    fun schritt(dt: Float) {
        for (i in 0 until 6) {
            x[i] -= (if (i < 3) 30f else 60f) * dt
            if (x[i] < -120f) {
                x[i] = BREITE + 120f
                y[i] = 120f + Random.nextFloat() * (HOEHE - 180f)
            }
        }
    }
}

/** Ein kleiner Text, der aufsteigt und verblasst, zum Beispiel "+3". */
private class Aufsteiger(val text: String, val x: Float, val y: Float, val groesse: Float, var alter: Float = 0f)

private val regenbogen = listOf(
    Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFE600), Color(0xFF34C759), Color(0xFF32D4F5), Color(0xFF9966FF),
)

/** Einhorn-Flug mit Compose-Zeichnung. Die Regeln stecken in [EinhornSpiel]. */
@Composable
fun EinhornScreen(onEnde: (Int) -> Unit) {
    val spiel = remember { EinhornSpiel() }
    val ende by rememberUpdatedState(onEnde)
    val wolken = remember { WolkenFeld() }
    val spur = remember { ArrayList<Float>() }
    val aufsteiger = remember { ArrayList<Aufsteiger>() }
    var bild by remember { mutableStateOf(0) }
    var uhr by remember { mutableStateOf(0f) }

    LaunchedEffect(spiel) {
        spiel.beiEnde = { punkte -> ende(punkte) }
        var letzte = 0L
        var zeit = 0f
        while (true) {
            withFrameNanos { t ->
                val dt = if (letzte == 0L) 0f else min(0.1f, (t - letzte) / 1_000_000_000f)
                letzte = t
                zeit += dt
                spiel.schritt(dt, zeit)
                wolken.schritt(dt)
                spur.add(spiel.y)
                if (spur.size > 28) spur.removeAt(0)
                for (e in spiel.ereignisse) {
                    val text = when (e.ding) {
                        EinhornRegeln.Ding.REGENBOGEN -> "+3"
                        EinhornRegeln.Ding.STERN -> "+1"
                        EinhornRegeln.Ding.HERZ -> "💖"
                        EinhornRegeln.Ding.GEWITTER -> "💥"
                        null -> ""
                    }
                    if (text.isNotEmpty()) aufsteiger.add(Aufsteiger(text, e.x, e.y, 44f))
                    if (e.levelHoch) aufsteiger.add(Aufsteiger("🌟", BREITE / 2, HOEHE / 2, 90f))
                }
                spiel.ereignisse.clear()
                aufsteiger.forEach { it.alter += dt }
                aufsteiger.removeAll { it.alter > 0.9f }
                uhr = zeit
                bild += 1
            }
        }
    }

    val farbe = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(spiel) { detectTapGestures(onPress = { spiel.tippe() }) },
    ) {
        @Suppress("UNUSED_VARIABLE") val abonniert = bild
        val s = min(size.width / BREITE, size.height / HOEHE)
        val ox = (size.width - BREITE * s) / 2f
        val oy = (size.height - HOEHE * s) / 2f
        fun sx(x: Float) = ox + x * s
        fun sy(y: Float) = oy + (HOEHE - y) * s

        // Himmel und Sonne
        drawRect(Brush.verticalGradient(listOf(Color(0xFF80B8FF), Color(0xFFFFD1ED))))
        val puls = 1f + 0.08f * sin(uhr * 2f)
        drawCircle(Color(0xFFFFEB80), radius = 110f * s * puls, center = Offset(sx(BREITE - 130f), sy(HOEHE - 110f)))

        // Wolken
        for (i in 0 until 6) {
            val gross = i < 3
            val f = if (gross) 1.6f else 1f
            val a = if (gross) 0.55f else 0.85f
            val teile = listOf(
                Triple(-36f, 0f, 28f), Triple(0f, 14f, 38f), Triple(38f, 2f, 30f), Triple(10f, -10f, 30f),
            )
            for ((dx, dy, r) in teile) {
                drawCircle(Color.White.copy(alpha = a), radius = r * f * s, center = Offset(sx(wolken.x[i] + dx * f), sy(wolken.y[i] + dy * f)))
            }
        }

        // Regenbogenspur hinter dem Einhorn
        for (i in spur.indices) {
            val anteil = (i + 1).toFloat() / spur.size
            drawCircle(
                regenbogen[i % regenbogen.size].copy(alpha = anteil * 0.8f),
                radius = 13f * s * anteil,
                center = Offset(sx(EinhornRegeln.EINHORN_X - 40f - (spur.size - 1 - i) * 7f), sy(spur[i] - 6f)),
            )
        }

        drawIntoCanvas { c ->
            val nc = c.nativeCanvas
            fun emoji(text: String, x: Float, y: Float, groesse: Float, alpha: Float = 1f) {
                farbe.typeface = Typeface.DEFAULT
                farbe.color = android.graphics.Color.BLACK
                farbe.textSize = groesse * s
                farbe.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
                nc.drawText(text, x, y - (farbe.descent() + farbe.ascent()) / 2f, farbe)
            }

            // Dinge, die von rechts kommen
            for (o in spiel.objekte) {
                val gewitter = o.ding == EinhornRegeln.Ding.GEWITTER
                val wippen = if (gewitter) sin(uhr * 5f) * 12f else 0f
                val groesse = (if (gewitter) 70f else 56f) * (if (gewitter) 1f else 1f + 0.07f * sin(uhr * 8f))
                emoji(o.ding.bild, sx(o.x), sy(o.y + wippen), groesse)
            }

            // Einhorn, nach rechts schauend und je nach Tempo leicht gekippt
            val kippen = max(-0.35f, min(0.35f, spiel.vy / 900f))
            val blinkt = spiel.schutz > 0f && (spiel.schutz * 10).toInt() % 2 == 0
            nc.save()
            nc.translate(sx(EinhornRegeln.EINHORN_X), sy(spiel.y))
            nc.rotate(-kippen * 57.29578f)
            nc.scale(-1f, 1f)
            emoji("🦄", 0f, 0f, 84f, if (blinkt) 0.4f else 1f)
            nc.restore()

            // Leben
            for (i in 0 until EinhornRegeln.MAX_LEBEN) {
                emoji("💖", sx(40f + i * 44f), sy(HOEHE - 40f), 34f, if (i < spiel.leben) 1f else 0.2f)
            }

            // Punkte
            farbe.typeface = Typeface.DEFAULT_BOLD
            farbe.color = android.graphics.Color.rgb(115, 51, 140)
            farbe.alpha = 255
            farbe.textSize = 40f * s
            nc.drawText("⭐ ${spiel.punkte}", sx(BREITE / 2), sy(HOEHE - 40f) - (farbe.descent() + farbe.ascent()) / 2f, farbe)

            // Aufsteigende Texte
            for (a in aufsteiger) {
                val t = a.alter / 0.9f
                farbe.typeface = Typeface.DEFAULT_BOLD
                farbe.color = android.graphics.Color.rgb(115, 51, 140)
                farbe.textSize = a.groesse * s
                farbe.alpha = ((1f - t).coerceIn(0f, 1f) * 255).toInt()
                nc.drawText(a.text, sx(min(a.x, BREITE - 40f)), sy(a.y + 60f * t) - (farbe.descent() + farbe.ascent()) / 2f, farbe)
            }

            // Hinweis vor dem Start und nach dem Spiel
            if (spiel.phase != EinhornSpiel.Phase.LAEUFT) {
                if (spiel.phase == EinhornSpiel.Phase.VORBEI) {
                    farbe.typeface = Typeface.DEFAULT_BOLD
                    farbe.color = android.graphics.Color.rgb(115, 51, 140)
                    farbe.alpha = 255
                    farbe.textSize = 72f * s
                    nc.drawText("⭐ ${spiel.punkte}", sx(BREITE / 2), sy(HOEHE / 2 + 60f) - (farbe.descent() + farbe.ascent()) / 2f, farbe)
                }
                emoji("👆", sx(BREITE / 2), sy(HOEHE / 2 - 50f + sin(uhr * 9f) * 9f), 80f)
            }
        }
    }
}
