package de.cemaras.yem1n.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Familiencode
import de.cemaras.yem1n.logik.JokerFrage
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas

private val EMOJIS = listOf("🙂", "👍", "💡", "🎉", "💪")

@Composable
private fun chipFarbenJoker() = FilterChipDefaults.filterChipColors(
    labelColor = Color.White,
    selectedContainerColor = Gelb,
    selectedLabelColor = Navy,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JokerScreen(vm: AppViewModel) {
    val f = vm.fortschritt
    val code = vm.jokerCode
    val eltern = f.jokerRolle == "eltern"

    // Ab Android 13 einmal um die Erlaubnis für Mitteilungen bitten, sobald es einen Familiencode gibt.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val erlaubnis = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }
    androidx.compose.runtime.LaunchedEffect(code) {
        if (code.isNotEmpty() && !de.cemaras.yem1n.speicher.Erinnerung.erlaubt(ctx)) {
            erlaubnis.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("🆘 Joker", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        if (vm.einwilligungOffen) {
            EinwilligungKarte(vm)
            return@Column
        }
        if (code.isEmpty()) {
            CodeEingabe(vm)
            return@Column
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Familiencode", color = TextSanft, fontSize = 13.sp)
            Text(Familiencode.zeige(code), fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Mit diesem Code finden sich Kind und Eltern.", color = TextSanft, fontSize = 13.sp)
            var qrOffen by remember { mutableStateOf(false) }
            TextButton(onClick = { qrOffen = true }) { Text("📷 QR-Code zeigen", color = Gelb, fontWeight = FontWeight.Bold) }
            if (qrOffen) {
                QrDialog("QR-Code der Familie", de.cemaras.yem1n.logik.QrPaket.inhalt("familie", code),
                    "Auf dem anderen Gerät bei Joker auf QR-Code scannen tippen.") { qrOffen = false }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !eltern, onClick = { vm.setzeJokerRolle("kind") }, label = { Text("Ich bin das Kind") }, colors = chipFarbenJoker())
                FilterChip(selected = eltern, onClick = { vm.setzeJokerRolle("eltern") }, label = { Text("Ich bin Mama oder Papa") }, colors = chipFarbenJoker())
            }
        }

        if (!eltern) FrageFormular(vm)
        if (eltern) NachschubKarte(vm)
        if (eltern) {
            Button(onClick = vm::oeffneErgebnisse, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("📊 Ergebnisse der Kinder", fontWeight = FontWeight.Bold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::jokerLaden, enabled = !vm.jokerBeschaeftigt) {
                Text(if (vm.jokerBeschaeftigt) "Einen Moment..." else "Neu laden", color = Color.White)
            }
        }
        if (vm.jokerMeldung.isNotEmpty()) Text(vm.jokerMeldung, color = TextSanft, fontSize = 14.sp)
        if (!eltern && vm.ergebnisStatus.isNotEmpty()) Text(vm.ergebnisStatus, color = TextSanft, fontSize = 13.sp)
        if (eltern) Text("Auf einem Eltern-Gerät werden Übungsrunden nicht an die Familie gesendet.", color = TextSanft, fontSize = 13.sp)

        vm.jokerFragen.forEach { frage -> FrageKarte(vm, frage, eltern) }

        TextButton(onClick = vm::vergissFamiliencode) { Text("Anderen Familiencode benutzen", color = TextSanft) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CodeEingabe(vm: AppViewModel) {
    var eingabe by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf(false) }
    Text(
        "Hier fragst du deine Eltern um Hilfe. Dafür braucht ihr einen gemeinsamen Familiencode.",
        color = TextSanft, fontSize = 16.sp,
    )
    Button(onClick = vm::neuerFamiliencode, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text("Neuen Familiencode erstellen", fontWeight = FontWeight.Bold)
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    OutlinedButton(
        onClick = {
            qrScannen(ctx, { roh ->
                val gelesen = de.cemaras.yem1n.logik.QrPaket.lese(roh)
                fehler = gelesen == null || gelesen.first != "familie" || !vm.setzeFamiliencode(gelesen.second)
            }, { fehler = true })
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text("📷 QR-Code scannen", color = Color.White, fontWeight = FontWeight.Bold) }
    Text("Oder gib den Code ein, den du schon hast:", color = TextSanft, fontSize = 14.sp)
    OutlinedTextField(
        value = eingabe,
        onValueChange = { eingabe = it.take(12); fehler = false },
        label = { Text("Familiencode") },
        placeholder = { Text("XXXXX-XXXXX") },
        singleLine = true,
        isError = fehler,
        supportingText = { if (fehler) Text("Der Code hat 10 Zeichen aus Buchstaben und Zahlen.") },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { fehler = !vm.setzeFamiliencode(eingabe) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text("Code benutzen", fontWeight = FontWeight.Bold) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrageFormular(vm: AppViewModel) {
    var text by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Frag deine Eltern", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        val uebrig = vm.fortschritt.jokerUebrig(de.cemaras.yem1n.logik.Tag.heute())
        Text(
            "Joker heute: " + "🃏".repeat(uebrig) + "▫️".repeat((vm.fortschritt.jokerLimit - uebrig).coerceAtLeast(0)) + "  ($uebrig von ${vm.fortschritt.jokerLimit})",
            color = TextSanft, fontSize = 14.sp,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(500) },
            label = { Text("Was verstehst du nicht?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Button(
            onClick = {
                vm.jokerFrageSenden("", "", text)
                text = ""
            },
            enabled = text.isNotBlank() && !vm.jokerBeschaeftigt && uebrig > 0,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Frage abschicken", fontWeight = FontWeight.Bold) }
        if (uebrig <= 0) {
            Text("Heute sind keine Joker mehr übrig.", color = TextSanft, fontSize = 14.sp)
            OutlinedButton(
                onClick = vm::jokerNachschubAnfragen,
                enabled = !vm.jokerBeschaeftigt,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("🙏 Neue Joker anfragen", color = Color.White) }
        }
    }
}

@Composable
private fun NachschubKarte(vm: AppViewModel) {
    val kind = vm.jokerNachschubOffen
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Joker-Nachschub", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text(
            if (kind != null) "$kind hat keine Joker mehr und fragt nach neuen."
            else "Keine offene Anfrage. Du kannst die Joker trotzdem jederzeit auffüllen.",
            color = Color.White, fontSize = 15.sp,
        )
        Button(
            onClick = vm::jokerFreigeben,
            enabled = !vm.jokerBeschaeftigt,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Joker wieder auf ${vm.fortschritt.jokerLimit} auffüllen", fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrageKarte(vm: AppViewModel, frage: JokerFrage, eltern: Boolean) {
    var antwort by remember(frage.id) { mutableStateOf("") }
    var emoji by remember(frage.id) { mutableStateOf(EMOJIS.first()) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(frage.kind, color = TextSanft, fontSize = 13.sp)
        Text(frage.text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        frage.antworten.forEach { a ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).glas(14.dp).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("${a.emoji} ${a.absender}", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(a.text, color = Color.White, fontSize = 16.sp)
                if (a.daumen) {
                    Text("👍 Danke!", color = Gelb, fontSize = 13.sp)
                } else if (!eltern) {
                    TextButton(onClick = { vm.jokerDaumen(frage.id, a.id) }, enabled = !vm.jokerBeschaeftigt) {
                        Text("👍 Das hat geholfen", color = Gelb)
                    }
                }
            }
        }
        if (eltern) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EMOJIS.forEach { e ->
                    FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) }, colors = chipFarbenJoker())
                }
            }
            OutlinedTextField(
                value = antwort,
                onValueChange = { antwort = it.take(500) },
                label = { Text("Deine Antwort") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Button(
                onClick = {
                    vm.jokerAntwortSenden(frage.id, emoji, antwort)
                    antwort = ""
                },
                enabled = antwort.isNotBlank() && !vm.jokerBeschaeftigt,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Antwort senden", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EinwilligungKarte(vm: AppViewModel) {
    var haken by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Text("Bevor es losgeht", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Gelb)
    Text(
        "Bitte lass diesen Schritt von einem Elternteil oder einer erziehungsberechtigten Person lesen und bestätigen.",
        color = Color.White, fontSize = 16.sp,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (p in de.cemaras.yem1n.logik.Einwilligung.punkte) {
            Row {
                Text("•  ", color = Gelb, fontSize = 15.sp)
                Text(p, color = Color.White, fontSize = 15.sp)
            }
        }
    }
    TextButton(onClick = { vm.oeffneInfoSeite(InfoSeite.DATENSCHUTZ) }) {
        Text("Datenschutzerklärung lesen", color = Gelb, fontWeight = FontWeight.Bold)
    }
    Row(verticalAlignment = Alignment.Top) {
        androidx.compose.material3.Checkbox(checked = haken, onCheckedChange = { haken = it })
        Text(
            "Ich bin Elternteil oder erziehungsberechtigte Person. Ich habe die Datenschutzerklärung gelesen und willige in die beschriebene Verarbeitung für mein Kind ein.",
            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    androidx.compose.material3.Button(onClick = vm::einwilligen, enabled = haken, modifier = Modifier.fillMaxWidth()) {
        Text("Einverstanden, weiter")
    }
    TextButton(onClick = vm::zurueck, modifier = Modifier.fillMaxWidth()) {
        Text("Nicht einverstanden", color = TextSanft)
    }
    Text("Ohne Einwilligung bleibt die App auf diesem Gerät. Üben geht trotzdem.", color = TextSanft, fontSize = 13.sp)
}
