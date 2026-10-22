package de.cemaras.yem1n.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Karteikasten
import de.cemaras.yem1n.logik.Sprachen
import de.cemaras.yem1n.logik.lehrplan.Vokabel
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.TextSanft

private enum class SprachModus { KARTEN, PAARE }

@Composable
fun SprachenScreen(vm: AppViewModel) {
    var sprache by rememberSaveable { mutableStateOf("en") }
    var thema by rememberSaveable { mutableStateOf<String?>(null) }
    var modus by rememberSaveable { mutableStateOf<String?>(null) }
    val info = Sprachen.info(sprache)
    val t = Sprachen.themen.firstOrNull { it.id == thema }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = {
            when {
                modus != null -> modus = null
                thema != null -> thema = null
                else -> vm.zurueck()
            }
        }) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("🗣️ Sprachen", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in Sprachen.alle) {
                KnopfChip("${s.flagge} ${s.name}", s.code == sprache) { sprache = s.code; thema = null; modus = null }
            }
        }

        val jetzt = System.currentTimeMillis()
        if (modus == "bericht") {
            BerichtAnsicht(vm, sprache)
            Spacer(Modifier.height(24.dp))
            return@Column
        }
        if (modus == "wdh") {
            val faellig = remember(sprache, jetzt / 60000) { faelligeWoerter(vm, sprache, jetzt) }
            Text("🔁 Heute wiederholen", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Gelb)
            KartenAnsicht(vm, faellig.map { it.second }, sprache, themaVon = { w -> faellig.first { it.second === w }.first })
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        if (t == null) {
            val faellig = faelligeWoerter(vm, sprache, jetzt).size
            Text("Wähle ein Thema. Die Wörter sprichst du laut nach.", color = TextSanft, fontSize = 14.sp)
            if (faellig > 0) BreitKnopf("🔁 Heute wiederholen ($faellig)", "Wörter, die wieder dran sind") { modus = "wdh" }
            else Text("🔁 Heute ist nichts zu wiederholen.", color = TextSanft, fontSize = 14.sp)
            for (x in Sprachen.themen) {
                val ws = Sprachen.woerter(x.id, sprache)
                val gelernt = ws.count { Karteikasten.gelernt(vm.fortschritt.wortStand[Karteikasten.schluessel(sprache, x.id, it.de)]) }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Karte)
                        .clickable { thema = x.id }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(x.emoji, fontSize = 28.sp)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(x.titel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("$gelernt von ${ws.size} gelernt", color = if (gelernt == ws.size && ws.isNotEmpty()) Mint else TextSanft, fontSize = 13.sp)
                    }
                    if (gelernt == ws.size && ws.isNotEmpty()) Text("✅", fontSize = 20.sp)
                }
            }
            BreitKnopf("📊 Bericht für Eltern", "Gelernte Wörter, schwierige Wörter, letzte Runden") { modus = "bericht" }
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        Text("${t.emoji} ${t.titel}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Gelb)
        val woerter = remember(t.id, sprache) { Sprachen.woerter(t.id, sprache) }
        when (modus) {
            "karten" -> KartenAnsicht(vm, woerter, sprache, themaVon = { t.id })
            "paare" -> PaareAnsicht(vm, woerter, sprache, t.id)
            else -> {
                BreitKnopf("🃏 Karten", "Wort anschauen, umdrehen, anhören") { modus = "karten" }
                BreitKnopf("🧩 Paare", "Zu jedem Wort das passende finden") { modus = "paare" }
                val lern = Sprachen.lernThema(t.id, sprache)
                if (lern != null) BreitKnopf("❓ Fragen", "Zehn Aufgaben mit Sternen") { vm.startLernThema(lern) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun KnopfChip(text: String, aktiv: Boolean, klick: () -> Unit) {
    Text(
        text, color = if (aktiv) Color(0xFF00205B) else Color.White, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (aktiv) Gelb else Karte)
            .clickable(onClick = klick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun BreitKnopf(titel: String, text: String, klick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Karte).clickable(onClick = klick).padding(16.dp),
    ) {
        Text(titel, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(text, color = TextSanft, fontSize = 14.sp)
    }
}

private fun faelligeWoerter(vm: AppViewModel, sprache: String, jetzt: Long): List<Pair<String, Vokabel>> =
    Sprachen.themen.flatMap { x ->
        Sprachen.woerter(x.id, sprache)
            .filter { Karteikasten.faellig(vm.fortschritt.wortStand[Karteikasten.schluessel(sprache, x.id, it.de)], jetzt) }
            .map { x.id to it }
    }

@Composable
private fun KartenAnsicht(vm: AppViewModel, woerter: List<Vokabel>, sprache: String, themaVon: (Vokabel) -> String) {
    var i by rememberSaveable { mutableStateOf(0) }
    var offen by rememberSaveable { mutableStateOf(false) }
    var gekonnt by rememberSaveable { mutableStateOf(0) }
    var nochmal by rememberSaveable { mutableStateOf(0) }
    if (woerter.isEmpty()) { Text("Hier ist nichts zu üben.", color = TextSanft); return }
    if (i >= woerter.size) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎉", fontSize = 64.sp)
            Text("Alle Karten durch", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Gelb)
            Text("$gekonnt gekonnt, $nochmal zum Wiederholen.", color = Color.White)
            TextButton(onClick = { i = 0; gekonnt = 0; nochmal = 0; offen = false }) { Text("Nochmal von vorn", color = Gelb, fontSize = 18.sp) }
        }
        return
    }
    val w = woerter[i]
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Karte).border(2.dp, if (offen) Mint else Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable {
                offen = !offen
                if (offen) vm.sprecher.sprich(w.text(sprache), sprache = sprache)
            }.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (w.emoji.isNotEmpty() && !de.cemaras.yem1n.logik.lehrplan.LehrplanEnglisch.nurZiffern(w.emoji)) Text(w.emoji, fontSize = 72.sp)
        Text(w.de, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        if (offen) {
            Text(w.text(sprache), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Mint, textAlign = TextAlign.Center)
            Text("🔊 nochmal anhören", color = Gelb, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            Text("Tippe zum Umdrehen", color = TextSanft, fontSize = 14.sp)
        }
    }
    Text("Karte ${i + 1} von ${woerter.size}", color = TextSanft, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    if (offen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BreitKnopfKlein("🔁 Nochmal", Modifier.weight(1f)) {
                vm.bewerteWort(sprache, themaVon(w), w.de, false); nochmal++; i++; offen = false
            }
            BreitKnopfKlein("✅ Gekonnt", Modifier.weight(1f)) {
                vm.bewerteWort(sprache, themaVon(w), w.de, true); gekonnt++; i++; offen = false
            }
        }
    }
}

@Composable
private fun BreitKnopfKlein(text: String, modifier: Modifier, klick: () -> Unit) {
    Text(
        text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Karte).border(1.dp, Gelb.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = klick).padding(vertical = 14.dp),
    )
}

@Composable
private fun PaareAnsicht(vm: AppViewModel, woerter: List<Vokabel>, sprache: String, themaId: String) {
    var runde by rememberSaveable { mutableStateOf(0) }
    val paare = remember(woerter, sprache, runde) { Sprachen.paare(woerter, sprache) }
    if (paare.size < 2) { Text("Zu wenige Wörter für dieses Spiel.", color = TextSanft); return }
    val rechts = remember(paare) { paare.shuffled() }
    var links by remember(paare) { mutableStateOf<String?>(null) }
    var gefunden by remember(paare) { mutableStateOf(setOf<String>()) }
    var versuche by remember(paare) { mutableStateOf(0) }
    var falsch by remember(paare) { mutableStateOf<String?>(null) }

    if (gefunden.size == paare.size) {
        androidx.compose.runtime.LaunchedEffect(paare) {
            vm.sprachRunde(sprache, themaId, "paare", paare.size, maxOf(versuche, paare.size))
            for (p in paare) vm.kannWort(sprache, themaId, p.de)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Geschafft! 🎉", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Gelb)
            Text("$versuche Versuche für ${paare.size} Paare", color = Color.White)
            TextButton(onClick = { runde++ }) { Text("Nochmal", color = Gelb, fontSize = 18.sp) }
        }
        return
    }
    Text("Tippe links ein deutsches Wort, dann rechts das passende.", color = TextSanft, fontSize = 14.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in paare) PaarKarte("${p.emoji} ${p.de}".trim(), p.de in gefunden, links == p.de, false) {
                if (p.de !in gefunden) { links = p.de; falsch = null }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in rechts) PaarKarte(p.fremd, p.de in gefunden, false, falsch == p.de) {
                if (p.de in gefunden) return@PaarKarte
                vm.sprecher.sprich(p.fremd, sprache = sprache)
                val gewaehlt = links ?: return@PaarKarte
                versuche++
                if (gewaehlt == p.de) { gefunden = gefunden + p.de; links = null; falsch = null } else falsch = p.de
            }
        }
    }
    Text("Versuche: $versuche", color = TextSanft, fontSize = 13.sp)
}

@Composable
private fun PaarKarte(text: String, fertig: Boolean, gewaehlt: Boolean, falsch: Boolean, klick: () -> Unit) {
    val rand = when { fertig -> Mint; gewaehlt -> Gelb; falsch -> Color(0xFFFF6B6B); else -> Color.White.copy(alpha = 0.3f) }
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
            .background(if (fertig) Mint.copy(alpha = 0.18f) else Karte)
            .border(2.dp, rand, RoundedCornerShape(14.dp)).clickable(onClick = klick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center) }
}

@Composable
private fun BerichtAnsicht(vm: AppViewModel, sprache: String) {
    val info = Sprachen.info(sprache)
    val stand = vm.fortschritt.wortStand
    val alle = Sprachen.themen.flatMap { x -> Sprachen.woerter(x.id, sprache).map { x to it } }
    val gelernt = alle.count { (x, w) -> Karteikasten.gelernt(stand[Karteikasten.schluessel(sprache, x.id, w.de)]) }
    Text("📊 Bericht ${info.flagge} ${info.name}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Gelb)
    Text("$gelernt von ${alle.size} Wörtern gelernt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text("Themen", color = Gelb, fontWeight = FontWeight.Bold)
    for (x in Sprachen.themen) {
        val ws = Sprachen.woerter(x.id, sprache)
        val g = ws.count { Karteikasten.gelernt(stand[Karteikasten.schluessel(sprache, x.id, it.de)]) }
        Row(Modifier.fillMaxWidth()) {
            Text("${x.emoji} ${x.titel}", color = Color.White, modifier = Modifier.weight(1f))
            Text("$g/${ws.size}", color = if (g == ws.size && ws.isNotEmpty()) Mint else TextSanft)
        }
    }
    Text("Schwierigste Wörter", color = Gelb, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    val schwer = alle.mapNotNull { (x, w) -> stand[Karteikasten.schluessel(sprache, x.id, w.de)]?.takeIf { it.f > 0 }?.let { Triple(w, it.f, x) } }
        .sortedByDescending { it.second }.take(5)
    if (schwer.isEmpty()) Text("Noch keine Fehler.", color = TextSanft)
    for ((w, f, _) in schwer) Row(Modifier.fillMaxWidth()) {
        Text("${w.de} · ${w.text(sprache)}", color = Color.White, modifier = Modifier.weight(1f))
        Text("${f}× falsch", color = TextSanft)
    }
    Text("Letzte Runden", color = Gelb, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    val runden = vm.fortschritt.sprachVerlauf.filter { it.sp == sprache }.takeLast(10).reversed()
    if (runden.isEmpty()) Text("Noch nichts geübt.", color = TextSanft)
    for (r in runden) {
        val titel = Sprachen.themen.firstOrNull { it.id == r.thema }?.titel ?: r.thema
        Row(Modifier.fillMaxWidth()) {
            Text("$titel (${r.art})", color = Color.White, modifier = Modifier.weight(1f))
            Text("${r.richtig} von ${r.gesamt}", color = TextSanft)
        }
    }
}
