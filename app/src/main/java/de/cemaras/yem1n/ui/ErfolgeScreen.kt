package de.cemaras.yem1n.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import de.cemaras.yem1n.logik.Erfolge
import de.cemaras.yem1n.logik.Tag
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.Fortschrittsring
import de.cemaras.yem1n.ui.theme.auswahl
import de.cemaras.yem1n.ui.theme.glas
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ErfolgeScreen(vm: AppViewModel) {
    val f = vm.fortschritt
    val heute = Tag.heute()
    val (serie, gerettet) = f.serie(heute)
    val beste = Erfolge.besteSerie(f.tage)
    val rundenHeute = f.rundenHeute(heute)
    val ziel = f.tagesziel
    val wochenRunden = Erfolge.wochenRunden(f, heute)
    val wochenZiel = Erfolge.wochenziel(ziel)
    val holbar = Erfolge.belohnungAbholbar(f, heute)
    var belohnungInfo by remember { mutableStateOf<String?>(null) }
    val pokale = Erfolge.pokale(f)

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        val name = f.name.trim()
        Text(
            if (name.isEmpty()) "Meine Erfolge" else "Erfolge von $name",
            fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White,
        )

        KarteMit {
            Text("🔥", fontSize = 44.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (serie == 1) "1 Tag in Folge" else "$serie Tage in Folge",
                    fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White,
                )
                Text("Beste Serie: $beste", fontSize = 14.sp, color = TextSanft)
                Text(
                    if (gerettet > 0) "🧊 Ein freier Tag hat deine Serie gerettet. Der nächste ist erst in einer Woche wieder frei."
                    else "🧊 Ein freier Tag pro Woche ist erlaubt, ohne dass die Serie endet.",
                    fontSize = 13.sp, color = TextSanft,
                )
            }
        }

        KarteMit {
            Ring(rundenHeute.toFloat() / maxOf(ziel, 1), "$rundenHeute/$ziel")
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Tagesziel", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                val rest = ziel - rundenHeute
                Text(
                    if (rest <= 0) "Geschafft! 🎉" else "Noch $rest ${if (rest == 1) "Runde" else "Runden"}",
                    fontSize = 14.sp, color = TextSanft,
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(wochenRunden.toFloat() / maxOf(wochenZiel, 1), "$wochenRunden/$wochenZiel")
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Wochenziel", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    val rest = wochenZiel - wochenRunden
                    Text(
                        if (rest <= 0) "Geschafft! 🎉"
                        else "Noch $rest ${if (rest == 1) "Runde" else "Runden"} bis Sonntag",
                        fontSize = 14.sp, color = TextSanft,
                    )
                }
            }
            if (holbar) {
                Button(
                    onClick = {
                        val m = vm.holeWochenBelohnung()
                        if (m > 0) belohnungInfo = "+$m Münzen"
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("🎁 Belohnung abholen: ${Erfolge.WOCHEN_BELOHNUNG} Münzen", fontWeight = FontWeight.Bold) }
            } else if (belohnungInfo != null) {
                Text("$belohnungInfo auf deinem Konto. Bis nächste Woche!", color = Mint, fontWeight = FontWeight.Bold)
            }
        }

        val geschafft = pokale.count { it.erreicht }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Abzeichen: $geschafft von ${pokale.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pokale.forEach { p ->
                    Column(
                        Modifier
                            .width(96.dp)
                            .height(96.dp)
                            .auswahl(p.erreicht, 16.dp)
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(p.emoji, fontSize = 30.sp, modifier = Modifier.alpha(if (p.erreicht) 1f else 0.25f))
                        Text(
                            p.titel, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            color = if (p.erreicht) Color.White else TextSanft,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun KarteMit(inhalt: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { inhalt() }
}

@Composable
private fun Ring(anteil: Float, text: String) {
    Fortschrittsring(anteil, groesse = 64.dp, strich = 7.dp) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}
