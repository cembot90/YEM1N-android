package de.cemaras.yem1n.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.logik.Tag
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.glas
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Navy
import de.cemaras.yem1n.ui.theme.TextSanft
import de.cemaras.yem1n.ui.theme.TiefNavy
import de.cemaras.yem1n.ui.theme.feder

/** Wurzel der App: Hintergrund, Zurück-Taste und die drei Bildschirme. */
@Composable
fun App(vm: AppViewModel) {
    BackHandler(enabled = vm.ansicht != Ansicht.Start) { vm.zurueck() }
    val ansichtView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(ansichtView) {
        SpielEffekte.ansicht = ansichtView
        onDispose { SpielEffekte.ansicht = null }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, TiefNavy))),
    ) {
        de.cemaras.yem1n.ui.theme.LichtFlecken()
        vm.meldung?.let {
            AlertDialog(
                onDismissRequest = vm::meldungGelesen,
                text = { Text(it) },
                confirmButton = { TextButton(onClick = vm::meldungGelesen) { Text("OK") } },
            )
        }
        if (vm.zeigeEinrichtung) {
            EinrichtungScreen(vm)
            return@Box
        }
        when (vm.ansicht) {
            Ansicht.Start -> StartScreen(vm)
            is Ansicht.Uebungen -> UebungenScreen(vm)
            is Ansicht.Ueben -> UebenScreen(vm)
            Ansicht.Einstellungen -> EinstellungenScreen(vm)
            Ansicht.Erfolge -> ErfolgeScreen(vm)
            Ansicht.Info -> InfoScreen(vm)
            Ansicht.Vorschule -> VorschulScreen(vm)
            Ansicht.Spiele -> SpieleScreen(vm)
            is Ansicht.VorschulPakete -> VorschulPaketScreen(vm, (vm.ansicht as Ansicht.VorschulPakete).arbeit)
            Ansicht.HaustierSeite -> HaustierScreen(vm)
            Ansicht.Joker -> JokerScreen(vm)
            Ansicht.Duell -> DuellScreen(vm)
            Ansicht.Lernwelt -> LernweltScreen(vm)
            Ansicht.Sprachen -> SprachenScreen(vm)
            Ansicht.Editor -> EditorScreen(vm)
            Ansicht.VokabelEditor -> VokabelEditorScreen(vm)
            Ansicht.ErgebnisseEltern -> ErgebnisseElternScreen(vm)
            is Ansicht.Spiel -> SpielScreen(vm, (vm.ansicht as Ansicht.Spiel).art)
            is Ansicht.VorschulSpiel -> VorschulSpielScreen(vm, (vm.ansicht as Ansicht.VorschulSpiel).art)
            is Ansicht.Englisch -> EnglischScreen(vm, (vm.ansicht as Ansicht.Englisch).thema)
            is Ansicht.InfoSeite -> InfoSeiteScreen(vm, (vm.ansicht as Ansicht.InfoSeite).seite)
        }
    }
}

internal fun sterneText(n: Int): String = "★".repeat(n.coerceIn(0, 3)) + "☆".repeat(3 - n.coerceIn(0, 3))

@Composable
internal fun KartenZeile(onClick: () -> Unit, inhalt: @Composable RowScope.() -> Unit) {
    val quelle = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .feder(quelle)
            .glas(18.dp)
            .clickable(interactionSource = quelle, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { inhalt() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartScreen(vm: AppViewModel) {
    var einfuegen by remember { mutableStateOf(false) }
    var dateiMeldung by remember { mutableStateOf<String?>(null) }
    val kontext = LocalContext.current
    val oeffner = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                kontext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
            dateiMeldung = if (text == null) "Die Datei konnte nicht gelesen werden."
            else vm.fuegeArbeitEin(text) ?: "Die Arbeit wurde importiert."
        }
    }
    val heute = Tag.heute()
    val (serie, _) = vm.fortschritt.serie(heute)
    val runden = vm.fortschritt.rundenHeute(heute)
    val ziel = vm.fortschritt.tagesziel
    val (faellig, wartend) = vm.fehlerStand
    val sichtbar = vm.arbeiten.withIndex().filter { vm.fachFilter == null || it.value.fach == vm.fachFilter }
    var menue by remember { mutableStateOf(false) }
    val klassen = sichtbar.map { it.value.klasse }.distinct().sorted()
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YEM1N", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Gelb, letterSpacing = 3.sp)
                    Text("Schule", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Color.White)
                    val gruss = vm.fortschritt.name.trim().let {
                        if (it.isEmpty()) "Such dir eine Übung aus" else "Hallo $it! Such dir eine Übung aus"
                    }
                    Text(gruss, color = TextSanft, fontSize = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RundKnopf("🎮") { vm.oeffneSpiele() }
                    RundKnopf("🏆") { vm.oeffneErfolge() }
                    Box {
                        RundKnopf("＋", betont = true) { menue = true }
                        androidx.compose.material3.DropdownMenu(expanded = menue, onDismissRequest = { menue = false }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Arbeit einfügen") }, onClick = { menue = false; einfuegen = true })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Eigene Arbeit bauen") }, onClick = { menue = false; vm.oeffneEditor() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Datei öffnen") }, onClick = { menue = false; oeffner.launch(arrayOf("*/*")) })
                            if (vm.fortschritt.klassencode.isNotEmpty()) {
                                androidx.compose.material3.DropdownMenuItem(text = { Text("Neue Aufgaben der Klasse holen") }, onClick = { menue = false; vm.klassenPaketeHolen() })
                            }
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Einstellungen") }, onClick = { menue = false; vm.oeffneEinstellungen() })
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .glas(18.dp)
                    .clickable(onClick = vm::oeffneErfolge)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Kennzahl("🔥", "$serie", if (serie == 1) "Tag" else "Tage")
                Kennzahl("🪙", "${vm.fortschritt.muenzen}", "Münzen")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    de.cemaras.yem1n.ui.theme.Fortschrittsring(
                        anteil = if (ziel > 0) minOf(runden, ziel).toFloat() / ziel else 1f,
                        groesse = 52.dp, strich = 6.dp,
                    ) { Text("🎯", fontSize = 18.sp) }
                    Text("${minOf(runden, ziel)} / $ziel heute", color = TextSanft, fontSize = 13.sp)
                }
            }
        }
        item {
            KartenZeile(onClick = vm::oeffneLernwelt) {
                Text("🌍", fontSize = 32.sp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lernwelt", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                    Text("Fertige Aufgaben nach dem Lehrplan", fontSize = 14.sp, color = TextSanft)
                }
                Text("›", color = Gelb, fontSize = 26.sp)
            }
        }
        if (vm.fortschritt.sprachenAn) {
            item {
                KartenZeile(onClick = vm::oeffneSprachen) {
                    Text("🗣️", fontSize = 32.sp)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sprachen", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("Englisch, Türkisch, Italienisch", fontSize = 14.sp, color = TextSanft)
                    }
                    Text("›", color = Gelb, fontSize = 26.sp)
                }
            }
        }
        item { Titel("Extra") }
        item { FehlerheftKnopf(faellig, wartend) { vm.startFehlerheft() } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SonderKnopf("📝", "Probearbeit", "30 Minuten", Modifier.weight(1f)) { vm.startProbe() }
                SonderKnopf("🎯", "Training", "deine Schwächen", Modifier.weight(1f)) { vm.startTraining() }
            }
        }
        item { Titel("Mehr") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tier = vm.haustier
                SonderKnopf(
                    tier?.emoji ?: "🥚", if (tier == null) "Haustier" else tier.name,
                    if (tier == null) "such dir eins aus" else tier.stufe.titel, Modifier.weight(1f),
                ) { vm.oeffneHaustier() }
                SonderKnopf("🆘", "Joker", "Frag deine Eltern", Modifier.weight(1f)) { vm.oeffneJoker() }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SonderKnopf("⚔️", "Duell", "Gegen die Familie", Modifier.weight(1f)) { vm.oeffneDuell() }
                if (vm.fortschritt.vorschulAn) {
                    SonderKnopf("🧸", "Vorschule", "für die Kleinen", Modifier.weight(1f)) { vm.oeffneVorschule() }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        val faecher = vm.faecher
        if (faecher.size > 1) {
            item {
                Row(
                    Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = vm.fachFilter == null, onClick = { vm.waehleFach(null) }, label = { Text("Alle") }, colors = chipFarben())
                    faecher.forEach { f ->
                        FilterChip(selected = vm.fachFilter == f, onClick = { vm.waehleFach(f) }, label = { Text(f) }, colors = chipFarben())
                    }
                }
            }
        }
        if (sichtbar.isEmpty()) {
            item {
                Text("Noch keine Klassenarbeit. Über das Plus oben lassen sich Arbeiten einfügen.", color = TextSanft, fontSize = 15.sp)
            }
        }
        for (klasse in klassen) {
            item(key = "k-$klasse") {
                Text(klasse, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 10.dp))
            }
            for (fach in sichtbar.filter { it.value.klasse == klasse }.map { it.value.fach }.distinct()) {
                item(key = "f-$klasse-$fach") {
                    Text("📘 $fach", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gelb)
                }
                items(sichtbar.filter { it.value.klasse == klasse && it.value.fach == fach }, key = { it.value.schluessel }) { (i, arbeit) ->
                    ArbeitKarte(vm, arbeit) { vm.oeffne(i) }
                }
            }
        }
    }
    if (einfuegen) {
        EinfuegenDialog(vm) { einfuegen = false }
    }
    dateiMeldung?.let {
        AlertDialog(
            onDismissRequest = { dateiMeldung = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { dateiMeldung = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun SonderKnopf(symbol: String, titel: String, untertitel: String, modifier: Modifier, onClick: () -> Unit) {
    val quelle = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier
            .feder(quelle)
            .glas(18.dp)
            .clickable(interactionSource = quelle, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, fontSize = 24.sp)
        Spacer(Modifier.size(10.dp))
        Column {
            Text(titel, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text(untertitel, fontSize = 13.sp, color = TextSanft)
        }
    }
}

@Composable
private fun chipFarben() = FilterChipDefaults.filterChipColors(
    labelColor = Color.White,
    selectedContainerColor = Gelb,
    selectedLabelColor = Navy,
)

@Composable
private fun Kennzahl(symbol: String, wert: String, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$symbol $wert", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(text, color = TextSanft, fontSize = 13.sp)
    }
}

@Composable
private fun FehlerheftKnopf(faellig: Int, wartend: Int, onClick: () -> Unit) {
    val untertitel = when {
        faellig > 0 -> "$faellig ${if (faellig == 1) "Fehler ist" else "Fehler sind"} dran"
        wartend > 0 -> "Alles erledigt, $wartend kommen in ein paar Tagen wieder"
        else -> "Noch keine Fehler. Super!"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (faellig > 0) Gelb else Karte)
            .clickable(enabled = faellig > 0, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📓", fontSize = 26.sp)
        Spacer(Modifier.size(12.dp))
        Column {
            val farbe = if (faellig > 0) Navy else Color.White
            Text("Fehlerheft", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = farbe)
            Text(untertitel, fontSize = 14.sp, color = if (faellig > 0) Navy else TextSanft)
        }
    }
}

@Composable
private fun ArbeitKarte(vm: AppViewModel, arbeit: Arbeit, onClick: () -> Unit) {
    val sterne = vm.fortschritt.sterneDerArbeit(arbeit.schluessel, arbeit.uebungen.map { it.titel })
    val anteil = if (arbeit.maxSterne == 0) 0f else sterne.toFloat() / arbeit.maxSterne
    KartenZeile(onClick) {
        Column(Modifier.weight(1f)) {
            Text(arbeit.titel, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${arbeit.klasse} · ${arbeit.fach}", color = TextSanft)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { anteil },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Gelb,
                trackColor = Color.White.copy(alpha = 0.15f),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text("★ $sterne / ${arbeit.maxSterne}", color = Gelb, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EinfuegenDialog(vm: AppViewModel, schliessen: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = schliessen,
        title = { Text("Arbeit einfügen") },
        text = {
            Column {
                Text("Kopiere das JSON aus Claude und füge es hier ein (lange auf das Feld tippen, dann Einfügen).")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; fehler = null },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                )
                fehler?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = Koralle)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = {
                    val f = vm.fuegeArbeitEin(text)
                    if (f == null) schliessen() else fehler = f
                },
            ) { Text("Importieren") }
        },
        dismissButton = { TextButton(onClick = schliessen) { Text("Abbrechen") } },
    )
}

@Composable
fun UebungenScreen(vm: AppViewModel) {
    val arbeit = vm.aktuelleArbeit ?: return
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text(arbeit.titel, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("${arbeit.klasse} · ${arbeit.fach}", color = TextSanft)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(arbeit.uebungen) { i, u ->
                // Die Überschrift der Gruppe steht nur über der ersten Übung der Gruppe.
                val neueGruppe = u.gruppe.isNotEmpty() && (i == 0 || arbeit.uebungen[i - 1].gruppe != u.gruppe)
                Column {
                    if (neueGruppe) {
                        Text(
                            u.gruppe,
                            color = Gelb,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = if (i == 0) 0.dp else 10.dp, bottom = 6.dp),
                        )
                    }
                    UebungZeile(vm, arbeit, i)
                }
            }
        }
    }
}

@Composable
private fun UebungZeile(vm: AppViewModel, arbeit: Arbeit, i: Int) {
    val u = arbeit.uebungen[i]
    val stand = vm.fortschritt.stand(arbeit.schluessel, u.titel)
    KartenZeile(onClick = { vm.startUebung(i) }) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Text(u.symbol.ifEmpty { "•" }, fontSize = 18.sp, color = Gelb, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(u.titel, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                if (stand.runden > 0) "${u.aufgaben.size} Aufgaben · Beste Runde ${stand.beste} richtig"
                else "${u.aufgaben.size} Aufgaben",
                color = TextSanft,
                fontSize = 14.sp,
            )
        }
        Text(sterneText(stand.sterne), color = Gelb, fontSize = 20.sp)
    }
}

@Composable
private fun Titel(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Gelb, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun RundKnopf(symbol: String, betont: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (betont) Gelb else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 20.sp, color = if (betont) Navy else Color.White, fontWeight = FontWeight.Black)
    }
}
