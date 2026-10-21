package de.cemaras.yem1n.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Lob
import de.cemaras.yem1n.logik.MemoryStand
import de.cemaras.yem1n.logik.VorschulArt
import de.cemaras.yem1n.logik.VorschulFrage
import de.cemaras.yem1n.logik.VorschulStand
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.logik.VorschulSprache
import kotlinx.coroutines.delay

/** Die Übersicht der Vorschule: Zahlenraum, Schulstart-Pass und alle Spiele. */
@Composable
fun VorschulScreen(vm: AppViewModel) {
    val stufe = vm.fortschritt.vorschulStufe
    val name = vm.fortschritt.name.trim()
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🧸", fontSize = 44.sp)
            Spacer(Modifier.padding(start = 12.dp))
            Column {
                Text("Vorschule", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Gelb)
                Text(
                    if (name.isEmpty()) "Spielen und fit für die Schule werden"
                    else "Hallo $name! Spielen und fit für die Schule werden",
                    color = TextSanft, fontSize = 15.sp,
                )
            }
        }
        Text("Zahlenraum", color = Color.White, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Bis 5", 2 to "Bis 10", 3 to "Bis 20").forEach { (wert, text) ->
                FilterChip(
                    selected = stufe == wert,
                    onClick = { vm.setzeVorschulStufe(wert) },
                    label = { Text(text) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = Color.White, selectedContainerColor = Gelb, selectedLabelColor = Navy,
                    ),
                )
            }
        }
        val angebot = VorschulSprache.angebot(vm.fortschritt.vorschulEnglisch, vm.fortschritt.vorschulTuerkisch)
        if (angebot.size > 1) {
            Text("Sprache", color = Color.White, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                angebot.forEach { sprache ->
                    FilterChip(
                        selected = vm.vorschulSpracheAktiv == sprache,
                        onClick = { vm.setzeVorschulSprache(sprache) },
                        label = { Text(VorschulSprache.name(sprache)) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White, selectedContainerColor = Gelb, selectedLabelColor = Navy,
                        ),
                    )
                }
            }
        }
        SchulstartPass(vm.fortschritt.vorschulStempel())
        Abschnitt("Zahlen und Rechnen")
        SpielRaster(VorschulArt.rechnen, vm)
        Abschnitt("Sprache und Laute")
        SpielRaster(VorschulArt.sprache, vm)
        Abschnitt("Spielen und Entdecken")
        SpielRaster(VorschulArt.spielen, vm)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Abschnitt(titel: String) {
    Text(titel, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Gelb, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SpielRaster(arten: List<VorschulArt>, vm: AppViewModel) {
    val stempel = vm.fortschritt.vorschulStempel()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        arten.chunked(2).forEach { reihe ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                reihe.forEach { art ->
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Karte)
                            .clickable { vm.startVorschul(art) }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(art.emoji, fontSize = 46.sp)
                        Text(
                            art.titel, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp,
                            textAlign = TextAlign.Center, maxLines = 2,
                        )
                        if (art in stempel) Text("✅", fontSize = 14.sp)
                    }
                }
                if (reihe.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Ein Stempel für jedes Spiel, das mit mindestens einem Stern geschafft ist. */
@Composable
private fun SchulstartPass(stempel: Set<VorschulArt>) {
    val stationen = VorschulArt.stationen
    val anzahl = stationen.count { it in stempel }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Karte).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎒 Mein Schulstart-Pass", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.White, modifier = Modifier.weight(1f))
            Text("$anzahl von ${stationen.size}", fontWeight = FontWeight.Black, color = Gelb)
        }
        stationen.chunked(6).forEach { reihe ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reihe.forEach { art ->
                    val hat = art in stempel
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (hat) Gelb.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.08f))
                            .alpha(if (hat) 1f else 0.45f),
                        contentAlignment = Alignment.Center,
                    ) { Text(art.emoji, fontSize = 20.sp) }
                }
                repeat(6 - reihe.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(
            if (anzahl == stationen.size) "Alle Stempel! Du bist bereit für die Schule. 🎉"
            else "Schaffe jedes Spiel mit mindestens einem Stern und sammle alle Stempel.",
            fontSize = 13.sp, color = TextSanft,
        )
    }
}

/** Ein Spiel: Memory oder eine Runde mit Fragen. */
@Composable
fun VorschulSpielScreen(vm: AppViewModel, art: VorschulArt) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text(art.titel, color = TextSanft, fontWeight = FontWeight.SemiBold)
        }
        if (art == VorschulArt.MEMORY) {
            val m = vm.memory
            if (m != null) MemorySpiel(vm, m)
        } else {
            val s = vm.vorschul
            if (s != null) FragenSpiel(vm, s)
        }
    }
}

@Composable
private fun FragenSpiel(vm: AppViewModel, s: VorschulStand) {
    val frage = s.aktuelle
    if (frage == null) {
        VorschulErgebnis(
            emoji = if (s.sterne == 3) "🎉" else if (s.sterne >= 1) "👏" else "💪",
            titel = "${s.perfekt} von ${s.fragen.size} gleich richtig!",
            zusatz = null,
            sterne = s.sterne,
            perfekt = s.perfekt, gesamt = s.fragen.size,
            vm = vm,
        )
        return
    }
    // Nach dem Treffer kurz warten, dann kommt die nächste Frage von selbst
    LaunchedEffect(s.nr, s.richtigGewaehlt) {
        if (s.richtigGewaehlt != null) {
            delay(1200)
            vm.vorschulWeiter()
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LinearProgressIndicator(
            progress = { s.nr.toFloat() / s.fragen.size },
            modifier = Modifier.fillMaxWidth(),
            color = Gelb,
            trackColor = Color.White.copy(alpha = 0.15f),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Karte)
                .clickable { vm.frageVorlesen() }
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔊", fontSize = 24.sp)
            Spacer(Modifier.padding(start = 10.dp))
            Text(frage.text.text(vm.vorschulSpracheAktiv), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        FrageBild(frage)
        Optionen(frage, s) { vm.vorschulWahl(it) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FrageBild(f: VorschulFrage) {
    if (f.gross.isNotEmpty()) {
        Text(f.gross, fontSize = 96.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
    if (f.zeige.isNotEmpty()) EmojiReihen(f.zeige)
    if (f.plus) {
        Text("+", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Gelb)
        EmojiReihen(f.zeigeZwei)
    }
    if (f.folge.isNotEmpty()) {
        Text(f.folge.joinToString(" ") + " ❓", fontSize = 36.sp, textAlign = TextAlign.Center)
    }
}

/** Bilder in Reihen zu je fünf. */
@Composable
private fun EmojiReihen(emojis: List<String>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        emojis.chunked(5).forEach { reihe ->
            Text(reihe.joinToString(" "), fontSize = 36.sp)
        }
    }
}

@Composable
private fun Optionen(f: VorschulFrage, s: VorschulStand, onWahl: (Int) -> Unit) {
    // Drei einfache Antworten nebeneinander, sonst zwei pro Reihe
    val spalten = if (f.optionen.size == 3 && !f.gruppenOptionen) 3 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        f.optionen.withIndex().toList().chunked(spalten).forEach { reihe ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                reihe.forEach { (i, o) ->
                    OptionKnopf(o, f.gruppenOptionen, i == s.richtigGewaehlt, i in s.falsch, Modifier.weight(1f)) { onWahl(i) }
                }
                repeat(spalten - reihe.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun OptionKnopf(
    text: String, gruppe: Boolean, richtig: Boolean, falsch: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val groesse by animateFloatAsState(if (richtig) 1.06f else 1f, label = "treffer")
    val form = RoundedCornerShape(26.dp)
    Box(
        modifier
            .heightIn(min = 112.dp)
            .scale(groesse)
            .alpha(if (falsch) 0.45f else 1f)
            .clip(form)
            .background(if (richtig) Mint.copy(alpha = 0.6f) else if (falsch) Koralle.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.14f))
            .border(if (richtig) 4.dp else 1.5.dp, if (richtig) Mint else Color.White.copy(alpha = 0.18f), form)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = if (gruppe) 28.sp else 50.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MemorySpiel(vm: AppViewModel, m: MemoryStand) {
    if (m.fertig) {
        VorschulErgebnis(
            emoji = "🎉", titel = "Alle Paare gefunden!", zusatz = "In ${m.zuege} Zügen",
            sterne = de.cemaras.yem1n.logik.Pruefer.sterneFuer(m.paare, m.gesamt),
            perfekt = m.paare, gesamt = m.gesamt, vm = vm,
        )
        return
    }
    // Passen zwei Karten nicht zusammen, bleiben sie kurz offen und werden dann zugedeckt
    LaunchedEffect(m.sperre, m.zuege) {
        if (m.sperre) {
            delay(900)
            vm.memoryZudecken()
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Finde zwei gleiche Bilder", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
        m.karten.withIndex().toList().chunked(3).forEach { reihe ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                reihe.forEach { (i, k) ->
                    val zeigen = k.offen || k.weg
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 96.dp)
                            .alpha(if (k.weg) 0.3f else 1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (zeigen) Color.White.copy(alpha = 0.16f) else Gelb.copy(alpha = 0.85f))
                            .clickable { vm.memoryTippe(i) },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (zeigen) k.emoji else "❔", fontSize = 40.sp) }
                }
                repeat(3 - reihe.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text("Züge: ${m.zuege}", color = TextSanft)
    }
}

@Composable
private fun VorschulErgebnis(
    emoji: String, titel: String, zusatz: String?, sterne: Int, perfekt: Int, gesamt: Int, vm: AppViewModel,
) {
    val name = vm.fortschritt.name
    val ende = remember(perfekt, gesamt) { Lob.fuerEnde(perfekt, gesamt, name) }
    Column(
        Modifier.fillMaxSize().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 90.sp)
        Spacer(Modifier.height(10.dp))
        Text(titel, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        if (zusatz != null) Text(zusatz, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gelb)
        Spacer(Modifier.height(10.dp))
        Text(sterneText(sterne), fontSize = 48.sp, color = Gelb)
        Spacer(Modifier.height(10.dp))
        Text(ende, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
        if (vm.verdient > 0) {
            Spacer(Modifier.height(8.dp))
            Text("🪙 +${vm.verdient} Münzen", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gelb)
        }
        if (vm.neuePokale.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Neues Abzeichen!", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
            vm.neuePokale.forEach {
                Text("${it.emoji} ${it.titel}", fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = vm::vorschulNochmal, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Nochmal", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = vm::zurueck,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, Gelb),
        ) { Text("Fertig", fontSize = 18.sp, color = Gelb) }
    }
}
