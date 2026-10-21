package de.cemaras.yem1n.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.TiefNavy

/** Wurzel der App: Hintergrund, Zurück-Taste und die drei Bildschirme. */
@Composable
fun App(vm: AppViewModel) {
    BackHandler(enabled = vm.ansicht != Ansicht.Start) { vm.zurueck() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, TiefNavy))),
    ) {
        when (vm.ansicht) {
            Ansicht.Start -> StartScreen(vm)
            is Ansicht.Uebungen -> UebungenScreen(vm)
            is Ansicht.Ueben -> UebenScreen(vm)
        }
    }
}

internal fun sterneText(n: Int): String = "★".repeat(n.coerceIn(0, 3)) + "☆".repeat(3 - n.coerceIn(0, 3))

@Composable
internal fun KartenZeile(onClick: () -> Unit, inhalt: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Karte)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { inhalt() }
}

@Composable
fun StartScreen(vm: AppViewModel) {
    var einfuegen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("YEM1N", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text("Was wollen wir heute üben?", color = TextSanft, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(vm.arbeiten) { i, arbeit ->
                ArbeitKarte(vm, arbeit) { vm.oeffne(i) }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { einfuegen = true },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Gelb),
        ) { Text("Arbeit einfügen", color = Gelb) }
        Spacer(Modifier.height(12.dp))
    }
    if (einfuegen) {
        EinfuegenDialog(vm) { einfuegen = false }
    }
}

@Composable
private fun ArbeitKarte(vm: AppViewModel, arbeit: Arbeit, onClick: () -> Unit) {
    val sterne = vm.fortschritt.sterneDerArbeit(arbeit.schluessel, arbeit.uebungen.map { it.titel })
    val anteil = if (arbeit.maxSterne == 0) 0f else sterne.toFloat() / arbeit.maxSterne
    KartenZeile(onClick) {
        Column(Modifier.weight(1f)) {
            Text(arbeit.titel, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${arbeit.klasse} · ${arbeit.fach}", color = TextSanft)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { anteil },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Gelb,
                trackColor = Color.White.copy(alpha = 0.15f),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text("★ $sterne / ${arbeit.maxSterne}", color = Gelb, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EinfuegenDialog(vm: AppViewModel, schliessen: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text("Arbeit einfügen") },
        text = {
            Column {
                Text("Kopiere das JSON aus Claude und füge es hier ein (lange auf das Feld tippen, dann Einfügen).")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; fehler = null },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                )
                fehler?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Koralle)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = {
                    val f = vm.fuegeArbeitEin(text)
                    if (f == null) schliessen() else fehler = f
                },
            ) { Text("Importieren") }
        },
        dismissButton = { TextButton(onClick = schliessen) { Text("Abbrechen") } },
    )
}

@Composable
fun UebungenScreen(vm: AppViewModel) {
    val arbeit = vm.aktuelleArbeit ?: return
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text(arbeit.titel, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("${arbeit.klasse} · ${arbeit.fach}", color = TextSanft)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(arbeit.uebungen) { i, u ->
                // Die Überschrift der Gruppe steht nur über der ersten Übung der Gruppe.
                val neueGruppe = u.gruppe.isNotEmpty() && (i == 0 || arbeit.uebungen[i - 1].gruppe != u.gruppe)
                Column {
                    if (neueGruppe) {
                        Text(
                            u.gruppe,
                            color = Gelb,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = if (i == 0) 0.dp else 10.dp, bottom = 6.dp),
                        )
                    }
                    UebungZeile(vm, arbeit, i)
                }
            }
        }
    }
}

@Composable
private fun UebungZeile(vm: AppViewModel, arbeit: Arbeit, i: Int) {
    val u = arbeit.uebungen[i]
    val stand = vm.fortschritt.stand(arbeit.schluessel, u.titel)
    KartenZeile(onClick = { vm.startUebung(i) }) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Text(u.symbol.ifEmpty { "•" }, fontSize = 18.sp, color = Gelb, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(u.titel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                if (stand.runden > 0) "${u.aufgaben.size} Aufgaben · Beste Runde ${stand.beste} richtig"
                else "${u.aufgaben.size} Aufgaben",
                color = TextSanft,
                fontSize = 14.sp,
            )
        }
        Text(sterneText(stand.sterne), color = Gelb, fontSize = 20.sp)
    }
}
