package de.cemaras.yem1n.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.TextSanft

@Composable
fun EinstellungenScreen(vm: AppViewModel) {
    var name by remember { mutableStateOf(vm.fortschritt.name) }
    var loeschen by remember { mutableStateOf<Arbeit?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("Einstellungen", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        Abschnitt("Name des Kindes")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30); vm.setzeName(name) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Vorname") },
        )
        Text("Damit lobt dich die App mit deinem Namen.", color = TextSanft, fontSize = 14.sp)

        Abschnitt("Farbwelt")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val rosa = vm.fortschritt.rosa
            if (!rosa) {
                Button(onClick = { vm.setzeFarbwelt(false) }) { Text("Blau und Gelb") }
                OutlinedButton(onClick = { vm.setzeFarbwelt(true) }, border = BorderStroke(1.dp, Gelb)) {
                    Text("Rosa", color = Gelb)
                }
            } else {
                OutlinedButton(onClick = { vm.setzeFarbwelt(false) }, border = BorderStroke(1.dp, Gelb)) {
                    Text("Blau und Gelb", color = Gelb)
                }
                Button(onClick = { vm.setzeFarbwelt(true) }) { Text("Rosa") }
            }
        }

        Abschnitt("Tagesziel")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { vm.setzeTagesziel(vm.fortschritt.tagesziel - 1) },
                border = BorderStroke(1.dp, Gelb),
            ) { Text("-", color = Gelb, fontSize = 20.sp) }
            Text(
                "${vm.fortschritt.tagesziel} ${if (vm.fortschritt.tagesziel == 1) "Runde" else "Runden"} am Tag",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            OutlinedButton(
                onClick = { vm.setzeTagesziel(vm.fortschritt.tagesziel + 1) },
                border = BorderStroke(1.dp, Gelb),
            ) { Text("+", color = Gelb, fontSize = 20.sp) }
        }

        Abschnitt("Hilfe")
        OutlinedButton(
            onClick = vm::oeffneInfo,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Gelb),
        ) { Text("Info, Anleitung und Datenschutz", color = Gelb) }

        Abschnitt("Arbeiten")
        if (vm.arbeiten.isEmpty()) {
            Text("Keine Arbeiten vorhanden. Auf der Startseite lassen sich neue einfügen.", color = TextSanft)
        }
        vm.arbeiten.forEach { a ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(a.titel, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${a.klasse} · ${a.fach}", color = TextSanft, fontSize = 14.sp)
                }
                TextButton(onClick = { loeschen = a }) { Text("Löschen", color = Koralle) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    loeschen?.let { a ->
        AlertDialog(
            onDismissRequest = { loeschen = null },
            title = { Text("\"${a.titel}\" löschen?") },
            text = { Text("Die Arbeit verschwindet von diesem Gerät. Du kannst sie später wieder einfügen.") },
            confirmButton = {
                Button(onClick = { vm.loescheArbeit(a); loeschen = null }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { loeschen = null }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun Abschnitt(titel: String) {
    Spacer(Modifier.height(20.dp))
    Text(titel, color = Gelb, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
}
