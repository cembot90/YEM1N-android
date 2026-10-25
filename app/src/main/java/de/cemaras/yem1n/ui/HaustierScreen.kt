package de.cemaras.yem1n.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.Eule
import androidx.compose.foundation.layout.width
import de.cemaras.yem1n.logik.Haustier
import de.cemaras.yem1n.logik.HaustierArt
import de.cemaras.yem1n.logik.HaustierHut
import de.cemaras.yem1n.logik.HaustierStufe
import de.cemaras.yem1n.logik.HaustierZeit
import de.cemaras.yem1n.logik.Tag
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.auswahl
import de.cemaras.yem1n.ui.theme.glas
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.TextSanft

/** Gibt eine Funktion zurück, die (ab Android 13) einmal um die Erlaubnis für Mitteilungen bittet. */
@Composable
fun rememberErinnerungsFrage(vm: AppViewModel): () -> Unit {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val starter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { vm.erinnerungNeuPlanen() }
    return {
        if (!de.cemaras.yem1n.speicher.Erinnerung.erlaubt(ctx)) starter.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else vm.erinnerungNeuPlanen()
    }
}

@Composable
fun HaustierScreen(vm: AppViewModel) {
    val tier = vm.haustier
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text("🪙 ${vm.fortschritt.muenzen}", color = Gelb, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (tier == null) HaustierWahl(vm) else HaustierStall(vm, tier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HaustierWahl(vm: AppViewModel) {
    var art by remember { mutableStateOf("katze") }
    var name by remember { mutableStateOf("") }
    val frage = rememberErinnerungsFrage(vm)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Such dir ein Haustier aus", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        Text("🥚", fontSize = 90.sp)
        Text(
            "Es schlüpft, wenn du eine Runde übst. Danach wächst es mit jeder Runde.",
            color = TextSanft, textAlign = TextAlign.Center,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HaustierArt.alle.forEach { a ->
                val aktiv = a.id == art
                Column(
                    Modifier
                        .auswahl(aktiv, 18.dp)
                        .clickable { art = a.id }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(a.emoji, fontSize = 40.sp)
                    Text(a.name, color = Color.White, fontSize = 13.sp)
                }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(Haustier.NAMENSLAENGE) },
            label = { Text("Name") },
            placeholder = { Text("Pip") },
            singleLine = true,
        )
        Button(
            onClick = {
                vm.neuesHaustier(name, art)
                if (vm.fortschritt.haustierErinnerung) frage()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Das ist meins!", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun HaustierStall(vm: AppViewModel, tier: Haustier) {
    val muenzen = vm.fortschritt.muenzen
    val heute = Tag.heute()
    var hinweis by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(tier.name, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("${tier.stufe.titel} ${tier.stimmung.emoji}", color = Gelb, fontWeight = FontWeight.Bold)
        val schlaeft = tier.stufe != HaustierStufe.EI && HaustierZeit.schlaeftJetzt()
        OlliBlase(vm, tier, muenzen, heute, schlaeft)
        Tierbild(tier, schlaeft)
        val weiter = tier.stufe.naechste
        if (weiter != null) {
            Column(Modifier.fillMaxWidth()) {
                Text("Auf dem Weg zu: ${weiter.titel}", color = TextSanft, fontSize = 13.sp)
                Balken(tier.fortschritt.toFloat(), Gelb)
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🍽️ Hunger (satt)", color = TextSanft, fontSize = 13.sp)
            Balken((tier.satt / 100).toFloat(), if (tier.satt < Haustier.HUNGRIG_GRENZE) Koralle else Mint)
            Text("❤️ Laune", color = TextSanft, fontSize = 13.sp)
            Balken((tier.laune / 100).toFloat(), if (tier.laune < 30) Koralle else Mint)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TierKnopf("🍪", "Leckerli", "${Haustier.LECKERLI_PREIS} 🪙", tier.kannLeckerli(muenzen), Modifier.weight(1f)) {
                vm.haustierLeckerli()
                hinweis = "Mmh, lecker!"
            }
            TierKnopf("🎾", "Spielen", "${Haustier.SPIELZEUG_PREIS} 🪙", tier.kannSpielen(muenzen), Modifier.weight(1f)) {
                vm.haustierSpielen()
                hinweis = "Das macht Spaß!"
            }
        }
        if (tier.geschenkOffen(heute) && tier.stufe != HaustierStufe.EI) {
            Button(onClick = {
                val plus = vm.haustierGeschenk()
                if (plus > 0) hinweis = "${tier.name} schenkt dir $plus Münzen."
            }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("🎁 Tagesgeschenk abholen", fontWeight = FontWeight.Bold)
            }
        }
        hinweis?.let { Text(it, color = Gelb, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        var frageWechsel by remember { mutableStateOf(false) }
        if (frageWechsel) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { frageWechsel = false },
                title = { Text("Anderes Haustier aussuchen?") },
                text = { Text("Dein jetziges Haustier und alle Hüte sind dann weg.") },
                confirmButton = {
                    TextButton(onClick = { frageWechsel = false; vm.haustierWechseln() }) { Text("Ja, mein Haustier geht") }
                },
                dismissButton = { TextButton(onClick = { frageWechsel = false }) { Text("Nein, behalten") } },
            )
        }
        if (tier.stufe == HaustierStufe.EI) {
            Text("Leckerli, Spielen und Hüte gibt es, sobald ${tier.name} geschlüpft ist.", color = TextSanft, textAlign = TextAlign.Center, fontSize = 13.sp)
        } else {
            Hutladen(vm, tier)
        }
        val frageErinnerung = rememberErinnerungsFrage(vm)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Erinnere mich, wenn mein Haustier Hunger hat", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
            androidx.compose.material3.Switch(checked = vm.fortschritt.haustierErinnerung, onCheckedChange = {
                vm.setzeErinnerung(it)
                if (it) frageErinnerung()
            })
        }
        TextButton(onClick = { frageWechsel = true }) { Text("Anderes Haustier aussuchen", color = TextSanft) }
    }
}

@Composable
private fun Tierbild(tier: Haustier, schlaeft: Boolean = false) {
    val wippen = rememberInfiniteTransition(label = "wippen")
    val hoch by wippen.animateFloat(
        initialValue = 0f, targetValue = if (schlaeft) 0f else -10f,
        animationSpec = infiniteRepeatable(tween(if (tier.stimmung.name == "FROH") 500 else 1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hoch",
    )
    val groesse = tier.stufe.groesse
    Box(Modifier.height((groesse + 40).dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(tier.emoji, fontSize = groesse.sp, modifier = Modifier.offset(y = hoch.dp).alpha(if (schlaeft) 0.6f else 1f))
        if (schlaeft) Text("💤", fontSize = (groesse * 0.4f).sp, modifier = Modifier.offset(x = (groesse * 0.5f).dp, y = (-groesse * 0.4f).dp))
        val hut = HaustierHut.finde(tier.hut)
        if (hut != null && tier.stufe != HaustierStufe.EI) {
            Text(hut.emoji, fontSize = (groesse * 0.45f).sp, modifier = Modifier.offset(y = (hoch - groesse * 0.45f).dp))
        }
    }
}

@Composable
private fun Sprechblase(text: String) {
    Text(
        text,
        modifier = Modifier.clip(RoundedCornerShape(18.dp)).glas(18.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        color = Color.White, textAlign = TextAlign.Center, fontSize = 16.sp,
    )
}

@Composable
private fun Balken(anteil: Float, farbe: Color) {
    LinearProgressIndicator(
        progress = { anteil.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
        color = farbe,
        trackColor = Color.White.copy(alpha = 0.15f),
    )
}

@Composable
private fun TierKnopf(symbol: String, titel: String, preis: String, an: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .auswahl(false, 18.dp, aktiv = an)
            .clickable(enabled = an, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(symbol, fontSize = 30.sp)
        Text(titel, color = if (an) Color.White else TextSanft, fontWeight = FontWeight.Bold)
        Text(preis, color = TextSanft, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hutladen(vm: AppViewModel, tier: Haustier) {
    val muenzen = vm.fortschritt.muenzen
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Hüte", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Gelb)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HaustierHut.alle.forEach { h ->
                val habe = h.id in tier.huete
                val traegt = tier.hut == h.id
                val kann = habe || tier.kannKaufen(h.id, muenzen)
                Column(
                    Modifier
                        .auswahl(traegt, 16.dp, aktiv = kann)
                        .clickable(enabled = kann) {
                            if (habe) vm.haustierTragen(if (traegt) "" else h.id) else vm.haustierKaufen(h.id)
                        }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(h.emoji, fontSize = 32.sp)
                    Text(h.name, color = if (kann) Color.White else TextSanft, fontSize = 12.sp)
                    Text(
                        when {
                            traegt -> "abnehmen"
                            habe -> "aufsetzen"
                            else -> "${h.preis} 🪙"
                        },
                        color = TextSanft, fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

/** Olli erklärt beim ersten Besuch das Haustier, danach gibt sie Tipps. Antippen zeigt den nächsten. */
@Composable
private fun OlliBlase(vm: AppViewModel, tier: Haustier, muenzen: Int, heute: Long, schlaeft: Boolean) {
    val einfuehrung = remember(tier.name, tier.stufe == HaustierStufe.EI) { Eule.einfuehrung(tier) }
    var schritt by remember { mutableStateOf(if (vm.fortschritt.euleEinfuehrung) -1 else 0) }
    var nummer by remember { mutableStateOf(0) }
    val tipps = remember(tier, muenzen, heute, vm.familie.aktiv, schlaeft) {
        Eule.tipps(tier, muenzen, vm.familie.aktiv, heute, schlaeft)
    }
    val text = if (schritt >= 0) einfuehrung[schritt] else tipps[Math.floorMod(nummer, tipps.size)]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .clip(RoundedCornerShape(18.dp)).glas(18.dp)
                .clickable { if (schritt < 0) nummer++ }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(Eule.BILD, fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text, color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "🔊 Vorlesen", color = Gelb, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { vm.sprecher.sprich(Eule.sprechbar(text)) },
                )
            }
        }
        if (schritt >= 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (schritt < einfuehrung.lastIndex) "Weiter (${schritt + 1} von ${einfuehrung.size})" else "Los geht's!",
                color = Color(0xFF00205B), fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(50)).background(Gelb)
                    .clickable {
                        if (schritt < einfuehrung.lastIndex) schritt++
                        else { schritt = -1; vm.euleEinfuehrungGesehen() }
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}
