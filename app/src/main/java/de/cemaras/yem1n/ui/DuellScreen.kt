package de.cemaras.yem1n.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.DuellHerausforderung
import de.cemaras.yem1n.logik.DuellLauf
import de.cemaras.yem1n.logik.DuellPost
import de.cemaras.yem1n.logik.DuellRegeln
import de.cemaras.yem1n.logik.DuellSpiel
import de.cemaras.yem1n.logik.DuellStand
import de.cemaras.yem1n.logik.DuellStufe
import de.cemaras.yem1n.logik.Familiencode
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DuellScreen(vm: AppViewModel) {
    val spiel = vm.duellSpiel
    if (spiel != null) DuellSpielAnsicht(vm, spiel) else DuellStart(vm)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuellStart(vm: AppViewModel) {
    val code = vm.jokerCode
    var meine by remember { mutableStateOf(vm.duellMeineStufe) }
    var andere by remember { mutableStateOf(if (vm.duellMeineStufe == DuellStufe.PROFI) DuellStufe.KLASSE3 else DuellStufe.PROFI) }
    val geraet = vm.fortschritt.duellGeraet
    val offen = DuellPost.offen(vm.duelle, vm.duellAntworten, geraet)
    val wartend = DuellPost.wartend(vm.duelle, vm.duellAntworten, geraet)
    val entschieden = DuellPost.entschieden(vm.duelle, vm.duellAntworten)
    val format = remember { SimpleDateFormat("dd.MM.", Locale.GERMANY) }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("⚔️ Duell", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Kopfrechnen gegen deine Familie. Du rechnest vor, die anderen rechnen nach, jeder auf seiner Stufe.", color = TextSanft, fontSize = 15.sp)

        if (!Familiencode.istGueltig(code)) {
            Text("Für das Duell braucht ihr einen gemeinsamen Familiencode.", color = Color.White, fontSize = 16.sp)
            Button(onClick = vm::oeffneJoker, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Familiencode einrichten", fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        offen.forEach { d ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Gelb)
                    .clickable { vm.duellAnnehmen(d) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⚔️", fontSize = 34.sp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("${d.von} fordert dich heraus!", color = Navy, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("${d.von} hat ${DuellRegeln.standText(d.vonStand)}.", color = Navy, fontSize = 13.sp)
                    Text("Du rechnest auf ${d.anStufe.emoji} ${d.anStufe.kurz}. Tippe zum Annehmen.", color = Navy, fontSize = 13.sp)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Neue Herausforderung", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Ich rechne auf", color = TextSanft, fontSize = 13.sp)
            StufenWahl(meine) { meine = it }
            Text("Die anderen rechnen auf", color = TextSanft, fontSize = 13.sp)
            StufenWahl(andere) { andere = it }
            Button(onClick = { vm.duellNeu(meine, andere) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Vorrechnen und abschicken", fontWeight = FontWeight.Bold)
            }
        }

        wartend.forEach { d ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).glas(18.dp).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⏳", fontSize = 24.sp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Deine Herausforderung wartet", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("${DuellRegeln.standText(d.vonStand)}. Die anderen rechnen auf ${d.anStufe.kurz}.", color = TextSanft, fontSize = 12.sp)
                }
            }
        }

        if (entschieden.isNotEmpty()) {
            Text("Entschieden", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
            entschieden.forEach { p ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).glas(18.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row {
                        Text(p.siegerName?.let { "🏆 $it gewinnt" } ?: "🤝 Unentschieden", color = Gelb, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Text(format.format(Date(DuellRegeln.zeitpunktMs(p.antwort.zeitpunkt))), color = TextSanft, fontSize = 12.sp)
                    }
                    Text("${p.duell.vonStufe.emoji} ${p.duell.von}: ${DuellRegeln.standText(p.duell.vonStand)}", color = Color.White, fontSize = 13.sp)
                    Text("${p.duell.anStufe.emoji} ${p.antwort.name}: ${DuellRegeln.standText(p.antwort.stand)}", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        OutlinedButton(onClick = vm::duellLaden, enabled = !vm.duellBeschaeftigt) {
            Text(if (vm.duellBeschaeftigt) "Einen Moment..." else "Neu laden", color = Color.White)
        }
        if (vm.duellMeldung.isNotEmpty()) Text(vm.duellMeldung, color = Koralle, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StufenWahl(gewaehlt: DuellStufe, onWahl: (DuellStufe) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val farben = FilterChipDefaults.filterChipColors(
            labelColor = Color.White, selectedContainerColor = Gelb, selectedLabelColor = Navy,
        )
        DuellStufe.entries.forEach { s ->
            FilterChip(selected = gewaehlt == s, onClick = { onWahl(s) }, label = { Text("${s.emoji} ${s.kurz}") }, colors = farben)
        }
    }
    Text(gewaehlt.beschreibung, color = TextSanft, fontSize = 12.sp)
}

@Composable
private fun DuellSpielAnsicht(vm: AppViewModel, spiel: DuellSpiel) {
    var lauf by remember(spiel) { mutableStateOf(DuellLauf(spiel.aufgaben)) }
    var countdown by remember(spiel) { mutableStateOf(3) }
    var startNs by remember(spiel) { mutableStateOf(0L) }
    var stand by remember(spiel) { mutableStateOf<DuellStand?>(null) }
    var sekunden by remember(spiel) { mutableStateOf(0.0) }
    var eingabe by remember(spiel, lauf.index) { mutableStateOf("") }
    var rueckmeldung by remember(spiel) { mutableStateOf<Boolean?>(null) }
    val gegner = spiel.herausforderung

    LaunchedEffect(spiel) {
        while (countdown > 0) {
            delay(800)
            countdown--
        }
        startNs = System.nanoTime()
        while (stand == null) {
            delay(100)
            sekunden = (System.nanoTime() - startNs) / 1e9
        }
    }

    fun abgeben() {
        if (countdown > 0 || stand != null) return
        val r = lauf.antworte(eingabe) ?: return
        lauf = r.first
        rueckmeldung = r.second
        if (lauf.fertig) {
            val s = lauf.stand((System.nanoTime() - startNs) / 1e9)
            stand = s
            vm.duellFertig(s)
        }
    }

    Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::duellSpielBeenden) { Text("✕ Beenden", color = Gelb, fontSize = 16.sp) }
                if (gegner != null) {
                    Text(
                        "Gegen ${gegner.von}: ${gegner.vonStand.richtig} richtig, ${DuellRegeln.zeitText(gegner.vonStand.sekunden)}",
                        color = TextSanft, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                    )
                }
            }
            Text("${vm.duellName}  ${spiel.stufe.emoji} ${spiel.stufe.kurz}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row {
                Text("Aufgabe ${minOf(lauf.index + 1, lauf.aufgaben.size)} von ${lauf.aufgaben.size}", color = TextSanft, modifier = Modifier.weight(1f))
                Text("⏱ " + DuellRegeln.zeitText(sekunden + lauf.strafe), color = Gelb, fontWeight = FontWeight.Bold)
            }
            val a = lauf.aktuelle
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).glas(26.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(a?.rechnung?.let { "$it =" } ?: "Fertig", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                Text(
                    when (rueckmeldung) {
                        true -> "Richtig!"
                        false -> "Falsch, 3 Sekunden Strafe"
                        null -> " "
                    },
                    color = if (rueckmeldung == false) Koralle else Mint, fontSize = 14.sp,
                )
            }
            OutlinedTextField(
                value = eingabe,
                onValueChange = { eingabe = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Deine Antwort") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { abgeben() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { abgeben() },
                enabled = eingabe.isNotEmpty() && countdown == 0 && stand == null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("OK", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }

        if (countdown > 0) {
            Text(
                "$countdown", color = Gelb, fontSize = 120.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        stand?.let { s ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(24.dp).clip(RoundedCornerShape(30.dp)).glas(30.dp).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val sieger = gegner?.let { DuellRegeln.sieger(s, it.vonStand) }
                    Text(
                        when {
                            gegner == null -> "⚔️"
                            sieger == 0 -> "🏆"
                            sieger == 1 -> "💪"
                            else -> "🤝"
                        },
                        fontSize = 64.sp,
                    )
                    Text(
                        when {
                            gegner == null -> "Herausforderung steht!"
                            sieger == 0 -> "Du hast gewonnen!"
                            sieger == 1 -> "${gegner.von} war diesmal besser. Revanche?"
                            else -> "Unentschieden!"
                        },
                        color = Gelb, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                    )
                    Text(DuellRegeln.standText(s), color = Color.White, fontSize = 16.sp)
                    Text(
                        if (gegner == null) "Deine Familie bekommt die Herausforderung jetzt." else "Das Ergebnis ist bei deiner Familie.",
                        color = TextSanft, fontSize = 13.sp, textAlign = TextAlign.Center,
                    )
                    Button(onClick = vm::duellSpielBeenden, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Fertig", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
