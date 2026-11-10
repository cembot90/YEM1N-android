package de.cemaras.yem1n.ui

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import de.cemaras.yem1n.logik.SpielArt
import de.cemaras.yem1n.logik.SpielRegeln
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.glas
import de.cemaras.yem1n.ui.theme.feder
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft

/** Die Spiele-Übersicht: Münzen, Spiele und der Grund, falls gerade nicht gespielt werden darf. */
@Composable
fun SpieleScreen(vm: AppViewModel) {
    val f = vm.fortschritt
    val sperre = vm.spielSperre
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("Spiele", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Gelb)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).glas(20.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("🪙 ${f.muenzen} Münzen", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                "Ein Spiel kostet ${SpielRegeln.PREIS} Münzen. Münzen bekommst du fürs Üben.",
                color = TextSanft, fontSize = 14.sp,
            )
        }
        SpielArt.entries.forEach { art ->
            val bestwert = f.bestwerte[art.schluessel] ?: 0
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).glas(24.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(art.emoji, fontSize = 40.sp)
                    Spacer(Modifier.padding(start = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(art.titel, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        if (bestwert > 0) Text("Bestwert: $bestwert", fontSize = 13.sp, color = Gelb)
                    }
                }
                Text(art.beschreibung, fontSize = 14.sp, color = TextSanft)
                if (art.verfuegbar) {
                    val quelle = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        onClick = { vm.startSpiel(art) },
                        enabled = sperre == null,
                        interactionSource = quelle,
                        modifier = Modifier.fillMaxWidth().height(50.dp).feder(quelle),
                    ) {
                        Text(
                            if (sperre == null) "Spielen für ${SpielRegeln.PREIS} Münzen" else "Noch nicht möglich",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Text("Kommt bald", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Gelb)
                }
            }
        }
        if (sperre != null) {
            Text(
                sperre, color = Gelb, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).glas(20.dp).padding(16.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Das Spiel selbst, mit Schließen-Knopf und Bestwert. */
@Composable
fun SpielScreen(vm: AppViewModel, art: SpielArt) {
    val bestwert = vm.fortschritt.bestwerte[art.schluessel] ?: 0
    QuerformatSperre()
    Box(Modifier.fillMaxSize()) {
        when (art) {
            SpielArt.EINHORN -> EinhornScreen(onEnde = { punkte -> vm.spielEnde(art, punkte) })
            SpielArt.LAUF -> LaufScreen(onEnde = { punkte -> vm.spielEnde(art, punkte) })
            SpielArt.ELFMETER -> ElfmeterScreen(onEnde = { punkte -> vm.spielEnde(art, punkte) })
            SpielArt.JAGD -> JagdScreen(onEnde = { punkte -> vm.spielEnde(art, punkte) })
            SpielArt.HOCKEY -> HockeyScreen(onEnde = { punkte -> vm.spielEnde(art, punkte) })
        }
        Column(
            Modifier.align(Alignment.TopEnd).systemBarsPadding().padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Gelb).clickable(onClick = vm::zurueck),
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = Navy, fontWeight = FontWeight.Black, fontSize = 18.sp) }
            if (bestwert > 0) {
                Text(
                    if (vm.rekord) "Neuer Rekord: $bestwert" else "Bestwert: $bestwert",
                    fontWeight = FontWeight.Black, fontSize = 13.sp,
                    color = if (vm.rekord) Navy else Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (vm.rekord) Gelb else Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/** Die Spiele sind fürs Querformat gemacht. Solange eins läuft, dreht sich der Bildschirm so, danach wieder frei. */
@Composable
private fun QuerformatSperre() {
    val kontext = LocalContext.current
    DisposableEffect(Unit) {
        val aktivitaet = generateSequence(kontext) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
        aktivitaet?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { aktivitaet?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}
