package de.cemaras.yem1n.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import de.cemaras.yem1n.logik.AntwortProtokoll
import de.cemaras.yem1n.logik.ErgebnisUebersicht
import de.cemaras.yem1n.logik.ProblemAufgabe
import de.cemaras.yem1n.logik.RundenErgebnisEintrag
import de.cemaras.yem1n.logik.Schwaeche
import de.cemaras.yem1n.logik.Tag
import de.cemaras.yem1n.ui.theme.Koralle
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
    var tage by remember { mutableStateOf(7) }
    var offen by remember { mutableStateOf<String?>(null) }
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

        val imZeitraum = AntwortProtokoll.imZeitraum(liste, tage, Tag.heute())
        RadarKarte(imZeitraum, tage) { tage = it }
        ProblemKarte(AntwortProtokoll.problemAufgaben(imZeitraum))
        ZuletztKarte(AntwortProtokoll.zuletztGeuebt(alle), format)

        OutlinedButton(onClick = vm::ergebnisseLaden, enabled = !vm.ergebnisseBeschaeftigt) {
            Text(if (vm.ergebnisseBeschaeftigt) "Einen Moment..." else "Neu laden", color = Color.White)
        }
        if (vm.ergebnisseMeldung.isNotEmpty()) Text(vm.ergebnisseMeldung, color = TextSanft, fontSize = 14.sp)

        liste.forEach { e ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).glas(18.dp)
                    .clickable { offen = if (offen == e.eintragId) null else e.eintragId }
                    .padding(14.dp),
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
                if (offen == e.eintragId) AntwortenListe(e)
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

@Composable
private fun AntwortenListe(e: RundenErgebnisEintrag) {
    val liste = AntwortProtokoll.liste(e.details)
    if (liste.isEmpty()) {
        Text("Zu dieser Runde gibt es keine einzelnen Antworten.", color = TextSanft, fontSize = 13.sp)
        return
    }
    Spacer(Modifier.height(6.dp))
    liste.forEach { d ->
        val rechnung = AntwortProtokoll.einzeilig(d.rechnung)
        val (zeichen, text, farbe) = when (d.stand) {
            AntwortProtokoll.RICHTIG -> Triple("✅", "$rechnung ${d.eingabe}".trim(), Mint)
            AntwortProtokoll.FALSCH -> Triple("❌", "$rechnung geschrieben: ${AntwortProtokoll.einzeilig(d.eingabe).ifEmpty { "nichts" }}, richtig: ${d.loesung}", Koralle)
            AntwortProtokoll.ANGESCHAUT -> Triple("👀", "$rechnung Lösung angeschaut: ${d.loesung}", Gelb)
            else -> Triple("▫️", "$rechnung nicht beantwortet", TextSanft)
        }
        Text("$zeichen $text", color = farbe, fontSize = 13.sp)
    }
}

private fun schwaecheFarbe(anteil: Double): Color = when {
    anteil < 0.6 -> Koralle
    anteil < 0.8 -> Gelb
    else -> Mint
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RadarKarte(runden: List<RundenErgebnisEintrag>, tage: Int, onTage: (Int) -> Unit) {
    val schwach = AntwortProtokoll.schwaechste(runden)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Schwächen-Radar", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val farben = FilterChipDefaults.filterChipColors(
                labelColor = Color.White, selectedContainerColor = Gelb, selectedLabelColor = Navy,
            )
            listOf(7 to "7 Tage", 30 to "30 Tage", 0 to "Alles").forEach { (t, name) ->
                FilterChip(selected = tage == t, onClick = { onTage(t) }, label = { Text(name) }, colors = farben)
            }
        }
        if (schwach.isEmpty()) {
            Text(
                if (runden.isEmpty()) "Noch keine Daten in diesem Zeitraum." else "Keine Schwächen gefunden. Alles sitzt.",
                color = TextSanft, fontSize = 14.sp,
            )
        } else {
            schwach.forEach { SchwaecheZeile(it) }
            Text(
                "Tipp: Übt als Nächstes ${schwach.first().titel}. Das Training auf dem Startbildschirm wählt die schwächsten Übungen automatisch.",
                color = Color.White, fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SchwaecheZeile(e: Schwaeche) {
    val farbe = schwaecheFarbe(e.anteil)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(e.titel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("${Math.round(e.anteil * 100)} %", color = farbe, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.14f)),
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth(e.anteil.toFloat().coerceIn(0.02f, 1f)).height(7.dp)
                    .clip(RoundedCornerShape(4.dp)).background(farbe),
            )
        }
        var t = "${e.runden} ${if (e.runden == 1) "Runde" else "Runden"}"
        if (e.angesehen > 0) t += ", ${e.angesehen} Lösungen angeschaut"
        if (e.trend > 0) t += ", wird besser"
        if (e.trend < 0) t += ", zuletzt schlechter"
        Text(t, color = TextSanft, fontSize = 12.sp)
    }
}

@Composable
private fun ProblemKarte(probleme: List<ProblemAufgabe>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Aufgaben, die haken", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        if (probleme.isEmpty()) {
            Text("Keine Fehler in diesem Zeitraum, oder die Runden haben noch keine einzelnen Antworten.", color = TextSanft, fontSize = 14.sp)
        }
        probleme.forEach { p ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(AntwortProtokoll.einzeilig(p.rechnung).ifEmpty { p.frage }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${p.uebung}, richtig: ${p.loesung}", color = Mint, fontSize = 13.sp)
                var t = ""
                if (p.falsch > 0) t += "${p.falsch}x falsch"
                if (p.angeschaut > 0) t += (if (t.isEmpty()) "" else ", ") + "${p.angeschaut}x Lösung angeschaut"
                t += if (p.zuletztRichtig) ", zuletzt richtig" else ", noch nicht sicher"
                Text(t, color = TextSanft, fontSize = 12.sp)
                if (p.falscheEingaben.isNotEmpty()) {
                    Text("Geschrieben: " + p.falscheEingaben.take(3).joinToString(", "), color = Koralle, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ZuletztKarte(kinder: List<Pair<String, Long>>, format: SimpleDateFormat) {
    if (kinder.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Kinder in der Familie", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        kinder.forEach { (name, zeit) ->
            Row {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("zuletzt geübt " + format.format(Date(zeit)), color = TextSanft, fontSize = 13.sp)
            }
        }
    }
}
