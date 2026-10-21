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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
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
import de.cemaras.yem1n.logik.ErgebnisUebersicht
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ErgebnisseElternScreen(vm: AppViewModel) {
    var kind by remember { mutableStateOf<String?>(null) }
    val alle = vm.ergebnisse
    val liste = ErgebnisUebersicht.fuerKind(alle, kind)
    val u = ErgebnisUebersicht.von(liste)
    val kinder = ErgebnisUebersicht.von(alle).proKind.map { it.first }
    val format = remember { SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY) }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text("📊 Ergebnisse", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        if (kinder.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val farben = FilterChipDefaults.filterChipColors(
                    labelColor = Color.White, selectedContainerColor = Gelb, selectedLabelColor = Navy,
                )
                FilterChip(selected = kind == null, onClick = { kind = null }, label = { Text("Alle") }, colors = farben)
                kinder.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k) }, colors = farben)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Kennzahl2("${u.runden}", "Runden")
            Kennzahl2("${u.prozent} %", "richtig")
            Kennzahl2("${u.sterne} ⭐", "Sterne")
        }

        OutlinedButton(onClick = vm::ergebnisseLaden, enabled = !vm.ergebnisseBeschaeftigt) {
            Text(if (vm.ergebnisseBeschaeftigt) "Einen Moment..." else "Neu laden", color = Color.White)
        }
        if (vm.ergebnisseMeldung.isNotEmpty()) Text(vm.ergebnisseMeldung, color = TextSanft, fontSize = 14.sp)

        liste.forEach { e ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).glas(18.dp).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.uebung.ifBlank { "Runde" }, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("⭐".repeat(e.sterne).ifEmpty { "·" }, fontSize = 16.sp)
                }
                Text("${e.richtig} von ${e.gesamt} richtig" + if (e.angesehen > 0) ", ${e.angesehen} Lösung gezeigt" else "",
                    color = Mint, fontSize = 14.sp)
                Text(
                    listOf(e.kind.ifBlank { "Kind" }, e.fach, e.arbeit).filter { it.isNotBlank() }.joinToString(" · ") +
                        "  ·  " + format.format(Date(e.zeitpunkt)),
                    color = TextSanft, fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Kennzahl2(wert: String, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(wert, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text(text, color = TextSanft, fontSize = 13.sp)
    }
}
