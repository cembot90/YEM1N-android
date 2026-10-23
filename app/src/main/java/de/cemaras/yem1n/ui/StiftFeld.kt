package de.cemaras.yem1n.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Handschrift
import de.cemaras.yem1n.speicher.StiftPunkt
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.TextSanft
import kotlinx.coroutines.delay

/**
 * Karopapier zum Schreiben mit Stift oder Finger. Kurz nach dem letzten Strich liest die Erkennung die Zahl
 * und trägt sie in [setze] ein, das Kind sieht also, was die App gelesen hat.
 */
@Composable
fun StiftFeld(vm: AppViewModel, wert: String, maxZiffern: Int, beschriftung: String, setze: (String) -> Unit, fertig: () -> Unit) {
    val striche = remember { mutableStateListOf<List<Offset>>() }
    val punkte = remember { mutableListOf<MutableList<StiftPunkt>>() }
    var version by remember { mutableIntStateOf(0) }
    var liest by remember { mutableStateOf(false) }
    var nichtLesbar by remember { mutableStateOf(false) }

    LaunchedEffect(version) {
        if (version == 0) return@LaunchedEffect
        delay(600)
        liest = true
        val kopie = punkte.map { it.toList() }
        vm.stiftLeser.lesen(kopie) { vorschlaege ->
            liest = false
            val z = Handschrift.lies(vorschlaege, maxZiffern)
            if (z != null) { setze(z); nichtLesbar = false } else { setze(""); nichtLesbar = true }
        }
    }

    fun leeren() {
        striche.clear(); punkte.clear(); version = 0; liest = false; nichtLesbar = false; setze("")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp))
                .border(2.dp, Gelb.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
        ) {
            Canvas(
                Modifier.matchParentSize().pointerInput(Unit) {
                    awaitEachGesture {
                        val runter = awaitFirstDown(requireUnconsumed = false)
                        val liste = mutableListOf(Offset(runter.position.x, runter.position.y))
                        val p = mutableListOf(StiftPunkt(runter.position.x, runter.position.y, System.currentTimeMillis()))
                        striche.add(liste.toList()); punkte.add(p)
                        val nr = striche.size - 1
                        runter.consume()
                        while (true) {
                            val ereignis = awaitPointerEvent(PointerEventPass.Main)
                            val c = ereignis.changes.firstOrNull() ?: break
                            if (!c.pressed) { c.consume(); break }
                            liste.add(c.position); p.add(StiftPunkt(c.position.x, c.position.y, System.currentTimeMillis()))
                            striche[nr] = liste.toList()
                            c.consume()
                        }
                        version += 1
                    }
                },
            ) {
                drawRect(Color(0xFFFAFAF5))
                val k = 22.dp.toPx()
                var x = k
                while (x < size.width) { drawLine(Color(0x8C9EBFE6), Offset(x, 0f), Offset(x, size.height), 1f); x += k }
                var y = k
                while (y < size.height) { drawLine(Color(0x8C9EBFE6), Offset(0f, y), Offset(size.width, y), 1f); y += k }
                for (s in striche) {
                    if (s.size < 2) { s.firstOrNull()?.let { drawCircle(Color(0xFF0D1A4D), 3.5.dp.toPx(), it) }; continue }
                    val weg = Path().apply { moveTo(s[0].x, s[0].y); for (i in 1 until s.size) lineTo(s[i].x, s[i].y) }
                    drawPath(weg, Color(0xFF0D1A4D), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            if (striche.isEmpty()) {
                Text("✏️ Schreib hier deine Zahl", color = Color(0xFF6B7A99), fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { leeren() }) { Text("🧽 Wegwischen", color = Gelb) }
            Text(
                when {
                    liest -> "Ich lese ..."
                    nichtLesbar -> "Nicht lesbar. Schreib etwas größer."
                    wert.isNotEmpty() -> "$beschriftung: $wert"
                    else -> ""
                },
                color = if (wert.isNotEmpty() && !liest) Color.White else TextSanft,
                fontSize = if (wert.isNotEmpty() && !liest) 26.sp else 14.sp,
                fontWeight = if (wert.isNotEmpty()) FontWeight.Black else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = fertig, enabled = wert.isNotEmpty() && !liest) { Text("✔ Prüfen", color = Gelb) }
        }
    }
}
