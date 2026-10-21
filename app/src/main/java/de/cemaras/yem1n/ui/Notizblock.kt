package de.cemaras.yem1n.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Ein Blatt zum Rechnen mit dem Finger. Die Striche bleiben für diese Aufgabe erhalten, bis es weitergeht. */
@Composable
fun Notizblock(striche: SnapshotStateList<List<Offset>>, onSchliessen: () -> Unit) {
    Dialog(onDismissRequest = onSchliessen, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFFFFBEA)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { p -> striche.add(listOf(p)) },
                            onDrag = { change, _ ->
                                change.consume()
                                if (striche.isNotEmpty()) striche[striche.lastIndex] = striche.last() + change.position
                            },
                        )
                    },
            ) {
                // Karolinien
                val abstand = 36.dp.toPx()
                var x = abstand
                while (x < size.width) {
                    drawLine(Color(0x2200205B), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += abstand
                }
                var y = abstand
                while (y < size.height) {
                    drawLine(Color(0x2200205B), Offset(0f, y), Offset(size.width, y), 1f)
                    y += abstand
                }
                for (strich in striche) {
                    if (strich.size == 1) {
                        drawCircle(Color(0xFF00205B), 3.dp.toPx(), strich[0])
                    } else {
                        val pfad = Path().apply {
                            moveTo(strich[0].x, strich[0].y)
                            for (i in 1 until strich.size) lineTo(strich[i].x, strich[i].y)
                        }
                        drawPath(pfad, Color(0xFF00205B), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { striche.clear() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Löschen") }
                Button(onClick = onSchliessen, modifier = Modifier.weight(1f).height(52.dp)) { Text("Fertig") }
            }
        }
    }
}
