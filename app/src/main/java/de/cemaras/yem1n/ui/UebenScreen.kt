package de.cemaras.yem1n.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import de.cemaras.yem1n.logik.Sonderrunden
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.cemaras.yem1n.daten.Aufgabe
import de.cemaras.yem1n.logik.ErklaerFreund
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import de.cemaras.yem1n.logik.Lob
import de.cemaras.yem1n.logik.Mauer
import de.cemaras.yem1n.logik.RundenStand
import de.cemaras.yem1n.logik.Stand
import de.cemaras.yem1n.ui.theme.Gelb
import de.cemaras.yem1n.ui.theme.Himmel
import de.cemaras.yem1n.ui.theme.Karte
import de.cemaras.yem1n.ui.theme.Koralle
import de.cemaras.yem1n.ui.theme.Mint
import de.cemaras.yem1n.ui.theme.TextSanft

@Composable
fun UebenScreen(vm: AppViewModel) {
    val s = vm.runde ?: return
    val uebung = vm.aktuelleUebung ?: return
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::zurueck) { Text("‹ Zurück", color = Gelb, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text(uebung.titel, color = TextSanft, fontWeight = FontWeight.SemiBold)
        }
        if (s.fertig) {
            Ergebnis(
                s, vm.fortschritt.name, vm.verdient, !(vm.istFehlerheft || vm.istProbe), vm.istProbe,
                vm.neuePokale.map { "${it.emoji} ${it.titel}" }, vm.haustierText,
                onNochmal = vm::nochmal, onFertig = vm::zurueck,
                naechsteTitel = vm.naechsteUebung?.second?.titel, onNaechste = vm::startNaechsteUebung,
            )
        } else {
            if (vm.istProbe) ProbeUhr(vm.probeStart)
            val anteil = s.index.toFloat() / s.aufgaben.size
            LinearProgressIndicator(
                progress = { anteil },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Gelb,
                trackColor = Color.White.copy(alpha = 0.15f),
            )
            Text(
                "Aufgabe ${s.index + 1} von ${s.aufgaben.size}",
                color = TextSanft,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )
            Aufgabenansicht(s, vm)
        }
    }
}

/** Die Uhr der Probearbeit zählt von 30 Minuten herunter. Danach darf das Kind trotzdem weitermachen. */
@Composable
private fun ProbeUhr(start: Long) {
    var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start) {
        while (true) {
            delay(1000)
            jetzt = System.currentTimeMillis()
        }
    }
    val rest = Sonderrunden.PROBE_MINUTEN * 60L - (jetzt - start) / 1000
    val text = if (rest > 0) "⏱ %d:%02d".format(rest / 60, rest % 60) else "⏱ Die Zeit ist um, mach in Ruhe fertig."
    Text(text, color = if (rest > 0) Gelb else Koralle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun ColumnScope.Aufgabenansicht(s: RundenStand, vm: AppViewModel) {
    val a = s.aktuell ?: return
    var e1 by remember(s.index, s.mauerSchritt) { mutableStateOf("") }
    var e2 by remember(s.index) { mutableStateOf("") }
    var tippSichtbar by remember(s.index) { mutableStateOf(false) }
    var notizOffen by remember(s.index) { mutableStateOf(false) }
    val notiz = remember(s.index) { androidx.compose.runtime.mutableStateListOf<List<androidx.compose.ui.geometry.Offset>>() }
    if (notizOffen) Notizblock(notiz) { notizOffen = false }
    val fokus = remember { FocusRequester() }
    val brauchtTastatur = !s.bewertet && (a.art == Aufgabe.ART_ZAHL || a.art == Aufgabe.ART_REST ||
        a.art == Aufgabe.ART_TEXT || a.art == Aufgabe.ART_MAUER)
    LaunchedEffect(s.index, s.mauerSchritt, s.bewertet) {
        if (brauchtTastatur) runCatching { fokus.requestFocus() }
    }

    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (a.frage.isNotEmpty()) {
            Text(a.frage, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
        }
        if (a.tr.isNotEmpty()) {
            Text(a.tr, fontSize = 15.sp, color = Himmel, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))

        if (a.bild.isNotEmpty()) {
            AufgabenBild(a.bild, vm)
            Spacer(Modifier.height(10.dp))
        }
        if (a.gross.isNotEmpty()) {
            Text(a.gross, fontSize = 96.sp, fontWeight = FontWeight.Black, color = Gelb)
        }
        if (a.folge.isNotEmpty()) {
            Text(a.folge.joinToString("  ") + "  ?", fontSize = 40.sp, textAlign = TextAlign.Center)
        }
        if (a.istMauer) {
            Zahlenmauer(a, s)
        } else if (a.rechnung.isNotEmpty()) {
            Text(
                a.rechnung,
                fontSize = if (a.rechnung.length > 24 || a.rechnung.contains('\n')) 22.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))

        if (!s.bewertet) {
            TextButton(onClick = { notizOffen = true }) {
                Text(if (notiz.isEmpty()) "📝 Notizblock" else "📝 Notizblock (beschrieben)", color = Himmel)
            }
        }

        if (a.hinweis.isNotEmpty() && !s.bewertet && !vm.istProbe) {
            if (tippSichtbar) {
                Text(a.hinweis, color = Himmel, fontSize = 18.sp, textAlign = TextAlign.Center)
            } else {
                TextButton(onClick = { tippSichtbar = true }) { Text("Tipp anzeigen", color = Himmel) }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (s.kurzerHinweis.isNotEmpty() && !s.bewertet) {
            Text(s.kurzerHinweis, color = Mint, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }

        if (s.bewertet) {
            Rueckmeldung(s, vm.fortschritt.name, vm)
        } else {
            Eingabe(a, e1, e2, fokus, { e1 = it }, { e2 = it }, vm)
        }
        Spacer(Modifier.height(12.dp))
    }

    // Knöpfe unten, außerhalb des Scrollbereichs
    if (s.bewertet) {
        val letzte = s.index == s.aufgaben.size - 1
        Button(
            onClick = vm::weiter,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(if (letzte) "Fertig" else "Weiter", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    } else {
        val gewaehlt = a.art == Aufgabe.ART_WAHL || a.art == Aufgabe.ART_VERGLEICH
        if (!gewaehlt) {
            Button(
                onClick = { vm.antworte(e1, e2) },
                enabled = e1.isNotBlank() && (a.art != Aufgabe.ART_REST || e2.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Prüfen", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
        TextButton(onClick = vm::zeigeLoesung, modifier = Modifier.fillMaxWidth()) {
            Text("Lösung zeigen", color = TextSanft)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Eingabe(
    a: Aufgabe,
    e1: String,
    e2: String,
    fokus: FocusRequester,
    setze1: (String) -> Unit,
    setze2: (String) -> Unit,
    vm: AppViewModel,
) {
    when (a.art) {
        Aufgabe.ART_VERGLEICH -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (zeichen in listOf("<", "=", ">")) {
                    AuswahlKnopf(zeichen, Modifier.weight(1f)) { vm.antworte(zeichen) }
                }
            }
        }
        Aufgabe.ART_WAHL -> {
            val kurz = a.optionen.all { it.length <= 3 }
            if (kurz) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (o in a.optionen) AuswahlKnopf(o, Modifier.weight(1f)) { vm.antworte(o) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (o in a.optionen) AuswahlKnopf(o, Modifier.fillMaxWidth()) { vm.antworte(o) }
                }
            }
        }
        Aufgabe.ART_REST -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Zahlenfeld(e1, setze1, "Ergebnis", Modifier.weight(1f).focusRequester(fokus), ImeAction.Next, laenge(a.antwort)) {}
                Zahlenfeld(e2, setze2, "Rest", Modifier.weight(1f), ImeAction.Done, laenge(a.antwort2)) { vm.antworte(e1, e2) }
            }
        }
        Aufgabe.ART_TEXT -> {
            OutlinedTextField(
                value = e1,
                onValueChange = setze1,
                modifier = Modifier.fillMaxWidth().focusRequester(fokus),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 26.sp, textAlign = TextAlign.Center),
                placeholder = { Text("Deine Antwort", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.antworte(e1) }),
            )
        }
        else -> {
            Zahlenfeld(e1, setze1, "Antwort", Modifier.fillMaxWidth().focusRequester(fokus), ImeAction.Done, laenge(a.antwort)) {
                vm.antworte(e1)
            }
        }
    }
}

/** Drei Ziffern reichen fast immer. Große Lösungen ab Klasse 4 bekommen sieben Stellen. */
private fun laenge(loesung: String): Int = if (loesung.length > 3) 7 else 3

@Composable
private fun Zahlenfeld(
    wert: String,
    setze: (String) -> Unit,
    beschriftung: String,
    modifier: Modifier,
    ime: ImeAction,
    maxZiffern: Int = 3,
    fertig: () -> Unit,
) {
    OutlinedTextField(
        value = wert,
        // Die Antwortfelder nehmen 3 Ziffern, bei großen Lösungen 7, damit die Länge nichts verrät (wie iOS).
        onValueChange = { neu -> if (neu.length <= maxZiffern && neu.all { it.isDigit() }) setze(neu) },
        modifier = modifier,
        singleLine = true,
        label = { Text(beschriftung) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ime),
        keyboardActions = KeyboardActions(onDone = { fertig() }),
    )
}

@Composable
private fun AuswahlKnopf(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        border = BorderStroke(2.dp, Gelb),
        shape = RoundedCornerShape(18.dp),
    ) { Text(text, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Gelb) }
}

@Composable
private fun Zahlenmauer(a: Aufgabe, s: RundenStand) {
    val aktuellePosition = if (s.bewertet) null else Mauer.position(a.reihen, s.mauerSchritt)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in a.reihen.indices.reversed()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in a.reihen[r].indices) {
                    val wert = s.mauerWerte.getOrNull(r)?.getOrNull(c)
                    val istDran = aktuellePosition == (r to c)
                    Box(
                        Modifier
                            .size(width = 64.dp, height = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (wert != null) Karte.copy(alpha = 0.22f) else Karte)
                            .border(
                                if (istDran) 3.dp else 1.dp,
                                if (istDran) Gelb else Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            wert?.toString() ?: "?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (wert != null) Color.White else TextSanft,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Rueckmeldung(s: RundenStand, name: String, vm: AppViewModel) {
    val stand = s.staende[s.index]
    val farbe = when (stand) {
        Stand.RICHTIG -> Mint
        Stand.FALSCH -> Koralle
        else -> Himmel
    }
    val titel = remember(s.index, stand) {
        when (stand) {
            Stand.RICHTIG -> Lob.fuerRichtig(s.serie, false, name)
            Stand.FALSCH -> Lob.titelDaneben()
            else -> "Hier ist die Lösung"
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(farbe.copy(alpha = 0.16f))
            .border(2.dp, farbe, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(titel, fontSize = 24.sp, fontWeight = FontWeight.Black, color = farbe)
        if (s.erklaerung.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(s.erklaerung, fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
        }
        val aufgabe = s.aktuell
        val gegeben = s.gegeben.getOrNull(s.index).orEmpty()
        if (stand == Stand.FALSCH && aufgabe != null && ErklaerFreund.lohntSich(aufgabe, gegeben)) {
            Spacer(Modifier.height(10.dp))
            EuleErklaert(ErklaerFreund.satz(aufgabe, gegeben), s.index) { vm.sprecher.sprich(it) }
        }
    }
}

/** Der Knopf "Warum war das falsch?" und danach der Satz der Eule. */
@Composable
private fun EuleErklaert(satz: String, schluessel: Int, vorlesen: (String) -> Unit) {
    var offen by remember(schluessel) { mutableStateOf(false) }
    if (!offen) {
        Text(
            "🦉 Warum war das falsch?", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Gelb,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Gelb.copy(alpha = 0.15f))
                .clickable { offen = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    } else {
        Row(verticalAlignment = Alignment.Top) {
            Text("🦉", fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(satz, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "🔊 Vorlesen", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gelb,
                    modifier = Modifier.clickable { vorlesen(satz) },
                )
            }
        }
    }
}

@Composable
private fun Ergebnis(
    s: RundenStand, name: String, muenzen: Int, nochmalMoeglich: Boolean, mitNote: Boolean,
    neueAbzeichen: List<String>, haustier: String?, onNochmal: () -> Unit, onFertig: () -> Unit,
    naechsteTitel: String? = null, onNaechste: () -> Unit = {},
) {
    val ende = remember(s.richtig, s.aufgaben.size) { Lob.fuerEnde(s.richtig, s.aufgaben.size, name) }
    Column(
        Modifier.fillMaxSize().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Geschafft!", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Gelb)
        Spacer(Modifier.height(12.dp))
        Text(sterneText(s.sterne), fontSize = 52.sp, color = Gelb)
        Spacer(Modifier.height(12.dp))
        Text("${s.richtig} von ${s.aufgaben.size} richtig", fontSize = 22.sp, color = Color.White)
        if (mitNote) {
            Text(
                "Note ${Sonderrunden.note(s.richtig, s.aufgaben.size)}",
                fontSize = 28.sp, fontWeight = FontWeight.Black, color = Gelb,
            )
        }
        if (s.angeschaut > 0) {
            Text("${s.angeschaut} Lösungen angeschaut", fontSize = 16.sp, color = Himmel)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            ende,
            fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center,
        )
        if (muenzen > 0) {
            Spacer(Modifier.height(8.dp))
            Text("🪙 +$muenzen Münzen", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gelb)
        }
        if (haustier != null) {
            Spacer(Modifier.height(8.dp))
            Text(haustier, fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
        }
        if (neueAbzeichen.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Neues Abzeichen!", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Gelb)
            neueAbzeichen.forEach { Text(it, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center) }
        }
        Spacer(Modifier.height(28.dp))
        if (naechsteTitel != null) {
            Button(onClick = onNaechste, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Nächste Übung: $naechsteTitel", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(10.dp))
        }
        if (nochmalMoeglich) {
            if (naechsteTitel != null) {
                OutlinedButton(
                    onClick = onNochmal,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = BorderStroke(1.dp, Gelb),
                ) { Text("Nochmal", fontSize = 18.sp, color = Gelb) }
            } else {
                Button(onClick = onNochmal, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Nochmal", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(
            onClick = onFertig,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, Gelb),
        ) { Text("Zur Übersicht", fontSize = 18.sp, color = Gelb) }
    }
}
