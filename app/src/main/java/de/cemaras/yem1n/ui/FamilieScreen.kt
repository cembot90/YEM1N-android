package de.cemaras.yem1n.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.logik.FamilienGeraet
import de.cemaras.yem1n.logik.FamilienZugang
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.glas
import kotlinx.coroutines.delay

@Composable
private fun GlasKarte(inhalt: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).glas(22.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = inhalt,
    )
}

private fun rolleText(rolle: String) = if (rolle == "eltern") "Eltern" else "Kind"

/**
 * Die Familie dieses Geräts: anlegen, mit QR-Code beitreten, auf die Freigabe warten, Geräte verwalten (Eltern),
 * verlassen oder alles löschen. Keine Codes zum Abtippen, jedes neue Gerät muss von den Eltern erlaubt werden.
 */
@Composable
fun FamilieScreen(vm: AppViewModel) {
    val fam = vm.familie
    val z = fam.zugang

    // Solange die Seite offen ist: ein wartendes Gerät fragt nach der Freigabe, Eltern sehen neue Geräte.
    LaunchedEffect(z != null, vm.einwilligungOffen) {
        while (vm.familie.zugang != null && !vm.einwilligungOffen) {
            delay(if (vm.familie.wartet) 4_000L else 15_000L)
            vm.familieAktualisieren()
        }
    }

    // Ab Android 13 einmal um die Erlaubnis für Mitteilungen bitten, sobald das Gerät zur Familie gehört.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val erlaubnis = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(fam.aktiv) {
        if (fam.aktiv && !de.cemaras.yem1n.speicher.Erinnerung.erlaubt(ctx)) {
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
        Text("👨‍👩‍👧 Familie", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        if (vm.einwilligungOffen) {
            EinwilligungKarte(vm)
            return@Column
        }

        // Gehört das Gerät zur Familie, kommt man nur mit der Eltern-PIN weiter (die erste PIN legen die Eltern jetzt fest).
        if (!PinTor(vm)) return@Column

        when {
            z == null -> NichtVerbunden(vm)
            z.wartet -> Wartet(vm, z)
            !z.aktiv -> Abgelehnt(vm)
            else -> Verbunden(vm, z)
        }

        if (fam.meldung.isNotEmpty()) Text(fam.meldung, color = Koralle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NichtVerbunden(vm: AppViewModel) {
    val fam = vm.familie
    var name by remember { mutableStateOf(vm.fortschritt.name) }
    var qrFehler by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Text(
        "Hier verbindest du dieses Gerät mit deiner Familie. Dann gehen Joker, Duelle und Ergebnisse hin und her. " +
            "Ohne Familie bleibt alles nur auf diesem Gerät.",
        color = TextSanft, fontSize = 16.sp,
    )
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(40); qrFehler = false },
        label = { Text("Name dieses Geräts") },
        supportingText = { Text("Kind: der Vorname des Kindes. Eltern: zum Beispiel Mama oder Papa.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    GlasKarte {
        Text("Ich bin Mama oder Papa", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text("Du legst die Familie an. Dieses Gerät wird das erste Eltern-Gerät.", color = Color.White, fontSize = 15.sp)
        Button(
            onClick = { vm.familieAnlegen(name) },
            enabled = name.isNotBlank() && !fam.beschaeftigt,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (fam.beschaeftigt) "Einen Moment ..." else "Neue Familie anlegen", fontWeight = FontWeight.Bold) }
    }

    GlasKarte {
        Text("Meine Familie gibt es schon", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text(
            "Auf einem Eltern-Gerät steht unter Familie > Gerät hinzufügen ein QR-Code. Scanne ihn hier. " +
                "Danach müssen die Eltern dieses Gerät noch erlauben.",
            color = Color.White, fontSize = 15.sp,
        )
        OutlinedButton(
            onClick = {
                qrFehler = false
                qrScannen(ctx, { roh -> vm.familieBeitreten(roh, name) { f -> qrFehler = f != null } }, { qrFehler = true })
            },
            enabled = name.isNotBlank() && !fam.beschaeftigt,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("📷 QR-Code scannen", color = Color.White, fontWeight = FontWeight.Bold) }
        if (qrFehler && fam.meldung.isEmpty()) {
            Text("Das hat nicht geklappt. Bitte noch einmal scannen.", color = Koralle, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Wartet(vm: AppViewModel, z: FamilienZugang) {
    GlasKarte {
        Text("Fast geschafft!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text("Dieses Gerät wartet darauf, dass Mama oder Papa es erlaubt.", color = Color.White, fontSize = 16.sp)
        Text("Prüfcode", color = TextSanft, fontSize = 13.sp)
        Text(z.pruefcode, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(
            "Dieser Code steht auch auf dem Eltern-Gerät in der Liste. Die Eltern erlauben nur, wenn er gleich ist.",
            color = TextSanft, fontSize = 13.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::familieAktualisieren) { Text("Nachsehen", color = Color.White) }
            TextButton(onClick = { vm.familieVerlassen(false) }) { Text("Abbrechen", color = TextSanft) }
        }
    }
}

@Composable
private fun Abgelehnt(vm: AppViewModel) {
    GlasKarte {
        Text("Nicht erlaubt", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Koralle)
        Text(
            "Die Eltern haben dieses Gerät nicht erlaubt. Wenn das ein Versehen war, bitte sie um einen neuen QR-Code.",
            color = Color.White, fontSize = 16.sp,
        )
        Button(onClick = { vm.familieVerlassen(false) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("OK") }
    }
}

@Composable
private fun Verbunden(vm: AppViewModel, z: FamilienZugang) {
    val fam = vm.familie
    val eltern = z.istEltern
    var verlassenOffen by remember { mutableStateOf(false) }
    var loeschenOffen by remember { mutableStateOf(false) }

    GlasKarte {
        Text("Dieses Gerät", color = TextSanft, fontSize = 13.sp)
        Text(z.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("${rolleText(z.rolle)}, Prüfcode ${z.pruefcode}", color = Mint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }

    if (eltern) {
        GeraeteKarte(vm)
        EinladenKarte(vm)
    } else {
        Text(
            "Dieses Gerät gehört zur Familie. Fragen, Duelle und fertige Runden gehen nur an die Familie.",
            color = TextSanft, fontSize = 14.sp,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = vm::oeffneJoker) { Text("🆘 Joker") }
        if (eltern) Button(onClick = vm::oeffneErgebnisse) { Text("📊 Ergebnisse") }
    }

    GlasKarte {
        Text("Verlassen und löschen", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
        OutlinedButton(onClick = { verlassenOffen = true }, enabled = !fam.beschaeftigt) {
            Text("Dieses Gerät aus der Familie nehmen", color = Color.White)
        }
        if (eltern) {
            OutlinedButton(onClick = { loeschenOffen = true }, enabled = !fam.beschaeftigt) {
                Text("Ganze Familie löschen", color = Koralle)
            }
            Text(
                "Löscht alle Einträge in der Cloud und entfernt alle Geräte. Das geht nicht rückgängig.",
                color = TextSanft, fontSize = 13.sp,
            )
        }
    }

    if (verlassenOffen) VerlassenDialog(vm, eltern) { verlassenOffen = false }
    if (loeschenOffen) {
        AlertDialog(
            onDismissRequest = { loeschenOffen = false },
            title = { Text("Ganze Familie löschen?") },
            text = {
                Text(
                    "Alle Fragen, Antworten, Ergebnisse und Duelle werden in der Cloud gelöscht. " +
                        "Alle Geräte der Familie werden entfernt. Das geht nicht rückgängig.",
                )
            },
            confirmButton = {
                TextButton(onClick = { loeschenOffen = false; vm.familieLoeschen() }) { Text("Ja, alles löschen", color = Koralle) }
            },
            dismissButton = { TextButton(onClick = { loeschenOffen = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun VerlassenDialog(vm: AppViewModel, eltern: Boolean, zu: () -> Unit) {
    var loeschen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = zu,
        title = { Text("Gerät aus der Familie nehmen?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Danach gehen von diesem Gerät keine Fragen und Ergebnisse mehr an die Familie. " +
                        "Zum Wiederverbinden braucht es einen neuen QR-Code und die Freigabe der Eltern.",
                )
                if (!eltern) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = loeschen, onCheckedChange = { loeschen = it })
                        Text("Meine Einträge in der Cloud auch löschen")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { zu(); vm.familieVerlassen(loeschen && !eltern) }) { Text("Ja, Gerät entfernen", color = Koralle) }
        },
        dismissButton = { TextButton(onClick = zu) { Text("Abbrechen") } },
    )
}

@Composable
private fun GeraeteKarte(vm: AppViewModel) {
    val fam = vm.familie
    var ziel by remember { mutableStateOf<FamilienGeraet?>(null) }
    val liste = fam.geraete
        .filter { it.status == "aktiv" || it.status == "wartet" }
        .sortedWith(compareBy({ it.status != "wartet" }, { it.name.lowercase() }))

    GlasKarte {
        Text("Geräte der Familie", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        if (liste.isEmpty()) Text("Noch keine Geräte geladen.", color = TextSanft, fontSize = 14.sp)
        liste.forEach { g ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).glas(14.dp).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        g.name + if (g.ich) " (dieses Gerät)" else "",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(rolleText(g.rolle), color = Mint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Prüfcode ${g.pruefcode}", color = TextSanft, fontSize = 13.sp)
                if (g.status == "wartet") {
                    Text("Wartet auf deine Freigabe. Ist der Prüfcode auf dem neuen Gerät genau gleich?", color = Color.White, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.familieGeraetErlauben(g.id) }, enabled = !fam.beschaeftigt) { Text("Erlauben") }
                        TextButton(onClick = { vm.familieGeraetAblehnen(g.id) }, enabled = !fam.beschaeftigt) {
                            Text("Ablehnen", color = Koralle)
                        }
                    }
                } else if (!g.ich) {
                    TextButton(onClick = { ziel = g }, enabled = !fam.beschaeftigt) { Text("Entfernen", color = Koralle) }
                }
            }
        }
        OutlinedButton(onClick = vm::familieAktualisieren) { Text("Neu laden", color = Color.White) }
    }

    ziel?.let { g ->
        AlertDialog(
            onDismissRequest = { ziel = null },
            title = { Text("${g.name} entfernen?") },
            text = { Text("Das Gerät gehört dann nicht mehr zur Familie. Zum Wiederverbinden braucht es einen neuen QR-Code.") },
            confirmButton = {
                TextButton(onClick = { ziel = null; vm.familieGeraetEntfernen(g.id) }) { Text("Ja, entfernen", color = Koralle) }
            },
            dismissButton = { TextButton(onClick = { ziel = null }) { Text("Abbrechen") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EinladenKarte(vm: AppViewModel) {
    val fam = vm.familie
    GlasKarte {
        Text("Gerät hinzufügen", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
        Text(
            "Das neue Gerät scannt einen QR-Code. Danach erscheint es oben in der Liste und du erlaubst es. " +
                "Der QR-Code gilt 10 Minuten und nur für ein Gerät.",
            color = Color.White, fontSize = 14.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = { vm.familieEinladen("kind") }, enabled = !fam.beschaeftigt) { Text("📱 Kind-Gerät") }
            Button(onClick = { vm.familieEinladen("eltern") }, enabled = !fam.beschaeftigt) { Text("👨‍👩‍👧 Eltern-Gerät") }
        }
    }
    fam.einladung?.let { e ->
        QrDialog(
            if (e.rolle == "eltern") "Neues Eltern-Gerät" else "Neues Kind-Gerät",
            e.qr,
            "Auf dem neuen Gerät unter Familie auf QR-Code scannen tippen. Gilt 10 Minuten, nur einmal.",
        ) { fam.einladungSchliessen(); vm.familieAktualisieren() }
    }
}
