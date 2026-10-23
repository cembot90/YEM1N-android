package de.cemaras.yem1n.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import de.cemaras.yem1n.logik.EigenThema
import de.cemaras.yem1n.logik.EigenWort
import de.cemaras.yem1n.logik.EigeneThemen
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas

/** Vokabel-Editor: eigene Themen mit Wörtern in Deutsch, Englisch, Türkisch und Italienisch. */
@Composable
fun VokabelEditorScreen(vm: AppViewModel) {
    val thema = vm.vokabelThema
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontWeight = FontWeight.Bold) }
        Text(if (thema == null) "Eigene Wörter" else "Thema bearbeiten", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (thema == null) ThemenListe(vm) else ThemaForm(vm, thema)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemenListe(vm: AppViewModel) {
    var loeschen by remember { mutableStateOf<EigenThema?>(null) }
    EdHilfe("Baue ein eigenes Thema, zum Beispiel für die nächste Vokabelarbeit. Es erscheint danach im Bereich Sprachen bei Karten, Paaren und Tippen.")
    if (vm.eigeneThemen.isEmpty()) EdHilfe("Noch keins da.")
    for (t in vm.eigeneThemen) {
        Row(Modifier.fillMaxWidth().glas(16.dp).clickable { vm.vokabelThema = t }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t.emoji, fontSize = 28.sp)
            Spacer(Modifier.padding(horizontal = 6.dp))
            Column(Modifier.weight(1f)) {
                Text(t.titel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("${t.woerter.count { it.gueltig }} Wörter", color = TextSanft, fontSize = 13.sp)
            }
            EdKlein("🗑") { loeschen = t }
        }
    }
    EdKnopf("＋ Neues Thema", betont = true) {
        vm.vokabelThema = EigenThema(id = "", titel = "", emoji = "📘", klasse = 3, woerter = listOf(EigenWort(), EigenWort(), EigenWort()))
    }
    loeschen?.let { t ->
        AlertDialog(
            onDismissRequest = { loeschen = null }, title = { Text("${t.titel} löschen?") },
            text = { Text("Das Thema und seine Wörter sind danach weg.") },
            confirmButton = { TextButton({ loeschen = null; vm.setzeEigeneThemen(vm.eigeneThemen.filter { it.id != t.id }) }) { Text("Löschen") } },
            dismissButton = { TextButton({ loeschen = null }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun ThemaForm(vm: AppViewModel, start: EigenThema) {
    var t by remember(start.id) { mutableStateOf(start) }
    var fehler by remember { mutableStateOf<List<String>>(emptyList()) }
    EdFeld("Titel", t.titel) { t = t.copy(titel = it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) { EdFeld("Bild (Emoji)", t.emoji) { t = t.copy(emoji = it.take(4)) } }
        Box(Modifier.weight(1f)) {
            EdFeld("Klasse (1 bis 4)", t.klasse.toString(), zahl = true) { s -> s.toIntOrNull()?.let { t = t.copy(klasse = it.coerceIn(1, 4)) } }
        }
    }
    Text("Wörter (${t.woerter.count { it.gueltig }} fertig)", color = Gelb, fontWeight = FontWeight.Bold)
    EdHilfe("Deutsch, Englisch und Türkisch sind Pflicht. Italienisch ist freiwillig.")
    t.woerter.forEachIndexed { i, w ->
        Column(Modifier.fillMaxWidth().glas(16.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Wort ${i + 1}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                EdKlein("🗑") { t = t.copy(woerter = t.woerter.filterIndexed { j, _ -> j != i }) }
            }
            fun neu(x: EigenWort) { t = t.copy(woerter = t.woerter.mapIndexed { j, o -> if (j == i) x else o }) }
            EdFeld("Bild (Emoji, freiwillig)", w.emoji) { neu(w.copy(emoji = it.take(4))) }
            EdFeld("Deutsch", w.de) { neu(w.copy(de = it)) }
            EdFeld("Englisch", w.en) { neu(w.copy(en = it)) }
            EdFeld("Türkisch", w.tr) { neu(w.copy(tr = it)) }
            EdFeld("Italienisch (freiwillig)", w.it) { neu(w.copy(it = it)) }
        }
    }
    EdKnopf("＋ Wort hinzufügen") { t = t.copy(woerter = t.woerter + EigenWort()) }
    for (f in fehler) Text(f, color = Koralle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    EdKnopf("✅ Speichern", betont = true) {
        val sauber = EigeneThemen.bereinigt(t)
        val probleme = EigeneThemen.pruefe(sauber)
        fehler = probleme
        if (probleme.isEmpty()) {
            val mitId = if (sauber.id.isEmpty()) sauber.copy(id = EigeneThemen.neueId(sauber.titel, vm.eigeneThemen)) else sauber
            val rest = vm.eigeneThemen.filter { it.id != mitId.id }
            val neueListe = if (vm.eigeneThemen.any { it.id == mitId.id }) vm.eigeneThemen.map { if (it.id == mitId.id) mitId else it } else rest + mitId
            vm.setzeEigeneThemen(neueListe)
            vm.vokabelThema = null
        }
    }
}
