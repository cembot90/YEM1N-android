package de.cemaras.yem1n.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.BildArt
import de.cemaras.yem1n.logik.ErklaerBild
import de.cemaras.yem1n.logik.ErklaerBildPlan
import de.cemaras.yem1n.logik.Eule
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.TextSanft
import kotlinx.coroutines.delay
import kotlin.math.abs

private val Keks = Color(0xFFC78545)

/** Der Knopf "Zeig es mir als Bild" und danach das Bild von Olli. */
@Composable
fun ErklaerBildKnopf(bild: ErklaerBild, ruhig: Boolean, vorlesen: (String) -> Unit) {
    var offen by remember(bild) { mutableStateOf(false) }
    if (!offen) {
        Text(
            "🦉 Zeig es mir als Bild", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00205B),
            modifier = Modifier
                .clip(RoundedCornerShape(50)).background(Gelb)
                .clickable { offen = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    } else {
        ErklaerBildView(bild, ruhig, vorlesen)
    }
}

private fun sprechText(b: ErklaerBild): String = b.saetze.joinToString(" ")
    .replace(" · ", " mal ").replace(" : ", " geteilt durch ").replace(" - ", " minus ")
    .replace(" + ", " plus ").replace(" = ", " gleich ").replace("<", "kleiner").replace(">", "größer")

@Composable
private fun ErklaerBildView(bild: ErklaerBild, ruhig: Boolean, vorlesen: (String) -> Unit) {
    val fertig = ErklaerBildPlan.schrittZahl(bild.art)
    var schritt by remember(bild) { mutableStateOf(if (ruhig) fertig else 0) }
    var nochmal by remember(bild) { mutableStateOf(0) }
    LaunchedEffect(bild, nochmal) {
        if (ruhig) { schritt = fertig; return@LaunchedEffect }
        schritt = 0
        val pause = if (fertig > 12) 160L else if (fertig > 6) 300L else 650L
        for (i in 1..maxOf(fertig, 1)) {
            delay(pause)
            schritt = i
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f)).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val a = bild.art) {
                is BildArt.Punkte -> PunkteBild(a.reihen, a.spalten, null, schritt)
                is BildArt.Nachbar -> PunkteBild(a.reihen, a.spalten, a.kern, schritt)
                is BildArt.Teller -> TellerBild(a, schritt)
                is BildArt.Spruenge -> Zahlenstrahl(a.start, a.schritte, schritt)
                is BildArt.Rechenweg -> RechenwegBild(a.zeilen, schritt)
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Text("🦉", fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (satz in bild.saetze) {
                    Text(satz, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 2.dp)) {
                    Text("🔊 Vorlesen", color = Gelb, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { vorlesen(Eule.sprechbar(sprechText(bild))) })
                    Text("↺ Nochmal", color = Gelb, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { nochmal++ })
                }
            }
        }
    }
}

@Composable
private fun Punkt(farbe: Color, g: Int) {
    Box(Modifier.size(g.dp).clip(CircleShape).background(farbe))
}

@Composable
private fun PunkteBild(reihen: Int, spalten: Int, kern: Int?, schritt: Int) {
    val alle = maxOf(reihen, kern ?: 0)
    val g = if (alle > 6 || spalten > 8) 11 else 15
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until alle) {
            var farbe = Gelb
            var sichtbar: Boolean
            var weg = false
            if (kern == null) {
                sichtbar = i < schritt
            } else if (i < kern) {
                weg = reihen < kern && i == kern - 1 && schritt >= 2
                farbe = if (weg) Koralle else Gelb
                sichtbar = schritt >= 1
            } else {
                farbe = Mint
                sichtbar = schritt >= 2
            }
            Row(
                Modifier.alpha(if (sichtbar) (if (weg) 0.45f else 1f) else 0f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(spalten) { Punkt(farbe, g) }
                Text(if (weg) "weg" else ((i + 1) * spalten).toString(), color = farbe, fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TellerBild(a: BildArt.Teller, schritt: Int) {
    val gelegt = minOf(schritt, a.jeder)
    val restDa = a.rest > 0 && schritt > a.jeder
    val keks = if (a.jeder > 12) 7 else 10
    val spalten = if (a.jeder > 16) 5 else if (a.jeder > 9) 4 else 3
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(a.teller) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier.width(((keks + 2) * spalten).dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        var rest = gelegt
                        while (rest > 0) {
                            val inZeile = minOf(rest, spalten)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(inZeile) { Punkt(Keks, keks) } }
                            rest -= inZeile
                        }
                    }
                }
            }
        }
        if (a.rest > 0) {
            Row(
                Modifier.alpha(if (restDa) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Rest:", color = Koralle, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                repeat(a.rest) { Punkt(Keks, 13) }
            }
        }
    }
}

@Composable
private fun RechenwegBild(zeilen: List<String>, schritt: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        zeilen.forEachIndexed { i, zeile ->
            val letzte = i == zeilen.size - 1
            Column(Modifier.alpha(if (i <= schritt) 1f else 0f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (i > 0) Text("↓", color = TextSanft, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(zeile, fontSize = if (letzte) 30.sp else 24.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (letzte) Gelb else Color.White, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Zahlenstrahl(start: Int, schritte: List<Int>, sichtbar: Int) {
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val n = maxOf(schritte.size, 1)
        val rand = 26.dp.toPx()
        val breite = size.width - 2 * rand
        val linieY = size.height - 34.dp.toPx()
        val rechts = schritte.sum() >= 0
        fun x(i: Int): Float {
            val anteil = i.toFloat() / n
            return if (rechts) rand + anteil * breite else size.width - rand - anteil * breite
        }
        drawLine(Color.White.copy(alpha = 0.7f), Offset(8.dp.toPx(), linieY), Offset(size.width - 8.dp.toPx(), linieY), 2.5.dp.toPx())
        var wert = start
        beschrifte(start.toString(), x(0), linieY + 22.dp.toPx(), Color.White, 15f)
        strich(x(0), linieY)
        for ((i, s) in schritte.withIndex()) {
            if (i >= sichtbar) break
            val von = x(i); val bis = x(i + 1)
            wert += s
            val farbe = if (s > 0) Mint else Koralle
            val bogen = Path().apply {
                moveTo(von, linieY)
                quadraticBezierTo((von + bis) / 2, linieY - 62.dp.toPx(), bis, linieY)
            }
            drawPath(bogen, farbe, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            val richtung = if (bis > von) -1f else 1f
            val spitze = Path().apply {
                moveTo(bis + richtung * 9.dp.toPx(), linieY - 10.dp.toPx())
                lineTo(bis, linieY)
                lineTo(bis + richtung, linieY - 13.dp.toPx())
            }
            drawPath(spitze, farbe, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            beschrifte(if (s > 0) "+$s" else "-${abs(s)}", (von + bis) / 2, linieY - 40.dp.toPx(), farbe, 14f)
            strich(bis, linieY)
            val letzter = i == schritte.size - 1
            beschrifte(wert.toString(), bis, linieY + 22.dp.toPx(), if (letzter) Gelb else Color.White, if (letzter) 17f else 15f)
        }
    }
}

private fun DrawScope.strich(x: Float, y: Float) {
    drawLine(Color.White, Offset(x, y - 7.dp.toPx()), Offset(x, y + 7.dp.toPx()), 2.dp.toPx())
}

private fun DrawScope.beschrifte(text: String, x: Float, y: Float, farbe: Color, groesseSp: Float) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = farbe.toArgb()
        textSize = groesseSp * density
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
