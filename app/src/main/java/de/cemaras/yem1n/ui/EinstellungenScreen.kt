package de.cemaras.yem1n.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.daten.Arbeit
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.TextSanft

@Composable
fun EinstellungenScreen(vm: AppViewModel) {
    var name by remember { mutableStateOf(vm.fortschritt.name) }
    var loeschen by remember { mutableStateOf<Arbeit?>(null) }
    var seite by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    androidx.activity.compose.BackHandler(enabled = seite != null) { seite = null }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = { if (seite != null) seite = null else vm.zurueck() }) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
        Text(
            gruppen.firstOrNull { it.id == seite }?.let { "${it.symbol} ${it.titel}" } ?: "Einstellungen",
            fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White,
        )
        if (seite == null) {
            Spacer(Modifier.height(12.dp))
            for (g in gruppen) {
                KartenZeile(onClick = { seite = g.id }) {
                    Text(g.symbol, fontSize = 26.sp)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.titel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(g.text, color = TextSanft, fontSize = 13.sp)
                    }
                    Text("›", color = Gelb, fontSize = 24.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (seite == "ich") {
            Abschnitt("Name des Kindes")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30); vm.setzeName(name) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Vorname") },
            )
            Text("Damit lobt dich die App mit deinem Namen.", color = TextSanft, fontSize = 14.sp)

        }
        if (seite == "ich") {
            Abschnitt("Farbwelt")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val rosa = vm.fortschritt.rosa
                if (!rosa) {
                    Button(onClick = { vm.setzeFarbwelt(false) }) { Text("Blau und Gelb") }
                    OutlinedButton(onClick = { vm.setzeFarbwelt(true) }, border = BorderStroke(1.dp, Gelb)) {
                        Text("Rosa", color = Gelb)
                    }
                } else {
                    OutlinedButton(onClick = { vm.setzeFarbwelt(false) }, border = BorderStroke(1.dp, Gelb)) {
                        Text("Blau und Gelb", color = Gelb)
                    }
                    Button(onClick = { vm.setzeFarbwelt(true) }) { Text("Rosa") }
                }
            }

        }
        if (seite == "ich") {
            Abschnitt("Tagesziel")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { vm.setzeTagesziel(vm.fortschritt.tagesziel - 1) },
                    border = BorderStroke(1.dp, Gelb),
                ) { Text("-", color = Gelb, fontSize = 20.sp) }
                Text(
                    "${vm.fortschritt.tagesziel} ${if (vm.fortschritt.tagesziel == 1) "Runde" else "Runden"} am Tag",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                OutlinedButton(
                    onClick = { vm.setzeTagesziel(vm.fortschritt.tagesziel + 1) },
                    border = BorderStroke(1.dp, Gelb),
                ) { Text("+", color = Gelb, fontSize = 20.sp) }
            }

        }
        if (seite == "bereiche") {
            Abschnitt("Bereiche")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vorschule anzeigen", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.vorschulAn, onCheckedChange = vm::setzeVorschulAn)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sprachen in der Lernwelt (Englisch, Türkisch, Italienisch)", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.sprachenAn, onCheckedChange = vm::setzeSprachenAn)
            }
            Text("Diese Sprachen lernt das Kind im Bereich Sprachen:", color = TextSanft, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            for (sp in de.cemaras.yem1n.logik.Sprachen.alle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${sp.flagge} ${sp.name}", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                    androidx.compose.material3.Switch(checked = sp.code in vm.fortschritt.lernSprachen, onCheckedChange = { vm.setzeLernSprache(sp.code, it) })
                }
            }

        }
        if (seite == "spiele") {
            Abschnitt("Spiele")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spiele erlauben", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = vm.fortschritt.spieleErlaubt, onCheckedChange = vm::setzeSpieleErlaubt)
            }
            if (vm.fortschritt.spieleErlaubt) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { vm.setzeSpieleProTag(vm.fortschritt.spieleProTag - 1) },
                        border = BorderStroke(1.dp, Gelb),
                    ) { Text("-", color = Gelb, fontSize = 20.sp) }
                    Text(
                        "${vm.fortschritt.spieleProTag} ${if (vm.fortschritt.spieleProTag == 1) "Spiel" else "Spiele"} am Tag",
                        color = Color.White, fontSize = 18.sp,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    OutlinedButton(
                        onClick = { vm.setzeSpieleProTag(vm.fortschritt.spieleProTag + 1) },
                        border = BorderStroke(1.dp, Gelb),
                    ) { Text("+", color = Gelb, fontSize = 20.sp) }
                }
            }
            Text("Ein Spiel kostet 15 Münzen und gibt es erst, wenn heute eine Übung geschafft ist.", color = TextSanft, fontSize = 13.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Töne in den Spielen", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.spielTon, onCheckedChange = vm::setzeSpielTon)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibration in den Spielen", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.spielHaptik, onCheckedChange = vm::setzeSpielHaptik)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sparmodus (weniger Bewegung, für ältere Geräte, geht auch mit "Animationen entfernen" in den Android-Einstellungen)", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.sparmodus, onCheckedChange = vm::setzeSparmodus)
            }

        }
        if (seite == "ich") {
            Abschnitt("Lernzeit")
            val ctxZeit = androidx.compose.ui.platform.LocalContext.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tägliche Erinnerung ans Üben", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.lernzeitAn, onCheckedChange = vm::setzeLernzeit)
            }
            if (vm.fortschritt.lernzeitAn) {
                OutlinedButton(onClick = {
                    val m = vm.fortschritt.lernzeitMinuten
                    android.app.TimePickerDialog(ctxZeit, { _, h, min -> vm.setzeLernzeitUhrzeit(h * 60 + min) }, m / 60, m % 60, true).show()
                }) { Text("Uhrzeit: ${de.cemaras.yem1n.logik.LernzeitErinnerung.uhrzeitText(vm.fortschritt.lernzeitMinuten)}") }
                Text("Wer an dem Tag schon geübt hat, wird nicht erinnert.", color = TextSanft, fontSize = 13.sp)
            }

            if (vm.fortschritt.jokerRolle == "eltern") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sonntags um 18 Uhr an den Wochenbericht erinnern", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                    androidx.compose.material3.Switch(checked = vm.fortschritt.wochenberichtAn, onCheckedChange = vm::setzeWochenbericht)
                }
            }

        }
        if (seite == "familie") {
            Abschnitt("Familie und Joker")
            Text("Einwilligung: ${de.cemaras.yem1n.logik.Einwilligung.datumText(vm.fortschritt.einwilligungDatum)}", color = TextSanft, fontSize = 13.sp)
            if (!vm.einwilligungOffen) {
                OutlinedButton(onClick = vm::einwilligungWiderrufen) { Text("Einwilligung widerrufen") }
            }
            val familie = vm.jokerCode
            Text(
                if (familie.isEmpty()) "Noch kein Familiencode. Ohne ihn läuft die App nur auf diesem Gerät."
                else "Familiencode: $familie",
                color = Color.White, fontSize = 16.sp,
            )
            Button(onClick = vm::oeffneJoker) { Text(if (familie.isEmpty()) "Familiencode einrichten" else "Joker und Ergebnisse öffnen") }
            Text(
                "Für Joker und Ergebnisse zwischen den Geräten der Familie. Dafür ist keine Apple-ID nötig.",
                color = TextSanft, fontSize = 13.sp,
            )

        }
        if (seite == "familie") {
            Abschnitt("Klassencode")
            var klassenEingabe by remember { mutableStateOf("") }
            var klassenFehler by remember { mutableStateOf(false) }
            if (vm.fortschritt.klassencode.isEmpty()) {
                Text("Mit dem Klassencode der Lehrerin oder des Admins kommen die Aufgaben der Klasse von selbst. Nur unterschriebene Pakete werden angenommen.", color = TextSanft, fontSize = 13.sp)
                androidx.compose.material3.OutlinedTextField(
                    value = klassenEingabe, onValueChange = { klassenEingabe = it; klassenFehler = false },
                    label = { Text("Klassencode") }, singleLine = true, isError = klassenFehler,
                    supportingText = { if (klassenFehler) Text("Der Code besteht aus 10 Zeichen, zum Beispiel ABCDE-23456.") },
                )
                Button(onClick = { klassenFehler = !vm.setzeKlassencode(klassenEingabe) }, enabled = klassenEingabe.isNotBlank() && !vm.einwilligungOffen) { Text("Eintragen") }
                if (vm.einwilligungOffen) Text("Zuerst oben die Einwilligung der Eltern geben.", color = TextSanft, fontSize = 13.sp)
                Text("Du willst selbst Aufgaben an eine Klasse schicken? Dann legst du eine eigene Klasse an und bist ihr Admin. Nur dieses Gerät darf dann Pakete für den Klassencode unterschreiben.", color = TextSanft, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                OutlinedButton(onClick = vm::klasseAnlegen, enabled = !vm.einwilligungOffen && !vm.adminBeschaeftigt, border = BorderStroke(1.dp, Gelb)) { Text("Eigene Klasse anlegen", color = Gelb) }
                if (vm.adminMeldung.isNotEmpty()) Text(vm.adminMeldung, color = Color.White, fontSize = 14.sp)
            } else {
                Text("Klassencode: ${vm.fortschritt.klassencode}", color = Color.White, fontSize = 16.sp)
                Button(onClick = vm::klassenPaketeHolen, enabled = !vm.klassenBeschaeftigt) { Text(if (vm.klassenBeschaeftigt) "Suche ..." else "Neue Aufgaben holen") }
                OutlinedButton(onClick = vm::entferneKlassencode) { Text("Klassencode entfernen") }
                if (vm.istKlassenAdmin) {
                    androidx.compose.runtime.LaunchedEffect(vm.fortschritt.klassencode) { vm.adminPaketeLaden() }
                    Text("Du bist Admin dieser Klasse. Wähle eine Arbeit, die an die Klasse gehen soll:", color = Gelb, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    vm.arbeiten.forEach { a ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.titel, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("${a.klasse} · ${a.fach}", color = TextSanft, fontSize = 13.sp)
                            }
                            TextButton(onClick = { vm.klassePaketSenden(a) }, enabled = !vm.adminBeschaeftigt) { Text("Senden", color = Gelb) }
                        }
                    }
                    if (vm.adminPakete.isNotEmpty()) {
                        Text("Schon bei der Klasse:", color = TextSanft, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        vm.adminPakete.forEach { (name, titel) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(titel, color = Color.White, modifier = Modifier.weight(1f))
                                TextButton(onClick = { vm.klassePaketLoeschen(name) }, enabled = !vm.adminBeschaeftigt) { Text("Löschen", color = Koralle) }
                            }
                        }
                    }
                    if (vm.adminMeldung.isNotEmpty()) Text(vm.adminMeldung, color = Color.White, fontSize = 14.sp)
                }
            }

        }
        if (seite == "spiele") {
            Abschnitt("Haustier")
            val frageErinnerung = rememberErinnerungsFrage(vm)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Erinnerung am Nachmittag, wenn das Haustier hungrig wird", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.haustierErinnerung, onCheckedChange = {
                    vm.setzeErinnerung(it)
                    if (it) frageErinnerung()
                })
            }

        }
        if (seite == "stimmen") {
            Abschnitt("Vorlesen")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Fragen und Lob laut vorlesen (Vorschule)", color = Color.White, fontSize = 16.sp,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                androidx.compose.material3.Switch(checked = vm.fortschritt.vorlesen, onCheckedChange = vm::setzeVorlesen)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aufgaben beim Üben von selbst vorlesen", color = Color.White, fontSize = 16.sp,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                androidx.compose.material3.Switch(checked = vm.fortschritt.autoVorlesen, onCheckedChange = vm::setzeAutoVorlesen)
            }
            Text("Rechtschreib-Aufgaben und Zahlenmauern werden nicht vorgelesen, sonst wäre die Lösung verraten.", color = TextSanft, fontSize = 13.sp)
            Text("Dafür braucht das Gerät eine deutsche Sprachausgabe. Sie lässt sich in den Android-Einstellungen unter Sprache und Eingabe laden.", color = TextSanft, fontSize = 13.sp)

        }
        if (seite == "bereiche") {
            Abschnitt("Sprachen in der Vorschule")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Englisch anbieten", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.vorschulEnglisch, onCheckedChange = vm::setzeVorschulEnglisch)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Türkisch anbieten", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                androidx.compose.material3.Switch(checked = vm.fortschritt.vorschulTuerkisch, onCheckedChange = vm::setzeVorschulTuerkisch)
            }
            Text("Dann gibt es in der Vorschule einen Umschalter für die Fragen. Vorgelesen wird mit der natürlichen Stimme, wenn sie unten geladen ist, sonst mit der Stimme des Geräts (falls sie die Sprache kann).", color = TextSanft, fontSize = 13.sp)

        }
        if (seite == "stimmen") {
            Abschnitt("Natürliche Stimmen")
            Text(
                "Klingt viel natürlicher als die Stimme des Geräts. Die Stimme wird einmal aus dem Internet geladen (am besten im WLAN) und liest danach ohne Internet vor.",
                color = TextSanft, fontSize = 13.sp,
            )
            for (p in de.cemaras.yem1n.logik.StimmPaket.stimmen) {
                val flagge = when (p.sprache) { "tr" -> "🇹🇷 Türkisch"; "en" -> "🇬🇧 Englisch"; else -> "🇩🇪 Deutsch" }
                val prozent = vm.stimmenLaden[p.id]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("$flagge: ${p.name}", color = Color.White, fontSize = 16.sp)
                        Text(
                            if (prozent != null) "Lädt ... $prozent %" else if (vm.stimmeDa(p.sprache)) "Geladen" else "${p.megabyte} MB",
                            color = TextSanft, fontSize = 13.sp,
                        )
                    }
                    if (prozent != null) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { prozent / 100f }, color = Gelb, modifier = Modifier.padding(4.dp).size(28.dp),
                        )
                    } else if (vm.stimmeDa(p.sprache)) {
                        TextButton(onClick = { vm.probeStimme(p.sprache) }) { Text("Hören", color = Gelb) }
                        TextButton(onClick = { vm.entferneStimme(p.sprache) }) { Text("Löschen", color = TextSanft) }
                    } else {
                        OutlinedButton(onClick = { vm.ladeStimme(p.sprache) }, border = BorderStroke(1.dp, Gelb)) { Text("Laden", color = Gelb) }
                    }
                }
            }
            vm.stimmenFehler?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 14.sp) }

        }
        if (seite == "info") {
            Abschnitt("Hilfe")
            OutlinedButton(
                onClick = vm::oeffneInfo,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Gelb),
            ) { Text("Info, Anleitung und Datenschutz", color = Gelb) }

        }
        if (seite == "info") {
            Abschnitt("Arbeiten")
            if (vm.arbeiten.isEmpty()) {
                Text("Keine Arbeiten vorhanden. Auf der Startseite lassen sich neue einfügen.", color = TextSanft)
            }
            vm.arbeiten.forEach { a ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(a.titel, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${a.klasse} · ${a.fach}", color = TextSanft, fontSize = 14.sp)
                    }
                    TextButton(onClick = { loeschen = a }) { Text("Löschen", color = Koralle) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    loeschen?.let { a ->
        AlertDialog(
            onDismissRequest = { loeschen = null },
            title = { Text("\"${a.titel}\" löschen?") },
            text = { Text("Die Arbeit verschwindet von diesem Gerät. Du kannst sie später wieder einfügen.") },
            confirmButton = {
                Button(onClick = { vm.loescheArbeit(a); loeschen = null }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { loeschen = null }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun Abschnitt(titel: String) {
    Spacer(Modifier.height(20.dp))
    Text(titel, color = Gelb, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
}

private class EinstellGruppe(val id: String, val symbol: String, val titel: String, val text: String)

private val gruppen = listOf(
    EinstellGruppe("ich", "👤", "Ich", "Name, Farben, Tagesziel, Lernzeit"),
    EinstellGruppe("bereiche", "🧩", "Bereiche", "Vorschule und Sprachen ein- oder ausblenden"),
    EinstellGruppe("spiele", "🎮", "Spiele und Haustier", "Spielzeit, Töne, Erinnerung"),
    EinstellGruppe("stimmen", "🔊", "Vorlesen und Stimmen", "Sprachausgabe und natürliche Stimmen"),
    EinstellGruppe("familie", "☁️", "Familie und Cloud", "Einwilligung, Familiencode, Klassencode"),
    EinstellGruppe("info", "ℹ️", "Info und Arbeiten", "Anleitung, Datenschutz, Arbeiten löschen"),
)
