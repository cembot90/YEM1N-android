package de.cemaras.yem1n.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Profil
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.auswahl
import de.cemaras.yem1n.ui.theme.glas

/** Erster Start: wer benutzt das Gerät, und beim Kind Name, Klasse, Sprachen, Farben und Ziel. Alles lässt sich später ändern. */
@Composable
fun EinrichtungScreen(vm: AppViewModel) {
    var schritt by rememberSaveable { mutableIntStateOf(0) }
    var rolle by rememberSaveable { mutableStateOf("kind") }
    var name by rememberSaveable { mutableStateOf(vm.fortschritt.name) }
    var klasse by rememberSaveable { mutableStateOf("") }
    var sprachen by rememberSaveable { mutableStateOf(vm.fortschritt.sprachenAn) }
    var rosa by rememberSaveable { mutableStateOf(vm.fortschritt.rosa) }
    var ziel by rememberSaveable { mutableIntStateOf(vm.fortschritt.tagesziel) }
    var auto by rememberSaveable { mutableStateOf(vm.fortschritt.autoVorlesen) }
    val letzter = if (rolle == "kind") 4 else 1

    androidx.activity.compose.BackHandler(enabled = schritt > 0) { schritt -= 1 }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Willkommen bei YEM1N", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        if (schritt > 0) Text("Schritt $schritt von $letzter", color = TextSanft, fontSize = 14.sp)

        Column(Modifier.fillMaxWidth().glas(26.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                schritt == 0 -> {
                    Frage("Wer benutzt dieses Gerät?", "Das Kind übt, die Eltern sehen den Fortschritt. Du kannst es später in den Einstellungen ändern.")
                    Wahl("🎓 Kind", "Ich übe für die Schule.", rolle == "kind") { rolle = "kind" }
                    Wahl("🔔 Eltern", "Ich sehe den Fortschritt.", rolle == "eltern") { rolle = "eltern" }
                }
                rolle == "eltern" -> {
                    Frage("So geht es weiter", "Öffne auf dem Startbildschirm Mehr > Joker. Dort gibst du die Einwilligung und legst den Familiencode an oder gibst den Code vom anderen Elternteil ein. Dann trägst du den Code auf dem Gerät deines Kindes ein.")
                }
                schritt == 1 -> {
                    Frage("Wie heißt das Kind?", "Ein Vorname oder Spitzname reicht, bitte keinen Nachnamen. Der Name erscheint bei den Eltern.")
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(30) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Name") },
                    )
                }
                schritt == 2 -> {
                    Frage("In welcher Klasse?", "Danach zeigt die Lernwelt die passenden Themen. Du kannst es später ändern.")
                    Profil.klassen.forEach { k -> Wahl(k, "", klasse == k) { klasse = k } }
                    Wahl("Weiß ich nicht", "", klasse.isEmpty()) { klasse = "" }
                }
                schritt == 3 -> {
                    Frage("Auch Sprachen üben?", "Wörter in anderen Sprachen mit Vorlesen. Du kannst es später unter Einstellungen > Bereiche ändern.")
                    Wahl("Ja, Sprachen anzeigen", "", sprachen) { sprachen = true }
                    Wahl("Nein, erstmal nicht", "", !sprachen) { sprachen = false }
                }
                else -> {
                    Frage("Farben, Ziel und Vorlesen", "Alles lässt sich später in den Einstellungen ändern.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Wahl("Blau und Gelb", "", !rosa, Modifier.weight(1f)) { rosa = false }
                        Wahl("Rosa", "", rosa, Modifier.weight(1f)) { rosa = true }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tagesziel: $ziel ${if (ziel == 1) "Runde" else "Runden"}", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { ziel = (ziel - 1).coerceAtLeast(1) }) { Text("-", color = Gelb, fontSize = 22.sp) }
                        TextButton(onClick = { ziel = (ziel + 1).coerceAtMost(5) }) { Text("+", color = Gelb, fontSize = 22.sp) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aufgaben automatisch vorlesen", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                        Switch(checked = auto, onCheckedChange = { auto = it })
                    }
                    Text("Sonst liest die App nur vor, wenn das Kind auf Vorlesen tippt.", color = TextSanft, fontSize = 13.sp)
                }
            }
        }

        Button(
            onClick = {
                if (schritt < letzter) schritt += 1
                else vm.einrichtungAbschliessen(rolle, name, klasse, sprachen, rosa, ziel, auto)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(if (schritt < letzter) "Weiter" else "Los geht's", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        if (schritt > 0) TextButton(onClick = { schritt -= 1 }) { Text("Zurück", color = Gelb) }
        else TextButton(onClick = vm::einrichtungUeberspringen) { Text("Später einrichten", color = Gelb) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Frage(titel: String, text: String) {
    Text(titel, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
    Text(text, fontSize = 14.sp, color = TextSanft)
}

@Composable
private fun Wahl(titel: String, text: String, gewaehlt: Boolean, modifier: Modifier = Modifier, klick: () -> Unit) {
    Column(
        modifier.fillMaxWidth().auswahl(gewaehlt, 18.dp).clickable(onClick = klick).heightIn(min = 52.dp).padding(14.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(titel, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (text.isNotEmpty()) Text(text, fontSize = 13.sp, color = TextSanft)
    }
}
