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
import de.cemaras.yem1n.logik.Sprachen
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

        if (t == null) {
            Text("Wähle ein Thema. Die Wörter sprichst du laut nach.", color = TextSanft, fontSize = 14.sp)
            for (x in Sprachen.themen) {
                val anzahl = Sprachen.woerter(x.id, sprache).size
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Karte)
                        .clickable { thema = x.id }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(x.emoji, fontSize = 28.sp)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(x.titel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("$anzahl Wörter", color = TextSanft, fontSize = 13.sp)
                    }
                }
            }
            return@Column
        }

        Text("${t.emoji} ${t.titel}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Gelb)
        val woerter = remember(t.id, sprache) { Sprachen.woerter(t.id, sprache) }
        when (modus) {
            "karten" -> KartenAnsicht(vm, woerter, sprache)
            "paare" -> PaareAnsicht(vm, woerter, sprache)
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

@Composable
private fun KartenAnsicht(vm: AppViewModel, woerter: List<de.cemaras.yem1n.logik.lehrplan.Vokabel>, sprache: String) {
    var i by rememberSaveable { mutableStateOf(0) }
    var offen by rememberSaveable { mutableStateOf(false) }
    if (woerter.isEmpty()) return
    val w = woerter[i.coerceIn(0, woerter.lastIndex)]
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { i = (i - 1 + woerter.size) % woerter.size; offen = false }) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("${i + 1} von ${woerter.size}", color = TextSanft)
        TextButton(onClick = { i = (i + 1) % woerter.size; offen = false }) { Text("Weiter ›", color = Gelb, fontSize = 16.sp) }
    }
}

@Composable
private fun PaareAnsicht(vm: AppViewModel, woerter: List<de.cemaras.yem1n.logik.lehrplan.Vokabel>, sprache: String) {
    var runde by rememberSaveable { mutableStateOf(0) }
    val paare = remember(woerter, sprache, runde) { Sprachen.paare(woerter, sprache) }
    if (paare.size < 2) { Text("Zu wenige Wörter für dieses Spiel.", color = TextSanft); return }
    val rechts = remember(paare) { paare.shuffled() }
    var links by remember(paare) { mutableStateOf<String?>(null) }
    var gefunden by remember(paare) { mutableStateOf(setOf<String>()) }
    var versuche by remember(paare) { mutableStateOf(0) }
    var falsch by remember(paare) { mutableStateOf<String?>(null) }

    if (gefunden.size == paare.size) {
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
