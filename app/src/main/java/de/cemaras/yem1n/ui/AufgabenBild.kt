package de.cemaras.yem1n.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Malfarbe
import de.cemaras.yem1n.logik.Noten
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Navy
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// Kleine Bilder über der Aufgabe. Der Code steht in Aufgabe.bild als "art:wert":
//   wort:🍎|Apfel     großes Bild, der Lautsprecher sagt das Wort
//   emoji:🥁          großes Bild
//   englisch:dog      Knopf, der das englische Wort vorliest
//   note:g1           Notenzeile mit einer Note, dazu der echte Ton
//   notenwert:halbe   eine einzelne Note (ganze, halbe, viertel, achtel)
//   form:dreieck      eine Form (kreis, dreieck, quadrat, rechteck, oval, stern)
//   mischen:blau+gelb zwei Farbkleckse, die gemischt werden
//   farbe:orange      ein Farbfeld
//   strichliste:7     Striche in Fünferbündeln
//   rechteck:5x3      Rechteck aus Kästchen, 5 breit und 3 hoch
//   uhr:8:30          Uhr mit Zeigern
//   toene:c1|g1       zwei Töne zum Anhören und Vergleichen
// Ohne Doppelpunkt (Vorschule) ist es einfach ein großes Bild aus Zeichen.

/** Zerlegt "art:wert" am ersten Doppelpunkt. */
fun bildTeile(code: String): Pair<String, String> {
    val i = code.indexOf(':')
    return if (i < 0) code to "" else code.substring(0, i) to code.substring(i + 1)
}

@Composable
fun AufgabenBild(code: String, vm: AppViewModel) {
    val (art, wert) = bildTeile(code)
    when (art) {
        "wort" -> {
            val stuecke = wert.split("|")
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stuecke.first(), fontSize = 76.sp, textAlign = TextAlign.Center)
                if (stuecke.size > 1) {
                    GelbKnopf("🔊 Wort anhören") { vm.sprecher.sprich(stuecke[1]) }
                }
            }
        }
        "emoji" -> Text(wert, fontSize = 76.sp, textAlign = TextAlign.Center)
        "englisch" -> GelbKnopf("🔊 Auf Englisch anhören") { vm.sprecher.sprich(wert, sprache = "en") }
        "note" -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NotenZeile(wert, Modifier.size(width = 220.dp, height = 120.dp))
            GelbKnopf("🎵 Ton anhören") { TonGeber.spiele(wert) }
        }
        "notenwert" -> NotenwertBild(wert, Modifier.size(width = 90.dp, height = 110.dp))
        "form" -> FormBild(wert, Modifier.size(width = 130.dp, height = 110.dp))
        "mischen" -> {
            val farben = wert.split("+")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Klecks(farben.getOrElse(0) { "" })
                Text("+", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
                Klecks(farben.getOrElse(1) { "" })
                Text("=", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(
                    "?", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center,
                    modifier = Modifier.size(58.dp).background(Color.White.copy(alpha = 0.85f), CircleShape)
                        .padding(top = 6.dp),
                )
            }
        }
        "farbe" -> Klecks(wert, Modifier.padding(10.dp))
        "strichliste" -> StrichlisteBild(wert.toIntOrNull() ?: 0, Modifier.fillMaxWidth().height(70.dp))
        "rechteck" -> {
            val masse = wert.split("x").mapNotNull { it.toIntOrNull() }
            KaestchenRechteck(masse.getOrElse(0) { 1 }, masse.getOrElse(1) { 1 })
        }
        "uhr" -> {
            val zeit = wert.split(":").mapNotNull { it.toIntOrNull() }
            UhrBild(zeit.getOrElse(0) { 12 }, zeit.getOrElse(1) { 0 }, Modifier.size(150.dp))
        }
        "toene" -> {
            val zwei = wert.split("|")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GelbKnopf("▶ Ton 1") { TonGeber.spiele(zwei.getOrElse(0) { "" }) }
                GelbKnopf("▶ Ton 2") { TonGeber.spiele(zwei.getOrElse(1) { "" }) }
            }
        }
        // Vorschule: ein einfaches Zeichen
        else -> if (wert.isEmpty()) Text(code, fontSize = 44.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GelbKnopf(text: String, klick: () -> Unit) {
    Button(
        onClick = klick,
        colors = ButtonDefaults.buttonColors(containerColor = Gelb, contentColor = Navy),
    ) { Text(text, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun Klecks(name: String, modifier: Modifier = Modifier) {
    val farbe = Color(Malfarbe.argb(name))
    androidx.compose.foundation.layout.Box(
        modifier
            .size(58.dp)
            .background(farbe, CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            .semantics { contentDescription = Malfarbe.anzeige(name) },
    )
}

@Composable
fun NotenZeile(note: String, modifier: Modifier = Modifier) {
    val gelb = Gelb
    Canvas(modifier.semantics { contentDescription = "Eine Note auf der Notenzeile" }) {
        val abstand = 13.dp.toPx()
        val unten = size.height / 2 + 2 * abstand
        val links = 10.dp.toPx()
        val rechts = size.width - 10.dp.toPx()
        val weiss = Color.White.copy(alpha = 0.85f)
        for (i in 0 until 5) {
            val y = unten - i * abstand
            drawLine(weiss, Offset(links, y), Offset(rechts, y), strokeWidth = 1.6.dp.toPx())
        }
        val s = Noten.stufe[note] ?: 0
        val x = size.width * 0.62f
        val y = unten - s * abstand / 2
        if (s <= -2) {
            drawLine(weiss, Offset(x - 16.dp.toPx(), unten + abstand), Offset(x + 16.dp.toPx(), unten + abstand), strokeWidth = 1.6.dp.toPx())
        }
        drawOval(gelb, Offset(x - 9.dp.toPx(), y - 6.5.dp.toPx()), Size(18.dp.toPx(), 13.dp.toPx()))
        if (s < 4) {
            drawLine(gelb, Offset(x + 8.dp.toPx(), y), Offset(x + 8.dp.toPx(), y - 40.dp.toPx()), strokeWidth = 2.4.dp.toPx())
        } else {
            drawLine(gelb, Offset(x - 8.dp.toPx(), y), Offset(x - 8.dp.toPx(), y + 40.dp.toPx()), strokeWidth = 2.4.dp.toPx())
        }
    }
}

@Composable
fun NotenwertBild(wert: String, modifier: Modifier = Modifier) {
    val gelb = Gelb
    Canvas(modifier.semantics { contentDescription = "Eine Note" }) {
        val x = size.width / 2 - 6.dp.toPx()
        val y = size.height - 22.dp.toPx()
        val ovalOben = Offset(x - 12.dp.toPx(), y - 9.dp.toPx())
        val ovalGroesse = Size(24.dp.toPx(), 18.dp.toPx())
        val voll = wert == "viertel" || wert == "achtel"
        if (voll) drawOval(gelb, ovalOben, ovalGroesse) else drawOval(gelb, ovalOben, ovalGroesse, style = Stroke(3.5.dp.toPx()))
        if (wert == "ganze") return@Canvas
        drawLine(gelb, Offset(x + 11.dp.toPx(), y - 2.dp.toPx()), Offset(x + 11.dp.toPx(), y - 68.dp.toPx()), strokeWidth = 3.dp.toPx())
        if (wert == "achtel") {
            val fahne = Path().apply {
                moveTo(x + 11.dp.toPx(), y - 68.dp.toPx())
                cubicTo(
                    x + 16.dp.toPx(), y - 52.dp.toPx(),
                    x + 32.dp.toPx(), y - 46.dp.toPx(),
                    x + 26.dp.toPx(), y - 30.dp.toPx(),
                )
            }
            drawPath(fahne, gelb, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun FormBild(form: String, modifier: Modifier = Modifier) {
    val gelb = Gelb
    Canvas(modifier.semantics { contentDescription = "Eine Form" }) {
        val mitte = Offset(size.width / 2, size.height / 2)
        fun mittig(b: Float, h: Float) = Offset(mitte.x - b / 2, mitte.y - h / 2) to Size(b, h)
        when (form) {
            "kreis" -> drawCircle(gelb, 50.dp.toPx(), mitte)
            "quadrat" -> mittig(96.dp.toPx(), 96.dp.toPx()).let { drawRect(gelb, it.first, it.second) }
            "rechteck" -> mittig(126.dp.toPx(), 76.dp.toPx()).let { drawRect(gelb, it.first, it.second) }
            "oval" -> mittig(126.dp.toPx(), 80.dp.toPx()).let { drawOval(gelb, it.first, it.second) }
            "dreieck" -> {
                val b = 110.dp.toPx()
                val h = 96.dp.toPx()
                val p = Path().apply {
                    moveTo(mitte.x, mitte.y - h / 2)
                    lineTo(mitte.x + b / 2, mitte.y + h / 2)
                    lineTo(mitte.x - b / 2, mitte.y + h / 2)
                    close()
                }
                drawPath(p, gelb, style = Fill)
            }
            "stern" -> {
                val aussen = 53.dp.toPx()
                val innen = aussen * 0.42f
                val p = Path()
                for (i in 0 until 10) {
                    val w = i * Math.PI / 5 - Math.PI / 2
                    val r = if (i % 2 == 0) aussen else innen
                    val pt = Offset(mitte.x + (cos(w) * r).toFloat(), mitte.y + (sin(w) * r).toFloat())
                    if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
                }
                p.close()
                drawPath(p, gelb, style = Fill)
            }
        }
    }
}

/** Striche wie an der Tafel: vier senkrecht, der fünfte quer darüber */
@Composable
fun StrichlisteBild(anzahl: Int, modifier: Modifier = Modifier) {
    val gelb = Gelb
    Canvas(modifier.semantics { contentDescription = "Eine Strichliste" }) {
        val hoehe = 46.dp.toPx()
        val oben = (size.height - hoehe) / 2
        val abstand = 10.dp.toPx()
        val buendelBreite = 4 * abstand + 18.dp.toPx()
        val buendel = (anzahl + 4) / 5
        val gesamt = buendel * buendelBreite
        var x = max(8.dp.toPx(), (size.width - gesamt) / 2)
        var rest = anzahl
        val breite = 3.5.dp.toPx()
        while (rest > 0) {
            val hier = min(rest, 5)
            val senkrecht = min(hier, 4)
            for (i in 0 until senkrecht) {
                val px = x + i * abstand
                drawLine(gelb, Offset(px, oben), Offset(px, oben + hoehe), strokeWidth = breite, cap = StrokeCap.Round)
            }
            if (hier == 5) {
                drawLine(
                    gelb, Offset(x - 6.dp.toPx(), oben + hoehe - 6.dp.toPx()),
                    Offset(x + 3 * abstand + 6.dp.toPx(), oben + 6.dp.toPx()), strokeWidth = breite, cap = StrokeCap.Round,
                )
            }
            x += buendelBreite
            rest -= hier
        }
    }
}

/** Ein Rechteck aus gleich großen Kästchen */
@Composable
fun KaestchenRechteck(breite: Int, hoehe: Int) {
    val seite = if (breite > 8 || hoehe > 6) 18.dp else 24.dp
    Column(
        Modifier
            .border(2.dp, Color.White, RoundedCornerShape(5.dp))
            .padding(4.dp)
            .semantics { contentDescription = "Ein Rechteck aus $breite mal $hoehe Kästchen" },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(max(hoehe, 1)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(max(breite, 1)) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(seite).background(Gelb.copy(alpha = 0.85f), RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

/** Eine Uhr mit Stunden- und Minutenzeiger */
@Composable
fun UhrBild(stunde: Int, minute: Int, modifier: Modifier = Modifier) {
    val gelb = Gelb
    Canvas(modifier.semantics { contentDescription = "Eine Uhr" }) {
        val r = min(size.width, size.height) / 2 - 4.dp.toPx()
        val m = Offset(size.width / 2, size.height / 2)
        drawCircle(Color.White, r, m)
        drawCircle(gelb, r, m, style = Stroke(4.dp.toPx()))
        val farbe = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            textSize = 12.sp.toPx()
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        for (i in 0 until 12) {
            val w = i * Math.PI / 6
            val s = sin(w).toFloat()
            val c = cos(w).toFloat()
            drawLine(
                Color.Black.copy(alpha = 0.75f),
                Offset(m.x + s * (r - 4.dp.toPx()), m.y - c * (r - 4.dp.toPx())),
                Offset(m.x + s * (r - 12.dp.toPx()), m.y - c * (r - 12.dp.toPx())),
                strokeWidth = (if (i % 3 == 0) 3.dp else 1.5.dp).toPx(),
            )
            val zahl = if (i == 0) 12 else i
            drawContext.canvas.nativeCanvas.drawText(
                zahl.toString(), m.x + s * (r - 24.dp.toPx()), m.y - c * (r - 24.dp.toPx()) + 4.dp.toPx(), farbe,
            )
        }
        val minutenWinkel = minute / 60.0 * 2 * Math.PI
        val stundenWinkel = ((stunde % 12) + minute / 60.0) / 12 * 2 * Math.PI
        drawLine(
            Color(0xFF1A4DCC), m,
            Offset(m.x + (sin(minutenWinkel) * (r - 16.dp.toPx())).toFloat(), m.y - (cos(minutenWinkel) * (r - 16.dp.toPx())).toFloat()),
            strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round,
        )
        drawLine(
            Color(0xFFD92626), m,
            Offset(m.x + (sin(stundenWinkel) * (r * 0.5f)).toFloat(), m.y - (cos(stundenWinkel) * (r * 0.5f)).toFloat()),
            strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round,
        )
        drawCircle(Color.Black, 5.dp.toPx(), m)
    }
}
